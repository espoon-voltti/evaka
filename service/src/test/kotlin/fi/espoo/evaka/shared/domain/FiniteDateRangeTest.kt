// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.domain

import java.lang.IllegalArgumentException
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class FiniteDateRangeTest {
    @Test
    fun `start and end are inclusive, so a range containing just one date can be created`() {
        assertEquals(1, testRange(1, 1).dates().count())
    }

    @Test
    fun `start cannot be after end`() {
        assertThrows<IllegalArgumentException> { testRange(2, 1) }
    }

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
    fun `intersection and overlaps work when there is overlap`() {
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

        //   12345
        // E -----
        // F  ---
        // Z  ---
        val e = testRange(1, 5)
        val f = testRange(2, 4)
        val z = testRange(2, 4)
        assertEquals(z, e.intersection(f))
        assertEquals(z, f.intersection(e))
        assertTrue(e.overlaps(f))
        assertTrue(f.overlaps(e))
    }

    @Test
    fun `a range overlaps and intersects fully with itself`() {
        //   12345
        // X -----
        // X -----
        val x = testRange(1, 5)
        assertEquals(x, x.intersection(x))
        assertTrue(x.overlaps(x))
    }

    @Test
    fun `adjacent ranges don't contain each other`() {
        //   1234
        // A --
        // B   --
        val a = testRange(1, 2)
        val b = testRange(3, 4)
        assertFalse(a.contains(b))
        assertFalse(b.contains(a))
    }

    @Test
    fun `ranges with a gap between them don't contain each other`() {
        //   12345
        // A --
        // B    --
        val a = testRange(1, 2)
        val b = testRange(4, 5)
        assertFalse(a.contains(b))
        assertFalse(b.contains(a))
    }

    @Test
    fun `a larger range contains a smaller range fully within its bounds, but not vice versa`() {
        //   12345
        // A -----
        // B  ---
        val a = testRange(1, 5)
        val b = testRange(2, 4)
        assertTrue(a.contains(b))
        assertFalse(b.contains(a))
    }

    @Test
    fun `a range contains itself`() {
        //   12345
        // X -----
        // X -----
        val x = testRange(1, 5)
        assertTrue(x.contains(x))
    }

    @Test
    fun `all adjacentTo functions work correctly when the two ranges are adjacent`() {
        //   1234
        // A --
        // B   --
        val a = testRange(1, 2)
        val b = testRange(3, 4)
        assertTrue(a.leftAdjacentTo(b))
        assertTrue(b.rightAdjacentTo(a))
        assertTrue(a.adjacentTo(b))
        assertFalse(b.leftAdjacentTo(a))
        assertFalse(a.rightAdjacentTo(b))
    }

    @Test
    fun `all adjacentTo functions work correctly when the two ranges are not adjacent`() {
        //   12345
        // A --
        // B    --
        val a = testRange(1, 2)
        val b = testRange(4, 5)
        assertFalse(a.leftAdjacentTo(b))
        assertFalse(b.rightAdjacentTo(a))
        assertFalse(a.adjacentTo(b))
        assertFalse(b.leftAdjacentTo(a))
        assertFalse(a.rightAdjacentTo(b))

        //   12345
        // C -----
        // D  ---
        val c = testRange(1, 5)
        val d = testRange(2, 4)
        assertFalse(c.leftAdjacentTo(d))
        assertFalse(d.rightAdjacentTo(c))
        assertFalse(c.adjacentTo(d))
        assertFalse(d.leftAdjacentTo(c))
        assertFalse(c.rightAdjacentTo(d))
    }

    @Test
    fun `a range is not adjacent to itself`() {
        //   12345
        // X -----
        // X -----
        val x = testRange(1, 5)
        assertFalse(x.leftAdjacentTo(x))
        assertFalse(x.rightAdjacentTo(x))
        assertFalse(x.adjacentTo(x))
    }

    @Test
    fun `adjacent ranges have no gap`() {
        //   1234
        // A --
        // B   --
        val a = testRange(1, 2)
        val b = testRange(3, 4)
        assertNull(a.gap(b))
        assertNull(b.gap(a))
    }

    @Test
    fun `overlapping ranges have no gap`() {
        //   1234
        // A ---
        // B  ---
        val a = testRange(1, 3)
        val b = testRange(2, 4)
        assertNull(a.gap(b))
        assertNull(b.gap(a))

        //   12345
        // C -----
        // D  ---
        val c = testRange(1, 5)
        val d = testRange(2, 4)
        assertNull(c.gap(d))
        assertNull(d.gap(c))
    }

    @Test
    fun `non-overlapping ranges have a gap`() {
        //   12345
        // A --
        // B    --
        // X   -
        val a = testRange(1, 2)
        val b = testRange(4, 5)
        val x = testRange(3, 3)
        assertEquals(x, a.gap(b))
        assertEquals(x, b.gap(a))
    }

    @Test
    fun `a range doesn't have a gap with itself`() {
        //   12345
        // X -----
        // X -----
        val x = testRange(1, 5)
        assertNull(x.gap(x))
    }

    @Test
    fun `durationInDays returns 1 for one day periods`() {
        assertEquals(1, LocalDate.of(2019, 1, 1).toFiniteDateRange().durationInDays())
    }

    @Test
    fun `subtract - no overlap`() {
        //   12345
        // A --
        // B    --
        // X --
        // Y    --
        val a = testRange(1, 2)
        val b = testRange(4, 5)
        val x = listOf(a)
        val y = listOf(b)
        assertEquals(x, a.subtract(b))
        assertEquals(y, b.subtract(a))
    }

    @Test
    fun `subtract - full overlap`() {
        //   12345
        // A -----
        // A -----
        // X
        val a = testRange(1, 5)
        val x = emptyList<FiniteDateRange>()
        assertEquals(x, a.subtract(a))
    }

    @Test
    fun `subtract - overlap`() {
        //   12345
        // A ---
        // B   ---
        // X --
        // Y    --
        val a = testRange(1, 3)
        val b = testRange(3, 5)
        val x = listOf(testRange(1, 2))
        val y = listOf(testRange(4, 5))
        assertEquals(x, a.subtract(b))
        assertEquals(y, b.subtract(a))
    }

    @Test
    fun `subtract - fully contained`() {
        //   12345
        // A -----
        // B  ---
        // X -   -
        val a = testRange(1, 5)
        val b = testRange(2, 4)
        val x = listOf(testRange(1, 1), testRange(5, 5))
        assertEquals(x, a.subtract(b))
        assertEquals(emptyList(), b.subtract(a))
    }

    @Test
    fun `subtract multiple`() {
        val period = testRange(1, 31)
        val other1 = testRange(1, 5)
        val other2 = testRange(8, 15)
        val other3 = testRange(10, 20)
        val complement = period.complement(listOf(other1, other2, other3))
        assertEquals(listOf(testRange(6, 7), testRange(21, 31)), complement)
    }

    @Test
    fun `subtract none`() {
        val period = testRange(1, 31)
        val complement = period.complement(emptyList())
        assertEquals(listOf(testRange(1, 31)), complement)
    }

    @Test
    fun `merging two ranges with a gap returns a range that ignores the gap and contains both ranges`() {
        //   12345
        // A --
        // B    --
        // X -----
        val a = testRange(1, 2)
        val b = testRange(4, 5)
        val x = testRange(1, 5)
        assertEquals(x, a.merge(b))
        assertEquals(x, b.merge(a))
    }

    @Test
    fun `merging two overlapping ranges returns a range that contains both`() {
        //   12345
        // A ---
        // B   ---
        // X -----
        val a = testRange(1, 3)
        val b = testRange(3, 5)
        val x = testRange(1, 5)
        assertEquals(x, a.merge(b))
        assertEquals(x, b.merge(a))
    }

    @Test
    fun `merging a range with itself returns the same range`() {
        //   12345
        // A -----
        // A -----
        val a = testRange(1, 5)
        assertEquals(a, a.merge(a))
    }

    @Test
    fun `a range includes all its dates`() {
        val range = testRange(1, 10)
        assertEquals(10, range.dates().count())
        assertTrue(range.dates().all { range.includes(it) })
        listOf(1, 5, 10).map(::testDate).forEach { date -> assertTrue(range.includes(date)) }
    }

    @Test
    fun `a range doesn't include a date outside its bounds`() {
        //   12345
        // A  ---
        // B -
        val a = testRange(2, 4)
        val b = testDate(1)
        assertFalse(a.includes(b))
    }

    private fun testDate(day: Int) = LocalDate.of(2019, 1, day)
    private fun testRange(from: Int, to: Int) = FiniteDateRange(testDate(from), testDate(to))
}
