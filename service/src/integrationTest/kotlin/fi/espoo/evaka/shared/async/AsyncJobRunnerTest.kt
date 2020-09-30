// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.async

import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.config.SharedIntegrationTestConfig
import fi.espoo.evaka.shared.db.runAfterCommit
import fi.espoo.evaka.shared.db.withSpringTx
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

@SpringJUnitConfig(SharedIntegrationTestConfig::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AsyncJobRunnerTest {
    private lateinit var asyncJobRunner: AsyncJobRunner

    @Autowired
    lateinit var jdbi: Jdbi

    @Autowired
    lateinit var dataSource: DataSource

    @Autowired
    lateinit var txManager: PlatformTransactionManager

    private val user = AuthenticatedUser(UUID.randomUUID(), setOf())

    @BeforeAll
    fun setup() {
        asyncJobRunner = AsyncJobRunner(jdbi, dataSource, txManager, syncMode = false)
    }

    @BeforeEach
    @AfterAll
    fun clean() {
        asyncJobRunner.notifyDecisionCreated = { _ -> }
        jdbi.open().use { h -> h.execute("TRUNCATE async_job") }
    }

    @Test
    fun testPlanRollback() {
        assertThrows<LetsRollbackException> {
            withSpringTx(txManager) {
                asyncJobRunner.plan(listOf(NotifyDecisionCreated(UUID.randomUUID(), user)))
                throw LetsRollbackException()
            }
        }
        assertEquals(0, asyncJobRunner.getPendingJobCount())
    }

    @Test
    fun testCompleteHappyCase() {
        val decisionId = UUID.randomUUID()
        val future = this.setAsyncJobCallback { msg ->
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive())
            msg
        }
        withSpringTx(txManager) {
            asyncJobRunner.plan(listOf(NotifyDecisionCreated(decisionId, user)))
            runAfterCommit { asyncJobRunner.scheduleImmediateRun() }
        }
        val result = future.get(10, TimeUnit.SECONDS)
        asyncJobRunner.waitUntilNoRunningJobs()
        assertEquals(decisionId, result.decisionId)
    }

    @Test
    fun testCompleteRetry() {
        val decisionId = UUID.randomUUID()
        val failingFuture = this.setAsyncJobCallback { _ -> throw LetsRollbackException() }
        withSpringTx(txManager) {
            asyncJobRunner.plan(listOf(NotifyDecisionCreated(decisionId, user)), 20, Duration.ZERO)
            runAfterCommit { asyncJobRunner.scheduleImmediateRun(maxCount = 1) }
        }

        val exception = assertThrows<ExecutionException> { failingFuture.get(10, TimeUnit.SECONDS) }
        assertTrue(exception.cause is LetsRollbackException)
        asyncJobRunner.waitUntilNoRunningJobs()
        assertEquals(1, asyncJobRunner.getPendingJobCount())

        val future = this.setAsyncJobCallback { msg -> msg }
        asyncJobRunner.scheduleImmediateRun(maxCount = 1)
        future.get(10, TimeUnit.SECONDS)
        asyncJobRunner.waitUntilNoRunningJobs()
    }

    private fun <R> setAsyncJobCallback(f: (msg: NotifyDecisionCreated) -> R): Future<R> {
        val future = CompletableFuture<R>()
        asyncJobRunner.notifyDecisionCreated = { msg ->
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
