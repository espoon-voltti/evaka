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
    fun getSettingsForJob(job: ScheduledJob): ScheduledJobSettings?

    companion object {
        fun daily(at: LocalTime): Schedule = Daily(helsinkiZone, at)
        fun cron(expression: String): Schedule = CronSchedule(expression, helsinkiZone)
    }
}

data class ScheduledJobSettings(
    val enabled: Boolean,
    val schedule: Schedule,
    val retryCount: Int? = null
) {
    companion object {
        fun default(job: ScheduledJob) = when (job) {
            ScheduledJob.VardaUpdate -> ScheduledJobSettings(
                enabled = false,
                schedule = JobSchedule.cron("0 0 23 * * 1,2,3,4,5"), // mon - fri @ 23 pm
                retryCount = 1
            )
            ScheduledJob.VardaReset -> ScheduledJobSettings(
                enabled = true,
                schedule = JobSchedule.cron("0 0 20 * * 1,2,3,4,5"), // mon - fri @ 20 pm
                retryCount = 1
            )
            ScheduledJob.EndOfDayAttendanceUpkeep -> ScheduledJobSettings(
                enabled = true,
                schedule = JobSchedule.daily(LocalTime.of(0, 0))
            )
            ScheduledJob.EndOfDayStaffAttendanceUpkeep -> ScheduledJobSettings(
                enabled = true,
                schedule = JobSchedule.daily(LocalTime.of(0, 0))
            )
            ScheduledJob.EndOfDayReservationUpkeep -> ScheduledJobSettings(
                enabled = true,
                schedule = JobSchedule.daily(LocalTime.of(0, 0))
            )
            ScheduledJob.KoskiUpdate -> ScheduledJobSettings(
                enabled = false,
                schedule = JobSchedule.daily(LocalTime.of(0, 0)),
                retryCount = 1
            )
            ScheduledJob.RemoveOldDraftApplications -> ScheduledJobSettings(
                enabled = false,
                schedule = JobSchedule.daily(LocalTime.of(0, 30))
            )
            ScheduledJob.CancelOutdatedTransferApplications -> ScheduledJobSettings(
                enabled = false,
                schedule = JobSchedule.daily(LocalTime.of(0, 35))
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
                enabled = true,
                schedule = JobSchedule.cron("0 0 0 25 * ?") // Monthly on 25th
            )
            ScheduledJob.InactivePeopleCleanup -> ScheduledJobSettings(
                enabled = false,
                schedule = JobSchedule.daily(LocalTime.of(3, 30)),
                retryCount = 1
            )
            ScheduledJob.InactiveEmployeesRoleReset -> ScheduledJobSettings(
                enabled = true,
                schedule = JobSchedule.daily(LocalTime.of(3, 15))
            )
        }
    }
}

class DefaultJobSchedule(val env: ScheduledJobsEnv) : JobSchedule {
    override fun getSettingsForJob(job: ScheduledJob): ScheduledJobSettings? = env.jobs[job]
}
