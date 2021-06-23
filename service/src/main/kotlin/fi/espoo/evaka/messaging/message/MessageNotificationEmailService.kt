// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging.message

import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.emailclient.IEmailClient
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.SendMessageNotificationEmail
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class MessageNotificationEmailService(
    private val asyncJobRunner: AsyncJobRunner,
    private val emailClient: IEmailClient,
    env: Environment
) {
    init {
        asyncJobRunner.sendMessageNotificationEmail = ::sendMessageNotification
    }

    val baseUrl: String = env.getRequiredProperty("application.frontend.baseurl")
    val baseUrlSv: String = env.getRequiredProperty("application.frontend.baseurl.sv")
    val senderAddress: String = env.getRequiredProperty("fi.espoo.evaka.email.reply_to_address")
    val senderNameFi: String = env.getRequiredProperty("fi.espoo.evaka.email.sender_name.fi")
    val senderNameSv: String = env.getRequiredProperty("fi.espoo.evaka.email.sender_name.sv")

    fun getFromAddress(language: Language) = when (language) {
        Language.sv -> "$senderNameSv <$senderAddress>"
        else -> "$senderNameFi <$senderAddress>"
    }

    fun getMessageNotifications(tx: Database.Transaction, messageId: UUID): List<SendMessageNotificationEmail> {
        return tx.createQuery(
            """
            SELECT DISTINCT
                mr.id as message_recipient_id,
                p.id as person_id,
                p.email as person_email,
                coalesce(lower(p.language), 'fi') as language
            FROM message_recipients mr
            JOIN message_account ma ON ma.id = mr.recipient_id 
            JOIN person p ON p.id = ma.person_id
            WHERE mr.message_id = :messageId
              AND mr.read_at IS NULL
              AND mr.notification_sent_at IS NULL
              AND p.email IS NOT NULL
            """.trimIndent()
        )
            .bind("messageId", messageId)
            .map { row ->
                SendMessageNotificationEmail(
                    messageRecipientId = row.mapColumn("message_recipient_id"),
                    personEmail = row.mapColumn("person_email"),
                    language = getLanguage(row.mapColumn("language"))
                )
            }
            .toList()
    }

    fun scheduleSendingMessageNotifications(tx: Database.Transaction, messageId: UUID) {
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

    fun sendMessageNotification(db: Database, msg: SendMessageNotificationEmail) {
        val (messageRecipientId, personEmail, language) = msg

        db.transaction { tx ->
            emailClient.sendEmail(
                traceId = messageRecipientId.toString(),
                toAddress = personEmail,
                fromAddress = getFromAddress(language),
                subject = getSubject(),
                htmlBody = getHtml(language),
                textBody = getText(language)
            )
            tx.markNotificationAsSent(messageRecipientId)
        }
    }

    private fun getSubject(): String {
        val postfix = if (System.getenv("VOLTTI_ENV") == "prod") "" else " [${System.getenv("VOLTTI_ENV")}]"

        return "Uusi viesti eVakassa / Ny meddelande i eVaka / New message in eVaka$postfix"
    }

    private fun getCitizenMessagesUrl(lang: Language): String {
        val base = when (lang) {
            Language.sv -> baseUrlSv
            else -> baseUrl
        }
        return "$base/messages"
    }

    private fun getHtml(language: Language): String {
        val messagesUrl = getCitizenMessagesUrl(language)
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

    private fun getText(language: Language): String {
        val messagesUrl = getCitizenMessagesUrl(language)
        return """
                Sinulle on saapunut uusi tiedote/viesti eVakaan. Lue viesti täältä: $messagesUrl
                
                Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.
                
                -----
       
                Du har fått ett nytt allmänt/personligt meddelande i eVaka. Läs meddelandet här: $messagesUrl
                
                Detta besked skickas automatiskt av eVaka. Svara inte på detta besked. 
                
                -----
                
                You have received a new eVaka bulletin/message. Read the message here: $messagesUrl
                
                This is an automatic message from the eVaka system. Do not reply to this message.  
        """.trimIndent()
    }
}
