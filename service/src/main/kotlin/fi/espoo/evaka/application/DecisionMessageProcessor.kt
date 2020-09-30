// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.decision.DecisionService
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.NotifyDecision2Created
import fi.espoo.evaka.shared.async.SendDecision2
import fi.espoo.evaka.shared.config.Roles
import fi.espoo.evaka.shared.db.runAfterCommit
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class DecisionMessageProcessor(
    private val asyncJobRunner: AsyncJobRunner,
    private val decisionService: DecisionService
) {
    init {
        asyncJobRunner.notifyDecision2Created = ::runCreateJob2
        asyncJobRunner.sendDecision2 = ::runSendJob2
    }

    fun runCreateJob2(msg: NotifyDecision2Created) {
        val user = msg.user
        val decisionId = msg.decisionId

        user.requireOneOfRoles(Roles.ADMIN, Roles.SERVICE_WORKER, Roles.UNIT_SUPERVISOR)

        decisionService.createDecisionPdfs(user, decisionId)

        logger.info { "Successfully created decision pdf(s) for decision (id: $decisionId)." }
        if (msg.sendAsMessage) {
            logger.info { "Sending decision pdf(s) for decision (id: $decisionId)." }
            asyncJobRunner.plan(listOf(SendDecision2(decisionId, msg.user)))
            runAfterCommit { asyncJobRunner.scheduleImmediateRun() }
        }
    }

    fun runSendJob2(msg: SendDecision2) {
        val decisionId = msg.decisionId

        decisionService.deliverDecisionToGuardians(decisionId)
        logger.info { "Successfully sent decision(s) pdf for decision (id: $decisionId)." }
    }
}
