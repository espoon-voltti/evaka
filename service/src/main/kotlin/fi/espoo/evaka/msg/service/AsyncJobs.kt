// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.service

import fi.espoo.evaka.msg.service.attachments.DocumentService
import fi.espoo.evaka.msg.service.sfi.ISfiClientService
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.SuomiFiAsyncJob
import fi.espoo.evaka.shared.message.SuomiFiMessage
import org.apache.commons.text.StringEscapeUtils.unescapeJava
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.concurrent.TimeUnit

@Service
class AsyncJobs(
    private val docService: DocumentService,
    private val sfiClient: ISfiClientService,
    asyncJobRunner: AsyncJobRunner<SuomiFiAsyncJob>
) {
    init {
        asyncJobRunner.registerHandler { _, payload: SuomiFiAsyncJob.SendMessage ->
            sendMessagePDF(payload.message)
            TimeUnit.SECONDS.sleep(1)
        }
    }

    fun sendMessagePDF(details: SuomiFiMessage) {
        val pdfBytes = docService.getDocument(details.documentBucket, details.documentKey)
        val messageDetails = toPdfMessageDetails(pdfBytes, details)
        sfiClient.sendMessage(ISfiClientService.MessageMetadata(messageDetails))
    }

    private fun toRecipient(details: SuomiFiMessage) =
        ISfiClientService.Recipient(
            ssn = details.ssn,
            name = details.lastName + " " + details.firstName,
            email = null,
            phone = null,
            address = ISfiClientService.RecipientAddress(
                streetAddress = details.streetAddress,
                postalCode = details.postalCode,
                postOffice = details.postOffice,
                countryCode = details.countryCode
            )
        )

    private fun toPdfMessageDetails(pdfBytes: ByteArray, details: SuomiFiMessage) =
        ISfiClientService.MessageDetails(
            messageId = details.messageId,
            uniqueCaseIdentifier = details.documentId,
            header = details.messageHeader,
            content = unescapeJava(details.messageContent),
            sendDate = LocalDate.now(),
            senderName = "",
            pdfDetails = toPdfDetails(pdfBytes, details.documentDisplayName),
            recipient = toRecipient(details),
            emailNotification = toEmailNotification(details.emailHeader, details.emailContent)
        )

    private fun toEmailNotification(emailHeader: String?, emailContent: String?) =
        if (emailHeader != null && emailContent != null) ISfiClientService.EmailNotification(
            emailHeader,
            emailContent
        ) else null

    private fun toPdfDetails(documentBytes: ByteArray, documentDisplayName: String) =
        ISfiClientService.PdfDetails(
            fileDescription = stripFileExtension(documentDisplayName),
            fileType = "application/pdf",
            fileName = documentDisplayName,
            content = documentBytes
        )

    fun stripFileExtension(fileName: String) = fileName.substringBefore(".")
}
