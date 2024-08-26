// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.data

import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.HelsinkiDateTimeRange
import java.util.Objects

/**
 * An immutable data structure that is conceptually similar to a `Map<HelsinkiDateTime, T>` but
 * provides batch operations that use `HelsinkiDateTimeRange` parameters.
 *
 * This data structure is useful if you want to assign values to timestamps, and are using ranges
 * for convenience or performance reasons. For example, a `Map<HelsinkiDateTime, T>` for a full day
 * (24 hours -> 86 400 000 000 microseconds) assigned to one same value contains internally 86 400
 * 000 000 entries, but the equivalent `DateTimeMap<T>` contains just 1 entry.
 */
class DateTimeMap<T> private constructor(entries: List<Pair<HelsinkiDateTimeRange, T>>) :
    RangeBasedMap<T, HelsinkiDateTime, HelsinkiDateTimeRange, DateTimeMap<T>>(entries) {
    override fun List<Pair<HelsinkiDateTimeRange, T>>.toThis(): DateTimeMap<T> =
        if (isEmpty()) empty() else DateTimeMap(this)

    override fun range(start: HelsinkiDateTime, end: HelsinkiDateTime): HelsinkiDateTimeRange =
        HelsinkiDateTimeRange(start, end)

    override fun equals(other: Any?): Boolean =
        other is DateTimeMap<*> && this.entries == other.entries

    override fun hashCode(): Int = Objects.hash(entries)

    override fun toString(): String =
        entries.joinToString(separator = ",", prefix = "{", postfix = "}") { (range, value) ->
            "[${range.start},${range.end}): $value"
        }

    /**
     * Returns a transposed (= "inverted") map where every distinct value is mapped to a datetime
     * set containing all non-adjacent non-overlapping ranges where this value was present in the
     * original map.
     *
     * Examples with integers:
     * - { [1-100]:1 }.transpose() returns { 1: {[1-100]} }
     * - { [1-2]:1, [3-4]:3, [5-6]:1 }.transpose() returns { 1: {[1-2], [5-6]}, 3:{[3-4]} }
     */
    fun transpose(): Map<T, DateTimeSet> =
        entries
            .groupingBy { it.second }
            .fold(DateTimeSet.of()) { acc, entry -> acc.add(entry.first) }

    companion object {
        private val EMPTY: DateTimeMap<*> = DateTimeMap<Any>(emptyList())

        fun <T> empty(): DateTimeMap<T> {
            @Suppress("UNCHECKED_CAST")
            return EMPTY as DateTimeMap<T>
        }

        /** Returns a new datetime map containing all the given entries */
        fun <T> of(vararg ranges: Pair<HelsinkiDateTimeRange, T>): DateTimeMap<T> =
            empty<T>().setAll(ranges.asSequence())

        /** Returns a new datetime map containing all the given ranges mapped to the given value. */
        fun <T> of(ranges: Iterable<HelsinkiDateTimeRange>, value: T): DateTimeMap<T> =
            empty<T>().setAll(ranges.asSequence().map { it to value })

        /** Returns a new datetime map containing all the given ranges mapped to the given value. */
        fun <T> of(ranges: Sequence<HelsinkiDateTimeRange>, value: T): DateTimeMap<T> =
            empty<T>().setAll(ranges.map { it to value })

        /** Returns a new datetime map containing all the given entries */
        fun <T> of(ranges: Iterable<Pair<HelsinkiDateTimeRange, T>>): DateTimeMap<T> =
            empty<T>().setAll(ranges.asSequence())

        /** Returns a new datetime map containing all the given entries */
        fun <T> of(ranges: Sequence<Pair<HelsinkiDateTimeRange, T>>): DateTimeMap<T> =
            empty<T>().setAll(ranges)
    }
}
