// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared

import fi.espoo.evaka.shared.data.DateSet
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class DateSetTest {
    @Test
    fun `an empty date set contains nothing`() {
        val set = DateSet.of()
        assertFalse(
            set.contains(FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2020, 1, 1)))
        )
        assertEquals(emptyList(), set.ranges().toList())
    }

    @Test
    fun `a range can be added to a date set`() {
        val range = FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2020, 1, 1))
        val set = DateSet.of(range)
        assertTrue(set.contains(range))
        assertEquals(listOf(range), set.ranges().toList())
    }

    @Test
    fun `multiple ranges can be added to a date set`() {
        val ranges =
            listOf(
                FiniteDateRange(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 6, 1)),
                FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 3, 1))
            )
        val set = DateSet.of(ranges)
        assertTrue(ranges.all { set.contains(it) })
        assertEquals(ranges, set.ranges().toList())
    }

    @Test
    fun `overlapping ranges are merged when added to a date set`() {
        val ranges =
            listOf(
                FiniteDateRange(LocalDate.of(2018, 1, 1), LocalDate.of(2019, 6, 1)),
                FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2020, 1, 1))
            )
        val set = DateSet.of(ranges)
        assertTrue(ranges.all { set.contains(it) })
        assertEquals(
            listOf(FiniteDateRange(start = ranges[0].start, end = ranges[1].end)),
            set.ranges().toList()
        )
    }

    @Test
    fun `adjacent ranges are merged when added to a date set`() {
        val ranges =
            listOf(
                FiniteDateRange(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 12, 31)),
                FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 5, 31)),
                FiniteDateRange(LocalDate.of(2019, 6, 1), LocalDate.of(2020, 1, 1))
            )
        val set = DateSet.of(ranges)
        assertTrue(ranges.all { set.contains(it) })
        assertEquals(
            listOf(FiniteDateRange(start = ranges[0].start, end = ranges[2].end)),
            set.ranges().toList()
        )
    }

    @Test
    fun `ranges can be removed from a date set`() {
        val ranges =
            listOf(
                FiniteDateRange(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 6, 1)),
                FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 3, 1))
            )
        val set = DateSet.of(ranges).removeAll(ranges)
        assertEquals(emptyList(), set.ranges().toList())
    }

    @Test
    fun `parts of ranges can be removed from a date set`() {
        val ranges =
            listOf(
                FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 3, 1)),
                FiniteDateRange(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 6, 1))
            )
        val set =
            DateSet.of(ranges)
                .remove(FiniteDateRange(LocalDate.of(2018, 5, 2), LocalDate.of(2019, 1, 31)))
        assertEquals(
            listOf(
                FiniteDateRange(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 5, 1)),
                FiniteDateRange(LocalDate.of(2019, 2, 1), LocalDate.of(2019, 3, 1))
            ),
            set.ranges().toList()
        )
    }
}
