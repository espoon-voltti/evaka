// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.decision.DecisionService
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.NotifyDecisionCreated
import fi.espoo.evaka.shared.async.SendDecision
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class DecisionMessageProcessor(
    private val asyncJobRunner: AsyncJobRunner,
    private val decisionService: DecisionService,
    private val acl: AccessControlList
) {
    init {
        asyncJobRunner.notifyDecisionCreated = ::runCreateJob
        asyncJobRunner.sendDecision = ::runSendJob
    }

    fun runCreateJob(db: Database, msg: NotifyDecisionCreated) = db.transaction { tx ->
        val user = msg.user
        val decisionId = msg.decisionId

        acl.getRolesForDecision(user, msg.decisionId).requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR)

        decisionService.createDecisionPdfs(tx, user, decisionId)

        logger.info { "Successfully created decision pdf(s) for decision (id: $decisionId)." }
        if (msg.sendAsMessage) {
            logger.info { "Sending decision pdf(s) for decision (id: $decisionId)." }
            asyncJobRunner.plan(tx, listOf(SendDecision(decisionId, msg.user)))
        }
    }

    fun runSendJob(db: Database, msg: SendDecision) = db.transaction { tx ->
        val decisionId = msg.decisionId

        decisionService.deliverDecisionToGuardians(tx, decisionId)
        logger.info { "Successfully sent decision(s) pdf for decision (id: $decisionId)." }
    }
}
