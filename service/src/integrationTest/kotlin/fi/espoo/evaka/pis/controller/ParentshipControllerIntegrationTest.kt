// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controller

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.pis.Creator
import fi.espoo.evaka.pis.controllers.ParentshipController
import fi.espoo.evaka.pis.createParentship
import fi.espoo.evaka.pis.getParentships
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testDaycare
import java.time.LocalDate
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
    private val serviceWorker = DevEmployee(roles = setOf(UserRole.SERVICE_WORKER))
    private val financeAdmin = DevEmployee(roles = setOf(UserRole.FINANCE_ADMIN))
    private val unitSupervisor = DevEmployee()

    @BeforeEach
    fun init() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
            tx.insert(testArea)
            tx.insert(testDaycare)
            tx.insert(testAdult_1, DevPersonType.ADULT)
            listOf(testChild_1, testChild_2).forEach { tx.insert(it, DevPersonType.CHILD) }
            tx.insert(serviceWorker)
            tx.insert(financeAdmin)
            tx.insert(unitSupervisor)
            tx.insertDaycareAclRow(
                daycareId = testDaycare.id,
                employeeId = unitSupervisor.id,
                role = UserRole.UNIT_SUPERVISOR
            )
            tx.insert(
                DevPlacement(childId = child.id, unitId = testDaycare.id, endDate = LocalDate.now())
            )
        }
    }

    @Test
    fun `service worker can create and fetch parentships`() {
        `can create and fetch parentships`(serviceWorker.user)
    }

    @Test
    fun `can add a sibling parentship and fetch parentships`() {
        `can add a sibling parentship and fetch parentships`(unitSupervisor.user)
    }

    @Test
    fun `finance admin can create and fetch parentships`() {
        `can create and fetch parentships`(financeAdmin.user)
    }

    fun `can create and fetch parentships`(user: AuthenticatedUser.Employee) {
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

    fun `can add a sibling parentship and fetch parentships`(user: AuthenticatedUser.Employee) {
        db.transaction { tx ->
            tx.createParentship(
                child.id,
                parent.id,
                child.dateOfBirth,
                child.dateOfBirth.plusDays(200),
                Creator.DVV
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
        `can update parentship duration`(serviceWorker.user)
    }

    @Test
    fun `unit supervisor can update parentships`() {
        `can update parentship duration`(unitSupervisor.user)
    }

    @Test
    fun `finance admin can update parentships`() {
        `can update parentship duration`(financeAdmin.user)
    }

    fun `can update parentship duration`(user: AuthenticatedUser.Employee) {
        val parentship =
            db.transaction { tx ->
                tx.createParentship(
                    child.id,
                    parent.id,
                    child.dateOfBirth.plusDays(500),
                    child.dateOfBirth.plusDays(700),
                    Creator.DVV
                )
                tx.createParentship(
                    child.id,
                    parent.id,
                    child.dateOfBirth,
                    child.dateOfBirth.plusDays(200),
                    Creator.DVV
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
        `cannot delete parentship`(serviceWorker.user)
    }

    @Test
    fun `unit supervisor cannot delete parentships`() {
        `cannot delete parentship`(unitSupervisor.user)
    }

    @Test
    fun `finance admin can delete parentships`() {
        `can delete parentship`(financeAdmin.user)
    }

    fun `can delete parentship`(user: AuthenticatedUser.Employee) {
        val parentship =
            db.transaction { tx ->
                tx.createParentship(
                        child.id,
                        parent.id,
                        child.dateOfBirth,
                        child.dateOfBirth.plusDays(100),
                        Creator.DVV
                    )
                    .also {
                        tx.createParentship(
                            child.id,
                            parent.id,
                            child.dateOfBirth.plusDays(200),
                            child.dateOfBirth.plusDays(300),
                            Creator.DVV
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

    fun `cannot delete parentship`(user: AuthenticatedUser.Employee) {
        val parentship =
            db.transaction { tx ->
                tx.createParentship(
                        child.id,
                        parent.id,
                        child.dateOfBirth,
                        child.dateOfBirth.plusDays(100),
                        Creator.DVV
                    )
                    .also {
                        tx.createParentship(
                            child.id,
                            parent.id,
                            child.dateOfBirth.plusDays(200),
                            child.dateOfBirth.plusDays(300),
                            Creator.DVV
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
    fun `error is thrown if service worker tries to create a partnership with a start date before child's date of birth`() {
        val request =
            ParentshipController.ParentshipRequest(
                parent.id,
                child.id,
                child.dateOfBirth.minusDays(1),
                child.dateOfBirth.plusYears(1)
            )
        assertThrows<BadRequest> {
            controller.createParentship(dbInstance(), serviceWorker.user, clock, request)
        }
    }

    @Test
    fun `error is thrown if service worker tries to create a partnership with a end date after child's 18th birthday`() {
        val request =
            ParentshipController.ParentshipRequest(
                parent.id,
                child.id,
                child.dateOfBirth,
                child.dateOfBirth.plusYears(18)
            )
        assertThrows<BadRequest> {
            controller.createParentship(dbInstance(), serviceWorker.user, clock, request)
        }
    }
}
