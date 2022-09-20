// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.messaging

import fi.espoo.evaka.invoicing.service.FinanceDecisionGenerator
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class FeeDecisionGenerationJobProcessor(
    private val generator: FinanceDecisionGenerator,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>
) {
    init {
        asyncJobRunner.registerHandler<AsyncJob.NotifyFeeThresholdsUpdated>(::runJob)
        asyncJobRunner.registerHandler<AsyncJob.GenerateFinanceDecisions>(::runJob)
    }

    fun runJob(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.NotifyFeeThresholdsUpdated
    ) {
        logger.info { "Handling fee thresholds update event for date range (id: ${msg.dateRange})" }
        db.transaction {
            planFinanceDecisionGeneration(it, clock, asyncJobRunner, msg.dateRange, listOf())
        }
    }

    fun runJob(db: Database.Connection, clock: EvakaClock, msg: AsyncJob.GenerateFinanceDecisions) {
        logger.info { "Generating finance decisions with parameters $msg" }
        db.transaction { tx ->
            when (msg.person) {
                is AsyncJob.GenerateFinanceDecisions.Person.Adult ->
                    generator.generateNewDecisionsForAdult(
                        tx,
                        clock,
                        msg.person.adultId,
                        msg.dateRange.start
                    )
                is AsyncJob.GenerateFinanceDecisions.Person.Child ->
                    generator.generateNewDecisionsForChild(
                        tx,
                        clock,
                        msg.person.childId,
                        msg.dateRange.start
                    )
            }
        }
    }
}

fun planFinanceDecisionGeneration(
    tx: Database.Transaction,
    clock: EvakaClock,
    asyncJobRunner: AsyncJobRunner<AsyncJob>,
    dateRange: DateRange,
    targetHeadsOfFamily: List<PersonId>
) {
    val heads =
        targetHeadsOfFamily.ifEmpty {
            tx.createQuery(
                    "SELECT head_of_child FROM fridge_child WHERE daterange(start_date, end_date, '[]') && :dateRange AND conflict = false"
                )
                .bind("dateRange", dateRange)
                .mapTo<PersonId>()
                .list()
        }

    asyncJobRunner.plan(
        tx,
        heads.distinct().map { AsyncJob.GenerateFinanceDecisions.forAdult(it, dateRange) },
        runAt = clock.now()
    )
}
