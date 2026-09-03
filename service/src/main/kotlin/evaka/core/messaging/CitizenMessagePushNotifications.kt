// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.messaging

import evaka.core.pis.NotificationCategory
import evaka.core.shared.CitizenPushSubscriptionId
import evaka.core.shared.FeatureConfig
import evaka.core.shared.MessageAccountId
import evaka.core.shared.MessageId
import evaka.core.shared.MessageRecipientId
import evaka.core.shared.MessageThreadId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.UiLanguage
import evaka.core.webpush.CitizenPushNotifications
import evaka.core.webpush.MessagePushNotificationData
import evaka.core.webpush.PushNotificationMessageProvider
import java.time.Duration
import org.springframework.stereotype.Service

@Service
class CitizenMessagePushNotifications(
    private val pushNotifications: CitizenPushNotifications,
    private val messageProvider: PushNotificationMessageProvider,
    private val featureConfig: FeatureConfig,
    asyncJobRunner: AsyncJobRunner<AsyncJob>,
) {
    init {
        asyncJobRunner.registerHandler { db, clock, job: AsyncJob.SendCitizenMessagePushNotification
            ->
            send(db, clock, job.recipient, job.subscription)
        }
    }

    fun getAsyncJobs(
        tx: Database.Read,
        messages: Collection<MessageId>,
    ): List<AsyncJob.SendCitizenMessagePushNotification> =
        tx.createQuery {
                sql(
                    """
SELECT mr.id AS recipient, cps.id AS subscription
FROM message_recipients mr
JOIN message m ON mr.message_id = m.id
JOIN message_thread mt ON m.thread_id = mt.id
JOIN message_account ma ON mr.recipient_id = ma.id
JOIN citizen_push_subscription cps ON cps.person_id = ma.person_id
WHERE m.id = ANY(${bind(messages)})
AND mr.read_at IS NULL
AND m.content_deleted_at IS NULL
AND mt.is_copy IS FALSE
"""
                )
            }
            .toList<AsyncJob.SendCitizenMessagePushNotification>()

    private data class MessageNotification(
        val threadId: MessageThreadId,
        val type: MessageType,
        val title: String,
        val urgent: Boolean,
        val sensitive: Boolean,
        val senderId: MessageAccountId,
        val language: UiLanguage,
    )

    private fun Database.Read.getNotification(recipient: MessageRecipientId): MessageNotification? =
        createQuery {
            sql(
                """
SELECT
    m.thread_id,
    mt.message_type AS type,
    mt.title,
    mt.urgent,
    mt.sensitive,
    m.sender_id,
    coalesce(cu.preferred_ui_language, 'FI') AS language
FROM message_recipients mr
JOIN message m ON mr.message_id = m.id
JOIN message_thread mt ON m.thread_id = mt.id
JOIN message_account ma ON mr.recipient_id = ma.id
LEFT JOIN citizen_user cu ON cu.id = ma.person_id
WHERE mr.id = ${bind(recipient)}
AND mr.read_at IS NULL
AND m.content_deleted_at IS NULL
"""
            )
        }
        .exactlyOneOrNull()

    fun send(
        dbc: Database.Connection,
        clock: EvakaClock,
        recipient: MessageRecipientId,
        subscription: CitizenPushSubscriptionId,
    ) {
        val (notification, sender) =
            dbc.read { tx ->
                tx.getNotification(recipient)?.let { notification ->
                    Pair(
                        notification,
                        tx.getMessageAccount(
                            notification.senderId,
                            municipalAccountName = featureConfig.municipalMessageAccountName,
                            serviceWorkerAccountName =
                                featureConfig.serviceWorkerMessageAccountName,
                            financeAccountName = featureConfig.financeMessageAccountName,
                        ),
                    )
                }
            } ?: return

        val isSenderMunicipalAccount = sender.type == AccountType.MUNICIPAL
        val content =
            messageProvider.messageNotification(
                notification.language,
                MessagePushNotificationData(
                    type = notification.type,
                    urgent = notification.urgent,
                    sensitive = notification.sensitive,
                    senderName = sender.name,
                    title = notification.title,
                    isSenderMunicipalAccount = isSenderMunicipalAccount,
                ),
            )
        pushNotifications.send(
            dbc,
            clock,
            subscription,
            category =
                when (notification.type) {
                    MessageType.MESSAGE -> NotificationCategory.MESSAGE_NOTIFICATION
                    MessageType.BULLETIN ->
                        if (isSenderMunicipalAccount) NotificationCategory.BULLETIN_NOTIFICATION
                        else NotificationCategory.MESSAGE_NOTIFICATION
                },
            language = notification.language,
            content = content,
            path = "/messages/${notification.threadId}",
            tag = "message-${notification.threadId}",
            ttl = Duration.ofDays(1),
        )
    }
}
