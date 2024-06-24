// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.dao

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.identity.getDobFromSsn
import fi.espoo.evaka.pis.createEmptyPerson
import fi.espoo.evaka.pis.createPersonFromVtj
import fi.espoo.evaka.pis.searchPeople
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.pis.updatePersonFromVtj
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import org.junit.jupiter.api.Test

class PersonQueriesIntegrationTest : PureJdbiTest(resetDbBeforeEach = true) {
    @Test
    fun `creating an empty person sets their date of birth to current date`() =
        db.transaction { tx ->
            val now = HelsinkiDateTime.of(LocalDate.of(2019, 1, 1), LocalTime.of(12, 0, 1))
            val identity: PersonDTO = tx.createEmptyPerson(MockEvakaClock(now))
            assertEquals(identity.dateOfBirth, now.toLocalDate())
        }

    @Test
    fun `createPersonFromVtj creates person with correct data`() {
        val tempId = PersonId(UUID.randomUUID())
        val validSSN = "010199-8137"

        val inputPerson = testPerson(validSSN)

        val created = db.transaction { it.createPersonFromVtj(inputPerson) }

        assertNotEquals(tempId, created.id)

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

        val beforeUpdate = db.transaction { it.createPersonFromVtj(originalPerson) }

        val updated =
            beforeUpdate.copy(
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

        assertEquals(updated.id, actual.id)

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

    private val adminUser =
        AuthenticatedUser.Employee(
            id = EmployeeId(UUID.randomUUID()),
            roles = setOf(UserRole.ADMIN)
        )

    @Test
    fun `person can be found by ssn`() {
        val created = db.transaction { createVtjPerson(it) }
        val persons =
            db.read { it.searchPeople(adminUser, "010199-8137", "last_name", "ASC", false) }
        assertEquals(1, persons.size)
        assertEquals(
            persons[0].socialSecurityNumber,
            (created.identity as? ExternalIdentifier.SSN)?.ssn
        )
    }

    @Test
    fun `person can be found by case-insensitive ssn`() {
        val created = db.transaction { createVtjPerson(it, "230601A329J") }
        val persons =
            db.read { it.searchPeople(adminUser, "230601a329J", "last_name", "ASC", false) }
        assertEquals(1, persons.size)
        assertEquals(
            persons[0].socialSecurityNumber,
            (created.identity as? ExternalIdentifier.SSN)?.ssn
        )
    }

    @Test
    fun `person can be found by first part of address`() {
        val created = db.transaction { createVtjPerson(it) }
        val persons = db.read { it.searchPeople(adminUser, "Jokutie", "last_name", "ASC", false) }
        assertEquals(persons[0].streetAddress, created.streetAddress)
    }

    @Test
    fun `person can be found by last name`() {
        val created = db.transaction { createVtjPerson(it) }
        val persons = db.read { it.searchPeople(adminUser, "O'Brien", "last_name", "ASC", false) }

        assertEquals(persons[0].lastName, created.lastName)
    }

    @Test
    fun `person can be found by first name`() {
        val created = db.transaction { createVtjPerson(it) }
        val persons = db.read { it.searchPeople(adminUser, "Matti", "last_name", "ASC", false) }

        assertEquals(persons[0].firstName, created.firstName)
    }

    @Test
    fun `person can be found by first name when it contains umlauts`() {
        val created = db.transaction { createVtjPerson(it) }
        val persons = db.read { it.searchPeople(adminUser, "Yrjö", "last_name", "ASC", false) }

        assertEquals(persons[0].firstName, created.firstName)
    }

    @Test
    fun `restricted search cannot find a child without acl access`() {
        db.transaction { createVtjPerson(it) }
        val persons =
            db.read {
                it.searchPeople(
                    AuthenticatedUser.Employee(
                        EmployeeId(UUID.randomUUID()),
                        setOf(UserRole.UNIT_SUPERVISOR)
                    ),
                    "Matti",
                    "last_name",
                    "ASC",
                    true
                )
            }

        assertEquals(0, persons.size)
    }

    @Test
    fun `person with multiple first names can be found`() {
        val created = db.transaction { createVtjPerson(it) }
        val persons =
            db.read { it.searchPeople(adminUser, "Matti Jari-Ville", "last_name", "ASC", false) }

        assertEquals(persons[0].firstName, created.firstName)
    }

    @Test
    fun `person can be found with the full address`() {
        val created = db.transaction { createVtjPerson(it) }
        val persons =
            db.read { it.searchPeople(adminUser, "Jokutie 66", "last_name", "ASC", false) }

        assertEquals(persons[0].streetAddress, created.streetAddress)
    }

    @Test
    fun `person can be found by partial last name`() {
        val created = db.transaction { createVtjPerson(it) }
        val persons = db.read { it.searchPeople(adminUser, "O'Br", "last_name", "ASC", false) }

        assertEquals(persons[0].lastName, created.lastName)
    }

    @Test
    fun `person can be found by partial first name`() {
        val created = db.transaction { createVtjPerson(it) }
        val persons = db.read { it.searchPeople(adminUser, "Matt", "last_name", "ASC", false) }

        assertEquals(persons[0].firstName, created.firstName)
    }

    @Test
    fun `person can be found by partial address`() {
        val created = db.transaction { createVtjPerson(it) }
        val persons = db.read { it.searchPeople(adminUser, "Jokut", "last_name", "ASC", false) }

        assertEquals(persons[0].streetAddress, created.streetAddress)
    }

    @Test
    fun `person can not be found with just a substring match on first name`() {
        db.transaction { createVtjPerson(it) }
        val persons = db.read { it.searchPeople(adminUser, "atti", "last_name", "ASC", false) }

        assertEquals(persons, emptyList())
    }

    @Test
    fun `person can not be found with just a substring match on last name`() {
        db.transaction { createVtjPerson(it) }
        val persons = db.read { it.searchPeople(adminUser, "eikäläine", "last_name", "ASC", false) }

        assertEquals(persons, emptyList())
    }

    @Test
    fun `person can not be found with just a substring match on address`() {
        db.transaction { createVtjPerson(it) }
        val persons = db.read { it.searchPeople(adminUser, "okuti", "last_name", "ASC", false) }

        assertEquals(persons, emptyList())
    }

    @Test
    fun `PostgreSQL text search operator characters are ignored in search terms`() {
        val created = db.transaction { createVtjPerson(it) }
        val persons =
            db.read { it.searchPeople(adminUser, "'&Jokut!|&", "last_name", "ASC", false) }

        assertEquals(1, persons.size)
        assertEquals(
            persons[0].socialSecurityNumber,
            (created.identity as? ExternalIdentifier.SSN)?.ssn
        )
    }

    @Test
    fun `angle brackets are ignored in search terms`() {
        val created = db.transaction { createVtjPerson(it) }
        val persons = db.read { it.searchPeople(adminUser, "<Jokut>", "last_name", "ASC", false) }

        assertEquals(1, persons.size)
        assertEquals(
            persons[0].socialSecurityNumber,
            (created.identity as? ExternalIdentifier.SSN)?.ssn
        )
    }

    @Test
    fun `single quote is ignored at the start of search terms`() {
        val created = db.transaction { createVtjPerson(it) }
        val persons = db.read { it.searchPeople(adminUser, "'O'Brien", "last_name", "ASC", false) }

        assertEquals(1, persons.size)
        assertEquals(
            persons[0].socialSecurityNumber,
            (created.identity as? ExternalIdentifier.SSN)?.ssn
        )
    }

    private fun createVtjPerson(
        tx: Database.Transaction,
        validSSN: String = "010199-8137"
    ): PersonDTO {
        val inputPerson = testPerson(validSSN)
        return tx.createPersonFromVtj(inputPerson)
    }

    private fun testPerson(validSSN: String): PersonDTO =
        PersonDTO(
            id = PersonId(UUID.randomUUID()),
            duplicateOf = null,
            identity = ExternalIdentifier.SSN.getInstance(validSSN),
            ssnAddingDisabled = false,
            dateOfBirth = getDobFromSsn(validSSN),
            firstName = "Matti Yrjö Jari-Ville",
            lastName = "O'Brien",
            preferredName = "Matti",
            email = null,
            phone = "",
            backupPhone = "",
            language = "fi",
            nationalities = listOf("248", "060"),
            streetAddress = "Jokutie 66",
            postalCode = "00000",
            postOffice = "Jokula",
            residenceCode = "",
            restrictedDetailsEnabled = true,
            restrictedDetailsEndDate = LocalDate.now().plusYears(1)
        )
}
