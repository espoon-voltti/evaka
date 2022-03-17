// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.ExcludeCodeGen
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.StaffAttendanceExternalId
import fi.espoo.evaka.shared.StaffAttendanceId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.updateExactlyOne
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo
import java.util.UUID

fun Database.Read.getStaffAttendances(unitId: DaycareId, now: HelsinkiDateTime): List<StaffMember> = createQuery(
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
        (
            SELECT coalesce(array_agg(id), '{}'::UUID[])
            FROM daycare_group dg
            JOIN daycare_group_acl dgacl ON dg.id = dgacl.daycare_group_id
            WHERE dg.daycare_id = :unitId AND dgacl.employee_id = dacl.employee_id
        ) AS group_ids
    FROM daycare_acl dacl
    JOIN employee e on e.id = dacl.employee_id
    LEFT JOIN LATERAL (
        SELECT sa.id, sa.employee_id, sa.arrived, sa.departed, sa.group_id
        FROM staff_attendance_realtime sa
        WHERE sa.employee_id = dacl.employee_id AND tstzrange(sa.arrived, sa.departed) && tstzrange(:rangeStart, :rangeEnd)
        ORDER BY sa.arrived DESC 
        LIMIT 1
    ) att ON TRUE
    WHERE dacl.daycare_id = :unitId AND dacl.role IN ('STAFF', 'SPECIAL_EDUCATION_TEACHER')
    ORDER BY e.last_name, e.first_name
    """.trimIndent()
)
    .bind("unitId", unitId)
    .bind("rangeStart", now.atStartOfDay())
    .bind("rangeEnd", now.atEndOfDay())
    .mapTo<StaffMember>()
    .list()

fun Database.Read.getStaffAttendance(id: StaffAttendanceId): StaffMemberAttendance? = createQuery(
    """
    SELECT id, employee_id, group_id, arrived, departed
    FROM staff_attendance_realtime
    WHERE id = :id
"""
).bind("id", id).mapTo<StaffMemberAttendance>().firstOrNull()

fun Database.Read.getExternalStaffAttendance(id: StaffAttendanceExternalId): ExternalStaffMember? = createQuery(
    """
    SELECT id, name, group_id, arrived
    FROM staff_attendance_external
    WHERE id = :id
"""
).bind("id", id).mapTo<ExternalStaffMember>().firstOrNull()

fun Database.Read.getExternalStaffAttendances(unitId: DaycareId): List<ExternalStaffMember> = createQuery(
    """
    SELECT sae.id, sae.name, sae.group_id, sae.arrived
    FROM staff_attendance_external sae
    JOIN daycare_group dg on sae.group_id = dg.id
    WHERE dg.daycare_id = :unitId AND departed IS NULL 
    """.trimIndent()
)
    .bind("unitId", unitId)
    .mapTo<ExternalStaffMember>()
    .list()

fun Database.Transaction.markStaffArrival(employeeId: EmployeeId, groupId: GroupId, arrivalTime: HelsinkiDateTime): StaffAttendanceId = createUpdate(
    """
    INSERT INTO staff_attendance_realtime (employee_id, group_id, arrived) VALUES (
        :employeeId, :groupId, :arrived
    )
    RETURNING id
    """.trimIndent()
)
    .bind("employeeId", employeeId)
    .bind("groupId", groupId)
    .bind("arrived", arrivalTime)
    .executeAndReturnGeneratedKeys()
    .mapTo<StaffAttendanceId>()
    .one()

fun Database.Transaction.markStaffDeparture(attendanceId: StaffAttendanceId, departureTime: HelsinkiDateTime) = createUpdate(
    """
    UPDATE staff_attendance_realtime 
    SET departed = :departed
    WHERE id = :id AND departed IS NULL AND arrived < :departed
    """.trimIndent()
)
    .bind("id", attendanceId)
    .bind("departed", departureTime)
    .updateExactlyOne()

@ExcludeCodeGen
data class ExternalStaffArrival(
    val name: String,
    val groupId: GroupId,
    val arrived: HelsinkiDateTime
)
fun Database.Transaction.markExternalStaffArrival(params: ExternalStaffArrival): StaffAttendanceExternalId = createUpdate(
    """
    INSERT INTO staff_attendance_external (name, group_id, arrived) VALUES (
        :name, :groupId, :arrived
    ) RETURNING id
    """.trimIndent()
)
    .bindKotlin(params)
    .executeAndReturnGeneratedKeys()
    .mapTo<StaffAttendanceExternalId>()
    .one()

@ExcludeCodeGen
data class ExternalStaffDeparture(
    val id: StaffAttendanceExternalId,
    val departed: HelsinkiDateTime
)
fun Database.Transaction.markExternalStaffDeparture(params: ExternalStaffDeparture) = createUpdate(
    """
    UPDATE staff_attendance_external 
    SET departed = :departed
    WHERE id = :id AND departed IS NULL AND arrived < :departed
    """.trimIndent()
)
    .bindKotlin(params)
    .updateExactlyOne()

@ExcludeCodeGen
data class RawAttendance(
    val id: UUID,
    val groupId: GroupId,
    val arrived: HelsinkiDateTime,
    val departed: HelsinkiDateTime?,
    val employeeId: EmployeeId,
    val firstName: String,
    val lastName: String,
)

fun Database.Read.getStaffAttendancesForDateRange(unitId: DaycareId, range: FiniteDateRange): List<RawAttendance> =
    createQuery(
        """ 
SELECT sa.id, sa.employee_id, sa.arrived, sa.departed, sa.group_id, emp.first_name, emp.last_name 
FROM staff_attendance_realtime sa
JOIN employee emp ON sa.employee_id = emp.id
WHERE tstzrange(sa.arrived, sa.departed) && tstzrange(:start, :end)
        """.trimIndent()
    )
        .bind("unitId", unitId)
        .bind("start", range.start)
        .bind("end", range.end)
        .mapTo<RawAttendance>()
        .list()

@ExcludeCodeGen
data class RawEmployee(
    val id: EmployeeId,
    val firstName: String,
    val lastName: String,
)

fun Database.Read.getCurrentStaffForAttendanceCalendar(unitId: DaycareId): List<RawEmployee> = createQuery(
    """
SELECT DISTINCT dacl.employee_id as id, e.first_name, e.last_name
FROM daycare_acl dacl
JOIN employee e on e.id = dacl.employee_id
WHERE dacl.daycare_id = :unitId AND dacl.role IN ('STAFF', 'SPECIAL_EDUCATION_TEACHER')
    """.trimIndent()
)
    .bind("unitId", unitId)
    .mapTo<RawEmployee>()
    .list()

fun Database.Read.getExternalStaffAttendancesByDateRange(unitId: DaycareId, range: FiniteDateRange): List<ExternalAttendance> = createQuery(
    """
    SELECT sae.id, sae.name, sae.group_id, sae.arrived, sae.departed
    FROM staff_attendance_external sae
    JOIN daycare_group dg on sae.group_id = dg.id
    WHERE dg.daycare_id = :unitId AND tstzrange(sae.arrived, sae.departed) && tstzrange(:start, :end)
    """.trimIndent()
)
    .bind("unitId", unitId)
    .bind("start", range.start)
    .bind("end", range.end)
    .mapTo<ExternalAttendance>()
    .list()
