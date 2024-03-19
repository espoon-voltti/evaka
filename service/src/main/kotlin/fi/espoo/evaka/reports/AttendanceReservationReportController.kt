// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import com.fasterxml.jackson.annotation.JsonFormat
import fi.espoo.evaka.Audit
import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.dailyservicetimes.getDailyServiceTimesForChildren
import fi.espoo.evaka.daycare.CareType
import fi.espoo.evaka.daycare.getDaycare
import fi.espoo.evaka.occupancy.familyUnitPlacementCoefficient
import fi.espoo.evaka.shared.AbsenceId
import fi.espoo.evaka.shared.AttendanceReservationId
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class AttendanceReservationReportController(private val accessControl: AccessControl) {

    @GetMapping("/reports/attendance-reservation/{unitId}")
    fun getAttendanceReservationReportByUnit(
        db: Database,
        clock: EvakaClock,
        user: AuthenticatedUser,
        @PathVariable unitId: DaycareId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) start: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) end: LocalDate,
        @RequestParam groupIds: List<GroupId>?
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
                        unitId
                    )
                    tx.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    getAttendanceReservationReport(
                        tx,
                        start,
                        end,
                        unitId,
                        groupIds?.ifEmpty { null }
                    )
                }
            }
            .also {
                Audit.AttendanceReservationReportRead.log(
                    targetId = unitId,
                    meta =
                        mapOf(
                            "groupIds" to groupIds,
                            "start" to start,
                            "end" to end,
                            "count" to it.size
                        )
                )
            }
    }

    @GetMapping("/reports/attendance-reservation/{unitId}/by-child")
    fun getAttendanceReservationReportByUnitAndChild(
        db: Database,
        clock: EvakaClock,
        user: AuthenticatedUser,
        @PathVariable unitId: DaycareId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) start: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) end: LocalDate,
        @RequestParam groupIds: List<GroupId>?,
    ): List<AttendanceReservationReportByChildRow> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_ATTENDANCE_RESERVATION_REPORT,
                        unitId
                    )
                    tx.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    tx.getAttendanceReservationReportByChild(
                        start,
                        end,
                        unitId,
                        groupIds?.ifEmpty { null }
                    )
                }
            }
            .also {
                Audit.AttendanceReservationReportRead.log(
                    targetId = unitId,
                    meta =
                        mapOf(
                            "groupIds" to groupIds,
                            "start" to start,
                            "end" to end,
                            "count" to it.size
                        )
                )
            }
    }
}

private data class PlacementInfoRow(
    val date: LocalDate,
    val childId: ChildId,
    val groupId: GroupId?,
    val groupName: String?,
    val occupancyCoefficientUnder: Double,
    val occupancyCoefficientOver: Double
)

private fun Database.Read.getPlacementInfo(
    start: LocalDate,
    end: LocalDate,
    unitId: DaycareId,
    groupIds: List<GroupId>?
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
    dgp.daycare_group_id as group_id,
    dg.name as group_name,
    default_sno.occupancy_coefficient AS occupancy_coefficient_over, 
    default_sno.occupancy_coefficient_under_3y AS occupancy_coefficient_under
FROM dates
JOIN placement pl ON daterange(pl.start_date, pl.end_date, '[]') @> dates.date
LEFT JOIN daycare_group_placement dgp on pl.id = dgp.daycare_placement_id AND daterange(dgp.start_date, dgp.end_date, '[]') @> dates.date
LEFT JOIN service_need_option default_sno ON default_sno.valid_placement_type = pl.type AND default_sno.default_option
LEFT JOIN daycare_group dg ON dg.id = dgp.daycare_group_id
WHERE pl.unit_id = ${bind(unitId)} AND (${bind(groupIds)}::uuid[] IS NULL OR dg.id = ANY(${bind(groupIds)}))
AND NOT EXISTS(
    SELECT 1
    FROM backup_care bc
    WHERE bc.child_id = pl.child_id AND daterange(bc.start_date, bc.end_date, '[]') @> dates.date
)
UNION
SELECT 
    dates.date, 
    bc.child_id,
    bc.group_id, 
    dg.name as group_name,
    default_sno.occupancy_coefficient AS occupancy_coefficient_over, 
    default_sno.occupancy_coefficient_under_3y AS occupancy_coefficient_under
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

private data class ChildRow(val childId: ChildId, val dateOfBirth: LocalDate)

private fun Database.Read.getChildInfo(
    children: List<ChildId>,
): List<ChildRow> {
    return createQuery {
            sql(
                """
SELECT p.id as child_id, p.date_of_birth
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
    val occupancyCoefficientUnder: Double,
    val occupancyCoefficientOver: Double
)

private fun Database.Read.getServiceNeeds(
    start: LocalDate,
    end: LocalDate,
    children: List<ChildId>,
): List<ServiceNeedRow> {
    return createQuery {
            sql(
                """
SELECT
    pl.child_id,
    daterange(sn.start_date, sn.end_date, '[]') * daterange(${bind(start)}, ${bind(end)}, '[]') as range,
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
    val capacityFactor: Double
)

private fun Database.Read.getCapacityFactors(
    range: FiniteDateRange,
    children: List<ChildId>,
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
    val childId: ChildId
)

private fun Database.Read.getReservations(
    start: LocalDate,
    end: LocalDate,
    children: List<ChildId>,
): Map<ChildId, List<HelsinkiDateTimeRange>> {
    return createQuery {
            sql(
                """
SELECT pl.child_id, ar.date, ar.start_time, ar.end_time
FROM attendance_reservation ar
JOIN placement pl on ar.child_id = pl.child_id AND daterange(pl.start_date, pl.end_date, '[]') @> ar.date
WHERE pl.child_id = ANY(${bind(children)}) AND daterange(${bind(start)}, ${bind(end)}, '[]') @> ar.date
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
                    end = HelsinkiDateTime.of(it.date, it.endTime)
                )
            }
        )
}

private data class AbsenceRow(val date: LocalDate, val childId: ChildId)

private fun Database.Read.getAbsences(
    start: LocalDate,
    end: LocalDate,
    children: List<ChildId>,
): Map<ChildId, List<LocalDate>> {
    return createQuery {
            sql(
                """
SELECT ab.child_id, ab.date
FROM absence ab
WHERE ab.child_id = ANY(${bind(children)}) AND daterange(${bind(start)}, ${bind(end)}, '[]') @> ab.date;
    """
            )
        }
        .toList<AbsenceRow>()
        .groupBy(keySelector = { it.childId }, valueTransform = { it.date })
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
    val absent: Boolean
) {
    fun isPresentPessimistic(during: HelsinkiDateTimeRange, bufferMinutes: Long = 15): Boolean {
        if (absent) return false
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
    val staffCountRequired: Double
)

private fun getAttendanceReservationReport(
    db: Database.Read,
    start: LocalDate,
    end: LocalDate,
    unitId: DaycareId,
    groupIds: List<GroupId>?
): List<AttendanceReservationReportRow> {
    val range = FiniteDateRange(start, end)
    val daycare = db.getDaycare(unitId)!!
    val placementStuff = db.getPlacementInfo(start, end, unitId, groupIds)
    val allChildren = placementStuff.map { it.childId }.distinct()
    val childInfoMap = db.getChildInfo(allChildren).associateBy { it.childId }
    val serviceNeedsMap = db.getServiceNeeds(start, end, allChildren).groupBy { it.childId }
    val assistanceNeedsMap = db.getCapacityFactors(range, allChildren).groupBy { it.childId }
    val reservationsMap = db.getReservations(start, end, allChildren)
    val absencesMap = db.getAbsences(start, end, allChildren)
    val serviceTimesMap = db.getDailyServiceTimesForChildren(allChildren.toSet())

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
            val absent = absencesMap[childId]?.contains(date) ?: false
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
                absent = absent
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
                .filter { daycare.operationDays.contains(it.dayOfWeek.value) }
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
                                .toDouble()
                    )
                }
                .toList()
        }
}

private fun Database.Read.getAttendanceReservationReportByChild(
    start: LocalDate,
    end: LocalDate,
    unitId: DaycareId,
    groupIds: List<GroupId>?
): List<AttendanceReservationReportByChildRow> {
    return createQuery {
            sql(
                """
WITH
dates AS (SELECT generate_series::date AS date FROM generate_series(${bind(start)}, ${bind(end)}, interval '1 day')),
children AS (
  SELECT
    g.id AS group_id, g.name AS group_name,
    date, p.id, p.last_name, p.first_name, bc.id IS NOT NULL AS is_backup_care
  FROM dates date
  JOIN placement pl ON date BETWEEN pl.start_date AND pl.end_date
  JOIN person p ON p.id = pl.child_id
  LEFT JOIN daycare_group_placement dgp ON dgp.daycare_placement_id = pl.id AND date BETWEEN dgp.start_date AND dgp.end_date
  LEFT JOIN backup_care bc ON bc.child_id = p.id AND date BETWEEN bc.start_date AND bc.end_date
  JOIN daycare u ON u.id = coalesce(bc.unit_id, pl.unit_id)
  LEFT JOIN daycare_group g ON g.daycare_id = u.id AND g.id = coalesce(bc.group_id, dgp.daycare_group_id)
  WHERE u.id = ${bind(unitId)}
    AND (${bind(groupIds)}::uuid[] IS NULL OR g.id = ANY(${bind(groupIds)}))
    AND extract(isodow FROM date) = ANY(u.operation_days)
)
SELECT
  ${if (groupIds != null) "c.group_id, c.group_name" else "NULL AS group_id, NULL as group_name"},
  c.date,
  c.id AS child_id,
  c.last_name AS child_last_name,
  c.first_name AS child_first_name,
  c.is_backup_care,
  a.id AS absence_id,
  a.absence_type,
  r.id AS reservation_id,
  COALESCE(r.start_time, (daily_service_time_for_date(c.date, c.id)).start) AS reservation_start_time,
  COALESCE(r.end_time, (daily_service_time_for_date(c.date, c.id)).end) AS reservation_end_time
FROM children c
LEFT JOIN absence a ON a.child_id = c.id AND a.date = c.date
LEFT JOIN attendance_reservation r ON r.child_id = c.id AND r.date = c.date AND r.start_time IS NOT NULL AND r.end_time IS NOT NULL
"""
            )
        }
        .toList<AttendanceReservationReportByChildRow>()
}

data class AttendanceReservationReportByChildRow(
    val groupId: GroupId?,
    val groupName: String?,
    val date: LocalDate,
    val childId: ChildId,
    val childLastName: String,
    val childFirstName: String,
    val isBackupCare: Boolean,
    val absenceId: AbsenceId?,
    val absenceType: AbsenceType?,
    val reservationId: AttendanceReservationId?,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    val reservationStartTime: LocalTime?,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    val reservationEndTime: LocalTime?
)
