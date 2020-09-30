// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.identity.VolttiIdentifier
import fi.espoo.evaka.pis.AbstractIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID

private val validSSN = "080512A918W"
private val ssn = ExternalIdentifier.SSN.getInstance(validSSN)
class PersonServiceIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var service: PersonService

    private val personId: VolttiIdentifier = UUID.randomUUID()

    @Test
    fun `creating an empty person sets their date of birth to current date`() {
        val identity: PersonDTO = service.createEmpty()
        assertEquals(identity.dateOfBirth, LocalDate.now())
    }

    @Test
    fun `getting non-existent person with random id returns null`() {
        val identity: PersonDTO? = service.getPerson(personId)
        assertNull(identity)
    }

    @Test
    fun `getting non-existent person returns person with generated UUID`() {
        val person = service.getOrCreatePersonIdentity(
            PersonIdentityRequest(
                identity = ssn,
                firstName = "Matti",
                lastName = "Meikäläinen",
                email = "matti.meikalainen@example.com",
                language = "fi"
            )
        )
        assertNotNull(person)
        assertEquals(validSSN, (person.identity as ExternalIdentifier.SSN).ssn)
    }
}
