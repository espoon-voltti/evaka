// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.async

import fi.espoo.evaka.shared.Tracing
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.shared.randomTracingId
import fi.espoo.evaka.shared.withDetachedSpan
import fi.espoo.evaka.shared.withValue
import fi.espoo.voltti.logging.MdcKey
import fi.espoo.voltti.logging.loggers.error
import fi.espoo.voltti.logging.loggers.info
import io.micrometer.core.instrument.Counter
import io.opentracing.Tracer
import io.opentracing.tag.Tags
import java.lang.reflect.UndeclaredThrowableException
import java.time.Duration
import java.util.Timer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.FutureTask
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.fixedRateTimer
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import mu.KotlinLogging
import org.jdbi.v3.core.Jdbi

data class AsyncJobPoolConfig(
    val concurrency: Int,
    val backgroundPollingInterval: Duration,
    val throttleInterval: Duration? = null
)

private data class Registration<T : AsyncJobPayload>(
    val handler: (db: Database, clock: EvakaClock, msg: T) -> Unit
) {
    fun run(db: Database, clock: EvakaClock, msg: AsyncJobPayload) =
        @Suppress("UNCHECKED_CAST") handler(db, clock, msg as T)
}

class AsyncJobMetrics(val executedJobs: Counter, val failedJobs: Counter)

class AsyncJobPool<T : AsyncJobPayload>(
    name: String,
    private val config: AsyncJobPoolConfig,
    private val jdbi: Jdbi,
    private val tracer: Tracer,
    private val metrics: AtomicReference<AsyncJobMetrics>
) : AutoCloseable {
    private val fullName: String = "${AsyncJobPool::class.simpleName}.$name"
    private val logger = KotlinLogging.logger("${AsyncJobPool::class.qualifiedName}.$name")

    private val handlersLock: Lock = ReentrantLock()
    private val handlers: ConcurrentHashMap<AsyncJobType<out T>, Registration<*>> =
        ConcurrentHashMap()

    private val backgroundTimer: AtomicReference<Timer> = AtomicReference()
    private val executor =
        config.let {
            val corePoolSize = 1
            val maximumPoolSize = it.concurrency
            val keepAliveTime = Pair(1L, TimeUnit.MINUTES)
            val workQueue = SynchronousQueue<Runnable>()
            val threadNumber = AtomicInteger(1)
            val threadFactory = { r: Runnable ->
                thread(
                    start = false,
                    name = "$fullName.worker-${threadNumber.getAndIncrement()}",
                    priority = Thread.MIN_PRIORITY,
                    block = r::run
                )
            }
            ThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize,
                keepAliveTime.first,
                keepAliveTime.second,
                workQueue,
                threadFactory,
                ThreadPoolExecutor.DiscardPolicy()
            )
        }

    val activeWorkerCount: Int
        get() = executor.activeCount

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

    fun startBackgroundPolling() {
        val newTimer =
            fixedRateTimer(
                "$fullName.timer",
                daemon = true,
                period = config.backgroundPollingInterval.toMillis()
            ) {
                executor.execute { runWorker(RealEvakaClock()) }
            }
        backgroundTimer.getAndSet(newTimer)?.cancel()
    }

    fun runPendingJobs(clock: EvakaClock, maxCount: Int) {
        executor.execute { runWorker(clock, maxCount) }
    }

    fun runPendingJobsSync(clock: EvakaClock, maxCount: Int): Int {
        val task = FutureTask { runWorker(clock, maxCount) }
        while (!executor.queue.offer(task)) {
            // no available workers
            if (!executor.prestartCoreThread()) {
                // worker capacity full
                TimeUnit.MILLISECONDS.sleep(100)
            }
        }
        return task.get()
    }

    private fun runWorker(clock: EvakaClock, maxCount: Int = 1_000) =
        tracer.withDetachedSpan("asyncjob.worker $fullName") {
            var executed = 0
            Database(jdbi, tracer).connect { dbc ->
                var remaining = maxCount
                do {
                    val job = dbc.transaction { it.claimJob(clock.now(), handlers.keys) }
                    if (job != null) {
                        tracer.withDetachedSpan(
                            "asyncjob.run ${job.jobType.name}",
                            Tracing.asyncJobId withValue job.jobId,
                            Tracing.asyncJobRemainingAttempts withValue job.remainingAttempts,
                        ) {
                            runPendingJob(dbc, clock, job)
                        }
                        metrics.get()?.executedJobs?.increment()
                        executed += 1
                    }
                    remaining -= 1
                    config.throttleInterval?.toMillis()?.run { Thread.sleep(this) }
                } while (job != null && remaining > 0 && !executor.isTerminating)
            }
            executed
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
                        registration.run(Database(jdbi, tracer), clock, msg)
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
            metrics.get()?.failedJobs?.increment()
            tracer.activeSpan()?.setTag(Tags.ERROR, true)
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
        backgroundTimer.get()?.cancel()

        executor.shutdown()
        if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
            logger.error { "Some async jobs did not terminate in time during shutdown" }
        }
        executor.shutdownNow()
    }
}
