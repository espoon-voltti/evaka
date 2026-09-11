// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.webpush

import evaka.core.EvakaEnv
import evaka.core.pis.NotificationCategory
import evaka.core.shared.CitizenPushSubscriptionId
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.UiLanguage
import fi.espoo.voltti.logging.loggers.info
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Duration
import org.springframework.stereotype.Service

@Service
class CitizenPushNotifications(private val webPush: WebPush?, private val env: EvakaEnv) {
    private val logger = KotlinLogging.logger {}

    /**
     * Sends one notification to a citizen's browser.
     *
     * A [category] the citizen has switched off stops the send. A test notification passes null,
     * because the citizen asked for it whatever their settings say.
     */
    fun send(
        dbc: Database.Connection,
        clock: EvakaClock,
        subscription: CitizenPushSubscriptionId,
        category: NotificationCategory?,
        language: UiLanguage,
        content: PushNotificationContent,
        path: String,
        tag: String,
        ttl: Duration,
    ) {
        if (webPush == null) return

        val (vapidJwt, endpoint) =
            dbc.transaction { tx ->
                tx.getCitizenPushTarget(subscription)
                    ?.takeIf { category == null || category !in it.disabledCategories }
                    ?.let { Pair(webPush.getValidToken(tx, clock, it.endpoint.uri), it.endpoint) }
            } ?: return
        dbc.close()

        logger.info(mapOf("endpoint" to endpoint.uri)) {
            "Sending push notification to citizen subscription $subscription"
        }
        val message =
            WebPushMessage.Declarative(
                DeclarativeNotification(
                    title = content.title,
                    navigate = frontendBaseUrl(language) + path,
                    body = content.body,
                    tag = tag,
                )
            )
        try {
            webPush.send(vapidJwt, WebPushNotification(endpoint, ttl, message))
            dbc.transaction { it.markCitizenPushSent(subscription, clock.now()) }
        } catch (e: WebPush.SubscriptionExpired) {
            logger.warn {
                "Subscription $subscription expired (HTTP status ${e.status}) -> deleting"
            }
            dbc.transaction { it.deleteCitizenPushSubscription(subscription) }
        }
    }

    private fun frontendBaseUrl(language: UiLanguage) =
        when (language) {
            UiLanguage.SV -> env.frontendBaseUrlSv
            else -> env.frontendBaseUrlFi
        }
}
