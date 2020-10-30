// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.dvv

import fi.espoo.evaka.shared.async.AsyncJobPayload
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.AsyncJobType
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.transaction
import mu.KotlinLogging
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
class DvvModificationsBatchRefreshService(
    private val asyncJobRunner: AsyncJobRunner,
    private val jdbi: Jdbi,
    private val dvvModificationsService: DvvModificationsService
) {
    init {
        asyncJobRunner.dvvModificationsRefresh = ::doDvvModificationsRefresh
    }

    fun doDvvModificationsRefresh(h: Handle, msg: DvvModificationsRefresh) {
        logger.info("DvvModificationsRefresh: updating ${msg.ssns.size} ssns")
        dvvModificationsService.updatePersonsFromDvv(h, msg.ssns)
    }

    fun scheduleBatch(): Int {
        val jobCount = jdbi.transaction { h ->
            deleteOldJobs(h)

            val ssns = getPersonSsnsToUpdate(h)

            asyncJobRunner.plan(
                h,
                payloads = listOf(
                    DvvModificationsRefresh(
                        ssns = ssns,
                        requestingUserId = UUID.fromString("00000000-0000-0000-0000-000000000000")
                    )
                ),
                runAt = Instant.now(),
                retryCount = 10
            )

            ssns.size
        }

        asyncJobRunner.scheduleImmediateRun()

        return jobCount
    }

    private fun deleteOldJobs(h: Handle) {
        h.createUpdate("DELETE FROM async_job WHERE type = 'DVV_MODIFICATIONS_REFRESH' AND (claimed_by IS NULL OR completed_at IS NOT NULL)").execute()
    }

    fun getPersonSsnsToUpdate(h: Handle): List<String> {
        //language=sql
        return h.createQuery(
            """
SELECT DISTINCT(social_security_number) from PERSON p JOIN (
SELECT head_of_child FROM fridge_child
WHERE daterange(start_date, end_date, '[]') @> current_date AND conflict = false) hoc ON p.id = hoc.head_of_child
            """.trimIndent()
        )
            .mapTo<String>()
            .toList()
    }
}

data class DvvModificationsRefresh(val ssns: List<String>, val requestingUserId: UUID) : AsyncJobPayload {
    override val asyncJobType = AsyncJobType.DVV_MODIFICATIONS_REFRESH
    override val user: AuthenticatedUser? = null
}
