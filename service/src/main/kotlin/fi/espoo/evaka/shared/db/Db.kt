// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.db

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionDetailed
import fi.espoo.evaka.invoicing.domain.FeeDecisionSummary
import fi.espoo.evaka.shared.Id
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.array.SqlArrayType
import org.jdbi.v3.core.array.SqlArrayTypeFactory
import org.jdbi.v3.core.config.ConfigRegistry
import org.jdbi.v3.core.generic.GenericType
import org.jdbi.v3.core.generic.GenericTypes
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.core.mapper.ColumnMapper
import org.jdbi.v3.core.mapper.ColumnMapperFactory
import org.jdbi.v3.core.mapper.RowMapperFactory
import org.jdbi.v3.core.mapper.SingleColumnMapper
import org.jdbi.v3.core.qualifier.QualifiedType
import org.jdbi.v3.core.result.RowView
import org.jdbi.v3.core.statement.SqlStatement
import org.jdbi.v3.core.statement.StatementContext
import org.jdbi.v3.jackson2.Jackson2Config
import org.jdbi.v3.jackson2.Jackson2Plugin
import org.jdbi.v3.json.Json
import org.jdbi.v3.postgres.PostgresPlugin
import java.lang.reflect.Type
import java.sql.ResultSet
import java.util.Optional
import java.util.UUID

/**
 * Registers the given JDBI column mapper, which will be used to map data from database to values of type T.
 *
 * This works fine for simple types where there are no extra shenanigans (inheritance, type parameters).
 */
private inline fun <reified T> Jdbi.register(columnMapper: ColumnMapper<T>) =
    register(columnMapper) { type -> type == T::class.java }

/**
 * Registers the given JDBI column mapper, using the given function to decide when the mapper will be used.
 *
 * By accepting a custom `isSupported` function, we can support cases where the target type (function parameter `type`)
 * and T have a complex relationship.
 */
private inline fun <reified T> Jdbi.register(
    columnMapper: ColumnMapper<T>,
    crossinline isSupported: (Type) -> Boolean
) {
    registerColumnMapper(
        ColumnMapperFactory { type, _ ->
            Optional.ofNullable(columnMapper.takeIf { isSupported(type) })
        }
    )
    // Support mapping a single column result.
    // We need an explicit row mapper for T, or JDBI KotlinMapper will try to map it field-by-field, even in the single column case
    val rowMapper = SingleColumnMapper(columnMapper)
    registerRowMapper(
        RowMapperFactory { type, _ ->
            Optional.ofNullable(rowMapper.takeIf { isSupported(type) })
        }
    )
}

fun configureJdbi(jdbi: Jdbi): Jdbi {
    val objectMapper = ObjectMapper()
        .registerModule(JavaTimeModule())
        .registerModule(Jdk8Module())
        .registerModule(ParameterNamesModule())
        .registerModule(KotlinModule())
    jdbi.installPlugin(KotlinPlugin())
        .installPlugin(PostgresPlugin())
        .installPlugin(Jackson2Plugin())
    jdbi.getConfig(Jackson2Config::class.java).mapper = objectMapper
    jdbi.registerArgument(finiteDateRangeArgumentFactory)
    jdbi.registerArgument(dateRangeArgumentFactory)
    jdbi.registerArgument(coordinateArgumentFactory)
    jdbi.registerArgument(identityArgumentFactory)
    jdbi.registerArgument(externalIdArgumentFactory)
    jdbi.registerArgument(idArgumentFactory)
    jdbi.registerArgument(helsinkiDateTimeArgumentFactory)
    jdbi.register(finiteDateRangeColumnMapper)
    jdbi.register(dateRangeColumnMapper)
    jdbi.register(coordinateColumnMapper)
    jdbi.register(externalIdColumnMapper)
    jdbi.register(idColumnMapper) { type ->
        // Since Id<T: DatabaseTable> has a type parameter, we can't rely on simple equals check. The parameter 'type' passed to this
        // function will not be a JVM Class but a JVM ParameterizedType. We erase the type parameters and get the raw class
        // before doing an assignability check. This means that our "universal" mapper for Id<*> types will be used for
        // all concrete types like Id<Something> or Id<SomethingElse>
        Id::class.java.isAssignableFrom(GenericTypes.getErasedType(type))
    }
    jdbi.register(helsinkiDateTimeColumnMapper)
    jdbi.registerArrayType(UUID::class.java, "uuid")
    jdbi.registerArrayType { elementType, _ ->
        Optional.ofNullable(SqlArrayType.of<Id<*>>("uuid") { it.raw }
            .takeIf { Id::class.java.isAssignableFrom(GenericTypes.getErasedType(elementType)) })
    }
    jdbi.registerRowMapper(FeeDecision::class.java, feeDecisionRowMapper(objectMapper))
    jdbi.registerRowMapper(FeeDecisionDetailed::class.java, feeDecisionDetailedRowMapper(objectMapper))
    jdbi.registerRowMapper(FeeDecisionSummary::class.java, feeDecisionSummaryRowMapper)
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
inline fun <reified T : Any?> RowView.mapJsonColumn(name: String): T =
    mapColumn(name, QualifiedType.of(object : GenericType<T>() {}).with(Json::class.java))

/**
 * Maps a row to a value.
 *
 * This function works with Kotlin better than row.getRow().
 */
inline fun <reified T> RowView.mapRow(type: Class<T> = T::class.java): T = getRow(type)
