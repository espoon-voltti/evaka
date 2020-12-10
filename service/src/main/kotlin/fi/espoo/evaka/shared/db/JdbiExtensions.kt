// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.db

import fi.espoo.evaka.identity.ExternalId
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.shared.domain.ClosedPeriod
import fi.espoo.evaka.shared.domain.Coordinate
import fi.espoo.evaka.shared.domain.Period
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
import java.util.Optional
import java.util.function.Function

val closedPeriodArgumentFactory = PgObjectArgumentFactory.of<ClosedPeriod> {
    PGobject().apply {
        type = "daterange"
        value = "[${it.start},${it.end}]"
    }
}
val periodArgumentFactory = PgObjectArgumentFactory.of<Period> {
    PGobject().apply {
        type = "daterange"
        value = "[${it.start},${it.end ?: ""}]"
    }
}

val coordinateArgumentFactory = PgObjectArgumentFactory.of<Coordinate> {
    PGpoint().apply {
        y = it.lat
        x = it.lon
    }
}

val identityArgumentFactory = PgObjectArgumentFactory.of<ExternalIdentifier> {
    PGobject().apply {
        type = "text"
        value = when (it) {
            is ExternalIdentifier.SSN -> it.ssn
            is ExternalIdentifier.NoID -> null
        }
    }
}

val externalIdArgumentFactory = ToStringArgumentFactory.of<ExternalId>()

val closedPeriodColumnMapper = PgObjectColumnMapper {
    assert(it.type == "daterange")
    it.value?.let { value ->
        val parts = value.trim('[', ')').split(',')
        val start = LocalDate.parse(parts[0])
        val end = LocalDate.parse(parts[1]).minusDays(1)
        ClosedPeriod(start, end)
    }
}

val periodColumnMapper = PgObjectColumnMapper {
    assert(it.type == "daterange")
    it.value?.let { value ->
        val parts = value.trim('[', ')').split(',')
        val start = LocalDate.parse(parts[0])
        val end = if (parts[1].isNotEmpty()) {
            LocalDate.parse(parts[1]).minusDays(1)
        } else {
            null
        }
        Period(start, end)
    }
}

val coordinateColumnMapper = PgObjectColumnMapper {
    (it as PGpoint)
    Coordinate(it.y, it.x)
}

val externalIdColumnMapper =
    ColumnMapper<ExternalId> { r, columnNumber, _ -> ExternalId.parse(r.getString(columnNumber)) }

class PgObjectArgumentFactory<T>(
    private val clazz: Class<T>,
    private val serializer: (T) -> PGobject
) : ArgumentFactory.Preparable {
    override fun prepare(type: Type, config: ConfigRegistry): Optional<Function<Any?, Argument>> = if (type == clazz) {
        Optional.of(
            Function { nullableValue ->
                if (nullableValue == null) NullArgument(Types.OTHER)
                else (clazz.cast(nullableValue))?.let { value -> PgObjectArgument(serializer(value)) }!!
            }
        )
    } else {
        Optional.empty()
    }

    override fun prePreparedTypes(): MutableCollection<out Type> = mutableListOf(clazz)

    companion object {
        inline fun <reified T> of(noinline serializer: (T) -> PGobject) =
            PgObjectArgumentFactory(T::class.java, serializer)
    }
}

class PgObjectArgument(val value: PGobject) : Argument {
    override fun apply(position: Int, statement: PreparedStatement, ctx: StatementContext) =
        statement.setObject(position, value)

    override fun toString(): String = value.toString()
}

class PgObjectColumnMapper<T>(private val deserializer: (PGobject) -> T?) : ColumnMapper<T> {
    override fun map(r: ResultSet, columnNumber: Int, ctx: StatementContext): T? = r.getObject(columnNumber)?.let {
        deserializer(it as PGobject)
    }
}

class ToStringArgumentFactory<T>(private val clazz: Class<T>) : ArgumentFactory.Preparable {
    override fun prepare(type: Type, config: ConfigRegistry): Optional<Function<Any?, Argument>> = if (type == clazz) {
        Optional.of(
            Function { nullableValue ->
                if (nullableValue == null) NullArgument(Types.VARCHAR)
                else (clazz.cast(nullableValue))?.let { value -> ToStringArgument(value) }!!
            }
        )
    } else {
        Optional.empty()
    }

    override fun prePreparedTypes(): MutableCollection<out Type> = mutableListOf(clazz)

    companion object {
        inline fun <reified T> of() = ToStringArgumentFactory(T::class.java)
    }
}

class ToStringArgument<T>(val value: T) : Argument {
    override fun apply(position: Int, statement: PreparedStatement, ctx: StatementContext) =
        statement.setString(position, value.toString())

    override fun toString(): String = value.toString()
}
