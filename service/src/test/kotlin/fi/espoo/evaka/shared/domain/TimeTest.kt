// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.domain

import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
}

class FiniteDateRangeTest {
    @Test
    fun `intersection and overlaps return null and false if there is no overlap`() {
        //   1234
        // A --
        // B   --
        val a = testRange(1, 2)
        val b = testRange(3, 4)
        assertNull(a.intersection(b))
        assertNull(b.intersection(a))
        assertFalse(a.overlaps(b))
        assertFalse(b.overlaps(a))

        //   12345
        // C --
        // D    --
        val c = testRange(1, 2)
        val d = testRange(4, 5)
        assertNull(c.intersection(d))
        assertNull(d.intersection(c))
        assertFalse(c.overlaps(d))
        assertFalse(d.overlaps(c))
    }

    @Test
    fun `intersection and overlaps work`() {
        //   1234
        // A ---
        // B  ---
        // X  --
        val a = testRange(1, 3)
        val b = testRange(2, 4)
        val x = testRange(2, 3)
        assertEquals(x, a.intersection(b))
        assertEquals(x, b.intersection(a))
        assertTrue(a.overlaps(b))
        assertTrue(b.overlaps(a))

        //   12345
        // C ---
        // D   ---
        // Y   -
        val c = testRange(1, 3)
        val d = testRange(3, 5)
        val y = testRange(3, 3)
        assertEquals(y, c.intersection(d))
        assertEquals(y, d.intersection(c))
        assertTrue(c.overlaps(d))
        assertTrue(d.overlaps(c))
    }

    @Test
    fun `durationInDays returns 1 for one day periods`() {
        assertEquals(1, LocalDate.of(2019, 1, 1).toFiniteDateRange().durationInDays())
    }

    @Test
    fun `complement - no overlap`() {
        val period = testRange(1, 15)
        val other = testRange(16, 31)
        val complement = period.complement(other)
        assertEquals(listOf(testRange(1, 15)), complement)
    }

    @Test
    fun `complement - full overlap`() {
        val period = testRange(1, 31)
        val other = testRange(1, 31)
        val complement = period.complement(other)
        assertEquals(emptyList(), complement)
    }

    @Test
    fun `complement - overlap start`() {
        val period = testRange(1, 31)
        val other = testRange(1, 15)
        val complement = period.complement(other)
        assertEquals(listOf(testRange(16, 31)), complement)
    }

    @Test
    fun `complement - overlap end`() {
        val period = testRange(1, 31)
        val other = testRange(15, 31)
        val complement = period.complement(other)
        assertEquals(listOf(testRange(1, 14)), complement)
    }

    @Test
    fun `complement - overlap middle`() {
        val period = testRange(1, 31)
        val other = testRange(10, 20)
        val complement = period.complement(other)
        assertEquals(listOf(testRange(1, 9), testRange(21, 31)), complement)
    }

    @Test
    fun `complement multiple`() {
        val period = testRange(1, 31)
        val other1 = testRange(1, 5)
        val other2 = testRange(8, 15)
        val other3 = testRange(10, 20)
        val complement = period.complement(listOf(other1, other2, other3))
        assertEquals(listOf(testRange(6, 7), testRange(21, 31)), complement)
    }

    @Test
    fun `complement none`() {
        val period = testRange(1, 31)
        val complement = period.complement(emptyList())
        assertEquals(listOf(testRange(1, 31)), complement)
    }

    private fun testRange(from: Int, to: Int) = FiniteDateRange(LocalDate.of(2019, 1, from), LocalDate.of(2019, 1, to))
}

class DateRangeTest {
    @Test
    fun `intersection and overlaps return null and false if there is no overlap`() {
        //   1234
        // A --
        // B   --
        val a = testRange(1, 2)
        val b = testRange(3, 4)
        assertNull(a.intersection(b))
        assertNull(b.intersection(a))
        assertFalse(a.overlaps(b))
        assertFalse(b.overlaps(a))

        //   12345
        // C --
        // D    --
        val c = testRange(1, 2)
        val d = testRange(4, null)
        assertNull(c.intersection(d))
        assertNull(d.intersection(c))
        assertFalse(c.overlaps(d))
        assertFalse(d.overlaps(c))

        //   12345
        // E --
        // F   ---+
        val e = testRange(1, 2)
        val f = testRange(3, null)
        assertNull(e.intersection(f))
        assertNull(f.intersection(e))
        assertFalse(e.overlaps(f))
        assertFalse(f.overlaps(e))
    }

    @Test
    fun `intersection and overlaps work with finite ranges`() {
        //   1234
        // A ---
        // B  ---
        // X  --
        val a = testRange(1, 3)
        val b = testRange(2, 4)
        val x = testRange(2, 3)
        assertEquals(x, a.intersection(b))
        assertEquals(x, b.intersection(a))
        assertTrue(a.overlaps(b))
        assertTrue(b.overlaps(a))

        //   12345
        // C ---
        // D   ---
        // Y   -
        val c = testRange(1, 3)
        val d = testRange(3, 5)
        val y = testRange(3, 3)
        assertEquals(y, c.intersection(d))
        assertEquals(y, d.intersection(c))
        assertTrue(c.overlaps(d))
        assertTrue(d.overlaps(c))
    }

    @Test
    fun `intersection and overlaps work with infinite ranges`() {
        //   12345
        // A ---
        // B   ---+
        // X   -
        val a = testRange(1, 3)
        val b = testRange(3, null)
        val x = testRange(3, 3)
        assertEquals(x, a.intersection(b))
        assertEquals(x, b.intersection(a))
        assertTrue(a.overlaps(b))
        assertTrue(b.overlaps(a))

        //   12345
        // C ---
        // D   ---+
        // Y   -
        val c = testRange(1, 3)
        val d = testRange(3, null)
        val y = testRange(3, 3)
        assertEquals(y, c.intersection(d))
        assertEquals(y, d.intersection(c))
        assertTrue(c.overlaps(d))
        assertTrue(d.overlaps(c))

        //   12345
        // E -----+
        // F   ---+
        // Z   ---+
        val e = testRange(1, null)
        val f = testRange(3, null)
        val z = testRange(3, null)
        assertEquals(z, e.intersection(f))
        assertEquals(z, f.intersection(e))
        assertTrue(e.overlaps(f))
        assertTrue(f.overlaps(e))
    }

    private fun testRange(from: Int, to: Int?) =
        DateRange(LocalDate.of(2019, 1, from), if (to == null) null else LocalDate.of(2019, 1, to))
}
