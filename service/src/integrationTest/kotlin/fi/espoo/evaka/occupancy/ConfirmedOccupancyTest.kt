// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.occupancy

import com.fasterxml.jackson.module.kotlin.readValue
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.daycare.service.CareType
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestAbsence
import fi.espoo.evaka.shared.dev.insertTestBackUpCare
import fi.espoo.evaka.shared.dev.insertTestCaretakers
import fi.espoo.evaka.shared.dev.insertTestDaycare
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestDaycareGroupPlacement
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.testAreaId
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.testDecisionMaker_1
import org.jdbi.v3.core.Handle
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class ConfirmedOccupancyTest : FullApplicationTest() {
    @BeforeEach
    fun beforeEach() {
        jdbi.handle(::insertGeneralTestFixtures)
    }

    @AfterEach
    fun afterEach() {
        jdbi.handle(::resetDatabase)
    }

    private val defaultPeriod = FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31))

    @Test
    fun `confirmed occupancy calculation does not break when there are no children placed into a unit`() {
        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 0.0, 0)),
            result
        )
    }

    @Test
    fun `occupancy calculation is correct for child over 3 years old in full time daycare without service need`() {
        jdbi.handle(createOccupancyTestFixture(testDaycare.id, defaultPeriod, LocalDate.of(2015, 1, 1), PlacementType.DAYCARE))

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 1.0, 1)),
            result
        )
    }

    @Test
    fun `occupancy group level calculation is correct for child over 3 years old in full time daycare without service need`() {
        val childId = UUID.randomUUID()
        val placementId = UUID.randomUUID()
        jdbi.handle(createOccupancyTestFixture(childId, testDaycare.id, defaultPeriod, LocalDate.of(2015, 1, 1), PlacementType.DAYCARE, placementId = placementId))

        val groupId1 = jdbi.handle { it.insertTestDaycareGroup(DevDaycareGroup(daycareId = testDaycare.id, startDate = LocalDate.of(2000, 1, 1))) }
        jdbi.handle { h -> insertTestCaretakers(h, groupId1, amount = 2.0, startDate = LocalDate.of(2000, 1, 1)) }
        jdbi.handle { h -> insertTestDaycareGroupPlacement(h, placementId, groupId1, startDate = defaultPeriod.start, endDate = defaultPeriod.end) }

        val groupId2 = jdbi.handle { it.insertTestDaycareGroup(DevDaycareGroup(daycareId = testDaycare.id, startDate = LocalDate.of(2000, 1, 1))) }
        jdbi.handle { h -> insertTestCaretakers(h, groupId2, amount = 1.0, startDate = LocalDate.of(2000, 1, 1)) }

        val result = fetchAndParseOccupancyByGroups(testDaycare.id, defaultPeriod)

        val expectedPeriodGroup1 = OccupancyPeriod(defaultPeriod, sum = 1.0, headcount = 1, caretakers = 2.0, percentage = 7.1)
        val expectedPeriodGroup2 = OccupancyPeriod(defaultPeriod, sum = 0.0, headcount = 0, caretakers = 1.0, percentage = 0.0)
        assertEquals(
            setOf(
                OccupancyResponseGroupLevel(
                    groupId1,
                    occupancies = OccupancyResponse(
                        occupancies = listOf(expectedPeriodGroup1),
                        min = expectedPeriodGroup1,
                        max = expectedPeriodGroup1
                    )
                ),
                OccupancyResponseGroupLevel(
                    groupId2,
                    occupancies = OccupancyResponse(
                        occupancies = listOf(expectedPeriodGroup2),
                        min = expectedPeriodGroup2,
                        max = expectedPeriodGroup2
                    )
                )
            ),
            result.toSet()
        )
    }

    @Test
    fun `occupancy calculation is correct for child under 3 years old in full time daycare without service need`() {
        jdbi.handle(createOccupancyTestFixture(testDaycare.id, defaultPeriod, LocalDate.of(2017, 1, 1), PlacementType.DAYCARE))

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 1.75, 1)),
            result
        )
    }

    @Test
    fun `occupancy calculation is correct for child over 3 years old in daycare with full time service need`() {
        jdbi.handle(
            createOccupancyTestFixture(
                unitId = testDaycare.id,
                period = defaultPeriod,
                dateOfBirth = LocalDate.of(2015, 1, 1),
                placementType = PlacementType.DAYCARE,
                hours = 30.0
            )
        )

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 1.0, 1)),
            result
        )
    }

    @Test
    fun `occupancy calculation is correct for child over 3 years old in daycare with part time hours but no part time placement`() {
        jdbi.handle(
            createOccupancyTestFixture(
                unitId = testDaycare.id,
                period = defaultPeriod,
                dateOfBirth = LocalDate.of(2015, 1, 1),
                placementType = PlacementType.DAYCARE,
                hours = 25.0
            )
        )

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 1.0, 1)),
            result
        )
    }

    @Test
    fun `occupancy calculation is correct for child over 3 years old in daycare with part time placement`() {
        jdbi.handle(
            createOccupancyTestFixture(
                unitId = testDaycare.id,
                period = defaultPeriod,
                dateOfBirth = LocalDate.of(2015, 1, 1),
                placementType = PlacementType.DAYCARE_PART_TIME,
                hours = 25.0
            )
        )

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 0.54, 1)),
            result
        )
    }

    @Test
    fun `occupancy calculation is correct for child over 3 years old in preschool daycare with no daycare hours`() {
        jdbi.handle(
            createOccupancyTestFixture(
                unitId = testDaycare.id,
                period = defaultPeriod,
                dateOfBirth = LocalDate.of(2015, 1, 1),
                placementType = PlacementType.PRESCHOOL_DAYCARE,
                hours = 20.0
            )
        )

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 0.5, 1)),
            result
        )
    }

    @Test
    fun `occupancy calculation is correct for child over 3 years old in preschool daycare with 20 daycare hours`() {
        jdbi.handle(
            createOccupancyTestFixture(
                unitId = testDaycare.id,
                period = defaultPeriod,
                dateOfBirth = LocalDate.of(2015, 1, 1),
                placementType = PlacementType.PRESCHOOL_DAYCARE,
                hours = 40.0
            )
        )

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 1.0, 1)),
            result
        )
    }

    @Test
    fun `absences do not affect confirmed occupancy calculation`() {
        val childId = UUID.randomUUID()
        jdbi.handle(
            createOccupancyTestFixture(
                childId = childId,
                unitId = testDaycare.id,
                period = defaultPeriod,
                dateOfBirth = LocalDate.of(2015, 1, 1),
                placementType = PlacementType.PRESCHOOL_DAYCARE,
                hours = 40.0
            )
        )

        jdbi.handle { h -> insertTestAbsence(h, childId = childId, date = defaultPeriod.start, careType = CareType.PRESCHOOL, absenceType = AbsenceType.SICKLEAVE) }
        jdbi.handle { h -> insertTestAbsence(h, childId = childId, date = defaultPeriod.start, careType = CareType.PRESCHOOL_DAYCARE, absenceType = AbsenceType.SICKLEAVE) }

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 1.0, 1)),
            result
        )
    }

    @Test
    fun `backup care does not affect confirmed occupancy calculation`() {
        val childId = UUID.randomUUID()
        jdbi.handle(
            createOccupancyTestFixture(
                childId = childId,
                unitId = testDaycare.id,
                period = defaultPeriod,
                dateOfBirth = LocalDate.of(2015, 1, 1),
                placementType = PlacementType.PRESCHOOL_DAYCARE,
                hours = 40.0
            )
        )

        jdbi.handle { h -> insertTestBackUpCare(h, childId, testDaycare2.id, defaultPeriod.start.plusDays(2), defaultPeriod.start.plusDays(5)) }

        val resultOriginalUnit = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)
        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 1.0, 1)),
            resultOriginalUnit
        )

        val resultBackupUnit = fetchAndParseOccupancy(testDaycare2.id, defaultPeriod)
        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 0.0, 0)),
            resultBackupUnit
        )
    }

    @Test
    fun `occupancy calculation is correct for child over 3 years old in preschool daycare with 25 daycare hours`() {
        jdbi.handle(
            createOccupancyTestFixture(
                unitId = testDaycare.id,
                period = defaultPeriod,
                dateOfBirth = LocalDate.of(2015, 1, 1),
                placementType = PlacementType.PRESCHOOL_DAYCARE,
                hours = 45.0
            )
        )

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 1.0, 1)),
            result
        )
    }

    @Test
    fun `occupancy calculation is correct for child over 3 years old in preparatory daycare without daycare hours`() {
        jdbi.handle(
            createOccupancyTestFixture(
                unitId = testDaycare.id,
                period = defaultPeriod,
                dateOfBirth = LocalDate.of(2015, 1, 1),
                placementType = PlacementType.PREPARATORY_DAYCARE,
                hours = 25.0,
                assistanceExtra = null
            )
        )

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 0.5, 1)),
            result
        )
    }

    @Test
    fun `occupancy calculation is correct for child over 3 years old in preparatory daycare with 20 daycare hours`() {
        jdbi.handle(
            createOccupancyTestFixture(
                unitId = testDaycare.id,
                period = defaultPeriod,
                dateOfBirth = LocalDate.of(2015, 1, 1),
                placementType = PlacementType.PREPARATORY_DAYCARE,
                hours = 45.0,
                assistanceExtra = null
            )
        )

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 1.0, 1)),
            result
        )
    }

    @Test
    fun `occupancy calculation is correct for child over 3 years old in preschool daycare with preparatory and 25 daycare hours`() {
        jdbi.handle(
            createOccupancyTestFixture(
                unitId = testDaycare.id,
                period = defaultPeriod,
                dateOfBirth = LocalDate.of(2015, 1, 1),
                placementType = PlacementType.PREPARATORY_DAYCARE,
                hours = 50.0,
                assistanceExtra = null
            )
        )

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 1.0, 1)),
            result
        )
    }

    @Test
    fun `occupancy calculation is correct for 5-year-old child in daycare with part time hours`() {
        jdbi.handle(
            createOccupancyTestFixture(
                unitId = testDaycare.id,
                period = defaultPeriod,
                dateOfBirth = LocalDate.of(2014, 1, 1),
                placementType = PlacementType.DAYCARE,
                hours = 20.0
            )
        )

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(
                OccupancyPeriod(defaultPeriod.copy(end = LocalDate.of(2019, 7, 31)), 1.0, 1),
                OccupancyPeriod(defaultPeriod.copy(start = LocalDate.of(2019, 8, 1)), 0.5, 1)
            ),
            result
        )
    }

    @Test
    fun `occupancy calculation is correct for a child in full time 5-year-old daycare`() {
        jdbi.handle(
            createOccupancyTestFixture(
                unitId = testDaycare.id,
                period = defaultPeriod,
                dateOfBirth = LocalDate.of(2014, 1, 1),
                placementType = PlacementType.DAYCARE_FIVE_YEAR_OLDS,
                hours = 25.0
            )
        )

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(
                OccupancyPeriod(defaultPeriod, 1.0, 1)
            ),
            result
        )
    }

    @Test
    fun `occupancy calculation is correct for a child in part time 5-year-old daycare`() {
        jdbi.handle(
            createOccupancyTestFixture(
                unitId = testDaycare.id,
                period = defaultPeriod,
                dateOfBirth = LocalDate.of(2014, 1, 1),
                placementType = PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS,
                hours = 20.0
            )
        )

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(
                OccupancyPeriod(defaultPeriod, 0.5, 1)
            ),
            result
        )
    }

    @Test
    fun `occupancy calculation is correct for child under 3 years old in daycare with part time placement`() {
        jdbi.handle(
            createOccupancyTestFixture(
                unitId = testDaycare.id,
                period = defaultPeriod,
                dateOfBirth = LocalDate.of(2017, 1, 1),
                placementType = PlacementType.DAYCARE_PART_TIME,
                hours = 25.0
            )
        )

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 1.75, 1)),
            result
        )
    }

    @Test
    fun `occupancy calculation is correct for child over 3 years old in full time daycare with assistance extra`() {
        jdbi.handle(
            createOccupancyTestFixture(
                unitId = testDaycare.id,
                period = defaultPeriod,
                dateOfBirth = LocalDate.of(2015, 1, 1),
                placementType = PlacementType.DAYCARE,
                hours = null,
                assistanceExtra = 5.0
            )
        )

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 5.0, 1)),
            result
        )
    }

    @Test
    fun `occupancy calculation is correct for child under 3 years old in full time daycare with assistance extra`() {
        jdbi.handle(
            createOccupancyTestFixture(
                unitId = testDaycare.id,
                period = defaultPeriod,
                dateOfBirth = LocalDate.of(2017, 1, 1),
                placementType = PlacementType.DAYCARE,
                hours = null,
                assistanceExtra = 5.0
            )
        )

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 8.75, 1)),
            result
        )
    }

    @Test
    fun `occupancy calculation splits partially overlapping coefficients`() {
        val periods = listOf(
            FiniteDateRange(defaultPeriod.start, LocalDate.of(2019, 6, 30)),
            FiniteDateRange(LocalDate.of(2019, 2, 1), LocalDate.of(2019, 2, 28)),
            FiniteDateRange(LocalDate.of(2019, 2, 15), LocalDate.of(2019, 5, 31)),
            FiniteDateRange(LocalDate.of(2019, 5, 31), LocalDate.of(2019, 9, 1)),
            FiniteDateRange(LocalDate.of(2019, 9, 1), defaultPeriod.end)
        )

        periods
            .map { period ->
                createOccupancyTestFixture(testDaycare.id, period, LocalDate.of(2015, 1, 1), PlacementType.DAYCARE, 40.0)
            }
            .let(::insertFixtures)
            .let(jdbi::handle)

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(
                OccupancyPeriod(FiniteDateRange(defaultPeriod.start, LocalDate.of(2019, 1, 31)), 1.0, 1),
                OccupancyPeriod(FiniteDateRange(LocalDate.of(2019, 2, 1), LocalDate.of(2019, 2, 14)), 2.0, 2),
                OccupancyPeriod(FiniteDateRange(LocalDate.of(2019, 2, 15), LocalDate.of(2019, 2, 28)), 3.0, 3),
                OccupancyPeriod(FiniteDateRange(LocalDate.of(2019, 3, 1), LocalDate.of(2019, 5, 30)), 2.0, 2),
                OccupancyPeriod(FiniteDateRange(LocalDate.of(2019, 5, 31), LocalDate.of(2019, 5, 31)), 3.0, 3),
                OccupancyPeriod(FiniteDateRange(LocalDate.of(2019, 6, 1), LocalDate.of(2019, 6, 30)), 2.0, 2),
                OccupancyPeriod(FiniteDateRange(LocalDate.of(2019, 7, 1), LocalDate.of(2019, 8, 31)), 1.0, 1),
                OccupancyPeriod(FiniteDateRange(LocalDate.of(2019, 9, 1), LocalDate.of(2019, 9, 1)), 2.0, 2),
                OccupancyPeriod(FiniteDateRange(LocalDate.of(2019, 9, 2), defaultPeriod.end), 1.0, 1)
            ),
            result
        )
    }

    @Test
    fun `confirmed occupancy calculation does not include placement plans`() {
        jdbi.handle(
            createPlanOccupancyTestFixture(
                unitId = testDaycare.id,
                period = defaultPeriod,
                dateOfBirth = LocalDate.of(2015, 1, 1),
                placementType = PlacementType.DAYCARE
            )
        )

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 0.0, 0)),
            result
        )
    }

    @Test
    fun `occupancy calculation includes caretakers if they exist and calculates a percentage`() {
        jdbi.handle(createOccupancyTestFixture(testDaycare.id, defaultPeriod, LocalDate.of(2015, 1, 1), PlacementType.DAYCARE))
        jdbi.handle { h ->
            val groupId = h.insertTestDaycareGroup(DevDaycareGroup(daycareId = testDaycare.id, startDate = defaultPeriod.start))
            insertTestCaretakers(h, groupId, amount = 1.0)
        }

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 1.0, 1, 1.0, 14.3)),
            result
        )
    }

    @Test
    fun `occupancy calculation works with multiple groups and caretakers`() {
        jdbi.handle(createOccupancyTestFixture(testDaycare.id, defaultPeriod, LocalDate.of(2015, 1, 1), PlacementType.DAYCARE))
        jdbi.handle { h ->
            val groupId_1 = h.insertTestDaycareGroup(DevDaycareGroup(daycareId = testDaycare.id, startDate = defaultPeriod.start))
            insertTestCaretakers(h, groupId_1, amount = 1.0)
            val groupId_2 = h.insertTestDaycareGroup(DevDaycareGroup(daycareId = testDaycare.id, startDate = defaultPeriod.start))
            insertTestCaretakers(h, groupId_2, amount = 1.0)
        }

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 1.0, 1, 2.0, 7.1)),
            result
        )
    }

    @Test
    fun `occupancy calculation does not break if caretaker amount is 0`() {
        jdbi.handle(createOccupancyTestFixture(testDaycare.id, defaultPeriod, LocalDate.of(2015, 1, 1), PlacementType.DAYCARE))
        jdbi.handle { h ->
            val groupId = h.insertTestDaycareGroup(DevDaycareGroup(daycareId = testDaycare.id, startDate = defaultPeriod.start))
            insertTestCaretakers(h, groupId, amount = 0.0)
        }

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 1.0, 1, null, null)),
            result
        )
    }

    @Test
    fun `occupancy calculation does not break if caretaker amount of one group is 0`() {
        jdbi.handle(createOccupancyTestFixture(testDaycare.id, defaultPeriod, LocalDate.of(2015, 1, 1), PlacementType.DAYCARE))
        jdbi.handle { h ->
            val groupId_1 = h.insertTestDaycareGroup(DevDaycareGroup(daycareId = testDaycare.id, startDate = defaultPeriod.start))
            insertTestCaretakers(h, groupId_1, amount = 1.0)
            val groupId_2 = h.insertTestDaycareGroup(DevDaycareGroup(daycareId = testDaycare.id, startDate = defaultPeriod.start))
            insertTestCaretakers(h, groupId_2, amount = 0.0)
        }

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 1.0, 1, 1.0, 14.3)),
            result
        )
    }

    @Test
    fun `occupancy calculation uses same coefficient for all placements into FAMILY or GROUP_FAMILY units`() {
        val unit = jdbi.handle { h ->
            h.insertTestDaycare(DevDaycare(areaId = testAreaId, name = "Ryhmis", type = setOf(fi.espoo.evaka.daycare.CareType.GROUP_FAMILY)))
        }
        jdbi.handle(createOccupancyTestFixture(unit, defaultPeriod, LocalDate.of(2015, 1, 1), PlacementType.DAYCARE))
        jdbi.handle(createOccupancyTestFixture(unit, defaultPeriod, LocalDate.of(2017, 1, 1), PlacementType.DAYCARE))
        jdbi.handle(createOccupancyTestFixture(unit, defaultPeriod, LocalDate.of(2013, 1, 1), PlacementType.PRESCHOOL))
        jdbi.handle(createOccupancyTestFixture(unit, defaultPeriod, LocalDate.of(2013, 1, 1), PlacementType.PRESCHOOL_DAYCARE))
        jdbi.handle(createOccupancyTestFixture(unit, defaultPeriod, LocalDate.of(2014, 1, 1), PlacementType.PRESCHOOL))

        val result = fetchAndParseOccupancy(unit, defaultPeriod)

        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 8.75, 5)),
            result
        )
    }

    private val testUser = AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.SERVICE_WORKER))

    private fun fetchAndParseOccupancy(unitId: UUID, period: FiniteDateRange): List<OccupancyPeriod> {
        val (_, response, result) = http
            .get("/occupancy/by-unit/$unitId?from=${period.start}&to=${period.end}&type=CONFIRMED")
            .asUser(testUser)
            .responseString()
        assertEquals(200, response.statusCode)

        return objectMapper.readValue<OccupancyResponse>(result.get()).occupancies
    }

    private fun fetchAndParseOccupancyByGroups(unitId: UUID, period: FiniteDateRange): List<OccupancyResponseGroupLevel> {
        val (_, response, result) = http
            .get("/occupancy/by-unit/$unitId/groups?from=${period.start}&to=${period.end}&type=CONFIRMED")
            .asUser(testUser)
            .responseString()
        assertEquals(200, response.statusCode)

        return objectMapper.readValue(result.get())
    }

    private fun insertFixtures(fixtures: List<(Handle) -> Unit>) = { h: Handle ->
        fixtures.forEach { it(h) }
    }
}
