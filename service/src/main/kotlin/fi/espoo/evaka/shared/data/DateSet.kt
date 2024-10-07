// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.data

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.util.StdConverter
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.toFiniteDateRange
import java.time.LocalDate
import java.util.Objects

/**
 * An immutable data structure that is conceptually similar to a `Set<LocalDate>` but provides batch
 * operations that use `FiniteDateRange` parameters.
 */
@JsonSerialize(converter = DateSet.ToJson::class)
@JsonDeserialize(converter = DateSet.FromJson::class)
class DateSet private constructor(ranges: List<FiniteDateRange>) :
    RangeBasedSet<LocalDate, FiniteDateRange, DateSet>(ranges) {
    override fun List<FiniteDateRange>.toThis(): DateSet = if (isEmpty()) EMPTY else DateSet(this)

    override fun range(start: LocalDate, end: LocalDate): FiniteDateRange =
        FiniteDateRange(start, end)

    override fun equals(other: Any?): Boolean = other is DateSet && this.ranges == other.ranges

    override fun hashCode(): Int = Objects.hash(ranges)

    override fun toString(): String =
        ranges.joinToString(separator = ",", prefix = "{", postfix = "}")

    companion object {
        private val EMPTY = DateSet(emptyList())

        /** Returns an empty date set */
        fun empty(): DateSet = EMPTY

        /** Returns a new date set containing all the given ranges */
        fun of(vararg ranges: FiniteDateRange): DateSet = empty().addAll(ranges.asSequence())

        /** Returns a new date set containing all the given ranges */
        fun of(ranges: Iterable<FiniteDateRange>): DateSet = empty().addAll(ranges)

        /** Returns a new date set containing all the given ranges */
        fun of(ranges: Sequence<FiniteDateRange>): DateSet = empty().addAll(ranges)

        /**
         * Returns a new date set containing all the given dates.
         *
         * *Note that DateSet is not an efficient data structure for random unconnected dates*. If
         * you have a large amount of dates that cannot be joined into a smaller amount of ranges,
         * consider using `Set<LocalDate>` instead.
         */
        fun ofDates(vararg dates: LocalDate): DateSet =
            empty().addAll(dates.asSequence().map { it.toFiniteDateRange() })

        /**
         * Returns a new date set containing all the given dates.
         *
         * *Note that DateSet is not an efficient data structure for random unconnected dates*. If
         * you have a large amount of dates that cannot be joined into a smaller amount of ranges,
         * consider using `Set<LocalDate>` instead.
         */
        fun ofDates(dates: Iterable<LocalDate>): DateSet =
            empty().addAll(dates.asSequence().map { it.toFiniteDateRange() })

        /**
         * Returns a new date set containing all the given dates.
         *
         * *Note that DateSet is not an efficient data structure for random unconnected dates*. If
         * you have a large amount of dates that cannot be joined into a smaller amount of ranges,
         * consider using `Set<LocalDate>` instead.
         */
        fun ofDates(dates: Sequence<LocalDate>): DateSet =
            empty().addAll(dates.map { it.toFiniteDateRange() })
    }

    class FromJson : StdConverter<List<FiniteDateRange>, DateSet>() {
        override fun convert(value: List<FiniteDateRange>): DateSet = DateSet.of(value)
    }

    class ToJson : StdConverter<DateSet, List<FiniteDateRange>>() {
        override fun convert(value: DateSet): List<FiniteDateRange> = value.ranges().toList()
    }
}
