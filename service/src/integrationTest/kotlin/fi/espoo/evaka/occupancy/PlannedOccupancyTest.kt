// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.occupancy

import com.fasterxml.jackson.module.kotlin.readValue
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.config.Roles
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestPlacementPlan
import fi.espoo.evaka.shared.domain.ClosedPeriod
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
        jdbi.handle(::insertGeneralTestFixtures)
    }

    @AfterEach
    fun afterEach() {
        jdbi.handle(::resetDatabase)
    }

    private val defaultPeriod = ClosedPeriod(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))

    @Test
    fun `planned occupancy calculation does not break when there are no children placed into a unit`() {
        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 0.0, 0)),
            result
        )
    }

    @Test
    fun `occupancy calculation is correct for child over 3 years old with full time placement plan`() {
        jdbi.handle(
            createPlanOccupancyTestFixture(
                testDaycare.id,
                defaultPeriod,
                LocalDate.of(2015, 1, 1),
                PlacementType.DAYCARE
            )
        )

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 1.0, 1)),
            result
        )
    }

    @Test
    fun `occupancy calculation is correct for child under 3 years old with full time placement plan`() {
        jdbi.handle(
            createPlanOccupancyTestFixture(
                testDaycare.id,
                defaultPeriod,
                LocalDate.of(2017, 1, 1),
                PlacementType.DAYCARE
            )
        )

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 1.75, 1)),
            result
        )
    }

    @Test
    fun `occupancy calculation is correct for child over 3 years old with part time placement plan`() {
        jdbi.handle(
            createPlanOccupancyTestFixture(
                testDaycare.id,
                defaultPeriod,
                LocalDate.of(2015, 1, 1),
                PlacementType.DAYCARE_PART_TIME
            )
        )

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 0.54, 1)),
            result
        )
    }

    @Test
    fun `occupancy calculation is correct for child over 3 years old with only preschool placement plan`() {
        jdbi.handle(
            createPlanOccupancyTestFixture(
                testDaycare.id,
                defaultPeriod,
                LocalDate.of(2015, 1, 1),
                PlacementType.PRESCHOOL
            )
        )

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 0.5, 1)),
            result
        )
    }

    @Test
    fun `occupancy calculation is correct for child under 3 years old with part time placement plan`() {
        jdbi.handle(
            createPlanOccupancyTestFixture(
                testDaycare.id,
                defaultPeriod,
                LocalDate.of(2017, 1, 1),
                PlacementType.DAYCARE_PART_TIME
            )
        )

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 1.75, 1)),
            result
        )
    }

    @Test
    fun `occupancy calculation is correct for child over 3 years old with full time placement plan and assistance need`() {
        jdbi.handle(
            createPlanOccupancyTestFixture(
                testDaycare.id,
                defaultPeriod,
                LocalDate.of(2015, 1, 1),
                PlacementType.DAYCARE,
                3.0
            )
        )

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 3.0, 1)),
            result
        )
    }

    @Test
    fun `occupancy calculation is correct for child under 3 years old with full time placement plan and assistance need`() {
        jdbi.handle(
            createPlanOccupancyTestFixture(
                testDaycare.id,
                defaultPeriod,
                LocalDate.of(2017, 1, 1),
                PlacementType.DAYCARE,
                3.0
            )
        )

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 5.25, 1)),
            result
        )
    }

    @Test
    fun `occupancy calculation is correct for child over 3 years old with preschool placement plan and distinct daycare`() {
        jdbi.handle(
            createPreschoolPlanWithDistinctDaycareOccupancyTestFixture(
                testDaycare.id,
                defaultPeriod,
                LocalDate.of(2015, 1, 1),
                defaultPeriod.copy(start = defaultPeriod.start.plusMonths(1))
            )
        )

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(
                OccupancyPeriod(defaultPeriod.copy(end = defaultPeriod.start.plusMonths(1).minusDays(1)), 0.5, 1),
                OccupancyPeriod(defaultPeriod.copy(start = defaultPeriod.start.plusMonths(1)), 1.0, 1)
            ),
            result
        )
    }

    @Test
    fun `occupancy calculation is correct for child over 3 years old with full time placement plan over an existing part time placement`() {
        val childId = UUID.randomUUID()
        jdbi.handle { h ->
            createOccupancyTestFixture(
                childId,
                testDaycare.id,
                defaultPeriod,
                LocalDate.of(2015, 1, 1),
                PlacementType.DAYCARE_PART_TIME
            )(h)
            val applicationId = insertTestApplication(h, childId = childId)
            insertTestPlacementPlan(
                h,
                applicationId = applicationId,
                unitId = testDaycare.id,
                type = PlacementType.DAYCARE,
                startDate = defaultPeriod.start.plusMonths(6),
                endDate = defaultPeriod.end
            )
        }

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(
                OccupancyPeriod(defaultPeriod.copy(end = defaultPeriod.start.plusMonths(6).minusDays(1)), 0.54, 1),
                OccupancyPeriod(defaultPeriod.copy(start = defaultPeriod.start.plusMonths(6)), 1.0, 1)
            ),
            result
        )
    }

    @Test
    fun `occupancy calculation is correct for child over 3 years old with part time placement plan over an existing full time placement`() {
        val childId = UUID.randomUUID()
        jdbi.handle { h ->
            createOccupancyTestFixture(
                childId,
                testDaycare.id,
                defaultPeriod,
                LocalDate.of(2015, 1, 1),
                PlacementType.DAYCARE
            )(h)
            val applicationId = insertTestApplication(h, childId = childId)
            insertTestPlacementPlan(
                h,
                applicationId = applicationId,
                unitId = testDaycare.id,
                type = PlacementType.DAYCARE_PART_TIME,
                startDate = defaultPeriod.start.plusMonths(6),
                endDate = defaultPeriod.end
            )
        }

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(
                OccupancyPeriod(defaultPeriod.copy(end = defaultPeriod.start.plusMonths(6).minusDays(1)), 1.0, 1),
                OccupancyPeriod(defaultPeriod.copy(start = defaultPeriod.start.plusMonths(6)), 0.54, 1)
            ),
            result
        )
    }

    @Test
    fun `occupancy calculation picks latest placement plant when two placement plans are valid during same period`() {
        val childId = UUID.randomUUID()
        jdbi.handle { h ->
            createPlanOccupancyTestFixture(
                childId,
                testDaycare.id,
                defaultPeriod,
                LocalDate.of(2015, 1, 1),
                PlacementType.DAYCARE
            )(h)
            val applicationId = insertTestApplication(h, childId = childId)
            insertTestPlacementPlan(
                h,
                applicationId = applicationId,
                unitId = testDaycare.id,
                type = PlacementType.DAYCARE_PART_TIME,
                startDate = defaultPeriod.start.plusMonths(1),
                endDate = defaultPeriod.end,
                updated = Instant.now().plusMillis(10000)
            )
        }

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 1.0, 1)),
            result
        )
    }

    @Test
    fun `occupancy calculation is correct when the placement has been rejected`() {
        val childId = UUID.randomUUID()
        jdbi.handle { h ->
            createPlanOccupancyTestFixture(
                childId,
                testDaycare.id,
                defaultPeriod,
                LocalDate.of(2015, 1, 1),
                PlacementType.DAYCARE,
                0.0,
                defaultPeriod,
                true

            )(h)
        }

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 0.0, 0)),
            result
        )
    }

    private val testUser = AuthenticatedUser(testDecisionMaker_1.id, setOf(Roles.SERVICE_WORKER))

    private fun fetchAndParseOccupancy(unitId: UUID, period: ClosedPeriod): List<OccupancyPeriod> {
        val (_, response, result) = http.get("/occupancy/by-unit/$unitId?from=${period.start}&to=${period.end}&type=PLANNED")
            .asUser(testUser)
            .responseString()
        assertEquals(200, response.statusCode)

        return objectMapper.readValue<OccupancyResponse>(result.get()).occupancies
    }
}
