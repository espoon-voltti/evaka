// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.sficlient

import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.SfiMessageId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.util.UUID

fun Database.Transaction.storeSfiGetEventsContinuationToken(continuationToken: String) {
    createUpdate {
            sql(
                """
INSERT INTO sfi_get_events_continuation_token (continuation_token)
VALUES (${bind(continuationToken)})
"""
            )
        }
        .execute()
}

fun Database.Read.getLatestSfiGetEventsContinuationToken(): String? =
    createQuery {
            sql(
                """
SELECT continuation_token, created_at
FROM sfi_get_events_continuation_token
ORDER BY created_at DESC limit 1;
"""
            )
        }
        .mapTo<String>()
        .exactlyOneOrNull()

fun Database.Read.getSentSfiMessageBySfiId(sfiId: Int): SentSfiMessage? =
    createQuery { sql("SELECT * FROM sfi_message WHERE sfi_id = ${bind(sfiId)}") }
        .mapTo<SentSfiMessage>()
        .exactlyOneOrNull()

fun Database.Transaction.storeSentSfiMessage(message: SentSfiMessage) {
    createUpdate {
            sql(
                """
INSERT INTO sfi_message (
    external_id,
    sfi_id,
    guardian_id,
    decision_id,
    document_id,
    fee_decision_id,
    voucher_value_decision_id
) VALUES (
    ${bind(message.externalId)},
    ${bind(message.sfiId)},
    ${bind(message.guardianId)},
    ${bind(message.decisionId)},
    ${bind(message.documentId)},
    ${bind(message.feeDecisionId)},
    ${bind(message.voucherValueDecisionId)}
)
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<SfiMessageId>()
}

data class SentSfiMessage(
    val id: UUID? = null,
    val guardianId: PersonId,
    val externalId: String,
    val sfiId: Int? = null,
    val createdAt: HelsinkiDateTime? = null,
    val updatedAt: HelsinkiDateTime? = null,
    val decisionId: UUID? = null,
    val documentId: UUID? = null,
    val feeDecisionId: UUID? = null,
    val voucherValueDecisionId: UUID? = null,
)
