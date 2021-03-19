// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.utils

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

val zoneId: ZoneId = ZoneId.of("Europe/Helsinki")

class EvakaClock {
    companion object {
        var instant: Instant? = null
            private set

        fun useRealTime() {
            instant = null
        }

        fun set(zoned: ZonedDateTime) {
            instant = zoned.toInstant()
        }

        fun set(date: LocalDate, time: LocalTime = LocalTime.MIDNIGHT) =
            set(ZonedDateTime.of(date, time, zoneId))

        fun set(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0, second: Int = 0) =
            set(LocalDate.of(year, month, day), LocalTime.of(hour, minute, second))

        private val zoned: ZonedDateTime
            get() = instant
                ?.let { ZonedDateTime.ofInstant(it, zoneId) }
                ?: throw IllegalStateException("Time not set")

        fun plusSeconds(seconds: Int) {
            instant = instant?.plusSeconds(seconds.toLong())
        }

        fun plusMinutes(minutes: Int) {
            instant = zoned.plusMinutes(minutes.toLong()).toInstant()
        }

        fun plusHours(hours: Int) {
            instant = zoned.plusHours(hours.toLong()).toInstant()
        }

        fun plusDays(days: Int) {
            instant = zoned.plusDays(days.toLong()).toInstant()
        }

        fun plusMonths(months: Int) {
            instant = zoned.plusMonths(months.toLong()).toInstant()
        }

        fun plusYears(years: Int) {
            instant = zoned.plusYears(years.toLong()).toInstant()
        }
    }
}

class EInstant {
    companion object {
        fun now(): Instant = EvakaClock.instant ?: Instant.now()
    }
}

class EZonedDateTime {
    companion object {
        fun now(): ZonedDateTime = EvakaClock.instant?.let { ZonedDateTime.ofInstant(it, zoneId) } ?: ZonedDateTime.now(zoneId)
    }
}

class EOffsetDateTime {
    companion object {
        fun now(): OffsetDateTime = EvakaClock.instant?.let { OffsetDateTime.ofInstant(it, zoneId) } ?: OffsetDateTime.now(zoneId)
    }
}

class ELocalDateTime {
    companion object {
        fun now(): LocalDateTime = EvakaClock.instant?.let { LocalDateTime.ofInstant(it, zoneId) } ?: LocalDateTime.now(zoneId)
    }
}

class ELocalDate {
    companion object {
        fun now(): LocalDate = EvakaClock.instant?.let { LocalDate.ofInstant(it, zoneId) } ?: LocalDate.now(zoneId)
    }
}
