// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controller

import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.pis.AbstractIntegrationTest
import fi.espoo.evaka.pis.controllers.ParentshipController
import fi.espoo.evaka.pis.createParentship
import fi.espoo.evaka.pis.createPerson
import fi.espoo.evaka.pis.getParentships
import fi.espoo.evaka.pis.service.PersonIdentityRequest
import fi.espoo.evaka.pis.service.PersonJSON
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Forbidden
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ParentshipControllerIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var controller: ParentshipController

    @Test
    fun `service worker can create and fetch parentships`() {
        `can create and fetch parentships`(AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.SERVICE_WORKER)))
    }

    @Test
    fun `unit supervisor can create and fetch parentships`() {
        `can create and fetch parentships`(AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.UNIT_SUPERVISOR)))
    }

    @Test
    fun `finance admin can create and fetch parentships`() {
        `can create and fetch parentships`(AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.FINANCE_ADMIN)))
    }

    fun `can create and fetch parentships`(user: AuthenticatedUser) {
        val parent = testPerson1()
        val child = testPerson2()

        val startDate = child.dateOfBirth
        val endDate = startDate.plusDays(200)
        val reqBody = ParentshipController.ParentshipRequest(parent.id, child.id, startDate, endDate)
        val createResponse = controller.createParentship(db, user, reqBody)
        assertEquals(HttpStatus.CREATED, createResponse.statusCode)
        with(createResponse.body!!) {
            assertNotNull(this.id)
            assertEquals(parent.id, this.headOfChildId)
            assertEquals(child, this.child)
            assertEquals(startDate, this.startDate)
            assertEquals(endDate, this.endDate)
        }

        val getResponse = controller.getParentships(db, user, headOfChildId = parent.id)
        assertEquals(HttpStatus.OK, getResponse.statusCode)
        assertEquals(1, getResponse.body?.size ?: 0)
        with(getResponse.body!!.first()) {
            assertNotNull(this.id)
            assertEquals(parent.id, this.headOfChildId)
            assertEquals(child, this.child)
            assertEquals(startDate, this.startDate)
            assertEquals(endDate, this.endDate)
        }

        assertEquals(0, controller.getParentships(db, user, headOfChildId = child.id).body!!.size)
    }

    @Test
    fun `service worker can update parentships`() {
        `can update parentship duration`(AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.SERVICE_WORKER)))
    }

    @Test
    fun `unit supervisor can update parentships`() {
        `can update parentship duration`(AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.UNIT_SUPERVISOR)))
    }

    @Test
    fun `finance admin can update parentships`() {
        `can update parentship duration`(AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.FINANCE_ADMIN)))
    }

    fun `can update parentship duration`(user: AuthenticatedUser) {
        val adult = testPerson1()
        val child = testPerson2()
        val parentship = db.transaction { tx ->
            tx.createParentship(child.id, adult.id, child.dateOfBirth.plusDays(500), child.dateOfBirth.plusDays(700))
            tx.createParentship(child.id, adult.id, child.dateOfBirth, child.dateOfBirth.plusDays(200))
        }
        val newStartDate = child.dateOfBirth.plusDays(100)
        val newEndDate = child.dateOfBirth.plusDays(300)
        val requestBody = ParentshipController.ParentshipUpdateRequest(newStartDate, newEndDate)
        val updateResponse = controller.updateParentship(db, user, parentship.id, requestBody)
        assertEquals(HttpStatus.OK, updateResponse.statusCode)
        with(updateResponse.body!!) {
            assertEquals(parentship.id, this.id)
            assertEquals(newStartDate, this.startDate)
            assertEquals(newEndDate, this.endDate)
        }

        // child1 should have new dates
        val fetched1 = controller.getParentship(db, user, parentship.id).body!!
        assertEquals(newStartDate, fetched1.startDate)
        assertEquals(newEndDate, fetched1.endDate)
    }

    @Test
    fun `service worker cannot delete parentships`() {
        `cannot delete parentship`(AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.SERVICE_WORKER)))
    }

    @Test
    fun `unit supervisor cannot delete parentships`() {
        `cannot delete parentship`(AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.UNIT_SUPERVISOR)))
    }

    @Test
    fun `finance admin can delete parentships`() {
        `can delete parentship`(AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.FINANCE_ADMIN)))
    }

    fun `can delete parentship`(user: AuthenticatedUser) {
        val adult = testPerson1()
        val child = testPerson2()
        val parentship = db.transaction { tx ->
            tx.createParentship(child.id, adult.id, child.dateOfBirth, child.dateOfBirth.plusDays(100)).also {
                tx.createParentship(child.id, adult.id, child.dateOfBirth.plusDays(200), child.dateOfBirth.plusDays(300))
                assertEquals(2, tx.getParentships(headOfChildId = adult.id, childId = null).size)
            }
        }

        val delResponse = controller.deleteParentship(db, user, parentship.id)
        assertEquals(HttpStatus.NO_CONTENT, delResponse.statusCode)
        db.read { r ->
            assertEquals(1, r.getParentships(headOfChildId = adult.id, childId = null).size)
        }
    }

    fun `cannot delete parentship`(user: AuthenticatedUser) {
        val adult = testPerson1()
        val child = testPerson2()
        val parentship = db.transaction { tx ->
            tx.createParentship(child.id, adult.id, child.dateOfBirth, child.dateOfBirth.plusDays(100)).also {
                tx.createParentship(child.id, adult.id, child.dateOfBirth.plusDays(200), child.dateOfBirth.plusDays(300))
                assertEquals(2, tx.getParentships(headOfChildId = adult.id, childId = null).size)
            }
        }
        assertThrows<Forbidden> { controller.deleteParentship(db, user, parentship.id) }
    }

    @Test
    fun `error is thrown if enduser tries to get parentships`() {
        val user = AuthenticatedUser.Citizen(UUID.randomUUID())
        val parent = testPerson1()
        val child = testPerson2()
        db.transaction { tx ->
            tx.createParentship(child.id, parent.id, child.dateOfBirth, child.dateOfBirth.plusDays(200))
        }
        assertThrows<Forbidden> { controller.getParentships(db, user, headOfChildId = parent.id) }
    }

    @Test
    fun `error is thrown if enduser tries to update parentship`() {
        val user = AuthenticatedUser.Citizen(UUID.randomUUID())
        val parent = testPerson1()
        val child = testPerson2()
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
        val parent = testPerson1()
        val child = testPerson2()
        val parentship = db.transaction { tx ->
            tx.createParentship(child.id, parent.id, child.dateOfBirth, child.dateOfBirth.plusDays(200))
        }
        assertThrows<Forbidden> { controller.deleteParentship(db, user, parentship.id) }
    }

    @Test
    fun `error is thrown if service worker tries to create a partnership with a start date before child's date of birth`() {
        val user = AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.SERVICE_WORKER))
        val parent = testPerson1()
        val child = testPerson2()
        val request = ParentshipController.ParentshipRequest(parent.id, child.id, child.dateOfBirth.minusDays(1), child.dateOfBirth.plusYears(1))
        assertThrows<BadRequest> { controller.createParentship(db, user, request) }
    }

    @Test
    fun `error is thrown if service worker tries to create a partnership with a end date after child's 18th birthday`() {
        val user = AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.SERVICE_WORKER))
        val parent = testPerson1()
        val child = testPerson2()
        val request = ParentshipController.ParentshipRequest(parent.id, child.id, child.dateOfBirth, child.dateOfBirth.plusYears(18))
        assertThrows<BadRequest> { controller.createParentship(db, user, request) }
    }

    private fun createPerson(ssn: String, firstName: String): PersonJSON = db.transaction { tx ->
        tx.createPerson(
            PersonIdentityRequest(
                identity = ExternalIdentifier.SSN.getInstance(ssn),
                firstName = firstName,
                lastName = "Meikäläinen",
                email = "",
                language = "fi"
            )
        ).let { PersonJSON.from(it) }
    }

    private fun testPerson1() = createPerson("140881-172X", "Aku")
    private fun testPerson2() = createPerson("150786-1766", "Iines")
}
