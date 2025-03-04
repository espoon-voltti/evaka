// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

import fi.espoo.evaka.Sensitive
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.identity.ExternalId
import fi.espoo.evaka.identity.isValidSSN
import fi.espoo.evaka.pairing.MobileDevice
import fi.espoo.evaka.pairing.deleteDevice
import fi.espoo.evaka.pairing.listPersonalDevices
import fi.espoo.evaka.pis.controllers.PinCode
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.*
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import java.time.LocalDate
import org.jdbi.v3.json.Json

data class NewEmployee(
    val firstName: String,
    val lastName: String,
    val email: String?,
    val externalId: ExternalId?,
    val employeeNumber: String?,
    val roles: Set<UserRole> = setOf(),
    val temporaryInUnitId: DaycareId?,
    val active: Boolean,
)

data class EmployeeUser(
    val id: EmployeeId,
    val preferredFirstName: String? = null,
    val firstName: String,
    val lastName: String,
    val globalRoles: Set<UserRole> = setOf(),
    val allScopedRoles: Set<UserRole> = setOf(),
    val active: Boolean,
)

data class EmployeeRoles(
    val globalRoles: Set<UserRole> = setOf(),
    val allScopedRoles: Set<UserRole> = setOf(),
)

data class DaycareRole(
    val daycareId: DaycareId,
    val daycareName: String,
    val role: UserRole,
    val endDate: LocalDate?,
)

data class ScheduledDaycareRole(
    val daycareId: DaycareId,
    val daycareName: String,
    val role: UserRole,
    val startDate: LocalDate,
    val endDate: LocalDate?,
)

data class DaycareGroupRole(
    val daycareId: DaycareId,
    val daycareName: String,
    val groupId: GroupId,
    val groupName: String,
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
    @Json val scheduledDaycareRoles: List<ScheduledDaycareRole> = listOf(),
    @Json val daycareGroupRoles: List<DaycareGroupRole> = listOf(),
    @Json val personalMobileDevices: List<MobileDevice> = listOf(),
    val temporaryUnitName: String?,
    val active: Boolean,
    val hasSsn: Boolean,
)

fun Database.Transaction.createEmployee(employee: NewEmployee): Employee =
    createUpdate {
            sql(
                """
INSERT INTO employee (first_name, last_name, email, external_id, employee_number, roles, temporary_in_unit_id, active)
VALUES (${bind(employee.firstName)}, ${bind(employee.lastName)}, ${bind(employee.email)}, ${bind(employee.externalId)}, ${bind(employee.employeeNumber)}, ${bind(employee.roles)}::user_role[], ${bind(employee.temporaryInUnitId)}, ${bind(employee.active)})
RETURNING id, first_name, last_name, email, external_id, created, updated, roles, temporary_in_unit_id, active, (social_security_number IS NOT NULL) AS has_ssn
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<Employee>()

fun Database.Transaction.updateExternalIdByEmployeeNumber(
    employeeNumber: String,
    externalId: ExternalId,
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
RETURNING id, preferred_first_name, first_name, last_name, email, external_id, created, updated, roles, active, (social_security_number IS NOT NULL) AS has_ssn
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<Employee>()
}

data class EmployeeSuomiFiLoginRequest(
    val firstName: String,
    val lastName: String,
    val ssn: Sensitive<String>,
)

fun Database.Transaction.loginEmployeeWithSuomiFi(
    now: HelsinkiDateTime,
    request: EmployeeSuomiFiLoginRequest,
): Employee? =
    createUpdate {
            sql(
                """
UPDATE employee
SET last_login = ${bind(now)}, first_name = ${bind(request.firstName)}, last_name = ${bind(request.lastName)}
WHERE social_security_number = ${bind(request.ssn.value)}
RETURNING id, preferred_first_name, first_name, last_name, email, external_id, created, updated, temporary_in_unit_id, active, (social_security_number IS NOT NULL) AS has_ssn
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOneOrNull<Employee>()

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
    createQuery {
            sql(
                """
SELECT employee_number
FROM employee
WHERE id = ${bind(id)}
"""
            )
        }
        .exactlyOneOrNull<String>()

private fun Database.Read.searchEmployees(
    id: EmployeeId? = null,
    externalId: ExternalId? = null,
    temporaryInUnitId: DaycareId? = null,
): Database.Result<Employee> =
    createQuery {
            sql(
                """
SELECT e.id, preferred_first_name, first_name, last_name, email, external_id, e.created, e.updated, roles, temporary_in_unit_id, e.active, (social_security_number IS NOT NULL) AS has_ssn
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
SELECT DISTINCT e.id, e.preferred_first_name, e.first_name, e.last_name, e.email, e.external_id, e.created, e.updated, e.roles, e.active, (e.social_security_number IS NOT NULL) AS has_ssn
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
    (employee.social_security_number IS NOT NULL) AS has_ssn,
    temp_unit.name as temporary_unit_name,
    employee.roles AS global_roles,
    (
        SELECT jsonb_agg(jsonb_build_object('daycareId', acl.daycare_id, 'daycareName', d.name, 'role', acl.role, 'endDate', acl.end_date))
        FROM daycare_acl acl
        JOIN daycare d ON acl.daycare_id = d.id
        WHERE acl.employee_id = employee.id
    ) AS daycare_roles,
    (
        SELECT jsonb_agg(jsonb_build_object('daycareId', acls.daycare_id, 'daycareName', d.name, 'role', acls.role, 'startDate', acls.start_date, 'endDate', acls.end_date))
        FROM daycare_acl_schedule acls
        JOIN daycare d ON acls.daycare_id = d.id
        WHERE acls.employee_id = employee.id
    ) AS scheduled_daycare_roles,
    (
        SELECT jsonb_agg(jsonb_build_object('daycareId', d.id, 'daycareName', d.name, 'groupId', dg.id, 'groupName', dg.name))
        FROM daycare_group_acl acl
        JOIN daycare_group dg on dg.id = acl.daycare_group_id
        JOIN daycare d ON d.id = dg.daycare_id
        WHERE acl.employee_id = employee.id
    ) AS daycare_group_roles,
    (
        SELECT jsonb_agg(jsonb_build_object('id', md.id, 'name', md.name))
        FROM mobile_device md
        WHERE md.employee_id = employee.id
    ) AS personal_mobile_devices
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
    role: UserRole,
    endDate: LocalDate?,
) {
    executeBatch(daycareIds) {
        sql(
            """
INSERT INTO daycare_acl (daycare_id, employee_id, role, end_date) 
VALUES (${bind { daycareId -> daycareId }}, ${bind(id)}, ${bind(role)}, ${bind(endDate)})
ON CONFLICT (employee_id, daycare_id) DO UPDATE SET role = ${bind(role)}, end_date = ${bind(endDate)}
"""
        )
    }
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

fun getEmployees(
    tx: Database.Read,
    searchTerm: String = "",
    hideDeactivated: Boolean = false,
    globalRoles: Set<UserRole>?,
    unitRoles: Set<UserRole>?,
    unitProviderTypes: Set<ProviderType>?,
): List<EmployeeWithDaycareRoles> {
    val conditions =
        Predicate.allNotNull(
            if (searchTerm.isNotBlank()) employeeFreeTextSearchPredicate(searchTerm) else null,
            if (hideDeactivated) Predicate { where("$it.active = TRUE") } else null,
            if (globalRoles != null) Predicate { where("$it.roles && ${bind(globalRoles)}") }
            else null,
            if (unitRoles != null)
                Predicate {
                    where(
                        """
                            EXISTS (SELECT FROM daycare_acl
                                    WHERE daycare_acl.employee_id = $it.id
                                      AND daycare_acl.role = ANY (${bind(unitRoles)})
                            ${if (unitProviderTypes != null) """
                                AND EXISTS (SELECT FROM daycare
                                            WHERE daycare.id = daycare_acl.daycare_id
                                              AND daycare.provider_type = ANY (${bind(unitProviderTypes)}))"""
                        else ""})"""
                            .trimIndent()
                    )
                }
            else null,
        )

    return tx.createQuery {
            sql(
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
    (employee.social_security_number IS NOT NULL) AS has_ssn,
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
    (
        SELECT jsonb_agg(jsonb_build_object('id', md.id, 'name', md.name))
        FROM mobile_device md
        WHERE md.employee_id = employee.id
    ) AS personal_mobile_devices
FROM employee
LEFT JOIN daycare temp_unit ON temp_unit.id = employee.temporary_in_unit_id
WHERE ${predicate(conditions.forTable("employee"))}
ORDER BY last_name, first_name DESC
"""
            )
        }
        .toList<EmployeeWithDaycareRoles>()
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
    val inactiveEmployees =
        createQuery {
                sql(
                    """
    SELECT e.id
    FROM employee e
    LEFT JOIN daycare_acl d ON d.employee_id = e.id
    LEFT JOIN daycare_group_acl dg ON dg.employee_id = e.id
    WHERE (
        SELECT max(ts)
        FROM unnest(ARRAY[e.last_login, d.updated, dg.updated]) ts
    ) < ${bind(now)} - interval '56 days'
    AND e.active = true
"""
                )
            }
            .toList<EmployeeId>()
    inactiveEmployees.forEach { employeeId -> deactivateEmployeeRemoveRolesAndPin(employeeId) }
    return inactiveEmployees
}

fun Database.Transaction.deactivateEmployeeRemoveRolesAndPin(id: EmployeeId) {
    updateEmployeeActive(id = id, active = false)
    updateEmployeeGlobalRoles(id = id, globalRoles = emptyList())
    deleteEmployeeDaycareRoles(id = id, daycareId = null)
    removePinCode(userId = id)
    listPersonalDevices(id).forEach { deleteDevice(it.id) }
}

fun Database.Transaction.setEmployeePreferredFirstName(
    employeeId: EmployeeId,
    preferredFirstName: String?,
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
SELECT id, first_name, last_name, email, external_id, created, updated, active, (social_security_number IS NOT NULL) AS has_ssn
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
SELECT id, first_name, last_name, email, external_id, created, updated, active, (social_security_number IS NOT NULL) AS has_ssn
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

data class NewSsnEmployee(
    val ssn: Sensitive<String>,
    val firstName: String,
    val lastName: String,
    val email: String?,
) {
    init {
        if (!isValidSSN(ssn.value)) throw BadRequest("Invalid SSN")
    }
}

fun Database.Transaction.createEmployeeWithSsn(request: NewSsnEmployee): EmployeeId =
    createUpdate {
            sql(
                """
INSERT INTO employee (social_security_number, first_name, last_name, email, active)
VALUES (${bind(request.ssn.value)}, ${bind(request.firstName)}, ${bind(request.lastName)}, ${bind(request.email)}, true)
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()

private fun Database.Read.employeeNumbersQuery(
    employeeNumbers: Collection<String>
): Database.Query {
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

fun Database.Read.getEmployeeIdsByNumbers(
    employeeNumbers: Collection<String>
): Map<String, EmployeeId> {
    return employeeNumbersQuery(employeeNumbers).toMap { columnPair("employee_number", "id") }
}

fun Database.Read.getEmployeeIdsByNumbersMapById(
    employeeNumbers: Collection<String>
): Map<EmployeeId, String> {
    return employeeNumbersQuery(employeeNumbers).toMap { columnPair("id", "employee_number") }
}
