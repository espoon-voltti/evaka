// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.emailclient.IEmailClient
import fi.espoo.evaka.emailclient.IEmailMessageProvider
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
class ApplicationReceivedEmailService(private val emailClient: IEmailClient, private val emailMessageProvider: IEmailMessageProvider, env: EmailEnv) {

    private val senderAddressFi = env.applicationReceivedSenderAddressFi
    private val senderNameFi = env.applicationReceivedSenderNameFi
    private val senderAddressSv = env.applicationReceivedSenderAddressSv
    private val senderNameSv = env.applicationReceivedSenderNameSv

    fun sendApplicationEmail(personId: UUID, toAddress: String, language: Language, type: ApplicationType, sentWithinPreschoolApplicationPeriod: Boolean? = null) {
        val fromAddress = when (language) {
            Language.sv -> "$senderNameSv <$senderAddressSv>"
            else -> "$senderNameFi <$senderAddressFi>"
        }

        val subject = when (type) {
            ApplicationType.DAYCARE -> emailMessageProvider.getDaycareApplicationReceivedEmailSubject()
            ApplicationType.CLUB -> emailMessageProvider.getClubApplicationReceivedEmailSubject()
            ApplicationType.PRESCHOOL -> emailMessageProvider.getPreschoolApplicationReceivedEmailSubject()
        }

        val html = when (type) {
            ApplicationType.DAYCARE -> emailMessageProvider.getDaycareApplicationReceivedEmailHtml()
            ApplicationType.CLUB -> emailMessageProvider.getClubApplicationReceivedEmailHtml()
            ApplicationType.PRESCHOOL -> emailMessageProvider.getPreschoolApplicationReceivedEmailHtml(sentWithinPreschoolApplicationPeriod!!)
        }

        val text = when (type) {
            ApplicationType.DAYCARE -> emailMessageProvider.getDaycareApplicationReceivedEmailText()
            ApplicationType.CLUB -> emailMessageProvider.getClubApplicationReceivedEmailText()
            ApplicationType.PRESCHOOL -> emailMessageProvider.getPreschoolApplicationReceivedEmailText(sentWithinPreschoolApplicationPeriod!!)
        }

        logger.info { "Sending application email (personId: $personId)" }
        emailClient.sendEmail(personId.toString(), toAddress, fromAddress, subject, html, text)
    }
}
