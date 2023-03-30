// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.shared.AbsenceId
import fi.espoo.evaka.shared.AttendanceReservationId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import java.time.LocalTime

fun convertMidnightEndTime(reservation: Reservation) =
    if (
        reservation is Reservation.Times &&
            reservation.endTime == LocalTime.of(0, 0).withNano(0).withSecond(0)
    ) {
        reservation.copy(endTime = LocalTime.of(23, 59))
    } else {
        reservation
    }

// TODO: Allow reservations that have no times for holiday periods, if reserving before the deadline
fun validateReservationTimeRange(reservation: Reservation) {
    if (reservation !is Reservation.Times) {
        throw BadRequest("Reservation must have times")
    }
    if (reservation.endTime <= reservation.startTime) {
        throw BadRequest(
            "Reservation start (${reservation.startTime}) must be before end (${reservation.endTime})"
        )
    }
}

data class DailyReservationRequest(
    val childId: ChildId,
    val date: LocalDate,
    val reservations: List<Reservation>?,
    val absent: Boolean
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
sealed class Reservation {
    @JsonTypeName("NO_TIMES") object NoTimes : Reservation()

    @JsonTypeName("TIMES")
    data class Times(val startTime: LocalTime, val endTime: LocalTime) : Reservation() {
        init {
            if (endTime != LocalTime.of(0, 0) && startTime > endTime) {
                throw IllegalArgumentException("Times must be in order")
            }
        }
    }

    companion object {
        fun fromLocalTimes(startTime: LocalTime?, endTime: LocalTime?) =
            if (startTime != null && endTime != null) {
                Times(startTime, endTime)
            } else if (startTime == null && endTime == null) {
                NoTimes
            } else {
                throw IllegalArgumentException("Both start and end times must be null or not null")
            }
    }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
sealed class ReservationSpan : Comparable<ReservationSpan> {
    @JsonTypeName("NO_TIMES") data class NoTimes(val date: LocalDate) : ReservationSpan()

    @JsonTypeName("TIMES")
    data class Times(val startTime: HelsinkiDateTime, val endTime: HelsinkiDateTime) :
        ReservationSpan() {
        init {
            if (startTime > endTime) {
                throw IllegalArgumentException("Times must be in order")
            }

            // TODO: This broke in production, because Database.Read.getReservationSpans() can
            // return results that span 2 nights
            //
            // val startDate = startTime.toLocalDate()
            // val endDate = endTime.toLocalDate()
            //
            // if (endDate > startDate.plusDays(1)) {
            //     throw IllegalArgumentException("Times can span at most one night")
            // }
        }
    }

    override fun compareTo(other: ReservationSpan): Int {
        return when {
            this is Times && other is Times -> this.startTime.compareTo(other.startTime)
            this is NoTimes && other is NoTimes -> this.date.compareTo(other.date)
            this is NoTimes && other is Times ->
                this.date.compareTo(other.startTime.toLocalDate()).let { if (it == 0) -1 else it }
            this is Times && other is NoTimes ->
                this.startTime.toLocalDate().compareTo(other.date).let { if (it == 0) 1 else it }
            else -> throw IllegalStateException("Unknown reservation range type")
        }
    }
}

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
