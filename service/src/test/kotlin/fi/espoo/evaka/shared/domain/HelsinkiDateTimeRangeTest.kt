// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.domain

import fi.espoo.evaka.shared.data.BoundedRange
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class HelsinkiDateTimeRangeTest {
    @Test
    fun `start is inclusive and end is exclusive, so a range containing just one date is invalid`() {
        // TODO: change constructor semantics
        // assertThrows<IllegalArgumentException> { testRange(1, 1) }
        assertNull(HelsinkiDateTimeRange.tryCreate(testDateTime(1), testDateTime(1)))
    }

    @Test
    fun `start cannot be after end`() {
        // TODO: change constructor to throw IllegalArgumentException instead
        assertThrows<IllegalStateException> { testRange(2, 1) }
        assertNull(HelsinkiDateTimeRange.tryCreate(testDateTime(2), testDateTime(1)))
    }

    @Test
    fun `intersection and overlaps return null and false if there is no overlap`() {
        //   12345
        // A --_
        // B   --_
        val a = testRange(1, 3)
        val b = testRange(3, 5)
        assertNull(a.intersection(b))
        assertNull(b.intersection(a))
        assertFalse(a.overlaps(b))
        assertFalse(b.overlaps(a))

        //   123456
        // C --_
        // D    --_
        val c = testRange(1, 3)
        val d = testRange(4, 6)
        assertNull(c.intersection(d))
        assertNull(d.intersection(c))
        assertFalse(c.overlaps(d))
        assertFalse(d.overlaps(c))
    }

    @Test
    fun `intersection and overlaps work when there is overlap`() {
        //   12345
        // A ---_
        // B  ---_
        // X  --_
        val a = testRange(1, 4)
        val b = testRange(2, 5)
        val x = testRange(2, 4)
        assertEquals(x, a.intersection(b))
        assertEquals(x, b.intersection(a))
        assertTrue(a.overlaps(b))
        assertTrue(b.overlaps(a))

        //   123456
        // C ---_
        // D   ---_
        // Y   -_
        val c = testRange(1, 4)
        val d = testRange(3, 6)
        val y = testRange(3, 4)
        assertEquals(y, c.intersection(d))
        assertEquals(y, d.intersection(c))
        assertTrue(c.overlaps(d))
        assertTrue(d.overlaps(c))

        //   123456
        // E -----_
        // F  ---_
        // Z  ---_
        val e = testRange(1, 6)
        val f = testRange(2, 5)
        val z = testRange(2, 5)
        assertEquals(z, e.intersection(f))
        assertEquals(z, f.intersection(e))
        assertTrue(e.overlaps(f))
        assertTrue(f.overlaps(e))
    }

    @Test
    fun `a range overlaps and intersects fully with itself`() {
        //   123456
        // X -----_
        // X -----_
        val x = testRange(1, 6)
        assertEquals(x, x.intersection(x))
        assertTrue(x.overlaps(x))
    }

    @Test
    fun `adjacent ranges don't contain each other`() {
        //   12345
        // A --_
        // B   --_
        val a = testRange(1, 3)
        val b = testRange(3, 5)
        assertFalse(a.contains(b))
        assertFalse(b.contains(a))
    }

    @Test
    fun `ranges with a gap between them don't contain each other`() {
        //   123456
        // A --_
        // B    --_
        val a = testRange(1, 3)
        val b = testRange(4, 6)
        assertFalse(a.contains(b))
        assertFalse(b.contains(a))
    }

    @Test
    fun `a larger range contains a smaller range fully within its bounds, but not vice versa`() {
        //   123456
        // A -----_
        // B  ---_
        val a = testRange(1, 6)
        val b = testRange(2, 5)
        assertTrue(a.contains(b))
        assertFalse(b.contains(a))
    }

    @Test
    fun `a range contains itself`() {
        //   123456
        // X -----_
        // X -----_
        val x = testRange(1, 6)
        assertTrue(x.contains(x))
    }

    @Test
    fun `all adjacentTo functions work correctly when the two ranges are adjacent`() {
        //   12345
        // A --_
        // B   --_
        val a = testRange(1, 3)
        val b = testRange(3, 5)
        assertTrue(a.leftAdjacentTo(b))
        assertTrue(b.rightAdjacentTo(a))
        assertTrue(a.adjacentTo(b))
        assertFalse(b.leftAdjacentTo(a))
        assertFalse(a.rightAdjacentTo(b))
    }

    @Test
    fun `all adjacentTo functions work correctly when the two ranges are not adjacent`() {
        //   123456
        // A --_
        // B    --_
        val a = testRange(1, 3)
        val b = testRange(4, 6)
        assertFalse(a.leftAdjacentTo(b))
        assertFalse(b.rightAdjacentTo(a))
        assertFalse(a.adjacentTo(b))
        assertFalse(b.leftAdjacentTo(a))
        assertFalse(a.rightAdjacentTo(b))

        //   123456
        // C -----_
        // D  ---_
        val c = testRange(1, 6)
        val d = testRange(2, 5)
        assertFalse(c.leftAdjacentTo(d))
        assertFalse(d.rightAdjacentTo(c))
        assertFalse(c.adjacentTo(d))
        assertFalse(d.leftAdjacentTo(c))
        assertFalse(c.rightAdjacentTo(d))
    }

    @Test
    fun `a range is not adjacent to itself`() {
        //   123456
        // X ------_
        // X ------_
        val x = testRange(1, 6)
        assertFalse(x.leftAdjacentTo(x))
        assertFalse(x.rightAdjacentTo(x))
        assertFalse(x.adjacentTo(x))
    }

    @Test
    fun `adjacent ranges have no gap`() {
        //   12345
        // A --_
        // B   --_
        val a = testRange(1, 3)
        val b = testRange(3, 5)
        assertNull(a.gap(b))
        assertNull(b.gap(a))
    }

    @Test
    fun `overlapping ranges have no gap`() {
        //   12345
        // A ---_
        // B  ---_
        val a = testRange(1, 4)
        val b = testRange(2, 5)
        assertNull(a.gap(b))
        assertNull(b.gap(a))

        //   123456
        // C -----_
        // D  ---_
        val c = testRange(1, 6)
        val d = testRange(2, 5)
        assertNull(c.gap(d))
        assertNull(d.gap(c))
    }

    @Test
    fun `non-overlapping ranges have a gap`() {
        //   123456
        // A --_
        // B    --_
        // X   -_
        val a = testRange(1, 3)
        val b = testRange(4, 6)
        val x = testRange(3, 4)
        assertEquals(x, a.gap(b))
        assertEquals(x, b.gap(a))
    }

    @Test
    fun `a range doesn't have a gap with itself`() {
        //   123456
        // X -----_
        // X -----_
        val x = testRange(1, 6)
        assertNull(x.gap(x))
    }

    @Test
    fun `subtract - no overlap`() {
        //   123456
        // A --_
        // B    --_
        // X --_
        // Y    --_
        val a = testRange(1, 3)
        val b = testRange(4, 6)
        val x = listOf(a)
        val y = listOf(b)
        assertEquals(x, a.subtract(b).toList())
        assertEquals(y, b.subtract(a).toList())
    }

    @Test
    fun `subtract - full overlap`() {
        //   123456
        // A -----_
        // A -----_
        // X
        val a = testRange(1, 6)
        val x = emptyList<HelsinkiDateTimeRange>()
        assertEquals(x, a.subtract(a).toList())
    }

    @Test
    fun `subtract - overlap`() {
        //   123456
        // A ---_
        // B   ---_
        // X --_
        // Y    --_
        val a = testRange(1, 4)
        val b = testRange(3, 6)
        val x = listOf(testRange(1, 3))
        val y = listOf(testRange(4, 6))
        assertEquals(x, a.subtract(b).toList())
        assertEquals(y, b.subtract(a).toList())
    }

    @Test
    fun `subtract - fully contained`() {
        //   123456
        // A -----_
        // B  ---_
        // X -_  -_
        val a = testRange(1, 6)
        val b = testRange(2, 5)
        val x = listOf(testRange(1, 2), testRange(5, 6))
        assertEquals(x, a.subtract(b).toList())
        assertEquals(emptyList(), b.subtract(a).toList())
    }

    @Test
    fun `merging two ranges with a gap returns a range that ignores the gap and contains both ranges`() {
        //   123456
        // A --_
        // B    --_
        // X -----_
        val a = testRange(1, 3)
        val b = testRange(4, 6)
        val x = testRange(1, 6)
        assertEquals(x, a.merge(b))
        assertEquals(x, b.merge(a))
    }

    @Test
    fun `merging two overlapping ranges returns a range that contains both`() {
        //   123456
        // A ---_
        // B   ---_
        // X -----_
        val a = testRange(1, 4)
        val b = testRange(3, 6)
        val x = testRange(1, 6)
        assertEquals(x, a.merge(b))
        assertEquals(x, b.merge(a))
    }

    @Test
    fun `merging a range with itself returns the same range`() {
        //   123456
        // A -----_
        // A -----_
        val a = testRange(1, 6)
        assertEquals(a, a.merge(a))
    }

    @Test
    fun `a range includes all its start, middle, but not its end`() {
        val range = testRange(1, 10)
        assertTrue(range.includes(testDateTime(1)))
        assertTrue(range.includes(testDateTime(5)))
        assertFalse(range.includes(testDateTime(10)))
    }

    @Test
    fun `a range doesn't include a datetime outside its bounds`() {
        //   123456
        // A  ---_
        // B -
        val a = testRange(2, 5)
        val b = testDateTime(1)
        assertFalse(a.includes(b))
    }

    @Test
    fun `relation is correct when ranges have no overlap but there is a gap`() {
        //   12345678
        // A --_
        // B      --_
        val a = testRange(1, 3)
        val b = testRange(6, 8)
        assertEquals(BoundedRange.Relation.LeftTo(gap = testRange(3, 6)), a.relationTo(b))
        assertEquals(BoundedRange.Relation.RightTo(gap = testRange(3, 6)), b.relationTo(a))
    }

    @Test
    fun `relation is correct when ranges are adjacent`() {
        //   12345
        // A --_
        // B   --_
        val a = testRange(1, 3)
        val b = testRange(3, 5)
        assertEquals(
            BoundedRange.Relation.LeftTo<HelsinkiDateTimeRange>(gap = null),
            a.relationTo(b)
        )
        assertEquals(
            BoundedRange.Relation.RightTo<HelsinkiDateTimeRange>(gap = null),
            b.relationTo(a)
        )
    }

    @Test
    fun `relation is correct when ranges are equal`() {
        //   12345
        // A ----_
        // A ----_
        val a = testRange(1, 5)
        assertEquals(
            BoundedRange.Relation.Overlap(
                left = null,
                overlap = a,
                right = null
            ),
            a.relationTo(a)
        )
    }

    @Test
    fun `relation is correct when there is overlap`() {
        //   12345678
        // A -----_
        // B   -----_
        val a = testRange(1, 6)
        val b = testRange(3, 8)
        assertEquals(
            BoundedRange.Relation.Overlap(
                left = BoundedRange.Relation.Remainder(testRange(1, 3), isFirst = true),
                overlap = testRange(3, 6),
                right = BoundedRange.Relation.Remainder(testRange(6, 8), isFirst = false)
            ),
            a.relationTo(b)
        )
        assertEquals(
            BoundedRange.Relation.Overlap(
                left = BoundedRange.Relation.Remainder(testRange(1, 3), isFirst = false),
                overlap = testRange(3, 6),
                right = BoundedRange.Relation.Remainder(testRange(6, 8), isFirst = true)
            ),
            b.relationTo(a)
        )
    }

    private fun testDateTime(hour: Int) = HelsinkiDateTime.of(LocalDateTime.of(2019, 1, 1, hour, 0))

    private fun testRange(
        from: Int,
        to: Int
    ) = HelsinkiDateTimeRange(testDateTime(from), testDateTime(to))
}
