// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.absence

import fi.espoo.evaka.attendance.getChildAttendanceStartDatesByRange
import fi.espoo.evaka.dailyservicetimes.DailyServiceTimesValue
import fi.espoo.evaka.dailyservicetimes.getChildDailyServiceTimes
import fi.espoo.evaka.daycare.getClubTerms
import fi.espoo.evaka.daycare.getDaycare
import fi.espoo.evaka.daycare.getPreschoolTerms
import fi.espoo.evaka.daycare.getUnitOperationDays
import fi.espoo.evaka.holidayperiod.getHolidayPeriodsInRange
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.ScheduleType
import fi.espoo.evaka.placement.getChildPlacementTypesByRange
import fi.espoo.evaka.reservations.Reservation
import fi.espoo.evaka.reservations.getChildAttendanceReservationStartDatesByRange
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.serviceneed.getActualServiceNeedInfosByRangeAndGroup
import fi.espoo.evaka.shared.AbsenceId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.DatabaseEnum
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.HelsinkiDateTimeRange
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.shared.domain.getHolidays
import fi.espoo.evaka.shared.domain.isOperationalDate
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month

fun getGroupMonthCalendar(
    tx: Database.Read,
    groupId: GroupId,
    year: Int,
    month: Int,
    includeNonOperationalDays: Boolean
): GroupMonthCalendar {
    val range = FiniteDateRange.ofMonth(year, Month.of(month))

    val clubTerms = tx.getClubTerms()
    val preschoolTerms = tx.getPreschoolTerms()
    val daycare =
        tx.getDaycare(tx.getDaycareIdByGroup(groupId))
            ?: throw BadRequest("Couldn't find daycare with group with id $groupId")
    val groupName =
        tx.getGroupName(groupId) ?: throw BadRequest("Couldn't find group with id $groupId")
    val placementList = tx.getPlacementsByRange(groupId, range)
    val absences = tx.getAbsencesInGroupByRange(groupId, range)
    val actualServiceNeeds =
        tx.getActualServiceNeedInfosByRangeAndGroup(groupId, range).groupBy { it.childId }.toMap()
    val backupCares = tx.getBackupCaresAffectingGroup(groupId, range)
    val reservations = tx.getGroupReservations(groupId, range)
    val attendances = tx.getGroupAttendances(groupId, range).groupBy { it.childId }.toMap()
    val dailyServiceTimes = tx.getGroupDailyServiceTimes(groupId, range)

    val operationDays = daycare.operationDays.map { DayOfWeek.of(it) }.toSet()
    val holidays = tx.getHolidays(range)
    val operationDates =
        range
            .dates()
            .filter { includeNonOperationalDays || it.isOperationalDate(operationDays, holidays) }
            .toSet()

    val holidayPeriods = tx.getHolidayPeriodsInRange(range)

    val children =
        placementList
            .map { (child, placements) ->
                val placementDateRanges = placements.map { it.dateRange }
                val absenceDates =
                    absences.keys
                        .mapNotNull { if (it.first == child.id) it.second else null }
                        .toSet()

                val possibleAttendanceDates =
                    placementDateRanges
                        .flatMap { it.dates() }
                        .filterNot { absenceDates.contains(it) }
                        .filter { operationDates.contains(it) }

                val supplementedReservations =
                    supplementReservationsWithDailyServiceTimes(
                        possibleAttendanceDates,
                        reservations.entries.mapNotNull {
                            if (it.key.first == child.id) {
                                it.key.second to it.value
                            } else null
                        },
                        dailyServiceTimes[child.id]?.map { it.times },
                        absenceDates
                    )
                GroupMonthCalendarChild(
                    id = child.id,
                    firstName = child.firstName,
                    lastName = child.lastName,
                    dateOfBirth = child.dateOfBirth,
                    actualServiceNeeds = actualServiceNeeds[child.id] ?: emptyList(),
                    reservationTotalHours =
                        sumOfHours(supplementedReservations, placementDateRanges, range),
                    attendanceTotalHours =
                        attendances[child.id]
                            ?.map { HelsinkiDateTimeRange.of(it.date, it.startTime, it.endTime) }
                            ?.let { sumOfHours(it, placementDateRanges, range) } ?: 0,
                )
            }
            .sortedWith(compareBy({ it.lastName }, { it.firstName }))

    val days =
        FiniteDateRange.ofMonth(year, Month.of(month))
            .dates()
            .map { date ->
                val isHolidayPeriodDate = holidayPeriods.any { it.period.includes(date) }
                val operationalDate = date.isOperationalDate(operationDays, holidays)
                GroupMonthCalendarDay(
                    date = date,
                    holiday = !operationalDate,
                    holidayPeriod = isHolidayPeriodDate,
                    children =
                        if (includeNonOperationalDays || operationalDate) {
                            placementList
                                .mapNotNull { (child, placements) ->
                                    val placement =
                                        placements.find { it.dateRange.includes(date) }
                                            ?: return@mapNotNull null
                                    val childAbsences = absences[child.id to date] ?: emptyList()
                                    val childReservations =
                                        reservations[child.id to date] ?: emptyList()
                                    val scheduleType =
                                        placement.type.scheduleType(date, clubTerms, preschoolTerms)

                                    GroupMonthCalendarDayChild(
                                        childId = child.id,
                                        absenceCategories = placement.type.absenceCategories(),
                                        backupCare =
                                            backupCares[child.id]?.any { it.includes(date) }
                                                ?: false,
                                        scheduleType = scheduleType,
                                        missingHolidayReservation =
                                            scheduleType == ScheduleType.RESERVATION_REQUIRED &&
                                                isHolidayPeriodDate &&
                                                childReservations.isEmpty() &&
                                                childAbsences.isEmpty(),
                                        absences =
                                            childAbsences.map { AbsenceWithModifierInfo.from(it) },
                                        reservations = childReservations,
                                        dailyServiceTimes =
                                            (dailyServiceTimes[child.id]?.map { it.times }
                                                    ?: emptyList())
                                                .find { it.validityPeriod.includes(date) }
                                                ?.let { value ->
                                                    when (value) {
                                                        is DailyServiceTimesValue.RegularTimes ->
                                                            value.regularTimes
                                                        is DailyServiceTimesValue.IrregularTimes ->
                                                            value.getTimesOnDate(date)
                                                        is DailyServiceTimesValue.VariableTimes ->
                                                            null
                                                    }
                                                },
                                    )
                                }
                                .sortedBy { it.childId }
                        } else null
                )
            }
            .toList()

    return GroupMonthCalendar(
        groupId,
        daycare.name,
        daycare.operationTimes,
        groupName,
        children,
        days,
    )
}

fun getAbsencesOfChildByMonth(
    tx: Database.Read,
    childId: ChildId,
    year: Int,
    month: Int
): List<Absence> {
    val range = DateRange.ofMonth(year, Month.of(month))
    return tx.getAbsencesOfChildByRange(childId, range)
}

fun getFutureAbsencesOfChild(
    tx: Database.Read,
    evakaClock: EvakaClock,
    childId: ChildId
): List<Absence> {
    val period = DateRange(evakaClock.today().plusDays(1), null)
    return tx.getAbsencesOfChildByRange(childId, period)
}

/**
 * Inserts/deletes absences to achieve the given state. Does not replace identical existing
 * absences. Returns ids of inserted and deleted absences.
 */
fun setChildDateAbsences(
    tx: Database.Transaction,
    now: HelsinkiDateTime,
    userId: EvakaUserId,
    childId: ChildId,
    date: LocalDate,
    absences: Map<AbsenceCategory, AbsenceType>
): Pair<List<AbsenceId>, List<AbsenceId>> {
    val alreadyUpToDateCategories =
        absences.entries.mapNotNull { (category, type) ->
            category.takeIf { tx.absenceExists(date, childId, category, type) }
        }
    val deletedAbsences =
        tx.deleteChildAbsences(
            childId = childId,
            date = date,
            categories = AbsenceCategory.entries.toSet().minus(alreadyUpToDateCategories.toSet())
        )

    val insertedAbsences =
        tx.insertAbsences(
            now = now,
            userId = userId,
            absences =
                absences
                    .filterKeys { !alreadyUpToDateCategories.contains(it) }
                    .map { (category, type) -> AbsenceUpsert(childId, date, category, type) }
        )

    return insertedAbsences to deletedAbsences
}

fun generateAbsencesFromIrregularDailyServiceTimes(
    tx: Database.Transaction,
    now: HelsinkiDateTime,
    childId: ChildId
) {
    // Change absences from tomorrow onwards
    val period = DateRange(now.toLocalDate().plusDays(1), null)

    val irregularDailyServiceTimes =
        tx.getChildDailyServiceTimes(childId).flatMap {
            if (
                it.times.validityPeriod.overlaps(period) &&
                    it.times is DailyServiceTimesValue.IrregularTimes &&
                    !it.times.isEmpty()
            ) {
                listOf(it.times)
            } else {
                emptyList()
            }
        }
    if (irregularDailyServiceTimes.isNotEmpty()) {
        val attendanceDates = tx.getChildAttendanceStartDatesByRange(childId, period)
        val reservationDates = tx.getChildAttendanceReservationStartDatesByRange(childId, period)
        val placementTypes = tx.getChildPlacementTypesByRange(childId, period)
        val unitOperationDays = tx.getUnitOperationDays()

        val absencesToAdd =
            irregularDailyServiceTimes.flatMap dst@{ dailyServiceTimes ->
                val validityPeriod =
                    period.intersection(dailyServiceTimes.validityPeriod) ?: return@dst listOf()
                placementTypes.flatMap pt@{ placementType ->
                    val effectivePeriod =
                        placementType.period.intersection(validityPeriod) ?: return@pt listOf()
                    val operationDays = unitOperationDays[placementType.unitId] ?: setOf()
                    effectivePeriod.dates().toList().flatMap date@{ date ->
                        val dayOfWeek = date.dayOfWeek
                        if (!operationDays.contains(dayOfWeek)) return@date listOf()

                        val isIrregularAbsenceDay =
                            dailyServiceTimes.timesForDayOfWeek(date.dayOfWeek) == null
                        if (!isIrregularAbsenceDay) return@date listOf()

                        val hasAttendance = attendanceDates.any { it == date }
                        val hasReservation = reservationDates.any { it == date }
                        if (hasAttendance || hasReservation) return@date listOf()

                        placementType.placementType.absenceCategories().map { category ->
                            AbsenceUpsert(
                                childId = childId,
                                date = date,
                                absenceType = AbsenceType.OTHER_ABSENCE,
                                category = category
                            )
                        }
                    }
                }
            }
        tx.upsertGeneratedAbsences(now, absencesToAdd)
    }
    tx.deleteOldGeneratedAbsencesInRange(now, childId, period)
}

fun deleteFutureNonGeneratedAbsencesByCategoryInRange(
    tx: Database.Transaction,
    clock: EvakaClock,
    childId: ChildId,
    range: DateRange,
    categoriesToDelete: Set<AbsenceCategory>
) {
    if (range.start.isBefore(clock.now().toLocalDate().plusDays(1))) {
        throw BadRequest("Could not delete future absences - Range has to be in future ")
    }

    val futureAbsences = getFutureAbsencesOfChild(tx, clock, childId)

    val futureAbsencesToDelete =
        futureAbsences.filter { absence -> categoriesToDelete.contains(absence.category) }

    if (futureAbsencesToDelete.isNotEmpty()) {
        tx.deleteNonSystemGeneratedAbsencesByCategoryInRange(childId, range, categoriesToDelete)
    }
}

private fun supplementReservationsWithDailyServiceTimes(
    possibleAttendanceDates: List<LocalDate>,
    reservations: List<Pair<LocalDate, List<ChildReservation>>>,
    dailyServiceTimesList: List<DailyServiceTimesValue>?,
    absenceDates: Set<LocalDate>
): List<HelsinkiDateTimeRange> {
    val reservationRanges =
        reservations
            .flatMap { (date, reservations) ->
                reservations.mapNotNull { res ->
                    when (res.reservation) {
                        is Reservation.Times ->
                            HelsinkiDateTimeRange.of(
                                date,
                                res.reservation.startTime.withNano(0).withSecond(0),
                                res.reservation.endTime.withNano(0).withSecond(0)
                            )

                        // Reserved but no times -> use daily service times
                        is Reservation.NoTimes -> null
                    }
                }
            }
            .sortedBy { it.start }
            .fold(listOf<HelsinkiDateTimeRange>()) { timeRanges, timeRange ->
                if (timeRanges.isEmpty()) {
                    listOf(timeRange)
                } else {
                    if (timeRange.start.durationSince(timeRanges.last().end).toMinutes() <= 1) {
                        timeRanges.dropLast(1) +
                            HelsinkiDateTimeRange(timeRanges.last().start, timeRange.end)
                    } else {
                        timeRanges + timeRange
                    }
                }
            }
            .filterNot { absenceDates.contains(it.start.toLocalDate()) }

    val reservedDates = reservationRanges.map { it.start.toLocalDate() }.toSet()

    val dailyServiceTimeRanges =
        if (dailyServiceTimesList.isNullOrEmpty()) {
            listOf()
        } else {
            val dates = possibleAttendanceDates.filterNot { reservedDates.contains(it) }
            dailyServiceTimesToPerDateTimeRanges(dates, dailyServiceTimesList)
        }

    return (reservationRanges + dailyServiceTimeRanges)
        .sortedBy { it.start }
        .fold(listOf()) { timeRanges, timeRange ->
            val lastTimeRange = timeRanges.lastOrNull()
            when {
                lastTimeRange == null -> listOf(timeRange)
                lastTimeRange.contains(timeRange) -> timeRanges
                lastTimeRange.overlaps(timeRange) ->
                    timeRanges + timeRange.copy(start = lastTimeRange.end)
                else -> timeRanges + timeRange
            }
        }
}

private fun dailyServiceTimesToPerDateTimeRanges(
    dates: List<LocalDate>,
    dailyServiceTimesList: List<DailyServiceTimesValue>
): List<HelsinkiDateTimeRange> {
    return dates.mapNotNull { date ->
        val dailyServiceTimes =
            dailyServiceTimesList.find { it.validityPeriod.includes(date) }
                ?: return@mapNotNull null

        val times =
            when (dailyServiceTimes) {
                is DailyServiceTimesValue.RegularTimes ->
                    dailyServiceTimes.regularTimes.start to dailyServiceTimes.regularTimes.end
                is DailyServiceTimesValue.IrregularTimes -> {
                    val times = dailyServiceTimes.timesForDayOfWeek(date.dayOfWeek)
                    if (times != null) times.start to times.end else null
                }
                is DailyServiceTimesValue.VariableTimes -> null
            }

        times?.let { (start, end) ->
            HelsinkiDateTimeRange(
                HelsinkiDateTime.of(date, start),
                HelsinkiDateTime.of(if (end < start) date.plusDays(1) else date, end)
            )
        }
    }
}

private fun sumOfHours(
    dateTimeRanges: List<HelsinkiDateTimeRange>,
    placementDateRanges: List<FiniteDateRange>,
    spanningDateRange: FiniteDateRange
): Int {
    val placementDateTimeRanges =
        placementDateRanges
            .mapNotNull { it.intersection(spanningDateRange) }
            .map {
                HelsinkiDateTimeRange(
                    HelsinkiDateTime.of(it.start, LocalTime.of(0, 0)),
                    HelsinkiDateTime.of(it.end.plusDays(1), LocalTime.of(0, 0))
                )
            }

    return dateTimeRanges
        .flatMap { timeRange -> placementDateTimeRanges.mapNotNull { timeRange.intersection(it) } }
        .fold(0L) { sum, (start, end) ->
            sum +
                end.durationSince(start).toMinutes().let {
                    if (end.toLocalTime().withNano(0).withSecond(0) == LocalTime.of(23, 59)) it + 1
                    else it
                }
        }
        .let { minutes ->
            BigDecimal(minutes).divide(BigDecimal(60), 0, RoundingMode.FLOOR).toInt()
        }
}

enum class AbsenceCategory : DatabaseEnum {
    BILLABLE,
    NONBILLABLE;

    override val sqlType: String = "absence_category"
}

data class GroupMonthCalendar(
    val groupId: GroupId,
    val daycareName: String,
    val daycareOperationTimes: List<TimeRange?>,
    val groupName: String,
    val children: List<GroupMonthCalendarChild>,
    val days: List<GroupMonthCalendarDay>,
)

data class GroupMonthCalendarChild(
    val id: ChildId,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate,
    val actualServiceNeeds: List<ChildServiceNeedInfo>,
    val reservationTotalHours: Int,
    val attendanceTotalHours: Int,
)

data class GroupMonthCalendarDay(
    val date: LocalDate,
    val holiday: Boolean,
    val holidayPeriod: Boolean,
    val children: List<GroupMonthCalendarDayChild>? // null if not an operation day for the unit
)

data class GroupMonthCalendarDayChild(
    val childId: ChildId,
    val absenceCategories: Set<AbsenceCategory>,
    val backupCare: Boolean,
    val missingHolidayReservation: Boolean,
    val absences: List<AbsenceWithModifierInfo>,
    val reservations: List<ChildReservation>,
    val dailyServiceTimes: TimeRange?,
    val scheduleType: ScheduleType
)

data class ChildServiceNeedInfo(
    val childId: ChildId,
    val hasContractDays: Boolean,
    val optionName: String,
    val validDuring: FiniteDateRange,
    val shiftCare: ShiftCareType
)

data class AbsencePlacement(val dateRange: FiniteDateRange, val type: PlacementType)

data class Absence(
    val childId: ChildId,
    val date: LocalDate,
    val category: AbsenceCategory,
    val absenceType: AbsenceType,
    val modifiedByStaff: Boolean,
    val modifiedAt: HelsinkiDateTime
) {
    fun editableByCitizen(): Boolean = absenceType != AbsenceType.FREE_ABSENCE && !modifiedByStaff
}

data class AbsenceWithModifierInfo(
    val absenceType: AbsenceType,
    val category: AbsenceCategory,
    val modifiedByStaff: Boolean,
    val modifiedAt: HelsinkiDateTime
) {
    companion object {
        fun from(absence: Absence): AbsenceWithModifierInfo =
            AbsenceWithModifierInfo(
                absence.absenceType,
                absence.category,
                absence.modifiedByStaff,
                absence.modifiedAt
            )
    }
}

enum class AbsenceType : DatabaseEnum {
    /** A normal absence that has been informed to the staff */
    OTHER_ABSENCE,

    /** An absence caused by sickness */
    SICKLEAVE,

    /** A planned shift work absence or contract based absence */
    PLANNED_ABSENCE,

    /** An absence that has not been informed to the staff */
    UNKNOWN_ABSENCE,

    /** A forced absence (e.g. the daycare is closed) */
    FORCE_MAJEURE,

    /** An absence because of a parent leave */
    PARENTLEAVE,

    /**
     * An absence during a holiday season that fulfils specific requirements for being free of
     * charge
     */
    FREE_ABSENCE,

    /** An absence during a holiday season that warrants to charge a fine */
    UNAUTHORIZED_ABSENCE;

    override val sqlType: String = "absence_type"
}
