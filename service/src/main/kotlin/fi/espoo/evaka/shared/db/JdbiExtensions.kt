// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.db

import fi.espoo.evaka.identity.ExternalId
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.invoicing.service.ProductKey
import fi.espoo.evaka.shared.DatabaseTable
import fi.espoo.evaka.shared.Id
import fi.espoo.evaka.shared.data.DateSet
import fi.espoo.evaka.shared.domain.Coordinate
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.HelsinkiDateTimeRange
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.shared.domain.TimeRangeEndpoint
import java.lang.reflect.Type
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import java.text.ParsePosition
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
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

val dateSetArgumentFactory =
    pgObjectArgumentFactory<DateSet> {
        PGobject().apply {
            type = "datemultirange"
            if (it != null) {
                value =
                    it.ranges().joinToString(separator = ",", prefix = "{", postfix = "}") { range
                        ->
                        "[${range.start},${range.end}]"
                    }
            }
        }
    }

val timeRangeEndpointArgumentFactory =
    pgObjectArgumentFactory<TimeRangeEndpoint> {
        PGobject().apply {
            type = "time"
            value = it?.toDbString()
        }
    }

val timeRangeArgumentFactory =
    pgObjectArgumentFactory<TimeRange> {
        PGobject().apply {
            type = "timerange"
            value = it?.toDbString()
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

val yearMonthArgumentFactory =
    customArgumentFactory<YearMonth>(Types.DATE) { CustomObjectArgument(it.atDay(1)) }

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

private class Parser(private var text: CharSequence) {
    fun peekChar(): Char? = this.text.firstOrNull()

    fun parseChar(): Char =
        (this.text.firstOrNull() ?: error("Unexpected end of text")).also {
            this.text = this.text.drop(1)
        }

    fun parseLiteral(literal: String): Boolean =
        this.text.startsWith(literal).also { found ->
            if (found) {
                this.text = this.text.drop(literal.length)
            }
        }

    fun parseDate(): LocalDate {
        val position = ParsePosition(0)
        return DateTimeFormatter.ISO_DATE.parse(text, position).query(LocalDate::from).also {
            this.text = this.text.drop(position.index)
        }
    }

    fun parseDateRangeEnd(): LocalDate =
        // PostgreSQL uses exclusive end bound in date ranges, so a valid ISO "9999-12-31" end date
        // is presented as 10000-01-01, which can't be parsed with the standard LocalDate ISO parser
        if (this.parseLiteral("10000-01-01")) {
            LocalDate.of(10000, 1, 1)
        } else {
            this.parseDate()
        }

    private data class RawRange<Lower : Any, Upper : Any>(
        val lower: Lower?,
        val lowerInclusive: Boolean,
        val upper: Upper?,
        val upperInclusive: Boolean,
    )

    private fun <Lower : Any, Upper : Any> parseRange(
        parseLower: (parser: Parser) -> Lower,
        parseUpper: (parser: Parser) -> Upper,
    ): RawRange<Lower, Upper> {
        val lowerInclusive =
            when (val char = parseChar()) {
                '[' -> true
                '(' -> false
                else -> error("Expected '[' or '(', got $char")
            }
        val lower =
            when (peekChar()) {
                ',' -> null
                else -> parseLower(this)
            }
        when (val char = parseChar()) {
            ',' -> {}
            else -> error("Expected comma, got $char")
        }
        val upper =
            when (peekChar()) {
                ']',
                ')' -> null
                else -> parseUpper(this)
            }
        val upperInclusive =
            when (val char = parseChar()) {
                ']' -> true
                ')' -> false
                else -> error("Expected ']' or ')', got $char")
            }
        return RawRange(lower, lowerInclusive, upper, upperInclusive)
    }

    fun parseFiniteDateRange(): FiniteDateRange =
        parseRange({ it.parseDate() }, { it.parseDateRangeEnd() }).let { range ->
            checkNotNull(range.lower) { "FiniteDateRange lower must not be null" }
            checkNotNull(range.upper) { "FiniteDateRange upper must not be null" }
            FiniteDateRange(
                start = range.lower.let { if (range.lowerInclusive) it else it.plusDays(1) },
                end = range.upper.let { if (range.upperInclusive) it else it.minusDays(1) },
            )
        }

    fun parseDateRange(): DateRange =
        parseRange({ it.parseDate() }, { it.parseDateRangeEnd() }).let { range ->
            checkNotNull(range.lower) { "DateRange lower must not be null" }
            DateRange(
                start = range.lower.let { if (range.lowerInclusive) it else it.plusDays(1) },
                end = range.upper?.let { if (range.upperInclusive) it else it.minusDays(1) },
            )
        }

    fun <T : Any> parseMultiRange(parseRange: (parser: Parser) -> T): List<T> {
        when (val char = parseChar()) {
            '{' -> {}
            else -> error("Expected '{', got $char")
        }
        val result = mutableListOf<T>()
        while (true) {
            when (peekChar()) {
                ',' -> parseChar()
                '}' -> break
            }
            result.add(parseRange(this))
        }
        when (val char = parseChar()) {
            '}' -> {}
            else -> error("Expected '}', got $char")
        }
        return result
    }
}

val dateSetColumnMapper = PgObjectColumnMapper { obj ->
    assert(obj.type == "datemultirange")
    obj.value
        ?.let(::Parser)
        ?.parseMultiRange { it.parseFiniteDateRange() }
        ?.let { DateSet.unsafeRaw(it) }
}

val finiteDateRangeColumnMapper = PgObjectColumnMapper { obj ->
    assert(obj.type == "daterange")
    obj.value?.let(::Parser)?.parseFiniteDateRange()
}

val dateRangeColumnMapper = PgObjectColumnMapper { obj ->
    assert(obj.type == "daterange")
    obj.value?.let(::Parser)?.parseDateRange()
}

val timeRangeColumnMapper = PgObjectColumnMapper {
    assert(it.type == "timerange" || it.type == "timerange_non_nullable_range")
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

val yearMonthColumnMapper = ColumnMapper { r, columnNumber, _ ->
    r.getObject(columnNumber, LocalDate::class.java)?.let { YearMonth.from(it) }
}

val productKeyColumnMapper = ColumnMapper { rs, columnNumber, _ ->
    ProductKey(rs.getString(columnNumber))
}

class CustomArgumentFactory<T>(
    private val clazz: Class<T>,
    private val sqlType: Int,
    private val f: (T) -> Argument?,
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

class PgObjectArgumentFactory<T>(private val clazz: Class<T>, private val f: (T?) -> PGobject) :
    ArgumentFactory.Preparable {
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
    noinline f: (T) -> Argument?,
): CustomArgumentFactory<T> = CustomArgumentFactory(T::class.java, sqlType, f)

inline fun <reified T> pgObjectArgumentFactory(noinline serializer: (T?) -> PGobject) =
    PgObjectArgumentFactory(T::class.java, serializer)

inline fun <reified T> toStringArgumentFactory() =
    customArgumentFactory<T>(Types.VARCHAR) { CustomStringArgument(it.toString()) }

class PgObjectColumnMapper<T>(val deserializer: (PGobject) -> T?) : ColumnMapper<T> {
    override fun map(r: ResultSet, columnNumber: Int, ctx: StatementContext): T? =
        r.getObject(columnNumber)?.let { deserializer(it as PGobject) }
}
