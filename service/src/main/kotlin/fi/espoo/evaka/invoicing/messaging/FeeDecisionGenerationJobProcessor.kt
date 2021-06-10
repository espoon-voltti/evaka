// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.messaging

import fi.espoo.evaka.invoicing.service.FinanceDecisionGenerator
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.GenerateFinanceDecisions
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import mu.KotlinLogging
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.stereotype.Component
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Component
class FeeDecisionGenerationJobProcessor(
    private val generator: FinanceDecisionGenerator,
    private val asyncJobRunner: AsyncJobRunner
) {
    init {
        asyncJobRunner.generateFinanceDecisions = ::runJob
    }

    fun runJob(db: Database, msg: GenerateFinanceDecisions) {
        logger.info { "Generating finance decisions with parameters $msg" }
        db.transaction { tx ->
            when (msg.person) {
                is GenerateFinanceDecisions.Person.Adult ->
                    generator.generateNewDecisionsForAdult(tx, msg.person.adultId, msg.dateRange)
                is GenerateFinanceDecisions.Person.Child ->
                    generator.generateNewDecisionsForChild(tx, msg.person.childId, msg.dateRange)
            }
        }
    }
}

fun planFinanceDecisionGeneration(
    tx: Database.Transaction,
    asyncJobRunner: AsyncJobRunner,
    dateRange: DateRange,
    targetHeadsOfFamily: List<UUID>
) {
    val heads = targetHeadsOfFamily.ifEmpty {
        tx.createQuery(
            "SELECT head_of_child FROM fridge_child WHERE daterange(start_date, end_date, '[]') && :dateRange AND conflict = false"
        )
            .bind("dateRange", dateRange)
            .mapTo<UUID>()
            .list()
    }

    asyncJobRunner.plan(tx, heads.distinct().map { GenerateFinanceDecisions.forAdult(it, dateRange) })
}
