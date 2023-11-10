// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.pis.Employee
import fi.espoo.evaka.pis.EmployeeWithDaycareRoles
import fi.espoo.evaka.pis.NewEmployee
import fi.espoo.evaka.pis.PagedEmployeesWithDaycareRoles
import fi.espoo.evaka.pis.createEmployee
import fi.espoo.evaka.pis.deleteEmployee
import fi.espoo.evaka.pis.getEmployee
import fi.espoo.evaka.pis.getEmployeeWithRoles
import fi.espoo.evaka.pis.getEmployees
import fi.espoo.evaka.pis.getEmployeesPaged
import fi.espoo.evaka.pis.getFinanceDecisionHandlers
import fi.espoo.evaka.pis.isPinLocked
import fi.espoo.evaka.pis.setEmployeePreferredFirstName
import fi.espoo.evaka.pis.updateEmployee
import fi.espoo.evaka.pis.updateEmployeeActive
import fi.espoo.evaka.pis.upsertPinCode
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/employee")
class EmployeeController(private val accessControl: AccessControl) {

    @GetMapping
    fun getEmployees(db: Database, user: AuthenticatedUser, clock: EvakaClock): List<Employee> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.READ_EMPLOYEES
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
        user: AuthenticatedUser,
        clock: EvakaClock
    ): List<Employee> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.READ_FINANCE_DECISION_HANDLERS
                    )
                    it.getFinanceDecisionHandlers()
                }
            }
            .map { if (it.active) it else it.copy(lastName = "${it.lastName} (deaktivoitu)") }
            .sortedBy { it.email }
            .also { Audit.EmployeesRead.log() }
    }

    @GetMapping("/{id}")
    fun getEmployee(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable(value = "id") id: EmployeeId
    ): Employee {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(it, user, clock, Action.Employee.READ, id)
                    it.getEmployee(id)
                } ?: throw NotFound()
            }
            .also { Audit.EmployeeRead.log(targetId = id) }
    }

    data class EmployeeUpdate(val globalRoles: List<UserRole>)

    @PutMapping("/{id}")
    fun updateEmployee(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable(value = "id") id: EmployeeId,
        @RequestBody body: EmployeeUpdate
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(it, user, clock, Action.Employee.UPDATE, id)

                it.updateEmployee(id = id, globalRoles = body.globalRoles)
            }
        }
        Audit.EmployeeUpdate.log(targetId = id, meta = mapOf("globalRoles" to body.globalRoles))
    }

    @PutMapping("/{id}/activate")
    fun activateEmployee(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable(value = "id") id: EmployeeId
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(it, user, clock, Action.Employee.ACTIVATE, id)

                it.updateEmployeeActive(id = id, active = true)
            }
        }
        Audit.EmployeeActivate.log(targetId = id)
    }

    @PutMapping("/{id}/deactivate")
    fun deactivateEmployee(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable(value = "id") id: EmployeeId
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(it, user, clock, Action.Employee.DEACTIVATE, id)

                it.updateEmployeeActive(id = id, active = false)
            }
        }
        Audit.EmployeeDeactivate.log(targetId = id)
    }

    @GetMapping("/{id}/details")
    fun getEmployeeDetails(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable(value = "id") id: EmployeeId
    ): EmployeeWithDaycareRoles {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Employee.READ_DETAILS,
                        id
                    )
                    it.getEmployeeWithRoles(id)
                } ?: throw NotFound("employee $id not found")
            }
            .also { Audit.EmployeeRead.log(targetId = id) }
    }

    @PostMapping("")
    fun createEmployee(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestBody employee: NewEmployee
    ): Employee {
        return db.connect { dbc ->
                dbc.transaction {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.CREATE_EMPLOYEE
                    )
                    it.createEmployee(employee)
                }
            }
            .also { Audit.EmployeeCreate.log(targetId = it.id) }
    }

    @DeleteMapping("/{id}")
    fun deleteEmployee(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable(value = "id") id: EmployeeId
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(it, user, clock, Action.Employee.DELETE, id)
                it.deleteEmployee(id)
            }
        }
        Audit.EmployeeDelete.log(targetId = id)
    }

    @PostMapping("/pin-code")
    fun upsertPinCode(db: Database, user: AuthenticatedUser.Employee, @RequestBody body: PinCode) {
        db.connect { dbc -> dbc.transaction { tx -> tx.upsertPinCode(user.id, body) } }
        Audit.PinCodeUpdate.log(targetId = user.id)
    }

    @GetMapping("/pin-code/is-pin-locked")
    fun isPinLocked(db: Database, user: AuthenticatedUser.Employee): Boolean {
        return db.connect { dbc -> dbc.read { tx -> tx.isPinLocked(user.id) } }
            .also { Audit.PinCodeLockedRead.log(targetId = user.id) }
    }

    @PostMapping("/search")
    fun searchEmployees(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestBody body: SearchEmployeeRequest
    ): PagedEmployeesWithDaycareRoles {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.SEARCH_EMPLOYEES
                    )
                    getEmployeesPaged(
                        tx,
                        body.page ?: 1,
                        (body.pageSize ?: 50).coerceAtMost(100),
                        body.searchTerm ?: ""
                    )
                }
            }
            .also { Audit.EmployeesRead.log() }
    }

    data class EmployeePreferredFirstName(
        val preferredFirstName: String?,
        val preferredFirstNameOptions: List<String>
    )

    @GetMapping("/preferred-first-name")
    fun getEmployeePreferredFirstName(
        db: Database,
        user: AuthenticatedUser.Employee
    ): EmployeePreferredFirstName {
        return db.connect { dbc ->
                dbc.read { tx ->
                    val employee = tx.getEmployee(user.id) ?: throw NotFound()
                    EmployeePreferredFirstName(
                        preferredFirstName = employee.preferredFirstName,
                        preferredFirstNameOptions = possiblePreferredFirstNames(employee)
                    )
                }
            }
            .also { Audit.EmployeePreferredFirstNameRead.log(targetId = user.id) }
    }

    data class EmployeeSetPreferredFirstNameUpdateRequest(val preferredFirstName: String?)

    @PostMapping("/preferred-first-name")
    fun setEmployeePreferredFirstName(
        db: Database,
        user: AuthenticatedUser.Employee,
        @RequestBody body: EmployeeSetPreferredFirstNameUpdateRequest
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
        Audit.EmployeePreferredFirstNameUpdate.log(targetId = user.id)
    }

    private fun possiblePreferredFirstNames(employee: Employee): List<String> {
        val fullFirstNames = employee.firstName.split("\\s+".toRegex())
        val splitTwoPartNames =
            fullFirstNames.filter { it.contains('-') }.map { it.split("\\-+".toRegex()) }.flatten()

        return fullFirstNames + splitTwoPartNames
    }
}

data class PinCode(val pin: String)

data class SearchEmployeeRequest(val page: Int?, val pageSize: Int?, val searchTerm: String?)
