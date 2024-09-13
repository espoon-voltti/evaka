// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.db

import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.withSpan
import io.opentelemetry.api.trace.Tracer
import java.time.Duration
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import org.intellij.lang.annotations.Language
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.mapper.ColumnMapper
import org.jdbi.v3.core.qualifier.QualifiedType
import org.jdbi.v3.core.result.RowView
import org.jdbi.v3.core.statement.Slf4JSqlLogger
import org.jdbi.v3.json.Json
import org.slf4j.LoggerFactory

// What does it mean when a function accepts a Database/Database.* parameter?
//
//     fun doStuff(db: Database):
//         To call this function, you need to have a database reference *without* an active
// connection or transaction.
//         The function can connect/disconnect from the database 0 to N times, and do whatever it
// wants using the connection(s).
//     fun doStuff(db: Database.Connection)
//         To call this function, you need to have a lazy database connection *without* an active
// transaction.
//         The function can read/write the database and freely execute 0 to N individual
// transactions. It may also close the connection temporarily and reuse it afterwards.
//     fun doStuff(tx: Database.Read)
//         To call this function, you need to have an active read-only transaction.
//         The function can only read the database, and can't manage transactions by itself.
//     fun doStuff(tx: Database.Transaction)
//         To call this function, you need to have an active transaction.
//         The function can read/write the database, and can't manage transactions by itself.

/**
 * A database reference that can be used to obtain *one database connection at a time*.
 *
 * Tied to the thread that created it, and throws `IllegalStateException` if used in the wrong
 * thread.
 */
class Database(private val jdbi: Jdbi, private val tracer: Tracer) {
    private val threadId = ThreadId()
    private var hasOpenHandle = false

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
        check(!hasOpenHandle) { "Already connected to database" }
        return Connection(threadId, tracer, this::openHandle)
    }

    private fun openHandle(): Handle =
        jdbi.open().also {
            check(!hasOpenHandle) { "Already connected to database" }
            hasOpenHandle = true
            it.addCleanable { hasOpenHandle = false }
        }

    companion object {
        val sqlLogger = Slf4JSqlLogger(LoggerFactory.getLogger("fi.espoo.evaka.sql"))
    }

    /**
     * A single *possibly open* database connection tied to a single thread.
     *
     * Whenever a transaction is started, the underlying raw handle is opened lazily. Once the
     * connection is closed with `close()` and any new transaction will once again lazily open a raw
     * handle.
     */
    open class Connection
    internal constructor(
        private val threadId: ThreadId,
        private val tracer: Tracer,
        private val openRawHandle: () -> Handle,
    ) : AutoCloseable {
        private var rawHandle: Handle? = null

        private fun getRawHandle(): Handle = rawHandle ?: openRawHandle().also { rawHandle = it }

        /**
         * Enters read mode, runs the given function, and exits read mode regardless of any
         * exceptions the function may have thrown.
         *
         * Throws `IllegalStateException` if this database connection is already in read mode or a
         * transaction
         */
        fun <T> read(f: (db: Read) -> T): T {
            threadId.assertCurrentThread()
            val handle = this.getRawHandle()
            check(!handle.isInTransaction) { "Already in a transaction" }
            handle.isReadOnly = true
            try {
                return tracer.withSpan("db.transaction read") {
                    handle.inTransaction<T, Exception> { f(Read(handle)) }
                }
            } finally {
                handle.isReadOnly = false
            }
        }

        /**
         * Starts a transaction, runs the given function, and commits or rolls back the transaction
         * depending on whether the function threw an exception or not.
         *
         * Throws `IllegalStateException` if this database connection is already in read mode or a
         * transaction.
         */
        fun <T> transaction(f: (db: Transaction) -> T): T {
            threadId.assertCurrentThread()
            val handle = this.getRawHandle()
            check(!handle.isInTransaction) { "Already in a transaction" }
            val hooks = TransactionHooks()
            return tracer
                .withSpan("db.transaction read/write") {
                    handle.inTransaction<T, Exception> { f(Transaction(it, hooks)) }
                }
                .also { hooks.afterCommit.forEach { it() } }
        }

        override fun close() {
            threadId.assertCurrentThread()
            this.rawHandle?.close()
            this.rawHandle = null
        }
    }

    /**
     * A single database connection in read mode.
     *
     * Tied to the thread that created it, and throws `IllegalStateException` if used in the wrong
     * thread.
     */
    open class Read internal constructor(val handle: Handle) {
        @Deprecated("Use new query API instead")
        fun createQuery(@Language("sql") sql: String): Query = Query(handle.createQuery(sql))

        fun createQuery(f: QuerySql.Builder.() -> QuerySql): Query =
            @Suppress("DEPRECATION") createQuery(QuerySql.Builder().run { f(this) })

        fun createQuery(fragment: QuerySql): Query {
            val raw = handle.createQuery(fragment.sql.toString())
            for ((idx, binding) in fragment.bindings.withIndex()) {
                raw.bindByType(idx, binding.value, binding.type)
            }
            return Query(raw)
        }

        fun setLockTimeout(duration: Duration) =
            handle.execute("SET LOCAL lock_timeout = '${duration.toMillis()}ms'")

        fun setStatementTimeout(duration: Duration) =
            handle.execute("SET LOCAL statement_timeout = '${duration.toMillis()}ms'")
    }

    /**
     * A single database connection running a transaction.
     *
     * Tied to the thread that created it, and throws `IllegalStateException` if used in the wrong
     * thread.
     */
    class Transaction internal constructor(handle: Handle, private val hooks: TransactionHooks) :
        Read(handle) {
        private var savepointId: Long = 0

        fun nextSavepoint(): String = "savepoint-${savepointId++}"

        @Deprecated("Use new query API instead")
        fun createUpdate(@Language("sql") sql: String): Update = Update(handle.createUpdate(sql))

        fun createUpdate(f: QuerySql.Builder.() -> QuerySql): Update =
            @Suppress("DEPRECATION") createUpdate(QuerySql.Builder().run { f(this) })

        fun createUpdate(fragment: QuerySql): Update {
            val raw = handle.createUpdate(fragment.sql.toString())
            for ((idx, binding) in fragment.bindings.withIndex()) {
                raw.bindByType(idx, binding.value, binding.type)
            }
            return Update(raw)
        }

        fun execute(f: QuerySql.Builder.() -> QuerySql): Int =
            createUpdate(QuerySql.Builder().run { f(this) }).execute()

        fun <R> prepareBatch(f: BatchSql.Builder<R>.() -> BatchSql<R>): PreparedBatch<R> {
            val batch = BatchSql.Builder<R>().run { f(this) }
            val raw = handle.prepareBatch(batch.sql.toString())
            return PreparedBatch(raw, batch.bindings)
        }

        fun <R> prepareBatch(
            rows: Iterable<R>,
            f: BatchSql.Builder<R>.() -> BatchSql<R>,
        ): PreparedBatch<R> = prepareBatch(f).addAll(rows)

        fun <R> prepareBatch(
            rows: Sequence<R>,
            f: BatchSql.Builder<R>.() -> BatchSql<R>,
        ): PreparedBatch<R> = prepareBatch(f).addAll(rows)

        fun <R> executeBatch(
            rows: Iterable<R>,
            f: BatchSql.Builder<R>.() -> BatchSql<R>,
        ): IntArray = prepareBatch(f).addAll(rows).execute()

        fun <R> executeBatch(
            rows: Sequence<R>,
            f: BatchSql.Builder<R>.() -> BatchSql<R>,
        ): IntArray = prepareBatch(f).addAll(rows).execute()

        /**
         * Registers a function to be called after this transaction has been committed successfully.
         *
         * If the exactly same function (= object instance) has already been registered, this is a
         * no-op.
         */
        fun afterCommit(f: () -> Unit) {
            hooks.afterCommit += f
        }

        fun <T> subTransaction(f: () -> T): T {
            val savepointName = nextSavepoint()
            handle.savepoint(savepointName)
            val result =
                try {
                    f()
                } catch (e: Throwable) {
                    try {
                        handle.rollbackToSavepoint(savepointName)
                    } catch (rollback: Exception) {
                        e.addSuppressed(rollback)
                    }
                    throw e
                }
            handle.releaseSavepoint(savepointName)
            return result
        }

        companion object {
            /**
             * Wraps an existing raw JDBI handle into a `Transaction` object.
             *
             * This is mostly intended for tests where the main API can't be used. Use *very*
             * sparingly!
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

        inline fun <reified T> bind(name: String, value: T): This =
            bindByType(name, value, createQualifiedType(*defaultQualifiers<T>()))

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

        fun bindJson(name: String, value: Any): This =
            bindByType(
                name,
                value,
                QualifiedType.of(value.javaClass).withAnnotationClasses(listOf(Json::class.java)),
            )

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

        fun logFinalSql(): This {
            raw.setSqlLogger(Database.sqlLogger)
            return self()
        }
    }

    class Query internal constructor(override val raw: org.jdbi.v3.core.statement.Query) :
        SqlStatement<Query>(), ResultBearing {
        override fun self(): Query = this

        fun setFetchSize(fetchSize: Int): Query = this.also { raw.setFetchSize(fetchSize) }

        /** Runs the query and maps the results automatically to a list */
        inline fun <reified T> toList(
            vararg qualifiers: KClass<out Annotation> = defaultQualifiers<T>()
        ): List<T> = mapTo(createQualifiedType<T>(*qualifiers)).toList()

        /** Runs the query and maps the results automatically to a set */
        inline fun <reified T> toSet(
            vararg qualifiers: KClass<out Annotation> = defaultQualifiers<T>()
        ): Set<T> = mapTo(createQualifiedType<T>(*qualifiers)).toSet()

        /** Runs the query, checks that it returns exactly one row, and maps it automatically */
        inline fun <reified T> exactlyOne(
            vararg qualifiers: KClass<out Annotation> = defaultQualifiers<T>()
        ): T = mapTo(createQualifiedType<T>(*qualifiers)).exactlyOne()

        /**
         * Runs the query, checks that it returns at most one row, and maps it automatically if it
         * exists
         */
        inline fun <reified T> exactlyOneOrNull(
            vararg qualifiers: KClass<out Annotation> = defaultQualifiers<T>()
        ): T? = mapTo(createQualifiedType<T>(*qualifiers)).exactlyOneOrNull()

        inline fun <reified T> mapTo(
            qualifiers: Array<KClass<out Annotation>> = defaultQualifiers<T>()
        ): Result<T> = mapTo(createQualifiedType(*qualifiers))

        override fun <T> mapTo(type: QualifiedType<T>): Result<T> = Result(raw.mapTo(type))

        override fun <T> map(mapper: Row.() -> T): Result<T> =
            Result(raw.map { row -> mapper(Row(row)) })
    }

    @JvmInline
    value class Result<T> private constructor(private val rows: ResultSequence<T>) {
        internal constructor(
            inner: org.jdbi.v3.core.result.ResultIterable<T>
        ) : this(ResultSequence(inner))

        /**
         * Processes the query result rows in a callback.
         *
         * This function makes it possible to process the rows using normal Kotlin collection
         * functions (e.g. fold, groupBy, associateBy, etc...) without first collecting everything
         * into a temporary list and then throwing the list away.
         *
         * There are two rules, enforced with checks that throw errors if violated:
         * - the sequence may only be iterated once
         * - the sequence and its iterator may not be used after the callback has finished
         */
        fun <R> useSequence(f: (Sequence<T>) -> R): R = rows.use { f(it) }

        /**
         * Processes the query result rows in a callback taking an Iterable.
         *
         * This function makes it possible to process the rows using normal Kotlin collection
         * functions (e.g. fold, groupBy, associateBy, etc...) without first collecting everything
         * into a temporary list and then throwing the list away.
         *
         * Consider using `useSequence` instead if you intend to avoid temporary throw-away
         * collections, because most `Iterable` functions return concrete intermediate collections,
         * while most `Sequence` functions return lazy sequences.
         *
         * There are two rules, enforced with checks that throw errors if violated:
         * - the iterable may only be iterated once
         * - the iterable and its iterator may not be used after the callback has finished
         */
        fun <R> useIterable(f: (Iterable<T>) -> R): R = rows.use { f(it.asIterable()) }

        fun toList(): List<T> = rows.use { it.toList() }

        fun toSet(): Set<T> = rows.use { it.toSet() }

        fun exactlyOne(): T =
            rows.use {
                val iterator = it.iterator()
                if (!iterator.hasNext()) error("Expected exactly one result, got none")
                val result = iterator.next()
                if (iterator.hasNext()) error("Expected exactly one result, got more than one")
                result
            }

        fun exactlyOneOrNull(): T? =
            rows.use {
                val iterator = it.iterator()
                if (!iterator.hasNext()) null
                else {
                    val result = iterator.next()
                    if (iterator.hasNext()) error("Expected 0-1 results, got more than one")
                    result
                }
            }
    }

    class Update internal constructor(override val raw: org.jdbi.v3.core.statement.Update) :
        SqlStatement<Update>() {
        override fun self(): Update = this

        fun execute() = raw.execute()

        fun executeAndReturnGeneratedKeys(): UpdateResult =
            UpdateResult(raw.executeAndReturnGeneratedKeys())

        fun updateExactlyOne(
            notFoundMsg: String = "Not found",
            foundMultipleMsg: String = "Found multiple",
        ) {
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

    class LegacyPreparedBatch
    internal constructor(private val raw: org.jdbi.v3.core.statement.PreparedBatch) {

        fun add(): LegacyPreparedBatch {
            raw.add()
            return this
        }

        fun execute(): IntArray = raw.execute()

        fun executeAndReturn(): UpdateResult = UpdateResult(raw.executePreparedBatch())

        inline fun <reified T> bind(name: String, value: T): LegacyPreparedBatch =
            bindByType(name, value, createQualifiedType(*defaultQualifiers<T>()))

        inline fun <reified T> registerColumnMapper(mapper: ColumnMapper<T>): LegacyPreparedBatch =
            registerColumnMapper(createQualifiedType(), mapper)

        fun <T> registerColumnMapper(
            type: QualifiedType<T>,
            mapper: ColumnMapper<T>,
        ): LegacyPreparedBatch {
            raw.registerColumnMapper(type, mapper)
            return this
        }

        fun addBinding(binding: Binding<*>): LegacyPreparedBatch {
            raw.bindByType(binding.name, binding.value, binding.type)
            return this
        }

        fun addBindings(bindings: Iterable<Binding<*>>): LegacyPreparedBatch {
            for (binding in bindings) {
                raw.bindByType(binding.name, binding.value, binding.type)
            }
            return this
        }

        fun addBindings(bindings: Sequence<Binding<*>>): LegacyPreparedBatch {
            for (binding in bindings) {
                raw.bindByType(binding.name, binding.value, binding.type)
            }
            return this
        }

        fun bindJson(name: String, value: Any): LegacyPreparedBatch =
            bindByType(
                name,
                value,
                QualifiedType.of(value.javaClass).withAnnotationClasses(listOf(Json::class.java)),
            )

        fun <T> bindByType(name: String, value: T, type: QualifiedType<T>): LegacyPreparedBatch {
            raw.bindByType(name, value, type)
            return this
        }

        fun bindKotlin(value: Any): LegacyPreparedBatch {
            raw.bindKotlin(value)
            return this
        }

        fun bindKotlin(name: String, value: Any): LegacyPreparedBatch {
            raw.bindKotlin(name, value)
            return this
        }
    }

    class PreparedBatch<R>
    internal constructor(
        private val raw: org.jdbi.v3.core.statement.PreparedBatch,
        private val bindings: List<BatchBinding<R, *>>,
    ) {
        fun execute(): IntArray = raw.execute()

        fun executeAndReturn(): UpdateResult = UpdateResult(raw.executePreparedBatch())

        inline fun <reified T> registerColumnMapper(mapper: ColumnMapper<T>): PreparedBatch<R> =
            registerColumnMapper(createQualifiedType(), mapper)

        fun <T> registerColumnMapper(
            type: QualifiedType<T>,
            mapper: ColumnMapper<T>,
        ): PreparedBatch<R> {
            raw.registerColumnMapper(type, mapper)
            return this
        }

        private fun bindAll(row: R) {
            for ((idx, binding) in bindings.withIndex()) {
                when (binding) {
                    is ValueBinding<*> -> {
                        raw.bindByType(idx, binding.value, binding.type)
                    }
                    is LazyBinding<R, *> -> {
                        raw.bindByType(idx, binding.getValue(row), binding.getType(row))
                    }
                }
            }
        }

        fun add(row: R): PreparedBatch<R> {
            bindAll(row)
            raw.add()
            return this
        }

        fun addAll(rows: Iterable<R>): PreparedBatch<R> {
            for (row in rows) {
                bindAll(row)
                raw.add()
            }
            return this
        }

        fun addAll(rows: Sequence<R>): PreparedBatch<R> {
            for (row in rows) {
                bindAll(row)
                raw.add()
            }
            return this
        }
    }

    @JvmInline
    value class UpdateResult(private val raw: org.jdbi.v3.core.result.ResultBearing) :
        ResultBearing {
        /** Runs the query and maps the results automatically to a list */
        inline fun <reified T : Any> toList(
            vararg qualifiers: KClass<out Annotation> = defaultQualifiers<T>()
        ): List<T> = mapTo(createQualifiedType<T>(*qualifiers)).toList()

        /** Runs the query and maps the results automatically to a set */
        inline fun <reified T : Any> toSet(
            vararg qualifiers: KClass<out Annotation> = defaultQualifiers<T>()
        ): Set<T> = mapTo(createQualifiedType<T>(*qualifiers)).toSet()

        /** Runs the query, checks that it returns exactly one row, and maps it automatically */
        inline fun <reified T : Any> exactlyOne(
            vararg qualifiers: KClass<out Annotation> = defaultQualifiers<T>()
        ): T = mapTo(createQualifiedType<T>(*qualifiers)).exactlyOne()

        /**
         * Runs the query, checks that it returns at most one row, and maps it automatically if it
         * exists
         */
        inline fun <reified T : Any> exactlyOneOrNull(
            vararg qualifiers: KClass<out Annotation> = defaultQualifiers<T>()
        ): T? = mapTo(createQualifiedType<T>(*qualifiers)).exactlyOneOrNull()

        inline fun <reified T> mapTo(
            qualifiers: Array<out KClass<out Annotation>> = defaultQualifiers<T>()
        ): Result<T> = mapTo(createQualifiedType(*qualifiers))

        override fun <T> mapTo(type: QualifiedType<T>): Result<T> = Result(raw.mapTo(type))

        override fun <T> map(mapper: Row.() -> T): Result<T> =
            Result(raw.map { row -> mapper(Row(row)) })
    }

    interface ResultBearing {
        fun <T> mapTo(type: QualifiedType<T>): Result<T>

        fun <T> map(mapper: Row.() -> T): Result<T>

        /** Runs the query and maps the results to a list using the given mapper */
        fun <T> toList(mapper: Row.() -> T): List<T> = map(mapper).toList()

        /** Runs the query and maps the results to a set using the given mapper */
        fun <T> toSet(mapper: Row.() -> T): Set<T> = map(mapper).toSet()

        /** Runs the query and maps the results to a map using the given mapper */
        fun <K, V> toMap(mapper: Row.() -> Pair<K, V>): Map<K, V> =
            map(mapper).useIterable { it.toMap() }

        /**
         * Runs the query, checks that it returns exactly one row, and maps it using the given
         * mapper
         */
        fun <T> exactlyOne(mapper: Row.() -> T): T = map(mapper).exactlyOne()

        /**
         * Runs the query, checks that it returns at most one row, and maps it using the given
         * mapper if it exists
         */
        fun <T> exactlyOneOrNull(mapper: Row.() -> T): T? = map(mapper).exactlyOneOrNull()
    }
}

internal data class TransactionHooks(val afterCommit: LinkedHashSet<() -> Unit> = LinkedHashSet())

internal data class ThreadId(val id: Long = Thread.currentThread().threadId()) {
    fun assertCurrentThread() =
        assert(Thread.currentThread().threadId() == id) {
            "Database accessed from the wrong thread"
        }
}

data class Binding<T>(val name: String, val value: T, val type: QualifiedType<T>) {
    companion object {
        inline fun <reified T> of(
            name: String,
            value: T,
            qualifiers: Array<out KClass<out Annotation>> = defaultQualifiers<T>(),
        ) = Binding(name, value, createQualifiedType(*qualifiers))
    }
}

sealed interface BatchBinding<out R, T>

data class LazyBinding<R, T>(
    val getValue: (row: R) -> T,
    val getType: (row: R) -> QualifiedType<T>,
) : BatchBinding<R, T> {
    companion object {
        inline fun <R, reified T> of(
            noinline getValue: (row: R) -> T,
            noinline getQualifiers: (value: T) -> Array<KClass<out Annotation>> = {
                defaultQualifiers<T>()
            },
        ) = LazyBinding(getValue) { createQualifiedType(*getQualifiers(getValue(it))) }
    }
}

data class ValueBinding<T>(val value: T, val type: QualifiedType<T>) : BatchBinding<Nothing, T> {
    companion object {
        inline fun <reified T> of(
            value: T,
            qualifiers: Array<out KClass<out Annotation>> = defaultQualifiers<T>(),
        ) = ValueBinding(value, createQualifiedType(*qualifiers))
    }
}

@JvmInline
value class QuerySqlString(@Language("sql") private val sql: String) {
    override fun toString(): String = sql
}

abstract class SqlBuilder {
    protected abstract fun addBinding(binding: ValueBinding<*>)

    /**
     * Binds the given value as a query parameter using the default serialization for the value's
     * compile-time type.
     *
     * This function scans default qualifiers specified on the type, so types annotated with `@Json`
     * are automatically always serialized as JSON.
     */
    inline fun <reified T> bind(value: T): Binding =
        bind(ValueBinding.of(value, defaultQualifiers<T>()))

    /**
     * Binds the given value as a query parameter using JSON serialization.
     *
     * This function ignores default qualifiers specified on the type, and explicitly chooses JSON
     * serialization.
     */
    inline fun <reified T> bindJson(value: T): Binding =
        bind(
            ValueBinding(
                value,
                // Use runtime type information for non-null values with inheritance
                // Otherwise Jackson will serialize only the fields in T which might be a
                // superclass while the runtime value might be a concrete subclass
                if (value is Any && value.javaClass != T::class.java)
                    QualifiedType.of(value.javaClass).with(Json::class.java)
                // Use compile-time type information for other values, including nulls
                else createQualifiedType(Json::class),
            )
        )

    fun bind(binding: ValueBinding<*>): Binding {
        addBinding(binding)
        return Binding
    }

    fun subquery(f: QuerySql.Builder.() -> QuerySql): QuerySqlString = subquery(QuerySql { f() })

    fun subquery(fragment: QuerySql): QuerySqlString {
        fragment.bindings.forEach(this::addBinding)
        return fragment.sql
    }

    fun predicate(predicate: PredicateSql): PredicateSqlString {
        predicate.bindings.forEach(this::addBinding)
        return predicate.sql
    }

    /** A marker type used for bound parameters that can be used in a template string */
    object Binding {
        override fun toString(): String = "?"
    }
}

/** A builder for SQL, including bound parameter values. */
data class QuerySql(val sql: QuerySqlString, val bindings: List<ValueBinding<out Any?>>) {
    companion object {
        operator fun invoke(f: Builder.() -> QuerySql): QuerySql = Builder().run { f(this) }
    }

    class Builder : SqlBuilder() {
        private var bindings: List<ValueBinding<out Any?>> = listOf()
        private var used: Boolean = false

        override fun addBinding(binding: ValueBinding<*>) {
            this.bindings += binding
        }

        private fun combine(separator: String, queries: Iterable<QuerySql>): QuerySql {
            check(!used) { "builder has already been used" }
            this.used = true
            for (query in queries) {
                bindings += query.bindings
            }
            return QuerySql(
                QuerySqlString(queries.joinToString(separator) { it.sql.toString() }),
                bindings,
            )
        }

        fun union(all: Boolean, queries: Iterable<QuerySql>): QuerySql =
            combine(if (all) "\nUNION ALL\n" else "\nUNION\n", queries)

        fun sql(@Language("sql") sql: String): QuerySql {
            check(!used) { "builder has already been used" }
            this.used = true
            return QuerySql(QuerySqlString(sql), bindings)
        }
    }
}

data class BatchSql<R>(val sql: QuerySqlString, val bindings: List<BatchBinding<R, out Any?>>) {
    companion object {
        fun <R> of(f: Builder<R>.() -> BatchSql<R>): BatchSql<R> = Builder<R>().run { f(this) }
    }

    class Builder<R> : SqlBuilder() {
        private var bindings: List<BatchBinding<R, out Any?>> = emptyList()
        private var used: Boolean = false

        override fun addBinding(binding: ValueBinding<*>) {
            this.bindings += binding
        }

        fun bind(binding: LazyBinding<R, *>): Binding {
            this.bindings += binding
            return Binding
        }

        inline fun <reified T> bind(noinline getValue: (row: R) -> T): Binding =
            bind(LazyBinding.of(getValue))

        /**
         * Binds a value returned by the given getter as a query parameter using JSON serialization.
         *
         * This function ignores default qualifiers specified on the type, and explicitly chooses
         * JSON serialization.
         */
        inline fun <reified T> bindJson(noinline getValue: (row: R) -> T): Binding =
            bind(
                LazyBinding(getValue) { row ->
                    val value = getValue(row)
                    // Use runtime type information for non-null values with inheritance
                    // Otherwise Jackson will serialize only the fields in T which might be a
                    // superclass while the runtime value might be a concrete subclass
                    if (value is Any && value.javaClass != T::class.java)
                        QualifiedType.of(value.javaClass).with(Json::class.java)
                    // Use compile-time type information for other values, including nulls
                    else createQualifiedType(Json::class)
                }
            )

        fun sql(@Language("sql") sql: String): BatchSql<R> {
            check(!used) { "builder has already been used" }
            this.used = true
            return BatchSql(QuerySqlString(sql), bindings)
        }
    }
}

@JvmInline
value class Row(private val row: RowView) {
    inline fun <reified K, reified V> columnPair(
        firstColumn: String,
        secondColumn: String,
    ): Pair<K, V> = column<K>(firstColumn) to column<V>(secondColumn)

    inline fun <reified T> column(name: String, vararg annotations: KClass<out Annotation>): T {
        val type = createQualifiedType<T>(*annotations)
        val value = column(name, type)
        if (null !is T && value == null) {
            error("Non-nullable column $name was null")
        }
        return value
    }

    fun <T> column(name: String, type: QualifiedType<T>): T = row.getColumn(name, type)

    inline fun <reified T> jsonColumn(name: String): T = column(name, Json::class)

    inline fun <reified T> row(): T = row(typeOf<T>()) as T

    fun row(type: KType): Any? = row.getRow(type.asJdbiJavaType())
}

/**
 * A custom wrapper for JDBI ResultIterable that allows getting the iterator exactly once.
 *
 * The original JDBI implementation allows getting multiple iterators which all use the underlying
 * JDBC ResultSet, leading to unexpected behaviour.
 */
private class ResultSequence<T>(iterable: org.jdbi.v3.core.result.ResultIterable<T>) :
    Sequence<T>, AutoCloseable {
    private var iterator: ResultIterator<T>? = ResultIterator(iterable.iterator())

    override fun iterator(): Iterator<T> =
        iterator.also { iterator = null } ?: error("The iterator has already been taken")

    override fun close() {
        iterator?.close()
    }
}

/**
 * A custom wrapper for JDBI ResultIterator that throws an error if used after it has been closed.
 *
 * The original JDBI implementation just silently stops returning results if closed.
 */
private class ResultIterator<T>(private val inner: org.jdbi.v3.core.result.ResultIterator<T>) :
    Iterator<T>, AutoCloseable {
    private var closed = false

    override fun hasNext(): Boolean =
        if (closed) error("The iterator has already been closed") else inner.hasNext()

    override fun next(): T =
        if (closed) error("The iterator has already been closed") else inner.next()

    override fun close() {
        closed = true
        inner.close()
    }
}
