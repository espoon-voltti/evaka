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
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.vtjclient.service.persondetails.MockPersonDetailsService
import fi.espoo.evaka.vtjclient.service.persondetails.legacyMockVtjDataset
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class PersonControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    private val admin =
        AuthenticatedUser.Employee(EmployeeId(UUID.randomUUID()), setOf(UserRole.ADMIN))
    private val devEmployee = DevEmployee(id = admin.id)

    @Autowired lateinit var controller: PersonController
    @Autowired lateinit var childController: ChildController

    private val clock = MockEvakaClock(HelsinkiDateTime.of(LocalDateTime.of(2022, 1, 1, 12, 0)))

    @BeforeEach
    fun beforeEach() {
        MockPersonDetailsService.add(legacyMockVtjDataset())
    }

    @Test
    fun `Search finds person by first and last name`() {
        val user =
            AuthenticatedUser.Employee(
                EmployeeId(UUID.randomUUID()),
                setOf(UserRole.SERVICE_WORKER),
            )
        val person = createPerson()

        val response =
            controller.searchPerson(
                dbInstance(),
                user,
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
        val user =
            AuthenticatedUser.Employee(
                EmployeeId(UUID.randomUUID()),
                setOf(UserRole.SERVICE_WORKER),
            )
        val person = createPerson()

        val response =
            controller.searchPerson(
                dbInstance(),
                user,
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
        val user =
            AuthenticatedUser.Employee(
                EmployeeId(UUID.randomUUID()),
                setOf(UserRole.SERVICE_WORKER),
            )
        val person = createPerson()

        val response =
            controller.searchPerson(
                dbInstance(),
                user,
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
        val user =
            AuthenticatedUser.Employee(
                EmployeeId(UUID.randomUUID()),
                setOf(UserRole.SERVICE_WORKER),
            )
        val person = createPerson()

        // IDEOGRAPHIC SPACE, not supported by default in regexes
        // unless Java's Pattern.UNICODE_CHARACTER_CLASS-like functionality is enabled.
        val response =
            controller.searchPerson(
                dbInstance(),
                user,
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
        val guardianId =
            db.transaction { tx ->
                tx.insert(
                    DevPerson(
                        lastName = "Karhula",
                        firstName = "Johannes Olavi Antero Tapio",
                        ssn = "070644-937X",
                    ),
                    DevPersonType.RAW_ROW,
                )
            }

        val dependants = controller.getPersonDependants(dbInstance(), admin, clock, guardianId)
        assertEquals(3, dependants.size)

        val blockedDependant = dependants.find { it.socialSecurityNumber == "070714A9126" }!!
        db.transaction { tx ->
            tx.blockGuardian(childId = blockedDependant.id, guardianId = guardianId)
            tx.execute {
                sql("UPDATE person SET vtj_guardians_queried = NULL, vtj_dependants_queried = NULL")
            }
        }

        assertEquals(2, controller.getPersonDependants(dbInstance(), admin, clock, guardianId).size)
    }

    @Test
    fun `Guardian blocklist prevents guardians from being added from VTJ data`() {
        val childId =
            db.transaction { tx ->
                tx.insert(
                    DevPerson(
                        lastName = "Karhula",
                        firstName = "Jari-Petteri Mukkelis-Makkelis Vetelä-Viljami Eelis-Juhani",
                        ssn = "070714A9126",
                    ),
                    DevPersonType.RAW_ROW,
                )
            }

        controller.getPersonGuardians(dbInstance(), admin, clock, childId).let { response ->
            assertEquals(2, response.guardians.size)
            assertEquals(0, response.blockedGuardians?.size)

            val blockedGuardian =
                response.guardians.find { it.socialSecurityNumber == "070644-937X" }!!
            db.transaction { tx ->
                tx.blockGuardian(childId = childId, guardianId = blockedGuardian.id)
                tx.execute {
                    sql(
                        "UPDATE person SET vtj_guardians_queried = NULL, vtj_dependants_queried = NULL"
                    )
                }
            }
        }

        controller.getPersonGuardians(dbInstance(), admin, clock, childId).let { response ->
            assertEquals(1, response.guardians.size)
            assertEquals(1, response.blockedGuardians?.size)
        }
    }

    @Test
    fun `Update person rejects invalid email`() {
        val person = createPerson(testPerson.copy(id = PersonId(UUID.randomUUID())))

        val personPatch = PersonPatch(email = "test@example.com ")

        assertThrows<BadRequest> {
            controller.updatePersonDetails(dbInstance(), admin, clock, person.id, personPatch)
        }
    }

    private fun createPerson(person: DevPerson = testPerson): PersonDTO {
        return db.transaction { tx ->
            tx.insert(person, DevPersonType.RAW_ROW).let { tx.getPersonById(it)!! }
        }
    }

    private val testPerson =
        DevPerson(
            ssn = "140881-172X",
            dateOfBirth = getDobFromSsn("140881-172X"),
            firstName = "Matti",
            lastName = "Meikäläinen",
            email = "",
            language = "fi",
        )
}
