// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.application

import evaka.core.EmailEnv
import evaka.core.daycare.domain.Language
import evaka.core.emailclient.Email
import evaka.core.emailclient.EmailClient
import evaka.core.emailclient.IEmailMessageProvider
import evaka.core.pis.EmailMessageType
import evaka.core.shared.PersonId
import evaka.core.shared.db.Database
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class ApplicationReceivedEmailService(
    private val emailClient: EmailClient,
    private val emailMessageProvider: IEmailMessageProvider,
    private val emailEnv: EmailEnv,
) {
    fun sendApplicationEmail(
        dbc: Database.Connection,
        personId: PersonId,
        language: Language,
        type: ApplicationType,
        sentWithinPreschoolApplicationPeriod: Boolean? = null,
    ) {
        val fromAddress = emailEnv.sender(language)
        val content =
            when (type) {
                ApplicationType.CLUB -> {
                    emailMessageProvider.clubApplicationReceived(language)
                }

                ApplicationType.DAYCARE -> {
                    emailMessageProvider.daycareApplicationReceived(language)
                }

                ApplicationType.PRESCHOOL -> {
                    emailMessageProvider.preschoolApplicationReceived(
                        language,
                        sentWithinPreschoolApplicationPeriod!!,
                    )
                }
            }
        logger.info { "Sending application email (personId: $personId)" }
        Email.create(
                dbc = dbc,
                personId = personId,
                emailType = EmailMessageType.TRANSACTIONAL,
                fromAddress = fromAddress,
                content = content,
                traceId = personId.toString(),
            )
            ?.also { emailClient.send(it) }
    }
}
