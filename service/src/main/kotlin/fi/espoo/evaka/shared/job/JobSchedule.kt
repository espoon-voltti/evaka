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
        ScheduledJob.EndOfDayAttendanceUpkeep -> JobSchedule.daily(LocalTime.of(0, 0))
    }
}
