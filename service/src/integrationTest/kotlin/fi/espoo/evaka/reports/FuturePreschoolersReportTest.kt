// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.ParentshipId
import fi.espoo.evaka.shared.dev.DevChild
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevParentship
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insertTestCareArea
import fi.espoo.evaka.shared.dev.insertTestChild
import fi.espoo.evaka.shared.dev.insertTestDaycare
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.shared.dev.insertTestParentship
import fi.espoo.evaka.shared.dev.insertTestPerson
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_2
import fi.espoo.evaka.testVoucherDaycare
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals

class FuturePreschoolersReportTest : PureJdbiTest(resetDbBeforeEach = true) {

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertTestCareArea(testArea)
            testDecisionMaker_2.let {
                tx.insertTestEmployee(
                    DevEmployee(id = it.id, firstName = it.firstName, lastName = it.lastName)
                )
            }
            tx.insertTestDaycare(testDaycare)
            tx.insertTestDaycare(testVoucherDaycare)
            tx.insertTestDaycareGroup(DevDaycareGroup(
                id = GroupId(UUID.randomUUID()),
                daycareId = testDaycare.id,
                name = "Test group 1"
            ))
            tx.insertTestDaycareGroup(DevDaycareGroup(
                id = GroupId(UUID.randomUUID()),
                daycareId = testVoucherDaycare.id,
                name = "Test voucher group 1"
            ))
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
            tx.insertTestPerson(testAdult_1)
            children.forEach {
                tx.insertTestPerson(it)
                tx.insertTestChild(DevChild(id = it.id))
                tx.insertTestPlacement(
                    DevPlacement(
                        childId = it.id,
                        unitId = testDaycare.id,
                        endDate = LocalDate.now()
                    )
                )
                tx.insertTestParentship(
                    DevParentship(
                        id = ParentshipId(UUID.randomUUID()),
                        headOfChildId = testAdult_1.id,
                        childId = it.id,
                        startDate = it.dateOfBirth,
                        endDate = LocalDate.now()
                    )
                )
            }
        }

        val report = getChildrenReport()
        assertEquals(1, report.size)
    }

    @Test
    fun `municipal groups are found in report`() {
        val report = getGroupReport(true)
        assertEquals(1, report.size)
    }

    @Test
    fun `private groups are found in report`() {
        val report = getGroupReport(false)
        assertEquals(1, report.size)
    }

    private fun getChildrenReport(): List<FuturePreschoolersReportRow> {
        return db.read { it.getFuturePreschoolerRows() }
    }

    private fun getGroupReport(municipal: Boolean): List<PreschoolGroupsReportRow> {
        return db.read { it.getPreschoolGroupsRows(municipal) }
    }
}
