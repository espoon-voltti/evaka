// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import fi.espoo.evaka.pis.getTransferablePersonReferences
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.db.mapPSQLException
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import java.time.LocalDate
import org.springframework.stereotype.Service

@Service
class MergeService(private val asyncJobRunner: AsyncJobRunner<AsyncJob>) {
    fun mergePeople(
        tx: Database.Transaction,
        clock: EvakaClock,
        master: PersonId,
        duplicate: PersonId
    ) {
        // language=sql
        val feeAffectingDatesSQL =
            """
            WITH dates AS (
                SELECT min(start_date) AS min_date, max(coalesce(end_date, 'infinity'::date)) AS max_date FROM fridge_partner SET WHERE person_id = :id_duplicate
                UNION
                SELECT min(start_date) AS min_date, max(end_date) AS max_date FROM fridge_child SET WHERE head_of_child = :id_duplicate OR child_id = :id_duplicate
                UNION
                SELECT min(valid_from) AS min_date, max(coalesce(valid_to, 'infinity'::date)) AS max_date FROM income WHERE person_id = :id_duplicate
                UNION
                SELECT min(valid_from) AS min_date, max(coalesce(valid_to, 'infinity'::date)) AS max_date FROM fee_alteration WHERE person_id = :id_duplicate
                UNION
                SELECT min(start_date) AS min_date, max(end_date) AS max_date FROM placement SET WHERE child_id = :id_duplicate
            )
            SELECT min(min_date) AS min_date, max(max_date) AS max_date FROM dates HAVING min(min_date) IS NOT NULL;
            """.trimIndent(
            )
        val feeAffectingDateRange =
            tx.createQuery(feeAffectingDatesSQL)
                .bind("id_duplicate", duplicate)
                .map { row ->
                    DateRange(
                        row.mapColumn("min_date"),
                        row.mapColumn<LocalDate?>("max_date")?.takeIf {
                            it.isBefore(LocalDate.of(2200, 1, 1))
                        } // infinity -> null
                    )
                }
                .firstOrNull()

        val personReferences = tx.getTransferablePersonReferences()

        // language=sql
        val updateSQL =
            """
            INSERT INTO child (id, allergies, diet, additionalinfo) VALUES (
                :id_master,
                coalesce((SELECT allergies FROM child WHERE id = :id_master), ''),
                coalesce((SELECT diet FROM child WHERE id = :id_master), ''),
                coalesce((SELECT additionalinfo FROM child WHERE id = :id_master), '')
            ) ON CONFLICT(id) DO UPDATE SET
                allergies = concat((SELECT allergies FROM child WHERE id = :id_master), ' ', coalesce((SELECT allergies FROM child WHERE id = :id_duplicate), '')),
                diet = concat( (SELECT diet FROM child WHERE id = :id_master), ' ', coalesce((SELECT diet FROM child WHERE id = :id_duplicate), '')),
                additionalinfo = concat((SELECT additionalinfo FROM child WHERE id = :id_master), ' ', coalesce((SELECT additionalinfo FROM child WHERE id = :id_duplicate), ''));
                
            UPDATE child SET allergies = '', diet = '', additionalinfo = '' WHERE id = :id_duplicate;
            
            UPDATE evaka_user SET citizen_id = NULL WHERE citizen_id = :id_duplicate;

            ${personReferences.joinToString(separator = "") { (table, column) ->
                """
                UPDATE $table SET $column = :id_master WHERE $column = :id_duplicate;
            """
            }}
            
            UPDATE message SET sender_id = (SELECT id FROM message_account WHERE person_id = :id_master) WHERE sender_id = (SELECT id FROM message_account WHERE person_id = :id_duplicate);
            UPDATE message_content SET author_id = (SELECT id FROM message_account WHERE person_id = :id_master) WHERE author_id = (SELECT id FROM message_account WHERE person_id = :id_duplicate);
            UPDATE message_recipients SET recipient_id = (SELECT id FROM message_account WHERE person_id = :id_master) WHERE recipient_id = (SELECT id FROM message_account WHERE person_id = :id_duplicate);
            UPDATE message_draft SET account_id = (SELECT id FROM message_account WHERE person_id = :id_master) WHERE account_id = (SELECT id FROM message_account WHERE person_id = :id_duplicate);
            """.trimIndent(
            )

        try {
            tx.createUpdate(updateSQL)
                .bind("id_master", master)
                .bind("id_duplicate", duplicate)
                .execute()
        } catch (e: Exception) {
            throw mapPSQLException(e)
        }

        if (feeAffectingDateRange != null) {
            // language=sql
            val parentsSQL =
                """
                SELECT DISTINCT head_of_child
                FROM fridge_child
                WHERE head_of_child = :id OR child_id = :id
                """.trimIndent(
                )
            tx.createQuery(parentsSQL).bind("id", master).mapTo<PersonId>().forEach { parentId ->
                sendFamilyUpdatedMessage(tx, clock, parentId, feeAffectingDateRange)
            }
        }
    }

    fun deleteEmptyPerson(tx: Database.Transaction, id: PersonId) {
        val personReferences = tx.getTransferablePersonReferences()

        // language=sql
        val sql1 =
            """
            SELECT 
                ${personReferences.joinToString(separator = " + ") { (table, column) ->
                """
                    (SELECT count(*) FROM $table WHERE $column = :id)
                """.trimIndent()
            }} +
                (SELECT count(*) FROM message WHERE sender_id = (SELECT id FROM message_account WHERE person_id = :id)) +
                (SELECT count(*) FROM message_content WHERE author_id = (SELECT id FROM message_account WHERE person_id = :id)) +
                (SELECT count(*) FROM message_recipients WHERE recipient_id = (SELECT id FROM message_account WHERE person_id = :id)) +
                (SELECT count(*) FROM message_draft WHERE account_id = (SELECT id FROM message_account WHERE person_id = :id)) AS count;
            """.trimIndent(
            )

        val referenceCount = tx.createQuery(sql1).bind("id", id).mapTo<Int>().one()
        if (referenceCount > 0) {
            throw Conflict("Person is still referenced from somewhere and cannot be deleted")
        }

        // language=sql
        val sql2 =
            """
            DELETE FROM message_account WHERE person_id = :id;
            DELETE FROM guardian WHERE guardian_id = :id OR child_id = :id;
            DELETE FROM child WHERE id = :id;
            DELETE FROM person WHERE id = :id;
            """.trimIndent(
            )

        tx.createUpdate(sql2).bind("id", id).execute()
    }

    private fun sendFamilyUpdatedMessage(
        tx: Database.Transaction,
        clock: EvakaClock,
        adultId: PersonId,
        dateRange: DateRange
    ) {
        asyncJobRunner.plan(
            tx,
            listOf(AsyncJob.GenerateFinanceDecisions.forAdult(adultId, dateRange)),
            runAt = clock.now()
        )
    }
}
