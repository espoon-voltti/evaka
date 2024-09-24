// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class DuplicatePeopleReportTest : FullApplicationTest(resetDbBeforeEach = true) {
    private val adminUser =
        AuthenticatedUser.Employee(
            id = EmployeeId(UUID.randomUUID()),
            roles = setOf(UserRole.ADMIN),
        )

    @Test
    fun `two people with identical names and dates of birth are matched`() {
        val firstName = "Dup Licate"
        val lastName = "Person"
        val dateOfBirth = LocalDate.of(1970, 1, 1)
        val ssn = "010170-1113"
        val personWithSsn =
            DevPerson(
                firstName = firstName,
                lastName = lastName,
                dateOfBirth = dateOfBirth,
                ssn = ssn,
            )
        val personWithoutSsn = personWithSsn.copy(id = PersonId(UUID.randomUUID()), ssn = null)
        db.transaction {
            it.insert(personWithSsn, DevPersonType.RAW_ROW)
            it.insert(personWithoutSsn, DevPersonType.RAW_ROW)
        }

        val (_, _, result) =
            http
                .get("/employee/reports/duplicate-people")
                .asUser(adminUser)
                .responseObject<List<DuplicatePeopleReportRow>>(jsonMapper)

        assertEquals(2, result.get().size)
        assertTrue(result.get().all { it.id == personWithSsn.id || it.id == personWithoutSsn.id })
    }

    @Test
    fun `two people with partially matching first names and white spaces in names are matched`() {
        val firstName = "Dup Licate"
        val lastName = "Person"
        val dateOfBirth = LocalDate.of(1970, 1, 1)
        val ssn = "010170-1113"
        val personWithSsn =
            DevPerson(
                firstName = firstName,
                lastName = lastName,
                dateOfBirth = dateOfBirth,
                ssn = ssn,
            )
        val personWithoutSsn =
            personWithSsn.copy(
                id = PersonId(UUID.randomUUID()),
                firstName = firstName.split(" ")[0] + " ",
                ssn = null,
                lastName = " " + lastName,
            )
        db.transaction {
            it.insert(personWithSsn, DevPersonType.RAW_ROW)
            it.insert(personWithoutSsn, DevPersonType.RAW_ROW)
        }

        val (_, _, result) =
            http
                .get("/employee/reports/duplicate-people")
                .asUser(adminUser)
                .responseObject<List<DuplicatePeopleReportRow>>(jsonMapper)

        assertEquals(2, result.get().size)
        assertTrue(result.get().all { it.id == personWithSsn.id || it.id == personWithoutSsn.id })
    }

    @Test
    fun `two people with identical names but different dates of birth are not matched`() {
        val firstName = "Dup Licate"
        val lastName = "Person"
        val dateOfBirth = LocalDate.of(1970, 1, 1)
        val ssn = "010170-1113"
        val personWithSsn =
            DevPerson(
                firstName = firstName,
                lastName = lastName,
                dateOfBirth = dateOfBirth,
                ssn = ssn,
            )
        val personWithoutSsn =
            personWithSsn.copy(
                id = PersonId(UUID.randomUUID()),
                ssn = null,
                dateOfBirth = dateOfBirth.plusDays(1),
            )
        db.transaction {
            it.insert(personWithSsn, DevPersonType.RAW_ROW)
            it.insert(personWithoutSsn, DevPersonType.RAW_ROW)
        }

        val (_, _, result) =
            http
                .get("/employee/reports/duplicate-people")
                .asUser(adminUser)
                .responseObject<List<DuplicatePeopleReportRow>>(jsonMapper)

        assertEquals(0, result.get().size)
    }

    @Test
    fun `two people with identical names and dates of birth but different ssns are not matched`() {
        val firstName = "Dup Licate"
        val lastName = "Person"
        val dateOfBirth = LocalDate.of(1970, 1, 1)
        val ssn1 = "010170-1113"
        val personWithSsn1 =
            DevPerson(
                firstName = firstName,
                lastName = lastName,
                dateOfBirth = dateOfBirth,
                ssn = ssn1,
            )
        val ssn2 = "010170-1124"
        val personWithSsn2 = personWithSsn1.copy(id = PersonId(UUID.randomUUID()), ssn = ssn2)
        db.transaction {
            it.insert(personWithSsn1, DevPersonType.RAW_ROW)
            it.insert(personWithSsn2, DevPersonType.RAW_ROW)
        }

        val (_, _, result) =
            http
                .get("/employee/reports/duplicate-people")
                .asUser(adminUser)
                .responseObject<List<DuplicatePeopleReportRow>>(jsonMapper)

        assertEquals(0, result.get().size)
    }
}
