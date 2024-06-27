// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevFridgeChild
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FamilyConflictReportTest : FullApplicationTest(resetDbBeforeEach = true) {
    val today = LocalDate.now()

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(testArea)
            tx.insert(testDaycare)
            tx.insert(testDaycare2)
            tx.insert(testAdult_1, DevPersonType.ADULT)
            tx.insert(testChild_1, DevPersonType.CHILD)
        }
    }

    @Test
    fun `conflicting child relationship without a placement is not shown`() {
        db.transaction {
            it.insert(
                DevFridgeChild(
                    childId = testChild_1.id,
                    startDate = today,
                    endDate = today.plusYears(1),
                    headOfChild = testAdult_1.id,
                    conflict = true
                )
            )
        }

        getAndAssert(today, today, listOf())
    }

    @Test
    fun `conflicting child relationship with a placement is shown`() {
        db.transaction {
            it.insert(
                DevFridgeChild(
                    childId = testChild_1.id,
                    startDate = today,
                    endDate = today.plusYears(1),
                    headOfChild = testAdult_1.id,
                    conflict = true
                )
            )
        }
        insertPlacement(testChild_1.id, today, today)

        getAndAssert(today, today, listOf(toReportRow(testAdult_1, 0, 1)))
    }

    @Test
    fun `normal child relationship with a placement is not shown`() {
        db.transaction {
            it.insert(
                DevFridgeChild(
                    childId = testChild_1.id,
                    startDate = today,
                    endDate = today.plusYears(1),
                    headOfChild = testAdult_1.id,
                    conflict = false
                )
            )
        }
        insertPlacement(testChild_1.id, today, today)

        getAndAssert(today, today, listOf())
    }

    @Test
    fun `conflicting child relationship with a future placement is shown`() {
        db.transaction {
            it.insert(
                DevFridgeChild(
                    childId = testChild_1.id,
                    startDate = today,
                    endDate = today.plusYears(1),
                    headOfChild = testAdult_1.id,
                    conflict = true
                )
            )
        }
        insertPlacement(testChild_1.id, today.plusDays(7), today.plusDays(14))

        getAndAssert(today, today, listOf(toReportRow(testAdult_1, 0, 1)))
    }

    @Test
    fun `conflicting child relationship show the unit of the first placement`() {
        db.transaction {
            it.insert(
                DevFridgeChild(
                    childId = testChild_1.id,
                    startDate = today,
                    endDate = today.plusYears(1),
                    headOfChild = testAdult_1.id,
                    conflict = true
                )
            )
        }
        insertPlacement(testChild_1.id, today.plusDays(8), today.plusDays(14), testDaycare.id)
        insertPlacement(testChild_1.id, today.plusDays(1), today.plusDays(7), testDaycare2.id)

        getAndAssert(today, today, listOf(toReportRow(testAdult_1, 0, 1, testDaycare2)))
    }

    private val testUser =
        AuthenticatedUser.Employee(EmployeeId(UUID.randomUUID()), setOf(UserRole.ADMIN))

    private fun getAndAssert(
        from: LocalDate,
        to: LocalDate,
        expected: List<FamilyConflictReportRow>
    ) {
        val (_, response, result) =
            http
                .get("/reports/family-conflicts", listOf("from" to from, "to" to to))
                .asUser(testUser)
                .responseObject<List<FamilyConflictReportRow>>(jsonMapper)

        assertEquals(200, response.statusCode)
        assertEquals(expected, result.get())
    }

    private fun insertPlacement(
        childId: ChildId,
        startDate: LocalDate,
        endDate: LocalDate,
        unitId: DaycareId = testDaycare.id
    ) =
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = childId,
                    unitId = unitId,
                    startDate = startDate,
                    endDate = endDate
                )
            )
        }

    private fun toReportRow(
        person: DevPerson,
        partnerConflictCount: Int,
        childConflictCount: Int,
        unit: DevDaycare = testDaycare
    ) =
        FamilyConflictReportRow(
            careAreaName = testArea.name,
            unitId = unit.id,
            unitName = unit.name,
            id = person.id,
            firstName = person.firstName,
            lastName = person.lastName,
            socialSecurityNumber = person.ssn,
            partnerConflictCount = partnerConflictCount,
            childConflictCount = childConflictCount
        )
}
