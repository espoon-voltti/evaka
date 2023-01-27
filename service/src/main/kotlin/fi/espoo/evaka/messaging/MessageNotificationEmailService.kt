// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.emailclient.IEmailClient
import fi.espoo.evaka.emailclient.IEmailMessageProvider
import fi.espoo.evaka.shared.MessageId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.JobParams
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.Duration
import kotlin.random.Random
import org.springframework.stereotype.Service

@Service
class MessageNotificationEmailService(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val emailClient: IEmailClient,
    private val emailMessageProvider: IEmailMessageProvider,
    private val emailEnv: EmailEnv
) {
    init {
        asyncJobRunner.registerHandler(::sendMessageNotification)
    }

    fun getMessageNotifications(
        tx: Database.Transaction,
        messageIds: List<MessageId>
    ): List<AsyncJob.SendMessageNotificationEmail> {
        return tx.createQuery(
                """
            SELECT DISTINCT
                m.thread_id,
                m.id AS message_id,
                mr.id as message_recipient_id,
                mr.recipient_id,
                p.id as person_id,
                p.email as person_email,
                coalesce(lower(p.language), 'fi') as language,
                t.urgent
            FROM message m
            JOIN message_recipients mr ON mr.message_id = m.id
            JOIN message_account ma ON ma.id = mr.recipient_id 
            JOIN person p ON p.id = ma.person_id
            JOIN message_thread t ON m.thread_id = t.id
            WHERE m.id = ANY(:messageIds)
              AND mr.read_at IS NULL
              AND mr.notification_sent_at IS NULL
              AND p.email IS NOT NULL
            """
                    .trimIndent()
            )
            .bind("messageIds", messageIds)
            .map { row ->
                AsyncJob.SendMessageNotificationEmail(
                    threadId = row.mapColumn("thread_id"),
                    messageId = row.mapColumn("message_id"),
                    messageRecipientId = row.mapColumn("message_recipient_id"),
                    recipientId = row.mapColumn("recipient_id"),
                    personEmail = row.mapColumn("person_email"),
                    language = getLanguage(row.mapColumn("language")),
                    urgent = row.mapColumn("urgent")
                )
            }
            .toList()
    }

    fun scheduleSendingMessageNotifications(
        tx: Database.Transaction,
        messageIds: List<MessageId>,
        runAt: HelsinkiDateTime,
        spreadSeconds: Long = 0
    ) {
        val asyncJobs =
            getMessageNotifications(tx, messageIds).map { payload ->
                JobParams(
                    payload = payload,
                    retryCount = 10,
                    retryInterval = Duration.ofMinutes(5),
                    runAt =
                        if (spreadSeconds == 0L) runAt
                        else runAt.plusSeconds(Random.nextLong(spreadSeconds))
                )
            }

        asyncJobRunner.plan(tx, asyncJobs)
    }

    private fun getLanguage(languageStr: String?): Language {
        return when (languageStr?.lowercase()) {
            "sv" -> Language.sv
            "en" -> Language.en
            else -> Language.fi
        }
    }

    fun sendMessageNotification(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.SendMessageNotificationEmail
    ) {
        val (threadId, messageId, recipientId, messageRecipientId, personEmail, language, urgent) =
            msg

        db.transaction { tx ->
            // The message has been undone and the recipient should no longer get an email
            // notification
            if (
                messageId != null &&
                    recipientId != null &&
                    !tx.unreadMessageForRecipientExists(messageId, recipientId)
            ) {
                return@transaction
            }

            emailClient.sendEmail(
                traceId = messageRecipientId.toString(),
                toAddress = personEmail,
                fromAddress = emailEnv.sender(language),
                content = emailMessageProvider.messageNotification(language, threadId, urgent)
            )
            tx.markNotificationAsSent(messageRecipientId, clock.now())
        }
    }
}
