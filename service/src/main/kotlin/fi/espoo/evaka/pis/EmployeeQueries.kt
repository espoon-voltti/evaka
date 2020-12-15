// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

import fi.espoo.evaka.identity.ExternalId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo
import java.util.UUID

data class NewEmployee(
    val firstName: String,
    val lastName: String,
    val email: String?,
    val externalId: ExternalId?,
    val roles: Set<UserRole> = setOf()
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
SELECT id, first_name, last_name, email, external_id, created, updated, roles
FROM employee
WHERE (:id::uuid IS NULL OR id = :id) AND external_id IS NOT NULL
    """.trimIndent()
).bind("id", id)
    .mapTo<Employee>()
    .asSequence()

fun Handle.getEmployees(): List<Employee> = searchEmployees().toList()
fun Handle.getEmployee(id: UUID): Employee? = searchEmployees(id = id).firstOrNull()
fun Handle.getEmployeeAuthenticatedUser(externalId: ExternalId): AuthenticatedUser? = createQuery(
    // language=SQL
    """
SELECT id, employee.roles
  || (
    SELECT array_agg(DISTINCT role)
    FROM daycare_acl
    WHERE employee_id = employee.id
  )
  AS roles
FROM employee
WHERE external_id = :externalId
    """.trimIndent()
).bind("externalId", externalId).mapTo<AuthenticatedUser>().singleOrNull()

fun Handle.deleteEmployee(employeeId: UUID) = createUpdate(
    // language=SQL
    """
DELETE FROM employee
WHERE id = :employeeId
    """.trimIndent()
).bind("employeeId", employeeId)
    .execute()

fun Handle.deleteEmployees() = createUpdate(
    // language=SQL
    """
DELETE FROM employee
WHERE 1 = 1
    """.trimIndent()
).execute()

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
