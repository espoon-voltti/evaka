// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.db

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import fi.espoo.evaka.shared.domain.ClosedPeriod
import fi.espoo.evaka.shared.domain.Coordinate
import fi.espoo.evaka.shared.domain.Period
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.core.qualifier.QualifiedType
import org.jdbi.v3.core.result.RowView
import org.jdbi.v3.core.statement.SqlStatement
import org.jdbi.v3.core.statement.StatementContext
import org.jdbi.v3.jackson2.Jackson2Config
import org.jdbi.v3.jackson2.Jackson2Plugin
import org.jdbi.v3.json.Json
import org.jdbi.v3.postgres.PostgresPlugin
import org.springframework.jdbc.datasource.DataSourceUtils
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.DefaultTransactionDefinition
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.sql.ResultSet
import javax.sql.DataSource

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

fun configureJdbi(jdbi: Jdbi): Jdbi {
    jdbi.installPlugin(KotlinPlugin())
        .installPlugin(PostgresPlugin())
        .installPlugin(Jackson2Plugin())
    jdbi.getConfig(Jackson2Config::class.java).mapper = ObjectMapper()
        .registerModule(JavaTimeModule())
        .registerModule(Jdk8Module())
        .registerModule(ParameterNamesModule())
        .registerModule(KotlinModule())
    jdbi.registerArgument(closedPeriodArgumentFactory)
    jdbi.registerArgument(periodArgumentFactory)
    jdbi.registerArgument(coordinateArgumentFactory)
    jdbi.registerArgument(identityArgumentFactory)
    jdbi.registerColumnMapper(ClosedPeriod::class.java, closedPeriodColumnMapper)
    jdbi.registerColumnMapper(Period::class.java, periodColumnMapper)
    jdbi.registerColumnMapper(Coordinate::class.java, coordinateColumnMapper)
    return jdbi
}

/**
 * Binds a nullable argument to an SQL statement.
 *
 * SqlStatement.`bind` can't handle null values, because it figures out the type from the passed value, and if the
 * value is null, there is no type information available. This function uses Kotlin reified types to figure out the type
 * at compile-time.
 */
inline fun <reified T : Any, This : SqlStatement<This>> SqlStatement<This>.bindNullable(name: String, value: T?): This =
    this.bindByType(name, value, QualifiedType.of(T::class.java))

inline fun <reified T : Any, This : SqlStatement<This>> SqlStatement<This>.bindNullable(name: String, value: Collection<T>?): This =
    this.bindNullable(name, value?.toTypedArray())

/**
 * Maps a result set column to a value.
 *
 * This function is often better than rs.getXXX() functions, because configured Jdbi column mappers are
 * used when appropriate and it's not restricted to types supported by the underlying ResultSet.
 */
inline fun <reified T : Any> StatementContext.mapColumn(rs: ResultSet, name: String): T =
    mapNullableColumn(rs, name) ?: throw IllegalStateException("Non-nullable column $name was null")

/**
 * Maps a result set column to a nullable value.
 *
 * This function is often better than rs.getXXX() functions, because configured Jdbi column mappers are
 * used when appropriate and it's not restricted to types supported by the underlying ResultSet.
 */
inline fun <reified T : Any> StatementContext.mapNullableColumn(rs: ResultSet, name: String): T? =
    findColumnMapperFor(T::class.java).orElseThrow {
        throw IllegalStateException("No column mapper found for type ${T::class}")
    }.map(rs, name, this)

/**
 * Runs a function within a Spring-managed transaction.
 *
 * Similar to annotating a method with @Transactional, and calling that method from outside declaring class so Spring
 * reflection-based proxying works.
 */
fun <T> withSpringTx(
    txManager: PlatformTransactionManager,
    readOnly: Boolean = false,
    requiresNew: Boolean = false,
    f: () -> T
): T {
    val status = txManager.getTransaction(
        DefaultTransactionDefinition().apply {
            this.isReadOnly = readOnly
            this.propagationBehavior = if (requiresNew) {
                TransactionDefinition.PROPAGATION_REQUIRES_NEW
            } else {
                TransactionDefinition.PROPAGATION_REQUIRED
            }
        }
    )
    try {
        val result = f()
        txManager.commit(status)
        return result
    } catch (e: RuntimeException) {
        txManager.rollback(status)
        throw e
    }
}

/**
 * Acquires the current thread-local Spring JDBC connection for the given data source, converts it into a JDBI handle,
 * runs the given function, and closes the connection.
 */
inline fun <T> withSpringHandle(dataSource: DataSource, crossinline f: (Handle) -> T): T {
    val connection = DataSourceUtils.getConnection(dataSource)
    try {
        return configureJdbi(Jdbi.create(connection)).handle(f)
    } finally {
        DataSourceUtils.releaseConnection(connection, dataSource)
    }
}

/**
 * Registers a callback to be called after the current transaction is committed.
 *
 * If the transaction is rolled back, the function will not be called.
 * Does nothing if no transaction is active.
 */
fun runAfterCommit(f: () -> Unit) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCommit() {
                f()
            }
        })
    }
}

/**
 * Maps a row column to a value.
 *
 * This function works with Kotlin better than row.getColumn().
 */
inline fun <reified T> RowView.mapColumn(name: String, type: QualifiedType<T> = QualifiedType.of(T::class.java)): T {
    val value = getColumn(name, type)
    if (null !is T && value == null) {
        throw throw IllegalStateException("Non-nullable column $name was null")
    }
    return value
}

/**
 * Maps a row json column to a value.
 */
inline fun <reified T : Any> RowView.mapJsonColumn(name: String): T =
    mapColumn(name, QualifiedType.of(T::class.java).with(Json::class.java))
