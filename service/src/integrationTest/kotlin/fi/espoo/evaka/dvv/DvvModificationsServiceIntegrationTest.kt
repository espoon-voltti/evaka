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

class DvvModificationsServiceIntegrationTest : DvvModificationsServiceIntegrationTestBase() {

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
        createTestPerson(testPerson.copy(ssn = "010180-999A"))
        dvvModificationsService.updatePersonsFromDvv(listOf("010180-999A"))
        assertEquals(LocalDate.parse("2019-07-30"), h.getPersonBySSN("010180-999A")?.dateOfDeath)
    }

    @Test
    fun `person restricted details started`() = jdbi.handle { h ->
        createTestPerson(testPerson.copy(ssn = "020180-999Y"))
        dvvModificationsService.updatePersonsFromDvv(listOf("020180-999Y"))
        assertEquals(true, h.getPersonBySSN("020180-999Y")?.restrictedDetailsEnabled)
    }

    @Test
    fun `person restricted details ended`() = jdbi.handle { h ->
        createTestPerson(testPerson.copy(ssn = "030180-999L", restrictedDetailsEnabled = true))
        dvvModificationsService.updatePersonsFromDvv(listOf("030180-999L"))
        assertEquals(false, h.getPersonBySSN("030180-999L")?.restrictedDetailsEnabled)
        assertEquals(LocalDate.parse("2030-01-01"), h.getPersonBySSN("030180-999L")?.restrictedDetailsEndDate)
    }

    @Test
    fun `person ssn change`() = jdbi.handle { h ->
        val testId = createTestPerson(testPerson.copy(ssn = "010181-999K"))
        dvvModificationsService.updatePersonsFromDvv(listOf("010181-999K"))
        assertEquals(testId, h.getPersonBySSN("010281-999C")?.id)
    }

    val testPerson = DevPerson(
        id = UUID.randomUUID(),
        ssn = "set this",
        dateOfBirth = LocalDate.parse("1980-01-01"),
        dateOfDeath = null,
        firstName = "etunimi",
        lastName = "sukunimi",
        streetAddress = "Katuosoite",
        postalCode = "02230",
        postOffice = "Espoo",
        restrictedDetailsEnabled = false
    )

    private fun createTestPerson(devPerson: DevPerson): UUID = jdbi.handle { h ->
        h.insertTestPerson(devPerson)
    }
}
