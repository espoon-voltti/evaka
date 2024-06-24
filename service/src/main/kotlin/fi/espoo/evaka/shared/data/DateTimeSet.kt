// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.data

import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.HelsinkiDateTimeRange
import java.util.Objects

/**
 * An immutable data structure that is conceptually similar to a `Set<HelsinkiDateTime>` but
 * provides batch operations that use `HelsinkiDateTimeRange` parameters.
 */
class DateTimeSet private constructor(
    ranges: List<HelsinkiDateTimeRange>
) : RangeBasedSet<HelsinkiDateTime, HelsinkiDateTimeRange, DateTimeSet>(ranges) {
    override fun List<HelsinkiDateTimeRange>.toThis(): DateTimeSet = if (isEmpty()) EMPTY else DateTimeSet(this)

    override fun range(
        start: HelsinkiDateTime,
        end: HelsinkiDateTime
    ): HelsinkiDateTimeRange = HelsinkiDateTimeRange(start, end)

    override fun equals(other: Any?): Boolean = other is DateTimeSet && this.ranges == other.ranges

    override fun hashCode(): Int = Objects.hash(ranges)

    override fun toString(): String = ranges.joinToString(separator = ",", prefix = "{", postfix = "}")

    companion object {
        private val EMPTY = DateTimeSet(emptyList())

        /** Returns a empty datetime set */
        fun empty(): DateTimeSet = EMPTY

        /** Returns a new datetime set containing all the given ranges */
        fun of(vararg ranges: HelsinkiDateTimeRange): DateTimeSet = empty().addAll(ranges.asSequence())

        /** Returns a new datetime set containing all the given ranges */
        fun of(ranges: Iterable<HelsinkiDateTimeRange>): DateTimeSet = empty().addAll(ranges)

        /** Returns a new datetime set containing all the given ranges */
        fun of(ranges: Sequence<HelsinkiDateTimeRange>): DateTimeSet = empty().addAll(ranges)
    }
}
