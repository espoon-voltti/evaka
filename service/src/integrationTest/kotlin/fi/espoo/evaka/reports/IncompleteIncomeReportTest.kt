// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevGuardian
import fi.espoo.evaka.shared.dev.DevIncome
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.security.PilotFeature
import java.time.LocalDate
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class IncompleteIncomeReportTest : PureJdbiTest(resetDbBeforeEach = true) {

    val testArea = DevCareArea()

    val testDaycare =
        DevDaycare(
            name = "Test Daycare",
            areaId = testArea.id,
            ophOrganizerOid = "1.2.246.562.10.888888888888",
            enabledPilotFeatures =
                setOf(
                    PilotFeature.MESSAGING,
                    PilotFeature.MOBILE,
                    PilotFeature.RESERVATIONS,
                    PilotFeature.PLACEMENT_TERMINATION,
                ),
            openingDate = LocalDate.of(2000, 1, 1),
        )

    val testDecisionMaker_2 = DevEmployee()

    val testAdult_1 =
        DevPerson(
            dateOfBirth = LocalDate.of(1980, 1, 1),
            ssn = "010180-1232",
            firstName = "John",
            lastName = "Doe",
        )

    val testAdult_2 =
        DevPerson(
            dateOfBirth = LocalDate.of(1979, 2, 1),
            ssn = "010279-123L",
            firstName = "Joan",
            lastName = "Doe",
        )

    val testIncome =
        DevIncome(
            personId = testAdult_1.id,
            effect = IncomeEffect.INCOMPLETE,
            validFrom = LocalDate.of(2024, 10, 15),
            validTo = null,
            modifiedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
        )

    val testIncomeUserEdited =
        DevIncome(
            personId = testAdult_1.id,
            effect = IncomeEffect.INCOMPLETE,
            validFrom = LocalDate.of(2024, 10, 15),
            validTo = null,
            modifiedBy = testDecisionMaker_2.evakaUserId,
        )

    val children =
        setOf(
            DevPerson(
                dateOfBirth = LocalDate.of(LocalDate.now().year - 5, 6, 1),
                ssn = "111111-999X",
                restrictedDetailsEnabled = false,
            ),
            DevPerson(
                dateOfBirth = LocalDate.of(LocalDate.now().year - 4, 7, 1),
                ssn = "222222-998Y",
                restrictedDetailsEnabled = false,
            ),
            DevPerson(
                dateOfBirth = LocalDate.of(LocalDate.now().year - 6, 8, 1),
                ssn = "333333-997Z",
                restrictedDetailsEnabled = false,
            ),
        )

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(testArea)
            tx.insert(testDaycare)
            tx.insert(testAdult_1, DevPersonType.RAW_ROW)
            tx.insert(testAdult_2, DevPersonType.RAW_ROW)
            children.forEachIndexed { i, it ->
                tx.insert(it, DevPersonType.CHILD)
                tx.insert(DevGuardian(guardianId = testAdult_1.id, childId = it.id))
                if (i % 2 == 1) tx.insert(DevGuardian(guardianId = testAdult_2.id, childId = it.id))
            }
        }
    }

    @Test
    fun `adults that have expired incomes and children have some placements, are found in report`() {
        db.transaction { tx ->
            tx.insert(testIncome)
            children.forEach { it ->
                tx.insert(
                    DevPlacement(
                        childId = it.id,
                        unitId = testDaycare.id,
                        endDate = LocalDate.of(2024, 12, 31),
                    )
                )
            }
        }
        val report = getIncompleteIncomeReport(LocalDate.of(2024, 10, 20))
        assertEquals(1, report.size)
    }

    @Test
    fun `adults that have expired incomes but their children does not have any placements, are not found in report`() {
        db.transaction { tx -> tx.insert(testIncome) }
        val report = getIncompleteIncomeReport(LocalDate.of(2024, 10, 20))
        assertEquals(0, report.size)
    }

    @Test
    fun `after editing expired incomes by evaka employee, persons are not found in report`() {
        db.transaction { tx ->
            tx.insert(testDecisionMaker_2)
            tx.insert(testIncomeUserEdited)
            children.forEach { it ->
                tx.insert(
                    DevPlacement(
                        childId = it.id,
                        unitId = testDaycare.id,
                        endDate = LocalDate.of(2024, 12, 31),
                    )
                )
            }
        }
        val report = getIncompleteIncomeReport(LocalDate.of(2024, 10, 20))
        assertEquals(0, report.size)
    }

    private fun getIncompleteIncomeReport(date: LocalDate): List<IncompleteIncomeDbRow> {

        return db.read { it.getIncompleteReport(date) }
    }
}
