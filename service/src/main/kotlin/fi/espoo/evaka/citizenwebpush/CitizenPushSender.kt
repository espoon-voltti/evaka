// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.citizenwebpush

import fi.espoo.evaka.shared.PersonId
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
 * Caller-supplied strategy for building a VAPID JWT for a given endpoint.
 * Production wiring passes `webPush.getValidToken(tx, clock, uri)` inside a short DB transaction.
 * Tests supply a stub that returns a mock VapidJwt without needing a DB.
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
        threadId: String,
        category: CitizenPushCategory,
        senderName: String,
        language: CitizenPushLanguage,
        jwtProvider: VapidJwtProvider = defaultJwtProviderOrNoop(),
    ) {
        if (webPush == null) return
        val file = store.load(personId) ?: return
        val entries = file.subscriptions.filter { category in it.enabledCategories }
        if (entries.isEmpty()) return

        val titleAndBody = CitizenPushMessages.forMessage(category, language, senderName)
        val payload =
            WebPushPayload.NotificationV1(
                title = titleAndBody.title,
                body = titleAndBody.body,
                tag = "msg-$threadId",
                url = "/messages/$threadId",
            )

        entries.forEach { entry -> sendOne(personId, entry, payload, jwtProvider) }
    }

    fun sendTest(
        personId: PersonId,
        language: CitizenPushLanguage,
        jwtProvider: VapidJwtProvider = defaultJwtProviderOrNoop(),
    ) {
        if (webPush == null) return
        val file = store.load(personId) ?: return
        val titleAndBody = CitizenPushMessages.forTest(language)
        val payload =
            WebPushPayload.NotificationV1(
                title = titleAndBody.title,
                body = titleAndBody.body,
                tag = "welcome",
                url = "/personal-details",
            )
        file.subscriptions.forEach { entry -> sendOne(personId, entry, payload, jwtProvider) }
    }

    private fun sendOne(
        personId: PersonId,
        entry: CitizenPushSubscriptionEntry,
        payload: WebPushPayload.NotificationV1,
        jwtProvider: VapidJwtProvider,
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
            webPush!!.send(vapidJwt, notification)
        } catch (e: WebPush.SubscriptionExpired) {
            logger.warn { "Citizen push subscription expired (${e.status}); removing" }
            store.removeSubscription(personId, entry.endpoint)
        } catch (e: Exception) {
            logger.warn(e) { "Citizen push send failed; swallowing" }
        }
    }

    private fun defaultJwtProviderOrNoop(): VapidJwtProvider =
        VapidJwtProvider { _ ->
            error(
                "No VapidJwtProvider supplied. Production callers must route through " +
                    "CitizenPushSender.notifyMessage(..., jwtProvider = { uri -> " +
                    "db.transaction { tx -> webPush.getValidToken(tx, clock, uri) } }).",
            )
        }

    /**
     * Production entry point called from MessageService.handleMarkMessageAsSent, OUTSIDE its main
     * transaction. Opens its own short read-only transaction to resolve per-recipient context,
     * then releases the connection before doing S3 / HTTP. Best-effort: any exception is caught
     * and logged, never re-thrown.
     */
    fun handleSentMessages(
        db: fi.espoo.evaka.shared.db.Database.Connection,
        clock: fi.espoo.evaka.shared.domain.EvakaClock,
        messageIds: List<fi.espoo.evaka.shared.MessageId>,
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
                    r.threadType == fi.espoo.evaka.messaging.MessageType.BULLETIN ->
                        CitizenPushCategory.BULLETIN
                    else -> CitizenPushCategory.MESSAGE
                }
            try {
                notifyMessage(
                    personId = r.personId,
                    threadId = r.threadId.toString(),
                    category = category,
                    senderName = r.senderName,
                    language = CitizenPushLanguage.fromPersonLanguage(r.language),
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
    val personId: fi.espoo.evaka.shared.PersonId,
    val threadId: fi.espoo.evaka.shared.MessageThreadId,
    val language: String?,
    val urgent: Boolean,
    val threadType: fi.espoo.evaka.messaging.MessageType,
    val senderName: String,
)

fun fi.espoo.evaka.shared.db.Database.Read.getCitizenPushRecipients(
    messageIds: List<fi.espoo.evaka.shared.MessageId>
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
    coalesce(sender_account.name, '') AS sender_name
FROM message m
JOIN message_recipients mr ON mr.message_id = m.id
JOIN message_account ma ON ma.id = mr.recipient_id
JOIN person p ON p.id = ma.person_id
JOIN message_thread t ON m.thread_id = t.id
LEFT JOIN message_account sender_account ON sender_account.id = m.sender_id
WHERE m.id = ANY(${bind(messageIds)})
"""
            )
        }
        .toList<CitizenPushRecipientRow>()
