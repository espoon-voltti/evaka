// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controller

import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.pis.AbstractIntegrationTest
import fi.espoo.evaka.pis.DaycareRole
import fi.espoo.evaka.pis.controllers.EmployeeController
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.unitSupervisorOfTestDaycare
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.util.UUID

class EmployeeControllerSearchIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var controller: EmployeeController

    @BeforeEach
    internal fun setUp() {
        db.transaction { tx ->
            insertGeneralTestFixtures(tx.handle)
        }
    }

    @Test
    fun `admin searches employees`() {
        val user = AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.ADMIN))
        val response = controller.searchEmployees(db, user, page = 1, pageSize = 3, searchTerm = null)
        assertEquals(HttpStatus.OK, response.statusCode)
        val body = response.body ?: fail("missing body")
        assertEquals(3, body.total)
        assertEquals(1, body.pages)

        val decisionMaker = body.data.find { it.id == testDecisionMaker_1.id } ?: fail("decisionMaker not found")
        assertEquals(listOf(UserRole.SERVICE_WORKER), decisionMaker.globalRoles)
        assertEquals(0, decisionMaker.daycareRoles.size)

        val supervisor = body.data.find { it.id == unitSupervisorOfTestDaycare.id } ?: fail("supervisor not found")
        assertEquals(0, supervisor.globalRoles.size)
        assertEquals(
            listOf(DaycareRole(daycareId = testDaycare.id, daycareName = testDaycare.name, role = UserRole.UNIT_SUPERVISOR)),
            supervisor.daycareRoles
        )
    }

    @Test
    fun `admin searches employees with free text`() {
        val user = AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.ADMIN))
        val response = controller.searchEmployees(db, user, page = 1, pageSize = 10, searchTerm = "super")
        assertEquals(HttpStatus.OK, response.statusCode)
        val body = response.body ?: fail("missing body")
        assertEquals(1, body.data.size)
        assertEquals("Sammy", body.data[0].firstName)
        assertEquals("Supervisor", body.data[0].lastName)
    }
}
