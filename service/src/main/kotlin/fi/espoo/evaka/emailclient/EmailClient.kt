// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.emailclient

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService
import com.amazonaws.services.simpleemail.model.AccountSendingPausedException
import com.amazonaws.services.simpleemail.model.Body
import com.amazonaws.services.simpleemail.model.ConfigurationSetDoesNotExistException
import com.amazonaws.services.simpleemail.model.ConfigurationSetSendingPausedException
import com.amazonaws.services.simpleemail.model.Content
import com.amazonaws.services.simpleemail.model.Destination
import com.amazonaws.services.simpleemail.model.MailFromDomainNotVerifiedException
import com.amazonaws.services.simpleemail.model.Message
import com.amazonaws.services.simpleemail.model.SendEmailRequest
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class EmailClient(private val client: AmazonSimpleEmailService) : IEmailClient {
    private val charset = "UTF-8"

    override fun sendEmail(traceId: String, toAddress: String, fromAddress: String, subject: String, htmlBody: String, textBody: String) {
        logger.info { "Sending email (traceId: $traceId)" }

        if (validateToAddress(traceId, toAddress)) {
            try {
                val request = SendEmailRequest()
                    .withDestination(Destination(listOf(toAddress)))
                    .withMessage(
                        Message()
                            .withBody(
                                Body()
                                    .withHtml(Content().withCharset(charset).withData(htmlBody))
                                    .withText(Content().withCharset(charset).withData(textBody))
                            )
                            .withSubject(Content().withCharset(charset).withData(subject))
                    )
                    .withSource(fromAddress)

                client.sendEmail(request)
                logger.info { "Email sent (traceId: $traceId)" }
            } catch (e: Exception) {
                when (e) {
                    is MailFromDomainNotVerifiedException, is ConfigurationSetDoesNotExistException, is ConfigurationSetSendingPausedException, is AccountSendingPausedException ->
                        logger.error(e) { "Will not send email (traceId: $traceId): ${e.message}" }
                    else -> {
                        logger.error(e) { "Couldn't send email (traceId: $traceId): ${e.message}" }
                        throw e
                    }
                }
            }
        } else {
            logger.warn("Will not send email due to invalid toAddress: (traceId: $traceId)")
        }
    }
}
