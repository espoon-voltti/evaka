// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controller

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.pis.controllers.ParentshipController
import fi.espoo.evaka.pis.createParentship
import fi.espoo.evaka.pis.getParentships
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testDaycare
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class ParentshipControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var controller: ParentshipController

    private val parent = testAdult_1
    private val child = testChild_1

    private val clock = RealEvakaClock()
    private val unitSupervisorId = EmployeeId(UUID.randomUUID())

    @BeforeEach
    fun init() {
        db.transaction {
            it.insertGeneralTestFixtures()
            it.insert(DevEmployee(unitSupervisorId))
            it.insertDaycareAclRow(
                daycareId = testDaycare.id,
                employeeId = unitSupervisorId,
                role = UserRole.UNIT_SUPERVISOR
            )
            it.insert(
                DevPlacement(childId = child.id, unitId = testDaycare.id, endDate = LocalDate.now())
            )
        }
    }

    @Test
    fun `service worker can create and fetch parentships`() {
        `can create and fetch parentships`(
            AuthenticatedUser.Employee(
                EmployeeId(UUID.randomUUID()),
                setOf(UserRole.SERVICE_WORKER)
            )
        )
    }

    @Test
    fun `can add a sibling parentship and fetch parentships`() {
        `can add a sibling parentship and fetch parentships`(
            AuthenticatedUser.Employee(unitSupervisorId, setOf())
        )
    }

    @Test
    fun `finance admin can create and fetch parentships`() {
        `can create and fetch parentships`(
            AuthenticatedUser.Employee(EmployeeId(UUID.randomUUID()), setOf(UserRole.FINANCE_ADMIN))
        )
    }

    fun `can create and fetch parentships`(user: AuthenticatedUser) {
        val startDate = child.dateOfBirth
        val endDate = startDate.plusDays(200)
        val reqBody =
            ParentshipController.ParentshipRequest(parent.id, child.id, startDate, endDate)
        controller.createParentship(dbInstance(), user, clock, reqBody)

        val getResponse =
            controller.getParentships(dbInstance(), user, clock, headOfChildId = parent.id)
        with(getResponse.first().data) {
            assertNotNull(this.id)
            assertEquals(parent.id, this.headOfChildId)
            assertEquals(child, this.child)
            assertEquals(startDate, this.startDate)
            assertEquals(endDate, this.endDate)
        }

        assertEquals(
            0,
            controller.getParentships(dbInstance(), user, clock, headOfChildId = child.id).size
        )
    }

    fun `can add a sibling parentship and fetch parentships`(user: AuthenticatedUser) {
        db.transaction { tx ->
            tx.createParentship(
                child.id,
                parent.id,
                child.dateOfBirth,
                child.dateOfBirth.plusDays(200)
            )
        }

        val sibling = testChild_2
        val startDate = sibling.dateOfBirth
        val endDate = startDate.plusDays(200)
        val reqBody =
            ParentshipController.ParentshipRequest(parent.id, sibling.id, startDate, endDate)
        controller.createParentship(dbInstance(), user, clock, reqBody)

        val getResponse =
            controller.getParentships(dbInstance(), user, clock, headOfChildId = parent.id)
        assertEquals(2, getResponse.size)
        getResponse
            .map { it.data }
            .find { it.childId == sibling.id }
            .let {
                assertNotNull(it)
                assertNotNull(it.id)
                assertEquals(parent.id, it.headOfChildId)
                assertEquals(sibling.id, it.childId)
                assertEquals(startDate, it.startDate)
                assertEquals(endDate, it.endDate)
            }

        assertEquals(
            0,
            controller.getParentships(dbInstance(), user, clock, headOfChildId = child.id).size
        )
    }

    @Test
    fun `service worker can update parentships`() {
        `can update parentship duration`(
            AuthenticatedUser.Employee(
                EmployeeId(UUID.randomUUID()),
                setOf(UserRole.SERVICE_WORKER)
            )
        )
    }

    @Test
    fun `unit supervisor can update parentships`() {
        `can update parentship duration`(AuthenticatedUser.Employee(unitSupervisorId, setOf()))
    }

    @Test
    fun `finance admin can update parentships`() {
        `can update parentship duration`(
            AuthenticatedUser.Employee(EmployeeId(UUID.randomUUID()), setOf(UserRole.FINANCE_ADMIN))
        )
    }

    fun `can update parentship duration`(user: AuthenticatedUser) {
        val parentship =
            db.transaction { tx ->
                tx.createParentship(
                    child.id,
                    parent.id,
                    child.dateOfBirth.plusDays(500),
                    child.dateOfBirth.plusDays(700)
                )
                tx.createParentship(
                    child.id,
                    parent.id,
                    child.dateOfBirth,
                    child.dateOfBirth.plusDays(200)
                )
            }
        val newStartDate = child.dateOfBirth.plusDays(100)
        val newEndDate = child.dateOfBirth.plusDays(300)
        val requestBody = ParentshipController.ParentshipUpdateRequest(newStartDate, newEndDate)
        controller.updateParentship(dbInstance(), user, clock, parentship.id, requestBody)

        // child1 should have new dates
        val fetched1 = controller.getParentship(dbInstance(), user, clock, parentship.id)
        assertEquals(newStartDate, fetched1.startDate)
        assertEquals(newEndDate, fetched1.endDate)
    }

    @Test
    fun `service worker cannot delete parentships`() {
        `cannot delete parentship`(
            AuthenticatedUser.Employee(
                EmployeeId(UUID.randomUUID()),
                setOf(UserRole.SERVICE_WORKER)
            )
        )
    }

    @Test
    fun `unit supervisor cannot delete parentships`() {
        `cannot delete parentship`(AuthenticatedUser.Employee(unitSupervisorId, setOf()))
    }

    @Test
    fun `finance admin can delete parentships`() {
        `can delete parentship`(
            AuthenticatedUser.Employee(EmployeeId(UUID.randomUUID()), setOf(UserRole.FINANCE_ADMIN))
        )
    }

    fun `can delete parentship`(user: AuthenticatedUser) {
        val parentship =
            db.transaction { tx ->
                tx.createParentship(
                        child.id,
                        parent.id,
                        child.dateOfBirth,
                        child.dateOfBirth.plusDays(100)
                    )
                    .also {
                        tx.createParentship(
                            child.id,
                            parent.id,
                            child.dateOfBirth.plusDays(200),
                            child.dateOfBirth.plusDays(300)
                        )
                        assertEquals(
                            2,
                            tx.getParentships(headOfChildId = parent.id, childId = null).size
                        )
                    }
            }

        controller.deleteParentship(dbInstance(), user, clock, parentship.id)
        db.read { r ->
            assertEquals(1, r.getParentships(headOfChildId = parent.id, childId = null).size)
        }
    }

    fun `cannot delete parentship`(user: AuthenticatedUser) {
        val parentship =
            db.transaction { tx ->
                tx.createParentship(
                        child.id,
                        parent.id,
                        child.dateOfBirth,
                        child.dateOfBirth.plusDays(100)
                    )
                    .also {
                        tx.createParentship(
                            child.id,
                            parent.id,
                            child.dateOfBirth.plusDays(200),
                            child.dateOfBirth.plusDays(300)
                        )
                        assertEquals(
                            2,
                            tx.getParentships(headOfChildId = parent.id, childId = null).size
                        )
                    }
            }
        assertThrows<Forbidden> {
            controller.deleteParentship(dbInstance(), user, clock, parentship.id)
        }
    }

    @Test
    fun `error is thrown if enduser tries to get parentships`() {
        val user = AuthenticatedUser.Citizen(PersonId(UUID.randomUUID()), CitizenAuthLevel.STRONG)
        db.transaction { tx ->
            tx.createParentship(
                child.id,
                parent.id,
                child.dateOfBirth,
                child.dateOfBirth.plusDays(200)
            )
        }
        assertThrows<Forbidden> {
            controller.getParentships(dbInstance(), user, clock, headOfChildId = parent.id)
        }
    }

    @Test
    fun `error is thrown if enduser tries to update parentship`() {
        val user = AuthenticatedUser.Citizen(PersonId(UUID.randomUUID()), CitizenAuthLevel.STRONG)
        val parentship =
            db.transaction { tx ->
                tx.createParentship(
                    child.id,
                    parent.id,
                    child.dateOfBirth,
                    child.dateOfBirth.plusDays(200)
                )
            }
        val newStartDate = child.dateOfBirth.plusDays(100)
        val newEndDate = child.dateOfBirth.plusDays(300)
        val requestBody = ParentshipController.ParentshipUpdateRequest(newStartDate, newEndDate)
        assertThrows<Forbidden> {
            controller.updateParentship(dbInstance(), user, clock, parentship.id, requestBody)
        }
    }

    @Test
    fun `error is thrown if enduser tries to delete parentship`() {
        val user = AuthenticatedUser.Citizen(PersonId(UUID.randomUUID()), CitizenAuthLevel.STRONG)
        val parentship =
            db.transaction { tx ->
                tx.createParentship(
                    child.id,
                    parent.id,
                    child.dateOfBirth,
                    child.dateOfBirth.plusDays(200)
                )
            }
        assertThrows<Forbidden> {
            controller.deleteParentship(dbInstance(), user, clock, parentship.id)
        }
    }

    @Test
    fun `error is thrown if service worker tries to create a partnership with a start date before child's date of birth`() {
        val user =
            AuthenticatedUser.Employee(
                EmployeeId(UUID.randomUUID()),
                setOf(UserRole.SERVICE_WORKER)
            )
        val request =
            ParentshipController.ParentshipRequest(
                parent.id,
                child.id,
                child.dateOfBirth.minusDays(1),
                child.dateOfBirth.plusYears(1)
            )
        assertThrows<BadRequest> { controller.createParentship(dbInstance(), user, clock, request) }
    }

    @Test
    fun `error is thrown if service worker tries to create a partnership with a end date after child's 18th birthday`() {
        val user =
            AuthenticatedUser.Employee(
                EmployeeId(UUID.randomUUID()),
                setOf(UserRole.SERVICE_WORKER)
            )
        val request =
            ParentshipController.ParentshipRequest(
                parent.id,
                child.id,
                child.dateOfBirth,
                child.dateOfBirth.plusYears(18)
            )
        assertThrows<BadRequest> { controller.createParentship(dbInstance(), user, clock, request) }
    }
}
