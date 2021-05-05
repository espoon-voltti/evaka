// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controller

import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.pis.AbstractIntegrationTest
import fi.espoo.evaka.pis.controllers.PersonController
import fi.espoo.evaka.pis.createPerson
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.pis.service.ContactInfo
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.pis.service.PersonIdentityRequest
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.domain.Forbidden
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.util.UUID

class PersonControllerIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var controller: PersonController

    private val contactInfo = ContactInfo(
        email = "test@hii",
        phone = "+358401234567",
        backupPhone = "",
        invoiceRecipientName = "Laskun saaja",
        invoicingStreetAddress = "Laskutusosoite",
        invoicingPostalCode = "02123",
        invoicingPostOffice = "Espoo",
        forceManualFeeDecisions = true
    )

    @Test
    fun `Finance admin can update end user's contact info`() {
        updateContactInfo(AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.FINANCE_ADMIN)))
    }

    @Test
    fun `Service worker can update end user's contact info`() {
        updateContactInfo(AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.SERVICE_WORKER)))
    }

    @Test
    fun `Unit supervisor can update end user's contact info`() {
        updateContactInfo(AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.UNIT_SUPERVISOR)))
    }

    @Test
    fun `End user cannot end update end user's contact info`() {
        val user = AuthenticatedUser.Citizen(UUID.randomUUID())
        assertThrows<Forbidden> {
            controller.updateContactInfo(db, user, UUID.randomUUID(), contactInfo)
        }
    }

    @Test
    fun `Search finds person by first and last name`() {
        val user = AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.SERVICE_WORKER))
        val person = createPerson()

        val response = controller.findBySearchTerms(
            db,
            user,
            searchTerm = "${person.firstName} ${person.lastName}",
            orderBy = "first_name",
            sortDirection = "DESC"
        )

        assertEquals(HttpStatus.OK, response?.statusCode)

        with(response?.body!!) {
            assertEquals(person.id, this.first().id)
        }
    }

    @Test
    fun `Search treats tabs as spaces in search terms`() {
        val user = AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.SERVICE_WORKER))
        val person = createPerson()

        val response = controller.findBySearchTerms(
            db,
            user,
            searchTerm = "${person.firstName}\t${person.lastName}",
            orderBy = "first_name",
            sortDirection = "DESC"
        )

        assertEquals(HttpStatus.OK, response?.statusCode)

        with(response?.body!!) {
            assertEquals(person.id, this.first().id)
        }
    }

    @Test
    fun `Search treats non-breaking spaces as spaces in search terms`() {
        val user = AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.SERVICE_WORKER))
        val person = createPerson()

        val response = controller.findBySearchTerms(
            db,
            user,
            searchTerm = "${person.firstName}\u00A0${person.lastName}",
            orderBy = "first_name",
            sortDirection = "DESC"
        )

        assertEquals(HttpStatus.OK, response?.statusCode)

        with(response?.body!!) {
            assertEquals(person.id, this.first().id)
        }
    }

    @Test
    fun `Search treats obscrube unicode spaces as spaces in search terms`() {
        val user = AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.SERVICE_WORKER))
        val person = createPerson()

        // IDEOGRAPHIC SPACE, not supported by default in regexes
        // unless Java's Pattern.UNICODE_CHARACTER_CLASS-like functionality is enabled.
        val response = controller.findBySearchTerms(
            db,
            user,
            searchTerm = "${person.firstName}\u3000${person.lastName}",
            orderBy = "first_name",
            sortDirection = "DESC"
        )

        assertEquals(HttpStatus.OK, response?.statusCode)

        with(response?.body!!) {
            assertEquals(person.id, this.first().id)
        }
    }

    private fun updateContactInfo(user: AuthenticatedUser) {
        val person = createPerson()
        val actual = controller.updateContactInfo(db, user, person.id, contactInfo)

        assertEquals(HttpStatus.OK, actual.statusCode)

        val updated = db.read { it.getPersonById(person.id) }
        assertEquals(contactInfo.email, updated?.email)
        assertEquals(contactInfo.invoicingStreetAddress, updated?.invoicingStreetAddress)
        assertEquals(contactInfo.invoicingPostalCode, updated?.invoicingPostalCode)
        assertEquals(contactInfo.invoicingPostOffice, updated?.invoicingPostOffice)
        assertEquals(contactInfo.forceManualFeeDecisions, updated?.forceManualFeeDecisions)
    }

    private fun createPerson(): PersonDTO {
        val ssn = "140881-172X"
        return db.transaction {
            it.createPerson(
                PersonIdentityRequest(
                    identity = ExternalIdentifier.SSN.getInstance(ssn),
                    firstName = "Matti",
                    lastName = "Meikäläinen",
                    email = "",
                    language = "fi"
                )
            )
        }
    }
}
