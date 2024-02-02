// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import fi.espoo.evaka.shared.domain.TimeRange
import java.time.LocalTime
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals

class UsedServiceTests {
    private fun range(h1: Int, h2: Int) = TimeRange(LocalTime.of(h1, 0), LocalTime.of(h2, 0))

    private fun range(hm1: Pair<Int, Int>, hm2: Pair<Int, Int>) =
        TimeRange(LocalTime.of(hm1.first, hm1.second), LocalTime.of(hm2.first, hm2.second))

    @Test
    fun `uses hours divided by 21 if no reservations or attendances exist`() {
        assertEquals(
            UsedService.Average(durationInMinutes = (120.0 * 60 / 21).roundToInt()),
            UsedService.compute(
                serviceNeedHours = 120,
                reservations = listOf(),
                attendances = listOf()
            )
        )
    }

    @Test
    fun `uses reservations if no attendances exist`() {
        assertEquals(
            UsedService.Ranges(listOf(range(9, 12))),
            UsedService.compute(
                serviceNeedHours = 120,
                reservations = listOf(range(9, 12)),
                attendances = listOf()
            )
        )
    }

    @Test
    fun `uses attendances if no reservations exist`() {
        assertEquals(
            UsedService.Ranges(listOf(range(9, 12))),
            UsedService.compute(
                serviceNeedHours = 120,
                reservations = listOf(),
                attendances = listOf(range(9, 12))
            )
        )
    }

    @Test
    fun `supports multiple distinct ranges`() {
        assertEquals(
            UsedService.Ranges(listOf(range(9, 12), range(17, 20))),
            UsedService.compute(
                serviceNeedHours = 120,
                reservations = listOf(range(9, 12), range(17, 20)),
                attendances = listOf()
            )
        )
        assertEquals(
            UsedService.Ranges(listOf(range(9, 12), range(17, 20))),
            UsedService.compute(
                serviceNeedHours = 120,
                reservations = listOf(range(9, 12)),
                attendances = listOf(range(17, 20))
            )
        )
        assertEquals(
            UsedService.Ranges(listOf(range(9, 12), range(17, 20))),
            UsedService.compute(
                serviceNeedHours = 120,
                reservations = listOf(),
                attendances = listOf(range(9, 12), range(17, 20))
            )
        )
    }

    @Test
    fun `merges adjacent and overlapping ranges`() {
        assertEquals(
            UsedService.Ranges(listOf(range(9, 15))),
            UsedService.compute(
                serviceNeedHours = 120,
                reservations = listOf(range(9, 12)),
                attendances = listOf(range(11, 14), range(14, 15))
            )
        )
    }
}
