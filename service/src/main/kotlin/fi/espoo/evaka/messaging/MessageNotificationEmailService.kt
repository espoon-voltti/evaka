// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.emailclient.Email
import fi.espoo.evaka.emailclient.EmailClient
import fi.espoo.evaka.emailclient.IEmailMessageProvider
import fi.espoo.evaka.emailclient.MessageThreadData
import fi.espoo.evaka.pis.EmailMessageType
import fi.espoo.evaka.shared.HtmlSafe
import fi.espoo.evaka.shared.MessageId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.Duration
import org.springframework.stereotype.Service

@Service
class MessageNotificationEmailService(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val emailClient: EmailClient,
    private val emailMessageProvider: IEmailMessageProvider,
    private val emailEnv: EmailEnv,
) {
    init {
        asyncJobRunner.registerHandler(::sendMessageNotification)
    }

    fun getMessageNotifications(
        tx: Database.Transaction,
        messageIds: List<MessageId>,
    ): List<AsyncJob.SendMessageNotificationEmail> {
        return tx.createQuery {
                sql(
                    """
SELECT DISTINCT
    m.thread_id,
    m.id AS message_id,
    mr.id as message_recipient_id,
    m.sender_id,
    mr.recipient_id,
    p.id as person_id,
    CASE 
        WHEN lower(p.language) = 'fi' THEN 'fi'
        WHEN lower(p.language) = 'sv' THEN 'sv'
        WHEN lower(p.language) = 'en' THEN 'en'
        ELSE 'fi'
    END language,   
    t.urgent
FROM message m
JOIN message_recipients mr ON mr.message_id = m.id
JOIN message_account ma ON ma.id = mr.recipient_id 
JOIN person p ON p.id = ma.person_id
JOIN message_thread t ON m.thread_id = t.id
WHERE m.id = ANY(${bind(messageIds)})
  AND mr.read_at IS NULL
  AND mr.email_notification_sent_at IS NULL
  AND p.email IS NOT NULL
"""
                )
            }
            .toList<AsyncJob.SendMessageNotificationEmail>()
    }

    fun scheduleSendingMessageNotifications(
        tx: Database.Transaction,
        messageIds: List<MessageId>,
        runAt: HelsinkiDateTime,
    ) {
        asyncJobRunner.plan(
            tx,
            getMessageNotifications(tx, messageIds),
            retryCount = 10,
            retryInterval = Duration.ofMinutes(5),
            runAt = runAt,
        )
    }

    fun sendMessageNotification(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.SendMessageNotificationEmail,
    ) {
        val thread =
            db.transaction { tx ->
                // The message has been undone and the recipient should no longer get an email
                // notification
                if (!tx.unreadMessageForRecipientExists(msg.messageId, msg.recipientId)) {
                    null
                } else {
                    tx.getMessageThreadStub(msg.threadId)
                }
            } ?: return

        val isSenderMunicipalAccount =
            db.transaction { tx -> tx.getMessageAccountType(msg.senderId) == AccountType.MUNICIPAL }

        Email.create(
                dbc = db,
                personId = msg.personId,
                emailType =
                    when (thread.type) {
                        MessageType.MESSAGE -> EmailMessageType.MESSAGE_NOTIFICATION
                        MessageType.BULLETIN -> EmailMessageType.BULLETIN_NOTIFICATION
                    },
                fromAddress = emailEnv.sender(msg.language),
                content =
                    emailMessageProvider.messageNotification(
                        msg.language,
                        MessageThreadData(
                            id = thread.id,
                            type = thread.type,
                            title = HtmlSafe(thread.title),
                            urgent = thread.urgent,
                            sensitive = thread.sensitive,
                            isCopy = thread.isCopy,
                        ),
                        isSenderMunicipalAccount,
                    ),
                traceId = msg.messageRecipientId.toString(),
            )
            ?.also {
                emailClient.send(it)
                db.transaction { tx ->
                    tx.markEmailNotificationAsSent(msg.messageRecipientId, clock.now())
                }
            }
    }
}
