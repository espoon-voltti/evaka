// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.sficlient

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

interface SfiMessagesClient {
    fun send(msg: SfiMessage)
}

@JsonIgnoreProperties(value = ["language"]) // ignore legacy properties
data class SfiMessage(
    val messageId: String,
    val documentId: String, // This is sent to suomi.fi as reference id (UUID)
    val documentBucket: String,
    val documentKey: String,
    val documentDisplayName: String, // Document name is shown to end user
    val ssn: String,
    val firstName: String,
    val lastName: String,
    val streetAddress: String,
    val postalCode: String,
    val postOffice: String,
    val countryCode: String = "FI",
    val messageHeader: String,
    val messageContent: String,
    val emailHeader: String? = null,
    val emailContent: String? = null,
)
