// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.EvakaEnv
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.emailclient.IEmailClient
import fi.espoo.evaka.shared.MessageId
import fi.espoo.evaka.shared.MessageThreadId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.springframework.stereotype.Service

@Service
class MessageNotificationEmailService(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val emailClient: IEmailClient,
    env: EvakaEnv,
    emailEnv: EmailEnv
) {
    init {
        asyncJobRunner.registerHandler(::sendMessageNotification)
    }

    val baseUrl: String = env.frontendBaseUrlFi
    val baseUrlSv: String = env.frontendBaseUrlSv
    val senderAddress: String = emailEnv.senderAddress
    val senderNameFi: String = emailEnv.senderNameFi
    val senderNameSv: String = emailEnv.senderNameSv

    fun getFromAddress(language: Language) = when (language) {
        Language.sv -> "$senderNameSv <$senderAddress>"
        else -> "$senderNameFi <$senderAddress>"
    }

    fun getMessageNotifications(tx: Database.Transaction, messageId: MessageId): List<AsyncJob.SendMessageNotificationEmail> {
        return tx.createQuery(
            """
            SELECT DISTINCT
                m.thread_id,
                mr.id as message_recipient_id,
                p.id as person_id,
                p.email as person_email,
                coalesce(lower(p.language), 'fi') as language
            FROM message m
            JOIN message_recipients mr ON mr.message_id = m.id
            JOIN message_account ma ON ma.id = mr.recipient_id 
            JOIN person p ON p.id = ma.person_id
            WHERE m.id = :messageId
              AND mr.read_at IS NULL
              AND mr.notification_sent_at IS NULL
              AND p.email IS NOT NULL
            """.trimIndent()
        )
            .bind("messageId", messageId)
            .map { row ->
                AsyncJob.SendMessageNotificationEmail(
                    threadId = row.mapColumn("thread_id"),
                    messageRecipientId = row.mapColumn("message_recipient_id"),
                    personEmail = row.mapColumn("person_email"),
                    language = getLanguage(row.mapColumn("language"))
                )
            }
            .toList()
    }

    fun scheduleSendingMessageNotifications(tx: Database.Transaction, messageId: MessageId) {
        asyncJobRunner.plan(
            tx,
            payloads = getMessageNotifications(tx, messageId),
            runAt = HelsinkiDateTime.now(),
            retryCount = 10
        )
    }

    private fun getLanguage(languageStr: String?): Language {
        return when (languageStr?.lowercase()) {
            "sv" -> Language.sv
            "en" -> Language.en
            else -> Language.fi
        }
    }

    fun sendMessageNotification(db: Database.Connection, msg: AsyncJob.SendMessageNotificationEmail) {
        val (threadId, messageRecipientId, personEmail, language) = msg

        db.transaction { tx ->
            emailClient.sendEmail(
                traceId = messageRecipientId.toString(),
                toAddress = personEmail,
                fromAddress = getFromAddress(language),
                subject = getSubject(),
                htmlBody = getHtml(language, threadId),
                textBody = getText(language, threadId)
            )
            tx.markNotificationAsSent(messageRecipientId)
        }
    }

    private fun getSubject(): String {
        val postfix = if (System.getenv("VOLTTI_ENV") == "prod") "" else " [${System.getenv("VOLTTI_ENV")}]"

        return "Uusi viesti eVakassa / Nytt meddelande i eVaka / New message in eVaka$postfix"
    }

    private fun getCitizenMessageUrl(lang: Language, threadId: MessageThreadId?): String {
        val base = when (lang) {
            Language.sv -> baseUrlSv
            else -> baseUrl
        }
        return "$base/messages${if (threadId == null) "" else "/$threadId"}"
    }

    private fun getHtml(language: Language, threadId: MessageThreadId?): String {
        val messagesUrl = getCitizenMessageUrl(language, threadId)
        return """
                <p>Sinulle on saapunut uusi tiedote/viesti eVakaan. Lue viesti täältä: <a href="$messagesUrl">$messagesUrl</a></p>
                <p>Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.</p>
            
                <hr>
                
                <p>Du har fått ett nytt allmänt/personligt meddelande i eVaka. Läs meddelandet här: <a href="$messagesUrl">$messagesUrl</a></p>
                <p>Detta besked skickas automatiskt av eVaka. Svara inte på detta besked.</p>          
                
                <hr>
                
                <p>You have received a new eVaka bulletin/message. Read the message here: <a href="$messagesUrl">$messagesUrl</a></p>
                <p>This is an automatic message from the eVaka system. Do not reply to this message.</p>       
        """.trimIndent()
    }

    private fun getText(language: Language, threadId: MessageThreadId?): String {
        val messageUrl = getCitizenMessageUrl(language, threadId)
        return """
                Sinulle on saapunut uusi tiedote/viesti eVakaan. Lue viesti täältä: $messageUrl
                
                Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.
                
                -----
       
                Du har fått ett nytt allmänt/personligt meddelande i eVaka. Läs meddelandet här: $messageUrl
                
                Detta besked skickas automatiskt av eVaka. Svara inte på detta besked. 
                
                -----
                
                You have received a new eVaka bulletin/message. Read the message here: $messageUrl
                
                This is an automatic message from the eVaka system. Do not reply to this message.  
        """.trimIndent()
    }
}
