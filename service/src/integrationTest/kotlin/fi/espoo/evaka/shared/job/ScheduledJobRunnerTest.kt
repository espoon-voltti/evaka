// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.job

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.application.utils.helsinkiZone
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.config.getTestDataSource
import fi.espoo.evaka.shared.dev.resetDatabase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ScheduledJobRunnerTest : PureJdbiTest() {
    private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>
    private val testTime = LocalTime.of(1, 0)
    private val testSchedule = object : JobSchedule {
        override fun getSettingsForJob(job: ScheduledJob): ScheduledJobSettings? =
            if (job == ScheduledJob.EndOfDayAttendanceUpkeep) {
                ScheduledJobSettings(
                    enabled = true,
                    schedule = JobSchedule.daily(testTime)
                )
            } else null
    }

    @BeforeEach
    fun beforeEach() {
        db.transaction { it.resetDatabase() }
        asyncJobRunner = AsyncJobRunner(jdbi)
    }

    @Test
    fun `a job specified by DailySchedule is scheduled and executed correctly`() {
        val executedJob = AtomicReference<ScheduledJob?>(null)
        asyncJobRunner.registerHandler { _, msg: AsyncJob.RunScheduledJob ->
            val previous = executedJob.getAndSet(msg.job)
            assertNull(previous)
        }
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

        asyncJobRunner.runPendingJobsSync()
        assertEquals(executedJob.get(), ScheduledJob.EndOfDayAttendanceUpkeep)
    }
}
