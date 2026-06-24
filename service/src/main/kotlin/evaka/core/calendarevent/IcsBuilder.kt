// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.calendarevent

import evaka.core.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Generates RFC 5545 (iCalendar) documents for calendar exports.
 *
 * Serving the result with `Content-Type: text/calendar` lets browsers - notably iOS Safari, which
 * ignores the `download` attribute on `data:` URIs - recognize the content and offer to add it to
 * the device calendar.
 */
sealed interface IcsEventTime {
    /** An all-day event. [endExclusive] is the day after the last day of the event. */
    data class AllDay(val start: LocalDate, val endExclusive: LocalDate) : IcsEventTime

    data class Timed(val start: HelsinkiDateTime, val end: HelsinkiDateTime) : IcsEventTime
}

data class IcsEvent(
    val uid: String,
    val summary: String,
    val location: String,
    val time: IcsEventTime,
)

private const val CRLF = "\r\n"

private val dateFormat = DateTimeFormatter.ofPattern("yyyyMMdd")
private val localDateTimeFormat = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
private val utcTimestampFormat = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssX")

// VTIMEZONE definition for Europe/Helsinki. Inlined so the produced calendar is self-contained and
// does not rely on the importing client knowing the zone.
private val helsinkiVTimezone =
    listOf(
        "BEGIN:VTIMEZONE",
        "TZID:Europe/Helsinki",
        "BEGIN:STANDARD",
        "TZNAME:EET",
        "TZOFFSETFROM:+0300",
        "TZOFFSETTO:+0200",
        "DTSTART:19701025T040000",
        "RRULE:FREQ=YEARLY;BYMONTH=10;BYDAY=-1SU",
        "END:STANDARD",
        "BEGIN:DAYLIGHT",
        "TZNAME:EEST",
        "TZOFFSETFROM:+0200",
        "TZOFFSETTO:+0300",
        "DTSTART:19700329T030000",
        "RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=-1SU",
        "END:DAYLIGHT",
        "END:VTIMEZONE",
    )

fun buildIcsCalendar(event: IcsEvent, dtstamp: HelsinkiDateTime): String {
    val lines =
        mutableListOf(
            "BEGIN:VCALENDAR",
            "VERSION:2.0",
            "PRODID:-//City of Espoo//eVaka//EN",
            "METHOD:REQUEST",
        )

    if (event.time is IcsEventTime.Timed) {
        lines += helsinkiVTimezone
    }

    lines += "BEGIN:VEVENT"
    lines += "UID:${event.uid}"
    lines += "DTSTAMP:${utcTimestampFormat.format(dtstamp.toInstant().atOffset(ZoneOffset.UTC))}"
    lines += "SUMMARY:${escapeText(event.summary)}"
    lines += "LOCATION:${escapeText(event.location)}"
    when (val time = event.time) {
        is IcsEventTime.AllDay -> {
            lines += "DTSTART;VALUE=DATE:${dateFormat.format(time.start)}"
            lines += "DTEND;VALUE=DATE:${dateFormat.format(time.endExclusive)}"
        }
        is IcsEventTime.Timed -> {
            lines +=
                "DTSTART;TZID=Europe/Helsinki:${localDateTimeFormat.format(time.start.toLocalDateTime())}"
            lines +=
                "DTEND;TZID=Europe/Helsinki:${localDateTimeFormat.format(time.end.toLocalDateTime())}"
        }
    }
    lines += "END:VEVENT"
    lines += "END:VCALENDAR"

    return lines.joinToString("") { foldLine(it) + CRLF }
}

/** Escapes a TEXT value as defined in RFC 5545 section 3.3.11. */
private fun escapeText(value: String): String =
    value
        .replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace(",", "\\,")
        .replace("\r\n", "\\n")
        .replace("\n", "\\n")

/**
 * Folds a content line so that no physical line exceeds 75 octets, as required by RFC 5545 section
 * 3.1. Continuation lines are prefixed with a single space. Folding happens on octet boundaries
 * without splitting a multi-byte UTF-8 character.
 */
private fun foldLine(line: String): String {
    val bytes = line.toByteArray(Charsets.UTF_8)
    if (bytes.size <= 75) return line

    val builder = StringBuilder()
    var lineOctets = 0
    var first = true
    for (codePoint in line.codePoints()) {
        val charOctets = String(Character.toChars(codePoint)).toByteArray(Charsets.UTF_8).size
        // First line may use 75 octets; continuation lines reserve 1 octet for the leading space.
        val limit = if (first) 75 else 74
        if (lineOctets + charOctets > limit) {
            builder.append(CRLF).append(' ')
            lineOctets = 0
            first = false
        }
        builder.appendCodePoint(codePoint)
        lineOctets += charOctets
    }
    return builder.toString()
}
