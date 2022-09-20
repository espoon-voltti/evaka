// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.domain

import com.fasterxml.jackson.annotation.JsonFormat
import fi.espoo.evaka.ForceCodeGenType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters.lastDayOfMonth
import org.jdbi.v3.core.mapper.PropagateNull

fun orMax(date: LocalDate?): LocalDate = date ?: LocalDate.MAX

fun minEndDate(first: LocalDate?, second: LocalDate?): LocalDate? {
    return when {
        first == null -> second
        second == null -> first
        else -> minOf(first, second)
    }
}

fun maxEndDate(first: LocalDate?, second: LocalDate?): LocalDate? {
    return when {
        first == null || second == null -> null
        else -> maxOf(first, second)
    }
}

/** A closed (inclusive) date range with finite start and end */
data class FiniteDateRange(val start: LocalDate, val end: LocalDate) {
    init {
        require(start <= end) {
            "Attempting to initialize invalid finite date range with start: $start, end: $end"
        }
    }

    fun asDateRange(): DateRange = DateRange(start, end)

    /** Returns true if this date range fully contains the given date range. */
    fun contains(other: FiniteDateRange) = this.start <= other.start && other.end <= this.end

    /** Returns true if this date range fully contains the given date range. */
    fun contains(other: DateRange) = this.asDateRange().contains(other)

    /** Returns true if this date range includes the given date. */
    fun includes(date: LocalDate) = this.start <= date && date <= this.end

    /** Returns true if this date range overlaps at least partially with the given date range. */
    fun overlaps(other: FiniteDateRange) = this.start <= other.end && other.start <= this.end

    /** Returns true if this date range overlaps at least partially with the given date range. */
    fun overlaps(other: DateRange) = this.asDateRange().overlaps(other)

    fun leftAdjacentTo(other: FiniteDateRange): Boolean = this.end.plusDays(1) == other.start
    fun rightAdjacentTo(other: FiniteDateRange): Boolean = other.end.plusDays(1) == this.start
    fun adjacentTo(other: FiniteDateRange): Boolean =
        leftAdjacentTo(other) || rightAdjacentTo(other)

    fun intersection(other: FiniteDateRange): FiniteDateRange? {
        val start = maxOf(this.start, other.start)
        val end = minOf(this.end, other.end)
        return if (start <= end) FiniteDateRange(start, end) else null
    }

    fun complement(other: FiniteDateRange): List<FiniteDateRange> {
        return when {
            !other.overlaps(this) -> listOf(this)
            other.contains(this) -> emptyList()
            other.start <= this.start && other.end < this.end ->
                listOf(FiniteDateRange(other.end.plusDays(1), this.end))
            other.start > this.start && other.end >= this.end ->
                listOf(FiniteDateRange(this.start, other.start.minusDays(1)))
            other.start > this.start && other.end < this.end ->
                listOf(
                    FiniteDateRange(this.start, other.start.minusDays(1)),
                    FiniteDateRange(other.end.plusDays(1), this.end)
                )
            else -> error("Bug: missing when case")
        }
    }

    fun complement(others: Collection<FiniteDateRange>): List<FiniteDateRange> {
        return others.fold(
            initial = listOf(this),
            operation = { acc, other -> acc.flatMap { it.complement(other) } }
        )
    }

    /** Returns a lazy sequence of all dates included in this date range. */
    fun dates(): Sequence<LocalDate> =
        generateSequence(start) { if (it < end) it.plusDays(1) else null }

    /** Returns the total duration of this date range counted in days. */
    fun durationInDays(): Long =
        ChronoUnit.DAYS.between(start, end.plusDays(1)) // adjust to exclusive range

    companion object {
        fun ofMonth(year: Int, month: Month): FiniteDateRange {
            val start = LocalDate.of(year, month, 1)
            val end = start.with(lastDayOfMonth())
            return FiniteDateRange(start, end)
        }
    }
}

/** A closed (inclusive) date range with finite start and possibly infinite (= null) end */
data class DateRange(val start: LocalDate, val end: LocalDate?) {
    init {
        check(start <= orMax(end)) {
            "Attempting to initialize invalid date range with start: $start, end: $end"
        }
    }

    fun asFiniteDateRange(): FiniteDateRange? = end?.let { FiniteDateRange(start, it) }
    fun asFiniteDateRange(defaultEnd: LocalDate): FiniteDateRange =
        FiniteDateRange(start, end ?: defaultEnd)

    fun contains(other: DateRange) =
        this.start <= other.start && orMax(other.end) <= orMax(this.end)
    fun contains(other: FiniteDateRange) = contains(other.asDateRange())

    fun includes(date: LocalDate) = this.start <= date && date <= orMax(this.end)

    fun overlaps(other: DateRange) =
        this.start <= orMax(other.end) && other.start <= orMax(this.end)
    fun overlaps(other: FiniteDateRange) = overlaps(other.asDateRange())

    fun intersection(other: DateRange): DateRange? {
        val start = maxOf(this.start, other.start)
        val end = if (this.end == null || other.end == null) null else minOf(this.end, other.end)
        return if (start <= end) DateRange(start, end) else null
    }

    companion object {
        fun ofMonth(year: Int, month: Month): DateRange {
            val start = LocalDate.of(year, month, 1)
            val end = start.with(lastDayOfMonth())
            return DateRange(start, end)
        }
    }
}

private fun periodsCanMerge(first: DateRange, second: DateRange): Boolean =
    first.overlaps(second) || first.end?.let { first.end.plusDays(1) == second.start } ?: false

private fun minimalCover(first: DateRange, second: DateRange): DateRange =
    DateRange(
        minOf(first.start, second.start),
        if (first.end == null || second.end == null) null else maxOf(first.end, second.end)
    )

private fun <T> simpleEquals(a: T, b: T): Boolean = a == b

fun <T> mergePeriods(
    values: List<Pair<DateRange, T>>,
    equals: (T, T) -> Boolean = ::simpleEquals
): List<Pair<DateRange, T>> {
    return values
        .sortedBy { (period, _) -> period.start }
        .fold(listOf()) { periods, (period, value) ->
            when {
                periods.isEmpty() -> listOf(period to value)
                else ->
                    periods.last().let { (lastPeriod, lastValue) ->
                        when {
                            equals(lastValue, value) && periodsCanMerge(lastPeriod, period) ->
                                periods.dropLast(1) + (minimalCover(lastPeriod, period) to value)
                            else -> periods + (period to value)
                        }
                    }
            }
        }
}

fun asDistinctPeriods(periods: List<DateRange>, spanningPeriod: DateRange): List<DateRange> {
    // Includes the end dates with one day added to fill in gaps in original periods
    val allStartDates =
        (periods.flatMap { listOf(it.start, it.end?.plusDays(1)) } + spanningPeriod.start)
            .asSequence()
            .filterNotNull()
            .filter { spanningPeriod.start <= it && it <= orMax(spanningPeriod.end) }
            .distinct()
            .sorted()

    // Includes the start dates with one day subtracted to fill in gaps in original periods
    val allEndDates =
        (periods.flatMap { listOf(it.end, it.start.minusDays(1)) } + spanningPeriod.end)
            .filter { spanningPeriod.start <= orMax(it) && orMax(it) <= orMax(spanningPeriod.end) }
            .distinct()
            .sortedBy { orMax(it) }

    return allStartDates
        .map { start -> DateRange(start, allEndDates.find { end -> start <= orMax(end) }) }
        .toList()
}

fun LocalDate.isWeekend() =
    this.dayOfWeek == DayOfWeek.SATURDAY || this.dayOfWeek == DayOfWeek.SUNDAY

fun LocalDate.toFiniteDateRange(): FiniteDateRange = FiniteDateRange(this, this)

data class TimeRange(
    @ForceCodeGenType(String::class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    @PropagateNull
    val start: LocalTime,
    @ForceCodeGenType(String::class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    @PropagateNull
    val end: LocalTime
)
