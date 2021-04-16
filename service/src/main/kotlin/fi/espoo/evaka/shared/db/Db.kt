// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.db

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import fi.espoo.evaka.identity.ExternalId
import fi.espoo.evaka.shared.domain.Coordinate
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.generic.GenericType
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.core.qualifier.QualifiedType
import org.jdbi.v3.core.result.RowView
import org.jdbi.v3.core.statement.SqlStatement
import org.jdbi.v3.core.statement.StatementContext
import org.jdbi.v3.jackson2.Jackson2Config
import org.jdbi.v3.jackson2.Jackson2Plugin
import org.jdbi.v3.json.Json
import org.jdbi.v3.postgres.PostgresPlugin
import java.sql.ResultSet
import java.util.UUID

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
    jdbi.registerArgument(finiteDateRangeArgumentFactory)
    jdbi.registerArgument(dateRangeArgumentFactory)
    jdbi.registerArgument(coordinateArgumentFactory)
    jdbi.registerArgument(identityArgumentFactory)
    jdbi.registerArgument(externalIdArgumentFactory)
    jdbi.registerArgument(helsinkiDateTimeArgumentFactory)
    jdbi.registerColumnMapper(FiniteDateRange::class.java, finiteDateRangeColumnMapper)
    jdbi.registerColumnMapper(DateRange::class.java, dateRangeColumnMapper)
    jdbi.registerColumnMapper(Coordinate::class.java, coordinateColumnMapper)
    jdbi.registerColumnMapper(ExternalId::class.java, externalIdColumnMapper)
    jdbi.registerColumnMapper(HelsinkiDateTime::class.java, helsinkiDateTimeColumnMapper)
    jdbi.registerArrayType(UUID::class.java, "uuid")
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

inline fun <reified T : Any, This : SqlStatement<This>> SqlStatement<This>.bindNullable(
    name: String,
    value: Collection<T>?
): This =
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
    mapColumn(name, QualifiedType.of(object : GenericType<T>() {}).with(Json::class.java))
