// SPDX-FileCopyrightText: 2021-2022 City of Tampere
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.titania

import fi.espoo.evaka.attendance.RawAttendance
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime

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

fun Database.Read.getEmployeeIdsByNumbers(employeeNumbers: List<String>): Map<String, EmployeeId> {
    return employeeNumbersQuery(employeeNumbers).toMap { columnPair("employee_number", "id") }
}

fun Database.Read.getEmployeeIdsByNumbersMapById(
    employeeNumbers: Collection<String>
): Map<EmployeeId, String> {
    return employeeNumbersQuery(employeeNumbers).toMap { columnPair("id", "employee_number") }
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

fun Database.Read.findStaffAttendancesBy(
    employeeIds: Collection<EmployeeId>? = null,
    period: FiniteDateRange? = null,
): List<RawAttendance> =
    createQuery {
            val employeeIdFilter: Predicate =
                if (employeeIds == null) Predicate.alwaysTrue()
                else Predicate { where("$it.employee_id = ANY (${bind(employeeIds)})") }
            val daterangeFilter: Predicate =
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
WHERE ${predicate(employeeIdFilter.forTable("sa"))}
AND ${predicate(daterangeFilter.forTable("sa"))}
        """
            )
        }
        .toList<RawAttendance>()

fun Database.Transaction.insertReportRows(
    requestTime: HelsinkiDateTime,
    rows: List<TitaniaOverLappingShifts>,
) {
    executeBatch(rows) {
        sql(
            """
                INSERT INTO titania_errors (request_time, employee_id, shift_date, shift_begins, shift_ends, overlapping_shift_begins, overlapping_shift_ends)
                VALUES (${bind { requestTime} }, ${bind { it.employeeId} }::uuid, ${bind { it.shiftDate} }, ${bind { it.shiftBegins} }, ${bind { it.shiftEnds} }, ${bind { it.overlappingShiftBegins} }, ${bind { it.overlappingShiftEnds} })
            """
                .trimIndent()
        )
    }
}

fun Database.Transaction.cleanTitaniaErrors(now: HelsinkiDateTime) {
    execute {
        sql(
            """
		DELETE
		FROM titania_errors
		WHERE request_time < ${bind(now)} - interval '30 days'
	    """
        )
    }
}
