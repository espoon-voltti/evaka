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
        assertEquals(emptyList<FiniteDateRange>(), timeline.ranges().toList())
    }

    @Test
    fun `a period can be added to a timeline`() {
        val period = FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2020, 1, 1))
        val timeline = Timeline.of(period)
        assertTrue(timeline.contains(period))
        assertEquals(listOf(period), timeline.ranges().toList())
    }

    @Test
    fun `multiple ranges can be added to a timeline`() {
        val ranges = listOf(
            FiniteDateRange(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 6, 1)),
            FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 3, 1))
        )
        val timeline = Timeline.of(ranges)
        assertTrue(ranges.all { timeline.contains(it) })
        assertEquals(ranges, timeline.ranges().toList())
    }

    @Test
    fun `overlapping ranges are merged when added to a timeline`() {
        val ranges = listOf(
            FiniteDateRange(LocalDate.of(2018, 1, 1), LocalDate.of(2019, 6, 1)),
            FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2020, 1, 1))
        )
        val timeline = Timeline.of(ranges)
        assertTrue(ranges.all { timeline.contains(it) })
        assertEquals(listOf(FiniteDateRange(start = ranges[0].start, end = ranges[1].end)), timeline.ranges().toList())
    }

    @Test
    fun `adjacent ranges are merged when added to a timeline`() {
        val ranges = listOf(
            FiniteDateRange(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 12, 31)),
            FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 5, 31)),
            FiniteDateRange(LocalDate.of(2019, 6, 1), LocalDate.of(2020, 1, 1))
        )
        val timeline = Timeline.of(ranges)
        assertTrue(ranges.all { timeline.contains(it) })
        assertEquals(listOf(FiniteDateRange(start = ranges[0].start, end = ranges[2].end)), timeline.ranges().toList())
    }

    @Test
    fun `ranges can be removed from a timeline`() {
        val ranges = listOf(
            FiniteDateRange(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 6, 1)),
            FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 3, 1))
        )
        val timeline = Timeline.of(ranges).removeAll(ranges)
        assertEquals(emptyList<FiniteDateRange>(), timeline.ranges().toList())
    }

    @Test
    fun `parts of ranges can be removed from a timeline`() {
        val ranges = listOf(
            FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 3, 1)),
            FiniteDateRange(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 6, 1))
        )
        val timeline = Timeline.of(ranges).remove(FiniteDateRange(LocalDate.of(2018, 5, 2), LocalDate.of(2019, 1, 31)))
        assertEquals(
            listOf(
                FiniteDateRange(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 5, 1)),
                FiniteDateRange(LocalDate.of(2019, 2, 1), LocalDate.of(2019, 3, 1))
            ),
            timeline.ranges().toList()
        )
    }
}
