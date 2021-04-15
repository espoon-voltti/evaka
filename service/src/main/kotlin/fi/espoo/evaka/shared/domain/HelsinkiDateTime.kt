// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.domain

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.util.StdConverter
import fi.espoo.evaka.shared.utils.europeHelsinki
import java.time.Clock
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month
import java.time.ZonedDateTime

fun Instant.toHelsinkiDateTime(): HelsinkiDateTime = HelsinkiDateTime.from(this)
fun ZonedDateTime.toHelsinkiDateTime(): HelsinkiDateTime = HelsinkiDateTime.from(this)

/**
 * A timestamp in Europe/Helsinki timezone
 */
@JsonSerialize(converter = HelsinkiDateTime.ToJson::class)
@JsonDeserialize(converter = HelsinkiDateTime.FromJson::class)
data class HelsinkiDateTime private constructor(private val instant: Instant) : Comparable<HelsinkiDateTime> {
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

    fun plusYears(years: Long): HelsinkiDateTime = update { it.plusYears(years) }
    fun plusMonths(months: Long): HelsinkiDateTime = update { it.plusMonths(months) }
    fun plusWeeks(weeks: Long): HelsinkiDateTime = update { it.plusWeeks(weeks) }
    fun plusDays(days: Long): HelsinkiDateTime = update { it.plusDays(days) }
    fun plusHours(hours: Long): HelsinkiDateTime = update { it.plusHours(hours) }
    fun plusMinutes(minutes: Long): HelsinkiDateTime = update { it.plusMinutes(minutes) }
    fun plusSeconds(seconds: Long): HelsinkiDateTime = update { it.plusSeconds(seconds) }

    fun isAfter(other: HelsinkiDateTime): Boolean = this.instant.isAfter(other.instant)
    fun isBefore(other: HelsinkiDateTime): Boolean = this.instant.isBefore(other.instant)

    fun toLocalDateTime(): LocalDateTime = LocalDateTime.ofInstant(instant, europeHelsinki)
    fun toLocalTime(): LocalTime = LocalTime.ofInstant(instant, europeHelsinki)
    fun toLocalDate(): LocalDate = LocalDate.ofInstant(instant, europeHelsinki)
    fun toInstant(): Instant = instant
    fun toZonedDateTime(): ZonedDateTime = ZonedDateTime.ofInstant(instant, europeHelsinki)

    /**
     * Returns the amount of time elapsed since the given timestamp
     */
    fun durationSince(other: HelsinkiDateTime): Duration = Duration.between(other.toZonedDateTime(), this.toZonedDateTime())

    /**
     * Returns the amount of time elapsed since this timestamp
     */
    fun elapsed(clock: Clock? = Clock.systemUTC()): Duration = now(clock).durationSince(this)

    private inline fun update(crossinline f: (ZonedDateTime) -> ZonedDateTime): HelsinkiDateTime = from(f(toZonedDateTime()).toInstant())

    override fun compareTo(other: HelsinkiDateTime): Int = this.instant.compareTo(other.instant)
    override fun toString(): String = toZonedDateTime().toString()

    companion object {
        fun of(date: LocalDate, time: LocalTime): HelsinkiDateTime = from(ZonedDateTime.of(date, time, europeHelsinki))
        fun of(dateTime: LocalDateTime): HelsinkiDateTime = from(ZonedDateTime.of(dateTime, europeHelsinki))
        fun now(clock: Clock? = Clock.systemUTC()): HelsinkiDateTime = HelsinkiDateTime(Instant.now(clock))
        fun from(value: Instant): HelsinkiDateTime = HelsinkiDateTime(value)
        fun from(value: ZonedDateTime): HelsinkiDateTime = HelsinkiDateTime(value.toInstant())
    }

    class FromJson : StdConverter<ZonedDateTime, HelsinkiDateTime>() {
        override fun convert(value: ZonedDateTime): HelsinkiDateTime = value.toHelsinkiDateTime()
    }

    class ToJson : StdConverter<HelsinkiDateTime, ZonedDateTime>() {
        override fun convert(value: HelsinkiDateTime): ZonedDateTime = value.toZonedDateTime()
    }
}
