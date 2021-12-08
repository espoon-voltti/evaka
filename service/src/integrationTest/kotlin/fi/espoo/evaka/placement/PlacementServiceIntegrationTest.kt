// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.GroupPlacementId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.dev.insertTestServiceNeed
import fi.espoo.evaka.shared.dev.resetDatabase
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.security.PilotFeature
import fi.espoo.evaka.snDefaultDaycare
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.testDecisionMaker_1
import org.jdbi.v3.core.kotlin.mapTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlacementServiceIntegrationTest : FullApplicationTest() {

    val childId = testChild_1.id
    val unitId = testDaycare.id
    final val year = LocalDate.now().year + 1
    final val month = 1
    val placementStart = LocalDate.of(year, month, 10)
    val placementEnd = LocalDate.of(year, month, 20)

    lateinit var oldPlacement: Placement
    lateinit var groupId1: GroupId
    lateinit var groupId2: GroupId
    lateinit var daycarePlacementId: PlacementId
    lateinit var groupPlacementId: GroupPlacementId

    @BeforeEach
    internal fun setUp() {
        db.transaction {
            it.resetDatabase()
            it.insertGeneralTestFixtures()
        }
        db.transaction {
            groupId1 = it.insertTestDaycareGroup(
                DevDaycareGroup(
                    daycareId = unitId,
                    startDate = placementStart,
                    name = "group 1"
                )
            )
            groupId2 = it.insertTestDaycareGroup(
                DevDaycareGroup(
                    daycareId = unitId,
                    startDate = placementStart,
                    name = "group 2"
                )
            )

            oldPlacement = it.insertPlacement(
                PlacementType.PRESCHOOL,
                childId,
                unitId,
                placementStart,
                placementEnd
            )
            daycarePlacementId = oldPlacement.id

            groupPlacementId = it.insertTestDaycareGroupPlacement(
                daycarePlacementId = daycarePlacementId,
                groupId = groupId1,
                startDate = placementStart,
                endDate = placementEnd
            )
        }
    }

    /*
    old          XXXXX
    new               yyyyy
    result       XXXXXyyyyy
     */
    @Test
    fun `inserting placement without overlap`() {
        val newPlacement = db.transaction {
            createPlacement(
                it,
                PlacementType.PRESCHOOL,
                childId,
                unitId,
                LocalDate.of(year, month, 21),
                LocalDate.of(year, month, 30),
                useFiveYearsOldDaycare = true
            )
        }.first()

        val placements = db.read { it.getPlacementsForChild(childId) }

        assertEquals(2, placements.size)
        assertTrue(placements.containsAll(listOf(oldPlacement, newPlacement)))
    }

    /*
    old          XXXXX
    new          yyyyy
    result       yyyyy
     */
    @Test
    fun `inserting identical placement`() {
        val newPlacements = db.transaction {
            createPlacement(
                it,
                PlacementType.PRESCHOOL,
                childId,
                unitId,
                LocalDate.of(year, month, 10),
                LocalDate.of(year, month, 20),
                useFiveYearsOldDaycare = true
            )
        }

        val placements = db.read { it.getPlacementsForChild(childId) }

        assertEquals(newPlacements, placements)
    }

    /*
    old          XXXXX
    new           yyyy
    result       Xyyyy
     */
    @Test
    fun `old placement starts earlier`() {
        val newPlacement = db.transaction {
            createPlacement(
                it,
                PlacementType.PRESCHOOL,
                childId,
                unitId,
                LocalDate.of(year, month, 11),
                LocalDate.of(year, month, 20),
                useFiveYearsOldDaycare = true
            )
        }.first()

        val placements = db.read { it.getPlacementsForChild(childId) }

        assertEquals(2, placements.size)
        assertTrue(placements.contains(newPlacement))
        assertTrue(
            placements.any {
                it.id == oldPlacement.id &&
                    it.startDate.isEqual(oldPlacement.startDate) &&
                    it.endDate.isEqual(newPlacement.startDate.minusDays(1))
            }
        )
    }

    @Test
    fun `updating placement calls clearOldPlacements`() {
        val newPlacement = db.transaction {
            createPlacement(
                it,
                PlacementType.PRESCHOOL,
                childId,
                unitId,
                LocalDate.of(year, month, 21),
                LocalDate.of(year, month, 30),
                useFiveYearsOldDaycare = true
            )
        }.first()

        val originalPlacements = db.read { it.getPlacementsForChild(childId) }

        assertEquals(2, originalPlacements.size)
        assertTrue(originalPlacements.containsAll(listOf(oldPlacement, newPlacement)))

        val newStart = oldPlacement.endDate.minusDays(5)
        db.transaction {
            it.updatePlacement(
                id = newPlacement.id,
                startDate = newStart,
                endDate = newPlacement.endDate,
                useFiveYearsOldDaycare = true
            )
        }

        val newPlacements = db.read { it.getPlacementsForChild(childId) }
        val old = newPlacements.find { it.id == oldPlacement.id }!!
        val updated = newPlacements.find { it.id == newPlacement.id }!!

        assertEquals(2, newPlacements.size)
        assertEquals(newStart.minusDays(1), old.endDate)
        assertEquals(oldPlacement.startDate, old.startDate)
        assertEquals(newStart, updated.startDate)
        assertEquals(newPlacement.endDate, updated.endDate)
    }

    /*
    old          XXXXX
    new           yyyyy
    result       Xyyyyy
     */
    @Test
    fun `old placement starts earlier, ends earlier`() {
        val newPlacement = db.transaction {
            createPlacement(
                it,
                PlacementType.PRESCHOOL,
                childId,
                unitId,
                LocalDate.of(year, month, 11),
                LocalDate.of(year, month, 21),
                useFiveYearsOldDaycare = true
            )
        }.first()

        val placements = db.read { it.getPlacementsForChild(childId) }

        assertEquals(2, placements.size)
        assertTrue(placements.contains(newPlacement))
        assertTrue(
            placements.any {
                it.id == oldPlacement.id &&
                    it.startDate.isEqual(oldPlacement.startDate) &&
                    it.endDate.isEqual(newPlacement.startDate.minusDays(1))
            }
        )
    }

    /*
    old          XXXXX
    new           yyy
    result       XyyyX
     */
    @Test
    fun `old placement starts earlier, ends later`() {
        val newPlacement = db.transaction {
            createPlacement(
                it,
                PlacementType.PRESCHOOL,
                childId,
                unitId,
                LocalDate.of(year, month, 11),
                LocalDate.of(year, month, 19),
                useFiveYearsOldDaycare = true
            )
        }.first()

        val placements = db.read { it.getPlacementsForChild(childId) }

        assertEquals(3, placements.size)
        assertTrue(placements.contains(newPlacement))
        assertTrue(
            placements.any {
                it.id == oldPlacement.id &&
                    it.startDate.isEqual(oldPlacement.startDate) &&
                    it.endDate.isEqual(newPlacement.startDate.minusDays(1))
            }
        )
        assertTrue(
            placements.any {
                it.id != oldPlacement.id && it.id != newPlacement.id &&
                    it.startDate.isEqual(newPlacement.endDate.plusDays(1)) &&
                    it.endDate.isEqual(oldPlacement.endDate)
            }
        )
    }

    /*
    old          XXXXX
    new          yyyyyy
    result       yyyyyy
     */
    @Test
    fun `old placement ends earlier`() {
        val newPlacement = db.transaction {
            createPlacement(
                it,
                PlacementType.PRESCHOOL,
                childId,
                unitId,
                LocalDate.of(year, month, 10),
                LocalDate.of(year, month, 21),
                useFiveYearsOldDaycare = true
            )
        }.first()

        val placements = db.read { it.getPlacementsForChild(childId) }

        assertEquals(1, placements.size)
        assertEquals(listOf(newPlacement), placements)
    }

    /*
    old          XXXXX
    new          yyyy
    result       yyyyX
     */
    @Test
    fun `old placement ends later`() {
        val newPlacement = db.transaction {
            createPlacement(
                it,
                PlacementType.PRESCHOOL,
                childId,
                unitId,
                LocalDate.of(year, month, 10),
                LocalDate.of(year, month, 19),
                useFiveYearsOldDaycare = true
            )
        }.first()

        val placements = db.read { it.getPlacementsForChild(childId) }

        assertEquals(2, placements.size)
        assertTrue(placements.contains(newPlacement))
        assertTrue(
            placements.any {
                it.id == oldPlacement.id &&
                    it.startDate.isEqual(newPlacement.endDate.plusDays(1)) &&
                    it.endDate.isEqual(oldPlacement.endDate)
            }
        )
    }

    /*
    old          XXXXX
    new         yyyyyy
    result      yyyyyy
     */
    @Test
    fun `old placement starts later`() {
        val newPlacement = db.transaction {
            createPlacement(
                it,
                PlacementType.PRESCHOOL,
                childId,
                unitId,
                LocalDate.of(year, month, 9),
                LocalDate.of(year, month, 20),
                useFiveYearsOldDaycare = true
            )
        }.first()

        val placements = db.read { it.getPlacementsForChild(childId) }

        assertEquals(1, placements.size)
        assertTrue(placements.contains(newPlacement))
    }

    /*
    old          XXXXX
    new         yyyyyyy
    result      yyyyyyy
     */
    @Test
    fun `old placement starts later, ends earlier`() {
        val newPlacement = db.transaction {
            createPlacement(
                it,
                PlacementType.PRESCHOOL,
                childId,
                unitId,
                LocalDate.of(year, month, 9),
                LocalDate.of(year, month, 21),
                useFiveYearsOldDaycare = true
            )
        }.first()

        val placements = db.read { it.getPlacementsForChild(childId) }

        assertEquals(1, placements.size)
        assertTrue(placements.contains(newPlacement))
    }

    /*
    old          XXXXX
    new         yyyyy
    result      yyyyyX
     */
    @Test
    fun `old placement starts later, ends later`() {
        val newPlacement = db.transaction {
            createPlacement(
                it,
                PlacementType.PRESCHOOL,
                childId,
                unitId,
                LocalDate.of(year, month, 9),
                LocalDate.of(year, month, 19),
                useFiveYearsOldDaycare = true
            )
        }.first()

        val placements = db.read { it.getPlacementsForChild(childId) }

        assertEquals(2, placements.size)
        assertTrue(placements.contains(newPlacement))
        assertTrue(
            placements.any {
                it.id == oldPlacement.id &&
                    it.startDate.isEqual(newPlacement.endDate.plusDays(1)) &&
                    it.endDate.isEqual(oldPlacement.endDate)
            }
        )
    }

    /*
   old         XXXYYY
   new          ZZZZ
   result      XZZZZY
    */
    @Test
    fun `placement overlaps with two earlier placements`() {
        val old2 = db.transaction {
            createPlacement(
                it,
                PlacementType.PRESCHOOL_DAYCARE,
                childId,
                unitId,
                LocalDate.of(year, month, 21),
                LocalDate.of(year, month, 31),
                useFiveYearsOldDaycare = true
            )
        }.first()
        val newPlacement = db.transaction {
            createPlacement(
                it,
                PlacementType.PREPARATORY,
                childId,
                unitId,
                LocalDate.of(year, month, 15),
                LocalDate.of(year, month, 25),
                useFiveYearsOldDaycare = true
            )
        }.first()

        val placements = db.read { it.getPlacementsForChild(childId) }

        assertEquals(3, placements.size)
        assertTrue(placements.contains(newPlacement))
        assertTrue(
            placements.any {
                it.id == oldPlacement.id &&
                    it.endDate.isEqual(newPlacement.startDate.minusDays(1)) &&
                    it.startDate.isEqual(oldPlacement.startDate)
            }
        )
        assertTrue(
            placements.any {
                it.id == old2.id &&
                    it.startDate.isEqual(newPlacement.endDate.plusDays(1)) &&
                    it.endDate.isEqual(old2.endDate)
            }
        )
    }

    @Test
    fun `updating placement endDate to be earlier`() {
        val oldPlacement = db.transaction {
            createPlacement(
                it,
                PlacementType.DAYCARE,
                childId,
                unitId,
                LocalDate.of(year, month, 10),
                LocalDate.of(year, month, 20),
                useFiveYearsOldDaycare = true
            )
        }.first()

        db.transaction {
            it.updatePlacement(
                id = oldPlacement.id,
                startDate = oldPlacement.startDate,
                endDate = LocalDate.of(year, month, 19),
                useFiveYearsOldDaycare = true
            )
        }

        val updated = db.read { it.getPlacement(oldPlacement.id) }
        val expected = oldPlacement.copy(endDate = LocalDate.of(year, month, 19))
        assertEquals(expected, updated)
    }

    @Test
    fun `updating placement endDate to be earlier cuts group placements and service needs`() {
        val oldPlacement = db.transaction {
            createPlacement(
                it,
                PlacementType.DAYCARE,
                childId,
                unitId,
                LocalDate.of(year, month, 10),
                LocalDate.of(year, month, 20),
                useFiveYearsOldDaycare = true
            )
        }.first()
        val groupId = db.transaction { it.insertTestDaycareGroup(DevDaycareGroup(daycareId = unitId)) }
        db.transaction {
            it.insertTestDaycareGroupPlacement(
                groupId = groupId,
                daycarePlacementId = oldPlacement.id,
                startDate = oldPlacement.startDate,
                endDate = oldPlacement.endDate
            )
            it.insertTestServiceNeed(
                confirmedBy = testDecisionMaker_1.id,
                placementId = oldPlacement.id,
                period = FiniteDateRange(oldPlacement.startDate, oldPlacement.endDate),
                optionId = snDefaultDaycare.id
            )
        }

        db.transaction {
            it.updatePlacement(
                id = oldPlacement.id,
                startDate = oldPlacement.startDate,
                endDate = LocalDate.of(year, month, 15),
                useFiveYearsOldDaycare = true
            )
        }

        val groupPlacementEndDates = db.read {
            it.createQuery("SELECT end_date FROM daycare_group_placement WHERE daycare_group_id = :id")
                .bind("id", groupId)
                .mapTo<LocalDate>()
                .list()
        }
        assertEquals(1, groupPlacementEndDates.size)
        assertEquals(LocalDate.of(year, month, 15), groupPlacementEndDates.first())

        val serviceNeedEndDates = db.read {
            it.createQuery("SELECT end_date FROM service_need WHERE placement_id = :id")
                .bind("id", oldPlacement.id)
                .mapTo<LocalDate>()
                .list()
        }
        assertEquals(1, serviceNeedEndDates.size)
        assertEquals(LocalDate.of(year, month, 15), serviceNeedEndDates.first())
    }

    @Test
    fun `changing placement type by creating new placement ends the group placements and service needs also`() {
        val oldPlacement = db.transaction {
            createPlacement(
                it,
                PlacementType.DAYCARE,
                childId,
                unitId,
                LocalDate.of(year, month, 1),
                LocalDate.of(year, month, 30),
                useFiveYearsOldDaycare = true
            )
        }.first()
        val groupId = db.transaction { it.insertTestDaycareGroup(DevDaycareGroup(daycareId = unitId)) }
        db.transaction {
            it.insertTestDaycareGroupPlacement(
                groupId = groupId,
                daycarePlacementId = oldPlacement.id,
                startDate = oldPlacement.startDate,
                endDate = oldPlacement.endDate
            )
            it.insertTestServiceNeed(
                confirmedBy = testDecisionMaker_1.id,
                placementId = oldPlacement.id,
                period = FiniteDateRange(oldPlacement.startDate, oldPlacement.endDate),
                optionId = snDefaultDaycare.id
            )
        }

        db.transaction {
            createPlacement(
                it,
                PlacementType.PRESCHOOL_DAYCARE,
                childId,
                unitId,
                LocalDate.of(year, month, 15),
                LocalDate.of(year, month, 30),
                useFiveYearsOldDaycare = true
            )
        }

        data class QueryResult(
            val startDate: LocalDate,
            val endDate: LocalDate
        )

        val groupPlacements = db.read {
            it.createQuery("SELECT start_date, end_date FROM daycare_group_placement WHERE daycare_group_id = :id")
                .bind("id", groupId)
                .mapTo<QueryResult>()
                .list()
        }

        assertEquals(1, groupPlacements.size)
        assertEquals(LocalDate.of(year, month, 1), groupPlacements.first().startDate)
        assertEquals(LocalDate.of(year, month, 14), groupPlacements.first().endDate)

        val serviceNeeds = db.read {
            it.createQuery("SELECT start_date, end_date FROM service_need WHERE placement_id = :id")
                .bind("id", oldPlacement.id)
                .mapTo<QueryResult>()
                .list()
        }

        assertEquals(1, serviceNeeds.size)
        assertEquals(LocalDate.of(year, month, 1), serviceNeeds.first().startDate)
        assertEquals(LocalDate.of(year, month, 14), serviceNeeds.first().endDate)
    }

    @Test
    fun `updating placement endDate to be later`() {
        val oldPlacement = db.transaction {
            createPlacement(
                it,
                PlacementType.DAYCARE,
                childId,
                unitId,
                LocalDate.of(year, month, 10),
                LocalDate.of(year, month, 20),
                useFiveYearsOldDaycare = true
            )
        }.first()

        db.transaction {
            it.updatePlacement(
                id = oldPlacement.id,
                startDate = oldPlacement.startDate,
                endDate = LocalDate.of(year, month, 21),
                useFiveYearsOldDaycare = true
            )
        }

        val updated = db.read { it.getPlacement(oldPlacement.id) }
        val expected = oldPlacement.copy(endDate = LocalDate.of(year, month, 21))
        assertEquals(expected, updated)
    }

    @Test
    fun `updating placement endDate to be earlier than startDate is not allowed`() {
        val oldPlacement = db.transaction {
            createPlacement(
                it,
                PlacementType.DAYCARE,
                childId,
                unitId,
                LocalDate.of(year, month, 10),
                LocalDate.of(year, month, 20),
                useFiveYearsOldDaycare = true
            )
        }.first()
        db.transaction {
            assertThrows<BadRequest> {
                it.updatePlacement(
                    id = oldPlacement.id,
                    startDate = oldPlacement.startDate,
                    endDate = oldPlacement.startDate.minusDays(1),
                    useFiveYearsOldDaycare = true
                )
            }
        }
    }

    @Test
    fun `transferring splits the group placement`() {
        val transferDate = placementStart.plusDays(5)
        db.transaction {
            it.transferGroup(groupPlacementId, groupId2, transferDate)
        }

        val groupPlacements = db.read {
            it.getDetailedDaycarePlacements(
                daycareId = unitId,
                childId = childId,
                startDate = null,
                endDate = null
            )
        }
            .also { assertEquals(1, it.size) }
            .first()
            .groupPlacements
            .also { assertEquals(2, it.size) }

        val p1 = groupPlacements.first { it.groupId == groupId1 }
        val p2 = groupPlacements.first { it.groupId == groupId2 }
        assertEquals(p1.startDate, placementStart)
        assertEquals(p1.endDate, transferDate.minusDays(1))
        assertEquals(p2.startDate, transferDate)
        assertEquals(p2.endDate, placementEnd)
    }

    @Test
    fun `transferring deletes old group placement if start dates match`() {
        val transferDate = placementStart
        db.transaction { it.transferGroup(groupPlacementId, groupId2, transferDate) }

        val groupPlacements = db.read {
            it.getDetailedDaycarePlacements(
                daycareId = unitId,
                childId = childId,
                startDate = null,
                endDate = null
            )
        }
            .also { assertEquals(1, it.size) }
            .first()
            .groupPlacements
            .also { assertEquals(1, it.size) }

        val p2 = groupPlacements.first { it.groupId == groupId2 }
        assertEquals(p2.startDate, placementStart)
        assertEquals(p2.endDate, placementEnd)
    }

    @Test
    fun `if no group placements then getDaycarePlacements shows full range without groupId`() {
        val daycarePlacementId = PlacementId(UUID.randomUUID())
        val daycarePlacementStartDate = placementStart
        val daycarePlacementEndDate = placementStart.plusDays(5)
        val daycarePlacementType = PlacementType.DAYCARE

        db.transaction {
            it.insertTestPlacement(
                id = daycarePlacementId,
                childId = testChild_2.id,
                unitId = testDaycare2.id,
                startDate = daycarePlacementStartDate,
                endDate = daycarePlacementEndDate,
                type = daycarePlacementType
            )
        }

        val placements = db.read { it.getDetailedDaycarePlacements(testDaycare2.id, null, null, null) }

        assertEquals(
            setOf(
                DaycarePlacementWithDetails(
                    id = daycarePlacementId,
                    child = ChildBasics(
                        id = testChild_2.id,
                        dateOfBirth = testChild_2.dateOfBirth,
                        socialSecurityNumber = testChild_2.ssn,
                        firstName = testChild_2.firstName,
                        lastName = testChild_2.lastName
                    ),
                    daycare = DaycareBasics(
                        testDaycare2.id,
                        testDaycare2.name,
                        testDaycare2.areaName,
                        ProviderType.MUNICIPAL,
                        listOf(PilotFeature.MESSAGING)
                    ),
                    startDate = daycarePlacementStartDate,
                    endDate = daycarePlacementEndDate,
                    type = daycarePlacementType,
                    missingServiceNeedDays = 6,
                    groupPlacements = listOf(
                        DaycareGroupPlacement(
                            id = null,
                            groupId = null,
                            groupName = null,
                            daycarePlacementId = daycarePlacementId,
                            startDate = daycarePlacementStartDate,
                            endDate = daycarePlacementEndDate
                        )
                    ),
                    serviceNeeds = emptyList(),
                    terminationRequestedBy = null,
                    terminationRequestedDate = null
                )
            ),
            placements
        )
    }

    @Test
    fun `if there are group placements then getDaycarePlacements also shows gaps without groupId`() {
        val date = { day: Int -> LocalDate.of(year, month, day) }

        val daycarePlacementId = PlacementId(UUID.randomUUID())
        val daycarePlacementStartDate = date(1)
        val daycarePlacementEndDate = date(20)
        val daycarePlacementType = PlacementType.DAYCARE

        db.transaction {
            it.insertTestPlacement(
                id = daycarePlacementId,
                childId = testChild_2.id,
                unitId = testDaycare2.id,
                startDate = daycarePlacementStartDate,
                endDate = daycarePlacementEndDate,
                type = daycarePlacementType
            )
        }

        val groupPlacementId1 = GroupPlacementId(UUID.randomUUID())
        val groupPlacementId2 = GroupPlacementId(UUID.randomUUID())
        val groupPlacementId3 = GroupPlacementId(UUID.randomUUID())
        val groupPlacementId4 = GroupPlacementId(UUID.randomUUID())
        val groupPlacementId5 = GroupPlacementId(UUID.randomUUID())
        val groupPlacementIds =
            listOf(groupPlacementId1, groupPlacementId2, groupPlacementId3, groupPlacementId4, groupPlacementId5)
        val groupPlacementDays = listOf(3 to 5, 6 to 9, 12 to 12, 16 to 17, 19 to 20)

        db.transaction { tx ->
            groupPlacementDays.forEachIndexed { index, (startDate, endDate) ->
                tx.insertTestDaycareGroupPlacement(
                    id = groupPlacementIds[index],
                    groupId = groupId1,
                    daycarePlacementId = daycarePlacementId,
                    startDate = date(startDate),
                    endDate = date(endDate)
                )
            }
        }

        val placements = db.read { it.getDetailedDaycarePlacements(testDaycare2.id, null, null, null) }

        assertEquals(
            setOf(
                DaycarePlacementWithDetails(
                    id = daycarePlacementId,
                    child = ChildBasics(
                        id = testChild_2.id,
                        dateOfBirth = testChild_2.dateOfBirth,
                        socialSecurityNumber = testChild_2.ssn,
                        firstName = testChild_2.firstName,
                        lastName = testChild_2.lastName
                    ),
                    daycare = DaycareBasics(
                        testDaycare2.id,
                        testDaycare2.name,
                        testDaycare2.areaName,
                        ProviderType.MUNICIPAL,
                        listOf(PilotFeature.MESSAGING)
                    ),
                    startDate = daycarePlacementStartDate,
                    endDate = daycarePlacementEndDate,
                    type = daycarePlacementType,
                    missingServiceNeedDays = 20,
                    groupPlacements = listOf(
                        DaycareGroupPlacement(
                            id = null,
                            groupId = null,
                            groupName = null,
                            daycarePlacementId = daycarePlacementId,
                            startDate = date(1),
                            endDate = date(2)
                        ),
                        DaycareGroupPlacement(
                            id = groupPlacementId1,
                            groupId = groupId1,
                            groupName = "group 1",
                            daycarePlacementId = daycarePlacementId,
                            startDate = date(3),
                            endDate = date(5)
                        ),
                        DaycareGroupPlacement(
                            id = groupPlacementId2,
                            groupId = groupId1,
                            groupName = "group 1",
                            daycarePlacementId = daycarePlacementId,
                            startDate = date(6),
                            endDate = date(9)
                        ),
                        DaycareGroupPlacement(
                            id = null,
                            groupId = null,
                            groupName = null,
                            daycarePlacementId = daycarePlacementId,
                            startDate = date(10),
                            endDate = date(11)
                        ),
                        DaycareGroupPlacement(
                            id = groupPlacementId3,
                            groupId = groupId1,
                            groupName = "group 1",
                            daycarePlacementId = daycarePlacementId,
                            startDate = date(12),
                            endDate = date(12)
                        ),
                        DaycareGroupPlacement(
                            id = null,
                            groupId = null,
                            groupName = null,
                            daycarePlacementId = daycarePlacementId,
                            startDate = date(13),
                            endDate = date(15)
                        ),
                        DaycareGroupPlacement(
                            id = groupPlacementId4,
                            groupId = groupId1,
                            groupName = "group 1",
                            daycarePlacementId = daycarePlacementId,
                            startDate = date(16),
                            endDate = date(17)
                        ),
                        DaycareGroupPlacement(
                            id = null,
                            groupId = null,
                            groupName = null,
                            daycarePlacementId = daycarePlacementId,
                            startDate = date(18),
                            endDate = date(18)
                        ),
                        DaycareGroupPlacement(
                            id = groupPlacementId5,
                            groupId = groupId1,
                            groupName = "group 1",
                            daycarePlacementId = daycarePlacementId,
                            startDate = date(19),
                            endDate = date(20)
                        )
                    ),
                    serviceNeeds = emptyList(),
                    terminationRequestedBy = null,
                    terminationRequestedDate = null
                ),
            ),
            placements
        )
    }
}
