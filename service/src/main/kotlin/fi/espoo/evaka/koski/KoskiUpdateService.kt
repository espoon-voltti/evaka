// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.koski

import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.UploadToKoski
import fi.espoo.evaka.shared.db.Database
import mu.KotlinLogging
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

private val logger = KotlinLogging.logger { }

data class KoskiSearchParams(
    val personIds: List<UUID> = listOf(),
    val daycareIds: List<UUID> = listOf()
)

@Service
class KoskiUpdateService(
    private val env: Environment,
    private val asyncJobRunner: AsyncJobRunner
) {
    init {
        asyncJobRunner.scheduleKoskiUploads = { db, msg -> scheduleKoskiUploads(db, msg.params) }
    }

    fun scheduleKoskiUploads(db: Database, params: KoskiSearchParams) {
        val isEnabled: Boolean = env.getRequiredProperty("fi.espoo.integration.koski.enabled", Boolean::class.java)
        if (isEnabled) {
            db.transaction { tx ->
                val requests = tx.getPendingStudyRights(LocalDate.now(), params)
                logger.info { "Scheduling ${requests.size} Koski upload requests" }
                asyncJobRunner.plan(tx, requests.map { UploadToKoski(it) }, retryCount = 1)
            }
        }
    }
}
