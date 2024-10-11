// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.data

/**
 * A data structure that is conceptually an immutable set of some comparable points (e.g.
 * `Set<Point>`), but most operations use bounded ranges of those same points.
 *
 * Invariants:
 * - no overlapping ranges (they are merged when adding to the set)
 * - no adjacent ranges (they are merged when adding to the set)
 * - functions that return points/ranges always return them in ascending order
 *
 * Note: this implementation is *very* inefficient
 */
abstract class RangeBasedSet<
    Point : Comparable<Point>,
    Range : BoundedRange<Point, Range>,
    This : RangeBasedSet<Point, Range, This>,
>(protected val ranges: List<Range>) {
    /** Returns a sequence of all non-adjacent ranges in the set, sorted in ascending order */
    fun ranges(): Sequence<Range> = this.ranges.asSequence()

    /**
     * Returns a sequence of all non-adjacent ranges in the set, sorted in ascending order.
     *
     * The ranges are intersections instead of the originals, so they never extend outside the given
     * range.
     */
    fun intersectRanges(range: Range): Sequence<Range> =
        partition(this.ranges, range, adjacentBelongToCenter = false) { it }
            .center
            .asSequence()
            .mapNotNull { it.intersection(range) }

    /**
     * Returns the largest range that covers all the points in all the ranges in the set, or null if
     * the set is empty.
     */
    fun spanningRange(): Range? =
        this.ranges.firstOrNull()?.let { first ->
            this.ranges.lastOrNull()?.let { last -> range(first.start, last.end) }
        }

    /**
     * Returns a sequence of all the non-adjacent gaps between the ranges in the set.
     *
     * The sequence is empty if the set is empty or has only one range.
     */
    fun gaps(): Sequence<Range> =
        this.ranges().windowed(2).mapNotNull { pair -> pair[0].gap(pair[1]) }

    /** Returns true if the set is empty. */
    fun isEmpty(): Boolean = this.ranges.isEmpty()

    /** Returns true if the set is not empty. */
    fun isNotEmpty(): Boolean = this.ranges.isNotEmpty()

    /** Returns a new set containing the given range and all currently contained ranges. */
    fun add(range: Range): This = add(this.ranges, range).toThis()

    /** Returns a new set containing all the given ranges and all currently contained ranges. */
    fun addAll(vararg ranges: Range): This = ranges.fold(this.ranges, ::add).toThis()

    /**
     * Returns a new set containing all the ranges in the given set and all currently contained
     * ranges.
     */
    fun addAll(set: RangeBasedSet<Point, Range, This>): This =
        set.ranges.fold(this.ranges, ::add).toThis()

    /** Returns a new set containing all the given ranges and all currently contained ranges. */
    fun addAll(ranges: Iterable<Range>): This = ranges.fold(this.ranges, ::add).toThis()

    /** Returns a new set containing all the given ranges and all currently contained ranges. */
    fun addAll(ranges: Sequence<Range>): This = ranges.fold(this.ranges, ::add).toThis()

    operator fun plus(range: Range): This = add(range)

    operator fun plus(set: RangeBasedSet<Point, Range, This>): This = addAll(set)

    operator fun plus(ranges: Iterable<Range>): This = addAll(ranges)

    operator fun plus(ranges: Sequence<Range>): This = addAll(ranges)

    /** Returns a new set with the given range removed from the currently contained ranges. */
    fun remove(range: Range): This = remove(this.ranges, range).toThis()

    /** Returns a new set with all the given ranges removed from the currently contained ranges. */
    fun removeAll(vararg ranges: Range): This = ranges.fold(this.ranges, ::remove).toThis()

    /**
     * Returns a new set with all the ranges in the given set removed from the currently contained
     * ranges.
     */
    fun removeAll(set: RangeBasedSet<Point, Range, This>): This =
        set.ranges.fold(this.ranges, ::remove).toThis()

    /** Returns a new set with all the given ranges removed from the currently contained ranges. */
    fun removeAll(ranges: Iterable<Range>): This = ranges.fold(this.ranges, ::remove).toThis()

    /** Returns a new set with all the given ranges removed from the currently contained ranges. */
    fun removeAll(ranges: Sequence<Range>): This = ranges.fold(this.ranges, ::remove).toThis()

    operator fun minus(range: Range): This = remove(range)

    operator fun minus(set: RangeBasedSet<Point, Range, This>): This = removeAll(set)

    operator fun minus(ranges: Iterable<Range>): This = removeAll(ranges)

    operator fun minus(ranges: Sequence<Range>): This = removeAll(ranges)

    /** Returns true if the given range is fully contained by the set. */
    fun contains(range: Range): Boolean =
        partition(this.ranges, range, adjacentBelongToCenter = false) { it }
            .center
            .any { it.contains(range) }

    /** Returns true if any of the ranges includes the given point. */
    fun includes(point: Point): Boolean = contains(range(point))

    /**
     * Returns a new set representing the intersections of currently contained ranges and ranges in
     * the given set.
     */
    fun intersection(other: RangeBasedSet<Point, Range, This>): This =
        intersection(this.ranges.iterator(), other.ranges().iterator()).toThis()

    /**
     * Returns a new set representing the intersections of currently contained ranges and the given
     * ranges.
     */
    fun intersection(other: Iterable<Range>): This =
        intersection(this.ranges.iterator(), other.iterator()).toThis()

    /**
     * Returns a new set representing the intersections of currently contained ranges and the given
     * ranges.
     */
    fun intersection(other: Sequence<Range>): This =
        intersection(this.ranges.iterator(), other.iterator()).toThis()

    /** Converts a raw sorted list of ranges to a concrete `RangeBasedSet` subclass object. */
    protected abstract fun List<Range>.toThis(): This

    /** Constructs a range from endpoints. */
    protected abstract fun range(start: Point, end: Point): Range

    /** Constructs the smallest range that includes the given point. */
    protected abstract fun range(point: Point): Range

    companion object {
        /**
         * Returns a view into the original list.
         *
         * This is sometimes better than the Kotlin standard subList(IntRange) which copies all the
         * data instead of returning a view
         */
        private fun <T> List<T>.sliceView(range: IntRange): List<T> =
            if (range.isEmpty()) emptyList() else subList(range.first, range.last + 1)

        data class Partition<T>(val left: List<T>, val center: List<T>, val right: List<T>)

        /**
         * Partitions a sorted list of ranges into left/center/right list views depending on
         * adjacentBelongToCenter.
         *
         * If adjacentBelongToCenter is true, adjacent ranges are included in the center list but
         * not left/right. If adjacentBelongToCenter is false, adjacent ranges are included in the
         * corresponding left/right list but not center.
         *
         * The returned partitioning contains *list views*, which are views into the original list
         * instead of full copies. Warning: if a mutable list is used, mutations to the original are
         * reflected in the returned views and may violate the partitioning.
         *
         * Examples:
         *
         * ranges={[1, 3], [5, 6], [9, 10]}, range=[4,7], adjacentBelongToCenter=true
         * - left: {}
         * - center: {[1, 3], [5, 6]}
         * - right: {[9, 10]}
         *
         * ranges={[1, 3], [5, 6], [9, 10]}, range=[4,7], adjacentBelongToCenter=false
         * - left: {[1, 3]}
         * - center: {[5, 6]}
         * - right: {[9, 10]}
         */
        fun <T, Point : Comparable<Point>, Range : BoundedRange<Point, Range>> partition(
            sortedList: List<T>,
            range: Range,
            adjacentBelongToCenter: Boolean,
            getRange: (T) -> Range,
        ): Partition<T> {
            // Find the smallest index of the position that does *not* belong to the left list
            val leftIdx =
                sortedList
                    .binarySearch {
                        when (val relation = getRange(it).relationTo(range)) {
                            is BoundedRange.Relation.LeftTo ->
                                if (adjacentBelongToCenter && relation.gap == null) 1 else -1
                            else -> 1
                        }
                    }
                    // If an exact match is not found, the returned index is (-insertion point + 1)
                    // Our comparison function above never returns 0 so the returned index always
                    // has this format. See binarySearch docs for more details
                    .let { -(it + 1) }

            // Find the smallest index of the position that belongs to the left list
            val rightIdx =
                sortedList
                    .binarySearch(fromIndex = leftIdx) {
                        when (val relation = getRange(it).relationTo(range)) {
                            is BoundedRange.Relation.RightTo ->
                                if (adjacentBelongToCenter && relation.gap == null) -1 else 1
                            else -> -1
                        }
                    }
                    // If an exact match is not found, the returned index is (-insertion point + 1).
                    // Our comparison function above never returns 0 so the returned index always
                    // has this format. See binarySearch docs for more details
                    .let { -(it + 1) }

            val left = 0..<leftIdx
            val center = leftIdx..<rightIdx
            val right = rightIdx..<sortedList.size
            if (!left.isEmpty() && !center.isEmpty()) require(left.last < center.first)
            if (!center.isEmpty() && !right.isEmpty()) require(center.last < right.first)
            if (!left.isEmpty() && !right.isEmpty()) require(left.last < right.first)
            return Partition(
                left = sortedList.sliceView(left),
                center = sortedList.sliceView(center),
                right = sortedList.sliceView(right),
            )
        }

        /**
         * Adds a range to a sorted list of non-overlapping ranges.
         *
         * Returns a new list that is guaranteed to contain non-adjacent ranges that cover all the
         * points in the original ranges and the given range. The given list *must* contain elements
         * in sorted order, and the returned list is also guaranteed to be sorted.
         */
        fun <Point : Comparable<Point>, Range : BoundedRange<Point, Range>> add(
            sortedRanges: List<Range>,
            range: Range,
        ): List<Range> {
            val p = partition(sortedRanges, range, adjacentBelongToCenter = true) { it }
            val result = mutableListOf<Range>()
            result += p.left
            result +=
                if (p.center.isNotEmpty()) range.merge(p.center.first()).merge(p.center.last())
                else range
            result += p.right
            return result
        }

        /**
         * Removes a range from a sorted non-overlapping list of ranges.
         *
         * Returns a new list containing ranges which are guaranteed to not contain any of the
         * points in the given range. The given list *must* contain elements in sorted order, and
         * the returned list is also guaranteed to be sorted.
         */
        fun <Point : Comparable<Point>, Range : BoundedRange<Point, Range>> remove(
            sortedRanges: List<Range>,
            range: Range,
        ): List<Range> {
            val p = partition(sortedRanges, range, adjacentBelongToCenter = false) { it }
            val result = mutableListOf<Range>()
            result += p.left
            if (p.center.isNotEmpty()) result += p.center.first() - range
            if (p.center.size > 1) result += p.center.last() - range
            result += p.right
            return result
        }

        /**
         * Calculates the intersection of two sorted iterators of non-overlapping ranges.
         *
         * The iterators *must* return elements in sorted order, and the returned list is also
         * guaranteed to be sorted.
         */
        fun <Point : Comparable<Point>, Range : BoundedRange<Point, Range>> intersection(
            iterA: Iterator<Range>,
            iterB: Iterator<Range>,
        ): List<Range> {
            fun <T : Any> Iterator<T>.nextOrNull() = if (hasNext()) next() else null
            // Intersection between two sorted lists
            // Inspiration from:
            // https://www.baeldung.com/cs/list-intersection#2-sorted-lists-approach
            var a = iterA.nextOrNull()
            var b = iterB.nextOrNull()
            val ranges = mutableListOf<Range>()
            while (a != null && b != null) {
                a.intersection(b)?.let { ranges.add(it) }
                if (a.end <= b.end) {
                    a = iterA.nextOrNull()
                } else {
                    b = iterB.nextOrNull()
                }
            }
            return ranges
        }
    }
}
