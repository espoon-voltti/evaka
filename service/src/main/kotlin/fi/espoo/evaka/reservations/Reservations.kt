// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import fi.espoo.evaka.attendance.deleteAttendancesByDate
import fi.espoo.evaka.attendance.getChildPlacementTypes
import fi.espoo.evaka.attendance.insertAttendance
import fi.espoo.evaka.daycare.getClubTerms
import fi.espoo.evaka.daycare.getPreschoolTerms
import fi.espoo.evaka.daycare.service.AbsenceCategory
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.daycare.service.AbsenceUpsert
import fi.espoo.evaka.daycare.service.FullDayAbsenseUpsert
import fi.espoo.evaka.daycare.service.clearOldAbsences
import fi.espoo.evaka.daycare.service.clearOldCitizenEditableAbsences
import fi.espoo.evaka.daycare.service.getAbsenceDatesForChildrenInRange
import fi.espoo.evaka.daycare.service.insertAbsences
import fi.espoo.evaka.daycare.service.upsertFullDayAbsences
import fi.espoo.evaka.holidayperiod.getHolidayPeriodsInRange
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.ScheduleType
import fi.espoo.evaka.shared.AbsenceId
import fi.espoo.evaka.shared.AttendanceId
import fi.espoo.evaka.shared.AttendanceReservationId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EvakaUserId
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

    val reservableRange = getReservableRange(now, citizenReservationThresholdHours)
    if (isCitizen && !requests.all { request -> reservableRange.includes(request.date) }) {
        throw BadRequest("Some days are not reservable", "NON_RESERVABLE_DAYS")
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
        if (isCitizen) {
            tx.clearOldCitizenEditableAbsences(
                validated.map { it.childId to it.date },
                reservableRange
            )
        } else {
            tx.clearOldAbsences(validated.map { it.childId to it.date })
        }

    // Keep old reservations in the confirmed range if absences are added on top of them
    val (absenceRequests, otherRequests) =
        validated.partition { it is DailyReservationRequest.Absent }
    val deletedReservations =
        tx.clearOldReservations(
            absenceRequests
                .filter { reservableRange.includes(it.date) }
                .map { it.childId to it.date } + otherRequests.map { it.childId to it.date }
        )

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
            FullDayAbsenseUpsert(
                it.childId,
                it.date,
                if (hasContractDays && reservableRange.includes(it.date))
                    AbsenceType.PLANNED_ABSENCE
                else AbsenceType.OTHER_ABSENCE
            )
        }
    val upsertedAbsences =
        if (absences.isNotEmpty()) {
            tx.upsertFullDayAbsences(userId, now, absences)
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

data class ChildDatePresence(
    val date: LocalDate,
    val childId: ChildId,
    val unitId: DaycareId,
    val reservations: List<Reservation>,
    val attendances: List<OpenTimeRange>,
    val absenceBillable: AbsenceType?,
    val absenceNonbillable: AbsenceType?
)

data class UpsertChildDatePresenceResult(
    val insertedReservations: List<AttendanceReservationId>,
    val deletedReservations: List<AttendanceReservationId>,
    val insertedAttendances: List<AttendanceId>,
    val deletedAttendances: List<AttendanceId>,
    val insertedAbsences: List<AbsenceId>,
    val deletedAbsences: List<AbsenceId>,
)

fun upsertChildDatePresence(
    tx: Database.Transaction,
    userId: EvakaUserId,
    now: HelsinkiDateTime,
    input: ChildDatePresence
): UpsertChildDatePresenceResult {
    val placementType =
        tx.getChildPlacementTypes(setOf(input.childId), input.date)[input.childId]
            ?: throw BadRequest("No placement")
    input.validate(now, placementType)

    // check which of the reservations already exist, so that they won't be unnecessarily replaced
    // and metadata lost
    val reservations =
        input.reservations.map { reservation ->
            val existingId =
                tx.createQuery<Any> {
                        sql(
                            """
            SELECT id 
            FROM attendance_reservation ar
            WHERE date = ${bind(input.date)} AND child_id = ${bind(input.childId)} AND
            ${when (reservation) {
                is Reservation.NoTimes -> "start_time IS NULL AND end_time IS NULL"
                is Reservation.Times -> "start_time = ${bind(reservation.startTime)} AND end_time = ${bind(reservation.endTime)}"
            }}
        """
                        )
                    }
                    .exactlyOneOrNull<AttendanceReservationId>()
            reservation to existingId
        }

    val deletedReservations =
        tx.deleteReservations(
            date = input.date,
            childId = input.childId,
            skip = reservations.mapNotNull { it.second }
        )

    val insertedReservations =
        reservations
            .filter { it.second == null }
            .map { tx.insertReservation(userId, input.date, input.childId, it.first) }

    val deletedAttendances = tx.deleteAttendancesByDate(childId = input.childId, date = input.date)
    val insertedAttendances =
        input.attendances.map { attendance ->
            tx.insertAttendance(
                input.childId,
                input.unitId,
                input.date,
                attendance.startTime,
                attendance.endTime
            )
        }

    val absenceChanges =
        AbsenceCategory.entries.map { category ->
            val type =
                when (category) {
                    AbsenceCategory.BILLABLE -> input.absenceBillable
                    AbsenceCategory.NONBILLABLE -> input.absenceNonbillable
                }
            val identicalAbsenceExists =
                type != null &&
                    tx.absenceExists(
                        date = input.date,
                        childId = input.childId,
                        category = category,
                        type = type
                    )
            val deletedAbsence =
                if (identicalAbsenceExists) {
                    // do not delete and replace identical absence so that metadata is not lost
                    null
                } else {
                    tx.deleteAbsenceOfCategory(input.date, input.childId, category)
                }

            val insertedAbsence =
                if (type == null || identicalAbsenceExists) {
                    null
                } else {
                    tx.insertAbsences(
                            now,
                            userId,
                            listOf(AbsenceUpsert(input.childId, input.date, category, type))
                        )
                        .first()
                }

            insertedAbsence to deletedAbsence
        }

    return UpsertChildDatePresenceResult(
        insertedReservations = insertedReservations,
        deletedReservations = deletedReservations,
        insertedAttendances = insertedAttendances,
        deletedAttendances = deletedAttendances,
        insertedAbsences = absenceChanges.mapNotNull { it.first },
        deletedAbsences = absenceChanges.mapNotNull { it.second }
    )
}

private fun ChildDatePresence.validate(now: HelsinkiDateTime, placementType: PlacementType) {
    if (reservations.size > 2) throw BadRequest("Too many reservations")
    if (reservations.map { it == Reservation.NoTimes }.distinct().size > 1)
        throw BadRequest("Mixed reservation types")
    reservations.filterIsInstance<Reservation.Times>().forEach { r ->
        if (!r.endTime.isAfter(r.startTime)) throw BadRequest("Inverted time range")
    }
    reservations.filterIsInstance<Reservation.Times>().zipWithNext().forEach { (r1, r2) ->
        if (r2.startTime.isBefore(r1.endTime)) throw BadRequest("Overlapping reservation times")
    }

    attendances.forEach { a ->
        if (a.endTime != null && !a.endTime.isAfter(a.startTime))
            throw BadRequest("Inverted time range")
    }
    attendances.zipWithNext().forEach { (a1, a2) ->
        if (a1.endTime == null || a2.startTime.isBefore(a1.endTime))
            throw BadRequest("Overlapping attendance times")
    }
    attendances
        .flatMap { listOfNotNull(it.startTime, it.endTime) }
        .map { HelsinkiDateTime.of(date, it) }
        .forEach {
            if (it.isAfter(now.plusMinutes(30))) {
                throw BadRequest("Cannot mark attendances into future")
            }
        }

    if (
        (absenceBillable != null &&
            !placementType.absenceCategories().contains(AbsenceCategory.BILLABLE)) ||
            (absenceNonbillable != null &&
                !placementType.absenceCategories().contains(AbsenceCategory.NONBILLABLE))
    ) {
        throw BadRequest("Invalid absence category")
    }
}

private fun Database.Transaction.deleteReservations(
    date: LocalDate,
    childId: ChildId,
    skip: List<AttendanceReservationId>
): List<AttendanceReservationId> {
    return createQuery<Any> {
            sql(
                """
        DELETE FROM attendance_reservation
        WHERE date = ${bind(date)} AND child_id = ${bind(childId)} AND NOT (id = ANY (${bind(skip)}))
        RETURNING id
    """
            )
        }
        .toList<AttendanceReservationId>()
}

private fun Database.Transaction.insertReservation(
    userId: EvakaUserId,
    date: LocalDate,
    childId: ChildId,
    reservation: Reservation
): AttendanceReservationId {
    return createQuery<Any> {
            sql(
                """
        INSERT INTO attendance_reservation (child_id, created_by, date, start_time, end_time) 
        VALUES (${bind(childId)}, ${bind(userId)}, ${bind(date)}, ${bind(reservation.asTimeRange()?.start)}, ${bind(reservation.asTimeRange()?.end)})
        RETURNING id
    """
            )
        }
        .exactlyOne<AttendanceReservationId>()
}

private fun Database.Read.absenceExists(
    date: LocalDate,
    childId: ChildId,
    category: AbsenceCategory,
    type: AbsenceType
): Boolean {
    return createQuery<Any> {
            sql(
                """
        SELECT exists(
            SELECT 1 FROM absence
            WHERE child_id = ${bind(childId)} 
                AND date = ${bind(date)} 
                AND category = ${bind(category)} 
                AND absence_type = ${bind(type)}
        )
    """
            )
        }
        .exactlyOne()
}

private fun Database.Transaction.deleteAbsenceOfCategory(
    date: LocalDate,
    childId: ChildId,
    category: AbsenceCategory
): AbsenceId? {
    return createQuery<Any>() {
            sql(
                """
            DELETE FROM absence
            WHERE child_id = ${bind(childId)} AND date = ${bind(date)} AND category = ${bind(category)}
            RETURNING id
    """
            )
        }
        .exactlyOneOrNull()
}
