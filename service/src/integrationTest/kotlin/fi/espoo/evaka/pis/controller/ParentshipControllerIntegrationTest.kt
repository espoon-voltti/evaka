// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controller

import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.pis.AbstractIntegrationTest
import fi.espoo.evaka.pis.controllers.ParentshipController
import fi.espoo.evaka.pis.createParentship
import fi.espoo.evaka.pis.getParentships
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testDaycare
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ParentshipControllerIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var controller: ParentshipController

    private val parent = testAdult_1
    private val child = testChild_1

    private val unitSupervisorId = UUID.randomUUID()

    @BeforeEach
    fun init() {
        db.transaction {
            it.insertGeneralTestFixtures()
            it.insertTestEmployee(DevEmployee(unitSupervisorId))
            it.insertDaycareAclRow(
                daycareId = testDaycare.id,
                employeeId = unitSupervisorId,
                role = UserRole.UNIT_SUPERVISOR
            )
            it.insertTestPlacement(DevPlacement(childId = child.id, unitId = testDaycare.id, endDate = LocalDate.now()))
        }
    }

    @Test
    fun `service worker can create and fetch parentships`() {
        `can create and fetch parentships`(
            AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.SERVICE_WORKER))
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
        `can create and fetch parentships`(AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.FINANCE_ADMIN)))
    }

    fun `can create and fetch parentships`(user: AuthenticatedUser) {
        val startDate = child.dateOfBirth
        val endDate = startDate.plusDays(200)
        val reqBody =
            ParentshipController.ParentshipRequest(PersonId(parent.id), PersonId(child.id), startDate, endDate)
        controller.createParentship(db, user, reqBody)

        val getResponse = controller.getParentships(db, user, headOfChildId = PersonId(parent.id))
        with(getResponse.first()) {
            assertNotNull(this.id)
            assertEquals(parent.id, this.headOfChildId)
            assertEquals(child, this.child)
            assertEquals(startDate, this.startDate)
            assertEquals(endDate, this.endDate)
        }

        assertEquals(0, controller.getParentships(db, user, headOfChildId = PersonId(child.id)).size)
    }

    fun `can add a sibling parentship and fetch parentships`(user: AuthenticatedUser) {
        val parentship = db.transaction { tx ->
            tx.createParentship(child.id, parent.id, child.dateOfBirth, child.dateOfBirth.plusDays(200))
        }

        val sibling = testChild_2
        val startDate = sibling.dateOfBirth
        val endDate = startDate.plusDays(200)
        val reqBody =
            ParentshipController.ParentshipRequest(PersonId(parent.id), PersonId(sibling.id), startDate, endDate)
        controller.createParentship(db, user, reqBody)

        val getResponse = controller.getParentships(db, user, headOfChildId = PersonId(parent.id))
        with(getResponse.first()) {
            assertNotNull(this.id)
            assertEquals(parentship.headOfChildId, this.headOfChildId)
            assertEquals(parentship.child.id, this.childId)
            assertEquals(parentship.startDate, this.startDate)
            assertEquals(parentship.endDate, this.endDate)
        }
        with(getResponse.last()) {
            assertNotNull(this.id)
            assertEquals(parent.id, this.headOfChildId)
            assertEquals(sibling.id, this.childId)
            assertEquals(startDate, this.startDate)
            assertEquals(endDate, this.endDate)
        }

        assertEquals(0, controller.getParentships(db, user, headOfChildId = PersonId(child.id)).size)
    }

    @Test
    fun `service worker can update parentships`() {
        `can update parentship duration`(AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.SERVICE_WORKER)))
    }

    @Test
    fun `unit supervisor can update parentships`() {
        `can update parentship duration`(AuthenticatedUser.Employee(unitSupervisorId, setOf()))
    }

    @Test
    fun `finance admin can update parentships`() {
        `can update parentship duration`(AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.FINANCE_ADMIN)))
    }

    fun `can update parentship duration`(user: AuthenticatedUser) {
        val parentship = db.transaction { tx ->
            tx.createParentship(child.id, parent.id, child.dateOfBirth.plusDays(500), child.dateOfBirth.plusDays(700))
            tx.createParentship(child.id, parent.id, child.dateOfBirth, child.dateOfBirth.plusDays(200))
        }
        val newStartDate = child.dateOfBirth.plusDays(100)
        val newEndDate = child.dateOfBirth.plusDays(300)
        val requestBody = ParentshipController.ParentshipUpdateRequest(newStartDate, newEndDate)
        controller.updateParentship(db, user, parentship.id, requestBody)

        // child1 should have new dates
        val fetched1 = controller.getParentship(db, user, parentship.id)
        assertEquals(newStartDate, fetched1.startDate)
        assertEquals(newEndDate, fetched1.endDate)
    }

    @Test
    fun `service worker cannot delete parentships`() {
        `cannot delete parentship`(AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.SERVICE_WORKER)))
    }

    @Test
    fun `unit supervisor cannot delete parentships`() {
        `cannot delete parentship`(AuthenticatedUser.Employee(unitSupervisorId, setOf()))
    }

    @Test
    fun `finance admin can delete parentships`() {
        `can delete parentship`(AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.FINANCE_ADMIN)))
    }

    fun `can delete parentship`(user: AuthenticatedUser) {
        val parentship = db.transaction { tx ->
            tx.createParentship(child.id, parent.id, child.dateOfBirth, child.dateOfBirth.plusDays(100)).also {
                tx.createParentship(
                    child.id,
                    parent.id,
                    child.dateOfBirth.plusDays(200),
                    child.dateOfBirth.plusDays(300)
                )
                assertEquals(2, tx.getParentships(headOfChildId = parent.id, childId = null).size)
            }
        }

        controller.deleteParentship(db, user, parentship.id)
        db.read { r ->
            assertEquals(1, r.getParentships(headOfChildId = parent.id, childId = null).size)
        }
    }

    fun `cannot delete parentship`(user: AuthenticatedUser) {
        val parentship = db.transaction { tx ->
            tx.createParentship(child.id, parent.id, child.dateOfBirth, child.dateOfBirth.plusDays(100)).also {
                tx.createParentship(
                    child.id,
                    parent.id,
                    child.dateOfBirth.plusDays(200),
                    child.dateOfBirth.plusDays(300)
                )
                assertEquals(2, tx.getParentships(headOfChildId = parent.id, childId = null).size)
            }
        }
        assertThrows<Forbidden> { controller.deleteParentship(db, user, parentship.id) }
    }

    @Test
    fun `error is thrown if enduser tries to get parentships`() {
        val user = AuthenticatedUser.Citizen(UUID.randomUUID())
        db.transaction { tx ->
            tx.createParentship(child.id, parent.id, child.dateOfBirth, child.dateOfBirth.plusDays(200))
        }
        assertThrows<Forbidden> { controller.getParentships(db, user, headOfChildId = PersonId(parent.id)) }
    }

    @Test
    fun `error is thrown if enduser tries to update parentship`() {
        val user = AuthenticatedUser.Citizen(UUID.randomUUID())
        val parentship = db.transaction { tx ->
            tx.createParentship(child.id, parent.id, child.dateOfBirth, child.dateOfBirth.plusDays(200))
        }
        val newStartDate = child.dateOfBirth.plusDays(100)
        val newEndDate = child.dateOfBirth.plusDays(300)
        val requestBody = ParentshipController.ParentshipUpdateRequest(newStartDate, newEndDate)
        assertThrows<Forbidden> { controller.updateParentship(db, user, parentship.id, requestBody) }
    }

    @Test
    fun `error is thrown if enduser tries to delete parentship`() {
        val user = AuthenticatedUser.Citizen(UUID.randomUUID())
        val parentship = db.transaction { tx ->
            tx.createParentship(child.id, parent.id, child.dateOfBirth, child.dateOfBirth.plusDays(200))
        }
        assertThrows<Forbidden> { controller.deleteParentship(db, user, parentship.id) }
    }

    @Test
    fun `error is thrown if service worker tries to create a partnership with a start date before child's date of birth`() {
        val user = AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.SERVICE_WORKER))
        val request = ParentshipController.ParentshipRequest(
            PersonId(parent.id),
            PersonId(child.id),
            child.dateOfBirth.minusDays(1),
            child.dateOfBirth.plusYears(1)
        )
        assertThrows<BadRequest> { controller.createParentship(db, user, request) }
    }

    @Test
    fun `error is thrown if service worker tries to create a partnership with a end date after child's 18th birthday`() {
        val user = AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.SERVICE_WORKER))
        val request = ParentshipController.ParentshipRequest(
            PersonId(parent.id),
            PersonId(child.id),
            child.dateOfBirth,
            child.dateOfBirth.plusYears(18)
        )
        assertThrows<BadRequest> { controller.createParentship(db, user, request) }
    }
}
