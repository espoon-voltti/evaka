// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.emailclient

import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class MockEmailClient : IEmailClient {
    companion object {
        val emails = mutableListOf<MockEmail>()

        fun getEmail(toAddress: String): MockEmail? {
            return emails.find { email -> email.toAddress === toAddress }
        }
    }

    override fun sendEmail(
        traceId: String,
        toAddress: String,
        fromAddress: String,
        subject: String,
        htmlBody: String,
        textBody: String
    ) {
        if (validateToAddress(traceId, toAddress)) {
            logger.info { "Mock sending application email (personId: $traceId toAddress: $toAddress)" }
            emails.add(MockEmail(traceId, toAddress, fromAddress, subject, htmlBody, textBody))
        }
    }
}

data class MockEmail(
    val traceId: String,
    val toAddress: String,
    val fromAddress: String,
    val subject: String,
    val htmlBody: String,
    val textBody: String
)
