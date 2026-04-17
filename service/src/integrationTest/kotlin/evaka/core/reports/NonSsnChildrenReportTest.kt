// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.reports

import evaka.core.FullApplicationTest
import evaka.core.shared.EmployeeId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.auth.UserRole
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.varda.Henkilo
import evaka.core.varda.VardaUpdater
import evaka.core.varda.addNewChildrenForVardaUpdate
import evaka.core.varda.setVardaUpdateSuccess
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

    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val daycare2 = DevDaycare(areaId = area.id)

    private val jimmyNoSsn =
        DevPerson(
            firstName = "Jimmy",
            lastName = "No SSN",
            dateOfBirth = today.minusYears(3),
            ssn = null,
            ophPersonOid = null,
        )

    private val jackieNoSsn =
        DevPerson(
            firstName = "Jackie",
            lastName = "No SSN",
            dateOfBirth = today.minusYears(4),
            ssn = null,
            ophPersonOid = "MockOID",
        )

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(daycare2)
            tx.insert(jimmyNoSsn, DevPersonType.CHILD)
            tx.insert(jackieNoSsn, DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    childId = jimmyNoSsn.id,
                    unitId = daycare.id,
                    startDate = today.minusDays(7),
                    endDate = today.plusYears(1),
                )
            )
            tx.insert(
                DevPlacement(
                    childId = jackieNoSsn.id,
                    unitId = daycare2.id,
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
                    lastSentToKoski = null,
                ),
                NonSsnChildrenReportRow(
                    childId = jimmyNoSsn.id,
                    firstName = jimmyNoSsn.firstName,
                    lastName = jimmyNoSsn.lastName,
                    dateOfBirth = jimmyNoSsn.dateOfBirth,
                    ophPersonOid = null,
                    lastSentToVarda = null,
                    lastSentToKoski = null,
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
