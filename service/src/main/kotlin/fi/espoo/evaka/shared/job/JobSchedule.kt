// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.job

import com.github.kagkarlsson.scheduler.task.schedule.CronSchedule
import com.github.kagkarlsson.scheduler.task.schedule.Daily
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.europeHelsinki
import java.time.LocalTime

interface JobSchedule {
    val jobs: List<ScheduledJobDefinition>

    companion object {
        fun daily(at: LocalTime): Schedule = Daily(europeHelsinki, at)

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
