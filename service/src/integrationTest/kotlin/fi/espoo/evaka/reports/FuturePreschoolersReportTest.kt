// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevGuardian
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_2
import fi.espoo.evaka.testRoundTheClockDaycare
import fi.espoo.evaka.testVoucherDaycare
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FuturePreschoolersReportTest : PureJdbiTest(resetDbBeforeEach = true) {


    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(testArea)
            testDecisionMaker_2.let {
                tx.insert(DevEmployee(id = it.id, firstName = it.firstName, lastName = it.lastName))
            }
            tx.insert(testDaycare.copy(openingDate = LocalDate.of(2000, 1, 1)))
            tx.insert(testVoucherDaycare.copy(openingDate = LocalDate.of(2000, 1, 1)))
            tx.insert(testRoundTheClockDaycare.copy(openingDate = LocalDate.of(2000, 1, 1)))
            tx.insert(
                DevDaycareGroup(
                    id = GroupId(UUID.randomUUID()),
                    daycareId = testDaycare.id,
                    name = "Test group 1"
                )
            )
            tx.insert(
                DevDaycareGroup(
                    id = GroupId(UUID.randomUUID()),
                    daycareId = testVoucherDaycare.id,
                    name = "Test voucher group 1"
                )
            )
        }
    }

    @Test
    fun `children with correct age are found in report`() {
        db.transaction { tx ->
            val children =
                setOf(
                    DevPerson(
                        id = ChildId(UUID.randomUUID()),
                        dateOfBirth = LocalDate.of(LocalDate.now().year - 5, 6, 1),
                        ssn = "111111-999X",
                        firstName = "Just",
                        lastName = "Sopiva",
                        streetAddress = "Testitie 1",
                        postalCode = "02770",
                        postOffice = "Espoo",
                        restrictedDetailsEnabled = false
                    ),
                    DevPerson(
                        id = ChildId(UUID.randomUUID()),
                        dateOfBirth = LocalDate.of(LocalDate.now().year - 4, 7, 1),
                        ssn = "222222-998Y",
                        firstName = "Turhan",
                        lastName = "Nuori",
                        streetAddress = "Testitie 2",
                        postalCode = "02770",
                        postOffice = "Espoo",
                        restrictedDetailsEnabled = false
                    ),
                    DevPerson(
                        id = ChildId(UUID.randomUUID()),
                        dateOfBirth = LocalDate.of(LocalDate.now().year - 6, 8, 1),
                        ssn = "333333-997Z",
                        firstName = "Liian",
                        lastName = "Vanha",
                        streetAddress = "Testitie 3",
                        postalCode = "02770",
                        postOffice = "Espoo",
                        restrictedDetailsEnabled = false
                    )
                )
            tx.insert(testAdult_1, DevPersonType.RAW_ROW)
            tx.insert(testAdult_2, DevPersonType.RAW_ROW)
            children.forEachIndexed { i, it ->
                tx.insert(it, DevPersonType.CHILD)
                tx.insert(
                    DevPlacement(
                        childId = it.id,
                        unitId = testDaycare.id,
                        endDate = LocalDate.now()
                    )
                )
                tx.insert(DevGuardian(guardianId = testAdult_1.id, childId = it.id))
                if (i % 2 == 1) tx.insert(DevGuardian(guardianId = testAdult_2.id, childId = it.id))
            }
        }

        val report = getChildrenReport()
        assertEquals(1, report.size)
    }

    @Test
    fun `open preschool units are found in report`() {
        val report = getUnitReport()
        assertEquals(2, report.size)
    }

    @Test
    fun `open source are found in report`() {
        val report = getSourceUnitReport()
        assertEquals(3, report.size)
    }

    private fun getChildrenReport(): List<FuturePreschoolersReportRow> {
        return db.read { it.getFuturePreschoolerRows(LocalDate.of(2023, 1, 1)) }
    }

    private fun getUnitReport(): List<PreschoolUnitsReportRow> {
        return db.read { it.getPreschoolUnitsRows(LocalDate.of(2023, 1, 1)) }
    }

    private fun getSourceUnitReport(): List<SourceUnitsReportRow> {
        return db.read { it.getSourceUnitsRows(LocalDate.of(2023, 1, 1)) }
    }
}
