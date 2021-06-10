// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.daily

import com.github.kagkarlsson.scheduler.ScheduledExecution
import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.Daily
import fi.espoo.evaka.application.utils.helsinkiZone
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.RunDailyJob
import fi.espoo.evaka.shared.db.Database
import org.jdbi.v3.core.Jdbi
import java.time.Duration
import javax.sql.DataSource

private const val SCHEDULER_THREADS = 1
private const val ASYNC_JOB_RETRY_COUNT = 12
private val POLLING_INTERVAL = Duration.ofMinutes(1)

class DailyJobRunner(
    private val jdbi: Jdbi,
    private val asyncJobRunner: AsyncJobRunner,
    dataSource: DataSource,
    schedule: DailySchedule
) : AutoCloseable {
    val scheduler = Scheduler.create(dataSource)
        .startTasks(
            DailyJob.values()
                .mapNotNull { job -> schedule.getTimeForJob(job)?.let { Pair(job, it) } }
                .map { (job, time) ->
                    Tasks.recurring(job.name, Daily(helsinkiZone, time)).execute { _, _ ->
                        Database(jdbi).connect { this.planAsyncJob(it, job) }
                    }
                }
        )
        .threads(SCHEDULER_THREADS)
        .pollingInterval(POLLING_INTERVAL)
        .build()

    private fun planAsyncJob(db: Database.Connection, job: DailyJob) {
        db.transaction { tx ->
            asyncJobRunner.plan(tx, listOf(RunDailyJob(job)), retryCount = ASYNC_JOB_RETRY_COUNT)
        }
        asyncJobRunner.scheduleImmediateRun()
    }

    fun getScheduledExecutionsForTask(job: DailyJob): List<ScheduledExecution<Unit>> =
        scheduler.getScheduledExecutionsForTask(job.name, Unit::class.java)

    override fun close() = scheduler.stop()
}
