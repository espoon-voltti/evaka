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
    fun subtract(other: This): List<This>

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
}
