// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.auth

import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import org.jdbi.v3.core.mapper.Nested

data class DaycareAclRow(
    @Nested val employee: DaycareAclRowEmployee,
    val role: UserRole,
    val groupIds: List<GroupId>,
    val endDate: LocalDate?,
)

data class DaycareAclRowEmployee(
    val id: EmployeeId,
    val firstName: String,
    val lastName: String,
    val email: String?,
    val employeeNumber: String?,
    val temporary: Boolean,
    val hasStaffOccupancyEffect: Boolean?,
    val active: Boolean,
)

fun Database.Read.getDaycareAclRows(
    daycareId: DaycareId,
    includeStaffOccupancy: Boolean,
    includeStaffEmployeeNumber: Boolean,
    role: UserRole? = null,
): List<DaycareAclRow> =
    createQuery {
            sql(
                """
SELECT e.id,
       e.first_name,
       e.last_name,
       e.email,
       CASE
            WHEN (${bind(includeStaffEmployeeNumber)} IS TRUE) THEN
                e.employee_number
            ELSE NULL END as employee_number,
       role,
       active,
       coalesce(group_ids, array []::uuid[]) AS group_ids,
       temporary_in_unit_id IS NOT NULL      AS temporary,
       CASE
            WHEN (${bind(includeStaffOccupancy)} IS TRUE) THEN
                (soc.coefficient IS NOT NULL and soc.coefficient > 0)
            ELSE NULL END                   as hasStaffOccupancyEffect,
       end_date
FROM daycare_acl
         JOIN employee e on daycare_acl.employee_id = e.id
         LEFT JOIN (SELECT daycare_id, employee_id, array_agg(dg.id) AS group_ids
                    FROM daycare_group_acl acl
                             JOIN daycare_group dg ON acl.daycare_group_id = dg.id
                    GROUP BY daycare_id, employee_id) groups USING (daycare_id, employee_id)
         LEFT JOIN staff_occupancy_coefficient soc USING (daycare_id, employee_id)
WHERE daycare_id = ${bind(daycareId)} ${if (role != null) "AND role = ${bind(role)}" else ""}
    """
            )
        }
        .toList<DaycareAclRow>()

fun Database.Read.hasAnyDaycareAclRow(employeeId: EmployeeId): Boolean =
    createQuery {
            sql(
                """
        SELECT EXISTS(
            SELECT 1 FROM daycare_acl
            WHERE employee_id = ${bind(employeeId)}
        )
    """
            )
        }
        .exactlyOne<Boolean>()

fun Database.Transaction.insertDaycareAclRow(
    daycareId: DaycareId,
    employeeId: EmployeeId,
    role: UserRole,
    endDate: LocalDate? = null,
) = execute {
    sql(
        """
INSERT INTO daycare_acl (daycare_id, employee_id, role, end_date)
VALUES (${bind(daycareId)}, ${bind(employeeId)}, ${bind(role)}, ${bind(endDate)})
ON CONFLICT (daycare_id, employee_id) DO UPDATE SET role = excluded.role, end_date = excluded.end_date
    """
    )
}

fun Database.Transaction.updateAclRowEndDate(
    daycareId: DaycareId,
    employeeId: EmployeeId,
    endDate: LocalDate?,
) = execute {
    sql(
        """
UPDATE daycare_acl
SET end_date = ${bind(endDate)}
WHERE daycare_id = ${bind(daycareId)} AND employee_id = ${bind(employeeId)}
    """
    )
}

fun Database.Transaction.deleteDaycareAclRow(
    daycareId: DaycareId,
    employeeId: EmployeeId,
    role: UserRole,
) = execute {
    sql(
        """
DELETE FROM daycare_acl
WHERE daycare_id = ${bind(daycareId)}
AND employee_id = ${bind(employeeId)}
AND role = ${bind(role)}
    """
    )
}

data class EndedDaycareAclRow(
    val daycareId: DaycareId,
    val employeeId: EmployeeId,
    val role: UserRole,
)

fun Database.Read.getEndedDaycareAclRows(today: LocalDate) =
    createQuery {
            sql(
                """
SELECT daycare_id, employee_id, role
FROM daycare_acl
WHERE end_date IS NOT NULL AND end_date < ${bind(today)}
    """
            )
        }
        .toList<EndedDaycareAclRow>()

fun Database.Transaction.syncDaycareGroupAcl(
    daycareId: DaycareId,
    employeeId: EmployeeId,
    groupIds: Collection<GroupId>,
    now: HelsinkiDateTime = HelsinkiDateTime.now(),
) {
    // Delete rows that are not in the supplied groupIds
    createUpdate {
            sql(
                """
DELETE FROM daycare_group_acl
WHERE employee_id = ${bind(employeeId)}
AND daycare_group_id IN (
    SELECT id FROM daycare_group WHERE daycare_id = ${bind(daycareId)}
)
AND daycare_group_id <> ALL (${bind(groupIds)})
"""
            )
        }
        .execute()

    // Insert rows that are in the supplied groupIds and do not already exist
    executeBatch(groupIds) {
        sql(
            """
INSERT INTO daycare_group_acl (daycare_group_id, employee_id, created, updated)
SELECT id, ${bind(employeeId)}, ${bind(now)}, ${bind(now)}
FROM daycare_group
WHERE id = ${bind { it }} AND daycare_id = ${bind(daycareId)}
ON CONFLICT (daycare_group_id, employee_id) DO NOTHING
"""
        )
    }
}

data class ScheduledDaycareAclRow(
    val id: EmployeeId,
    val firstName: String,
    val lastName: String,
    val email: String?,
    val employeeNumber: String?,
    val role: UserRole,
    val startDate: LocalDate,
    val endDate: LocalDate?,
)

fun Database.Read.getScheduledDaycareAclRows(
    daycareId: DaycareId,
    includeStaffEmployeeNumber: Boolean,
): List<ScheduledDaycareAclRow> =
    createQuery {
            sql(
                """
SELECT e.id,
       e.first_name,
       e.last_name,
       e.email,
       CASE
            WHEN (${bind(includeStaffEmployeeNumber)} IS TRUE) THEN
                e.employee_number
            ELSE NULL END as employee_number,
       das.role,
       das.start_date,
       das.end_date
FROM daycare_acl_schedule das
JOIN employee e on das.employee_id = e.id
WHERE das.daycare_id = ${bind(daycareId)}
    """
            )
        }
        .toList<ScheduledDaycareAclRow>()

fun Database.Transaction.insertScheduledDaycareAclRow(
    employeeId: EmployeeId,
    daycareIds: List<DaycareId>,
    role: UserRole,
    startDate: LocalDate,
    endDate: LocalDate?,
) =
    executeBatch(daycareIds) {
        sql(
            """
INSERT INTO daycare_acl_schedule (daycare_id, employee_id, role, start_date, end_date)
VALUES (${bind { it }}, ${bind(employeeId)}, ${bind(role)}, ${bind(startDate)}, ${bind(endDate)})
ON CONFLICT (daycare_id, employee_id) DO UPDATE 
    SET role = excluded.role, start_date = excluded.start_date, end_date = excluded.end_date
    """
        )
    }

fun Database.Transaction.deleteScheduledDaycareAclRows(employeeId: EmployeeId) = execute {
    sql(
        """
DELETE FROM daycare_acl_schedule
WHERE employee_id = ${bind(employeeId)}
    """
    )
}

fun Database.Transaction.deleteScheduledDaycareAclRow(
    employeeId: EmployeeId,
    daycareId: DaycareId,
) = execute {
    sql(
        """
DELETE FROM daycare_acl_schedule
WHERE daycare_id = ${bind(daycareId)} AND employee_id = ${bind(employeeId)}
    """
    )
}

fun Database.Transaction.upsertAclRowsFromScheduled(today: LocalDate) =
    createQuery {
            sql(
                """
WITH rows_to_activate AS (
    DELETE FROM daycare_acl_schedule
    WHERE start_date <= ${bind(today)}
    RETURNING daycare_id, employee_id, role, end_date
)
INSERT INTO daycare_acl (daycare_id, employee_id, role, end_date)
SELECT daycare_id, employee_id, role, end_date FROM rows_to_activate
ON CONFLICT (daycare_id, employee_id) DO UPDATE SET role = excluded.role, end_date = excluded.end_date
RETURNING employee_id
    """
            )
        }
        .toSet<EmployeeId>()
