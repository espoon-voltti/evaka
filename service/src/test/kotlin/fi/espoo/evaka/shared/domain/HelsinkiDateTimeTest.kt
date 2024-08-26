// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.domain

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import fi.espoo.evaka.shared.config.defaultJsonMapperBuilder
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class HelsinkiDateTimeTest {
    private val jsonMapper = defaultJsonMapperBuilder().build()
    private val summerValue =
        HelsinkiDateTime.from(
            ZonedDateTime.of(LocalDate.of(2021, 4, 14), LocalTime.of(16, 2), europeHelsinki)
        )
    private val winterValue =
        HelsinkiDateTime.from(
            ZonedDateTime.of(LocalDate.of(2020, 12, 1), LocalTime.of(23, 59), europeHelsinki)
        )

    @Test
    fun `a JSON timestamp with +3 offset (with DST) is deserialized correctly`() {
        val hdt =
            jsonMapper.readValue("\"2021-04-14T16:02:00+03:00\"", HelsinkiDateTime::class.java)
        assertEquals(summerValue, hdt)
    }

    @Test
    fun `a JSON timestamp with +2 offset (without DST) is deserialized correctly`() {
        val hdt =
            jsonMapper.readValue("\"2020-12-01T23:59:00+02:00\"", HelsinkiDateTime::class.java)
        assertEquals(winterValue, hdt)
    }

    @Test
    fun `a JSON timestamp with +0 offset is deserialized correctly`() {
        val hdt =
            jsonMapper.readValue("\"2021-04-14T13:02:00+00:00\"", HelsinkiDateTime::class.java)
        assertEquals(summerValue, hdt)
    }

    @Test
    fun `a JSON timestamp with Z offset is deserialized correctly`() {
        val hdt = jsonMapper.readValue("\"2021-04-14T13:02:00Z\"", HelsinkiDateTime::class.java)
        assertEquals(summerValue, hdt)
    }

    @Test
    fun `a JSON timestamp with a ridiculous offset is deserialized correctly`() {
        val hdt =
            jsonMapper.readValue("\"2021-04-15T02:29:00+13:27\"", HelsinkiDateTime::class.java)
        assertEquals(summerValue, hdt)
    }

    @Test
    fun `a JSON timestamp with no offset throws an error`() {
        assertThrows<InvalidFormatException> {
            jsonMapper.readValue("\"2021-04-14T13:02:00\"", HelsinkiDateTime::class.java)
        }
    }

    @Test
    fun `serialization to a JSON timestamp works`() {
        assertEquals("\"2021-04-14T16:02:00+03:00\"", jsonMapper.writeValueAsString(summerValue))
        assertEquals("\"2020-12-01T23:59:00+02:00\"", jsonMapper.writeValueAsString(winterValue))
    }

    @Test
    fun `plus and minus functions work correctly()`() {
        val value =
            summerValue
                .plusYears(1)
                .plusMonths(1)
                .plusWeeks(1)
                .plusDays(1)
                .plusHours(1)
                .plusMinutes(1)
                .plusSeconds(1)
        assertEquals(HelsinkiDateTime.of(LocalDate.of(2022, 5, 22), LocalTime.of(17, 3, 1)), value)
        assertEquals(
            summerValue,
            value
                .minusYears(1)
                .minusMonths(1)
                .minusWeeks(1)
                .minusDays(1)
                .minusHours(1)
                .minusMinutes(1)
                .minusSeconds(1),
        )
    }

    @Test
    fun `elapsed works correctly`() {
        val end = winterValue.toZonedDateTime().plusDays(42).plusSeconds(1)
        val clock = Clock.fixed(end.toInstant(), europeHelsinki)
        assertEquals(Duration.ofDays(42).plusSeconds(1), winterValue.elapsed(clock))
    }
}
