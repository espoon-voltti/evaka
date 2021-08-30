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
import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.NotFound
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/employee")
class EmployeeController {

    @GetMapping
    fun getEmployees(db: Database.Connection, user: AuthenticatedUser): ResponseEntity<List<Employee>> {
        Audit.EmployeesRead.log()
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR)
        return ResponseEntity.ok(db.read { it.getEmployees() }.sortedBy { it.email })
    }

    @GetMapping("/finance-decision-handler")
    fun getFinanceDecisionHandlers(db: Database.Connection, user: AuthenticatedUser): ResponseEntity<List<Employee>> {
        Audit.EmployeesRead.log()
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR, UserRole.FINANCE_ADMIN)
        return ResponseEntity.ok(db.read { it.getFinanceDecisionHandlers() }.sortedBy { it.email })
    }

    @GetMapping("/{id}")
    fun getEmployee(db: Database.Connection, user: AuthenticatedUser, @PathVariable(value = "id") id: UUID): ResponseEntity<Employee> {
        Audit.EmployeeRead.log(targetId = id)
        if (user.id != id) {
            user.requireOneOfRoles(
                UserRole.ADMIN,
                UserRole.SERVICE_WORKER,
                UserRole.FINANCE_ADMIN,
                UserRole.UNIT_SUPERVISOR,
                UserRole.STAFF,
                UserRole.DIRECTOR
            )
        }
        return db.read { it.getEmployee(id) }?.let { ResponseEntity.ok().body(it) }
            ?: ResponseEntity.notFound().build()
    }

    data class EmployeeUpdate(
        val globalRoles: List<UserRole>
    )
    @PutMapping("/{id}")
    fun updateEmployee(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable(value = "id") id: UUID,
        @RequestBody body: EmployeeUpdate
    ): ResponseEntity<Unit> {
        Audit.EmployeeUpdate.log(targetId = id, objectId = body.globalRoles)
        user.requireOneOfRoles(UserRole.ADMIN)

        db.transaction {
            it.updateEmployee(
                id = id,
                globalRoles = body.globalRoles
            )
        }

        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{id}/details")
    fun getEmployeeDetails(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable(value = "id") id: UUID
    ): ResponseEntity<EmployeeWithDaycareRoles> {
        Audit.EmployeeRead.log(targetId = id)
        user.requireOneOfRoles(UserRole.ADMIN)

        return db.read { it.getEmployeeWithRoles(id) }
            ?.let { ResponseEntity.ok().body(it) }
            ?: throw NotFound("employee $id not found")
    }

    @PostMapping("")
    fun createEmployee(db: Database.Connection, user: AuthenticatedUser, @RequestBody employee: NewEmployee): ResponseEntity<Employee> {
        Audit.EmployeeCreate.log(targetId = employee.externalId)
        user.requireOneOfRoles(UserRole.ADMIN)
        return ResponseEntity.ok().body(db.transaction { it.createEmployee(employee) })
    }

    @DeleteMapping("/{id}")
    fun deleteEmployee(db: Database.Connection, user: AuthenticatedUser, @PathVariable(value = "id") id: UUID): ResponseEntity<Unit> {
        Audit.EmployeeDelete.log(targetId = id)
        user.requireOneOfRoles(UserRole.ADMIN)
        db.transaction { it.deleteEmployee(id) }
        return ResponseEntity.ok().build()
    }

    @PostMapping("/pin-code")
    fun upsertPinCode(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestBody body: PinCode
    ): ResponseEntity<Unit> {
        Audit.PinCodeUpdate.log(targetId = user.id)
        return db.transaction { tx ->
            tx.upsertPinCode(
                user.id, body
            )
        }.let {
            ResponseEntity.noContent().build()
        }
    }

    @GetMapping("/pin-code/is-pin-locked")
    fun isPinLocked(
        db: Database.Connection,
        user: AuthenticatedUser
    ): Boolean {
        Audit.PinCodeLockedRead.log(targetId = user.id)
        return db.read { tx ->
            tx.isPinLocked(user.id)
        }
    }

    @PostMapping("/search")
    fun searchEmployees(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestBody body: SearchEmployeeRequest
    ): ResponseEntity<Paged<EmployeeWithDaycareRoles>> {
        Audit.EmployeesRead.log()
        user.requireOneOfRoles(UserRole.ADMIN)
        return db.read { tx ->
            getEmployeesPaged(tx, body.page ?: 1, (body.pageSize ?: 50).coerceAtMost(100), body.searchTerm ?: "")
        }.let { ResponseEntity.ok(it) }
    }
}

data class PinCode(val pin: String)

data class SearchEmployeeRequest(
    val page: Int?,
    val pageSize: Int?,
    val searchTerm: String?,
)
