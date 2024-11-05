// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.auth

import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.jdbi.v3.core.mapper.Nested

data class DaycareAclRow(
    @Nested val employee: DaycareAclRowEmployee,
    val role: UserRole,
    val groupIds: List<GroupId>,
)

data class DaycareAclRowEmployee(
    val id: EmployeeId,
    val firstName: String,
    val lastName: String,
    val email: String?,
    val temporary: Boolean,
    val hasStaffOccupancyEffect: Boolean?,
    val active: Boolean,
)

fun Database.Read.getDaycareAclRows(
    daycareId: DaycareId,
    includeStaffOccupancy: Boolean,
    role: UserRole? = null,
): List<DaycareAclRow> =
    createQuery {
            sql(
                """
SELECT e.id,
       e.first_name,
       e.last_name,
       e.email,
       role,
       active,
       coalesce(group_ids, array []::uuid[]) AS group_ids,
       temporary_in_unit_id IS NOT NULL      AS temporary,
       CASE
            WHEN (${bind(includeStaffOccupancy)} IS TRUE) THEN
                (soc.coefficient IS NOT NULL and soc.coefficient > 0)
            ELSE NULL END                   as hasStaffOccupancyEffect
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
) =
    createUpdate {
            sql(
                """
INSERT INTO daycare_acl (daycare_id, employee_id, role)
VALUES (${bind(daycareId)}, ${bind(employeeId)}, ${bind(role)})
ON CONFLICT (daycare_id, employee_id) DO UPDATE SET role = excluded.role
    """
            )
        }
        .execute()

fun Database.Transaction.deleteDaycareAclRow(
    daycareId: DaycareId,
    employeeId: EmployeeId,
    role: UserRole,
) =
    createUpdate {
            sql(
                """
DELETE FROM daycare_acl
WHERE daycare_id = ${bind(daycareId)}
AND employee_id = ${bind(employeeId)}
AND role = ${bind(role)}
    """
            )
        }
        .execute()

fun Database.Transaction.clearDaycareGroupAcl(daycareId: DaycareId, employeeId: EmployeeId) =
    createUpdate {
            sql(
                """
DELETE FROM daycare_group_acl
WHERE employee_id = ${bind(employeeId)}
AND daycare_group_id IN (SELECT id FROM daycare_group WHERE daycare_id = ${bind(daycareId)})
"""
            )
        }
        .execute()

fun Database.Transaction.insertDaycareGroupAcl(
    daycareId: DaycareId,
    employeeId: EmployeeId,
    groupIds: Collection<GroupId>,
    now: HelsinkiDateTime,
) =
    executeBatch(groupIds) {
        sql(
            """
INSERT INTO daycare_group_acl
SELECT id, ${bind(employeeId)}, ${bind(now)}, ${bind(now)}
FROM daycare_group
WHERE id = ${bind { it }} AND daycare_id = ${bind(daycareId)}
"""
        )
    }
