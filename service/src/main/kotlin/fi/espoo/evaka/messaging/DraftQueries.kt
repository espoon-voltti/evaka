// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.MessageDraftId
import fi.espoo.evaka.shared.db.Database

fun Database.Read.getDrafts(accountId: MessageAccountId): List<DraftContent> =
    @Suppress("DEPRECATION")
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
        .toList<DraftContent>()

fun Database.Transaction.initDraft(accountId: MessageAccountId): MessageDraftId {
    @Suppress("DEPRECATION")
    return this.createQuery(
            """
        INSERT INTO message_draft (account_id) VALUES (:accountId) RETURNING id
        """
                .trimIndent()
        )
        .bind("accountId", accountId)
        .exactlyOne<MessageDraftId>()
}

fun Database.Transaction.updateDraft(
    accountId: MessageAccountId,
    id: MessageDraftId,
    draft: UpdatableDraftContent
) =
    @Suppress("DEPRECATION")
    createUpdate(
            """
        UPDATE message_draft
        SET
            account_id = :accountId,
            title = :title,
            content = :content,
            urgent = :urgent,
            sensitive = :sensitive,
            type = :type,
            recipient_ids = :recipientIds,
            recipient_names = :recipientNames
        WHERE id = :id
        """
                .trimIndent()
        )
        .bindKotlin(draft)
        .bind("id", id)
        .bind("accountId", accountId)
        .updateExactlyOne()

fun Database.Transaction.deleteDraft(accountId: MessageAccountId, draftId: MessageDraftId) {
    @Suppress("DEPRECATION")
    this.createUpdate("DELETE FROM message_draft WHERE id = :id AND account_id = :accountId")
        .bind("accountId", accountId)
        .bind("id", draftId)
        .execute()
}
