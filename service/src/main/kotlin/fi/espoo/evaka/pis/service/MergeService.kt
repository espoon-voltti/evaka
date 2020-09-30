// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.NotifyFamilyUpdated
import fi.espoo.evaka.shared.db.getUUID
import fi.espoo.evaka.shared.db.runAfterCommit
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class MergeService(
    private val jdbc: NamedParameterJdbcTemplate,
    private val asyncJobRunner: AsyncJobRunner
) {
    @Transactional
    fun mergePeople(master: UUID, duplicate: UUID) {
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
        val feeAffectingDateRange = jdbc.query(feeAffectingDatesSQL, mapOf("id_duplicate" to duplicate)) { rs, _ ->
            Pair(
                rs.getDate("min_date").toLocalDate(),
                rs.getDate("max_date")?.toLocalDate()?.takeIf { it.isBefore(LocalDate.of(2200, 1, 1)) } // infinity -> null
            )
        }.firstOrNull()

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
            """.trimIndent()
        jdbc.update(
            updateSQL,
            mapOf(
                "id_master" to master,
                "id_duplicate" to duplicate
            )
        )

        if (feeAffectingDateRange != null) {
            // language=sql
            val parentsSQL =
                """
                SELECT DISTINCT head_of_child
                FROM fridge_child
                WHERE head_of_child = :id OR child_id = :id
                """.trimIndent()
            jdbc.query(parentsSQL, mapOf("id" to master)) { rs, _ -> rs.getUUID("head_of_child") }.forEach { parentId ->
                sendFamilyUpdatedMessage(parentId, feeAffectingDateRange.first, feeAffectingDateRange.second)
            }
        }
    }

    @Transactional
    fun deleteEmptyPerson(id: UUID) {
        // language=sql
        val sql1 =
            """
            SELECT ensure_empty_person(:id);
            """.trimIndent()

        jdbc.query(sql1, mapOf("id" to id)) { }

        // language=sql
        val sql2 =
            """
            DELETE FROM child WHERE id = :id;
            DELETE FROM person WHERE id = :id;
            """.trimIndent()

        jdbc.update(sql2, mapOf("id" to id))
    }

    private fun sendFamilyUpdatedMessage(adultId: UUID, startDate: LocalDate, endDate: LocalDate?) {
        asyncJobRunner.plan(listOf(NotifyFamilyUpdated(adultId, startDate, endDate)))
        runAfterCommit { asyncJobRunner.scheduleImmediateRun() }
    }
}
