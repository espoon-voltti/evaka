// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.daily

import java.time.LocalTime

interface DailySchedule {
    fun getTimeForJob(job: DailyJob): LocalTime?
}

class DefaultDailySchedule : DailySchedule {
    override fun getTimeForJob(job: DailyJob): LocalTime? = when (job) {
        DailyJob.EndOfDayAttendanceUpkeep -> LocalTime.of(0, 0)
    }
}
