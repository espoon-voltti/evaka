// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.db

import fi.espoo.evaka.identity.ExternalId
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.invoicing.service.ProductKey
import fi.espoo.evaka.shared.DatabaseTable
import fi.espoo.evaka.shared.Id
import fi.espoo.evaka.shared.domain.Coordinate
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.HelsinkiDateTimeRange
import fi.espoo.evaka.shared.domain.TimeRange
import java.lang.reflect.Type
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID
import java.util.function.Function
import org.jdbi.v3.core.argument.Argument
import org.jdbi.v3.core.argument.ArgumentFactory
import org.jdbi.v3.core.argument.NullArgument
import org.jdbi.v3.core.config.ConfigRegistry
import org.jdbi.v3.core.generic.GenericTypes
import org.jdbi.v3.core.mapper.ColumnMapper
import org.jdbi.v3.core.statement.StatementContext
import org.postgresql.geometric.PGpoint
import org.postgresql.util.PGobject

val finiteDateRangeArgumentFactory =
    pgObjectArgumentFactory<FiniteDateRange> {
        PGobject().apply {
            type = "daterange"
            if (it != null) {
                value = "[${it.start},${it.end}]"
            }
        }
    }

val dateRangeArgumentFactory =
    pgObjectArgumentFactory<DateRange> {
        PGobject().apply {
            type = "daterange"
            if (it != null) {
                value = "[${it.start},${it.end ?: ""}]"
            }
        }
    }

val timeRangeArgumentFactory =
    pgObjectArgumentFactory<TimeRange> {
        PGobject().apply {
            type = "timerange"
            if (it != null) {
                value = "(${it.start},${it.end})"
            }
        }
    }

val coordinateArgumentFactory =
    pgObjectArgumentFactory<Coordinate> {
        PGpoint().apply {
            if (it != null) {
                y = it.lat
                x = it.lon
            }
        }
    }

val identityArgumentFactory =
    customArgumentFactory<ExternalIdentifier>(Types.VARCHAR) {
        when (it) {
            is ExternalIdentifier.SSN -> CustomStringArgument(it.ssn)
            is ExternalIdentifier.NoID -> null
        }
    }

val externalIdArgumentFactory = toStringArgumentFactory<ExternalId>()

val idArgumentFactory =
    pgObjectArgumentFactory<Id<*>> {
        PGobject().apply {
            type = "uuid"
            if (it != null) {
                value = it.raw.toString()
            }
        }
    }

val helsinkiDateTimeArgumentFactory =
    customArgumentFactory<HelsinkiDateTime>(Types.TIMESTAMP_WITH_TIMEZONE) {
        CustomObjectArgument(it.toZonedDateTime().toOffsetDateTime())
    }

val helsinkiDateTimeRangeArgumentFactory =
    pgObjectArgumentFactory<HelsinkiDateTimeRange> {
        PGobject().apply {
            type = "tstzrange"
            if (it != null) {
                value = "[${it.start.toInstant()},${it.end.toInstant()})"
            }
        }
    }

val productKeyArgumentFactory =
    customArgumentFactory<ProductKey>(Types.VARCHAR) { CustomStringArgument(it.value) }

val databaseEnumArgumentFactory =
    ArgumentFactory.Preparable { type, _ ->
        val erasedType = GenericTypes.getErasedType(type)
        if (DatabaseEnum::class.java.isAssignableFrom(erasedType) && erasedType.isEnum) {
            val sqlType = (erasedType.enumConstants[0] as DatabaseEnum).sqlType
            Optional.of(
                Function { nullableValue ->
                    CustomObjectArgument(
                        PGobject().apply {
                            this.type = sqlType
                            if (nullableValue != null) {
                                this.value = nullableValue.toString()
                            }
                        }
                    )
                }
            )
        } else {
            Optional.empty()
        }
    }

val finiteDateRangeColumnMapper = PgObjectColumnMapper {
    assert(it.type == "daterange")
    it.value?.let { value ->
        val parts = value.trim('[', ')').split(',')
        val start = LocalDate.parse(parts[0])
        val end = LocalDate.parse(parts[1]).minusDays(1)
        FiniteDateRange(start, end)
    }
}

val dateRangeColumnMapper = PgObjectColumnMapper {
    assert(it.type == "daterange")
    it.value?.let { value ->
        val parts = value.trim('[', ')').split(',')
        val start = LocalDate.parse(parts[0])
        val end =
            if (parts[1].isNotEmpty()) {
                LocalDate.parse(parts[1]).minusDays(1)
            } else {
                null
            }
        DateRange(start, end)
    }
}

val timeRangeColumnMapper = PgObjectColumnMapper {
    assert(it.type == "timerange")
    it.value?.let { value ->
        val parts = value.trim('(', ')').split(',')
        TimeRange(LocalTime.parse(parts[0]), LocalTime.parse(parts[1]))
    }
}

val coordinateColumnMapper = PgObjectColumnMapper {
    (it as PGpoint)
    Coordinate(it.y, it.x)
}

val externalIdColumnMapper = ColumnMapper { r, columnNumber, _ ->
    r.getString(columnNumber)?.let { ExternalId.parse(it) }
}

val idColumnMapper =
    ColumnMapper<Id<*>> { r, columnNumber, _ ->
        r.getObject(columnNumber, UUID::class.java)?.let { Id<DatabaseTable>(it) }
    }

val helsinkiDateTimeColumnMapper = ColumnMapper { r, columnNumber, _ ->
    r.getObject(columnNumber, OffsetDateTime::class.java)?.let {
        HelsinkiDateTime.from(it.toInstant())
    }
}

val productKeyColumnMapper = ColumnMapper { rs, columnNumber, _ ->
    ProductKey(rs.getString(columnNumber))
}

class CustomArgumentFactory<T>(
    private val clazz: Class<T>,
    private val sqlType: Int,
    private inline val f: (T) -> Argument?
) : ArgumentFactory.Preparable {
    init {
        check(sqlType != Types.OTHER) {
            "OTHER cannot be used because the type of NULL values is lost"
        }
    }

    override fun prepare(type: Type, config: ConfigRegistry): Optional<Function<Any?, Argument>> =
        Optional.ofNullable(
            if (clazz.isAssignableFrom(GenericTypes.getErasedType(type))) {
                Function { nullableValue ->
                    clazz.cast(nullableValue)?.let(f) ?: NullArgument(sqlType)
                }
            } else {
                null
            }
        )
}

class PgObjectArgumentFactory<T>(
    private val clazz: Class<T>,
    private inline val f: (T?) -> PGobject
) : ArgumentFactory.Preparable {
    override fun prepare(type: Type, config: ConfigRegistry): Optional<Function<Any?, Argument>> =
        Optional.ofNullable(
            if (clazz.isAssignableFrom(GenericTypes.getErasedType(type))) {
                Function { nullableValue -> CustomObjectArgument(f(clazz.cast(nullableValue))) }
            } else {
                null
            }
        )
}

class CustomObjectArgument(val value: Any) : Argument {
    override fun apply(position: Int, statement: PreparedStatement, ctx: StatementContext) =
        statement.setObject(position, value)
    override fun toString(): String = value.toString()
}

class CustomStringArgument(val value: String) : Argument {
    override fun apply(position: Int, statement: PreparedStatement, ctx: StatementContext) =
        statement.setString(position, value)

    override fun toString(): String = value
}

inline fun <reified T> customArgumentFactory(
    sqlType: Int,
    noinline f: (T) -> Argument?
): CustomArgumentFactory<T> = CustomArgumentFactory(T::class.java, sqlType, f)

inline fun <reified T> pgObjectArgumentFactory(noinline serializer: (T?) -> PGobject) =
    PgObjectArgumentFactory(T::class.java, serializer)

inline fun <reified T> toStringArgumentFactory() =
    customArgumentFactory<T>(Types.VARCHAR) { CustomStringArgument(it.toString()) }

class PgObjectColumnMapper<T>(private inline val deserializer: (PGobject) -> T?) : ColumnMapper<T> {
    override fun map(r: ResultSet, columnNumber: Int, ctx: StatementContext): T? =
        r.getObject(columnNumber)?.let { deserializer(it as PGobject) }
}
