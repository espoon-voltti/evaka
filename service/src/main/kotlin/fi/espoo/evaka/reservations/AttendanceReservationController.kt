// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.EvakaEnv
import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.absence.ChildServiceNeedInfo
import fi.espoo.evaka.absence.getAbsencesOfChildByRange
import fi.espoo.evaka.assistance.getAssistanceFactorsForChildrenOverRange
import fi.espoo.evaka.attendance.OngoingAttendanceWithUnit
import fi.espoo.evaka.attendance.deleteAbsencesByDate
import fi.espoo.evaka.attendance.getOngoingAttendanceInAnyUnitForChild
import fi.espoo.evaka.dailyservicetimes.DailyServiceTimesValue
import fi.espoo.evaka.dailyservicetimes.getChildDailyServiceTimes
import fi.espoo.evaka.dailyservicetimes.getDailyServiceTimesForChildren
import fi.espoo.evaka.daycare.CareType
import fi.espoo.evaka.daycare.Daycare
import fi.espoo.evaka.daycare.getClubTerms
import fi.espoo.evaka.daycare.getDaycare
import fi.espoo.evaka.daycare.getDaycareGroupSummaries
import fi.espoo.evaka.daycare.getPreschoolTerms
import fi.espoo.evaka.holidayperiod.HolidayPeriod
import fi.espoo.evaka.holidayperiod.getHolidayPeriods
import fi.espoo.evaka.holidayperiod.getHolidayPeriodsInRange
import fi.espoo.evaka.occupancy.familyUnitPlacementCoefficient
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.ScheduleType
import fi.espoo.evaka.placement.getPlacementsForChildDuring
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.serviceneed.getChildServiceNeedInfos
import fi.espoo.evaka.serviceneed.getServiceNeedOptions
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.data.DateMap
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.domain.TimeInterval
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.shared.domain.getHolidays
import fi.espoo.evaka.shared.domain.getOperationalDatesForChild
import fi.espoo.evaka.shared.domain.getOperationalDatesForChildren
import fi.espoo.evaka.shared.domain.toFiniteDateRange
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.user.EvakaUser
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import org.jdbi.v3.core.mapper.PropagateNull
import org.jdbi.v3.json.Json
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class AttendanceReservationController(
    private val ac: AccessControl,
    private val featureConfig: FeatureConfig,
    private val env: EvakaEnv,
) {
    @GetMapping("/employee/attendance-reservations")
    fun getAttendanceReservations(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam unitId: DaycareId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
        @RequestParam includeNonOperationalDays: Boolean = false,
    ): UnitAttendanceReservations {
        if (to < from || from.plusMonths(1) < to) throw BadRequest("Invalid query dates")
        val period = FiniteDateRange(from, to)

        return db.connect { dbc ->
                dbc.read { tx ->
                    ac.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_ATTENDANCE_RESERVATIONS,
                        unitId,
                    )
                    val clubTerms = tx.getClubTerms()
                    val preschoolTerms = tx.getPreschoolTerms()

                    val unit = tx.getDaycare(unitId) ?: throw NotFound("Unit $unitId not found")
                    val familyUnitPlacement =
                        unit.type.any {
                            listOf(CareType.FAMILY, CareType.GROUP_FAMILY).contains(it)
                        }
                    val groups =
                        tx.getDaycareGroupSummaries(unitId)
                            .filter { it.endDate == null || it.endDate.isAfter(clock.today()) }
                            .map {
                                UnitAttendanceReservations.ReservationGroup(
                                    id = it.id,
                                    name = it.name,
                                )
                            }

                    val holidays = getHolidays(period)
                    val holidayPeriods = tx.getHolidayPeriodsInRange(period)
                    val unitOperationalDays =
                        getUnitOperationalDayData(
                            period,
                            unit,
                            holidays,
                            holidayPeriods,
                            includeNonOperationalDays,
                        )

                    val placementInfo = createDateMaps(tx, unitId, period)
                    val childIds = placementInfo.keys
                    val serviceTimes = tx.getDailyServiceTimesForChildren(childIds)
                    val childData = tx.getChildData(unitId, childIds, period)
                    val serviceNeedOptions = tx.getServiceNeedOptions().associateBy { it.id }
                    val defaultServiceNeedOptions =
                        serviceNeedOptions.values
                            .filter { it.defaultOption }
                            .associateBy { it.validPlacementType }
                    val assistanceFactors =
                        tx.getAssistanceFactorsForChildrenOverRange(childIds, period)

                    UnitAttendanceReservations(
                        unit = unit.name,
                        groups = groups,
                        children = childData.values.map { it.child },
                        days =
                            unitOperationalDays.map { day ->
                                val date = day.date
                                day.copy(
                                    children =
                                        childData.values.mapNotNull { childData ->
                                            val intermittentShiftCare =
                                                childData.child.serviceNeeds.any {
                                                    it.validDuring.includes(date) &&
                                                        it.shiftCare == ShiftCareType.INTERMITTENT
                                                }
                                            if (
                                                !intermittentShiftCare &&
                                                    !childData.operationalDays.contains(date)
                                            ) {
                                                return@mapNotNull null
                                            }
                                            val childId = childData.child.id
                                            val placementStatus =
                                                placementInfo[childId]?.getValue(date)
                                                    ?: return@mapNotNull null
                                            val age =
                                                ChronoUnit.YEARS.between(
                                                    childData.child.dateOfBirth,
                                                    date,
                                                )
                                            val coefficient =
                                                if (familyUnitPlacement)
                                                    BigDecimal(familyUnitPlacementCoefficient)
                                                else
                                                    childData.child.serviceNeeds
                                                        .find { sn ->
                                                            sn.validDuring.includes(date)
                                                        }
                                                        ?.let { sn ->
                                                            val option =
                                                                serviceNeedOptions[sn.optionId]
                                                            if (age < 3)
                                                                option
                                                                    ?.realizedOccupancyCoefficientUnder3y
                                                            else
                                                                option?.realizedOccupancyCoefficient
                                                        }
                                                        ?: run {
                                                            val option =
                                                                defaultServiceNeedOptions[
                                                                    placementStatus.placementType]
                                                            if (age < 3)
                                                                option
                                                                    ?.realizedOccupancyCoefficientUnder3y
                                                            else
                                                                option?.realizedOccupancyCoefficient
                                                        }
                                                        ?: BigDecimal.ONE
                                            val factor =
                                                assistanceFactors
                                                    .find {
                                                        it.childId == childId &&
                                                            it.validDuring.includes(date)
                                                    }
                                                    ?.capacityFactor
                                                    ?.toBigDecimal() ?: BigDecimal.ONE

                                            UnitAttendanceReservations.ChildRecordOfDay(
                                                childId = childData.child.id,
                                                reservations =
                                                    childData.reservations[date]
                                                        ?.takeIf {
                                                            !placementStatus.backupOtherUnit
                                                        }
                                                        ?.sorted() ?: emptyList(),
                                                attendances =
                                                    childData.attendances[date]
                                                        ?.takeIf {
                                                            !placementStatus.backupOtherUnit
                                                        }
                                                        ?.sortedBy { it.interval.start }
                                                        ?: emptyList(),
                                                absenceBillable =
                                                    childData.absences[date]
                                                        ?.get(AbsenceCategory.BILLABLE)
                                                        ?.takeIf {
                                                            !placementStatus.backupOtherUnit
                                                        },
                                                absenceNonbillable =
                                                    childData.absences[date]
                                                        ?.get(AbsenceCategory.NONBILLABLE)
                                                        ?.takeIf {
                                                            !placementStatus.backupOtherUnit
                                                        },
                                                possibleAbsenceCategories =
                                                    placementStatus.placementType
                                                        .absenceCategories(),
                                                shiftCare =
                                                    childData.child.serviceNeeds
                                                        .find { it.validDuring.includes(date) }
                                                        ?.shiftCare,
                                                dailyServiceTimes =
                                                    serviceTimes[childId]?.find {
                                                        it.validityPeriod.includes(day.date)
                                                    },
                                                groupId = placementStatus.groupId,
                                                backupGroupId =
                                                    placementStatus.backupGroupId?.takeIf {
                                                        !placementStatus.backupOtherUnit
                                                    },
                                                inOtherUnit = placementStatus.backupOtherUnit,
                                                scheduleType =
                                                    placementStatus.placementType.scheduleType(
                                                        date,
                                                        clubTerms,
                                                        preschoolTerms,
                                                    ),
                                                occupancy = coefficient * factor,
                                            )
                                        }
                                )
                            },
                    )
                }
            }
            .also {
                Audit.UnitAttendanceReservationsRead.log(
                    targetId = AuditId(unitId),
                    meta = mapOf("from" to from, "to" to to),
                )
            }
    }

    data class OngoingAttendanceResponse(val ongoingAttendance: OngoingAttendanceWithUnit?)

    @GetMapping("/employee/attendance-reservations/ongoing")
    fun getOngoingChildAttendance(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam childId: ChildId,
    ): OngoingAttendanceResponse {
        return db.connect { dbc ->
                dbc.read { tx ->
                    ac.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Child.READ_ONGOING_ATTENDANCE,
                        childId,
                    )
                    val attendance = tx.getOngoingAttendanceInAnyUnitForChild(childId)
                    OngoingAttendanceResponse(attendance)
                }
            }
            .also { Audit.ChildAttendanceOngoingRead.log(targetId = AuditId(childId)) }
    }

    @PostMapping("/employee/attendance-reservations")
    fun postReservations(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: List<DailyReservationRequest>,
    ) {
        val children = body.map { it.childId }.toSet()

        db.connect { dbc ->
                dbc.transaction {
                    ac.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Child.CREATE_ATTENDANCE_RESERVATION,
                        children,
                    )
                    createReservationsAndAbsences(
                        it,
                        clock.now(),
                        user,
                        body,
                        featureConfig.citizenReservationThresholdHours,
                        env.plannedAbsenceEnabledForHourBasedServiceNeeds,
                    )
                }
            }
            ?.also {
                Audit.AttendanceReservationEmployeeCreate.log(
                    targetId = AuditId(children),
                    meta =
                        mapOf(
                            "deletedAbsences" to it.deletedAbsences,
                            "deletedReservations" to it.deletedReservations,
                            "upsertedAbsences" to it.upsertedAbsences,
                            "upsertedReservations" to it.upsertedReservations,
                        ),
                )
            }
    }

    @PostMapping("/employee/attendance-reservations/child-date")
    fun postChildDatePresence(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: ChildDatePresence,
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    ac.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Child.UPSERT_CHILD_DATE_PRESENCE,
                        body.childId,
                    )

                    upsertChildDatePresence(tx, user.evakaUserId, clock.now(), body)
                }
            }
            .also { result ->
                Audit.ChildDatePresenceUpsert.log(
                    targetId = AuditId(body.childId),
                    meta =
                        mapOf(
                            "date" to body.date,
                            "insertedReservations" to result.insertedReservations,
                            "deletedReservations" to result.deletedReservations,
                            "insertedAttendances" to result.insertedAttendances,
                            "deletedAttendances" to result.deletedAttendances,
                        ),
                )
            }
    }

    data class ExpectedAbsencesRequest(
        val childId: ChildId,
        val date: LocalDate,
        val attendances: List<TimeRange>,
    )

    data class ExpectedAbsencesResponse(val categories: Set<AbsenceCategory>?)

    @PostMapping("/employee/attendance-reservations/child-date/expected-absences")
    fun getExpectedAbsences(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: ExpectedAbsencesRequest,
    ): ExpectedAbsencesResponse {
        if (!body.date.isBefore(clock.today())) {
            return ExpectedAbsencesResponse(categories = null)
        }

        return db.connect { dbc ->
                dbc.transaction { tx ->
                    ac.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Child.UPSERT_CHILD_DATE_PRESENCE,
                        body.childId,
                    )

                    ExpectedAbsencesResponse(
                        categories =
                            getExpectedAbsenceCategories(
                                tx = tx,
                                date = body.date,
                                childId = body.childId,
                                attendanceTimes = body.attendances,
                            )
                    )
                }
            }
            .also {
                Audit.ChildDatePresenceExpectedAbsencesCheck.log(
                    targetId = AuditId(body.childId),
                    meta = mapOf("date" to body.date),
                )
            }
    }

    @GetMapping("/employee-mobile/attendance-reservations/by-child/{childId}/confirmed-range")
    fun getConfirmedRangeData(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
    ): List<ConfirmedRangeDate> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    ac.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Child.READ_NON_RESERVABLE_RESERVATIONS,
                        childId,
                    )
                    getConfirmedRangeDates(
                        tx,
                        clock,
                        childId,
                        featureConfig.citizenReservationThresholdHours,
                    )
                }
            }
            .also { Audit.ChildConfirmedRangeReservationsRead.log(targetId = AuditId(childId)) }
    }

    @PutMapping("/employee-mobile/attendance-reservations/by-child/{childId}/confirmed-range")
    fun setConfirmedRangeReservations(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
        @RequestBody body: List<ConfirmedRangeDateUpdate>,
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    ac.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Child.UPDATE_NON_RESERVABLE_RESERVATIONS,
                        childId,
                    )

                    val range =
                        getConfirmedRange(
                            clock.now(),
                            featureConfig.citizenReservationThresholdHours,
                        )
                    if (!body.all { range.includes(it.date) }) {
                        throw BadRequest("Request contains reservable day")
                    }

                    // Remove rows from absence on dates that will have a reservation
                    body
                        .filter { it.absenceType == null }
                        .forEach { tx.deleteAbsencesByDate(childId, it.date) }

                    val previousReservations =
                        getConfirmedRangeDates(
                                tx,
                                clock,
                                childId,
                                featureConfig.citizenReservationThresholdHours,
                            )
                            .associateBy { it.date }

                    val changedReservations =
                        body.filter { new ->
                            new.reservations.toSet() !=
                                (previousReservations[new.date]?.reservations ?: emptyList())
                                    .map { it.toReservation() }
                                    .toSet()
                        }

                    // Remove rows from attendance_reservation on dates that get updated
                    changedReservations
                        .map { DateRange(it.date, it.date) }
                        .forEach { tx.deleteReservationsInRange(childId, it) }

                    tx.insertValidReservations(
                        user.evakaUserId,
                        clock.now(),
                        changedReservations.flatMap {
                            it.reservations.map { reservation ->
                                ReservationInsert(
                                    childId = childId,
                                    date = it.date,
                                    reservation.asTimeRange(),
                                )
                            }
                        },
                    )
                }
            }
            .also { Audit.ChildConfirmedRangeReservationsUpdate.log(targetId = AuditId(childId)) }
    }

    data class ReservationChildInfo(
        val id: ChildId,
        val firstName: String,
        val lastName: String,
        val preferredName: String,
        val dateOfBirth: LocalDate,
    )

    data class ChildReservationInfo(
        val childId: ChildId,
        val reservations: List<ReservationResponse>,
        val groupId: GroupId?,
        val absent: Boolean,
        val backupPlacement: BackupPlacementType?,
        val dailyServiceTimes: DailyServiceTimesValue?,
        val scheduleType: ScheduleType,
        val isInHolidayPeriod: Boolean,
    )

    enum class BackupPlacementType {
        OUT_ON_BACKUP_PLACEMENT,
        IN_BACKUP_PLACEMENT,
    }

    data class DailyChildReservationResult(
        val children: Map<ChildId, ReservationChildInfo>,
        val childReservations: List<ChildReservationInfo>,
    )

    @GetMapping("/employee-mobile/attendance-reservations/confirmed-days/daily")
    fun getChildReservationsForDay(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @RequestParam unitId: DaycareId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) examinationDate: LocalDate,
    ): DailyChildReservationResult {
        return db.connect { dbc ->
                dbc.read { tx ->
                    ac.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_CHILD_RESERVATIONS,
                        unitId,
                    )

                    val rowsByDate =
                        tx.getChildReservationsOfUnitForDay(unitId = unitId, day = examinationDate)

                    val clubTerms = tx.getClubTerms()
                    val preschoolTerms = tx.getPreschoolTerms()
                    val holidayPeriods = tx.getHolidayPeriods()
                    val dateRowsByChild = rowsByDate.associateBy { it.childId }
                    val childIds = dateRowsByChild.keys
                    val dailyServiceTimes = tx.getDailyServiceTimesForChildren(childIds)
                    val childMap = mutableMapOf<ChildId, ReservationChildInfo>()

                    val isOperationalDateByChild =
                        tx.getOperationalDatesForChildren(
                                range = examinationDate.toFiniteDateRange(),
                                children = childIds,
                            )
                            .mapValues { it.value.contains(examinationDate) }

                    val isHolidayPeriod = holidayPeriods.any { it.period.includes(examinationDate) }
                    val childReservationInfos =
                        dateRowsByChild.map { row ->
                            // every row duplicates full basic info for child
                            val childRow = row.value
                            childMap.putIfAbsent(
                                row.key,
                                ReservationChildInfo(
                                    id = childRow.childId,
                                    firstName = childRow.firstName,
                                    lastName = childRow.lastName,
                                    preferredName = childRow.preferredName,
                                    dateOfBirth = childRow.dateOfBirth,
                                ),
                            )

                            val scheduleType =
                                childRow.placementType.scheduleType(
                                    examinationDate,
                                    clubTerms,
                                    preschoolTerms,
                                )

                            val reservations =
                                row.value.reservations
                                    .sortedBy { it.start }
                                    .map {
                                        ReservationTimesForDate(
                                                startTime = it.start,
                                                endTime = it.end,
                                                date = examinationDate,
                                                modifiedAt = it.createdAt,
                                                modifiedBy = it.createdBy,
                                                staffCreated = it.staffCreated,
                                            )
                                            .toReservationTimes()
                                    }

                            val absences = row.value.absences.map { it.category }.toSet()
                            // TODO relay absence's staff created info to ChildReservationInfo

                            ChildReservationInfo(
                                reservations = reservations,
                                absent =
                                    absences.containsAll(
                                        childRow.placementType.absenceCategories()
                                    ) ||
                                        (isOperationalDateByChild[row.key] != true &&
                                            reservations.isEmpty()),
                                groupId = childRow.groupId,
                                childId = childRow.childId,
                                backupPlacement =
                                    if (childRow.unitId != childRow.placementUnitId)
                                        if (childRow.placementUnitId == unitId)
                                            BackupPlacementType.OUT_ON_BACKUP_PLACEMENT
                                        else BackupPlacementType.IN_BACKUP_PLACEMENT
                                    else null,
                                dailyServiceTimes =
                                    dailyServiceTimes[row.key]?.find {
                                        it.validityPeriod.includes(examinationDate)
                                    },
                                scheduleType = scheduleType,
                                isInHolidayPeriod = isHolidayPeriod,
                            )
                        }

                    DailyChildReservationResult(
                        children = childMap,
                        childReservations = childReservationInfos,
                    )
                }
            }
            .also {
                Audit.ChildReservationStatusRead.log(
                    targetId = AuditId(unitId),
                    meta = mapOf("childCount" to it.children.size),
                )
            }
    }

    data class GroupReservationStatisticResult(
        val calculatedPresent: BigDecimal,
        val presentCount: Int,
        val absentCount: Int,
        val groupId: GroupId?,
    )

    data class DayReservationStatisticsResult(
        val date: LocalDate,
        val groupStatistics: List<GroupReservationStatisticResult>,
    )

    @GetMapping("/employee-mobile/attendance-reservations/confirmed-days/stats")
    fun getReservationStatisticsForConfirmedDays(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @RequestParam unitId: DaycareId,
    ): List<DayReservationStatisticsResult> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    ac.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_UNIT_RESERVATION_STATISTICS,
                        unitId,
                    )

                    val unitData =
                        tx.getDaycare(unitId) ?: throw BadRequest("Invalid unit id $unitId")

                    val confirmedRange =
                        getConfirmedRange(
                            clock.now(),
                            featureConfig.citizenReservationThresholdHours,
                        )
                    val holidays = getHolidays(confirmedRange)
                    val operationalDays =
                        getUnitOperationalDayData(
                            confirmedRange,
                            unitData,
                            holidays,
                            emptyList(),
                            false,
                        )

                    val nextConfirmedUnitDays =
                        operationalDays
                            .filter { !it.dateInfo.isHoliday || it.dateInfo.shiftCareOpenOnHoliday }
                            .map { it.date }
                            .sorted()

                    val rowsByDate =
                        tx.getReservationStatisticsForUnit(
                            unitId = unitId,
                            confirmedDays = nextConfirmedUnitDays,
                        )

                    nextConfirmedUnitDays.map { date ->
                        val groupResults =
                            rowsByDate[date]?.map {
                                GroupReservationStatisticResult(
                                    presentCount = it.present,
                                    calculatedPresent = it.calculatedPresent,
                                    absentCount = it.absent,
                                    groupId = it.groupId,
                                )
                            }
                        DayReservationStatisticsResult(
                            date = date,
                            groupStatistics =
                                groupResults
                                    ?: listOf(
                                        GroupReservationStatisticResult(
                                            presentCount = 0,
                                            calculatedPresent = BigDecimal.ZERO,
                                            absentCount = 0,
                                            groupId = null,
                                        )
                                    ),
                        )
                    }
                }
            }
            .also {
                Audit.UnitDailyReservationStatistics.log(
                    targetId = AuditId(unitId),
                    meta = mapOf("dayCount" to it.size),
                )
            }
    }
}

data class ConfirmedRangeDate(
    val date: LocalDate,
    val scheduleType: ScheduleType,
    val reservations: List<ReservationResponse>,
    val absenceType: AbsenceType?,
    val dailyServiceTimes: DailyServiceTimesValue?,
)

data class UnitAttendanceReservations(
    val unit: String,
    val groups: List<ReservationGroup>,
    val children: List<Child>,
    val days: List<OperationalDay>,
) {
    data class ReservationGroup(@PropagateNull val id: GroupId, val name: String)

    data class OperationalDay(
        val date: LocalDate,
        val dateInfo: UnitDateInfo,
        val children: List<ChildRecordOfDay>,
    )

    data class UnitDateInfo(
        val normalOperatingTimes: TimeRange?,
        val shiftCareOperatingTimes: TimeRange?,
        val shiftCareOpenOnHoliday: Boolean,
        val isHoliday: Boolean,
        val isInHolidayPeriod: Boolean,
    )

    data class ChildRecordOfDay(
        val childId: ChildId,
        val reservations: List<ReservationResponse>,
        val attendances: List<AttendanceTimesForDate>,
        val absenceBillable: AbsenceTypeResponse?,
        val absenceNonbillable: AbsenceTypeResponse?,
        val possibleAbsenceCategories: Set<AbsenceCategory>,
        val shiftCare: ShiftCareType?,
        val dailyServiceTimes: DailyServiceTimesValue?,
        val groupId: GroupId?,
        val backupGroupId: GroupId?,
        val inOtherUnit: Boolean,
        val scheduleType: ScheduleType,
        val occupancy: BigDecimal,
    )

    data class Child(
        val id: ChildId,
        val firstName: String,
        val lastName: String,
        val preferredName: String,
        val dateOfBirth: LocalDate,
        @Json val serviceNeeds: List<ChildServiceNeedInfo>,
    )
}

data class ConfirmedRangeDateUpdate(
    val date: LocalDate,
    val reservations: List<Reservation>,
    val absenceType: AbsenceType?,
)

private fun getConfirmedRangeDates(
    tx: Database.Read,
    clock: EvakaClock,
    childId: ChildId,
    citizenReservationThresholdHours: Long,
): List<ConfirmedRangeDate> {
    val clubTerms = tx.getClubTerms()
    val preschoolTerms = tx.getPreschoolTerms()
    val range = getConfirmedRange(clock.now(), citizenReservationThresholdHours)
    val operationalDays = tx.getOperationalDatesForChild(range, childId)
    val placements = tx.getPlacementsForChildDuring(childId, range.start, range.end)
    val reservations = tx.getReservationsForChildInRange(childId, range)
    val absences = tx.getAbsencesOfChildByRange(childId, range.asDateRange())
    val dailyServiceTimes = tx.getChildDailyServiceTimes(childId)
    return operationalDays
        .sorted()
        .mapNotNull { date ->
            val placement =
                placements.firstOrNull { FiniteDateRange(it.startDate, it.endDate).includes(date) }
                    ?: return@mapNotNull null
            val reservationTimes = reservations[date] ?: emptyList()
            val daysAbsences = absences.filter { it.date == date }
            val dailyServiceTime =
                dailyServiceTimes.firstOrNull { it.times.validityPeriod.includes(date) }
            val absenceCategories = placement.type.absenceCategories()
            val isFullDayAbsent = daysAbsences.map { it.category }.toSet() == absenceCategories

            ConfirmedRangeDate(
                date = date,
                scheduleType = placement.type.scheduleType(date, clubTerms, preschoolTerms),
                reservations = reservationTimes,
                absenceType =
                    if (isFullDayAbsent)
                        (daysAbsences.firstOrNull { it.category == AbsenceCategory.BILLABLE }
                                ?: absences.firstOrNull())
                            ?.absenceType
                    else null,
                dailyServiceTimes = dailyServiceTime?.times,
            )
        }
        .toList()
}

private fun getUnitOperationalDayData(
    period: FiniteDateRange,
    unit: Daycare,
    holidays: Set<LocalDate>,
    holidayPeriods: List<HolidayPeriod>,
    includeNonOperationalDays: Boolean,
): List<UnitAttendanceReservations.OperationalDay> {
    val holidayPeriodDates = holidayPeriods.flatMap { it.period.dates() }.toSet()
    return period
        .dates()
        .map { date ->
            UnitAttendanceReservations.OperationalDay(
                date = date,
                dateInfo =
                    UnitAttendanceReservations.UnitDateInfo(
                        normalOperatingTimes = unit.operationTimes[date.dayOfWeek.value - 1],
                        shiftCareOperatingTimes =
                            (unit.shiftCareOperationTimes ?: unit.operationTimes)[
                                date.dayOfWeek.value - 1],
                        shiftCareOpenOnHoliday = unit.shiftCareOpenOnHolidays,
                        isHoliday = holidays.contains(date),
                        isInHolidayPeriod = holidayPeriodDates.contains(date),
                    ),
                children = emptyList(),
            )
        }
        .filter {
            includeNonOperationalDays ||
                it.dateInfo.normalOperatingTimes != null ||
                it.dateInfo.shiftCareOperatingTimes != null
        }
        .toList()
}

private fun createDateMaps(
    tx: Database.Read,
    unitId: DaycareId,
    period: FiniteDateRange,
): Map<ChildId, DateMap<ChildPlacementStatus>> {
    val placements = tx.getPlacements(unitId, period)
    val groupPlacements = tx.getGroupPlacements(unitId, period)
    val backupPlacements = tx.getBackupPlacements(unitId, period)
    val childIds = placements.keys + groupPlacements.keys + backupPlacements.keys
    return childIds.associateWith { childId ->
        createDateMapForChild(
            unitId = unitId,
            placements = placements[childId] ?: emptyList(),
            groupPlacements = groupPlacements[childId] ?: emptyList(),
            backupPlacements = backupPlacements[childId] ?: emptyList(),
        )
    }
}

private fun createDateMapForChild(
    unitId: DaycareId,
    placements: List<ChildPlacement>,
    groupPlacements: List<ChildGroupPlacement>,
    backupPlacements: List<ChildBackupPlacement>,
): DateMap<ChildPlacementStatus> {
    return DateMap.of(
            placements.map { p ->
                p.period to
                    ChildPlacementStatus(
                        placementType = p.placementType,
                        groupId = null,
                        backupGroupId = null,
                        backupOtherUnit = false,
                    )
            }
        )
        .update(
            entries =
                groupPlacements.map { gp ->
                    gp.period to
                        ChildPlacementStatus(
                            placementType = gp.placementType,
                            groupId = gp.groupId,
                            backupGroupId = null,
                            backupOtherUnit = false,
                        )
                },
            resolve = { _, _, new -> new },
        )
        .update(
            entries =
                backupPlacements.map { bc ->
                    bc.period to
                        ChildPlacementStatus(
                            placementType = bc.placementType,
                            groupId = null,
                            backupGroupId = bc.groupId,
                            backupOtherUnit = bc.unitId != unitId,
                        )
                },
            resolve = { _, old, new -> new.copy(groupId = old.groupId) },
        )
}

private data class ChildPlacementStatus(
    val placementType: PlacementType,
    val groupId: GroupId?,
    val backupGroupId: GroupId?,
    val backupOtherUnit: Boolean,
)

private data class ChildPlacement(
    val period: FiniteDateRange,
    val childId: ChildId,
    val placementType: PlacementType,
)

private data class ChildGroupPlacement(
    val period: FiniteDateRange,
    val childId: ChildId,
    val groupId: GroupId?,
    val placementType: PlacementType,
)

private data class ChildBackupPlacement(
    val period: FiniteDateRange,
    val childId: ChildId,
    val unitId: DaycareId,
    val groupId: GroupId?,
    val placementType: PlacementType,
)

private fun Database.Read.getPlacements(
    unitId: DaycareId,
    dateRange: FiniteDateRange,
): Map<ChildId, List<ChildPlacement>> =
    createQuery {
            sql(
                """
SELECT
    daterange(p.start_date, p.end_date, '[]') AS period,
    p.child_id,
    p.type AS placement_type
FROM placement p
WHERE p.unit_id = ${bind(unitId)} AND daterange(p.start_date, p.end_date, '[]') && ${bind(dateRange)}
"""
            )
        }
        .toList<ChildPlacement>()
        .groupBy { it.childId }

private fun Database.Read.getGroupPlacements(
    unitId: DaycareId,
    dateRange: FiniteDateRange,
): Map<ChildId, List<ChildGroupPlacement>> =
    createQuery {
            sql(
                """
SELECT
    daterange(dgp.start_date, dgp.end_date, '[]') AS period,
    p.child_id,
    dgp.daycare_group_id AS group_id,
    p.type AS placement_type
FROM daycare_group_placement dgp
JOIN placement p ON p.id = dgp.daycare_placement_id
WHERE p.unit_id = ${bind(unitId)} AND daterange(dgp.start_date, dgp.end_date, '[]') && ${bind(dateRange)}
"""
            )
        }
        .toList<ChildGroupPlacement>()
        .groupBy { it.childId }

private fun Database.Read.getBackupPlacements(
    unitId: DaycareId,
    dateRange: FiniteDateRange,
): Map<ChildId, List<ChildBackupPlacement>> =
    createQuery {
            sql(
                """
SELECT
    daterange(bc.start_date, bc.end_date, '[]') AS period,
    bc.child_id,
    bc.unit_id,
    bc.group_id,
    p.type AS placement_type
FROM backup_care bc
JOIN placement p ON p.child_id = bc.child_id AND daterange(p.start_date, p.end_date, '[]') && daterange(bc.start_date, bc.end_date, '[]')
WHERE (p.unit_id = ${bind(unitId)} OR bc.unit_id = ${bind(unitId)}) AND daterange(bc.start_date, bc.end_date, '[]') && ${bind(dateRange)}
"""
            )
        }
        .toList<ChildBackupPlacement>()
        .groupBy { it.childId }

data class ChildData(
    val child: UnitAttendanceReservations.Child,
    val reservations: Map<LocalDate, List<ReservationResponse>>,
    val attendances: Map<LocalDate, List<AttendanceTimesForDate>>,
    val absences: Map<LocalDate, Map<AbsenceCategory, AbsenceTypeResponse>>,
    val operationalDays: Set<LocalDate>,
)

private data class ChildDataQueryResult(
    val id: ChildId,
    val firstName: String,
    val lastName: String,
    val preferredName: String,
    val dateOfBirth: LocalDate,
    @Json val reservations: List<ReservationTimesForDate>,
    @Json val attendances: List<AttendanceTimesForDate>,
    @Json val absences: List<AbsenceForDate>,
)

private data class ReservationTimesForDate(
    val date: LocalDate,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val modifiedAt: HelsinkiDateTime,
    val modifiedBy: EvakaUser,
    val staffCreated: Boolean,
) {
    fun toReservationTimes() =
        when {
            startTime == null || endTime == null ->
                ReservationResponse.NoTimes(staffCreated, modifiedAt, modifiedBy)
            else ->
                ReservationResponse.Times(
                    TimeRange(startTime, endTime),
                    staffCreated,
                    modifiedAt,
                    modifiedBy,
                )
        }
}

data class AttendanceTimesForDate(
    val date: LocalDate,
    val interval: TimeInterval,
    val modifiedAt: HelsinkiDateTime,
    val modifiedBy: EvakaUser,
)

private data class AbsenceForDate(
    val date: LocalDate,
    @Json val absenceTypeResponse: AbsenceTypeResponse,
    val category: AbsenceCategory,
)

fun Database.Read.getChildData(
    unitId: DaycareId,
    childIds: Set<ChildId>,
    dateRange: FiniteDateRange,
): Map<ChildId, ChildData> {
    val operationalDays = getOperationalDatesForChildren(dateRange, childIds)
    val serviceNeedInfos = getChildServiceNeedInfos(unitId, childIds, dateRange)

    return createQuery {
            sql(
                """
SELECT
    p.id,
    p.first_name,
    p.last_name,
    p.preferred_name,
    p.date_of_birth,
    coalesce((
        SELECT jsonb_agg(jsonb_build_object(
            'date', ar.date,
            'startTime', ar.start_time,
            'endTime', ar.end_time,
            'modifiedAt', ar.created_at,
            'modifiedBy', jsonb_build_object(
                'id', eu.id,
                'name', eu.name,
                'type', eu.type
            ),
            'staffCreated', eu.type <> 'CITIZEN'
        ) ORDER BY ar.date, ar.start_time)
        FROM attendance_reservation ar 
        JOIN evaka_user eu ON ar.created_by = eu.id
        WHERE ar.child_id = p.id AND between_start_and_end(${bind(dateRange)}, ar.date)
    ), '[]'::jsonb) AS reservations,
    coalesce((
        SELECT jsonb_agg(jsonb_build_object(
            'date', att.date,
            'interval', jsonb_build_object(
                'start', att.start_time,
                'end', att.end_time
            ),
            'modifiedAt', att.modified_at,
            'modifiedBy', jsonb_build_object(
                'id', eu.id,
                'name', eu.name,
                'type', eu.type
            )
        ) ORDER BY att.date, att.start_time)
        FROM child_attendance att
        JOIN evaka_user eu ON att.modified_by = eu.id
        WHERE att.child_id = p.id AND between_start_and_end(${bind(dateRange)}, att.date)
    ), '[]'::jsonb) AS attendances,
    coalesce((
        SELECT jsonb_agg(jsonb_build_object(
            'date', a.date,
            'category', a.category,
            'absenceTypeResponse', jsonb_build_object(
                'absenceType', a.absence_type,
                'staffCreated', eu.type <> 'CITIZEN'
            )
        ) ORDER BY a.date)
        FROM absence a
        JOIN evaka_user eu ON a.modified_by = eu.id 
        WHERE a.child_id = p.id AND between_start_and_end(${bind(dateRange)}, a.date)
    ), '[]'::jsonb) AS absences
FROM person p
WHERE p.id = ANY(${bind(childIds)})
"""
            )
        }
        .toList<ChildDataQueryResult>()
        .map { row ->
            ChildData(
                child =
                    UnitAttendanceReservations.Child(
                        id = row.id,
                        firstName = row.firstName,
                        lastName = row.lastName,
                        preferredName = row.preferredName,
                        dateOfBirth = row.dateOfBirth,
                        serviceNeeds =
                            serviceNeedInfos
                                .filter { it.childId == row.id }
                                .sortedBy { it.validDuring.start },
                    ),
                reservations =
                    row.reservations.groupBy(
                        keySelector = { it.date },
                        valueTransform = { it.toReservationTimes() },
                    ),
                attendances = row.attendances.groupBy(keySelector = { it.date }),
                absences =
                    row.absences
                        .groupBy(
                            keySelector = { it.date },
                            valueTransform = { it.category to it.absenceTypeResponse },
                        )
                        .mapValues { it.value.toMap() },
                operationalDays = operationalDays.getOrDefault(row.id, emptySet()),
            )
        }
        .associateBy { it.child.id }
}
