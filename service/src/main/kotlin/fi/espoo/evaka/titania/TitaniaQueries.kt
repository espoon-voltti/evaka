// SPDX-FileCopyrightText: 2021-2022 City of Tampere
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.titania

import fi.espoo.evaka.attendance.RawAttendance
import fi.espoo.evaka.pis.NewEmployee
import fi.espoo.evaka.shared.DatabaseTable
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.domain.FiniteDateRange

fun Database.Read.employeeNumbersQuery(employeeNumbers: Collection<String>): Database.Query {
    val sql =
        """
        SELECT id, employee_number
        FROM employee
        WHERE employee_number = ANY (:employeeNumbers)
    """
            .trimIndent()
    return createQuery(sql).bind("employeeNumbers", employeeNumbers)
}

fun Database.Read.getEmployeeIdsByNumbers(employeeNumbers: List<String>): Map<String, EmployeeId> {
    return employeeNumbersQuery(employeeNumbers).toMap { columnPair("employee_number", "id") }
}

fun Database.Transaction.createEmployees(employees: List<NewEmployee>): Map<String, EmployeeId> {
    val sql =
        """
        INSERT INTO employee (first_name, last_name, email, external_id, employee_number, roles, active)
        VALUES (:employee.firstName, :employee.lastName, :employee.email, :employee.externalId, :employee.employeeNumber, :employee.roles::user_role[], true)
        RETURNING id, employee_number
    """
            .trimIndent()
    val batch = prepareBatch(sql)
    employees.forEach { employee -> batch.bindKotlin("employee", employee).add() }
    return batch.executeAndReturn().toMap { columnPair("employee_number", "id") }
}

fun Database.Read.getEmployeeIdsByNumbersMapById(
    employeeNumbers: Collection<String>
): Map<EmployeeId, String> {
    return employeeNumbersQuery(employeeNumbers).toMap { columnPair("id", "employee_number") }
}

fun Database.Read.findStaffAttendancePlansBy(
    employeeIds: Collection<EmployeeId>? = null,
    period: FiniteDateRange? = null
): List<StaffAttendancePlan> =
    createQuery<DatabaseTable> {
            val employeeIdFilter: Predicate<DatabaseTable.StaffAttendancePlan> =
                if (employeeIds == null) Predicate.alwaysTrue()
                else Predicate { where("employee_id = ANY (${bind(employeeIds)})") }
            val daterangeFilter: Predicate<DatabaseTable.StaffAttendancePlan> =
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
    val sql =
        """
        INSERT INTO staff_attendance_plan (employee_id, type, start_time, end_time, description)
        VALUES (:employeeId, :type, :startTime, :endTime, :description)
    """
            .trimIndent()
    val batch = prepareBatch(sql)
    plans.forEach { plan -> batch.bindKotlin(plan).add() }
    return batch.execute()
}

fun Database.Transaction.deleteStaffAttendancePlansBy(
    employeeIds: Collection<EmployeeId>? = null,
    period: FiniteDateRange? = null
): List<StaffAttendancePlan> =
    createQuery<DatabaseTable> {
            val employeeIdFilter: Predicate<DatabaseTable.StaffAttendancePlan> =
                if (employeeIds == null) Predicate.alwaysTrue()
                else Predicate { where("employee_id = ANY (${bind(employeeIds)})") }
            val daterangeFilter: Predicate<DatabaseTable.StaffAttendancePlan> =
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

fun Database.Read.findStaffAttendancesBy(
    employeeIds: Collection<EmployeeId>? = null,
    period: FiniteDateRange? = null
): List<RawAttendance> =
    createQuery<DatabaseTable> {
            val employeeIdFilter: Predicate<DatabaseTable.StaffAttendancePlan> =
                if (employeeIds == null) Predicate.alwaysTrue()
                else Predicate { where("$it.employee_id = ANY (${bind(employeeIds)})") }
            val daterangeFilter: Predicate<DatabaseTable.StaffAttendancePlan> =
                if (period == null) Predicate.alwaysTrue()
                else
                    Predicate {
                        where(
                            "daterange((arrived at time zone 'Europe/Helsinki')::date, (departed at time zone 'Europe/Helsinki')::date, '[]') && daterange(${bind(period.start)}, ${bind(period.end)}, '[]')"
                        )
                    }

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
    sa.departed_automatically
FROM staff_attendance_realtime sa
LEFT JOIN daycare_group dg on sa.group_id = dg.id
JOIN employee emp ON sa.employee_id = emp.id
LEFT JOIN staff_occupancy_coefficient soc ON soc.daycare_id = dg.daycare_id AND soc.employee_id = emp.id
WHERE ${predicate(employeeIdFilter.forTable("sa"))}
AND ${predicate(daterangeFilter.forTable("sa"))}
        """
            )
        }
        .toList<RawAttendance>()
