// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.insertTestPerson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class DuplicatePeopleReportTest : FullApplicationTest() {
    @BeforeEach
    fun beforeEach() {
        db.transaction { it.resetDatabase() }
    }

    private val adminUser = AuthenticatedUser.Employee(id = UUID.randomUUID(), roles = setOf(UserRole.ADMIN))

    @Test
    fun `two people with identical names and dates of birth are matched`() {
        val firstName = "Dup Licate"
        val lastName = "Person"
        val dateOfBirth = LocalDate.of(1970, 1, 1)
        val ssn = "010170-1113"
        val personWithSsn = DevPerson(
            firstName = firstName,
            lastName = lastName,
            dateOfBirth = dateOfBirth,
            ssn = ssn
        )
        val personWithoutSsn = personWithSsn.copy(id = UUID.randomUUID(), ssn = null)
        db.transaction {
            it.handle.insertTestPerson(personWithSsn)
            it.handle.insertTestPerson(personWithoutSsn)
        }

        val (_, _, result) = http.get("/reports/duplicate-people")
            .asUser(adminUser)
            .responseObject<List<DuplicatePeopleReportRow>>(objectMapper)

        assertEquals(2, result.get().size)
        assertTrue(result.get().all { it.id == personWithSsn.id || it.id == personWithoutSsn.id })
    }

    @Test
    fun `two people with partially matching first names are matched`() {
        val firstName = "Dup Licate"
        val lastName = "Person"
        val dateOfBirth = LocalDate.of(1970, 1, 1)
        val ssn = "010170-1113"
        val personWithSsn = DevPerson(
            firstName = firstName,
            lastName = lastName,
            dateOfBirth = dateOfBirth,
            ssn = ssn
        )
        val personWithoutSsn =
            personWithSsn.copy(id = UUID.randomUUID(), firstName = firstName.split(" ")[0], ssn = null)
        db.transaction {
            it.handle.insertTestPerson(personWithSsn)
            it.handle.insertTestPerson(personWithoutSsn)
        }

        val (_, _, result) = http.get("/reports/duplicate-people")
            .asUser(adminUser)
            .responseObject<List<DuplicatePeopleReportRow>>(objectMapper)

        assertEquals(2, result.get().size)
        assertTrue(result.get().all { it.id == personWithSsn.id || it.id == personWithoutSsn.id })
    }

    @Test
    fun `two people with identical names but different dates of birth are not matched`() {
        val firstName = "Dup Licate"
        val lastName = "Person"
        val dateOfBirth = LocalDate.of(1970, 1, 1)
        val ssn = "010170-1113"
        val personWithSsn = DevPerson(
            firstName = firstName,
            lastName = lastName,
            dateOfBirth = dateOfBirth,
            ssn = ssn
        )
        val personWithoutSsn =
            personWithSsn.copy(id = UUID.randomUUID(), ssn = null, dateOfBirth = dateOfBirth.plusDays(1))
        db.transaction {
            it.handle.insertTestPerson(personWithSsn)
            it.handle.insertTestPerson(personWithoutSsn)
        }

        val (_, _, result) = http.get("/reports/duplicate-people")
            .asUser(adminUser)
            .responseObject<List<DuplicatePeopleReportRow>>(objectMapper)

        assertEquals(0, result.get().size)
    }

    @Test
    fun `two people with identical names and dates of birth but different ssns are not matched`() {
        val firstName = "Dup Licate"
        val lastName = "Person"
        val dateOfBirth = LocalDate.of(1970, 1, 1)
        val ssn1 = "010170-1113"
        val personWithSsn1 = DevPerson(
            firstName = firstName,
            lastName = lastName,
            dateOfBirth = dateOfBirth,
            ssn = ssn1
        )
        val ssn2 = "010170-1124"
        val personWithSsn2 = personWithSsn1.copy(id = UUID.randomUUID(), ssn = ssn2)
        db.transaction {
            it.handle.insertTestPerson(personWithSsn1)
            it.handle.insertTestPerson(personWithSsn2)
        }

        val (_, _, result) = http.get("/reports/duplicate-people")
            .asUser(adminUser)
            .responseObject<List<DuplicatePeopleReportRow>>(objectMapper)

        assertEquals(0, result.get().size)
    }
}
