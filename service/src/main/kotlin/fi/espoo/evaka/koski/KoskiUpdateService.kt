// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.koski

import fi.espoo.evaka.invoicing.controller.parseUUID
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.UploadToKoski
import fi.espoo.evaka.shared.db.transaction
import mu.KotlinLogging
import org.jdbi.v3.core.Jdbi
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
    private val jdbi: Jdbi,
    private val env: Environment,
    private val asyncJobRunner: AsyncJobRunner
) {
    fun update(
        personIds: String?,
        daycareIds: String?
    ) {
        val isEnabled: Boolean = env.getRequiredProperty("fi.espoo.integration.koski.enabled", Boolean::class.java)
        if (isEnabled) {
            jdbi.transaction { h ->
                val params = KoskiSearchParams(
                    personIds = personIds?.split(",")?.map(::parseUUID) ?: listOf(),
                    daycareIds = daycareIds?.split(",")?.map(::parseUUID) ?: listOf()
                )
                val requests = h.getPendingStudyRights(LocalDate.now(), params)
                logger.info { "Scheduling ${requests.size} upload requests" }
                asyncJobRunner.plan(h, requests.map { UploadToKoski(it) }, retryCount = 1)
            }
            asyncJobRunner.scheduleImmediateRun()
        }
    }
}
