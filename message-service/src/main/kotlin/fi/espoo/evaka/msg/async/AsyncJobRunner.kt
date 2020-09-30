// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.async

import fi.espoo.evaka.msg.utils.exhaust
import fi.espoo.evaka.msg.utils.handle
import fi.espoo.evaka.msg.utils.transaction
import fi.espoo.voltti.logging.MdcKey
import mu.KotlinLogging
import org.jdbi.v3.core.Jdbi
import java.lang.reflect.UndeclaredThrowableException
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger { }

private const val threadPoolSize = 1
private const val defaultRetryCount = 48 * 60 / 15 // 48h when used with default 15 minute retry interval
private val defaultRetryInterval = Duration.ofMinutes(15)

private val noHandler = { msg: Any ->
    logger.warn("No job handler configured for $msg")
}

open class AsyncJobRunner(private val jdbi: Jdbi) : AutoCloseable {
    private val executor: ScheduledThreadPoolExecutor = ScheduledThreadPoolExecutor(threadPoolSize)
    private var periodicRunner: ScheduledFuture<*>? = null
    private val runningCount: AtomicInteger = AtomicInteger(0)

    @Volatile
    var sendMessage: (msg: SendMessage) -> Unit = noHandler

    fun plan(
        payloads: Iterable<AsyncJobPayload>,
        retryCount: Int = defaultRetryCount,
        retryInterval: Duration = defaultRetryInterval,
        runAt: Instant = Instant.now()
    ) {
        jdbi.transaction { h ->
            payloads.forEach { payload ->
                h.insertJob(
                    JobParams(
                        payload = payload,
                        retryCount = retryCount,
                        retryInterval = retryInterval,
                        runAt = runAt
                    )
                )
            }
        }
    }

    fun scheduleImmediateRun(maxCount: Int = 1_000) {
        executor.execute { this.runPendingJobs(maxCount) }
    }

    fun schedulePeriodicRun(pollingInterval: Duration, maxCount: Int = 1_000) {
        this.periodicRunner?.cancel(false)
        if (!pollingInterval.isZero && !pollingInterval.isNegative) {
            this.periodicRunner =
                this.executor.scheduleWithFixedDelay(
                    { this.runPendingJobs(maxCount) },
                    0,
                    pollingInterval.toNanos(),
                    TimeUnit.NANOSECONDS
                )
        } else {
            this.periodicRunner = null
        }
    }

    fun getRunningCount(): Int = runningCount.get()

    fun getPendingJobCount(types: Collection<AsyncJobType> = AsyncJobType.values().toList()): Int =
        jdbi.handle { h -> h.getPendingJobCount(types) }

    fun waitUntilNoRunningJobs(timeout: Duration = Duration.ofSeconds(10)) {
        val start = Instant.now()
        do {
            if (getRunningCount() == 0) return
            TimeUnit.MILLISECONDS.sleep(100)
        } while (Duration.between(start, Instant.now()).abs() < timeout)
        error { "Timed out while waiting for running jobs to finish" }
    }

    private fun runPendingJobs(maxCount: Int) {
        jdbi.handle { h ->
            var remaining = maxCount
            do {
                val job = h.transaction { it.claimJob() }?.also(this::runPendingJob)
                remaining -= 1
            } while (job != null && remaining > 0)
        }
    }

    private fun runPendingJob(job: ClaimedJobRef) {
        try {
            MdcKey.TRACE_ID.set(job.jobId.toString())
            MdcKey.SPAN_ID.set(job.jobId.toString())
            runningCount.incrementAndGet()
            logger.info { "Running async job $job" }
            val completed = when (job.jobType) {
                AsyncJobType.SEND_MESSAGE -> runJob(job, this.sendMessage)
            }.exhaust()
            if (completed) {
                logger.info { "Completed async job $job" }
            } else {
                logger.info { "Skipped async job $job due to contention" }
            }
        } catch (e: Throwable) {
            val exception = (e as? UndeclaredThrowableException)?.cause ?: e
            logger.error(exception) { "Failed to run async job $job" }
        } finally {
            runningCount.decrementAndGet()
            MdcKey.SPAN_ID.unset()
            MdcKey.TRACE_ID.unset()
            MdcKey.USER_ID.unset()
        }
    }

    private inline fun <reified T : AsyncJobPayload> runJob(job: ClaimedJobRef, crossinline f: (msg: T) -> Unit) =
        jdbi.handle { tx ->
            tx.startJob(job, T::class.java)?.let { msg ->
                f(msg)
                tx.completeJob(job)
                true
            } ?: false
        }

    override fun close() {
        this.executor.shutdown()
        this.executor.awaitTermination(10, TimeUnit.SECONDS)
        this.executor.shutdownNow()
    }
}
