// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg

import fi.espoo.evaka.msg.async.AsyncJobRunner
import fi.espoo.evaka.msg.async.SendMessage
import fi.espoo.evaka.msg.controllers.PdfSendMessage
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

private val testMsg = PdfSendMessage(
    messageId = UUID.randomUUID().toString(),
    documentUri = "",
    documentId = "",
    documentDisplayName = "",
    ssn = "",
    firstName = "",
    lastName = "",
    streetAddress = "",
    postalCode = "",
    postOffice = "",
    messageHeader = "",
    messageContent = "",
    emailHeader = null,
    emailContent = null
)

class AsyncJobRunnerTest : AbstractIntegrationTest() {
    private lateinit var asyncJobRunner: AsyncJobRunner

    @Autowired
    lateinit var jdbi: Jdbi

    @BeforeEach
    @AfterEach
    fun clean() {
        asyncJobRunner = AsyncJobRunner(jdbi)
        jdbi.open().use { h -> h.execute("TRUNCATE async_job") }
    }

    @Test
    fun testCompleteHappyCase() {
        val future = this.setAsyncJobCallback { msg -> msg }
        asyncJobRunner.plan(listOf(SendMessage(testMsg)))
        asyncJobRunner.scheduleImmediateRun()
        val result = future.get(10, TimeUnit.SECONDS)
        asyncJobRunner.waitUntilNoRunningJobs()
        assertEquals(testMsg, result.msg)
    }

    @Test
    fun testCompleteRetry() {
        val failingFuture = this.setAsyncJobCallback { _ -> throw LetsRollbackException() }
        asyncJobRunner.plan(listOf(SendMessage(testMsg)), 20, Duration.ZERO)
        asyncJobRunner.scheduleImmediateRun(maxCount = 1)

        try {
            failingFuture.get(10, TimeUnit.SECONDS)
            fail("Expected exception")
        } catch (e: ExecutionException) {
            assertTrue(e.cause is LetsRollbackException)
        }
        asyncJobRunner.waitUntilNoRunningJobs()
        assertEquals(1, asyncJobRunner.getPendingJobCount())

        val future = this.setAsyncJobCallback { msg -> msg }
        asyncJobRunner.scheduleImmediateRun(maxCount = 1)
        future.get(10, TimeUnit.SECONDS)
        asyncJobRunner.waitUntilNoRunningJobs()
    }

    private fun <R> setAsyncJobCallback(f: (msg: SendMessage) -> R): Future<R> {
        val future = CompletableFuture<R>()
        asyncJobRunner.sendMessage = { msg ->
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
