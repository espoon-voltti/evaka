// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared

import fi.espoo.evaka.shared.domain.FiniteDateRange

/**
 * A timeline is basically an immutable set of dates, but provides simple read/write operations that involve date ranges
 * instead of individual dates.
 *
 * Invariants:
 *   * no overlapping date ranges (they are merged when adding to the timeline)
 *   * no adjacent date ranges (they are merged when adding to the timeline)
 *   * functions that return dates/ranges always return them in ascending order
 *
 * Note: this implementation is *very* inefficient
 */
data class Timeline private constructor(private val ranges: List<FiniteDateRange>) : Iterable<FiniteDateRange> by ranges {
    constructor() : this(emptyList())

    fun add(range: FiniteDateRange) = this.ranges.partition { it.overlaps(range) || it.adjacentTo(range) }
        .let { (conflicts, unchanged) ->
            val result = unchanged.toMutableList()
            result.add(
                conflicts.fold(range) { acc, it ->
                    FiniteDateRange(
                        minOf(it.start, acc.start),
                        maxOf(it.end, acc.end)
                    )
                }
            )
            result.sortBy { it.start }
            Timeline(result)
        }

    fun addAll(ranges: Iterable<FiniteDateRange>) = ranges.fold(this) { timeline, range -> timeline.add(range) }
    fun addAll(ranges: Sequence<FiniteDateRange>) = ranges.fold(this) { timeline, range -> timeline.add(range) }

    fun remove(range: FiniteDateRange) = this.ranges.partition { it.overlaps(range) }
        .let { (conflicts, unchanged) ->
            val result = unchanged.toMutableList()
            for (conflict in conflicts) {
                conflict.intersection(range)?.let { intersection ->
                    if (conflict.start != intersection.start) {
                        result.add(FiniteDateRange(start = conflict.start, end = intersection.start.minusDays(1)))
                    }
                    if (conflict.end != intersection.end) {
                        result.add(FiniteDateRange(start = intersection.end.plusDays(1), end = conflict.end))
                    }
                }
            }
            result.sortBy { it.start }
            Timeline(result)
        }

    fun removeAll(ranges: Iterable<FiniteDateRange>) = ranges.fold(this) { timeline, range -> timeline.remove(range) }
    fun removeAll(ranges: Sequence<FiniteDateRange>) = ranges.fold(this) { timeline, range -> timeline.remove(range) }

    fun contains(range: FiniteDateRange) = this.ranges.any { it.contains(range) }

    fun intersection(other: Timeline): Timeline {
        val iterA = this.iterator()
        val iterB = other.iterator()
        var a = if (iterA.hasNext()) iterA.next() else null
        var b = if (iterB.hasNext()) iterB.next() else null
        val ranges = mutableListOf<FiniteDateRange>()
        while (a != null && b != null) {
            a.intersection(b)?.let { ranges.add(it) }
            if (a.end <= b.end) {
                a = if (iterA.hasNext()) iterA.next() else null
            } else {
                b = if (iterB.hasNext()) iterB.next() else null
            }
        }
        return Timeline(ranges)
    }

    fun ranges() = this.ranges.asSequence()
    fun gaps() = this.ranges().windowed(2).mapNotNull { pair ->
        FiniteDateRange(pair[0].end.plusDays(1), pair[1].start.minusDays(1))
    }

    fun spanningRange() = this.ranges.firstOrNull()?.let { first ->
        this.ranges.lastOrNull()?.let { last ->
            FiniteDateRange(first.start, last.end)
        }
    }

    fun isEmpty() = this.ranges.isEmpty()

    companion object {
        fun of(): Timeline = Timeline()
        fun of(vararg ranges: FiniteDateRange): Timeline = Timeline().addAll(ranges.asIterable())
        fun of(ranges: Collection<FiniteDateRange>): Timeline = Timeline().addAll(ranges)
    }
}
