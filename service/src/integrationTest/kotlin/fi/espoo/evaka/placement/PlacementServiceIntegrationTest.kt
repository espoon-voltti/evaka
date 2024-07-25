// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.insertServiceNeedOptions
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.GroupPlacementId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.dev.DevBackupCare
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevServiceNeed
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.PilotFeature
import fi.espoo.evaka.snDefaultDaycare
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testDecisionMaker_1
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PlacementServiceIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    private val area1 = DevCareArea(name = "Area 1", shortName = "area1")
    private val daycare1 = DevDaycare(areaId = area1.id, name = "Daycare 1")
    private val area2 = DevCareArea(name = "Area 2", shortName = "area2")
    private val daycare2 =
        DevDaycare(
            areaId = area2.id,
            name = "Daycare 2",
            enabledPilotFeatures = setOf(PilotFeature.MESSAGING)
        )

    val childId = testChild_1.id
    val unitId = daycare1.id
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
    fun setUp() {
        db.transaction { tx ->
            tx.insert(testDecisionMaker_1)
            tx.insert(area1)
            tx.insert(daycare1)
            tx.insert(area2)
            tx.insert(daycare2)
            listOf(testChild_1, testChild_2).forEach { tx.insert(it, DevPersonType.CHILD) }
            tx.insertServiceNeedOptions()
            groupId1 =
                tx.insert(
                    DevDaycareGroup(
                        daycareId = unitId,
                        startDate = placementStart,
                        name = "group 1"
                    )
                )
            groupId2 =
                tx.insert(
                    DevDaycareGroup(
                        daycareId = unitId,
                        startDate = placementStart,
                        name = "group 2"
                    )
                )

            oldPlacement =
                tx.insertPlacement(
                    PlacementType.DAYCARE,
                    childId,
                    unitId,
                    placementStart,
                    placementEnd,
                    false
                )
            daycarePlacementId = oldPlacement.id

            groupPlacementId =
                tx.insert(
                    DevDaycareGroupPlacement(
                        daycarePlacementId = daycarePlacementId,
                        daycareGroupId = groupId1,
                        startDate = placementStart,
                        endDate = placementEnd
                    )
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
        val newPlacement =
            db.transaction {
                    createPlacement(
                        it,
                        childId,
                        unitId,
                        FiniteDateRange(
                            LocalDate.of(year, month, 21),
                            LocalDate.of(year, month, 30)
                        ),
                        PlacementType.PRESCHOOL,
                        useFiveYearsOldDaycare = true,
                        placeGuarantee = false
                    )
                }
                .first()

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
        val newPlacements =
            db.transaction {
                createPlacement(
                    it,
                    childId,
                    unitId,
                    FiniteDateRange(LocalDate.of(year, month, 10), LocalDate.of(year, month, 20)),
                    PlacementType.PRESCHOOL,
                    useFiveYearsOldDaycare = true,
                    placeGuarantee = false
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
        val newPlacement =
            db.transaction {
                    createPlacement(
                        it,
                        childId,
                        unitId,
                        FiniteDateRange(
                            LocalDate.of(year, month, 11),
                            LocalDate.of(year, month, 20)
                        ),
                        PlacementType.PRESCHOOL,
                        useFiveYearsOldDaycare = true,
                        placeGuarantee = false
                    )
                }
                .first()

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
        val newPlacement =
            db.transaction {
                    createPlacement(
                        it,
                        childId,
                        unitId,
                        FiniteDateRange(
                            LocalDate.of(year, month, 21),
                            LocalDate.of(year, month, 30)
                        ),
                        PlacementType.PRESCHOOL,
                        useFiveYearsOldDaycare = true,
                        placeGuarantee = false
                    )
                }
                .first()

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
        val newPlacement =
            db.transaction {
                    createPlacement(
                        it,
                        childId,
                        unitId,
                        FiniteDateRange(
                            LocalDate.of(year, month, 11),
                            LocalDate.of(year, month, 21)
                        ),
                        PlacementType.PRESCHOOL,
                        useFiveYearsOldDaycare = true,
                        placeGuarantee = false
                    )
                }
                .first()

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
        val newPlacement =
            db.transaction {
                    createPlacement(
                        it,
                        childId,
                        unitId,
                        FiniteDateRange(
                            LocalDate.of(year, month, 11),
                            LocalDate.of(year, month, 19)
                        ),
                        PlacementType.PRESCHOOL,
                        useFiveYearsOldDaycare = true,
                        placeGuarantee = false
                    )
                }
                .first()

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
                it.id != oldPlacement.id &&
                    it.id != newPlacement.id &&
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
        val newPlacement =
            db.transaction {
                    createPlacement(
                        it,
                        childId,
                        unitId,
                        FiniteDateRange(
                            LocalDate.of(year, month, 10),
                            LocalDate.of(year, month, 21)
                        ),
                        PlacementType.PRESCHOOL,
                        useFiveYearsOldDaycare = true,
                        placeGuarantee = false
                    )
                }
                .first()

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
        val newPlacement =
            db.transaction {
                    createPlacement(
                        it,
                        childId,
                        unitId,
                        FiniteDateRange(
                            LocalDate.of(year, month, 10),
                            LocalDate.of(year, month, 19)
                        ),
                        PlacementType.PRESCHOOL,
                        useFiveYearsOldDaycare = true,
                        placeGuarantee = false
                    )
                }
                .first()

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
        val newPlacement =
            db.transaction {
                    createPlacement(
                        it,
                        childId,
                        unitId,
                        FiniteDateRange(
                            LocalDate.of(year, month, 9),
                            LocalDate.of(year, month, 20)
                        ),
                        PlacementType.PRESCHOOL,
                        useFiveYearsOldDaycare = true,
                        placeGuarantee = false
                    )
                }
                .first()

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
        val newPlacement =
            db.transaction {
                    createPlacement(
                        it,
                        childId,
                        unitId,
                        FiniteDateRange(
                            LocalDate.of(year, month, 9),
                            LocalDate.of(year, month, 21)
                        ),
                        PlacementType.PRESCHOOL,
                        useFiveYearsOldDaycare = true,
                        placeGuarantee = false
                    )
                }
                .first()

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
        val newPlacement =
            db.transaction {
                    createPlacement(
                        it,
                        childId,
                        unitId,
                        FiniteDateRange(
                            LocalDate.of(year, month, 9),
                            LocalDate.of(year, month, 19)
                        ),
                        PlacementType.PRESCHOOL,
                        useFiveYearsOldDaycare = true,
                        placeGuarantee = false
                    )
                }
                .first()

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
        val old2 =
            db.transaction {
                    createPlacement(
                        it,
                        childId,
                        unitId,
                        FiniteDateRange(
                            LocalDate.of(year, month, 21),
                            LocalDate.of(year, month, 31)
                        ),
                        PlacementType.PRESCHOOL_DAYCARE,
                        useFiveYearsOldDaycare = true,
                        placeGuarantee = false
                    )
                }
                .first()
        val newPlacement =
            db.transaction {
                    createPlacement(
                        it,
                        childId,
                        unitId,
                        FiniteDateRange(
                            LocalDate.of(year, month, 15),
                            LocalDate.of(year, month, 25)
                        ),
                        PlacementType.PREPARATORY,
                        useFiveYearsOldDaycare = true,
                        placeGuarantee = false
                    )
                }
                .first()

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

    /*
    old          XXXXX
    new           yyy
    result       Xyyy
     */
    @Test
    fun `old placement starts earlier, ends later and new placement type is CLUB`() {
        val newPlacement =
            db.transaction {
                    createPlacements(
                        tx = it,
                        childId = childId,
                        unitId = unitId,
                        placementTypePeriods =
                            listOf(
                                FiniteDateRange(
                                    LocalDate.of(year, month, 11),
                                    LocalDate.of(year, month, 19)
                                ) to PlacementType.CLUB
                            ),
                        cancelPlacementsAfterClub = true,
                        placeGuarantee = false
                    )
                }
                .first()

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
        assertFalse(
            placements.any {
                it.id != oldPlacement.id &&
                    it.id != newPlacement.id &&
                    it.startDate.isEqual(newPlacement.endDate.plusDays(1)) &&
                    it.endDate.isEqual(oldPlacement.endDate)
            }
        )
    }

    /*
    old          XXXXX
    new          yyyy
    result       yyyy
     */
    @Test
    fun `old placement ends later and new placement type is CLUB`() {
        val newPlacement =
            db.transaction {
                    createPlacements(
                        tx = it,
                        childId = childId,
                        unitId = unitId,
                        placementTypePeriods =
                            listOf(
                                FiniteDateRange(
                                    LocalDate.of(year, month, 10),
                                    LocalDate.of(year, month, 19)
                                ) to PlacementType.CLUB
                            ),
                        cancelPlacementsAfterClub = true,
                        placeGuarantee = false
                    )
                }
                .first()

        val placements = db.read { it.getPlacementsForChild(childId) }

        assertEquals(1, placements.size)
        assertTrue(placements.contains(newPlacement))
        assertFalse(placements.any { it.id == oldPlacement.id })
    }

    /*
    old          XXXXX
    new         yyyyy
    result      yyyyy
     */
    @Test
    fun `old placement starts later, ends later and new placement type is CLUB`() {
        val newPlacement =
            db.transaction {
                    createPlacements(
                        tx = it,
                        childId = childId,
                        unitId = unitId,
                        placementTypePeriods =
                            listOf(
                                FiniteDateRange(
                                    LocalDate.of(year, month, 9),
                                    LocalDate.of(year, month, 19)
                                ) to PlacementType.CLUB
                            ),
                        cancelPlacementsAfterClub = true,
                        placeGuarantee = false
                    )
                }
                .first()

        val placements = db.read { it.getPlacementsForChild(childId) }

        assertEquals(1, placements.size)
        assertTrue(placements.contains(newPlacement))
        assertFalse(placements.any { it.id == oldPlacement.id })
    }

    @Test
    fun `adding a new placement of type CLUB deletes future placements`() {
        val futurePlacement =
            db.transaction {
                it.insertPlacement(
                    PlacementType.DAYCARE,
                    childId,
                    unitId,
                    LocalDate.of(year + 2, 8, 1),
                    LocalDate.of(year + 2, 12, 1),
                    false
                )
            }

        val newPlacement =
            db.transaction {
                    createPlacements(
                        tx = it,
                        childId = childId,
                        unitId = unitId,
                        placementTypePeriods =
                            listOf(
                                FiniteDateRange(
                                    LocalDate.of(year, month, 9),
                                    LocalDate.of(year, month, 19)
                                ) to PlacementType.CLUB
                            ),
                        cancelPlacementsAfterClub = true,
                        placeGuarantee = false
                    )
                }
                .first()

        val placements = db.read { it.getPlacementsForChild(childId) }

        assertEquals(1, placements.size)
        assertTrue(placements.contains(newPlacement))
        assertFalse(placements.contains(futurePlacement))
        assertFalse(placements.any { it.id == oldPlacement.id })
    }

    @Test
    fun `adding a new placement of type CLUB does NOT delete future placements during a PRESCHOOL term`() {
        // insert a future PRESCHOOL placement
        val (futurePreschoolDaycare, futureDaycareInPreschoolTerm, futurePreschool) =
            db.transaction {
                listOf(
                    it.insertPlacement(
                        PlacementType.PRESCHOOL_DAYCARE,
                        childId,
                        unitId,
                        LocalDate.of(year + 1, 8, 1),
                        LocalDate.of(year + 2, 6, 30),
                        false
                    ),
                    it.insertPlacement(
                        PlacementType.DAYCARE,
                        childId,
                        unitId,
                        LocalDate.of(year + 2, 7, 1),
                        LocalDate.of(year + 2, 7, 31),
                        false
                    ),
                    it.insertPlacement(
                        PlacementType.PRESCHOOL,
                        childId,
                        unitId,
                        LocalDate.of(year + 2, 9, 1),
                        LocalDate.of(year + 3, 4, 11),
                        false
                    )
                )
            }

        val newPlacement =
            db.transaction {
                    createPlacements(
                        tx = it,
                        childId = childId,
                        unitId = unitId,
                        placementTypePeriods =
                            listOf(
                                FiniteDateRange(
                                    LocalDate.of(year, month, 9),
                                    LocalDate.of(year, month, 19)
                                ) to PlacementType.CLUB
                            ),
                        cancelPlacementsAfterClub = true,
                        placeGuarantee = false
                    )
                }
                .first()

        val placements = db.read { it.getPlacementsForChild(childId) }

        assertEquals(4, placements.size)
        assertTrue(placements.contains(newPlacement))
        assertTrue(placements.contains(futurePreschoolDaycare))
        assertTrue(placements.contains(futureDaycareInPreschoolTerm))
        assertTrue(placements.contains(futurePreschool))
        assertFalse(placements.any { it.id == oldPlacement.id })
    }

    @Test
    fun `adding a new placement of type CLUB does NOT delete future PREPARATORY, PREPARATORY_DAYCARE placements`() {
        // insert a future PRESCHOOL placement
        val (futurePreparatoryDaycare, futureDaycareInPreparatoryTerm, futurePreparatory) =
            db.transaction {
                listOf(
                    it.insertPlacement(
                        PlacementType.PREPARATORY_DAYCARE,
                        childId,
                        unitId,
                        LocalDate.of(year + 1, 8, 1),
                        LocalDate.of(year + 2, 6, 30),
                        false
                    ),
                    it.insertPlacement(
                        PlacementType.DAYCARE,
                        childId,
                        unitId,
                        LocalDate.of(year + 2, 7, 1),
                        LocalDate.of(year + 2, 7, 31),
                        false
                    ),
                    it.insertPlacement(
                        PlacementType.PREPARATORY_DAYCARE,
                        childId,
                        unitId,
                        LocalDate.of(year + 2, 9, 1),
                        LocalDate.of(year + 3, 4, 11),
                        false
                    )
                )
            }

        val newPlacement =
            db.transaction {
                    createPlacements(
                        tx = it,
                        childId = childId,
                        unitId = unitId,
                        placementTypePeriods =
                            listOf(
                                FiniteDateRange(
                                    LocalDate.of(year, month, 9),
                                    LocalDate.of(year, month, 19)
                                ) to PlacementType.CLUB
                            ),
                        cancelPlacementsAfterClub = true,
                        placeGuarantee = false
                    )
                }
                .first()

        val placements = db.read { it.getPlacementsForChild(childId) }

        assertEquals(4, placements.size)
        assertTrue(placements.contains(newPlacement))
        assertTrue(placements.contains(futurePreparatoryDaycare))
        assertTrue(placements.contains(futureDaycareInPreparatoryTerm))
        assertTrue(placements.contains(futurePreparatory))
        assertFalse(placements.any { it.id == oldPlacement.id })
    }

    @Test
    fun `updating placement endDate to be earlier`() {
        val oldPlacement =
            db.transaction {
                    createPlacement(
                        it,
                        childId,
                        unitId,
                        FiniteDateRange(
                            LocalDate.of(year, month, 10),
                            LocalDate.of(year, month, 20)
                        ),
                        PlacementType.DAYCARE,
                        useFiveYearsOldDaycare = true,
                        placeGuarantee = false
                    )
                }
                .first()

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
        val oldPlacement =
            db.transaction {
                    createPlacement(
                        it,
                        childId,
                        unitId,
                        FiniteDateRange(
                            LocalDate.of(year, month, 10),
                            LocalDate.of(year, month, 20)
                        ),
                        PlacementType.DAYCARE,
                        useFiveYearsOldDaycare = true,
                        placeGuarantee = false
                    )
                }
                .first()
        val groupId = db.transaction { it.insert(DevDaycareGroup(daycareId = unitId)) }
        db.transaction {
            it.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = oldPlacement.id,
                    daycareGroupId = groupId,
                    startDate = oldPlacement.startDate,
                    endDate = oldPlacement.endDate
                )
            )
            val period = FiniteDateRange(oldPlacement.startDate, oldPlacement.endDate)
            it.insert(
                DevServiceNeed(
                    placementId = oldPlacement.id,
                    startDate = period.start,
                    endDate = period.end,
                    optionId = snDefaultDaycare.id,
                    confirmedBy = EvakaUserId(testDecisionMaker_1.id.raw),
                    confirmedAt = HelsinkiDateTime.now()
                )
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

        val groupPlacementEndDates =
            db.read {
                it.createQuery {
                        sql(
                            "SELECT end_date FROM daycare_group_placement WHERE daycare_group_id = ${bind(groupId)}"
                        )
                    }
                    .toList<LocalDate>()
            }
        assertEquals(1, groupPlacementEndDates.size)
        assertEquals(LocalDate.of(year, month, 15), groupPlacementEndDates.first())

        val serviceNeedEndDates =
            db.read {
                it.createQuery {
                        sql(
                            "SELECT end_date FROM service_need WHERE placement_id = ${bind(oldPlacement.id)}"
                        )
                    }
                    .toList<LocalDate>()
            }
        assertEquals(1, serviceNeedEndDates.size)
        assertEquals(LocalDate.of(year, month, 15), serviceNeedEndDates.first())
    }

    @Test
    fun `changing placement type by creating new placement ends the group placements and service needs also`() {
        val oldPlacement =
            db.transaction {
                    createPlacement(
                        it,
                        childId,
                        unitId,
                        FiniteDateRange(
                            LocalDate.of(year, month, 1),
                            LocalDate.of(year, month, 30)
                        ),
                        PlacementType.DAYCARE,
                        useFiveYearsOldDaycare = true,
                        placeGuarantee = false
                    )
                }
                .first()
        val groupId = db.transaction { it.insert(DevDaycareGroup(daycareId = unitId)) }
        db.transaction {
            it.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = oldPlacement.id,
                    daycareGroupId = groupId,
                    startDate = oldPlacement.startDate,
                    endDate = oldPlacement.endDate
                )
            )
            val period = FiniteDateRange(oldPlacement.startDate, oldPlacement.endDate)
            it.insert(
                DevServiceNeed(
                    placementId = oldPlacement.id,
                    startDate = period.start,
                    endDate = period.end,
                    optionId = snDefaultDaycare.id,
                    confirmedBy = EvakaUserId(testDecisionMaker_1.id.raw),
                    confirmedAt = HelsinkiDateTime.now()
                )
            )
        }

        db.transaction {
            createPlacement(
                it,
                childId,
                unitId,
                FiniteDateRange(LocalDate.of(year, month, 15), LocalDate.of(year, month, 30)),
                PlacementType.PRESCHOOL_DAYCARE,
                useFiveYearsOldDaycare = true,
                placeGuarantee = false
            )
        }

        data class QueryResult(val startDate: LocalDate, val endDate: LocalDate)

        val groupPlacements =
            db.read {
                it.createQuery {
                        sql(
                            "SELECT start_date, end_date FROM daycare_group_placement WHERE daycare_group_id = ${bind(groupId)}"
                        )
                    }
                    .toList<QueryResult>()
            }

        assertEquals(1, groupPlacements.size)
        assertEquals(LocalDate.of(year, month, 1), groupPlacements.first().startDate)
        assertEquals(LocalDate.of(year, month, 14), groupPlacements.first().endDate)

        val serviceNeeds =
            db.read {
                it.createQuery {
                        sql(
                            "SELECT start_date, end_date FROM service_need WHERE placement_id = ${bind(oldPlacement.id)}"
                        )
                    }
                    .toList<QueryResult>()
            }

        assertEquals(1, serviceNeeds.size)
        assertEquals(LocalDate.of(year, month, 1), serviceNeeds.first().startDate)
        assertEquals(LocalDate.of(year, month, 14), serviceNeeds.first().endDate)
    }

    @Test
    fun `updating placement endDate to be later`() {
        val oldPlacement =
            db.transaction {
                    createPlacement(
                        it,
                        childId,
                        unitId,
                        FiniteDateRange(
                            LocalDate.of(year, month, 10),
                            LocalDate.of(year, month, 20)
                        ),
                        PlacementType.DAYCARE,
                        useFiveYearsOldDaycare = true,
                        placeGuarantee = false
                    )
                }
                .first()

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
        val oldPlacement =
            db.transaction {
                    createPlacement(
                        it,
                        childId,
                        unitId,
                        FiniteDateRange(
                            LocalDate.of(year, month, 10),
                            LocalDate.of(year, month, 20)
                        ),
                        PlacementType.DAYCARE,
                        useFiveYearsOldDaycare = true,
                        placeGuarantee = false
                    )
                }
                .first()
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
        db.transaction { it.transferGroup(groupPlacementId, groupId2, transferDate) }

        val groupPlacements =
            db.read {
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

        val groupPlacements =
            db.read {
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
            it.insert(
                DevPlacement(
                    id = daycarePlacementId,
                    type = daycarePlacementType,
                    childId = testChild_2.id,
                    unitId = daycare2.id,
                    startDate = daycarePlacementStartDate,
                    endDate = daycarePlacementEndDate
                )
            )
        }

        val placements = db.read { it.getDetailedDaycarePlacements(daycare2.id, null, null, null) }

        assertEquals(
            setOf(
                DaycarePlacementWithDetails(
                    id = daycarePlacementId,
                    child =
                        ChildBasics(
                            id = testChild_2.id,
                            dateOfBirth = testChild_2.dateOfBirth,
                            socialSecurityNumber = testChild_2.ssn,
                            firstName = testChild_2.firstName,
                            lastName = testChild_2.lastName
                        ),
                    daycare =
                        DaycareBasics(
                            daycare2.id,
                            daycare2.name,
                            area2.name,
                            ProviderType.MUNICIPAL,
                            daycare2.enabledPilotFeatures.toList(),
                            Language.fi
                        ),
                    startDate = daycarePlacementStartDate,
                    endDate = daycarePlacementEndDate,
                    type = daycarePlacementType,
                    missingServiceNeedDays = 6,
                    groupPlacements =
                        listOf(
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
                    defaultServiceNeedOptionNameFi = "Kokopäiväinen",
                    terminatedBy = null,
                    terminationRequestedDate = null,
                    placeGuarantee = false
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
            it.insert(
                DevPlacement(
                    id = daycarePlacementId,
                    type = daycarePlacementType,
                    childId = testChild_2.id,
                    unitId = daycare2.id,
                    startDate = daycarePlacementStartDate,
                    endDate = daycarePlacementEndDate
                )
            )
        }

        val groupPlacementId1 = GroupPlacementId(UUID.randomUUID())
        val groupPlacementId2 = GroupPlacementId(UUID.randomUUID())
        val groupPlacementId3 = GroupPlacementId(UUID.randomUUID())
        val groupPlacementId4 = GroupPlacementId(UUID.randomUUID())
        val groupPlacementId5 = GroupPlacementId(UUID.randomUUID())
        val groupPlacementIds =
            listOf(
                groupPlacementId1,
                groupPlacementId2,
                groupPlacementId3,
                groupPlacementId4,
                groupPlacementId5
            )
        val groupPlacementDays = listOf(3 to 5, 6 to 9, 12 to 12, 16 to 17, 19 to 20)

        db.transaction { tx ->
            groupPlacementDays.forEachIndexed { index, (startDate, endDate) ->
                tx.insert(
                    DevDaycareGroupPlacement(
                        id = groupPlacementIds[index],
                        daycarePlacementId = daycarePlacementId,
                        daycareGroupId = groupId1,
                        startDate = date(startDate),
                        endDate = date(endDate)
                    )
                )
            }
        }

        val placements = db.read { it.getDetailedDaycarePlacements(daycare2.id, null, null, null) }

        assertEquals(
            setOf(
                DaycarePlacementWithDetails(
                    id = daycarePlacementId,
                    child =
                        ChildBasics(
                            id = testChild_2.id,
                            dateOfBirth = testChild_2.dateOfBirth,
                            socialSecurityNumber = testChild_2.ssn,
                            firstName = testChild_2.firstName,
                            lastName = testChild_2.lastName
                        ),
                    daycare =
                        DaycareBasics(
                            daycare2.id,
                            daycare2.name,
                            area2.name,
                            ProviderType.MUNICIPAL,
                            daycare2.enabledPilotFeatures.toList(),
                            Language.fi
                        ),
                    startDate = daycarePlacementStartDate,
                    endDate = daycarePlacementEndDate,
                    type = daycarePlacementType,
                    missingServiceNeedDays = 20,
                    groupPlacements =
                        listOf(
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
                    defaultServiceNeedOptionNameFi = "Kokopäiväinen",
                    terminatedBy = null,
                    terminationRequestedDate = null,
                    placeGuarantee = false
                )
            ),
            placements
        )
    }

    @Test
    fun `only missing group placements after evaka launch are included`() {
        val evakaLaunch = LocalDate.of(2020, 3, 1)
        val placement =
            db.transaction { tx ->
                val placement =
                    tx.insertPlacement(
                        PlacementType.DAYCARE,
                        testChild_1.id,
                        daycare1.id,
                        evakaLaunch.minusYears(1),
                        evakaLaunch.plusYears(1),
                        false
                    )
                tx.insert(
                    DevDaycareGroupPlacement(
                        daycarePlacementId = placement.id,
                        daycareGroupId = groupId1,
                        startDate = evakaLaunch,
                        endDate = evakaLaunch.plusMonths(6)
                    )
                )
                placement
            }

        val result = db.read { tx -> getMissingGroupPlacements(tx, daycare1.id) }
        assertEquals(
            listOf(
                MissingGroupPlacement(
                    placement.id,
                    placement.type,
                    false,
                    FiniteDateRange(placement.startDate, placement.endDate),
                    testChild_1.id,
                    testChild_1.firstName,
                    testChild_1.lastName,
                    testChild_1.dateOfBirth,
                    listOf(),
                    listOf(),
                    "Kokopäiväinen",
                    FiniteDateRange(evakaLaunch.plusMonths(6).plusDays(1), evakaLaunch.plusYears(1))
                )
            ),
            result
        )
    }

    @Test
    fun `only missing backup care group placements after evaka launch are included`() {
        val evakaLaunch = LocalDate.of(2020, 3, 1)
        val backupCareId =
            db.transaction { tx ->
                val placement =
                    tx.insertPlacement(
                        PlacementType.DAYCARE,
                        testChild_1.id,
                        daycare1.id,
                        evakaLaunch.minusYears(1),
                        evakaLaunch.plusYears(1),
                        false
                    )
                tx.insert(
                    DevDaycareGroupPlacement(
                        daycarePlacementId = placement.id,
                        daycareGroupId = groupId1,
                        startDate = placement.startDate,
                        endDate = placement.endDate
                    )
                )
                tx.insert(
                    DevBackupCare(
                        childId = testChild_1.id,
                        unitId = daycare2.id,
                        groupId = null,
                        period =
                            FiniteDateRange(evakaLaunch.minusYears(1), evakaLaunch.minusDays(1))
                    )
                )
                tx.insert(
                    DevBackupCare(
                        childId = testChild_1.id,
                        unitId = daycare2.id,
                        groupId = null,
                        period = FiniteDateRange(evakaLaunch, evakaLaunch.plusYears(1))
                    )
                )
            }

        val result = db.read { tx -> getMissingGroupPlacements(tx, daycare2.id) }
        assertEquals(
            listOf(
                MissingGroupPlacement(
                    PlacementId(backupCareId.raw),
                    null,
                    true,
                    FiniteDateRange(evakaLaunch, evakaLaunch.plusYears(1)),
                    testChild_1.id,
                    testChild_1.firstName,
                    testChild_1.lastName,
                    testChild_1.dateOfBirth,
                    listOf(daycare1.name),
                    listOf(),
                    "",
                    FiniteDateRange(evakaLaunch, evakaLaunch.plusYears(1))
                )
            ),
            result
        )
    }
}
