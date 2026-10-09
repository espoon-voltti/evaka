// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.messaging

import evaka.core.shared.AttachmentId
import evaka.core.shared.ChildId
import evaka.core.shared.MessageContentId
import evaka.core.shared.MessageDraftId
import evaka.core.shared.MessageThreadId
import evaka.core.shared.db.Database
import evaka.core.shared.db.Predicate
import evaka.core.shared.db.QuerySql
import evaka.core.shared.domain.HelsinkiDateTime
import java.time.LocalDate

data class DeletedThreadContent(
    val contentId: MessageContentId,
    val attachmentIds: List<AttachmentId>,
)

data class DeletedMessageThread(val threadId: MessageThreadId, val childIds: List<ChildId>)

data class DeletedMessageThreadBatch(
    val threads: List<DeletedMessageThread>,
    val contents: List<DeletedThreadContent>,
)

data class DeletedMessageDraft(val draftId: MessageDraftId, val attachmentIds: List<AttachmentId>)

/**
 * A thread that an application still references survives its own retention rules. Removing an
 * expired application clears the application link of its threads and cascades away the notes that
 * copy the messages, which is what eventually satisfies this predicate.
 */
private val unreferencedByApplication = Predicate {
    where(
        """
$it.application_id IS NULL AND
NOT EXISTS (
    SELECT 1
    FROM message m
    JOIN application_note an ON an.message_content_id = m.content_id
    WHERE m.thread_id = $it.id
)
"""
    )
}

/** A thread with no messages at all expires by its own creation time */
private fun lastMessageBefore(expiresBefore: HelsinkiDateTime) = Predicate {
    where(
        """
$it.created < ${bind(expiresBefore)} AND
NOT EXISTS (
    SELECT 1
    FROM message m
    WHERE m.thread_id = $it.id AND m.created >= ${bind(expiresBefore)}
)
"""
    )
}

/**
 * The last placement end is null when the children of the thread have no placements, which leaves
 * the thread to expire by its messages.
 */
private fun expiredByChildPlacements(
    placementExpireDate: LocalDate,
    expiresBefore: HelsinkiDateTime,
) = Predicate {
    where(
        """
COALESCE(
    (
        SELECT max(pl.end_date)
        FROM message_thread_children mtc
        JOIN placement pl ON pl.child_id = mtc.child_id
        WHERE mtc.thread_id = $it.id
    ) < ${bind(placementExpireDate)},
    ${predicate(lastMessageBefore(expiresBefore).forTable(it))}
)
"""
    )
}

fun Database.Transaction.deleteExpiredBulletinThreads(
    placementExpireDate: LocalDate,
    expiresBefore: HelsinkiDateTime,
    limit: Int,
): DeletedMessageThreadBatch {
    // A municipal bulletin shares one thread across a whole area or unit and records no children at
    // all, so it has no placement to expire by and falls to the age limit instead.
    //
    // A staff copy shares its content with its original and is deleted after the original.
    // Regular messages were copied too until 2024, so copies of any type are deleted.
    //
    // Bulletins linked to an application exist only because of an earlier bug.
    val threadIds = createQuery {
        sql(
            """
SELECT mt.id
FROM message_thread mt
WHERE
    (mt.message_type = 'BULLETIN' OR mt.is_copy) AND
    ${predicate(unreferencedByApplication.forTable("mt"))} AND
    CASE
        WHEN mt.is_copy THEN NOT EXISTS (
            SELECT 1
            FROM message copy_message
            JOIN message original ON original.content_id = copy_message.content_id
            JOIN message_thread original_thread ON original_thread.id = original.thread_id
            WHERE copy_message.thread_id = mt.id AND NOT original_thread.is_copy
        )
        ELSE ${predicate(expiredByChildPlacements(placementExpireDate, expiresBefore).forTable("mt"))}
    END
LIMIT ${bind(limit)}
FOR UPDATE OF mt
"""
        )
    }
        .toList<MessageThreadId>()

    return deleteMessageThreads(threadIds)
}

/**
 * Bulletins, staff copies, and the threads of the service worker and finance accounts have their
 * own removal rules.
 */
private val regularThread = Predicate {
    where(
        """
$it.message_type = 'MESSAGE' AND
NOT $it.is_copy AND
NOT EXISTS (
    SELECT 1
    FROM message m
    JOIN message_account sender ON sender.id = m.sender_id
    WHERE m.thread_id = $it.id AND sender.type IN ('SERVICE_WORKER', 'FINANCE')
)
"""
    )
}

/**
 * A regular thread expires once the last placement of its children has ended before
 * [placementExpireDate]. A thread whose children have no placements expires once it has had no
 * messages since [expiresBefore].
 *
 * Threads started before September 2022, and until November 2023 by a citizen who chose none of
 * their children, record no children, so they expire by their messages.
 *
 * A thread of an application, or one whose content an application note references, is kept even
 * when it has expired, until the application itself is expired and deleted.
 */
fun Database.Transaction.deleteExpiredRegularThreads(
    placementExpireDate: LocalDate,
    expiresBefore: HelsinkiDateTime,
    limit: Int,
): DeletedMessageThreadBatch {
    val threadIds = createQuery {
        sql(
            """
SELECT mt.id
FROM message_thread mt
WHERE
    ${predicate(regularThread.forTable("mt"))} AND
    ${predicate(unreferencedByApplication.forTable("mt"))} AND
    ${predicate(expiredByChildPlacements(placementExpireDate, expiresBefore).forTable("mt"))}
LIMIT ${bind(limit)}
FOR UPDATE OF mt
"""
        )
    }
        .toList<MessageThreadId>()

    return deleteMessageThreads(threadIds)
}

/**
 * Deletes the threads of the service worker account whose application is gone.
 *
 * Every service worker thread has an application, and each of its messages is also copied into a
 * note of that application. Only removing the application clears both the link and the notes, so
 * the thread cannot be deleted before that.
 */
fun Database.Transaction.deleteExpiredServiceWorkerThreads(limit: Int): DeletedMessageThreadBatch {
    val threadIds = createQuery {
        sql(
            """
WITH service_worker_thread AS (
    SELECT DISTINCT m.thread_id
    FROM message_account acc
    JOIN message m ON m.sender_id = acc.id
    WHERE acc.type = 'SERVICE_WORKER'
)
SELECT mt.id
FROM service_worker_thread swt
JOIN message_thread mt ON mt.id = swt.thread_id
WHERE ${predicate(unreferencedByApplication.forTable("mt"))}
LIMIT ${bind(limit)}
FOR UPDATE OF mt
"""
        )
    }
        .toList<MessageThreadId>()

    return deleteMessageThreads(threadIds)
}

/**
 * A finance thread concerns every child connected to its citizens. It expires once the last
 * placement of those children has ended before [placementExpireDate], if they have any, and it has
 * had no messages since [expiresBefore].
 */
fun Database.Transaction.deleteExpiredFinanceThreads(
    financeConnectionsQuery: QuerySql,
    placementExpireDate: LocalDate,
    expiresBefore: HelsinkiDateTime,
    limit: Int,
): DeletedMessageThreadBatch {
    val threadIds = createQuery {
        sql(
            """
WITH finance_connection AS (
    ${subquery(financeConnectionsQuery)}
), finance_thread AS (
    SELECT DISTINCT m.thread_id
    FROM message_account acc
    JOIN message m ON m.sender_id = acc.id
    WHERE acc.type = 'FINANCE'
)
SELECT mt.id
FROM finance_thread ft
JOIN message_thread mt ON mt.id = ft.thread_id
LEFT JOIN LATERAL (
    SELECT max(pl.end_date) AS last_placement_end
    FROM message m
    JOIN message_recipients mr ON mr.message_id = m.id
    JOIN message_account acc ON acc.id = mr.recipient_id AND acc.type = 'CITIZEN'
    JOIN finance_connection fc ON fc.person_id = acc.person_id
    JOIN placement pl ON pl.child_id = fc.child_id
    WHERE m.thread_id = mt.id
) children ON true
WHERE
    (
        children.last_placement_end IS NULL OR
        children.last_placement_end < ${bind(placementExpireDate)}
    ) AND
    ${predicate(lastMessageBefore(expiresBefore).forTable("mt"))}
LIMIT ${bind(limit)}
FOR UPDATE OF mt
"""
        )
    }
        .toList<MessageThreadId>()

    return deleteMessageThreads(threadIds)
}

fun personIdsWithFinanceThreads() = QuerySql {
    sql(
        """
SELECT DISTINCT recipient.person_id
FROM message m
JOIN message_recipients mr ON mr.message_id = m.id
JOIN message_account recipient ON recipient.id = mr.recipient_id AND recipient.type = 'CITIZEN'
WHERE EXISTS (
    SELECT 1
    FROM message sent
    JOIN message_account sender ON sender.id = sent.sender_id
    WHERE sent.thread_id = m.thread_id AND sender.type = 'FINANCE'
)
"""
    )
}

private fun Database.Transaction.deleteMessageThreads(
    threadIds: List<MessageThreadId>
): DeletedMessageThreadBatch {
    if (threadIds.isEmpty()) return DeletedMessageThreadBatch(emptyList(), emptyList())

    val childIdsByThread = createQuery {
        sql(
            "SELECT thread_id, child_id FROM message_thread_children WHERE thread_id = ANY(${bind(threadIds)})"
        )
    }
        .toList { columnPair<MessageThreadId, ChildId>("thread_id", "child_id") }
        .groupBy({ it.first }, { it.second })

    val contentIds = createQuery {
        sql("SELECT DISTINCT content_id FROM message WHERE thread_id = ANY(${bind(threadIds)})")
    }
        .toList<MessageContentId>()

    execute { sql("DELETE FROM message_thread WHERE id = ANY(${bind(threadIds)})") }

    return DeletedMessageThreadBatch(
        threads = threadIds.map { DeletedMessageThread(it, childIdsByThread[it] ?: emptyList()) },
        contents = deleteUnreferencedMessageContents(contentIds),
    )
}

/**
 * All threads of a single send share one content, so a content outlives the batch that removed only
 * some of its threads and is deleted once the last one is gone.
 */
private fun Database.Transaction.deleteUnreferencedMessageContents(
    contentIds: List<MessageContentId>
): List<DeletedThreadContent> {
    if (contentIds.isEmpty()) return emptyList()

    val deletableIds = createQuery {
        sql(
            """
SELECT mc.id
FROM message_content mc
WHERE
    mc.id = ANY(${bind(contentIds)}) AND
    NOT EXISTS (SELECT 1 FROM message m WHERE m.content_id = mc.id)
"""
        )
    }
        .toList<MessageContentId>()

    if (deletableIds.isEmpty()) return emptyList()

    // attachment.message_content_id is ON DELETE SET NULL, so the attachments must be read before
    // the contents are deleted
    val attachmentsByContent = createQuery {
        sql(
            "SELECT message_content_id, id FROM attachment WHERE message_content_id = ANY(${bind(deletableIds)})"
        )
    }
        .toList { columnPair<MessageContentId, AttachmentId>("message_content_id", "id") }
        .groupBy({ it.first }, { it.second })

    execute { sql("DELETE FROM message_content WHERE id = ANY(${bind(deletableIds)})") }

    return deletableIds.map { DeletedThreadContent(it, attachmentsByContent[it] ?: emptyList()) }
}

fun Database.Transaction.deleteExpiredMessageDrafts(
    expiresBefore: HelsinkiDateTime,
    limit: Int,
): List<DeletedMessageDraft> {
    val draftIds = createQuery {
        sql(
            """
SELECT id
FROM message_draft
WHERE created_at < ${bind(expiresBefore)}
LIMIT ${bind(limit)}
FOR UPDATE
"""
        )
    }
        .toList<MessageDraftId>()

    if (draftIds.isEmpty()) return emptyList()

    // attachment.message_draft_id is ON DELETE SET NULL, so the attachments must be read before the
    // drafts are deleted
    val attachmentsByDraft = createQuery {
        sql(
            "SELECT message_draft_id, id FROM attachment WHERE message_draft_id = ANY(${bind(draftIds)})"
        )
    }
        .toList { columnPair<MessageDraftId, AttachmentId>("message_draft_id", "id") }
        .groupBy({ it.first }, { it.second })

    execute { sql("DELETE FROM message_draft WHERE id = ANY(${bind(draftIds)})") }

    return draftIds.map { DeletedMessageDraft(it, attachmentsByDraft[it] ?: emptyList()) }
}
