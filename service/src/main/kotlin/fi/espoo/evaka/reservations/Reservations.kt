// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.absence.AbsenceUpsert
import fi.espoo.evaka.absence.FullDayAbsenseUpsert
import fi.espoo.evaka.absence.clearOldAbsences
import fi.espoo.evaka.absence.clearOldCitizenEditableAbsences
import fi.espoo.evaka.absence.getAbsenceDatesForChildrenInRange
import fi.espoo.evaka.absence.setChildDateAbsences
import fi.espoo.evaka.absence.upsertAbsences
import fi.espoo.evaka.absence.upsertFullDayAbsences
import fi.espoo.evaka.attendance.deleteAttendancesByDate
import fi.espoo.evaka.attendance.getChildPlacementTypes
import fi.espoo.evaka.attendance.insertAttendance
import fi.espoo.evaka.daycare.getClubTerms
import fi.espoo.evaka.daycare.getPreschoolTerms
import fi.espoo.evaka.holidayperiod.HolidayPeriodEffect
import fi.espoo.evaka.holidayperiod.getHolidayPeriodsInRange
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.ScheduleType
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.shared.AbsenceId
import fi.espoo.evaka.shared.AttendanceReservationId
import fi.espoo.evaka.shared.ChildAttendanceId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.data.TimeSet
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.TimeInterval
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.shared.utils.mapOfNotNullValues
import fi.espoo.evaka.user.EvakaUser
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToLong

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
sealed interface DailyReservationRequest {
    val childId: ChildId
    val date: LocalDate

    @JsonTypeName("RESERVATIONS")
    data class Reservations(
        override val childId: ChildId,
        override val date: LocalDate,
        val reservation: TimeRange,
        val secondReservation: TimeRange? = null,
    ) : DailyReservationRequest

    @JsonTypeName("PRESENT")
    data class Present(override val childId: ChildId, override val date: LocalDate) :
        DailyReservationRequest

    @JsonTypeName("ABSENT")
    data class Absent(override val childId: ChildId, override val date: LocalDate) :
        DailyReservationRequest

    @JsonTypeName("NOTHING")
    data class Nothing(override val childId: ChildId, override val date: LocalDate) :
        DailyReservationRequest
}

fun reservationRequestRange(body: List<DailyReservationRequest>): FiniteDateRange {
    val minDate = body.minOfOrNull { it.date } ?: throw BadRequest("No requests")
    val maxDate = body.maxOfOrNull { it.date } ?: throw BadRequest("No requests")
    return FiniteDateRange(minDate, maxDate)
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
sealed class Reservation : Comparable<Reservation> {
    @JsonTypeName("NO_TIMES") object NoTimes : Reservation()

    @JsonTypeName("TIMES") data class Times(val range: TimeRange) : Reservation()

    fun asTimeRange(): TimeRange? =
        when (this) {
            is NoTimes -> null
            is Times -> range
        }

    override fun compareTo(other: Reservation): Int {
        return when {
            this is Times && other is Times -> this.range.start.compareTo(other.range.start)
            this is NoTimes && other is NoTimes -> 0
            this is NoTimes && other is Times -> -1
            this is Times && other is NoTimes -> 1
            else -> throw IllegalStateException("Unknown reservation type")
        }
    }

    companion object {
        fun of(startTime: LocalTime?, endTime: LocalTime?) =
            if (startTime != null && endTime != null) {
                Times(TimeRange(startTime, endTime))
            } else if (startTime == null && endTime == null) {
                NoTimes
            } else {
                throw IllegalArgumentException("Both start and end times must be null or not null")
            }
    }
}

data class AbsenceTypeResponse(val absenceType: AbsenceType, val staffCreated: Boolean)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
sealed class ReservationResponse : Comparable<ReservationResponse> {
    @JsonTypeName("NO_TIMES")
    data class NoTimes(
        val staffCreated: Boolean,
        val modifiedAt: HelsinkiDateTime?,
        val modifiedBy: EvakaUser?,
    ) : ReservationResponse()

    @JsonTypeName("TIMES")
    data class Times(
        val range: TimeRange,
        val staffCreated: Boolean,
        val modifiedAt: HelsinkiDateTime?,
        val modifiedBy: EvakaUser?,
    ) : ReservationResponse()

    override fun compareTo(other: ReservationResponse): Int {
        return when {
            this is Times && other is Times -> this.range.start.compareTo(other.range.start)
            this is NoTimes && other is NoTimes -> 0
            this is NoTimes && other is Times -> -1
            this is Times && other is NoTimes -> 1
            else -> throw IllegalStateException("Unknown reservation type")
        }
    }

    fun asTimeRange(): TimeRange? {
        return when (this) {
            is NoTimes -> null
            is Times -> range
        }
    }

    companion object {
        fun from(reservationRow: ReservationRow) =
            when (reservationRow.reservation) {
                is Reservation.NoTimes ->
                    NoTimes(
                        reservationRow.staffCreated,
                        reservationRow.modifiedAt,
                        reservationRow.modifiedBy,
                    )
                is Reservation.Times ->
                    Times(
                        reservationRow.reservation.range,
                        reservationRow.staffCreated,
                        reservationRow.modifiedAt,
                        reservationRow.modifiedBy,
                    )
            }
    }
}

data class ReservationRow(
    val date: LocalDate,
    val childId: ChildId,
    val reservation: Reservation,
    val staffCreated: Boolean,
    val modifiedAt: HelsinkiDateTime,
    val modifiedBy: EvakaUser,
)

data class CreateReservationsResult(
    val deletedAbsences: List<AbsenceId>,
    val deletedReservations: List<AttendanceReservationId>,
    val upsertedAbsences: List<AbsenceId>,
    val upsertedReservations: List<AttendanceReservationId>,
)

fun createReservationsAndAbsences(
    tx: Database.Transaction,
    now: HelsinkiDateTime,
    user: AuthenticatedUser,
    requests: List<DailyReservationRequest>,
    citizenReservationThresholdHours: Long,
    plannedAbsenceEnabledForHourBasedServiceNeeds: Boolean = false,
): CreateReservationsResult? {
    if (requests.isEmpty()) return null

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
    val placements = tx.getReservationPlacements(childIds, reservationsRange.asDateRange())
    val plannedAbsenceEnabledRanges =
        tx.getPlannedAbsenceEnabledRanges(
            childIds,
            reservationsRange,
            plannedAbsenceEnabledForHourBasedServiceNeeds,
        )
    val childReservationDates =
        tx.getReservationDatesForChildrenInRange(childIds, reservationsRange)
    val childAbsenceDates = tx.getAbsenceDatesForChildrenInRange(childIds, reservationsRange)

    val isReservableChild = { req: DailyReservationRequest ->
        placements[req.childId]
            ?.find { it.range.includes(req.date) }
            ?.type
            ?.scheduleType(req.date, clubTerms, preschoolTerms) == ScheduleType.RESERVATION_REQUIRED
    }
    val reservationEnabledPlacementRangesByChild =
        tx.getReservationEnabledPlacementRangesByChild(childIds)

    val holidayPeriodEffect = { req: DailyReservationRequest ->
        val holidayPeriod = holidayPeriods.find { it.period.includes(req.date) }
        val reservationEnabledPlacementRanges =
            reservationEnabledPlacementRangesByChild[req.childId]
        if (holidayPeriod != null && reservationEnabledPlacementRanges != null) {
            holidayPeriod.effect(today, reservationEnabledPlacementRanges)
        } else {
            null
        }
    }

    val validated =
        requests
            .asSequence()
            .mapNotNull { request ->
                val isReservable = isReservableChild(request)
                val effect = holidayPeriodEffect(request)
                if (effect is HolidayPeriodEffect.NotYetReservable) {
                    // Not allowed to make any changes
                    null
                } else if (effect is HolidayPeriodEffect.ReservationsOpen) {
                    // Everything is allowed on open holiday periods, but reservations with times
                    // are changed to reservations without times in a later step
                    request
                } else if (effect is HolidayPeriodEffect.ReservationsClosed) {
                    // Only reservations with times are allowed on closed holiday periods
                    if (isReservable && request is DailyReservationRequest.Present) {
                        throw BadRequest("Reservations in closed holiday periods must have times")
                    }
                    if (isCitizen) {
                        // Citizens cannot add reservations on days without existing
                        // reservations OR with absences on closed holiday periods
                        val hasReservation =
                            (childReservationDates[request.childId] ?: setOf()).contains(
                                request.date
                            )
                        val hasAbsence =
                            (childAbsenceDates[request.childId] ?: setOf()).contains(request.date)

                        when (request) {
                            // if placement start date is after holiday period response deadline
                            // reservation is allowed
                            is DailyReservationRequest.Reservations,
                            is DailyReservationRequest.Present ->
                                request.takeIf { hasReservation && !hasAbsence }
                            is DailyReservationRequest.Absent -> request
                            is DailyReservationRequest.Nothing -> null
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
            // Don't create reservations for children whose placement type doesn't require them
            .map { request ->
                when (request) {
                    is DailyReservationRequest.Reservations,
                    is DailyReservationRequest.Present -> {
                        if (!isReservableChild(request)) {
                            DailyReservationRequest.Nothing(request.childId, request.date)
                        } else {
                            request
                        }
                    }
                    else -> request
                }
            }
            // Transform `Reservation` to `Present` on open holiday period days
            .map { request ->
                if (
                    request is DailyReservationRequest.Reservations &&
                        holidayPeriodEffect(request) is HolidayPeriodEffect.ReservationsOpen
                ) {
                    DailyReservationRequest.Present(request.childId, request.date)
                } else {
                    request
                }
            }
            .toList()

    val deletedAbsences =
        if (isCitizen) {
            tx.clearOldCitizenEditableAbsences(
                validated.map { it.childId to it.date },
                reservableRange,
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
            now,
            validated.filterIsInstance<DailyReservationRequest.Reservations>().flatMap { res ->
                listOfNotNull(res.reservation, res.secondReservation).map {
                    ReservationInsert(res.childId, res.date, it)
                }
            } +
                validated.filterIsInstance<DailyReservationRequest.Present>().map {
                    ReservationInsert(it.childId, it.date, null)
                },
        )

    val fullDayAbsences =
        validated.filterIsInstance<DailyReservationRequest.Absent>().map {
            val plannedAbsenceEnabled =
                plannedAbsenceEnabledRanges[it.childId]?.includes(it.date) ?: false
            FullDayAbsenseUpsert(
                it.childId,
                it.date,
                if (plannedAbsenceEnabled && reservableRange.includes(it.date))
                    AbsenceType.PLANNED_ABSENCE
                else AbsenceType.OTHER_ABSENCE,
            )
        }
    val upsertedFullDayAbsences =
        if (fullDayAbsences.isNotEmpty()) {
            tx.upsertFullDayAbsences(userId, now, fullDayAbsences)
        } else {
            emptyList()
        }

    val fixedScheduleAbsences =
        validated.filterIsInstance<DailyReservationRequest.Reservations>().flatMap { req ->
            val placement =
                placements[req.childId]?.find { p -> p.range.includes(req.date) }
                    ?: return@flatMap emptyList()
            val plannedAbsenceEnabled =
                plannedAbsenceEnabledRanges[req.childId]?.includes(req.date) ?: false
            getExpectedAbsenceCategories(
                    req.date,
                    listOfNotNull(req.reservation, req.secondReservation),
                    placement.type,
                    placement.unitLanguage,
                    placement.dailyPreschoolTime,
                    placement.dailyPreparatoryTime,
                    preschoolTerms,
                )
                ?.map {
                    AbsenceUpsert(
                        req.childId,
                        req.date,
                        it,
                        if (plannedAbsenceEnabled && reservableRange.includes(req.date)) {
                            AbsenceType.PLANNED_ABSENCE
                        } else {
                            AbsenceType.OTHER_ABSENCE
                        },
                    )
                } ?: emptyList()
        }
    val upsertedFixedScheduleAbsences =
        if (fixedScheduleAbsences.isNotEmpty()) {
            tx.upsertAbsences(now, user.evakaUserId, fixedScheduleAbsences)
        } else {
            emptyList()
        }

    return CreateReservationsResult(
        deletedAbsences,
        deletedReservations,
        upsertedFullDayAbsences + upsertedFixedScheduleAbsences,
        upsertedReservations,
    )
}

data class ChildDatePresence(
    val date: LocalDate,
    val childId: ChildId,
    val unitId: DaycareId,
    val reservations: List<Reservation>,
    val attendances: List<TimeInterval>,
    val absenceBillable: AbsenceType?,
    val absenceNonbillable: AbsenceType?,
)

data class UpsertChildDatePresenceResult(
    val insertedReservations: List<AttendanceReservationId>,
    val deletedReservations: List<AttendanceReservationId>,
    val insertedAttendances: List<ChildAttendanceId>,
    val deletedAttendances: List<ChildAttendanceId>,
    val insertedAbsences: List<AbsenceId>,
    val deletedAbsences: List<AbsenceId>,
)

fun upsertChildDatePresence(
    tx: Database.Transaction,
    userId: EvakaUserId,
    now: HelsinkiDateTime,
    input: ChildDatePresence,
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
                tx.createQuery {
                        sql(
                            """
            SELECT id 
            FROM attendance_reservation ar
            WHERE date = ${bind(input.date)} AND child_id = ${bind(input.childId)} AND
            ${when (reservation) {
                is Reservation.NoTimes -> "start_time IS NULL AND end_time IS NULL"
                is Reservation.Times -> "start_time = ${bind(reservation.range.start)} AND end_time = ${bind(reservation.range.end)}"
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
            skip = reservations.mapNotNull { it.second },
        )

    val insertedReservations =
        reservations
            .filter { it.second == null }
            .map { tx.insertReservation(userId, now, input.date, input.childId, it.first) }

    val deletedAttendances = tx.deleteAttendancesByDate(childId = input.childId, date = input.date)
    val insertedAttendances =
        input.attendances.map { attendance ->
            tx.insertAttendance(
                input.childId,
                input.unitId,
                input.date,
                TimeInterval(attendance.start, attendance.end),
                now,
                userId,
            )
        }

    val (insertedAbsences, deletedAbsences) =
        setChildDateAbsences(
            tx,
            now,
            userId,
            input.childId,
            input.date,
            mapOfNotNullValues(
                AbsenceCategory.NONBILLABLE to input.absenceNonbillable,
                AbsenceCategory.BILLABLE to input.absenceBillable,
            ),
        )

    return UpsertChildDatePresenceResult(
        insertedReservations = insertedReservations,
        deletedReservations = deletedReservations,
        insertedAttendances = insertedAttendances,
        deletedAttendances = deletedAttendances,
        insertedAbsences = insertedAbsences,
        deletedAbsences = deletedAbsences,
    )
}

private fun ChildDatePresence.validate(now: HelsinkiDateTime, placementType: PlacementType) {
    if (reservations.size > 2) throw BadRequest("Too many reservations")
    if (reservations.map { it == Reservation.NoTimes }.distinct().size > 1)
        throw BadRequest("Mixed reservation types")
    reservations.filterIsInstance<Reservation.Times>().zipWithNext().forEach { (r1, r2) ->
        if (r2.range.overlaps(r1.range)) throw BadRequest("Overlapping reservation times")
    }

    attendances.zipWithNext().forEach { (a1, a2) ->
        if (a1.overlaps(a2)) throw BadRequest("Overlapping attendance times")
    }

    val threshold = now.toLocalTime().plusMinutes(30)
    attendances.forEach {
        if (
            date == now.toLocalDate() &&
                (it.end == null && it.startsAfter(threshold) ||
                    it.end != null && it.overlaps(TimeInterval(threshold, null)))
        ) {
            throw BadRequest(
                "Cannot mark attendances into future",
                errorCode = "attendanceInFuture",
            )
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
    skip: List<AttendanceReservationId>,
): List<AttendanceReservationId> {
    return createQuery {
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
    now: HelsinkiDateTime,
    date: LocalDate,
    childId: ChildId,
    reservation: Reservation,
): AttendanceReservationId {
    return createQuery {
            sql(
                """
        INSERT INTO attendance_reservation (child_id, created_at, created_by, date, start_time, end_time) 
        VALUES (${bind(childId)}, ${bind(now)}, ${bind(userId)}, ${bind(date)}, ${bind(reservation.asTimeRange()?.start)}, ${bind(reservation.asTimeRange()?.end)})
        RETURNING id
    """
            )
        }
        .exactlyOne<AttendanceReservationId>()
}

data class UsedServiceResult(
    val reservedMinutes: Long,
    val usedServiceMinutes: Long,
    val usedServiceRanges: List<TimeRange>,
)

fun computeUsedService(
    today: LocalDate,
    date: LocalDate,
    serviceNeedHours: Int,
    placementType: PlacementType,
    preschoolTime: TimeRange?,
    preparatoryTime: TimeRange?,
    operationDates: Set<LocalDate>,
    shiftCareType: ShiftCareType,
    absences: List<Pair<AbsenceType, AbsenceCategory>>,
    reservations: List<TimeRange>,
    attendances: List<TimeInterval>,
): UsedServiceResult {
    // Today's date is taken to be "in the future" if child has no attendances today or there's an
    // ongoing attendance
    val isDateInFuture =
        date > today ||
            date == today && (attendances.isEmpty() || attendances.any { it.end == null })

    val endedAttendances = attendances.mapNotNull { it.asTimeRange() }

    if (!operationDates.contains(date) && shiftCareType != ShiftCareType.INTERMITTENT) {
        return UsedServiceResult(
            reservedMinutes = 0,
            usedServiceMinutes = 0,
            usedServiceRanges = emptyList(),
        )
    }

    val fixedScheduleTimes =
        listOfNotNull(
            placementType.fixedScheduleRange(
                dailyPreschoolTime = preschoolTime,
                dailyPreparatoryTime = preparatoryTime,
            )
        )
    val effectiveReservations = TimeSet.of(reservations).removeAll(fixedScheduleTimes)

    // Five-year-olds get 4 hours for free
    val freeMinutes =
        when (placementType) {
            PlacementType.DAYCARE_FIVE_YEAR_OLDS -> 4 * 60
            else -> 0
        }
    val minutesOf = { timeSet: TimeSet ->
        maxOf(0, timeSet.ranges().sumOf { it.duration.toMinutes() } - freeMinutes)
    }

    if (isDateInFuture) {
        return UsedServiceResult(
            reservedMinutes = minutesOf(effectiveReservations),
            usedServiceMinutes = 0,
            usedServiceRanges = emptyList(),
        )
    }

    val absenceTypes = absences.map { it.first }.toSet()
    val absenceCategories = absences.map { it.second }.toSet()
    val isPlannedAbsence =
        absenceTypes == setOf(AbsenceType.PLANNED_ABSENCE) &&
            absenceCategories == placementType.absenceCategories()
    val isRefundedAbsence =
        absenceTypes.intersect(
            setOf(AbsenceType.FORCE_MAJEURE, AbsenceType.FREE_ABSENCE, AbsenceType.PARENTLEAVE)
        ) == absenceTypes && absenceCategories == placementType.absenceCategories()

    if (endedAttendances.isEmpty() && isPlannedAbsence) {
        return UsedServiceResult(
            reservedMinutes = 0,
            usedServiceMinutes = 0,
            usedServiceRanges = emptyList(),
        )
    }

    if (operationDates.contains(date) && reservations.isEmpty() && endedAttendances.isEmpty()) {
        val daysInMonth =
            if (isRefundedAbsence) {
                val range = FiniteDateRange.ofMonth(date)
                operationDates.count { range.includes(it) }
            } else {
                21
            }
        val dailyAverage = serviceNeedHours.toDouble() * 60 / daysInMonth
        return UsedServiceResult(
            reservedMinutes = 0,
            usedServiceMinutes = maxOf(0, dailyAverage.roundToLong() - freeMinutes),
            usedServiceRanges = emptyList(),
        )
    }

    val effectiveAttendances = TimeSet.of(endedAttendances).removeAll(fixedScheduleTimes)
    val usedService = effectiveReservations + effectiveAttendances

    return UsedServiceResult(
        reservedMinutes = minutesOf(effectiveReservations),
        usedServiceMinutes = minutesOf(usedService),
        usedServiceRanges = usedService.ranges().toList(),
    )
}
