// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.oulu

import fi.espoo.evaka.ScheduledJobsEnv
import fi.espoo.evaka.oulu.dw.DwQuery
import fi.espoo.evaka.oulu.dw.FabricHistoryQuery
import fi.espoo.evaka.oulu.dw.FabricQuery
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.AsyncJobType
import fi.espoo.evaka.shared.async.removeUnclaimedJobs
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.job.JobSchedule
import fi.espoo.evaka.shared.job.ScheduledJobDefinition
import fi.espoo.evaka.shared.job.ScheduledJobSettings
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalTime

enum class EvakaOuluScheduledJob(
    val fn: (EvakaOuluScheduledJobs, Database.Connection, EvakaClock) -> Unit,
    val defaultSettings: ScheduledJobSettings,
) {
    PlanDwExportJobs(
        { jobs, db, clock -> jobs.planDwJobs(db, clock, DwQuery.entries) },
        ScheduledJobSettings(enabled = false, schedule = JobSchedule.daily(LocalTime.of(20, 0))),
    ),
    PlanFabricExportJobs(
        { jobs, db, clock -> jobs.planFabricJobs(db, clock, FabricQuery.entries) },
        ScheduledJobSettings(enabled = false, schedule = JobSchedule.daily(LocalTime.of(20, 0))),
    ),
    PlanFabricHistoryJobs(
        { jobs, db, clock -> jobs.planFabricHistoryJobs(db, clock, FabricHistoryQuery.entries) },
        ScheduledJobSettings(enabled = false, schedule = JobSchedule.daily(LocalTime.of(20, 0))),
    ),
}

class EvakaOuluScheduledJobs(
    private val asyncJobRunner: AsyncJobRunner<EvakaOuluAsyncJob>,
    env: ScheduledJobsEnv<EvakaOuluScheduledJob>,
) : JobSchedule {
    private val logger = KotlinLogging.logger {}

    override val jobs: List<ScheduledJobDefinition> =
        env.jobs.map {
            ScheduledJobDefinition(it.key, it.value) { db, clock -> it.key.fn(this, db, clock) }
        }

    fun planDwJobs(db: Database.Connection, clock: EvakaClock, selectedQueries: List<DwQuery>?) {
        val queries = selectedQueries ?: DwQuery.entries
        logger.info { "Planning DW jobs for ${queries.size} queries" }
        db.transaction { tx ->
            tx.removeUnclaimedJobs(setOf(AsyncJobType(EvakaOuluAsyncJob.SendDWQuery::class)))
            asyncJobRunner.plan(
                tx,
                queries.asSequence().map(EvakaOuluAsyncJob::SendDWQuery),
                runAt = clock.now(),
                retryCount = 1,
            )
        }
    }

    fun planFabricJobs(
        db: Database.Connection,
        clock: EvakaClock,
        selectedQueries: List<FabricQuery>?,
    ) {
        val queries = selectedQueries ?: FabricQuery.entries
        logger.info { "Planning Fabric jobs for ${queries.size} queries" }
        db.transaction { tx ->
            tx.removeUnclaimedJobs(setOf(AsyncJobType(EvakaOuluAsyncJob.SendFabricQuery::class)))
            asyncJobRunner.plan(
                tx,
                queries.asSequence().map(EvakaOuluAsyncJob::SendFabricQuery),
                runAt = clock.now(),
                retryCount = 1,
            )
        }
    }

    fun planFabricHistoryJobs(
        db: Database.Connection,
        clock: EvakaClock,
        selectedQueries: List<FabricHistoryQuery>?,
    ) {
        val queries = selectedQueries ?: FabricHistoryQuery.entries
        logger.info { "Planning Fabric History jobs for ${queries.size} queries" }
        db.transaction { tx ->
            tx.removeUnclaimedJobs(
                setOf(AsyncJobType(EvakaOuluAsyncJob.SendFabricHistoryQuery::class))
            )
            asyncJobRunner.plan(
                tx,
                queries.asSequence().map(EvakaOuluAsyncJob::SendFabricHistoryQuery),
                runAt = clock.now(),
                retryCount = 1,
            )
        }
    }
}
