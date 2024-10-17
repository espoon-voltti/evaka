// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.data

import io.kotest.common.runBlocking
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.positiveInt
import io.kotest.property.checkAll
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

abstract class RangeBasedMapPropertyTest<
    Point : Comparable<Point>,
    Range : BoundedRange<Point, Range>,
    RangeMap : RangeBasedMap<Int, Point, Range, RangeMap>,
> {

    protected abstract fun emptyMap(): RangeMap

    protected abstract fun arbitraryMap(value: Arb<Int> = Arb.int()): Arb<RangeMap>

    protected abstract fun arbitraryRange(duration: Arb<Int> = defaultDuration()): Arb<Range>

    protected open fun defaultDuration(): Arb<Int> = Arb.positiveInt(max = 20)

    protected abstract fun pointsOfRange(range: Range): Sequence<Point>

    @Test
    fun `it never contains overlapping ranges`() {
        runBlocking {
            checkAll(arbitraryMap()) { map ->
                val ranges = map.ranges().toList()
                assertFalse(
                    ranges.withIndex().any { (index, range) ->
                        ranges.withIndex().any { (index2, range2) ->
                            index != index2 && (range.overlaps(range2))
                        }
                    }
                )
            }
        }
    }

    @Test
    fun `it never contains adjacent ranges with the same value`() {
        runBlocking {
            checkAll(arbitraryMap()) { map ->
                val entries = map.entries().toList()
                assertFalse(
                    entries.withIndex().any { (index, entry) ->
                        entries.withIndex().any { (index2, entry2) ->
                            index != index2 &&
                                (entry.first.adjacentTo(entry2.first) &&
                                    entry.second == entry2.second)
                        }
                    }
                )
            }
        }
    }

    @Test
    fun `it always returns ranges in ascending order`() {
        runBlocking {
            checkAll(arbitraryMap()) { map ->
                assertContentEquals(
                    map.ranges().sortedWith(compareBy({ it.start }, { it.end })),
                    map.ranges(),
                )
            }
        }
    }

    @Test
    fun `it contains every range added to it, and every range returned by its ranges function`() {
        runBlocking {
            checkAll(Arb.list(arbitraryRange())) { ranges ->
                val map = emptyMap().setAll(ranges.map { it to 1 })
                assertTrue(ranges.all { map.contains(it) })
                assertTrue(map.ranges().all { map.contains(it) })
            }
        }
    }

    @Test
    fun `when a range is removed, it no longer contains the range`() {
        runBlocking {
            checkAll(Arb.list(arbitraryRange()), arbitraryRange()) { otherRanges, range ->
                val map = emptyMap().set(range, 1).setAll(otherRanges.map { it to 1 }).remove(range)
                assertFalse(map.contains(range))
            }
        }
    }

    @Test
    fun `when all its ranges are removed, it returns an empty set`() {
        runBlocking {
            checkAll(arbitraryMap()) { map ->
                assertEquals(emptyMap(), map.removeAll(map.ranges()))
            }
        }
    }

    @Test
    fun `spanningRange is equal to merging all ranges in it`() {
        runBlocking {
            checkAll(arbitraryMap()) { map ->
                assertEquals(map.ranges().reduceOrNull { a, b -> a.merge(b) }, map.spanningRange())
            }
        }
    }

    @Test
    fun `it contains none of its gaps`() {
        runBlocking {
            checkAll(arbitraryMap()) { map -> assertFalse(map.gaps().any { map.contains(it) }) }
        }
    }

    @Test
    fun `it is empty only if there are no ranges`() {
        runBlocking {
            checkAll(arbitraryMap()) { map ->
                assertEquals(map.ranges().count() == 0, map.isEmpty())
                assertEquals(!map.isEmpty(), map.isNotEmpty())
            }
        }
    }

    @Test
    fun `it returns a value for every point of every range`() {
        runBlocking {
            checkAll(Arb.list(arbitraryRange(duration = Arb.positiveInt(10)))) { ranges ->
                val value = 1
                val map = emptyMap().setAll(ranges.asSequence().map { it to value })
                for (range in map.ranges()) {
                    for (point in pointsOfRange(range)) {
                        assertEquals(value, map.getValue(point))
                    }
                }
            }
        }
    }
}
