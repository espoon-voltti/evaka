// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.job

import com.github.kagkarlsson.scheduler.ScheduledExecution
import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.RunScheduledJob
import fi.espoo.evaka.shared.db.Database
import fi.espoo.voltti.logging.loggers.info
import mu.KotlinLogging
import org.jdbi.v3.core.Jdbi
import java.time.Duration
import javax.sql.DataSource

private const val SCHEDULER_THREADS = 1
private const val ASYNC_JOB_RETRY_COUNT = 12
private val POLLING_INTERVAL = Duration.ofMinutes(1)

private val logger = KotlinLogging.logger { }

class ScheduledJobRunner(
    private val jdbi: Jdbi,
    private val asyncJobRunner: AsyncJobRunner,
    dataSource: DataSource,
    schedule: JobSchedule
) : AutoCloseable {
    val scheduler: Scheduler = Scheduler.create(dataSource)
        .startTasks(
            ScheduledJob.values()
                .mapNotNull { job -> schedule.getScheduleForJob(job)?.let { Pair(job, it) } }
                .map { (job, schedule) ->
                    val logMeta = mapOf("jobName" to job.name)
                    logger.info(logMeta) { "Scheduling job ${job.name}: $schedule" }
                    Tasks.recurring(job.name, schedule).execute { _, _ ->
                        Database(jdbi).connect { this.planAsyncJob(it, job) }
                    }
                }
        )
        .threads(SCHEDULER_THREADS)
        .pollingInterval(POLLING_INTERVAL)
        .build()

    private fun planAsyncJob(db: Database.Connection, job: ScheduledJob) {
        val logMeta = mapOf("jobName" to job.name)
        logger.info(logMeta) { "Planning scheduled job ${job.name}" }
        db.transaction { tx ->
            asyncJobRunner.plan(tx, listOf(RunScheduledJob(job)), retryCount = ASYNC_JOB_RETRY_COUNT)
        }
        asyncJobRunner.scheduleImmediateRun()
    }

    fun getScheduledExecutionsForTask(job: ScheduledJob): List<ScheduledExecution<Unit>> =
        scheduler.getScheduledExecutionsForTask(job.name, Unit::class.java)

    override fun close() = scheduler.stop()
}
