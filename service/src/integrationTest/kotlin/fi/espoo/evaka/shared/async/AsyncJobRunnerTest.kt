// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.async

import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.config.getTestDataSource
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.configureJdbi
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AsyncJobRunnerTest {
    private data class TestJob(val data: UUID) : AsyncJobPayload {
        override val user: AuthenticatedUser? = null
    }

    private lateinit var asyncJobRunner: AsyncJobRunner<TestJob>
    private lateinit var jdbi: Jdbi
    private lateinit var db: Database

    private val currentCallback: AtomicReference<(msg: TestJob) -> Unit> = AtomicReference()

    @BeforeAll
    fun setup() {
        jdbi = configureJdbi(Jdbi.create(getTestDataSource()))
    }

    @AfterEach
    fun afterEach() {
        asyncJobRunner.close()
        currentCallback.set(null)
    }

    @BeforeEach
    fun clean() {
        asyncJobRunner = AsyncJobRunner(jdbi, syncMode = false)
        asyncJobRunner.registerHandler { _, msg: TestJob ->
            currentCallback.get()(msg)
        }
        jdbi.open().use { h -> h.execute("TRUNCATE async_job") }
        db = Database(jdbi)
    }

    @Test
    fun testPlanRollback() {
        assertThrows<LetsRollbackException> {
            db.transaction { tx ->
                asyncJobRunner.plan(tx, listOf(TestJob(UUID.randomUUID())))
                throw LetsRollbackException()
            }
        }
        assertEquals(0, asyncJobRunner.getPendingJobCount())
    }

    @Test
    fun testCompleteHappyCase() {
        val id = UUID.randomUUID()
        val future = this.setAsyncJobCallback { msg -> msg }
        db.transaction { asyncJobRunner.plan(it, listOf(TestJob(id))) }
        asyncJobRunner.scheduleImmediateRun(maxCount = 1)
        val result = future.get(10, TimeUnit.SECONDS)
        asyncJobRunner.waitUntilNoRunningJobs()
        assertEquals(id, result.data)
    }

    @Test
    fun testCompleteRetry() {
        val id = UUID.randomUUID()
        val failingFuture = this.setAsyncJobCallback { throw LetsRollbackException() }
        db.transaction { asyncJobRunner.plan(it, listOf(TestJob(id)), 20, Duration.ZERO) }
        asyncJobRunner.scheduleImmediateRun(maxCount = 1)

        val exception = assertThrows<ExecutionException> { failingFuture.get(10, TimeUnit.SECONDS) }
        assertTrue(exception.cause is LetsRollbackException)
        asyncJobRunner.waitUntilNoRunningJobs()
        assertEquals(1, asyncJobRunner.getPendingJobCount())

        val future = this.setAsyncJobCallback { msg -> msg }
        asyncJobRunner.scheduleImmediateRun(maxCount = 1)
        future.get(10, TimeUnit.SECONDS)
        asyncJobRunner.waitUntilNoRunningJobs()
    }

    private fun <R> setAsyncJobCallback(f: (msg: TestJob) -> R): Future<R> {
        val future = CompletableFuture<R>()
        currentCallback.set { msg: TestJob ->
            try {
                future.complete(f(msg))
            } catch (t: Throwable) {
                future.completeExceptionally(t)
                throw t
            }
        }
        return future
    }
}

class LetsRollbackException : RuntimeException()
