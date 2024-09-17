// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.data

import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test

class DateSetTest {
    @Test
    fun `an empty date set has no ranges, gaps, or spanning range`() {
        val set = DateSet.empty()
        assertEquals(emptyList(), set.ranges().toList())
        assertEquals(emptyList(), set.gaps().toList())
        assertNull(set.spanningRange())
        assertTrue(set.isEmpty())
    }

    @Test
    fun `a date set with a single range has no gaps and its range is the spanning range`() {
        val range = testRange(1, 2)
        val set = DateSet.of(range)
        assertEquals(listOf(range), set.ranges().toList())
        assertEquals(emptyList(), set.gaps().toList())
        assertEquals(range, set.spanningRange())
        assertFalse(set.isEmpty())
    }

    @Test
    fun `a date set with three ranges has two gaps`() {
        // 12345678
        // AA BB CC
        //   G  G
        val set = DateSet.of(testRange(1, 2), testRange(4, 5), testRange(7, 8))
        val gaps = listOf(testRange(3, 3), testRange(6, 6))
        assertEquals(gaps, set.gaps().toList())
    }

    @RepeatedTest(100)
    fun `multiple non-adjacent non-overlapping ranges can be added to a date set, in any order`() {
        // 123456
        // AA
        //    BBB
        val ranges = listOf(testRange(1, 2), testRange(4, 6)).shuffled()
        val set = DateSet.of(ranges)
        assertTrue(ranges.all { set.contains(it) })
        assertEquals(ranges.sortedBy { it.start }, set.ranges().toList())
    }

    @RepeatedTest(100)
    fun `overlapping ranges are merged when added to a date set, in any order`() {
        // 123456
        // AAAA
        //   BBBB
        // XXXXXX
        val ranges = listOf(testRange(1, 4), testRange(3, 6)).shuffled()
        val x = testRange(1, 6)
        val set = DateSet.of(ranges)
        assertTrue(ranges.all { set.contains(it) })
        assertEquals(listOf(x), set.ranges().toList())
    }

    @RepeatedTest(100)
    fun `adjacent ranges are merged when added to a date set, in any order`() {
        // 123456
        // AA
        //   BB
        //     CC
        // XXXXXX
        val ranges = listOf(testRange(1, 2), testRange(3, 4), testRange(5, 6)).shuffled()
        val x = testRange(1, 6)
        val set = DateSet.of(ranges)
        assertTrue(ranges.all { set.contains(it) })
        assertEquals(listOf(x), set.ranges().toList())
    }

    @RepeatedTest(100)
    fun `ranges can be removed from a date set, in any order`() {
        val ranges = listOf(testRange(1, 2), testRange(5, 6)).shuffled()
        val set = DateSet.of(ranges).removeAll(ranges)
        assertFalse(ranges.all { set.contains(it) })
        assertEquals(emptyList(), set.ranges().toList())
    }

    @RepeatedTest(100)
    fun `parts of ranges can be removed from a date set, in any order`() {
        // 12345678
        // ++ ++ ++
        //  --- --
        // A   B  C
        val added = listOf(testRange(1, 2), testRange(4, 5), testRange(7, 8)).shuffled()
        val removed = listOf(testRange(2, 4), testRange(6, 7)).shuffled()
        val set = DateSet.of(added).removeAll(removed)
        assertEquals(
            listOf(testRange(1, 1), testRange(5, 5), testRange(8, 8)),
            set.ranges().toList(),
        )
    }

    @Test
    fun `an empty date set includes and contains nothing`() {
        val set = DateSet.empty()
        assertFalse(set.contains(testRange(1, 2)))
        assertFalse(set.includes(testDate(1)))
    }

    @Test
    fun `a date set with a single range contains and fully includes its range`() {
        val range = testRange(1, 2)
        val set = DateSet.of(range)
        assertTrue(set.contains(range))
        assertTrue(range.dates().all { set.includes(it) })
    }

    @Test
    fun `intersection between date sets works correctly`() {
        // 12345678
        // AA AA AA
        //  BBB   B
        //  X X   X
        val a = DateSet.of(testRange(1, 2), testRange(4, 5), testRange(7, 8))
        val b = DateSet.of(testRange(2, 4), testRange(8, 8))
        val x = DateSet.of(testRange(2, 2), testRange(4, 4), testRange(8, 8))
        assertEquals(x, a.intersection(b))
        assertEquals(x, b.intersection(a))
    }

    @Test
    fun `intersectRanges takes a clamp range that both filters and clamps the returned results`() {
        // 123456789
        // AAA AA AA
        //  RRRRRRR
        //  == == =
        val a = DateSet.of(testRange(1, 3), testRange(5, 6), testRange(8, 9))
        val r = testRange(2, 8)
        assertEquals(
            listOf(testRange(2, 3), testRange(5, 6), testRange(8, 8)),
            a.intersectRanges(r).toList(),
        )
    }

    private fun testDate(day: Int) = LocalDate.of(2019, 1, day)

    private fun testRange(from: Int, to: Int) = FiniteDateRange(testDate(from), testDate(to))
}
