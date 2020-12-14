// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.service

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fi.espoo.evaka.msg.async.AsyncJobRunner
import fi.espoo.evaka.msg.controllers.PdfSendMessage
import fi.espoo.evaka.msg.service.attachments.DocumentService
import fi.espoo.evaka.msg.service.sfi.ISfiClientService
import fi.espoo.evaka.msg.service.sfi.SfiResponse.Mapper.createOkResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

private val recipient = ISfiClientService.Recipient(
    ssn = "101010-1010",
    name = "Jari Johannes Mikkonen",
    email = "jari.mikkonen@espoo.fi",
    phone = "0501234567",
    address = ISfiClientService.RecipientAddress(
        streetAddress = "Katutie 1 A 17",
        postalCode = "00230",
        postOffice = "Espoo",
        countryCode = "FI"
    )
)

@ExtendWith(MockitoExtension::class)
class AsyncJobsTest {
    @InjectMocks
    lateinit var asyncJobs: AsyncJobs

    @Mock
    lateinit var attachmentService: DocumentService

    @Mock
    lateinit var sfiClient: ISfiClientService

    @Mock
    lateinit var asyncJobRunner: AsyncJobRunner

    @Test
    fun `decision attachment is retrieved from document service`() {
        val documentId = "16989643-6bc1-4978-a996-10bd838c8229f"
        val path = "s3://evaka_clubdecision_prod/$documentId"

        val documentDisplayName = "Kerhopäätös Mikko Ilmari Mikkonen.pdf"
        val messageHeader = "Testiviestin otsikko"
        val messageContent = "Testiviestin sisältö"
        whenever(attachmentService.getDocument(path)).thenReturn("pdf".toByteArray())
        whenever(sfiClient.sendMessage(any())).thenReturn(createOkResponse())
        val details = getMessage(path, documentDisplayName, documentId, messageHeader, messageContent)
        asyncJobs.sendMessagePDF(details)

        verify(attachmentService).getDocument(path)

        argumentCaptor<ISfiClientService.MessageMetadata>().apply {
            verify(sfiClient).sendMessage(capture())

            val sent = firstValue
            assertThat(sent.message.pdfDetails.fileName).isEqualTo(documentDisplayName)
            assertThat(sent.message.recipient.ssn).isEqualTo("101010-1010")
            assertThat(sent.message.header).isEqualTo(messageHeader)
            assertThat(sent.message.content).isEqualTo(messageContent)
            assertThat(sent.message.uniqueCaseIdentifier).isEqualTo("16989643-6bc1-4978-a996-10bd838c8229f")
        }
    }

    private fun getMessage(
        documentUri: String,
        documentDisplayName: String,
        documentId: String,
        messageHeader: String,
        messageContent: String
    ) = PdfSendMessage(
        messageId = null,
        documentUri = documentUri,
        documentDisplayName = documentDisplayName,
        documentId = documentId,
        ssn = recipient.ssn,
        firstName = recipient.ssn,
        lastName = recipient.ssn,
        language = "fi",
        streetAddress = recipient.address.streetAddress,
        postalCode = recipient.address.postalCode,
        postOffice = recipient.address.postOffice,
        countryCode = recipient.address.countryCode,
        messageHeader = messageHeader,
        messageContent = messageContent,
        emailHeader = null,
        emailContent = null
    )

    @Test
    fun `file name postfix is removed from file name`() {
        val result = asyncJobs.stripFileExtension("Kerhopäätös Mikko Ilmari Mikkonen.pdf")
        assertThat(result).isEqualTo("Kerhopäätös Mikko Ilmari Mikkonen")
    }

    @Test
    fun `attempt to remove file name postfix when there is no postfix does not break`() {
        val result = asyncJobs.stripFileExtension("Kerhopäätös Mikko Ilmari Mikkonen")
        assertThat(result).isEqualTo("Kerhopäätös Mikko Ilmari Mikkonen")
    }

    @Test
    fun `attempt to remove file name postfix from emtpy string does not break`() {
        val result = asyncJobs.stripFileExtension("")
        assertThat(result).isEqualTo("")
    }
}
