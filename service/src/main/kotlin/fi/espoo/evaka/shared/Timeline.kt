// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared

import fi.espoo.evaka.shared.domain.ClosedPeriod
import java.time.LocalDate

/**
 * A timeline is basically an immutable set of dates, but provides simple read/write operations that involve periods
 * instead of individual dates.
 *
 * Invariants:
 *   * no overlapping periods (they are merged when adding to the timeline)
 *   * no adjacent periods (they are merged when adding to the timeline)
 *   * functions that return dates/periods always return them in ascending order
 *
 * Note: this implementation is *not* very efficient
 */
class Timeline private constructor(private val periods: List<ClosedPeriod>) : Iterable<ClosedPeriod> by periods {
    constructor() : this(emptyList())

    fun add(period: ClosedPeriod) = this.periods.partition { it.overlaps(period) || it.adjacentTo(period) }
        .let { (conflicts, unchanged) ->
            val result = unchanged.toMutableList()
            result.add(
                conflicts.fold(period) { acc, it ->
                    ClosedPeriod(
                        minOf(it.start, acc.start),
                        maxOf(it.end, acc.end)
                    )
                }
            )
            result.sortBy { it.start }
            Timeline(result)
        }

    fun addAll(periods: Iterable<ClosedPeriod>) = periods.fold(this) { timeline, period -> timeline.add(period) }
    fun addAll(periods: Sequence<ClosedPeriod>) = periods.fold(this) { timeline, period -> timeline.add(period) }

    fun add(date: LocalDate) = this.add(ClosedPeriod(start = date, end = date))

    fun remove(period: ClosedPeriod) = this.periods.partition { it.overlaps(period) }
        .let { (conflicts, unchanged) ->
            val result = unchanged.toMutableList()
            for (conflict in conflicts) {
                conflict.intersection(period)?.let { intersection ->
                    if (conflict.start != intersection.start) {
                        result.add(ClosedPeriod(start = conflict.start, end = intersection.start.minusDays(1)))
                    }
                    if (conflict.end != intersection.end) {
                        result.add(ClosedPeriod(start = intersection.end.plusDays(1), end = conflict.end))
                    }
                }
            }
            result.sortBy { it.start }
            Timeline(result)
        }

    fun remove(date: LocalDate) = this.remove(ClosedPeriod(start = date, end = date))
    fun removeAll(periods: Iterable<ClosedPeriod>) = periods.fold(this) { timeline, period -> timeline.remove(period) }
    fun removeAll(periods: Sequence<ClosedPeriod>) = periods.fold(this) { timeline, period -> timeline.remove(period) }

    fun includes(date: LocalDate) = this.periods.any { it.includes(date) }
    fun contains(period: ClosedPeriod) = this.periods.any { it.contains(period) }

    fun periods() = this.periods.asSequence()
    fun dates() = this.periods.asSequence().flatMap { it.dates() }

    companion object {
        fun of(): Timeline = Timeline()
        fun of(vararg periods: ClosedPeriod): Timeline = Timeline().addAll(periods.asIterable())
        fun of(periods: Collection<ClosedPeriod>): Timeline = Timeline().addAll(periods)
    }
}
