// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.data

import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.HelsinkiDateTimeRange
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test

class DateTimeMapTest {
    @Test
    fun `an empty datetime map has no ranges, gaps, or spanning range`() {
        val set = DateTimeMap.empty<String>()
        assertEquals(emptyList(), set.ranges().toList())
        assertEquals(emptyList(), set.gaps().toList())
        assertNull(set.spanningRange())
        assertTrue(set.isEmpty())
    }

    @Test
    fun `a datetime map with a single range has no gaps and its range is the spanning range`() {
        val range = testRange(1 to 3)
        val set = DateTimeMap.of(range to "X")
        assertEquals(listOf(range to "X"), set.entries().toList())
        assertEquals(listOf(range), set.ranges().toList())
        assertEquals(emptyList(), set.gaps().toList())
        assertEquals(range, set.spanningRange())
        assertFalse(set.isEmpty())
    }

    @Test
    fun `a datetime map with three ranges has two gaps`() {
        // 12345678901
        // AA_ BB_ CC_
        //   GG_ GG_
        val set =
            DateTimeMap.of(testEntry(1 to 3, "A"), testEntry(5 to 7, "B"), testEntry(9 to 11, "C"))
        val gaps = listOf(testRange(3 to 5), testRange(7 to 9))
        assertEquals(gaps, set.gaps().toList())
    }

    @RepeatedTest(100)
    fun `multiple non-adjacent non-overlapping ranges can be added to a datetime map, in any order`() {
        // 1234567
        // AA_
        //    BBB_
        val entries = listOf(testEntry(1 to 3, "A"), testEntry(4 to 7, "B")).shuffled()
        val set = DateTimeMap.of(entries)
        assertTrue(entries.all { set.contains(it.first) })
        assertEquals(entries.sortedBy { it.first.start }, set.entries().toList())
    }

    @RepeatedTest(100)
    fun `overlapping entries with the same value are merged when added to a datetime map, in any order`() {
        //    1234567
        //    AAA_
        // +    AA_
        // +       A_
        // =  AAAA_A_
        val entries =
            listOf(testEntry(1 to 4, "A"), testEntry(3 to 5, "A"), testEntry(6 to 7, "A"))
                .shuffled()
        val result = listOf(testEntry(1 to 5, "A"), testEntry(6 to 7, "A"))
        val set = DateTimeMap.of(entries)
        assertTrue(entries.all { set.contains(it.first) })
        assertEquals(result, set.entries().toList())
    }

    @Test
    fun `overlapping entries with a different value are overwritten when added with the set function to a datetime map`() {
        //    123456
        //    AAA_
        // +    BB_
        // +       C_
        // =  AABB_C_
        val result = listOf(testEntry(1 to 3, "A"), testEntry(3 to 5, "B"), testEntry(6 to 7, "C"))
        val set =
            DateTimeMap.of(testEntry(1 to 4, "A"))
                .set(testEntry(3 to 5, "B"))
                .set(testEntry(6 to 7, "C"))
        assertEquals(result, set.entries().toList())
    }

    @RepeatedTest(100)
    fun `adjacent entries with the same value are merged when added to a datetime map, in any order`() {
        //    1234567
        //    AA_
        // +    AA_
        // +      AA_
        // =  AAAAAA_
        val entries =
            listOf(testEntry(1 to 3, "A"), testEntry(3 to 5, "A"), testEntry(5 to 7, "A"))
                .shuffled()
        val result = listOf(testEntry(1 to 7, "A"))
        val set = DateTimeMap.of(entries)
        assertTrue(entries.all { set.contains(it.first) })
        assertEquals(result, set.entries().toList())
    }

    @RepeatedTest(100)
    fun `adjacent entries with different values are not merged when added to a datetime map, in any order`() {
        //    1234567
        //    AA_
        // +    BB_
        // +      CC_
        // =  AABBCC_
        val entries =
            listOf(testEntry(1 to 3, "A"), testEntry(3 to 5, "B"), testEntry(5 to 7, "C"))
                .shuffled()
        val result = listOf(testEntry(1 to 3, "A"), testEntry(3 to 5, "B"), testEntry(5 to 7, "C"))
        val set = DateTimeMap.of(entries)
        assertTrue(entries.all { set.contains(it.first) })
        assertEquals(result, set.entries().toList())
    }

    @RepeatedTest(100)
    fun `update works`() {
        //    1234567
        //    11_
        // +   111_
        // +     111_
        // +   1111_
        // +      11_
        // +  111_
        // =  243332_
        val resolve = { _: HelsinkiDateTimeRange, old: Int, new: Int -> old + new }
        val entries =
            listOf(
                    testEntry(1 to 3, 1),
                    testEntry(2 to 5, 1),
                    testEntry(4 to 7, 1),
                    testEntry(2 to 6, 1),
                    testEntry(5 to 7, 1),
                    testEntry(1 to 4, 1),
                )
                .shuffled()
        val set = DateTimeMap.empty<Int>().update(entries, resolve)
        assertTrue(entries.all { set.contains(it.first) })
        assertEquals(
            listOf(
                testEntry(1 to 2, 2),
                testEntry(2 to 3, 4),
                testEntry(3 to 6, 3),
                testEntry(6 to 7, 2),
            ),
            set.entries().toList(),
        )
    }

    @Test
    fun `intersectRanges,intersectEntries,intersectValues take a clamp range that both filters and clamps the returned results`() {
        // 123456789
        // AA_ B_ C_
        //  RRRRRR_
        //  =_ =_
        val map =
            DateTimeMap.of(
                testRange(1 to 3) to "A",
                testRange(5 to 6) to "B",
                testRange(8 to 9) to "C",
            )
        val r = testRange(2 to 8)
        assertEquals(listOf(testRange(2 to 3), testRange(5 to 6)), map.intersectRanges(r).toList())
        assertEquals(listOf("A", "B"), map.intersectValues(r).toList())
        assertEquals(
            listOf(testRange(2 to 3) to "A", testRange(5 to 6) to "B"),
            map.intersectEntries(r).toList(),
        )
    }

    private fun testDateTime(hour: Int) = HelsinkiDateTime.of(LocalDateTime.of(2019, 1, 1, hour, 0))

    private fun testRange(range: Pair<Int, Int>) =
        HelsinkiDateTimeRange(testDateTime(range.first), testDateTime(range.second))

    private fun <T> testEntry(range: Pair<Int, Int>, value: T) = testRange(range) to value
}
