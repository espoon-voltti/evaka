// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.serviceneed.application

import evaka.core.EmailEnv
import evaka.core.daycare.domain.Language
import evaka.core.daycare.getDaycare
import evaka.core.emailclient.Email
import evaka.core.emailclient.EmailClient
import evaka.core.emailclient.IEmailMessageProvider
import evaka.core.pis.EmailMessageType
import evaka.core.placement.getPlacementsForChildDuring
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.NotFound
import io.github.oshai.kotlinlogging.KotlinLogging
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

        val fromAddress = emailEnv.sender(language)
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
