// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.data

import io.kotest.common.runBlocking
import io.kotest.property.Arb
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

abstract class RangeBasedSetPropertyTest<
    Point : Comparable<Point>,
    Range : BoundedRange<Point, Range>,
    RangeSet : RangeBasedSet<Point, Range, RangeSet>,
> {

    protected abstract fun emptySet(): RangeSet

    protected abstract fun arbitrarySet(): Arb<RangeSet>

    protected abstract fun arbitraryRange(): Arb<Range>

    @Test
    fun `it never contains overlapping or adjacent ranges`() {
        runBlocking {
            checkAll(arbitrarySet()) { set ->
                val ranges = set.ranges().toList()
                assertFalse(
                    ranges.withIndex().any { (index, range) ->
                        ranges.withIndex().any { (index2, range2) ->
                            index != index2 && (range.overlaps(range2) || range.adjacentTo(range2))
                        }
                    }
                )
            }
        }
    }

    @Test
    fun `it always returns ranges in ascending order`() {
        runBlocking {
            checkAll(arbitrarySet()) { set ->
                assertContentEquals(
                    set.ranges().sortedWith(compareBy({ it.start }, { it.end })),
                    set.ranges(),
                )
            }
        }
    }

    @Test
    fun `it contains every range added to it`() {
        runBlocking {
            checkAll(Arb.list(arbitraryRange())) { ranges ->
                val set = emptySet().addAll(ranges)
                assertTrue(set.ranges().all { set.contains(it) })
            }
        }
    }

    @Test
    fun `when a range is removed, it no longer contains the range`() {
        runBlocking {
            checkAll(Arb.list(arbitraryRange()), arbitraryRange()) { otherRanges, range ->
                val set = emptySet().add(range).addAll(otherRanges).remove(range)
                assertFalse(set.contains(range))
            }
        }
    }

    @Test
    fun `when all its ranges are removed, it returns an empty set`() {
        runBlocking {
            checkAll(arbitrarySet()) { set ->
                assertEquals(emptySet(), set.removeAll(set.ranges()))
            }
        }
    }

    @Test
    fun `spanningRange is equal to merging all ranges in it`() {
        runBlocking {
            checkAll(arbitrarySet()) { set ->
                assertEquals(set.ranges().reduceOrNull { a, b -> a.merge(b) }, set.spanningRange())
            }
        }
    }

    @Test
    fun `it contains none of its gaps`() {
        runBlocking {
            checkAll(arbitrarySet()) { set -> assertFalse(set.gaps().any { set.contains(it) }) }
        }
    }

    @Test
    fun `it is empty only if there are no ranges`() {
        runBlocking {
            checkAll(arbitrarySet()) { set ->
                assertEquals(set.ranges().count() == 0, set.isEmpty())
                assertEquals(!set.isEmpty(), set.isNotEmpty())
            }
        }
    }

    @Test
    fun `intersectRanges always returns ranges that are fully contained the original parameter`() {
        runBlocking {
            checkAll(arbitrarySet(), arbitraryRange()) { set, range ->
                assertTrue(set.intersectRanges(range).all { range.contains(it) })
            }
        }
    }

    @Test
    fun `if intersectRanges result is removed from a set, it never contains the original parameter and has no overlap with it`() {
        runBlocking {
            checkAll(arbitrarySet(), arbitraryRange()) { set, range ->
                val removed = set.removeAll(set.intersectRanges(range))
                assertFalse(removed.contains(range))
                assertFalse(removed.ranges().any { it.overlaps(range) })
            }
        }
    }
}
