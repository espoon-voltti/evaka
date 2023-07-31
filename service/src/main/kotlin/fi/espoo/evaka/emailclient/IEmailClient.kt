// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.emailclient

import mu.KotlinLogging

private val EMAIL_PATTERN = "^([\\w.%+-]+)@([\\w-]+\\.)+([\\w]{2,})\$".toRegex()

private val logger = KotlinLogging.logger {}

interface IEmailClient {
    val whitelist: List<Regex>?

    fun sendEmail(
        traceId: String,
        toAddress: String,
        fromAddress: String,
        content: EmailContent,
    ) {
        if (!toAddress.matches(EMAIL_PATTERN)) {
            logger.warn("Will not send email due to invalid toAddress: (traceId: $traceId)")
            return
        }

        val whitelist = this.whitelist
        if (whitelist != null && !whitelist.any { it.matches(toAddress) }) {
            logger.info {
                "Not sending email to $toAddress because it does not match any of the entries in whitelist"
            }
            return
        }

        logger.info { "Sending email (traceId: $traceId)" }
        sendValidatedEmail(toAddress, fromAddress, content, traceId)
    }

    fun sendValidatedEmail(
        toAddress: String,
        fromAddress: String,
        content: EmailContent,
        traceId: String,
    )
}
