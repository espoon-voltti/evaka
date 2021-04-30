// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.occupancy

import com.fasterxml.jackson.module.kotlin.readValue
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.reports.OccupancyReportRowGroupedValues
import fi.espoo.evaka.reports.OccupancyUnitReportResultRow
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestDaycare
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.dev.insertTestPlacementPlan
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAreaId
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class PlannedOccupancyTest : FullApplicationTest() {
    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
        }
    }

    @AfterEach
    fun afterEach() {
        db.transaction { tx ->
            tx.resetDatabase()
        }
    }

    private val defaultPeriod = FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
    val defaultPeriodSplit1 = FiniteDateRange(defaultPeriod.start, LocalDate.of(2019, 1, 15))
    val defaultPeriodSplit2 = FiniteDateRange(LocalDate.of(2019, 1, 16), defaultPeriod.end)

    @Test
    fun `planned occupancy calculation does not break when there are no children placed into a unit`() {
        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 0.0, 0)),
            result
        )

        val reportResult1 = fetchAndParsePlannedOccupancyReportForUnitDay(testAreaId, testDaycare.id, defaultPeriod.start)
        assertEquals(0.0, reportResult1?.sum)
        assertEquals(0, reportResult1?.headcount)
        val reportResult2 = fetchAndParsePlannedOccupancyReportForUnitDay(testAreaId, testDaycare.id, defaultPeriod.end)
        assertEquals(0.0, reportResult2?.sum)
        assertEquals(0, reportResult2?.headcount)
    }

    @Test
    fun `occupancy calculation is correct for child over 3 years old with full time placement plan`() {
        db.transaction {
            it.createPlanOccupancyTestFixture(
                testDaycare.id,
                defaultPeriod,
                LocalDate.of(2015, 1, 1),
                PlacementType.DAYCARE
            )
        }

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 1.0, 1)),
            result
        )

        val reportResult1 = fetchAndParsePlannedOccupancyReportForUnitDay(testAreaId, testDaycare.id, defaultPeriod.start)
        assertEquals(1.0, reportResult1?.sum)
        assertEquals(1, reportResult1?.headcount)
        val reportResult2 = fetchAndParsePlannedOccupancyReportForUnitDay(testAreaId, testDaycare.id, defaultPeriod.end)
        assertEquals(1.0, reportResult2?.sum)
        assertEquals(1, reportResult2?.headcount)
    }

    @Test
    fun `occupancy calculation is correct for child under 3 years old with full time placement plan`() {
        db.transaction {
            it.createPlanOccupancyTestFixture(
                testDaycare.id,
                defaultPeriod,
                LocalDate.of(2017, 1, 1),
                PlacementType.DAYCARE
            )
        }

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 1.75, 1)),
            result
        )

        val reportResult1 = fetchAndParsePlannedOccupancyReportForUnitDay(testAreaId, testDaycare.id, defaultPeriod.start)
        assertEquals(1.75, reportResult1?.sum)
        assertEquals(1, reportResult1?.headcount)
        val reportResult2 = fetchAndParsePlannedOccupancyReportForUnitDay(testAreaId, testDaycare.id, defaultPeriod.end)
        assertEquals(1.75, reportResult2?.sum)
        assertEquals(1, reportResult2?.headcount)
    }

    @Test
    fun `occupancy calculation is correct for child over 3 years old with part time placement plan`() {
        db.transaction {
            it.createPlanOccupancyTestFixture(
                testDaycare.id,
                defaultPeriod,
                LocalDate.of(2015, 1, 1),
                PlacementType.DAYCARE_PART_TIME
            )
        }

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 0.54, 1)),
            result
        )

        val reportResult1 = fetchAndParsePlannedOccupancyReportForUnitDay(testAreaId, testDaycare.id, defaultPeriod.start)
        assertEquals(0.54, reportResult1?.sum)
        assertEquals(1, reportResult1?.headcount)
        val reportResult2 = fetchAndParsePlannedOccupancyReportForUnitDay(testAreaId, testDaycare.id, defaultPeriod.end)
        assertEquals(0.54, reportResult2?.sum)
        assertEquals(1, reportResult2?.headcount)
    }

    @Test
    fun `occupancy calculation is correct for child over 3 years old with only preschool placement plan`() {
        db.transaction {
            it.createPlanOccupancyTestFixture(
                testDaycare.id,
                defaultPeriod,
                LocalDate.of(2015, 1, 1),
                PlacementType.PRESCHOOL
            )
        }

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 0.5, 1)),
            result
        )

        val reportResult1 = fetchAndParsePlannedOccupancyReportForUnitDay(testAreaId, testDaycare.id, defaultPeriod.start)
        assertEquals(0.5, reportResult1?.sum)
        assertEquals(1, reportResult1?.headcount)
        val reportResult2 = fetchAndParsePlannedOccupancyReportForUnitDay(testAreaId, testDaycare.id, defaultPeriod.end)
        assertEquals(0.5, reportResult2?.sum)
        assertEquals(1, reportResult2?.headcount)
    }

    @Test
    fun `occupancy calculation is correct for child under 3 years old with part time placement plan`() {
        db.transaction {
            it.createPlanOccupancyTestFixture(
                testDaycare.id,
                defaultPeriod,
                LocalDate.of(2017, 1, 1),
                PlacementType.DAYCARE_PART_TIME
            )
        }

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 1.75, 1)),
            result
        )

        val reportResult1 = fetchAndParsePlannedOccupancyReportForUnitDay(testAreaId, testDaycare.id, defaultPeriod.start)
        assertEquals(1.75, reportResult1?.sum)
        assertEquals(1, reportResult1?.headcount)
        val reportResult2 = fetchAndParsePlannedOccupancyReportForUnitDay(testAreaId, testDaycare.id, defaultPeriod.end)
        assertEquals(1.75, reportResult2?.sum)
        assertEquals(1, reportResult2?.headcount)
    }

    @Test
    fun `occupancy calculation is correct for child over 3 years old with full time placement plan and assistance need`() {
        db.transaction {
            it.createPlanOccupancyTestFixture(
                testDaycare.id,
                defaultPeriod,
                LocalDate.of(2015, 1, 1),
                PlacementType.DAYCARE,
                3.0
            )
        }

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 3.0, 1)),
            result
        )

        val reportResult1 = fetchAndParsePlannedOccupancyReportForUnitDay(testAreaId, testDaycare.id, defaultPeriod.start)
        assertEquals(3.0, reportResult1?.sum)
        assertEquals(1, reportResult1?.headcount)
        val reportResult2 = fetchAndParsePlannedOccupancyReportForUnitDay(testAreaId, testDaycare.id, defaultPeriod.end)
        assertEquals(3.0, reportResult2?.sum)
        assertEquals(1, reportResult2?.headcount)
    }

    @Test
    fun `occupancy calculation is correct for child under 3 years old with full time placement plan and assistance need`() {
        db.transaction {
            it.createPlanOccupancyTestFixture(
                testDaycare.id,
                defaultPeriod,
                LocalDate.of(2017, 1, 1),
                PlacementType.DAYCARE,
                3.0
            )
        }

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 5.25, 1)),
            result
        )

        val reportResult1 = fetchAndParsePlannedOccupancyReportForUnitDay(testAreaId, testDaycare.id, defaultPeriod.start)
        assertEquals(5.25, reportResult1?.sum)
        assertEquals(1, reportResult1?.headcount)
        val reportResult2 = fetchAndParsePlannedOccupancyReportForUnitDay(testAreaId, testDaycare.id, defaultPeriod.end)
        assertEquals(5.25, reportResult2?.sum)
        assertEquals(1, reportResult2?.headcount)
    }

    @Test
    fun `occupancy calculation is correct for child over 3 years old with preschool placement plan and distinct daycare`() {
        db.transaction {
            it.createPreschoolPlanWithDistinctDaycareOccupancyTestFixture(
                testDaycare.id,
                defaultPeriod,
                LocalDate.of(2015, 1, 1),
                defaultPeriodSplit2
            )
        }

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(
                OccupancyPeriod(defaultPeriodSplit1, 0.5, 1),
                OccupancyPeriod(defaultPeriodSplit2, 1.0, 1)
            ),
            result
        )

        val reportResult1 = fetchAndParsePlannedOccupancyReportForUnitDay(testAreaId, testDaycare.id, defaultPeriodSplit1.end)
        assertEquals(0.5, reportResult1?.sum)
        assertEquals(1, reportResult1?.headcount)
        val reportResult2 = fetchAndParsePlannedOccupancyReportForUnitDay(testAreaId, testDaycare.id, defaultPeriodSplit2.start)
        assertEquals(1.0, reportResult2?.sum)
        assertEquals(1, reportResult2?.headcount)
    }

    @Test
    fun `occupancy calculation is correct for child over 3 years old with full time placement plan over an existing part time placement`() {
        val childId = UUID.randomUUID()
        db.transaction { tx ->
            tx.createOccupancyTestFixture(
                childId,
                testDaycare.id,
                defaultPeriod,
                LocalDate.of(2015, 1, 1),
                PlacementType.DAYCARE_PART_TIME
            )
            val applicationId = insertTestApplication(tx.handle, childId = childId, guardianId = testAdult_1.id)
            insertTestPlacementPlan(
                tx.handle,
                applicationId = applicationId,
                unitId = testDaycare.id,
                type = PlacementType.DAYCARE,
                startDate = defaultPeriodSplit2.start,
                endDate = defaultPeriodSplit2.end
            )
        }

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(
                OccupancyPeriod(defaultPeriodSplit1, 0.54, 1),
                OccupancyPeriod(defaultPeriodSplit2, 1.0, 1)
            ),
            result
        )

        val reportResult1 = fetchAndParsePlannedOccupancyReportForUnitDay(testAreaId, testDaycare.id, defaultPeriodSplit1.end)
        assertEquals(0.54, reportResult1?.sum)
        assertEquals(1, reportResult1?.headcount)
        val reportResult2 = fetchAndParsePlannedOccupancyReportForUnitDay(testAreaId, testDaycare.id, defaultPeriodSplit2.start)
        assertEquals(1.0, reportResult2?.sum)
        assertEquals(1, reportResult2?.headcount)
    }

    @Test
    fun `occupancy calculation is correct for child over 3 years old with part time placement plan over an existing full time placement`() {
        val childId = UUID.randomUUID()
        db.transaction { tx ->
            tx.createOccupancyTestFixture(
                childId,
                testDaycare.id,
                defaultPeriod,
                LocalDate.of(2015, 1, 1),
                PlacementType.DAYCARE
            )
            val applicationId = insertTestApplication(tx.handle, childId = childId, guardianId = testAdult_1.id)
            insertTestPlacementPlan(
                tx.handle,
                applicationId = applicationId,
                unitId = testDaycare.id,
                type = PlacementType.DAYCARE_PART_TIME,
                startDate = defaultPeriodSplit2.start,
                endDate = defaultPeriodSplit2.end
            )
        }

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(
                OccupancyPeriod(defaultPeriod, 1.0, 1)
            ),
            result
        )

        val reportResult1 = fetchAndParsePlannedOccupancyReportForUnitDay(testAreaId, testDaycare.id, defaultPeriod.start)
        assertEquals(1.0, reportResult1?.sum)
        assertEquals(1, reportResult1?.headcount)
        val reportResult2 = fetchAndParsePlannedOccupancyReportForUnitDay(testAreaId, testDaycare.id, defaultPeriod.end)
        assertEquals(1.0, reportResult2?.sum)
        assertEquals(1, reportResult2?.headcount)
    }

    @Test
    fun `when child is planned to transfer to another unit the child is counted into both units`() {
        val childId = UUID.randomUUID()
        val daycareId1 = testDaycare.id
        val daycareId2 = UUID.randomUUID()

        db.transaction { tx ->
            tx.handle.insertTestDaycare(DevDaycare(id = daycareId2, areaId = testDaycare.areaId, name = "foo"))

            tx.createPlanOccupancyTestFixture(
                childId,
                daycareId2,
                defaultPeriodSplit2,
                LocalDate.of(2015, 1, 1),
                PlacementType.DAYCARE_PART_TIME
            )

            tx.handle.insertTestPlacement(DevPlacement(type = PlacementType.DAYCARE, childId = childId, unitId = daycareId1, startDate = defaultPeriod.start, endDate = defaultPeriod.end))
        }

        val result1 = fetchAndParseOccupancy(daycareId1, defaultPeriod)
        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 1.0, 1)),
            result1
        )

        val result2 = fetchAndParseOccupancy(daycareId2, defaultPeriod)
        assertEquals(
            listOf(
                OccupancyPeriod(defaultPeriodSplit1, 0.0, 0),
                OccupancyPeriod(defaultPeriodSplit2, 0.54, 1)
            ),
            result2
        )

        val reportResult1 = fetchAndParsePlannedOccupancyReportForUnitDay(testAreaId, daycareId1, defaultPeriodSplit2.start)
        assertEquals(1.0, reportResult1?.sum)
        assertEquals(1, reportResult1?.headcount)

        val reportResult2 = fetchAndParsePlannedOccupancyReportForUnitDay(testAreaId, daycareId2, defaultPeriodSplit2.start)
        assertEquals(0.54, reportResult2?.sum)
        assertEquals(1, reportResult2?.headcount)
    }

    @Test
    fun `occupancy calculation picks latest placement plan when two placement plans are valid during same period`() {
        val childId = UUID.randomUUID()
        db.transaction { tx ->
            tx.createPlanOccupancyTestFixture(
                childId,
                testDaycare.id,
                defaultPeriod,
                LocalDate.of(2015, 1, 1),
                PlacementType.DAYCARE
            )
            val applicationId = insertTestApplication(tx.handle, childId = childId, guardianId = testAdult_1.id)
            insertTestPlacementPlan(
                tx.handle,
                applicationId = applicationId,
                unitId = testDaycare.id,
                type = PlacementType.DAYCARE_PART_TIME,
                startDate = defaultPeriodSplit2.start,
                endDate = defaultPeriodSplit2.end,
                updated = Instant.now().plusMillis(10000)
            )
        }

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 1.0, 1)),
            result
        )

        val reportResult1 = fetchAndParsePlannedOccupancyReportForUnitDay(testAreaId, testDaycare.id, defaultPeriod.start)
        assertEquals(1.0, reportResult1?.sum)
        assertEquals(1, reportResult1?.headcount)
        val reportResult2 = fetchAndParsePlannedOccupancyReportForUnitDay(testAreaId, testDaycare.id, defaultPeriod.end)
        assertEquals(1.0, reportResult2?.sum)
        assertEquals(1, reportResult2?.headcount)
    }

    @Test
    fun `occupancy calculation is correct when the placement has been rejected`() {
        val childId = UUID.randomUUID()
        db.transaction { tx ->
            tx.createPlanOccupancyTestFixture(
                childId,
                testDaycare.id,
                defaultPeriod,
                LocalDate.of(2015, 1, 1),
                PlacementType.DAYCARE,
                0.0,
                defaultPeriod,
                true
            )
        }

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 0.0, 0)),
            result
        )

        val reportResult1 = fetchAndParsePlannedOccupancyReportForUnitDay(testAreaId, testDaycare.id, defaultPeriod.start)
        assertEquals(0.0, reportResult1?.sum)
        assertEquals(0, reportResult1?.headcount)
        val reportResult2 = fetchAndParsePlannedOccupancyReportForUnitDay(testAreaId, testDaycare.id, defaultPeriod.end)
        assertEquals(0.0, reportResult2?.sum)
        assertEquals(0, reportResult2?.headcount)
    }

    private val testUser = AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.SERVICE_WORKER))

    private fun fetchAndParseOccupancy(unitId: UUID, period: FiniteDateRange): List<OccupancyPeriod> {
        val (_, response, result) = http.get("/occupancy/by-unit/$unitId?from=${period.start}&to=${period.end}&type=PLANNED")
            .asUser(testUser)
            .responseString()
        assertEquals(200, response.statusCode)

        return objectMapper.readValue<OccupancyResponse>(result.get()).occupancies
    }

    private fun fetchAndParsePlannedOccupancyReportForUnitDay(careAreaId: UUID, unitId: UUID, date: LocalDate): OccupancyReportRowGroupedValues? {
        val (_, response, result) = http.get(
            "/reports/occupancy-by-unit",
            listOf("type" to "PLANNED", "careAreaId" to careAreaId, "year" to date.year, "month" to date.month.value)
        )
            .asUser(testUser)
            .responseString()
        assertEquals(200, response.statusCode)

        return objectMapper.readValue<List<OccupancyUnitReportResultRow>>(result.get())
            .first { it.unitId == unitId }
            .occupancies[date]
    }
}
