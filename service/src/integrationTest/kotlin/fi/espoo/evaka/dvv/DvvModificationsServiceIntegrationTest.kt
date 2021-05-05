// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.dvv

import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.pis.getPersonBySSN
import fi.espoo.evaka.pis.service.getChildGuardians
import fi.espoo.evaka.pis.service.getGuardianChildIds
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.insertTestPerson
import fi.espoo.evaka.vtjclient.dto.NativeLanguage
import fi.espoo.evaka.vtjclient.dto.RestrictedDetails
import fi.espoo.evaka.vtjclient.dto.VtjPerson
import fi.espoo.evaka.vtjclient.service.persondetails.MockPersonDetailsService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class DvvModificationsServiceIntegrationTest : DvvModificationsServiceIntegrationTestBase() {

    @BeforeEach
    private fun beforeEach() {
        db.transaction { tx ->
            tx.resetDatabase()
            tx.storeDvvModificationToken("100", "101", 0, 0)
        }
    }

    @AfterEach
    private fun afterEach() {
        db.transaction { tx ->
            tx.deleteDvvModificationToken("100")
            DvvIntegrationTestPersonService.resetSsnUpdateCounts()
        }
    }

    @Test
    fun `get modification token for today`() = db.transaction { tx ->
        assertEquals("101", tx.getNextDvvModificationToken())
        val response = dvvModificationsService.getDvvModifications(tx, listOf("nimenmuutos"))
        assertEquals(1, response.size)
        assertEquals("102", tx.getNextDvvModificationToken())
        val createdDvvModificationToken = tx.getDvvModificationToken("101")!!
        assertEquals("101", createdDvvModificationToken.token)
        assertEquals("102", createdDvvModificationToken.nextToken)
        assertEquals(1, createdDvvModificationToken.ssnsSent)
        assertEquals(1, createdDvvModificationToken.modificationsReceived)

        tx.deleteDvvModificationToken("101")
    }

    @Test
    fun `person date of death`() {
        createTestPerson(testPerson.copy(ssn = "010180-999A"))
        dvvModificationsService.updatePersonsFromDvv(dbInstance(), listOf("010180-999A"))
        assertEquals(LocalDate.parse("2019-07-30"), db.read { it.getPersonBySSN("010180-999A") }?.dateOfDeath)
        assertEquals(0, DvvIntegrationTestPersonService.getSsnUpdateCount("010180-999A"))
    }

    @Test
    fun `person restricted details started`() {
        createTestPerson(testPerson.copy(ssn = "020180-999Y"))
        dvvModificationsService.updatePersonsFromDvv(dbInstance(), listOf("020180-999Y"))
        val modifiedPerson = db.read { it.getPersonBySSN("020180-999Y") }!!
        assertEquals(true, modifiedPerson.restrictedDetailsEnabled)
        assertEquals("", modifiedPerson.streetAddress)
        assertEquals("", modifiedPerson.postalCode)
        assertEquals("", modifiedPerson.postOffice)
        assertEquals(0, DvvIntegrationTestPersonService.getSsnUpdateCount("020180-999Y"))
    }

    @Test
    fun `person restricted details ended and address is set`() {
        createTestPerson(testPerson.copy(ssn = "030180-999L", restrictedDetailsEnabled = true, streetAddress = "", postalCode = "", postOffice = ""))
        dvvModificationsService.updatePersonsFromDvv(dbInstance(), listOf("030180-999L"))
        val modifiedPerson = db.read { it.getPersonBySSN("030180-999L") }!!
        assertEquals(false, modifiedPerson.restrictedDetailsEnabled)
        assertEquals(LocalDate.parse("2030-01-01"), modifiedPerson.restrictedDetailsEndDate)
        assertEquals("Vanhakatu 10h5 3", modifiedPerson.streetAddress)
        assertEquals("02230", modifiedPerson.postalCode)
        assertEquals("Espoo", modifiedPerson.postOffice)
        assertEquals(0, DvvIntegrationTestPersonService.getSsnUpdateCount("020180-999Y"))
    }

    @Test
    fun `person address change`() {
        createTestPerson(testPerson.copy(ssn = "040180-9998"))
        dvvModificationsService.updatePersonsFromDvv(dbInstance(), listOf("040180-9998"))
        val modifiedPerson = db.read { it.getPersonBySSN("040180-9998") }!!
        assertEquals("Uusitie 17 A 2", modifiedPerson.streetAddress)
        assertEquals("02940", modifiedPerson.postalCode)
        assertEquals("ESPOO", modifiedPerson.postOffice)
        assertEquals("123456789V1B033 ", modifiedPerson.residenceCode)
        assertEquals(0, DvvIntegrationTestPersonService.getSsnUpdateCount("040180-9998"))
    }

    @Test
    fun `person ssn change`() {
        val testId = createTestPerson(testPerson.copy(ssn = "010181-999K"))
        // Should update SSN from 010181-999K to 010181-999C
        dvvModificationsService.updatePersonsFromDvv(dbInstance(), listOf("010181-999K"))
        assertEquals(testId, db.read { it.getPersonBySSN("010281-999C") }?.id)
        assertEquals(null, db.read { it.getPersonBySSN("010281-999K") })
        assertEquals(0, DvvIntegrationTestPersonService.getSsnUpdateCount("010281-999K"))
        assertEquals(0, DvvIntegrationTestPersonService.getSsnUpdateCount("010281-999C"))
    }

    @Test
    fun `new custodian added`() {
        val custodian = testPerson.copy(firstName = "Harri", lastName = "Huollettava", ssn = "050118A999W")
        var caretaker: DevPerson = testPerson.copy(ssn = "050180-999W", dependants = listOf(custodian))

        caretaker = caretaker.copy(dependants = listOf(custodian))

        createTestPerson(custodian)
        createVtjPerson(custodian)
        createVtjPerson(caretaker)

        dvvModificationsService.updatePersonsFromDvv(dbInstance(), listOf("050180-999W"))
        val dvvCustodian = db.read { it.getPersonBySSN("050118A999W") }!!
        assertEquals("Harri", dvvCustodian.firstName)
        assertEquals("Huollettava", dvvCustodian.lastName)
        assertTrue(
            db.read { tx ->
                tx.getChildGuardians(dvvCustodian.id)
                    .map { tx.getPersonById(it) }.filterNotNull()
                    .any() { guardian -> guardian.identity.toString() == caretaker.ssn }
            }
        )
        assertEquals(1, DvvIntegrationTestPersonService.getSsnUpdateCount(caretaker.ssn.toString()))
        assertEquals(1, DvvIntegrationTestPersonService.getSsnCustodianUpdateCount(caretaker.ssn.toString()))
    }

    @Test
    fun `new caretaker added`() {
        var custodian: DevPerson = testPerson.copy(ssn = "060118A999J")
        val caretaker = testPerson.copy(firstName = "Harri", lastName = "Huoltaja", ssn = "060180-999J", dependants = listOf(custodian))
        custodian = custodian.copy(guardians = listOf(caretaker))

        createTestPerson(custodian)
        createVtjPerson(custodian)
        createVtjPerson(caretaker)

        dvvModificationsService.updatePersonsFromDvv(dbInstance(), listOf("060118A999J"))
        val dvvCaretaker = db.read { it.getPersonBySSN("060180-999J") }!!
        assertEquals("Harri", dvvCaretaker.firstName)
        assertEquals("Huoltaja", dvvCaretaker.lastName)

        assertTrue(
            db.read { tx ->
                tx.getGuardianChildIds(dvvCaretaker.id)
                    .map { tx.getPersonById(it) }.filterNotNull()
                    .any() { child -> child.identity.toString() == custodian.ssn }
            }
        )

        assertEquals(1, DvvIntegrationTestPersonService.getSsnUpdateCount(caretaker.ssn.toString()))
        assertEquals(1, DvvIntegrationTestPersonService.getSsnCustodianUpdateCount(caretaker.ssn.toString()))
    }

    @Test
    fun `new single caretaker modification groups cause only one VTJ update`() {
        var custodian: DevPerson = testPerson.copy(ssn = "010118-999A")
        val caretaker = testPerson.copy(firstName = "Harri", lastName = "Huoltaja", ssn = "010579-9999", dependants = listOf(custodian))
        custodian = custodian.copy(guardians = listOf(caretaker))

        createTestPerson(custodian)
        createVtjPerson(custodian)
        createVtjPerson(caretaker)

        dvvModificationsService.updatePersonsFromDvv(dbInstance(), listOf("yksinhuoltaja-muutos"))
        val dvvCaretaker = db.read { it.getPersonBySSN(caretaker.ssn!!) }!!
        assertEquals("Harri", dvvCaretaker.firstName)
        assertEquals("Huoltaja", dvvCaretaker.lastName)

        assertTrue(
            db.read { tx ->
                tx.getGuardianChildIds(dvvCaretaker.id)
                    .map { tx.getPersonById(it) }.filterNotNull()
                    .any() { child -> child.identity.toString() == custodian.ssn }
            }
        )

        assertEquals(1, DvvIntegrationTestPersonService.getSsnUpdateCount(caretaker.ssn.toString()))
        assertEquals(1, DvvIntegrationTestPersonService.getSsnCustodianUpdateCount(caretaker.ssn.toString()))
    }

    @Test
    fun `name changed`() {
        val SSN = "010179-9992"
        val personWithOldName: DevPerson = testPerson.copy(firstName = "Ville", lastName = "Vanhanimi", ssn = SSN)
        val personWithNewName = testPerson.copy(firstName = "Urkki", lastName = "Uusinimi", ssn = SSN)

        createTestPerson(personWithOldName)
        createVtjPerson(personWithNewName)

        dvvModificationsService.updatePersonsFromDvv(dbInstance(), listOf(SSN))
        val updatedPerson = db.read { it.getPersonBySSN(SSN) }!!
        assertEquals("Urkki", updatedPerson.firstName)
        assertEquals("Uusinimi", updatedPerson.lastName)
    }

    @Test
    fun `paging works`() {
        // The mock server has been rigged so that if the token is negative, it will return the requested batch with
        // ajanTasalla=false and next token = token + 1 causing the dvv client to do a request for the subsequent page,
        // until token is 0 and then it will return ajanTasalla=true
        // So if the paging works correctly there should Math.abs(original_token) + 1 identical records
        db.transaction {
            it.resetDatabase()
            it.storeDvvModificationToken("10000", "-2", 0, 0)
        }
        try {
            createTestPerson(testPerson.copy(ssn = "010180-999A"))
            assertEquals(3, dvvModificationsService.updatePersonsFromDvv(dbInstance(), listOf("010180-999A")))
            db.read {
                assertEquals("1", it.getNextDvvModificationToken())
                assertEquals(LocalDate.parse("2019-07-30"), it.getPersonBySSN("010180-999A")?.dateOfDeath)
            }
        } finally {
            db.transaction {
                it.deleteDvvModificationToken("0")
            }
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

    private fun createTestPerson(devPerson: DevPerson): UUID = db.transaction { tx ->
        tx.insertTestPerson(devPerson)
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
}
