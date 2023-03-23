// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.shared.MessageId
import fi.espoo.evaka.shared.MessageRecipientId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.webpush.WebPush
import fi.espoo.evaka.webpush.WebPushCrypto
import fi.espoo.evaka.webpush.WebPushEndpoint
import fi.espoo.evaka.webpush.WebPushNotification
import fi.espoo.evaka.webpush.WebPushPayload
import fi.espoo.evaka.webpush.deletePushSubscription
import fi.espoo.voltti.logging.loggers.info
import java.time.Duration
import org.springframework.stereotype.Service

@Service
class MessagePushNotifications(
    private val webPush: WebPush?,
    asyncJobRunner: AsyncJobRunner<AsyncJob>
) {
    init {
        asyncJobRunner.registerHandler { db, clock, job: AsyncJob.SendMessagePushNotification ->
            db.transaction { tx -> send(tx, clock, job.recipient, job.device) }
        }
    }

    fun getAsyncJobs(
        tx: Database.Read,
        messages: Collection<MessageId>
    ): List<AsyncJob.SendMessagePushNotification> =
        tx.createQuery<Any> {
                sql(
                    """
SELECT mr.id AS recipient, mdps.device AS device
FROM message_recipients mr
JOIN message_account ma ON mr.recipient_id = ma.id
JOIN daycare_group dg ON ma.daycare_group_id = dg.id
JOIN mobile_device md ON dg.daycare_id = md.unit_id
JOIN mobile_device_push_subscription mdps ON md.id = mdps.device
WHERE mr.message_id = ANY(${bind(messages)})
AND mr.read_at IS NULL
AND ma.type = 'GROUP'
            """
                )
            }
            .mapTo<AsyncJob.SendMessagePushNotification>()
            .toList()

    data class GroupNotification(val groupName: String, val endpoint: WebPushEndpoint)

    private fun Database.Read.getNotification(
        messageRecipient: MessageRecipientId,
        device: MobileDeviceId
    ): GroupNotification? =
        createQuery<Any> {
                sql(
                    """
SELECT dg.name AS group_name, mdps.endpoint, mdps.auth_secret, mdps.ecdh_key
FROM message_recipients mr
JOIN message_account ma ON mr.recipient_id = ma.id
JOIN daycare_group dg ON ma.daycare_group_id = dg.id
JOIN mobile_device md ON dg.daycare_id = md.unit_id
JOIN mobile_device_push_subscription mdps ON md.id = mdps.device
WHERE mr.id = ${bind(messageRecipient)}
AND md.id = ${bind(device)}
AND mr.read_at IS NULL
AND ma.type = 'GROUP'
       """
                )
            }
            .map { row ->
                GroupNotification(
                    groupName = row.mapColumn("group_name"),
                    WebPushEndpoint(
                        uri = row.mapColumn("endpoint"),
                        ecdhPublicKey =
                            WebPushCrypto.decodePublicKey(row.mapColumn<ByteArray>("ecdh_key")),
                        authSecret = row.mapColumn("auth_secret")
                    )
                )
            }
            .singleOrNull()

    fun send(
        tx: Database.Transaction,
        clock: EvakaClock,
        recipient: MessageRecipientId,
        device: MobileDeviceId
    ) {
        val notification = tx.getNotification(recipient, device) ?: return
        if (webPush != null) {
            logger.info(mapOf("endpoint" to notification.endpoint.uri)) {
                "Sending push notification to $device"
            }
            try {
                webPush.send(
                    clock,
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
                logger.error("Subscription expired for device $device -> deleting", e)
                tx.deletePushSubscription(device)
            }
        }
    }
}
