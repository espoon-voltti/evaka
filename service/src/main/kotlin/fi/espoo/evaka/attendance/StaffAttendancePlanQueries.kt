// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.domain.FiniteDateRange

fun Database.Read.employeeNumbersQuery(employeeNumbers: Collection<String>): Database.Query {
    return createQuery {
        sql(
            """
            SELECT id, employee_number
            FROM employee
            WHERE employee_number = ANY (${bind(employeeNumbers)})
            """
        )
    }
}

fun Database.Read.findStaffAttendancePlansBy(
    employeeIds: Collection<EmployeeId>? = null,
    period: FiniteDateRange? = null,
): List<StaffAttendancePlan> =
    createQuery {
            val employeeIdFilter: Predicate =
                if (employeeIds == null) Predicate.alwaysTrue()
                else Predicate { where("employee_id = ANY (${bind(employeeIds)})") }
            val daterangeFilter: Predicate =
                if (period == null) Predicate.alwaysTrue()
                else Predicate { where("${bind(period.asHelsinkiDateTimeRange())} @> start_time") }

            sql(
                """
            SELECT employee_id, type, start_time, end_time, description
            FROM staff_attendance_plan
            WHERE ${predicate(employeeIdFilter.forTable("staff_attendance_plan"))}
            AND ${predicate(daterangeFilter.forTable("staff_attendance_plan"))}
        """
            )
        }
        .toList<StaffAttendancePlan>()

fun Database.Transaction.insertStaffAttendancePlans(plans: List<StaffAttendancePlan>): IntArray {
    if (plans.isEmpty()) {
        return IntArray(0)
    }
    return executeBatch(plans) {
        sql(
            """
INSERT INTO staff_attendance_plan (employee_id, type, start_time, end_time, description)
VALUES (${bind { it.employeeId }}, ${bind { it.type }}, ${bind { it.startTime }}, ${bind { it.endTime }}, ${bind { it.description }})
"""
        )
    }
}

fun Database.Transaction.deleteStaffAttendancePlansBy(
    employeeIds: Collection<EmployeeId>? = null,
    period: FiniteDateRange? = null,
): List<StaffAttendancePlan> =
    createQuery {
            val employeeIdFilter: Predicate =
                if (employeeIds == null) Predicate.alwaysTrue()
                else Predicate { where("employee_id = ANY (${bind(employeeIds)})") }
            val daterangeFilter: Predicate =
                if (period == null) Predicate.alwaysTrue()
                else Predicate { where("${bind(period.asHelsinkiDateTimeRange())} @> start_time") }

            sql(
                """
            DELETE FROM staff_attendance_plan
            WHERE ${predicate(employeeIdFilter.forTable("staff_attendance_plan"))}
            AND ${predicate(daterangeFilter.forTable("staff_attendance_plan"))}
            RETURNING employee_id, type, start_time, end_time, description
        """
            )
        }
        .toList<StaffAttendancePlan>()
