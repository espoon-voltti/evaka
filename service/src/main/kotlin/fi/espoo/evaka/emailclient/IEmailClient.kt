// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.emailclient

private val EMAIL_PATTERN = "^([\\w.%+-]+)@([\\w-]+\\.)+([\\w]{2,})\$".toRegex()

interface IEmailClient {
    fun sendEmail(traceId: String, toAddress: String, fromAddress: String, subject: String, htmlBody: String, textBody: String)

    fun validateToAddress(traceId: String, toAddress: String): Boolean {
        if (toAddress.matches(EMAIL_PATTERN)) {
            return true
        }

        return false
    }
}
