// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.videocall

import fi.espoo.evaka.citizenwebpush.CitizenPushSubscriptionEntry
import fi.espoo.evaka.citizenwebpush.CitizenPushSubscriptionStore
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.webpush.WebPush
import fi.espoo.evaka.webpush.WebPushCrypto
import fi.espoo.evaka.webpush.WebPushEndpoint
import fi.espoo.evaka.webpush.WebPushNotification
import fi.espoo.evaka.webpush.WebPushPayload
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Duration
import java.util.UUID
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * Video call invite push notifications. Bypasses the per-category opt-in filter: any guardian with
 * an active push subscription rings, which matches user expectations for a phone-like call.
 */
@Component
class VideoCallPushSender(
    private val store: CitizenPushSubscriptionStore,
    private val webPush: WebPush?,
) {
    fun notifyIncomingCall(
        db: Database,
        clock: EvakaClock,
        personId: PersonId,
        roomId: UUID,
        employeeName: String,
        childName: String,
    ) {
        val wp = webPush ?: return
        val file = store.load(personId) ?: return
        if (file.subscriptions.isEmpty()) return
        val payload =
            WebPushPayload.NotificationV1(
                title = "Videopuhelu: $employeeName",
                body = "Haluaa soittaa lapsestanne $childName",
                tag = "video-call-$roomId",
                url = "/video-call/$roomId",
                iconPath = "/citizen/notifications/welcome.png",
            )
        for (entry in file.subscriptions) {
            sendOne(personId, entry, payload) { uri ->
                db.connect { dbc -> dbc.transaction { tx -> wp.getValidToken(tx, clock, uri) } }
            } ?: continue
        }
    }

    private fun sendOne(
        personId: PersonId,
        entry: CitizenPushSubscriptionEntry,
        payload: WebPushPayload.NotificationV1,
        jwtProvider: (java.net.URI) -> fi.espoo.evaka.webpush.VapidJwt,
    ): Unit? {
        val wp = webPush ?: return null
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
                return null
            }

        val notification =
            WebPushNotification(
                endpoint = endpoint,
                ttl = Duration.ofMinutes(2),
                payloads = listOf(payload),
            )

        try {
            val jwt = jwtProvider(endpoint.uri)
            wp.send(jwt, notification)
        } catch (e: WebPush.SubscriptionExpired) {
            logger.warn { "Citizen push subscription expired (${e.status}); removing" }
            store.removeSubscription(personId, entry.endpoint)
        } catch (e: Exception) {
            logger.warn(e) { "Video-call push send failed; swallowing" }
        }
        return Unit
    }
}
