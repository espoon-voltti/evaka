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
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.util.UUID

fun Database.Transaction.storeSfiGetEventsContinuationToken(continuationToken: String) {
    createUpdate {
            sql(
                """
INSERT INTO sfi_get_events_continuation_token (continuation_token)
VALUES (${bind(continuationToken)})
ON CONFLICT DO NOTHING
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

private fun Database.Read.hasSfiMessageBeenSent(
    predicate: Predicate,
    guardianId: PersonId?,
): Boolean {
    val fullPredicate =
        Predicate.allNotNull(
            predicate,
            guardianId?.let { Predicate { where("$it.guardian_id = ${bind(guardianId)}") } },
        )
    return createQuery {
            sql(
                """
SELECT EXISTS (
    SELECT 1
    FROM sfi_message
    WHERE ${predicate(fullPredicate.forTable("sfi_message"))}
)
"""
            )
        }
        .exactlyOne()
}

fun Database.Read.hasDecisionSfiMessageBeenSent(
    decisionId: DecisionId,
    guardianId: PersonId? = null,
) = hasSfiMessageBeenSent(Predicate { where("$it.decision_id = ${bind(decisionId)}") }, guardianId)

fun Database.Read.hasChildDocumentSfiMessageBeenSent(
    documentId: ChildDocumentId,
    guardianId: PersonId? = null,
) = hasSfiMessageBeenSent(Predicate { where("$it.document_id = ${bind(documentId)}") }, guardianId)

fun Database.Read.hasFeeDecisionSfiMessageBeenSent(
    feeDecisionId: FeeDecisionId,
    guardianId: PersonId? = null,
) =
    hasSfiMessageBeenSent(
        Predicate { where("$it.fee_decision_id = ${bind(feeDecisionId)}") },
        guardianId,
    )

fun Database.Read.hasVoucherValueDecisionSfiMessageBeenSent(
    voucherValueDecisionId: VoucherValueDecisionId,
    guardianId: PersonId? = null,
) =
    hasSfiMessageBeenSent(
        Predicate { where("$it.voucher_value_decision_id = ${bind(voucherValueDecisionId)}") },
        guardianId,
    )

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

fun Database.Transaction.upsertSfiMessageEventIfSfiMessageExists(
    event: SfiMessageEvent
): SfiMessageEventId =
    createUpdate {
            sql(
                """
WITH existing_message AS (
    SELECT 1 FROM sfi_message WHERE id = ${bind(event.messageId)}
)
INSERT INTO sfi_message_event (message_id, event_type)
SELECT ${bind(event.messageId)}, ${bind(event.eventType)}
FROM existing_message
ON CONFLICT (message_id, event_type)
DO UPDATE SET updated_at = now()
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
