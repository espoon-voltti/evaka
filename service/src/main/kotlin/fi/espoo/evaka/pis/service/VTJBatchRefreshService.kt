// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.VTJRefresh
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
class VTJBatchRefreshService(
    private val fridgeFamilyService: FridgeFamilyService,
    private val asyncJobRunner: AsyncJobRunner,
    private val jdbi: Jdbi
) {

    init {
        asyncJobRunner.vtjRefresh = ::doVTJRefresh
    }

    fun doVTJRefresh(msg: VTJRefresh) {
        fridgeFamilyService.doVTJRefresh(msg)
    }

    fun scheduleBatch(): Int {
        val jobCount = jdbi.transaction { h ->
            deleteOldJobs(h)

            val requestingUserId = UUID.fromString("00000000-0000-0000-0000-000000000000")
            val personIds = getPersonIdsToRefresh(h)
            personIds.forEachIndexed { i, personId ->
                kotlin.run {
                    asyncJobRunner.plan(
                        payloads = listOf(
                            VTJRefresh(
                                personId = personId,
                                requestingUserId = requestingUserId
                            )
                        ),
                        runAt = Instant.now().plusSeconds(i.toLong())
                    )
                }
            }

            personIds.size
        }

        asyncJobRunner.scheduleImmediateRun()

        return jobCount
    }

    private fun deleteOldJobs(h: Handle) {
        h.createUpdate("DELETE FROM async_job WHERE type = 'VTJ_REFRESH' AND (claimed_by IS NULL OR completed_at IS NOT NULL)").execute()
    }

    private fun getPersonIdsToRefresh(h: Handle): Set<UUID> {
        // language=sql
        val sql =
            """
            SELECT head_of_child FROM fridge_child 
            WHERE daterange(start_date, end_date, '[]') @> current_date AND conflict = false
            """.trimIndent()

        return h.createQuery(sql).mapTo<UUID>()
            .toHashSet()
    }
}
