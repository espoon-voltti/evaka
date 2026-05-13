// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.shared.job

import com.github.kagkarlsson.scheduler.task.ExecutionComplete
import evaka.core.shared.domain.europeHelsinki
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.IsoFields
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class AnnualOnIsoWeekTest {
    private val defaultRunAt = LocalTime.of(0, 10)

    private fun next(schedule: AnnualOnIsoWeek, fromHelsinki: ZonedDateTime): ZonedDateTime =
        schedule
            .getNextExecutionTime(ExecutionComplete.simulatedSuccess(fromHelsinki.toInstant()))
            .atZone(europeHelsinki)

    @Test
    fun `picks the upcoming Wed of ISO week 17 in the current ISO year`() {
        val schedule =
            AnnualOnIsoWeek(isoWeek = 17, dayOfWeek = DayOfWeek.WEDNESDAY, runAt = defaultRunAt)
        val from = ZonedDateTime.of(2026, 1, 1, 12, 0, 0, 0, europeHelsinki)
        val result = next(schedule, from)

        assertEquals(LocalDate.of(2026, 4, 22), result.toLocalDate())
        assertEquals(defaultRunAt, result.toLocalTime())
    }

    @Test
    fun `rolls over to next year after the target has passed`() {
        val schedule =
            AnnualOnIsoWeek(isoWeek = 17, dayOfWeek = DayOfWeek.WEDNESDAY, runAt = defaultRunAt)
        val from = ZonedDateTime.of(2026, 4, 23, 0, 0, 0, 0, europeHelsinki)
        val result = next(schedule, from)

        assertEquals(LocalDate.of(2027, 4, 28), result.toLocalDate())
    }

    @Test
    fun `rolls over even if reference is exactly the target instant`() {
        val schedule =
            AnnualOnIsoWeek(isoWeek = 17, dayOfWeek = DayOfWeek.WEDNESDAY, runAt = defaultRunAt)
        val from = ZonedDateTime.of(2026, 4, 22, 0, 10, 0, 0, europeHelsinki)
        val result = next(schedule, from)

        assertEquals(LocalDate.of(2027, 4, 28), result.toLocalDate())
    }

    @Test
    fun `runAt constructor parameter sets the local time of execution`() {
        val schedule =
            AnnualOnIsoWeek(
                isoWeek = 17,
                dayOfWeek = DayOfWeek.WEDNESDAY,
                runAt = LocalTime.of(2, 30),
            )
        val from = ZonedDateTime.of(2026, 1, 1, 12, 0, 0, 0, europeHelsinki)

        assertEquals(LocalTime.of(2, 30), next(schedule, from).toLocalTime())
    }

    @Test
    fun `target date after spring DST transition still resolves to the configured local time`() {
        // Helsinki spring DST 2026: 2026-03-29. Pick week 14 Mon = 2026-03-30.
        val schedule =
            AnnualOnIsoWeek(isoWeek = 14, dayOfWeek = DayOfWeek.MONDAY, runAt = defaultRunAt)
        val from = ZonedDateTime.of(2026, 3, 1, 0, 0, 0, 0, europeHelsinki)
        val result = next(schedule, from)

        assertEquals(LocalDate.of(2026, 3, 30), result.toLocalDate())
        assertEquals(defaultRunAt, result.toLocalTime())
        assertTrue(result.toInstant().isAfter(from.toInstant()))
    }

    @Test
    fun `isDeterministic returns true`() {
        assertTrue(
            AnnualOnIsoWeek(isoWeek = 17, dayOfWeek = DayOfWeek.WEDNESDAY, runAt = defaultRunAt)
                .isDeterministic()
        )
    }

    @Test
    fun `getInitialExecutionTime returns the same as getNextExecutionTime`() {
        val schedule =
            AnnualOnIsoWeek(isoWeek = 17, dayOfWeek = DayOfWeek.WEDNESDAY, runAt = defaultRunAt)
        val now = ZonedDateTime.of(2026, 1, 1, 12, 0, 0, 0, europeHelsinki).toInstant()

        val initial = schedule.getInitialExecutionTime(now)
        val nextAfterSimulatedSuccess =
            schedule.getNextExecutionTime(ExecutionComplete.simulatedSuccess(now))

        assertEquals(nextAfterSimulatedSuccess, initial)
        assertTrue(initial.isAfter(now))
    }

    @Test
    fun `rejects ISO week below 1`() {
        assertFailsWith<IllegalArgumentException> {
            AnnualOnIsoWeek(isoWeek = 0, dayOfWeek = DayOfWeek.MONDAY, runAt = defaultRunAt)
        }
    }

    @Test
    fun `rejects ISO week above 52`() {
        assertFailsWith<IllegalArgumentException> {
            AnnualOnIsoWeek(isoWeek = 53, dayOfWeek = DayOfWeek.MONDAY, runAt = defaultRunAt)
        }
    }

    @Test
    fun `Mon through Sun of ISO W1 map to consecutive dates even when the Jan 4 anchor is Sunday`() {
        // 2026-01-04 (the Jan 4 anchor for ISO year 2026) is a Sunday, the latest possible
        // weekday for the anchor. ISO W1 2026 spans Mon 2025-12-29..Sun 2026-01-04. Selecting any
        // weekday other than Sunday therefore exercises a backward with(DAY_OF_WEEK) adjustment,
        // and the result must stay inside W1 (proving the per-week boundary holds without
        // explicit handling in the code, and that DAY_OF_WEEK=1 maps to the earliest date).
        val from = ZonedDateTime.of(2025, 6, 1, 0, 0, 0, 0, europeHelsinki)
        val expectedByDay =
            listOf(
                DayOfWeek.MONDAY to LocalDate.of(2025, 12, 29),
                DayOfWeek.TUESDAY to LocalDate.of(2025, 12, 30),
                DayOfWeek.WEDNESDAY to LocalDate.of(2025, 12, 31),
                DayOfWeek.THURSDAY to LocalDate.of(2026, 1, 1),
                DayOfWeek.FRIDAY to LocalDate.of(2026, 1, 2),
                DayOfWeek.SATURDAY to LocalDate.of(2026, 1, 3),
                DayOfWeek.SUNDAY to LocalDate.of(2026, 1, 4),
            )
        for ((day, expectedDate) in expectedByDay) {
            val schedule = AnnualOnIsoWeek(isoWeek = 1, dayOfWeek = day, runAt = defaultRunAt)
            val result = next(schedule, from).toLocalDate()
            assertEquals(expectedDate, result, "dayOfWeek=$day date")
            assertEquals(
                1,
                result.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR),
                "dayOfWeek=$day stays in W1",
            )
            assertEquals(
                2026,
                result.get(IsoFields.WEEK_BASED_YEAR),
                "dayOfWeek=$day stays in ISO year 2026",
            )
        }
    }

    @Test
    fun `next execution is strictly after the reference instant`() {
        val schedule =
            AnnualOnIsoWeek(isoWeek = 1, dayOfWeek = DayOfWeek.MONDAY, runAt = defaultRunAt)
        // Reference is exactly week 1 Monday 00:10 of 2026 — must roll forward.
        val from = ZonedDateTime.of(LocalDate.of(2025, 12, 29), LocalTime.of(0, 10), europeHelsinki)
        assertEquals(2026, from.toLocalDate().get(IsoFields.WEEK_BASED_YEAR))
        assertEquals(1, from.toLocalDate().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR))
        assertEquals(DayOfWeek.MONDAY, from.dayOfWeek)

        val result: Instant =
            schedule.getNextExecutionTime(ExecutionComplete.simulatedSuccess(from.toInstant()))
        assertTrue(result.isAfter(from.toInstant()))
        // Next ISO 2027 W1 Monday is 2027-01-04 (since Jan 1 2027 is Friday).
        assertEquals(LocalDate.of(2027, 1, 4), result.atZone(europeHelsinki).toLocalDate())
    }
}
