// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controller

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.identity.ExternalId
import fi.espoo.evaka.pis.Employee
import fi.espoo.evaka.pis.NewEmployee
import fi.espoo.evaka.pis.controllers.EmployeeController
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

class EmployeeControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {

    @Autowired
    lateinit var employeeController: EmployeeController

    @Test
    fun `no employees return empty list`() {
        val user = AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.ADMIN))
        val employees = employeeController.getEmployees(Database(jdbi), user)
        assertEquals(listOf(), employees)
    }

    @Test
    fun `admin can add employee`() {
        val user = AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.ADMIN))
        val emp = employeeController.createEmployee(Database(jdbi), user, requestFromEmployee(employee1))
        assertEquals(employee1.firstName, emp.firstName)
        assertEquals(employee1.lastName, emp.lastName)
        assertEquals(employee1.email, emp.email)
        assertEquals(employee1.externalId, emp.externalId)
    }

    @Test
    fun `admin gets all employees`() {
        val user = AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.ADMIN))
        employeeController.createEmployee(Database(jdbi), user, requestFromEmployee(employee1))
        employeeController.createEmployee(Database(jdbi), user, requestFromEmployee(employee2))
        val employees = employeeController.getEmployees(Database(jdbi), user)
        assertEquals(2, employees.size)
        assertEquals(employee1.email, employees[0].email)
        assertEquals(employee2.email, employees[1].email)
    }

    @Test
    fun `admin can first create, then get employee`() {
        val user = AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.ADMIN))
        assertEquals(0, employeeController.getEmployees(Database(jdbi), user).size)

        val responseCreate = employeeController.createEmployee(Database(jdbi), user, requestFromEmployee(employee1))
        assertEquals(1, employeeController.getEmployees(Database(jdbi), user).size)

        val responseGet = employeeController.getEmployee(Database(jdbi), user, responseCreate.id)
        assertEquals(1, employeeController.getEmployees(Database(jdbi), user).size)
        assertEquals(responseCreate.id, responseGet.id)
    }

    @Test
    fun `admin can delete employee`() {
        val user = AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.ADMIN))
        val body = employeeController.createEmployee(Database(jdbi), user, requestFromEmployee(employee1))

        employeeController.deleteEmployee(Database(jdbi), user, body.id)

        assertEquals(0, employeeController.getEmployees(Database(jdbi), user).size)
    }

    fun requestFromEmployee(employee: Employee) = NewEmployee(
        email = employee.email,
        firstName = employee.firstName,
        lastName = employee.lastName,
        externalId = employee.externalId
    )

    val employee1 = Employee(
        email = "employee1@espoo.fi",
        firstName = "etunimi1",
        lastName = "sukunimi1",
        externalId = ExternalId.of(namespace = "espoo-ad", value = UUID.randomUUID().toString()),
        created = Instant.now(),
        updated = Instant.now(),
        id = EmployeeId(UUID.randomUUID())
    )

    val employee2 = Employee(
        email = "employee2@espoo.fi",
        firstName = "etunimi2",
        lastName = "sukunimi2",
        externalId = ExternalId.of(namespace = "espoo-ad", value = UUID.randomUUID().toString()),
        created = Instant.now(),
        updated = Instant.now(),
        id = EmployeeId(UUID.randomUUID())
    )
}
