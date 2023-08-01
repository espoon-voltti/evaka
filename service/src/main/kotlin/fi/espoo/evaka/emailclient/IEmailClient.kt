// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.emailclient

import fi.espoo.evaka.pis.EmailMessageType
import fi.espoo.evaka.pis.getEnabledEmailTypes
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import mu.KotlinLogging

private val EMAIL_PATTERN = "^([\\w.%+-]+)@([\\w-]+\\.)+([\\w]{2,})\$".toRegex()

private val logger = KotlinLogging.logger {}

interface IEmailClient {
    val whitelist: List<Regex>?

    fun sendEmail(
        dbc: Database.Connection,
        personId: PersonId,
        emailType: EmailMessageType,
        toAddress: String,
        fromAddress: String,
        content: EmailContent,
        traceId: String,
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

        val enabledEmailTypes = dbc.read { tx -> tx.getEnabledEmailTypes(personId) }
        if (emailType !in enabledEmailTypes) {
            logger.info {
                "Not sending email (traceId: $traceId): $emailType not enabled for person $personId"
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
