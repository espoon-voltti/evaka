// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging.message

import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.MessageDraftId
import fi.espoo.evaka.shared.db.Database
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo

fun Database.Read.getDrafts(accountId: MessageAccountId): List<DraftContent> =
    this.createQuery(
        """
        SELECT
            draft.*,
            (SELECT coalesce(jsonb_agg(json_build_object(
                    'id', id,
                    'name', name,
                    'contentType', content_type
                )), '[]'::jsonb) FROM (
                SELECT id, name, content_type
                FROM attachment a
                WHERE a.message_draft_id = draft.id
                ORDER BY a.created
            ) s) AS attachments
        FROM message_draft draft
        WHERE draft.account_id = :accountId
        ORDER BY draft.created DESC
        """
    )
        .bind("accountId", accountId)
        .mapTo<DraftContent>()
        .list()

fun Database.Transaction.initDraft(accountId: MessageAccountId): MessageDraftId {
    return this.createQuery(
        """
        INSERT INTO message_draft (account_id) VALUES (:accountId) RETURNING id
        """.trimIndent()
    )
        .bind("accountId", accountId)
        .mapTo<MessageDraftId>()
        .one()
}

fun Database.Transaction.upsertDraft(accountId: MessageAccountId, id: MessageDraftId, draft: UpsertableDraftContent) {
    this.createUpdate(
        """
        INSERT INTO message_draft (id, account_id, title, content, type, recipient_ids, recipient_names)
        VALUES (:id, :accountId, :title, :content, :type, :recipientIds, :recipientNames)
        ON CONFLICT (id)
        DO UPDATE SET
             account_id = excluded.account_id,
             title = excluded.title,
             content = excluded.content,
             type = excluded.type,
             recipient_ids = excluded.recipient_ids,
             recipient_names = excluded.recipient_names
        """.trimIndent()
    ).bindKotlin(draft)
        .bind("id", id)
        .bind("accountId", accountId)
        .execute()
}

fun Database.Transaction.deleteDraft(accountId: MessageAccountId, draftId: MessageDraftId) {
    this.createUpdate("DELETE FROM message_draft WHERE id = :id AND account_id = :accountId")
        .bind("accountId", accountId)
        .bind("id", draftId)
        .execute()
}

fun Database.Read.draftBelongsToAnyAccount(draftId: MessageDraftId, accountIds: Set<MessageAccountId>): Boolean =
    this.createQuery(
        """
SELECT 1 
FROM message_draft
WHERE
    id = :id AND
    account_id = ANY(:accountIds)
        """.trimIndent()
    )
        .bind("id", draftId)
        .bind("accountIds", accountIds.toTypedArray())
        .mapTo<Int>().any()
