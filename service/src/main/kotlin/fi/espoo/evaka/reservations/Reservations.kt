// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import fi.espoo.evaka.shared.db.Database
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class DailyReservationRequest(
    val childId: UUID,
    val date: LocalDate,
    val reservations: List<TimeRange>?
)

data class TimeRange(
    val startTime: LocalTime,
    val endTime: LocalTime,
)

fun Database.Transaction.clearOldAbsences(childDatePairs: List<Pair<UUID, LocalDate>>) {
    val batch = prepareBatch(
        "DELETE FROM absence WHERE child_id = :childId AND date = :date"
    )

    childDatePairs.forEach { (childId, date) ->
        batch.bind("childId", childId).bind("date", date).add()
    }

    batch.execute()
}

fun Database.Transaction.clearOldReservations(reservations: List<Pair<UUID, LocalDate>>) {
    val batch = prepareBatch(
        """
        DELETE FROM attendance_reservation 
        WHERE child_id = :childId AND start_date = :date
        """.trimIndent()
    )

    reservations.forEach { (childId, date) ->
        batch
            .bind("childId", childId)
            .bind("date", date)
            .add()
    }

    batch.execute()
}

fun handleOvernightReserations(reservations: List<TimeRange>?, date: LocalDate) = reservations?.flatMap {
    res ->
    if (!res.endTime.isAfter(res.startTime)) listOf(
        Pair(TimeRange(startTime = res.startTime, endTime = LocalTime.MIDNIGHT.minusMinutes(1)), date),
        Pair(TimeRange(startTime = LocalTime.MIDNIGHT, endTime = res.endTime), date.plusDays(1))
    ) else listOf(Pair(res, date))
}
