// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.data

import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate
import java.util.Objects

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

    fun transpose(): Map<T, DateSet> =
        entries.groupingBy { it.second }.fold(DateSet.of()) { acc, entry -> acc.add(entry.first) }

    companion object {
        private val EMPTY: DateMap<*> = DateMap<Any>(emptyList())
        fun <T> empty(): DateMap<T> {
            @Suppress("UNCHECKED_CAST") return EMPTY as DateMap<T>
        }
        fun <T> of(vararg ranges: Pair<FiniteDateRange, T>): DateMap<T> =
            empty<T>().setAll(ranges.asSequence())
        fun <T> of(ranges: Iterable<FiniteDateRange>, value: T): DateMap<T> =
            empty<T>().setAll(ranges.asSequence().map { it to value })
        fun <T> of(ranges: Sequence<FiniteDateRange>, value: T): DateMap<T> =
            empty<T>().setAll(ranges.map { it to value })
        fun <T> of(ranges: Iterable<Pair<FiniteDateRange, T>>): DateMap<T> =
            empty<T>().setAll(ranges.asSequence())
        fun <T> of(ranges: Sequence<Pair<FiniteDateRange, T>>): DateMap<T> =
            empty<T>().setAll(ranges)
    }
}
