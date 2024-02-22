// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.auth

import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.db.Database
import org.jdbi.v3.core.mapper.Nested

data class DaycareAclRow(
    @Nested val employee: DaycareAclRowEmployee,
    val role: UserRole,
    val groupIds: List<GroupId>
)

data class DaycareAclRowEmployee(
    val id: EmployeeId,
    val firstName: String,
    val lastName: String,
    val email: String?,
    val temporary: Boolean,
    val hasStaffOccupancyEffect: Boolean?,
    val active: Boolean
)

fun Database.Read.getDaycareAclRows(
    daycareId: DaycareId,
    includeStaffOccupancy: Boolean
): List<DaycareAclRow> =
    @Suppress("DEPRECATION")
    createQuery(
            // language=SQL
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
            WHEN (:includeStaffOccupancy IS TRUE) THEN
                (soc.coefficient IS NOT NULL and soc.coefficient > 0)
            ELSE NULL END                   as hasStaffOccupancyEffect
FROM daycare_acl
         JOIN employee e on daycare_acl.employee_id = e.id
         LEFT JOIN (SELECT daycare_id, employee_id, array_agg(dg.id) AS group_ids
                    FROM daycare_group_acl acl
                             JOIN daycare_group dg ON acl.daycare_group_id = dg.id
                    GROUP BY daycare_id, employee_id) groups USING (daycare_id, employee_id)
         LEFT JOIN staff_occupancy_coefficient soc USING (daycare_id, employee_id)
WHERE daycare_id = :daycareId
    """
                .trimIndent()
        )
        .bind("daycareId", daycareId)
        .bind("includeStaffOccupancy", includeStaffOccupancy)
        .toList<DaycareAclRow>()

fun Database.Read.hasAnyDaycareAclRow(employeeId: EmployeeId): Boolean =
    @Suppress("DEPRECATION")
    createQuery(
            """
        SELECT EXISTS(
            SELECT 1 FROM daycare_acl
            WHERE employee_id = :employeeId
        )
    """
                .trimIndent()
        )
        .bind("employeeId", employeeId)
        .exactlyOne<Boolean>()

fun Database.Transaction.insertDaycareAclRow(
    daycareId: DaycareId,
    employeeId: EmployeeId,
    role: UserRole
) =
    @Suppress("DEPRECATION")
    createUpdate(
            // language=SQL
            """
INSERT INTO daycare_acl (daycare_id, employee_id, role)
VALUES (:daycareId, :employeeId, :role)
ON CONFLICT (daycare_id, employee_id) DO UPDATE SET role = excluded.role
    """
                .trimIndent()
        )
        .bind("daycareId", daycareId)
        .bind("employeeId", employeeId)
        .bind("role", role)
        .execute()

fun Database.Transaction.deleteDaycareAclRow(
    daycareId: DaycareId,
    employeeId: EmployeeId,
    role: UserRole
) =
    @Suppress("DEPRECATION")
    createUpdate(
            // language=SQL
            """
DELETE FROM daycare_acl
WHERE daycare_id = :daycareId
AND employee_id = :employeeId
AND role = :role
    """
                .trimIndent()
        )
        .bind("daycareId", daycareId)
        .bind("employeeId", employeeId)
        .bind("role", role)
        .execute()

fun Database.Transaction.clearDaycareGroupAcl(daycareId: DaycareId, employeeId: EmployeeId) =
    @Suppress("DEPRECATION")
    createUpdate(
            """
DELETE FROM daycare_group_acl
WHERE employee_id = :employeeId
AND daycare_group_id IN (SELECT id FROM daycare_group WHERE daycare_id = :daycareId)
"""
        )
        .bind("daycareId", daycareId)
        .bind("employeeId", employeeId)
        .execute()

fun Database.Transaction.insertDaycareGroupAcl(
    daycareId: DaycareId,
    employeeId: EmployeeId,
    groupIds: Collection<GroupId>
) =
    prepareBatch(
            """
INSERT INTO daycare_group_acl
SELECT id, :employeeId
FROM daycare_group
WHERE id = :groupId AND daycare_id = :daycareId
"""
        )
        .let { batch ->
            groupIds.forEach { groupId ->
                batch
                    .bind("daycareId", daycareId)
                    .bind("employeeId", employeeId)
                    .bind("groupId", groupId)
                    .add()
            }
            batch.execute()
        }
