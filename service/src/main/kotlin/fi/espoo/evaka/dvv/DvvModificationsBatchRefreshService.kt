// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.dvv

import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import java.util.UUID
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class DvvModificationsBatchRefreshService(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val dvvModificationsService: DvvModificationsService
) {
    init {
        asyncJobRunner.registerHandler(::doDvvModificationsRefresh)
    }

    fun doDvvModificationsRefresh(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.DvvModificationsRefresh
    ) {
        logger.info("DvvModificationsRefresh: starting to process ${msg.ssns.size} ssns")
        val modificationCount = dvvModificationsService.updatePersonsFromDvv(db, clock, msg.ssns)
        logger.info(
            "DvvModificationsRefresh: finished processing $modificationCount DVV person modifications for ${msg.ssns.size} ssns"
        )
    }

    fun scheduleBatch(db: Database.Connection, clock: EvakaClock): Int {
        val jobCount =
            db.transaction { tx ->
                tx.deleteOldJobs()

                val ssns = tx.getPersonSsnsToUpdate()

                asyncJobRunner.plan(
                    tx,
                    payloads =
                        listOf(
                            AsyncJob.DvvModificationsRefresh(
                                ssns = ssns,
                                requestingUserId =
                                    UUID.fromString("00000000-0000-0000-0000-000000000000")
                            )
                        ),
                    runAt = clock.now(),
                    retryCount = 10
                )

                ssns.size
            }

        return jobCount
    }
}

private fun Database.Transaction.deleteOldJobs() =
    createUpdate(
            "DELETE FROM async_job WHERE type = 'DVV_MODIFICATIONS_REFRESH' AND (claimed_by IS NULL OR completed_at IS NOT NULL)"
        )
        .execute()

private fun Database.Read.getPersonSsnsToUpdate(): List<String> =
    createQuery(
            // language=sql
            """
SELECT DISTINCT(social_security_number) FROM person
    """.trimIndent()
        )
        .mapTo<String>()
        .toList()
