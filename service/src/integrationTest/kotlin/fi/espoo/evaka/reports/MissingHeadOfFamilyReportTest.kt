// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.shared.DatabaseTable
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevFosterParent
import fi.espoo.evaka.shared.dev.DevFridgeChild
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.insertFosterParent
import fi.espoo.evaka.shared.dev.insertFridgeChild
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class MissingHeadOfFamilyReportTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired
    private lateinit var missingHeadOfFamilyReportController: MissingHeadOfFamilyReportController

    private val today: LocalDate = LocalDate.now()
    private val employeeId = EmployeeId(UUID.randomUUID())
    private val user = AuthenticatedUser.Employee(employeeId, setOf(UserRole.ADMIN))

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
            tx.insertTestEmployee(
                DevEmployee(
                    id = employeeId,
                    firstName = "Test",
                    lastName = "Employee",
                    roles = setOf(UserRole.ADMIN)
                )
            )
        }
    }

    @Test
    fun `query with no results`() {
        assertEquals(listOf(), getReport(today, today))
    }

    @Test
    fun `child with placement without head of family is shown`() {
        db.transaction {
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = today,
                endDate = today,
            )
        }

        assertEquals(
            listOf(toReportRow(testChild_1, listOf(FiniteDateRange(today, today)))),
            getReport(today, today)
        )
    }

    @Test
    fun `child with placement who is deceased is not shown`() {
        db.transaction {
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = today,
                endDate = today,
            )
            it.createUpdate("UPDATE person set date_of_death = :dod")
                .bind("dod", today.minusDays(1))
                .execute()
        }

        assertEquals(listOf(), getReport(today, today))
    }

    @Test
    fun `child with head of family for each day is not shown`() {
        db.transaction {
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = today,
                endDate = today,
            )
            it.insertFridgeChild(
                DevFridgeChild(
                    childId = testChild_1.id,
                    startDate = today,
                    endDate = today,
                    headOfChild = testAdult_1.id
                )
            )
        }

        assertEquals(listOf(), getReport(today, today))
    }

    @Test
    fun `child with foster parent is shown depending on query param`() {
        db.transaction {
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = today,
                endDate = today,
            )
            it.insertFosterParent(
                DevFosterParent(
                    childId = testChild_1.id,
                    parentId = testAdult_1.id,
                    validDuring = DateRange(today, today)
                )
            )
        }

        assertEquals(listOf(), getReport(today, today, showFosterChildren = false))
        assertEquals(
            listOf(toReportRow(testChild_1, listOf(FiniteDateRange(today, today)))),
            getReport(today, today, showFosterChildren = true)
        )
    }

    @Test
    fun `duplicate child is shown depending on query param`() {
        db.transaction {
            it.createUpdate<DatabaseTable> {
                    sql(
                        """
                        UPDATE person
                        SET duplicate_of = ${bind(testChild_2.id)}
                        WHERE id = ${bind(testChild_1.id)}
                        """
                    )
                }
                .execute()
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = today,
                endDate = today,
            )
        }

        assertEquals(listOf(), getReport(today, today, showIntentionalDuplicates = false))
        assertEquals(
            listOf(toReportRow(testChild_1, listOf(FiniteDateRange(today, today)))),
            getReport(today, today, showIntentionalDuplicates = true)
        )
    }

    @Test
    fun `ranges without head are computed correctly`() {
        db.transaction {
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = today,
                endDate = today.plusDays(10),
            )
            it.insertFridgeChild(
                DevFridgeChild(
                    childId = testChild_1.id,
                    headOfChild = testAdult_1.id,
                    startDate = today.plusDays(1),
                    endDate = today.plusDays(2),
                )
            )
            it.insertFridgeChild(
                DevFridgeChild(
                    childId = testChild_1.id,
                    headOfChild = testAdult_1.id,
                    startDate = today.plusDays(5),
                    endDate = today.plusDays(6),
                )
            )
            it.insertFosterParent(
                DevFosterParent(
                    childId = testChild_1.id,
                    parentId = testAdult_1.id,
                    validDuring = DateRange(today.plusDays(8), today.plusDays(8))
                )
            )
        }

        assertEquals(
            listOf(
                toReportRow(
                    testChild_1,
                    listOf(
                        FiniteDateRange(today, today),
                        FiniteDateRange(today.plusDays(3), today.plusDays(4)),
                        FiniteDateRange(today.plusDays(7), today.plusDays(7)),
                        FiniteDateRange(today.plusDays(9), today.plusDays(10))
                    )
                ),
            ),
            getReport(today, today.plusDays(11), showFosterChildren = false)
        )
        assertEquals(
            listOf(
                toReportRow(
                    testChild_1,
                    listOf(
                        FiniteDateRange(today, today),
                        FiniteDateRange(today.plusDays(3), today.plusDays(4)),
                        FiniteDateRange(today.plusDays(7), today.plusDays(10))
                    )
                ),
            ),
            getReport(today, today.plusDays(11), showFosterChildren = true)
        )
    }

    @Test
    fun `works with multiple children`() {
        db.transaction {
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = today,
                endDate = today,
            )
            it.insertTestPlacement(
                childId = testChild_2.id,
                unitId = testDaycare2.id,
                startDate = today,
                endDate = today,
            )
        }

        assertEquals(
            listOf(
                    toReportRow(testChild_1, listOf(FiniteDateRange(today, today))),
                    toReportRow(testChild_2, listOf(FiniteDateRange(today, today))),
                )
                .sortedWith(compareBy({ it.lastName }, { it.firstName })),
            getReport(today, today)
        )
    }

    private fun getReport(
        from: LocalDate,
        to: LocalDate?,
        showFosterChildren: Boolean = false,
        showIntentionalDuplicates: Boolean = false
    ): List<MissingHeadOfFamilyReportRow> =
        missingHeadOfFamilyReportController.getMissingHeadOfFamilyReport(
            dbInstance(),
            user,
            RealEvakaClock(),
            from,
            to,
            showFosterChildren = showFosterChildren,
            showIntentionalDuplicates = showIntentionalDuplicates
        )

    private fun toReportRow(child: DevPerson, rangesWithoutHead: List<FiniteDateRange>) =
        MissingHeadOfFamilyReportRow(
            childId = child.id,
            firstName = child.firstName,
            lastName = child.lastName,
            rangesWithoutHead = rangesWithoutHead
        )
}
