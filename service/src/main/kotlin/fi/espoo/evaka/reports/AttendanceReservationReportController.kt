// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.absence.getAbsencesOfChildrenByRange
import fi.espoo.evaka.dailyservicetimes.getDailyServiceTimesForChildren
import fi.espoo.evaka.daycare.CareType
import fi.espoo.evaka.daycare.getDaycare
import fi.espoo.evaka.occupancy.familyUnitPlacementCoefficient
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.HelsinkiDateTimeRange
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalTime
import java.time.Period
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class AttendanceReservationReportController(private val accessControl: AccessControl) {

    @GetMapping("/employee/reports/attendance-reservation/{unitId}")
    fun getAttendanceReservationReportByUnit(
        db: Database,
        clock: EvakaClock,
        user: AuthenticatedUser.Employee,
        @PathVariable unitId: DaycareId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) start: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) end: LocalDate,
        @RequestParam groupIds: List<GroupId>?,
    ): List<AttendanceReservationReportRow> {
        if (start.isAfter(end)) throw BadRequest("Inverted time range")
        if (end.isAfter(start.plusMonths(2))) throw BadRequest("Too long time range")

        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_ATTENDANCE_RESERVATION_REPORT,
                        unitId,
                    )
                    tx.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    getAttendanceReservationReport(
                        tx,
                        start,
                        end,
                        unitId,
                        groupIds?.ifEmpty { null },
                    )
                }
            }
            .also {
                Audit.AttendanceReservationReportRead.log(
                    targetId = AuditId(unitId),
                    meta =
                        mapOf(
                            "groupIds" to groupIds,
                            "start" to start,
                            "end" to end,
                            "count" to it.size,
                        ),
                )
            }
    }

    data class AttendanceReservationReportByChildBody(
        val range: FiniteDateRange,
        val unitId: DaycareId,
        val groupIds: List<GroupId>,
    )

    @PostMapping("/employee/reports/attendance-reservation/by-child")
    fun getAttendanceReservationReportByChild(
        db: Database,
        clock: EvakaClock,
        user: AuthenticatedUser.Employee,
        @RequestBody body: AttendanceReservationReportByChildBody,
    ): List<AttendanceReservationReportByChildGroup> =
        db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_ATTENDANCE_RESERVATION_REPORT,
                        body.unitId,
                    )
                    tx.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    getAttendanceReservationReportByChild(
                        tx,
                        body.range,
                        body.unitId,
                        body.groupIds.ifEmpty { null },
                    )
                }
            }
            .also {
                Audit.AttendanceReservationReportRead.log(
                    targetId = AuditId(body.unitId),
                    meta =
                        mapOf(
                            "groupIds" to body.groupIds,
                            "start" to body.range.start,
                            "end" to body.range.end,
                        ),
                )
            }
}

private data class PlacementInfoRow(
    val date: LocalDate,
    val childId: ChildId,
    val groupId: GroupId?,
    val groupName: String?,
    val placementType: PlacementType,
    val occupancyCoefficientUnder: Double,
    val occupancyCoefficientOver: Double,
    val backupCare: Boolean,
)

private fun Database.Read.getPlacementInfo(
    start: LocalDate,
    end: LocalDate,
    unitId: DaycareId,
    groupIds: List<GroupId>?,
): List<PlacementInfoRow> {
    val dates = generateSequence(start) { if (it < end) it.plusDays(1) else null }.toList()
    return createQuery {
            sql(
                """
WITH dates AS (
    SELECT unnest(${bind(dates)}::date[]) AS date
)
SELECT 
    dates.date, 
    pl.child_id, 
    dgp.daycare_group_id AS group_id,
    dg.name AS group_name,
    pl.type AS placement_type,
    default_sno.occupancy_coefficient AS occupancy_coefficient_over, 
    default_sno.occupancy_coefficient_under_3y AS occupancy_coefficient_under,
    false AS backup_care
FROM dates
JOIN placement pl ON daterange(pl.start_date, pl.end_date, '[]') @> dates.date
LEFT JOIN daycare_group_placement dgp on pl.id = dgp.daycare_placement_id AND daterange(dgp.start_date, dgp.end_date, '[]') @> dates.date
LEFT JOIN service_need_option default_sno ON default_sno.valid_placement_type = pl.type AND default_sno.default_option
LEFT JOIN daycare_group dg ON dg.id = dgp.daycare_group_id
WHERE pl.unit_id = ${bind(unitId)} ${if (groupIds != null) "AND dg.id = ANY(${bind(groupIds)})" else ""}
AND NOT EXISTS(
    SELECT 1
    FROM backup_care bc
    WHERE bc.child_id = pl.child_id AND daterange(bc.start_date, bc.end_date, '[]') @> dates.date
)
UNION ALL
SELECT 
    dates.date, 
    bc.child_id,
    bc.group_id, 
    dg.name AS group_name,
    pl.type AS placement_type,
    default_sno.occupancy_coefficient AS occupancy_coefficient_over, 
    default_sno.occupancy_coefficient_under_3y AS occupancy_coefficient_under,
    true AS backup_care
FROM dates
JOIN backup_care bc ON daterange(bc.start_date, bc.end_date, '[]') @> dates.date
JOIN placement pl ON daterange(pl.start_date, pl.end_date, '[]') @> dates.date AND pl.child_id = bc.child_id
LEFT JOIN service_need_option default_sno ON default_sno.valid_placement_type = pl.type AND default_sno.default_option
LEFT JOIN daycare_group dg ON dg.id = bc.group_id
WHERE bc.unit_id = ${bind(unitId)} AND (${bind(groupIds)}::uuid[] IS NULL OR dg.id = ANY(${bind(groupIds)}));
"""
            )
        }
        .toList<PlacementInfoRow>()
}

private data class ChildRow(
    val childId: ChildId,
    val dateOfBirth: LocalDate,
    val lastName: String,
    val firstName: String,
)

private fun Database.Read.getChildInfo(children: Set<ChildId>): List<ChildRow> {
    return createQuery {
            sql(
                """
SELECT p.id as child_id, p.date_of_birth, p.last_name, p.first_name
FROM person p
WHERE p.id = ANY(${bind(children)})
"""
            )
        }
        .toList<ChildRow>()
}

private data class ServiceNeedRow(
    val range: FiniteDateRange,
    val childId: ChildId,
    val shiftCare: ShiftCareType,
    val occupancyCoefficientUnder: Double,
    val occupancyCoefficientOver: Double,
)

private fun Database.Read.getServiceNeeds(
    start: LocalDate,
    end: LocalDate,
    children: Set<ChildId>,
): List<ServiceNeedRow> {
    return createQuery {
            sql(
                """
SELECT
    pl.child_id,
    daterange(sn.start_date, sn.end_date, '[]') * daterange(${bind(start)}, ${bind(end)}, '[]') as range,
    sn.shift_care,
    sno.occupancy_coefficient_under_3y AS occupancy_coefficient_under,
    sno.occupancy_coefficient AS occupancy_coefficient_over
FROM service_need sn
JOIN placement pl on pl.id = sn.placement_id
LEFT JOIN service_need_option sno ON sno.id = sn.option_id
WHERE pl.child_id = ANY(${bind(children)}) AND daterange(sn.start_date, sn.end_date, '[]') && daterange(${bind(start)}, ${bind(end)}, '[]');
    """
            )
        }
        .toList<ServiceNeedRow>()
}

private data class AssistanceNeedRow(
    val range: FiniteDateRange,
    val childId: ChildId,
    val capacityFactor: Double,
)

private fun Database.Read.getCapacityFactors(
    range: FiniteDateRange,
    children: Set<ChildId>,
): List<AssistanceNeedRow> =
    createQuery {
            sql(
                """
SELECT
    child_id,
    valid_during * ${bind(range)} AS range,
    capacity_factor
FROM assistance_factor af
WHERE child_id = ANY(${bind(children)}) AND valid_during && ${bind(range)}
"""
            )
        }
        .toList<AssistanceNeedRow>()

private data class ReservationRow(
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val childId: ChildId,
)

private fun Database.Read.getReservations(
    range: FiniteDateRange,
    children: Set<ChildId>,
): Map<ChildId, List<HelsinkiDateTimeRange>> {
    return createQuery {
            sql(
                """
SELECT pl.child_id, ar.date, ar.start_time, ar.end_time
FROM attendance_reservation ar
JOIN placement pl on ar.child_id = pl.child_id AND daterange(pl.start_date, pl.end_date, '[]') @> ar.date
WHERE pl.child_id = ANY(${bind(children)}) AND between_start_and_end(${bind(range)}, ar.date)
AND ar.start_time IS NOT NULL AND ar.end_time IS NOT NULL
    """
            )
        }
        .toList<ReservationRow>()
        .groupBy(
            keySelector = { it.childId },
            valueTransform = {
                HelsinkiDateTimeRange(
                    start = HelsinkiDateTime.of(it.date, it.startTime),
                    end = HelsinkiDateTime.of(it.date, it.endTime),
                )
            },
        )
}

data class DailyChildData(
    val date: LocalDate,
    val childId: ChildId,
    val groupId: GroupId?,
    val groupName: String?,
    val age: Int,
    val capacityFactor: Double,
    val serviceTimes: HelsinkiDateTimeRange?,
    val reservations: List<HelsinkiDateTimeRange>,
    val fullDayAbsence: Boolean,
) {
    fun isPresentPessimistic(during: HelsinkiDateTimeRange, bufferMinutes: Long = 15): Boolean {
        if (fullDayAbsence) return false
        if (reservations.isNotEmpty()) {
            return reservations.any {
                it.copy(end = it.end.plusMinutes(bufferMinutes)).overlaps(during)
            }
        } else if (serviceTimes != null) {
            return serviceTimes
                .copy(end = serviceTimes.end.plusMinutes(bufferMinutes))
                .overlaps(during)
        }
        return false
    }
}

private data class Group(val id: GroupId?, val name: String?)

data class AttendanceReservationReportRow(
    val groupId: GroupId?,
    val groupName: String?,
    val dateTime: HelsinkiDateTime,
    val childCountUnder3: Int,
    val childCountOver3: Int,
    val childCount: Int,
    val capacityFactor: Double,
    val staffCountRequired: Double,
)

fun getAttendanceReservationReport(
    tx: Database.Read,
    start: LocalDate,
    end: LocalDate,
    unitId: DaycareId,
    groupIds: List<GroupId>?,
): List<AttendanceReservationReportRow> {
    val range = FiniteDateRange(start, end)
    val daycare = tx.getDaycare(unitId)!!
    val placementStuff = tx.getPlacementInfo(start, end, unitId, groupIds)
    val allChildren = placementStuff.map { it.childId }.toSet()
    val childInfoMap = tx.getChildInfo(allChildren).associateBy { it.childId }
    val serviceNeedsMap = tx.getServiceNeeds(start, end, allChildren).groupBy { it.childId }
    val assistanceNeedsMap = tx.getCapacityFactors(range, allChildren).groupBy { it.childId }
    val reservationsMap = tx.getReservations(range, allChildren)
    val absences =
        tx.getAbsencesOfChildrenByRange(allChildren, range.asDateRange())
            .groupBy { it.childId }
            .mapValues { (_, absences) ->
                absences.groupBy({ it.date }, { it.category }).mapValues { it.value.toSet() }
            }
    val serviceTimesMap = tx.getDailyServiceTimesForChildren(allChildren.toSet())

    val dailyChildData =
        placementStuff.map { placementInfo ->
            val date = placementInfo.date
            val childId = placementInfo.childId

            val serviceNeed = serviceNeedsMap[childId]?.firstOrNull { it.range.includes(date) }
            val childAgeYears = Period.between(childInfoMap[childId]!!.dateOfBirth, date).years
            val serviceNeedFactor =
                when {
                    daycare.type.any {
                        listOf(CareType.FAMILY, CareType.GROUP_FAMILY).contains(it)
                    } -> familyUnitPlacementCoefficient.toDouble()
                    childAgeYears < 3 ->
                        serviceNeed?.occupancyCoefficientUnder
                            ?: placementInfo.occupancyCoefficientUnder
                    else ->
                        serviceNeed?.occupancyCoefficientOver
                            ?: placementInfo.occupancyCoefficientOver
                }
            val assistanceNeedFactor =
                assistanceNeedsMap[childId]?.firstOrNull { it.range.includes(date) }?.capacityFactor
                    ?: 1.0
            val capacityFactor = serviceNeedFactor * assistanceNeedFactor
            val reservations =
                reservationsMap[childId]?.filter { it.start.toLocalDate() == date } ?: emptyList()
            val absenceCategories = absences[childId]?.get(date) ?: emptySet()
            val fullDayAbsence =
                absenceCategories == placementInfo.placementType.absenceCategories()
            val serviceTimes =
                serviceTimesMap[childId]
                    ?.firstOrNull { it.validityPeriod.includes(date) }
                    ?.getTimesOnDate(date)

            DailyChildData(
                childId = childId,
                date = date,
                groupId = placementInfo.groupId,
                groupName = placementInfo.groupName,
                age = childAgeYears,
                capacityFactor = capacityFactor,
                serviceTimes = serviceTimes?.asHelsinkiDateTimeRange(date),
                reservations = reservations,
                fullDayAbsence = fullDayAbsence,
            )
        }

    val seqEnd = HelsinkiDateTime.atStartOfDay(end).plusDays(1).minusMinutes(15)
    return dailyChildData
        .groupBy { if (groupIds != null) Group(it.groupId, it.groupName) else Group(null, null) }
        .let { it.ifEmpty { mapOf(Group(null, null) to emptyList()) } }
        .flatMap { (group, childrenInGroup) ->
            generateSequence(HelsinkiDateTime.atStartOfDay(start)) {
                    if (it < seqEnd) it.plusMinutes(15) else null
                }
                .filter {
                    (daycare.shiftCareOperationDays ?: daycare.operationDays).contains(
                        it.dayOfWeek.value
                    )
                }
                .map { intervalStart ->
                    val interval =
                        HelsinkiDateTimeRange(intervalStart, intervalStart.plusMinutes(15))
                    val childrenPresent =
                        childrenInGroup.filter { it.isPresentPessimistic(interval) }

                    AttendanceReservationReportRow(
                        groupId = group.id,
                        groupName = group.name,
                        dateTime = intervalStart,
                        childCountUnder3 = childrenPresent.count { it.age < 3 },
                        childCountOver3 = childrenPresent.count { it.age >= 3 },
                        childCount = childrenPresent.count(),
                        capacityFactor =
                            BigDecimal(childrenPresent.sumOf { it.capacityFactor })
                                .setScale(2, RoundingMode.HALF_UP)
                                .toDouble(),
                        staffCountRequired =
                            BigDecimal(childrenPresent.sumOf { it.capacityFactor } / 7)
                                .setScale(1, RoundingMode.HALF_UP)
                                .toDouble(),
                    )
                }
                .toList()
        }
}

private fun getAttendanceReservationReportByChild(
    tx: Database.Read,
    range: FiniteDateRange,
    unitId: DaycareId,
    groupIds: List<GroupId>?,
): List<AttendanceReservationReportByChildGroup> {
    val daycare = tx.getDaycare(unitId)!!

    val placementStuff = tx.getPlacementInfo(range.start, range.end, unitId, groupIds)
    val allChildren = placementStuff.map { it.childId }.toSet()
    val childInfoMap = tx.getChildInfo(allChildren).associateBy { it.childId }
    val serviceNeedsMap =
        tx.getServiceNeeds(range.start, range.end, allChildren).groupBy { it.childId }
    val reservationsMap = tx.getReservations(range, allChildren)
    val absences =
        tx.getAbsencesOfChildrenByRange(allChildren, range.asDateRange())
            .groupBy { it.childId }
            .mapValues { (_, absences) ->
                absences.groupBy({ it.date }, { it.category }).mapValues { it.value.toSet() }
            }
    val serviceTimesMap = tx.getDailyServiceTimesForChildren(allChildren.toSet())

    val rows =
        placementStuff.flatMap { placementInfo ->
            val serviceNeed =
                serviceNeedsMap[placementInfo.childId]?.firstOrNull {
                    it.range.includes(placementInfo.date)
                }
            val operationDays =
                daycare.shiftCareOperationDays.takeIf {
                    serviceNeed != null && serviceNeed.shiftCare != ShiftCareType.NONE
                } ?: daycare.operationDays
            if (!operationDays.contains(placementInfo.date.dayOfWeek.value)) {
                return@flatMap emptyList()
            }

            val reservations =
                (reservationsMap[placementInfo.childId] ?: emptyList())
                    .filter { it.start.toLocalDate() == placementInfo.date }
                    .sortedBy { it.start }
                    .map { TimeRange(it.start.toLocalTime(), it.end.toLocalTime()) }
            val serviceTimes =
                serviceTimesMap[placementInfo.childId]
                    ?.firstOrNull { it.validityPeriod.includes(placementInfo.date) }
                    ?.getTimesOnDate(placementInfo.date)
            val absenceCategories =
                absences[placementInfo.childId]?.get(placementInfo.date) ?: emptySet()
            val fullDayAbsence =
                absenceCategories == placementInfo.placementType.absenceCategories()

            val entry = { reservation: TimeRange?, absent: Boolean ->
                AttendanceReservationByChildEntry(
                    childId = placementInfo.childId,
                    groupId = placementInfo.groupId,
                    groupName = placementInfo.groupName,
                    date = placementInfo.date,
                    reservation = reservation,
                    fullDayAbsence = absent,
                    backupCare = placementInfo.backupCare,
                )
            }
            if (fullDayAbsence) {
                listOf(entry(null, true))
            } else {
                val effectiveReservations = reservations.ifEmpty { listOfNotNull(serviceTimes) }
                if (effectiveReservations.isEmpty()) {
                    listOf(entry(null, false))
                } else {
                    effectiveReservations.map { entry(it, false) }
                }
            }
        }

    return if (groupIds == null) {
        // Whole unit as one "group"
        listOf(
            AttendanceReservationReportByChildGroup(
                groupId = null,
                groupName = null,
                items = toItems(rows, childInfoMap),
            )
        )
    } else {
        rows
            .groupBy { it.groupId }
            .map { (groupId, rowsOfGroup) ->
                AttendanceReservationReportByChildGroup(
                    groupId = groupId,
                    groupName = rowsOfGroup.firstOrNull()?.groupName,
                    items = toItems(rowsOfGroup, childInfoMap),
                )
            }
            .sortedBy { it.groupName }
    }
}

private data class AttendanceReservationByChildEntry(
    val childId: ChildId,
    val groupId: GroupId?,
    val groupName: String?,
    val date: LocalDate,
    val reservation: TimeRange?,
    val fullDayAbsence: Boolean,
    val backupCare: Boolean,
)

private fun toItems(
    rows: List<AttendanceReservationByChildEntry>,
    childInfoMap: Map<ChildId, ChildRow>,
): List<AttendanceReservationReportByChildItem> =
    rows
        .map { row ->
            val childInfo = childInfoMap[row.childId]!!
            AttendanceReservationReportByChildItem(
                childId = row.childId,
                childFirstName = childInfo.firstName,
                childLastName = childInfo.lastName,
                date = row.date,
                reservation = row.reservation,
                fullDayAbsence = row.fullDayAbsence,
                backupCare = row.backupCare,
            )
        }
        .sortedWith(compareBy({ it.date }, { it.childLastName }, { it.childFirstName }))

data class AttendanceReservationReportByChildGroup(
    // `groupId` and `groupName` will be null if the report covers the whole unit
    val groupId: GroupId?,
    val groupName: String?,
    val items: List<AttendanceReservationReportByChildItem>,
)

data class AttendanceReservationReportByChildItem(
    val date: LocalDate,
    val childId: ChildId,
    val childLastName: String,
    val childFirstName: String,
    val reservation: TimeRange?,
    val fullDayAbsence: Boolean,
    val backupCare: Boolean,
)
