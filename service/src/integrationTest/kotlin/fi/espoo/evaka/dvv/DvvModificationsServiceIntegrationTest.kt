// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.dvv

import fi.espoo.evaka.pis.createParentship
import fi.espoo.evaka.pis.getParentships
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.pis.getPersonBySSN
import fi.espoo.evaka.pis.service.getChildGuardians
import fi.espoo.evaka.pis.service.getGuardianChildIds
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.vtjclient.dto.NativeLanguage
import fi.espoo.evaka.vtjclient.dto.PersonAddress
import fi.espoo.evaka.vtjclient.dto.RestrictedDetails
import fi.espoo.evaka.vtjclient.dto.VtjPerson
import fi.espoo.evaka.vtjclient.service.persondetails.MockPersonDetailsService
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DvvModificationsServiceIntegrationTest :
    DvvModificationsServiceIntegrationTestBase(resetDbBeforeEach = true) {
    @BeforeEach
    fun beforeEach() {
        db.transaction { tx -> tx.storeDvvModificationToken("100", "101", 0, 0) }
    }

    @Test
    fun `get modification token for today`() {
        val caretaker =
            testPerson.copy(
                firstName = "Harri",
                lastName = "Huoltaja",
                ssn = "010579-9999",
                dependants = emptyList()
            )
        createVtjPerson(caretaker)

        db.read { assertEquals("101", it.getNextDvvModificationToken()) }

        val updated = updatePeopleFromDvv(listOf("nimenmuutos"))

        db.read {
            assertEquals(1, updated)
            assertEquals("102", it.getNextDvvModificationToken())
            val createdDvvModificationToken = it.getDvvModificationToken("101")!!
            assertEquals("101", createdDvvModificationToken.token)
            assertEquals("102", createdDvvModificationToken.nextToken)
            assertEquals(1, createdDvvModificationToken.ssnsSent)
            assertEquals(1, createdDvvModificationToken.modificationsReceived)
        }

        db.transaction { tx -> tx.deleteDvvModificationToken("101") }
    }

    @Test
    fun `modification token is not updated if there is an exception during update`() {
        db.read { assertEquals("101", it.getNextDvvModificationToken()) }

        assertThrows<Exception> { updatePeopleFromDvv(listOf("rikkinainen_tietue")) }

        db.read { assertEquals("101", it.getNextDvvModificationToken()) }
    }

    @Test
    fun `person date of death`() {
        createTestPerson(testPerson.copy(ssn = "010180-999A"))
        updatePeopleFromDvv(listOf("010180-999A"))
        assertEquals(
            LocalDate.parse("2019-07-30"),
            db.read { it.getPersonBySSN("010180-999A") }?.dateOfDeath
        )
    }

    @Test
    fun `person restricted details started`() {
        createTestPerson(testPerson.copy(ssn = "020180-999Y"))
        createVtjPerson(
            testPerson.copy(
                ssn = "020180-999Y",
                streetAddress = "",
                postalCode = "",
                postOffice = "",
                restrictedDetailsEnabled = true,
                restrictedDetailsEndDate = LocalDate.of(2030, 1, 1)
            )
        )
        updatePeopleFromDvv(listOf("020180-999Y"))
        val modifiedPerson = db.read { it.getPersonBySSN("020180-999Y") }!!
        assertEquals(true, modifiedPerson.restrictedDetailsEnabled)
        assertEquals("", modifiedPerson.streetAddress)
        assertEquals("", modifiedPerson.postalCode)
        assertEquals("", modifiedPerson.postOffice)
    }

    @Test
    fun `person restricted details ended and address is set`() {
        createTestPerson(
            testPerson.copy(
                ssn = "030180-999L",
                restrictedDetailsEnabled = true,
                streetAddress = "",
                postalCode = "",
                postOffice = ""
            )
        )
        createVtjPerson(
            testPerson.copy(
                ssn = "030180-999L",
                streetAddress = "Uusitie 17 A 2",
                postalCode = "02940",
                postOffice = "ESPOO",
                restrictedDetailsEndDate = LocalDate.of(2030, 1, 1)
            )
        )
        updatePeopleFromDvv(listOf("030180-999L"))
        val modifiedPerson = db.read { it.getPersonBySSN("030180-999L") }!!
        assertEquals(false, modifiedPerson.restrictedDetailsEnabled)
        assertEquals(LocalDate.parse("2030-01-01"), modifiedPerson.restrictedDetailsEndDate)
        assertEquals("Uusitie 17 A 2", modifiedPerson.streetAddress)
        assertEquals("02940", modifiedPerson.postalCode)
        assertEquals("ESPOO", modifiedPerson.postOffice)
    }

    @Test
    fun `person address change`() {
        createTestPerson(testPerson.copy(ssn = "040180-9998"))
        createVtjPerson(
            testPerson.copy(
                ssn = "040180-9998",
                streetAddress = "Uusitie 17 A 2",
                postalCode = "02940",
                postOffice = "ESPOO",
                residenceCode = "abc123"
            )
        )
        updatePeopleFromDvv(listOf("040180-9998"))
        val modifiedPerson = db.read { it.getPersonBySSN("040180-9998") }!!
        assertEquals("Uusitie 17 A 2", modifiedPerson.streetAddress)
        assertEquals("02940", modifiedPerson.postalCode)
        assertEquals("ESPOO", modifiedPerson.postOffice)
        assertEquals("abc123", modifiedPerson.residenceCode)
    }

    @Test
    fun `person ssn change`() {
        val testId = createTestPerson(testPerson.copy(ssn = "010181-999K"))
        // Should update SSN from 010181-999K to 010181-999C
        updatePeopleFromDvv(listOf("010181-999K"))
        assertEquals(testId, db.read { it.getPersonBySSN("010281-999C") }?.id)
        assertEquals(null, db.read { it.getPersonBySSN("010281-999K") })
    }

    @Test
    fun `new custodian added`() {
        val custodian =
            testPerson.copy(firstName = "Harri", lastName = "Huollettava", ssn = "050118A999W")
        var caretaker: DevPerson =
            testPerson.copy(ssn = "050180-999W", dependants = listOf(custodian))

        caretaker = caretaker.copy(dependants = listOf(custodian))

        createTestPerson(custodian)
        createVtjPerson(custodian)
        createVtjPerson(caretaker)

        updatePeopleFromDvv(listOf("050180-999W"))
        val dvvCustodian = db.read { it.getPersonBySSN("050118A999W") }!!
        assertEquals("Harri", dvvCustodian.firstName)
        assertEquals("Huollettava", dvvCustodian.lastName)
        assertTrue(
            db.read { tx ->
                tx.getChildGuardians(dvvCustodian.id)
                    .map { tx.getPersonById(it) }
                    .filterNotNull()
                    .any { guardian -> guardian.identity.toString() == caretaker.ssn }
            }
        )
    }

    @Test
    fun `new caretaker added`() {
        var custodian: DevPerson = testPerson.copy(ssn = "060118A999J")
        val caretaker =
            testPerson.copy(
                firstName = "Harri",
                lastName = "Huoltaja",
                ssn = "060180-999J",
                dependants = listOf(custodian)
            )
        custodian = custodian.copy(guardians = listOf(caretaker))

        createTestPerson(custodian)
        createVtjPerson(custodian)
        createVtjPerson(caretaker)

        updatePeopleFromDvv(listOf("060118A999J"))
        val dvvCaretaker = db.read { it.getPersonBySSN("060180-999J") }!!
        assertEquals("Harri", dvvCaretaker.firstName)
        assertEquals("Huoltaja", dvvCaretaker.lastName)

        assertTrue(
            db.read { tx ->
                tx.getGuardianChildIds(dvvCaretaker.id)
                    .map { tx.getPersonById(it) }
                    .filterNotNull()
                    .any { child -> child.identity.toString() == custodian.ssn }
            }
        )
    }

    @Test
    fun `new single caretaker modification groups cause only one VTJ update`() {
        var custodian: DevPerson = testPerson.copy(ssn = "010118-999A")
        val caretaker =
            testPerson.copy(
                firstName = "Harri",
                lastName = "Huoltaja",
                ssn = "010579-9999",
                dependants = listOf(custodian)
            )
        custodian = custodian.copy(guardians = listOf(caretaker))

        createTestPerson(custodian)
        createVtjPerson(custodian)
        createVtjPerson(caretaker)

        updatePeopleFromDvv(listOf("yksinhuoltaja-muutos"))
        val dvvCaretaker = db.read { it.getPersonBySSN(caretaker.ssn!!) }!!
        assertEquals("Harri", dvvCaretaker.firstName)
        assertEquals("Huoltaja", dvvCaretaker.lastName)

        assertTrue(
            db.read { tx ->
                tx.getGuardianChildIds(dvvCaretaker.id)
                    .map { tx.getPersonById(it) }
                    .filterNotNull()
                    .any { child -> child.identity.toString() == custodian.ssn }
            }
        )
    }

    @Test
    fun `Unknown muutostietue causes a VTJ update`() {
        val targetPerson: DevPerson =
            testPerson.copy(ssn = "140921A999X", streetAddress = "Tuntemattoman katuosoite")
        createTestPerson(targetPerson)
        createVtjPerson(targetPerson)
        updatePeopleFromDvv(listOf("tuntematon_muutos"))
        val updatedPerson = db.read { it.getPersonBySSN(targetPerson.ssn!!) }!!
        assertEquals(targetPerson.streetAddress, updatedPerson.streetAddress)
    }

    @Test
    fun `name changed`() {
        val SSN = "010179-9992"
        val personWithOldName: DevPerson =
            testPerson.copy(firstName = "Ville", lastName = "Vanhanimi", ssn = SSN)
        val personWithNewName =
            testPerson.copy(firstName = "Urkki", lastName = "Uusinimi", ssn = SSN)

        createTestPerson(personWithOldName)
        createVtjPerson(personWithNewName)

        updatePeopleFromDvv(listOf(SSN))
        val updatedPerson = db.read { it.getPersonBySSN(SSN) }!!
        assertEquals("Urkki", updatedPerson.firstName)
        assertEquals("Uusinimi", updatedPerson.lastName)
    }

    @Test
    fun `paging works`() {
        // The mock server has been rigged so that if the token is negative, it will return the
        // requested batch with
        // ajanTasalla=false and next token = token + 1 causing the dvv client to do a request for
        // the subsequent page,
        // until token is 0 and then it will return ajanTasalla=true
        // So if the paging works correctly there should Math.abs(original_token) + 1 identical
        // records
        db.transaction { it.storeDvvModificationToken("10000", "-2", 0, 0) }
        try {
            createTestPerson(testPerson.copy(ssn = "010180-999A"))
            assertEquals(3, updatePeopleFromDvv(listOf("010180-999A")))
            db.read {
                assertEquals("1", it.getNextDvvModificationToken())
                assertEquals(
                    LocalDate.parse("2019-07-30"),
                    it.getPersonBySSN("010180-999A")?.dateOfDeath
                )
            }
        } finally {
            db.transaction { it.deleteDvvModificationToken("0") }
        }
    }

    @Test
    fun `a child born is added as a fridge child starting from the child's date of birth`() {
        val ssn = "010170-123F"
        val personWithoutChildren = testPerson.copy(ssn = ssn)
        val childDateOfBirth = LocalDate.of(2020, 1, 1)
        val child = testPerson.copy(dateOfBirth = childDateOfBirth, ssn = "010120A123K")

        createTestPerson(personWithoutChildren)
        createVtjPerson(personWithoutChildren.copy(dependants = listOf(child)))

        db.read { tx ->
            val person = tx.getPersonBySSN(ssn)
            val children = tx.getParentships(headOfChildId = person!!.id, childId = null)
            assertEquals(listOf(), children)
        }
        updatePeopleFromDvv(listOf(ssn))
        db.read { tx ->
            val person = tx.getPersonBySSN(ssn)
            val children = tx.getParentships(headOfChildId = person!!.id, childId = null)
            assertEquals(
                listOf(child.ssn to childDateOfBirth),
                children.map { it.child.socialSecurityNumber to it.startDate }
            )
        }
    }

    @Test
    fun `a child born three months ago is added as a fridge child starting from the child's date of birth`() {
        val ssn = "010170-123F"
        val personWithoutChildren = testPerson.copy(ssn = ssn)
        val childDateOfBirth = LocalDate.of(2020, 1, 1)
        val child = testPerson.copy(dateOfBirth = childDateOfBirth, ssn = "010120A123K")
        val currentDate = childDateOfBirth.plusMonths(3)

        createTestPerson(personWithoutChildren)
        createVtjPerson(personWithoutChildren.copy(dependants = listOf(child)))

        db.read { tx ->
            val person = tx.getPersonBySSN(ssn)
            val children = tx.getParentships(headOfChildId = person!!.id, childId = null)
            assertEquals(listOf(), children)
        }
        updatePeopleFromDvv(listOf(ssn), currentDate)
        db.read { tx ->
            val person = tx.getPersonBySSN(ssn)
            val children = tx.getParentships(headOfChildId = person!!.id, childId = null)
            assertEquals(
                listOf(child.ssn to childDateOfBirth),
                children.map { it.child.socialSecurityNumber to it.startDate }
            )
        }
    }

    @Test
    fun `a child born four months ago is added as a fridge child starting from the child's date of birth`() {
        val ssn = "010170-123F"
        val personWithoutChildren = testPerson.copy(ssn = ssn)
        val childDateOfBirth = LocalDate.of(2020, 1, 1)
        val child = testPerson.copy(dateOfBirth = childDateOfBirth, ssn = "010120A123K")
        val currentDate = childDateOfBirth.plusMonths(4)

        createTestPerson(personWithoutChildren)
        createVtjPerson(personWithoutChildren.copy(dependants = listOf(child)))

        db.read { tx ->
            val person = tx.getPersonBySSN(ssn)
            val children = tx.getParentships(headOfChildId = person!!.id, childId = null)
            assertEquals(listOf(), children)
        }
        updatePeopleFromDvv(listOf(ssn), currentDate)
        db.read { tx ->
            val person = tx.getPersonBySSN(ssn)
            val children = tx.getParentships(headOfChildId = person!!.id, childId = null)
            assertEquals(
                listOf(child.ssn to currentDate),
                children.map { it.child.socialSecurityNumber to it.startDate }
            )
        }
    }

    @Test
    fun `a child born is ignored if the child is already in a fridge family`() {
        val ssn = "010170-123F"
        val personWithoutChildren = testPerson.copy(ssn = ssn)
        val parent = testPerson.copy(id = PersonId(UUID.randomUUID()))
        val childDateOfBirth = LocalDate.of(2020, 1, 1)
        val child =
            testPerson.copy(
                id = PersonId(UUID.randomUUID()),
                dateOfBirth = childDateOfBirth,
                ssn = "010120A123K"
            )

        createTestPerson(personWithoutChildren)
        createTestPerson(parent)
        createTestPerson(child)
        createVtjPerson(personWithoutChildren.copy(dependants = listOf(child)))
        db.transaction {
            it.createParentship(
                child.id,
                parent.id,
                child.dateOfBirth,
                child.dateOfBirth.plusYears(18).minusDays(1)
            )
        }

        db.read { tx ->
            val person = tx.getPersonBySSN(ssn)
            val children = tx.getParentships(headOfChildId = person!!.id, childId = null)
            assertEquals(listOf(), children)
        }
        updatePeopleFromDvv(listOf(ssn))
        db.read { tx ->
            val person = tx.getPersonBySSN(ssn)
            val children = tx.getParentships(headOfChildId = person!!.id, childId = null)
            assertEquals(listOf(), children)
        }
    }

    private fun updatePeopleFromDvv(
        ssns: List<String>,
        currentDate: LocalDate = LocalDate.of(2020, 1, 1)
    ): Int {
        val clock = MockEvakaClock(HelsinkiDateTime.of(currentDate, LocalTime.of(3, 0)))
        val updatedCount = dvvModificationsService.updatePersonsFromDvv(db, clock, ssns)

        asyncJobRunner.runPendingJobsSync(clock)

        return updatedCount
    }

    val testPerson =
        DevPerson(
            id = PersonId(UUID.randomUUID()),
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

    private fun createTestPerson(devPerson: DevPerson): PersonId =
        db.transaction { tx -> tx.insert(devPerson, DevPersonType.RAW_ROW) }

    private fun createVtjPerson(person: DevPerson) {
        MockPersonDetailsService.addPerson(
            VtjPerson(
                socialSecurityNumber = person.ssn!!,
                firstNames = person.firstName,
                lastName = person.lastName,
                address =
                    if (person.streetAddress.isNullOrBlank()) {
                        null
                    } else {
                        PersonAddress(
                            streetAddress = person.streetAddress,
                            postalCode = person.postalCode,
                            postOffice = person.postOffice,
                            postOfficeSe = person.postOffice,
                            streetAddressSe = person.streetAddress
                        )
                    },
                residenceCode = person.residenceCode,
                nativeLanguage = NativeLanguage(languageName = "FI", code = "fi"),
                restrictedDetails =
                    RestrictedDetails(
                        enabled = person.restrictedDetailsEnabled,
                        endDate = person.restrictedDetailsEndDate
                    ),
                guardians = person.guardians.map(::asVtjPerson),
                dependants = person.dependants.map(::asVtjPerson)
            )
        )
    }

    private fun asVtjPerson(person: DevPerson): VtjPerson =
        VtjPerson(
            socialSecurityNumber = person.ssn!!,
            firstNames = person.firstName,
            lastName = person.lastName,
            nativeLanguage = NativeLanguage(languageName = "FI", code = "fi"),
            restrictedDetails =
                RestrictedDetails(
                    enabled = person.restrictedDetailsEnabled,
                    endDate = person.restrictedDetailsEndDate
                )
        )
}
