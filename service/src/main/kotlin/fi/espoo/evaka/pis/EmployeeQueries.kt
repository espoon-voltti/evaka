// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

import fi.espoo.evaka.identity.ExternalId
import fi.espoo.evaka.pis.controllers.PinCode
import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.freeTextSearchQueryForColumns
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.mapToPaged
import fi.espoo.evaka.shared.withCountMapper
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.json.Json
import java.util.UUID

data class NewEmployee(
    val firstName: String,
    val lastName: String,
    val email: String?,
    val externalId: ExternalId?,
    val roles: Set<UserRole> = setOf()
)

data class EmployeeUser(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val globalRoles: Set<UserRole> = setOf(),
    val allScopedRoles: Set<UserRole> = setOf()
)

data class DaycareRole(
    val daycareId: UUID,
    val daycareName: String,
    val role: UserRole
)

data class EmployeeWithDaycareRoles(
    val id: UUID,
    val created: HelsinkiDateTime,
    val updated: HelsinkiDateTime?,
    val firstName: String,
    val lastName: String,
    val email: String?,
    val globalRoles: List<UserRole> = listOf(),
    @Json
    val daycareRoles: List<DaycareRole> = listOf()
)

fun Handle.createEmployee(employee: NewEmployee): Employee = createUpdate(
    // language=SQL
    """
INSERT INTO employee (first_name, last_name, email, external_id, roles)
VALUES (:employee.firstName, :employee.lastName, :employee.email, :employee.externalId, :employee.roles::user_role[])
RETURNING id, first_name, last_name, email, external_id, created, updated, roles
    """.trimIndent()
).bindKotlin("employee", employee)
    .executeAndReturnGeneratedKeys()
    .mapTo<Employee>()
    .asSequence().first()

private fun Handle.searchEmployees(id: UUID? = null) = createQuery(
    // language=SQL
    """
SELECT e.id, first_name, last_name, email, external_id, e.created, e.updated, roles
FROM employee e
LEFT JOIN mobile_device md on e.id = md.id 
WHERE (:id::uuid IS NULL OR e.id = :id) AND md.id IS NULL
    """.trimIndent()
).bind("id", id)
    .mapTo<Employee>()
    .asSequence()

private fun Handle.searchFinanceDecisionHandlers(id: UUID? = null) = createQuery(
    // language=SQL
    """
SELECT DISTINCT e.id, e.first_name, e.last_name, e.email, e.external_id, e.created, e.updated, e.roles
FROM employee e
JOIN daycare ON daycare.finance_decision_handler = e.id
WHERE (:id::uuid IS NULL OR e.id = :id)
    """.trimIndent()
).bind("id", id)
    .mapTo<Employee>()
    .asSequence()

fun Handle.getEmployees(): List<Employee> = searchEmployees().toList()
fun Handle.getFinanceDecisionHandlers(): List<Employee> = searchFinanceDecisionHandlers().toList()
fun Handle.getEmployee(id: UUID): Employee? = searchEmployees(id = id).firstOrNull()

private fun Database.Read.createEmployeeUserQuery(where: String) = createQuery(
    """
SELECT id, first_name, last_name, email, employee.roles AS global_roles, (
    SELECT array_agg(DISTINCT role ORDER BY role)
    FROM daycare_acl
    WHERE employee_id = employee.id
) AS all_scoped_roles
FROM employee
$where
"""
)

fun Database.Read.getEmployeeUser(id: UUID): EmployeeUser? = createEmployeeUserQuery("WHERE id = :id")
    .bind("id", id)
    .mapTo<EmployeeUser>()
    .singleOrNull()

fun Database.Read.getEmployeeUserByExternalId(externalId: ExternalId): EmployeeUser? = createEmployeeUserQuery("WHERE external_id = :externalId")
    .bind("externalId", externalId)
    .mapTo<EmployeeUser>()
    .singleOrNull()

fun getEmployeesPaged(
    tx: Database.Read,
    page: Int,
    pageSize: Int,
    searchTerm: String = ""
): Paged<EmployeeWithDaycareRoles> {

    val (freeTextQuery, freeTextParams) = freeTextSearchQueryForColumns(listOf("employee"), listOf("first_name", "last_name"), searchTerm)

    val params = mapOf(
        "offset" to (page - 1) * pageSize,
        "pageSize" to pageSize
    )

    val conditions = listOfNotNull(
        if (searchTerm.isNotBlank()) freeTextQuery else null,
    )

    val whereClause = conditions.takeIf { it.isNotEmpty() }?.joinToString(" AND ") ?: "TRUE"

    // language=SQL
    val sql = """
SELECT
    id,
    created,
    updated,
    first_name,
    last_name,
    email,
    employee.roles AS global_roles,
    (
        SELECT jsonb_agg(json_build_object('daycareId', dav.daycare_id, 'daycareName', d.name, 'role', dav.role))
        FROM daycare_acl_view dav
        JOIN daycare d ON dav.daycare_id = d.id
        WHERE dav.employee_id = employee.id
    ) AS daycare_roles,
    count(*) OVER () AS count
FROM employee
WHERE $whereClause
ORDER BY last_name, first_name DESC
LIMIT :pageSize OFFSET :offset
    """.trimIndent()
    return tx.createQuery(sql)
        .bindMap(params + freeTextParams)
        .map(withCountMapper<EmployeeWithDaycareRoles>())
        .let(mapToPaged(pageSize))
}

fun Handle.deleteEmployee(employeeId: UUID) = createUpdate(
    // language=SQL
    """
DELETE FROM employee
WHERE id = :employeeId
    """.trimIndent()
).bind("employeeId", employeeId)
    .execute()

fun Handle.deleteEmployeeByExternalId(externalId: ExternalId) = createUpdate(
    // language=SQL
    """
DELETE FROM employee
WHERE external_id = :externalId
    """.trimIndent()
).bind("externalId", externalId)
    .execute()

fun Handle.deleteEmployeeRolesByExternalId(externalId: ExternalId) = createUpdate(
    // language=SQL
    """
UPDATE employee
SET roles='{}'
WHERE external_id = :externalId
    """.trimIndent()
).bind("externalId", externalId)
    .execute()

fun Database.Transaction.upsertPinCode(
    userId: UUID,
    pinCode: PinCode
) {
    // Note: according to spec, setting a pin resets the failure and opens a locked pin
    // language=sql
    val sql = """
INSERT INTO employee_pin(user_id, pin)
VALUES(:userId, :pin)
ON CONFLICT (user_id) DO UPDATE SET
        user_id = :userId,
        pin = :pin,
        locked = false,
        failure_count = 0
    """.trimIndent()
    val updated = this.createUpdate(sql)
        .bind("userId", userId)
        .bind("pin", pinCode.pin)
        .execute()

    if (updated == 0) throw NotFound("Could not update pin code for $userId. User not found")
}

fun Handle.employeePinIsCorrect(employeeId: UUID, pin: String): Boolean = createQuery(
"""
SELECT EXISTS (
    SELECT 1
    FROM employee_pin
    WHERE user_id = :employeeId
    AND pin = :pin
)
    """.trimIndent()
).bind("employeeId", employeeId)
    .bind("pin", pin)
    .mapTo<Boolean>()
    .first()

fun Handle.updateEmployeePinFailureCountAndCheckIfLocked(employeeId: UUID): Boolean = createQuery(
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
    """.trimIndent()
).bind("employeeId", employeeId)
    .mapTo<Boolean>()
    .first()
