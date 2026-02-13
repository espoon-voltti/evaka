// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controller

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.identity.getDobFromSsn
import fi.espoo.evaka.pis.controllers.PersonController
import fi.espoo.evaka.pis.controllers.SearchPersonBody
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.pis.service.PersonPatch
import fi.espoo.evaka.pis.service.blockGuardian
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.BadRequest
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
