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
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevGuardianBlocklistEntry
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.insertTestGuardianBlocklistEntry
import fi.espoo.evaka.shared.dev.insertTestPerson
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class PersonControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    private val admin =
        AuthenticatedUser.Employee(EmployeeId(UUID.randomUUID()), setOf(UserRole.ADMIN))

    @Autowired lateinit var controller: PersonController

    private val clock = MockEvakaClock(HelsinkiDateTime.of(LocalDateTime.of(2022, 1, 1, 12, 0)))

    @Test
    fun `Search finds person by first and last name`() {
        val user =
            AuthenticatedUser.Employee(
                EmployeeId(UUID.randomUUID()),
                setOf(UserRole.SERVICE_WORKER)
            )
        val person = createPerson()

        val response =
            controller.findBySearchTerms(
                Database(jdbi),
                user,
                clock,
                SearchPersonBody(
                    searchTerm = "${person.firstName} ${person.lastName}",
                    orderBy = "first_name",
                    sortDirection = "DESC"
                )
            )

        assertEquals(person.id, response.first().id)
    }

    @Test
    fun `Search treats tabs as spaces in search terms`() {
        val user =
            AuthenticatedUser.Employee(
                EmployeeId(UUID.randomUUID()),
                setOf(UserRole.SERVICE_WORKER)
            )
        val person = createPerson()

        val response =
            controller.findBySearchTerms(
                Database(jdbi),
                user,
                clock,
                SearchPersonBody(
                    searchTerm = "${person.firstName}\t${person.lastName}",
                    orderBy = "first_name",
                    sortDirection = "DESC"
                )
            )

        assertEquals(person.id, response.first().id)
    }

    @Test
    fun `Search treats non-breaking spaces as spaces in search terms`() {
        val user =
            AuthenticatedUser.Employee(
                EmployeeId(UUID.randomUUID()),
                setOf(UserRole.SERVICE_WORKER)
            )
        val person = createPerson()

        val response =
            controller.findBySearchTerms(
                Database(jdbi),
                user,
                clock,
                SearchPersonBody(
                    searchTerm = "${person.firstName}\u00A0${person.lastName}",
                    orderBy = "first_name",
                    sortDirection = "DESC"
                )
            )

        assertEquals(person.id, response.first().id)
    }

    @Test
    fun `Search treats obscure unicode spaces as spaces in search terms`() {
        val user =
            AuthenticatedUser.Employee(
                EmployeeId(UUID.randomUUID()),
                setOf(UserRole.SERVICE_WORKER)
            )
        val person = createPerson()

        // IDEOGRAPHIC SPACE, not supported by default in regexes
        // unless Java's Pattern.UNICODE_CHARACTER_CLASS-like functionality is enabled.
        val response =
            controller.findBySearchTerms(
                Database(jdbi),
                user,
                clock,
                SearchPersonBody(
                    searchTerm = "${person.firstName}\u3000${person.lastName}",
                    orderBy = "first_name",
                    sortDirection = "DESC"
                )
            )

        assertEquals(person.id, response.first().id)
    }

    @Test
    fun `Guardian blocklist prevents dependants from being added from VTJ data`() {
        val guardianId =
            db.transaction { tx ->
                tx.insertTestPerson(
                    DevPerson(
                        lastName = "Karhula",
                        firstName = "Johannes Olavi Antero Tapio",
                        ssn = "070644-937X"
                    )
                )
            }

        val dependants = controller.getPersonDependants(Database(jdbi), admin, clock, guardianId)
        assertEquals(3, dependants.size)

        val blockedDependant = dependants.find { it.socialSecurityNumber == "070714A9126" }!!
        db.transaction { tx ->
            tx.createUpdate("DELETE FROM guardian WHERE child_id = :id")
                .bind("id", blockedDependant.id)
                .execute()
            tx.insertTestGuardianBlocklistEntry(
                DevGuardianBlocklistEntry(guardianId, blockedDependant.id)
            )
            tx.execute(
                "UPDATE person SET vtj_guardians_queried = NULL, vtj_dependants_queried = NULL"
            )
        }

        assertEquals(
            2,
            controller.getPersonDependants(Database(jdbi), admin, clock, guardianId).size
        )
    }

    @Test
    fun `Guardian blocklist prevents guardians from being added from VTJ data`() {
        val childId =
            db.transaction { tx ->
                tx.insertTestPerson(
                    DevPerson(
                        lastName = "Karhula",
                        firstName = "Jari-Petteri Mukkelis-Makkelis Vetelä-Viljami Eelis-Juhani",
                        ssn = "070714A9126"
                    )
                )
            }

        val guardians = controller.getPersonGuardians(Database(jdbi), admin, clock, childId)
        assertEquals(2, guardians.size)

        val blockedGuardian = guardians.find { it.socialSecurityNumber == "070644-937X" }!!
        db.transaction { tx ->
            tx.createUpdate("DELETE FROM guardian WHERE guardian_id = :id")
                .bind("id", blockedGuardian.id)
                .execute()
            tx.insertTestGuardianBlocklistEntry(
                DevGuardianBlocklistEntry(blockedGuardian.id, childId)
            )
            tx.execute(
                "UPDATE person SET vtj_guardians_queried = NULL, vtj_dependants_queried = NULL"
            )
        }

        assertEquals(1, controller.getPersonGuardians(Database(jdbi), admin, clock, childId).size)
    }

    private fun createPerson(): PersonDTO {
        val ssn = "140881-172X"
        return db.transaction { tx ->
            tx.insertTestPerson(
                    DevPerson(
                        ssn = ssn,
                        dateOfBirth = getDobFromSsn(ssn),
                        firstName = "Matti",
                        lastName = "Meikäläinen",
                        email = "",
                        language = "fi"
                    )
                )
                .let { tx.getPersonById(it)!! }
        }
    }
}
