// SPDX-FileCopyrightText: 2026 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.nokiankaupunki

import evaka.core.ScheduledJobsEnv
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.job.JobSchedule
import evaka.core.shared.job.ScheduledJobDefinition
import evaka.core.shared.job.ScheduledJobSettings
import evaka.trevaka.archival.planDocumentArchival
import java.time.LocalTime

enum class NokiaScheduledJob(
    val fn: (NokiaScheduledJobs, Database.Connection, EvakaClock) -> Unit,
    val defaultSettings: ScheduledJobSettings,
) {
    PlanDocumentArchival(
        NokiaScheduledJobs::archiveEligibleDocuments,
        ScheduledJobSettings(enabled = false, schedule = JobSchedule.daily(LocalTime.of(20, 0))),
    )
}

class NokiaScheduledJobs(
    private val coreAsyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val properties: NokiaProperties,
    env: ScheduledJobsEnv<NokiaScheduledJob>,
) : JobSchedule {

    override val jobs: List<ScheduledJobDefinition> =
        env.jobs.map {
            ScheduledJobDefinition(it.key, it.value) { db, clock -> it.key.fn(this, db, clock) }
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
