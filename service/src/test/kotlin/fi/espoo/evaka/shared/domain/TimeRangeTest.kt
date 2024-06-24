// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.domain

import fi.espoo.evaka.shared.data.BoundedRange
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TimeRangeTest {
    @Test
    fun `start is inclusive and end is exclusive, so a range containing just one time is invalid`() {
        assertThrows<IllegalArgumentException> { testRange(1, 1) }
    }

    @Test
    fun `a range spans the whole day when start = end = 00`() {
        assertEquals(24 * 60, testRange(0, 0).duration.toMinutes())
    }

    @Test
    fun `start cannot be after end`() {
        assertThrows<IllegalArgumentException> { testRange(2, 1) }
    }

    @Test
    fun `end = 00 means midnight`() {
        assertEquals(60, testRange(23, 0).duration.toMinutes())
    }

    @Test
    fun `intersection and overlaps return null and false if there is no overlap`() {
        //   01 02 03 04 05
        // A ------_
        // B       ------_
        val a = testRange(1, 3)
        val b = testRange(3, 5)
        assertNull(a.intersection(b))
        assertNull(b.intersection(a))
        assertFalse(a.overlaps(b))
        assertFalse(b.overlaps(a))

        //   01 02 03 04 05 06
        // C ------_
        // D          ------_
        val c = testRange(1, 3)
        val d = testRange(4, 6)
        assertNull(c.intersection(d))
        assertNull(d.intersection(c))
        assertFalse(c.overlaps(d))
        assertFalse(d.overlaps(c))

        //   00 01 02     23 00
        // E ------_
        // F              ---_
        val e = testRange(0, 2)
        val f = testRange(23, 0)
        assertNull(e.intersection(f))
        assertNull(f.intersection(e))
        assertFalse(e.overlaps(f))
        assertFalse(f.overlaps(e))
    }

    @Test
    fun `intersection and overlaps work when there is overlap`() {
        //   01 02 03 04 05
        // A ---------_
        // B    ---------_
        // X    ---------_
        val a = testRange(1, 4)
        val b = testRange(2, 5)
        val x = testRange(2, 4)
        assertEquals(x, a.intersection(b))
        assertEquals(x, b.intersection(a))
        assertTrue(a.overlaps(b))
        assertTrue(b.overlaps(a))

        //   01 02 03 04 05 06
        // C ---------_
        // D       ---------_
        // Y       ---_
        val c = testRange(1, 4)
        val d = testRange(3, 6)
        val y = testRange(3, 4)
        assertEquals(y, c.intersection(d))
        assertEquals(y, d.intersection(c))
        assertTrue(c.overlaps(d))
        assertTrue(d.overlaps(c))

        //   01 02 03 04 05 06
        // E ---------------_
        // F    ---------_
        // Z    ---------_
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
        //   01 02 03 04 05 06
        // X ---------------_
        // X ---------------_
        val x = testRange(1, 6)
        assertEquals(x, x.intersection(x))
        assertTrue(x.overlaps(x))
    }

    @Test
    fun `adjacent ranges don't contain each other`() {
        //   01 02 03 04 05
        // A ------_
        // B       ------_
        val a = testRange(1, 3)
        val b = testRange(3, 5)
        assertFalse(a.contains(b))
        assertFalse(b.contains(a))
    }

    @Test
    fun `ranges with a gap between them don't contain each other`() {
        //   01 02 03 04 05 06
        // A ------_
        // B          ------_
        val a = testRange(1, 3)
        val b = testRange(4, 6)
        assertFalse(a.contains(b))
        assertFalse(b.contains(a))

        //   00 01 02    23 00
        // C ------_
        // D             ---_
        val c = testRange(0, 2)
        val d = testRange(23, 0)
        assertFalse(c.contains(d))
        assertFalse(d.contains(c))
    }

    @Test
    fun `a larger range contains a smaller range fully within its bounds, but not vice versa`() {
        //   01 02 03 04 05 06
        // A ---------------_
        // B    ---------_
        val a = testRange(1, 6)
        val b = testRange(2, 5)
        assertTrue(a.contains(b))
        assertFalse(b.contains(a))
    }

    @Test
    fun `a range contains itself`() {
        //   01 02 03 04 05 06
        // X ---------------_
        // X ---------------_
        val x = testRange(1, 6)
        assertTrue(x.contains(x))
    }

    @Test
    fun `all adjacentTo functions work correctly when the two ranges are adjacent`() {
        //   01 02 03 04 05
        // A ------_
        // B       ------_
        val a = testRange(1, 3)
        val b = testRange(3, 5)
        assertTrue(a.leftAdjacentTo(b))
        assertTrue(b.rightAdjacentTo(a))
        assertTrue(a.adjacentTo(b))
        assertFalse(b.leftAdjacentTo(a))
        assertFalse(a.rightAdjacentTo(b))

        //   00 01 ... 22 23 00
        // C ------...----_
        // D              ---_
        val c = testRange(0, 23)
        val d = testRange(23, 0)
        assertTrue(c.leftAdjacentTo(d))
        assertTrue(d.rightAdjacentTo(c))
        assertTrue(c.adjacentTo(d))
        assertFalse(d.leftAdjacentTo(c))
        assertFalse(c.rightAdjacentTo(d))
    }

    @Test
    fun `all adjacentTo functions work correctly when the two ranges are not adjacent`() {
        //   01 02 03 04 05 06
        // A ------_
        // B          ------_
        val a = testRange(1, 3)
        val b = testRange(4, 6)
        assertFalse(a.leftAdjacentTo(b))
        assertFalse(b.rightAdjacentTo(a))
        assertFalse(a.adjacentTo(b))
        assertFalse(b.leftAdjacentTo(a))
        assertFalse(a.rightAdjacentTo(b))

        //   01 02 03 04 05 06
        // C ---------------_
        // D    ---------_
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
        //   01 02 03 04 05 06
        // A ---------------_
        // A ---------------_
        val a = testRange(1, 6)
        assertFalse(a.leftAdjacentTo(a))
        assertFalse(a.rightAdjacentTo(a))
        assertFalse(a.adjacentTo(a))

        //   00 01 ... 23 00
        // B ------...----_
        // B ------...----_
        val b = testRange(0, 0)
        assertFalse(b.leftAdjacentTo(b))
        assertFalse(b.rightAdjacentTo(b))
        assertFalse(b.adjacentTo(b))
    }

    @Test
    fun `adjacent ranges have no gap`() {
        //   01 02 03 04 05
        // A ------_
        // B       ------_
        val a = testRange(1, 3)
        val b = testRange(3, 5)
        assertNull(a.gap(b))
        assertNull(b.gap(a))

        //   00 01 ... 22 23 00
        // C -------------_
        // D              ---_
        val c = testRange(0, 23)
        val d = testRange(23, 0)
        assertNull(c.gap(d))
        assertNull(d.gap(c))
    }

    @Test
    fun `overlapping ranges have no gap`() {
        //   01 02 03 04 05
        // A ---------_
        // B    ---------_
        val a = testRange(1, 4)
        val b = testRange(2, 5)
        assertNull(a.gap(b))
        assertNull(b.gap(a))

        //   01 02 03 04 05 06
        // C ---------------_
        // D    ---------_
        val c = testRange(1, 6)
        val d = testRange(2, 5)
        assertNull(c.gap(d))
        assertNull(d.gap(c))
    }

    @Test
    fun `non-overlapping ranges have a gap`() {
        //   01 02 03 04 05 06
        // A ------_
        // B          ------_
        // X       ---_
        val a = testRange(1, 3)
        val b = testRange(4, 6)
        val x = testRange(3, 4)
        assertEquals(x, a.gap(b))
        assertEquals(x, b.gap(a))

        //   00 01 02 ... 22 23 00
        // C ------_
        // D                 ---_
        // Y       ---...----_

        val c = testRange(0, 2)
        val d = testRange(23, 0)
        val y = testRange(2, 23)
        assertEquals(y, c.gap(d))
        assertEquals(y, d.gap(c))
    }

    @Test
    fun `a range doesn't have a gap with itself`() {
        //   01 02 03 04 05 06
        // X ---------------_
        // X ---------------_
        val x = testRange(1, 6)
        assertNull(x.gap(x))
    }

    @Test
    fun `subtract - no overlap`() {
        //   01 02 03 04 05 06
        // A ------_
        // B          ------_
        // X ------_
        // Y          ------_
        val a = testRange(1, 3)
        val b = testRange(4, 6)
        val x = listOf(a)
        val y = listOf(b)
        assertEquals(x, a.subtract(b).toList())
        assertEquals(y, b.subtract(a).toList())

        //   00 01 02 ... 23 00
        // C ------_
        // D              ---_
        // Z ------_
        // W              ---_
        val c = testRange(0, 2)
        val d = testRange(23, 0)
        val z = listOf(c)
        val w = listOf(d)
        assertEquals(z, c.subtract(d).toList())
        assertEquals(w, d.subtract(c).toList())
    }

    @Test
    fun `subtract - full overlap`() {
        //   01 02 03 04 05 06
        // A ---------------_
        // A ---------------_
        // X
        val a = testRange(1, 6)
        val x = emptyList<TimeRange>()
        assertEquals(x, a.subtract(a).toList())

        //   00 01 02 ... 23 00
        // B ----------------_
        // B ----------------_
        // Y
        val b = testRange(0, 0)
        val y = emptyList<TimeRange>()
        assertEquals(y, b.subtract(b).toList())
    }

    @Test
    fun `subtract - overlap`() {
        //   01 02 03 04 05 06
        // A ---------_
        // B       ---------_
        // X ------_
        // Y          ------_
        val a = testRange(1, 4)
        val b = testRange(3, 6)
        val x = listOf(testRange(1, 3))
        val y = listOf(testRange(4, 6))
        assertEquals(x, a.subtract(b).toList())
        assertEquals(y, b.subtract(a).toList())
    }

    @Test
    fun `subtract - fully contained`() {
        //   01 02 03 04 05 06
        // A ---------------_
        // B    ---------_
        // X ---_        ---_
        val a = testRange(1, 6)
        val b = testRange(2, 5)
        val x = listOf(testRange(1, 2), testRange(5, 6))
        assertEquals(x, a.subtract(b).toList())
        assertEquals(emptyList(), b.subtract(a).toList())
    }

    @Test
    fun `merging two ranges with a gap returns a range that ignores the gap and contains both ranges`() {
        //   01 02 03 04 05 06
        // A ------_
        // B          ------_
        // X ---------------_
        val a = testRange(1, 3)
        val b = testRange(4, 6)
        val x = testRange(1, 6)
        assertEquals(x, a.merge(b))
        assertEquals(x, b.merge(a))

        //   00 01 02 ... 23 00
        // C ------_
        // D              ---_
        // Y ---------...----_
        val c = testRange(0, 2)
        val d = testRange(23, 0)
        val y = testRange(0, 0)
        assertEquals(y, c.merge(d))
        assertEquals(y, d.merge(c))
    }

    @Test
    fun `merging two overlapping ranges returns a range that contains both`() {
        //   01 02 03 04 05 06
        // A ---------_
        // B       ---------_
        // X ---------------_
        val a = testRange(1, 4)
        val b = testRange(3, 6)
        val x = testRange(1, 6)
        assertEquals(x, a.merge(b))
        assertEquals(x, b.merge(a))
    }

    @Test
    fun `merging a range with itself returns the same range`() {
        //   01 02 03 04 05 06
        // A ---------------_
        // A ---------------_
        val a = testRange(1, 6)
        assertEquals(a, a.merge(a))
    }

    @Test
    fun `a range includes all its start, middle, but not its end`() {
        val range = testRange(1, 10)
        assertTrue(range.includes(testTime(1)))
        assertTrue(range.includes(testTime(5)))
        assertFalse(range.includes(testTime(10)))
    }

    @Test
    fun `a range doesn't include a localtime outside its bounds`() {
        //   01 02 03 04 05
        // A    ---------_
        // B --
        // C             --
        val a = testRange(2, 5)
        val b = testTime(1)
        val c = testTime(5)
        assertFalse(a.includes(b))
        assertFalse(a.includes(c))

        //   00 01 ... 22 23 00
        // D           ------_
        // E --
        val d = testRange(22, 0)
        val e = testTime(0)
        assertFalse(d.includes(e))
    }

    @Test
    fun `a range includes a localtime inside its bounds`() {
        //   00 01 02
        // A ------_
        // B --
        val a = testRange(0, 2)
        val b = testTime(0)
        assertTrue(a.includes(b))
    }

    @Test
    fun `relation is correct when ranges have no overlap but there is a gap`() {
        //   01 02 03 04 05 06 07 08
        // A ------_
        // B                ------_
        val a = testRange(1, 3)
        val b = testRange(6, 8)
        assertEquals(BoundedRange.Relation.LeftTo(gap = testRange(3, 6)), a.relationTo(b))
        assertEquals(BoundedRange.Relation.RightTo(gap = testRange(3, 6)), b.relationTo(a))
    }

    @Test
    fun `relation is correct when ranges are adjacent`() {
        //   01 02 03 04 05
        // A ------_
        // B       ------_
        val a = testRange(1, 3)
        val b = testRange(3, 5)
        assertEquals(BoundedRange.Relation.LeftTo<TimeRange>(gap = null), a.relationTo(b))
        assertEquals(BoundedRange.Relation.RightTo<TimeRange>(gap = null), b.relationTo(a))
    }

    @Test
    fun `relation is correct when ranges are equal`() {
        //   01 02 03 04 05
        // A ------------_
        // A ------------_
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
        //   01 02 03 04 05 06 07 08
        // A ---------------_
        // B       ---------------_
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

        //   00 01 02 ... 22 23 00
        // C ----------------_
        // D              ------_
        val c = testRange(0, 23)
        val d = testRange(22, 0)
        assertEquals(
            BoundedRange.Relation.Overlap(
                left = BoundedRange.Relation.Remainder(testRange(0, 22), isFirst = true),
                overlap = testRange(22, 23),
                right = BoundedRange.Relation.Remainder(testRange(23, 0), isFirst = false)
            ),
            c.relationTo(d)
        )
        assertEquals(
            BoundedRange.Relation.Overlap(
                left = BoundedRange.Relation.Remainder(testRange(0, 22), isFirst = false),
                overlap = testRange(22, 23),
                right = BoundedRange.Relation.Remainder(testRange(23, 0), isFirst = true)
            ),
            d.relationTo(c)
        )
    }

    private fun testTime(hour: Int) = LocalTime.of(hour, 0)

    private fun testRange(
        from: Int,
        to: Int
    ) = TimeRange(testTime(from), testTime(to))
}
