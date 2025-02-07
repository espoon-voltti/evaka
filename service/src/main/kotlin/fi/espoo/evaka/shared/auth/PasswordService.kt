// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.auth

import fi.espoo.evaka.Sensitive
import fi.espoo.evaka.shared.domain.ServiceUnavailable
import fi.espoo.evaka.shared.noopMeter
import fi.espoo.evaka.shared.noopTracer
import fi.espoo.evaka.shared.withSpan
import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.api.trace.Tracer
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import org.springframework.stereotype.Service

@Service
class PasswordService(private val tracer: Tracer = noopTracer(), meter: Meter = noopMeter()) :
    AutoCloseable {
    private val algorithm = PasswordHashAlgorithm.DEFAULT
    private val passwordPlaceholder = algorithm.placeholder()
    private val pool: ThreadPoolExecutor = run {
        val corePoolSize = 1
        val maximumPoolSize = 16
        val workQueueCapacity = 128
        val keepAliveTime = Pair(15L, TimeUnit.MINUTES)
        val workQueue = ArrayBlockingQueue<Runnable>(workQueueCapacity)
        val threadNumber = AtomicInteger(1)
        val threadFactory = { r: Runnable ->
            thread(
                start = false,
                name = "${this.javaClass.simpleName}.worker-${threadNumber.getAndIncrement()}",
                priority = Thread.MIN_PRIORITY,
                block = r::run,
            )
        }
        val handler = RejectedExecutionHandler { _, _ ->
            throw ServiceUnavailable("No capacity to handle password operation")
        }
        ThreadPoolExecutor(
            corePoolSize,
            maximumPoolSize,
            keepAliveTime.first,
            keepAliveTime.second,
            workQueue,
            threadFactory,
            handler,
        )
    }
    private val activeWorkersGauge =
        meter
            .gaugeBuilder("evaka.auth.password_service.active_workers")
            .ofLongs()
            .buildWithCallback { it.record(pool.activeCount.toLong()) }
    private val queueCapacityGauge =
        meter
            .gaugeBuilder("evaka.auth.password_service.queue_capacity")
            .ofLongs()
            .buildWithCallback { it.record(pool.queue.remainingCapacity().toLong()) }
    private val matchCount = meter.counterBuilder("evaka.auth.password_service.match_count").build()
    private val encodeCount =
        meter.counterBuilder("evaka.auth.password_service.encode_count").build()

    /**
     * Checks if the given password matches the given optional encoded password.
     *
     * If the given encoded password is null, the password is checked against a dummy placeholder.
     * The hashing and comparison operations are executed in a separate worker thread, and may throw
     * `ServiceUnavailable` if the work queue is full.
     */
    @Throws(ServiceUnavailable::class)
    fun isMatch(password: Sensitive<String>, encoded: EncodedPassword?): Boolean =
        tracer.withSpan("isMatch") {
            pool
                .submit<Boolean> {
                    (encoded ?: passwordPlaceholder).isMatch(password).also { matchCount.add(1) }
                }
                .get()
        }

    /**
     * Encodes the given raw password.
     *
     * The encoding operation is executed in a separate worker thread, and may throw
     * `ServiceUnavailable` if the work queue is full.
     */
    @Throws(ServiceUnavailable::class)
    fun encode(password: Sensitive<String>): EncodedPassword =
        tracer.withSpan("encode") {
            pool
                .submit<EncodedPassword> { algorithm.encode(password).also { encodeCount.add(1) } }
                .get()
        }

    /**
     * Returns true if the encoded password should be rehashed for security and/or maintenance
     * reasons.
     */
    fun needsRehashing(encoded: EncodedPassword): Boolean = encoded.algorithm != algorithm

    override fun close() {
        pool.close()
        activeWorkersGauge.close()
        queueCapacityGauge.close()
    }
}
