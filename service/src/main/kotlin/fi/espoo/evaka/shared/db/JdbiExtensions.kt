// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.db

import fi.espoo.evaka.identity.ExternalId
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.shared.domain.Coordinate
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.jdbi.v3.core.argument.Argument
import org.jdbi.v3.core.argument.ArgumentFactory
import org.jdbi.v3.core.argument.NullArgument
import org.jdbi.v3.core.config.ConfigRegistry
import org.jdbi.v3.core.mapper.ColumnMapper
import org.jdbi.v3.core.statement.StatementContext
import org.postgresql.geometric.PGpoint
import org.postgresql.util.PGobject
import java.lang.reflect.Type
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Optional
import java.util.function.Function

val finiteDateRangeArgumentFactory = pgObjectArgumentFactory<FiniteDateRange> {
    PGobject().apply {
        type = "daterange"
        value = "[${it.start},${it.end}]"
    }
}
val dateRangeArgumentFactory = pgObjectArgumentFactory<DateRange> {
    PGobject().apply {
        type = "daterange"
        value = "[${it.start},${it.end ?: ""}]"
    }
}

val coordinateArgumentFactory = pgObjectArgumentFactory<Coordinate> {
    PGpoint().apply {
        y = it.lat
        x = it.lon
    }
}

val identityArgumentFactory = customArgumentFactory<ExternalIdentifier>(Types.VARCHAR) {
    when (it) {
        is ExternalIdentifier.SSN -> CustomStringArgument(it.ssn)
        is ExternalIdentifier.NoID -> null
    }
}

val externalIdArgumentFactory = toStringArgumentFactory<ExternalId>()

val helsinkiDateTimeArgumentFactory = customArgumentFactory<HelsinkiDateTime>(Types.TIMESTAMP_WITH_TIMEZONE) {
    CustomObjectArgument(it.toZonedDateTime().toOffsetDateTime())
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
        val end = if (parts[1].isNotEmpty()) {
            LocalDate.parse(parts[1]).minusDays(1)
        } else {
            null
        }
        DateRange(start, end)
    }
}

val coordinateColumnMapper = PgObjectColumnMapper {
    (it as PGpoint)
    Coordinate(it.y, it.x)
}

val externalIdColumnMapper =
    ColumnMapper { r, columnNumber, _ -> r.getString(columnNumber)?.let { ExternalId.parse(it) } }

val helsinkiDateTimeColumnMapper =
    ColumnMapper { r, columnNumber, _ -> r.getObject(columnNumber, OffsetDateTime::class.java)?.let { HelsinkiDateTime.from(it.toInstant()) } }

class CustomArgumentFactory<T>(private val clazz: Class<T>, private val sqlType: Int, private inline val f: (T) -> Argument?) : ArgumentFactory.Preparable {
    override fun prepare(type: Type, config: ConfigRegistry): Optional<Function<Any?, Argument>> = Optional.ofNullable(
        if (type == clazz) {
            Function { nullableValue -> clazz.cast(nullableValue)?.let(f) ?: NullArgument(sqlType) }
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

inline fun <reified T> customArgumentFactory(sqlType: Int, noinline f: (T) -> Argument?): CustomArgumentFactory<T> = CustomArgumentFactory(T::class.java, sqlType, f)

inline fun <reified T> pgObjectArgumentFactory(noinline serializer: (T) -> PGobject) = customArgumentFactory<T>(Types.OTHER) {
    CustomObjectArgument(serializer(it))
}

inline fun <reified T> toStringArgumentFactory() = customArgumentFactory<T>(Types.VARCHAR) {
    CustomStringArgument(it.toString())
}

class PgObjectColumnMapper<T>(private inline val deserializer: (PGobject) -> T?) : ColumnMapper<T> {
    override fun map(r: ResultSet, columnNumber: Int, ctx: StatementContext): T? = r.getObject(columnNumber)?.let {
        deserializer(it as PGobject)
    }
}
