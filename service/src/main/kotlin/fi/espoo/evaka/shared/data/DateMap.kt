// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.data

import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate
import java.util.Objects

/**
 * An immutable data structure that is conceptually similar to a `Map<LocalDate, T>` but provides
 * batch operations that use `FiniteDateRange` parameters.
 *
 * This data structure is useful if you want to assign values to dates, and are using ranges for
 * convenience or performance reasons. For example, a `Map<LocalDate, T>` for a full year (365
 * dates) assigned to one same value contains internally 365 entries, but the equivalent
 * `DateMap<T>` contains just 1 entry.
 */
class DateMap<T> private constructor(entries: List<Pair<FiniteDateRange, T>>) :
    RangeBasedMap<T, LocalDate, FiniteDateRange, DateMap<T>>(entries) {
    override fun List<Pair<FiniteDateRange, T>>.toThis(): DateMap<T> =
        if (isEmpty()) empty() else DateMap(this)

    override fun range(start: LocalDate, end: LocalDate): FiniteDateRange =
        FiniteDateRange(start, end)

    override fun equals(other: Any?): Boolean = other is DateMap<*> && this.entries == other.entries

    override fun hashCode(): Int = Objects.hash(entries)

    override fun toString(): String =
        entries.joinToString(separator = ",", prefix = "{", postfix = "}") { (range, value) ->
            "[${range.start},${range.end}]: $value"
        }

    /**
     * Returns a transposed (= "inverted") map where every distinct value is mapped to a date set
     * containing all non-adjacent non-overlapping ranges where this value was present in the
     * original map.
     *
     * Examples with integers:
     * - { [1-100]:1 }.transpose() returns { 1: {[1-100]} }
     * - { [1-2]:1, [3-4]:3, [5-6]:1 }.transpose() returns { 1: {[1-2], [5-6]}, 3:{[3-4]} }
     */
    fun transpose(): Map<T, DateSet> =
        entries.groupingBy { it.second }.fold(DateSet.of()) { acc, entry -> acc.add(entry.first) }

    companion object {
        private val EMPTY: DateMap<*> = DateMap<Any>(emptyList())

        /** Returns an empty date map */
        fun <T> empty(): DateMap<T> {
            @Suppress("UNCHECKED_CAST")
            return EMPTY as DateMap<T>
        }

        /** Returns a new date map containing all the given entries */
        fun <T> of(vararg entries: Pair<FiniteDateRange, T>): DateMap<T> =
            empty<T>().setAll(entries.asSequence())

        /** Returns a new date map containing all the given ranges mapped to the given value. */
        fun <T> of(ranges: Iterable<FiniteDateRange>, value: T): DateMap<T> =
            empty<T>().setAll(ranges.asSequence().map { it to value })

        /** Returns a new date map containing all the given ranges mapped to the given value. */
        fun <T> of(ranges: Sequence<FiniteDateRange>, value: T): DateMap<T> =
            empty<T>().setAll(ranges.map { it to value })

        /** Returns a new date map containing all the given entries */
        fun <T> of(entries: Iterable<Pair<FiniteDateRange, T>>): DateMap<T> =
            empty<T>().setAll(entries.asSequence())

        /** Returns a new date map containing all the given entries */
        fun <T> of(entries: Sequence<Pair<FiniteDateRange, T>>): DateMap<T> =
            empty<T>().setAll(entries)
    }
}
