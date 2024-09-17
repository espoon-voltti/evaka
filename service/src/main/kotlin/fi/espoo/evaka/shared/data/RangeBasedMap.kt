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
    This : RangeBasedMap<T, Point, Range, This>,
>(protected val entries: List<Pair<Range, T>>) {
    /**
     * Returns a sequence of all ranges in the map, sorted in ascending order.
     *
     * The returned ranges never overlap, but some may be adjacent if the adjacent ranges have
     * different values in the map.
     */
    fun ranges(): Sequence<Range> = entries.asSequence().map { it.first }

    /**
     * Returns a sequence of ranges in the map that overlap with the given range, sorted in
     * ascending order.
     *
     * The returned ranges never overlap, but some may be adjacent if the adjacent ranges have
     * different values in the map. The ranges are intersections instead of the originals, so they
     * never extend outside the given range.
     */
    fun intersectRanges(range: Range): Sequence<Range> =
        entries
            .asSequence()
            .dropWhile { !it.first.overlaps(range) }
            .takeWhile { it.first.overlaps(range) }
            .mapNotNull { it.first.intersection(range) }

    /**
     * Returns a sequence of all values in the map, sorted by range in ascending order.
     *
     * Duplicates are possible if the same value exists in multiple ranges in the original map.
     */
    fun values(): Sequence<T> = this.entries.asSequence().map { it.second }

    /**
     * Returns a sequence of values in the map that overlap with the given range, sorted by range in
     * ascending order.
     *
     * Duplicates are possible if the same value exists in multiple ranges in the original map.
     */
    fun intersectValues(range: Range): Sequence<T> =
        this.entries
            .asSequence()
            .dropWhile { !it.first.overlaps(range) }
            .takeWhile { it.first.overlaps(range) }
            .map { it.second }

    /** Returns a sequence of all entries in the map, sorted by range in ascending order. */
    fun entries(): Sequence<Pair<Range, T>> = this.entries.asSequence()

    /**
     * Returns a sequence of entries in the map that overlap with the given range, sorted by range
     * in ascending order.
     *
     * The returned entry ranges are intersections instead of the originals, so no data is ever
     * returned that continues outside the given clamp range.
     */
    fun intersectEntries(range: Range): Sequence<Pair<Range, T>> =
        this.entries
            .asSequence()
            .dropWhile { !it.first.overlaps(range) }
            .takeWhile { it.first.overlaps(range) }
            .mapNotNull { Pair(it.first.intersection(range) ?: return@mapNotNull null, it.second) }

    /**
     * Returns the largest range that covers all the points in all the ranges in the map, or null if
     * the map is empty.
     */
    fun spanningRange(): Range? =
        this.entries.firstOrNull()?.let { (first, _) ->
            this.entries.lastOrNull()?.let { (last, _) -> range(first.start, last.end) }
        }

    /**
     * Returns a sequence of all the non-adjacent gaps between the ranges in the map.
     *
     * The sequence is empty if the map is empty or contains only one range.
     */
    fun gaps(): Sequence<Range> = ranges().windowed(2).mapNotNull { pair -> pair[0].gap(pair[1]) }

    /** Returns true if the map is empty. */
    fun isEmpty(): Boolean = this.entries.isEmpty()

    /**
     * Returns a new map with the given range set to the given value. Any existing values
     * overlapping in any way with the given range are *overwritten*.
     */
    fun set(entry: Pair<Range, T>): This = update(entry.first, entry.second) { _, _, new -> new }

    /**
     * Returns a new map with the given range set to the given value. Any existing values
     * overlapping in any way with the given range are *overwritten*.
     */
    fun set(range: Range, value: T): This = update(range, value) { _, _, new -> new }

    /**
     * Returns a new map with all the given ranges set to the given value. Any existing values
     * overlapping in any way with the given ranges are *overwritten*.
     */
    fun set(ranges: Iterable<Range>, value: T): This = update(ranges, value) { _, _, new -> new }

    /**
     * Returns a new map with all the given ranges set to the given value. Any existing values
     * overlapping in any way with the given ranges are *overwritten*.
     */
    fun set(ranges: Sequence<Range>, value: T): This = update(ranges, value) { _, _, new -> new }

    /**
     * Returns a new map with all the ranges in the given map set to the given values. Any existing
     * values overlapping in any way with the given ranges are *overwritten*.
     */
    fun setAll(map: RangeBasedMap<T, Point, Range, This>): This =
        update(map.entries) { _, _, new -> new }

    /**
     * Returns a new map with all the given ranges set to the given values. Any existing values
     * overlapping in any way with the given ranges are *overwritten*.
     */
    fun setAll(ranges: Iterable<Pair<Range, T>>): This = update(ranges) { _, _, new -> new }

    /**
     * Returns a new map with all the given ranges set to the given values. Any existing values
     * overlapping in any way with the given ranges are *overwritten*.
     */
    fun setAll(ranges: Sequence<Pair<Range, T>>): This = update(ranges) { _, _, new -> new }

    /**
     * Returns a new map with all the ranges in the given map updated with the given values. Any
     * existing values overlapping in any way with the given ranges are resolved using the given
     * resolve function.
     */
    fun update(
        map: RangeBasedMap<T, Point, Range, This>,
        resolve: (range: Range, old: T, new: T) -> T,
    ): This =
        map.entries
            .fold(this.entries) { acc, (range, value) -> update(acc, range, value, resolve) }
            .toThis()

    /**
     * Returns a new map with all the given ranges updated with the given values. Any existing
     * values overlapping in any way with the given ranges are resolved using the given resolve
     * function.
     */
    fun update(
        entries: Iterable<Pair<Range, T>>,
        resolve: (range: Range, old: T, new: T) -> T,
    ): This =
        entries
            .fold(this.entries) { acc, (range, value) -> update(acc, range, value, resolve) }
            .toThis()

    /**
     * Returns a new map with all the given ranges updated with the given values. Any existing
     * values overlapping in any way with the given ranges are resolved using the given resolve
     * function.
     */
    fun update(
        entries: Sequence<Pair<Range, T>>,
        resolve: (range: Range, old: T, new: T) -> T,
    ): This =
        entries
            .fold(this.entries) { acc, (range, value) -> update(acc, range, value, resolve) }
            .toThis()

    /**
     * Returns a new map with all the given ranges updated with the given value. Any existing values
     * overlapping in any way with the given ranges are resolved using the given resolve function.
     */
    fun update(
        ranges: Iterable<Range>,
        value: T,
        resolve: (range: Range, old: T, new: T) -> T,
    ): This =
        ranges.fold(this.entries) { acc, range -> update(acc, range, value, resolve) }.toThis()

    /**
     * Returns a new map with all the given ranges updated with the given value. Any existing values
     * overlapping in any way with the given ranges are resolved using the given resolve function.
     */
    fun update(
        ranges: Sequence<Range>,
        value: T,
        resolve: (range: Range, old: T, new: T) -> T,
    ): This =
        ranges.fold(this.entries) { acc, range -> update(acc, range, value, resolve) }.toThis()

    /**
     * Returns a new map with the given range updated with the given value. Any existing values
     * overlapping in any way with the given range are resolved using the given resolve function.
     */
    fun update(range: Range, value: T, resolve: (range: Range, old: T, new: T) -> T): This =
        update(entries, range, value, resolve).toThis()

    /** Returns a new map with the given range removed from the current contained ranges. */
    fun remove(range: Range): This = remove(entries, range).toThis()

    /** Returns a new map with all the given ranges removed from the current contained ranges. */
    fun removeAll(ranges: Iterable<Range>): This = ranges.fold(this.entries, ::remove).toThis()

    /** Returns a new map with all the given ranges removed from the current contained ranges. */
    fun removeAll(ranges: Sequence<Range>): This = ranges.fold(this.entries, ::remove).toThis()

    operator fun minus(range: Range): This = remove(range)

    operator fun minus(ranges: Iterable<Range>): This = removeAll(ranges)

    operator fun minus(ranges: Sequence<Range>): This = removeAll(ranges)

    /**
     * Returns true if the given range is fully contained by the map.
     *
     * Note: the range may be mapped in smaller parts to different values. This function only cares
     * about whether one or more values can be found for the whole range.
     */
    fun contains(range: Range): Boolean =
        this.entries.fold(emptyList<Range>()) { acc, entry ->
            entry.first.intersection(range)?.let { overlap -> RangeBasedSet.add(acc, overlap) }
                ?: acc
        } == listOf(range)

    /** Converts a raw sorted list of entries to a concrete `RangeBasedMap` subclass object. */
    protected abstract fun List<Pair<Range, T>>.toThis(): This

    /** Constructs a range from endpoints. */
    protected abstract fun range(start: Point, end: Point): Range

    /** Gets value at given point or null if not exists */
    fun getValue(at: Point): T? = entries().find { it.first.includes(at) }?.second

    companion object {
        /**
         * Updates a sorted non-overlapping list of entries with the given range, value, and resolve
         * function.
         *
         * Parts of the given range that *don't overlap* with existing entries are added to the
         * resulting list with the given value. Parts of the given range that *do overlap* at least
         * partially with existing entries are resolved using the given resolve function. The
         * resolve function gets the overlapping part of the ranges and both old and new values as
         * arguments, and is free to return the old, new, or a completely new value for the entry
         * that will be added to the resulting list.
         *
         * The given list *must* contain elements in sorted order by range, and the returned list is
         * also guaranteed to be sorted.
         */
        fun <T, Point : Comparable<Point>, Range : BoundedRange<Point, Range>> update(
            entries: List<Pair<Range, T>>,
            range: Range,
            newValue: T,
            resolve: (range: Range, old: T, new: T) -> T,
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
                if (newRange == null) {
                    // Nothing new to update anymore, but we need to retain remaining old ranges
                    append(oldRange to oldValue)
                    continue
                }
                when (val relation = oldRange.relationTo(newRange)) {
                    is BoundedRange.Relation.LeftTo -> {
                        // Old ranges to the left are not affected by update
                        append(oldRange to oldValue)
                    }
                    is BoundedRange.Relation.Overlap -> {
                        // Any remainder on the left side can just be added as is
                        relation.left?.let {
                            append(it.range to if (it.isFirst) oldValue else newValue)
                        }
                        // Overlap needs to be resolved and then added. The `append` function
                        // handles all merging of adjacent same-value ranges, so we don't need to
                        // handle that here
                        append(relation.overlap to resolve(relation.overlap, oldValue, newValue))
                        // Right side needs to be looked at more carefully, because we need to do
                        // very different things depending on where any remainder comes from, if
                        // there is any
                        newRange =
                            relation.right?.let { remainder ->
                                if (remainder.isFirst) {
                                    // Remainder comes from the old range, so the new range was
                                    // completely in the overlap
                                    append(remainder.range to oldValue)
                                    null
                                } else {
                                    // Remainder comes from the new range, and the overlap was
                                    // resolved so use the remainder as the new range from now on
                                    remainder.range
                                }
                            }
                    }
                    is BoundedRange.Relation.RightTo -> {
                        // If the old range is to the right of the new range, we are guaranteed to
                        // never see more overlaps
                        append(newRange to newValue)
                        append(oldRange to oldValue)
                        newRange = null
                    }
                }
            }
            // If we didn't consume the new range completely while processing overlaps, the new
            // range is guaranteed to the right of all previously appended ranges
            newRange?.let { append(it to newValue) }
            return result
        }

        /**
         * Removes a range from a sorted non-overlapping list of entries.
         *
         * Returns a new list containing entries which are guaranteed to not contain any of the
         * points in the given range. The given list *must* contain elements in sorted order, and
         * the returned list is also guaranteed to be sorted.
         */
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
