// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

import fi.espoo.evaka.identity.ExternalId
import fi.espoo.evaka.pis.controllers.PinCode
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Binding
import fi.espoo.evaka.shared.db.Database
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
    val groupId: DaycareId,
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
    createUpdate(
            // language=SQL
            """
INSERT INTO employee (first_name, last_name, email, external_id, employee_number, roles, temporary_in_unit_id, active)
VALUES (:employee.firstName, :employee.lastName, :employee.email, :employee.externalId, :employee.employeeNumber, :employee.roles::user_role[], :employee.temporaryInUnitId, :employee.active)
RETURNING id, first_name, last_name, email, external_id, created, updated, roles, temporary_in_unit_id, active
    """
                .trimIndent()
        )
        .bindKotlin("employee", employee)
        .executeAndReturnGeneratedKeys()
        .exactlyOne<Employee>()

fun Database.Transaction.updateExternalIdByEmployeeNumber(
    employeeNumber: String,
    externalId: ExternalId
) =
    createUpdate(
            "UPDATE employee SET external_id = :externalId WHERE employee_number = :employeeNumber AND (external_id != :externalId OR external_id IS NULL)"
        )
        .bind("employeeNumber", employeeNumber)
        .bind("externalId", externalId)
        .updateNoneOrOne()

fun Database.Transaction.loginEmployee(clock: EvakaClock, employee: NewEmployee): Employee =
    createUpdate(
            // language=SQL
            """
INSERT INTO employee (first_name, last_name, email, external_id, employee_number, roles, active, last_login)
VALUES (:employee.firstName, :employee.lastName, :employee.email, :employee.externalId, :employee.employeeNumber, :employee.roles::user_role[], :employee.active, :now)
ON CONFLICT (external_id) DO UPDATE
SET last_login = :now, first_name = EXCLUDED.first_name, last_name = EXCLUDED.last_name, email = EXCLUDED.email, employee_number = EXCLUDED.employee_number
RETURNING id, preferred_first_name, first_name, last_name, email, external_id, created, updated, roles, active
    """
                .trimIndent()
        )
        .bindKotlin("employee", employee)
        .bind("now", clock.now())
        .executeAndReturnGeneratedKeys()
        .exactlyOne<Employee>()

fun Database.Read.getEmployeeRoles(id: EmployeeId): EmployeeRoles =
    createQuery(
            """
SELECT employee.roles AS global_roles, (
    SELECT array_agg(DISTINCT role ORDER BY role)
    FROM daycare_acl
    WHERE employee_id = employee.id
) AS all_scoped_roles
FROM employee
WHERE id = :id
    """
                .trimIndent()
        )
        .bind("id", id)
        .exactlyOne<EmployeeRoles>()

fun Database.Read.getEmployeeNumber(id: EmployeeId): String? =
    createQuery(
            """
SELECT employee_number
FROM employee
WHERE id = :id
    """
                .trimIndent()
        )
        .bind("id", id)
        .exactlyOneOrNull<String>()

private fun Database.Read.searchEmployees(
    id: EmployeeId? = null,
    externalId: ExternalId? = null,
    temporaryInUnitId: DaycareId? = null
): Database.Result<Employee> =
    createQuery(
            // language=SQL
            """
SELECT e.id, preferred_first_name, first_name, last_name, email, external_id, e.created, e.updated, roles, temporary_in_unit_id, e.active
FROM employee e
WHERE (:id::uuid IS NULL OR e.id = :id) AND (:externalId::text IS NULL OR e.external_id = :externalId)
  AND (:temporaryInUnitId IS NULL OR e.temporary_in_unit_id = :temporaryInUnitId)
    """
                .trimIndent()
        )
        .bind("id", id)
        .bind("externalId", externalId)
        .bind("temporaryInUnitId", temporaryInUnitId)
        .mapTo<Employee>()

private fun Database.Read.searchFinanceDecisionHandlers(id: EmployeeId? = null) =
    createQuery(
            // language=SQL
            """
SELECT DISTINCT e.id, e.preferred_first_name, e.first_name, e.last_name, e.email, e.external_id, e.created, e.updated, e.roles, e.active
FROM employee e
JOIN daycare ON daycare.finance_decision_handler = e.id
WHERE (:id::uuid IS NULL OR e.id = :id)
    """
                .trimIndent()
        )
        .bind("id", id)
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

private fun Database.Read.createEmployeeUserQuery(where: String) =
    createQuery(
        """
SELECT id, preferred_first_name, first_name, last_name, email, active, employee.roles AS global_roles, (
    SELECT array_agg(DISTINCT role ORDER BY role)
    FROM daycare_acl
    WHERE employee_id = employee.id
) AS all_scoped_roles
FROM employee
$where
"""
    )

fun Database.Transaction.markEmployeeLastLogin(clock: EvakaClock, id: EmployeeId) =
    createUpdate(
            """
UPDATE employee 
SET last_login = :now
WHERE id = :id
    """
                .trimIndent()
        )
        .bind("id", id)
        .bind("now", clock.now())
        .execute()

fun Database.Read.getEmployeeUser(id: EmployeeId): EmployeeUser? =
    createEmployeeUserQuery("WHERE id = :id").bind("id", id).exactlyOneOrNull<EmployeeUser>()

fun Database.Read.getEmployeeWithRoles(id: EmployeeId): EmployeeWithDaycareRoles? {
    // language=SQL
    val sql =
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
WHERE employee.id = :id
    """
            .trimIndent()

    return createQuery(sql).bind("id", id).exactlyOneOrNull<EmployeeWithDaycareRoles>()
}

fun Database.Transaction.updateEmployee(id: EmployeeId, firstName: String, lastName: String) =
    createUpdate(
            "UPDATE employee SET first_name = :firstName, last_name = :lastName WHERE id = :id"
        )
        .bind("id", id)
        .bind("firstName", firstName)
        .bind("lastName", lastName)
        .updateExactlyOne()

fun Database.Transaction.updateEmployeeActive(id: EmployeeId, active: Boolean) =
    createUpdate("UPDATE employee SET active = :active WHERE id = :id")
        .bind("id", id)
        .bind("active", active)
        .updateExactlyOne()

fun Database.Transaction.updateEmployee(id: EmployeeId, globalRoles: List<UserRole>) {
    // language=SQL
    val sql =
        """
UPDATE employee
SET roles = :roles::user_role[]
WHERE id = :id
    """
            .trimIndent()

    val updated = createUpdate(sql).bind("id", id).bind("roles", globalRoles).execute()

    if (updated != 1) throw NotFound("employee $id not found")
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

    // language=SQL
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
    return tx.createQuery(sql)
        .addBindings(params)
        .addBindings(freeTextParams)
        .mapToPaged(::PagedEmployeesWithDaycareRoles, pageSize)
}

fun Database.Transaction.deleteEmployee(employeeId: EmployeeId) =
    createUpdate(
            // language=SQL
            """
DELETE FROM employee
WHERE id = :employeeId
    """
                .trimIndent()
        )
        .bind("employeeId", employeeId)
        .execute()

fun Database.Transaction.upsertPinCode(userId: EmployeeId, pinCode: PinCode) {
    // Note: according to spec, setting a pin resets the failure and opens a locked pin
    // language=sql
    val sql =
        """
INSERT INTO employee_pin(user_id, pin)
VALUES(:userId, :pin)
ON CONFLICT (user_id) DO UPDATE SET
        user_id = :userId,
        pin = :pin,
        locked = false,
        failure_count = 0
    """
            .trimIndent()
    val updated = this.createUpdate(sql).bind("userId", userId).bind("pin", pinCode.pin).execute()

    if (updated == 0) throw NotFound("Could not update pin code for $userId. User not found")
}

fun Database.Read.getPinCode(userId: EmployeeId): PinCode? =
    createQuery("SELECT pin FROM employee_pin WHERE user_id = :userId")
        .bind("userId", userId)
        .exactlyOneOrNull<PinCode>()

fun Database.Read.employeePinIsCorrect(employeeId: EmployeeId, pin: String): Boolean =
    createQuery(
            """
SELECT EXISTS (
    SELECT 1
    FROM employee_pin
    WHERE user_id = :employeeId
    AND pin = :pin
    AND locked = false
)
"""
                .trimIndent()
        )
        .bind("employeeId", employeeId)
        .bind("pin", pin)
        .exactlyOne<Boolean>()

fun Database.Transaction.resetEmployeePinFailureCount(employeeId: EmployeeId) =
    createUpdate(
            """
UPDATE employee_pin
SET failure_count = 0
WHERE user_id = :employeeId
    """
                .trimIndent()
        )
        .bind("employeeId", employeeId)
        .execute()

fun Database.Transaction.updateEmployeePinFailureCountAndCheckIfLocked(
    employeeId: EmployeeId
): Boolean =
    createQuery(
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
    user_id = :employeeId
RETURNING locked
"""
                .trimIndent()
        )
        .bind("employeeId", employeeId)
        .exactlyOneOrNull<Boolean>() ?: false

fun Database.Read.isPinLocked(employeeId: EmployeeId): Boolean =
    createQuery("SELECT locked FROM employee_pin WHERE user_id = :id")
        .bind("id", employeeId)
        .exactlyOneOrNull<Boolean>() ?: false

fun Database.Transaction.clearRolesForInactiveEmployees(now: HelsinkiDateTime): List<EmployeeId> {
    return createQuery(
            """
WITH employees_to_reset AS (
    SELECT e.id
    FROM employee e
    LEFT JOIN daycare_acl d ON d.employee_id = e.id
    LEFT JOIN daycare_group_acl dg ON dg.employee_id = e.id
    WHERE (e.roles != '{}' OR d.employee_id IS NOT NULL OR dg.employee_id IS NOT NULL) AND (e.last_login IS NULL OR (
        SELECT max(ts)
        FROM unnest(ARRAY[e.last_login, d.updated, dg.updated]) ts
    ) < :now - interval '3 months')
), delete_daycare_acl AS (
    DELETE FROM daycare_acl WHERE employee_id = ANY(SELECT id FROM employees_to_reset)
), delete_daycare_group_acl AS (
    DELETE FROM daycare_group_acl WHERE employee_id = ANY(SELECT id FROM employees_to_reset)
)
UPDATE employee SET roles = '{}', active = FALSE
WHERE id = ANY (SELECT id FROM employees_to_reset)
RETURNING id
"""
        )
        .bind("now", now)
        .toList<EmployeeId>()
}

fun Database.Read.getEmployeeNamesByIds(employeeIds: List<EmployeeId>) =
    createQuery(
            """
SELECT id, concat(first_name, ' ', last_name) AS name
FROM employee
WHERE id = ANY(:ids)
        """
                .trimIndent()
        )
        .bind("ids", employeeIds)
        .toList<EmployeeIdWithName>()
        .map { it.id to it.name }
        .toMap()

fun Database.Transaction.setEmployeePreferredFirstName(
    employeeId: EmployeeId,
    preferredFirstName: String?
) =
    createUpdate(
            "UPDATE employee SET preferred_first_name = :preferredFirstName WHERE id = :employeeId"
        )
        .bind("employeeId", employeeId)
        .bind("preferredFirstName", preferredFirstName)
        .execute()

fun Database.Read.getEmployeesByRoles(roles: Set<UserRole>, unitId: DaycareId?): List<Employee> {
    val globalRoles = roles.filter { it.isGlobalRole() }
    val unitScopedRoles = roles.filter { it.isUnitScopedRole() }
    return if (unitId == null) {
        createQuery(
                """
SELECT id, first_name, last_name, email, external_id, created, updated, active
FROM employee
WHERE roles && :globalRoles::user_role[]
ORDER BY last_name, first_name
        """
            )
            .bind("globalRoles", globalRoles)
            .toList<Employee>()
    } else {
        createQuery(
                """
SELECT id, first_name, last_name, email, external_id, created, updated, active
FROM employee
WHERE roles && :globalRoles::user_role[] OR id IN (
    SELECT employee_id
    FROM daycare_acl
    WHERE daycare_id = :unitId AND role = ANY(:unitScopedRoles::user_role[])
)
ORDER BY last_name, first_name
        """
            )
            .bind("globalRoles", globalRoles)
            .bind("unitScopedRoles", unitScopedRoles)
            .bind("unitId", unitId)
            .toList<Employee>()
    }
}
