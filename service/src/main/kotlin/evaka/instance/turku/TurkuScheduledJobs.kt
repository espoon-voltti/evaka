// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.turku

import evaka.core.ScheduledJobsEnv
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.async.AsyncJobType
import evaka.core.shared.async.removeUnclaimedJobs
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.job.JobSchedule
import evaka.core.shared.job.ScheduledJobDefinition
import evaka.core.shared.job.ScheduledJobSettings
import evaka.instance.turku.dw.DwQuery
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalTime

enum class TurkuScheduledJob(
    val fn: (TurkuScheduledJobs, Database.Connection, EvakaClock) -> Unit,
    val defaultSettings: ScheduledJobSettings,
) {
    PlanDwExportJobs(
        { jobs, db, clock -> jobs.planDwExportJobs(db, clock, DwQuery.entries) },
        ScheduledJobSettings(enabled = false, schedule = JobSchedule.daily(LocalTime.of(20, 0))),
    )
}

class TurkuScheduledJobs(
    private val asyncJobRunner: AsyncJobRunner<TurkuAsyncJob>,
    env: ScheduledJobsEnv<TurkuScheduledJob>,
) : JobSchedule {
    private val logger = KotlinLogging.logger {}

    override val jobs: List<ScheduledJobDefinition> =
        env.jobs.map {
            ScheduledJobDefinition(it.key, it.value) { db, clock -> it.key.fn(this, db, clock) }
        }

    fun planDwExportJobs(
        db: Database.Connection,
        clock: EvakaClock,
        selectedQueries: List<DwQuery>?,
    ) {
        val queries = selectedQueries ?: DwQuery.entries
        logger.info { "Planning DW jobs for ${queries.size} queries" }
        db.transaction { tx ->
            tx.removeUnclaimedJobs(setOf(AsyncJobType(TurkuAsyncJob.SendDWQuery::class)))
            asyncJobRunner.plan(
                tx,
                queries.asSequence().map(TurkuAsyncJob::SendDWQuery),
                runAt = clock.now(),
                retryCount = 1,
            )
        }
    }
}
