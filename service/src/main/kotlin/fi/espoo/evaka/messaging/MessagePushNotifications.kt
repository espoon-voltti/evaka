// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.MessageId
import fi.espoo.evaka.shared.MessageRecipientId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.QuerySql
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.webpush.WebPush
import fi.espoo.evaka.webpush.WebPushCrypto
import fi.espoo.evaka.webpush.WebPushEndpoint
import fi.espoo.evaka.webpush.WebPushNotification
import fi.espoo.evaka.webpush.WebPushPayload
import fi.espoo.evaka.webpush.deletePushSubscription
import fi.espoo.voltti.logging.loggers.info
import java.time.Duration
import mu.KotlinLogging
import org.springframework.stereotype.Service

@Service
class MessagePushNotifications(
    private val webPush: WebPush?,
    private val accessControl: AccessControl,
    asyncJobRunner: AsyncJobRunner<AsyncJob>
) {
    init {
        asyncJobRunner.registerHandler { db, clock, job: AsyncJob.SendMessagePushNotification ->
            send(db, clock, job.recipient, job.device)
        }
    }

    private val logger = KotlinLogging.logger {}

    private fun getPendingPushNotifications() =
        QuerySql.of {
            sql(
                """
SELECT mr.message_id AS message, mr.id AS recipient, md.id AS device, dg.id AS group_id, dg.name AS group_name
FROM message_recipients mr
JOIN message_account ma ON mr.recipient_id = ma.id
JOIN daycare_group dg ON ma.daycare_group_id = dg.id
JOIN daycare d ON d.id = dg.daycare_id
JOIN mobile_device_push_group mdpg ON mdpg.daycare_group = dg.id
JOIN mobile_device md ON mdpg.device = md.id
WHERE mr.read_at IS NULL
AND ma.type = 'GROUP'
AND 'PUSH_NOTIFICATIONS' = ANY(d.enabled_pilot_features)
AND 'RECEIVED_MESSAGE' = ANY(md.push_notification_categories)
AND EXISTS (
    SELECT FROM mobile_device_push_subscription mdps
    WHERE mdps.device = md.id
)
AND CASE
    WHEN md.employee_id IS NULL THEN TRUE
    ELSE (
        EXISTS (
            SELECT FROM daycare_group_acl dgacl
            WHERE dgacl.daycare_group_id = dg.id AND dgacl.employee_id = md.employee_id
        ) OR
        EXISTS (
            SELECT FROM daycare_acl dacl
            WHERE dacl.daycare_id = dg.daycare_id AND dacl.employee_id = md.employee_id
        )
    )
END
"""
            )
        }

    fun getAsyncJobs(
        tx: Database.Read,
        messages: Collection<MessageId>
    ): List<AsyncJob.SendMessagePushNotification> =
        tx.createQuery {
                sql(
                    """
SELECT recipient, device
FROM (${subquery(getPendingPushNotifications())}) notification
WHERE notification.message = ANY(${bind(messages)})
"""
                )
            }
            .toList<AsyncJob.SendMessagePushNotification>()

    data class GroupNotification(
        val groupId: GroupId,
        val groupName: String,
        val endpoint: WebPushEndpoint
    )

    private fun Database.Read.getNotification(
        messageRecipient: MessageRecipientId,
        device: MobileDeviceId
    ): GroupNotification? =
        createQuery {
                sql(
                    """
SELECT group_id, group_name, mdps.endpoint, mdps.auth_secret, mdps.ecdh_key
FROM (${subquery(getPendingPushNotifications())}) notification
JOIN mobile_device_push_subscription mdps ON mdps.device = notification.device
WHERE notification.recipient = ${bind(messageRecipient)}
AND notification.device = ${bind(device)}
"""
                )
            }
            .exactlyOneOrNull {
                GroupNotification(
                    groupId = column("group_id"),
                    groupName = column("group_name"),
                    WebPushEndpoint(
                        uri = column("endpoint"),
                        ecdhPublicKey =
                            WebPushCrypto.decodePublicKey(column<ByteArray>("ecdh_key")),
                        authSecret = column("auth_secret")
                    )
                )
            }

    fun send(
        dbc: Database.Connection,
        clock: EvakaClock,
        recipient: MessageRecipientId,
        device: MobileDeviceId
    ) {
        if (webPush == null) return

        val (vapidJwt, notification) =
            dbc.transaction { tx ->
                tx.getNotification(recipient, device)
                    ?.takeIf {
                        accessControl.hasPermissionFor(
                            tx,
                            AuthenticatedUser.MobileDevice(device),
                            clock,
                            Action.Group.RECEIVE_PUSH_NOTIFICATIONS,
                            it.groupId
                        )
                    }
                    ?.let { Pair(webPush.getValidToken(tx, clock, it.endpoint.uri), it) }
            } ?: return
        dbc.close()

        logger.info(mapOf("endpoint" to notification.endpoint.uri)) {
            "Sending push notification to $device"
        }
        try {
            webPush.send(
                vapidJwt,
                WebPushNotification(
                    notification.endpoint,
                    ttl = Duration.ofDays(1),
                    payloads =
                        listOf(
                            WebPushPayload.NotificationV1(
                                title = "Uusi viesti ryhmälle ${notification.groupName}"
                            )
                        )
                )
            )
        } catch (e: WebPush.SubscriptionExpired) {
            logger.warn(
                "Subscription expired for device $device (HTTP status ${e.status}) -> deleting"
            )
            dbc.transaction { it.deletePushSubscription(device) }
        }
    }
}
