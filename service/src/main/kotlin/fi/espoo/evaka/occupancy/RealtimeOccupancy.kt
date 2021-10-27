// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.occupancy

import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.jdbi.v3.core.kotlin.mapTo
import java.time.LocalDate
import java.time.LocalTime

data class RealtimeOccupancy(
    val childAttendances: List<ChildOccupancyAttendance>,
    val staffAttendances: List<StaffOccupancyAttendance>
) {
    val childCapacitySumSeries: List<ChildCapacityPoint> by lazy {
        val arrivals = childAttendances.map { child -> ChildCapacityPoint(child.arrived, child.capacity) }
        val departures = childAttendances.mapNotNull { child ->
            child.departed?.let { ChildCapacityPoint(it, -child.capacity) }
        }
        val deltas = (arrivals + departures).sortedBy { it.time }
        deltas.fold(emptyList<ChildCapacityPoint>()) { list, event ->
            if (list.isEmpty()) {
                listOf(event)
            } else if (list.last().time == event.time) {
                list.dropLast(1) + list.last().let { it.copy(capacity = it.capacity + event.capacity) }
            } else {
                list + ChildCapacityPoint(event.time, list.last().capacity + event.capacity)
            }
        }
    }

    val staffCountSeries: List<StaffCountPoint> by lazy {
        val arrivals = staffAttendances.map { staff -> StaffCountPoint(staff.arrived, 1) }
        val departures = staffAttendances.mapNotNull { staff ->
            staff.departed?.let { StaffCountPoint(it, -1) }
        }
        val deltas = (arrivals + departures).sortedBy { it.time }
        deltas.fold(emptyList<StaffCountPoint>()) { list, event ->
            if (list.isEmpty()) {
                listOf(event)
            } else if (list.last().time == event.time) {
                list.dropLast(1) + list.last().let { it.copy(count = it.count + event.count) }
            } else {
                list + StaffCountPoint(event.time, list.last().count + event.count)
            }
        }
    }

    val occupancySeries: List<OccupancyPoint> by lazy {
        val times = (childCapacitySumSeries.map { it.time } + staffCountSeries.map { it.time }).sorted()
        times.map { time ->
            OccupancyPoint(
                time = time,
                childCapacity = childCapacitySumSeries.lastOrNull { it.time <= time }?.capacity ?: 0.0,
                staffCount = staffCountSeries.lastOrNull { it.time <= time }?.count ?: 0
            )
        }
    }
}

data class ChildOccupancyAttendance(
    val arrived: HelsinkiDateTime,
    val departed: HelsinkiDateTime?,
    val capacity: Double
)

data class ChildCapacityPoint(
    val time: HelsinkiDateTime,
    val capacity: Double
)

data class StaffOccupancyAttendance(
    val arrived: HelsinkiDateTime,
    val departed: HelsinkiDateTime?
)

data class StaffCountPoint(
    val time: HelsinkiDateTime,
    val count: Int
)

data class OccupancyPoint(
    val time: HelsinkiDateTime,
    val childCapacity: Double,
    val staffCount: Int
) {
    val occupancyRatio: Double?
        get() = when {
            staffCount > 0 -> childCapacity / (7.0 * staffCount)
            childCapacity == 0.0 -> 0.0
            else -> null
        }
}

fun Database.Read.getChildOccupancyAttendances(unitId: DaycareId, date: LocalDate) = createQuery(
    """
    SELECT 
        ca.arrived,
        ca.departed,
        COALESCE(an.capacity_factor, 1) * CASE 
            WHEN u.type && array['FAMILY', 'GROUP_FAMILY']::care_types[] THEN 1.75
            WHEN extract(YEARS FROM age(ch.date_of_birth)) < 3 THEN 1.75
            ELSE 1.0
        END AS capacity
    FROM child_attendance ca
    JOIN daycare u ON u.id = ca.unit_id
    JOIN person ch ON ch.id = ca.child_id
    LEFT JOIN assistance_need an on an.child_id = ch.id AND daterange(an.start_date, an.end_date, '[]') @> :date
    WHERE ca.unit_id = :unitId AND tstzrange(ca.arrived, ca.departed) && tstzrange(:dayStart, :dayEnd)
    """.trimIndent()
)
    .bind("unitId", unitId)
    .bind("date", date)
    .bind("dayStart", HelsinkiDateTime.of(date, LocalTime.MIN))
    .bind("dayEnd", HelsinkiDateTime.of(date, LocalTime.MAX))
    .mapTo<ChildOccupancyAttendance>()
    .list()

fun Database.Read.getStaffOccupancyAttendances(unitId: DaycareId, date: LocalDate) = createQuery(
    """
    SELECT sa.arrived, sa.departed
    FROM staff_attendance_realtime sa
    JOIN daycare_group dg ON dg.id = sa.group_id
    WHERE dg.daycare_id = :unitId AND tstzrange(sa.arrived, sa.departed) && tstzrange(:dayStart, :dayEnd)
    
    UNION 
    
    SELECT sae.arrived, sae.departed
    FROM staff_attendance_external sae
    JOIN daycare_group dg ON dg.id = sae.group_id
    WHERE dg.daycare_id = :unitId AND tstzrange(sae.arrived, sae.departed) && tstzrange(:dayStart, :dayEnd)
    """.trimIndent()
)
    .bind("unitId", unitId)
    .bind("date", date)
    .bind("dayStart", HelsinkiDateTime.of(date, LocalTime.MIN))
    .bind("dayEnd", HelsinkiDateTime.of(date, LocalTime.MAX))
    .mapTo<StaffOccupancyAttendance>()
    .list()
