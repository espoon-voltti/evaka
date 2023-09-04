// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.espoo

import fi.espoo.evaka.ScheduledJobsEnv
import fi.espoo.evaka.reports.patu.PatuReportingService
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.job.JobSchedule
import fi.espoo.evaka.shared.job.ScheduledJobDefinition
import fi.espoo.evaka.shared.job.ScheduledJobSettings
import java.time.LocalTime
import mu.KotlinLogging

enum class EspooScheduledJob(
    val fn: (EspooScheduledJobs, Database.Connection, EvakaClock) -> Unit,
    val defaultSettings: ScheduledJobSettings
) {
    SendPatuReport(
        EspooScheduledJobs::sendPatuReport,
        ScheduledJobSettings(enabled = false, schedule = JobSchedule.daily(LocalTime.of(6, 0)))
    )
}

class EspooScheduledJobs(
    private val patuReportingService: PatuReportingService,
    env: ScheduledJobsEnv<EspooScheduledJob>
) : JobSchedule {
    override val jobs: List<ScheduledJobDefinition> =
        env.jobs.map {
            ScheduledJobDefinition(it.key, it.value) { db, clock -> it.key.fn(this, db, clock) }
        }
    private val logger = KotlinLogging.logger {}

    fun sendPatuReport(db: Database.Connection, clock: EvakaClock) {
        val fourtyDaysAgo = clock.today().minusDays(40)
        logger.info("Sending patu report for $fourtyDaysAgo")
        patuReportingService.sendPatuReport(db, DateRange(fourtyDaysAgo, fourtyDaysAgo))
    }
}
