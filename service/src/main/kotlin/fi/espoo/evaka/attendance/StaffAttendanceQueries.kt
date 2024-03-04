// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.StaffAttendanceExternalId
import fi.espoo.evaka.shared.StaffAttendanceRealtimeId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.HelsinkiDateTimeRange
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

fun Database.Read.getStaffAttendances(unitId: DaycareId, now: HelsinkiDateTime): List<StaffMember> =
    @Suppress("DEPRECATION")
    createQuery(
            """
    SELECT DISTINCT 
        dacl.employee_id,
        coalesce(e.preferred_first_name, e.first_name) AS first_name,
        e.last_name,
        att.id AS attendance_id,
        att.employee_id AS attendance_employee_id,
        att.group_id AS attendance_group_id,
        att.arrived AS attendance_arrived,
        att.departed AS attendance_departed,
        att.type AS attendance_type,
        att.departed_automatically AS attendance_departed_automatically,
        coalesce(dgacl.group_ids, '{}'::uuid[]) AS group_ids,
        coalesce((
            SELECT jsonb_agg(jsonb_build_object('id', a.id, 'employeeId', a.employee_id, 'groupId', a.group_id, 'arrived', a.arrived, 'departed', a.departed, 'type', a.type, 'departedAutomatically', a.departed_automatically) ORDER BY a.arrived)
            FROM staff_attendance_realtime a
            LEFT JOIN daycare_group dg ON dg.id = a.group_id
            WHERE e.id = a.employee_id AND (dg.daycare_id = :unitId OR a.group_id IS NULL) AND tstzrange(a.arrived, a.departed) && tstzrange(:rangeStart, :rangeEnd)
        ), '[]'::jsonb) AS attendances,
        coalesce((
            SELECT jsonb_agg(jsonb_build_object('start', p.start_time, 'end', p.end_time, 'type', p.type) ORDER BY p.start_time)
            FROM staff_attendance_plan p
            WHERE e.id = p.employee_id AND tstzrange(p.start_time, p.end_time) && tstzrange(:rangeStart, :rangeEnd)
        ), '[]'::jsonb) AS planned_attendances,
        EXISTS(
            SELECT 1 FROM staff_attendance_realtime osar
            WHERE osar.employee_id = dacl.employee_id
              AND :now < osar.arrived
        ) AS has_future_attendances
    FROM daycare_acl dacl
    JOIN employee e on e.id = dacl.employee_id
    LEFT JOIN LATERAL (
        SELECT employee_id, array_agg(dg.id) AS group_ids
        FROM daycare_group dg
        JOIN daycare_group_acl dgacl ON dg.id = dgacl.daycare_group_id
        WHERE dg.daycare_id = :unitId AND dgacl.employee_id = dacl.employee_id
        GROUP BY employee_id
    ) dgacl ON true
    LEFT JOIN LATERAL (
        SELECT sa.id, sa.employee_id, sa.arrived, sa.departed, sa.group_id, sa.type, sa.departed_automatically
        FROM staff_attendance_realtime sa
        JOIN daycare_group dg ON dg.id = sa.group_id
        WHERE sa.employee_id = dacl.employee_id AND dg.daycare_id = :unitId AND tstzrange(sa.arrived, sa.departed) && tstzrange(:rangeStart, :rangeEnd)
        ORDER BY sa.arrived DESC
        LIMIT 1
    ) att ON TRUE
    WHERE dacl.daycare_id = :unitId AND (dacl.role IN ('STAFF', 'SPECIAL_EDUCATION_TEACHER', 'EARLY_CHILDHOOD_EDUCATION_SECRETARY') OR dgacl.employee_id IS NOT NULL)
    ORDER BY e.last_name, first_name
    """
                .trimIndent()
        )
        .bind("unitId", unitId)
        .bind("rangeStart", now.atStartOfDay())
        .bind("rangeEnd", now.atEndOfDay())
        .bind("now", now)
        .toList<StaffMember>()

fun Database.Read.getExternalStaffAttendance(
    id: StaffAttendanceExternalId,
    now: HelsinkiDateTime
): ExternalStaffMember? =
    @Suppress("DEPRECATION")
    createQuery(
            """
    SELECT sae.id, sae.name, sae.group_id, sae.arrived
    FROM staff_attendance_external sae
    WHERE id = :id
"""
        )
        .bind("id", id)
        .bind("now", now)
        .exactlyOneOrNull<ExternalStaffMember>()

fun Database.Read.getExternalStaffAttendances(
    unitId: DaycareId,
    now: HelsinkiDateTime
): List<ExternalStaffMember> =
    @Suppress("DEPRECATION")
    createQuery(
            """
    SELECT sae.id, sae.name, sae.group_id, sae.arrived
    FROM staff_attendance_external sae
    JOIN daycare_group dg on sae.group_id = dg.id
    WHERE dg.daycare_id = :unitId AND departed IS NULL 
    """
                .trimIndent()
        )
        .bind("unitId", unitId)
        .bind("now", now)
        .toList<ExternalStaffMember>()

fun Database.Transaction.markStaffArrival(
    employeeId: EmployeeId,
    groupId: GroupId,
    arrivalTime: HelsinkiDateTime,
    occupancyCoefficient: BigDecimal
): StaffAttendanceRealtimeId =
    @Suppress("DEPRECATION")
    createUpdate(
            """
    INSERT INTO staff_attendance_realtime (employee_id, group_id, arrived, occupancy_coefficient) VALUES (
        :employeeId, :groupId, :arrived, :occupancyCoefficient
    )
    RETURNING id
    """
                .trimIndent()
        )
        .bind("employeeId", employeeId)
        .bind("groupId", groupId)
        .bind("arrived", arrivalTime)
        .bind("occupancyCoefficient", occupancyCoefficient)
        .executeAndReturnGeneratedKeys()
        .exactlyOne<StaffAttendanceRealtimeId>()

data class StaffAttendance(
    val id: StaffAttendanceRealtimeId?,
    val employeeId: EmployeeId,
    val groupId: GroupId?,
    val arrived: HelsinkiDateTime,
    val departed: HelsinkiDateTime?,
    val occupancyCoefficient: BigDecimal,
    val type: StaffAttendanceType
)

fun Database.Transaction.upsertStaffAttendance(
    attendanceId: StaffAttendanceRealtimeId?,
    employeeId: EmployeeId?,
    groupId: GroupId?,
    arrivalTime: HelsinkiDateTime,
    departureTime: HelsinkiDateTime?,
    occupancyCoefficient: BigDecimal?,
    type: StaffAttendanceType,
    departedAutomatically: Boolean = false
): StaffAttendanceRealtimeId =
    if (attendanceId == null) {
        @Suppress("DEPRECATION")
        createUpdate(
                """
            INSERT INTO staff_attendance_realtime (employee_id, group_id, arrived, departed, occupancy_coefficient, type, departed_automatically)
            VALUES (:employeeId, :groupId, :arrived, :departed, :occupancyCoefficient, :type, :departedAutomatically)
            RETURNING id
        """
                    .trimIndent()
            )
            .bind("employeeId", employeeId)
            .bind("groupId", groupId)
            .bind("arrived", arrivalTime)
            .bind("departed", departureTime)
            .bind("occupancyCoefficient", occupancyCoefficient)
            .bind("type", type)
            .bind("departedAutomatically", departedAutomatically)
            .executeAndReturnGeneratedKeys()
            .exactlyOne<StaffAttendanceRealtimeId>()
    } else {
        @Suppress("DEPRECATION")
        createUpdate(
                """
            UPDATE staff_attendance_realtime
            SET group_id = :groupId, arrived = :arrived, departed = :departed, type = :type, departed_automatically = :departedAutomatically
            WHERE id = :id
        """
                    .trimIndent()
            )
            .bind("id", attendanceId)
            .bind("groupId", groupId)
            .bind("arrived", arrivalTime)
            .bind("departed", departureTime)
            .bind("type", type)
            .bind("departedAutomatically", departedAutomatically)
            .updateExactlyOne()
            .let { attendanceId }
    }

fun Database.Transaction.deleteStaffAttendance(attendanceId: StaffAttendanceRealtimeId) {
    @Suppress("DEPRECATION")
    createUpdate(
            """
           DELETE FROM staff_attendance_realtime
           WHERE id = :id
        """
                .trimIndent()
        )
        .bind("id", attendanceId)
        .updateExactlyOne()
}

fun Database.Transaction.markStaffDeparture(
    attendanceId: StaffAttendanceRealtimeId,
    departureTime: HelsinkiDateTime
) =
    @Suppress("DEPRECATION")
    createUpdate(
            """
    UPDATE staff_attendance_realtime 
    SET departed = :departed
    WHERE id = :id AND departed IS NULL AND arrived < :departed
    """
                .trimIndent()
        )
        .bind("id", attendanceId)
        .bind("departed", departureTime)
        .updateExactlyOne()

data class ExternalStaffArrival(
    val name: String,
    val groupId: GroupId,
    val arrived: HelsinkiDateTime,
    val occupancyCoefficient: BigDecimal
)

fun Database.Transaction.markExternalStaffArrival(
    params: ExternalStaffArrival
): StaffAttendanceExternalId =
    @Suppress("DEPRECATION")
    createUpdate(
            """
    INSERT INTO staff_attendance_external (name, group_id, arrived, occupancy_coefficient) VALUES (
        :name, :groupId, :arrived, :occupancyCoefficient
    ) RETURNING id
    """
                .trimIndent()
        )
        .bindKotlin(params)
        .executeAndReturnGeneratedKeys()
        .exactlyOne<StaffAttendanceExternalId>()

data class ExternalStaffDeparture(
    val id: StaffAttendanceExternalId,
    val departed: HelsinkiDateTime
)

fun Database.Transaction.markExternalStaffDeparture(params: ExternalStaffDeparture) =
    @Suppress("DEPRECATION")
    createUpdate(
            """
    UPDATE staff_attendance_external 
    SET departed = :departed, departed_automatically = false
    WHERE id = :id AND departed IS NULL AND arrived < :departed
    """
                .trimIndent()
        )
        .bindKotlin(params)
        .updateExactlyOne()

fun Database.Transaction.upsertExternalStaffAttendance(
    attendanceId: StaffAttendanceExternalId?,
    name: String?,
    groupId: GroupId?,
    arrivalTime: HelsinkiDateTime,
    departureTime: HelsinkiDateTime?,
    occupancyCoefficient: BigDecimal?,
    departedAutomatically: Boolean = false
): StaffAttendanceExternalId {
    if (attendanceId == null) {
        @Suppress("DEPRECATION")
        return createUpdate(
                """
            INSERT INTO staff_attendance_external (name, group_id, arrived, departed, occupancy_coefficient, departed_automatically)
            VALUES (:name, :groupId, :arrived, :departed, :occupancyCoefficient, :departedAutomatically)
            RETURNING id
            """
                    .trimIndent()
            )
            .bind("name", name)
            .bind("groupId", groupId)
            .bind("arrived", arrivalTime)
            .bind("departed", departureTime)
            .bind("occupancyCoefficient", occupancyCoefficient)
            .bind("departedAutomatically", departedAutomatically)
            .executeAndReturnGeneratedKeys()
            .exactlyOne<StaffAttendanceExternalId>()
    } else {
        @Suppress("DEPRECATION")
        return createUpdate(
                """
            UPDATE staff_attendance_external
            SET name = :name, arrived = :arrived, departed = :departed, departed_automatically = :departedAutomatically
            WHERE id = :id
            """
                    .trimIndent()
            )
            .bind("id", attendanceId)
            .bind("name", name)
            .bind("arrived", arrivalTime)
            .bind("departed", departureTime)
            .bind("departedAutomatically", departedAutomatically)
            .updateExactlyOne()
            .let { attendanceId }
    }
}

fun Database.Transaction.deleteExternalStaffAttendance(attendanceId: StaffAttendanceExternalId) {
    @Suppress("DEPRECATION")
    createUpdate(
            """
        DELETE FROM staff_attendance_external
        WHERE id = :id
        """
                .trimIndent()
        )
        .bind("id", attendanceId)
        .updateExactlyOne()
}

data class RawAttendance(
    val id: StaffAttendanceRealtimeId,
    val groupId: GroupId?,
    val arrived: HelsinkiDateTime,
    val departed: HelsinkiDateTime?,
    val occupancyCoefficient: BigDecimal,
    val currentOccupancyCoefficient: BigDecimal?,
    val type: StaffAttendanceType,
    val employeeId: EmployeeId,
    val firstName: String,
    val lastName: String,
    val departedAutomatically: Boolean
)

fun Database.Read.getStaffAttendancesForDateRange(
    unitId: DaycareId,
    range: FiniteDateRange
): List<RawAttendance> =
    @Suppress("DEPRECATION")
    createQuery(
            """ 
SELECT
    sa.id,
    sa.employee_id,
    sa.arrived,
    sa.departed,
    sa.group_id,
    sa.occupancy_coefficient,
    sa.type,
    emp.first_name,
    emp.last_name,
    soc.coefficient AS currentOccupancyCoefficient,
    sa.departed_automatically
FROM staff_attendance_realtime sa
JOIN daycare_group dg on sa.group_id = dg.id
JOIN employee emp ON sa.employee_id = emp.id
LEFT JOIN staff_occupancy_coefficient soc ON soc.daycare_id = dg.daycare_id AND soc.employee_id = emp.id
WHERE dg.daycare_id = :unitId AND tstzrange(sa.arrived, sa.departed) && tstzrange(:start, :end)
        """
                .trimIndent()
        )
        .bind("unitId", unitId)
        .bind("start", HelsinkiDateTime.of(range.start, LocalTime.of(0, 0)))
        .bind("end", HelsinkiDateTime.of(range.end.plusDays(1), LocalTime.of(0, 0)))
        .toList<RawAttendance>()

fun Database.Read.getEmployeeAttendancesForDate(
    unitId: DaycareId,
    employeeId: EmployeeId,
    date: LocalDate
): List<RawAttendance> =
    @Suppress("DEPRECATION")
    createQuery(
            """
SELECT
    sa.id,
    sa.employee_id,
    sa.arrived,
    sa.departed,
    sa.group_id,
    sa.occupancy_coefficient,
    sa.type,
    emp.first_name,
    emp.last_name,
    soc.coefficient AS currentOccupancyCoefficient,
    sa.departed_automatically
FROM staff_attendance_realtime sa
LEFT JOIN daycare_group dg on sa.group_id = dg.id
JOIN employee emp ON sa.employee_id = emp.id
LEFT JOIN staff_occupancy_coefficient soc ON soc.daycare_id = dg.daycare_id AND soc.employee_id = emp.id
WHERE (dg.daycare_id IS NULL OR dg.daycare_id = :unitId) AND emp.id = :employeeId AND tstzrange(sa.arrived, sa.departed) && tstzrange(:start, :end)
        """
                .trimIndent()
        )
        .bind("unitId", unitId)
        .bind("employeeId", employeeId)
        .bind("start", HelsinkiDateTime.of(date, LocalTime.of(0, 0)))
        .bind("end", HelsinkiDateTime.of(date.plusDays(1), LocalTime.of(0, 0)))
        .toList<RawAttendance>()

fun Database.Read.getStaffAttendancesWithoutGroup(
    range: FiniteDateRange,
    employeeIds: Set<EmployeeId>
): List<RawAttendance> =
    @Suppress("DEPRECATION")
    createQuery(
            """
SELECT
    sa.id,
    sa.employee_id,
    sa.arrived,
    sa.departed,
    sa.group_id,
    sa.occupancy_coefficient,
    sa.type,
    emp.first_name,
    emp.last_name,
    0 AS currentOccupancyCoefficient,
    sa.departed_automatically
FROM staff_attendance_realtime sa
JOIN employee emp ON sa.employee_id = emp.id
WHERE sa.employee_id = ANY(:employeeIds) AND sa.group_id IS NULL AND tstzrange(sa.arrived, sa.departed) && tstzrange(:start, :end)
"""
        )
        .bind("employeeIds", employeeIds)
        .bind("start", HelsinkiDateTime.of(range.start, LocalTime.of(0, 0)))
        .bind("end", HelsinkiDateTime.of(range.end.plusDays(1), LocalTime.of(0, 0)))
        .toList<RawAttendance>()

data class RawAttendanceEmployee(
    val id: EmployeeId,
    val firstName: String,
    val lastName: String,
    val currentOccupancyCoefficient: BigDecimal?,
)

fun Database.Read.getCurrentStaffForAttendanceCalendar(
    unitId: DaycareId,
    start: LocalDate,
    end: LocalDate
): List<RawAttendanceEmployee> =
    @Suppress("DEPRECATION")
    createQuery(
            """
SELECT DISTINCT dacl.employee_id as id, emp.first_name, emp.last_name, soc.coefficient AS currentOccupancyCoefficient
FROM daycare_acl dacl
JOIN employee emp on emp.id = dacl.employee_id
LEFT JOIN daycare_group_acl dgacl ON dgacl.employee_id = emp.id
LEFT JOIN staff_occupancy_coefficient soc ON soc.daycare_id = dacl.daycare_id AND soc.employee_id = emp.id
WHERE dacl.daycare_id = :unitId AND (dacl.role IN ('STAFF', 'SPECIAL_EDUCATION_TEACHER') OR dgacl.employee_id IS NOT NULL)
    """
                .trimIndent()
        )
        .bind("unitId", unitId)
        .bind("start", start)
        .bind("end", end)
        .toList<RawAttendanceEmployee>()

fun Database.Read.getExternalStaffAttendancesByDateRange(
    unitId: DaycareId,
    range: FiniteDateRange
): List<ExternalAttendance> =
    @Suppress("DEPRECATION")
    createQuery(
            """
    SELECT sae.id, sae.name, sae.group_id, sae.arrived, sae.departed, sae.occupancy_coefficient, sae.departed_automatically
    FROM staff_attendance_external sae
    JOIN daycare_group dg on sae.group_id = dg.id
    WHERE dg.daycare_id = :unitId AND tstzrange(sae.arrived, sae.departed) && tstzrange(:start, :end)
    """
                .trimIndent()
        )
        .bind("unitId", unitId)
        .bind("start", HelsinkiDateTime.of(range.start, LocalTime.of(0, 0)))
        .bind("end", HelsinkiDateTime.of(range.end.plusDays(1), LocalTime.of(0, 0)))
        .toList<ExternalAttendance>()

fun Database.Read.getGroupsForEmployees(
    unitId: DaycareId,
    employeeIds: Set<EmployeeId>
): Map<EmployeeId, List<GroupId>> =
    @Suppress("DEPRECATION")
    createQuery(
            """
    SELECT employee_id, array_agg(daycare_group_id) AS group_ids
    FROM daycare_group_acl
    WHERE
      daycare_group_id = ANY (SELECT id FROM daycare_group WHERE daycare_id = :unitId) AND
      employee_id = ANY(:employeeIds)
    GROUP BY employee_id
    """
                .trimIndent()
        )
        .bind("unitId", unitId)
        .bind("employeeIds", employeeIds)
        .toMap { columnPair("employee_id", "group_ids") }

fun Database.Transaction.addMissingStaffAttendanceDepartures(now: HelsinkiDateTime) {
    @Suppress("DEPRECATION")
    createUpdate(
            // language=SQL
            """ 
WITH missing_planned_departures AS (
    SELECT realtime.id, plan.end_time AS planned_departure
    FROM staff_attendance_realtime realtime
          JOIN staff_attendance_plan plan ON realtime.employee_id = plan.employee_id
                AND realtime.departed IS NULL
                AND realtime.type <> 'JUSTIFIED_CHANGE'
                AND tstzrange(plan.start_time, plan.end_time, '[)') @> realtime.arrived
                AND plan.end_time < :now
)
UPDATE staff_attendance_realtime realtime
SET departed = missing_planned_departures.planned_departure,
    departed_automatically = true
FROM missing_planned_departures WHERE realtime.id = missing_planned_departures.id
"""
        )
        .bind("now", now)
        .execute()

    createUpdate {
            sql(
                """
UPDATE staff_attendance_realtime a
SET departed = a.arrived + interval '12 hours',
    departed_automatically = true
WHERE a.departed IS NULL AND a.arrived + INTERVAL '12 hours' <= ${bind(now)}
        """
            )
        }
        .execute()

    createUpdate {
            sql(
                """
UPDATE staff_attendance_external a
SET departed = a.arrived + interval '12 hours',
    departed_automatically = true
WHERE a.departed IS NULL AND a.arrived + INTERVAL '12 hours' <= ${bind(now)}
        """
            )
        }
        .execute()
}

fun Database.Read.getRealtimeStaffAttendances(): List<StaffMemberAttendance> =
    @Suppress("DEPRECATION")
    createQuery("SELECT * FROM staff_attendance_realtime").toList<StaffMemberAttendance>()

fun Database.Read.getPlannedStaffAttendances(
    employeeId: EmployeeId,
    now: HelsinkiDateTime
): List<PlannedStaffAttendance> =
    @Suppress("DEPRECATION")
    createQuery(
            """
SELECT start_time AS start, end_time AS end, type FROM staff_attendance_plan
WHERE employee_id = :employeeId AND tstzrange(start_time, end_time) && tstzrange(:start, :end)
"""
        )
        .bind("employeeId", employeeId)
        .bind("start", now.minusHours(8))
        .bind("end", now.plusHours(8))
        .toList<PlannedStaffAttendance>()

fun Database.Read.getPlannedStaffAttendanceForDays(
    employeeIds: Collection<EmployeeId>,
    range: FiniteDateRange
): Map<EmployeeId, List<PlannedStaffAttendance>> =
    @Suppress("DEPRECATION")
    createQuery(
            """
SELECT start_time AS start, end_time AS end, type, employee_id FROM staff_attendance_plan
WHERE employee_id = ANY(:employeeIds) AND (tstzrange(start_time, end_time) && tstzrange(:startTime, :endTime))
"""
        )
        .bind("employeeIds", employeeIds)
        .bind("startTime", HelsinkiDateTime.of(range.start, LocalTime.MIDNIGHT))
        .bind("endTime", HelsinkiDateTime.of(range.end.plusDays(1), LocalTime.MIDNIGHT))
        .toList { column<EmployeeId>("employee_id") to row<PlannedStaffAttendance>() }
        .groupBy({ it.first }, { it.second })

fun Database.Read.getOngoingAttendance(employeeId: EmployeeId): StaffAttendance? =
    @Suppress("DEPRECATION")
    createQuery(
            """
SELECT id, employee_id, group_id, arrived, departed, occupancy_coefficient, type
FROM staff_attendance_realtime WHERE employee_id = :employeeId AND departed IS NULL
"""
        )
        .bind("employeeId", employeeId)
        .exactlyOneOrNull<StaffAttendance>()

fun Database.Read.getLatestDepartureToday(
    employeeId: EmployeeId,
    now: HelsinkiDateTime
): StaffAttendance? =
    @Suppress("DEPRECATION")
    createQuery(
            """
SELECT id, employee_id, group_id, arrived, departed, occupancy_coefficient, type
FROM staff_attendance_realtime 
WHERE employee_id = :employeeId 
    AND :startOfToday <= departed AND departed < :startOfTomorrow
ORDER BY departed DESC LIMIT 1   
"""
        )
        .bind("employeeId", employeeId)
        .bind("startOfToday", now.atStartOfDay())
        .bind("startOfTomorrow", now.plusDays(1).atStartOfDay())
        .exactlyOneOrNull<StaffAttendance>()

fun Database.Transaction.deleteStaffAttendancesInRangeExcept(
    unitId: DaycareId,
    employeeId: EmployeeId,
    timeRange: HelsinkiDateTimeRange,
    exceptIds: List<StaffAttendanceRealtimeId>
) =
    @Suppress("DEPRECATION")
    createUpdate(
            """
DELETE FROM staff_attendance_realtime
WHERE
    (group_id IS NULL OR group_id = ANY (SELECT id FROM daycare_group WHERE daycare_id = :unitId)) AND
    employee_id = :employeeId AND
    tstzrange(arrived, departed) && :timeRange AND
    NOT id = ANY(:exceptIds)
"""
        )
        .bind("unitId", unitId)
        .bind("employeeId", employeeId)
        .bind("timeRange", timeRange)
        .bind("exceptIds", exceptIds)
        .execute()

fun Database.Transaction.deleteExternalAttendancesInRangeExcept(
    name: String,
    timeRange: HelsinkiDateTimeRange,
    exceptIds: List<StaffAttendanceExternalId>
) =
    @Suppress("DEPRECATION")
    createUpdate(
            """
DELETE FROM staff_attendance_external
WHERE name = :name AND tstzrange(arrived, departed) && :timeRange AND NOT id = ANY(:exceptIds)
"""
        )
        .bind("name", name)
        .bind("timeRange", timeRange)
        .bind("exceptIds", exceptIds)
        .execute()
