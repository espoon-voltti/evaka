// SPDX-FileCopyrightText: 2026 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.kangasala

import evaka.core.OphEnv
import evaka.core.ScheduledJobsEnv
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.job.JobSchedule
import evaka.core.shared.job.ScheduledJobDefinition
import evaka.core.shared.job.ScheduledJobSettings
import evaka.trevaka.archival.planDocumentArchival
import evaka.trevaka.export.ChildDocumentTransferType
import evaka.trevaka.export.exportChildDocumentsViaSftp
import java.time.LocalTime

enum class KangasalaScheduledJob(
    val fn: (KangasalaScheduledJobs, Database.Connection, EvakaClock) -> Unit,
    val defaultSettings: ScheduledJobSettings,
) {
    ExportPreschoolToPrimaryChildDocuments(
        KangasalaScheduledJobs::exportPreschoolToPrimaryChildDocuments,
        ScheduledJobSettings(enabled = false, schedule = JobSchedule.cron("0 0 0 1 8 ?")),
    ),
    PlanDocumentArchival(
        KangasalaScheduledJobs::archiveEligibleDocuments,
        ScheduledJobSettings(enabled = false, schedule = JobSchedule.daily(LocalTime.of(20, 0))),
    ),
}

class KangasalaScheduledJobs(
    private val ophEnv: OphEnv,
    private val coreAsyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val properties: KangasalaProperties,
    env: ScheduledJobsEnv<KangasalaScheduledJob>,
) : JobSchedule {

    override val jobs: List<ScheduledJobDefinition> =
        env.jobs.map {
            ScheduledJobDefinition(it.key, it.value) { db, clock -> it.key.fn(this, db, clock) }
        }

    fun exportPreschoolToPrimaryChildDocuments(db: Database.Connection, clock: EvakaClock) {
        val primus = properties.primus ?: error("Primus not configured")
        exportChildDocumentsViaSftp(
            db,
            clock,
            ophEnv.municipalityCode,
            primus,
            ChildDocumentTransferType.PRESCHOOL_TO_PRIMARY,
        )
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
