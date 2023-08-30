// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.data

/**
 * A data structure that is conceptually an immutable map from some comparable points to some values
 * (e.g. `Map<Point, String>`), but most operations use bounded ranges instead of points.
 *
 * Invariants:
 * - no overlapping ranges (they are always resolved when adding to the data structure)
 * - no adjacent ranges *with the same value* (they are merged when adding to the data structure)
 * - functions that return points/ranges always return them in ascending order
 *
 * Note: this implementation is *very* inefficient
 */
abstract class RangeBasedMap<
    T,
    Point : Comparable<Point>,
    Range : BoundedRange<Point, Range>,
    This : RangeBasedMap<T, Point, Range, This>>(protected val entries: List<Pair<Range, T>>) :
    Iterable<Pair<Range, T>> by entries {
    fun ranges(): Sequence<Range> = entries.asSequence().map { it.first }
    fun entries(): Sequence<Pair<Range, T>> = this.entries.asSequence()
    fun spanningRange(): Range? =
        this.entries.firstOrNull()?.let { (first, _) ->
            this.entries.lastOrNull()?.let { (last, _) -> range(first.start, last.end) }
        }
    fun gaps(): Sequence<Range> = ranges().windowed(2).mapNotNull { pair -> pair[0].gap(pair[1]) }

    fun isEmpty(): Boolean = this.entries.isEmpty()

    fun set(entry: Pair<Range, T>): This = update(entry.first, entry.second) { _, _, new -> new }
    fun set(range: Range, value: T): This = update(range, value) { _, _, new -> new }
    fun set(ranges: Iterable<Range>, value: T): This = update(ranges, value) { _, _, new -> new }
    fun set(ranges: Sequence<Range>, value: T): This = update(ranges, value) { _, _, new -> new }
    fun setAll(ranges: Iterable<Pair<Range, T>>): This = update(ranges) { _, _, new -> new }
    fun setAll(ranges: Sequence<Pair<Range, T>>): This = update(ranges) { _, _, new -> new }

    fun update(
        entries: Iterable<Pair<Range, T>>,
        resolve: (range: Range, old: T, new: T) -> T
    ): This =
        entries
            .fold(this.entries) { acc, (range, value) -> update(acc, range, value, resolve) }
            .toThis()
    fun update(
        entries: Sequence<Pair<Range, T>>,
        resolve: (range: Range, old: T, new: T) -> T
    ): This =
        entries
            .fold(this.entries) { acc, (range, value) -> update(acc, range, value, resolve) }
            .toThis()
    fun update(
        ranges: Iterable<Range>,
        value: T,
        resolve: (range: Range, old: T, new: T) -> T
    ): This =
        ranges.fold(this.entries) { acc, range -> update(acc, range, value, resolve) }.toThis()
    fun update(
        ranges: Sequence<Range>,
        value: T,
        resolve: (range: Range, old: T, new: T) -> T
    ): This =
        ranges.fold(this.entries) { acc, range -> update(acc, range, value, resolve) }.toThis()
    fun update(range: Range, value: T, resolve: (range: Range, old: T, new: T) -> T): This =
        update(entries, range, value, resolve).toThis()

    fun remove(range: Range): This = remove(entries, range).toThis()
    fun removeAll(ranges: Iterable<Range>): This = ranges.fold(this.entries, ::remove).toThis()
    fun removeAll(ranges: Sequence<Range>): This = ranges.fold(this.entries, ::remove).toThis()

    fun contains(range: Range): Boolean =
        this.entries.fold(emptyList<Range>()) { acc, entry ->
            entry.first.intersection(range)?.let { overlap -> RangeBasedSet.add(acc, overlap) }
                ?: acc
        } == listOf(range)

    protected abstract fun List<Pair<Range, T>>.toThis(): This
    protected abstract fun range(start: Point, end: Point): Range

    companion object {
        fun <T, Point : Comparable<Point>, Range : BoundedRange<Point, Range>> update(
            entries: List<Pair<Range, T>>,
            range: Range,
            newValue: T,
            resolve: (range: Range, old: T, new: T) -> T
        ): List<Pair<Range, T>> {
            val result = mutableListOf<Pair<Range, T>>()

            fun Pair<Range, T>.tryMerge(other: Pair<Range, T>) =
                if (this.first.adjacentTo(other.first) && this.second == other.second)
                    this.first.merge(other.first) to this.second
                else null

            fun append(entry: Pair<Range, T>) =
                when (val last = result.lastOrNull()) {
                    null -> result.add(entry)
                    else -> {
                        when (val merged = last.tryMerge(entry)) {
                            null -> result.add(entry)
                            else -> {
                                result.removeLast()
                                result.add(merged)
                            }
                        }
                    }
                }

            var newRange: Range? = range
            for ((oldRange, oldValue) in entries) {
                if (newRange == null || oldRange.strictlyLeftTo(newRange)) {
                    append(oldRange to oldValue)
                    continue
                }
                newRange =
                    when (val overlap = oldRange.intersection(newRange)) {
                        null -> {
                            // If the old range is not on the left side and there's no overlap, it
                            // must be on the right side. Since the old ranges are sorted, further
                            // entries are guaranteed to not overlap.
                            append(newRange to newValue)
                            append(oldRange to oldValue)
                            null
                        }
                        else -> {
                            val subNew = newRange.subtract(overlap)
                            val subOld = oldRange.subtract(overlap)

                            // only one left range can be non-null, because otherwise there would
                            // be more overlap
                            subNew.left?.let { append(it to newValue) }
                            subOld.left?.let { append(it to oldValue) }

                            append(overlap to resolve(overlap, oldValue, newValue))

                            // only one right range can be non-null, because otherwise there would
                            // be more overlap
                            subOld.right?.let { append(it to oldValue) }
                            subNew.right
                        }
                    }
            }
            newRange?.let { append(it to newValue) }
            return result
        }
        fun <T, Point : Comparable<Point>, Range : BoundedRange<Point, Range>> remove(
            entries: List<Pair<Range, T>>,
            range: Range,
        ): List<Pair<Range, T>> =
            entries
                .partition { it.first.overlaps(range) }
                .let { (conflicts, unchanged) ->
                    val result = unchanged.toMutableList()
                    for (conflict in conflicts) {
                        result.addAll(conflict.first.subtract(range).map { it to conflict.second })
                    }
                    result.sortBy { it.first.start }
                    result
                }
    }
}
