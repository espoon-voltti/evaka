// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.job

import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import org.springframework.stereotype.Component

enum class ScheduledJob(val fn: (ScheduledJobs, Database.Connection) -> Unit) {
    EndOfDayAttendanceUpkeep(ScheduledJobs::endOfDayAttendanceUpkeep),
}

@Component
class ScheduledJobs(asyncJobRunner: AsyncJobRunner) {
    init {
        asyncJobRunner.runScheduledJob = { db, msg ->
            db.connect { msg.job.fn(this, it) }
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
