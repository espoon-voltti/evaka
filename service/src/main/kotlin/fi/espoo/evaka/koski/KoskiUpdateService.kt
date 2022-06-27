// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.koski

import fi.espoo.evaka.EvakaEnv
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.europeHelsinki
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalDate

private val logger = KotlinLogging.logger { }

data class KoskiSearchParams(
    val personIds: List<ChildId> = listOf(),
    val daycareIds: List<DaycareId> = listOf()
)

@Service
class KoskiUpdateService(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val env: EvakaEnv
) {
    init {
        asyncJobRunner.registerHandler { db, _, msg: AsyncJob.ScheduleKoskiUploads -> scheduleKoskiUploads(db, msg.params) }
    }

    fun scheduleKoskiUploads(db: Database.Connection, params: KoskiSearchParams) {
        if (env.koskiEnabled) {
            db.transaction { tx ->
                val requests = tx.getPendingStudyRights(LocalDate.now(europeHelsinki), params)
                logger.info { "Scheduling ${requests.size} Koski upload requests" }
                asyncJobRunner.plan(tx, requests.map { AsyncJob.UploadToKoski(it) }, retryCount = 1)
            }
        }
    }
}
