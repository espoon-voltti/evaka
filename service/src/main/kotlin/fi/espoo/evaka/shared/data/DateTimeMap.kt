// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.data

import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.HelsinkiDateTimeRange
import java.util.Objects

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

    fun transpose(): Map<T, DateTimeSet> =
        entries
            .groupingBy { it.second }
            .fold(DateTimeSet.of()) { acc, entry -> acc.add(entry.first) }

    companion object {
        private val EMPTY: DateTimeMap<*> = DateTimeMap<Any>(emptyList())
        fun <T> empty(): DateTimeMap<T> {
            @Suppress("UNCHECKED_CAST") return EMPTY as DateTimeMap<T>
        }
        fun <T> of(vararg ranges: Pair<HelsinkiDateTimeRange, T>): DateTimeMap<T> =
            empty<T>().setAll(ranges.asSequence())
        fun <T> of(ranges: Iterable<HelsinkiDateTimeRange>, value: T): DateTimeMap<T> =
            empty<T>().setAll(ranges.asSequence().map { it to value })
        fun <T> of(ranges: Sequence<HelsinkiDateTimeRange>, value: T): DateTimeMap<T> =
            empty<T>().setAll(ranges.map { it to value })
        fun <T> of(ranges: Iterable<Pair<HelsinkiDateTimeRange, T>>): DateTimeMap<T> =
            empty<T>().setAll(ranges.asSequence())
        fun <T> of(ranges: Sequence<Pair<HelsinkiDateTimeRange, T>>): DateTimeMap<T> =
            empty<T>().setAll(ranges)
    }
}
