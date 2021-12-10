// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.async

import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.config.getTestDataSource
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.configureJdbi
import fi.espoo.voltti.logging.MdcKey
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AsyncJobRunnerTest {
    private data class TestJob(
        val data: UUID = UUID.randomUUID(),
        override val user: AuthenticatedUser? = null
    ) : AsyncJobPayload

    private class LetsRollbackException : RuntimeException()

    private lateinit var asyncJobRunner: AsyncJobRunner<TestJob>
    private lateinit var jdbi: Jdbi
    private lateinit var db: Database.Connection

    private val currentCallback: AtomicReference<(msg: TestJob) -> Unit> = AtomicReference()

    @BeforeAll
    fun setup() {
        jdbi = configureJdbi(Jdbi.create(getTestDataSource()))
    }

    @AfterEach
    fun afterEach() {
        asyncJobRunner.close()
        currentCallback.set(null)
        db.close()
    }

    @BeforeEach
    fun clean() {
        asyncJobRunner = AsyncJobRunner(jdbi, AsyncJobRunnerConfig())
        asyncJobRunner.registerHandler { _, msg: TestJob ->
            currentCallback.get()(msg)
        }
        jdbi.open().use { h -> h.execute("TRUNCATE async_job") }
        db = Database(jdbi).connectWithManualLifecycle()
    }

    @Test
    fun `planned jobs are not saved if the transaction gets rolled back`() {
        assertThrows<LetsRollbackException> {
            db.transaction { tx ->
                asyncJobRunner.plan(tx, listOf(TestJob(UUID.randomUUID())))
                throw LetsRollbackException()
            }
        }
        assertEquals(0, asyncJobRunner.getPendingJobCount())
    }

    @Test
    fun `logging MDC keys are set during job execution`() {
        val job = TestJob(user = AuthenticatedUser.SystemInternalUser)
        val future = this.setAsyncJobCallback {
            assertEquals(job, it)
            val traceId = MdcKey.TRACE_ID.get()
            val spanId = MdcKey.SPAN_ID.get()
            assertNotNull(traceId)
            assertEquals(traceId, spanId)
            assertEquals(AuthenticatedUser.SystemInternalUser.id.toString(), MdcKey.USER_ID.get())
            assertEquals(AuthenticatedUser.SystemInternalUser.idHash.toString(), MdcKey.USER_ID_HASH.get())
        }
        db.transaction { asyncJobRunner.plan(it, listOf(job)) }
        asyncJobRunner.runPendingJobsSync(1)
        future.get(0, TimeUnit.SECONDS)
    }

    @Test
    fun `pending jobs can be executed manually`() {
        val job = TestJob()
        val future = this.setAsyncJobCallback { assertEquals(job, it) }
        db.transaction { asyncJobRunner.plan(it, listOf(job)) }
        asyncJobRunner.runPendingJobsSync()
        future.get(0, TimeUnit.SECONDS)
    }

    @Test
    fun `pending jobs are executed in the background if AsyncJobRunner has been started`() {
        val job = TestJob()
        val future = this.setAsyncJobCallback { assertEquals(job, it) }
        db.transaction { asyncJobRunner.plan(it, listOf(job)) }
        asyncJobRunner.start(pollingInterval = Duration.ofSeconds(1))
        future.get(10, TimeUnit.SECONDS)
    }

    @Test
    fun `AsyncJobRunner wakes up automatically after a transaction plans jobs`() {
        val job = TestJob()
        val future = this.setAsyncJobCallback { assertEquals(job, it) }
        asyncJobRunner.start(pollingInterval = Duration.ofDays(1))
        TimeUnit.MILLISECONDS.sleep(100)
        db.transaction { asyncJobRunner.plan(it, listOf(job)) }
        future.get(10, TimeUnit.SECONDS)
    }

    @Test
    fun `failed jobs get retried`() {
        val job = TestJob()
        val failingFuture = this.setAsyncJobCallback { throw LetsRollbackException() }
        db.transaction { asyncJobRunner.plan(it, listOf(job), 20, Duration.ZERO) }
        asyncJobRunner.runPendingJobsSync(1)

        val exception = assertThrows<ExecutionException> { failingFuture.get(10, TimeUnit.SECONDS) }
        assertTrue(exception.cause is LetsRollbackException)
        assertEquals(1, asyncJobRunner.getPendingJobCount())

        val future = this.setAsyncJobCallback { assertEquals(job, it) }
        asyncJobRunner.runPendingJobsSync(1)
        future.get(10, TimeUnit.SECONDS)
        assertEquals(0, asyncJobRunner.getPendingJobCount())
    }

    private fun <R> setAsyncJobCallback(f: (msg: TestJob) -> R): Future<R> {
        val future = CompletableFuture<R>()
        currentCallback.set { msg: TestJob ->
            try {
                val completed = future.complete(f(msg))
                assertTrue(completed)
            } catch (t: Throwable) {
                future.completeExceptionally(t)
                throw t
            }
        }
        return future
    }
}
