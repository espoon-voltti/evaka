// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.dao

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.identity.getDobFromSsn
import fi.espoo.evaka.pis.createEmptyPerson
import fi.espoo.evaka.pis.createPerson
import fi.espoo.evaka.pis.createPersonFromVtj
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.pis.searchPeople
import fi.espoo.evaka.pis.service.ContactInfo
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.pis.service.PersonIdentityRequest
import fi.espoo.evaka.pis.updatePersonContactInfo
import fi.espoo.evaka.pis.updatePersonFromVtj
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class PersonQueriesIntegrationTest : PureJdbiTest() {
    @BeforeEach
    fun setUp() {
        val legacyDataSql = this.javaClass.getResource("/legacy_db_data.sql").readText()
        db.transaction {
            it.resetDatabase()
            it.execute(legacyDataSql)
        }
    }

    @Test
    fun `creating an empty person sets their date of birth to current date`() = db.transaction { tx ->
        val identity: PersonDTO = tx.createEmptyPerson()
        Assertions.assertEquals(identity.dateOfBirth, LocalDate.now())
    }

    @Test
    fun `create person`() {
        val validSSN = "080512A918W"
        val fetchedPerson = db.transaction { tx ->
            tx.createPerson(
                PersonIdentityRequest(
                    identity = ExternalIdentifier.SSN.getInstance(validSSN),
                    firstName = "Matti",
                    lastName = "Meikäläinen",
                    email = "matti.meikalainen@example.com",
                    language = "fi"
                )
            )
        }

        Assertions.assertNotNull(fetchedPerson)
        Assertions.assertNotNull(fetchedPerson.id)
        Assertions.assertNotNull(fetchedPerson.customerId)
        Assertions.assertEquals(validSSN, (fetchedPerson.identity as ExternalIdentifier.SSN).ssn)
        Assertions.assertEquals(LocalDate.of(2012, 5, 8), fetchedPerson.dateOfBirth)
        Assertions.assertEquals("fi", fetchedPerson.language)
    }

    @Test
    fun `createPersonFromVtj creates person with correct data`() {
        val tempId = UUID.randomUUID()
        val tempCustomerId = 0L
        val validSSN = "010199-8137"

        val inputPerson = testPerson(validSSN)

        val created = db.transaction { it.createPersonFromVtj(inputPerson) }

        Assertions.assertNotEquals(tempId, created.id)
        Assertions.assertNotEquals(tempCustomerId, created.customerId)

        Assertions.assertEquals(validSSN, created.identity.toString())
        Assertions.assertEquals(inputPerson.dateOfBirth, created.dateOfBirth)
        Assertions.assertEquals(inputPerson.firstName, created.firstName)
        Assertions.assertEquals(inputPerson.lastName, created.lastName)

        Assertions.assertEquals(inputPerson.email, created.email)
        Assertions.assertEquals(inputPerson.phone, created.phone)
        Assertions.assertEquals(inputPerson.language, created.language)
        Assertions.assertEquals(inputPerson.nationalities, created.nationalities)

        Assertions.assertEquals(inputPerson.streetAddress, created.streetAddress)
        Assertions.assertEquals(inputPerson.postalCode, created.postalCode)
        Assertions.assertEquals(inputPerson.postOffice, created.postOffice)

        Assertions.assertEquals(inputPerson.restrictedDetailsEndDate, created.restrictedDetailsEndDate)
    }

    @Test
    fun `updatePersonFromVtj updates person with correct data`() {
        val validSSN = "230493-332S"

        val originalPerson = testPerson(validSSN)

        val beforeUpdate = db.transaction { it.createPersonFromVtj(originalPerson) }

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

        val actual = db.transaction { it.updatePersonFromVtj(updated) }

        Assertions.assertEquals(updated.id, actual.id)
        Assertions.assertEquals(updated.customerId, actual.customerId)

        Assertions.assertEquals(updated.identity.toString(), actual.identity.toString())
        Assertions.assertEquals(updated.dateOfBirth, actual.dateOfBirth)
        Assertions.assertEquals(updated.firstName, actual.firstName)
        Assertions.assertEquals(updated.lastName, actual.lastName)

        Assertions.assertEquals(updated.email, actual.email)
        Assertions.assertEquals(updated.phone, actual.phone)
        Assertions.assertEquals(updated.language, actual.language)
        Assertions.assertEquals(updated.nationalities, actual.nationalities)

        Assertions.assertEquals(updated.streetAddress, actual.streetAddress)
        Assertions.assertEquals(updated.postalCode, actual.postalCode)
        Assertions.assertEquals(updated.postOffice, actual.postOffice)

        Assertions.assertEquals(updated.restrictedDetailsEndDate, actual.restrictedDetailsEndDate)
    }

    @Test
    fun `end user's contact info can be updated`() {
        val validSSN = "230493-332S"
        val originalPerson = db.transaction { it.createPersonFromVtj(testPerson(validSSN)) }

        val contactInfo = ContactInfo(
            email = "test@emai.l",
            phone = "+3584012345678",
            backupPhone = "",
            invoiceRecipientName = "Laskun saaja",
            invoicingStreetAddress = "Laskutusosoite",
            invoicingPostalCode = "02123",
            invoicingPostOffice = "Espoo"
        )

        Assertions.assertTrue(db.transaction { it.updatePersonContactInfo(originalPerson.id, contactInfo) })

        val actual = db.read { it.getPersonById(originalPerson.id) }
        Assertions.assertEquals(contactInfo.email, actual?.email)
        Assertions.assertEquals(contactInfo.phone, actual?.phone)
        Assertions.assertEquals(contactInfo.invoicingStreetAddress, actual?.invoicingStreetAddress)
        Assertions.assertEquals(contactInfo.invoicingPostalCode, actual?.invoicingPostalCode)
        Assertions.assertEquals(contactInfo.invoicingPostOffice, actual?.invoicingPostOffice)
    }

    private val adminUser = AuthenticatedUser.Employee(id = UUID.randomUUID(), roles = setOf(UserRole.ADMIN))

    @Test
    fun `person can be found by ssn`() {
        val created = db.transaction { createVtjPerson(it) }
        val persons = db.read { it.searchPeople(adminUser, "010199-8137", "last_name", "ASC") }
        Assertions.assertEquals(1, persons.size)
        Assertions.assertEquals(persons[0].identity, created.identity)
    }

    @Test
    fun `person can be found by first part of address`() {
        val created = db.transaction { createVtjPerson(it) }
        val persons = db.read { it.searchPeople(adminUser, "Jokutie", "last_name", "ASC") }
        Assertions.assertEquals(persons[0].streetAddress, created.streetAddress)
    }

    @Test
    fun `person can be found by last name`() {
        val created = db.transaction { createVtjPerson(it) }
        val persons = db.read { it.searchPeople(adminUser, "O'Brien", "last_name", "ASC") }

        Assertions.assertEquals(persons[0].lastName, created.lastName)
    }

    @Test
    fun `person can be found by first name`() {
        val created = db.transaction { createVtjPerson(it) }
        val persons = db.read { it.searchPeople(adminUser, "Matti", "last_name", "ASC") }

        Assertions.assertEquals(persons[0].firstName, created.firstName)
    }

    @Test
    fun `person can be found by first name when it contains umlauts`() {
        val created = db.transaction { createVtjPerson(it) }
        val persons = db.read { it.searchPeople(adminUser, "Yrjö", "last_name", "ASC") }

        Assertions.assertEquals(persons[0].firstName, created.firstName)
    }

    @Test
    fun `unit supervisor cannot find a child without acl access`() {
        db.transaction { createVtjPerson(it) }
        val persons = db.read { it.searchPeople(AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.UNIT_SUPERVISOR)), "Matti", "last_name", "ASC") }

        Assertions.assertEquals(0, persons.size)
    }

    @Test
    fun `special education teacher cannot find a child without acl access`() {
        db.transaction { createVtjPerson(it) }
        val persons = db.read { it.searchPeople(AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.SPECIAL_EDUCATION_TEACHER)), "Matti", "last_name", "ASC") }

        Assertions.assertEquals(0, persons.size)
    }

    @Test
    fun `person with multiple first names can be found`() {
        val created = db.transaction { createVtjPerson(it) }
        val persons = db.read { it.searchPeople(adminUser, "Matti Jari-Ville", "last_name", "ASC") }

        Assertions.assertEquals(persons[0].firstName, created.firstName)
    }

    @Test
    fun `person can be found with the full address`() {
        val created = db.transaction { createVtjPerson(it) }
        val persons = db.read { it.searchPeople(adminUser, "Jokutie 66", "last_name", "ASC") }

        Assertions.assertEquals(persons[0].streetAddress, created.streetAddress)
    }

    @Test
    fun `person can be found by partial last name`() {
        val created = db.transaction { createVtjPerson(it) }
        val persons = db.read { it.searchPeople(adminUser, "O'Br", "last_name", "ASC") }

        Assertions.assertEquals(persons[0].lastName, created.lastName)
    }

    @Test
    fun `person can be found by partial first name`() {
        val created = db.transaction { createVtjPerson(it) }
        val persons = db.read { it.searchPeople(adminUser, "Matt", "last_name", "ASC") }

        Assertions.assertEquals(persons[0].firstName, created.firstName)
    }

    @Test
    fun `person can be found by partial address`() {
        val created = db.transaction { createVtjPerson(it) }
        val persons = db.read { it.searchPeople(adminUser, "Jokut", "last_name", "ASC") }

        Assertions.assertEquals(persons[0].streetAddress, created.streetAddress)
    }

    @Test
    fun `person can not be found with just a substring match on first name`() {
        db.transaction { createVtjPerson(it) }
        val persons = db.read { it.searchPeople(adminUser, "atti", "last_name", "ASC") }

        Assertions.assertEquals(persons, emptyList<PersonDTO>())
    }

    @Test
    fun `person can not be found with just a substring match on last name`() {
        db.transaction { createVtjPerson(it) }
        val persons = db.read { it.searchPeople(adminUser, "eikäläine", "last_name", "ASC") }

        Assertions.assertEquals(persons, emptyList<PersonDTO>())
    }

    @Test
    fun `person can not be found with just a substring match on address`() {
        db.transaction { createVtjPerson(it) }
        val persons = db.read { it.searchPeople(adminUser, "okuti", "last_name", "ASC") }

        Assertions.assertEquals(persons, emptyList<PersonDTO>())
    }

    @Test
    fun `PostgreSQL text search operator characters are ignored in search terms`() {
        val created = db.transaction { createVtjPerson(it) }
        val persons = db.read { it.searchPeople(adminUser, "'&Jokut!|&", "last_name", "ASC") }

        Assertions.assertEquals(1, persons.size)
        Assertions.assertEquals(persons[0].identity, created.identity)
    }

    @Test
    fun `angle brackets are ignored in search terms`() {
        val created = db.transaction { createVtjPerson(it) }
        val persons = db.read { it.searchPeople(adminUser, "<Jokut>", "last_name", "ASC") }

        Assertions.assertEquals(1, persons.size)
        Assertions.assertEquals(persons[0].identity, created.identity)
    }

    @Test
    fun `single quote is ignored at the start of search terms`() {
        val created = db.transaction { createVtjPerson(it) }
        val persons = db.read { it.searchPeople(adminUser, "'O'Brien", "last_name", "ASC") }

        Assertions.assertEquals(1, persons.size)
        Assertions.assertEquals(persons[0].identity, created.identity)
    }

    private fun createVtjPerson(tx: Database.Transaction): PersonDTO {
        val validSSN = "010199-8137"
        val inputPerson = testPerson(validSSN)
        return tx.createPersonFromVtj(inputPerson)
    }

    private fun testPerson(validSSN: String): PersonDTO {
        return PersonDTO(
            id = UUID.randomUUID(),
            customerId = 0L,
            identity = ExternalIdentifier.SSN.getInstance(validSSN),
            dateOfBirth = getDobFromSsn(validSSN),
            firstName = "Matti Yrjö Jari-Ville",
            lastName = "O'Brien",
            email = null,
            phone = null,
            backupPhone = "",
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
