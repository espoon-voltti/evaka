// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.koski

import fi.espoo.evaka.EvakaEnv
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.UploadToKoski
import fi.espoo.evaka.shared.db.Database
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

private val logger = KotlinLogging.logger { }

data class KoskiSearchParams(
    val personIds: List<UUID> = listOf(),
    val daycareIds: List<DaycareId> = listOf()
)

@Service
class KoskiUpdateService(
    private val asyncJobRunner: AsyncJobRunner,
    private val env: EvakaEnv
) {
    init {
        asyncJobRunner.scheduleKoskiUploads = { db, msg -> scheduleKoskiUploads(db, msg.params) }
    }

    fun scheduleKoskiUploads(db: Database, params: KoskiSearchParams) = db.connect {
        scheduleKoskiUploads(it, params)
    }
    fun scheduleKoskiUploads(db: Database.Connection, params: KoskiSearchParams) {
        if (env.koskiEnabled) {
            db.transaction { tx ->
                val requests = tx.getPendingStudyRights(LocalDate.now(), params)
                logger.info { "Scheduling ${requests.size} Koski upload requests" }
                asyncJobRunner.plan(tx, requests.map { UploadToKoski(it) }, retryCount = 1)
            }
        }
    }
}
