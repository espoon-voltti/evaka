// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controller

import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.pis.AbstractIntegrationTest
import fi.espoo.evaka.pis.controllers.PartnershipsController
import fi.espoo.evaka.pis.dao.PartnershipDAO
import fi.espoo.evaka.pis.dao.PersonDAO
import fi.espoo.evaka.pis.service.PersonIdentityRequest
import fi.espoo.evaka.pis.service.PersonJSON
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.config.Roles
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

    @Autowired
    lateinit var personDAO: PersonDAO

    @Autowired
    lateinit var partnershipDAO: PartnershipDAO

    @Test
    fun `service worker can create and fetch partnerships`() {
        `can create and fetch partnerships`(AuthenticatedUser(UUID.randomUUID(), setOf(Roles.SERVICE_WORKER)))
    }

    @Test
    fun `unit supervisor can create and fetch partnerships`() {
        `can create and fetch partnerships`(AuthenticatedUser(UUID.randomUUID(), setOf(Roles.UNIT_SUPERVISOR)))
    }

    @Test
    fun `finance admin can create and fetch partnerships`() {
        `can create and fetch partnerships`(AuthenticatedUser(UUID.randomUUID(), setOf(Roles.FINANCE_ADMIN)))
    }

    fun `can create and fetch partnerships`(user: AuthenticatedUser) {
        val adult1 = testPerson1()
        val adult2 = testPerson2()

        val startDate = LocalDate.now()
        val endDate = startDate.plusDays(200)
        val reqBody = PartnershipsController.PartnershipRequest(setOf(adult1.id, adult2.id), startDate, endDate)
        val createResponse = controller.createPartnership(user, reqBody)
        assertEquals(HttpStatus.CREATED, createResponse.statusCode)
        with(createResponse.body!!) {
            assertNotNull(this.id)
            assertEquals(startDate, this.startDate)
            assertEquals(endDate, this.endDate)
            assertEquals(setOf(adult1, adult2), this.partners)
        }

        val getResponse = controller.getPartnerships(user, adult1.id)
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
        `can delete partnership`(AuthenticatedUser(UUID.randomUUID(), setOf(Roles.SERVICE_WORKER)))
    }

    @Test
    fun `unit supervisor can delete partnerships`() {
        `can delete partnership`(AuthenticatedUser(UUID.randomUUID(), setOf(Roles.UNIT_SUPERVISOR)))
    }

    @Test
    fun `finance admin can delete partnerships`() {
        `can delete partnership`(AuthenticatedUser(UUID.randomUUID(), setOf(Roles.FINANCE_ADMIN)))
    }

    fun `can delete partnership`(user: AuthenticatedUser) {
        val adult1 = testPerson1()
        val adult2 = testPerson2()
        val partnership1 = partnershipDAO.createPartnership(adult1.id, adult2.id, LocalDate.now(), LocalDate.now().plusDays(100))
        partnershipDAO.createPartnership(adult1.id, adult2.id, LocalDate.now().plusDays(200), LocalDate.now().plusDays(300))
        assertEquals(2, partnershipDAO.getPartnershipsForPerson(adult1.id).size)

        val delResponse = controller.deletePartnership(user, partnership1.id)
        assertEquals(HttpStatus.NO_CONTENT, delResponse.statusCode)
        assertEquals(1, partnershipDAO.getPartnershipsForPerson(adult1.id).size)
    }

    @Test
    fun `service worker can update partnerships`() {
        `can update partnership duration`(AuthenticatedUser(UUID.randomUUID(), setOf(Roles.SERVICE_WORKER)))
    }

    @Test
    fun `unit supervisor can update partnerships`() {
        `can update partnership duration`(AuthenticatedUser(UUID.randomUUID(), setOf(Roles.UNIT_SUPERVISOR)))
    }

    @Test
    fun `finance admin can update partnerships`() {
        `can update partnership duration`(AuthenticatedUser(UUID.randomUUID(), setOf(Roles.FINANCE_ADMIN)))
    }

    fun `can update partnership duration`(user: AuthenticatedUser) {
        val adult1 = testPerson1()
        val adult2 = testPerson2()
        val partnership1 = partnershipDAO.createPartnership(adult1.id, adult2.id, LocalDate.now(), LocalDate.now().plusDays(200))
        partnershipDAO.createPartnership(adult1.id, adult2.id, LocalDate.now().plusDays(500), LocalDate.now().plusDays(700))

        val newStartDate = LocalDate.now().plusDays(100)
        val newEndDate = LocalDate.now().plusDays(300)
        val requestBody = PartnershipsController.PartnershipUpdateRequest(newStartDate, newEndDate)
        val updateResponse = controller.updatePartnership(user, partnership1.id, requestBody)
        assertEquals(HttpStatus.OK, updateResponse.statusCode)
        with(updateResponse.body!!) {
            assertEquals(partnership1.id, this.id)
            assertEquals(newStartDate, this.startDate)
            assertEquals(newEndDate, this.endDate)
            assertEquals(setOf(adult1, adult2), this.partners)
        }

        // partnership1 should have new dates
        val fetched1 = controller.getPartnership(user, partnership1.id).body!!
        assertEquals(newStartDate, fetched1.startDate)
        assertEquals(newEndDate, fetched1.endDate)
    }

    @Test
    fun `can updating partnership duration to overlap throws conflict`() {
        val user = AuthenticatedUser(UUID.randomUUID(), setOf(Roles.SERVICE_WORKER))
        val adult1 = testPerson1()
        val adult2 = testPerson2()
        val partnership1 = partnershipDAO.createPartnership(adult1.id, adult2.id, LocalDate.now(), LocalDate.now().plusDays(200))
        partnershipDAO.createPartnership(adult1.id, adult2.id, LocalDate.now().plusDays(500), LocalDate.now().plusDays(700))

        val newStartDate = LocalDate.now().plusDays(100)
        val newEndDate = LocalDate.now().plusDays(600)
        val requestBody = PartnershipsController.PartnershipUpdateRequest(newStartDate, newEndDate)
        assertThrows<Conflict> {
            controller.updatePartnership(user, partnership1.id, requestBody)
        }
    }

    @Test
    fun `error is thrown if enduser tries to get partnerships`() {
        val user = AuthenticatedUser(UUID.randomUUID(), setOf(Roles.END_USER))
        val adult1 = testPerson1()
        val adult2 = testPerson2()
        partnershipDAO.createPartnership(adult1.id, adult2.id, LocalDate.now(), LocalDate.now().plusDays(200))

        assertThrows<Forbidden> {
            controller.getPartnerships(user, adult1.id)
        }
    }

    @Test
    fun `error is thrown if enduser tries to create a partnership`() {
        val user = AuthenticatedUser(UUID.randomUUID(), setOf(Roles.END_USER))
        val adult1 = testPerson1()
        val adult2 = testPerson2()
        val startDate = LocalDate.now()
        val endDate = startDate.plusDays(200)
        val reqBody = PartnershipsController.PartnershipRequest(setOf(adult1.id, adult2.id), startDate, endDate)

        assertThrows<Forbidden> {
            controller.createPartnership(user, reqBody)
        }
    }

    @Test
    fun `error is thrown if enduser tries to update partnerships`() {
        val user = AuthenticatedUser(UUID.randomUUID(), setOf(Roles.END_USER))
        val adult1 = testPerson1()
        val adult2 = testPerson2()
        val partnership = partnershipDAO.createPartnership(adult1.id, adult2.id, LocalDate.now(), LocalDate.now().plusDays(200))

        val requestBody = PartnershipsController.PartnershipUpdateRequest(LocalDate.now(), LocalDate.now().plusDays(999))
        assertThrows<Forbidden> {
            controller.updatePartnership(user, partnership.id, requestBody)
        }
    }

    @Test
    fun `error is thrown if enduser tries to delete a partnership`() {
        val user = AuthenticatedUser(UUID.randomUUID(), setOf(Roles.END_USER))
        val partnership = partnershipDAO.createPartnership(testPerson1().id, testPerson2().id, LocalDate.now(), LocalDate.now().plusDays(200))

        assertThrows<Forbidden> {
            controller.deletePartnership(user, partnership.id)
        }
    }

    private fun createPerson(ssn: String, firstName: String): PersonJSON {
        return personDAO.getOrCreatePersonIdentity(
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
