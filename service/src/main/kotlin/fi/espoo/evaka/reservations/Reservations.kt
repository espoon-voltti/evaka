// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.daycare.service.clearOldCitizenEditableAbsences
import fi.espoo.evaka.daycare.service.getAbsenceDatesForChildrenInRange
import fi.espoo.evaka.holidayperiod.getHolidayPeriodsInRange
import fi.espoo.evaka.shared.AbsenceId
import fi.espoo.evaka.shared.AttendanceReservationId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate
import java.time.LocalTime

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
sealed interface DailyReservationRequest {
    val childId: ChildId
    val date: LocalDate

    @JsonTypeName("RESERVATIONS")
    data class Reservations(
        override val childId: ChildId,
        override val date: LocalDate,
        val reservation: Reservation,
        val secondReservation: Reservation? = null
    ) : DailyReservationRequest {
        fun hasTimes() =
            reservation is Reservation.Times &&
                (secondReservation == null || secondReservation is Reservation.Times)
        fun convertMidnightEndTime() =
            this.copy(
                reservation = reservation.convertMidnightEndTime(),
                secondReservation = secondReservation?.convertMidnightEndTime()
            )
    }

    @JsonTypeName("ABSENCE")
    data class Absence(
        override val childId: ChildId,
        override val date: LocalDate,
    ) : DailyReservationRequest

    @JsonTypeName("NOTHING")
    data class Nothing(
        override val childId: ChildId,
        override val date: LocalDate,
    ) : DailyReservationRequest
}

fun reservationRequestRange(body: List<DailyReservationRequest>): FiniteDateRange {
    val minDate = body.minOfOrNull { it.date } ?: throw BadRequest("No requests")
    val maxDate = body.maxOfOrNull { it.date } ?: throw BadRequest("No requests")
    return FiniteDateRange(minDate, maxDate)
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
sealed class Reservation : Comparable<Reservation> {
    @JsonTypeName("NO_TIMES") object NoTimes : Reservation()

    @JsonTypeName("TIMES")
    data class Times(val startTime: LocalTime, val endTime: LocalTime) : Reservation() {
        init {
            if (endTime != LocalTime.of(0, 0) && startTime > endTime) {
                throw IllegalArgumentException("Times must be in order")
            }
        }
    }

    override fun compareTo(other: Reservation): Int {
        return when {
            this is Times && other is Times -> this.startTime.compareTo(other.startTime)
            this is NoTimes && other is NoTimes -> 0
            this is NoTimes && other is Times -> -1
            this is Times && other is NoTimes -> 1
            else -> throw IllegalStateException("Unknown reservation type")
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

    fun convertMidnightEndTime() =
        if (this is Times && this.endTime == LocalTime.of(0, 0)) {
            this.copy(endTime = LocalTime.of(23, 59))
        } else {
            this
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
    today: LocalDate,
    user: AuthenticatedUser,
    requests: List<DailyReservationRequest>
): CreateReservationsResult {
    val (userId, isCitizen) =
        when (user) {
            is AuthenticatedUser.Citizen -> Pair(user.evakaUserId, true)
            is AuthenticatedUser.Employee -> Pair(user.evakaUserId, false)
            else -> throw BadRequest("Invalid user type")
        }

    val reservationsRange = reservationRequestRange(requests)
    val holidayPeriods = tx.getHolidayPeriodsInRange(reservationsRange)
    val childAbsenceDates =
        tx.getAbsenceDatesForChildrenInRange(requests.map { it.childId }.toSet(), reservationsRange)

    val (open, closed) = holidayPeriods.partition { it.reservationDeadline >= today }
    val openHolidayPeriodDates = open.flatMap { it.period.dates() }.toSet()
    val closedHolidayPeriodDates = closed.flatMap { it.period.dates() }.toSet()

    val validated =
        requests
            .mapNotNull { request ->
                val isOpenHolidayPeriod = openHolidayPeriodDates.contains(request.date)
                val isClosedHolidayPeriod = closedHolidayPeriodDates.contains(request.date)

                if (isOpenHolidayPeriod) {
                    // Everything is allowed on open holiday periods
                    request
                } else if (isClosedHolidayPeriod) {
                    // Only reservations with times are allowed on closed holiday periods
                    if (request is DailyReservationRequest.Reservations && !request.hasTimes()) {
                        throw BadRequest("Reservations in closed holiday periods must have times")
                    }
                    if (isCitizen) {
                        // Citizens cannot override absences on closed holiday periods. They're just
                        // filtered out
                        // here because they cannot be reliably filtered out in the UI.
                        val hasAbsence =
                            (childAbsenceDates[request.childId] ?: setOf()).contains(request.date)
                        if (request is DailyReservationRequest.Reservations && hasAbsence) {
                            null
                        } else {
                            request
                        }
                    } else {
                        request
                    }
                } else {
                    // Not a holiday period - only reservations with times are allowed
                    if (request is DailyReservationRequest.Reservations && !request.hasTimes()) {
                        throw BadRequest("Reservations outside holiday periods must have times")
                    }
                    request
                }
            }
            .map { request ->
                if (request is DailyReservationRequest.Reservations) {
                    request.convertMidnightEndTime()
                } else {
                    request
                }
            }

    val deletedAbsences =
        tx.clearOldCitizenEditableAbsences(validated.map { it.childId to it.date })
    val deletedReservations = tx.clearOldReservations(validated.map { it.childId to it.date })
    val upsertedReservations =
        tx.insertValidReservations(
            user.evakaUserId,
            validated.filterIsInstance<DailyReservationRequest.Reservations>()
        )

    val absences =
        validated.filterIsInstance<DailyReservationRequest.Absence>().map {
            AbsenceInsert(it.childId, it.date, AbsenceType.OTHER_ABSENCE)
        }
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
