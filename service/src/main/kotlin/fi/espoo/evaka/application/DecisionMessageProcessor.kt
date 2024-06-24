// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.decision.DecisionService
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class DecisionMessageProcessor(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val decisionService: DecisionService
) {
    init {
        asyncJobRunner.registerHandler(::runCreateJob)
        asyncJobRunner.registerHandler(::runSendJob)
    }

    fun runCreateJob(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.NotifyDecisionCreated
    ) = db.transaction { tx ->
        val decisionId = msg.decisionId

        decisionService.createDecisionPdf(tx, decisionId)

        logger.info { "Successfully created decision pdf(s) for decision (id: $decisionId)." }
        if (msg.sendAsMessage) {
            logger.info { "Sending decision pdf(s) for decision (id: $decisionId)." }
            asyncJobRunner.plan(
                tx,
                listOf(AsyncJob.SendDecision(decisionId)),
                runAt = clock.now()
            )
        }
    }

    fun runSendJob(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.SendDecision
    ) = db.transaction { tx ->
        val decisionId = msg.decisionId

        decisionService.deliverDecisionToGuardians(tx, clock, decisionId)
        logger.info { "Successfully sent decision(s) pdf for decision (id: $decisionId)." }
    }
}
