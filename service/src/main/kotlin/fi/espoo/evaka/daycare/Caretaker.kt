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

fun getCaretakers(
    tx: Database.Read,
    groupId: GroupId
): List<CaretakerAmount> =
    tx
        .createQuery {
            sql(
                """
            SELECT id, group_id, start_date, end_date, amount
            FROM daycare_caretaker
            WHERE group_id = ${bind(groupId)}
            ORDER BY start_date DESC
        """
            )
        }.toList()

fun insertCaretakers(
    tx: Database.Transaction,
    groupId: GroupId,
    startDate: LocalDate,
    endDate: LocalDate?,
    amount: Double
): DaycareCaretakerId {
    if (endDate != null && endDate.isBefore(startDate)) {
        throw BadRequest("End date cannot be before start")
    }

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
        return tx
            .createUpdate {
                sql(
                    """
INSERT INTO daycare_caretaker (group_id, start_date, end_date, amount) 
VALUES (${bind(groupId)}, ${bind(startDate)}, ${bind(endDate)}, ${bind(amount)})
RETURNING id
"""
                )
            }.executeAndReturnGeneratedKeys()
            .exactlyOne<DaycareCaretakerId>()
    } catch (e: Exception) {
        throw mapPSQLException(e)
    }
}

private fun Database.Transaction.endPreviousRow(
    groupId: GroupId,
    startDate: LocalDate
) {
    execute {
        sql(
            """
UPDATE daycare_caretaker
SET end_date = ${bind(startDate)} - interval '1 day'
WHERE group_id = ${bind(groupId)} AND daterange(start_date, end_date, '[]') @> ${bind(startDate)}
"""
        )
    }
}

fun updateCaretakers(
    tx: Database.Transaction,
    groupId: GroupId,
    id: DaycareCaretakerId,
    startDate: LocalDate,
    endDate: LocalDate?,
    amount: Double
) {
    if (endDate != null && endDate.isBefore(startDate)) {
        throw BadRequest("End date cannot be before start")
    }

    try {
        tx
            .execute {
                sql(
                    """
UPDATE daycare_caretaker
SET start_date = ${bind(startDate)}, end_date = ${bind(endDate)}, amount = ${bind(amount)}
WHERE id = ${bind(id)} AND group_id = ${bind(groupId)}
"""
                )
            }.let { updated -> if (updated == 0) throw NotFound("Caretakers $id not found") }
    } catch (e: Exception) {
        throw mapPSQLException(e)
    }
}

fun deleteCaretakers(
    tx: Database.Transaction,
    groupId: GroupId,
    id: DaycareCaretakerId
) {
    tx
        .execute {
            sql(
                "DELETE FROM daycare_caretaker WHERE id = ${bind(id)} AND group_id = ${bind(groupId)}"
            )
        }.let { deleted -> if (deleted == 0) throw NotFound("Caretakers $id not found") }
}

data class CaretakerAmount(
    val id: DaycareCaretakerId,
    val groupId: GroupId,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val amount: Double
)
