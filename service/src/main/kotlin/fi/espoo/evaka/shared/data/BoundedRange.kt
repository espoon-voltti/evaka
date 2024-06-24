// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.data

/**
 * A bounded range which has a finite start and end.
 *
 * This interface does not specify whether each endpoint is *open* (not included in the range) or
 * *closed* (included in the range), but the actual range type implementing this interface may do
 * so.
 */
interface BoundedRange<Point : Comparable<Point>, This : BoundedRange<Point, This>> {
    val start: Point
    val end: Point

    /**
     * Returns true if this range overlaps at least partially with the given range.
     *
     * Example: [1,5] overlaps with [4,6] but not with [6,8]
     */
    fun overlaps(other: This): Boolean

    /**
     * Returns true if this range fully contains the given range.
     *
     * Example: [1,5] fully contains [3,4] but not [4,6]
     */
    fun contains(other: This): Boolean = this.start <= other.start && other.end <= this.end

    /**
     * Returns true if this range is immediately left adjacent to the given range.
     *
     * Example: [1,5] is left adjacent to [6,7] but not to [7,8]
     */
    fun leftAdjacentTo(other: This): Boolean

    /**
     * Returns true if this range is immediately right adjacent to the given range.
     *
     * Example: [6,7] is right adjacent to [1,5] but [7,8] isn't
     */
    fun rightAdjacentTo(other: This): Boolean

    /**
     * Returns true if this range is strictly left to (= ends before) the given range.
     *
     * If true, there's no overlap, but a gap between the ranges may or may not be present.
     *
     * Example: [2,3] and [2,5] are both strictly left to [6,7] but [2,6] isn't
     */
    fun strictlyLeftTo(other: This): Boolean

    /**
     * Returns true if this range is strictly right to (= starts after) the given range.
     *
     * If true, there's no overlap, but a gap between the ranges may or may not be present.
     *
     * Example: [4,6] and [5,6] are both strictly right to [2,3] but [3,6] isn't
     */
    fun strictlyRightTo(other: This): Boolean

    /**
     * Returns true if this range is immediately left or right adjacent to the given range.
     *
     * Example: [4,5] is adjacent to [2,3] and [6,7] but not [1,2] or [7,8]
     */
    fun adjacentTo(other: This): Boolean = leftAdjacentTo(other) || rightAdjacentTo(other)

    /**
     * Calculates the intersection of this range with the given range, if there is any.
     *
     * Returns null if the ranges don't overlap, so there is no intersection. Examples:
     * - [2,5].intersect([4,6]) returns [4,5]
     * - [2,5].intersect([5,6]) returns [5,5]
     * - [2,5].intersect([6,7]) returns null
     */
    fun intersection(other: This): This?

    /**
     * Returns a range representing the gap between this range and the given range, if there is one.
     *
     * Returns null if the ranges are adjacent or overlap, so there is no gap. Examples:
     * - [2,3].gap([6,7]) returns [4,5]
     * - [2,5].gap([6,7]) returns null
     * - [2,5].gap([4,7]) returns null
     */
    fun gap(other: This): This?

    /**
     * Subtracts the given range from this range, returning 0 to 2 ranges representing the
     * remainders.
     *
     * Examples:
     * - [2,5].subtract([1,6]) returns {}
     * - [2,5].subtract([2,3]) returns {[4,5]}
     * - [2,5].subtract([3,4]) returns {[2,2], [5,5]}
     */
    fun subtract(other: This): SubtractResult<This>

    operator fun minus(other: This): SubtractResult<This> = subtract(other)

    /**
     * Merges this range with the given range, returning the smallest range that contains both
     * ranges.
     *
     * Example: [1,2].merge([5,6]) returns [1,6]
     */
    fun merge(other: This): This

    /**
     * Returns true if this range includes the given point.
     *
     * Example: [3,4] includes 3 and 4 but not 5
     */
    fun includes(point: Point): Boolean

    /**
     * Returns a description of the relation between this range and the given range.
     *
     * There are three possibilities:
     * - this range is strictly left to the given range
     * - this range has some overlap with the given range
     * - this range is strictly right to the given range
     */
    fun relationTo(other: This): Relation<This>

    /** Describes the relation between two ranges ("first" and "second"). */
    sealed class Relation<Range> {
        /**
         * First range is strictly left to the second range (= the first range ends before the
         * second range)
         */
        data class LeftTo<Range>(
            val gap: Range?
        ) : Relation<Range>()

        /** Ranges overlap at least partially */
        data class Overlap<Range>(
            /** A possible remainder extending to the left of the overlap */
            val left: Remainder<Range>?,
            /** The overlapping part of the ranges */
            val overlap: Range,
            /** A possible remainder extending to the right of the overlap */
            val right: Remainder<Range>?
        ) : Relation<Range>()

        /**
         * First range is strictly right to the second range (= the second range ends before the
         * first range)
         */
        data class RightTo<Range>(
            val gap: Range?
        ) : Relation<Range>()

        /**
         * Describes the remainder extending to the left or right of an overlap between two ranges
         */
        data class Remainder<Range>(
            val range: Range,
            /**
             * `true` if the remainder belongs to the first range, `false` if it belongs to the
             * second
             */
            val isFirst: Boolean
        )
    }

    sealed class SubtractResult<out This> : Iterable<This> {
        abstract val left: This?
        abstract val right: This?

        object None : SubtractResult<Nothing>() {
            override val left: Nothing? = null
            override val right: Nothing? = null

            override fun iterator(): Iterator<Nothing> = sequenceOf<Nothing>().iterator()
        }

        data class LeftRemainder<This>(
            override val left: This
        ) : SubtractResult<This>() {
            override val right: Nothing? = null

            override fun iterator(): Iterator<This> = sequenceOf(left).iterator()
        }

        data class RightRemainder<This>(
            override val right: This
        ) : SubtractResult<This>() {
            override val left: Nothing? = null

            override fun iterator(): Iterator<This> = sequenceOf(right).iterator()
        }

        data class Split<This>(
            override val left: This,
            override val right: This
        ) : SubtractResult<This>() {
            override fun iterator(): Iterator<This> = sequenceOf(left, right).iterator()
        }

        data class Original<This>(
            val range: This
        ) : SubtractResult<This>() {
            override val left: This? = null
            override val right: This? = null

            override fun iterator(): Iterator<This> = sequenceOf(range).iterator()
        }
    }
}
