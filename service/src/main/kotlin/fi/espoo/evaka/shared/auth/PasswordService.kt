// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.auth

import fi.espoo.evaka.Sensitive
import fi.espoo.evaka.shared.domain.ServiceUnavailable
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import org.springframework.stereotype.Service

@Service
class PasswordService : AutoCloseable {
    private val algorithm = PasswordHashAlgorithm.DEFAULT
    private val passwordPlaceholder = algorithm.placeholder()
    private val pool: ExecutorService = run {
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

    /**
     * Checks if the given password matches the given optional encoded password.
     *
     * If the given encoded password is null, the password is checked against a dummy placeholder.
     * The hashing and comparison operations are executed in a separate worker thread, and may throw
     * `ServiceUnavailable` if the work queue is full.
     */
    @Throws(ServiceUnavailable::class)
    fun isMatch(password: Sensitive<String>, encoded: EncodedPassword?): Boolean =
        pool.submit<Boolean> { (encoded ?: passwordPlaceholder).isMatch(password) }.get()

    /**
     * Encodes the given raw password.
     *
     * The encoding operation is executed in a separate worker thread, and may throw
     * `ServiceUnavailable` if the work queue is full.
     */
    @Throws(ServiceUnavailable::class)
    fun encode(password: Sensitive<String>): EncodedPassword =
        pool.submit<EncodedPassword> { algorithm.encode(password) }.get()

    /**
     * Returns true if the encoded password should be rehashed for security and/or maintenance
     * reasons.
     */
    fun needsRehashing(encoded: EncodedPassword): Boolean = encoded.algorithm != algorithm

    override fun close() = pool.close()
}
