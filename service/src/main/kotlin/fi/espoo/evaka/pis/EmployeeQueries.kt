// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

import fi.espoo.evaka.identity.ExternalId
import fi.espoo.evaka.pis.controllers.PinCode
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Binding
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.db.freeTextSearchQueryForColumns
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.mapToPaged
import org.jdbi.v3.json.Json

data class NewEmployee(
    val firstName: String,
    val lastName: String,
    val email: String?,
    val externalId: ExternalId?,
    val employeeNumber: String?,
    val roles: Set<UserRole> = setOf(),
    val temporaryInUnitId: DaycareId?,
    val active: Boolean
)

data class EmployeeUser(
    val id: EmployeeId,
    val preferredFirstName: String? = null,
    val firstName: String,
    val lastName: String,
    val globalRoles: Set<UserRole> = setOf(),
    val allScopedRoles: Set<UserRole> = setOf(),
    val active: Boolean
)

data class EmployeeRoles(
    val globalRoles: Set<UserRole> = setOf(),
    val allScopedRoles: Set<UserRole> = setOf()
)

data class DaycareRole(val daycareId: DaycareId, val daycareName: String, val role: UserRole)

data class DaycareGroupRole(
    val daycareId: DaycareId,
    val daycareName: String,
    val groupId: GroupId,
    val groupName: String
)

data class EmployeeWithDaycareRoles(
    val id: EmployeeId,
    val externalId: String?,
    val employeeNumber: String?,
    val created: HelsinkiDateTime,
    val updated: HelsinkiDateTime?,
    val lastLogin: HelsinkiDateTime?,
    val firstName: String,
    val lastName: String,
    val email: String?,
    val globalRoles: List<UserRole> = listOf(),
    @Json val daycareRoles: List<DaycareRole> = listOf(),
    @Json val daycareGroupRoles: List<DaycareGroupRole> = listOf(),
    val temporaryUnitName: String?,
    val active: Boolean
)

data class EmployeeIdWithName(val id: EmployeeId, val name: String)

fun Database.Transaction.createEmployee(employee: NewEmployee): Employee =
    createUpdate {
            sql(
                """
INSERT INTO employee (first_name, last_name, email, external_id, employee_number, roles, temporary_in_unit_id, active)
VALUES (${bind(employee.firstName)}, ${bind(employee.lastName)}, ${bind(employee.email)}, ${bind(employee.externalId)}, ${bind(employee.employeeNumber)}, ${bind(employee.roles)}::user_role[], ${bind(employee.temporaryInUnitId)}, ${bind(employee.active)})
RETURNING id, first_name, last_name, email, external_id, created, updated, roles, temporary_in_unit_id, active
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<Employee>()

fun Database.Transaction.updateExternalIdByEmployeeNumber(
    employeeNumber: String,
    externalId: ExternalId
) =
    createUpdate {
            sql(
                "UPDATE employee SET external_id = ${bind(externalId)} WHERE employee_number = ${bind(employeeNumber)} AND (external_id != ${bind(externalId)} OR external_id IS NULL)"
            )
        }
        .updateNoneOrOne()

fun Database.Transaction.loginEmployee(clock: EvakaClock, employee: NewEmployee): Employee {
    val now = clock.now()
    return createUpdate {
            sql(
                """
INSERT INTO employee (first_name, last_name, email, external_id, employee_number, roles, active, last_login)
VALUES (${bind(employee.firstName)}, ${bind(employee.lastName)}, ${bind(employee.email)}, ${bind(employee.externalId)}, ${
                bind(
                    employee.employeeNumber
                )
            }, ${bind(employee.roles)}::user_role[], ${bind(employee.active)}, ${bind(now)})
ON CONFLICT (external_id) DO UPDATE
SET last_login = ${bind(now)}, first_name = EXCLUDED.first_name, last_name = EXCLUDED.last_name, email = EXCLUDED.email, employee_number = EXCLUDED.employee_number, active = TRUE
RETURNING id, preferred_first_name, first_name, last_name, email, external_id, created, updated, roles, active
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<Employee>()
}

fun Database.Read.getEmployeeRoles(id: EmployeeId): EmployeeRoles =
    createQuery {
            sql(
                """
SELECT employee.roles AS global_roles, (
    SELECT array_agg(DISTINCT role ORDER BY role)
    FROM daycare_acl
    WHERE employee_id = employee.id
) AS all_scoped_roles
FROM employee
WHERE id = ${bind(id)}
"""
            )
        }
        .exactlyOne<EmployeeRoles>()

fun Database.Read.getEmployeeNumber(id: EmployeeId): String? =
    createQuery { sql("""
SELECT employee_number
FROM employee
WHERE id = ${bind(id)}
""") }
        .exactlyOneOrNull<String>()

private fun Database.Read.searchEmployees(
    id: EmployeeId? = null,
    externalId: ExternalId? = null,
    temporaryInUnitId: DaycareId? = null
): Database.Result<Employee> =
    createQuery {
            sql(
                """
SELECT e.id, preferred_first_name, first_name, last_name, email, external_id, e.created, e.updated, roles, temporary_in_unit_id, e.active
FROM employee e
WHERE (${bind(id)}::uuid IS NULL OR e.id = ${bind(id)}) AND (${bind(externalId)}::text IS NULL OR e.external_id = ${bind(externalId)})
  AND (${bind(temporaryInUnitId)} IS NULL OR e.temporary_in_unit_id = ${bind(temporaryInUnitId)})
"""
            )
        }
        .mapTo<Employee>()

private fun Database.Read.searchFinanceDecisionHandlers(id: EmployeeId? = null) =
    createQuery {
            sql(
                """
SELECT DISTINCT e.id, e.preferred_first_name, e.first_name, e.last_name, e.email, e.external_id, e.created, e.updated, e.roles, e.active
FROM employee e
JOIN daycare ON daycare.finance_decision_handler = e.id
WHERE (${bind(id)}::uuid IS NULL OR e.id = ${bind(id)})
    """
            )
        }
        .mapTo<Employee>()

fun Database.Read.getEmployees(): List<Employee> = searchEmployees().toList()

fun Database.Read.getTemporaryEmployees(unitId: DaycareId): List<Employee> =
    searchEmployees(temporaryInUnitId = unitId).toList()

fun Database.Read.getFinanceDecisionHandlers(): List<Employee> =
    searchFinanceDecisionHandlers().toList()

fun Database.Read.getEmployee(id: EmployeeId): Employee? =
    searchEmployees(id = id).exactlyOneOrNull()

fun Database.Read.getEmployeeByExternalId(externalId: ExternalId): Employee? =
    searchEmployees(externalId = externalId).exactlyOneOrNull()

private fun Database.Read.createEmployeeUserQuery(where: Predicate) = createQuery {
    sql(
        """
SELECT id, preferred_first_name, first_name, last_name, email, active, employee.roles AS global_roles, (
    SELECT array_agg(DISTINCT role ORDER BY role)
    FROM daycare_acl
    WHERE employee_id = employee.id
) AS all_scoped_roles
FROM employee
WHERE ${predicate(where.forTable("employee"))}
"""
    )
}

fun Database.Transaction.markEmployeeLastLogin(clock: EvakaClock, id: EmployeeId) =
    createUpdate {
            sql(
                """
UPDATE employee 
SET last_login = ${bind(clock.now())}
WHERE id = ${bind(id)}
"""
            )
        }
        .execute()

fun Database.Read.getEmployeeUser(id: EmployeeId): EmployeeUser? =
    createEmployeeUserQuery(Predicate { where("$it.id = ${bind(id)}") })
        .exactlyOneOrNull<EmployeeUser>()

fun Database.Read.getEmployeeWithRoles(id: EmployeeId): EmployeeWithDaycareRoles? {
    return createQuery {
            sql(
                """
SELECT
    employee.id,
    employee.external_id,
    employee.employee_number,
    employee.created,
    employee.updated,
    employee.last_login,
    employee.first_name,
    employee.last_name,
    employee.email,
    employee.active,
    temp_unit.name as temporary_unit_name,
    employee.roles AS global_roles,
    (
        SELECT jsonb_agg(jsonb_build_object('daycareId', acl.daycare_id, 'daycareName', d.name, 'role', acl.role))
        FROM daycare_acl acl
        JOIN daycare d ON acl.daycare_id = d.id
        WHERE acl.employee_id = employee.id
    ) AS daycare_roles,
    (
        SELECT jsonb_agg(jsonb_build_object('daycareId', d.id, 'daycareName', d.name, 'groupId', dg.id, 'groupName', dg.name))
        FROM daycare_group_acl acl
        JOIN daycare_group dg on dg.id = acl.daycare_group_id
        JOIN daycare d ON d.id = dg.daycare_id
        WHERE acl.employee_id = employee.id
    ) AS daycare_group_roles
FROM employee
LEFT JOIN daycare temp_unit ON temp_unit.id = employee.temporary_in_unit_id
WHERE employee.id = ${bind(id)}
"""
            )
        }
        .exactlyOneOrNull<EmployeeWithDaycareRoles>()
}

fun Database.Transaction.updateEmployee(id: EmployeeId, firstName: String, lastName: String) =
    createUpdate {
            sql(
                "UPDATE employee SET first_name = ${bind(firstName)}, last_name = ${bind(lastName)} WHERE id = ${bind(id)}"
            )
        }
        .updateExactlyOne()

fun Database.Transaction.updateEmployeeActive(id: EmployeeId, active: Boolean) =
    createUpdate { sql("UPDATE employee SET active = ${bind(active)} WHERE id = ${bind(id)}") }
        .updateExactlyOne()

fun Database.Transaction.upsertEmployeeDaycareRoles(
    id: EmployeeId,
    daycareIds: List<DaycareId>,
    role: UserRole
) {
    val batch =
        prepareBatch(
            """
        INSERT INTO daycare_acl (daycare_id, employee_id, role) 
        VALUES (:daycareId, :employeeId, :role)
        ON CONFLICT (employee_id, daycare_id) DO UPDATE SET role = :role
    """
        )
    daycareIds.forEach {
        batch.bind("daycareId", it).bind("employeeId", id).bind("role", role).add()
    }
    batch.execute()
}

fun Database.Transaction.updateEmployeeGlobalRoles(id: EmployeeId, globalRoles: List<UserRole>) {
    val updated =
        createUpdate {
                sql(
                    """
        UPDATE employee
        SET roles = ${bind(globalRoles.distinct())}
        WHERE id = ${bind(id)}
    """
                )
            }
            .execute()

    if (updated != 1) throw NotFound("employee $id not found")
}

fun Database.Transaction.deleteEmployeeDaycareRoles(id: EmployeeId, daycareId: DaycareId?) {
    createUpdate {
            sql(
                """
        DELETE FROM daycare_acl
        WHERE 
            employee_id = ${bind(id)}
            ${if (daycareId != null) "AND daycare_id = ${bind(daycareId)}" else ""}
    """
            )
        }
        .execute()

    createUpdate {
            sql(
                """
        DELETE FROM daycare_group_acl
        WHERE 
            employee_id = ${bind(id)}
            ${if (daycareId != null) """
                AND daycare_group_id IN (
                    SELECT id FROM daycare_group WHERE daycare_id = ${bind(daycareId)}
                )
            """ else ""}
    """
            )
        }
        .execute()
}

data class PagedEmployeesWithDaycareRoles(
    val data: List<EmployeeWithDaycareRoles>,
    val total: Int,
    val pages: Int,
)

fun getEmployeesPaged(
    tx: Database.Read,
    page: Int,
    pageSize: Int,
    searchTerm: String = ""
): PagedEmployeesWithDaycareRoles {
    val (freeTextQuery, freeTextParams) =
        freeTextSearchQueryForColumns(
            listOf("employee"),
            listOf("first_name", "last_name"),
            searchTerm
        )

    val params =
        listOf(Binding.of("offset", (page - 1) * pageSize), Binding.of("pageSize", pageSize))

    val conditions = listOfNotNull(if (searchTerm.isNotBlank()) freeTextQuery else null)

    val whereClause = conditions.takeIf { it.isNotEmpty() }?.joinToString(" AND ") ?: "TRUE"

    val sql =
        """
SELECT
    employee.id,
    employee.external_id,
    employee.employee_number,
    employee.created,
    employee.updated,
    employee.last_login,
    employee.preferred_first_name,
    employee.first_name,
    employee.last_name,
    employee.email,
    employee.active,
    temp_unit.name as temporary_unit_name,
    employee.roles AS global_roles,
    (
        SELECT jsonb_agg(jsonb_build_object('daycareId', acl.daycare_id, 'daycareName', d.name, 'role', acl.role))
        FROM daycare_acl acl
        JOIN daycare d ON acl.daycare_id = d.id
        WHERE acl.employee_id = employee.id
    ) AS daycare_roles,
    (
        SELECT jsonb_agg(jsonb_build_object('daycareId', d.id, 'daycareName', d.name, 'groupId', dg.id, 'groupName', dg.name))
        FROM daycare_group_acl acl
        JOIN daycare_group dg on dg.id = acl.daycare_group_id
        JOIN daycare d ON d.id = dg.daycare_id
        WHERE acl.employee_id = employee.id
    ) AS daycare_group_roles,
    count(*) OVER () AS count
FROM employee
LEFT JOIN daycare temp_unit ON temp_unit.id = employee.temporary_in_unit_id
WHERE $whereClause
ORDER BY last_name, first_name DESC
LIMIT :pageSize OFFSET :offset
    """
            .trimIndent()
    @Suppress("DEPRECATION")
    return tx.createQuery(sql)
        .addBindings(params)
        .addBindings(freeTextParams)
        .mapToPaged(::PagedEmployeesWithDaycareRoles, pageSize)
}

fun Database.Transaction.deleteEmployee(employeeId: EmployeeId) =
    createUpdate { sql("DELETE FROM employee WHERE id = ${bind(employeeId)}") }.execute()

fun Database.Transaction.upsertPinCode(userId: EmployeeId, pinCode: PinCode) {
    // Note: according to spec, setting a pin resets the failure and opens a locked pin
    val updated =
        this.createUpdate {
                sql(
                    """
INSERT INTO employee_pin (user_id, pin)
VALUES (${bind(userId)}, ${bind(pinCode.pin)})
ON CONFLICT (user_id) DO UPDATE SET
    user_id = ${bind(userId)},
    pin = ${bind(pinCode.pin)},
    locked = false,
    failure_count = 0
"""
                )
            }
            .execute()

    if (updated == 0) throw NotFound("Could not update pin code for $userId. User not found")
}

fun Database.Transaction.removePinCode(userId: EmployeeId) {
    createUpdate { sql("DELETE FROM employee_pin WHERE user_id = ${bind(userId)}") }.execute()
}

fun Database.Read.getPinCode(userId: EmployeeId): PinCode? =
    createQuery { sql("SELECT pin FROM employee_pin WHERE user_id = ${bind(userId)}") }
        .exactlyOneOrNull<PinCode>()

fun Database.Read.employeePinIsCorrect(employeeId: EmployeeId, pin: String): Boolean =
    createQuery {
            sql(
                """
SELECT EXISTS (
    SELECT 1
    FROM employee_pin
    WHERE user_id = ${bind(employeeId)}
    AND pin = ${bind(pin)}
    AND locked = false
)
"""
            )
        }
        .exactlyOne<Boolean>()

fun Database.Transaction.resetEmployeePinFailureCount(employeeId: EmployeeId) =
    createUpdate {
            sql(
                """
UPDATE employee_pin
SET failure_count = 0
WHERE user_id = ${bind(employeeId)}
"""
            )
        }
        .execute()

fun Database.Transaction.updateEmployeePinFailureCountAndCheckIfLocked(
    employeeId: EmployeeId
): Boolean =
    createQuery {
            sql(
                """
UPDATE employee_pin
SET 
    failure_count = failure_count + 1,
    locked = 
        CASE 
            WHEN failure_count < (4 + 1) THEN false
            ELSE true
        end    
WHERE 
    user_id = ${bind(employeeId)}
RETURNING locked
"""
            )
        }
        .exactlyOneOrNull<Boolean>() ?: false

fun Database.Read.isPinLocked(employeeId: EmployeeId): Boolean =
    createQuery { sql("SELECT locked FROM employee_pin WHERE user_id = ${bind(employeeId)}") }
        .exactlyOneOrNull<Boolean>() ?: false

fun Database.Transaction.deactivateInactiveEmployees(now: HelsinkiDateTime): List<EmployeeId> {
    val inactiveEmployees = getInactiveEmployees(now)
    inactiveEmployees.forEach { employeeId -> deactivateEmployeeRemoveRolesAndPin(employeeId) }
    return inactiveEmployees
}

fun Database.Transaction.deactivateEmployeeRemoveRolesAndPin(id: EmployeeId) {
    updateEmployeeActive(id = id, active = false)
    updateEmployeeGlobalRoles(id = id, globalRoles = emptyList())
    deleteEmployeeDaycareRoles(id = id, daycareId = null)
    removePinCode(userId = id)
}

fun Database.Read.getInactiveEmployees(now: HelsinkiDateTime): List<EmployeeId> {
    return createQuery {
            sql(
                """
    SELECT e.id
    FROM employee e
    LEFT JOIN daycare_acl d ON d.employee_id = e.id
    LEFT JOIN daycare_group_acl dg ON dg.employee_id = e.id
    WHERE (
        SELECT max(ts)
        FROM unnest(ARRAY[e.last_login, d.updated, dg.updated]) ts
    ) < ${bind(now)} - interval '45 days'
"""
            )
        }
        .toList<EmployeeId>()
}

fun Database.Read.getEmployeeNamesByIds(employeeIds: List<EmployeeId>) =
    createQuery {
            sql(
                """
SELECT id, concat(first_name, ' ', last_name) AS name
FROM employee
WHERE id = ANY(${bind(employeeIds)})
"""
            )
        }
        .toList<EmployeeIdWithName>()
        .associate { it.id to it.name }

fun Database.Transaction.setEmployeePreferredFirstName(
    employeeId: EmployeeId,
    preferredFirstName: String?
) =
    createUpdate {
            sql(
                "UPDATE employee SET preferred_first_name = ${bind(preferredFirstName)} WHERE id = ${bind(employeeId)}"
            )
        }
        .execute()

fun Database.Read.getEmployeesByRoles(roles: Set<UserRole>, unitId: DaycareId?): List<Employee> {
    val globalRoles = roles.filter { it.isGlobalRole() }
    val unitScopedRoles = roles.filter { it.isUnitScopedRole() }
    return if (unitId == null) {
        createQuery {
                sql(
                    """
SELECT id, first_name, last_name, email, external_id, created, updated, active
FROM employee
WHERE roles && ${bind(globalRoles)}::user_role[]
ORDER BY last_name, first_name
        """
                )
            }
            .toList<Employee>()
    } else {
        createQuery {
                sql(
                    """
SELECT id, first_name, last_name, email, external_id, created, updated, active
FROM employee
WHERE roles && ${bind(globalRoles)}::user_role[] OR id IN (
    SELECT employee_id
    FROM daycare_acl
    WHERE daycare_id = ${bind(unitId)} AND role = ANY(${bind(unitScopedRoles)}::user_role[])
)
ORDER BY last_name, first_name
        """
                )
            }
            .toList<Employee>()
    }
}
