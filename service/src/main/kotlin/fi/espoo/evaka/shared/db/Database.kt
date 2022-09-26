// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.db

import fi.espoo.evaka.shared.domain.NotFound
import org.intellij.lang.annotations.Language
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.mapper.ColumnMapper
import org.jdbi.v3.core.mapper.RowViewMapper
import org.jdbi.v3.core.qualifier.QualifiedType
import org.jdbi.v3.core.result.ResultIterable
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.KClass

// What does it mean when a function accepts a Database/Database.* parameter?
//
//     fun doStuff(db: Database):
//         To call this function, you need to have a database reference *without* an active connection or transaction.
//         The function can connect/disconnect from the database 0 to N times, and do whatever it wants using the connection(s).
//     fun doStuff(db: Database.Connection)
//         To call this function, you need to have a lazy database connection *without* an active transaction.
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
    fun <T> connect(f: (db: Connection) -> T): T = connectWithManualLifecycle().use(f)

    /**
     * Opens a new database connection and returns it. The connection *must be closed after use*.
     *
     * Throws `IllegalStateException` if a connection is already open
     */
    fun connectWithManualLifecycle(): Connection {
        threadId.assertCurrentThread()
        check(!connected.get()) { "Already connected to database" }
        return Connection(threadId, connected, lazy(LazyThreadSafetyMode.NONE) { jdbi.open() })
    }

    /**
     * A single lazily initialized database connection tied to a single thread
     */
    open class Connection internal constructor(private val threadId: ThreadId, private val connected: AtomicBoolean, private val lazyHandle: Lazy<Handle>) : AutoCloseable {
        private fun getHandle(): Handle {
            threadId.assertCurrentThread()
            return if (lazyHandle.isInitialized()) {
                lazyHandle.value
            } else {
                val wasConnected = connected.getAndSet(true)
                check(!wasConnected) { "Already connected to database" }
                lazyHandle.value
            }
        }
        /**
         * Enters read mode, runs the given function, and exits read mode regardless of any exceptions the function may have thrown.
         *
         * Throws `IllegalStateException` if this database connection is already in read mode or a transaction
         */
        fun <T> read(f: (db: Read) -> T): T {
            val handle = this.getHandle()
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
            val handle = this.getHandle()
            check(!handle.isInTransaction) { "Already in a transaction" }
            val hooks = TransactionHooks()
            return handle.inTransaction<T, Exception> { f(Transaction(it, hooks)) }.also {
                hooks.afterCommit.forEach { it() }
            }
        }

        fun isConnected(): Boolean = connected.get()

        override fun close() {
            threadId.assertCurrentThread()
            if (lazyHandle.isInitialized()) {
                val handle = lazyHandle.value
                if (!handle.isClosed) {
                    connected.set(false)
                    handle.close()
                }
            }
        }
    }

    /**
     * A single database connection in read mode.
     *
     * Tied to the thread that created it, and throws `IllegalStateException` if used in the wrong thread.
     */
    open class Read internal constructor(val handle: Handle) {
        fun createQuery(@Language("sql") sql: String): Query = Query(handle.createQuery(sql))
        fun createQuery(fragment: QueryFragment<*>): Query = Query(handle.createQuery(fragment.sql)).apply {
            addBindings(fragment.bindings)
        }

        fun setLockTimeout(duration: Duration) = handle.execute("SET LOCAL lock_timeout = '${duration.toMillis()}ms'")
        fun setStatementTimeout(duration: Duration) = handle.execute("SET LOCAL statement_timeout = '${duration.toMillis()}ms'")
    }

    /**
     * A single database connection running a transaction.
     *
     * Tied to the thread that created it, and throws `IllegalStateException` if used in the wrong thread.
     */
    class Transaction internal constructor(handle: Handle, private val hooks: TransactionHooks) : Read(handle) {
        private var savepointId: Long = 0

        fun nextSavepoint(): String = "savepoint-${savepointId++}"
        fun createUpdate(@Language("sql") sql: String): Update = Update(handle.createUpdate(sql))
        fun prepareBatch(@Language("sql") sql: String): PreparedBatch = PreparedBatch(handle.prepareBatch(sql))
        fun execute(@Language("sql") sql: String, vararg args: Any): Int = handle.execute(sql, *args)

        /**
         * Registers a function to be called after this transaction has been committed successfully.
         *
         * If the exactly same function (= object instance) has already been registered, this is a no-op.
         */
        fun afterCommit(f: () -> Unit) {
            hooks.afterCommit += f
        }

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
                return Transaction(handle, TransactionHooks())
            }
        }
    }

    abstract class SqlStatement<This : SqlStatement<This>> {
        protected abstract fun self(): This
        protected abstract val raw: org.jdbi.v3.core.statement.SqlStatement<*>

        inline fun <reified T> bind(name: String, value: T, qualifiers: Array<out KClass<out Annotation>> = defaultQualifiers<T>()): This =
            bindByType(name, value, createQualifiedType(*qualifiers))

        inline fun <reified T> registerColumnMapper(mapper: ColumnMapper<T>): This =
            registerColumnMapper(createQualifiedType(), mapper)

        fun <T> registerColumnMapper(type: QualifiedType<T>, mapper: ColumnMapper<T>): This {
            raw.registerColumnMapper(type, mapper)
            return self()
        }

        fun addBinding(binding: Binding<*>): This {
            raw.bindByType(binding.name, binding.value, binding.type)
            return self()
        }

        fun addBindings(bindings: Iterable<Binding<*>>): This {
            for (binding in bindings) {
                raw.bindByType(binding.name, binding.value, binding.type)
            }
            return self()
        }

        fun addBindings(bindings: Sequence<Binding<*>>): This {
            for (binding in bindings) {
                raw.bindByType(binding.name, binding.value, binding.type)
            }
            return self()
        }

        fun <T> bindByType(name: String, value: T, type: QualifiedType<T>): This {
            raw.bindByType(name, value, type)
            return self()
        }

        fun bindKotlin(value: Any): This {
            raw.bindKotlin(value)
            return self()
        }

        fun bindKotlin(name: String, value: Any): This {
            raw.bindKotlin(name, value)
            return self()
        }
    }

    class Query internal constructor(override val raw: org.jdbi.v3.core.statement.Query) : SqlStatement<Query>(), ResultBearing {
        override fun self(): Query = this

        inline fun <reified T> mapTo(qualifiers: Array<KClass<out Annotation>> = defaultQualifiers<T>()): ResultIterable<T> =
            mapTo(createQualifiedType(*qualifiers))

        override fun <T> mapTo(type: QualifiedType<T>): ResultIterable<T> = raw.mapTo(type)
        override fun <T> map(mapper: ColumnMapper<T>): ResultIterable<T> = raw.map(mapper)
        override fun <T> map(mapper: RowViewMapper<T>): ResultIterable<T> = raw.map(mapper)
    }

    class Update internal constructor(override val raw: org.jdbi.v3.core.statement.Update) : SqlStatement<Update>() {
        override fun self(): Update = this
        fun execute() = raw.execute()

        fun executeAndReturnGeneratedKeys(): UpdateResult = UpdateResult(raw.executeAndReturnGeneratedKeys())

        fun updateExactlyOne(notFoundMsg: String = "Not found", foundMultipleMsg: String = "Found multiple") {
            val rows = this.execute()
            if (rows == 0) throw NotFound(notFoundMsg)
            if (rows > 1) throw Error(foundMultipleMsg)
        }

        fun updateNoneOrOne(foundMultipleMsg: String = "Found multiple"): Int {
            val rows = this.execute()
            if (rows > 1) throw Error(foundMultipleMsg)
            return rows
        }
    }

    class PreparedBatch internal constructor(override val raw: org.jdbi.v3.core.statement.PreparedBatch) : SqlStatement<PreparedBatch>() {
        override fun self(): PreparedBatch = this

        fun add(): PreparedBatch {
            raw.add()
            return this
        }
        fun execute(): IntArray = raw.execute()

        fun executeAndReturn(): UpdateResult = UpdateResult(raw.executePreparedBatch())
    }

    @JvmInline
    value class UpdateResult(private val raw: org.jdbi.v3.core.result.ResultBearing) : ResultBearing {
        inline fun <reified T> mapTo(qualifiers: Array<out KClass<out Annotation>> = defaultQualifiers<T>()): ResultIterable<T> =
            mapTo(createQualifiedType(*qualifiers))

        override fun <T> mapTo(type: QualifiedType<T>): ResultIterable<T> = raw.mapTo(type)
        override fun <T> map(mapper: ColumnMapper<T>): ResultIterable<T> = raw.map(mapper)
        override fun <T> map(mapper: RowViewMapper<T>): ResultIterable<T> = raw.map(mapper)
    }

    interface ResultBearing {
        fun <T> mapTo(type: QualifiedType<T>): ResultIterable<T>
        fun <T> map(mapper: ColumnMapper<T>): ResultIterable<T>
        fun <T> map(mapper: RowViewMapper<T>): ResultIterable<T>
    }
}

internal data class TransactionHooks(val afterCommit: LinkedHashSet<() -> Unit> = LinkedHashSet())

internal data class ThreadId(val id: Long = Thread.currentThread().id) {
    fun assertCurrentThread() = assert(Thread.currentThread().id == id) { "Database accessed from the wrong thread" }
}

data class Binding<T>(val name: String, val value: T, val type: QualifiedType<T>) {
    companion object {
        inline fun <reified T> of(name: String, value: T, qualifiers: Array<out KClass<out Annotation>> = defaultQualifiers<T>()) =
            Binding(name, value, createQualifiedType(*qualifiers))
    }
}

/**
 * Some fragment of SQL, including bound parameter values.
 *
 * This is *very dynamic* and has almost no compile-time checks, but the phantom type parameter `Tag` can be used to
 * assign some type to a query fragment for documentation purpose and to prevent mixing different types of query fragments.
 */
data class QueryFragment<@Suppress("unused") Tag>(
    @Language("sql")
    val sql: String,
    val bindings: List<Binding<out Any?>> = emptyList()
) {
    inline fun <reified T> bind(name: String, value: T, qualifiers: Array<out KClass<out Annotation>> = defaultQualifiers<T>()): QueryFragment<Tag> =
        bind(Binding.of(name, value, qualifiers))

    fun <T> bind(binding: Binding<T>): QueryFragment<Tag> = copy(bindings = bindings + binding)
}
