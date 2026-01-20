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
import java.time.Duration
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class BulletinMessageThreadMigrationService(asyncJobRunner: AsyncJobRunner<AsyncJob>) {
    init {
        asyncJobRunner.registerHandler(::migrateBulletinMessageThreads)
    }

    fun migrateBulletinMessageThreads(
        dbc: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.MigrateMunicipalMessageThreads,
    ) {
        // A note on database transactions:
        // - New bulletin threads are created with the new format, so there's no risk that new data
        //   that needs to be migrated would be created when the migration is in progress.
        // - The migration updates "bookkeeping" references in a single, atomic transaction. After
        //   that, the clients start using the kept thread/message exclusively.
        // - The leftover data is then deleted in smaller batches in separate transactions to avoid
        //   long-running transactions and locks.

        var total = 0

        // Migrate municipal message threads (both citizen and staff copy threads)
        while (total < msg.batchSize) {
            val contentId = dbc.read { tx -> tx.findMunicipalContentIdWithDuplicateThreads() }

            if (contentId == null) {
                logger.info { "No more municipal message threads to migrate" }
                break
            }
            logger.info { "Consolidating municipal message threads for content $contentId" }
            consolidateMessageThreads(dbc, contentId, onlyStaffCopies = false)
            total++
        }

        // Migrate all bulletin staff copy threads
        while (total < msg.batchSize) {
            val contentId =
                dbc.read { tx -> tx.findBulletinContentIdWithDuplicateStaffCopyThreads() }

            if (contentId == null) {
                logger.info { "No more bulletin staff copy threads to migrate" }
                break
            }
            logger.info { "Consolidating staff copy threads for content $contentId" }
            consolidateMessageThreads(dbc, contentId, onlyStaffCopies = true)
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

private fun consolidateMessageThreads(
    dbc: Database.Connection,
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
        dbc.read { tx ->
            tx.createQuery {
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
        }

    threadsAndMessages.forEach { (isCopy, group) ->
        if (group.size <= 1) {
            // Shouldn't happen
            logger.info { "No consolidation needed for content $contentId (is_copy=$isCopy)" }
            return@forEach
        }

        val toKeep = group.first()
        val toDelete = group.drop(1)
        val threadIdsToDelete = toDelete.map { it.threadId }
        val messageIdsToDelete = toDelete.map { it.messageId }

        logger.info {
            "Consolidating ${toDelete.size} threads and messages into thread ${toKeep.threadId} for content $contentId (is_copy=$isCopy)"
        }

        // First transaction:
        // - Update "bookkeeping" references to point to the kept message/thread
        // - Delete child entries for the kept thread
        dbc.transaction { tx ->
            tx.setStatementTimeout(Duration.ofMinutes(10))

            tx.execute {
                sql(
                    """
UPDATE message_recipients
SET message_id = ${bind(toKeep.messageId)}
WHERE message_id = ANY(${bind(messageIdsToDelete)})
"""
                )
            }

            tx.execute {
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

            tx.execute {
                sql(
                    """
DELETE FROM message_thread_participant
WHERE
    thread_id = ANY(${bind(threadIdsToDelete)}) AND
    last_sent_timestamp IS NOT NULL
"""
                )
            }

            tx.execute {
                sql(
                    """
DELETE FROM message_thread_children
WHERE thread_id = ${bind(toKeep.threadId)}
"""
                )
            }
        }

        // At this point, the clients start using the kept thread exclusively. Now we can just
        // delete the unreferenced leftover data in separate transactions and smaller batches.
        logger.info { "Deleted bookkeeping references for content $contentId (is_copy=$isCopy)" }

        // NOTE: If the following fails halfway, e.g. the service restarts or there's a statement
        // timeout, data that can be removed needs to be searched manually.
        //
        // Example (adjust LIMIT to control batch size):
        //
        // -- Delete one orphan message
        // DELETE FROM message WHERE id = ANY(
        //     SELECT m.id
        //     FROM message m
        //     WHERE
        //         NOT EXISTS (SELECT FROM message_recipients mr WHERE mr.message_id = m.id) AND
        //         NOT EXISTS (SELECT FROM message_thread_participant mtp WHERE mtp.thread_id =
        // m.thread_id)
        //     LIMIT 1
        // );
        //
        // -- Delete one orphan thread
        // DELETE FROM message_thread WHERE id = (
        //     SELECT t.id
        //     FROM message_thread t
        //     WHERE
        //         NOT EXISTS (SELECT FROM message_thread_participant mtp WHERE mtp.thread_id =
        // t.id) AND
        //         NOT EXISTS (SELECT FROM message m WHERE m.thread_id = t.id)
        //     LIMIT 1
        // );

        // Avoid statement timeouts by doing at most 100 deletions per transaction

        logger.info { "Deleting unused message rows in chunks of 100" }
        messageIdsToDelete.chunked(100).forEach { messageIdBatch ->
            dbc.transaction { tx ->
                tx.setStatementTimeout(Duration.ofMinutes(10))
                tx.execute {
                    sql(
                        """
DELETE FROM message
WHERE id = ANY(${bind(messageIdBatch)})
"""
                    )
                }
            }
        }

        logger.info { "Deleting unused message_thread_child rows in chunks of 100" }
        threadIdsToDelete.chunked(100).forEach { threadIdBatch ->
            dbc.transaction { tx ->
                tx.execute {
                    tx.setStatementTimeout(Duration.ofMinutes(10))
                    sql(
                        """
DELETE FROM message_thread_children
WHERE thread_id = ANY(${bind(threadIdBatch)})
"""
                    )
                }
            }
        }

        logger.info { "Deleting unused message_thread rows in chunks of 100" }
        threadIdsToDelete.chunked(100).forEach { threadIdBatch ->
            dbc.transaction { tx ->
                tx.execute {
                    tx.setStatementTimeout(Duration.ofMinutes(10))
                    sql(
                        """
DELETE FROM message_thread
WHERE id = ANY(${bind(threadIdBatch)})
"""
                    )
                }
            }
        }

        logger.info {
            "Consolidated content $contentId (is_copy=$isCopy): kept thread ${toKeep.threadId}, deleted ${threadIdsToDelete.size} threads" +
                " and ${messageIdsToDelete.size} messages"
        }
    }
}
