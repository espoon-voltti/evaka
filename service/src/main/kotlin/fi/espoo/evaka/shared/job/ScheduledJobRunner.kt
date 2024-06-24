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
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.withDetachedSpan
import fi.espoo.voltti.logging.loggers.info
import io.opentracing.Tracer
import java.time.Duration
import javax.sql.DataSource
import mu.KotlinLogging
import org.jdbi.v3.core.Jdbi

private const val SCHEDULER_THREADS = 1
private const val ASYNC_JOB_RETRY_COUNT = 12
private val POLLING_INTERVAL = Duration.ofMinutes(1)

class ScheduledJobRunner(
    private val jdbi: Jdbi,
    private val tracer: Tracer,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val schedules: List<JobSchedule>,
    dataSource: DataSource
) : AutoCloseable {
    private val logger = KotlinLogging.logger {}

    init {
        val jobsByName =
            schedules
                .asSequence()
                .flatMap { it.jobs }
                .map { it.job }
                .groupBy { it.name }
                .values
        val notUnique = jobsByName.filterNot { it.count() == 1 }
        require(notUnique.isEmpty()) {
            val jobNames =
                notUnique.joinToString { jobs ->
                    jobs.joinToString(prefix = "[", postfix = "]") {
                        "${it.javaClass.name}.${it.name}"
                    }
                }
            "Scheduled job name conflict: $jobNames"
        }
        asyncJobRunner.registerHandler(::runJob)
    }

    val scheduler: Scheduler =
        Scheduler
            .create(dataSource)
            .startTasks(
                schedules
                    .asSequence()
                    .flatMap { it.jobs }
                    .partition { it.settings.enabled }
                    .let { (enabled, disabled) ->
                        logger.info {
                            "Ignoring disabled jobs: ${disabled.joinToString { it.job.name }}"
                        }
                        enabled.map { definition ->
                            val logMeta = mapOf("jobName" to definition.job.name)
                            logger.info(logMeta) {
                                "Scheduling job ${definition.job.name}: ${definition.settings.schedule}"
                            }
                            Tasks
                                .recurring(definition.job.name, definition.settings.schedule)
                                .execute { _, _ ->
                                    Database(jdbi, tracer).connect {
                                        this.planAsyncJob(it, definition)
                                    }
                                }
                        }
                    }
            ).threads(SCHEDULER_THREADS)
            .pollingInterval(POLLING_INTERVAL)
            .pollUsingLockAndFetch(0.5, 1.0)
            .deleteUnresolvedAfter(Duration.ofHours(1))
            .build()

    private fun planAsyncJob(
        db: Database.Connection,
        definition: ScheduledJobDefinition
    ) {
        val (job, settings) = definition
        val logMeta = mapOf("jobName" to job.name)
        logger.info(logMeta) { "Planning scheduled job ${job.name}" }
        db.transaction { tx ->
            asyncJobRunner.plan(
                tx,
                listOf(AsyncJob.RunScheduledJob(job.name)),
                retryCount = settings.retryCount ?: ASYNC_JOB_RETRY_COUNT,
                runAt = HelsinkiDateTime.now()
            )
        }
    }

    private fun runJob(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.RunScheduledJob
    ) {
        val definition =
            schedules.firstNotNullOfOrNull { schedule ->
                schedule.jobs.find { it.job.name == msg.job }
            } ?: error("Can't run unknown job ${msg.job}")
        val logMeta = mapOf("jobName" to msg.job)
        logger.info(logMeta) { "Running scheduled job ${msg.job}" }
        tracer.withDetachedSpan("scheduledjob ${msg.job}") { definition.jobFn(db, clock) }
    }

    fun getScheduledExecutionsForTask(job: Enum<*>): List<ScheduledExecution<Unit>> =
        scheduler.getScheduledExecutionsForTask(job.name, Unit::class.java)

    override fun close() = scheduler.stop()
}
