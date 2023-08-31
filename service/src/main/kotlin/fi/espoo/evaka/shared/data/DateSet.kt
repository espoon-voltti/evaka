// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.data

import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate
import java.util.Objects

class DateSet private constructor(ranges: List<FiniteDateRange>) :
    RangeBasedSet<LocalDate, FiniteDateRange, DateSet>(ranges) {
    override fun List<FiniteDateRange>.toThis(): DateSet = if (isEmpty()) EMPTY else DateSet(this)
    override fun range(start: LocalDate, end: LocalDate): FiniteDateRange =
        FiniteDateRange(start, end)
    override fun equals(other: Any?): Boolean = other is DateSet && this.ranges == other.ranges
    override fun hashCode(): Int = Objects.hash(ranges)
    override fun toString(): String =
        ranges.joinToString(separator = ",", prefix = "{", postfix = "}")

    fun addAll(other: DateSet) = addAll(other.ranges())
    fun removeAll(other: DateSet) = removeAll(other.ranges())
    fun intersection(other: DateSet) = intersection(other.ranges())

    companion object {
        private val EMPTY = DateSet(emptyList())
        fun empty(): DateSet = EMPTY
        fun of(vararg ranges: FiniteDateRange): DateSet = empty().addAll(ranges.asSequence())
        fun of(ranges: Iterable<FiniteDateRange>): DateSet = empty().addAll(ranges)
        fun of(ranges: Sequence<FiniteDateRange>): DateSet = empty().addAll(ranges)
    }
}
