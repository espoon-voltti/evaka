// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.data

import fi.espoo.evaka.shared.domain.MidnightAwareTime
import fi.espoo.evaka.shared.domain.TimeRange
import java.util.Objects

/**
 * An immutable data structure that is conceptually similar to a `Set<LocalTime>` but provides batch
 * operations that use `TimeRange` parameters.
 */
class TimeSet private constructor(ranges: List<TimeRange>) :
    RangeBasedSet<MidnightAwareTime, TimeRange, TimeSet>(ranges) {
    override fun List<TimeRange>.toThis(): TimeSet = if (isEmpty()) EMPTY else TimeSet(this)

    override fun range(start: MidnightAwareTime, end: MidnightAwareTime): TimeRange =
        TimeRange(start, end)

    override fun equals(other: Any?): Boolean = other is TimeSet && this.ranges == other.ranges

    override fun hashCode(): Int = Objects.hash(ranges)

    override fun toString(): String =
        ranges.joinToString(separator = ",", prefix = "{", postfix = "}")

    companion object {
        private val EMPTY = TimeSet(emptyList())
        /** Returns a empty datetime set */
        fun empty(): TimeSet = EMPTY
        /** Returns a new datetime set containing all the given ranges */
        fun of(vararg ranges: TimeRange): TimeSet = empty().addAll(ranges.asSequence())
        /** Returns a new datetime set containing all the given ranges */
        fun of(ranges: Iterable<TimeRange>): TimeSet = empty().addAll(ranges)
        /** Returns a new datetime set containing all the given ranges */
        fun of(ranges: Sequence<TimeRange>): TimeSet = empty().addAll(ranges)
    }
}
