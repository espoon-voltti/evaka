// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.citizenwebpush

import fi.espoo.evaka.messaging.MessageType
import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.MessageId
import fi.espoo.evaka.shared.MessageThreadId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.webpush.VapidJwt
import fi.espoo.evaka.webpush.WebPush
import fi.espoo.evaka.webpush.WebPushCrypto
import fi.espoo.evaka.webpush.WebPushEndpoint
import fi.espoo.evaka.webpush.WebPushNotification
import fi.espoo.evaka.webpush.WebPushPayload
import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.URI
import java.time.Duration

private val logger = KotlinLogging.logger {}

/**
 * Caller-supplied strategy for building a VAPID JWT for a given endpoint. Production wiring passes
 * `webPush.getValidToken(tx, clock, uri)` inside a short DB transaction. Tests supply a stub that
 * returns a mock VapidJwt without needing a DB.
 */
fun interface VapidJwtProvider {
    fun get(uri: URI): VapidJwt
}

class CitizenPushSender(
    private val store: CitizenPushSubscriptionStore,
    private val webPush: WebPush?,
) {
    fun notifyMessage(
        personId: PersonId,
        threadId: MessageThreadId,
        category: CitizenPushCategory,
        senderName: String,
        threadTitle: String,
        messageContent: String,
        language: CitizenPushLanguage,
        replyRecipientAccountIds: List<MessageAccountId>,
        jwtProvider: VapidJwtProvider = defaultJwtProviderOrNoop(),
    ) {
        val wp = webPush ?: return
        val file = store.load(personId) ?: return
        val entries = file.subscriptions.filter { category in it.enabledCategories }
        if (entries.isEmpty()) return

        val titleAndBody =
            CitizenPushMessages.forMessage(
                senderName = senderName,
                threadTitle = threadTitle,
                messageContent = messageContent,
            )
        val replyAction =
            if (category != CitizenPushCategory.BULLETIN && replyRecipientAccountIds.isNotEmpty()) {
                val strings = CitizenPushMessages.forReplyAction(language)
                WebPushPayload.NotificationV1.ReplyAction(
                    threadId = threadId,
                    recipientAccountIds = replyRecipientAccountIds.toSet(),
                    actionLabel = strings.actionLabel,
                    actionPlaceholder = strings.actionPlaceholder,
                    successTitle = strings.successTitle,
                    successBody = strings.successBody,
                    errorTitle = strings.errorTitle,
                    errorBody = strings.errorBody,
                )
            } else {
                null
            }
        val iconPath =
            when (category) {
                CitizenPushCategory.URGENT_MESSAGE -> "/citizen/notifications/urgent-message.png"
                CitizenPushCategory.MESSAGE -> "/citizen/notifications/message.png"
                CitizenPushCategory.BULLETIN -> "/citizen/notifications/bulletin.png"
            }
        val payload =
            WebPushPayload.NotificationV1(
                title = titleAndBody.title,
                body = titleAndBody.body,
                tag = "msg-$threadId",
                url = "/messages/$threadId",
                iconPath = iconPath,
                replyAction = replyAction,
            )

        entries.forEach { entry -> sendOne(personId, entry, payload, jwtProvider, wp) }
    }

    fun sendTest(
        personId: PersonId,
        language: CitizenPushLanguage,
        jwtProvider: VapidJwtProvider = defaultJwtProviderOrNoop(),
    ) {
        val wp = webPush ?: return
        val file = store.load(personId) ?: return
        val titleAndBody = CitizenPushMessages.forTest(language)
        val payload =
            WebPushPayload.NotificationV1(
                title = titleAndBody.title,
                body = titleAndBody.body,
                tag = "welcome",
                url = "/personal-details",
                iconPath = "/citizen/notifications/welcome.png",
            )
        file.subscriptions.forEach { entry -> sendOne(personId, entry, payload, jwtProvider, wp) }
    }

    private fun sendOne(
        personId: PersonId,
        entry: CitizenPushSubscriptionEntry,
        payload: WebPushPayload.NotificationV1,
        jwtProvider: VapidJwtProvider,
        webPush: WebPush,
    ) {
        val endpoint =
            try {
                WebPushEndpoint(
                    uri = entry.endpoint,
                    ecdhPublicKey = WebPushCrypto.decodePublicKey(entry.ecdhKey.toByteArray()),
                    authSecret = entry.authSecret.toByteArray(),
                )
            } catch (e: Exception) {
                logger.warn(e) { "Failed to decode stored push endpoint for $personId; removing" }
                store.removeSubscription(personId, entry.endpoint)
                return
            }

        val notification =
            WebPushNotification(
                endpoint = endpoint,
                ttl = Duration.ofDays(1),
                payloads = listOf(payload),
            )

        try {
            val vapidJwt = jwtProvider.get(endpoint.uri)
            webPush.send(vapidJwt, notification)
        } catch (e: WebPush.SubscriptionExpired) {
            logger.warn { "Citizen push subscription expired (${e.status}); removing" }
            store.removeSubscription(personId, entry.endpoint)
        } catch (e: Exception) {
            logger.warn(e) { "Citizen push send failed; swallowing" }
        }
    }

    private fun defaultJwtProviderOrNoop(): VapidJwtProvider = VapidJwtProvider { _ ->
        error(
            "No VapidJwtProvider supplied. Production callers must route through " +
                "CitizenPushSender.notifyMessage(..., jwtProvider = { uri -> " +
                "db.transaction { tx -> webPush.getValidToken(tx, clock, uri) } })."
        )
    }

    /**
     * Production entry point called from MessageService.handleMarkMessageAsSent, OUTSIDE its main
     * transaction. Opens its own short read-only transaction to resolve per-recipient context, then
     * releases the connection before doing S3 / HTTP. Best-effort: any exception is caught and
     * logged, never re-thrown.
     *
     * Known POC tradeoff: the jwtProvider closure opens one DB transaction per device when fetching
     * the cached VAPID JWT. For a staging POC this is fine; a production version should batch JWT
     * acquisition across all distinct push-service origins.
     */
    fun handleSentMessages(
        db: Database.Connection,
        clock: EvakaClock,
        messageIds: List<MessageId>,
    ) {
        if (webPush == null || messageIds.isEmpty()) return
        val recipients =
            try {
                db.transaction { tx -> tx.getCitizenPushRecipients(messageIds) }
            } catch (e: Exception) {
                logger.warn(e) { "Citizen push: failed to resolve recipients; skipping" }
                return
            }
        for (r in recipients) {
            val category =
                when {
                    r.urgent -> CitizenPushCategory.URGENT_MESSAGE
                    r.threadType == MessageType.BULLETIN -> CitizenPushCategory.BULLETIN
                    else -> CitizenPushCategory.MESSAGE
                }
            try {
                notifyMessage(
                    personId = r.personId,
                    threadId = r.threadId,
                    category = category,
                    senderName = r.senderName,
                    threadTitle = r.threadTitle,
                    messageContent = r.messageContent,
                    language = CitizenPushLanguage.fromPersonLanguage(r.language),
                    replyRecipientAccountIds = r.replyRecipientAccountIds,
                    jwtProvider =
                        VapidJwtProvider { uri ->
                            db.transaction { tx -> webPush.getValidToken(tx, clock, uri) }
                        },
                )
            } catch (e: Exception) {
                logger.warn(e) { "Citizen push: failed for ${r.personId}, continuing" }
            }
        }
    }
}

data class CitizenPushRecipientRow(
    val personId: PersonId,
    val threadId: MessageThreadId,
    val language: String?,
    val urgent: Boolean,
    val threadType: MessageType,
    val senderName: String,
    val threadTitle: String,
    val messageContent: String,
    val replyRecipientAccountIds: List<MessageAccountId>,
)

fun Database.Read.getCitizenPushRecipients(
    messageIds: List<MessageId>
): List<CitizenPushRecipientRow> =
    createQuery {
            sql(
                """
SELECT DISTINCT
    p.id AS person_id,
    m.thread_id,
    lower(p.language) AS language,
    t.urgent,
    t.message_type AS thread_type,
    m.sender_name,
    t.title AS thread_title,
    mc.content AS message_content,
    COALESCE(reply_participants.account_ids, ARRAY[]::uuid[]) AS reply_recipient_account_ids
FROM message m
JOIN message_content mc ON mc.id = m.content_id
JOIN message_recipients mr ON mr.message_id = m.id
JOIN message_account ma ON ma.id = mr.recipient_id
JOIN person p ON p.id = ma.person_id
JOIN message_thread t ON m.thread_id = t.id
LEFT JOIN LATERAL (
    SELECT array_agg(DISTINCT account_id) AS account_ids
    FROM (
        SELECT m2.sender_id AS account_id
        FROM message m2
        WHERE m2.thread_id = t.id
          AND m2.sent_at IS NOT NULL
          AND m2.sender_id <> ma.id
        UNION
        SELECT mr2.recipient_id AS account_id
        FROM message_recipients mr2
        JOIN message m2 ON m2.id = mr2.message_id
        WHERE m2.thread_id = t.id
          AND m2.sent_at IS NOT NULL
          AND mr2.recipient_id <> ma.id
    ) _p
) reply_participants ON TRUE
WHERE m.id = ANY(${bind(messageIds)})
  AND t.is_copy IS FALSE
"""
            )
        }
        .toList<CitizenPushRecipientRow>()
