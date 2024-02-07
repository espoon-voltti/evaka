// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import fi.espoo.evaka.Audit
import fi.espoo.evaka.absence.Absence
import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.absence.AbsenceType.OTHER_ABSENCE
import fi.espoo.evaka.absence.AbsenceType.PLANNED_ABSENCE
import fi.espoo.evaka.absence.AbsenceType.SICKLEAVE
import fi.espoo.evaka.absence.FullDayAbsenseUpsert
import fi.espoo.evaka.absence.clearOldCitizenEditableAbsences
import fi.espoo.evaka.absence.getAbsencesCitizen
import fi.espoo.evaka.absence.upsertFullDayAbsences
import fi.espoo.evaka.attendance.childrenHaveAttendanceInRange
import fi.espoo.evaka.attendance.getChildAttendancesCitizen
import fi.espoo.evaka.daycare.ClubTerm
import fi.espoo.evaka.daycare.PreschoolTerm
import fi.espoo.evaka.daycare.getClubTerms
import fi.espoo.evaka.daycare.getPreschoolTerms
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.ScheduleType
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ChildImageId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.shared.domain.getHolidays
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class ReservationControllerCitizen(
    private val accessControl: AccessControl,
    private val featureConfig: FeatureConfig
) {
    @GetMapping("/citizen/reservations")
    fun getReservations(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate
    ): ReservationsResponse {
        val requestedRange =
            try {
                FiniteDateRange(from, to)
            } catch (e: IllegalArgumentException) {
                throw BadRequest("Invalid date range $from - $to")
            }

        val today = clock.today()
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Person.READ_RESERVATIONS,
                        user.id
                    )
                    val holidays = tx.getHolidays(requestedRange)
                    val preschoolTerms = tx.getPreschoolTerms()
                    val clubTerms = tx.getClubTerms()
                    val children = tx.getReservationChildren(user.id, today)
                    val childIds = children.map { it.id }.toSet()
                    val placements =
                        tx.getReservationPlacements(
                            childIds,
                            // Include all future placements for upcomingPlacementType computation
                            DateRange(minOf(today, requestedRange.start), null)
                        )
                    val backupPlacements =
                        tx.getReservationBackupPlacements(childIds, requestedRange)

                    val absences: Map<Pair<ChildId, LocalDate>, List<Absence>> =
                        tx.getAbsencesCitizen(today, user.id, requestedRange).groupBy {
                            it.childId to it.date
                        }
                    val reservations: Map<Pair<ChildId, LocalDate>, List<ReservationResponse>> =
                        tx.getReservationsCitizen(today, user.id, requestedRange)
                            .groupBy { it.childId to it.date }
                            .mapValues { (_, reservations) ->
                                reservations.map { ReservationResponse.from(it) }
                            }
                    val attendances: Map<Pair<ChildId, LocalDate>, List<OpenTimeRange>> =
                        tx.getChildAttendancesCitizen(today, user.id, requestedRange)
                            .groupBy { it.childId to it.date }
                            .mapValues { (_, attendances) ->
                                attendances.map { OpenTimeRange(it.startTime, it.endTime) }
                            }
                    val reservableRange =
                        getReservableRange(
                            clock.now(),
                            featureConfig.citizenReservationThresholdHours,
                        )
                    val days =
                        requestedRange
                            .dates()
                            .map { date ->
                                val isHoliday = holidays.contains(date)
                                ReservationResponseDay(
                                    date = date,
                                    holiday = isHoliday,
                                    children =
                                        // Includes all children that are eligible for daycare
                                        // on this date: have a placement and is an operation day in
                                        // their unit, or child has intermittent shift care.
                                        //
                                        // Preschool/club only children are eligible when there's
                                        // activity on the date. This excludes e.g. Christmas or
                                        // winter holidays.
                                        children
                                            .mapNotNull { child ->
                                                placements[child.id]
                                                    ?.find { it.range.includes(date) }
                                                    ?.let { placement ->
                                                        placementDay(
                                                            date,
                                                            isHoliday,
                                                            placement,
                                                            backupPlacements[child.id],
                                                            clubTerms,
                                                            preschoolTerms
                                                        )
                                                    }
                                                    ?.let { placementDay ->
                                                        val key = Pair(child.id, date)
                                                        val childAbsences =
                                                            absences[key] ?: listOf()
                                                        val childReservations =
                                                            reservations[key] ?: listOf()
                                                        val childAttendances =
                                                            attendances[key] ?: listOf()

                                                        val isPast = date.isBefore(today)
                                                        val hasEndedAttendances =
                                                            childAttendances.any {
                                                                it.endTime != null
                                                            }
                                                        val usedService =
                                                            placementDay.daycareHoursPerMonth
                                                                ?.takeIf {
                                                                    isPast || hasEndedAttendances
                                                                }
                                                                ?.let { daycareHoursPerMonth ->
                                                                    UsedService.compute(
                                                                        daycareHoursPerMonth,
                                                                        placementDay.placementType,
                                                                        childAbsences.map {
                                                                            it.category
                                                                        },
                                                                        childReservations
                                                                            .mapNotNull {
                                                                                it.asTimeRange()
                                                                            },
                                                                        childAttendances
                                                                            .mapNotNull {
                                                                                it.asTimeRange()
                                                                            }
                                                                    )
                                                                }
                                                        ReservationResponseDayChild(
                                                            childId = child.id,
                                                            scheduleType =
                                                                placementDay.scheduleType,
                                                            shiftCare = placementDay.shiftCare,
                                                            absence =
                                                                selectSingleAbsence(childAbsences),
                                                            reservations = childReservations,
                                                            attendances = childAttendances,
                                                            usedService = usedService,
                                                            reservableTimeRange =
                                                                placementDay.reservableTimeRange
                                                        )
                                                    }
                                            }
                                            .sortedBy { it.childId }
                                )
                            }
                            .toList()

                    ReservationsResponse(
                        children =
                            children.map {
                                ReservationChild.from(
                                    it,
                                    days,
                                    placements[it.id] ?: emptyList(),
                                    today
                                )
                            },
                        days = days,
                        reservableRange = reservableRange
                    )
                }
            }
            .also {
                Audit.AttendanceReservationCitizenRead.log(
                    targetId = user.id,
                    meta = mapOf("from" to from, "to" to to)
                )
            }
    }

    @PostMapping("/citizen/reservations")
    fun postReservations(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @RequestBody body: List<DailyReservationRequest>
    ) {
        val children = body.map { it.childId }.toSet()

        val result =
            db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Child.CREATE_RESERVATION,
                        children
                    )

                    createReservationsAndAbsences(
                        tx,
                        clock.now(),
                        user,
                        body,
                        featureConfig.citizenReservationThresholdHours
                    )
                }
            }
        Audit.AttendanceReservationCitizenCreate.log(
            targetId = children,
            meta =
                mapOf(
                    "deletedAbsences" to result.deletedAbsences,
                    "deletedReservations" to result.deletedReservations,
                    "upsertedAbsences" to result.upsertedAbsences,
                    "upsertedReservations" to result.upsertedReservations
                )
        )
    }

    @PostMapping("/citizen/absences")
    fun postAbsences(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @RequestBody body: AbsenceRequest
    ) {
        if (body.dateRange.start.isBefore(clock.today())) {
            throw BadRequest("Cannot mark absences for past days")
        }

        if (!listOf(OTHER_ABSENCE, PLANNED_ABSENCE, SICKLEAVE).contains(body.absenceType)) {
            throw BadRequest("Invalid absence type")
        }

        val now = clock.now()

        val (deletedAbsences, deletedReservations, insertedAbsences) =
            db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Child.CREATE_ABSENCE,
                        body.childIds
                    )
                    if (tx.childrenHaveAttendanceInRange(body.childIds, body.dateRange)) {
                        throw BadRequest(
                            "Attendance already exists for given dates",
                            "ATTENDANCE_ALREADY_EXISTS"
                        )
                    }

                    val reservableRange =
                        getReservableRange(now, featureConfig.citizenReservationThresholdHours)
                    val childContractDays =
                        body.dateRange.intersection(reservableRange)?.let { range ->
                            tx.getReservationContractDayRanges(body.childIds, range)
                        } ?: emptyMap()

                    val deletedAbsences =
                        tx.clearOldCitizenEditableAbsences(
                            body.childIds.flatMap { childId ->
                                body.dateRange.dates().map { childId to it }
                            },
                            reservableRange = reservableRange
                        )
                    // Delete reservations on days in the reservable range. Reservations in the
                    // closed range are kept.
                    val deletedReservations =
                        body.dateRange.intersection(reservableRange)?.dates()?.let { dates ->
                            tx.clearOldReservations(
                                body.childIds.flatMap { childId ->
                                    dates.map { date -> childId to date }
                                }
                            )
                        }
                    val insertedAbsences =
                        tx.upsertFullDayAbsences(
                            user.evakaUserId,
                            now,
                            body.childIds.flatMap { childId ->
                                body.dateRange.dates().map { date ->
                                    FullDayAbsenseUpsert(
                                        childId,
                                        date,
                                        if (
                                            reservableRange.includes(date) &&
                                                body.absenceType == OTHER_ABSENCE
                                        ) {
                                            val hasContractDays =
                                                childContractDays[childId]?.includes(date) ?: false
                                            if (hasContractDays) {
                                                PLANNED_ABSENCE
                                            } else {
                                                body.absenceType
                                            }
                                        } else {
                                            body.absenceType
                                        }
                                    )
                                }
                            }
                        )
                    Triple(deletedAbsences, deletedReservations, insertedAbsences)
                }
            }
        Audit.AbsenceCitizenCreate.log(
            targetId = body.childIds,
            objectId = insertedAbsences,
            meta =
                mapOf(
                    "deletedAbsences" to deletedAbsences,
                    "deletedReservations" to deletedReservations
                )
        )
    }
}

data class PlacementDay(
    val placementType: PlacementType,
    val scheduleType: ScheduleType,
    val shiftCare: Boolean,
    val daycareHoursPerMonth: Int?,
    val reservableTimeRange: ReservableTimeRange
)

private fun placementDay(
    date: LocalDate,
    isHoliday: Boolean,
    placement: ReservationPlacement,
    backupPlacements: List<ReservationBackupPlacement>?,
    clubTerms: List<ClubTerm>,
    preschoolTerms: List<PreschoolTerm>
): PlacementDay? {
    val serviceNeed = placement.serviceNeeds.find { it.range.includes(date) }
    val backupPlacementForDate = backupPlacements?.find { it.range.includes(date) }

    // Backup placements take precedence
    val operationTimes = backupPlacementForDate?.operationTimes ?: placement.operationTimes

    val shiftCareType = serviceNeed?.shiftCareType ?: ShiftCareType.NONE
    val alwaysOpen = operationTimes.all { it != null }
    val shiftCare = alwaysOpen || shiftCareType == ShiftCareType.INTERMITTENT

    // null means that the unit is not open today
    val operationTime =
        if (isHoliday && !alwaysOpen) {
            null
        } else {
            operationTimes[date.dayOfWeek.value - 1]
        }

    return if (operationTime != null || shiftCare) {
        PlacementDay(
            placementType = placement.type,
            scheduleType = placement.type.scheduleType(date, clubTerms, preschoolTerms),
            shiftCare = shiftCare,
            daycareHoursPerMonth = serviceNeed?.daycareHoursPerMonth,
            reservableTimeRange =
                if (shiftCareType == ShiftCareType.INTERMITTENT) {
                    ReservableTimeRange.IntermittentShiftCare(operationTime)
                } else {
                    ReservableTimeRange.Normal(operationTime!!)
                }
        )
    } else {
        null
    }
}

/**
 * Show at most one absence per child per day. Taking the non-billable one is just a random choice
 * without any real meaning.
 */
private fun selectSingleAbsence(absences: List<Absence>): AbsenceInfo? =
    absences
        .let {
            if (it.size > 1) {
                it.first { a -> a.category == AbsenceCategory.NONBILLABLE }
            } else {
                it.firstOrNull()
            }
        }
        ?.let { AbsenceInfo(it.absenceType, it.editableByCitizen()) }

data class ReservationsResponse(
    val children: List<ReservationChild>,
    val days: List<ReservationResponseDay>,
    val reservableRange: FiniteDateRange
)

data class ReservationChild(
    val id: ChildId,
    val firstName: String,
    val lastName: String,
    val preferredName: String,
    val duplicateOf: PersonId?,
    val imageId: ChildImageId?,
    val upcomingPlacementType: PlacementType?,
    val monthSummaries: List<MonthSummary>
) {
    companion object {
        fun from(
            child: ReservationChildRow,
            days: List<ReservationResponseDay>,
            placements: List<ReservationPlacement>,
            today: LocalDate
        ): ReservationChild {
            val hasHourBasedServiceNeeds =
                placements.any { p -> p.serviceNeeds.any { sn -> sn.daycareHoursPerMonth != null } }
            val monthSummaries =
                if (hasHourBasedServiceNeeds) {
                    days
                        .mapNotNull { day ->
                            day.children.find { it.childId == child.id }?.let { day.date to it }
                        }
                        .groupBy(
                            { (date, _) -> date.year to date.monthValue },
                            { (_, childDay) -> childDay }
                        )
                        .mapNotNull { (yearMonth, childDays) ->
                            val (year, month) = yearMonth
                            val monthRange = FiniteDateRange.ofMonth(LocalDate.of(year, month, 1))

                            // As per how the hour-based service needs are used in practice, there
                            // should only be just one service need for the child for each month, so
                            // we'll take the first one
                            val daycareHoursPerMonth =
                                placements
                                    .find { it.range.overlaps(monthRange) }
                                    ?.serviceNeeds
                                    ?.find { it.daycareHoursPerMonth != null }
                                    ?.daycareHoursPerMonth

                            if (daycareHoursPerMonth == null) {
                                // Not an hour-based service need, don't generate a summary at all
                                return@mapNotNull null
                            }

                            MonthSummary(
                                year = yearMonth.first,
                                month = yearMonth.second,
                                serviceNeedMinutes = daycareHoursPerMonth * 60,
                                reservedMinutes =
                                    childDays.sumOf { day ->
                                        day.reservations
                                            .mapNotNull { it.asTimeRange() }
                                            .sumOf { it.durationInMinutes() }
                                    },
                                usedServiceMinutes =
                                    childDays.sumOf { it.usedService?.durationInMinutes ?: 0 }
                            )
                        }
                } else {
                    // No hour-based service needs, don't generate summaries
                    emptyList()
                }

            return ReservationChild(
                id = child.id,
                firstName = child.firstName,
                lastName = child.lastName,
                preferredName = child.preferredName,
                duplicateOf = child.duplicateOf,
                imageId = child.imageId,
                upcomingPlacementType = placements.find { it.range.end >= today }?.type,
                monthSummaries = monthSummaries
            )
        }
    }
}

data class MonthSummary(
    val year: Int,
    val month: Int,
    val serviceNeedMinutes: Int,
    val reservedMinutes: Int,
    val usedServiceMinutes: Int
)

data class ReservationResponseDay(
    val date: LocalDate,
    val holiday: Boolean,
    val children: List<ReservationResponseDayChild>
)

data class ReservationResponseDayChild(
    val childId: ChildId,
    val scheduleType: ScheduleType,
    val shiftCare: Boolean, // Whether child in 7-day-a-week or intermittent shift care
    val absence: AbsenceInfo?,
    val reservations: List<ReservationResponse>,
    val attendances: List<OpenTimeRange>,
    val usedService: UsedService?,
    val reservableTimeRange: ReservableTimeRange
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
sealed class ReservableTimeRange {
    // Child is in normal daycare: The child can make reservations on days according to the
    // placement unit's operation times
    @JsonTypeName("NORMAL") data class Normal(val range: TimeRange) : ReservableTimeRange()

    // Child is in intermittent shift care: The child can make any reservations on any days, and we
    // return the placement unit's operational times just for showing it as information in the UI.
    // `null` means that the placement unit is closed on this day.
    @JsonTypeName("INTERMITTENT_SHIFT_CARE")
    data class IntermittentShiftCare(val placementUnitOperationTime: TimeRange?) :
        ReservableTimeRange()
}

data class AbsenceInfo(val type: AbsenceType, val editable: Boolean)

data class AbsenceRequest(
    val childIds: Set<ChildId>,
    val dateRange: FiniteDateRange,
    val absenceType: AbsenceType
)
