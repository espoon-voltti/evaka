// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.dao

import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.identity.getDobFromSsn
import fi.espoo.evaka.pis.AbstractIntegrationTest
import fi.espoo.evaka.pis.service.ContactInfo
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.pis.service.PersonIdentityRequest
import junit.framework.TestCase.assertTrue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID

class PersonDAOIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var personDAO: PersonDAO

    @Test
    fun `get person identity`() {
        val validSSN = "080512A918W"
        val fetchedPerson = personDAO.getOrCreatePersonIdentity(
            PersonIdentityRequest(
                identity = ExternalIdentifier.SSN.getInstance(validSSN),
                firstName = "Matti",
                lastName = "Meikäläinen",
                email = "matti.meikalainen@example.com",
                language = "fi"
            )
        )
        assertNotNull(fetchedPerson)
        assertNotNull(fetchedPerson.id)
        assertNotNull(fetchedPerson.customerId)
        assertEquals(validSSN, (fetchedPerson.identity as ExternalIdentifier.SSN).ssn)
        assertEquals(LocalDate.of(2012, 5, 8), fetchedPerson.dateOfBirth)
        assertEquals("fi", fetchedPerson.language)
    }

    @Test
    fun `createPersonFromVtj creates person with correct data`() {
        val tempId = UUID.randomUUID()
        val tempCustomerId = 0L
        val validSSN = "010199-8137"

        val inputPerson = testPerson(validSSN)

        val created = personDAO.createPersonFromVtj(inputPerson)

        assertNotEquals(tempId, created.id)
        assertNotEquals(tempCustomerId, created.customerId)

        assertEquals(validSSN, created.identity.toString())
        assertEquals(inputPerson.dateOfBirth, created.dateOfBirth)
        assertEquals(inputPerson.firstName, created.firstName)
        assertEquals(inputPerson.lastName, created.lastName)

        assertEquals(inputPerson.email, created.email)
        assertEquals(inputPerson.phone, created.phone)
        assertEquals(inputPerson.language, created.language)
        assertEquals(inputPerson.nationalities, created.nationalities)

        assertEquals(inputPerson.streetAddress, created.streetAddress)
        assertEquals(inputPerson.postalCode, created.postalCode)
        assertEquals(inputPerson.postOffice, created.postOffice)

        assertEquals(inputPerson.restrictedDetailsEndDate, created.restrictedDetailsEndDate)
    }

    @Test
    fun `updatePersonFromVtj updates person with correct data`() {
        val validSSN = "230493-332S"

        val originalPerson = testPerson(validSSN)

        val beforeUpdate = personDAO.createPersonFromVtj(originalPerson)

        val updated = beforeUpdate.copy(
            firstName = "dfhjcn",
            lastName = "bvxdafs",
            language = "sv",
            nationalities = listOf("087"),

            streetAddress = "Muutie 8",
            postalCode = "00001",
            postOffice = "Muula",

            restrictedDetailsEnabled = false,
            restrictedDetailsEndDate = null
        )

        val actual = personDAO.updatePersonFromVtj(updated)

        assertEquals(updated.id, actual.id)
        assertEquals(updated.customerId, actual.customerId)

        assertEquals(updated.identity.toString(), actual.identity.toString())
        assertEquals(updated.dateOfBirth, actual.dateOfBirth)
        assertEquals(updated.firstName, actual.firstName)
        assertEquals(updated.lastName, actual.lastName)

        assertEquals(updated.email, actual.email)
        assertEquals(updated.phone, actual.phone)
        assertEquals(updated.language, actual.language)
        assertEquals(updated.nationalities, actual.nationalities)

        assertEquals(updated.streetAddress, actual.streetAddress)
        assertEquals(updated.postalCode, actual.postalCode)
        assertEquals(updated.postOffice, actual.postOffice)

        assertEquals(updated.restrictedDetailsEndDate, actual.restrictedDetailsEndDate)
    }

    @Test
    fun `end user's contact info can be updated`() {
        val validSSN = "230493-332S"
        val originalPerson = personDAO.createPersonFromVtj(testPerson(validSSN))

        val contactInfo = ContactInfo(
            email = "test@emai.l",
            phone = "+3584012345678",
            invoiceRecipientName = "Laskun saaja",
            invoicingStreetAddress = "Laskutusosoite",
            invoicingPostalCode = "02123",
            invoicingPostOffice = "Espoo"
        )

        assertTrue(personDAO.updateEndUsersContactInfo(originalPerson.id, contactInfo))

        val actual = personDAO.getPersonByVolttiId(originalPerson.id)
        assertEquals(contactInfo.email, actual?.email)
        assertEquals(contactInfo.phone, actual?.phone)
        assertEquals(contactInfo.invoicingStreetAddress, actual?.invoicingStreetAddress)
        assertEquals(contactInfo.invoicingPostalCode, actual?.invoicingPostalCode)
        assertEquals(contactInfo.invoicingPostOffice, actual?.invoicingPostOffice)
    }

    @Test
    fun `person can be found by ssn`() {
        val created = createVtjPerson()
        val persons = personDAO.findBySearchTerms("010199-8137", "last_name", "ASC")
        assertEquals(1, persons.size)
        assertEquals(persons[0].identity, created.identity)
    }

    @Test
    fun `person can be found by first part of address`() {
        val created = createVtjPerson()
        val persons = personDAO.findBySearchTerms("Jokutie", "last_name", "ASC")
        assertEquals(persons[0].streetAddress, created.streetAddress)
    }

    @Test
    fun `person can be found by last name`() {
        val created = createVtjPerson()
        val persons = personDAO.findBySearchTerms("O'Brien", "last_name", "ASC")

        assertEquals(persons[0].lastName, created.lastName)
    }

    @Test
    fun `person can be found by first name`() {
        val created = createVtjPerson()
        val persons = personDAO.findBySearchTerms("Matti", "last_name", "ASC")

        assertEquals(persons[0].firstName, created.firstName)
    }

    @Test
    fun `person with multiple first names can be found`() {
        val created = createVtjPerson()
        val persons = personDAO.findBySearchTerms("Matti Jari-Ville", "last_name", "ASC")

        assertEquals(persons[0].firstName, created.firstName)
    }

    @Test
    fun `person can be found with the full address`() {
        val created = createVtjPerson()
        val persons = personDAO.findBySearchTerms("Jokutie 66", "last_name", "ASC")

        assertEquals(persons[0].streetAddress, created.streetAddress)
    }

    @Test
    fun `person can be found by partial last name`() {
        val created = createVtjPerson()
        val persons = personDAO.findBySearchTerms("O'Br", "last_name", "ASC")

        assertEquals(persons[0].lastName, created.lastName)
    }

    @Test
    fun `person can be found by partial first name`() {
        val created = createVtjPerson()
        val persons = personDAO.findBySearchTerms("Matt", "last_name", "ASC")

        assertEquals(persons[0].firstName, created.firstName)
    }

    @Test
    fun `person can be found by partial address`() {
        val created = createVtjPerson()
        val persons = personDAO.findBySearchTerms("Jokut", "last_name", "ASC")

        assertEquals(persons[0].streetAddress, created.streetAddress)
    }

    @Test
    fun `person can not be found with just a substring match on first name`() {
        createVtjPerson()
        val persons = personDAO.findBySearchTerms("atti", "last_name", "ASC")

        assertEquals(persons, emptyList<PersonDTO>())
    }

    @Test
    fun `person can not be found with just a substring match on last name`() {
        createVtjPerson()
        val persons = personDAO.findBySearchTerms("eikäläine", "last_name", "ASC")

        assertEquals(persons, emptyList<PersonDTO>())
    }

    @Test
    fun `person can not be found with just a substring match on address`() {
        createVtjPerson()
        val persons = personDAO.findBySearchTerms("okuti", "last_name", "ASC")

        assertEquals(persons, emptyList<PersonDTO>())
    }

    @Test
    fun `PostgreSQL text search operator characters are ignored in search terms`() {
        val created = createVtjPerson()
        val persons = personDAO.findBySearchTerms("'&Jokut!|&", "last_name", "ASC")

        assertEquals(1, persons.size)
        assertEquals(persons[0].identity, created.identity)
    }

    @Test
    fun `angle brackets are ignored in search terms`() {
        val created = createVtjPerson()
        val persons = personDAO.findBySearchTerms("<Jokut>", "last_name", "ASC")

        assertEquals(1, persons.size)
        assertEquals(persons[0].identity, created.identity)
    }

    @Test
    fun `single quote is ignored at the start of search terms`() {
        val created = createVtjPerson()
        val persons = personDAO.findBySearchTerms("'O'Brien", "last_name", "ASC")

        assertEquals(1, persons.size)
        assertEquals(persons[0].identity, created.identity)
    }

    private fun createVtjPerson(): PersonDTO {
        val validSSN = "010199-8137"
        val inputPerson = testPerson(validSSN)
        return personDAO.createPersonFromVtj(inputPerson)
    }

    private fun testPerson(validSSN: String): PersonDTO {
        return PersonDTO(
            id = UUID.randomUUID(),
            customerId = 0L,
            identity = ExternalIdentifier.SSN.getInstance(validSSN),
            dateOfBirth = getDobFromSsn(validSSN),
            firstName = "Matti Pekka Jari-Ville",
            lastName = "O'Brien",
            email = null,
            phone = null,
            language = "fi",
            nationalities = listOf("248", "060"),

            streetAddress = "Jokutie 66",
            postalCode = "00000",
            postOffice = "Jokula",

            restrictedDetailsEnabled = true,
            restrictedDetailsEndDate = LocalDate.now().plusYears(1)
        )
    }
}
