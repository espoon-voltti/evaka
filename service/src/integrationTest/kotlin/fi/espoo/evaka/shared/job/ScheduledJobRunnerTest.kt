// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.job

import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.application.utils.helsinkiZone
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.config.getTestDataSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.util.concurrent.atomic.AtomicReference

class ScheduledJobRunnerTest : PureJdbiTest() {
    private lateinit var asyncJobRunner: AsyncJobRunner
    private val testTime = LocalTime.of(1, 0)
    private val testSchedule = object : JobSchedule {
        override fun getScheduleForJob(job: ScheduledJob): Schedule? = if (job == ScheduledJob.EndOfDayAttendanceUpkeep) {
            JobSchedule.daily(testTime)
        } else null
    }

    @BeforeEach
    fun beforeEach() {
        db.transaction { it.resetDatabase() }
        asyncJobRunner = AsyncJobRunner(jdbi, syncMode = true)
    }

    @Test
    fun `a job specified by DailySchedule is scheduled and executed correctly`() {
        ScheduledJobRunner(jdbi, asyncJobRunner, getTestDataSource(), testSchedule).use { runner ->
            runner.scheduler.start()
            val exec = runner.getScheduledExecutionsForTask(ScheduledJob.EndOfDayAttendanceUpkeep).singleOrNull()!!
            assertEquals(exec.executionTime.atZone(helsinkiZone).toLocalTime(), testTime)

            runner.scheduler.reschedule(exec.taskInstance, Instant.EPOCH)
            runner.scheduler.triggerCheckForDueExecutions()

            val start = Instant.now()
            while (asyncJobRunner.getPendingJobCount() == 0) {
                Thread.sleep(100)

                assert(Duration.between(start, Instant.now()) < Duration.ofSeconds(10))
            }
        }

        val executedJob = AtomicReference<ScheduledJob?>(null)
        asyncJobRunner.runScheduledJob = { _, msg ->
            val previous = executedJob.getAndSet(msg.job)
            assertNull(previous)
        }
        asyncJobRunner.runPendingJobsSync()
        assertEquals(executedJob.get(), ScheduledJob.EndOfDayAttendanceUpkeep)
    }
}
