// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dataremoval

import evaka.core.DataRemovalEnv
import evaka.core.document.childdocument.deleteExpiredChildDocuments
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.async.AsyncJobType
import evaka.core.shared.async.removeUnclaimedJobs
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class DataRemovalService(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val dataRemovalEnv: DataRemovalEnv,
) {
    init {
        asyncJobRunner.registerHandler(::deleteExpiredChildDocuments)
    }

    fun planDataRemoval(db: Database.Connection, clock: EvakaClock) {
        val payloads: List<AsyncJob> = listOf(AsyncJob.DeleteExpiredChildDocuments())
        db.transaction { tx ->
            tx.removeUnclaimedJobs(payloads.map { AsyncJobType.ofPayload(it) }.toSet())
            asyncJobRunner.plan(tx, payloads, retryCount = 1, runAt = clock.now())
        }
        logger.info { "Planned ${payloads.size} data removal job(s)" }
    }

    fun deleteExpiredChildDocuments(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.DeleteExpiredChildDocuments,
    ) {
        val limit = dataRemovalEnv.childDocumentLimit
        val deleted = db.transaction { tx ->
            val results = tx.deleteExpiredChildDocuments(clock.now(), limit = limit)
            val pdfKeys = results.flatMap { it.documentKeys }
            if (pdfKeys.isNotEmpty()) {
                asyncJobRunner.plan(
                    tx = tx,
                    payloads = pdfKeys.map { AsyncJob.DeleteChildDocumentPdf(it) },
                    runAt = clock.now(),
                )
            }
            results
        }
        logger.info { "Deleted ${deleted.size} expired child document(s)" }
        logger.infoChunked(
            items = deleted,
            meta = { chunk ->
                mapOf(
                    "deletedDocumentIds" to chunk.map { it.documentId },
                    "deletedDecisionIds" to chunk.mapNotNull { it.decisionId },
                    "deletedProcessIds" to chunk.mapNotNull { it.processId },
                    "deletedDocumentKeys" to chunk.flatMap { it.documentKeys },
                    "deletedSfiMessageIds" to chunk.flatMap { it.sfiMessageIds },
                )
            },
            message = { index, total ->
                "Expired child document deletion audit (chunk $index/$total)"
            },
        )
        if (limit != null && deleted.size >= limit) {
            logger.info {
                "Child document deletion hit batch limit of $limit; remaining backlog will be processed on the next run"
            }
        }
    }
}
