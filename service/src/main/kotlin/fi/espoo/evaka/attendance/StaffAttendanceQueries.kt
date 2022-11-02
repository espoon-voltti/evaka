// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.StaffAttendanceExternalId
import fi.espoo.evaka.shared.StaffAttendanceId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.db.mapRow
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.HelsinkiDateTimeRange
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

fun Database.Read.getStaffAttendances(unitId: DaycareId, now: HelsinkiDateTime): List<StaffMember> =
    createQuery(
            """
    SELECT DISTINCT 
        dacl.employee_id,
        e.first_name,
        e.last_name,
        att.id AS attendance_id,
        att.employee_id AS attendance_employee_id,
        att.group_id AS attendance_group_id,
        att.arrived AS attendance_arrived,
        att.departed AS attendance_departed,
        att.type AS attendance_type,
        coalesce(dgacl.group_ids, '{}'::uuid[]) AS group_ids,
        coalesce((
            SELECT jsonb_agg(jsonb_build_object('id', a.id, 'employeeId', a.employee_id, 'groupId', a.group_id, 'arrived', a.arrived, 'departed', a.departed, 'type', a.type))
            FROM staff_attendance_realtime a
            JOIN daycare_group dg ON dg.id = a.group_id
            WHERE e.id = a.employee_id AND dg.daycare_id = :unitId AND tstzrange(a.arrived, a.departed) && tstzrange(:rangeStart, :rangeEnd)
        ), '[]'::jsonb) AS attendances,
        coalesce((
            SELECT jsonb_agg(jsonb_build_object('start', p.start_time, 'end', p.end_time, 'type', p.type))
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
        SELECT sa.id, sa.employee_id, sa.arrived, sa.departed, sa.group_id, sa.type
        FROM staff_attendance_realtime sa
        JOIN daycare_group dg ON dg.id = sa.group_id
        WHERE sa.employee_id = dacl.employee_id AND dg.daycare_id = :unitId AND tstzrange(sa.arrived, sa.departed) && tstzrange(:rangeStart, :rangeEnd)
        ORDER BY sa.arrived DESC
        LIMIT 1
    ) att ON TRUE
    WHERE dacl.daycare_id = :unitId AND (dacl.role IN ('STAFF', 'SPECIAL_EDUCATION_TEACHER', 'EARLY_CHILDHOOD_EDUCATION_SECRETARY') OR dgacl.employee_id IS NOT NULL)
    ORDER BY e.last_name, e.first_name
    """
                .trimIndent()
        )
        .bind("unitId", unitId)
        .bind("rangeStart", now.atStartOfDay())
        .bind("rangeEnd", now.atEndOfDay())
        .bind("now", now)
        .mapTo<StaffMember>()
        .list()

fun Database.Read.getStaffAttendance(id: StaffAttendanceId): StaffMemberAttendance? =
    createQuery(
            """
    SELECT id, employee_id, group_id, arrived, departed, type
    FROM staff_attendance_realtime
    WHERE id = :id
"""
        )
        .bind("id", id)
        .mapTo<StaffMemberAttendance>()
        .firstOrNull()

fun Database.Read.getExternalStaffAttendance(
    id: StaffAttendanceExternalId,
    now: HelsinkiDateTime
): ExternalStaffMember? =
    createQuery(
            """
    SELECT sae.id, sae.name, sae.group_id, sae.arrived
    FROM staff_attendance_external sae
    WHERE id = :id
"""
        )
        .bind("id", id)
        .bind("now", now)
        .mapTo<ExternalStaffMember>()
        .firstOrNull()

fun Database.Read.getExternalStaffAttendances(
    unitId: DaycareId,
    now: HelsinkiDateTime
): List<ExternalStaffMember> =
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
        .mapTo<ExternalStaffMember>()
        .list()

fun Database.Transaction.markStaffArrival(
    employeeId: EmployeeId,
    groupId: GroupId,
    arrivalTime: HelsinkiDateTime,
    occupancyCoefficient: BigDecimal
): StaffAttendanceId =
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
        .mapTo<StaffAttendanceId>()
        .one()

data class StaffAttendance(
    val id: StaffAttendanceId?,
    val employeeId: EmployeeId,
    val groupId: GroupId,
    val arrived: HelsinkiDateTime,
    val departed: HelsinkiDateTime?,
    val occupancyCoefficient: BigDecimal,
    val type: StaffAttendanceType
)

fun Database.Transaction.upsertStaffAttendance(
    attendanceId: StaffAttendanceId?,
    employeeId: EmployeeId?,
    groupId: GroupId?,
    arrivalTime: HelsinkiDateTime,
    departureTime: HelsinkiDateTime?,
    occupancyCoefficient: BigDecimal?,
    type: StaffAttendanceType
): StaffAttendanceId =
    if (attendanceId === null) {
        createUpdate(
                """
            INSERT INTO staff_attendance_realtime (employee_id, group_id, arrived, departed, occupancy_coefficient, type)
            VALUES (:employeeId, :groupId, :arrived, :departed, :occupancyCoefficient, :type)
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
            .executeAndReturnGeneratedKeys()
            .mapTo<StaffAttendanceId>()
            .single()
    } else {
        createUpdate(
                """
            UPDATE staff_attendance_realtime
            SET group_id = :groupId, arrived = :arrived, departed = :departed, type = :type
            WHERE id = :id
        """
                    .trimIndent()
            )
            .bind("id", attendanceId)
            .bind("groupId", groupId)
            .bind("arrived", arrivalTime)
            .bind("departed", departureTime)
            .bind("type", type)
            .updateExactlyOne()
            .let { attendanceId }
    }

fun Database.Transaction.deleteStaffAttendance(attendanceId: StaffAttendanceId) {
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
    attendanceId: StaffAttendanceId,
    departureTime: HelsinkiDateTime
) =
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
        .mapTo<StaffAttendanceExternalId>()
        .one()

data class ExternalStaffDeparture(
    val id: StaffAttendanceExternalId,
    val departed: HelsinkiDateTime
)

fun Database.Transaction.markExternalStaffDeparture(params: ExternalStaffDeparture) =
    createUpdate(
            """
    UPDATE staff_attendance_external 
    SET departed = :departed
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
    occupancyCoefficient: BigDecimal?
): StaffAttendanceExternalId {
    if (attendanceId === null) {
        return createUpdate(
                """
            INSERT INTO staff_attendance_external (name, group_id, arrived, departed, occupancy_coefficient)
            VALUES (:name, :groupId, :arrived, :departed, :occupancyCoefficient)
            RETURNING id
            """
                    .trimIndent()
            )
            .bind("name", name)
            .bind("groupId", groupId)
            .bind("arrived", arrivalTime)
            .bind("departed", departureTime)
            .bind("occupancyCoefficient", occupancyCoefficient)
            .executeAndReturnGeneratedKeys()
            .mapTo<StaffAttendanceExternalId>()
            .single()
    } else {
        return createUpdate(
                """
            UPDATE staff_attendance_external
            SET name = :name, arrived = :arrived, departed = :departed
            WHERE id = :id
            """
                    .trimIndent()
            )
            .bind("id", attendanceId)
            .bind("name", name)
            .bind("arrived", arrivalTime)
            .bind("departed", departureTime)
            .updateExactlyOne()
            .let { attendanceId }
    }
}

fun Database.Transaction.deleteExternalStaffAttendance(attendanceId: StaffAttendanceExternalId) {
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
    val id: UUID,
    val groupId: GroupId,
    val arrived: HelsinkiDateTime,
    val departed: HelsinkiDateTime?,
    val occupancyCoefficient: BigDecimal,
    val currentOccupancyCoefficient: BigDecimal?,
    val type: StaffAttendanceType,
    val employeeId: EmployeeId,
    val firstName: String,
    val lastName: String,
)

fun Database.Read.getStaffAttendancesForDateRange(
    unitId: DaycareId,
    range: FiniteDateRange
): List<RawAttendance> =
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
    soc.coefficient AS currentOccupancyCoefficient
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
        .mapTo<RawAttendance>()
        .list()

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
        .mapTo<RawAttendanceEmployee>()
        .list()

fun Database.Read.getExternalStaffAttendancesByDateRange(
    unitId: DaycareId,
    range: FiniteDateRange
): List<ExternalAttendance> =
    createQuery(
            """
    SELECT sae.id, sae.name, sae.group_id, sae.arrived, sae.departed, sae.occupancy_coefficient
    FROM staff_attendance_external sae
    JOIN daycare_group dg on sae.group_id = dg.id
    WHERE dg.daycare_id = :unitId AND tstzrange(sae.arrived, sae.departed) && tstzrange(:start, :end)
    """
                .trimIndent()
        )
        .bind("unitId", unitId)
        .bind("start", HelsinkiDateTime.of(range.start, LocalTime.of(0, 0)))
        .bind("end", HelsinkiDateTime.of(range.end.plusDays(1), LocalTime.of(0, 0)))
        .mapTo<ExternalAttendance>()
        .list()

private data class EmployeeGroups(val employeeId: EmployeeId, val groupIds: List<GroupId>)

fun Database.Read.getGroupsForEmployees(
    employeeIds: Set<EmployeeId>
): Map<EmployeeId, List<GroupId>> =
    createQuery(
            """
    SELECT employee_id, array_agg(daycare_group_id) AS group_ids
    FROM daycare_group_acl
    WHERE employee_id = ANY(:employeeIds)
    GROUP BY employee_id
    """
                .trimIndent()
        )
        .bind("employeeIds", employeeIds)
        .mapTo<EmployeeGroups>()
        .associateBy({ it.employeeId }, { it.groupIds })

fun Database.Transaction.addMissingStaffAttendanceDepartures(now: HelsinkiDateTime) {
    createUpdate(
            // language=SQL
            """ 
WITH missing_planned_departures AS (
    SELECT realtime.id, plan.end_time AS planned_departure
    FROM staff_attendance_realtime realtime
          JOIN staff_attendance_plan plan ON realtime.employee_id = plan.employee_id
                AND realtime.departed IS NULL
                AND realtime.arrived BETWEEN plan.start_time AND plan.end_time
                AND plan.end_time < :now
)
UPDATE staff_attendance_realtime realtime
SET departed = missing_planned_departures.planned_departure
FROM missing_planned_departures WHERE realtime.id = missing_planned_departures.id
"""
        )
        .bind("now", now)
        .execute()

    val defaultDepartureTime = now.minusDays(1).withTime(LocalTime.of(18, 0))
    createUpdate(
            // language=SQL
            """
UPDATE staff_attendance_realtime a
SET departed = :defaultDepartureTime
FROM daycare_group g
JOIN daycare d ON g.daycare_id = d.id
WHERE a.departed IS NULL AND a.arrived < :startOfDay AND a.group_id = g.id AND NOT d.round_the_clock
        """
                .trimIndent()
        )
        .bind("startOfDay", now.atStartOfDay())
        .bind("defaultDepartureTime", defaultDepartureTime)
        .execute()

    createUpdate(
            // language=SQL
            """
UPDATE staff_attendance_external a
SET departed = :defaultDepartureTime
FROM daycare_group g
JOIN daycare d ON g.daycare_id = d.id
WHERE a.departed IS NULL AND a.arrived < :startOfDay AND a.group_id = g.id AND NOT d.round_the_clock
        """
                .trimIndent()
        )
        .bind("startOfDay", now.atStartOfDay())
        .bind("defaultDepartureTime", defaultDepartureTime)
        .execute()
}

fun Database.Read.getRealtimeStaffAttendances(): List<StaffMemberAttendance> =
    createQuery("SELECT * FROM staff_attendance_realtime").mapTo<StaffMemberAttendance>().list()

fun Database.Read.getPlannedStaffAttendances(
    employeeId: EmployeeId,
    now: HelsinkiDateTime
): List<PlannedStaffAttendance> =
    createQuery(
            """
SELECT start_time AS start, end_time AS end, type FROM staff_attendance_plan
WHERE employee_id = :employeeId AND tstzrange(start_time, end_time) && tstzrange(:start, :end)
"""
        )
        .bind("employeeId", employeeId)
        .bind("start", now.minusHours(8))
        .bind("end", now.plusHours(8))
        .mapTo<PlannedStaffAttendance>()
        .toList()

fun Database.Read.getPlannedStaffAttendanceForDays(
    employeeIds: Collection<EmployeeId>,
    range: FiniteDateRange
): Map<EmployeeId, List<PlannedStaffAttendance>> =
    createQuery(
            """
SELECT start_time AS start, end_time AS end, type, employee_id FROM staff_attendance_plan
WHERE employee_id = ANY(:employeeIds) AND (tstzrange(start_time, end_time) && tstzrange(:startTime, :endTime))
"""
        )
        .bind("employeeIds", employeeIds)
        .bind("startTime", HelsinkiDateTime.of(range.start, LocalTime.MIDNIGHT))
        .bind("endTime", HelsinkiDateTime.of(range.end.plusDays(1), LocalTime.MIDNIGHT))
        .map { row ->
            Pair(row.mapColumn<EmployeeId>("employee_id"), row.mapRow<PlannedStaffAttendance>())
        }
        .groupBy({ it.first }, { it.second })

fun Database.Read.getOngoingAttendance(employeeId: EmployeeId): StaffAttendance? =
    createQuery(
            """
SELECT id, employee_id, group_id, arrived, departed, occupancy_coefficient, type
FROM staff_attendance_realtime WHERE employee_id = :employeeId AND departed IS NULL
"""
        )
        .bind("employeeId", employeeId)
        .mapTo<StaffAttendance>()
        .findOne()
        .orElseGet { null }

fun Database.Transaction.deleteStaffAttendanceWithoutIds(
    employeeId: EmployeeId,
    timeRange: HelsinkiDateTimeRange,
    attendanceIds: List<StaffAttendanceId>
) =
    createUpdate(
            """
DELETE FROM staff_attendance_realtime
WHERE employee_id = :employeeId AND tstzrange(arrived, departed) && :timeRange AND NOT id = ANY(:attendanceIds)
"""
        )
        .bind("employeeId", employeeId)
        .bind("timeRange", timeRange)
        .bind("attendanceIds", attendanceIds)
        .execute()
