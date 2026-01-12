// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.shared.MessageContentId
import fi.espoo.evaka.shared.MessageId
import fi.espoo.evaka.shared.MessageThreadId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class MunicipalMessageThreadMigrationService(asyncJobRunner: AsyncJobRunner<AsyncJob>) {
    init {
        asyncJobRunner.registerHandler(::migrateMunicipalMessageThreads)
    }

    fun migrateMunicipalMessageThreads(
        db: Database.Connection,
        @Suppress("UNUSED_PARAMETER") clock: EvakaClock,
        msg: AsyncJob.MigrateMunicipalMessageThreads,
    ) {
        var total = 0
        while (true) {
            val contentId = db.read { tx -> tx.findMunicipalContentIdWithDuplicateThreads() }

            if (contentId == null) {
                logger.info { "No more municipal message threads to migrate" }
                break
            }
            db.transaction { tx -> tx.consolidateMunicipalMessageThreads(contentId) }

            total++
            if (total >= msg.batchSize) {
                logger.info {
                    "Processed $total municipal message threads, stopping for this batch"
                }
                break
            }
        }
        logger.info { "Migrated $total municipal message threads" }
    }
}

private fun Database.Read.findMunicipalContentIdWithDuplicateThreads(): MessageContentId? =
    createQuery {
            sql(
                """
SELECT content_id
FROM message
WHERE sender_id = (SELECT id FROM message_account WHERE type = 'MUNICIPAL')
GROUP BY content_id
HAVING COUNT(*) > 1
LIMIT 1
"""
            )
        }
        .exactlyOneOrNull()

private fun Database.Transaction.consolidateMunicipalMessageThreads(contentId: MessageContentId) {
    logger.info { "Consolidating municipal message threads for content $contentId" }

    data class ThreadAndMessage(val threadId: MessageThreadId, val messageId: MessageId)

    val threadsAndMessages: List<ThreadAndMessage> =
        createQuery {
                sql(
                    """
SELECT mt.id AS thread_id, m.id AS message_id
FROM message_thread mt
JOIN message m ON m.thread_id = mt.id
WHERE m.content_id = ${bind(contentId)}
ORDER BY mt.id
"""
                )
            }
            .toList()

    if (threadsAndMessages.size <= 1) {
        logger.info { "No consolidation needed for content $contentId" }
        return
    }

    val toKeep = threadsAndMessages.first()
    val toDelete = threadsAndMessages.drop(1)
    val threadIdsToDelete = toDelete.map { it.threadId }
    val messageIdsToDelete = toDelete.map { it.messageId }

    execute {
        sql(
            """
UPDATE message_recipients
SET message_id = ${bind(toKeep.messageId)}
WHERE message_id = ANY(${bind(messageIdsToDelete)})
"""
        )
    }

    execute {
        sql(
            """
UPDATE message_thread_participant
SET thread_id = ${bind(toKeep.threadId)}
WHERE thread_id = ANY(${bind(threadIdsToDelete)})
"""
        )
    }

    // Delete associated children from ALL threads
    execute {
        sql(
            """
DELETE FROM message_thread_children
WHERE message_thread_id = ANY(${bind(threadsAndMessages.map { it.threadId })})
"""
        )
    }

    execute {
        sql(
            """
DELETE FROM message
WHERE id = ANY(${bind(messageIdsToDelete)})
"""
        )
    }

    execute {
        sql(
            """
DELETE FROM message_thread
WHERE id = ANY(${bind(threadIdsToDelete)})
"""
        )
    }

    logger.info {
        "Consolidated content $contentId: kept thread ${toKeep.threadId}, deleted ${threadIdsToDelete.size} threads"
    }
}
