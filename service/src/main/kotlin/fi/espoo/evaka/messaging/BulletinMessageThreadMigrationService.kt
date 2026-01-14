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
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.domain.EvakaClock
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class BulletinMessageThreadMigrationService(asyncJobRunner: AsyncJobRunner<AsyncJob>) {
    init {
        asyncJobRunner.registerHandler(::migrateBulletinMessageThreads)
    }

    fun migrateBulletinMessageThreads(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.MigrateMunicipalMessageThreads,
    ) {
        var total = 0

        // Migrate municipal message threads (both citizen and staff copy threads)
        while (total < msg.batchSize) {
            val contentId = db.read { tx -> tx.findMunicipalContentIdWithDuplicateThreads() }

            if (contentId == null) {
                logger.info { "No more municipal message threads to migrate" }
                break
            }
            db.transaction { tx ->
                logger.info { "Consolidating municipal message threads for content $contentId" }
                tx.consolidateMessageThreads(contentId, onlyStaffCopies = false)
            }
            total++
        }

        // Migrate all bulletin staff copy threads
        while (total < msg.batchSize) {
            val contentId =
                db.read { tx -> tx.findBulletinContentIdWithDuplicateStaffCopyThreads() }

            if (contentId == null) {
                logger.info { "No more bulletin staff copy threads to migrate" }
                break
            }
            db.transaction { tx ->
                logger.info { "Consolidating staff copy threads for content $contentId" }
                tx.consolidateMessageThreads(contentId, onlyStaffCopies = true)
            }
            total++
        }

        logger.info { "Migrated $total message threads" }
    }
}

private fun Database.Read.findMunicipalContentIdWithDuplicateThreads(): MessageContentId? =
    createQuery {
            sql(
                """
SELECT m.content_id
FROM message m
JOIN message_thread mt ON mt.id = m.thread_id
WHERE m.sender_id = (SELECT id FROM message_account WHERE type = 'MUNICIPAL')
GROUP BY m.content_id, mt.is_copy
HAVING COUNT(*) > 1
LIMIT 1
"""
            )
        }
        .exactlyOneOrNull()

private fun Database.Read.findBulletinContentIdWithDuplicateStaffCopyThreads(): MessageContentId? =
    createQuery {
            sql(
                """
SELECT m.content_id
FROM message m
JOIN message_thread mt ON mt.id = m.thread_id
WHERE mt.message_type = 'BULLETIN' AND mt.is_copy
GROUP BY m.content_id
HAVING COUNT(*) > 1
LIMIT 1
"""
            )
        }
        .exactlyOneOrNull()

private fun Database.Transaction.consolidateMessageThreads(
    contentId: MessageContentId,
    onlyStaffCopies: Boolean,
) {
    data class ThreadAndMessage(
        val threadId: MessageThreadId,
        val messageId: MessageId,
        val isCopy: Boolean,
    )

    val isCopyPredicate =
        if (onlyStaffCopies) {
            Predicate { where("$it.is_copy") }
        } else {
            Predicate.alwaysTrue()
        }

    val threadsAndMessages: Map<Boolean, List<ThreadAndMessage>> =
        createQuery {
                sql(
                    """
SELECT mt.id AS thread_id, m.id AS message_id, mt.is_copy
FROM message_thread mt
JOIN message m ON m.thread_id = mt.id
WHERE m.content_id = ${bind(contentId)} AND ${predicate(isCopyPredicate.forTable("mt"))}
ORDER BY mt.is_copy, mt.id
"""
                )
            }
            .toList<ThreadAndMessage>()
            .groupBy { it.isCopy }

    threadsAndMessages.forEach { (isCopy, group) ->
        if (group.size <= 1) {
            logger.info { "No consolidation needed for content $contentId (is_copy=$isCopy)" }
            return@forEach
        }

        val toKeep = group.first()
        val toDelete = group.drop(1)
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

        // Consolidate reiceiver's message_thread_participant entries to the kept thread
        execute {
            sql(
                """
UPDATE message_thread_participant
SET thread_id = ${bind(toKeep.threadId)}
WHERE
    thread_id = ANY(${bind(threadIdsToDelete)}) AND
    last_received_timestamp IS NOT NULL
"""
            )
        }

        // Delete sender's message_thread_participant entries from deleted threads
        execute {
            sql(
                """
DELETE FROM message_thread_participant
WHERE
    thread_id = ANY(${bind(threadIdsToDelete)}) AND
    last_sent_timestamp IS NOT NULL
"""
            )
        }

        // Delete associated children from ALL threads in this group (including kept one)
        // This maintains the invariant that municipal bulletins and staff copies have no children
        execute {
            sql(
                """
DELETE FROM message_thread_children
WHERE thread_id = ANY(${bind(group.map { it.threadId })})
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
            "Consolidated content $contentId (is_copy=$isCopy): kept thread ${toKeep.threadId}, deleted ${threadIdsToDelete.size} threads"
        }
    }
}
