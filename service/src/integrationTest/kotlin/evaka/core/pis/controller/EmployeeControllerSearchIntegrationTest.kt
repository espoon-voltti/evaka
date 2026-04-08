// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis.controller

import evaka.core.FullApplicationTest
import evaka.core.daycare.domain.ProviderType
import evaka.core.pis.DaycareRole
import evaka.core.pis.controllers.EmployeeController
import evaka.core.pis.controllers.SearchEmployeeRequest
import evaka.core.shared.EmployeeId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.auth.UserRole
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.RealEvakaClock
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.springframework.beans.factory.annotation.Autowired

class EmployeeControllerSearchIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {

    @Autowired lateinit var controller: EmployeeController

    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val serviceWorker = DevEmployee(roles = setOf(UserRole.SERVICE_WORKER))
    private val employee2 = DevEmployee()
    private val employee3 = DevEmployee()
    private val supervisor = DevEmployee(firstName = "Sammy", lastName = "Supervisor")

    @BeforeEach
    fun setUp() {
        db.transaction { tx ->
            tx.insert(serviceWorker)
            tx.insert(employee2)
            tx.insert(employee3)
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(supervisor, mapOf(daycare.id to UserRole.UNIT_SUPERVISOR))
        }
    }

    @Test
    fun `admin searches employees`() {
        val user = AuthenticatedUser.Employee(EmployeeId(UUID.randomUUID()), setOf(UserRole.ADMIN))
        val body =
            controller.searchEmployees(
                dbInstance(),
                user,
                RealEvakaClock(),
                SearchEmployeeRequest(
                    searchTerm = null,
                    hideDeactivated = false,
                    globalRoles = emptySet(),
                    unitRoles = emptySet(),
                    unitProviderTypes = emptySet(),
                ),
            )

        assertEquals(4, body.size)

        val decisionMaker =
            body.find { it.id == serviceWorker.id } ?: fail("serviceWorker not found")
        assertEquals(listOf(UserRole.SERVICE_WORKER), decisionMaker.globalRoles)
        assertEquals(0, decisionMaker.daycareRoles.size)

        val foundSupervisor = body.find { it.id == supervisor.id } ?: fail("supervisor not found")
        assertEquals(0, foundSupervisor.globalRoles.size)
        assertEquals(
            listOf(
                DaycareRole(
                    daycareId = daycare.id,
                    daycareName = daycare.name,
                    role = UserRole.UNIT_SUPERVISOR,
                    endDate = null,
                )
            ),
            foundSupervisor.daycareRoles,
        )
    }

    @Test
    fun `admin searches employees with free text`() {
        val user = AuthenticatedUser.Employee(EmployeeId(UUID.randomUUID()), setOf(UserRole.ADMIN))
        val body =
            controller.searchEmployees(
                dbInstance(),
                user,
                RealEvakaClock(),
                SearchEmployeeRequest(
                    searchTerm = "super",
                    hideDeactivated = false,
                    globalRoles = emptySet(),
                    unitRoles = emptySet(),
                    unitProviderTypes = emptySet(),
                ),
            )
        assertEquals(1, body.size)
        assertEquals("Sammy", body[0].firstName)
        assertEquals("Supervisor", body[0].lastName)
    }

    @Test
    fun `admin searches employees with global roles`() {
        val user = AuthenticatedUser.Employee(EmployeeId(UUID.randomUUID()), setOf(UserRole.ADMIN))
        val body =
            controller.searchEmployees(
                dbInstance(),
                user,
                RealEvakaClock(),
                SearchEmployeeRequest(
                    searchTerm = null,
                    hideDeactivated = false,
                    globalRoles = setOf(UserRole.SERVICE_WORKER),
                    unitRoles = emptySet(),
                    unitProviderTypes = emptySet(),
                ),
            )
        assertEquals(1, body.size)
        assertTrue { body.all { it.globalRoles.toSet().contains(UserRole.SERVICE_WORKER) } }
    }

    @Test
    fun `admin searches employees with unit roles`() {
        val user = AuthenticatedUser.Employee(EmployeeId(UUID.randomUUID()), setOf(UserRole.ADMIN))
        val body =
            controller.searchEmployees(
                dbInstance(),
                user,
                RealEvakaClock(),
                SearchEmployeeRequest(
                    searchTerm = null,
                    hideDeactivated = false,
                    globalRoles = emptySet(),
                    unitRoles = setOf(UserRole.UNIT_SUPERVISOR),
                    unitProviderTypes = emptySet(),
                ),
            )
        assertEquals(1, body.size)
        assertTrue {
            body.all {
                it.daycareRoles.map { role -> role.role }.toSet().contains(UserRole.UNIT_SUPERVISOR)
            }
        }
    }

    @Test
    fun `admin searches employees with unit roles and provider types`() {
        val user = AuthenticatedUser.Employee(EmployeeId(UUID.randomUUID()), setOf(UserRole.ADMIN))
        val body1 =
            controller.searchEmployees(
                dbInstance(),
                user,
                RealEvakaClock(),
                SearchEmployeeRequest(
                    searchTerm = null,
                    hideDeactivated = false,
                    globalRoles = emptySet(),
                    unitRoles = setOf(UserRole.UNIT_SUPERVISOR),
                    unitProviderTypes = setOf(ProviderType.MUNICIPAL),
                ),
            )
        assertEquals(1, body1.size)
        assertTrue {
            body1.all {
                it.daycareRoles.map { role -> role.role }.toSet().contains(UserRole.UNIT_SUPERVISOR)
            }
        }
        val body2 =
            controller.searchEmployees(
                dbInstance(),
                user,
                RealEvakaClock(),
                SearchEmployeeRequest(
                    searchTerm = null,
                    hideDeactivated = false,
                    globalRoles = emptySet(),
                    unitRoles = setOf(UserRole.UNIT_SUPERVISOR),
                    unitProviderTypes = setOf(ProviderType.PURCHASED),
                ),
            )
        assertEquals(0, body2.size)
    }
}
