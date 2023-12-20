// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import fi.espoo.evaka.daycare.getClubTerms
import fi.espoo.evaka.daycare.getPreschoolTerms
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.daycare.service.clearOldCitizenEditableAbsences
import fi.espoo.evaka.daycare.service.getAbsenceDatesForChildrenInRange
import fi.espoo.evaka.holidayperiod.getHolidayPeriodsInRange
import fi.espoo.evaka.placement.ScheduleType
import fi.espoo.evaka.shared.AbsenceId
import fi.espoo.evaka.shared.AttendanceReservationId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.TimeRange
import java.time.LocalDate
import java.time.LocalTime

private fun TimeRange.convertMidnightEndTime() =
    if (this.end == LocalTime.of(0, 0)) {
        this.copy(end = LocalTime.of(23, 59))
    } else {
        this
    }

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
sealed interface DailyReservationRequest {
    val childId: ChildId
    val date: LocalDate

    @JsonTypeName("RESERVATIONS")
    data class Reservations(
        override val childId: ChildId,
        override val date: LocalDate,
        val reservation: TimeRange,
        val secondReservation: TimeRange? = null
    ) : DailyReservationRequest {
        fun convertMidnightEndTime() =
            this.copy(
                reservation = reservation.convertMidnightEndTime(),
                secondReservation = secondReservation?.convertMidnightEndTime()
            )
    }

    @JsonTypeName("PRESENT")
    data class Present(
        override val childId: ChildId,
        override val date: LocalDate,
    ) : DailyReservationRequest

    @JsonTypeName("ABSENT")
    data class Absent(
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

    fun asTimeRange(): TimeRange? =
        when (this) {
            is NoTimes -> null
            is Times -> TimeRange(startTime, endTime)
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
    now: HelsinkiDateTime,
    user: AuthenticatedUser,
    requests: List<DailyReservationRequest>,
    citizenReservationThresholdHours: Long
): CreateReservationsResult {
    val (userId, isCitizen) =
        when (user) {
            is AuthenticatedUser.Citizen -> Pair(user.evakaUserId, true)
            is AuthenticatedUser.Employee -> Pair(user.evakaUserId, false)
            else -> throw BadRequest("Invalid user type")
        }

    val today = now.toLocalDate()
    val reservationsRange = reservationRequestRange(requests)
    val clubTerms = tx.getClubTerms()
    val preschoolTerms = tx.getPreschoolTerms()
    val holidayPeriods = tx.getHolidayPeriodsInRange(reservationsRange)

    val childIds = requests.map { it.childId }.toSet()
    val placements = tx.getReservationPlacements(childIds, reservationsRange)
    val contractDayRanges = tx.getReservationContractDayRanges(childIds, reservationsRange)
    val childReservationDates =
        tx.getReservationDatesForChildrenInRange(childIds, reservationsRange)
    val childAbsenceDates = tx.getAbsenceDatesForChildrenInRange(childIds, reservationsRange)

    val (open, closed) = holidayPeriods.partition { it.reservationDeadline >= today }
    val openHolidayPeriodDates = open.flatMap { it.period.dates() }.toSet()
    val closedHolidayPeriodDates = closed.flatMap { it.period.dates() }.toSet()

    val unlockedRange = getReservableRange(now, citizenReservationThresholdHours)

    val isReservableChild = { req: DailyReservationRequest ->
        placements[req.childId]
            ?.find { it.range.includes(req.date) }
            ?.type
            ?.scheduleType(req.date, clubTerms, preschoolTerms) == ScheduleType.RESERVATION_REQUIRED
    }

    val validated =
        requests
            .mapNotNull { request ->
                val isReservable = isReservableChild(request)
                val isOpenHolidayPeriod = openHolidayPeriodDates.contains(request.date)
                val isClosedHolidayPeriod = closedHolidayPeriodDates.contains(request.date)

                if (isOpenHolidayPeriod) {
                    // Everything is allowed on open holiday periods
                    request
                } else if (isClosedHolidayPeriod) {
                    // Only reservations with times are allowed on closed holiday periods
                    if (isReservable && request is DailyReservationRequest.Present) {
                        throw BadRequest("Reservations in closed holiday periods must have times")
                    }
                    if (isCitizen) {
                        // Citizens cannot add reservations on days without existing reservations OR
                        // with absences on closed holiday periods
                        val hasReservation =
                            (childReservationDates[request.childId] ?: setOf()).contains(
                                request.date
                            )
                        val hasAbsence =
                            (childAbsenceDates[request.childId] ?: setOf()).contains(request.date)

                        val isAllowed =
                            when (request) {
                                is DailyReservationRequest.Reservations ->
                                    hasReservation && !hasAbsence
                                is DailyReservationRequest.Present -> hasReservation && !hasAbsence
                                is DailyReservationRequest.Absent -> true
                                is DailyReservationRequest.Nothing -> false
                            }

                        if (isAllowed) {
                            request
                        } else {
                            null
                        }
                    } else {
                        request
                    }
                } else {
                    // Not a holiday period - only reservations with times are allowed
                    if (isReservable && request is DailyReservationRequest.Present) {
                        throw BadRequest("Reservations outside holiday periods must have times")
                    }
                    request
                }
            }
            .map { request ->
                when (request) {
                    is DailyReservationRequest.Reservations,
                    is DailyReservationRequest.Present -> {
                        // Don't create reservations for children whose placement type doesn't
                        // require them
                        if (!isReservableChild(request)) {
                            DailyReservationRequest.Nothing(request.childId, request.date)
                        } else {
                            request
                        }
                    }
                    else -> request
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
            validated.filterIsInstance<DailyReservationRequest.Reservations>().flatMap { res ->
                listOfNotNull(res.reservation, res.secondReservation).map {
                    ReservationInsert(res.childId, res.date, it)
                }
            } +
                validated.filterIsInstance<DailyReservationRequest.Present>().map {
                    ReservationInsert(it.childId, it.date, null)
                }
        )

    val absences =
        validated.filterIsInstance<DailyReservationRequest.Absent>().map {
            val hasContractDays = contractDayRanges[it.childId]?.includes(it.date) ?: false
            AbsenceInsert(
                it.childId,
                it.date,
                if (hasContractDays && unlockedRange.includes(it.date)) AbsenceType.PLANNED_ABSENCE
                else AbsenceType.OTHER_ABSENCE
            )
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
