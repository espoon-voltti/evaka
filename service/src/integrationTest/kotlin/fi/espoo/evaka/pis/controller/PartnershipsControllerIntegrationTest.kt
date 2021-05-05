// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controller

import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.pis.AbstractIntegrationTest
import fi.espoo.evaka.pis.controllers.PartnershipsController
import fi.espoo.evaka.pis.createPartnership
import fi.espoo.evaka.pis.createPerson
import fi.espoo.evaka.pis.getPartnershipsForPerson
import fi.espoo.evaka.pis.service.PersonIdentityRequest
import fi.espoo.evaka.pis.service.PersonJSON
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.transaction
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.Forbidden
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.util.UUID

class PartnershipsControllerIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var controller: PartnershipsController

    @Test
    fun `service worker can create and fetch partnerships`() {
        `can create and fetch partnerships`(AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.SERVICE_WORKER)))
    }

    @Test
    fun `unit supervisor can create and fetch partnerships`() {
        `can create and fetch partnerships`(AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.UNIT_SUPERVISOR)))
    }

    @Test
    fun `finance admin can create and fetch partnerships`() {
        `can create and fetch partnerships`(AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.FINANCE_ADMIN)))
    }

    fun `can create and fetch partnerships`(user: AuthenticatedUser) {
        val adult1 = testPerson1()
        val adult2 = testPerson2()

        val startDate = LocalDate.now()
        val endDate = startDate.plusDays(200)
        val reqBody = PartnershipsController.PartnershipRequest(setOf(adult1.id, adult2.id), startDate, endDate)
        val createResponse = controller.createPartnership(db, user, reqBody)
        assertEquals(HttpStatus.CREATED, createResponse.statusCode)
        with(createResponse.body!!) {
            assertNotNull(this.id)
            assertEquals(startDate, this.startDate)
            assertEquals(endDate, this.endDate)
            assertEquals(setOf(adult1, adult2), this.partners)
        }

        val getResponse = controller.getPartnerships(db, user, adult1.id)
        assertEquals(HttpStatus.OK, getResponse.statusCode)
        assertEquals(1, getResponse.body?.size ?: 0)
        with(getResponse.body!!.first()) {
            assertNotNull(this.id)
            assertEquals(startDate, this.startDate)
            assertEquals(endDate, this.endDate)
            assertEquals(setOf(adult1, adult2), this.partners)
        }
    }

    @Test
    fun `service worker can delete partnerships`() {
        canDeletePartnership(AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.SERVICE_WORKER)))
    }

    @Test
    fun `unit supervisor can delete partnerships`() {
        canDeletePartnership(AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.UNIT_SUPERVISOR)))
    }

    @Test
    fun `finance admin can delete partnerships`() {
        canDeletePartnership(AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.FINANCE_ADMIN)))
    }

    private fun canDeletePartnership(user: AuthenticatedUser) {
        val adult1 = testPerson1()
        val adult2 = testPerson2()
        val partnership1 = db.transaction { tx ->
            tx.createPartnership(adult1.id, adult2.id, LocalDate.now(), LocalDate.now().plusDays(100)).also {
                tx.createPartnership(adult1.id, adult2.id, LocalDate.now().plusDays(200), LocalDate.now().plusDays(300))
                assertEquals(2, tx.getPartnershipsForPerson(adult1.id).size)
            }
        }

        val delResponse = controller.deletePartnership(db, user, partnership1.id)
        assertEquals(HttpStatus.NO_CONTENT, delResponse.statusCode)
        db.read { r ->
            assertEquals(1, r.getPartnershipsForPerson(adult1.id).size)
        }
    }

    @Test
    fun `service worker can update partnerships`() {
        canUpdatePartnershipDuration(AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.SERVICE_WORKER)))
    }

    @Test
    fun `unit supervisor can update partnerships`() {
        canUpdatePartnershipDuration(AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.UNIT_SUPERVISOR)))
    }

    @Test
    fun `finance admin can update partnerships`() {
        canUpdatePartnershipDuration(AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.FINANCE_ADMIN)))
    }

    private fun canUpdatePartnershipDuration(user: AuthenticatedUser) {
        val adult1 = testPerson1()
        val adult2 = testPerson2()
        val partnership1 = db.transaction { tx ->
            tx.createPartnership(adult1.id, adult2.id, LocalDate.now(), LocalDate.now().plusDays(200)).also {
                tx.createPartnership(adult1.id, adult2.id, LocalDate.now().plusDays(500), LocalDate.now().plusDays(700))
            }
        }

        val newStartDate = LocalDate.now().plusDays(100)
        val newEndDate = LocalDate.now().plusDays(300)
        val requestBody = PartnershipsController.PartnershipUpdateRequest(newStartDate, newEndDate)
        val updateResponse = controller.updatePartnership(db, user, partnership1.id, requestBody)
        assertEquals(HttpStatus.OK, updateResponse.statusCode)
        with(updateResponse.body!!) {
            assertEquals(partnership1.id, this.id)
            assertEquals(newStartDate, this.startDate)
            assertEquals(newEndDate, this.endDate)
            assertEquals(setOf(adult1, adult2), this.partners)
        }

        // partnership1 should have new dates
        val fetched1 = controller.getPartnership(db, user, partnership1.id).body!!
        assertEquals(newStartDate, fetched1.startDate)
        assertEquals(newEndDate, fetched1.endDate)
    }

    @Test
    fun `can updating partnership duration to overlap throws conflict`() {
        val user = AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.SERVICE_WORKER))
        val adult1 = testPerson1()
        val adult2 = testPerson2()
        val partnership1 = db.transaction { tx ->
            tx.createPartnership(adult1.id, adult2.id, LocalDate.now(), LocalDate.now().plusDays(200)).also {
                tx.createPartnership(adult1.id, adult2.id, LocalDate.now().plusDays(500), LocalDate.now().plusDays(700))
            }
        }

        val newStartDate = LocalDate.now().plusDays(100)
        val newEndDate = LocalDate.now().plusDays(600)
        val requestBody = PartnershipsController.PartnershipUpdateRequest(newStartDate, newEndDate)
        assertThrows<Conflict> {
            controller.updatePartnership(db, user, partnership1.id, requestBody)
        }
    }

    @Test
    fun `error is thrown if enduser tries to get partnerships`() {
        val user = AuthenticatedUser.Citizen(UUID.randomUUID())
        val adult1 = testPerson1()
        val adult2 = testPerson2()
        db.transaction { tx ->
            tx.createPartnership(adult1.id, adult2.id, LocalDate.now(), LocalDate.now().plusDays(200))
        }

        assertThrows<Forbidden> {
            controller.getPartnerships(db, user, adult1.id)
        }
    }

    @Test
    fun `error is thrown if enduser tries to create a partnership`() {
        val user = AuthenticatedUser.Citizen(UUID.randomUUID())
        val adult1 = testPerson1()
        val adult2 = testPerson2()
        val startDate = LocalDate.now()
        val endDate = startDate.plusDays(200)
        val reqBody = PartnershipsController.PartnershipRequest(setOf(adult1.id, adult2.id), startDate, endDate)

        assertThrows<Forbidden> {
            controller.createPartnership(db, user, reqBody)
        }
    }

    @Test
    fun `error is thrown if enduser tries to update partnerships`() {
        val user = AuthenticatedUser.Citizen(UUID.randomUUID())
        val adult1 = testPerson1()
        val adult2 = testPerson2()
        val partnership = db.transaction { tx ->
            tx.createPartnership(adult1.id, adult2.id, LocalDate.now(), LocalDate.now().plusDays(200))
        }

        val requestBody = PartnershipsController.PartnershipUpdateRequest(LocalDate.now(), LocalDate.now().plusDays(999))
        assertThrows<Forbidden> {
            controller.updatePartnership(db, user, partnership.id, requestBody)
        }
    }

    @Test
    fun `error is thrown if enduser tries to delete a partnership`() {
        val user = AuthenticatedUser.Citizen(UUID.randomUUID())
        val adult1 = testPerson1()
        val adult2 = testPerson2()
        val partnership = db.transaction { tx ->
            tx.createPartnership(adult1.id, adult2.id, LocalDate.now(), LocalDate.now().plusDays(200))
        }

        assertThrows<Forbidden> {
            controller.deletePartnership(db, user, partnership.id)
        }
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
