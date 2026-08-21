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

fun Database.Transaction.deleteExpiredBulletinThreads(
    recipientExpireDate: LocalDate,
    expiresBefore: HelsinkiDateTime,
    limit: Int,
): DeletedMessageThreadBatch {
    // A municipal bulletin shares one thread across a whole area or unit and records no children at
    // all, so it has no placement to expire by and falls to the age limit instead.
    //
    // A staff copy shares its content with the bulletin it copies. A copy is deleted as soon as the
    // original bulletin is gone, which takes one further round.
    //
    // Bulletins linked to an application exist only because of an earlier bug.
    val threadIds = createQuery {
        sql(
            """
SELECT mt.id
FROM message_thread mt
LEFT JOIN LATERAL (
    SELECT max(pl.end_date) AS last_placement_end
    FROM message_thread_children mtc
    JOIN placement pl ON pl.child_id = mtc.child_id
    WHERE mtc.thread_id = mt.id
) recipients ON true
WHERE
    mt.message_type = 'BULLETIN' AND
    ${predicate(unreferencedByApplication.forTable("mt"))} AND
    CASE
        WHEN mt.is_copy THEN NOT EXISTS (
            SELECT 1
            FROM message copy_message
            JOIN message original ON original.content_id = copy_message.content_id
            JOIN message_thread original_thread ON original_thread.id = original.thread_id
            WHERE copy_message.thread_id = mt.id AND NOT original_thread.is_copy
        )
        WHEN recipients.last_placement_end IS NOT NULL THEN
            recipients.last_placement_end < ${bind(recipientExpireDate)}
        ELSE
            mt.created < ${bind(expiresBefore)} AND
            NOT EXISTS (
                SELECT 1
                FROM message m
                WHERE m.thread_id = mt.id AND m.created >= ${bind(expiresBefore)}
            )
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
 * Deletes every thread, of any message type, whose children have all expired. A thread of several
 * children survives until the last of them expires, because the whole conversation is also part of
 * the data of the children who are still retained.
 *
 * A thread that records no children at all - a municipal bulletin, a staff copy, or a message of
 * the service worker or the finance account - has nothing to expire by and is left to the rules of
 * its own message type.
 *
 * A thread of an application, or one whose content an application note references, is kept even
 * when its children have expired, until the application itself is expired and deleted.
 */
fun Database.Transaction.deleteMessageThreadsOfExpiredChildren(
    expiredChildIdsQuery: QuerySql,
    limit: Int,
): DeletedMessageThreadBatch {
    val threadIds = createQuery {
        sql(
            """
WITH expired_child (id) AS (
    ${subquery(expiredChildIdsQuery)}
), expired_child_thread AS (
    SELECT DISTINCT mtc.thread_id
    FROM message_thread_children mtc
    JOIN expired_child ec ON ec.id = mtc.child_id
)
SELECT mt.id
FROM expired_child_thread ect
JOIN message_thread mt ON mt.id = ect.thread_id
WHERE
    NOT EXISTS (
        SELECT 1
        FROM message_thread_children mtc
        WHERE
            mtc.thread_id = mt.id AND
            NOT EXISTS (SELECT 1 FROM expired_child ec WHERE ec.id = mtc.child_id)
    ) AND
    ${predicate(unreferencedByApplication.forTable("mt"))}
LIMIT ${bind(limit)}
FOR UPDATE OF mt
"""
        )
    }
        .toList<MessageThreadId>()

    return deleteMessageThreads(threadIds)
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
