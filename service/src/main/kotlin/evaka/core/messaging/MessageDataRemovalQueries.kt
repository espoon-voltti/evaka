// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.messaging

import evaka.core.shared.AttachmentId
import evaka.core.shared.MessageContentId
import evaka.core.shared.MessageDraftId
import evaka.core.shared.MessageThreadId
import evaka.core.shared.db.Database
import evaka.core.shared.domain.HelsinkiDateTime

data class DeletedBulletinContent(
    val contentId: MessageContentId,
    val attachmentIds: List<AttachmentId>,
)

data class DeletedBulletinThreadBatch(
    val threadIds: List<MessageThreadId>,
    val contents: List<DeletedBulletinContent>,
)

data class DeletedBulletinDraft(val draftId: MessageDraftId, val attachmentIds: List<AttachmentId>)

fun Database.Transaction.deleteExpiredBulletinThreads(
    expiresBefore: HelsinkiDateTime,
    limit: Int,
): DeletedBulletinThreadBatch {
    // The original sender can reply to a bulletin, so a long-expired thread may still hold a recent
    // follow-up. Such a thread is kept until the follow-up expires too.
    //
    // Bulletins linked to an application exist only because of an earlier bug. Those bulletins are
    // deleted after the application is expired and deleted.
    val threadIds =
        createQuery {
                sql(
                    """
SELECT mt.id
FROM message_thread mt
WHERE
    mt.message_type = 'BULLETIN' AND
    mt.application_id IS NULL AND
    mt.created < ${bind(expiresBefore)} AND
    NOT EXISTS (
        SELECT 1
        FROM message m
        WHERE m.thread_id = mt.id AND m.created >= ${bind(expiresBefore)}
    ) AND
    NOT EXISTS (
        SELECT 1
        FROM message m
        JOIN application_note an ON an.message_content_id = m.content_id
        WHERE m.thread_id = mt.id
    )
LIMIT ${bind(limit)}
FOR UPDATE OF mt
"""
                )
            }
            .toList<MessageThreadId>()

    if (threadIds.isEmpty()) return DeletedBulletinThreadBatch(emptyList(), emptyList())

    val contentIds =
        createQuery {
                sql(
                    "SELECT DISTINCT content_id FROM message WHERE thread_id = ANY(${bind(threadIds)})"
                )
            }
            .toList<MessageContentId>()

    execute { sql("DELETE FROM message_thread WHERE id = ANY(${bind(threadIds)})") }

    return DeletedBulletinThreadBatch(
        threadIds = threadIds,
        contents = deleteUnreferencedMessageContents(contentIds),
    )
}

/**
 * All threads of a single send share one content, so a content outlives the batch that removed only
 * some of its threads and is deleted once the last one is gone.
 */
private fun Database.Transaction.deleteUnreferencedMessageContents(
    contentIds: List<MessageContentId>
): List<DeletedBulletinContent> {
    if (contentIds.isEmpty()) return emptyList()

    val deletableIds =
        createQuery {
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
    val attachmentsByContent =
        createQuery {
                sql(
                    "SELECT message_content_id, id FROM attachment WHERE message_content_id = ANY(${bind(deletableIds)})"
                )
            }
            .toList { columnPair<MessageContentId, AttachmentId>("message_content_id", "id") }
            .groupBy({ it.first }, { it.second })

    execute { sql("DELETE FROM message_content WHERE id = ANY(${bind(deletableIds)})") }

    return deletableIds.map { DeletedBulletinContent(it, attachmentsByContent[it] ?: emptyList()) }
}

fun Database.Transaction.deleteExpiredBulletinDrafts(
    expiresBefore: HelsinkiDateTime,
    limit: Int,
): List<DeletedBulletinDraft> {
    val draftIds =
        createQuery {
                sql(
                    """
SELECT id
FROM message_draft
WHERE
    type = 'BULLETIN' AND
    created_at < ${bind(expiresBefore)} AND
    modified_at < ${bind(expiresBefore)}
LIMIT ${bind(limit)}
FOR UPDATE
"""
                )
            }
            .toList<MessageDraftId>()

    if (draftIds.isEmpty()) return emptyList()

    // attachment.message_draft_id is ON DELETE SET NULL, so the attachments must be read before the
    // drafts are deleted
    val attachmentsByDraft =
        createQuery {
                sql(
                    "SELECT message_draft_id, id FROM attachment WHERE message_draft_id = ANY(${bind(draftIds)})"
                )
            }
            .toList { columnPair<MessageDraftId, AttachmentId>("message_draft_id", "id") }
            .groupBy({ it.first }, { it.second })

    execute { sql("DELETE FROM message_draft WHERE id = ANY(${bind(draftIds)})") }

    return draftIds.map { DeletedBulletinDraft(it, attachmentsByDraft[it] ?: emptyList()) }
}
