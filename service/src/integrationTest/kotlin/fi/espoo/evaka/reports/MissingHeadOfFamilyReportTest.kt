// SPDX-FileCopyrightText: 2017-2022 City of Espoo
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
import fi.espoo.evaka.shared.dev.DevFridgeChild
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.insertFridgeChild
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals

class MissingHeadOfFamilyReportTest : FullApplicationTest(resetDbBeforeEach = true) {

    val today = LocalDate.now()

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
        }
    }

    @Test
    fun `query with no results`() {
        getAndAssert(today, today, listOf())
    }

    @Test
    fun `child with placement without HOF is shown`() {
        insertPlacement(testChild_1.id, today, today)
        getAndAssert(today, today, listOf(toReportRow(testChild_1, 1)))
    }

    @Test
    fun `child with placement who is deceased is not shown`() {
        db.transaction { it.createUpdate("UPDATE person set date_of_death = :dod").bind("dod", today.minusDays(1)).execute() }
        insertPlacement(testChild_1.id, today, today)
        getAndAssert(today, today, listOf())
    }

    @Test
    fun `child with HOF for each day is not shown`() {
        insertPlacement(testChild_1.id, today, today)
        db.transaction {
            it.insertFridgeChild(
                DevFridgeChild(
                    childId = testChild_1.id,
                    startDate = today,
                    endDate = today,
                    headOfChild = testAdult_1.id
                )
            )
        }
        getAndAssert(today, today, listOf())
    }

    private val testUser = AuthenticatedUser.Employee(EmployeeId(UUID.randomUUID()), setOf(UserRole.ADMIN))

    private fun getAndAssert(from: LocalDate, to: LocalDate, expected: List<MissingHeadOfFamilyReportRow>) {
        val (_, response, result) = http.get(
            "/reports/missing-head-of-family",
            listOf("from" to from, "to" to to)
        )
            .asUser(testUser)
            .responseObject<List<MissingHeadOfFamilyReportRow>>(jsonMapper)

        assertEquals(200, response.statusCode)
        assertEquals(expected, result.get())
    }

    private fun insertPlacement(childId: ChildId, startDate: LocalDate, endDate: LocalDate = startDate.plusYears(1)) =
        db.transaction { tx ->
            tx.insertTestPlacement(
                childId = childId,
                unitId = testDaycare.id,
                startDate = startDate,
                endDate = endDate
            )
        }

    private fun toReportRow(child: DevPerson, daysWithoutHead: Int) = MissingHeadOfFamilyReportRow(
        childId = child.id,
        firstName = child.firstName,
        lastName = child.lastName,
        daysWithoutHead = daysWithoutHead,
        careAreaName = testArea.name,
        unitId = testDaycare.id,
        unitName = testDaycare.name
    )
}
