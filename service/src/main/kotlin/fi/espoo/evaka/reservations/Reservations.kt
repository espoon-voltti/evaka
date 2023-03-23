// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.shared.AbsenceId
import fi.espoo.evaka.shared.AttendanceReservationId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import java.time.LocalDate
import java.time.LocalTime

fun convertMidnightEndTime(timeRange: TimeRange) =
    if (timeRange.endTime == LocalTime.of(0, 0).withNano(0).withSecond(0)) {
        timeRange.copy(endTime = LocalTime.of(23, 59))
    } else {
        timeRange
    }

fun validateReservationTimeRange(timeRange: TimeRange) {
    if (timeRange.endTime <= timeRange.startTime) {
        throw BadRequest(
            "Reservation start (${timeRange.startTime}) must be before end (${timeRange.endTime})"
        )
    }
}

data class DailyReservationRequest(
    val childId: ChildId,
    val date: LocalDate,
    val reservations: List<TimeRange>?,
    val absent: Boolean
)

data class TimeRange(val startTime: LocalTime, val endTime: LocalTime)

data class OpenTimeRange(val startTime: LocalTime, val endTime: LocalTime?)

data class CreateReservationsResult(
    val deletedAbsences: List<AbsenceId>,
    val deletedReservations: List<AttendanceReservationId>,
    val upsertedAbsences: List<AbsenceId>,
    val upsertedReservations: List<AttendanceReservationId>
)

fun createReservationsAndAbsences(
    tx: Database.Transaction,
    userId: EvakaUserId,
    reservationRequests: List<DailyReservationRequest>
): CreateReservationsResult {
    val reservations =
        reservationRequests.map {
            it.copy(reservations = it.reservations?.map(::convertMidnightEndTime))
        }
    reservations.forEach { it.reservations?.forEach(::validateReservationTimeRange) }

    val deletedAbsences =
        tx.clearOldCitizenEditableAbsences(
            reservations
                .filter { it.reservations != null || it.absent }
                .map { it.childId to it.date }
        )
    val deletedReservations = tx.clearOldReservations(reservations.map { it.childId to it.date })
    val upsertedReservations =
        tx.insertValidReservations(userId, reservations.filterNot { it.absent })

    val absences =
        reservations
            .filter { it.absent }
            .map { AbsenceInsert(it.childId, it.date, AbsenceType.OTHER_ABSENCE) }
    val upsertedAbsences =
        if (absences.isNotEmpty()) {
            tx.insertAbsences(userId, absences)
        } else {
            emptyList()
        }

    return CreateReservationsResult(
        deletedAbsences,
        deletedReservations,
        upsertedAbsences,
        upsertedReservations
    )
}
