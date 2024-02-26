// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.domain.TimeRange
import java.time.LocalTime
import kotlin.math.roundToLong
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class UsedServiceTests {
    private fun range(h1: Int, h2: Int) = TimeRange(LocalTime.of(h1, 0), LocalTime.of(h2, 0))

    private fun compute(
        isFuture: Boolean = false,
        serviceNeedHours: Int = 120,
        placementType: PlacementType = PlacementType.DAYCARE,
        dailyPreschoolTimes: TimeRange? = null,
        dailyPreparatoryTimes: TimeRange? = null,
        absences: List<Pair<AbsenceType, AbsenceCategory>> = listOf(),
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
            assertEquals(it.usedServiceMinutes, (120.0 * 60 / 21).roundToLong())
            assertEquals(it.usedServiceRanges, emptyList())
        }

        compute(
                placementType = PlacementType.DAYCARE_FIVE_YEAR_OLDS,
                absences = listOf(),
                reservations = listOf(),
                attendances = listOf()
            )
            .also {
                // Five-year-olds get 4 hours for free
                assertEquals((120.0 * 60 / 21).roundToLong() - 4 * 60, it.usedServiceMinutes)
                assertEquals(emptyList(), it.usedServiceRanges)
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
    fun `returns no used service for full-day planned absences`() {
        compute(
                placementType = PlacementType.DAYCARE,
                absences = listOf(AbsenceType.PLANNED_ABSENCE to AbsenceCategory.BILLABLE)
            )
            .also { assertEquals(it.usedServiceRanges, listOf()) }
        compute(
                placementType = PlacementType.PRESCHOOL_DAYCARE,
                absences =
                    listOf(
                        AbsenceType.PLANNED_ABSENCE to AbsenceCategory.BILLABLE,
                        AbsenceType.PLANNED_ABSENCE to AbsenceCategory.NONBILLABLE
                    )
            )
            .also { assertEquals(it.usedServiceRanges, listOf()) }
    }

    @Test
    fun `uses hours divided by 21 if other than planned absences exist without reservation`() {
        compute(absences = listOf(AbsenceType.OTHER_ABSENCE to AbsenceCategory.BILLABLE)).also {
            assertEquals(it.usedServiceMinutes, (120.0 * 60 / 21).roundToLong())
            assertEquals(it.usedServiceRanges, emptyList())
        }
    }

    @Test
    fun `uses reserved time even if non-planned absences exist`() {
        compute(
                reservations = listOf(range(8, 16)),
                absences = listOf(AbsenceType.OTHER_ABSENCE to AbsenceCategory.BILLABLE)
            )
            .also {
                assertEquals(it.usedServiceMinutes, 480)
                assertEquals(it.usedServiceRanges, listOf(range(8, 16)))
            }
    }

    @ParameterizedTest(name = "preschool times should be excluded when placement type is {0}")
    @EnumSource(
        value = PlacementType::class,
        names = ["PRESCHOOL", "PRESCHOOL_CLUB", "PRESCHOOL_DAYCARE", "PRESCHOOL_DAYCARE_ONLY"]
    )
    fun `preschool times are excluded`(placementType: PlacementType) {
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
    fun `preparatory times are excluded`(placementType: PlacementType) {
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
    fun `reservations are used with preschool times excluded preschool if absences are marked`() {
        compute(
                placementType = PlacementType.PRESCHOOL_DAYCARE,
                dailyPreschoolTimes = range(9, 13),
                dailyPreparatoryTimes = range(9, 14),
                absences = listOf(AbsenceType.OTHER_ABSENCE to AbsenceCategory.BILLABLE),
                reservations = listOf(range(8, 16)),
                attendances = listOf(range(9, 13)),
            )
            .also { assertEquals(it.usedServiceRanges, listOf(range(8, 9), range(13, 16))) }
    }

    @Test
    fun `no service is used if the child attends only preschool`() {
        // No absence marked
        compute(
                placementType = PlacementType.PRESCHOOL_DAYCARE,
                dailyPreschoolTimes = range(9, 13),
                dailyPreparatoryTimes = range(9, 14),
                reservations = emptyList(),
                attendances = listOf(range(9, 13))
            )
            .also { assertEquals(it.usedServiceRanges, emptyList()) }

        // Absence marked for the billable time
        compute(
                placementType = PlacementType.PRESCHOOL_DAYCARE,
                dailyPreschoolTimes = range(9, 13),
                dailyPreparatoryTimes = range(9, 14),
                reservations = emptyList(),
                absences = listOf(AbsenceType.OTHER_ABSENCE to AbsenceCategory.BILLABLE),
                attendances = listOf(range(9, 13)),
            )
            .also { assertEquals(it.usedServiceRanges, emptyList()) }
    }

    @Test
    fun `preschool or preparatory times are not excluded when placement type is daycare`() {
        compute(
                placementType = PlacementType.DAYCARE,
                dailyPreschoolTimes = range(9, 13),
                dailyPreparatoryTimes = range(9, 14),
                reservations = listOf(range(8, 16)),
                attendances = listOf(range(9, 17))
            )
            .also { assertEquals(it.usedServiceRanges, listOf(range(8, 17))) }
    }

    @Test
    fun `5 year olds with connected daycare get 4 hours for free`() {
        compute(
                placementType = PlacementType.DAYCARE_FIVE_YEAR_OLDS,
                reservations = listOf(range(8, 16)),
                attendances = listOf(range(9, 17))
            )
            .also {
                assertEquals(5 * 60, it.usedServiceMinutes) // 9 hours - 4 hours = 5 hours
                assertEquals(listOf(range(8, 17)), it.usedServiceRanges)
            }

        compute(
                placementType = PlacementType.DAYCARE_FIVE_YEAR_OLDS,
                attendances = listOf(range(10, 13))
            )
            .also {
                assertEquals(0, it.usedServiceMinutes) // Will not be negative
                assertEquals(listOf(range(10, 13)), it.usedServiceRanges)
            }
    }

    @Test
    fun `5 year olds with part time do not get anything for free`() {
        compute(
                placementType = PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS,
                reservations = listOf(range(8, 16)),
                attendances = listOf(range(9, 17))
            )
            .also {
                assertEquals(9 * 60, it.usedServiceMinutes)
                assertEquals(listOf(range(8, 17)), it.usedServiceRanges)
            }
    }
}
