// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
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

    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val voucherDaycare =
        DevDaycare(areaId = area.id, providerType = ProviderType.PRIVATE_SERVICE_VOUCHER)
    private val child1 = DevPerson()
    private val child2 = DevPerson()

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(voucherDaycare)
            tx.insert(child1, DevPersonType.CHILD)
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
        val placementId = insertPlacement(child1.id, placementStart)

        getAndAssert(date, listOf(toReportRow(child1, placementStart, daycare, placementId)))
    }

    @Test
    fun `child with single placement starting at the middle of the query month is picked up`() {
        val date = LocalDate.of(2019, 1, 1)
        val placementStart = date.withDayOfMonth(15)
        val placementId = insertPlacement(child1.id, placementStart)

        getAndAssert(date, listOf(toReportRow(child1, placementStart, daycare, placementId)))
    }

    @Test
    fun `child with single placement starting before the query date is not picked up`() {
        val date = LocalDate.of(2019, 1, 1)
        val placementStart = date.minusMonths(1)
        insertPlacement(child1.id, placementStart)

        getAndAssert(date, listOf())
    }

    @Test
    fun `child with single placement starting after the query month is not picked up`() {
        val date = LocalDate.of(2019, 1, 1)
        val placementStart = date.plusMonths(1)
        insertPlacement(child1.id, placementStart)

        getAndAssert(date, listOf())
    }

    @Test
    fun `child with a placement before with no gap between is not picked up`() {
        val date = LocalDate.of(2019, 1, 1)
        val placementStart = date
        insertPlacement(child1.id, placementStart)
        insertPlacement(child1.id, placementStart.minusMonths(1), placementStart.minusDays(1))

        getAndAssert(date, listOf())
    }

    @Test
    fun `child with a placement before with a gap between is picked up`() {
        val date = LocalDate.of(2019, 1, 1)
        val placementStart = date
        val secondPlacementId = insertPlacement(child1.id, placementStart)
        insertPlacement(child1.id, placementStart.minusMonths(1), placementStart.minusDays(2))

        getAndAssert(date, listOf(toReportRow(child1, placementStart, daycare, secondPlacementId)))
    }

    @Test
    fun `child with a placement after is picked up`() {
        val date = LocalDate.of(2019, 1, 1)
        val placementStart = date
        val firstPlacementId =
            insertPlacement(child1.id, placementStart, placementStart.plusMonths(1))
        insertPlacement(
            child1.id,
            placementStart.plusMonths(1).plusDays(1),
            placementStart.plusMonths(2),
        )

        getAndAssert(date, listOf(toReportRow(child1, placementStart, daycare, firstPlacementId)))
    }

    @Test
    fun `child in a service voucher daycare is picked up`() {
        val date = LocalDate.of(2019, 1, 1)
        val placementStart = date
        val placementId = insertPlacement(child1.id, placementStart, placementStart, voucherDaycare)
        getAndAssert(
            date,
            listOf(
                toReportRow(
                    child1,
                    placementStart,
                    voucherDaycare,
                    placementId,
                    "palvelusetelialue",
                )
            ),
        )
    }

    private val testUnitSupervisor = DevEmployee(roles = setOf())

    private val testUnitSupervisor2 = DevEmployee(roles = setOf())

    @Test
    fun `unit supervisor can see only their own unit's placements`() {
        val startDay = testClock.today()
        val anotherDaycare = DevDaycare(name = "Another Daycare", areaId = area.id)
        val anotherPlacement =
            DevPlacement(
                childId = child2.id,
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
            tx.insertDaycareAclRow(daycare.id, testUnitSupervisor2.id, UserRole.UNIT_SUPERVISOR)

            tx.insert(child2, DevPersonType.CHILD)
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
            listOf(toReportRow(child2, startDay, anotherDaycare, anotherPlacement.id, area.name)),
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
        unit: DevDaycare = daycare,
    ) =
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = childId,
                    unitId = unit.id,
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
        careAreaName: String = area.name,
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
