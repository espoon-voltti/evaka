// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import java.time.LocalDate
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FiveYearOldDaycarePlacementsIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {

    private val childId = testChild_1.id
    private val yearChildTurnsFive = testChild_1.dateOfBirth.plusYears(5).year

    @BeforeEach
    fun beforeEach() {
        db.transaction { it.insertGeneralTestFixtures() }
    }

    @Test
    fun `new daycare placement gets split into 5-year-old periods`() {
        val startDate = LocalDate.of(yearChildTurnsFive - 1, 8, 1)
        val endDate = LocalDate.of(yearChildTurnsFive + 2, 7, 31)

        val newPlacements =
            db.transaction {
                createPlacement(
                    it,
                    childId,
                    testDaycare.id,
                    FiniteDateRange(startDate, endDate),
                    PlacementType.DAYCARE,
                    useFiveYearsOldDaycare = true
                )
            }

        assertEquals(3, newPlacements.size)
        newPlacements[0].let { placement ->
            assertEquals(PlacementType.DAYCARE, placement.type)
            assertEquals(startDate, placement.startDate)
            assertEquals(LocalDate.of(yearChildTurnsFive, 7, 31), placement.endDate)
        }
        newPlacements[1].let { placement ->
            assertEquals(PlacementType.DAYCARE_FIVE_YEAR_OLDS, placement.type)
            assertEquals(LocalDate.of(yearChildTurnsFive, 8, 1), placement.startDate)
            assertEquals(LocalDate.of(yearChildTurnsFive + 1, 7, 31), placement.endDate)
        }
        newPlacements[2].let { placement ->
            assertEquals(PlacementType.DAYCARE, placement.type)
            assertEquals(LocalDate.of(yearChildTurnsFive + 1, 8, 1), placement.startDate)
            assertEquals(endDate, placement.endDate)
        }
    }

    @Test
    fun `new daycare placement does not get split into 5-year-old periods if feature is disabled`() {
        val startDate = LocalDate.of(yearChildTurnsFive - 1, 8, 1)
        val endDate = LocalDate.of(yearChildTurnsFive + 2, 7, 31)

        val newPlacements =
            db.transaction {
                createPlacement(
                    it,
                    childId,
                    testDaycare.id,
                    FiniteDateRange(startDate, endDate),
                    PlacementType.DAYCARE,
                    useFiveYearsOldDaycare = false
                )
            }

        assertEquals(1, newPlacements.size)
        newPlacements[0].let { placement ->
            assertEquals(PlacementType.DAYCARE, placement.type)
            assertEquals(startDate, placement.startDate)
            assertEquals(endDate, placement.endDate)
        }
    }

    @Test
    fun `new part day daycare placement gets a correct 5-year-old placement type`() {
        val startDate = LocalDate.of(yearChildTurnsFive, 8, 1)
        val endDate = LocalDate.of(yearChildTurnsFive + 1, 7, 31)

        val newPlacements =
            db.transaction {
                createPlacement(
                    it,
                    childId,
                    testDaycare.id,
                    FiniteDateRange(startDate, endDate),
                    PlacementType.DAYCARE_PART_TIME,
                    useFiveYearsOldDaycare = true
                )
            }

        assertEquals(1, newPlacements.size)
        newPlacements.first().let { placement ->
            assertEquals(PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS, placement.type)
            assertEquals(startDate, placement.startDate)
            assertEquals(endDate, placement.endDate)
        }
    }

    @Test
    fun `extending a daycare placement to 5-year-old period starts a new placement`() {
        val originalStartDate = LocalDate.of(yearChildTurnsFive, 1, 1)
        val originalEndDate = LocalDate.of(yearChildTurnsFive, 5, 31)

        val oldPlacement =
            db.transaction {
                    createPlacement(
                        it,
                        childId,
                        testDaycare.id,
                        FiniteDateRange(originalStartDate, originalEndDate),
                        PlacementType.DAYCARE,
                        useFiveYearsOldDaycare = true
                    )
                }
                .first()

        val newEndDate = LocalDate.of(yearChildTurnsFive, 8, 31)
        db.transaction {
            it.updatePlacement(
                oldPlacement.id,
                originalStartDate,
                newEndDate,
                useFiveYearsOldDaycare = true
            )
        }

        val placements = db.read { it.getPlacementsForChild(childId) }.sortedBy { it.startDate }
        assertEquals(2, placements.size)
        placements.first().let { placement ->
            assertEquals(PlacementType.DAYCARE, placement.type)
            assertEquals(originalStartDate, placement.startDate)
            assertEquals(LocalDate.of(yearChildTurnsFive, 7, 31), placement.endDate)
        }
        placements.last().let { placement ->
            assertEquals(PlacementType.DAYCARE_FIVE_YEAR_OLDS, placement.type)
            assertEquals(LocalDate.of(yearChildTurnsFive, 8, 1), placement.startDate)
            assertEquals(newEndDate, placement.endDate)
        }
    }

    @Test
    fun `extending a 5-year-old daycare placement to beyond the period starts a new placement`() {
        val originalStartDate = LocalDate.of(yearChildTurnsFive + 1, 1, 1)
        val originalEndDate = LocalDate.of(yearChildTurnsFive + 1, 5, 31)

        val oldPlacement =
            db.transaction {
                    createPlacement(
                        it,
                        childId,
                        testDaycare.id,
                        FiniteDateRange(originalStartDate, originalEndDate),
                        PlacementType.DAYCARE,
                        useFiveYearsOldDaycare = true
                    )
                }
                .first()

        val newEndDate = LocalDate.of(yearChildTurnsFive + 1, 8, 31)
        db.transaction {
            it.updatePlacement(
                oldPlacement.id,
                originalStartDate,
                newEndDate,
                useFiveYearsOldDaycare = true
            )
        }

        val placements = db.read { it.getPlacementsForChild(childId) }.sortedBy { it.startDate }
        assertEquals(2, placements.size)
        placements.first().let { placement ->
            assertEquals(PlacementType.DAYCARE_FIVE_YEAR_OLDS, placement.type)
            assertEquals(originalStartDate, placement.startDate)
            assertEquals(LocalDate.of(yearChildTurnsFive + 1, 7, 31), placement.endDate)
        }
        placements.last().let { placement ->
            assertEquals(PlacementType.DAYCARE, placement.type)
            assertEquals(LocalDate.of(yearChildTurnsFive + 1, 8, 1), placement.startDate)
            assertEquals(newEndDate, placement.endDate)
        }
    }

    @Test
    fun `moving a daycare placement start date to 5-year-old period starts a new placement`() {
        val originalStartDate = LocalDate.of(yearChildTurnsFive + 1, 8, 1)
        val originalEndDate = LocalDate.of(yearChildTurnsFive + 1, 8, 31)

        val oldPlacement =
            db.transaction {
                    createPlacement(
                        it,
                        childId,
                        testDaycare.id,
                        FiniteDateRange(originalStartDate, originalEndDate),
                        PlacementType.DAYCARE,
                        useFiveYearsOldDaycare = true
                    )
                }
                .first()

        val newStartDate = LocalDate.of(yearChildTurnsFive + 1, 7, 1)
        db.transaction {
            it.updatePlacement(
                oldPlacement.id,
                newStartDate,
                originalEndDate,
                useFiveYearsOldDaycare = true
            )
        }

        val placements = db.read { it.getPlacementsForChild(childId) }.sortedBy { it.startDate }
        assertEquals(2, placements.size)
        placements.first().let { placement ->
            assertEquals(PlacementType.DAYCARE_FIVE_YEAR_OLDS, placement.type)
            assertEquals(newStartDate, placement.startDate)
            assertEquals(LocalDate.of(yearChildTurnsFive + 1, 7, 31), placement.endDate)
        }
        placements.last().let { placement ->
            assertEquals(PlacementType.DAYCARE, placement.type)
            assertEquals(LocalDate.of(yearChildTurnsFive + 1, 8, 1), placement.startDate)
            assertEquals(originalEndDate, placement.endDate)
        }
    }

    @Test
    fun `moving a 5-year-old daycare placement start date outside the period starts a new placement`() {
        val originalStartDate = LocalDate.of(yearChildTurnsFive, 8, 1)
        val originalEndDate = LocalDate.of(yearChildTurnsFive, 8, 31)

        val oldPlacement =
            db.transaction {
                    createPlacement(
                        it,
                        childId,
                        testDaycare.id,
                        FiniteDateRange(originalStartDate, originalEndDate),
                        PlacementType.DAYCARE,
                        useFiveYearsOldDaycare = true
                    )
                }
                .first()

        val newStartDate = LocalDate.of(yearChildTurnsFive, 7, 1)
        db.transaction {
            it.updatePlacement(
                oldPlacement.id,
                newStartDate,
                originalEndDate,
                useFiveYearsOldDaycare = true
            )
        }

        val placements = db.read { it.getPlacementsForChild(childId) }.sortedBy { it.startDate }
        assertEquals(2, placements.size)
        placements.first().let { placement ->
            assertEquals(PlacementType.DAYCARE, placement.type)
            assertEquals(newStartDate, placement.startDate)
            assertEquals(LocalDate.of(yearChildTurnsFive, 7, 31), placement.endDate)
        }
        placements.last().let { placement ->
            assertEquals(PlacementType.DAYCARE_FIVE_YEAR_OLDS, placement.type)
            assertEquals(LocalDate.of(yearChildTurnsFive, 8, 1), placement.startDate)
            assertEquals(originalEndDate, placement.endDate)
        }
    }

    @Test
    fun `moving a daycare placement to 5-year-old period changes the placements type`() {
        val originalStartDate = LocalDate.of(yearChildTurnsFive, 7, 1)
        val originalEndDate = LocalDate.of(yearChildTurnsFive, 7, 31)

        val oldPlacement =
            db.transaction {
                    createPlacement(
                        it,
                        childId,
                        testDaycare.id,
                        FiniteDateRange(originalStartDate, originalEndDate),
                        PlacementType.DAYCARE,
                        useFiveYearsOldDaycare = true
                    )
                }
                .first()

        val newStartDate = LocalDate.of(yearChildTurnsFive, 8, 1)
        val newEndDate = LocalDate.of(yearChildTurnsFive, 8, 31)
        db.transaction {
            it.updatePlacement(
                oldPlacement.id,
                newStartDate,
                newEndDate,
                useFiveYearsOldDaycare = true
            )
        }

        val placements = db.read { it.getPlacementsForChild(childId) }.sortedBy { it.startDate }
        assertEquals(1, placements.size)
        placements.first().let { placement ->
            assertEquals(PlacementType.DAYCARE_FIVE_YEAR_OLDS, placement.type)
            assertEquals(newStartDate, placement.startDate)
            assertEquals(newEndDate, placement.endDate)
        }
    }

    @Test
    fun `moving a daycare placement to 5-year-old period does not change the placement type if feature is disabled`() {
        val originalStartDate = LocalDate.of(yearChildTurnsFive, 7, 1)
        val originalEndDate = LocalDate.of(yearChildTurnsFive, 7, 31)

        val oldPlacement =
            db.transaction {
                    createPlacement(
                        it,
                        childId,
                        testDaycare.id,
                        FiniteDateRange(originalStartDate, originalEndDate),
                        PlacementType.DAYCARE,
                        useFiveYearsOldDaycare = false
                    )
                }
                .first()

        val newStartDate = LocalDate.of(yearChildTurnsFive, 8, 1)
        val newEndDate = LocalDate.of(yearChildTurnsFive, 8, 31)
        db.transaction {
            it.updatePlacement(
                oldPlacement.id,
                newStartDate,
                newEndDate,
                useFiveYearsOldDaycare = false
            )
        }

        val placements = db.read { it.getPlacementsForChild(childId) }.sortedBy { it.startDate }
        assertEquals(1, placements.size)
        placements.first().let { placement ->
            assertEquals(PlacementType.DAYCARE, placement.type)
            assertEquals(newStartDate, placement.startDate)
            assertEquals(newEndDate, placement.endDate)
        }
    }
}
