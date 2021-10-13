// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.StaffAttendanceExternalId
import fi.espoo.evaka.shared.StaffAttendanceId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.updateExactlyOne
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo

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

fun Database.Read.getExternalStaffAttendance(id: StaffAttendanceId): ExternalStaffMember? = createQuery(
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

fun Database.Transaction.markStaffArrival(employeeId: EmployeeId, groupId: GroupId, arrivalTime: HelsinkiDateTime) = createUpdate(
    """
    INSERT INTO staff_attendance_realtime (employee_id, group_id, arrived, departed) VALUES (
        :employeeId, :groupId, :arrived, NULL
    ) ON CONFLICT DO NOTHING 
    """.trimIndent()
)
    .bind("employeeId", employeeId)
    .bind("groupId", groupId)
    .bind("arrived", arrivalTime)
    .execute()

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

data class ExternalStaffDeparture(
    val id: StaffAttendanceId,
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
