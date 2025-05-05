// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.sficlient

import fi.espoo.evaka.sficlient.rest.EventType
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.DecisionId
import fi.espoo.evaka.shared.FeeDecisionId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.SfiMessageEventId
import fi.espoo.evaka.shared.SfiMessageId
import fi.espoo.evaka.shared.VoucherValueDecisionId
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

fun Database.Transaction.storeSentSfiMessage(message: SentSfiMessage): SfiMessageId =
    createUpdate {
            sql(
                """
INSERT INTO sfi_message (
    sfi_id,
    guardian_id,
    decision_id,
    document_id,
    fee_decision_id,
    voucher_value_decision_id
) VALUES (
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

data class SentSfiMessage(
    val id: UUID? = null,
    val guardianId: PersonId,
    val sfiId: Int? = null,
    val createdAt: HelsinkiDateTime? = null,
    val updatedAt: HelsinkiDateTime? = null,
    val decisionId: DecisionId? = null,
    val documentId: ChildDocumentId? = null,
    val feeDecisionId: FeeDecisionId? = null,
    val voucherValueDecisionId: VoucherValueDecisionId? = null,
)

fun Database.Transaction.upsertSfiMessageEvent(event: SfiMessageEvent): SfiMessageEventId =
    createUpdate {
            sql(
                """
            INSERT INTO sfi_message_event (message_id, event_type)
            VALUES (${bind(event.messageId)}, ${bind(event.eventType)})
            ON CONFLICT (message_id, event_type)
            DO UPDATE SET 
                message_id = EXCLUDED.message_id,
                updated_at = now()
            RETURNING id
            """
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<SfiMessageEventId>()

fun Database.Read.getSfiMessageEventsByMessageId(messageId: SfiMessageId): List<SfiMessageEvent> =
    createQuery {
            sql(
                """
SELECT id, created_at, updated_at, message_id, event_type
FROM sfi_message_event
WHERE message_id = ${bind(messageId)}
                """
            )
        }
        .mapTo<SfiMessageEvent>()
        .toList()

data class SfiMessageEvent(
    val id: UUID? = null,
    val createdAt: HelsinkiDateTime? = null,
    val updatedAt: HelsinkiDateTime? = null,
    val messageId: SfiMessageId,
    val eventType: EventType,
)

fun Database.Read.getSfiGetEventsContinuationTokens(): List<String> =
    createQuery { sql("SELECT continuation_token FROM sfi_get_events_continuation_token") }.toList()
