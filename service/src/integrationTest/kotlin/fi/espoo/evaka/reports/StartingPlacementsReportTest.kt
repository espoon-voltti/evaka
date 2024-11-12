// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testVoucherDaycare
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class StartingPlacementsReportTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired
    private lateinit var startingPlacementsReportController: StartingPlacementsReportController
    val testClock =
        MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2019, 1, 1), LocalTime.of(8, 0)))

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(testArea)
            tx.insert(testDaycare)
            tx.insert(testVoucherDaycare)
            tx.insert(testChild_1, DevPersonType.CHILD)
        }
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
        val placementId = insertPlacement(testChild_1.id, placementStart)

        getAndAssert(
            date,
            listOf(toReportRow(testChild_1, placementStart, testDaycare, placementId)),
        )
    }

    @Test
    fun `child with single placement starting at the middle of the query month is picked up`() {
        val date = LocalDate.of(2019, 1, 1)
        val placementStart = date.withDayOfMonth(15)
        val placementId = insertPlacement(testChild_1.id, placementStart)

        getAndAssert(
            date,
            listOf(toReportRow(testChild_1, placementStart, testDaycare, placementId)),
        )
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
        val secondPlacementId = insertPlacement(testChild_1.id, placementStart)
        insertPlacement(testChild_1.id, placementStart.minusMonths(1), placementStart.minusDays(2))

        getAndAssert(
            date,
            listOf(toReportRow(testChild_1, placementStart, testDaycare, secondPlacementId)),
        )
    }

    @Test
    fun `child with a placement after is picked up`() {
        val date = LocalDate.of(2019, 1, 1)
        val placementStart = date
        val firstPlacementId =
            insertPlacement(testChild_1.id, placementStart, placementStart.plusMonths(1))
        insertPlacement(
            testChild_1.id,
            placementStart.plusMonths(1).plusDays(1),
            placementStart.plusMonths(2),
        )

        getAndAssert(
            date,
            listOf(toReportRow(testChild_1, placementStart, testDaycare, firstPlacementId)),
        )
    }

    @Test
    fun `child in a service voucher daycare is picked up`() {
        val date = LocalDate.of(2019, 1, 1)
        val placementStart = date
        val placementId =
            insertPlacement(testChild_1.id, placementStart, placementStart, testVoucherDaycare)
        getAndAssert(
            date,
            listOf(
                toReportRow(
                    testChild_1,
                    placementStart,
                    testVoucherDaycare,
                    placementId,
                    "palvelusetelialue",
                )
            ),
        )
    }

    private val testUnitSupervisor =
        DevEmployee(id = EmployeeId(UUID.randomUUID()), roles = setOf())

    private val testUnitSupervisor2 =
        DevEmployee(id = EmployeeId(UUID.randomUUID()), roles = setOf())

    @Test
    fun `unit supervisor can see only their own unit's placements`() {
        val startDay = testClock.today()
        val anotherDaycare =
            DevDaycare(
                id = DaycareId(UUID.randomUUID()),
                name = "Another Daycare",
                areaId = testArea.id,
            )
        val anotherPlacement =
            DevPlacement(
                childId = testChild_2.id,
                unitId = anotherDaycare.id,
                startDate = startDay,
                endDate = startDay.plusMonths(1),
            )

        db.transaction { tx ->
            tx.insert(anotherDaycare)

            tx.insert(testUnitSupervisor)
            tx.insert(testUnitSupervisor2)

            tx.insertDaycareAclRow(
                anotherDaycare.id,
                testUnitSupervisor.id,
                UserRole.UNIT_SUPERVISOR,
            )
            tx.insertDaycareAclRow(testDaycare.id, testUnitSupervisor2.id, UserRole.UNIT_SUPERVISOR)

            tx.insert(testChild_2, DevPersonType.CHILD)
            tx.insert(anotherPlacement)
        }

        val result =
            startingPlacementsReportController.getStartingPlacementsReport(
                dbInstance(),
                testUnitSupervisor.user,
                testClock,
                startDay.year,
                startDay.monthValue,
            )
        assertEquals(
            listOf(
                toReportRow(
                    testChild_2,
                    startDay,
                    anotherDaycare,
                    anotherPlacement.id,
                    testArea.name,
                )
            ),
            result,
        )

        val result2 =
            startingPlacementsReportController.getStartingPlacementsReport(
                dbInstance(),
                testUnitSupervisor2.user,
                testClock,
                startDay.year,
                startDay.monthValue,
            )

        assertEquals(emptyList(), result2)
    }

    private val testAdmin =
        AuthenticatedUser.Employee(EmployeeId(UUID.randomUUID()), setOf(UserRole.ADMIN))

    private fun getAndAssert(date: LocalDate, expected: List<StartingPlacementsRow>) {
        val result =
            startingPlacementsReportController.getStartingPlacementsReport(
                dbInstance(),
                testAdmin,
                testClock,
                date.year,
                date.monthValue,
            )
        assertEquals(expected, result)
    }

    private fun insertPlacement(
        childId: ChildId,
        startDate: LocalDate,
        endDate: LocalDate = startDate.plusYears(1),
        daycare: DevDaycare = testDaycare,
    ) =
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = childId,
                    unitId = daycare.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )
        }

    private fun toReportRow(
        child: DevPerson,
        startDate: LocalDate,
        unit: DevDaycare,
        placementId: PlacementId,
        careAreaName: String = testArea.name,
    ) =
        StartingPlacementsRow(
            childId = child.id,
            firstName = child.firstName,
            lastName = child.lastName,
            dateOfBirth = child.dateOfBirth,
            placementStart = startDate,
            careAreaName = careAreaName,
            unitName = unit.name,
            placementId = placementId,
        )
}
