// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testArea2
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertContains
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class NonSsnChildrenReportTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var nonSsnChildrenReportController: NonSsnChildrenReportController
    private val adminUser =
        AuthenticatedUser.Employee(
            id = EmployeeId(UUID.randomUUID()),
            roles = setOf(UserRole.ADMIN)
        )

    private final val testDay: LocalDate = LocalDate.of(2021, 5, 5)
    private val testClock = MockEvakaClock(HelsinkiDateTime.of(testDay, LocalTime.of(12, 0)))
    private val jimmyNoSsn =
        DevPerson(
            id = PersonId(UUID.randomUUID()),
            firstName = "Jimmy",
            lastName = "No SSN",
            dateOfBirth = testDay.minusYears(3),
            ssn = null,
            ophPersonOid = null
        )

    private val jackieNoSsn =
        DevPerson(
            id = PersonId(UUID.randomUUID()),
            firstName = "Jackie",
            lastName = "No SSN",
            dateOfBirth = testDay.minusYears(4),
            ssn = null,
            ophPersonOid = "MockOID"
        )

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
            tx.insert(testArea)
            tx.insert(testDaycare)
            tx.insert(testArea2)
            tx.insert(testDaycare2)
            tx.insert(jimmyNoSsn, DevPersonType.CHILD)
            tx.insert(jackieNoSsn, DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    childId = jimmyNoSsn.id,
                    unitId = testDaycare.id,
                    startDate = testDay.minusDays(7),
                    endDate = testDay.plusYears(1)
                )
            )
            tx.insert(
                DevPlacement(
                    childId = jackieNoSsn.id,
                    unitId = testDaycare2.id,
                    startDate = testDay.minusDays(7),
                    endDate = testDay.plusYears(1)
                )
            )
        }
    }

    @Test
    fun `child data can be fetched`() {
        val expectation =
            listOf(
                NonSsnChildrenReportRow(
                    childId = jackieNoSsn.id,
                    firstName = jackieNoSsn.firstName,
                    lastName = jackieNoSsn.lastName,
                    dateOfBirth = jackieNoSsn.dateOfBirth,
                    existingPersonOid = jackieNoSsn.ophPersonOid,
                    vardaOid = null
                ),
                NonSsnChildrenReportRow(
                    childId = jimmyNoSsn.id,
                    firstName = jimmyNoSsn.firstName,
                    lastName = jimmyNoSsn.lastName,
                    dateOfBirth = jimmyNoSsn.dateOfBirth,
                    existingPersonOid = null,
                    vardaOid = null
                )
            )
        val result =
            nonSsnChildrenReportController.getNonSsnChildrenReportRows(
                dbInstance(),
                adminUser,
                testClock
            )

        assertEquals(expectation.size, result.size)
        result.forEach { assertContains(expectation, it) }
    }
}
