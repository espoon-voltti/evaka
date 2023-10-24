// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controller

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.controllers.ChildController
import fi.espoo.evaka.identity.getDobFromSsn
import fi.espoo.evaka.pis.controllers.PersonController
import fi.espoo.evaka.pis.controllers.SearchPersonBody
import fi.espoo.evaka.pis.getFosterChildren
import fi.espoo.evaka.pis.getFosterParents
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevChild
import fi.espoo.evaka.shared.dev.DevFosterParent
import fi.espoo.evaka.shared.dev.DevGuardian
import fi.espoo.evaka.shared.dev.DevGuardianBlocklistEntry
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestGuardian
import fi.espoo.evaka.shared.dev.insertTestGuardianBlocklistEntry
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class PersonControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    private val admin =
        AuthenticatedUser.Employee(EmployeeId(UUID.randomUUID()), setOf(UserRole.ADMIN))

    @Autowired lateinit var controller: PersonController
    @Autowired lateinit var childController: ChildController

    private val clock = MockEvakaClock(HelsinkiDateTime.of(LocalDateTime.of(2022, 1, 1, 12, 0)))

    @Test
    fun `duplicate throws forbidden when user is staff`() {
        val user = AuthenticatedUser.Employee(EmployeeId(UUID.randomUUID()), setOf(UserRole.STAFF))

        assertThrows<Forbidden> {
            controller.duplicate(dbInstance(), user, clock, PersonId(UUID.randomUUID()))
        }
    }

    @Test
    fun `duplicate throws not found when person doesn't exist`() {
        val user = AuthenticatedUser.Employee(EmployeeId(UUID.randomUUID()), setOf(UserRole.ADMIN))

        assertThrows<NotFound> {
            controller.duplicate(dbInstance(), user, clock, PersonId(UUID.randomUUID()))
        }
    }

    @Test
    fun `duplicate person data`() {
        val user = AuthenticatedUser.Employee(EmployeeId(UUID.randomUUID()), setOf(UserRole.ADMIN))
        val person = createPerson()
        assertThat(person).extracting { it.identity }.isNotNull

        val duplicateId = controller.duplicate(dbInstance(), user, clock, person.id)

        val original = controller.getPerson(dbInstance(), user, clock, person.id)
        val duplicate = controller.getPerson(dbInstance(), user, clock, duplicateId)
        assertThat(duplicate)
            .returns(null) { it.person.socialSecurityNumber }
            .returns(null) { it.person.updatedFromVtj }
            .returns(true) { it.person.ssnAddingDisabled }
            .usingRecursiveComparison()
            .ignoringFields(
                "person.id",
                "person.socialSecurityNumber",
                "person.updatedFromVtj",
                "person.ssnAddingDisabled"
            )
            .isEqualTo(original)
        val duplicateOf =
            db.transaction { tx ->
                tx.createQuery("SELECT duplicate_of FROM person WHERE id = :id")
                    .bind("id", duplicateId)
                    .exactlyOne<PersonId>()
            }
        assertThat(duplicateOf).isEqualTo(person.id)
    }

    @Test
    fun `duplicate child data`() {
        val user = AuthenticatedUser.Employee(EmployeeId(UUID.randomUUID()), setOf(UserRole.ADMIN))
        val person = createPerson()
        db.transaction { tx ->
            tx.insert(
                DevChild(
                    id = person.id,
                    allergies = "Heinänuha",
                    diet = "Gluteeniton",
                    medication = "Astma",
                    additionalInfo = "Lisätiedot"
                )
            )
        }

        val duplicateId = controller.duplicate(dbInstance(), user, clock, person.id)

        val original = childController.getAdditionalInfo(dbInstance(), user, clock, person.id)
        val duplicate = childController.getAdditionalInfo(dbInstance(), user, clock, duplicateId)
        assertThat(duplicate).isEqualTo(original)
    }

    @Test
    fun `duplicate guardians as foster parents`() {
        val user = AuthenticatedUser.Employee(EmployeeId(UUID.randomUUID()), setOf(UserRole.ADMIN))
        val person = createPerson()
        db.transaction { tx ->
            val guardianId = tx.insert(DevPerson())
            tx.insertTestGuardian(DevGuardian(guardianId = guardianId, childId = person.id))
        }

        val duplicateId = controller.duplicate(dbInstance(), user, clock, person.id)

        val fosterParents = db.transaction { tx -> tx.getFosterParents(duplicateId) }
        assertThat(fosterParents)
            .extracting({ it.child.id }, { it.parent.firstName }, { it.validDuring })
            .containsExactly(Tuple(duplicateId, "Test", DateRange(clock.today(), null)))
    }

    @Test
    fun `do not duplicate children as foster children`() {
        val user = AuthenticatedUser.Employee(EmployeeId(UUID.randomUUID()), setOf(UserRole.ADMIN))
        val person = createPerson()
        db.transaction { tx ->
            val childId = tx.insert(DevPerson())
            tx.insertTestGuardian(DevGuardian(guardianId = person.id, childId = childId))
        }

        val duplicateId = controller.duplicate(dbInstance(), user, clock, person.id)

        val fosterChildren = db.transaction { tx -> tx.getFosterChildren(duplicateId) }
        assertThat(fosterChildren).isEmpty()
    }

    @Test
    fun `duplicate foster parents as foster parents`() {
        val user = AuthenticatedUser.Employee(EmployeeId(UUID.randomUUID()), setOf(UserRole.ADMIN))
        val person = createPerson()
        val fosterValidDuring = DateRange(LocalDate.of(2023, 1, 27), LocalDate.of(2023, 12, 24))
        db.transaction { tx ->
            val fosterParentId = tx.insert(DevPerson())
            tx.insert(
                DevFosterParent(
                    parentId = fosterParentId,
                    childId = person.id,
                    validDuring = fosterValidDuring
                )
            )
        }

        val duplicateId = controller.duplicate(dbInstance(), user, clock, person.id)

        val fosterParents = db.transaction { tx -> tx.getFosterParents(duplicateId) }
        assertThat(fosterParents)
            .extracting({ it.child.id }, { it.parent.firstName }, { it.validDuring })
            .containsExactly(Tuple(duplicateId, "Test", fosterValidDuring))
    }

    @Test
    fun `do not duplicate foster children as foster children`() {
        val user = AuthenticatedUser.Employee(EmployeeId(UUID.randomUUID()), setOf(UserRole.ADMIN))
        val person = createPerson()
        val fosterValidDuring = DateRange(LocalDate.of(2023, 1, 27), LocalDate.of(2023, 12, 24))
        db.transaction { tx ->
            val childId = tx.insert(DevPerson())
            tx.insert(
                DevFosterParent(
                    parentId = person.id,
                    childId = childId,
                    validDuring = fosterValidDuring
                )
            )
        }

        val duplicateId = controller.duplicate(dbInstance(), user, clock, person.id)

        val fosterChildren = db.transaction { tx -> tx.getFosterChildren(duplicateId) }
        assertThat(fosterChildren).isEmpty()
    }

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
                dbInstance(),
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
                dbInstance(),
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
                dbInstance(),
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
                dbInstance(),
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
                tx.insert(
                    DevPerson(
                        lastName = "Karhula",
                        firstName = "Johannes Olavi Antero Tapio",
                        ssn = "070644-937X"
                    )
                )
            }

        val dependants = controller.getPersonDependants(dbInstance(), admin, clock, guardianId)
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
                        ssn = "070714A9126"
                    )
                )
            }

        val guardians = controller.getPersonGuardians(dbInstance(), admin, clock, childId)
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

        assertEquals(1, controller.getPersonGuardians(dbInstance(), admin, clock, childId).size)
    }

    private fun createPerson(): PersonDTO {
        val ssn = "140881-172X"
        return db.transaction { tx ->
            tx.insert(
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
