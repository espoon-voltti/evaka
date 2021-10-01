// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.async

import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.voltti.logging.MdcKey
import fi.espoo.voltti.logging.loggers.error
import fi.espoo.voltti.logging.loggers.info
import mu.KotlinLogging
import org.jdbi.v3.core.Jdbi
import java.lang.reflect.UndeclaredThrowableException
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private val logger = KotlinLogging.logger { }

private const val threadPoolSize = 4
private const val defaultRetryCount = 24 * 60 / 5 // 24h when used with default 5 minute retry interval
private val defaultRetryInterval = Duration.ofMinutes(5)

private data class Registration<T : AsyncJobPayload>(val handler: (db: Database, msg: T) -> Unit) {
    fun run(db: Database, msg: AsyncJobPayload) =
        @Suppress("UNCHECKED_CAST")
        handler(db, msg as T)
}

class AsyncJobRunner<T : AsyncJobPayload>(private val jdbi: Jdbi) : AutoCloseable {
    private val executor: ScheduledThreadPoolExecutor = ScheduledThreadPoolExecutor(threadPoolSize)
    private val periodicRunner: AtomicReference<ScheduledFuture<*>> = AtomicReference()
    private val runningCount: AtomicInteger = AtomicInteger(0)

    private val handlersLock: Lock = ReentrantLock()
    private val handlers: ConcurrentHashMap<AsyncJobType<out T>, Registration<*>> = ConcurrentHashMap()
    private val wakeUpHook: () -> Unit = { wakeUp() }

    val isStarted: Boolean
        get() = periodicRunner.get() != null

    val isBusy: Boolean
        get() = runningCount.get() > 0

    inline fun <reified P : T> registerHandler(noinline handler: (db: Database.Connection, msg: P) -> Unit) =
        registerHandler(AsyncJobType(P::class)) { db, msg -> db.connect { handler(it, msg) } }

    fun <P : T> registerHandler(jobType: AsyncJobType<out P>, handler: (db: Database, msg: P) -> Unit): Unit = handlersLock.withLock {
        require(!handlers.containsKey(jobType)) { "handler for $jobType has already been registered" }
        val ambiguousKey = handlers.keys.find { it.name == jobType.name }
        require(ambiguousKey == null) { "handlers for $jobType and $ambiguousKey have a name conflict" }
        handlers[jobType] = Registration(handler)
    }

    fun plan(
        tx: Database.Transaction,
        payloads: Iterable<T>,
        retryCount: Int = defaultRetryCount,
        retryInterval: Duration = defaultRetryInterval,
        runAt: HelsinkiDateTime = HelsinkiDateTime.now()
    ) {
        payloads.forEach { payload ->
            tx.insertJob(
                JobParams(
                    payload = payload,
                    retryCount = retryCount,
                    retryInterval = retryInterval,
                    runAt = runAt
                )
            )
        }
        tx.afterCommit(wakeUpHook)
    }

    fun start(pollingInterval: Duration) {
        val newRunner = this.executor.scheduleWithFixedDelay(
            { this.runPendingJobs() },
            0,
            pollingInterval.toNanos(),
            TimeUnit.NANOSECONDS
        )
        this.periodicRunner.getAndSet(newRunner)?.cancel(false)
    }

    fun wakeUp() {
        if (isStarted) {
            executor.execute { this.runPendingJobs() }
        }
    }

    fun runPendingJobsSync(maxCount: Int = 1_000) {
        this.executor.submit { this.runPendingJobs(maxCount) }.get()
    }

    fun getPendingJobCount(): Int =
        Database(jdbi).read { it.getPendingJobCount(handlers.keys) }

    fun waitUntilNoRunningJobs(timeout: Duration = Duration.ofSeconds(10)) {
        val start = Instant.now()
        do {
            if (!isBusy) return
            TimeUnit.MILLISECONDS.sleep(100)
        } while (Duration.between(start, Instant.now()).abs() < timeout)
        error { "Timed out while waiting for running jobs to finish" }
    }

    private fun runPendingJobs(maxCount: Int = 1_000) {
        Database(jdbi).connect { db ->
            var remaining = maxCount
            do {
                val job = db.transaction { it.claimJob(handlers.keys) }
                if (job != null) {
                    runPendingJob(db, job)
                }
                remaining -= 1
            } while (job != null && remaining > 0)
        }
    }

    private fun runPendingJob(db: Database.Connection, job: ClaimedJobRef<out T>) {
        val logMeta = mapOf(
            "jobId" to job.jobId,
            "jobType" to job.jobType.name,
            "remainingAttempts" to job.remainingAttempts
        )
        try {
            MdcKey.TRACE_ID.set(job.jobId.toString())
            MdcKey.SPAN_ID.set(job.jobId.toString())
            runningCount.incrementAndGet()
            logger.info(logMeta) { "Running async job $job" }
            val completed = db.transaction { tx ->
                tx.setLockTimeout(Duration.ofSeconds(5))
                val registration = handlers[job.jobType] ?: throw IllegalStateException("No handler found for ${job.jobType}")
                tx.startJob(job)?.let { msg ->
                    msg.user?.let {
                        MdcKey.USER_ID.set(it.id.toString())
                        MdcKey.USER_ID_HASH.set(it.idHash.toString())
                    }
                    registration.run(Database(jdbi), msg)
                    tx.completeJob(job)
                    true
                } ?: false
            }
            if (completed) {
                logger.info(logMeta) { "Completed async job $job" }
            } else {
                logger.info(logMeta) { "Skipped async job $job due to contention" }
            }
        } catch (e: Throwable) {
            val exception = (e as? UndeclaredThrowableException)?.cause ?: e
            logger.error(exception, logMeta) { "Failed to run async job $job" }
        } finally {
            runningCount.decrementAndGet()
            MdcKey.USER_ID_HASH.unset()
            MdcKey.USER_ID.unset()
            MdcKey.SPAN_ID.unset()
            MdcKey.TRACE_ID.unset()
        }
    }

    override fun close() {
        this.executor.shutdown()
        this.executor.awaitTermination(10, TimeUnit.SECONDS)
        this.executor.shutdownNow()
    }
}

private fun Database.Transaction.setLockTimeout(duration: Duration) = createUpdate(
    "SET LOCAL lock_timeout = <durationInMs>"
).define("durationInMs", "'${duration.toMillis()}ms'").execute()
