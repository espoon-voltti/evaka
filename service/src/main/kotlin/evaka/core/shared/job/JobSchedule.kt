// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.shared.job

import com.github.kagkarlsson.scheduler.task.schedule.CronSchedule
import com.github.kagkarlsson.scheduler.task.schedule.Daily
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.europeHelsinki
import java.time.LocalTime

interface JobSchedule {
    val jobs: List<ScheduledJobDefinition>

    companion object {
        fun daily(at: LocalTime): Schedule = Daily(europeHelsinki, at)

        fun nightly(): Schedule = Nightly()

        fun cron(expression: String): Schedule = CronSchedule(expression, europeHelsinki)
    }
}

data class ScheduledJobDefinition(
    val job: Enum<*>,
    val settings: ScheduledJobSettings,
    val jobFn: (db: Database.Connection, clock: EvakaClock) -> Unit,
)

data class ScheduledJobSettings(
    val enabled: Boolean,
    val schedule: Schedule,
    val retryCount: Int? = null,
)
