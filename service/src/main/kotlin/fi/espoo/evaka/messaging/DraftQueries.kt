// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.MessageDraftId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.domain.HelsinkiDateTime

fun Database.Read.getDrafts(
    accountId: MessageAccountId,
    accountAccessLimit: AccountAccessLimit = AccountAccessLimit.NoFurtherLimit,
): List<DraftContent> {
    val accountAccessPredicate =
        if (accountAccessLimit is AccountAccessLimit.AvailableFrom)
            Predicate { where("$it.modified_at >= ${bind(accountAccessLimit.date)}") }
        else Predicate.alwaysTrue()

    return createQuery {
            sql(
                """
SELECT
    draft.*,
    (SELECT coalesce(jsonb_agg(jsonb_build_object(
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
WHERE draft.account_id = ${bind(accountId)} AND
    ${predicate(accountAccessPredicate.forTable("draft"))}
ORDER BY draft.created_at DESC
"""
            )
        }
        .toList<DraftContent>()
}

fun Database.Transaction.initDraft(
    accountId: MessageAccountId,
    now: HelsinkiDateTime = HelsinkiDateTime.now(),
): MessageDraftId {
    return createQuery {
            sql(
                """
INSERT INTO message_draft (account_id, modified_at)
VALUES (${bind(accountId)}, ${bind(now)})
RETURNING id
"""
            )
        }
        .exactlyOne<MessageDraftId>()
}

fun Database.Transaction.updateDraft(
    accountId: MessageAccountId,
    id: MessageDraftId,
    draft: UpdatableDraftContent,
    now: HelsinkiDateTime,
) =
    createUpdate {
            sql(
                """
UPDATE message_draft
SET
    account_id = ${bind(accountId)},
    title = ${bind(draft.title)},
    content = ${bind(draft.content)},
    urgent = ${bind(draft.urgent)},
    sensitive = ${bind(draft.sensitive)},
    type = ${bind(draft.type)},
    recipient_ids = ${bind(draft.recipientIds)},
    recipient_names = ${bind(draft.recipientNames)},
    modified_at = ${bind(now)}
WHERE id = ${bind(id)}
"""
            )
        }
        .updateExactlyOne()

fun Database.Transaction.deleteDraft(accountId: MessageAccountId, draftId: MessageDraftId) {
    createUpdate {
            sql(
                "DELETE FROM message_draft WHERE id = ${bind(draftId)} AND account_id = ${bind(accountId)}"
            )
        }
        .execute()
}
