// SPDX-FileCopyrightText: 2026 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.ylojarvi

import evaka.core.OphEnv
import evaka.core.ScheduledJobsEnv
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.job.JobSchedule
import evaka.core.shared.job.ScheduledJobDefinition
import evaka.core.shared.job.ScheduledJobSettings
import evaka.trevaka.export.ChildDocumentTransferType
import evaka.trevaka.export.exportChildDocumentsViaSftp

enum class YlojarviScheduledJob(
    val fn: (YlojarviScheduledJobs, Database.Connection, EvakaClock) -> Unit,
    val defaultSettings: ScheduledJobSettings,
) {
    ExportPreschoolToPrimaryChildDocuments(
        YlojarviScheduledJobs::exportPreschoolToPrimaryChildDocuments,
        ScheduledJobSettings(enabled = false, schedule = JobSchedule.cron("0 0 0 1 8 ?")),
    )
}

class YlojarviScheduledJobs(
    private val ophEnv: OphEnv,
    private val properties: YlojarviProperties,
    env: ScheduledJobsEnv<YlojarviScheduledJob>,
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
}
