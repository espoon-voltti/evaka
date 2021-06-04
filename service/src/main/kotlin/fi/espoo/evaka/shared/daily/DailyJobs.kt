// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.daily

import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import org.springframework.stereotype.Component

enum class DailyJob(val fn: (DailyJobs, Database.Connection) -> Unit) {
    EndOfDayAttendanceUpkeep(DailyJobs::endOfDayAttendanceUpkeep),
}

@Component
class DailyJobs(asyncJobRunner: AsyncJobRunner) {
    init {
        asyncJobRunner.runDailyJob = { db, msg ->
            db.connect { msg.dailyJob.fn(this, it) }
        }
    }

    fun endOfDayAttendanceUpkeep(db: Database.Connection) {
        db.transaction {
            it.createUpdate(
                // language=SQL
                """
                    UPDATE child_attendance ca
                    SET departed = ((arrived AT TIME ZONE 'Europe/Helsinki')::date + time '23:59') AT TIME ZONE 'Europe/Helsinki'
                    WHERE departed IS NULL
                """.trimIndent()
            ).execute()
        }
    }
}
