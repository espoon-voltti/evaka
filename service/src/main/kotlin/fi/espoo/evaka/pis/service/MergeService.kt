// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.NotifyFamilyUpdated
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.getUUID
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class MergeService(private val asyncJobRunner: AsyncJobRunner) {
    fun mergePeople(tx: Database.Transaction, master: UUID, duplicate: UUID) {
        // language=sql
        val feeAffectingDatesSQL =
            """
            WITH dates AS (
                SELECT min(start_date) AS min_date, max(end_date) AS max_date FROM fridge_partner SET WHERE person_id = :id_duplicate
                UNION
                SELECT min(start_date) AS min_date, max(end_date) AS max_date FROM fridge_child SET WHERE head_of_child = :id_duplicate OR child_id = :id_duplicate
                UNION
                SELECT min(valid_from) AS min_date, max(coalesce(valid_to, 'infinity'::date)) AS max_date FROM income WHERE person_id = :id_duplicate
                UNION
                SELECT min(valid_from) AS min_date, max(coalesce(valid_to, 'infinity'::date)) AS max_date FROM fee_alteration WHERE person_id = :id_duplicate
                UNION
                SELECT min(start_date) AS min_date, max(end_date) AS max_date FROM placement SET WHERE child_id = :id_duplicate
                UNION
                SELECT min(start_date) AS min_date, max(end_date) AS max_date FROM service_need SET WHERE child_id = :id_duplicate
            )
            SELECT min(min_date) AS min_date, max(max_date) AS max_date FROM dates HAVING min(min_date) IS NOT NULL;
            """.trimIndent()
        val feeAffectingDateRange = tx.createQuery(feeAffectingDatesSQL)
            .bind("id_duplicate", duplicate)
            .map { rs, _ ->
                Pair(
                    rs.getDate("min_date").toLocalDate(),
                    rs.getDate("max_date")?.toLocalDate()
                        ?.takeIf { it.isBefore(LocalDate.of(2200, 1, 1)) } // infinity -> null
                )
            }
            .firstOrNull()

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
            
            UPDATE application SET guardian_id = :id_master WHERE guardian_id = :id_duplicate;
            UPDATE fridge_partner SET person_id = :id_master WHERE person_id = :id_duplicate;
            UPDATE fridge_child SET head_of_child = :id_master WHERE head_of_child = :id_duplicate;
            UPDATE fee_decision SET head_of_family = :id_master WHERE head_of_family = :id_duplicate;
            UPDATE fee_decision SET partner = :id_master WHERE partner = :id_duplicate;
            UPDATE income SET person_id = :id_master WHERE person_id = :id_duplicate;
            UPDATE invoice SET head_of_family = :id_master WHERE head_of_family = :id_duplicate;
            
            UPDATE absence SET child_id = :id_master WHERE child_id = :id_duplicate;
            UPDATE application SET child_id = :id_master WHERE child_id = :id_duplicate;
            UPDATE assistance_need SET child_id = :id_master WHERE child_id = :id_duplicate;
            UPDATE assistance_action SET child_id = :id_master WHERE child_id = :id_duplicate;
            UPDATE backup_care SET child_id = :id_master WHERE child_id = :id_duplicate;
            UPDATE fee_alteration SET person_id = :id_master WHERE person_id = :id_duplicate;
            UPDATE fee_decision_part SET child = :id_master WHERE child = :id_duplicate;
            UPDATE fridge_child SET child_id = :id_master WHERE child_id = :id_duplicate;
            UPDATE invoice_row SET child = :id_master WHERE child = :id_duplicate;
            UPDATE placement SET child_id = :id_master WHERE child_id = :id_duplicate;
            UPDATE service_need SET child_id = :id_master WHERE child_id = :id_duplicate;
            
            UPDATE message SET sender_id = (SELECT id FROM message_account WHERE person_id = :id_master) WHERE sender_id = (SELECT id FROM message_account WHERE person_id = :id_duplicate);
            UPDATE message_content SET author_id = (SELECT id FROM message_account WHERE person_id = :id_master) WHERE author_id = (SELECT id FROM message_account WHERE person_id = :id_duplicate);
            UPDATE message_recipients SET recipient_id = (SELECT id FROM message_account WHERE person_id = :id_master) WHERE recipient_id = (SELECT id FROM message_account WHERE person_id = :id_duplicate);
            """.trimIndent()
        tx.createUpdate(updateSQL)
            .bind("id_master", master)
            .bind("id_duplicate", duplicate)
            .execute()

        if (feeAffectingDateRange != null) {
            // language=sql
            val parentsSQL =
                """
                SELECT DISTINCT head_of_child
                FROM fridge_child
                WHERE head_of_child = :id OR child_id = :id
                """.trimIndent()
            tx.createQuery(parentsSQL)
                .bind("id", master)
                .map { rs, _ -> rs.getUUID("head_of_child") }
                .forEach { parentId ->
                    sendFamilyUpdatedMessage(tx, parentId, feeAffectingDateRange.first, feeAffectingDateRange.second)
                }
        }
    }

    fun deleteEmptyPerson(tx: Database.Transaction, id: UUID) {
        // language=sql
        val sql1 =
            """
            SELECT ensure_empty_person(:id);
            """.trimIndent()

        tx.createUpdate(sql1).bind("id", id).execute()

        // language=sql
        val sql2 =
            """
            DELETE FROM message_account WHERE person_id = :id;
            DELETE FROM child WHERE id = :id;
            DELETE FROM person WHERE id = :id;
            """.trimIndent()

        tx.createUpdate(sql2).bind("id", id).execute()
    }

    private fun sendFamilyUpdatedMessage(tx: Database.Transaction, adultId: UUID, startDate: LocalDate, endDate: LocalDate?) {
        asyncJobRunner.plan(tx, listOf(NotifyFamilyUpdated(adultId, startDate, endDate)))
    }
}
