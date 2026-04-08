// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis.controllers

import evaka.core.Audit
import evaka.core.AuditId
import evaka.core.EvakaEnv
import evaka.core.daycare.deactivatePersonalMessageAccountIfNeeded
import evaka.core.daycare.domain.ProviderType
import evaka.core.messaging.upsertEmployeeMessageAccount
import evaka.core.pis.Employee
import evaka.core.pis.EmployeeWithDaycareRoles
import evaka.core.pis.NewSsnEmployee
import evaka.core.pis.createEmployeeWithSsn
import evaka.core.pis.deactivateEmployeeRemoveRolesAndPin
import evaka.core.pis.deleteEmployee
import evaka.core.pis.deleteEmployeeDaycareRoles
import evaka.core.pis.getEmployee
import evaka.core.pis.getEmployeeWithRoles
import evaka.core.pis.getEmployees
import evaka.core.pis.getFinanceDecisionHandlers
import evaka.core.pis.isPinLocked
import evaka.core.pis.setEmployeePreferredFirstName
import evaka.core.pis.updateEmployeeActive
import evaka.core.pis.updateEmployeeEmail
import evaka.core.pis.updateEmployeeGlobalRoles
import evaka.core.pis.upsertEmployeeDaycareRoles
import evaka.core.pis.upsertPinCode
import evaka.core.shared.DaycareId
import evaka.core.shared.EmployeeId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.auth.UserRole
import evaka.core.shared.auth.deleteScheduledDaycareAclRow
import evaka.core.shared.auth.deleteScheduledDaycareAclRows
import evaka.core.shared.auth.insertScheduledDaycareAclRow
import evaka.core.shared.db.Database
import evaka.core.shared.db.mapPSQLException
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.Forbidden
import evaka.core.shared.domain.NotFound
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import java.time.LocalDate
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/employee/employees")
class EmployeeController(private val accessControl: AccessControl, private val env: EvakaEnv) {

    @GetMapping
    fun getEmployees(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): List<Employee> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.READ_EMPLOYEES,
                    )
                    it.getEmployees()
                }
            }
            .map { if (it.active) it else it.copy(lastName = "${it.lastName} (deaktivoitu)") }
            .sortedBy { it.email }
            .also { Audit.EmployeesRead.log() }
    }

    @GetMapping("/finance-decision-handler")
    fun getFinanceDecisionHandlers(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): List<Employee> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.READ_FINANCE_DECISION_HANDLERS,
                    )
                    it.getFinanceDecisionHandlers()
                }
            }
            .map { if (it.active) it else it.copy(lastName = "${it.lastName} (deaktivoitu)") }
            .sortedBy { it.email }
            .also { Audit.EmployeesRead.log() }
    }

    @PutMapping("/{id}/global-roles")
    fun updateEmployeeGlobalRoles(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: EmployeeId,
        @RequestBody body: List<UserRole>,
    ) {
        body.forEach { role ->
            if (!role.isGlobalRole()) {
                throw BadRequest("Role $role is not a global role")
            }
        }

        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Employee.UPDATE_GLOBAL_ROLES,
                    id,
                )

                if (body.contains(UserRole.ADMIN)) {
                    val employee = it.getEmployee(id) ?: throw NotFound("employee $id not found")
                    if (!canSetAsAdmin(employee.externalId?.toString(), employee.hasSsn)) {
                        throw BadRequest("Cannot set ADMIN role for this employee")
                    }
                }

                it.updateEmployeeGlobalRoles(id = id, globalRoles = body)
            }
        }
        Audit.EmployeeUpdateGlobalRoles.log(
            targetId = AuditId(id),
            meta = mapOf("globalRoles" to body),
        )
    }

    data class UpsertEmployeeDaycareRolesRequest(
        val daycareIds: List<DaycareId>,
        val role: UserRole,
        val startDate: LocalDate,
        val endDate: LocalDate?,
    ) {
        init {
            if (!role.isUnitScopedRole()) {
                throw BadRequest("Role is not a scoped role")
            }
        }
    }

    @PutMapping("/{id}/daycare-roles")
    fun upsertEmployeeDaycareRoles(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: EmployeeId,
        @RequestBody body: UpsertEmployeeDaycareRolesRequest,
    ) {
        if (body.daycareIds.isEmpty()) {
            throw BadRequest("No daycare IDs provided")
        }
        if (body.startDate.isBefore(clock.today())) {
            throw BadRequest("Start date cannot be in the past")
        }
        if (body.endDate != null && body.endDate.isBefore(body.startDate)) {
            throw BadRequest("End date cannot be before start date")
        }
        if (user.id == id) throw Forbidden("Cannot modify own roles")

        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Employee.UPDATE_DAYCARE_ROLES,
                    id,
                )

                if (body.startDate == clock.today()) {
                    it.upsertEmployeeDaycareRoles(id, body.daycareIds, body.role, body.endDate)
                    it.upsertEmployeeMessageAccount(id)
                } else {
                    it.insertScheduledDaycareAclRow(
                        id,
                        body.daycareIds,
                        body.role,
                        body.startDate,
                        body.endDate,
                    )
                }
            }
        }
        Audit.EmployeeUpdateDaycareRoles.log(
            targetId = AuditId(id),
            meta = mapOf("daycareIds" to body.daycareIds, "role" to body.role),
        )
    }

    @DeleteMapping("/{id}/daycare-roles")
    fun deleteEmployeeDaycareRoles(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: EmployeeId,
        @RequestParam daycareId: DaycareId?,
    ) {
        if (user.id == id) throw Forbidden("Cannot modify own roles")
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Employee.DELETE_DAYCARE_ROLES,
                    id,
                )

                tx.deleteEmployeeDaycareRoles(id, daycareId)
                if (daycareId == null) {
                    tx.deleteScheduledDaycareAclRows(id)
                }

                deactivatePersonalMessageAccountIfNeeded(tx, id)
            }
        }
        Audit.EmployeeDeleteDaycareRoles.log(
            targetId = AuditId(id),
            meta = mapOf("daycareId" to daycareId),
        )
    }

    @DeleteMapping("/{id}/scheduled-daycare-role")
    fun deleteEmployeeScheduledDaycareRole(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: EmployeeId,
        @RequestParam daycareId: DaycareId,
    ) {
        if (user.id == id) throw Forbidden("Cannot modify own roles")
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Employee.DELETE_DAYCARE_ROLES,
                    id,
                )

                tx.deleteScheduledDaycareAclRow(id, daycareId)
            }
        }
        Audit.EmployeeDeleteScheduledDaycareRole.log(
            targetId = AuditId(id),
            meta = mapOf("daycareId" to daycareId),
        )
    }

    @PutMapping("/{id}/activate")
    fun activateEmployee(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: EmployeeId,
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(it, user, clock, Action.Employee.ACTIVATE, id)

                it.updateEmployeeActive(id = id, active = true)
            }
        }
        Audit.EmployeeActivate.log(targetId = AuditId(id))
    }

    @PutMapping("/{id}/deactivate")
    fun deactivateEmployee(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: EmployeeId,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(tx, user, clock, Action.Employee.DEACTIVATE, id)

                tx.deactivateEmployeeRemoveRolesAndPin(id)
            }
        }
        Audit.EmployeeDeactivate.log(targetId = AuditId(id))
    }

    @GetMapping("/{id}/details")
    fun getEmployeeDetails(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: EmployeeId,
    ): EmployeeWithDaycareRoles {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Employee.READ_DETAILS,
                        id,
                    )
                    it.getEmployeeWithRoles(id)
                } ?: throw NotFound("employee $id not found")
            }
            .let { it.copy(canSetAsAdmin = canSetAsAdmin(it.externalId, it.hasSsn)) }
            .also { Audit.EmployeeRead.log(targetId = AuditId(id)) }
    }

    @DeleteMapping("/{id}")
    fun deleteEmployee(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: EmployeeId,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(tx, user, clock, Action.Employee.DELETE, id)
                tx.getEmployee(id)?.also {
                    if (!it.hasSsn) throw BadRequest("Cannot delete employee without SSN")
                    if (it.lastLogin != null)
                        throw BadRequest("Cannot delete employee who has already logged in")
                } ?: throw NotFound()
                tx.deleteEmployee(id)
            }
        }
        Audit.EmployeeDelete.log(targetId = AuditId(id))
    }

    @PostMapping("/pin-code")
    fun upsertPinCode(db: Database, user: AuthenticatedUser.Employee, @RequestBody body: PinCode) {
        db.connect { dbc -> dbc.transaction { tx -> tx.upsertPinCode(user.id, body) } }
        Audit.PinCodeUpdate.log(targetId = AuditId(user.id))
    }

    @GetMapping("/pin-code/is-pin-locked")
    fun isPinLocked(db: Database, user: AuthenticatedUser.Employee): Boolean {
        return db.connect { dbc -> dbc.read { tx -> tx.isPinLocked(user.id) } }
            .also { Audit.PinCodeLockedRead.log(targetId = AuditId(user.id)) }
    }

    @PostMapping("/search")
    fun searchEmployees(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: SearchEmployeeRequest,
    ): List<EmployeeWithDaycareRoles> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.SEARCH_EMPLOYEES,
                    )
                    getEmployees(
                        tx = tx,
                        searchTerm = body.searchTerm ?: "",
                        hideDeactivated = body.hideDeactivated ?: false,
                        globalRoles =
                            body.globalRoles
                                ?.takeIf { it.isNotEmpty() }
                                ?.filter { it.isGlobalRole() }
                                ?.toSet(),
                        unitRoles =
                            body.unitRoles
                                ?.takeIf { it.isNotEmpty() }
                                ?.filter { it.isUnitScopedRole() }
                                ?.toSet(),
                        unitProviderTypes =
                            body.unitProviderTypes?.takeIf { it.isNotEmpty() }?.toSet(),
                    )
                }
            }
            .map { it.copy(canSetAsAdmin = canSetAsAdmin(it.externalId, it.hasSsn)) }
            .also { Audit.EmployeesRead.log() }
    }

    data class EmployeePreferredFirstName(
        val preferredFirstName: String?,
        val preferredFirstNameOptions: List<String>,
    )

    @GetMapping("/preferred-first-name")
    fun getEmployeePreferredFirstName(
        db: Database,
        user: AuthenticatedUser.Employee,
    ): EmployeePreferredFirstName {
        return db.connect { dbc ->
                dbc.read { tx ->
                    val employee = tx.getEmployee(user.id) ?: throw NotFound()
                    EmployeePreferredFirstName(
                        preferredFirstName = employee.preferredFirstName,
                        preferredFirstNameOptions = possiblePreferredFirstNames(employee),
                    )
                }
            }
            .also { Audit.EmployeePreferredFirstNameRead.log(targetId = AuditId(user.id)) }
    }

    data class EmployeeSetPreferredFirstNameUpdateRequest(val preferredFirstName: String?)

    @PostMapping("/preferred-first-name")
    fun setEmployeePreferredFirstName(
        db: Database,
        user: AuthenticatedUser.Employee,
        @RequestBody body: EmployeeSetPreferredFirstNameUpdateRequest,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                val employee = tx.getEmployee(user.id) ?: throw NotFound()
                if (body.preferredFirstName == null) {
                    tx.setEmployeePreferredFirstName(user.id, null)
                } else {
                    if (possiblePreferredFirstNames(employee).contains(body.preferredFirstName)) {
                        tx.setEmployeePreferredFirstName(user.id, body.preferredFirstName)
                    } else {
                        throw NotFound("Given preferred first name is not allowed")
                    }
                }
            }
        }
        Audit.EmployeePreferredFirstNameUpdate.log(targetId = AuditId(user.id))
    }

    data class CreateSsnEmployeeResponse(val id: EmployeeId)

    @PostMapping("/create-with-ssn")
    fun createSsnEmployee(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: NewSsnEmployee,
    ): CreateSsnEmployeeResponse =
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.CREATE_EMPLOYEE,
                    )
                    val id =
                        try {
                            tx.createEmployeeWithSsn(body)
                        } catch (e: Exception) {
                            throw mapPSQLException(e)
                        }
                    CreateSsnEmployeeResponse(id)
                }
            }
            .also { Audit.EmployeeCreate.log(targetId = AuditId(it.id)) }

    data class EmployeeEmailRequest(val email: String?)

    @PostMapping("/{id}/update-email")
    fun updateEmployeeEmail(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: EmployeeId,
        @RequestBody body: EmployeeEmailRequest,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Employee.UPDATE_EMAIL,
                    id,
                )
                tx.getEmployee(id)?.also {
                    if (!it.hasSsn) throw BadRequest("Cannot update email of employee without SSN")
                } ?: throw NotFound()
                tx.updateEmployeeEmail(id, body.email)
            }
        }
        Audit.EmployeeEmailUpdate.log(targetId = AuditId(id))
    }

    private fun possiblePreferredFirstNames(employee: Employee): List<String> {
        val fullFirstNames = employee.firstName.split("\\s+".toRegex())
        val splitTwoPartNames =
            fullFirstNames.filter { it.contains('-') }.map { it.split("\\-+".toRegex()) }.flatten()

        return fullFirstNames + splitTwoPartNames
    }

    private fun canSetAsAdmin(externalId: String?, hasSsn: Boolean): Boolean =
        env.allowSfiAdmins || (externalId != null && !hasSsn)
}

data class PinCode(val pin: String)

data class SearchEmployeeRequest(
    val searchTerm: String?,
    val hideDeactivated: Boolean?,
    val globalRoles: Set<UserRole>?,
    val unitRoles: Set<UserRole>?,
    val unitProviderTypes: Set<ProviderType>?,
)
