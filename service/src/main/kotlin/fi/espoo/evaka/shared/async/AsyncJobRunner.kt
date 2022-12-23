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
    payloadType: KClass<T>,
    private val jdbi: Jdbi,
    private val config: AsyncJobRunnerConfig,
    private val tracer: Tracer
) : AutoCloseable {
    val name = "${AsyncJobRunner::class.simpleName}.${payloadType.simpleName}"
    private val logger =
        KotlinLogging.logger("${AsyncJobRunner::class.qualifiedName}.${payloadType.simpleName}")

    private val handlersLock: Lock = ReentrantLock()
    private val handlers: ConcurrentHashMap<AsyncJobType<out T>, Registration<*>> =
        ConcurrentHashMap()

    private val periodicTimer: AtomicReference<Timer> = AtomicReference()
    private val wakeUpHook: () -> Unit = {
        if (isStarted) {
            workerExecutor.execute { runWorker(RealEvakaClock()) }
        }
    }

    private val workerExecutor =
        config.let {
            val corePoolSize = 1
            val maximumPoolSize = it.threadPoolSize
            val keepAliveTime = Pair(1L, TimeUnit.MINUTES)
            val workQueue = SynchronousQueue<Runnable>()
            val threadNumber = AtomicInteger(1)
            val threadFactory = { r: Runnable ->
                thread(
                    start = false,
                    name = "$name.worker-${threadNumber.getAndIncrement()}",
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

    private val isStarted: Boolean
        get() = periodicTimer.get() != null

    private val isBusy: Boolean
        get() = workerExecutor.activeCount > 0

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
        val newTimer =
            fixedRateTimer("$name.timer", daemon = true, period = pollingInterval.toMillis()) {
                workerExecutor.execute { runWorker(RealEvakaClock()) }
            }
        periodicTimer.getAndSet(newTimer)?.cancel()
    }

    fun runPendingJobsSync(clock: EvakaClock, maxCount: Int = 1_000) {
        val task = FutureTask { runWorker(clock, maxCount) }
        while (!workerExecutor.queue.offer(task)) {
            // no available workers
            if (!workerExecutor.prestartCoreThread()) {
                // worker capacity full
                TimeUnit.MILLISECONDS.sleep(100)
            }
        }
        return task.get()
    }

    fun getPendingJobCount(): Int =
        Database(jdbi, tracer).connect { db -> db.read { it.getPendingJobCount(handlers.keys) } }

    fun waitUntilNoRunningJobs(timeout: Duration = Duration.ofSeconds(10)) {
        val start = Instant.now()
        do {
            if (!isBusy) return
            TimeUnit.MILLISECONDS.sleep(100)
        } while (Duration.between(start, Instant.now()).abs() < timeout)
        error { "Timed out while waiting for running jobs to finish" }
    }

    private fun runWorker(clock: EvakaClock, maxCount: Int = 1_000) =
        tracer.withDetachedSpan("asyncjob.worker $name") {
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
                    }
                    remaining -= 1
                    config.throttleInterval?.toMillis()?.run { Thread.sleep(this) }
                } while (job != null && remaining > 0 && !workerExecutor.isTerminating)
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
        periodicTimer.get()?.cancel()

        workerExecutor.shutdown()
        if (!workerExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
            logger.error { "Some async jobs did not terminate in time during shutdown" }
        }
        workerExecutor.shutdownNow()
    }
}
