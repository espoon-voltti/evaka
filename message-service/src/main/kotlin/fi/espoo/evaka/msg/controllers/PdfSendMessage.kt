// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.controllers

data class PdfSendMessage(
    val messageId: String?, // FIXME: nullable only to support legacy clients
    val documentUri: String, // Uri to S3 document: s3://<bucket name>/<document key>
    val documentId: String, // This is sent to suomi.fi as reference id (UUID)
    val documentDisplayName: String, // Document name is shown to end user
    val ssn: String,
    val firstName: String,
    val lastName: String,
    val language: String = "fi",
    val streetAddress: String,
    val postalCode: String,
    val postOffice: String,
    val countryCode: String = "FI",
    val messageHeader: String,
    val messageContent: String,
    val emailHeader: String?,
    val emailContent: String?
) {
    override fun toString(): String {
        return "PdfSendMessage(documentUri='$documentUri', documentId='$documentId')"
    }
}
