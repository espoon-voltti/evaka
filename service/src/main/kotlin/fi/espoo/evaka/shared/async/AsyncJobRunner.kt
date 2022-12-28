// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.async

import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.RealEvakaClock
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.opentracing.Tracer
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KClass
import org.jdbi.v3.core.Jdbi

private const val defaultRetryCount =
    24 * 60 / 5 // 24h when used with default 5 minute retry interval
private val defaultRetryInterval = Duration.ofMinutes(5)

class AsyncJobRunner<T : AsyncJobPayload>(
    private val payloadType: KClass<T>,
    config: AsyncJobPoolConfig,
    jdbi: Jdbi,
    tracer: Tracer
) : AutoCloseable {
    val name = "${AsyncJobRunner::class.simpleName}.${payloadType.simpleName}"

    private val metrics: AtomicReference<AsyncJobMetrics> = AtomicReference()
    private val pool: AsyncJobPool<T> =
        AsyncJobPool(name = payloadType.simpleName!!, config = config, jdbi, tracer, metrics)

    private val wakeUpHook: AtomicReference<() -> Unit> = AtomicReference()

    private val isBusy: Boolean
        get() = pool.activeWorkerCount > 0

    fun registerMeters(meterRegistry: MeterRegistry) {
        Gauge.builder("asyncJobWorkersActive") { pool.activeWorkerCount }
            .tag("payloadType", payloadType.simpleName!!)
            .register(meterRegistry)
        metrics.set(
            AsyncJobMetrics(
                Counter.builder("asyncJobsExecuted")
                    .tag("payloadType", payloadType.simpleName!!)
                    .register(meterRegistry),
                Counter.builder("asyncJobsFailed")
                    .tag("payloadType", payloadType.simpleName!!)
                    .register(meterRegistry)
            )
        )
    }

    inline fun <reified P : T> registerHandler(
        noinline handler: (db: Database.Connection, clock: EvakaClock, msg: P) -> Unit
    ) =
        registerHandler(AsyncJobType(P::class)) { db, clock, msg ->
            db.connect { handler(it, clock, msg) }
        }

    fun <P : T> registerHandler(
        jobType: AsyncJobType<out P>,
        handler: (db: Database, clock: EvakaClock, msg: P) -> Unit
    ): Unit = pool.registerHandler(jobType, handler)

    fun plan(
        tx: Database.Transaction,
        payloads: Iterable<T>,
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

    fun plan(tx: Database.Transaction, jobs: Iterable<JobParams<out T>>) {
        jobs.forEach { job -> tx.insertJob(job) }
        wakeUpHook.get()?.let { tx.afterCommit(it) }
    }

    fun startBackgroundPolling() = pool.startBackgroundPolling()

    fun enableAfterCommitHook() =
        wakeUpHook.set { pool.runPendingJobs(RealEvakaClock(), maxCount = 1_000) }

    fun runPendingJobsSync(clock: EvakaClock, maxCount: Int = 1_000): Int =
        pool.runPendingJobsSync(clock, maxCount)

    fun waitUntilNoRunningJobs(timeout: Duration = Duration.ofSeconds(10)) {
        val start = Instant.now()
        do {
            if (!isBusy) return
            TimeUnit.MILLISECONDS.sleep(100)
        } while (Duration.between(start, Instant.now()).abs() < timeout)
        error { "Timed out while waiting for running jobs to finish" }
    }

    override fun close() {
        pool.close()
    }
}
