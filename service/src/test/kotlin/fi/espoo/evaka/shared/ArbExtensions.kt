// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared

import fi.espoo.evaka.shared.data.DateSet
import fi.espoo.evaka.shared.data.DateTimeSet
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.HelsinkiDateTimeRange
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.localDate
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.positiveLong
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

fun Arb.Companion.finiteDateRange(
    start: Arb<LocalDate> =
        Arb.localDate(minDate = LocalDate.of(2019, 1, 1), maxDate = LocalDate.of(2030, 12, 1)),
    durationDays: Arb<Long> = Arb.positiveLong(max = 365_0L),
): Arb<FiniteDateRange> =
    Arb.bind(start, durationDays) { startDate, days ->
        FiniteDateRange(startDate, startDate.plusDays(days))
    }

fun Arb.Companion.dateSet(): Arb<DateSet> =
    Arb.list(Arb.finiteDateRange()).map { ranges -> DateSet.of(ranges) }

fun Arb.Companion.helsinkiDateTime(
    min: HelsinkiDateTime = HelsinkiDateTime.of(LocalDateTime.of(2019, 1, 1, 12, 0)),
    max: HelsinkiDateTime = HelsinkiDateTime.of(LocalDateTime.of(2030, 12, 31, 12, 0)),
): Arb<HelsinkiDateTime> =
    Arb.long(min = 0, max = Duration.between(min.toInstant(), max.toInstant()).seconds).map {
        secondsSinceMin ->
        min.plusSeconds(secondsSinceMin)
    }

fun Arb.Companion.helsinkiDateTimeRange(
    start: Arb<HelsinkiDateTime> = Arb.helsinkiDateTime(),
    durationSeconds: Arb<Long> = Arb.positiveLong(max = 48L * 60L * 60L),
): Arb<HelsinkiDateTimeRange> =
    Arb.bind(start, durationSeconds) { startTimestamp, seconds ->
        HelsinkiDateTimeRange(startTimestamp, startTimestamp.plusSeconds(seconds))
    }

fun Arb.Companion.dateTimeSet(): Arb<DateTimeSet> =
    Arb.list(Arb.helsinkiDateTimeRange()).map { ranges -> DateTimeSet.of(ranges) }
