// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.emailclient

import fi.espoo.evaka.pis.EmailMessageType
import fi.espoo.evaka.shared.DatabaseTable
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
        fromAddress: String,
        content: EmailContent,
        traceId: String,
    ) {
        val (toAddress, enabledEmailTypes) =
            dbc.read { tx -> tx.getEmailAddressAndEnabledTypes(personId) }

        if (toAddress == null) {
            logger.warn("Will not send email due to missing email address: (traceId: $traceId)")
            return
        }

        if (!toAddress.matches(EMAIL_PATTERN)) {
            logger.warn(
                "Will not send email due to invalid toAddress \"$toAddress\": (traceId: $traceId)"
            )
            return
        }

        val whitelist = this.whitelist
        if (whitelist != null && !whitelist.any { it.matches(toAddress) }) {
            logger.info {
                "Not sending email to $toAddress because it does not match any of the entries in whitelist"
            }
            return
        }

        if (emailType !in (enabledEmailTypes ?: EmailMessageType.values().toList())) {
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

private data class EmailAndEnabledEmailTypes(
    val email: String?,
    val enabledEmailTypes: List<EmailMessageType>?
)

private fun Database.Read.getEmailAddressAndEnabledTypes(
    personId: PersonId
): EmailAndEnabledEmailTypes {
    return createQuery<DatabaseTable> {
            sql("""SELECT email, enabled_email_types FROM person WHERE id = ${bind(personId)}""")
        }
        .mapTo<EmailAndEnabledEmailTypes>()
        .single()
}
