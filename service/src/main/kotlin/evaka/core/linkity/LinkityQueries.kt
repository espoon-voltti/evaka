// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.linkity

import evaka.core.attendance.StaffAttendanceType
import evaka.core.shared.EmployeeId
import evaka.core.shared.StaffAttendanceRealtimeId
import evaka.core.shared.db.Database
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime

data class ExportableAttendance(
    val id: StaffAttendanceRealtimeId,
    val arrived: HelsinkiDateTime,
    val departed: HelsinkiDateTime?,
    val type: StaffAttendanceType,
    val employeeId: EmployeeId,
    val sarastiaId: String,
)

fun Database.Read.getStaffAttendancesForEnabledDaycares(
    range: FiniteDateRange
): List<ExportableAttendance> {
    return createQuery {
            sql(
                """ 
SELECT
    sa.id,
    sa.employee_id,
    sa.arrived,
    sa.departed,
    sa.type,
    emp.employee_number as sarastia_id
FROM staff_attendance_realtime sa
JOIN employee emp ON sa.employee_id = emp.id
WHERE emp.employee_number IS NOT NULL 
    AND ${bind(range.asHelsinkiDateTimeRange())} @> arrived 
    AND departed IS NOT NULL
    AND EXISTS (
        SELECT
        FROM daycare_acl acl
        JOIN daycare d ON acl.daycare_id = d.id
        WHERE acl.employee_id = emp.id AND 'STAFF_ATTENDANCE_INTEGRATION' = ANY(d.enabled_pilot_features)
    )
"""
            )
        }
        .toList()
}

fun Database.Read.getEmployeeIdsForEnabledDaycares(
    employeeNumbers: Collection<String>
): Map<String, EmployeeId> {
    return createQuery {
            sql(
                """
SELECT id, employee_number
FROM employee e
WHERE employee_number = ANY (${bind(employeeNumbers)})
    AND EXISTS (
        SELECT
        FROM daycare_acl acl
        JOIN daycare d ON acl.daycare_id = d.id
        WHERE acl.employee_id = e.id AND 'STAFF_ATTENDANCE_INTEGRATION' = ANY(d.enabled_pilot_features)
    )
            """
            )
        }
        .toMap { columnPair("employee_number", "id") }
}
