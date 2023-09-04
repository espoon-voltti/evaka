// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.data

/**
 * A data structure that is conceptually an immutable set of some comparable points (e.g.
 * `Set<Point>`), but most operations use bounded ranges of those same points.
 *
 * Invariants:
 * - no overlapping ranges (they are merged when adding to the data structure)
 * - no adjacent ranges (they are merged when adding to the data structure)
 * - functions that return points/ranges always return them in ascending order
 *
 * Note: this implementation is *very* inefficient
 */
abstract class RangeBasedSet<
    Point : Comparable<Point>,
    Range : BoundedRange<Point, Range>,
    This : RangeBasedSet<Point, Range, This>>(protected val ranges: List<Range>) :
    Iterable<Range> by ranges {
    fun ranges(): Sequence<Range> = this.ranges.asSequence()
    fun spanningRange(): Range? =
        this.ranges.firstOrNull()?.let { first ->
            this.ranges.lastOrNull()?.let { last -> range(first.start, last.end) }
        }
    fun gaps(): Sequence<Range> =
        this.ranges().windowed(2).mapNotNull { pair -> pair[0].gap(pair[1]) }

    fun isEmpty(): Boolean = this.ranges.isEmpty()

    fun add(range: Range): This = add(this.ranges, range).toThis()
    fun addAll(vararg ranges: Range): This = ranges.fold(this.ranges, ::add).toThis()
    fun addAll(ranges: Iterable<Range>): This = ranges.fold(this.ranges, ::add).toThis()
    fun addAll(ranges: Sequence<Range>): This = ranges.fold(this.ranges, ::add).toThis()

    fun remove(range: Range): This = remove(this.ranges, range).toThis()
    fun removeAll(vararg ranges: Range): This = ranges.fold(this.ranges, ::remove).toThis()
    fun removeAll(ranges: Iterable<Range>): This = ranges.fold(this.ranges, ::remove).toThis()
    fun removeAll(ranges: Sequence<Range>): This = ranges.fold(this.ranges, ::remove).toThis()

    fun contains(range: Range): Boolean = this.ranges.any { it.contains(range) }
    fun includes(date: Point): Boolean = this.ranges.any { it.includes(date) }

    fun intersection(other: Iterable<Range>): This =
        intersection(this.ranges.iterator(), other.iterator()).toThis()
    fun intersection(other: Sequence<Range>): This =
        intersection(this.ranges.iterator(), other.iterator()).toThis()

    protected abstract fun List<Range>.toThis(): This
    protected abstract fun range(start: Point, end: Point): Range

    companion object {
        fun <Point : Comparable<Point>, Range : BoundedRange<Point, Range>> add(
            ranges: List<Range>,
            range: Range
        ): List<Range> =
            ranges
                .partition { it.overlaps(range) || it.adjacentTo(range) }
                .let { (conflicts, unchanged) ->
                    val result = unchanged.toMutableList()
                    result.add(conflicts.fold(range) { acc, it -> acc.merge(it) })
                    result.sortBy { it.start }
                    result
                }

        fun <Point : Comparable<Point>, Range : BoundedRange<Point, Range>> remove(
            ranges: List<Range>,
            range: Range
        ): List<Range> =
            ranges
                .partition { it.overlaps(range) }
                .let { (conflicts, unchanged) ->
                    val result = unchanged.toMutableList()
                    for (conflict in conflicts) {
                        result.addAll(conflict.subtract(range))
                    }
                    result.sortBy { it.start }
                    result
                }

        fun <Point : Comparable<Point>, Range : BoundedRange<Point, Range>> intersection(
            iterA: Iterator<Range>,
            iterB: Iterator<Range>
        ): List<Range> {
            var a = if (iterA.hasNext()) iterA.next() else null
            var b = if (iterB.hasNext()) iterB.next() else null
            val ranges = mutableListOf<Range>()
            while (a != null && b != null) {
                a.intersection(b)?.let { ranges.add(it) }
                if (a.end <= b.end) {
                    a = if (iterA.hasNext()) iterA.next() else null
                } else {
                    b = if (iterB.hasNext()) iterB.next() else null
                }
            }
            return ranges
        }
    }
}
