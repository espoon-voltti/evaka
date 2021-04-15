// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.domain

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.util.StdConverter
import fi.espoo.evaka.shared.utils.europeHelsinki
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime

fun Instant.toHelsinkiDateTime(): HelsinkiDateTime = HelsinkiDateTime.from(this)
fun ZonedDateTime.toHelsinkiDateTime(): HelsinkiDateTime = HelsinkiDateTime.from(this)

/**
 * A timestamp in Europe/Helsinki timezone
 */
@JsonSerialize(converter = HelsinkiDateTime.ToJson::class)
@JsonDeserialize(converter = HelsinkiDateTime.FromJson::class)
data class HelsinkiDateTime private constructor(private val instant: Instant) : Comparable<HelsinkiDateTime> {
    fun toInstant(): Instant = this.instant
    fun toZonedDateTime(): ZonedDateTime = ZonedDateTime.ofInstant(instant, europeHelsinki)

    fun update(f: (ZonedDateTime) -> ZonedDateTime): HelsinkiDateTime = HelsinkiDateTime(f(toZonedDateTime()).toInstant())

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
