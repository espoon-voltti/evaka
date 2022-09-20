// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.occupancy

import fi.espoo.evaka.attendance.StaffAttendanceType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.HelsinkiDateTimeRange

data class RealtimeOccupancy(
    val childAttendances: List<ChildOccupancyAttendance>,
    val staffAttendances: List<StaffOccupancyAttendance>
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
            childAttendances.fold(listOf()) { list, child ->
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
                childCapacity = childCapacitySumSeries.lastOrNull { it.time <= time }?.capacity
                        ?: 0.0,
                staffCapacity = staffCapacitySumSeries.lastOrNull { it.time <= time }?.capacity
                        ?: 0.0
            )
        }
    }
}

data class ChildOccupancyAttendance(
    val childId: ChildId,
    val arrived: HelsinkiDateTime,
    val departed: HelsinkiDateTime?,
    val capacity: Double
)

data class ChildCapacityPoint(val time: HelsinkiDateTime, val capacity: Double)

data class StaffOccupancyAttendance(
    val arrived: HelsinkiDateTime,
    val departed: HelsinkiDateTime?,
    val capacity: Double
)

data class StaffCapacityPoint(val time: HelsinkiDateTime, val capacity: Double)

data class OccupancyPoint(
    val time: HelsinkiDateTime,
    val childCapacity: Double,
    val staffCapacity: Double
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
    timeRange: HelsinkiDateTimeRange
): List<ChildOccupancyAttendance> =
    createQuery(
            """
    SELECT 
        ca.child_id,
        (ca.date + ca.start_time) AT TIME ZONE 'Europe/Helsinki' AS arrived,
        (ca.date + ca.end_time) AT TIME ZONE 'Europe/Helsinki' AS departed,
        COALESCE(an.capacity_factor, 1) * CASE 
            WHEN u.type && array['FAMILY', 'GROUP_FAMILY']::care_types[] THEN $familyUnitPlacementCoefficient
            WHEN extract(YEARS FROM age(ca.date, ch.date_of_birth)) < 3 THEN coalesce(sno.occupancy_coefficient_under_3y, default_sno.occupancy_coefficient_under_3y)
            ELSE coalesce(sno.occupancy_coefficient, default_sno.occupancy_coefficient)
        END AS capacity
    FROM child_attendance ca
    JOIN daycare u ON u.id = ca.unit_id
    JOIN person ch ON ch.id = ca.child_id
    LEFT JOIN placement pl on pl.child_id = ca.child_id AND daterange(pl.start_date, pl.end_date, '[]') @> ca.date
    LEFT JOIN service_need sn on sn.placement_id = pl.id AND daterange(sn.start_date, sn.end_date, '[]') @> ca.date
    LEFT JOIN service_need_option sno on sn.option_id = sno.id
    LEFT JOIN service_need_option default_sno on pl.type = default_sno.valid_placement_type AND default_sno.default_option
    LEFT JOIN assistance_need an on an.child_id = ch.id AND daterange(an.start_date, an.end_date, '[]') @> ca.date
    WHERE ca.unit_id = :unitId AND tstzrange((ca.date + ca.start_time) AT TIME ZONE 'Europe/Helsinki', (ca.date + ca.end_time) AT TIME ZONE 'Europe/Helsinki') && :timeRange
    ORDER BY ca.child_id
    """.trimIndent(
            )
        )
        .bind("unitId", unitId)
        .bind("timeRange", timeRange)
        .mapTo<ChildOccupancyAttendance>()
        .list()

val presentStaffAttendanceTypes =
    "'{${StaffAttendanceType.values().filter { it.presentInGroup() }.joinToString()}}'::staff_attendance_type[]"

fun Database.Read.getStaffOccupancyAttendances(
    unitId: DaycareId,
    timeRange: HelsinkiDateTimeRange
): List<StaffOccupancyAttendance> =
    createQuery(
            """
    SELECT sa.arrived, sa.departed, sa.occupancy_coefficient AS capacity
    FROM staff_attendance_realtime sa
    JOIN daycare_group dg ON dg.id = sa.group_id
    WHERE dg.daycare_id = :unitId AND tstzrange(sa.arrived, sa.departed) && :timeRange
    AND type = ANY($presentStaffAttendanceTypes)
    
    UNION ALL
    
    SELECT sae.arrived, sae.departed, sae.occupancy_coefficient AS capacity
    FROM staff_attendance_external sae
    JOIN daycare_group dg ON dg.id = sae.group_id
    WHERE dg.daycare_id = :unitId AND tstzrange(sae.arrived, sae.departed) && :timeRange
    """.trimIndent(
            )
        )
        .bind("unitId", unitId)
        .bind("timeRange", timeRange)
        .mapTo<StaffOccupancyAttendance>()
        .list()
