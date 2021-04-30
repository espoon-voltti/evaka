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
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestAbsence
import fi.espoo.evaka.shared.dev.insertTestBackUpCare
import fi.espoo.evaka.shared.dev.insertTestCaretakers
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.insertTestStaffAttendance
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.testDecisionMaker_1
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class RealizedOccupancyTest : FullApplicationTest() {
    val groupId = UUID.randomUUID()

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
            tx.handle.insertTestDaycareGroup(DevDaycareGroup(daycareId = testDaycare.id, id = groupId))
            insertTestCaretakers(tx.handle, groupId = groupId, amount = 3.0, startDate = LocalDate.of(2019, 1, 1))
        }
    }

    @AfterEach
    fun afterEach() {
        db.transaction { tx ->
            tx.resetDatabase()
        }
    }

    private val day1 = LocalDate.of(2019, 1, 1)
    private val day2 = LocalDate.of(2019, 1, 2)

    private val defaultPeriod = FiniteDateRange(day1, day2)

    @Test
    fun `realized occupancy calculation does not break when there are no children placed into a unit`() {
        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 0.0, 0)),
            result
        )
    }

    @Test
    fun `occupancy for a child present is counted normally`() {
        db.transaction {
            it.createOccupancyTestFixture(testDaycare.id, defaultPeriod, LocalDate.of(2017, 1, 1), PlacementType.DAYCARE)
        }

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 1.75, 1)),
            result
        )
    }

    @Test
    fun `group level occupancy for a child present is counted normally`() {
        val childId = UUID.randomUUID()
        val placementId = UUID.randomUUID()
        db.transaction { tx ->
            tx.createOccupancyTestFixture(childId, testDaycare.id, defaultPeriod, LocalDate.of(2017, 1, 1), PlacementType.DAYCARE, placementId = placementId)
            insertTestStaffAttendance(tx.handle, groupId = groupId, count = 1.0, date = day1)
            insertTestStaffAttendance(tx.handle, groupId = groupId, count = 2.0, date = day2)
            insertTestDaycareGroupPlacement(tx.handle, placementId, groupId, startDate = defaultPeriod.start, endDate = defaultPeriod.end)
        }

        val result = fetchAndParseOccupancyByGroups(testDaycare.id, defaultPeriod)

        val expectedPeriod1 = OccupancyPeriod(FiniteDateRange(day1, day1), sum = 1.75, headcount = 1, caretakers = 1.0, percentage = 25.0)
        val expectedPeriod2 = OccupancyPeriod(FiniteDateRange(day2, day2), sum = 1.75, headcount = 1, caretakers = 2.0, percentage = 12.5)
        assertEquals(
            listOf(
                OccupancyResponseGroupLevel(
                    groupId,
                    occupancies = OccupancyResponse(
                        occupancies = listOf(expectedPeriod1, expectedPeriod2),
                        min = expectedPeriod2,
                        max = expectedPeriod1
                    )
                )
            ),
            result
        )
    }

    @Test
    fun `realized occupancy during backupcare affects the backup daycare unit`() {
        val backupCareChild = UUID.randomUUID()
        val normalPlacementChild1 = UUID.randomUUID()
        val normalPlacementChild2 = UUID.randomUUID()
        db.transaction { tx ->
            tx.createOccupancyTestFixture(backupCareChild, testDaycare.id, defaultPeriod, LocalDate.of(2017, 1, 1), PlacementType.DAYCARE)
            insertTestBackUpCare(tx.handle, backupCareChild, testDaycare2.id, day2, day2)

            tx.createOccupancyTestFixture(normalPlacementChild1, testDaycare2.id, defaultPeriod, LocalDate.of(2017, 1, 1), PlacementType.DAYCARE)
            tx.createOccupancyTestFixture(normalPlacementChild2, testDaycare2.id, defaultPeriod, LocalDate.of(2017, 1, 1), PlacementType.DAYCARE)
        }

        val resultOriginalUnit = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)
        assertEquals(
            listOf(
                OccupancyPeriod(FiniteDateRange(day1, day1), 1.75, 1),
                OccupancyPeriod(FiniteDateRange(day2, day2), 0.0, 0)
            ),
            resultOriginalUnit
        )

        val resultBackupUnit = fetchAndParseOccupancy(testDaycare2.id, defaultPeriod)
        assertEquals(
            listOf(
                OccupancyPeriod(FiniteDateRange(day1, day1), 2 * 1.75, 2),
                OccupancyPeriod(FiniteDateRange(day2, day2), 3 * 1.75, 3)
            ),
            resultBackupUnit
        )
    }

    @Test
    fun `realized occupancy during backupcare with absence from backup care`() {
        val childId = UUID.randomUUID()
        db.transaction { tx ->
            tx.createOccupancyTestFixture(childId, testDaycare.id, defaultPeriod, LocalDate.of(2017, 1, 1), PlacementType.DAYCARE)
            insertTestBackUpCare(tx.handle, childId, testDaycare2.id, day1, day2)
            insertTestAbsence(tx.handle, childId = childId, date = day2, careType = CareType.DAYCARE, absenceType = AbsenceType.SICKLEAVE)
        }

        val resultBackupUnit = fetchAndParseOccupancy(testDaycare2.id, defaultPeriod)
        assertEquals(
            listOf(
                OccupancyPeriod(FiniteDateRange(day1, day1), 1.75, 1),
                OccupancyPeriod(FiniteDateRange(day2, day2), 0.0, 0)
            ),
            resultBackupUnit
        )
    }

    @Test
    fun `occupancy for a child absent is not counted`() {
        val childId = UUID.randomUUID()
        db.transaction { tx ->
            tx.createOccupancyTestFixture(childId, testDaycare.id, defaultPeriod, LocalDate.of(2017, 1, 1), PlacementType.DAYCARE)
            insertTestAbsence(tx.handle, childId = childId, date = LocalDate.of(2019, 1, 2), careType = CareType.DAYCARE)
        }

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(
                OccupancyPeriod(FiniteDateRange(day1, day1), 1.75, 1),
                OccupancyPeriod(FiniteDateRange(day2, day2), 0.0, 0)
            ),
            result
        )
    }

    @Test
    fun `occupancy for a child with absence of type PRESENCE is counted normally`() {
        val childId = UUID.randomUUID()
        db.transaction { tx ->
            tx.createOccupancyTestFixture(childId, testDaycare.id, defaultPeriod, LocalDate.of(2017, 1, 1), PlacementType.DAYCARE)
            insertTestAbsence(tx.handle, childId = childId, date = LocalDate.of(2019, 1, 2), careType = CareType.DAYCARE, absenceType = AbsenceType.PRESENCE)
        }

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(OccupancyPeriod(defaultPeriod, 1.75, 1)),
            result
        )
    }

    @Test
    fun `caretaker count is based on attendance`() {
        db.transaction { tx ->
            insertTestStaffAttendance(tx.handle, groupId = groupId, date = LocalDate.of(2019, 1, 1), count = 2.0)
            insertTestStaffAttendance(tx.handle, groupId = groupId, date = LocalDate.of(2019, 1, 2), count = 1.5)
        }

        val result = fetchAndParseOccupancy(testDaycare.id, defaultPeriod)

        assertEquals(
            listOf(
                OccupancyPeriod(
                    period = FiniteDateRange(day1, day1),
                    sum = 0.0,
                    headcount = 0,
                    caretakers = 2.0,
                    percentage = 0.0
                ),
                OccupancyPeriod(
                    period = FiniteDateRange(day2, day2),
                    sum = 0.0,
                    headcount = 0,
                    caretakers = 1.5,
                    percentage = 0.0
                )
            ),
            result
        )
    }

    @Test
    fun `realized occupancy is not calculated for dates in the future`() {
        val period = FiniteDateRange(LocalDate.now().minusDays(7), LocalDate.now().plusDays(7))
        db.transaction { tx ->
            tx.createOccupancyTestFixture(testDaycare.id, period, LocalDate.of(2015, 1, 1), PlacementType.DAYCARE)
            generateSequence(period.start) { date -> date.plusDays(1) }
                .takeWhile { date -> date <= LocalDate.now() }
                .forEach { date -> insertTestStaffAttendance(tx.handle, groupId = groupId, date = date, count = 1.0) }
        }

        val result = fetchAndParseOccupancy(testDaycare.id, period)

        assertEquals(
            listOf(OccupancyPeriod(period.copy(end = LocalDate.now()), 1.0, 1, 1.0, 14.3)),
            result
        )
    }

    private val testUser = AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.SERVICE_WORKER))

    private fun fetchAndParseOccupancy(unitId: UUID, period: FiniteDateRange): List<OccupancyPeriod> {
        val (_, response, result) = http
            .get("/occupancy/by-unit/$unitId?from=${period.start}&to=${period.end}&type=REALIZED")
            .asUser(testUser)
            .responseString()
        assertEquals(200, response.statusCode)

        return objectMapper.readValue<OccupancyResponse>(result.get()).occupancies
    }

    private fun fetchAndParseOccupancyByGroups(unitId: UUID, period: FiniteDateRange): List<OccupancyResponseGroupLevel> {
        val (_, response, result) = http
            .get("/occupancy/by-unit/$unitId/groups?from=${period.start}&to=${period.end}&type=REALIZED")
            .asUser(testUser)
            .responseString()
        assertEquals(200, response.statusCode)

        return objectMapper.readValue(result.get())
    }
}
