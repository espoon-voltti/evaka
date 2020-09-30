// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.utils

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi

/**
 * Starts a transaction, runs the given function, and commits or rolls back the transaction depending on whether
 * the function threw an exception or not.
 *
 * Same as Handle.inTransaction, but works better with Kotlin.
 *
 * @see org.jdbi.v3.core.Handle.inTransaction
 */
inline fun <T> Handle.transaction(crossinline f: (Handle) -> T): T {
    return this.inTransaction<T, Exception> { f(it) }
}

/**
 * Opens a database connection, runs the given function, and closes the connection.
 *
 * Same as Jdbi.withHandle, but works better with Kotlin.
 *
 * @see org.jdbi.v3.core.Jdbi.withHandle
 */
inline fun <T> Jdbi.handle(crossinline f: (Handle) -> T): T {
    return this.open().use { f(it) }
}

/**
 * Starts a transaction, runs the given function, and commits or rolls back the transaction depending on whether
 * the function threw an exception or not.
 *
 * Same as Jdbi.inTransaction, but works better with Kotlin.
 *
 * @see org.jdbi.v3.core.Jdbi.inTransaction
 */
inline fun <T> Jdbi.transaction(crossinline f: (Handle) -> T): T {
    return this.open().use { h -> h.inTransaction<T, Exception> { f(it) } }
}
