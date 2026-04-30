// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.oulu

import evaka.core.ScheduledJobsEnv
import evaka.core.bi.BiTable
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.async.AsyncJobType
import evaka.core.shared.async.removeUnclaimedJobs
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.job.JobSchedule
import evaka.core.shared.job.ScheduledJobDefinition
import evaka.core.shared.job.ScheduledJobSettings
import evaka.instance.oulu.dw.DwQuery
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalTime

enum class OuluScheduledJob(
    val fn: (OuluScheduledJobs, Database.Connection, EvakaClock) -> Unit,
    val defaultSettings: ScheduledJobSettings,
) {
    PlanDwExportJobs(
        { jobs, db, clock -> jobs.planDwJobs(db, clock) },
        ScheduledJobSettings(enabled = false, schedule = JobSchedule.daily(LocalTime.of(20, 0))),
    ),
    PlanBiExportJobs(
        { jobs, db, clock -> jobs.planBiJobs(db, clock) },
        ScheduledJobSettings(enabled = false, schedule = JobSchedule.daily(LocalTime.of(1, 0))),
    ),
}

class OuluScheduledJobs(
    private val asyncJobRunner: AsyncJobRunner<OuluAsyncJob>,
    env: ScheduledJobsEnv<OuluScheduledJob>,
    private val biTables: List<BiTable>,
) : JobSchedule {
    private val logger = KotlinLogging.logger {}

    override val jobs: List<ScheduledJobDefinition> =
        env.jobs.map {
            ScheduledJobDefinition(it.key, it.value) { db, clock -> it.key.fn(this, db, clock) }
        }

    fun planDwJobs(db: Database.Connection, clock: EvakaClock) {
        logger.info { "Planning DW jobs for ${DwQuery.entries.size} queries" }
        db.transaction { tx ->
            tx.removeUnclaimedJobs(setOf(AsyncJobType(OuluAsyncJob.SendDWQuery::class)))
            asyncJobRunner.plan(
                tx,
                DwQuery.entries.asSequence().map(OuluAsyncJob::SendDWQuery),
                runAt = clock.now(),
                retryCount = 1,
            )
        }
    }

    fun planBiJobs(db: Database.Connection, clock: EvakaClock) {
        logger.info { "Planning BI jobs for ${biTables.size} tables" }
        db.transaction { tx ->
            tx.removeUnclaimedJobs(setOf(AsyncJobType(OuluAsyncJob.SendBiTable::class)))
            asyncJobRunner.plan(
                tx,
                biTables.asSequence().map(OuluAsyncJob::SendBiTable),
                runAt = clock.now(),
                retryCount = 1,
            )
        }
    }
}
