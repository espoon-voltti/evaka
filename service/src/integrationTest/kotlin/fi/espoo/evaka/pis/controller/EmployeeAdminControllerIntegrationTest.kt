// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controller

import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.pis.AbstractIntegrationTest
import fi.espoo.evaka.pis.DaycareRole
import fi.espoo.evaka.pis.controllers.EmployeeAdminController
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.handle
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

class EmployeeAdminControllerIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var controller: EmployeeAdminController

    @BeforeEach
    override fun beforeEach() {
        jdbi.handle { h ->
            resetDatabase(h)
            insertGeneralTestFixtures(h)
        }
    }

    @Test
    fun `admin gets employees`() {
        val user = AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.ADMIN))
        val response = controller.getEmployees(db, user, page = 1, pageSize = 3)
        assertEquals(HttpStatus.OK, response.statusCode)
        val body = response.body ?: fail("missing body")
        assertEquals(3, body.total)
        assertEquals(1, body.pages)

        val decisionMaker = body.data[2]
        assertEquals(testDecisionMaker_1.id, decisionMaker.id)
        assertEquals(listOf(UserRole.SERVICE_WORKER), decisionMaker.globalRoles)
        assertEquals(0, decisionMaker.daycareRoles.size)

        val supervisor = body.data[0]
        assertEquals(unitSupervisorOfTestDaycare.id, supervisor.id)
        assertEquals(0, supervisor.globalRoles.size)
        assertEquals(
            listOf(DaycareRole(daycareId = testDaycare.id, daycareName = testDaycare.name, role = UserRole.UNIT_SUPERVISOR)),
            supervisor.daycareRoles
        )
    }
}
