// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.pis.Employee
import fi.espoo.evaka.pis.EmployeeWithDaycareRoles
import fi.espoo.evaka.pis.NewEmployee
import fi.espoo.evaka.pis.createEmployee
import fi.espoo.evaka.pis.deleteEmployee
import fi.espoo.evaka.pis.getEmployee
import fi.espoo.evaka.pis.getEmployeeWithRoles
import fi.espoo.evaka.pis.getEmployees
import fi.espoo.evaka.pis.getEmployeesPaged
import fi.espoo.evaka.pis.getFinanceDecisionHandlers
import fi.espoo.evaka.pis.isPinLocked
import fi.espoo.evaka.pis.updateEmployee
import fi.espoo.evaka.pis.upsertPinCode
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
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
    fun getEmployees(db: Database, user: AuthenticatedUser): List<Employee> {
        Audit.EmployeesRead.log()
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR)
        return db.connect { dbc -> dbc.read { it.getEmployees() } }.sortedBy { it.email }
    }

    @GetMapping("/finance-decision-handler")
    fun getFinanceDecisionHandlers(db: Database, user: AuthenticatedUser): List<Employee> {
        Audit.EmployeesRead.log()
        accessControl.requirePermissionFor(user, Action.Global.READ_FINANCE_DECISION_HANDLERS)
        return db.connect { dbc -> dbc.read { it.getFinanceDecisionHandlers() } }.sortedBy { it.email }
    }

    @GetMapping("/{id}")
    fun getEmployee(db: Database, user: AuthenticatedUser.Employee, @PathVariable(value = "id") id: EmployeeId): Employee {
        Audit.EmployeeRead.log(targetId = id)
        if (user.id != id) {
            @Suppress("DEPRECATION")
            user.requireOneOfRoles(
                UserRole.ADMIN,
                UserRole.SERVICE_WORKER,
                UserRole.FINANCE_ADMIN,
                UserRole.UNIT_SUPERVISOR,
                UserRole.STAFF,
                UserRole.DIRECTOR,
                UserRole.REPORT_VIEWER,
            )
        }
        return db.connect { dbc -> dbc.read { it.getEmployee(id) } } ?: throw NotFound()
    }

    data class EmployeeUpdate(
        val globalRoles: List<UserRole>
    )
    @PutMapping("/{id}")
    fun updateEmployee(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable(value = "id") id: EmployeeId,
        @RequestBody body: EmployeeUpdate
    ) {
        Audit.EmployeeUpdate.log(targetId = id, objectId = body.globalRoles)
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.ADMIN)

        db.connect { dbc ->
            dbc.transaction {
                it.updateEmployee(
                    id = id,
                    globalRoles = body.globalRoles
                )
            }
        }
    }

    @GetMapping("/{id}/details")
    fun getEmployeeDetails(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable(value = "id") id: EmployeeId
    ): EmployeeWithDaycareRoles {
        Audit.EmployeeRead.log(targetId = id)
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.ADMIN)

        return db.connect { dbc -> dbc.read { it.getEmployeeWithRoles(id) } }
            ?: throw NotFound("employee $id not found")
    }

    @PostMapping("")
    fun createEmployee(db: Database, user: AuthenticatedUser, @RequestBody employee: NewEmployee): Employee {
        Audit.EmployeeCreate.log(targetId = employee.externalId)
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.ADMIN)
        return db.connect { dbc -> dbc.transaction { it.createEmployee(employee) } }
    }

    @DeleteMapping("/{id}")
    fun deleteEmployee(db: Database, user: AuthenticatedUser, @PathVariable(value = "id") id: EmployeeId) {
        Audit.EmployeeDelete.log(targetId = id)
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.ADMIN)
        db.connect { dbc -> dbc.transaction { it.deleteEmployee(id) } }
    }

    @PostMapping("/pin-code")
    fun upsertPinCode(
        db: Database,
        user: AuthenticatedUser.Employee,
        @RequestBody body: PinCode
    ) {
        Audit.PinCodeUpdate.log(targetId = user.id)
        db.connect { dbc -> dbc.transaction { tx -> tx.upsertPinCode(user.id, body) } }
    }

    @GetMapping("/pin-code/is-pin-locked")
    fun isPinLocked(
        db: Database,
        user: AuthenticatedUser.Employee
    ): Boolean {
        Audit.PinCodeLockedRead.log(targetId = user.id)
        return db.connect { dbc -> dbc.read { tx -> tx.isPinLocked(user.id) } }
    }

    @PostMapping("/search")
    fun searchEmployees(
        db: Database,
        user: AuthenticatedUser,
        @RequestBody body: SearchEmployeeRequest
    ): Paged<EmployeeWithDaycareRoles> {
        Audit.EmployeesRead.log()
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.ADMIN)
        return db.connect { dbc ->
            dbc.read { tx ->
                getEmployeesPaged(tx, body.page ?: 1, (body.pageSize ?: 50).coerceAtMost(100), body.searchTerm ?: "")
            }
        }
    }
}

data class PinCode(val pin: String)

data class SearchEmployeeRequest(
    val page: Int?,
    val pageSize: Int?,
    val searchTerm: String?,
)
