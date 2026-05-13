// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.shared.job

import com.github.kagkarlsson.scheduler.task.ExecutionComplete
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import evaka.core.shared.domain.europeHelsinki
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoField
import java.time.temporal.IsoFields

/**
 * Note that changing the schedule's `(isoWeek, dayOfWeek, runAt)` for an already-deployed job does
 * not retroactively update the persisted `scheduled_tasks` row in db-scheduler — the row must be
 * cleared manually for the new values to take effect.
 */
class AnnualOnIsoWeek(
    private val isoWeek: Int,
    private val dayOfWeek: DayOfWeek,
    private val runAt: LocalTime,
) : Schedule {
    init {
        require(isoWeek in 1..52) { "ISO week must be 1..52, got $isoWeek" }
    }

    override fun getNextExecutionTime(executionComplete: ExecutionComplete): Instant =
        nextAfter(executionComplete.timeDone)

    override fun isDeterministic(): Boolean = true

    override fun toString(): String = "AnnualOnIsoWeek(week=$isoWeek, day=$dayOfWeek, runAt=$runAt)"

    private fun nextAfter(reference: Instant): Instant {
        val refIsoYear =
            reference.atZone(europeHelsinki).toLocalDate().get(IsoFields.WEEK_BASED_YEAR)
        val thisYear = candidateInstant(refIsoYear)
        return if (thisYear.isAfter(reference)) thisYear else candidateInstant(refIsoYear + 1)
    }

    private fun candidateInstant(isoYear: Int): Instant {
        // Jan 4 is always in ISO W1 of its ISO week-based year by definition.
        val date =
            LocalDate.of(isoYear, 1, 4)
                .with(IsoFields.WEEK_BASED_YEAR, isoYear.toLong())
                .with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, isoWeek.toLong())
                .with(ChronoField.DAY_OF_WEEK, dayOfWeek.value.toLong())
        return ZonedDateTime.of(date, runAt, europeHelsinki).toInstant()
    }
}
