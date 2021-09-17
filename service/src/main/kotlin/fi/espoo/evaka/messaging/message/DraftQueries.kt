// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging.message

import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.MessageDraftId
import fi.espoo.evaka.shared.db.Database
import org.jdbi.v3.core.kotlin.mapTo

fun Database.Read.getDrafts(accountId: MessageAccountId): List<DraftContent> {
    return this.createQuery("SELECT * FROM message_draft WHERE account_id = :accountId ORDER BY created DESC")
        .bind("accountId", accountId)
        .mapTo<DraftContent>()
        .list()
}

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
    ).bindMap(
        mapOf(
            "id" to id,
            "accountId" to accountId,
            "type" to draft.type,
            "title" to draft.title,
            "content" to draft.content,
            "recipientIds" to draft.recipientIds.toTypedArray(),
            "recipientNames" to draft.recipientNames.toTypedArray()
        )
    )
        .execute()
}

fun Database.Transaction.deleteDraft(accountId: MessageAccountId, draftId: MessageDraftId) {
    this.createUpdate("DELETE FROM message_draft WHERE id = :id AND account_id = :accountId")
        .bind("accountId", accountId)
        .bind("id", draftId)
        .execute()
}
