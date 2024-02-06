// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.domain.LocalHm
import fi.espoo.evaka.shared.domain.LocalHmRange
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals

class UsedServiceTests {
    private fun range(h1: Int, h2: Int) = LocalHmRange(LocalHm(h1, 0), LocalHm(h2, 0))

    private fun compute(
        serviceNeedHours: Int = 120,
        placementType: PlacementType = PlacementType.DAYCARE,
        absences: List<AbsenceCategory> = listOf(),
        reservations: List<LocalHmRange> = listOf(),
        attendances: List<LocalHmRange> = listOf()
    ) = UsedService.compute(serviceNeedHours, placementType, absences, reservations, attendances)

    @Test
    fun `uses hours divided by 21 if no absences, reservations or attendances exist`() {
        assertEquals(
            UsedService.Average(durationInMinutes = (120.0 * 60 / 21).roundToInt()),
            compute(absences = listOf(), reservations = listOf(), attendances = listOf())
        )
    }

    @Test
    fun `uses reservations if no attendances exist`() {
        assertEquals(
            UsedService.Ranges(listOf(this.range(9, 12))),
            compute(reservations = listOf(this.range(9, 12)))
        )
    }

    @Test
    fun `uses attendances if no reservations exist`() {
        assertEquals(
            UsedService.Ranges(listOf(this.range(9, 12))),
            compute(attendances = listOf(this.range(9, 12)))
        )
    }

    @Test
    fun `supports multiple distinct ranges`() {
        assertEquals(
            UsedService.Ranges(listOf(this.range(9, 12), this.range(17, 20))),
            compute(reservations = listOf(this.range(9, 12), this.range(17, 20)))
        )
        assertEquals(
            UsedService.Ranges(listOf(this.range(9, 12), this.range(17, 20))),
            compute(
                reservations = listOf(this.range(9, 12)),
                attendances = listOf(this.range(17, 20))
            )
        )
        assertEquals(
            UsedService.Ranges(listOf(this.range(9, 12), this.range(17, 20))),
            compute(attendances = listOf(this.range(9, 12), this.range(17, 20)))
        )
    }

    @Test
    fun `merges adjacent and overlapping ranges`() {
        assertEquals(
            UsedService.Ranges(listOf(this.range(9, 15))),
            compute(
                reservations = listOf(this.range(9, 12)),
                attendances = listOf(this.range(11, 14), this.range(14, 15))
            )
        )
    }

    @Test
    fun `returns no used service for full-day absences`() {
        assertEquals(
            UsedService.Ranges(listOf()),
            compute(
                placementType = PlacementType.DAYCARE,
                absences = listOf(AbsenceCategory.BILLABLE)
            )
        )
        assertEquals(
            UsedService.Ranges(listOf()),
            compute(
                placementType = PlacementType.PRESCHOOL_DAYCARE,
                absences = listOf(AbsenceCategory.BILLABLE, AbsenceCategory.NONBILLABLE)
            )
        )
    }
}
