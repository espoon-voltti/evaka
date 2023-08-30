// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.data

import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate
import java.util.Objects

class Timeline private constructor(ranges: List<FiniteDateRange>) :
    RangeBasedSet<LocalDate, FiniteDateRange, Timeline>(ranges) {
    override fun List<FiniteDateRange>.toThis(): Timeline =
        if (ranges.isEmpty()) EMPTY else Timeline(ranges)
    override fun range(start: LocalDate, end: LocalDate): FiniteDateRange =
        FiniteDateRange(start, end)
    override fun equals(other: Any?): Boolean = other is Timeline && this.ranges == other.ranges
    override fun hashCode(): Int = Objects.hash(ranges)
    override fun toString(): String =
        ranges.joinToString(separator = ",", prefix = "{", postfix = "}")

    fun addAll(other: Timeline) = addAll(other.ranges())
    fun removeAll(other: Timeline) = removeAll(other.ranges())
    fun intersection(other: Timeline) = intersection(other.ranges())

    fun gaps(): Sequence<FiniteDateRange> =
        this.ranges().windowed(2).map { pair ->
            FiniteDateRange(pair[0].end.plusDays(1), pair[1].start.minusDays(1))
        }

    companion object {
        private val EMPTY = Timeline(emptyList())
        fun empty(): Timeline = EMPTY
        fun of(vararg ranges: FiniteDateRange): Timeline = empty().addAll(ranges.asSequence())
        fun of(ranges: Iterable<FiniteDateRange>): Timeline = empty().addAll(ranges)
        fun of(ranges: Sequence<FiniteDateRange>): Timeline = empty().addAll(ranges)
    }
}
