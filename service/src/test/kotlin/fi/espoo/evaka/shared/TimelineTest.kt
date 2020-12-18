// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared

import fi.espoo.evaka.shared.domain.FiniteDateRange
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TimelineTest {
    @Test
    fun `an empty timeline contains nothing`() {
        val timeline = Timeline.of()
        assertFalse(timeline.contains(FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2020, 1, 1))))
        assertEquals(emptyList<FiniteDateRange>(), timeline.periods().toList())
    }

    @Test
    fun `a period can be added to a timeline`() {
        val period = FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2020, 1, 1))
        val timeline = Timeline.of(period)
        assertTrue(timeline.contains(period))
        assertEquals(listOf(period), timeline.periods().toList())
    }

    @Test
    fun `multiple periods can be added to a timeline`() {
        val periods = listOf(
            FiniteDateRange(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 6, 1)),
            FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 3, 1))
        )
        val timeline = Timeline.of(periods)
        assertTrue(periods.all { timeline.contains(it) })
        assertEquals(periods, timeline.periods().toList())
    }

    @Test
    fun `overlapping periods are merged when added to a timeline`() {
        val periods = listOf(
            FiniteDateRange(LocalDate.of(2018, 1, 1), LocalDate.of(2019, 6, 1)),
            FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2020, 1, 1))
        )
        val timeline = Timeline.of(periods)
        assertTrue(periods.all { timeline.contains(it) })
        assertEquals(listOf(FiniteDateRange(start = periods[0].start, end = periods[1].end)), timeline.periods().toList())
    }

    @Test
    fun `adjacent periods are merged when added to a timeline`() {
        val periods = listOf(
            FiniteDateRange(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 12, 31)),
            FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 5, 31)),
            FiniteDateRange(LocalDate.of(2019, 6, 1), LocalDate.of(2020, 1, 1))
        )
        val timeline = Timeline.of(periods)
        assertTrue(periods.all { timeline.contains(it) })
        assertEquals(listOf(FiniteDateRange(start = periods[0].start, end = periods[2].end)), timeline.periods().toList())
    }

    @Test
    fun `periods can be removed from a timeline`() {
        val periods = listOf(
            FiniteDateRange(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 6, 1)),
            FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 3, 1))
        )
        val timeline = Timeline.of(periods).removeAll(periods)
        assertEquals(emptyList<FiniteDateRange>(), timeline.periods().toList())
    }

    @Test
    fun `parts of periods can be removed from a timeline`() {
        val periods = listOf(
            FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 3, 1)),
            FiniteDateRange(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 6, 1))
        )
        val timeline = Timeline.of(periods).remove(FiniteDateRange(LocalDate.of(2018, 5, 2), LocalDate.of(2019, 1, 31)))
        assertEquals(
            listOf(
                FiniteDateRange(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 5, 1)),
                FiniteDateRange(LocalDate.of(2019, 2, 1), LocalDate.of(2019, 3, 1))
            ),
            timeline.periods().toList()
        )
    }
}
