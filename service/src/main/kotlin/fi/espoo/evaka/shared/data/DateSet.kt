// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.data

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate
import java.util.Objects

/**
 * An immutable data structure that is conceptually similar to a `Set<LocalDate>` but provides batch
 * operations that use `FiniteDateRange` parameters.
 */
@JsonSerialize(using = DateSetJsonSerializer::class)
class DateSet private constructor(
    ranges: List<FiniteDateRange>
) : RangeBasedSet<LocalDate, FiniteDateRange, DateSet>(ranges) {
    override fun List<FiniteDateRange>.toThis(): DateSet = if (isEmpty()) EMPTY else DateSet(this)

    override fun range(
        start: LocalDate,
        end: LocalDate
    ): FiniteDateRange = FiniteDateRange(start, end)

    override fun equals(other: Any?): Boolean = other is DateSet && this.ranges == other.ranges

    override fun hashCode(): Int = Objects.hash(ranges)

    override fun toString(): String = ranges.joinToString(separator = ",", prefix = "{", postfix = "}")

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
    }
}

class DateSetJsonSerializer : JsonSerializer<DateSet>() {
    override fun serialize(
        value: DateSet,
        gen: JsonGenerator,
        serializers: SerializerProvider
    ) = serializers.defaultSerializeValue(value.ranges().toList(), gen)
}
