// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TimeTest {
    @Test
    fun `asDistinctPeriods works in basic case`() {
        val periods = listOf(DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 3)))
        val spanningPeriod = periods[0]

        val result = asDistinctPeriods(periods, spanningPeriod)

        assertEquals(listOf(DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 3))), result)
    }

    @Test
    fun `asDistinctPeriods works with two periods`() {
        val periods = listOf(
            DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 3)),
            DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 5))
        )
        val spanningPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 5))

        val result = asDistinctPeriods(periods, spanningPeriod)

        assertEquals(
            listOf(
                DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 3)),
                DateRange(LocalDate.of(2019, 1, 4), LocalDate.of(2019, 1, 5))
            ),
            result
        )
    }

    @Test
    fun `asDistinctPeriods works with null end date`() {
        val periods = listOf(DateRange(LocalDate.of(2019, 1, 1), null))
        val spanningPeriod = periods[0]

        val result = asDistinctPeriods(periods, spanningPeriod)

        assertEquals(listOf(DateRange(LocalDate.of(2019, 1, 1), null)), result)
    }

    @Test
    fun `asDistinctPeriods works with two periods with one null end date`() {
        val periods = listOf(
            DateRange(LocalDate.of(2019, 1, 5), LocalDate.of(2019, 1, 7)),
            DateRange(LocalDate.of(2019, 1, 1), null)
        )
        val spanningPeriod = DateRange(LocalDate.of(2019, 1, 1), null)

        val result = asDistinctPeriods(periods, spanningPeriod)

        assertEquals(
            listOf(
                DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 4)),
                DateRange(LocalDate.of(2019, 1, 5), LocalDate.of(2019, 1, 7)),
                DateRange(LocalDate.of(2019, 1, 8), null)
            ),
            result
        )
    }

    @Test
    fun `asDistinctPeriods with a spanning period that splits other periods`() {
        val periods = listOf(DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31)))
        val spanningPeriod = DateRange(LocalDate.of(2019, 1, 10), LocalDate.of(2019, 1, 15))

        val result = asDistinctPeriods(periods, spanningPeriod)

        assertEquals(listOf(spanningPeriod), result)
    }

    @Test
    fun `asDistinctPeriods with a gap in periods`() {
        val periods = listOf(
            DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 5)),
            DateRange(LocalDate.of(2019, 1, 10), LocalDate.of(2019, 1, 15))
        )
        val spanningPeriod = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))

        val result = asDistinctPeriods(periods, spanningPeriod)

        assertEquals(
            listOf(
                DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 5)),
                DateRange(LocalDate.of(2019, 1, 6), LocalDate.of(2019, 1, 9)),
                DateRange(LocalDate.of(2019, 1, 10), LocalDate.of(2019, 1, 15)),
                DateRange(LocalDate.of(2019, 1, 16), LocalDate.of(2019, 1, 31))
            ),
            result
        )
    }

    @Test
    fun `mergePeriods works in simple case`() {
        val periods = listOf(
            DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31)) to 1,
            DateRange(LocalDate.of(2019, 2, 1), LocalDate.of(2019, 6, 30)) to 1,
            DateRange(LocalDate.of(2019, 7, 1), LocalDate.of(2020, 1, 1)) to 2
        )

        val result = mergePeriods(periods)

        assertEquals(
            listOf(
                DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 6, 30)) to 1,
                DateRange(LocalDate.of(2019, 7, 1), LocalDate.of(2020, 1, 1)) to 2
            ),
            result
        )
    }

    @Test
    fun `mergePeriods works with different value every day`() {
        val periods = listOf(
            DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 1)) to 1,
            DateRange(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 2)) to 2,
            DateRange(LocalDate.of(2019, 1, 3), LocalDate.of(2019, 1, 3)) to 3,
            DateRange(LocalDate.of(2019, 1, 4), LocalDate.of(2019, 1, 4)) to 4,
            DateRange(LocalDate.of(2019, 1, 5), LocalDate.of(2019, 1, 5)) to 5,
            DateRange(LocalDate.of(2019, 1, 6), LocalDate.of(2019, 1, 6)) to 6,
            DateRange(LocalDate.of(2019, 1, 7), LocalDate.of(2019, 1, 7)) to 7
        )

        val result = mergePeriods(periods)

        assertEquals(periods, result)
    }

    @Test
    fun `mergePeriods works with periods that have a gap between them`() {
        val periods = listOf(
            DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 1)) to 1,
            DateRange(LocalDate.of(2019, 1, 7), LocalDate.of(2019, 1, 7)) to 1
        )

        val result = mergePeriods(periods)

        assertEquals(periods, result)
    }

    @Test
    fun `mergePeriods works with periods that are not distinct`() {
        val periods = listOf(
            DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 5)) to 1,
            DateRange(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 3)) to 1,
            DateRange(LocalDate.of(2019, 1, 4), LocalDate.of(2019, 1, 7)) to 1
        )

        val result = mergePeriods(periods)

        assertEquals(listOf(DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 7)) to 1), result)
    }

    @Test
    fun `mergePeriods works with periods with multiple undefined ends`() {
        val periods = listOf(
            DateRange(LocalDate.of(2019, 1, 1), null) to 1,
            DateRange(LocalDate.of(2019, 1, 2), null) to 1,
            DateRange(LocalDate.of(2019, 1, 4), LocalDate.of(2019, 1, 7)) to 1
        )

        val result = mergePeriods(periods)

        assertEquals(listOf(DateRange(LocalDate.of(2019, 1, 1), null) to 1), result)
    }

    @Test
    fun `mergePeriods works with periods that are not in chronological order`() {
        val periods = listOf(
            DateRange(LocalDate.of(2019, 1, 4), LocalDate.of(2019, 1, 7)) to 1,
            DateRange(LocalDate.of(2019, 1, 1), null) to 1
        )

        val result = mergePeriods(periods)

        assertEquals(listOf(DateRange(LocalDate.of(2019, 1, 1), null) to 1), result)
    }

    @Test
    fun `FiniteDateRange intersection and overlaps returns null and false if there is no overlap`() {
        //   1234
        // A --
        // B   --
        val a = FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 2))
        val b = FiniteDateRange(LocalDate.of(2019, 1, 3), LocalDate.of(2019, 1, 4))
        assertNull(a.intersection(b))
        assertNull(b.intersection(a))
        assertFalse(a.overlaps(b))
        assertFalse(b.overlaps(a))

        //   12345
        // C --
        // D    --
        val c = FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 2))
        val d = FiniteDateRange(LocalDate.of(2019, 1, 4), LocalDate.of(2019, 1, 5))
        assertNull(c.intersection(d))
        assertNull(d.intersection(c))
        assertFalse(c.overlaps(d))
        assertFalse(d.overlaps(c))
    }

    @Test
    fun `FiniteDateRange intersection and overlaps works`() {
        //   1234
        // A ---
        // B  ---
        // X  --
        val a = FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 3))
        val b = FiniteDateRange(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 4))
        val x = FiniteDateRange(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 3))
        assertEquals(x, a.intersection(b))
        assertEquals(x, b.intersection(a))
        assertTrue(a.overlaps(b))
        assertTrue(b.overlaps(a))

        //   12345
        // C ---
        // D   ---
        // Y   -
        val c = FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 3))
        val d = FiniteDateRange(LocalDate.of(2019, 1, 3), LocalDate.of(2019, 1, 5))
        val y = FiniteDateRange(LocalDate.of(2019, 1, 3), LocalDate.of(2019, 1, 3))
        assertEquals(y, c.intersection(d))
        assertEquals(y, d.intersection(c))
        assertTrue(c.overlaps(d))
        assertTrue(d.overlaps(c))
    }

    @Test
    fun `FiniteDateRange durationInDays returns 1 for one day periods`() {
        assertEquals(1, LocalDate.of(2019, 1, 1).toFiniteDateRange().durationInDays())
    }
}
