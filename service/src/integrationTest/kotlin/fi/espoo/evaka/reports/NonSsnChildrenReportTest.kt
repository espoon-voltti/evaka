// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.FullApplicationTest
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
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.varda.new.Henkilo
import fi.espoo.evaka.varda.new.VardaUpdater
import fi.espoo.evaka.varda.new.addNewChildrenForVardaUpdate
import fi.espoo.evaka.varda.new.setVardaUpdateSuccess
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class NonSsnChildrenReportTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var nonSsnChildrenReportController: NonSsnChildrenReportController
    private val adminUser =
        AuthenticatedUser.Employee(
            id = EmployeeId(UUID.randomUUID()),
            roles = setOf(UserRole.ADMIN),
        )

    private final val today: LocalDate = LocalDate.of(2021, 5, 5)
    private val now = HelsinkiDateTime.of(today, LocalTime.of(12, 0))
    private val clock = MockEvakaClock(now)

    private val jimmyNoSsn =
        DevPerson(
            id = PersonId(UUID.randomUUID()),
            firstName = "Jimmy",
            lastName = "No SSN",
            dateOfBirth = today.minusYears(3),
            ssn = null,
            ophPersonOid = null,
        )

    private val jackieNoSsn =
        DevPerson(
            id = PersonId(UUID.randomUUID()),
            firstName = "Jackie",
            lastName = "No SSN",
            dateOfBirth = today.minusYears(4),
            ssn = null,
            ophPersonOid = "MockOID",
        )

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(testArea)
            tx.insert(testDaycare)
            tx.insert(testDaycare2)
            tx.insert(jimmyNoSsn, DevPersonType.CHILD)
            tx.insert(jackieNoSsn, DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    childId = jimmyNoSsn.id,
                    unitId = testDaycare.id,
                    startDate = today.minusDays(7),
                    endDate = today.plusYears(1),
                )
            )
            tx.insert(
                DevPlacement(
                    childId = jackieNoSsn.id,
                    unitId = testDaycare2.id,
                    startDate = today.minusDays(7),
                    endDate = today.plusYears(1),
                )
            )
            tx.addNewChildrenForVardaUpdate()

            val mockState =
                VardaUpdater.EvakaHenkiloNode(
                    henkilo =
                        Henkilo(
                            etunimet = jackieNoSsn.firstName,
                            sukunimi = jackieNoSsn.lastName,
                            henkilotunnus = jackieNoSsn.ssn,
                            henkilo_oid = jackieNoSsn.ophPersonOid,
                        ),
                    lapset = emptyList(),
                )
            tx.setVardaUpdateSuccess(jackieNoSsn.id, now, mockState)
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
                    ophPersonOid = jackieNoSsn.ophPersonOid,
                    lastSentToVarda = now,
                ),
                NonSsnChildrenReportRow(
                    childId = jimmyNoSsn.id,
                    firstName = jimmyNoSsn.firstName,
                    lastName = jimmyNoSsn.lastName,
                    dateOfBirth = jimmyNoSsn.dateOfBirth,
                    ophPersonOid = null,
                    lastSentToVarda = null,
                ),
            )
        val result =
            nonSsnChildrenReportController.getNonSsnChildrenReportRows(
                dbInstance(),
                adminUser,
                clock,
            )

        assertEquals(expectation.sortedBy { it.childId }, result.sortedBy { it.childId })
    }
}
