// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.daily

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

class DailyJobRunnerTest : PureJdbiTest() {
    private lateinit var asyncJobRunner: AsyncJobRunner
    private val testTime = LocalTime.of(1, 0)
    private val testSchedule = object : DailySchedule {
        override fun getTimeForJob(job: DailyJob): LocalTime? = if (job == DailyJob.EndOfDayAttendanceUpkeep) {
            testTime
        } else null
    }

    @BeforeEach
    fun beforeEach() {
        db.transaction { it.resetDatabase() }
        asyncJobRunner = AsyncJobRunner(jdbi, syncMode = true)
    }

    @Test
    fun `a job specified by DailySchedule is scheduled and executed correctly`() {
        DailyJobRunner(jdbi, asyncJobRunner, getTestDataSource(), testSchedule).use { runner ->
            runner.scheduler.start()
            val exec = runner.getScheduledExecutionsForTask(DailyJob.EndOfDayAttendanceUpkeep).singleOrNull()!!
            assertEquals(exec.executionTime.atZone(helsinkiZone).toLocalTime(), testTime)

            runner.scheduler.reschedule(exec.taskInstance, Instant.EPOCH)
            runner.scheduler.triggerCheckForDueExecutions()

            val start = Instant.now()
            while (asyncJobRunner.getPendingJobCount() == 0) {
                Thread.sleep(100)

                assert(Duration.between(start, Instant.now()) < Duration.ofSeconds(10))
            }
        }

        val executedJob = AtomicReference<DailyJob?>(null)
        asyncJobRunner.runDailyJob = { _, msg ->
            val previous = executedJob.getAndSet(msg.dailyJob)
            assertNull(previous)
        }
        asyncJobRunner.runPendingJobsSync()
        assertEquals(executedJob.get(), DailyJob.EndOfDayAttendanceUpkeep)
    }
}
