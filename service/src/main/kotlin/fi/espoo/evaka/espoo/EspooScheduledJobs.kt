// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.espoo

import com.fasterxml.jackson.databind.json.JsonMapper
import fi.espoo.evaka.LinkityEnv
import fi.espoo.evaka.ScheduledJobsEnv
import fi.espoo.evaka.espoo.bi.EspooBiTable
import fi.espoo.evaka.linkity.LinkityHttpClient
import fi.espoo.evaka.linkity.generateDateRangesForStaffAttendancePlanRequests
import fi.espoo.evaka.linkity.sendStaffAttendancesToLinkity
import fi.espoo.evaka.reports.patu.PatuReportingService
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.AsyncJobType
import fi.espoo.evaka.shared.async.removeUnclaimedJobs
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.job.JobSchedule
import fi.espoo.evaka.shared.job.ScheduledJobDefinition
import fi.espoo.evaka.shared.job.ScheduledJobSettings
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalTime

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
        ScheduledJobSettings(enabled = true, schedule = JobSchedule.daily(LocalTime.of(1, 0))),
    ),
    GetStaffAttendancePlansFromLinkity(
        EspooScheduledJobs::getStaffAttendancePlansFromLinkity,
        ScheduledJobSettings(enabled = true, schedule = JobSchedule.daily(LocalTime.of(1, 30))),
    ),
    SendStaffAttendancesToLinkity(
        EspooScheduledJobs::sendStaffAttendancesToLinkity,
        ScheduledJobSettings(enabled = true, schedule = JobSchedule.daily(LocalTime.of(9, 22))),
    ),
}

class EspooScheduledJobs(
    private val patuReportingService: PatuReportingService,
    private val espooAsyncJobRunner: AsyncJobRunner<EspooAsyncJob>,
    env: ScheduledJobsEnv<EspooScheduledJob>,
    private val linkityEnv: LinkityEnv?,
    private val jsonMapper: JsonMapper,
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
        val tables = EspooBiTable.values()
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

    fun getStaffAttendancePlansFromLinkity(db: Database.Connection, clock: EvakaClock) {
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
        sendStaffAttendancesToLinkity(period, db, linkityClient)
    }
}
