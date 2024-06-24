// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.emailclient

import mu.KotlinLogging
import org.unbescape.html.HtmlEscape
import software.amazon.awssdk.services.ses.SesClient
import software.amazon.awssdk.services.ses.model.AccountSendingPausedException
import software.amazon.awssdk.services.ses.model.Body
import software.amazon.awssdk.services.ses.model.ConfigurationSetDoesNotExistException
import software.amazon.awssdk.services.ses.model.ConfigurationSetSendingPausedException
import software.amazon.awssdk.services.ses.model.Content
import software.amazon.awssdk.services.ses.model.Destination
import software.amazon.awssdk.services.ses.model.MailFromDomainNotVerifiedException
import software.amazon.awssdk.services.ses.model.Message
import software.amazon.awssdk.services.ses.model.SendEmailRequest

private val logger = KotlinLogging.logger {}

class SESEmailClient(
    private val client: SesClient,
    private val whitelist: List<Regex>?,
    private val subjectPostfix: String?
) : EmailClient {
    private val charset = "UTF-8"

    override fun send(email: Email) {
        val toAddress = email.toAddress
        val fromAddress = email.fromAddress
        val content = email.content
        val traceId = email.traceId

        if (whitelist != null && !whitelist.any { it.matches(toAddress) }) {
            logger.info {
                "Not sending email to $toAddress because it does not match any of the entries in whitelist"
            }
            return
        }

        val html =
            """
<!DOCTYPE html>
<html>
<head>
<title>${HtmlEscape.escapeHtml5(content.subject)}</title>
</head>
<body>
${content.html}
</body>
</html>
"""
        logger.info { "Sending email (traceId: $traceId)" }
        try {
            val request =
                SendEmailRequest
                    .builder()
                    .destination(Destination.builder().toAddresses(toAddress).build())
                    .message(
                        Message
                            .builder()
                            .body(
                                Body
                                    .builder()
                                    .html(
                                        Content
                                            .builder()
                                            .charset(charset)
                                            .data(html)
                                            .build()
                                    ).text(
                                        Content
                                            .builder()
                                            .charset(charset)
                                            .data(content.text)
                                            .build()
                                    ).build()
                            ).subject(
                                Content
                                    .builder()
                                    .charset(charset)
                                    .data(
                                        when (subjectPostfix) {
                                            null,
                                            "" -> content.subject
                                            else -> "${content.subject} [$subjectPostfix]"
                                        }
                                    ).build()
                            ).build()
                    ).source(fromAddress)
                    .build()

            client.sendEmail(request)
            logger.info { "Email sent (traceId: $traceId)" }
        } catch (e: Exception) {
            when (e) {
                is MailFromDomainNotVerifiedException,
                is ConfigurationSetDoesNotExistException,
                is ConfigurationSetSendingPausedException,
                is AccountSendingPausedException ->
                    logger.error(e) { "Will not send email (traceId: $traceId): ${e.message}" }
                else -> {
                    logger.error(e) { "Couldn't send email (traceId: $traceId): ${e.message}" }
                    throw e
                }
            }
        }
    }
}
