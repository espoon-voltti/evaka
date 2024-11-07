// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.serviceneed.application

import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.daycare.getDaycare
import fi.espoo.evaka.emailclient.Email
import fi.espoo.evaka.emailclient.EmailClient
import fi.espoo.evaka.emailclient.IEmailMessageProvider
import fi.espoo.evaka.pis.EmailMessageType
import fi.espoo.evaka.placement.getPlacementsForChildDuring
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class ServiceApplicationService(
    private val emailClient: EmailClient,
    private val emailMessageProvider: IEmailMessageProvider,
    private val emailEnv: EmailEnv,
    asyncJobRunner: AsyncJobRunner<AsyncJob>,
) {
    init {
        asyncJobRunner.registerHandler(::sendServiceApplicationDecidedEmail)
    }

    fun sendServiceApplicationDecidedEmail(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.SendServiceApplicationDecidedEmail,
    ) {
        val (application, accepted, language) =
            db.read { tx ->
                val application =
                    tx.getServiceApplication(msg.serviceApplicationId) ?: throw NotFound()
                val accepted =
                    application.decision?.let {
                        it.status == ServiceApplicationDecisionStatus.ACCEPTED
                    } ?: throw NotFound()
                val language =
                    tx.getPlacementsForChildDuring(
                            childId = application.childId,
                            start = application.startDate,
                            end = application.startDate,
                        )
                        .firstOrNull()
                        ?.let { placement -> tx.getDaycare(placement.unitId)?.language }
                        ?: Language.fi

                Triple(application, accepted, language)
            }

        logger.info { "Sending service application decided email (${application.id})." }

        val fromAddress = emailEnv.applicationReceivedSender(language)
        val content =
            emailMessageProvider.serviceApplicationDecidedNotification(
                accepted = accepted,
                startDate = application.startDate,
            )

        Email.create(
                db,
                application.personId,
                EmailMessageType.DECISION_NOTIFICATION,
                fromAddress,
                content,
                "${msg.serviceApplicationId} - ${application.personId}",
            )
            ?.also { emailClient.send(it) }

        logger.info { "Successfully sent service application decided email (${application.id})." }
    }
}
