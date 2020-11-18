// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.service

import fi.espoo.evaka.pis.dao.PGConstants
import fi.espoo.evaka.pis.dao.mapPSQLException
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.getUUID
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.NotFound
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class CaretakerService {
    fun getCaretakers(tx: Database.Read, groupId: UUID): List<CaretakerAmount> {
        // language=sql
        val sql =
            """
            SELECT id, group_id, start_date, end_date, amount
            FROM daycare_caretaker
            WHERE group_id = :groupId
            ORDER BY start_date DESC
            """.trimIndent()

        return tx.createQuery(sql)
            .bind("groupId", groupId)
            .map { rs, _ ->
                CaretakerAmount(
                    id = rs.getUUID("id"),
                    groupId = rs.getUUID("group_id"),
                    startDate = rs.getDate("start_date").toLocalDate(),
                    endDate = rs.getDate("end_date").toLocalDate().takeIf { it.isBefore(PGConstants.infinity) },
                    amount = rs.getDouble("amount")
                )
            }
            .toList()
    }

    fun insert(tx: Database.Transaction, groupId: UUID, startDate: LocalDate, endDate: LocalDate?, amount: Double) {
        if (endDate != null && endDate.isBefore(startDate)) throw BadRequest("End date cannot be before start")

        tx.endPreviousRow(groupId, startDate)

        val end = if (endDate == null) "DEFAULT" else ":end"
        // language=sql
        val sql =
            """
            INSERT INTO daycare_caretaker (group_id, start_date, end_date, amount) 
            VALUES (:groupId, :start, $end, :amount)
            """.trimIndent()

        try {
            tx.createUpdate(sql)
                .bind("groupId", groupId)
                .bind("start", startDate)
                .bind("end", endDate)
                .bind("amount", amount)
                .execute()
        } catch (e: Exception) {
            throw mapPSQLException(e)
        }
    }

    private fun Database.Transaction.endPreviousRow(groupId: UUID, startDate: LocalDate) {
        // language=sql
        val sql =
            """
            UPDATE daycare_caretaker
            SET end_date = :start - interval '1 day'
            WHERE group_id = :groupId AND start_date < :start AND end_date >= :start
            """.trimIndent()

        createUpdate(sql).bind("groupId", groupId).bind("start", startDate).execute()
    }

    fun update(tx: Database.Transaction, groupId: UUID, id: UUID, startDate: LocalDate, endDate: LocalDate?, amount: Double) {
        if (endDate != null && endDate.isBefore(startDate)) throw BadRequest("End date cannot be before start")

        val end = if (endDate == null) "DEFAULT" else ":end"
        // language=sql
        val sql =
            """
            UPDATE daycare_caretaker
            SET start_date = :start, end_date = $end, amount = :amount
            WHERE id = :id AND group_id = :groupId
            """.trimIndent()

        try {
            tx.createUpdate(sql)
                .bind("groupId", groupId)
                .bind("id", id)
                .bind("start", startDate)
                .bind("end", endDate)
                .bind("amount", amount)
                .execute()
                .let { updated -> if (updated == 0) throw NotFound("Caretakers $id not found") }
        } catch (e: Exception) {
            throw mapPSQLException(e)
        }
    }

    fun delete(tx: Database.Transaction, groupId: UUID, id: UUID) {
        tx.createUpdate("DELETE FROM daycare_caretaker WHERE id = :id AND group_id = :groupId")
            .bind("groupId", groupId)
            .bind("id", id)
            .execute()
            .let { deleted -> if (deleted == 0) throw NotFound("Caretakers $id not found") }
    }
}

data class CaretakerAmount(
    val id: UUID,
    val groupId: UUID,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val amount: Double
)
