// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.StaffAttendanceExternalId
import fi.espoo.evaka.shared.StaffAttendanceRealtimeId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.asHelsinkiDateTimeRange
import fi.espoo.evaka.user.EvakaUser
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import kotlin.reflect.full.memberProperties
import org.jdbi.v3.core.mapper.Nested

fun Database.Read.getStaffAttendances(
    unitId: DaycareId,
    dateRange: FiniteDateRange,
    now: HelsinkiDateTime,
    employeeId: EmployeeId? = null,
): List<StaffMember> {
    val range = dateRange.asHelsinkiDateTimeRange()

    return createQuery {
            sql(
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
    att.occupancy_coefficient AS attendance_occupancy_coefficient,
    coalesce(
        (SELECT array_agg(acl.daycare_id) FROM daycare_acl acl WHERE acl.employee_id = e.id), 
        '{}'::uuid[]
    ) AS unit_ids,
    coalesce(dgacl.group_ids, '{}'::uuid[]) AS group_ids,
    EXISTS (
        SELECT 1 
        FROM staff_occupancy_coefficient soc 
        WHERE soc.employee_id = dacl.employee_id AND soc.daycare_id = ${bind(unitId)} AND soc.coefficient > 0
    ) AS occupancy_effect,
    coalesce((
        SELECT jsonb_agg(jsonb_build_object('id', a.id, 'employeeId', a.employee_id, 'groupId', a.group_id, 'arrived', a.arrived, 'departed', a.departed, 'type', a.type, 'departedAutomatically', a.departed_automatically, 'occupancyCoefficient', a.occupancy_coefficient) ORDER BY a.arrived)
        FROM staff_attendance_realtime a
        LEFT JOIN daycare_group dg ON dg.id = a.group_id
        WHERE e.id = a.employee_id AND (dg.daycare_id = ${bind(unitId)} OR a.group_id IS NULL) AND tstzrange(a.arrived, a.departed) && ${bind(range)}
    ), '[]'::jsonb) AS attendances,
    coalesce((
        SELECT jsonb_agg(jsonb_build_object('start', p.start_time, 'end', p.end_time, 'type', p.type, 'description', p.description) ORDER BY p.start_time)
        FROM staff_attendance_plan p
        WHERE e.id = p.employee_id AND tstzrange(p.start_time, p.end_time) && ${bind(range)}
    ), '[]'::jsonb) AS planned_attendances,
    EXISTS(
        SELECT 1 FROM staff_attendance_realtime osar
        WHERE osar.employee_id = dacl.employee_id
          AND ${bind(now)} < osar.arrived
    ) AS has_future_attendances
FROM daycare_acl dacl
JOIN employee e on e.id = dacl.employee_id
LEFT JOIN LATERAL (
    SELECT employee_id, array_agg(dg.id) AS group_ids
    FROM daycare_group dg
    JOIN daycare_group_acl dgacl ON dg.id = dgacl.daycare_group_id
    WHERE dg.daycare_id = ${bind(unitId)} AND dgacl.employee_id = dacl.employee_id
    GROUP BY employee_id
) dgacl ON true
LEFT JOIN LATERAL (
    SELECT sa.id, sa.employee_id, sa.arrived, sa.departed, sa.group_id, sa.type, sa.departed_automatically, sa.occupancy_coefficient
    FROM staff_attendance_realtime sa
    JOIN daycare_group dg ON dg.id = sa.group_id
    WHERE sa.employee_id = dacl.employee_id AND dg.daycare_id = ${bind(unitId)} AND tstzrange(sa.arrived, sa.departed) && ${bind(range)}
    ORDER BY sa.arrived DESC
    LIMIT 1
) att ON TRUE
WHERE dacl.daycare_id = ${bind(unitId)} 
    AND (dacl.role IN ('STAFF', 'SPECIAL_EDUCATION_TEACHER', 'EARLY_CHILDHOOD_EDUCATION_SECRETARY') OR dgacl.employee_id IS NOT NULL)
    ${if (employeeId != null) "AND e.id = ${bind(employeeId)}" else ""}
ORDER BY e.last_name, first_name
"""
            )
        }
        .toList()
}

fun Database.Read.getExternalStaffAttendance(id: StaffAttendanceExternalId): ExternalStaffMember? =
    createQuery {
            sql(
                """
SELECT sae.id, sae.name, sae.group_id, sae.arrived, sae.occupancy_coefficient > 0 AS occupancy_effect
FROM staff_attendance_external sae
WHERE id = ${bind(id)}
"""
            )
        }
        .exactlyOneOrNull()

fun Database.Read.getExternalStaffAttendances(unitId: DaycareId): List<ExternalStaffMember> =
    createQuery {
            sql(
                """
SELECT sae.id, sae.name, sae.group_id, sae.arrived, sae.occupancy_coefficient > 0 AS occupancy_effect
FROM staff_attendance_external sae
JOIN daycare_group dg on sae.group_id = dg.id
WHERE dg.daycare_id = ${bind(unitId)} AND departed IS NULL 
"""
            )
        }
        .toList()

fun Database.Transaction.markStaffArrival(
    employeeId: EmployeeId,
    groupId: GroupId,
    arrivalTime: HelsinkiDateTime,
    occupancyCoefficient: BigDecimal,
    modifiedAt: HelsinkiDateTime,
    modifiedBy: EvakaUserId,
): StaffAttendanceRealtimeId =
    createUpdate {
            sql(
                """
INSERT INTO staff_attendance_realtime (employee_id, group_id, arrived, occupancy_coefficient, arrived_added_at, arrived_added_by, arrived_modified_at, arrived_modified_by) VALUES (
    ${bind(employeeId)}, ${bind(groupId)}, ${bind(arrivalTime)}, ${bind(occupancyCoefficient)}, ${bind(modifiedAt)}, ${bind(modifiedBy)}, ${bind(modifiedAt)}, ${bind(modifiedBy)}
)
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<StaffAttendanceRealtimeId>()

data class StaffAttendance(
    val id: StaffAttendanceRealtimeId?,
    val employeeId: EmployeeId,
    val groupId: GroupId?,
    val arrived: HelsinkiDateTime,
    val departed: HelsinkiDateTime?,
    val occupancyCoefficient: BigDecimal,
    val type: StaffAttendanceType,
)

data class StaffAttendanceRealtimeAudit(
    val id: StaffAttendanceRealtimeId?,
    val employeeId: EmployeeId?,
    val groupId: GroupId?,
    val arrived: HelsinkiDateTime?,
    val departed: HelsinkiDateTime?,
    val occupancyCoefficient: BigDecimal?,
    val type: StaffAttendanceType?,
) {
    companion object {
        val fields =
            StaffAttendanceRealtimeAudit::id to
                StaffAttendanceRealtimeAudit::class
                    .memberProperties
                    .filter { it.name != "id" }
                    .associateBy { it.name }

        fun empty() =
            StaffAttendanceRealtimeAudit(
                id = null,
                employeeId = null,
                groupId = null,
                arrived = null,
                departed = null,
                occupancyCoefficient = null,
                type = null,
            )
    }
}

data class StaffAttendanceRealtimeChange(
    @Nested("old") val old: StaffAttendanceRealtimeAudit = StaffAttendanceRealtimeAudit.empty(),
    @Nested("new") val new: StaffAttendanceRealtimeAudit = StaffAttendanceRealtimeAudit.empty(),
)

fun Database.Transaction.upsertStaffAttendance(
    attendanceId: StaffAttendanceRealtimeId?,
    employeeId: EmployeeId?,
    groupId: GroupId?,
    arrivalTime: HelsinkiDateTime,
    departureTime: HelsinkiDateTime?,
    occupancyCoefficient: BigDecimal?,
    type: StaffAttendanceType,
    departedAutomatically: Boolean = false,
    modifiedAt: HelsinkiDateTime,
    modifiedBy: EvakaUserId,
): StaffAttendanceRealtimeChange? =
    if (attendanceId == null) {
        createUpdate {
                sql(
                    """
INSERT INTO staff_attendance_realtime (employee_id, group_id, arrived, departed, occupancy_coefficient, type, departed_automatically, arrived_added_at, arrived_added_by, arrived_modified_at, arrived_modified_by, departed_added_at, departed_added_by, departed_modified_at, departed_modified_by)
VALUES (
    ${bind(employeeId)},
    ${bind(groupId)},
    ${bind(arrivalTime)},
    ${bind(departureTime)},
    ${bind(occupancyCoefficient)},
    ${bind(type)},
    ${bind(departedAutomatically)},
    ${bind(modifiedAt)},
    ${bind(modifiedBy)},
    ${bind(modifiedAt)},
    ${bind(modifiedBy)},
    ${bind(modifiedAt.takeIf { departureTime != null })},
    ${bind(modifiedBy.takeIf { departureTime != null })},
    ${bind(modifiedAt.takeIf { departureTime != null })},
    ${bind(modifiedBy.takeIf { departureTime != null })}
)
RETURNING id, employee_id, group_id, arrived, departed, occupancy_coefficient, type
"""
                )
            }
            .executeAndReturnGeneratedKeys()
            .exactlyOne<StaffAttendanceRealtimeAudit>()
            .let { StaffAttendanceRealtimeChange(new = it) }
    } else {
        createUpdate {
                sql(
                    """
UPDATE staff_attendance_realtime new
SET group_id = ${bind(groupId)},
    arrived = ${bind(arrivalTime)},
    departed = ${bind(departureTime)},
    occupancy_coefficient = ${bind(occupancyCoefficient)},
    type = ${bind(type)},
    departed_automatically = ${bind(departedAutomatically)},
    arrived_modified_at = CASE WHEN old.arrived != ${bind(arrivalTime)} THEN ${bind(modifiedAt)} ELSE old.arrived_modified_at END,
    arrived_modified_by = CASE WHEN old.arrived != ${bind(arrivalTime)} THEN ${bind(modifiedBy)} ELSE old.arrived_modified_by END,
    departed_added_at = CASE WHEN old.arrived_added_at IS NOT NULL AND old.departed IS DISTINCT FROM ${bind(departureTime)} THEN coalesce(old.departed_added_at, ${bind(modifiedAt)}) ELSE old.departed_added_at END,
    departed_added_by = CASE WHEN old.arrived_added_by IS NOT NULL AND old.departed IS DISTINCT FROM ${bind(departureTime)} THEN coalesce(old.departed_added_by, ${bind(modifiedBy)}) ELSE old.departed_added_by END,
    departed_modified_at = CASE WHEN old.departed IS DISTINCT FROM ${bind(departureTime)} THEN ${bind(modifiedAt)} ELSE old.departed_modified_at END,
    departed_modified_by = CASE WHEN old.departed IS DISTINCT FROM ${bind(departureTime)} THEN ${bind(modifiedBy)} ELSE old.departed_modified_by END
FROM staff_attendance_realtime old
WHERE new.id = ${bind(attendanceId)}
  AND new.id = old.id
  AND (old.group_id IS DISTINCT FROM ${bind(groupId)}
    OR old.arrived != ${bind(arrivalTime)}
    OR old.departed IS DISTINCT FROM ${bind(departureTime)}
    OR old.occupancy_coefficient != ${bind(occupancyCoefficient)}
    OR old.type != ${bind(type)}
    OR old.departed_automatically != ${bind(departedAutomatically)}
  )
RETURNING old.id AS old_id, old.employee_id AS old_employee_id, old.group_id AS old_group_id, old.arrived AS old_arrived, old.departed AS old_departed, old.occupancy_coefficient AS old_occupancy_coefficient, old.type AS old_type,
          new.id AS new_id, new.employee_id AS new_employee_id, new.group_id AS new_group_id, new.arrived AS new_arrived, new.departed AS new_departed, new.occupancy_coefficient AS new_occupancy_coefficient, new.type AS new_type
"""
                )
            }
            .executeAndReturnGeneratedKeys()
            .exactlyOneOrNull<StaffAttendanceRealtimeChange>()
    }

fun Database.Transaction.deleteStaffAttendance(
    attendanceId: StaffAttendanceRealtimeId
): StaffAttendanceRealtimeAudit {
    return createUpdate {
            sql(
                """
DELETE FROM staff_attendance_realtime
WHERE id = ${bind(attendanceId)}
RETURNING id, employee_id, group_id, arrived, departed, occupancy_coefficient, type
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<StaffAttendanceRealtimeAudit>()
}

fun Database.Transaction.markStaffDeparture(
    attendanceId: StaffAttendanceRealtimeId,
    departureTime: HelsinkiDateTime,
    modifiedAt: HelsinkiDateTime,
    modifiedBy: EvakaUserId,
) =
    createUpdate {
            sql(
                """
UPDATE staff_attendance_realtime 
SET departed = ${bind(departureTime)},
    departed_modified_at = ${bind(modifiedAt)},
    departed_modified_by = ${bind(modifiedBy)}
WHERE id = ${bind(attendanceId)} AND departed IS NULL AND arrived < ${bind(departureTime)}
"""
            )
        }
        .updateExactlyOne()

data class ExternalStaffArrival(
    val name: String,
    val groupId: GroupId,
    val arrived: HelsinkiDateTime,
    val occupancyCoefficient: BigDecimal,
)

fun Database.Transaction.markExternalStaffArrival(
    params: ExternalStaffArrival
): StaffAttendanceExternalId =
    createUpdate {
            sql(
                """
INSERT INTO staff_attendance_external (name, group_id, arrived, occupancy_coefficient) VALUES (
    ${bind(params.name)}, ${bind(params.groupId)}, ${bind(params.arrived)}, ${bind(params.occupancyCoefficient)}
) RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<StaffAttendanceExternalId>()

data class ExternalStaffDeparture(
    val id: StaffAttendanceExternalId,
    val departed: HelsinkiDateTime,
)

fun Database.Transaction.markExternalStaffDeparture(params: ExternalStaffDeparture) =
    createUpdate {
            sql(
                """
UPDATE staff_attendance_external 
SET departed = ${bind(params.departed)}, departed_automatically = false
WHERE id = ${bind(params.id)} AND departed IS NULL AND arrived < ${bind(params.departed)}
"""
            )
        }
        .updateExactlyOne()

fun Database.Transaction.upsertExternalStaffAttendance(
    attendanceId: StaffAttendanceExternalId?,
    name: String?,
    groupId: GroupId?,
    arrivalTime: HelsinkiDateTime,
    departureTime: HelsinkiDateTime?,
    occupancyCoefficient: BigDecimal,
    departedAutomatically: Boolean = false,
): StaffAttendanceExternalId {
    if (attendanceId == null) {
        return createUpdate {
                sql(
                    """
INSERT INTO staff_attendance_external (name, group_id, arrived, departed, occupancy_coefficient, departed_automatically)
VALUES (${bind(name)}, ${bind(groupId)}, ${bind(arrivalTime)}, ${bind(departureTime)}, ${bind(occupancyCoefficient)}, ${bind(departedAutomatically)})
RETURNING id
"""
                )
            }
            .executeAndReturnGeneratedKeys()
            .exactlyOne<StaffAttendanceExternalId>()
    } else {
        return createUpdate {
                sql(
                    """
UPDATE staff_attendance_external
SET name = ${bind(name)}, arrived = ${bind(arrivalTime)}, departed = ${bind(departureTime)}, 
    occupancy_coefficient = ${bind(occupancyCoefficient)}, departed_automatically = ${bind(departedAutomatically)}
WHERE id = ${bind(attendanceId)}
"""
                )
            }
            .updateExactlyOne()
            .let { attendanceId }
    }
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
    val departedAutomatically: Boolean,
    val arrivedAddedAt: HelsinkiDateTime?,
    @Nested("arrived_added_by") val arrivedAddedBy: EvakaUser?,
    val arrivedModifiedAt: HelsinkiDateTime?,
    @Nested("arrived_modified_by") val arrivedModifiedBy: EvakaUser?,
    val departedAddedAt: HelsinkiDateTime?,
    @Nested("departed_added_by") val departedAddedBy: EvakaUser?,
    val departedModifiedAt: HelsinkiDateTime?,
    @Nested("departed_modified_by") val departedModifiedBy: EvakaUser?,
) {
    fun isPrevious(other: RawAttendance) =
        if (this.departed == null) false
        else
            this.departed.plusMinutes(1).durationSince(other.arrived).abs() <= Duration.ofMinutes(1)

    fun isNext(other: RawAttendance) =
        if (other.departed == null) false
        else
            other.departed.plusMinutes(1).durationSince(this.arrived).abs() <= Duration.ofMinutes(1)
}

fun Database.Read.getStaffAttendancesForDateRange(
    unitId: DaycareId,
    range: FiniteDateRange,
): List<RawAttendance> {
    val start = HelsinkiDateTime.of(range.start, LocalTime.of(0, 0))
    val end = HelsinkiDateTime.of(range.end.plusDays(1), LocalTime.of(0, 0))
    return createQuery {
            sql(
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
    sa.departed_automatically,
    sa.arrived_added_at,
    arrived_added_by.id AS arrived_added_by_id,
    arrived_added_by.name AS arrived_added_by_name,
    arrived_added_by.type AS arrived_added_by_type,
    sa.arrived_modified_at,
    arrived_modified_by.id AS arrived_modified_by_id,
    arrived_modified_by.name AS arrived_modified_by_name,
    arrived_modified_by.type AS arrived_modified_by_type,
    sa.departed_added_at,
    departed_added_by.id AS departed_added_by_id,
    departed_added_by.name AS departed_added_by_name,
    departed_added_by.type AS departed_added_by_type,
    sa.departed_modified_at,
    departed_modified_by.id AS departed_modified_by_id,
    departed_modified_by.name AS departed_modified_by_name,
    departed_modified_by.type AS departed_modified_by_type
FROM staff_attendance_realtime sa
JOIN daycare_group dg on sa.group_id = dg.id
JOIN employee emp ON sa.employee_id = emp.id
LEFT JOIN staff_occupancy_coefficient soc ON soc.daycare_id = dg.daycare_id AND soc.employee_id = emp.id
LEFT JOIN evaka_user arrived_added_by ON arrived_added_by.id = sa.arrived_added_by
LEFT JOIN evaka_user arrived_modified_by ON arrived_modified_by.id = sa.arrived_modified_by
LEFT JOIN evaka_user departed_added_by ON departed_added_by.id = sa.departed_added_by
LEFT JOIN evaka_user departed_modified_by ON departed_modified_by.id = sa.departed_modified_by
WHERE dg.daycare_id = ${bind(unitId)} AND tstzrange(sa.arrived, sa.departed) && tstzrange(${bind(start)}, ${bind(end)})
"""
            )
        }
        .toList()
}

fun Database.Read.getEmployeeAttendancesByArrivalDateDate(
    unitId: DaycareId,
    employeeId: EmployeeId,
    arrivalDate: LocalDate,
): List<RawAttendance> {
    return createQuery {
            sql(
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
    sa.departed_automatically,
    sa.arrived_added_at,
    arrived_added_by.id AS arrived_added_by_id,
    arrived_added_by.name AS arrived_added_by_name,
    arrived_added_by.type AS arrived_added_by_type,
    sa.arrived_modified_at,
    arrived_modified_by.id AS arrived_modified_by_id,
    arrived_modified_by.name AS arrived_modified_by_name,
    arrived_modified_by.type AS arrived_modified_by_type,
    sa.departed_added_at,
    departed_added_by.id AS departed_added_by_id,
    departed_added_by.name AS departed_added_by_name,
    departed_added_by.type AS departed_added_by_type,
    sa.departed_modified_at,
    departed_modified_by.id AS departed_modified_by_id,
    departed_modified_by.name AS departed_modified_by_name,
    departed_modified_by.type AS departed_modified_by_type
FROM staff_attendance_realtime sa
LEFT JOIN daycare_group dg on sa.group_id = dg.id
JOIN employee emp ON sa.employee_id = emp.id
LEFT JOIN staff_occupancy_coefficient soc ON soc.daycare_id = dg.daycare_id AND soc.employee_id = emp.id
LEFT JOIN evaka_user arrived_added_by ON arrived_added_by.id = sa.arrived_added_by
LEFT JOIN evaka_user arrived_modified_by ON arrived_modified_by.id = sa.arrived_modified_by
LEFT JOIN evaka_user departed_added_by ON departed_added_by.id = sa.departed_added_by
LEFT JOIN evaka_user departed_modified_by ON departed_modified_by.id = sa.departed_modified_by
WHERE (dg.daycare_id IS NULL OR dg.daycare_id = ${bind(unitId)}) 
    AND emp.id = ${bind(employeeId)} 
    AND between_start_and_end(${bind(arrivalDate.asHelsinkiDateTimeRange())}, sa.arrived)
"""
            )
        }
        .toList()
}

fun Database.Read.getStaffAttendancesWithoutGroup(
    range: FiniteDateRange,
    employeeIds: Set<EmployeeId>,
): List<RawAttendance> {
    val start = HelsinkiDateTime.of(range.start, LocalTime.of(0, 0))
    val end = HelsinkiDateTime.of(range.end.plusDays(1), LocalTime.of(0, 0))
    return createQuery {
            sql(
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
    sa.departed_automatically,
    sa.arrived_added_at,
    arrived_added_by.id AS arrived_added_by_id,
    arrived_added_by.name AS arrived_added_by_name,
    arrived_added_by.type AS arrived_added_by_type,
    sa.arrived_modified_at,
    arrived_modified_by.id AS arrived_modified_by_id,
    arrived_modified_by.name AS arrived_modified_by_name,
    arrived_modified_by.type AS arrived_modified_by_type,
    sa.departed_added_at,
    departed_added_by.id AS departed_added_by_id,
    departed_added_by.name AS departed_added_by_name,
    departed_added_by.type AS departed_added_by_type,
    sa.departed_modified_at,
    departed_modified_by.id AS departed_modified_by_id,
    departed_modified_by.name AS departed_modified_by_name,
    departed_modified_by.type AS departed_modified_by_type
FROM staff_attendance_realtime sa
JOIN employee emp ON sa.employee_id = emp.id
LEFT JOIN evaka_user arrived_added_by ON arrived_added_by.id = sa.arrived_added_by
LEFT JOIN evaka_user arrived_modified_by ON arrived_modified_by.id = sa.arrived_modified_by
LEFT JOIN evaka_user departed_added_by ON departed_added_by.id = sa.departed_added_by
LEFT JOIN evaka_user departed_modified_by ON departed_modified_by.id = sa.departed_modified_by
WHERE sa.employee_id = ANY(${bind(employeeIds)}) AND sa.group_id IS NULL AND tstzrange(sa.arrived, sa.departed) && tstzrange(${bind(start)}, ${bind(end)})
"""
            )
        }
        .toList()
}

data class RawAttendanceEmployee(
    val id: EmployeeId,
    val firstName: String,
    val lastName: String,
    val currentOccupancyCoefficient: BigDecimal?,
)

fun Database.Read.getCurrentStaffForAttendanceCalendar(
    unitId: DaycareId
): List<RawAttendanceEmployee> =
    createQuery {
            sql(
                """
SELECT DISTINCT dacl.employee_id as id, emp.first_name, emp.last_name, soc.coefficient AS currentOccupancyCoefficient
FROM daycare_acl dacl
JOIN employee emp on emp.id = dacl.employee_id
LEFT JOIN daycare_group_acl dgacl ON dgacl.employee_id = emp.id
LEFT JOIN staff_occupancy_coefficient soc ON soc.daycare_id = dacl.daycare_id AND soc.employee_id = emp.id
WHERE dacl.daycare_id = ${bind(unitId)} AND (dacl.role IN ('STAFF', 'SPECIAL_EDUCATION_TEACHER') OR dgacl.employee_id IS NOT NULL)
    """
            )
        }
        .toList()

fun Database.Read.getExternalStaffAttendancesByDateRange(
    unitId: DaycareId,
    range: FiniteDateRange,
): List<ExternalAttendance> {
    val start = HelsinkiDateTime.of(range.start, LocalTime.of(0, 0))
    val end = HelsinkiDateTime.of(range.end.plusDays(1), LocalTime.of(0, 0))
    return createQuery {
            sql(
                """
SELECT sae.id, sae.name, sae.group_id, sae.arrived, sae.departed, sae.occupancy_coefficient, sae.departed_automatically
FROM staff_attendance_external sae
JOIN daycare_group dg on sae.group_id = dg.id
WHERE dg.daycare_id = ${bind(unitId)} AND tstzrange(sae.arrived, sae.departed) && tstzrange(${bind(start)}, ${bind(end)})
"""
            )
        }
        .toList()
}

fun Database.Read.getGroupsForEmployees(
    unitId: DaycareId,
    employeeIds: Set<EmployeeId>,
): Map<EmployeeId, List<GroupId>> =
    createQuery {
            sql(
                """
SELECT employee_id, array_agg(daycare_group_id) AS group_ids
FROM daycare_group_acl
WHERE
    daycare_group_id = ANY (SELECT id FROM daycare_group WHERE daycare_id = ${bind(unitId)}) AND
    employee_id = ANY(${bind(employeeIds)})
GROUP BY employee_id
"""
            )
        }
        .toMap { columnPair("employee_id", "group_ids") }

fun Database.Transaction.addMissingStaffAttendanceDepartures(
    now: HelsinkiDateTime,
    modifiedBy: EvakaUserId,
) {
    execute {
        sql(
            """ 
WITH missing_planned_departures AS (
    SELECT realtime.id, plan.end_time AS planned_departure
    FROM staff_attendance_realtime realtime
          JOIN staff_attendance_plan plan ON realtime.employee_id = plan.employee_id
                AND realtime.departed IS NULL
                AND realtime.type = ANY (${bind(StaffAttendanceType.AUTOMATICALLY_DEPART_TYPES)})
                AND tstzrange(plan.start_time, plan.end_time, '[)') @> realtime.arrived
                AND plan.end_time < ${bind(now)}
)
UPDATE staff_attendance_realtime realtime
SET departed = missing_planned_departures.planned_departure,
    departed_automatically = true,
    departed_modified_at = ${bind(now)},
    departed_modified_by = ${bind(modifiedBy)}
FROM missing_planned_departures WHERE realtime.id = missing_planned_departures.id
"""
        )
    }

    execute {
        sql(
            """
UPDATE staff_attendance_realtime a
SET departed = a.arrived + interval '12 hours',
    departed_automatically = true,
    departed_modified_at = ${bind(now)},
    departed_modified_by = ${bind(modifiedBy)}
WHERE a.departed IS NULL AND a.arrived + INTERVAL '12 hours' <= ${bind(now)}
"""
        )
    }

    execute {
        sql(
            """
UPDATE staff_attendance_external a
SET departed = a.arrived + interval '12 hours',
    departed_automatically = true
WHERE a.departed IS NULL AND a.arrived + INTERVAL '12 hours' <= ${bind(now)}
"""
        )
    }
}

fun Database.Read.getRealtimeStaffAttendances(): List<StaffMemberAttendance> =
    createQuery { sql("SELECT * FROM staff_attendance_realtime") }.toList()

fun Database.Read.getPlannedStaffAttendances(
    employeeId: EmployeeId,
    now: HelsinkiDateTime,
): List<PlannedStaffAttendance> {
    val start = now.minusHours(8)
    val end = now.plusHours(8)
    return createQuery {
            sql(
                """
SELECT start_time AS start, end_time AS end, type, description FROM staff_attendance_plan
WHERE employee_id = ${bind(employeeId)} AND tstzrange(start_time, end_time) && tstzrange(${bind(start)}, ${bind(end)})
"""
            )
        }
        .toList()
}

fun Database.Read.getPlannedStaffAttendanceForDays(
    employeeIds: Collection<EmployeeId>,
    range: FiniteDateRange,
): Map<EmployeeId, List<PlannedStaffAttendance>> {
    val startTime = HelsinkiDateTime.of(range.start, LocalTime.MIDNIGHT)
    val endTime = HelsinkiDateTime.of(range.end.plusDays(1), LocalTime.MIDNIGHT)
    return createQuery {
            sql(
                """
SELECT start_time AS start, end_time AS end, type, employee_id, description FROM staff_attendance_plan
WHERE employee_id = ANY(${bind(employeeIds)}) AND (tstzrange(start_time, end_time) && tstzrange(${bind(startTime)}, ${bind(endTime)}))
"""
            )
        }
        .toList { column<EmployeeId>("employee_id") to row<PlannedStaffAttendance>() }
        .groupBy({ it.first }, { it.second })
}

fun Database.Read.getOngoingAttendance(employeeId: EmployeeId): StaffAttendance? =
    createQuery {
            sql(
                """
SELECT id, employee_id, group_id, arrived, departed, occupancy_coefficient, type
FROM staff_attendance_realtime WHERE employee_id = ${bind(employeeId)} AND departed IS NULL
"""
            )
        }
        .exactlyOneOrNull()

fun Database.Read.getLatestDepartureToday(
    employeeId: EmployeeId,
    now: HelsinkiDateTime,
): StaffAttendance? =
    createQuery {
            sql(
                """
SELECT id, employee_id, group_id, arrived, departed, occupancy_coefficient, type
FROM staff_attendance_realtime 
WHERE employee_id = ${bind(employeeId)} 
    AND ${bind(now.atStartOfDay())} <= departed AND departed < ${bind(now.plusDays(1).atStartOfDay())}
ORDER BY departed DESC LIMIT 1   
"""
            )
        }
        .exactlyOneOrNull<StaffAttendance>()

fun Database.Transaction.deleteStaffAttendancesOnDateExcept(
    unitId: DaycareId,
    employeeId: EmployeeId,
    arrivalDate: LocalDate,
    exceptIds: List<StaffAttendanceRealtimeId>,
) =
    createUpdate {
            sql(
                """
DELETE FROM staff_attendance_realtime
WHERE
    (group_id IS NULL OR group_id = ANY (SELECT id FROM daycare_group WHERE daycare_id = ${bind(unitId)})) AND
    employee_id = ${bind(employeeId)} AND
    between_start_and_end(${bind(arrivalDate.asHelsinkiDateTimeRange())}, arrived) AND
    NOT id = ANY(${bind(exceptIds)})
RETURNING id, employee_id, group_id, arrived, departed, occupancy_coefficient, type
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .toList<StaffAttendanceRealtimeAudit>()

fun Database.Transaction.deleteExternalAttendancesOnDateExcept(
    name: String,
    arrivalDate: LocalDate,
    exceptIds: List<StaffAttendanceExternalId>,
) = execute {
    sql(
        """
DELETE FROM staff_attendance_external
WHERE 
    name = ${bind(name)} AND 
    between_start_and_end(${bind(arrivalDate.asHelsinkiDateTimeRange())}, arrived) AND 
    NOT id = ANY(${bind(exceptIds)})
"""
    )
}

data class OpenGroupAttendance(
    val unitName: String,
    val unitId: DaycareId,
    val date: LocalDate,
    val groupId: GroupId,
)

fun Database.Read.getOpenGroupAttendancesForEmployee(employeeId: EmployeeId): OpenGroupAttendance? =
    createQuery {
            sql(
                """
                SELECT d.name as unit_name, dg.daycare_id as unit_id, sa.arrived::date AS date, sa.group_id
                FROM staff_attendance_realtime sa JOIN daycare_group dg ON sa.group_id = dg.id JOIN daycare d ON dg.daycare_id = d.id
                WHERE sa.employee_id = ${bind(employeeId)} AND sa.departed IS NULL
                """
            )
        }
        .exactlyOneOrNull<OpenGroupAttendance>()
