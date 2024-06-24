// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.db

import fi.espoo.evaka.shared.Id
import fi.espoo.evaka.shared.config.defaultJsonMapperBuilder
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.TimeRange
import java.lang.reflect.Type
import java.time.LocalDate
import java.util.Optional
import java.util.UUID
import java.util.function.Function
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.array.SqlArrayType
import org.jdbi.v3.core.generic.GenericTypes
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.core.mapper.ColumnMapper
import org.jdbi.v3.core.mapper.ColumnMapperFactory
import org.jdbi.v3.core.mapper.ColumnMappers
import org.jdbi.v3.core.mapper.RowMapperFactory
import org.jdbi.v3.core.mapper.SingleColumnMapper
import org.jdbi.v3.jackson2.Jackson2Config
import org.jdbi.v3.jackson2.Jackson2Plugin
import org.jdbi.v3.postgres.PostgresPlugin
import org.postgresql.util.PGobject

/**
 * Registers the given JDBI column mapper, which will be used to map data from database to values of
 * type T.
 *
 * This works fine for simple types where there are no extra shenanigans (inheritance, type
 * parameters).
 */
private inline fun <reified T> Jdbi.register(columnMapper: ColumnMapper<T>) = register(columnMapper) { type -> type == T::class.java }

/**
 * Registers the given JDBI column mapper, using the given function to decide when the mapper will
 * be used.
 *
 * By accepting a custom `isSupported` function, we can support cases where the target type
 * (function parameter `type`) and T have a complex relationship.
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
    // We need an explicit row mapper for T, or JDBI KotlinMapper will try to map it field-by-field,
    // even in the single column case
    val rowMapper = SingleColumnMapper(columnMapper)
    registerRowMapper(
        RowMapperFactory { type, _ -> Optional.ofNullable(rowMapper.takeIf { isSupported(type) }) }
    )
}

fun configureJdbi(jdbi: Jdbi): Jdbi {
    val jsonMapper = defaultJsonMapperBuilder().build()
    jdbi
        .installPlugin(KotlinPlugin())
        .installPlugin(PostgresPlugin())
        .installPlugin(Jackson2Plugin())
    jdbi.getConfig(Jackson2Config::class.java).mapper = jsonMapper
    jdbi.getConfig(ColumnMappers::class.java).coalesceNullPrimitivesToDefaults = false
    jdbi.registerArgument(finiteDateRangeArgumentFactory)
    jdbi.registerArgument(dateRangeArgumentFactory)
    jdbi.registerArgument(timeRangeEndpointArgumentFactory)
    jdbi.registerArgument(timeRangeArgumentFactory)
    jdbi.registerArgument(coordinateArgumentFactory)
    jdbi.registerArgument(identityArgumentFactory)
    jdbi.registerArgument(externalIdArgumentFactory)
    jdbi.registerArgument(idArgumentFactory)
    jdbi.registerArgument(helsinkiDateTimeArgumentFactory)
    jdbi.registerArgument(helsinkiDateTimeRangeArgumentFactory)
    jdbi.registerArgument(productKeyArgumentFactory)
    jdbi.registerArgument(databaseEnumArgumentFactory)
    jdbi.registerArgument(dateSetArgumentFactory)
    jdbi.register(finiteDateRangeColumnMapper)
    jdbi.register(dateRangeColumnMapper)
    jdbi.register(timeRangeColumnMapper)
    jdbi.register(coordinateColumnMapper)
    jdbi.register(externalIdColumnMapper)
    jdbi.register(idColumnMapper) { type ->
        // Since Id<T: DatabaseTable> has a type parameter, we can't rely on simple equals check.
        // The parameter 'type' passed to this
        // function will not be a JVM Class but a JVM ParameterizedType. We erase the type
        // parameters and get the raw class
        // before doing an assignability check. This means that our "universal" mapper for Id<*>
        // types will be used for
        // all concrete types like Id<Something> or Id<SomethingElse>
        Id::class.java.isAssignableFrom(GenericTypes.getErasedType(type))
    }
    jdbi.register(helsinkiDateTimeColumnMapper)
    jdbi.register(productKeyColumnMapper)
    jdbi.register(dateSetColumnMapper)
    jdbi.registerArrayType(UUID::class.java, "uuid")
    jdbi.registerArrayType(FiniteDateRange::class.java, "daterange") {
        PGobject().apply {
            type = "daterange"
            value = "[${it.start},${it.end}]"
        }
    }
    jdbi.registerArrayType(TimeRange::class.java, "timerange") { tr -> tr?.toDbString() }
    jdbi.registerArrayType(LocalDate::class.java, "date")
    jdbi.registerArrayType { elementType, _ ->
        Optional.ofNullable(
            (elementType as? Class<*>)?.let { elementClass ->
                if (DatabaseEnum::class.java.isAssignableFrom(elementClass)) {
                    // The sql type information should really be tied to the *class*, not the
                    // individual enum constants.
                    // However, this is not really possible with interfaces, so fetch the
                    // information from the first constant
                    // as a workaround
                    (elementClass.enumConstants?.first() as? DatabaseEnum)?.let {
                        SqlArrayType.of<Any>(it.sqlType, Function.identity())
                    }
                } else {
                    null
                }
            }
        )
    }
    jdbi.registerArrayType { elementType, _ ->
        Optional.ofNullable(
            SqlArrayType
                .of<Id<*>>("uuid") { it.raw }
                .takeIf { Id::class.java.isAssignableFrom(GenericTypes.getErasedType(elementType)) }
        )
    }
    return jdbi
}
