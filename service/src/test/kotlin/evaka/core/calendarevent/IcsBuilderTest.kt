// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.calendarevent

import evaka.core.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IcsBuilderTest {
    // 2026-06-24 09:30:00 Helsinki time == 06:30:00 UTC (summer, +03:00)
    private val dtstamp = HelsinkiDateTime.of(LocalDate.of(2026, 6, 24), LocalTime.of(9, 30, 0))

    private fun lines(ics: String): List<String> {
        assertTrue(ics.endsWith("\r\n"), "calendar must end with CRLF")
        return ics.removeSuffix("\r\n").split("\r\n")
    }

    @Test
    fun `builds an all-day event without a timezone block`() {
        val ics =
            buildIcsCalendar(
                IcsEvent(
                    uid = "daycare-event-abc@evaka",
                    summary = "Kevätjuhla",
                    location = "Päiväkoti Aurinko (Oravat)",
                    time =
                        IcsEventTime.AllDay(
                            start = LocalDate.of(2026, 6, 24),
                            endExclusive = LocalDate.of(2026, 6, 26),
                        ),
                ),
                dtstamp = dtstamp,
            )

        assertEquals(
            listOf(
                "BEGIN:VCALENDAR",
                "VERSION:2.0",
                "PRODID:-//City of Espoo//eVaka//EN",
                "METHOD:REQUEST",
                "BEGIN:VEVENT",
                "UID:daycare-event-abc@evaka",
                "DTSTAMP:20260624T063000Z",
                "SUMMARY:Kevätjuhla",
                "LOCATION:Päiväkoti Aurinko (Oravat)",
                "DTSTART;VALUE=DATE:20260624",
                "DTEND;VALUE=DATE:20260626",
                "END:VEVENT",
                "END:VCALENDAR",
            ),
            lines(ics),
        )
    }

    @Test
    fun `builds a timed event with a Helsinki timezone block and TZID references`() {
        val ics =
            buildIcsCalendar(
                IcsEvent(
                    uid = "time-xyz@evaka",
                    summary = "Keskusteluaika",
                    location = "Päiväkoti Aurinko",
                    time =
                        IcsEventTime.Timed(
                            start =
                                HelsinkiDateTime.of(LocalDate.of(2026, 6, 24), LocalTime.of(10, 0)),
                            end =
                                HelsinkiDateTime.of(LocalDate.of(2026, 6, 24), LocalTime.of(10, 30)),
                        ),
                ),
                dtstamp = dtstamp,
            )

        val result = lines(ics)
        assertTrue(result.contains("BEGIN:VTIMEZONE"), "expected VTIMEZONE block")
        assertTrue(result.contains("TZID:Europe/Helsinki"))
        assertTrue(result.contains("END:VTIMEZONE"))
        assertTrue(result.contains("DTSTART;TZID=Europe/Helsinki:20260624T100000"))
        assertTrue(result.contains("DTEND;TZID=Europe/Helsinki:20260624T103000"))
        // VTIMEZONE must be declared before it is referenced
        assertTrue(
            result.indexOf("END:VTIMEZONE") < result.indexOfFirst { it.startsWith("DTSTART;TZID=") }
        )
    }

    @Test
    fun `escapes commas, semicolons, backslashes and newlines in text values`() {
        val ics =
            buildIcsCalendar(
                IcsEvent(
                    uid = "u@evaka",
                    summary = "A, B; C\\D\nE",
                    location = "x",
                    time =
                        IcsEventTime.AllDay(
                            start = LocalDate.of(2026, 6, 24),
                            endExclusive = LocalDate.of(2026, 6, 25),
                        ),
                ),
                dtstamp = dtstamp,
            )

        assertTrue(lines(ics).contains("""SUMMARY:A\, B\; C\\D\nE"""))
    }

    @Test
    fun `folds content lines longer than 75 octets`() {
        val longSummary = "a".repeat(200)
        val ics =
            buildIcsCalendar(
                IcsEvent(
                    uid = "u@evaka",
                    summary = longSummary,
                    location = "x",
                    time =
                        IcsEventTime.AllDay(
                            start = LocalDate.of(2026, 6, 24),
                            endExclusive = LocalDate.of(2026, 6, 25),
                        ),
                ),
                dtstamp = dtstamp,
            )

        // No single physical line may exceed 75 octets
        ics.removeSuffix("\r\n").split("\r\n").forEach { line ->
            assertTrue(line.toByteArray(Charsets.UTF_8).size <= 75, "line exceeds 75 octets: $line")
        }
        // Folded continuation lines start with a single space
        assertTrue(ics.contains("\r\n "), "expected folded continuation line")
    }
}
