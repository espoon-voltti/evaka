// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controller

import fi.espoo.evaka.pis.AbstractIntegrationTest
import fi.espoo.evaka.pis.Employee
import fi.espoo.evaka.pis.NewEmployee
import fi.espoo.evaka.pis.controllers.EmployeeController
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.config.Roles
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.time.Instant
import java.util.UUID

class EmployeeControllerIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var employeeController: EmployeeController

    @Test
    fun `no employees return empty list`() {
        val user = AuthenticatedUser(UUID.randomUUID(), setOf(Roles.ADMIN))
        val response = employeeController.getEmployees(db, user)
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(emptyList<Employee>(), response.body)
    }

    @Test
    fun `admin can add employee`() {
        val user = AuthenticatedUser(UUID.randomUUID(), setOf(Roles.ADMIN))
        val response = employeeController.createEmployee(db, user, requestFromEmployee(employee1))
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(employee1.firstName, response.body!!.firstName)
        assertEquals(employee1.lastName, response.body!!.lastName)
        assertEquals(employee1.email, response.body!!.email)
        assertEquals(employee1.aad, response.body!!.aad)
    }

    @Test
    fun `admin gets all employees`() {
        val user = AuthenticatedUser(UUID.randomUUID(), setOf(Roles.ADMIN))
        assertEquals(HttpStatus.OK, employeeController.createEmployee(db, user, requestFromEmployee(employee1)).statusCode)
        assertEquals(HttpStatus.OK, employeeController.createEmployee(db, user, requestFromEmployee(employee2)).statusCode)
        val response = employeeController.getEmployees(db, user)
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(2, response.body?.size)
        assertEquals(employee1.email, response.body?.get(0)?.email)
        assertEquals(employee2.email, response.body?.get(1)?.email)
    }

    @Test
    fun `admin can first create, then get employee`() {
        val user = AuthenticatedUser(UUID.randomUUID(), setOf(Roles.ADMIN))
        assertEquals(0, employeeController.getEmployees(db, user).body?.size)

        val responseCreate = employeeController.createEmployee(db, user, requestFromEmployee(employee1))
        assertEquals(HttpStatus.OK, responseCreate.statusCode)
        assertEquals(1, employeeController.getEmployees(db, user).body?.size)

        val responseGet = employeeController.getEmployee(db, user, responseCreate.body.id)
        assertEquals(HttpStatus.OK, responseGet.statusCode)
        assertEquals(1, employeeController.getEmployees(db, user).body?.size)
        assertEquals(responseCreate.body?.id, responseGet.body?.id)
    }

    @Test
    fun `admin can delete employee`() {
        val user = AuthenticatedUser(UUID.randomUUID(), setOf(Roles.ADMIN))
        val createdEmployeeResponse = employeeController.createEmployee(db, user, requestFromEmployee(employee1))
        assertEquals(HttpStatus.OK, createdEmployeeResponse.statusCode)
        val response = employeeController.deleteEmployee(db, user, createdEmployeeResponse.body?.id!!)
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(0, employeeController.getEmployees(db, user).body?.size)
    }

    fun requestFromEmployee(employee: Employee) = NewEmployee(
        email = employee.email,
        firstName = employee.firstName,
        lastName = employee.lastName,
        aad = employee.aad
    )

    val employee1 = Employee(
        email = "employee1@espoo.fi",
        firstName = "etunimi1",
        lastName = "sukunimi1",
        aad = UUID.randomUUID(),
        created = Instant.now(),
        updated = Instant.now(),
        id = UUID.randomUUID()
    )

    val employee2 = Employee(
        email = "employee2@espoo.fi",
        firstName = "etunimi2",
        lastName = "sukunimi2",
        aad = UUID.randomUUID(),
        created = Instant.now(),
        updated = Instant.now(),
        id = UUID.randomUUID()
    )
}
