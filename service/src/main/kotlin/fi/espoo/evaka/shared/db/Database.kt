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
import java.util.concurrent.atomic.AtomicBoolean

// What does it mean when a function accepts a Database/Database.* parameter?
//
//     fun doStuff(db: Database):
//         To call this function, you need to have a database reference *without* an active connection or transaction.
//         The function can connect/disconnect from the database 0 to N times, and do whatever it wants using the connection(s).
//     fun doStuff(db: Database.Connection)
//         To call this function, you need to have an active database connection *without* an active transaction.
//         The function can read/write the database and freely execute 0 to N individual transactions.
//     fun doStuff(tx: Database.Read)
//         To call this function, you need to have an active read-only transaction.
//         The function can only read the database, and can't manage transactions by itself.
//     fun doStuff(tx: Database.Transaction)
//         To call this function, you need to have an active transaction.
//         The function can read/write the database, and can't manage transactions by itself.

/**
 * A database reference that can be used to obtain *one database connection at a time*.
 *
 * Tied to the thread that created it, and throws `IllegalStateException` if used in the wrong thread.
 */
class Database(private val jdbi: Jdbi) {
    private val threadId = ThreadId()
    private var connected = AtomicBoolean()

    /**
     * Opens a database connection, runs the given function, and closes the connection.
     *
     * Throws `IllegalStateException` if a connection is already open
     */
    fun <T> connect(f: (db: Connection) -> T): T = connect().use(f)

    /**
     * Opens a new database connection and returns it. The connection *must be closed after use*.
     *
     * Throws `IllegalStateException` if a connection is already open
     */
    fun connect(): Connection {
        threadId.assertCurrentThread()
        check(!connected.get()) { "Already connected to database" }
        connected.set(true)
        return Connection(threadId, connected, jdbi.open())
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
    class Connection internal constructor(private val threadId: ThreadId, private val connected: AtomicBoolean, private val handle: Handle) : AutoCloseable {
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

        override fun close() {
            threadId.assertCurrentThread()
            if (!handle.isClosed) {
                connected.set(false)
                handle.close()
            }
        }
    }

    /**
     * A single database connection in read mode.
     *
     * Tied to the thread that created it, and throws `IllegalStateException` if used in the wrong thread.
     */
    open class Read internal constructor(val handle: Handle) {
        fun createQuery(@Language("sql") sql: String): Query = handle.createQuery(sql)
    }

    /**
     * A single database connection running a transaction.
     *
     * Tied to the thread that created it, and throws `IllegalStateException` if used in the wrong thread.
     */
    class Transaction internal constructor(handle: Handle) : Database.Read(handle) {
        private var savepointId: Long = 0

        fun nextSavepoint(): String = "savepoint-${savepointId++}"
        fun createUpdate(@Language("sql") sql: String): Update = handle.createUpdate(sql)
        fun prepareBatch(@Language("sql") sql: String): PreparedBatch = handle.prepareBatch(sql)
        fun execute(@Language("sql") sql: String, vararg args: Any): Int = handle.execute(sql, *args)

        fun <T> subTransaction(f: () -> T): T {
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

        companion object {
            /**
             * Wraps an existing raw JDBI handle into a `Transaction` object.
             *
             * This is mostly intended for tests where the main API can't be used. Use *very* sparingly!
             */
            fun wrap(handle: Handle): Transaction {
                check(handle.isInTransaction) { "Wrapped handle must have an active transaction" }
                check(!handle.isReadOnly) { "Wrapped handle must not be read-only" }
                return Transaction(handle)
            }
        }
    }
}

internal data class ThreadId(val id: Long = Thread.currentThread().id) {
    fun assertCurrentThread() = assert(Thread.currentThread().id == id) { "Database accessed from the wrong thread" }
}
