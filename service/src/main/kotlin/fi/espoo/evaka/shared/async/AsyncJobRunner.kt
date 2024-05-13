// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.async

import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.RealEvakaClock
import io.micrometer.core.instrument.MeterRegistry
import io.opentracing.Tracer
import java.time.Duration
import java.time.Instant
import java.util.Timer
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.fixedRateTimer
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.math.max
import kotlin.reflect.KClass
import mu.KotlinLogging
import org.jdbi.v3.core.Jdbi

private const val defaultRetryCount =
    24 * 60 / 5 // 24h when used with default 5 minute retry interval
private val defaultRetryInterval = Duration.ofMinutes(5)

class AsyncJobRunner<T : AsyncJobPayload>(
    payloadType: KClass<T>,
    pools: Iterable<Pool<T>>,
    jdbi: Jdbi,
    tracer: Tracer
) : AutoCloseable {
    data class Pool<T : AsyncJobPayload>(
        val id: AsyncJobPool.Id<T>,
        val config: AsyncJobPool.Config,
        val jobs: Set<KClass<out T>>
    ) {
        fun withThrottleInterval(throttleInterval: Duration?) =
            copy(config = config.copy(throttleInterval = throttleInterval))
    }

    val name = "${AsyncJobRunner::class.simpleName}.${payloadType.simpleName}"

    private val logger = KotlinLogging.logger {}
    private val stateLock = ReentrantReadWriteLock()
    private var handlers: Map<AsyncJobType<out T>, AsyncJobPool.Handler<*>> = emptyMap()
    private var afterCommitHooks: Map<AsyncJobType<out T>, () -> Unit> = emptyMap()

    private val pools: List<AsyncJobPool<T>>
    private val jobsPerPool: Map<AsyncJobPool.Id<T>, Set<AsyncJobType<out T>>>
    private val backgroundTimer: AtomicReference<Timer> = AtomicReference()

    init {
        this.jobsPerPool =
            pools.associate { pool -> pool.id to pool.jobs.map { AsyncJobType(it) }.toSet() }
        this.pools =
            pools.map { AsyncJobPool(it.id, it.config, jdbi, tracer, PoolRegistration(it.id)) }
    }

    inner class PoolRegistration(val id: AsyncJobPool.Id<T>) : AsyncJobPool.Registration<T> {
        override fun jobTypes() = jobsPerPool[id] ?: emptySet()

        override fun handlerFor(jobType: AsyncJobType<out T>) =
            stateLock.read { requireNotNull(handlers[jobType]) { "No handler found for $jobType" } }
    }

    private val isBusy: Boolean
        get() = pools.any { it.activeWorkerCount > 0 }

    fun registerMeters(registry: MeterRegistry) = pools.forEach { it.registerMeters(registry) }

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
        stateLock.write {
            require(jobsPerPool.values.any { it.contains(jobType) }) {
                "No job pool defined for $jobType"
            }
            val ambiguousKey = handlers.keys.find { it.name == jobType.name }
            require(ambiguousKey == null) {
                "handlers for $jobType and $ambiguousKey have a name conflict"
            }

            require(!handlers.containsKey(jobType)) {
                "handler for $jobType has already been registered"
            }
            handlers = handlers + mapOf(jobType to AsyncJobPool.Handler(handler))
        }

    fun plan(
        tx: Database.Transaction,
        payloads: Iterable<T>,
        retryCount: Int = defaultRetryCount,
        retryInterval: Duration = defaultRetryInterval,
        runAt: HelsinkiDateTime
    ) = plan(tx, payloads.asSequence(), retryCount, retryInterval, runAt)

    fun plan(
        tx: Database.Transaction,
        payloads: Sequence<T>,
        retryCount: Int = defaultRetryCount,
        retryInterval: Duration = defaultRetryInterval,
        runAt: HelsinkiDateTime
    ) =
        plan(
            tx,
            payloads.map { payload ->
                JobParams(
                    payload = payload,
                    retryCount = retryCount,
                    retryInterval = retryInterval,
                    runAt = runAt
                )
            }
        )

    fun plan(tx: Database.Transaction, jobs: Iterable<JobParams<out T>>) =
        plan(tx, jobs.asSequence())

    fun plan(tx: Database.Transaction, jobs: Sequence<JobParams<out T>>) =
        stateLock.read {
            jobs.forEach { job ->
                val jobType = AsyncJobType.ofPayload(job.payload)
                val id = tx.insertJob(job)
                logger.debug {
                    "$name planned job $jobType(id=$id, runAt=${job.runAt}, retryCount=${job.retryCount}, retryInterval=${job.retryInterval})"
                }
                afterCommitHooks[jobType]?.let { tx.afterCommit(it) }
            }
        }

    fun startBackgroundPolling(
        clock: EvakaClock = RealEvakaClock(),
        pollingInterval: Duration = Duration.ofMinutes(1)
    ) {
        val newTimer =
            fixedRateTimer("$name.timer", period = pollingInterval.toMillis()) {
                pools.forEach { it.runPendingJobs(clock, maxCount = 1_000) }
            }
        backgroundTimer.getAndSet(newTimer)?.cancel()
    }

    fun stopBackgroundPolling() {
        backgroundTimer.getAndSet(null)?.cancel()
    }

    fun enableAfterCommitHooks(clock: EvakaClock = RealEvakaClock()) =
        stateLock.write {
            afterCommitHooks =
                pools
                    .flatMap { pool ->
                        val hook = { pool.runPendingJobs(clock, maxCount = 1_000) }
                        (jobsPerPool[pool.id] ?: emptySet()).map { jobType -> jobType to hook }
                    }
                    .toMap()
        }

    fun disableAfterCommitHooks() = stateLock.write { afterCommitHooks = emptyMap() }

    fun runPendingJobsSync(clock: EvakaClock, maxCount: Int = 1_000): Int {
        var totalCount = 0
        do {
            val executed =
                pools.fold(0) { count, pool ->
                    count + pool.runPendingJobsSync(clock, max(0, maxCount - totalCount - count))
                }
            // A job in one pool may plan a job for some other pool, so we can't just iterate once
            // and assume all jobs were executed. Instead, we're done only when all pools are done
            val done = executed == 0
            totalCount += executed
        } while (!done)
        logger.debug { "$name executed $totalCount jobs synchronously" }
        return totalCount
    }

    fun waitUntilNoRunningJobs(timeout: Duration = Duration.ofSeconds(10)) {
        val start = Instant.now()
        do {
            if (!isBusy) return
            TimeUnit.MILLISECONDS.sleep(100)
        } while (Duration.between(start, Instant.now()).abs() < timeout)
        error { "Timed out while waiting for running jobs to finish" }
    }

    override fun close() {
        stopBackgroundPolling()
        pools.forEach { it.close() }
    }
}
