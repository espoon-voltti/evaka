// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dvv

import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.async.AsyncJobType
import evaka.core.shared.async.removeUnclaimedJobs
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class DvvModificationsBatchRefreshService(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val dvvModificationsService: DvvModificationsService,
) {
    init {
        asyncJobRunner.registerHandler(::doDvvModificationsRefresh)
    }

    fun doDvvModificationsRefresh(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.DvvModificationsRefresh,
    ) {
        val ssns = db.read { it.getPersonSsnsToUpdate() }
        logger.info { "DvvModificationsRefresh: starting to process ${ssns.size} ssns" }
        val modificationCount = dvvModificationsService.updatePersonsFromDvv(db, clock, ssns)
        logger.info {
            "DvvModificationsRefresh: finished processing $modificationCount DVV person modifications for ${ssns.size} ssns"
        }
    }

    fun scheduleBatch(db: Database.Connection, clock: EvakaClock) {
        db.transaction { tx ->
            tx.removeUnclaimedJobs(setOf(AsyncJobType(AsyncJob.DvvModificationsRefresh::class)))
            asyncJobRunner.plan(
                tx,
                payloads =
                    listOf(
                        AsyncJob.DvvModificationsRefresh(
                            requestingUserId =
                                UUID.fromString("00000000-0000-0000-0000-000000000000")
                        )
                    ),
                runAt = clock.now(),
                retryCount = 10,
            )
        }
    }
}

private fun Database.Read.getPersonSsnsToUpdate(): List<String> = createQuery {
    sql(
        "SELECT DISTINCT(social_security_number) FROM person WHERE social_security_number IS NOT NULL"
    )
}
    .toList<String>()
