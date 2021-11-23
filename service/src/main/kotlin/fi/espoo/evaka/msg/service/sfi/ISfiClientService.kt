// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.service.sfi

import java.time.LocalDate

interface ISfiClientService {

    data class MessageMetadata(
        val message: MessageDetails
    )

    data class Recipient(
        val ssn: String,
        val name: String,
        val email: String?,
        val phone: String?,
        val address: RecipientAddress
    )

    data class RecipientAddress(
        val streetAddress: String,
        val postalCode: String,
        val postOffice: String,
        val countryCode: String = "FI"
    )

    data class MessageDetails(
        val messageId: String?,
        val uniqueCaseIdentifier: String,
        val header: String,
        val content: String,
        val sendDate: LocalDate,
        val senderName: String,
        val recipient: Recipient,
        val pdfDetails: PdfDetails,
        val emailNotification: EmailNotification?
    )

    data class EmailNotification(
        val header: String,
        val message: String
    )

    data class PdfDetails(
        val fileDescription: String,
        val fileType: String = "application/pdf",
        val fileName: String,
        val content: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as PdfDetails

            if (fileDescription != other.fileDescription) return false
            if (fileType != other.fileType) return false
            if (fileName != other.fileName) return false
            if (!content.contentEquals(other.content)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = fileDescription.hashCode()
            result = 31 * result + fileType.hashCode()
            result = 31 * result + fileName.hashCode()
            result = 31 * result + content.contentHashCode()
            return result
        }
    }

    fun sendMessage(metadata: MessageMetadata): SfiResponse
}
