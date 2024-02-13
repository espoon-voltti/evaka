// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.domain.TimeRange
import java.time.LocalTime
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class UsedServiceTests {
    private fun range(h1: Int, h2: Int) = TimeRange.of(LocalTime.of(h1, 0), LocalTime.of(h2, 0))

    private fun compute(
        isFuture: Boolean = false,
        serviceNeedHours: Int = 120,
        placementType: PlacementType = PlacementType.DAYCARE,
        dailyPreschoolTimes: TimeRange? = null,
        dailyPreparatoryTimes: TimeRange? = null,
        absences: List<AbsenceCategory> = listOf(),
        reservations: List<TimeRange> = listOf(),
        attendances: List<TimeRange> = listOf()
    ) =
        computeUsedService(
            isFuture,
            serviceNeedHours,
            placementType,
            dailyPreschoolTimes,
            dailyPreparatoryTimes,
            absences,
            reservations,
            attendances
        )

    @Test
    fun `computes only reserved minutes for future days`() {
        compute(isFuture = true, reservations = listOf(range(9, 12))).also {
            assertEquals(180, it.reservedMinutes)
            assertEquals(0, it.usedServiceMinutes)
            assertEquals(emptyList(), it.usedServiceRanges)
        }
    }

    @Test
    fun `uses hours divided by 21 if no absences, reservations or attendances exist`() {
        compute(absences = listOf(), reservations = listOf(), attendances = listOf()).also {
            assertEquals(it.usedServiceMinutes, (120.0 * 60 / 21).roundToInt())
            assertEquals(it.usedServiceRanges, emptyList())
        }
    }

    @Test
    fun `uses reservations if no attendances exist`() {
        compute(reservations = listOf(range(9, 12))).also {
            assertEquals(it.usedServiceRanges, listOf(range(9, 12)))
        }
    }

    @Test
    fun `uses attendances if no reservations exist`() {
        compute(attendances = listOf(range(9, 12))).also {
            assertEquals(it.usedServiceRanges, listOf(range(9, 12)))
        }
    }

    @Test
    fun `supports multiple distinct ranges`() {
        compute(reservations = listOf(range(9, 12), range(17, 20))).also {
            assertEquals(it.usedServiceRanges, listOf(range(9, 12), range(17, 20)))
        }
        compute(reservations = listOf(range(9, 12)), attendances = listOf(range(17, 20))).also {
            assertEquals(it.usedServiceRanges, listOf(range(9, 12), range(17, 20)))
        }
        compute(attendances = listOf(range(9, 12), range(17, 20))).also {
            assertEquals(it.usedServiceRanges, listOf(range(9, 12), range(17, 20)))
        }
    }

    @Test
    fun `merges adjacent and overlapping ranges`() {
        compute(
                reservations = listOf(range(9, 12)),
                attendances = listOf(range(11, 14), range(14, 15))
            )
            .also { assertEquals(it.usedServiceRanges, listOf(range(9, 15))) }
    }

    @Test
    fun `returns no used service for full-day absences`() {
        compute(placementType = PlacementType.DAYCARE, absences = listOf(AbsenceCategory.BILLABLE))
            .also { assertEquals(it.usedServiceRanges, listOf()) }
        compute(
                placementType = PlacementType.PRESCHOOL_DAYCARE,
                absences = listOf(AbsenceCategory.BILLABLE, AbsenceCategory.NONBILLABLE)
            )
            .also { assertEquals(it.usedServiceRanges, listOf()) }
    }

    @ParameterizedTest(name = "preschool times should be excluded when placement type is {0}")
    @EnumSource(
        value = PlacementType::class,
        names = ["PRESCHOOL", "PRESCHOOL_CLUB", "PRESCHOOL_DAYCARE", "PRESCHOOL_DAYCARE_ONLY"]
    )
    fun `preschool times should be excluded`(placementType: PlacementType) {
        compute(
                placementType = placementType,
                dailyPreschoolTimes = range(9, 13),
                dailyPreparatoryTimes = range(9, 14),
                reservations = listOf(range(8, 16)),
                attendances = listOf(range(9, 17))
            )
            .also { assertEquals(it.usedServiceRanges, listOf(range(8, 9), range(13, 17))) }
    }

    @ParameterizedTest(name = "preparatory times should be excluded when placement type is {0}")
    @EnumSource(
        value = PlacementType::class,
        names = ["PREPARATORY", "PREPARATORY_DAYCARE", "PREPARATORY_DAYCARE_ONLY"]
    )
    fun `preparatory times should be excluded`(placementType: PlacementType) {
        compute(
                placementType = placementType,
                dailyPreschoolTimes = range(9, 13),
                dailyPreparatoryTimes = range(9, 14),
                reservations = listOf(range(8, 16)),
                attendances = listOf(range(9, 17))
            )
            .also { assertEquals(it.usedServiceRanges, listOf(range(8, 9), range(14, 17))) }
    }

    @Test
    fun `preschool or preparatory times should not be excluded when placement type is daycare`() {
        compute(
                placementType = PlacementType.DAYCARE,
                dailyPreschoolTimes = range(9, 13),
                dailyPreparatoryTimes = range(9, 14),
                reservations = listOf(range(8, 16)),
                attendances = listOf(range(9, 17))
            )
            .also { assertEquals(it.usedServiceRanges, listOf(range(8, 17))) }
    }
}
