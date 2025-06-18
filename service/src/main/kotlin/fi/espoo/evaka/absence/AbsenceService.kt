// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.absence

import com.fasterxml.jackson.annotation.JsonIgnore
import fi.espoo.evaka.EvakaEnv
import fi.espoo.evaka.absence.AbsenceType.OTHER_ABSENCE
import fi.espoo.evaka.absence.AbsenceType.PLANNED_ABSENCE
import fi.espoo.evaka.absence.AbsenceType.SICKLEAVE
import fi.espoo.evaka.attendance.childrenHaveAttendanceInRange
import fi.espoo.evaka.dailyservicetimes.DailyServiceTimesValue
import fi.espoo.evaka.dailyservicetimes.ServiceTimesPresenceStatus
import fi.espoo.evaka.dailyservicetimes.getChildDailyServiceTimes
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.daycare.getClubTerms
import fi.espoo.evaka.daycare.getDaycare
import fi.espoo.evaka.daycare.getDaycaresById
import fi.espoo.evaka.daycare.getPreschoolTerms
import fi.espoo.evaka.holidayperiod.getHolidayPeriodsInRange
import fi.espoo.evaka.holidayperiod.getHolidayQuestionnaires
import fi.espoo.evaka.holidayperiod.getQuestionnaireAnswers
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.ScheduleType
import fi.espoo.evaka.placement.getPlacementsForChildDuring
import fi.espoo.evaka.reservations.AbsenceRequest
import fi.espoo.evaka.reservations.Reservation
import fi.espoo.evaka.reservations.clearOldReservations
import fi.espoo.evaka.reservations.computeUsedService
import fi.espoo.evaka.reservations.getPlannedAbsenceEnabledRanges
import fi.espoo.evaka.reservations.getReservableRange
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.serviceneed.getActualServiceNeedInfosByRangeAndGroup
import fi.espoo.evaka.serviceneed.getServiceNeedsByChild
import fi.espoo.evaka.shared.AbsenceId
import fi.espoo.evaka.shared.AttendanceReservationId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.data.DateTimeSet
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.DatabaseEnum
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.shared.domain.getHolidays
import fi.espoo.evaka.shared.domain.getOperationalDatesForChildren
import fi.espoo.evaka.shared.security.PilotFeature
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.Month
import org.springframework.stereotype.Service

@Service
class AbsenceService(
    private val featureConfig: FeatureConfig,
    private val env: EvakaEnv,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val absencePushNotifications: AbsencePushNotifications,
) {
    fun createAbsences(
        tx: Database.Transaction,
        user: AuthenticatedUser,
        clock: EvakaClock,
        body: AbsenceRequest,
    ): Triple<List<AbsenceId>, List<AttendanceReservationId>?, List<AbsenceId>> {
        val now = clock.now()
        val today = now.toLocalDate()
        val reservableRange =
            getReservableRange(now, featureConfig.citizenReservationThresholdHours)

        val range = body.dateRange.intersection(FiniteDateRange(today, reservableRange.end))
        if (range == null) {
            return Triple<List<AbsenceId>, List<AttendanceReservationId>?, List<AbsenceId>>(
                emptyList(),
                emptyList(),
                emptyList(),
            )
        }
        if (!listOf(OTHER_ABSENCE, PLANNED_ABSENCE, SICKLEAVE).contains(body.absenceType)) {
            throw BadRequest("Invalid absence type")
        }

        if (tx.childrenHaveAttendanceInRange(body.childIds, range)) {
            throw BadRequest(
                "Attendance already exists for given dates",
                "ATTENDANCE_ALREADY_EXISTS",
            )
        }

        val operationalDates = tx.getOperationalDatesForChildren(range, body.childIds)

        val childPlannedAbsenceEnabled =
            range.intersection(reservableRange)?.let {
                tx.getPlannedAbsenceEnabledRanges(
                    body.childIds,
                    it,
                    env.plannedAbsenceEnabledForHourBasedServiceNeeds,
                )
            } ?: emptyMap()

        val deletedAbsences =
            tx.clearOldCitizenEditableAbsences(
                body.childIds.flatMap { childId ->
                    range
                        .dates()
                        .filter { operationalDates[childId]?.contains(it) ?: false }
                        .map { childId to it }
                },
                reservableRange = reservableRange,
            )
        // Delete reservations on days in the reservable range. Reservations in the
        // closed range are kept.
        val deletedReservations =
            range.intersection(reservableRange)?.dates()?.let { dates ->
                tx.clearOldReservations(
                    body.childIds.flatMap { childId ->
                        dates
                            .filter { operationalDates[childId]?.contains(it) ?: false }
                            .map { date -> childId to date }
                    }
                )
            }

        val holidayPeriods = tx.getHolidayPeriodsInRange(range)
        val absenceInserts =
            range
                .dates()
                .filter { date ->
                    val holidayPeriod = holidayPeriods.find { it.period.includes(date) }
                    holidayPeriod == null || holidayPeriod.reservationsOpenOn <= today
                }
                .flatMap { date ->
                    body.childIds
                        .filter { operationalDates[it]?.contains(date) ?: false }
                        .map { childId ->
                            FullDayAbsenseUpsert(
                                childId,
                                date,
                                if (
                                    reservableRange.includes(date) &&
                                        body.absenceType == OTHER_ABSENCE
                                ) {
                                    val plannedAbsenceEnabled =
                                        childPlannedAbsenceEnabled[childId]?.includes(date) ?: false
                                    if (plannedAbsenceEnabled) {
                                        PLANNED_ABSENCE
                                    } else {
                                        body.absenceType
                                    }
                                } else {
                                    body.absenceType
                                },
                                body.absenceType,
                            )
                        }
                }
                .toList()

        val insertedAbsences = tx.upsertFullDayAbsences(user.evakaUserId, now, absenceInserts)

        val notifications =
            if (absenceInserts.any { it.date == today }) {
                absencePushNotifications.getAsyncJobs(tx, today, insertedAbsences)
            } else emptyList()
        if (notifications.isNotEmpty()) {
            asyncJobRunner.plan(tx, notifications, runAt = now)
        }

        return Triple(deletedAbsences, deletedReservations, insertedAbsences)
    }
}

fun getGroupMonthCalendar(
    tx: Database.Read,
    today: LocalDate,
    groupId: GroupId,
    year: Int,
    month: Int,
    featureConfig: FeatureConfig,
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
    val allAttendances = tx.getGroupAttendances(groupId, range)
    val attendancesByChild = allAttendances.groupBy { it.childId }
    val attendances = allAttendances.groupBy { it.childId to it.date }
    val dailyServiceTimes = tx.getGroupDailyServiceTimes(groupId, range)

    val holidays = getHolidays(range)
    val holidayPeriods = tx.getHolidayPeriodsInRange(range)
    val questionnaires =
        tx.getHolidayQuestionnaires(featureConfig.holidayQuestionnaireType).filter {
            it.getPeriods().any { dateRange -> dateRange.overlaps(range) }
        }
    val answers =
        tx.getQuestionnaireAnswers(questionnaires.map { it.id }, placementList.keys.map { it.id })

    val childIdsWithHourBasedServiceNeed =
        actualServiceNeeds
            .filterValues { serviceNeeds -> serviceNeeds.any { it.daycareHoursPerMonth != null } }
            .keys
    val operationDates = tx.getOperationalDatesForChildren(range, childIdsWithHourBasedServiceNeed)

    val usedServiceByChild = mutableMapOf<ChildId, UsedServiceData>()

    val days =
        range
            .dates()
            .map { date ->
                val isOperationDay =
                    daycare.operationDays.contains(date.dayOfWeek.value) && !holidays.contains(date)
                val isShiftCareOperationDay =
                    (daycare.shiftCareOperationDays ?: daycare.operationDays).contains(
                        date.dayOfWeek.value
                    ) && (daycare.shiftCareOpenOnHolidays || !holidays.contains(date))
                val isHolidayPeriodDate = holidayPeriods.any { it.period.includes(date) }
                val isQuestionnaireDate =
                    questionnaires.any {
                        it.getPeriods().any { dateRange -> dateRange.includes(date) }
                    }
                GroupMonthCalendarDay(
                    date = date,
                    isOperationDay = isOperationDay || isShiftCareOperationDay,
                    isInHolidayPeriod = isHolidayPeriodDate,
                    children =
                        placementList
                            .mapNotNull { (child, placements) ->
                                val placement =
                                    placements.find { it.range.includes(date) }
                                        ?: return@mapNotNull null

                                val childAbsences = absences[child.id to date] ?: emptyList()
                                val childAttendances = attendances[child.id to date] ?: emptyList()
                                val childReservations =
                                    reservations[child.id to date] ?: emptyList()
                                val noAnswersForChild =
                                    answers.none {
                                        it.childId == child.id &&
                                            (it.fixedPeriod != null || it.openRanges.isNotEmpty())
                                    }
                                val scheduleType =
                                    placement.type.scheduleType(date, clubTerms, preschoolTerms)

                                val serviceNeed =
                                    actualServiceNeeds[child.id]?.find {
                                        it.validDuring.includes(date)
                                    }
                                val shiftCare = serviceNeed?.shiftCare ?: ShiftCareType.NONE
                                val isChildOperationDay =
                                    when (shiftCare) {
                                        ShiftCareType.INTERMITTENT -> true
                                        ShiftCareType.FULL -> isShiftCareOperationDay
                                        ShiftCareType.NONE -> isOperationDay
                                    }
                                if (!isChildOperationDay && childAbsences.isEmpty()) {
                                    return@mapNotNull null
                                }

                                serviceNeed?.daycareHoursPerMonth?.let { daycareHoursPerMonth ->
                                    val usedService =
                                        computeUsedService(
                                            today = today,
                                            date = date,
                                            serviceNeedHours = daycareHoursPerMonth,
                                            placementType = placement.type,
                                            preschoolTime = placement.preschoolTime,
                                            preparatoryTime = placement.preparatoryTime,
                                            operationDates = operationDates[child.id] ?: emptySet(),
                                            shiftCareType = shiftCare,
                                            absences =
                                                childAbsences.map { it.absenceType to it.category },
                                            reservations =
                                                childReservations.mapNotNull {
                                                    it.reservation.asTimeRange()
                                                },
                                            attendances =
                                                childAttendances.map { it.asTimeInterval() },
                                        )
                                    usedServiceByChild.updateKey(child.id) {
                                        (it ?: UsedServiceData(daycareHoursPerMonth)).let { totals
                                            ->
                                            totals.copy(
                                                reservedMinutes =
                                                    totals.reservedMinutes +
                                                        usedService.reservedMinutes,
                                                usedServiceMinutes =
                                                    totals.usedServiceMinutes +
                                                        usedService.usedServiceMinutes,
                                            )
                                        }
                                    }
                                }

                                GroupMonthCalendarDayChild(
                                    childId = child.id,
                                    absenceCategories = placement.type.absenceCategories(),
                                    backupCare =
                                        backupCares[child.id]?.any { it.includes(date) } ?: false,
                                    scheduleType = scheduleType,
                                    shiftCare = shiftCare,
                                    missingHolidayReservation =
                                        daycare.enabledPilotFeatures.contains(
                                            PilotFeature.RESERVATIONS
                                        ) &&
                                            scheduleType == ScheduleType.RESERVATION_REQUIRED &&
                                            isHolidayPeriodDate &&
                                            childReservations.isEmpty() &&
                                            childAbsences.isEmpty(),
                                    missingHolidayQuestionnaireAnswer =
                                        placement.type.isInvoiced() &&
                                            daycare.providerType !=
                                                ProviderType.PRIVATE_SERVICE_VOUCHER &&
                                            isQuestionnaireDate &&
                                            noAnswersForChild,
                                    absences =
                                        childAbsences.map { AbsenceWithModifierInfo.from(it) },
                                    reservations = childReservations,
                                    dailyServiceTimes =
                                        (dailyServiceTimes[child.id]?.map { it.times }
                                                ?: emptyList())
                                            .find { it.validityPeriod.includes(date) }
                                            ?.getTimesOnDate(date)
                                            ?: ServiceTimesPresenceStatus.Unknown,
                                )
                            }
                            .sortedBy { it.childId },
                )
            }
            .toList()

    val children =
        placementList
            .map { (child, placements) ->
                val placementDateRanges = placements.map { it.range }
                val absenceDates =
                    absences.keys
                        .mapNotNull { if (it.first == child.id) it.second else null }
                        .toSet()

                val possibleAttendanceDates =
                    placementDateRanges
                        .flatMap { it.dates() }
                        .filterNot { absenceDates.contains(it) }
                        .filter { date ->
                            val shiftCare =
                                actualServiceNeeds[child.id]
                                    ?.find { it.validDuring.includes(date) }
                                    ?.shiftCare ?: ShiftCareType.NONE
                            when (shiftCare) {
                                ShiftCareType.NONE ->
                                    daycare.operationDays.contains(date.dayOfWeek.value) &&
                                        !holidays.contains(date)
                                ShiftCareType.FULL,
                                ShiftCareType.INTERMITTENT ->
                                    (daycare.shiftCareOperationDays ?: daycare.operationDays)
                                        .contains(date.dayOfWeek.value) &&
                                        (daycare.shiftCareOpenOnHolidays ||
                                            !holidays.contains(date))
                            }
                        }

                val supplementedReservations =
                    supplementReservationsWithDailyServiceTimes(
                        possibleAttendanceDates,
                        reservations.entries.mapNotNull {
                            if (it.key.first == child.id) {
                                it.key.second to it.value
                            } else null
                        },
                        dailyServiceTimes[child.id]?.map { it.times },
                        absenceDates,
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
                        (attendancesByChild[child.id] ?: emptyList())
                            .mapNotNull {
                                it.endTime?.let { endTime ->
                                    TimeRange(it.startTime, endTime)
                                        .fixMidnightEndTime()
                                        .asHelsinkiDateTimeRange(it.date)
                                }
                            }
                            .let { sumOfHours(DateTimeSet.of(it), placementDateRanges, range) },
                    usedService = usedServiceByChild[child.id]?.asUsedServiceTotals(),
                )
            }
            .sortedWith(compareBy({ it.lastName }, { it.firstName }))

    return GroupMonthCalendar(
        groupId = groupId,
        daycareName = daycare.name,
        daycareOperationTimes = daycare.operationTimes,
        shiftCareOperationTimes = daycare.shiftCareOperationTimes,
        groupName = groupName,
        children = children,
        days = days,
    )
}

fun <K, V> MutableMap<K, V>.updateKey(key: K, fn: (V?) -> V) {
    val current = this[key]
    this[key] = fn(current)
}

private data class UsedServiceData(
    val serviceNeedHours: Int,
    val reservedMinutes: Long = 0,
    val usedServiceMinutes: Long = 0,
) {
    fun asUsedServiceTotals(): UsedServiceTotals =
        UsedServiceTotals(
            serviceNeedHours,
            BigDecimal(reservedMinutes).divide(BigDecimal(60), 0, RoundingMode.FLOOR).toInt(),
            BigDecimal(usedServiceMinutes).divide(BigDecimal(60), 0, RoundingMode.FLOOR).toInt(),
        )
}

fun getAbsencesOfChildByMonth(
    tx: Database.Read,
    childId: ChildId,
    year: Int,
    month: Int,
): List<Absence> {
    val range = DateRange.ofMonth(year, Month.of(month))
    return tx.getAbsencesOfChildByRange(childId, range)
}

fun getFutureAbsencesOfChild(
    tx: Database.Read,
    evakaClock: EvakaClock,
    childId: ChildId,
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
    absences: Map<AbsenceCategory, AbsenceType>,
): Pair<List<AbsenceId>, List<AbsenceId>> {
    val alreadyUpToDateCategories =
        absences.entries.mapNotNull { (category, type) ->
            category.takeIf { tx.absenceExists(date, childId, category, type) }
        }
    val deletedAbsences =
        tx.deleteChildAbsences(
            childId = childId,
            date = date,
            categories = AbsenceCategory.entries.toSet().minus(alreadyUpToDateCategories.toSet()),
        )

    val insertedAbsences =
        tx.insertAbsences(
            now = now,
            userId = userId,
            absences =
                absences
                    .filterKeys { !alreadyUpToDateCategories.contains(it) }
                    .map { (category, type) -> AbsenceUpsert(childId, date, category, type) },
        )

    return insertedAbsences to deletedAbsences
}

fun generateAbsencesFromIrregularDailyServiceTimes(
    tx: Database.Transaction,
    now: HelsinkiDateTime,
    childId: ChildId,
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
        val placements = tx.getPlacementsForChildDuring(childId, period.start, period.end)
        val serviceNeeds =
            tx.getServiceNeedsByChild(childId).filter {
                FiniteDateRange(it.startDate, it.endDate).overlaps(period)
            }
        val unitIds = placements.map { it.unitId }.toSet()
        val units = tx.getDaycaresById(unitIds)

        val absencesToAdd =
            irregularDailyServiceTimes.flatMap dst@{ dailyServiceTimes ->
                val validityPeriod =
                    period.intersection(dailyServiceTimes.validityPeriod) ?: return@dst listOf()
                placements.flatMap pl@{ placement ->
                    val effectivePeriod =
                        FiniteDateRange(placement.startDate, placement.endDate)
                            .intersection(validityPeriod) ?: return@pl listOf()
                    val unit = units[placement.unitId] ?: return@pl listOf()

                    effectivePeriod.dates().toList().flatMap date@{ date ->
                        val serviceNeed =
                            serviceNeeds.find {
                                FiniteDateRange(it.startDate, it.endDate).includes(date)
                            }
                        val effectiveOperationDays =
                            if (
                                serviceNeed == null || serviceNeed.shiftCare == ShiftCareType.NONE
                            ) {
                                unit.operationDays
                            } else {
                                unit.shiftCareOperationDays ?: unit.operationDays
                            }

                        if (!effectiveOperationDays.contains(date.dayOfWeek.value))
                            return@date listOf()

                        val isIrregularAbsenceDay =
                            dailyServiceTimes.timesForDayOfWeek(date.dayOfWeek) == null
                        if (!isIrregularAbsenceDay) return@date listOf()

                        placement.type.absenceCategories().map { category ->
                            AbsenceUpsert(
                                childId = childId,
                                date = date,
                                absenceType = OTHER_ABSENCE,
                                category = category,
                            )
                        }
                    }
                }
            }
        tx.upsertGeneratedAbsences(now, absencesToAdd)
    }
    tx.deleteOldGeneratedAbsencesInRange(now, childId, period)
}

private fun supplementReservationsWithDailyServiceTimes(
    possibleAttendanceDates: List<LocalDate>,
    reservations: List<Pair<LocalDate, List<ChildReservation>>>,
    dailyServiceTimesList: List<DailyServiceTimesValue>?,
    absenceDates: Set<LocalDate>,
): DateTimeSet {
    val reservationRanges =
        DateTimeSet.of(
            reservations.flatMap { (date, reservations) ->
                if (absenceDates.contains(date)) {
                    emptyList()
                } else {
                    reservations.mapNotNull { res ->
                        when (res.reservation) {
                            is Reservation.Times ->
                                res.reservation.range
                                    .fixMidnightEndTime()
                                    .asHelsinkiDateTimeRange(date)

                            // Reserved but no times -> use daily service times
                            is Reservation.NoTimes -> null
                        }
                    }
                }
            }
        )

    val reservedDates = reservationRanges.ranges().map { it.start.toLocalDate() }.toSet()

    val dailyServiceTimeRanges =
        if (dailyServiceTimesList.isNullOrEmpty()) {
            DateTimeSet.empty()
        } else {
            val dates = possibleAttendanceDates.filterNot { reservedDates.contains(it) }
            dailyServiceTimesToPerDateTimeRanges(dates, dailyServiceTimesList)
        }

    return reservationRanges + dailyServiceTimeRanges
}

private fun dailyServiceTimesToPerDateTimeRanges(
    dates: List<LocalDate>,
    dailyServiceTimesList: List<DailyServiceTimesValue>,
): DateTimeSet {
    return DateTimeSet.of(
        dates.mapNotNull { date ->
            val dailyServiceTimes =
                dailyServiceTimesList.find { it.validityPeriod.includes(date) }
                    ?: return@mapNotNull null

            when (dailyServiceTimes) {
                is DailyServiceTimesValue.RegularTimes ->
                    dailyServiceTimes.regularTimes.asHelsinkiDateTimeRange(date)
                is DailyServiceTimesValue.IrregularTimes -> {
                    dailyServiceTimes
                        .timesForDayOfWeek(date.dayOfWeek)
                        ?.asHelsinkiDateTimeRange(date)
                }
                is DailyServiceTimesValue.VariableTimes -> null
            }
        }
    )
}

private fun sumOfHours(
    dateTimeRanges: DateTimeSet,
    placementDateRanges: List<FiniteDateRange>,
    spanningDateRange: FiniteDateRange,
): Int {
    return dateTimeRanges
        .intersection(DateTimeSet.of(placementDateRanges.map { it.asHelsinkiDateTimeRange() }))
        .intersectRanges(spanningDateRange.asHelsinkiDateTimeRange())
        .sumOf { it.getDuration().toMinutes() }
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
    val shiftCareOperationTimes: List<TimeRange?>?,
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
    val usedService: UsedServiceTotals?,
)

data class UsedServiceTotals(
    val serviceNeedHours: Int,
    val reservedHours: Int,
    val usedServiceHours: Int,
)

data class GroupMonthCalendarDay(
    val date: LocalDate,
    val isOperationDay: Boolean,
    val isInHolidayPeriod: Boolean,
    val children: List<GroupMonthCalendarDayChild>,
)

data class GroupMonthCalendarDayChild(
    val childId: ChildId,
    val absenceCategories: Set<AbsenceCategory>,
    val backupCare: Boolean,
    val missingHolidayReservation: Boolean,
    val missingHolidayQuestionnaireAnswer: Boolean,
    val absences: List<AbsenceWithModifierInfo>,
    val reservations: List<ChildReservation>,
    val dailyServiceTimes: ServiceTimesPresenceStatus,
    val scheduleType: ScheduleType,
    val shiftCare: ShiftCareType,
)

data class ChildServiceNeedInfo(
    val childId: ChildId,
    @get:JsonIgnore val optionId: ServiceNeedOptionId,
    val hasContractDays: Boolean,
    val daycareHoursPerMonth: Int?,
    val optionName: String,
    val validDuring: FiniteDateRange,
    val shiftCare: ShiftCareType,
    val partWeek: Boolean,
)

data class AbsencePlacement(
    val range: FiniteDateRange,
    val type: PlacementType,
    val preschoolTime: TimeRange?,
    val preparatoryTime: TimeRange?,
)

data class Absence(
    val childId: ChildId,
    val date: LocalDate,
    val category: AbsenceCategory,
    val absenceType: AbsenceType,
    val modifiedByStaff: Boolean,
    val modifiedAt: HelsinkiDateTime,
    val belongsToQuestionnaire: Boolean = false,
) {
    fun editableByCitizen(): Boolean =
        absenceType != AbsenceType.FREE_ABSENCE && !modifiedByStaff && !belongsToQuestionnaire
}

data class AbsenceWithModifierInfo(
    val absenceType: AbsenceType,
    val category: AbsenceCategory,
    val modifiedByStaff: Boolean,
    val modifiedAt: HelsinkiDateTime,
) {
    companion object {
        fun from(absence: Absence): AbsenceWithModifierInfo =
            AbsenceWithModifierInfo(
                absence.absenceType,
                absence.category,
                absence.modifiedByStaff,
                absence.modifiedAt,
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
