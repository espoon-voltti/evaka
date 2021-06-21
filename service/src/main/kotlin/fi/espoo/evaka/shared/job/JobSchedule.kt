// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.job

import com.github.kagkarlsson.scheduler.task.schedule.CronSchedule
import com.github.kagkarlsson.scheduler.task.schedule.Daily
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import fi.espoo.evaka.application.utils.helsinkiZone
import java.time.LocalTime

interface JobSchedule {
    fun getScheduleForJob(job: ScheduledJob): Schedule?

    companion object {
        fun daily(at: LocalTime): Schedule = Daily(helsinkiZone, at)
        fun cron(expression: String): Schedule = CronSchedule(expression, helsinkiZone)
    }
}

class DefaultJobSchedule : JobSchedule {
    override fun getScheduleForJob(job: ScheduledJob): Schedule? = when (job) {
        ScheduledJob.VardaUpdate -> JobSchedule.daily(LocalTime.of(23, 0))
        ScheduledJob.EndOfDayAttendanceUpkeep -> JobSchedule.daily(LocalTime.of(0, 0))
        ScheduledJob.KoskiUpdate -> JobSchedule.daily(LocalTime.of(0, 0))
        ScheduledJob.RemoveOldDraftApplications -> JobSchedule.daily(LocalTime.of(0, 30))
        ScheduledJob.CancelOutdatedTransferApplications -> JobSchedule.daily(LocalTime.of(0, 35))
        ScheduledJob.EndOutdatedVoucherValueDecisions -> JobSchedule.daily(LocalTime.of(1, 0))
        ScheduledJob.RemoveOldAsyncJobs -> JobSchedule.daily(LocalTime.of(3, 0))
        ScheduledJob.DvvUpdate -> JobSchedule.daily(LocalTime.of(6, 0))
        ScheduledJob.RemoveOldDaycareDailyNotes -> JobSchedule.daily(LocalTime.of(6, 30))
        ScheduledJob.SendPendingDecisionReminderEmails -> JobSchedule.daily(LocalTime.of(7, 0))

        ScheduledJob.FreezeVoucherValueReports -> JobSchedule.cron("0 0 0 25 * ?") // Monthly on 25th
    }
}
