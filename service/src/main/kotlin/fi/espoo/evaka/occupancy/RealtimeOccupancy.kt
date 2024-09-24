// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.occupancy

import fi.espoo.evaka.attendance.StaffAttendanceType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.db.PredicateSql
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.HelsinkiDateTimeRange

data class RealtimeOccupancy(
    val childAttendances: List<ChildOccupancyAttendance>,
    val staffAttendances: List<StaffOccupancyAttendance>,
) {
    val childCapacitySumSeries: List<ChildCapacityPoint> by lazy {
        val isMidnight = { time: HelsinkiDateTime -> time.hour == 0 && time.minute == 0 }
        val departedOneMinuteEarlier =
            { cur: ChildOccupancyAttendance, prev: ChildOccupancyAttendance ->
                isMidnight(cur.arrived) &&
                    cur.childId == prev.childId &&
                    cur.arrived.minusMinutes(1) == prev.departed
            }

        val attns: List<ChildOccupancyAttendance> =
            childAttendances.sortedWith(compareBy({ it.childId }, { it.arrived })).fold(listOf()) {
                list,
                child ->
                if (list.isNotEmpty() && departedOneMinuteEarlier(child, list.last())) {
                    list.dropLast(1) + list.last().copy(departed = child.departed)
                } else {
                    list + child
                }
            }
        val arrivals = attns.map { child -> ChildCapacityPoint(child.arrived, child.capacity) }
        val departures =
            attns.mapNotNull { child ->
                child.departed?.let { ChildCapacityPoint(it, -child.capacity) }
            }
        val deltas = (arrivals + departures).sortedBy { it.time }
        deltas.fold(listOf()) { list, event ->
            if (list.isEmpty()) {
                listOf(event)
            } else if (list.last().time == event.time) {
                list.dropLast(1) +
                    list.last().let { it.copy(capacity = it.capacity + event.capacity) }
            } else {
                list + ChildCapacityPoint(event.time, list.last().capacity + event.capacity)
            }
        }
    }

    val staffCapacitySumSeries: List<StaffCapacityPoint> by lazy {
        val arrivals =
            staffAttendances.map { staff -> StaffCapacityPoint(staff.arrived, staff.capacity) }
        val departures =
            staffAttendances.mapNotNull { staff ->
                staff.departed?.let { StaffCapacityPoint(it, -staff.capacity) }
            }
        val deltas = (arrivals + departures).sortedBy { it.time }
        deltas.fold(listOf()) { list, event ->
            if (list.isEmpty()) {
                listOf(event)
            } else if (list.last().time == event.time) {
                list.dropLast(1) +
                    list.last().let { it.copy(capacity = it.capacity + event.capacity) }
            } else {
                list + StaffCapacityPoint(event.time, list.last().capacity + event.capacity)
            }
        }
    }

    val occupancySeries: List<OccupancyPoint> by lazy {
        val times =
            (childCapacitySumSeries.map { it.time } + staffCapacitySumSeries.map { it.time })
                .sorted()
                .distinct()
        times.map { time ->
            OccupancyPoint(
                time = time,
                childCapacity =
                    childCapacitySumSeries.lastOrNull { it.time <= time }?.capacity ?: 0.0,
                staffCapacity =
                    staffCapacitySumSeries.lastOrNull { it.time <= time }?.capacity ?: 0.0,
            )
        }
    }
}

data class ChildOccupancyAttendance(
    val childId: ChildId,
    val arrived: HelsinkiDateTime,
    val departed: HelsinkiDateTime?,
    val capacity: Double,
)

data class ChildCapacityPoint(val time: HelsinkiDateTime, val capacity: Double)

data class StaffOccupancyAttendance(
    val arrived: HelsinkiDateTime,
    val departed: HelsinkiDateTime?,
    val capacity: Double,
)

data class StaffCapacityPoint(val time: HelsinkiDateTime, val capacity: Double)

data class OccupancyPoint(
    val time: HelsinkiDateTime,
    val childCapacity: Double,
    val staffCapacity: Double,
) {
    val occupancyRatio: Double?
        get() =
            when {
                staffCapacity > 0 -> childCapacity / staffCapacity
                childCapacity == 0.0 -> 0.0
                else -> null
            }
}

fun Database.Read.getChildOccupancyAttendances(
    unitId: DaycareId,
    timeRange: HelsinkiDateTimeRange,
    groupIds: List<GroupId>? = null,
): List<ChildOccupancyAttendance> {
    val groupFilter =
        if (groupIds == null) PredicateSql.alwaysTrue()
        else
            PredicateSql {
                where(
                    """
EXISTS(
    SELECT FROM daycare_group_placement dgp
    WHERE dgp.daycare_group_id = ANY (${bind(groupIds)}) AND dgp.daycare_placement_id = pl.id AND daterange(dgp.start_date, dgp.end_date, '[]') @> ca.date
) OR EXISTS(
    SELECT FROM backup_care bc
    WHERE bc.group_id = ANY (${bind(groupIds)}) AND bc.child_id = ca.child_id AND daterange(bc.start_date, bc.end_date, '[]') @> ca.date
)            
"""
                )
            }

    return createQuery {
            sql(
                """
SELECT 
    ca.child_id,
    ca.arrived,
    ca.departed,
    COALESCE(COALESCE(an.capacity_factor, 1) * CASE 
        WHEN u.type && array['FAMILY', 'GROUP_FAMILY']::care_types[] THEN $familyUnitPlacementCoefficient
        WHEN extract(YEARS FROM age(ca.date, ch.date_of_birth)) < 3 THEN coalesce(sno.realized_occupancy_coefficient_under_3y, default_sno.realized_occupancy_coefficient_under_3y)
        ELSE coalesce(sno.realized_occupancy_coefficient, default_sno.realized_occupancy_coefficient)
    END, 1.0) AS capacity
FROM child_attendance ca
JOIN daycare u ON u.id = ca.unit_id
JOIN person ch ON ch.id = ca.child_id
LEFT JOIN placement pl on pl.child_id = ca.child_id AND daterange(pl.start_date, pl.end_date, '[]') @> ca.date
LEFT JOIN service_need sn on sn.placement_id = pl.id AND daterange(sn.start_date, sn.end_date, '[]') @> ca.date
LEFT JOIN service_need_option sno on sn.option_id = sno.id
LEFT JOIN service_need_option default_sno on pl.type = default_sno.valid_placement_type AND default_sno.default_option
LEFT JOIN assistance_factor an on an.child_id = ch.id AND an.valid_during @> ca.date
WHERE
    ca.unit_id = ${bind(unitId)} AND
    tstzrange(ca.arrived, ca.departed) && ${bind(timeRange)} AND
    ${predicate(groupFilter)}
"""
            )
        }
        .toList<ChildOccupancyAttendance>()
}

val presentStaffAttendanceTypes =
    "'{${StaffAttendanceType.entries.filter { it.presentInGroup() }.joinToString()}}'::staff_attendance_type[]"

fun Database.Read.getStaffOccupancyAttendances(
    unitId: DaycareId,
    timeRange: HelsinkiDateTimeRange,
    groupIds: List<GroupId>? = null,
): List<StaffOccupancyAttendance> {
    val groupFilter =
        if (groupIds == null) Predicate.alwaysTrue()
        else Predicate { where("$it.group_id = ANY (${bind(groupIds)})") }
    return createQuery {
            sql(
                """
SELECT sa.arrived, sa.departed, sa.occupancy_coefficient AS capacity
FROM staff_attendance_realtime sa
JOIN daycare_group dg ON dg.id = sa.group_id
WHERE
    dg.daycare_id = ${bind(unitId)} AND tstzrange(sa.arrived, sa.departed) && ${bind(timeRange)} AND
    type = ANY($presentStaffAttendanceTypes) AND
    ${predicate(groupFilter.forTable("sa"))}

UNION ALL

SELECT sae.arrived, sae.departed, sae.occupancy_coefficient AS capacity
FROM staff_attendance_external sae
JOIN daycare_group dg ON dg.id = sae.group_id
WHERE
    dg.daycare_id = ${bind(unitId)} AND
    tstzrange(sae.arrived, sae.departed) && ${bind(timeRange)} AND
    ${predicate(groupFilter.forTable("sae"))}
"""
            )
        }
        .toList<StaffOccupancyAttendance>()
}
