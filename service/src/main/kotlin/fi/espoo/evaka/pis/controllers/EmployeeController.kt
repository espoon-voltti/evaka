// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.pis.Employee
import fi.espoo.evaka.pis.NewEmployee
import fi.espoo.evaka.pis.createEmployee
import fi.espoo.evaka.pis.deleteEmployee
import fi.espoo.evaka.pis.getEmployee
import fi.espoo.evaka.pis.getEmployees
import fi.espoo.evaka.pis.getFinanceDecisionHandlers
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
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
        return ResponseEntity.ok(db.read { it.handle.getEmployees() }.sortedBy { it.email })
    }

    @GetMapping("/finance-decision-handler")
    fun getFinanceDecisionHandlers(db: Database.Connection, user: AuthenticatedUser): ResponseEntity<List<Employee>> {
        Audit.EmployeesRead.log()
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR, UserRole.FINANCE_ADMIN)
        return ResponseEntity.ok(db.read { it.handle.getFinanceDecisionHandlers() }.sortedBy { it.email })
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
        return db.read { it.handle.getEmployee(id) }?.let { ResponseEntity.ok().body(it) }
            ?: ResponseEntity.notFound().build()
    }

    @PostMapping("")
    fun createEmployee(db: Database.Connection, user: AuthenticatedUser, @RequestBody employee: NewEmployee): ResponseEntity<Employee> {
        Audit.EmployeeCreate.log(targetId = employee.externalId)
        user.requireOneOfRoles(UserRole.ADMIN)
        return ResponseEntity.ok().body(db.transaction { it.handle.createEmployee(employee) })
    }

    @DeleteMapping("/{id}")
    fun deleteEmployee(db: Database.Connection, user: AuthenticatedUser, @PathVariable(value = "id") id: UUID): ResponseEntity<Unit> {
        Audit.EmployeeDelete.log(targetId = id)
        user.requireOneOfRoles(UserRole.ADMIN)
        db.transaction { it.handle.deleteEmployee(id) }
        return ResponseEntity.ok().build()
    }
}
