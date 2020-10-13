// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
package fi.espoo.evaka.dvv

import fi.espoo.evaka.pis.getPersonBySSN
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.insertTestPerson
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class DvvModificationServiceIntegrationTest : DvvModificationServiceIntegrationTestBase() {

    @BeforeEach
    private fun beforeEach() {
        jdbi.handle { h ->
            resetDatabase(h)
            storeDvvModificationToken(h, "100", "101", 0, 0)
        }
    }

    @AfterEach
    private fun afterEach() {
        jdbi.handle { h ->
            deleteDvvModificationToken(h, "100")
        }
    }

    @Test
    fun `get modification token for today`() = jdbi.handle { h ->
        assertEquals("101", getNextDvvModificationToken(h))
        val response = dvvModificationsService.getDvvModifications(listOf("nimenmuutos"))
        assertEquals(1, response.size)
        assertEquals("102", getNextDvvModificationToken(h))
        val createdDvvModificationToken = getDvvModificationToken(h, "101")!!
        assertEquals("101", createdDvvModificationToken.token)
        assertEquals("102", createdDvvModificationToken.nextToken)
        assertEquals(1, createdDvvModificationToken.ssnsSent)
        assertEquals(1, createdDvvModificationToken.modificationsReceived)

        deleteDvvModificationToken(h, "101")
    }

    @Test
    fun `person date of death`() = jdbi.handle { h ->
        h.insertTestPerson(
            DevPerson(
                id = UUID.randomUUID(),
                dateOfBirth = LocalDate.parse("1920-11-01"),
                dateOfDeath = null,
                ssn = "010180-999A",
                firstName = "etunimi",
                lastName = "sukunimi",
                streetAddress = "Katuosoite",
                postalCode = "02230",
                postOffice = "Espoo"
            )
        )
        dvvModificationsService.updatePersonsFromDvv(listOf("010180-999A"))
        assertEquals(LocalDate.parse("2019-07-30"), h.getPersonBySSN("010180-999A")?.dateOfDeath)
    }
}
