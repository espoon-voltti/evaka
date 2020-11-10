// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.db

import org.intellij.lang.annotations.Language
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.statement.PreparedBatch
import org.jdbi.v3.core.statement.Query
import org.jdbi.v3.core.statement.Update

private data class ThreadId(val id: Long = Thread.currentThread().id) {
    fun assertCurrentThread() = assert(Thread.currentThread().id == id) { "Database accessed from the wrong thread" }
}

/**
 * A database reference that can be used to obtain *one database connection at a time*.
 *
 * Tied to the thread that created it, and throws `IllegalStateException` if used in the wrong thread.
 */
class Database(private val jdbi: Jdbi) {
    private val threadId = ThreadId()
    private var connected = false

    /**
     * Opens a database connection, runs the given function, and closes the connection.
     *
     * Throws `IllegalStateException` if a connection is already open
     */
    fun <T> connect(f: (db: Connection) -> T): T {
        threadId.assertCurrentThread()
        check(!connected) { "Already connected to database" }
        try {
            connected = true
            return jdbi.open().use {
                f(Connection(it))
            }
        } finally {
            connected = false
        }
    }

    /**
     * Opens a database connection in read mode, runs the given function, and closes the connection.
     *
     * Throws `IllegalStateException` if a connection is already open
     */
    fun <T> read(f: (db: Read) -> T): T = connect { it.read(f) }

    /**
     * Opens a database connection, runs the given function within a transaction, and closes the connection.
     *
     * Throws `IllegalStateException` if a connection is already open
     */
    fun <T> transaction(f: (db: Transaction) -> T): T = connect { it.transaction(f) }

    /**
     * A single database connection tied to a single thread
     */
    inner class Connection internal constructor(private val handle: Handle) {
        /**
         * Enters read mode, runs the given function, and exits read mode regardless of any exceptions the function may have thrown.
         *
         * Throws `IllegalStateException` if this database connection is already in read mode or a transaction
         */
        fun <T> read(f: (db: Read) -> T): T {
            threadId.assertCurrentThread()
            check(!handle.isInTransaction) { "Already in a transaction" }
            handle.isReadOnly = true
            try {
                return handle.inTransaction<T, Exception> { f(Read(handle)) }
            } finally {
                handle.isReadOnly = false
            }
        }

        /**
         * Starts a transaction, runs the given function, and commits or rolls back the transaction depending on whether
         * the function threw an exception or not.
         *
         * Throws `IllegalStateException` if this database connection is already in read mode or a transaction.
         */
        fun <T> transaction(f: (db: Transaction) -> T): T {
            threadId.assertCurrentThread()
            check(!handle.isInTransaction) { "Already in a transaction" }
            return handle.transaction { f(Transaction(it)) }
        }
    }

    /**
     * A single database connection in read mode.
     *
     * Tied to the thread that created it, and throws `IllegalStateException` if used in the wrong thread.
     */
    open inner class Read internal constructor(val handle: Handle) {
        fun createQuery(@Language("sql") sql: String): Query = handle.createQuery(sql)
    }

    /**
     * A single database connection running a transaction.
     *
     * Tied to the thread that created it, and throws `IllegalStateException` if used in the wrong thread.
     */
    inner class Transaction internal constructor(handle: Handle) : Database.Read(handle) {
        private var savepointId: Long = 0

        fun nextSavepoint(): String = "savepoint-${savepointId++}"
        fun createUpdate(@Language("sql") sql: String): Update = handle.createUpdate(sql)
        fun prepareBatch(@Language("sql") sql: String): PreparedBatch = handle.prepareBatch(sql)
        fun execute(@Language("sql") sql: String, vararg args: Any): Int = handle.execute(sql, *args)

        inline fun <reified T> withSavepoint(crossinline f: () -> T): T {
            val savepointName = nextSavepoint()
            handle.savepoint(savepointName)
            val result = try {
                f()
            } catch (e: Throwable) {
                try {
                    handle.rollbackToSavepoint(savepointName)
                } catch (rollback: Exception) {
                    e.addSuppressed(rollback)
                }
                throw e
            }
            handle.release(savepointName)
            return result
        }
    }
}
