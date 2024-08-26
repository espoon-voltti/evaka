// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.daycare.PreschoolTerm
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.PreschoolTermId
import fi.espoo.evaka.shared.data.DateSet
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.TimeRange
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class AbsenceCalculatorTests {
    val today = LocalDate.of(2023, 9, 14)
    val termBreakDay = LocalDate.of(2024, 1, 3)

    @Test
    fun `placement PRESCHOOL`() {
        assertEquals(
            setOf(AbsenceCategory.NONBILLABLE),
            testGetExpectedAbsenceCategories(
                placementType = PlacementType.PRESCHOOL,
                attendanceTimes = listOf(),
            ),
        )
        assertEquals(
            null,
            testGetExpectedAbsenceCategories(
                date = termBreakDay,
                placementType = PlacementType.PRESCHOOL,
                attendanceTimes = listOf(),
            ),
        )
        assertEquals(
            setOf(AbsenceCategory.NONBILLABLE),
            testGetExpectedAbsenceCategories(
                placementType = PlacementType.PRESCHOOL,
                attendanceTimes = listOf(TimeRange(LocalTime.of(8, 0), LocalTime.of(9, 30))),
            ),
        )
        assertEquals(
            setOf(),
            testGetExpectedAbsenceCategories(
                placementType = PlacementType.PRESCHOOL,
                attendanceTimes = listOf(TimeRange(LocalTime.of(9, 10), LocalTime.of(12, 45))),
            ),
        )
    }

    @Test
    fun `placement PRESCHOOL_DAYCARE`() {
        assertEquals(
            setOf(AbsenceCategory.NONBILLABLE, AbsenceCategory.BILLABLE),
            testGetExpectedAbsenceCategories(
                placementType = PlacementType.PRESCHOOL_DAYCARE,
                attendanceTimes = listOf(),
            ),
        )
        assertEquals(
            setOf(AbsenceCategory.BILLABLE),
            testGetExpectedAbsenceCategories(
                date = termBreakDay,
                placementType = PlacementType.PRESCHOOL_DAYCARE,
                attendanceTimes = listOf(),
            ),
        )
        assertEquals(
            setOf(AbsenceCategory.NONBILLABLE),
            testGetExpectedAbsenceCategories(
                placementType = PlacementType.PRESCHOOL_DAYCARE,
                attendanceTimes = listOf(TimeRange(LocalTime.of(8, 0), LocalTime.of(9, 30))),
            ),
        )
        assertEquals(
            setOf(AbsenceCategory.BILLABLE),
            testGetExpectedAbsenceCategories(
                placementType = PlacementType.PRESCHOOL_DAYCARE,
                attendanceTimes = listOf(TimeRange(LocalTime.of(9, 10), LocalTime.of(12, 45))),
            ),
        )
        assertEquals(
            setOf(),
            testGetExpectedAbsenceCategories(
                placementType = PlacementType.PRESCHOOL_DAYCARE,
                attendanceTimes = listOf(TimeRange(LocalTime.of(8, 10), LocalTime.of(13, 45))),
            ),
        )
    }

    @Test
    fun `placement PREPARATORY`() {
        assertEquals(
            setOf(AbsenceCategory.NONBILLABLE),
            testGetExpectedAbsenceCategories(
                placementType = PlacementType.PREPARATORY,
                attendanceTimes = listOf(),
            ),
        )
        assertEquals(
            null,
            testGetExpectedAbsenceCategories(
                date = termBreakDay,
                placementType = PlacementType.PREPARATORY,
                attendanceTimes = listOf(),
            ),
        )
        assertEquals(
            setOf(AbsenceCategory.NONBILLABLE),
            testGetExpectedAbsenceCategories(
                placementType = PlacementType.PREPARATORY,
                attendanceTimes = listOf(TimeRange(LocalTime.of(8, 0), LocalTime.of(9, 30))),
            ),
        )
        assertEquals(
            setOf(),
            testGetExpectedAbsenceCategories(
                placementType = PlacementType.PREPARATORY,
                attendanceTimes = listOf(TimeRange(LocalTime.of(9, 10), LocalTime.of(13, 45))),
            ),
        )
    }

    @Test
    fun `placement PREPARATORY_DAYCARE`() {
        assertEquals(
            setOf(AbsenceCategory.NONBILLABLE, AbsenceCategory.BILLABLE),
            testGetExpectedAbsenceCategories(
                placementType = PlacementType.PREPARATORY_DAYCARE,
                attendanceTimes = listOf(),
            ),
        )
        assertEquals(
            setOf(AbsenceCategory.BILLABLE),
            testGetExpectedAbsenceCategories(
                date = termBreakDay,
                placementType = PlacementType.PREPARATORY_DAYCARE,
                attendanceTimes = listOf(),
            ),
        )
        assertEquals(
            setOf(AbsenceCategory.NONBILLABLE),
            testGetExpectedAbsenceCategories(
                placementType = PlacementType.PREPARATORY_DAYCARE,
                attendanceTimes = listOf(TimeRange(LocalTime.of(8, 0), LocalTime.of(9, 30))),
            ),
        )
        assertEquals(
            setOf(AbsenceCategory.BILLABLE),
            testGetExpectedAbsenceCategories(
                placementType = PlacementType.PREPARATORY_DAYCARE,
                attendanceTimes = listOf(TimeRange(LocalTime.of(9, 10), LocalTime.of(13, 45))),
            ),
        )
        assertEquals(
            setOf(),
            testGetExpectedAbsenceCategories(
                placementType = PlacementType.PREPARATORY_DAYCARE,
                attendanceTimes = listOf(TimeRange(LocalTime.of(8, 10), LocalTime.of(14, 45))),
            ),
        )
    }

    @Test
    fun `placement DAYCARE`() {
        assertEquals(
            setOf(AbsenceCategory.BILLABLE),
            testGetExpectedAbsenceCategories(
                placementType = PlacementType.DAYCARE,
                attendanceTimes = listOf(),
            ),
        )
        assertEquals(
            setOf(AbsenceCategory.BILLABLE),
            testGetExpectedAbsenceCategories(
                placementType = PlacementType.DAYCARE,
                attendanceTimes = listOf(TimeRange(LocalTime.of(9, 0), LocalTime.of(9, 10))),
            ),
        )
        assertEquals(
            setOf(),
            testGetExpectedAbsenceCategories(
                placementType = PlacementType.DAYCARE,
                attendanceTimes = listOf(TimeRange(LocalTime.of(9, 0), LocalTime.of(9, 30))),
            ),
        )
    }

    @Test
    fun `placement DAYCARE_FIVE_YEAR_OLDS`() {
        assertEquals(
            setOf(AbsenceCategory.NONBILLABLE, AbsenceCategory.BILLABLE),
            testGetExpectedAbsenceCategories(
                placementType = PlacementType.DAYCARE_FIVE_YEAR_OLDS,
                attendanceTimes = listOf(),
            ),
        )
        assertEquals(
            setOf(AbsenceCategory.BILLABLE),
            testGetExpectedAbsenceCategories(
                placementType = PlacementType.DAYCARE_FIVE_YEAR_OLDS,
                attendanceTimes = listOf(TimeRange(LocalTime.of(9, 0), LocalTime.of(13, 5))),
            ),
        )
        assertEquals(
            setOf(),
            testGetExpectedAbsenceCategories(
                placementType = PlacementType.DAYCARE_FIVE_YEAR_OLDS,
                attendanceTimes = listOf(TimeRange(LocalTime.of(9, 0), LocalTime.of(13, 30))),
            ),
        )
    }

    private fun testGetExpectedAbsenceCategories(
        placementType: PlacementType,
        attendanceTimes: List<TimeRange>,
        date: LocalDate = today.minusDays(1),
    ): Set<AbsenceCategory>? =
        getExpectedAbsenceCategories(
            date = date,
            attendanceTimes = attendanceTimes,
            placementType = placementType,
            unitLanguage = Language.fi,
            dailyPreschoolTime = TimeRange(LocalTime.of(9, 0), LocalTime.of(13, 0)),
            dailyPreparatoryTime = TimeRange(LocalTime.of(9, 0), LocalTime.of(14, 0)),
            preschoolTerms =
                listOf(
                    PreschoolTerm(
                        id = PreschoolTermId(UUID.randomUUID()),
                        finnishPreschool =
                            FiniteDateRange(LocalDate.of(2023, 8, 15), LocalDate.of(2024, 5, 31)),
                        swedishPreschool =
                            FiniteDateRange(LocalDate.of(2023, 8, 15), LocalDate.of(2024, 5, 31)),
                        extendedTerm =
                            FiniteDateRange(LocalDate.of(2023, 8, 1), LocalDate.of(2024, 5, 31)),
                        applicationPeriod =
                            FiniteDateRange(LocalDate.of(2023, 1, 1), LocalDate.of(2024, 5, 31)),
                        termBreaks =
                            DateSet.of(
                                FiniteDateRange(
                                    LocalDate.of(2023, 12, 18),
                                    LocalDate.of(2024, 1, 5),
                                )
                            ),
                    )
                ),
        )
}
