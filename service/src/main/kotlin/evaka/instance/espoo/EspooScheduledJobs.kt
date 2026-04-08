// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.espoo

import evaka.core.EvakaEnv
import evaka.core.ScheduledJobsEnv
import evaka.core.linkity.LinkityHttpClient
import evaka.core.linkity.generateDateRangesForStaffAttendancePlanRequests
import evaka.core.linkity.sendStaffAttendancesToLinkity
import evaka.core.reports.patu.PatuReportingService
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.async.AsyncJobType
import evaka.core.shared.async.removeUnclaimedJobs
import evaka.core.shared.db.Database
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.job.JobSchedule
import evaka.core.shared.job.ScheduledJobDefinition
import evaka.core.shared.job.ScheduledJobSettings
import evaka.instance.espoo.bi.EspooBiJob
import evaka.instance.espoo.bi.EspooBiTable
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalTime
import tools.jackson.databind.json.JsonMapper

enum class EspooScheduledJob(
    val fn: (EspooScheduledJobs, Database.Connection, EvakaClock) -> Unit,
    val defaultSettings: ScheduledJobSettings,
) {
    SendPatuReport(
        EspooScheduledJobs::sendPatuReport,
        ScheduledJobSettings(enabled = false, schedule = JobSchedule.daily(LocalTime.of(6, 0))),
    ),
    PlanBiJobs(
        EspooScheduledJobs::planBiJobs,
        ScheduledJobSettings(enabled = true, schedule = JobSchedule.nightly()),
    ),
    PlanStaffAttendancePlanJobs(
        EspooScheduledJobs::planStaffAttendancePlanJobs,
        ScheduledJobSettings(enabled = true, schedule = JobSchedule.nightly(), retryCount = 1),
    ),
    SendStaffAttendancesToLinkity(
        EspooScheduledJobs::sendStaffAttendancesToLinkity,
        ScheduledJobSettings(enabled = true, schedule = JobSchedule.nightly()),
    ),
}

class EspooScheduledJobs(
    private val patuReportingService: PatuReportingService,
    private val espooAsyncJobRunner: AsyncJobRunner<EspooAsyncJob>,
    env: ScheduledJobsEnv<EspooScheduledJob>,
    private val linkityEnv: LinkityEnv?,
    private val jsonMapper: JsonMapper,
    private val espooBiJob: EspooBiJob?,
    private val evakaEnv: EvakaEnv,
) : JobSchedule {
    override val jobs: List<ScheduledJobDefinition> =
        env.jobs.map {
            ScheduledJobDefinition(it.key, it.value) { db, clock -> it.key.fn(this, db, clock) }
        }
    private val logger = KotlinLogging.logger {}

    fun sendPatuReport(db: Database.Connection, clock: EvakaClock) {
        val fourtyDaysAgo = clock.today().minusDays(40)
        logger.info { "Sending patu report for $fourtyDaysAgo" }
        patuReportingService.sendPatuReport(db, DateRange(fourtyDaysAgo, fourtyDaysAgo))
    }

    fun planBiJobs(db: Database.Connection, clock: EvakaClock) {
        if (espooBiJob == null) {
            logger.info { "BI integration not configured, skipping" }
            return
        }
        val tables = EspooBiTable.entries
        logger.info { "Planning BI jobs for ${tables.size} tables" }
        db.transaction { tx ->
            tx.removeUnclaimedJobs(setOf(AsyncJobType(EspooAsyncJob.SendBiTable::class)))
            espooAsyncJobRunner.plan(
                tx,
                tables.asSequence().map(EspooAsyncJob::SendBiTable),
                runAt = clock.now(),
                retryCount = 1,
            )
        }
    }

    fun planStaffAttendancePlanJobs(db: Database.Connection, clock: EvakaClock) {
        val weeksAhead = 6L
        val daysInSingleRun = 7L
        val startDate = clock.today()
        val endDate = startDate.plusWeeks(weeksAhead).minusDays(1)

        logger.info {
            "Scheduling Linkity shifts fetch from $startDate to $endDate in $daysInSingleRun day chunks"
        }
        db.transaction { tx ->
            val dateRanges =
                generateDateRangesForStaffAttendancePlanRequests(
                        startDate,
                        endDate,
                        daysInSingleRun,
                    )
                    .map { EspooAsyncJob.GetStaffAttendancePlansFromLinkity(it) }

            espooAsyncJobRunner.plan(tx, dateRanges, runAt = clock.now())
        }
    }

    fun sendStaffAttendancesToLinkity(db: Database.Connection, clock: EvakaClock) {
        val today = clock.today()
        val period = FiniteDateRange(today.minusWeeks(6), today)
        if (linkityEnv == null) {
            logger.warn { "Linkity environment not configured" }
            return
        }
        val linkityClient = LinkityHttpClient(linkityEnv, jsonMapper)
        logger.info { "Sending staff attendances to Linkity for period $period" }
        sendStaffAttendancesToLinkity(
            period,
            db,
            linkityClient,
            evakaEnv.staffAttendanceDriftMinutes,
        )
    }
}
