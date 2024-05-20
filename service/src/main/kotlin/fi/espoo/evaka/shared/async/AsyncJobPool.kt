// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.async

import fi.espoo.evaka.shared.Tracing
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.randomTracingId
import fi.espoo.evaka.shared.withDetachedSpan
import fi.espoo.evaka.shared.withValue
import fi.espoo.voltti.logging.MdcKey
import fi.espoo.voltti.logging.loggers.error
import fi.espoo.voltti.logging.loggers.info
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.opentracing.Tracer
import io.opentracing.tag.Tags
import java.lang.reflect.UndeclaredThrowableException
import java.time.Duration
import java.util.concurrent.FutureTask
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.reflect.KClass
import mu.KotlinLogging
import org.jdbi.v3.core.Jdbi

class AsyncJobPool<T : AsyncJobPayload>(
    val id: Id<T>,
    config: Config,
    private val jdbi: Jdbi,
    private val tracer: Tracer,
    private val registration: Registration<T>
) : AutoCloseable {
    data class Id<T : Any>(val jobType: KClass<T>, val pool: String) {
        override fun toString(): String = "${jobType.simpleName}.$pool"
    }

    private data class Metrics(val executedJobs: Counter, val failedJobs: Counter)

    data class Config(val concurrency: Int = 1, val throttleInterval: Duration? = null)

    data class Handler<T : AsyncJobPayload>(
        val handler: (db: Database, clock: EvakaClock, msg: T) -> Unit
    ) {
        fun run(db: Database, clock: EvakaClock, msg: AsyncJobPayload) =
            @Suppress("UNCHECKED_CAST") handler(db, clock, msg as T)
    }

    interface Registration<T : AsyncJobPayload> {
        fun jobTypes(): Collection<AsyncJobType<out T>>

        fun handlerFor(jobType: AsyncJobType<out T>): Handler<*>
    }

    private val fullName: String = "${AsyncJobPool::class.simpleName}.$id"
    private val logger = KotlinLogging.logger("${AsyncJobPool::class.qualifiedName}.$id")
    private val metrics: AtomicReference<Metrics> = AtomicReference()

    private val throttleInterval = config.throttleInterval ?: Duration.ZERO
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
                    block = {
                        try {
                            r.run()
                        } catch (e: Exception) {
                            logger.error(e) { "Error running pool $fullName worker" }
                        }
                    }
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

    fun registerMeters(meterRegistry: MeterRegistry) {
        Gauge.builder("asyncJobWorkersActive") { activeWorkerCount }
            .tag("jobType", id.jobType.simpleName!!)
            .tag("pool", id.pool)
            .register(meterRegistry)
        metrics.set(
            Metrics(
                Counter.builder("asyncJobsExecuted")
                    .tag("jobType", id.jobType.simpleName!!)
                    .tag("pool", id.pool)
                    .register(meterRegistry),
                Counter.builder("asyncJobsFailed")
                    .tag("jobType", id.jobType.simpleName!!)
                    .tag("pool", id.pool)
                    .register(meterRegistry)
            )
        )
    }

    fun runPendingJobs(clock: EvakaClock, maxCount: Int) =
        executor.execute { runWorker(clock, maxCount) }

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

    private fun runWorker(clock: EvakaClock, maxCount: Int) =
        tracer.withDetachedSpan("asyncjob.worker $fullName") {
            Database(jdbi, tracer).connect { dbc ->
                dbc.transaction { it.upsertPermit(this.id) }
                var executed = 0
                while (maxCount - executed > 0 && !executor.isTerminating) {
                    val job =
                        dbc.transaction { tx ->
                            tx.setStatementTimeout(Duration.ofSeconds(120))
                            // In the worst case we need to wait for the duration of (N service
                            // instances) * (M workers per pool) * (throttle interval) if every
                            // worker in the cluster is queuing and every one sleeps.
                            //
                            // The value here is just a guess that should be long enough in all
                            // valid cases, and we get a loud exception if this assumption is broken
                            tx.setLockTimeout(Duration.ofSeconds(60))
                            val permit = tx.claimPermit(this.id)
                            Thread.sleep(
                                Duration.between(
                                    clock.now().toInstant(),
                                    permit.availableAt.toInstant()
                                )
                            )
                            tx.claimJob(clock.now(), registration.jobTypes())?.also {
                                tx.updatePermit(this.id, clock.now().plus(throttleInterval))
                            }
                        } ?: break
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
                executed
            }
        }

    private fun runPendingJob(
        db: Database.Connection,
        clock: EvakaClock,
        job: ClaimedJobRef<out T>,
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
                    tx.startJob(job, clock.now())?.let { msg ->
                        msg.user?.let {
                            MdcKey.USER_ID.set(it.rawId().toString())
                            MdcKey.USER_ID_HASH.set(it.rawIdHash.toString())
                            tracer.activeSpan()?.setTag(Tracing.enduserIdHash, it.rawIdHash)
                        }
                        registration.handlerFor(job.jobType).run(Database(jdbi, tracer), clock, msg)
                        tx.completeJob(job, clock.now())
                        true
                    } ?: false
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
        executor.shutdown()
        if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
            logger.error { "Some async jobs did not terminate in time during shutdown" }
        }
        executor.shutdownNow()
    }
}
