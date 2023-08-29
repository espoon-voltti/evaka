// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared

import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate
import java.util.Objects

/**
 * A timeline is basically an immutable set of dates, but provides simple read/write operations that
 * involve date ranges instead of individual dates.
 *
 * Invariants:
 * * no overlapping date ranges (they are merged when adding to the timeline)
 * * no adjacent date ranges (they are merged when adding to the timeline)
 * * functions that return dates/ranges always return them in ascending order
 *
 * Note: this implementation is *very* inefficient
 */
class Timeline private constructor(private val ranges: List<FiniteDateRange>) {
    override fun equals(other: Any?): Boolean = other is Timeline && this.ranges == other.ranges
    override fun hashCode(): Int = Objects.hash(ranges)
    override fun toString(): String =
        ranges.joinToString(separator = ",", prefix = "{", postfix = "}")

    fun add(range: FiniteDateRange) =
        this.ranges
            .partition { it.overlaps(range) || it.adjacentTo(range) }
            .let { (conflicts, unchanged) ->
                val result = unchanged.toMutableList()
                result.add(
                    conflicts.fold(range) { acc, it ->
                        FiniteDateRange(minOf(it.start, acc.start), maxOf(it.end, acc.end))
                    }
                )
                result.sortBy { it.start }
                Timeline(result)
            }

    fun addAll(other: Timeline) = addAll(other.ranges())
    fun addAll(ranges: Iterable<FiniteDateRange>) =
        ranges.fold(this) { timeline, range -> timeline.add(range) }
    fun addAll(ranges: Sequence<FiniteDateRange>) =
        ranges.fold(this) { timeline, range -> timeline.add(range) }

    fun remove(range: FiniteDateRange) =
        this.ranges
            .partition { it.overlaps(range) }
            .let { (conflicts, unchanged) ->
                val result = unchanged.toMutableList()
                for (conflict in conflicts) {
                    result.addAll(conflict.complement(range))
                }
                result.sortBy { it.start }
                Timeline(result)
            }

    fun removeAll(other: Timeline) = removeAll(other.ranges())
    fun removeAll(ranges: Iterable<FiniteDateRange>) =
        ranges.fold(this) { timeline, range -> timeline.remove(range) }
    fun removeAll(ranges: Sequence<FiniteDateRange>) =
        ranges.fold(this) { timeline, range -> timeline.remove(range) }

    fun contains(range: FiniteDateRange) = this.ranges.any { it.contains(range) }

    fun includes(date: LocalDate) = this.ranges.any { it.includes(date) }

    fun intersection(other: Timeline): Timeline {
        val iterA = this.ranges.iterator()
        val iterB = other.ranges.iterator()
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

    fun ranges(): Sequence<FiniteDateRange> = this.ranges.asSequence()
    fun gaps(): Sequence<FiniteDateRange> =
        this.ranges().windowed(2).map { pair ->
            FiniteDateRange(pair[0].end.plusDays(1), pair[1].start.minusDays(1))
        }

    fun spanningRange(): FiniteDateRange? =
        this.ranges.firstOrNull()?.let { first ->
            this.ranges.lastOrNull()?.let { last -> FiniteDateRange(first.start, last.end) }
        }

    fun isEmpty() = this.ranges.isEmpty()

    companion object {
        private val EMPTY = Timeline(emptyList())
        fun empty(): Timeline = EMPTY
        fun of(vararg ranges: FiniteDateRange): Timeline = empty().addAll(ranges.asSequence())
        fun of(ranges: Iterable<FiniteDateRange>): Timeline = empty().addAll(ranges)
        fun of(ranges: Sequence<FiniteDateRange>): Timeline = empty().addAll(ranges)
    }
}
