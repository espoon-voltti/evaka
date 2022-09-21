// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vasu

import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.EvakaEnv
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.emailclient.IEmailClient
import fi.espoo.evaka.shared.VasuDocumentId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class VasuNotificationService(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val emailClient: IEmailClient,
    env: EvakaEnv,
    emailEnv: EmailEnv
) {
    init {
        asyncJobRunner.registerHandler(::sendVasuNotificationEmail)
    }

    val baseUrl: String = env.frontendBaseUrlFi
    val baseUrlSv: String = env.frontendBaseUrlSv
    val senderAddress: String = emailEnv.senderAddress
    val senderNameFi: String = emailEnv.senderNameFi
    val senderNameSv: String = emailEnv.senderNameSv

    fun scheduleEmailNotification(tx: Database.Transaction, id: VasuDocumentId) {
        logger.info { "Scheduling sending of vasu/leops notification emails (id: $id)" }
        asyncJobRunner.plan(
            tx,
            payloads = getVasuNotifications(tx, id),
            runAt = HelsinkiDateTime.now(),
            retryCount = 10
        )
    }

    private fun getFromAddress(language: Language) = when (language) {
        Language.sv -> "$senderNameSv <$senderAddress>"
        else -> "$senderNameFi <$senderAddress>"
    }

    private fun getVasuNotifications(
        tx: Database.Read,
        vasuDocumentId: VasuDocumentId
    ): List<AsyncJob.SendVasuNotificationEmail> {
        return tx.createQuery(
            """
SELECT 
    doc.id AS vasu_document_id,
    parent.email AS recipient_email,
    parent.language AS language
FROM curriculum_document doc
    JOIN person child ON doc.child_id = child.id
    JOIN guardian g ON doc.child_id = g.child_id
    JOIN person parent ON g.guardian_id = parent.id
WHERE
    doc.id = :id
            """.trimIndent()
        )
            .bind("id", vasuDocumentId)
            .mapTo<AsyncJob.SendVasuNotificationEmail>()
            .toList()
    }

    fun sendVasuNotificationEmail(db: Database.Connection, clock: EvakaClock, msg: AsyncJob.SendVasuNotificationEmail) {
        logger.info("Sending vasu/leops notification email for document ${msg.vasuDocumentId} to ${msg.recipientEmail}")

        emailClient.sendEmail(
            traceId = msg.vasuDocumentId.toString(),
            toAddress = msg.recipientEmail,
            fromAddress = getFromAddress(msg.language),
            subject = getSubject(),
            htmlBody = getHtml(msg.language),
            textBody = getText(msg.language)
        )
    }

    private fun getSubject(): String {
        val postfix = if (System.getenv("VOLTTI_ENV") == "prod") "" else " [${System.getenv("VOLTTI_ENV")}]"
        return "Uusi dokumentti eVakassa / Nytt dokument i eVaka / New document in eVaka$postfix"
    }

    private fun getDocumentsUrl(lang: Language): String {
        val base = when (lang) {
            Language.sv -> baseUrlSv
            else -> baseUrl
        }
        return "$base/vasu"
    }

    private fun getHtml(language: Language): String {
        val documentsUrl = getDocumentsUrl(language)
        return """
                <p>Sinulle on saapunut uusi dokumentti eVakaan. Lue dokumentti täältä: <a href="$documentsUrl">$documentsUrl</a></p>
                <p>Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.</p>
            
                <hr>
                
                <p>Du har fått ett nytt dokument i eVaka. Läs dokumentet här: <a href="$documentsUrl">$documentsUrl</a></p>
                <p>Detta besked skickas automatiskt av eVaka. Svara inte på detta besked.</p>          
                
                <hr>
                
                <p>You have received a new eVaka document. Read the document here: <a href="$documentsUrl">$documentsUrl</a></p>
                <p>This is an automatic message from the eVaka system. Do not reply to this message.</p>       
        """.trimIndent()
    }

    private fun getText(language: Language): String {
        val documentsUrl = getDocumentsUrl(language)
        return """
                Sinulle on saapunut uusi dokumentti eVakaan. Lue dokumentti täältä: $documentsUrl
                
                Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.
                
                -----
       
                Du har fått ett nytt dokument i eVaka. Läs dokumentet här: $documentsUrl
                
                Detta besked skickas automatiskt av eVaka. Svara inte på detta besked. 
                
                -----
                
                You have received a new eVaka document. Read the document here: $documentsUrl
                
                This is an automatic message from the eVaka system. Do not reply to this message.  
        """.trimIndent()
    }
}
