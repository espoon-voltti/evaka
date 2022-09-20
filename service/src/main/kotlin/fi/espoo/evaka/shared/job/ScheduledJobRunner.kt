// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.job

import com.github.kagkarlsson.scheduler.ScheduledExecution
import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.voltti.logging.loggers.info
import java.time.Duration
import javax.sql.DataSource
import mu.KotlinLogging
import org.jdbi.v3.core.Jdbi

private const val SCHEDULER_THREADS = 1
private const val ASYNC_JOB_RETRY_COUNT = 12
private val POLLING_INTERVAL = Duration.ofMinutes(1)

private val logger = KotlinLogging.logger {}

class ScheduledJobRunner(
    private val jdbi: Jdbi,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    dataSource: DataSource,
    schedule: JobSchedule
) : AutoCloseable {
    val scheduler: Scheduler =
        Scheduler.create(dataSource)
            .startTasks(
                ScheduledJob.values()
                    .mapNotNull { job ->
                        val settings = schedule.getSettingsForJob(job)
                        if (settings?.enabled == true) job to settings else null
                    }
                    .map { (job, settings) ->
                        val logMeta = mapOf("jobName" to job.name)
                        logger.info(logMeta) { "Scheduling job ${job.name}: $schedule" }
                        Tasks.recurring(job.name, settings.schedule).execute { _, _ ->
                            Database(jdbi).connect {
                                this.planAsyncJob(
                                    it,
                                    job,
                                    settings.retryCount ?: ASYNC_JOB_RETRY_COUNT
                                )
                            }
                        }
                    }
            )
            .threads(SCHEDULER_THREADS)
            .pollingInterval(POLLING_INTERVAL)
            .build()

    private fun planAsyncJob(db: Database.Connection, job: ScheduledJob, retryCount: Int) {
        val logMeta = mapOf("jobName" to job.name)
        logger.info(logMeta) { "Planning scheduled job ${job.name}" }
        db.transaction { tx ->
            asyncJobRunner.plan(
                tx,
                listOf(AsyncJob.RunScheduledJob(job)),
                retryCount = retryCount,
                runAt = HelsinkiDateTime.now()
            )
        }
        asyncJobRunner.wakeUp()
    }

    fun getScheduledExecutionsForTask(job: ScheduledJob): List<ScheduledExecution<Unit>> =
        scheduler.getScheduledExecutionsForTask(job.name, Unit::class.java)

    override fun close() = scheduler.stop()
}
