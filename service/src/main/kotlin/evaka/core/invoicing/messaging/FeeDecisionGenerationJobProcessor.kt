// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.invoicing.messaging

import evaka.core.invoicing.service.FinanceDecisionGenerator
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class FeeDecisionGenerationJobProcessor(
    private val generator: FinanceDecisionGenerator,
    asyncJobRunner: AsyncJobRunner<AsyncJob>,
) {
    init {
        asyncJobRunner.registerHandler<AsyncJob.GenerateFinanceDecisions>(::runJob)
    }

    fun runJob(db: Database.Connection, clock: EvakaClock, msg: AsyncJob.GenerateFinanceDecisions) {
        logger.info { "Generating finance decisions with parameters $msg" }
        db.transaction { tx ->
            when (msg.person) {
                is AsyncJob.GenerateFinanceDecisions.Person.Adult -> {
                    generator.generateNewDecisionsForAdult(
                        tx,
                        msg.person.adultId,
                        skipPropagation = msg.person.skipPropagation == true,
                    )
                }

                is AsyncJob.GenerateFinanceDecisions.Person.Child -> {
                    generator.generateNewDecisionsForChild(tx, msg.person.childId)
                }
            }
        }
    }
}
