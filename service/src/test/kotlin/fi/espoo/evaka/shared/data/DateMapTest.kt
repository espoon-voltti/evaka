// SPDX-FileCopyrightText: 2017-2023 City of Espoo
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

class DateMapTest {
    @Test
    fun `an empty date map has no ranges, gaps, or spanning range`() {
        val set = DateMap.empty<String>()
        assertEquals(emptyList(), set.ranges().toList())
        assertEquals(emptyList(), set.gaps().toList())
        assertNull(set.spanningRange())
        assertTrue(set.isEmpty())
    }

    @Test
    fun `a date map with a single range has no gaps and its range is the spanning range`() {
        val range = testRange(1..2)
        val set = DateMap.of(range to "X")
        assertEquals(listOf(range to "X"), set.entries().toList())
        assertEquals(listOf(range), set.ranges().toList())
        assertEquals(emptyList(), set.gaps().toList())
        assertEquals(range, set.spanningRange())
        assertFalse(set.isEmpty())
    }

    @Test
    fun `a date map with three ranges has two gaps`() {
        // 12345678
        // AA BB CC
        //   G  G
        val set = DateMap.of(testEntry(1..2, "A"), testEntry(4..5, "B"), testEntry(7..8, "C"))
        val gaps = listOf(testRange(3..3), testRange(6..6))
        assertEquals(gaps, set.gaps().toList())
    }

    @RepeatedTest(100)
    fun `multiple non-adjacent non-overlapping ranges can be added to a date map, in any order`() {
        // 123456
        // AA
        //    BBB
        val entries = listOf(testEntry(1..2, "A"), testEntry(4..6, "B")).shuffled()
        val set = DateMap.of(entries)
        assertTrue(entries.all { set.contains(it.first) })
        assertEquals(entries.sortedBy { it.first.start }, set.entries().toList())
    }

    @RepeatedTest(100)
    fun `overlapping entries with the same value are merged when added to a date map, in any order`() {
        //    123456
        //    AAA
        // +    AA
        // +       A
        // =  AAAA A
        val entries =
            listOf(testEntry(1..3, "A"), testEntry(3..4, "A"), testEntry(6..6, "A")).shuffled()
        val result = listOf(testEntry(1..4, "A"), testEntry(6..6, "A"))
        val set = DateMap.of(entries)
        assertTrue(entries.all { set.contains(it.first) })
        assertEquals(result, set.entries().toList())
    }

    @Test
    fun `overlapping entries with a different value are overwritten when added with the set function to a date map`() {
        //    123456
        //    AAA
        // +    BB
        // +       C
        // =  AABB C
        val result =
            listOf(
                testEntry(1..2, "A"),
                testEntry(3..4, "B"),
                testEntry(6..6, "C")
            )
        val set =
            DateMap.of(testEntry(1..3, "A")).set(testEntry(3..4, "B")).set(testEntry(6..6, "C"))
        assertEquals(result, set.entries().toList())
    }

    @RepeatedTest(100)
    fun `adjacent entries with the same value are merged when added to a date map, in any order`() {
        //    123456
        //    AA
        // +    AA
        // +      AA
        // =  AAAAAA
        val entries =
            listOf(
                testEntry(1..2, "A"),
                testEntry(3..4, "A"),
                testEntry(5..6, "A")
            ).shuffled()
        val result =
            listOf(
                testEntry(1..6, "A")
            )
        val set = DateMap.of(entries)
        assertTrue(entries.all { set.contains(it.first) })
        assertEquals(result, set.entries().toList())
    }

    @RepeatedTest(100)
    fun `adjacent entries with different values are not merged when added to a date map, in any order`() {
        //    123456
        //    AA
        // +    BB
        // +      CC
        // =  AABBCC
        val entries =
            listOf(
                testEntry(1..2, "A"),
                testEntry(3..4, "B"),
                testEntry(5..6, "C")
            ).shuffled()
        val result =
            listOf(
                testEntry(1..2, "A"),
                testEntry(3..4, "B"),
                testEntry(5..6, "C")
            )
        val set = DateMap.of(entries)
        assertTrue(entries.all { set.contains(it.first) })
        assertEquals(result, set.entries().toList())
    }

    @RepeatedTest(100)
    fun `update works`() {
        //    123456
        //    11
        // +   111
        // +     111
        // +   1111
        // +      11
        // +  111
        // =  243332
        val resolve = { _: FiniteDateRange, old: Int, new: Int -> old + new }
        val entries =
            listOf(
                testEntry(1..2, 1),
                testEntry(2..4, 1),
                testEntry(4..6, 1),
                testEntry(2..5, 1),
                testEntry(5..6, 1),
                testEntry(1..3, 1)
            ).shuffled()
        val set = DateMap.empty<Int>().update(entries, resolve)
        assertTrue(entries.all { set.contains(it.first) })
        assertEquals(
            listOf(
                testEntry(1..1, 2),
                testEntry(2..2, 4),
                testEntry(3..5, 3),
                testEntry(6..6, 2)
            ),
            set.entries().toList()
        )
    }

    private fun testDate(day: Int) = LocalDate.of(2019, 1, day)

    private fun testRange(range: IntRange) = FiniteDateRange(testDate(range.first), testDate(range.last))

    private fun <T> testEntry(
        range: IntRange,
        value: T
    ) = testRange(range) to value
}
