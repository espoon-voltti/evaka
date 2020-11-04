// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.service

import fi.espoo.evaka.pis.dao.PGConstants
import fi.espoo.evaka.pis.dao.mapPSQLException
import fi.espoo.evaka.shared.db.getUUID
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.NotFound
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
@Transactional(readOnly = true)
class CaretakerService(
    private val jdbc: NamedParameterJdbcTemplate
) {
    fun getCaretakers(groupId: UUID): List<CaretakerAmount> {
        // language=sql
        val sql =
            """
            SELECT id, group_id, start_date, end_date, amount
            FROM daycare_caretaker
            WHERE group_id = :groupId
            ORDER BY start_date DESC
            """.trimIndent()

        return jdbc.query(
            sql,
            mapOf("groupId" to groupId)
        ) { rs, _ ->
            CaretakerAmount(
                id = rs.getUUID("id"),
                groupId = rs.getUUID("group_id"),
                startDate = rs.getDate("start_date").toLocalDate(),
                endDate = rs.getDate("end_date").toLocalDate().takeIf { it.isBefore(PGConstants.infinity) },
                amount = rs.getDouble("amount")
            )
        }
    }

    @Transactional
    fun insert(groupId: UUID, startDate: LocalDate, endDate: LocalDate?, amount: Double) {
        if (endDate != null && endDate.isBefore(startDate)) throw BadRequest("End date cannot be before start")

        endPreviousRow(groupId, startDate)

        val end = if (endDate == null) "DEFAULT" else ":end"
        // language=sql
        val sql =
            """
            INSERT INTO daycare_caretaker (group_id, start_date, end_date, amount) 
            VALUES (:groupId, :start, $end, :amount)
            """.trimIndent()

        try {
            jdbc.update(
                sql,
                mapOf("groupId" to groupId, "start" to startDate, "end" to endDate, "amount" to amount)
            )
        } catch (e: Exception) {
            throw mapPSQLException(e)
        }
    }

    private fun endPreviousRow(groupId: UUID, startDate: LocalDate) {
        // language=sql
        val sql =
            """
            UPDATE daycare_caretaker
            SET end_date = :start - interval '1 day'
            WHERE group_id = :groupId AND start_date < :start AND end_date >= :start
            """.trimIndent()

        jdbc.update(
            sql,
            mapOf("groupId" to groupId, "start" to startDate)
        )
    }

    @Transactional
    fun update(groupId: UUID, id: UUID, startDate: LocalDate, endDate: LocalDate?, amount: Double) {
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
            jdbc.update(
                sql,
                mapOf("groupId" to groupId, "id" to id, "start" to startDate, "end" to endDate, "amount" to amount)
            ).let { updated -> if (updated == 0) throw NotFound("Caretakers $id not found") }
        } catch (e: Exception) {
            throw mapPSQLException(e)
        }
    }

    @Transactional
    fun delete(groupId: UUID, id: UUID) {
        jdbc.update(
            "DELETE FROM daycare_caretaker WHERE id = :id AND group_id = :groupId",
            mapOf("groupId" to groupId, "id" to id)
        ).let { deleted -> if (deleted == 0) throw NotFound("Caretakers $id not found") }
    }
}

data class CaretakerAmount(
    val id: UUID,
    val groupId: UUID,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val amount: Double
)
