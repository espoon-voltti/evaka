// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
package fi.espoo.evaka.dvv

import fi.espoo.evaka.pis.getPersonBySSN
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.insertTestPerson
import fi.espoo.evaka.vtjclient.dto.NativeLanguage
import fi.espoo.evaka.vtjclient.dto.RestrictedDetails
import fi.espoo.evaka.vtjclient.dto.VtjPerson
import fi.espoo.evaka.vtjclient.service.persondetails.MockPersonDetailsService
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
        val response = dvvModificationsService.getDvvModifications(h, listOf("nimenmuutos"))
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
        dvvModificationsService.updatePersonsFromDvv(h, listOf("010180-999A"))
        assertEquals(LocalDate.parse("2019-07-30"), h.getPersonBySSN("010180-999A")?.dateOfDeath)
    }

    @Test
    fun `person restricted details started`() = jdbi.handle { h ->
        createTestPerson(testPerson.copy(ssn = "020180-999Y"))
        dvvModificationsService.updatePersonsFromDvv(h, listOf("020180-999Y"))
        val modifiedPerson = h.getPersonBySSN("020180-999Y")!!
        assertEquals(true, modifiedPerson.restrictedDetailsEnabled)
        assertEquals("", modifiedPerson.streetAddress)
        assertEquals("", modifiedPerson.postalCode)
        assertEquals("", modifiedPerson.postOffice)
    }

    @Test
    fun `person restricted details ended and address is set`() = jdbi.handle { h ->
        createTestPerson(testPerson.copy(ssn = "030180-999L", restrictedDetailsEnabled = true, streetAddress = "", postalCode = "", postOffice = ""))
        dvvModificationsService.updatePersonsFromDvv(h, listOf("030180-999L"))
        val modifiedPerson = h.getPersonBySSN("030180-999L")!!
        assertEquals(false, modifiedPerson.restrictedDetailsEnabled)
        assertEquals(LocalDate.parse("2030-01-01"), modifiedPerson.restrictedDetailsEndDate)
        assertEquals("Vanhakatu 10h5 3", modifiedPerson.streetAddress)
        assertEquals("02230", modifiedPerson.postalCode)
        assertEquals("Espoo", modifiedPerson.postOffice)
    }

    @Test
    fun `person address change`() = jdbi.handle { h ->
        createTestPerson(testPerson.copy(ssn = "040180-9998"))
        dvvModificationsService.updatePersonsFromDvv(h, listOf("040180-9998"))
        val modifiedPerson = h.getPersonBySSN("040180-9998")!!
        assertEquals("Uusitie 17 A 2", modifiedPerson.streetAddress)
        assertEquals("02940", modifiedPerson.postalCode)
        assertEquals("ESPOO", modifiedPerson.postOffice)
        assertEquals("123456789V1B033 ", modifiedPerson.residenceCode)
    }

    @Test
    fun `person ssn change`() = jdbi.handle { h ->
        val testId = createTestPerson(testPerson.copy(ssn = "010181-999K"))
        dvvModificationsService.updatePersonsFromDvv(h, listOf("010181-999K"))
        assertEquals(testId, h.getPersonBySSN("010281-999C")?.id)
    }

    @Test
    fun `new custodian added`() = jdbi.handle { h ->
        var caretaker: DevPerson = testPerson.copy(ssn = "050180-999W")
        val custodian = testPerson.copy(firstName = "Harri", lastName = "Huollettava", ssn = "050118A999W", guardians = listOf(caretaker))
        caretaker = caretaker.copy(dependants = listOf(custodian))

        createTestPerson(custodian)
        createVtjPerson(custodian)
        createVtjPerson(caretaker)

        dvvModificationsService.updatePersonsFromDvv(h, listOf("050118A999W"))
        val dvvCustodian = h.getPersonBySSN("050118A999W")!!
        assertEquals("Harri", dvvCustodian.firstName)
        assertEquals("Huollettava", dvvCustodian.lastName)
    }

    @Test
    fun `new caretaker added`() = jdbi.handle { h ->
        var custodian: DevPerson = testPerson.copy(ssn = "060118A999J")
        val caretaker = testPerson.copy(firstName = "Harri", lastName = "Huoltaja", ssn = "060180-999J", dependants = listOf(custodian))
        custodian = custodian.copy(guardians = listOf(caretaker))

        createTestPerson(custodian)
        createVtjPerson(custodian)
        createVtjPerson(caretaker)

        dvvModificationsService.updatePersonsFromDvv(h, listOf("060118A999J"))
        val dvvCaretaker = h.getPersonBySSN("060180-999J")!!
        assertEquals("Harri", dvvCaretaker.firstName)
        assertEquals("Huoltaja", dvvCaretaker.lastName)
    }

    @Test
    fun `name changed`() = jdbi.handle { h ->
        val SSN = "010179-9992"
        var personWithOldName: DevPerson = testPerson.copy(firstName = "Ville", lastName = "Vanhanimi", ssn = SSN)
        val personWithNewName = testPerson.copy(firstName = "Urkki", lastName = "Uusinimi", ssn = SSN)

        createTestPerson(personWithOldName)
        createVtjPerson(personWithNewName)

        dvvModificationsService.updatePersonsFromDvv(h, listOf(SSN))
        val updatedPerson = h.getPersonBySSN(SSN)!!
        assertEquals("Urkki", updatedPerson.firstName)
        assertEquals("Uusinimi", updatedPerson.lastName)
    }

    @Test
    fun `paging works`() = jdbi.handle { h ->
        // The mock server has been rigged so that if the token is negative, it will return the requested batch with
        // ajanTasalla=false and next token = token + 1 causing the dvv client to do a request for the subsequent page,
        // until token is 0 and then it will return ajanTasalla=true
        // So if the paging works correctly there should Math.abs(original_token) + 1 identical records
        resetDatabase(h)
        storeDvvModificationToken(h, "10000", "-2", 0, 0)
        try {
            createTestPerson(testPerson.copy(ssn = "010180-999A"))
            assertEquals(3, dvvModificationsService.updatePersonsFromDvv(h, listOf("010180-999A")))
            assertEquals("1", getNextDvvModificationToken(h))
            assertEquals(LocalDate.parse("2019-07-30"), h.getPersonBySSN("010180-999A")?.dateOfDeath)
        } finally {
            deleteDvvModificationToken(h, "0")
        }
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

    private fun createVtjPerson(person: DevPerson) {
        MockPersonDetailsService.addPerson(
            VtjPerson(
                socialSecurityNumber = person.ssn!!,
                firstNames = person.firstName,
                lastName = person.lastName,
                nativeLanguage = NativeLanguage(languageName = "FI", code = "fi"),
                restrictedDetails = RestrictedDetails(enabled = person.restrictedDetailsEnabled, endDate = person.restrictedDetailsEndDate),
                guardians = person.guardians.map(::asVtjPerson),
                dependants = person.dependants.map(::asVtjPerson)
            )
        )
    }

    private fun asVtjPerson(person: DevPerson): VtjPerson = VtjPerson(
        socialSecurityNumber = person.ssn!!,
        firstNames = person.firstName,
        lastName = person.lastName,
        nativeLanguage = NativeLanguage(languageName = "FI", code = "fi"),
        restrictedDetails = RestrictedDetails(enabled = person.restrictedDetailsEnabled, endDate = person.restrictedDetailsEndDate)
    )

    private fun deleteVtjPerson(ssn: String) {
        MockPersonDetailsService.deletePerson(ssn)
    }
}
