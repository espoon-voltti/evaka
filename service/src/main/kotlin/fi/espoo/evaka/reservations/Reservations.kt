// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.db.Database
import java.time.LocalDate
import java.time.LocalTime

data class DailyReservationRequest(
    val childId: ChildId,
    val date: LocalDate,
    val reservations: List<TimeRange>?
)

data class TimeRange(
    val startTime: LocalTime,
    val endTime: LocalTime,
)

fun Database.Transaction.clearOldAbsences(childDatePairs: List<Pair<ChildId, LocalDate>>) {
    val batch = prepareBatch(
        "DELETE FROM absence WHERE child_id = :childId AND date = :date"
    )

    childDatePairs.forEach { (childId, date) ->
        batch.bind("childId", childId).bind("date", date).add()
    }

    batch.execute()
}

fun Database.Transaction.clearOldReservations(reservations: List<Pair<ChildId, LocalDate>>) {
    val batch = prepareBatch("DELETE FROM attendance_reservation WHERE child_id = :childId AND date = :date")

    reservations.forEach { (childId, date) ->
        batch
            .bind("childId", childId)
            .bind("date", date)
            .add()
    }

    batch.execute()
}
