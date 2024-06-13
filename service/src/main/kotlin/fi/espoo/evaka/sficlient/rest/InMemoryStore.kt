// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.sficlient.rest

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * An in-memory store that manages a single immutable value in a thread-safe way.
 *
 * It uses a lock to ensure thread-safety and to avoid requesting a new value multiple times
 * concurrently.
 */
class InMemoryStore<T : Any>(private val requestValue: () -> T) {
    private val lock = ReentrantLock()
    private var value: T? = null

    /**
     * Expires a value, if it's the current value.
     *
     * If the value has already changed, this method does nothing. This avoids unnecessary
     * expiration if another thread has already requested a new perfectly valid value.
     */
    fun expire(value: T): Unit =
        lock.withLock {
            if (this.value == value) {
                this.value = null
            }
        }

    /**
     * Returns the current value, requesting a new one if necessary.
     *
     * This method throws an error if there is no value and requesting a new one fails.
     */
    fun get(): T = lock.withLock { value ?: requestValue().also { value = it } }
}
