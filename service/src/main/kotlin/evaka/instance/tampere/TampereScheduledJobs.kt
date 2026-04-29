// SPDX-FileCopyrightText: 2023 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.tampere

import evaka.core.ScheduledJobsEnv
import evaka.core.bi.BiTable
import evaka.core.reports.REPORT_STATEMENT_TIMEOUT
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.async.AsyncJobType
import evaka.core.shared.async.removeUnclaimedJobs
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.job.JobSchedule
import evaka.core.shared.job.ScheduledJobDefinition
import evaka.core.shared.job.ScheduledJobSettings
import evaka.instance.tampere.export.ExportUnitsAclService
import evaka.trevaka.archival.planDocumentArchival
import evaka.trevaka.export.ExportPreschoolChildDocumentsService
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalTime

enum class TampereScheduledJob(
    val fn: (TampereScheduledJobs, Database.Connection, EvakaClock) -> Unit,
    val defaultSettings: ScheduledJobSettings,
) {
    ExportPreschoolChildDocuments(
        TampereScheduledJobs::exportPreschoolChildDocuments,
        ScheduledJobSettings(
            enabled = false,
            schedule = JobSchedule.cron("0 0 0 1 8 ?"),
        ), // Yearly on the first of August
    ),
    ExportUnitsAcl(
        TampereScheduledJobs::exportUnitsAcl,
        ScheduledJobSettings(enabled = false, schedule = JobSchedule.daily(LocalTime.of(0, 0))),
    ),
    PlanBiExportJobs(
        { jobs, db, clock -> jobs.planBiJobs(db, clock) },
        ScheduledJobSettings(enabled = false, schedule = JobSchedule.daily(LocalTime.of(1, 0))),
    ),
    PlanDocumentArchival(
        TampereScheduledJobs::archiveEligibleDocuments,
        ScheduledJobSettings(enabled = false, schedule = JobSchedule.daily(LocalTime.of(20, 0))),
    ),
}

class TampereScheduledJobs(
    private val exportPreschoolChildDocumentsService: ExportPreschoolChildDocumentsService,
    private val exportUnitsAclService: ExportUnitsAclService,
    private val asyncJobRunner: AsyncJobRunner<TampereAsyncJob>,
    private val coreAsyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val properties: TampereProperties,
    env: ScheduledJobsEnv<TampereScheduledJob>,
    private val biTables: List<BiTable>,
) : JobSchedule {
    private val logger = KotlinLogging.logger {}

    override val jobs: List<ScheduledJobDefinition> =
        env.jobs.map {
            ScheduledJobDefinition(it.key, it.value) { db, clock -> it.key.fn(this, db, clock) }
        }

    fun exportUnitsAcl(db: Database.Connection, clock: EvakaClock) {
        db.transaction { tx -> exportUnitsAclService.exportUnitsAcl(tx, clock.now()) }
    }

    fun exportPreschoolChildDocuments(db: Database.Connection, clock: EvakaClock) {
        db.read { tx ->
            tx.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
            exportPreschoolChildDocumentsService.exportPreschoolChildDocuments(
                tx,
                clock.now(),
                properties.bucket.export,
            )
        }
    }

    fun planBiJobs(db: Database.Connection, clock: EvakaClock) {
        logger.info { "Planning BI jobs for ${biTables.size} tables" }
        db.transaction { tx ->
            tx.removeUnclaimedJobs(setOf(AsyncJobType(TampereAsyncJob.SendBiTable::class)))
            asyncJobRunner.plan(
                tx,
                biTables.asSequence().map(TampereAsyncJob::SendBiTable),
                runAt = clock.now(),
                retryCount = 1,
            )
        }
    }

    fun archiveEligibleDocuments(db: Database.Connection, clock: EvakaClock) {
        planDocumentArchival(
            db,
            clock,
            coreAsyncJobRunner,
            properties.archival?.schedule ?: error("No archival configuration available"),
        )
    }
}
