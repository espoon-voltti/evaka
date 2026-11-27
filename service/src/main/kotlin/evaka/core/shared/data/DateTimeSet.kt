// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.shared.data

import evaka.core.shared.domain.FiniteDateTimeRange
import evaka.core.shared.domain.HelsinkiDateTime
import java.time.Duration
import java.util.Objects

/**
 * An immutable data structure that is conceptually similar to a `Set<HelsinkiDateTime>` but
 * provides batch operations that use `FiniteDateTimeRange` parameters.
 */
class DateTimeSet private constructor(ranges: List<FiniteDateTimeRange>) :
    RangeBasedSet<HelsinkiDateTime, FiniteDateTimeRange, DateTimeSet>(ranges) {
    override fun List<FiniteDateTimeRange>.toThis(): DateTimeSet =
        if (isEmpty()) EMPTY else DateTimeSet(this)

    override fun range(start: HelsinkiDateTime, end: HelsinkiDateTime): FiniteDateTimeRange =
        FiniteDateTimeRange(start, end)

    override fun range(point: HelsinkiDateTime): FiniteDateTimeRange =
        FiniteDateTimeRange(point, point.plus(Duration.ofNanos(1000)))

    override fun equals(other: Any?): Boolean = other is DateTimeSet && this.ranges == other.ranges

    override fun hashCode(): Int = Objects.hash(ranges)

    override fun toString(): String =
        ranges.joinToString(separator = ",", prefix = "{", postfix = "}")

    companion object {
        private val EMPTY = DateTimeSet(emptyList())

        /** Returns a empty datetime set */
        fun empty(): DateTimeSet = EMPTY

        /** Returns a new datetime set containing all the given ranges */
        fun of(vararg ranges: FiniteDateTimeRange): DateTimeSet =
            empty().addAll(ranges.asSequence())

        /** Returns a new datetime set containing all the given ranges */
        fun of(ranges: Iterable<FiniteDateTimeRange>): DateTimeSet = empty().addAll(ranges)

        /** Returns a new datetime set containing all the given ranges */
        fun of(ranges: Sequence<FiniteDateTimeRange>): DateTimeSet = empty().addAll(ranges)
    }
}
