// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.messaging

import fi.espoo.evaka.invoicing.service.DecisionGenerator
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.NotifyFamilyUpdated
import fi.espoo.evaka.shared.async.NotifyFeeAlterationUpdated
import fi.espoo.evaka.shared.async.NotifyIncomeUpdated
import fi.espoo.evaka.shared.async.NotifyPlacementPlanApplied
import fi.espoo.evaka.shared.async.NotifyServiceNeedUpdated
import fi.espoo.evaka.shared.db.transaction
import fi.espoo.evaka.shared.domain.Period
import mu.KotlinLogging
import org.jdbi.v3.core.Jdbi
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class FeeDecisionGenerationJobProcessor(
    private val generator: DecisionGenerator,
    private val jdbi: Jdbi,
    asyncJobRunner: AsyncJobRunner
) {
    init {
        asyncJobRunner.notifyFamilyUpdated = ::runJob
        asyncJobRunner.notifyFeeAlterationUpdated = ::runJob
        asyncJobRunner.notifyIncomeUpdated = ::runJob
        asyncJobRunner.notifyPlacementPlanApplied = ::runJob
        asyncJobRunner.notifyServiceNeedUpdated = ::runJob
    }

    fun runJob(msg: NotifyFamilyUpdated) = jdbi.transaction { h ->
        logger.info { "Handling family updated event for person (id: ${msg.adultId})" }
        generator.handleFamilyUpdate(h, msg.adultId, Period(msg.startDate, msg.endDate))
    }

    fun runJob(msg: NotifyFeeAlterationUpdated) = jdbi.transaction { h ->
        logger.info { "Handling fee alteration updated event ($msg)" }
        generator.handleFeeAlterationChange(h, msg.personId, Period(msg.startDate, msg.endDate))
    }

    fun runJob(msg: NotifyIncomeUpdated) = jdbi.transaction { h ->
        logger.info { "Handling income updated event ($msg)" }
        generator.handleIncomeChange(h, msg.personId, Period(msg.startDate, msg.endDate))
    }

    fun runJob(msg: NotifyPlacementPlanApplied) = jdbi.transaction { h ->
        logger.info { "Handling placement plan accepted event ($msg)" }
        generator.handlePlacement(h, msg.childId, Period(msg.startDate, msg.endDate))
    }

    fun runJob(msg: NotifyServiceNeedUpdated) = jdbi.transaction { h ->
        logger.info { "Handling service need updated event for child (id: ${msg.childId})" }
        generator.handleServiceNeed(h, msg.childId, Period(msg.startDate, msg.endDate))
    }
}
