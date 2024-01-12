// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.domain

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.util.StdConverter
import fi.espoo.evaka.shared.data.BoundedRange
import java.time.Clock
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoField
import java.time.temporal.TemporalAmount

val europeHelsinki: ZoneId = ZoneId.of("Europe/Helsinki")

fun ZonedDateTime.toHelsinkiDateTime(): HelsinkiDateTime = HelsinkiDateTime.from(this)

/** A timestamp in Europe/Helsinki timezone */
@JsonSerialize(converter = HelsinkiDateTime.ToJson::class)
@JsonDeserialize(converter = HelsinkiDateTime.FromJson::class)
data class HelsinkiDateTime private constructor(private val instant: Instant) :
    Comparable<HelsinkiDateTime> {
    val year: Int
        get() = toZonedDateTime().year

    val month: Month
        get() = toZonedDateTime().month

    val dayOfMonth: Int
        get() = toZonedDateTime().dayOfMonth

    val dayOfWeek: DayOfWeek
        get() = toZonedDateTime().dayOfWeek

    val hour: Int
        get() = toZonedDateTime().hour

    val minute: Int
        get() = toZonedDateTime().minute

    val second: Int
        get() = toZonedDateTime().second

    fun minusYears(years: Long): HelsinkiDateTime = update { it.minusYears(years) }

    fun minusMonths(months: Long): HelsinkiDateTime = update { it.minusMonths(months) }

    fun minusWeeks(weeks: Long): HelsinkiDateTime = update { it.minusWeeks(weeks) }

    fun minusDays(days: Long): HelsinkiDateTime = update { it.minusDays(days) }

    fun minusHours(hours: Long): HelsinkiDateTime = update { it.minusHours(hours) }

    fun minusMinutes(minutes: Long): HelsinkiDateTime = update { it.minusMinutes(minutes) }

    fun minusSeconds(seconds: Long): HelsinkiDateTime = update { it.minusSeconds(seconds) }

    operator fun minus(duration: TemporalAmount): HelsinkiDateTime = update { it + duration }

    fun plusYears(years: Long): HelsinkiDateTime = update { it.plusYears(years) }

    fun plusMonths(months: Long): HelsinkiDateTime = update { it.plusMonths(months) }

    fun plusWeeks(weeks: Long): HelsinkiDateTime = update { it.plusWeeks(weeks) }

    fun plusDays(days: Long): HelsinkiDateTime = update { it.plusDays(days) }

    fun plusHours(hours: Long): HelsinkiDateTime = update { it.plusHours(hours) }

    fun plusMinutes(minutes: Long): HelsinkiDateTime = update { it.plusMinutes(minutes) }

    fun plusSeconds(seconds: Long): HelsinkiDateTime = update { it.plusSeconds(seconds) }

    operator fun plus(duration: TemporalAmount): HelsinkiDateTime = update { it + duration }

    fun isAfter(other: HelsinkiDateTime): Boolean = this.instant.isAfter(other.instant)

    fun isBefore(other: HelsinkiDateTime): Boolean = this.instant.isBefore(other.instant)

    fun withTime(time: LocalTime): HelsinkiDateTime = update {
        it.withHour(time.hour).withMinute(time.minute).withSecond(time.second).withNano(time.nano)
    }

    fun atStartOfDay() = withTime(LocalTime.MIN)

    fun atEndOfDay() = withTime(LocalTime.MAX)

    /**
     * Returns the Europe/Helsinki local date+time at the point in time represented by this
     * timestamp
     */
    fun toLocalDateTime(): LocalDateTime = LocalDateTime.ofInstant(instant, europeHelsinki)

    /** Returns the Europe/Helsinki local time at the point in time represented by this timestamp */
    fun toLocalTime(): LocalTime = LocalTime.ofInstant(instant, europeHelsinki)

    /** Returns the Europe/Helsinki local date at the point in time represented by this timestamp */
    fun toLocalDate(): LocalDate = LocalDate.ofInstant(instant, europeHelsinki)

    /**
     * Converts this timestamp to an `Instant`.
     *
     * The returned value represents the same point in time as this timestamp.
     */
    fun toInstant(): Instant = instant

    /**
     * Converts this timestamp to a `ZonedDateTime`.
     *
     * The returned value represents the same point in time as this timestamp, and is guaranteed to
     * use the Europe/Helsinki timezone.
     */
    fun toZonedDateTime(): ZonedDateTime = ZonedDateTime.ofInstant(instant, europeHelsinki)

    /** Returns the amount of time elapsed since the given timestamp */
    fun durationSince(other: HelsinkiDateTime): Duration =
        Duration.between(other.toZonedDateTime(), this.toZonedDateTime())

    /** Returns the amount of time elapsed since this timestamp */
    fun elapsed(clock: Clock? = Clock.systemUTC()): Duration = now(clock).durationSince(this)

    private inline fun update(crossinline f: (ZonedDateTime) -> ZonedDateTime): HelsinkiDateTime =
        from(f(toZonedDateTime()).toInstant())

    override fun compareTo(other: HelsinkiDateTime): Int = this.instant.compareTo(other.instant)

    override fun toString(): String = toZonedDateTime().toString()

    companion object {
        /**
         * Creates a `HelsinkiDateTime` of the point in time when the Europe/Helsinki local time
         * matches the given values
         */
        fun of(date: LocalDate, time: LocalTime): HelsinkiDateTime =
            from(ZonedDateTime.of(date, time, europeHelsinki))

        /**
         * Creates a `HelsinkiDateTime` of the point in time when the Europe/Helsinki local time
         * matches the given value
         */
        fun of(dateTime: LocalDateTime): HelsinkiDateTime =
            from(ZonedDateTime.of(dateTime, europeHelsinki))

        /**
         * Returns the current `HelsinkiDateTime` based on the given clock, or the system default
         * clock
         */
        fun now(clock: Clock? = Clock.systemUTC()): HelsinkiDateTime = from(Instant.now(clock))

        /**
         * Converts an `Instant` to `HelsinkiDateTime` by reinterpreting its timestamp in
         * Europe/Helsinki timezone
         */
        fun from(value: Instant): HelsinkiDateTime = HelsinkiDateTime(value.truncateNanos())

        /**
         * Converts a `ZonedDateTime` to `HelsinkiDateTime` by reinterpreting its timestamp in
         * Europe/Helsinki timezone
         */
        fun from(value: ZonedDateTime): HelsinkiDateTime =
            HelsinkiDateTime(value.toInstant().truncateNanos())

        fun atStartOfDay(date: LocalDate): HelsinkiDateTime =
            from(date.atStartOfDay(europeHelsinki))
    }

    class FromJson : StdConverter<ZonedDateTime, HelsinkiDateTime>() {
        override fun convert(value: ZonedDateTime): HelsinkiDateTime = value.toHelsinkiDateTime()
    }

    class ToJson : StdConverter<HelsinkiDateTime, ZonedDateTime>() {
        override fun convert(value: HelsinkiDateTime): ZonedDateTime = value.toZonedDateTime()
    }
}

// Truncate nanoseconds to avoid surprises when serializing to/from PostgreSQL, which only supports
// microsecond precision
private fun Instant.truncateNanos() = with(ChronoField.MICRO_OF_SECOND, nano / 1000L)

data class HelsinkiDateTimeRange(
    override val start: HelsinkiDateTime,
    override val end: HelsinkiDateTime
) : BoundedRange<HelsinkiDateTime, HelsinkiDateTimeRange> {
    init {
        check(start <= end) {
            "Attempting to initialize invalid time range with start: $start, end: $end"
        }
    }

    override fun toString(): String = "[$start,$end)"

    override fun overlaps(other: HelsinkiDateTimeRange) =
        this.start < other.end && other.start < this.end

    override fun leftAdjacentTo(other: HelsinkiDateTimeRange): Boolean = this.end == other.start

    override fun rightAdjacentTo(other: HelsinkiDateTimeRange): Boolean = other.end == this.start

    override fun strictlyLeftTo(other: HelsinkiDateTimeRange): Boolean = this.end <= other.start

    override fun strictlyRightTo(other: HelsinkiDateTimeRange): Boolean = other.end <= this.start

    override fun intersection(other: HelsinkiDateTimeRange): HelsinkiDateTimeRange? =
        tryCreate(maxOf(this.start, other.start), minOf(this.end, other.end))

    override fun gap(other: HelsinkiDateTimeRange): HelsinkiDateTimeRange? =
        tryCreate(minOf(this.end, other.end), maxOf(this.start, other.start))

    override fun subtract(
        other: HelsinkiDateTimeRange
    ): BoundedRange.SubtractResult<HelsinkiDateTimeRange> =
        if (this.overlaps(other)) {
            val left = tryCreate(this.start, other.start)
            val right = tryCreate(other.end, this.end)
            if (left != null) {
                if (right != null) {
                    BoundedRange.SubtractResult.Split(left, right)
                } else {
                    BoundedRange.SubtractResult.LeftRemainder(left)
                }
            } else {
                if (right != null) {
                    BoundedRange.SubtractResult.RightRemainder(right)
                } else {
                    BoundedRange.SubtractResult.None
                }
            }
        } else BoundedRange.SubtractResult.Original(this)

    override fun merge(other: HelsinkiDateTimeRange): HelsinkiDateTimeRange =
        HelsinkiDateTimeRange(minOf(this.start, other.start), maxOf(this.end, other.end))

    override fun includes(point: HelsinkiDateTime): Boolean =
        this.start <= point && point < this.end

    override fun relationTo(
        other: HelsinkiDateTimeRange
    ): BoundedRange.Relation<HelsinkiDateTimeRange> =
        when {
            this.end <= other.start ->
                BoundedRange.Relation.LeftTo(gap = tryCreate(this.end, other.start))
            other.end <= this.start ->
                BoundedRange.Relation.RightTo(gap = tryCreate(other.end, this.start))
            else ->
                BoundedRange.Relation.Overlap(
                    left =
                        when {
                            this.start < other.start ->
                                BoundedRange.Relation.Remainder(
                                    range = HelsinkiDateTimeRange(this.start, other.start),
                                    isFirst = true
                                )
                            other.start < this.start ->
                                BoundedRange.Relation.Remainder(
                                    range = HelsinkiDateTimeRange(other.start, this.start),
                                    isFirst = false
                                )
                            else -> null
                        },
                    overlap =
                        HelsinkiDateTimeRange(
                            maxOf(this.start, other.start),
                            minOf(this.end, other.end)
                        ),
                    right =
                        when {
                            other.end < this.end ->
                                BoundedRange.Relation.Remainder(
                                    range = HelsinkiDateTimeRange(other.end, this.end),
                                    isFirst = true
                                )
                            this.end < other.end ->
                                BoundedRange.Relation.Remainder(
                                    range = HelsinkiDateTimeRange(this.end, other.end),
                                    isFirst = false
                                )
                            else -> null
                        }
                )
        }

    fun getDuration() = Duration.between(start.toInstant(), end.toInstant())

    companion object {
        fun tryCreate(start: HelsinkiDateTime, end: HelsinkiDateTime): HelsinkiDateTimeRange? =
            if (start < end) HelsinkiDateTimeRange(start, end) else null

        fun of(date: LocalDate, startTime: LocalTime, endTime: LocalTime) =
            HelsinkiDateTimeRange(
                HelsinkiDateTime.of(date, startTime),
                HelsinkiDateTime.of(date, endTime)
            )
    }
}
