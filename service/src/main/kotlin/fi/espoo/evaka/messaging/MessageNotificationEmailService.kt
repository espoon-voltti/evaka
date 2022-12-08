// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.EvakaEnv
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.emailclient.IEmailClient
import fi.espoo.evaka.shared.BulletinId
import fi.espoo.evaka.shared.BulletinRecipientId
import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.MessageId
import fi.espoo.evaka.shared.MessageThreadId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.JobParams
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.Duration
import java.util.UUID
import kotlin.random.Random
import org.springframework.stereotype.Service

@Service
class MessageNotificationEmailService(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val emailClient: IEmailClient,
    env: EvakaEnv,
    private val emailEnv: EmailEnv
) {
    init {
        asyncJobRunner.registerHandler(::sendMessageNotification)
        asyncJobRunner.registerHandler(::sendBulletinNotification)
    }

    val baseUrl: String = env.frontendBaseUrlFi
    val baseUrlSv: String = env.frontendBaseUrlSv

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

    data class BulletinNotification(
        val bulletinId: BulletinId,
        val recipientId: MessageAccountId,
        val bulletinRecipientId: BulletinRecipientId,
        val personEmail: String,
        val language: Language,
        val urgent: Boolean = false
    )

    fun getBulletinNotification(
        tx: Database.Transaction,
        bulletinId: BulletinId,
        recipientId: MessageAccountId
    ): BulletinNotification? {
        return tx.createQuery(
                """
SELECT DISTINCT
    b.id AS bulletin_id,
    br.recipient_id,
    br.id as bulletin_recipient_id,
    p.id as person_id,
    p.email as person_email,
    coalesce(lower(p.language), 'fi') as language,
    b.urgent
FROM bulletin b
JOIN bulletin_recipients br ON br.bulletin_id = b.id
JOIN message_account ma ON ma.id = br.recipient_id
JOIN person p ON p.id = ma.person_id
WHERE b.id = :bulletinId AND br.recipient_id = :recipientId
  AND br.read_at IS NULL
  AND br.notification_sent_at IS NULL
  AND p.email IS NOT NULL
"""
            )
            .bind("bulletinId", bulletinId)
            .bind("recipientId", recipientId)
            .mapTo<BulletinNotification>()
            .firstOrNull()
    }

    fun scheduleSendingMessageNotifications(
        tx: Database.Transaction,
        messageIds: List<MessageId>,
        runAt: HelsinkiDateTime
    ) {
        val asyncJobs =
            getMessageNotifications(tx, messageIds).map { payload ->
                JobParams(
                    payload = payload,
                    retryCount = 10,
                    retryInterval = Duration.ofMinutes(5),
                    runAt = runAt
                )
            }

        asyncJobRunner.plan(tx, asyncJobs)
    }

    fun scheduleSendingBulletinNotifications(
        tx: Database.Transaction,
        bulletinId: BulletinId,
        recipientIds: List<MessageAccountId>,
        runAt: HelsinkiDateTime,
        spreadSeconds: Long = 0
    ) {
        val asyncJobs =
            recipientIds.map { recipientId ->
                JobParams(
                    payload = AsyncJob.SendBulletinNotificationEmail(bulletinId, recipientId),
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
                subject = getSubject(urgent),
                htmlBody = getMessageHtml(language, threadId, urgent),
                textBody = getMessageText(language, threadId, urgent)
            )
            tx.markMessageNotificationAsSent(messageRecipientId, clock.now())
        }
    }

    fun sendBulletinNotification(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.SendBulletinNotificationEmail
    ) {
        db.transaction { tx ->
            getBulletinNotification(tx, msg.bulletinId, msg.recipientId)?.let { notification ->
                emailClient.sendEmail(
                    traceId = notification.bulletinRecipientId.toString(),
                    toAddress = notification.personEmail,
                    fromAddress = emailEnv.sender(notification.language),
                    subject = getSubject(notification.urgent),
                    htmlBody =
                        getBulletinHtml(
                            notification.language,
                            notification.bulletinId,
                            notification.urgent
                        ),
                    textBody =
                        getBulletinText(
                            notification.language,
                            notification.bulletinId,
                            notification.urgent
                        )
                )
                tx.markBulletinNotificationAsSent(notification.bulletinRecipientId, clock.now())
            }
        }
    }

    private fun getSubject(urgent: Boolean): String {
        return if (urgent) {
            "Uusi kiireellinen viesti eVakassa / Nytt brådskande meddelande i eVaka / New urgent message in eVaka"
        } else {
            "Uusi viesti eVakassa / Nytt meddelande i eVaka / New message in eVaka"
        }
    }

    private fun getCitizenMessageUrl(lang: Language, id: UUID?): String {
        val base =
            when (lang) {
                Language.sv -> baseUrlSv
                else -> baseUrl
            }
        return "$base/messages${if (id == null) "" else "/$id"}"
    }

    private fun getMessageHtml(
        language: Language,
        threadId: MessageThreadId?,
        urgent: Boolean
    ): String {
        val messagesUrl = getCitizenMessageUrl(language, threadId?.raw)
        return """
                <p>Sinulle on saapunut uusi ${if (urgent) "kiireellinen " else ""}viesti eVakaan. Lue viesti ${if (urgent) "mahdollisimman pian " else ""}täältä: <a href="$messagesUrl">$messagesUrl</a></p>
                <p>Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.</p>
            
                <hr>
                
                <p>Du har fått ett nytt ${if (urgent) "brådskande " else ""}personligt meddelande i eVaka. Läs meddelandet ${if (urgent) "så snart som möjligt " else ""}här: <a href="$messagesUrl">$messagesUrl</a></p>
                <p>Detta besked skickas automatiskt av eVaka. Svara inte på detta besked.</p>          
                
                <hr>
                
                <p>You have received a new ${if (urgent) "urgent " else ""}eVaka message. Read the message ${if (urgent) "as soon as possible " else ""}here: <a href="$messagesUrl">$messagesUrl</a></p>
                <p>This is an automatic message from the eVaka system. Do not reply to this message.</p>       
        """
            .trimIndent()
    }

    private fun getMessageText(
        language: Language,
        threadId: MessageThreadId?,
        urgent: Boolean
    ): String {
        val messageUrl = getCitizenMessageUrl(language, threadId?.raw)
        return """
                Sinulle on saapunut uusi ${if (urgent) "kiireellinen " else ""}viesti eVakaan. Lue viesti ${if (urgent) "mahdollisimman pian " else ""}täältä: $messageUrl
                
                Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.
                
                -----
       
                Du har fått ett nytt ${if (urgent) "brådskande " else ""}personligt meddelande i eVaka. Läs meddelandet ${if (urgent) "så snart som möjligt " else ""}här: $messageUrl
                
                Detta besked skickas automatiskt av eVaka. Svara inte på detta besked. 
                
                -----
                
                You have received a new ${if (urgent) "urgent " else ""}eVaka message. Read the message ${if (urgent) "as soon as possible " else ""}here: $messageUrl
                
                This is an automatic message from the eVaka system. Do not reply to this message.  
        """
            .trimIndent()
    }

    private fun getBulletinHtml(
        language: Language,
        bulletinId: BulletinId,
        urgent: Boolean
    ): String {
        val messagesUrl = getCitizenMessageUrl(language, bulletinId.raw)
        return """
                <p>Sinulle on saapunut uusi ${if (urgent) "kiireellinen " else ""}tiedote eVakaan. Lue viesti ${if (urgent) "mahdollisimman pian " else ""}täältä: <a href="$messagesUrl">$messagesUrl</a></p>
                <p>Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.</p>

                <hr>

                <p>Du har fått ett nytt ${if (urgent) "brådskande " else ""}allmänt meddelande i eVaka. Läs meddelandet ${if (urgent) "så snart som möjligt " else ""}här: <a href="$messagesUrl">$messagesUrl</a></p>
                <p>Detta besked skickas automatiskt av eVaka. Svara inte på detta besked.</p>

                <hr>

                <p>You have received a new ${if (urgent) "urgent " else ""}eVaka bulletin. Read the message ${if (urgent) "as soon as possible " else ""}here: <a href="$messagesUrl">$messagesUrl</a></p>
                <p>This is an automatic message from the eVaka system. Do not reply to this message.</p>
        """
            .trimIndent()
    }

    private fun getBulletinText(
        language: Language,
        bulletinId: BulletinId,
        urgent: Boolean
    ): String {
        val messageUrl = getCitizenMessageUrl(language, bulletinId.raw)
        return """
                Sinulle on saapunut uusi ${if (urgent) "kiireellinen " else ""}tiedote eVakaan. Lue viesti ${if (urgent) "mahdollisimman pian " else ""}täältä: $messageUrl

                Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.

                -----

                Du har fått ett nytt ${if (urgent) "brådskande " else ""}allmänt meddelande i eVaka. Läs meddelandet ${if (urgent) "så snart som möjligt " else ""}här: $messageUrl

                Detta besked skickas automatiskt av eVaka. Svara inte på detta besked.

                -----

                You have received a new ${if (urgent) "urgent " else ""}eVaka bulletin. Read the message ${if (urgent) "as soon as possible " else ""}here: $messageUrl

                This is an automatic message from the eVaka system. Do not reply to this message.
        """
            .trimIndent()
    }
}
