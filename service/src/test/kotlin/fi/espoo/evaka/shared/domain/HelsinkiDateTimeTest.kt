// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.domain

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import fi.espoo.evaka.shared.config.defaultObjectMapper
import fi.espoo.evaka.shared.utils.europeHelsinki
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

class HelsinkiDateTimeTest {
    private val objectMapper = defaultObjectMapper()
    private val summerValue = HelsinkiDateTime.from(ZonedDateTime.of(LocalDate.of(2021, 4, 14), LocalTime.of(16, 2), europeHelsinki))
    private val winterValue = HelsinkiDateTime.from(ZonedDateTime.of(LocalDate.of(2020, 12, 1), LocalTime.of(23, 59), europeHelsinki))

    @Test
    fun `a JSON timestamp with +3 offset (with DST) is deserialized correctly`() {
        val hdt = objectMapper.readValue("\"2021-04-14T16:02:00+03:00\"", HelsinkiDateTime::class.java)
        assertEquals(summerValue, hdt)
    }

    @Test
    fun `a JSON timestamp with +2 offset (without DST) is deserialized correctly`() {
        val hdt = objectMapper.readValue("\"2020-12-01T23:59:00+02:00\"", HelsinkiDateTime::class.java)
        assertEquals(winterValue, hdt)
    }

    @Test
    fun `a JSON timestamp with +0 offset is deserialized correctly`() {
        val hdt = objectMapper.readValue("\"2021-04-14T13:02:00+00:00\"", HelsinkiDateTime::class.java)
        assertEquals(summerValue, hdt)
    }

    @Test
    fun `a JSON timestamp with Z offset is deserialized correctly`() {
        val hdt = objectMapper.readValue("\"2021-04-14T13:02:00Z\"", HelsinkiDateTime::class.java)
        assertEquals(summerValue, hdt)
    }

    @Test
    fun `a JSON timestamp with a ridiculous offset is deserialized correctly`() {
        val hdt = objectMapper.readValue("\"2021-04-15T02:29:00+13:27\"", HelsinkiDateTime::class.java)
        assertEquals(summerValue, hdt)
    }

    @Test
    fun `a JSON timestamp with no offset throws an error`() {
        assertThrows<InvalidFormatException> {
            objectMapper.readValue("\"2021-04-14T13:02:00\"", HelsinkiDateTime::class.java)
        }
    }

    @Test
    fun `serialization to a JSON timestamp works`() {
        assertEquals("\"2021-04-14T16:02:00+03:00\"", objectMapper.writeValueAsString(summerValue))
        assertEquals("\"2020-12-01T23:59:00+02:00\"", objectMapper.writeValueAsString(winterValue))
    }
}
