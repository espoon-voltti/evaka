// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.decision.DecisionService
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.NotifyDecisionCreated
import fi.espoo.evaka.shared.async.SendDecision
import fi.espoo.evaka.shared.config.Roles
import mu.KotlinLogging
import org.jdbi.v3.core.Handle
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class DecisionMessageProcessor(
    private val asyncJobRunner: AsyncJobRunner,
    private val decisionService: DecisionService
) {
    init {
        asyncJobRunner.notifyDecisionCreated = ::runCreateJob
        asyncJobRunner.sendDecision = ::runSendJob
    }

    fun runCreateJob(h: Handle, msg: NotifyDecisionCreated) {
        val user = msg.user
        val decisionId = msg.decisionId

        user.requireOneOfRoles(Roles.ADMIN, Roles.SERVICE_WORKER, Roles.UNIT_SUPERVISOR)

        decisionService.createDecisionPdfs(h, user, decisionId)

        logger.info { "Successfully created decision pdf(s) for decision (id: $decisionId)." }
        if (msg.sendAsMessage) {
            logger.info { "Sending decision pdf(s) for decision (id: $decisionId)." }
            asyncJobRunner.plan(h, listOf(SendDecision(decisionId, msg.user)))
        }
    }

    fun runSendJob(h: Handle, msg: SendDecision) {
        val decisionId = msg.decisionId

        decisionService.deliverDecisionToGuardians(h, decisionId)
        logger.info { "Successfully sent decision(s) pdf for decision (id: $decisionId)." }
    }
}
