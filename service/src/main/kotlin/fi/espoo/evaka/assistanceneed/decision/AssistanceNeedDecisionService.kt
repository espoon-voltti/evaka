// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceneed.decision

import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.emailclient.IEmailClient
import fi.espoo.evaka.emailclient.IEmailMessageProvider
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.pis.service.getChildGuardians
import fi.espoo.evaka.shared.AssistanceNeedDecisionId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class AssistanceNeedDecisionService(
    private val emailClient: IEmailClient,
    private val emailMessageProvider: IEmailMessageProvider,
    env: EmailEnv,
    asyncJobRunner: AsyncJobRunner<AsyncJob>
) {
    private val senderAddressFi = env.applicationReceivedSenderAddressFi
    private val senderNameFi = env.applicationReceivedSenderNameFi
    private val senderAddressSv = env.applicationReceivedSenderAddressSv
    private val senderNameSv = env.applicationReceivedSenderNameSv

    init {
        asyncJobRunner.registerHandler(::runSendAssistanceNeedDecisionEmail)
    }

    fun runSendAssistanceNeedDecisionEmail(db: Database.Connection, clock: EvakaClock, msg: AsyncJob.SendAssistanceNeedDecisionEmail) {
        db.transaction { tx ->
            this.sendDecisionEmail(tx, msg.decisionId)
            logger.info { "Successfully sent assistance need decision email (id: ${msg.decisionId})." }
        }
    }

    fun sendDecisionEmail(tx: Database.Transaction, decisionId: AssistanceNeedDecisionId) {
        val decision = tx.getAssistanceNeedDecisionById(decisionId)

        if (decision.child?.id == null) {
            throw IllegalStateException("Assistance need decision must have a child associated with it")
        }

        logger.info { "Sending assistance need decision email (decisionId: $decision)" }

        val fromAddress = when (decision.language) {
            AssistanceNeedDecisionLanguage.SV -> "$senderNameSv <$senderAddressSv>"
            else -> "$senderNameFi <$senderAddressFi>"
        }

        tx.getChildGuardians(decision.child.id).map {
            Pair(it, tx.getPersonById(it)?.email)
        }.toMap().forEach { (guardianId, email) ->
            if (email != null) {
                emailClient.sendEmail(
                    "$decisionId - $guardianId",
                    email,
                    fromAddress,
                    emailMessageProvider.getAssistanceNeedDecisionEmailSubject(),
                    emailMessageProvider.getAssistanceNeedDecisionEmailHtml(decision.child.id, decision.id),
                    emailMessageProvider.getAssistanceNeedDecisionEmailText(decision.child.id, decision.id)
                )
            }
        }
    }
}
