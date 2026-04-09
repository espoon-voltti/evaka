// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controller

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.controllers.ChildController
import fi.espoo.evaka.identity.getDobFromSsn
import fi.espoo.evaka.pis.controllers.PersonController
import fi.espoo.evaka.pis.controllers.SearchPersonBody
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.pis.service.PersonPatch
import fi.espoo.evaka.pis.service.blockGuardian
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.vtjclient.service.persondetails.MockPersonDetailsService
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class PersonControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    private val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val serviceWorker = DevEmployee(roles = setOf(UserRole.SERVICE_WORKER))

    @Autowired lateinit var controller: PersonController
    @Autowired lateinit var childController: ChildController

    private val clock = MockEvakaClock(2022, 1, 1, 12, 0)

    // VTJ mock persons for guardian blocklist tests
    private val johannesKarhula =
        DevPerson(
            firstName = "Johannes Olavi Antero Tapio",
            lastName = "Karhula",
            ssn = "070644-937X",
            streetAddress = "Kamreerintie 1",
            postalCode = "00340",
            postOffice = "Espoo",
        )
    private val villeVilkas =
        DevPerson(
            firstName = "Ville",
            lastName = "Vilkas",
            ssn = "311299-999E",
            streetAddress = "Toistie 33",
            postalCode = "02230",
            postOffice = "Espoo",
        )
    private val jariPetteriKarhula =
        DevPerson(
            firstName = "Jari-Petteri Mukkelis-Makkelis Vetelä-Viljami Eelis-Juhani",
            lastName = "Karhula",
            ssn = "070714A9126",
            streetAddress = "Kamreerintie 1",
            postalCode = "00340",
            postOffice = "Espoo",
        )
    private val kaarinaKarhula =
        DevPerson(
            firstName = "Kaarina Veera Nelli",
            lastName = "Karhula",
            ssn = "160616A978U",
            streetAddress = "Kamreerintie 1",
            postalCode = "00340",
            postOffice = "Espoo",
        )
    private val porriKarhula =
        DevPerson(
            firstName = "Porri Hatter",
            lastName = "Karhula",
            ssn = "160620A999J",
            restrictedDetailsEnabled = true,
        )

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(admin)
            tx.insert(serviceWorker)
        }
        MockPersonDetailsService.addPersons(
            johannesKarhula,
            villeVilkas,
            jariPetteriKarhula,
            kaarinaKarhula,
            porriKarhula,
        )
        MockPersonDetailsService.addDependants(
            johannesKarhula,
            jariPetteriKarhula,
            kaarinaKarhula,
            porriKarhula,
        )
        MockPersonDetailsService.addDependants(villeVilkas, jariPetteriKarhula)
    }

    @Test
    fun `Search finds person by first and last name`() {
        val person = createPerson()

        val response =
            controller.searchPerson(
                dbInstance(),
                serviceWorker.user,
                clock,
                SearchPersonBody(
                    searchTerm = "${person.firstName} ${person.lastName}",
                    orderBy = "first_name",
                    sortDirection = "DESC",
                ),
            )

        assertEquals(person.id, response.first().id)
    }

    @Test
    fun `Search treats tabs as spaces in search terms`() {
        val person = createPerson()

        val response =
            controller.searchPerson(
                dbInstance(),
                serviceWorker.user,
                clock,
                SearchPersonBody(
                    searchTerm = "${person.firstName}\t${person.lastName}",
                    orderBy = "first_name",
                    sortDirection = "DESC",
                ),
            )

        assertEquals(person.id, response.first().id)
    }

    @Test
    fun `Search treats non-breaking spaces as spaces in search terms`() {
        val person = createPerson()

        val response =
            controller.searchPerson(
                dbInstance(),
                serviceWorker.user,
                clock,
                SearchPersonBody(
                    searchTerm = "${person.firstName}\u00A0${person.lastName}",
                    orderBy = "first_name",
                    sortDirection = "DESC",
                ),
            )

        assertEquals(person.id, response.first().id)
    }

    @Test
    fun `Search treats obscure unicode spaces as spaces in search terms`() {
        val person = createPerson()

        // IDEOGRAPHIC SPACE, not supported by default in regexes
        // unless Java's Pattern.UNICODE_CHARACTER_CLASS-like functionality is enabled.
        val response =
            controller.searchPerson(
                dbInstance(),
                serviceWorker.user,
                clock,
                SearchPersonBody(
                    searchTerm = "${person.firstName}\u3000${person.lastName}",
                    orderBy = "first_name",
                    sortDirection = "DESC",
                ),
            )

        assertEquals(person.id, response.first().id)
    }

    @Test
    fun `Guardian blocklist prevents dependants from being added from VTJ data`() {
        val guardianId = db.transaction { tx -> tx.insert(johannesKarhula, DevPersonType.RAW_ROW) }

        val dependants = controller.getPersonDependants(dbInstance(), admin.user, clock, guardianId)
        assertEquals(3, dependants.size)

        val blockedDependant =
            dependants.find { it.socialSecurityNumber == jariPetteriKarhula.ssn }!!
        db.transaction { tx ->
            tx.blockGuardian(childId = blockedDependant.id, guardianId = guardianId)
            tx.execute {
                sql("UPDATE person SET vtj_guardians_queried = NULL, vtj_dependants_queried = NULL")
            }
        }

        assertEquals(
            2,
            controller.getPersonDependants(dbInstance(), admin.user, clock, guardianId).size,
        )
    }

    @Test
    fun `Guardian blocklist prevents guardians from being added from VTJ data`() {
        val childId = db.transaction { tx -> tx.insert(jariPetteriKarhula, DevPersonType.RAW_ROW) }

        controller.getPersonGuardians(dbInstance(), admin.user, clock, childId).let { response ->
            assertEquals(2, response.guardians.size)
            assertEquals(0, response.blockedGuardians?.size)

            val blockedGuardian =
                response.guardians.find { it.socialSecurityNumber == johannesKarhula.ssn }!!
            db.transaction { tx ->
                tx.blockGuardian(childId = childId, guardianId = blockedGuardian.id)
                tx.execute {
                    sql(
                        "UPDATE person SET vtj_guardians_queried = NULL, vtj_dependants_queried = NULL"
                    )
                }
            }
        }

        controller.getPersonGuardians(dbInstance(), admin.user, clock, childId).let { response ->
            assertEquals(1, response.guardians.size)
            assertEquals(1, response.blockedGuardians?.size)
        }
    }

    private val sensitiveTestPerson =
        DevPerson(
            ssn = "140881-172X",
            dateOfBirth = getDobFromSsn("140881-172X"),
            firstName = "Matti",
            lastName = "Meikäläinen",
            email = "matti@example.com",
            phone = "0401234567",
            language = "fi",
            streetAddress = "Testikatu 1",
            postalCode = "00100",
            postOffice = "Helsinki",
            residenceCode = "12345",
            municipalityOfResidence = "Helsinki",
            ophPersonOid = "1.2.246.562.24.00000000001",
        )

    private val invoicePatch =
        PersonPatch(
            invoiceRecipientName = "Matti Meikäläinen",
            invoicingStreetAddress = "Laskukatu 2",
            invoicingPostalCode = "00200",
            invoicingPostOffice = "Helsinki",
            forceManualFeeDecisions = true,
        )

    @Test
    fun `getPerson returns basic person info`() {
        val personId =
            db.transaction { tx -> tx.insert(sensitiveTestPerson, DevPersonType.RAW_ROW) }

        val response = controller.getPerson(dbInstance(), admin.user, clock, personId)

        assertEquals(sensitiveTestPerson.firstName, response.person.firstName)
        assertEquals(sensitiveTestPerson.lastName, response.person.lastName)
        assertEquals(sensitiveTestPerson.dateOfBirth, response.person.dateOfBirth)
        assertEquals(
            sensitiveTestPerson.restrictedDetailsEnabled,
            response.person.restrictedDetailsEnabled,
        )
        assertEquals(true, response.person.hasSsn)
        assertEquals(false, response.person.ssnAddingDisabled)

        val noSsnPersonId =
            db.transaction { tx -> tx.insert(DevPerson(ssn = null), DevPersonType.RAW_ROW) }
        val noSsnResponse = controller.getPerson(dbInstance(), admin.user, clock, noSsnPersonId)
        assertEquals(false, noSsnResponse.person.hasSsn)
    }

    @Test
    fun `getChild returns basic person info`() {
        val personId =
            db.transaction { tx -> tx.insert(sensitiveTestPerson, DevPersonType.RAW_ROW) }

        val response =
            childController.getChild(dbInstance(), admin.user, clock, ChildId(personId.raw))

        assertEquals(sensitiveTestPerson.firstName, response.person.firstName)
        assertEquals(sensitiveTestPerson.lastName, response.person.lastName)
        assertEquals(sensitiveTestPerson.dateOfBirth, response.person.dateOfBirth)
        assertEquals(true, response.person.hasSsn)
        assertEquals(false, response.person.ssnAddingDisabled)
        assertEquals(
            sensitiveTestPerson.restrictedDetailsEnabled,
            response.person.restrictedDetailsEnabled,
        )

        val noSsnPersonId =
            db.transaction { tx -> tx.insert(DevPerson(ssn = null), DevPersonType.RAW_ROW) }
        val noSsnResponse =
            childController.getChild(dbInstance(), admin.user, clock, ChildId(noSsnPersonId.raw))
        assertEquals(false, noSsnResponse.person.hasSsn)
    }

    @Test
    fun `getPersonSensitiveDetails returns sensitive data for admin`() {
        val personId =
            db.transaction { tx -> tx.insert(sensitiveTestPerson, DevPersonType.RAW_ROW) }
        controller.updatePersonDetails(dbInstance(), admin.user, clock, personId, invoicePatch)

        val response =
            controller.getPersonSensitiveDetails(dbInstance(), admin.user, clock, personId)

        assertEquals(sensitiveTestPerson.ssn, response.socialSecurityNumber)
        assertEquals(sensitiveTestPerson.language, response.language)
        assertEquals(sensitiveTestPerson.email, response.email)
        assertEquals(sensitiveTestPerson.phone, response.phone)
        assertEquals(sensitiveTestPerson.streetAddress, response.streetAddress)
        assertEquals(sensitiveTestPerson.postalCode, response.postalCode)
        assertEquals(sensitiveTestPerson.postOffice, response.postOffice)
        assertEquals(sensitiveTestPerson.residenceCode, response.residenceCode)
        assertEquals(sensitiveTestPerson.municipalityOfResidence, response.municipalityOfResidence)
        assertEquals(invoicePatch.invoiceRecipientName, response.invoiceRecipientName)
        assertEquals(invoicePatch.invoicingStreetAddress, response.invoicingStreetAddress)
        assertEquals(invoicePatch.invoicingPostalCode, response.invoicingPostalCode)
        assertEquals(invoicePatch.invoicingPostOffice, response.invoicingPostOffice)
        assertEquals(invoicePatch.forceManualFeeDecisions, response.forceManualFeeDecisions)
        assertEquals(sensitiveTestPerson.ophPersonOid, response.ophPersonOid)
    }

    @Test
    fun `getPersonSensitiveDetails redacts invoice and OPH fields for service worker`() {
        val personId =
            db.transaction { tx -> tx.insert(sensitiveTestPerson, DevPersonType.RAW_ROW) }
        controller.updatePersonDetails(dbInstance(), admin.user, clock, personId, invoicePatch)

        val response =
            controller.getPersonSensitiveDetails(dbInstance(), serviceWorker.user, clock, personId)

        assertEquals(sensitiveTestPerson.ssn, response.socialSecurityNumber)
        assertEquals(sensitiveTestPerson.language, response.language)
        assertEquals(sensitiveTestPerson.email, response.email)
        assertEquals(sensitiveTestPerson.phone, response.phone)
        assertEquals(sensitiveTestPerson.streetAddress, response.streetAddress)
        assertEquals("", response.invoiceRecipientName)
        assertEquals("", response.invoicingStreetAddress)
        assertEquals("", response.invoicingPostalCode)
        assertEquals("", response.invoicingPostOffice)
        assertEquals(false, response.forceManualFeeDecisions)
        assertEquals(null, response.ophPersonOid)
    }

    @Test
    fun `getPersonSensitiveDetails returns 403 for unauthorized user`() {
        val unauthorizedEmployee = DevEmployee()
        db.transaction { tx -> tx.insert(unauthorizedEmployee) }
        val personId =
            db.transaction { tx -> tx.insert(sensitiveTestPerson, DevPersonType.RAW_ROW) }

        assertThrows<Forbidden> {
            controller.getPersonSensitiveDetails(
                dbInstance(),
                unauthorizedEmployee.user,
                clock,
                personId,
            )
        }
    }

    @Test
    fun `Update person rejects invalid email`() {
        val person = createPerson()

        val personPatch = PersonPatch(email = "test@example.com ")

        assertThrows<BadRequest> {
            controller.updatePersonDetails(dbInstance(), admin.user, clock, person.id, personPatch)
        }
    }

    private val searchPerson =
        DevPerson(
            ssn = "140881-172X",
            dateOfBirth = getDobFromSsn("140881-172X"),
            firstName = "Matti",
            lastName = "Meikäläinen",
            email = "",
            language = "fi",
        )

    private fun createPerson(): PersonDTO {
        return db.transaction { tx ->
            tx.insert(searchPerson, DevPersonType.RAW_ROW).let { tx.getPersonById(it)!! }
        }
    }
}
