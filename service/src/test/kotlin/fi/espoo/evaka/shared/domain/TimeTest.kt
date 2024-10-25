// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.domain

import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class TimeTest {
    @Test
    fun `asDistinctPeriods works in basic case`() {
        val periods = listOf(FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 3)))
        val spanningPeriod = periods[0]

        val result = asDistinctPeriods(periods, spanningPeriod)

        assertEquals(
            listOf(FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 3))),
            result,
        )
    }

    @Test
    fun `asDistinctPeriods works with two periods`() {
        val periods =
            listOf(
                FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 3)),
                FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 5)),
            )
        val spanningPeriod = FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 5))

        val result = asDistinctPeriods(periods, spanningPeriod)

        assertEquals(
            listOf(
                FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 3)),
                FiniteDateRange(LocalDate.of(2019, 1, 4), LocalDate.of(2019, 1, 5)),
            ),
            result,
        )
    }

    @Test
    fun `asDistinctPeriods with a spanning period that splits other periods`() {
        val periods = listOf(FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31)))
        val spanningPeriod = FiniteDateRange(LocalDate.of(2019, 1, 10), LocalDate.of(2019, 1, 15))

        val result = asDistinctPeriods(periods, spanningPeriod)

        assertEquals(listOf(spanningPeriod), result)
    }

    @Test
    fun `asDistinctPeriods with a gap in periods`() {
        val periods =
            listOf(
                FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 5)),
                FiniteDateRange(LocalDate.of(2019, 1, 10), LocalDate.of(2019, 1, 15)),
            )
        val spanningPeriod = FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))

        val result = asDistinctPeriods(periods, spanningPeriod)

        assertEquals(
            listOf(
                FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 5)),
                FiniteDateRange(LocalDate.of(2019, 1, 6), LocalDate.of(2019, 1, 9)),
                FiniteDateRange(LocalDate.of(2019, 1, 10), LocalDate.of(2019, 1, 15)),
                FiniteDateRange(LocalDate.of(2019, 1, 16), LocalDate.of(2019, 1, 31)),
            ),
            result,
        )
    }

    @Test
    fun `mergePeriods works in simple case`() {
        val periods =
            listOf(
                FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31)) to 1,
                FiniteDateRange(LocalDate.of(2019, 2, 1), LocalDate.of(2019, 6, 30)) to 1,
                FiniteDateRange(LocalDate.of(2019, 7, 1), LocalDate.of(2020, 1, 1)) to 2,
            )

        val result = mergePeriods(periods)

        assertEquals(
            listOf(
                FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 6, 30)) to 1,
                FiniteDateRange(LocalDate.of(2019, 7, 1), LocalDate.of(2020, 1, 1)) to 2,
            ),
            result,
        )
    }

    @Test
    fun `mergePeriods works with different value every day`() {
        val periods =
            listOf(
                FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 1)) to 1,
                FiniteDateRange(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 2)) to 2,
                FiniteDateRange(LocalDate.of(2019, 1, 3), LocalDate.of(2019, 1, 3)) to 3,
                FiniteDateRange(LocalDate.of(2019, 1, 4), LocalDate.of(2019, 1, 4)) to 4,
                FiniteDateRange(LocalDate.of(2019, 1, 5), LocalDate.of(2019, 1, 5)) to 5,
                FiniteDateRange(LocalDate.of(2019, 1, 6), LocalDate.of(2019, 1, 6)) to 6,
                FiniteDateRange(LocalDate.of(2019, 1, 7), LocalDate.of(2019, 1, 7)) to 7,
            )

        val result = mergePeriods(periods)

        assertEquals(periods, result)
    }

    @Test
    fun `mergePeriods works with periods that have a gap between them`() {
        val periods =
            listOf(
                FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 1)) to 1,
                FiniteDateRange(LocalDate.of(2019, 1, 7), LocalDate.of(2019, 1, 7)) to 1,
            )

        val result = mergePeriods(periods)

        assertEquals(periods, result)
    }

    @Test
    fun `mergePeriods works with periods that are not distinct`() {
        val periods =
            listOf(
                FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 5)) to 1,
                FiniteDateRange(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 1, 3)) to 1,
                FiniteDateRange(LocalDate.of(2019, 1, 4), LocalDate.of(2019, 1, 7)) to 1,
            )

        val result = mergePeriods(periods)

        assertEquals(
            listOf(FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 7)) to 1),
            result,
        )
    }

    @Test
    fun `mergePeriods works with periods that are not in chronological order`() {
        val periods =
            listOf(
                FiniteDateRange(LocalDate.of(2019, 1, 4), LocalDate.of(2019, 1, 7)) to 1,
                FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 3)) to 1,
            )

        val result = mergePeriods(periods)

        assertEquals(
            listOf(FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 7)) to 1),
            result,
        )
    }
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
