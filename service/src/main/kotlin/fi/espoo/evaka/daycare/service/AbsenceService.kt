// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.service

import fi.espoo.evaka.attendance.getChildAttendanceStartDatesByRange
import fi.espoo.evaka.dailyservicetimes.DailyServiceTimesValue
import fi.espoo.evaka.dailyservicetimes.getChildDailyServiceTimes
import fi.espoo.evaka.daycare.getDaycare
import fi.espoo.evaka.daycare.getUnitOperationDays
import fi.espoo.evaka.holidayperiod.HolidayPeriod
import fi.espoo.evaka.holidayperiod.getHolidayPeriodsInRange
import fi.espoo.evaka.placement.getChildPlacementTypesByRange
import fi.espoo.evaka.reservations.Reservation
import fi.espoo.evaka.reservations.getChildAttendanceReservationStartDatesByRange
import fi.espoo.evaka.serviceneed.getActualServiceNeedInfosByRangeAndGroup
import fi.espoo.evaka.shared.AbsenceId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.DatabaseEnum
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.HelsinkiDateTimeRange
import fi.espoo.evaka.shared.domain.getHolidays
import fi.espoo.evaka.shared.domain.isOperationalDate
import fi.espoo.evaka.user.EvakaUserType
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month

fun getAbsencesInGroupByMonth(
    tx: Database.Read,
    groupId: GroupId,
    year: Int,
    month: Int
): AbsenceGroup {
    val range = FiniteDateRange.ofMonth(year, Month.of(month))

    val daycare =
        tx.getDaycare(tx.getDaycareIdByGroup(groupId))
            ?: throw BadRequest("Couldn't find daycare with group with id $groupId")
    val groupName =
        tx.getGroupName(groupId) ?: throw BadRequest("Couldn't find group with id $groupId")
    val placementList = tx.getPlacementsByRange(groupId, range)
    val absenceList = tx.getAbsencesInGroupByRange(groupId, range).groupBy { it.childId }.toMap()
    val actualServiceNeeds =
        tx.getActualServiceNeedInfosByRangeAndGroup(groupId, range).groupBy { it.childId }.toMap()
    val backupCareList =
        tx.getBackupCaresAffectingGroup(groupId, range).groupBy { it.childId }.toMap()
    val reservations = tx.getGroupReservations(groupId, range).groupBy { it.childId }.toMap()
    val attendances = tx.getGroupAttendances(groupId, range).groupBy { it.childId }.toMap()
    val dailyServiceTimes = tx.getGroupDailyServiceTimes(groupId, range)

    val operationalDays = daycare.operationDays.map { DayOfWeek.of(it) }.toSet()
    val holidays = tx.getHolidays(range)
    val operationalDates = range.dates().filter { it.isOperationalDate(operationalDays, holidays) }
    val setOfOperationalDays = operationalDates.toSet()

    val holidayPeriods = tx.getHolidayPeriodsInRange(range)
    val missingHolidayReservations =
        getMissingHolidayReservations(
            placementList.keys.map { it.id },
            actualServiceNeeds,
            absenceList,
            reservations,
            setOfOperationalDays,
            holidayPeriods
        )

    val children =
        placementList.map { (child, placements) ->
            val placementDateRanges = placements.map { it.dateRange }
            val supplementedReservations =
                supplementReservationsWithDailyServiceTimes(
                    placementDateRanges,
                    setOfOperationalDays,
                    reservations[child.id],
                    dailyServiceTimes[child.id]?.map { it.times },
                    absenceList[child.id]
                )

            AbsenceChild(
                child = child,
                placements =
                    range
                        .dates()
                        .mapNotNull { date ->
                            placements
                                .find { it.dateRange.includes(date) }
                                ?.let { date to it.categories }
                        }
                        .toMap(),
                actualServiceNeeds = actualServiceNeeds[child.id] ?: listOf(),
                absences = absenceList[child.id]?.groupBy { it.date } ?: mapOf(),
                backupCares =
                    backupCareList[child.id]
                        ?.flatMap { it.period.dates().map { date -> date to true } }
                        ?.toMap()
                        ?: mapOf(),
                missingHolidayReservations = missingHolidayReservations[child.id] ?: listOf(),
                reservationTotalHours =
                    sumOfHours(supplementedReservations, placementDateRanges, range),
                attendanceTotalHours =
                    attendances[child.id]
                        ?.map { HelsinkiDateTimeRange.of(it.date, it.startTime, it.endTime) }
                        ?.let { sumOfHours(it, placementDateRanges, range) }
            )
        }

    return AbsenceGroup(groupId, daycare.name, groupName, children, operationalDates.toList())
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

private fun getMissingHolidayReservations(
    childIds: List<ChildId>,
    serviceNeeds: Map<ChildId, List<ChildServiceNeedInfo>>,
    absences: Map<ChildId, List<AbsenceWithModifierInfo>>,
    reservations: Map<ChildId, List<ChildReservation>>,
    unitOperationalDates: Set<LocalDate>,
    holidayPeriods: List<HolidayPeriod>
): Map<ChildId, List<LocalDate>> {
    val holidayDates =
        holidayPeriods.flatMap { it.period.dates() }.toSet().intersect(unitOperationalDates)
    return childIds.associateWith { childId ->
        val reservedDates = reservations[childId]?.map { it.date } ?: listOf()
        val absenceDates = absences[childId]?.map { it.date } ?: listOf()
        val answeredDates = (reservedDates + absenceDates).toSet()

        val datesWithServiceNeed =
            serviceNeeds[childId]?.flatMap { it.validDuring.dates() }?.toSet() ?: emptySet()
        val datesRequiringAnswer = holidayDates.intersect(datesWithServiceNeed)

        (datesRequiringAnswer - answeredDates).toList()
    }
}

private fun supplementReservationsWithDailyServiceTimes(
    placementDateRanges: List<FiniteDateRange>,
    unitOperationalDays: Set<LocalDate>,
    reservations: List<ChildReservation>?,
    dailyServiceTimesList: List<DailyServiceTimesValue>?,
    absences: List<AbsenceWithModifierInfo>?
): List<HelsinkiDateTimeRange> {
    val absenceDates = absences?.map { it.date }?.toSet() ?: setOf()

    val reservationRanges =
        reservations
            ?.flatMap {
                when (it.reservation) {
                    is Reservation.Times ->
                        listOf(
                            HelsinkiDateTimeRange.of(
                                it.date,
                                it.reservation.startTime.withNano(0).withSecond(0),
                                it.reservation.endTime.withNano(0).withSecond(0)
                            )
                        )

                    // Reserved but no times -> use daily service times
                    is Reservation.NoTimes -> listOf()
                }
            }
            ?.sortedBy { it.start }
            ?.fold(listOf<HelsinkiDateTimeRange>()) { timeRanges, timeRange ->
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
            ?.filterNot { absenceDates.contains(it.start.toLocalDate()) }
            ?: listOf()

    val reservedDates = reservationRanges.map { it.start.toLocalDate() }.toSet()

    val dailyServiceTimeRanges =
        if (dailyServiceTimesList.isNullOrEmpty()) {
            listOf()
        } else {
            placementDateRanges
                .flatMap { it.dates() }
                .filterNot { reservedDates.contains(it) }
                .filterNot { absenceDates.contains(it) }
                .filter { unitOperationalDays.contains(it) }
                .mapNotNull { date ->
                    val dailyServiceTimes =
                        dailyServiceTimesList.find { it.validityPeriod.includes(date) }
                            ?: return@mapNotNull null

                    val times =
                        when (dailyServiceTimes) {
                            is DailyServiceTimesValue.RegularTimes ->
                                dailyServiceTimes.regularTimes.start to
                                    dailyServiceTimes.regularTimes.end
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

data class AbsenceGroup(
    val groupId: GroupId,
    val daycareName: String,
    val groupName: String,
    val children: List<AbsenceChild>,
    val operationDays: List<LocalDate>
)

data class AbsenceChild(
    val child: Child,
    val placements: Map<LocalDate, Set<AbsenceCategory>>,
    val actualServiceNeeds: List<ChildServiceNeedInfo>,
    val absences: Map<LocalDate, List<AbsenceWithModifierInfo>>,
    val backupCares: Map<LocalDate, Boolean>,
    val missingHolidayReservations: List<LocalDate>,
    val reservationTotalHours: Int?,
    val attendanceTotalHours: Int?
)

data class Child(
    val id: ChildId,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate
)

data class ChildServiceNeedInfo(
    val childId: ChildId,
    val hasContractDays: Boolean,
    val optionName: String,
    val validDuring: FiniteDateRange
)

data class AbsencePlacement(val dateRange: FiniteDateRange, val categories: Set<AbsenceCategory>)

data class Absence(
    val id: AbsenceId,
    val childId: ChildId,
    val date: LocalDate,
    val category: AbsenceCategory,
    val absenceType: AbsenceType
)

data class AbsenceWithModifierInfo(
    val id: AbsenceId,
    val childId: ChildId,
    val date: LocalDate,
    val category: AbsenceCategory,
    val absenceType: AbsenceType,
    val modifiedByType: EvakaUserType,
    val modifiedAt: HelsinkiDateTime
)

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
