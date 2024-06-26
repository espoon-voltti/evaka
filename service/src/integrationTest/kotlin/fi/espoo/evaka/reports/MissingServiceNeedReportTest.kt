// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.insertServiceNeedOptions
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testVoucherDaycare
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MissingServiceNeedReportTest : FullApplicationTest(resetDbBeforeEach = true) {
    val today = LocalDate.now()

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
            tx.insert(testChild_1, DevPersonType.CHILD)
            tx.insertServiceNeedOptions()
        }
    }

    @Test
    fun `child without service need is reported`() {
        insertPlacement(testChild_1.id, today, today, testDaycare)
        getAndAssert(
            today,
            today,
            listOf(
                MissingServiceNeedReportRow(
                    careAreaName = testArea.name,
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    unitName = testDaycare.name,
                    daysWithoutServiceNeed = 1,
                    firstName = testChild_1.firstName,
                    lastName = testChild_1.lastName
                )
            )
        )
    }

    @Test
    fun `child without service need in service voucher unit shown on service voucher care area`() {
        insertPlacement(testChild_1.id, today, today, testVoucherDaycare)
        getAndAssert(
            today,
            today,
            listOf(
                MissingServiceNeedReportRow(
                    careAreaName = "palvelusetelialue",
                    childId = testChild_1.id,
                    unitId = testVoucherDaycare.id,
                    unitName = testVoucherDaycare.name,
                    daysWithoutServiceNeed = 1,
                    firstName = testChild_1.firstName,
                    lastName = testChild_1.lastName
                )
            )
        )
    }

    private fun insertPlacement(
        childId: ChildId,
        startDate: LocalDate,
        endDate: LocalDate = startDate.plusYears(1),
        daycare: DevDaycare
    ) =
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = childId,
                    unitId = daycare.id,
                    startDate = startDate,
                    endDate = endDate
                )
            )
        }

    private val testUser =
        AuthenticatedUser.Employee(EmployeeId(UUID.randomUUID()), setOf(UserRole.ADMIN))

    private fun getAndAssert(
        from: LocalDate,
        to: LocalDate,
        expected: List<MissingServiceNeedReportRow>
    ) {
        val (_, response, result) =
            http
                .get("/reports/missing-service-need", listOf("from" to from, "to" to to))
                .asUser(testUser)
                .responseObject<List<MissingServiceNeedReportRow>>(jsonMapper)

        assertEquals(200, response.statusCode)
        assertEquals(expected, result.get())
    }
}
