// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.shared.domain

import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class DateTimeRangeTest {
    @Test
    fun `start cannot be after end`() {
        assertFailsWith<IllegalArgumentException> { testRange(2, 1) }
    }

    @Test
    fun `an empty FiniteDateTimeRange can be converted, because FiniteDateTimeRange allows it`() {
        assertEquals(testRange(1, 1), testFiniteRange(1, 1).asDateTimeRange())
    }

    @Test
    fun `a range includes its start but not its end`() {
        val range = testRange(1, 10)
        assertFalse(range.includes(testDateTime(1).minusSeconds(1)))
        assertTrue(range.includes(testDateTime(1)))
        assertTrue(range.includes(testDateTime(10).minusSeconds(1)))
        assertFalse(range.includes(testDateTime(10)))

        val rangeWithoutEnd = testRange(5, null)
        assertFalse(rangeWithoutEnd.includes(testDateTime(5).minusSeconds(1)))
        assertTrue(rangeWithoutEnd.includes(testDateTime(5)))
        assertTrue(
            rangeWithoutEnd.includes(HelsinkiDateTime.of(LocalDateTime.of(9999, 12, 31, 23, 59))),
        )
    }

    @Test
    fun `operations on ranges with finite ends match FiniteDateTimeRange`() {
        val pairs =
            listOf(
                // adjacent
                testFiniteRange(1, 3) to testFiniteRange(3, 5),
                // gap between
                testFiniteRange(1, 3) to testFiniteRange(4, 6),
                // partial overlap
                testFiniteRange(1, 4) to testFiniteRange(2, 6),
                // one within the other
                testFiniteRange(1, 6) to testFiniteRange(2, 5),
                // equal
                testFiniteRange(1, 6) to testFiniteRange(1, 6),
            )
        for ((a, b) in pairs + pairs.map { (a, b) -> b to a }) {
            val x = a.asDateTimeRange()
            assertEquals(a.overlaps(b), x.overlaps(b), "$a overlaps $b")
            assertEquals(a.overlaps(b), x.overlaps(b.asDateTimeRange()), "$a overlaps $b")
            assertEquals(a.contains(b), x.contains(b), "$a contains $b")
            assertEquals(a.contains(b), x.contains(b.asDateTimeRange()), "$a contains $b")
            assertEquals(a.intersection(b), x.intersection(b), "$a intersection $b")
            assertEquals(
                a.intersection(b)?.asDateTimeRange(),
                x.intersection(b.asDateTimeRange()),
                "$a intersection $b",
            )
        }
    }

    @Test
    fun `a range with null end overlaps the ranges that end after its start`() {
        //   1234567
        // A   ----->
        // B --_
        // C ---_
        // D     --_
        // E     --->
        val a = testRange(3, null)
        val b = testRange(1, 3)
        val c = testRange(1, 4)
        val d = testRange(5, 7)
        val e = testRange(5, null)
        assertFalse(a.overlaps(b))
        assertFalse(b.overlaps(a))
        assertTrue(a.overlaps(c))
        assertTrue(c.overlaps(a))
        assertTrue(a.overlaps(d))
        assertTrue(d.overlaps(a))
        assertTrue(a.overlaps(e))
        assertTrue(e.overlaps(a))
    }

    @Test
    fun `intersection with a range with null end`() {
        //   1234567
        // A   ----->
        // B --_
        // C -----_
        // D     --->
        val a = testRange(3, null)
        val b = testRange(1, 3)
        val c = testRange(1, 6)
        val d = testRange(5, null)
        assertNull(a.intersection(b))
        assertNull(b.intersection(a))
        assertEquals(testRange(3, 6), a.intersection(c))
        assertEquals(testRange(3, 6), c.intersection(a))
        assertEquals(testFiniteRange(3, 6), a.intersection(testFiniteRange(1, 6)))
        assertEquals(d, a.intersection(d))
        assertEquals(d, d.intersection(a))
    }

    @Test
    fun `a range with null end contains the ranges that start at or after its start`() {
        //   1234567
        // A   ----->
        // B ---_
        // C    --_
        // D     --->
        val a = testRange(3, null)
        val b = testRange(1, 4)
        val c = testRange(4, 6)
        val d = testRange(5, null)
        assertTrue(a.contains(a))
        assertFalse(a.contains(b))
        assertTrue(a.contains(c))
        assertFalse(c.contains(a))
        assertTrue(a.contains(d))
        assertFalse(d.contains(a))
    }

    @Test
    fun `asFiniteDateTimeRange returns null or uses the default end only when the end is null`() {
        assertEquals(testFiniteRange(1, 3), testRange(1, 3).asFiniteDateTimeRange())
        assertNull(testRange(1, null).asFiniteDateTimeRange())
        assertEquals(testFiniteRange(1, 3), testRange(1, 3).asFiniteDateTimeRange(testDateTime(5)))
        assertEquals(
            testFiniteRange(1, 5),
            testRange(1, null).asFiniteDateTimeRange(testDateTime(5)),
        )
    }

    private fun testDateTime(hour: Int) = HelsinkiDateTime.of(LocalDateTime.of(2019, 1, 1, hour, 0))

    private fun testRange(from: Int, to: Int?) =
        DateTimeRange(testDateTime(from), to?.let { testDateTime(it) })

    private fun testFiniteRange(from: Int, to: Int) =
        FiniteDateTimeRange(testDateTime(from), testDateTime(to))
}
