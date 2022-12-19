// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.async

import fi.espoo.evaka.shared.Tracing
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.shared.randomTracingId
import fi.espoo.evaka.shared.withDetachedSpan
import fi.espoo.evaka.shared.withValue
import fi.espoo.voltti.logging.MdcKey
import fi.espoo.voltti.logging.loggers.error
import fi.espoo.voltti.logging.loggers.info
import io.opentracing.Tracer
import java.lang.reflect.UndeclaredThrowableException
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.reflect.KClass
import mu.KotlinLogging
import org.jdbi.v3.core.Jdbi

private const val defaultRetryCount =
    24 * 60 / 5 // 24h when used with default 5 minute retry interval
private val defaultRetryInterval = Duration.ofMinutes(5)

private data class Registration<T : AsyncJobPayload>(
    val handler: (db: Database, clock: EvakaClock, msg: T) -> Unit
) {
    fun run(db: Database, clock: EvakaClock, msg: AsyncJobPayload) =
        @Suppress("UNCHECKED_CAST") handler(db, clock, msg as T)
}

data class AsyncJobRunnerConfig(
    val threadPoolSize: Int = 4,
    val throttleInterval: Duration? = null
)

class AsyncJobRunner<T : AsyncJobPayload>(
    val payloadType: KClass<T>,
    private val jdbi: Jdbi,
    private val config: AsyncJobRunnerConfig,
    private val tracer: Tracer
) : AutoCloseable {
    private val runnerName = "${AsyncJobRunner::class.qualifiedName}.${payloadType.simpleName}"
    private val logger = KotlinLogging.logger(runnerName)

    private val executor: ScheduledThreadPoolExecutor =
        ScheduledThreadPoolExecutor(
            config.threadPoolSize,
            object : ThreadFactory {
                private val default = Executors.defaultThreadFactory()
                override fun newThread(r: Runnable): Thread =
                    default.newThread(r).also { it.priority = Thread.MIN_PRIORITY }
            }
        )
    private val periodicRunner: AtomicReference<ScheduledFuture<*>> = AtomicReference()
    private val activeWorkerCount: AtomicInteger = AtomicInteger(0)

    private val handlersLock: Lock = ReentrantLock()
    private val handlers: ConcurrentHashMap<AsyncJobType<out T>, Registration<*>> =
        ConcurrentHashMap()
    private val wakeUpHook: () -> Unit = { wakeUp() }

    val isStarted: Boolean
        get() = periodicRunner.get() != null

    val isBusy: Boolean
        get() = activeWorkerCount.get() > 0

    inline fun <reified P : T> registerHandler(
        noinline handler: (db: Database.Connection, clock: EvakaClock, msg: P) -> Unit
    ) =
        registerHandler(AsyncJobType(P::class)) { db, clock, msg ->
            db.connect { handler(it, clock, msg) }
        }

    fun <P : T> registerHandler(
        jobType: AsyncJobType<out P>,
        handler: (db: Database, clock: EvakaClock, msg: P) -> Unit
    ): Unit =
        handlersLock.withLock {
            require(!handlers.containsKey(jobType)) {
                "handler for $jobType has already been registered"
            }
            val ambiguousKey = handlers.keys.find { it.name == jobType.name }
            require(ambiguousKey == null) {
                "handlers for $jobType and $ambiguousKey have a name conflict"
            }
            handlers[jobType] = Registration(handler)
        }

    fun plan(
        tx: Database.Transaction,
        payloads: Iterable<T>,
        retryCount: Int = defaultRetryCount,
        retryInterval: Duration = defaultRetryInterval,
        runAt: HelsinkiDateTime
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

    fun plan(tx: Database.Transaction, jobs: List<JobParams<out T>>) {
        jobs.forEach { job -> tx.insertJob(job) }
        tx.afterCommit(wakeUpHook)
    }

    fun start(pollingInterval: Duration) {
        val newRunner =
            this.executor.scheduleWithFixedDelay(
                {
                    tracer.withDetachedSpan("asyncjob.poll $runnerName") {
                        this.runPendingJobs(RealEvakaClock())
                    }
                },
                0,
                pollingInterval.toNanos(),
                TimeUnit.NANOSECONDS
            )
        this.periodicRunner.getAndSet(newRunner)?.cancel(false)
    }

    fun wakeUp() {
        if (isStarted) {
            executor.execute {
                tracer.withDetachedSpan("asyncjob.wakeup $runnerName") {
                    this.runPendingJobs(RealEvakaClock())
                }
            }
        }
    }

    fun runPendingJobsSync(clock: EvakaClock, maxCount: Int = 1_000) {
        this.executor
            .submit {
                tracer.withDetachedSpan("asyncjob.sync $runnerName") {
                    this.runPendingJobs(clock, maxCount)
                }
            }
            .get()
    }

    fun getPendingJobCount(): Int =
        Database(jdbi).connect { db -> db.read { it.getPendingJobCount(handlers.keys) } }

    fun waitUntilNoRunningJobs(timeout: Duration = Duration.ofSeconds(10)) {
        val start = Instant.now()
        do {
            if (!isBusy) return
            TimeUnit.MILLISECONDS.sleep(100)
        } while (Duration.between(start, Instant.now()).abs() < timeout)
        error { "Timed out while waiting for running jobs to finish" }
    }

    private fun runPendingJobs(clock: EvakaClock, maxCount: Int = 1_000) {
        Database(jdbi).connect { db ->
            var remaining = maxCount
            activeWorkerCount.incrementAndGet()
            try {
                do {
                    val job = db.transaction { it.claimJob(clock.now(), handlers.keys) }
                    if (job != null) {
                        tracer.withDetachedSpan(
                            "asyncjob.run ${job.jobType.name}",
                            Tracing.asyncJobId withValue job.jobId,
                            Tracing.asyncJobRemainingAttempts withValue job.remainingAttempts,
                        ) {
                            runPendingJob(db, clock, job)
                        }
                    }
                    remaining -= 1
                    config.throttleInterval?.toMillis()?.run { Thread.sleep(this) }
                } while (job != null && remaining > 0 && !executor.isTerminating)
            } finally {
                activeWorkerCount.decrementAndGet()
            }
        }
    }

    private fun runPendingJob(
        db: Database.Connection,
        clock: EvakaClock,
        job: ClaimedJobRef<out T>
    ) {
        val logMeta =
            mapOf(
                "jobId" to job.jobId,
                "jobType" to job.jobType.name,
                "remainingAttempts" to job.remainingAttempts
            )
        try {
            val traceId = randomTracingId()
            MdcKey.TRACE_ID.set(traceId)
            MdcKey.SPAN_ID.set(randomTracingId())
            tracer.activeSpan()?.setTag(Tracing.evakaTraceId, traceId)
            logger.info(logMeta) { "Running async job $job" }
            val completed =
                db.transaction { tx ->
                    tx.setLockTimeout(Duration.ofSeconds(5))
                    val registration =
                        handlers[job.jobType]
                            ?: throw IllegalStateException("No handler found for ${job.jobType}")
                    tx.startJob(job, clock.now())?.let { msg ->
                        msg.user?.let {
                            MdcKey.USER_ID.set(it.rawId().toString())
                            MdcKey.USER_ID_HASH.set(it.rawIdHash.toString())
                            tracer.activeSpan()?.setTag(Tracing.enduserIdHash, it.rawIdHash)
                        }
                        registration.run(Database(jdbi), clock, msg)
                        tx.completeJob(job, clock.now())
                        true
                    }
                        ?: false
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
            MdcKey.USER_ID_HASH.unset()
            MdcKey.USER_ID.unset()
            MdcKey.SPAN_ID.unset()
            MdcKey.TRACE_ID.unset()
        }
    }

    override fun close() {
        this.executor.shutdown()
        if (!this.executor.awaitTermination(10, TimeUnit.SECONDS)) {
            logger.error { "Some async jobs did not terminate in time during shutdown" }
        }
        this.executor.shutdownNow()
    }
}
