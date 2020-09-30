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
import fi.espoo.evaka.pis.getEmployeeAuthenticatedUser
import fi.espoo.evaka.pis.getEmployees
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.config.Roles
import fi.espoo.evaka.shared.db.transaction
import fi.espoo.evaka.shared.domain.BadRequest
import org.jdbi.v3.core.Jdbi
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
class EmployeeController(private val jdbi: Jdbi) {

    @GetMapping()
    fun getEmployees(user: AuthenticatedUser): ResponseEntity<List<Employee>> {
        Audit.EmployeesRead.log()
        user.requireOneOfRoles(Roles.ADMIN, Roles.SERVICE_WORKER, Roles.UNIT_SUPERVISOR)
        return ResponseEntity.ok(jdbi.transaction { it.getEmployees() }.sortedBy { it.email })
    }

    @GetMapping("/{id}")
    fun getEmployee(user: AuthenticatedUser, @PathVariable(value = "id") id: UUID): ResponseEntity<Employee> {
        Audit.EmployeeRead.log(targetId = id)
        if (user.id != id) {
            user.requireOneOfRoles(
                Roles.ADMIN,
                Roles.SERVICE_WORKER,
                Roles.FINANCE_ADMIN,
                Roles.UNIT_SUPERVISOR,
                Roles.STAFF,
                Roles.DIRECTOR
            )
        }
        return jdbi.transaction { it.getEmployee(id) }?.let { ResponseEntity.ok().body(it) }
            ?: ResponseEntity.notFound().build()
    }

    @PostMapping("")
    fun createEmployee(user: AuthenticatedUser, @RequestBody employee: NewEmployee): ResponseEntity<Employee> {
        Audit.EmployeeCreate.log(targetId = employee.aad)
        user.requireOneOfRoles(Roles.ADMIN)
        return ResponseEntity.ok().body(jdbi.transaction { it.createEmployee(employee) })
    }

    @PostMapping("/identity")
    fun getOrCreateEmployee(@RequestBody employee: NewEmployee): ResponseEntity<AuthenticatedUser> {
        Audit.EmployeeGetOrCreate.log(targetId = employee.aad)
        if (employee.aad == null) {
            throw BadRequest("Cannot create or fetch employee without aad")
        }
        return ResponseEntity.ok(
            jdbi.transaction {
                it.getEmployeeAuthenticatedUser(employee.aad)
                    ?: AuthenticatedUser(it.createEmployee(employee).id, employee.roles)
            }
        )
    }

    @DeleteMapping("/{id}")
    fun deleteEmployee(user: AuthenticatedUser, @PathVariable(value = "id") id: UUID): ResponseEntity<Unit> {
        Audit.EmployeeDelete.log(targetId = id)
        user.requireOneOfRoles(Roles.ADMIN)
        jdbi.transaction { it.deleteEmployee(id) }
        return ResponseEntity.ok().build()
    }
}
