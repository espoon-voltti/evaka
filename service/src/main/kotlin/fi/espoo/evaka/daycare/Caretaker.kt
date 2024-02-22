// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare

import fi.espoo.evaka.shared.DaycareCaretakerId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapPSQLException
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.NotFound
import java.time.LocalDate
import org.postgresql.util.PSQLException
import org.postgresql.util.PSQLState

fun getCaretakers(tx: Database.Read, groupId: GroupId): List<CaretakerAmount> {
    // language=sql
    val sql =
        """
            SELECT id, group_id, start_date, end_date, amount
            FROM daycare_caretaker
            WHERE group_id = :groupId
            ORDER BY start_date DESC
        """
            .trimIndent()

    @Suppress("DEPRECATION")
    return tx.createQuery(sql).bind("groupId", groupId).toList<CaretakerAmount>()
}

fun insertCaretakers(
    tx: Database.Transaction,
    groupId: GroupId,
    startDate: LocalDate,
    endDate: LocalDate?,
    amount: Double
): DaycareCaretakerId {
    if (endDate != null && endDate.isBefore(startDate))
        throw BadRequest("End date cannot be before start")

    try {
        tx.endPreviousRow(groupId, startDate)
    } catch (e: Exception) {
        throw if (e.cause is PSQLException) {
            when ((e.cause as PSQLException).sqlState) {
                PSQLState.CHECK_VIOLATION.state -> Conflict("Non-unique start date")
                else -> e
            }
        } else {
            e
        }
    }

    try {
        @Suppress("DEPRECATION")
        return tx.createUpdate(
                """
            INSERT INTO daycare_caretaker (group_id, start_date, end_date, amount) 
            VALUES (:groupId, :start, :end, :amount)
            RETURNING id
            """
            )
            .bind("groupId", groupId)
            .bind("start", startDate)
            .bind("end", endDate)
            .bind("amount", amount)
            .executeAndReturnGeneratedKeys()
            .exactlyOne<DaycareCaretakerId>()
    } catch (e: Exception) {
        throw mapPSQLException(e)
    }
}

private fun Database.Transaction.endPreviousRow(groupId: GroupId, startDate: LocalDate) {
    // language=sql
    val sql =
        """
            UPDATE daycare_caretaker
            SET end_date = :start - interval '1 day'
            WHERE group_id = :groupId AND daterange(start_date, end_date, '[]') @> :start
        """
            .trimIndent()

    @Suppress("DEPRECATION")
    createUpdate(sql).bind("groupId", groupId).bind("start", startDate).execute()
}

fun updateCaretakers(
    tx: Database.Transaction,
    groupId: GroupId,
    id: DaycareCaretakerId,
    startDate: LocalDate,
    endDate: LocalDate?,
    amount: Double
) {
    if (endDate != null && endDate.isBefore(startDate))
        throw BadRequest("End date cannot be before start")

    // language=sql
    val sql =
        """
            UPDATE daycare_caretaker
            SET start_date = :start, end_date = :end, amount = :amount
            WHERE id = :id AND group_id = :groupId
        """
            .trimIndent()

    try {
        @Suppress("DEPRECATION")
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

fun deleteCaretakers(tx: Database.Transaction, groupId: GroupId, id: DaycareCaretakerId) {
    @Suppress("DEPRECATION")
    tx.createUpdate("DELETE FROM daycare_caretaker WHERE id = :id AND group_id = :groupId")
        .bind("groupId", groupId)
        .bind("id", id)
        .execute()
        .let { deleted -> if (deleted == 0) throw NotFound("Caretakers $id not found") }
}

data class CaretakerAmount(
    val id: DaycareCaretakerId,
    val groupId: GroupId,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val amount: Double
)
