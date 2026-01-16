// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * A thread-safe rate limiter that enforces a strict minimum time interval between acquires.
 *
 * @param ratePerSecond How many acquires are allowed per second.
 */
class StrictRateLimiter(ratePerSecond: Double) {
    private val minIntervalNanos = (1.0 / ratePerSecond * 1_000_000_000.0).toLong()

    private val lock = ReentrantLock()
    private var lastAcquireTimeNanos: Long = 0

    fun acquire() {
        lock.withLock {
            val now = System.nanoTime()
            val timeSinceLastAcquire = now - lastAcquireTimeNanos

            if (timeSinceLastAcquire < minIntervalNanos) {
                val waitTimeNanos = minIntervalNanos - timeSinceLastAcquire

                // We're sleeping inside a lock, so this will block other threads trying to acquire.
                // In this case it's actually what we want: The other threads would have to sleep
                // anyway.
                Thread.sleep(waitTimeNanos / 1_000_000, (waitTimeNanos % 1_000_000).toInt())

                lastAcquireTimeNanos = System.nanoTime()
            } else {
                lastAcquireTimeNanos = now
            }
        }
    }
}
