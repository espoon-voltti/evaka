// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.emailclient.Email
import fi.espoo.evaka.emailclient.EmailClient
import fi.espoo.evaka.emailclient.IEmailMessageProvider
import fi.espoo.evaka.pis.EmailMessageType
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import mu.KotlinLogging
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
        val fromAddress = emailEnv.applicationReceivedSender(language)
        val content =
            when (type) {
                ApplicationType.CLUB -> emailMessageProvider.clubApplicationReceived(language)
                ApplicationType.DAYCARE -> emailMessageProvider.daycareApplicationReceived(language)
                ApplicationType.PRESCHOOL ->
                    emailMessageProvider.preschoolApplicationReceived(
                        language,
                        sentWithinPreschoolApplicationPeriod!!,
                    )
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
