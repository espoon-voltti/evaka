// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.job

import com.github.kagkarlsson.scheduler.task.schedule.CronSchedule
import com.github.kagkarlsson.scheduler.task.schedule.Daily
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import fi.espoo.evaka.application.utils.helsinkiZone
import org.springframework.core.env.Environment
import org.springframework.core.env.getProperty
import java.time.LocalTime

interface JobSchedule {
    fun getScheduleForJob(job: ScheduledJob): Schedule?

    companion object {
        fun daily(at: LocalTime): Schedule = Daily(helsinkiZone, at)
        fun cron(expression: String): Schedule = CronSchedule(expression, helsinkiZone)
    }
}

data class ScheduledJobSettings(val enabled: Boolean, val schedule: Schedule) {
    companion object {
        fun default(job: ScheduledJob) = when (job) {
            ScheduledJob.VardaUpdate -> ScheduledJobSettings(
                enabled = false,
                schedule = JobSchedule.daily(LocalTime.of(23, 0))
            )
            ScheduledJob.EndOfDayAttendanceUpkeep -> ScheduledJobSettings(
                enabled = true,
                schedule = JobSchedule.daily(LocalTime.of(0, 0))
            )
            ScheduledJob.KoskiUpdate -> ScheduledJobSettings(
                enabled = false,
                schedule = JobSchedule.daily(LocalTime.of(0, 0))
            )
            ScheduledJob.RemoveOldDraftApplications -> ScheduledJobSettings(
                enabled = false,
                schedule = JobSchedule.daily(LocalTime.of(0, 30))
            )
            ScheduledJob.CancelOutdatedTransferApplications -> ScheduledJobSettings(
                enabled = false,
                schedule = JobSchedule.daily(LocalTime.of(0, 35))
            )
            ScheduledJob.EndOutdatedVoucherValueDecisions -> ScheduledJobSettings(
                enabled = false,
                schedule = JobSchedule.daily(LocalTime.of(1, 0))
            )
            ScheduledJob.RemoveOldAsyncJobs -> ScheduledJobSettings(
                enabled = true,
                schedule = JobSchedule.daily(LocalTime.of(3, 0))
            )
            ScheduledJob.DvvUpdate -> ScheduledJobSettings(
                enabled = false,
                schedule = JobSchedule.daily(LocalTime.of(4, 0))
            )
            ScheduledJob.RemoveOldDaycareDailyNotes -> ScheduledJobSettings(
                enabled = true,
                schedule = JobSchedule.daily(LocalTime.of(6, 30))
            )
            ScheduledJob.SendPendingDecisionReminderEmails -> ScheduledJobSettings(
                enabled = false,
                schedule = JobSchedule.daily(LocalTime.of(7, 0))
            )
            ScheduledJob.FreezeVoucherValueReports -> ScheduledJobSettings(
                enabled = false,
                schedule = JobSchedule.cron("0 0 0 25 * ?") // Monthly on 25th
            )
            ScheduledJob.InactivePeopleCleanup -> ScheduledJobSettings(
                enabled = true,
                schedule = JobSchedule.daily(LocalTime.of(3, 30))
            )
        }
    }
}

private fun snakeCaseName(job: ScheduledJob): String = job.name.flatMapIndexed { idx, ch ->
    when {
        idx == 0 -> listOf(ch.lowercaseChar())
        ch.isUpperCase() -> listOf('_', ch.lowercaseChar())
        else -> listOf(ch)
    }
}.joinToString(separator = "")

class DefaultJobSchedule(val settings: Map<ScheduledJob, ScheduledJobSettings>) : JobSchedule {
    override fun getScheduleForJob(job: ScheduledJob): Schedule? = settings[job]?.let {
        val enabled = it.enabled
        it.schedule.takeIf { enabled }
    }

    companion object {
        fun fromEnvironment(env: Environment): DefaultJobSchedule = DefaultJobSchedule(
            ScheduledJob.values().associate { job ->
                val envPrefix = "evaka.job.${snakeCaseName(job)}"
                val default = ScheduledJobSettings.default(job)
                val settings = ScheduledJobSettings(
                    enabled = env.getProperty<Boolean>("$envPrefix.enabled") ?: default.enabled,
                    schedule = env.getProperty<String>("$envPrefix.cron")?.let(JobSchedule::cron)
                        ?: default.schedule
                )
                (job to settings)
            }
        )
    }
}
