// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.job

import com.github.kagkarlsson.scheduler.task.schedule.CronSchedule
import com.github.kagkarlsson.scheduler.task.schedule.Daily
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import fi.espoo.evaka.ScheduledJobsEnv
import fi.espoo.evaka.application.utils.helsinkiZone
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
                enabled = false,
                schedule = JobSchedule.daily(LocalTime.of(3, 30))
            )
            ScheduledJob.InactiveEmployeesRoleReset -> ScheduledJobSettings(
                enabled = true,
                schedule = JobSchedule.daily(LocalTime.of(3, 15))
            )
        }
    }
}

class DefaultJobSchedule(val env: ScheduledJobsEnv) : JobSchedule {
    override fun getScheduleForJob(job: ScheduledJob): Schedule? = env.jobs[job]?.let {
        val enabled = it.enabled
        it.schedule.takeIf { enabled }
    }
}
