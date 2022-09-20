// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testVoucherDaycare
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class StartingPlacementsReportTest : FullApplicationTest(resetDbBeforeEach = true) {
    @BeforeEach
    fun beforeEach() {
        db.transaction { tx -> tx.insertGeneralTestFixtures() }
    }

    @Test
    fun `query with no placements in database`() {
        val date = LocalDate.of(2019, 1, 1)

        getAndAssert(date, listOf())
    }

    @Test
    fun `child with single placement starting at query date is picked up`() {
        val date = LocalDate.of(2019, 1, 1)
        val placementStart = date
        insertPlacement(testChild_1.id, placementStart)

        getAndAssert(date, listOf(toReportRow(testChild_1, placementStart)))
    }

    @Test
    fun `child with single placement starting at the middle of the query month is picked up`() {
        val date = LocalDate.of(2019, 1, 1)
        val placementStart = date.withDayOfMonth(15)
        insertPlacement(testChild_1.id, placementStart)

        getAndAssert(date, listOf(toReportRow(testChild_1, placementStart)))
    }

    @Test
    fun `child with single placement starting before the query date is not picked up`() {
        val date = LocalDate.of(2019, 1, 1)
        val placementStart = date.minusMonths(1)
        insertPlacement(testChild_1.id, placementStart)

        getAndAssert(date, listOf())
    }

    @Test
    fun `child with single placement starting after the query month is not picked up`() {
        val date = LocalDate.of(2019, 1, 1)
        val placementStart = date.plusMonths(1)
        insertPlacement(testChild_1.id, placementStart)

        getAndAssert(date, listOf())
    }

    @Test
    fun `child with a placement before with no gap between is not picked up`() {
        val date = LocalDate.of(2019, 1, 1)
        val placementStart = date
        insertPlacement(testChild_1.id, placementStart)
        insertPlacement(testChild_1.id, placementStart.minusMonths(1), placementStart.minusDays(1))

        getAndAssert(date, listOf())
    }

    @Test
    fun `child with a placement before with a gap between is picked up`() {
        val date = LocalDate.of(2019, 1, 1)
        val placementStart = date
        insertPlacement(testChild_1.id, placementStart)
        insertPlacement(testChild_1.id, placementStart.minusMonths(1), placementStart.minusDays(2))

        getAndAssert(date, listOf(toReportRow(testChild_1, placementStart)))
    }

    @Test
    fun `child with a placement after is picked up`() {
        val date = LocalDate.of(2019, 1, 1)
        val placementStart = date
        insertPlacement(testChild_1.id, placementStart, placementStart.plusMonths(1))
        insertPlacement(
            testChild_1.id,
            placementStart.plusMonths(1).plusDays(1),
            placementStart.plusMonths(2)
        )

        getAndAssert(date, listOf(toReportRow(testChild_1, placementStart)))
    }

    @Test
    fun `child in a service voucher daycare is picked up`() {
        val date = LocalDate.of(2019, 1, 1)
        val placementStart = date
        insertPlacement(testChild_1.id, placementStart, placementStart, testVoucherDaycare)
        getAndAssert(date, listOf(toReportRow(testChild_1, placementStart, "palvelusetelialue")))
    }

    private val testUser =
        AuthenticatedUser.Employee(EmployeeId(UUID.randomUUID()), setOf(UserRole.ADMIN))

    private fun getAndAssert(date: LocalDate, expected: List<StartingPlacementsRow>) {
        val (_, response, result) =
            http
                .get(
                    "/reports/starting-placements",
                    listOf("year" to date.year, "month" to date.monthValue)
                )
                .asUser(testUser)
                .responseObject<List<StartingPlacementsRow>>(jsonMapper)

        assertEquals(200, response.statusCode)
        assertEquals(expected, result.get())
    }

    private fun insertPlacement(
        childId: ChildId,
        startDate: LocalDate,
        endDate: LocalDate = startDate.plusYears(1),
        daycare: DevDaycare = testDaycare
    ) =
        db.transaction { tx ->
            tx.insertTestPlacement(
                childId = childId,
                unitId = daycare.id,
                startDate = startDate,
                endDate = endDate
            )
        }

    private fun toReportRow(
        child: DevPerson,
        startDate: LocalDate,
        careAreaName: String = testArea.name
    ) =
        StartingPlacementsRow(
            childId = child.id,
            firstName = child.firstName,
            lastName = child.lastName,
            dateOfBirth = child.dateOfBirth,
            ssn = child.ssn,
            placementStart = startDate,
            careAreaName = careAreaName
        )
}
