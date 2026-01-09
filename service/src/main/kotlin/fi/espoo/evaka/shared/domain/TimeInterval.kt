// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.domain

import java.time.LocalTime
import tools.jackson.core.JsonGenerator
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.ValueSerializer
import tools.jackson.databind.annotation.JsonDeserialize
import tools.jackson.databind.annotation.JsonSerialize

@JsonSerialize(using = TimeIntervalJsonSerializer::class)
@JsonDeserialize(using = TimeIntervalJsonDeserializer::class)
@com.fasterxml.jackson.databind.annotation.JsonSerialize(
    using = TimeIntervalJsonSerializerJackson2::class
)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(
    using = TimeIntervalJsonDeserializerJackson2::class
)
data class TimeInterval(val start: TimeRangeEndpoint.Start, val end: TimeRangeEndpoint.End?) {
    constructor(
        start: TimeRangeEndpoint,
        end: TimeRangeEndpoint?,
    ) : this(start.asStart(), end?.asEnd())

    constructor(
        start: LocalTime,
        end: LocalTime?,
    ) : this(TimeRangeEndpoint.Start(start), end?.let { TimeRangeEndpoint.End(it) })

    init {
        require(end == null || start < end) {
            "Attempting to initialize invalid TimeInterval with start: $start, end: $end"
        }
    }

    fun overlaps(other: TimeInterval): Boolean =
        this.start < (other.end ?: TimeRangeEndpoint.End.MAX) &&
            other.start < (this.end ?: TimeRangeEndpoint.End.MAX)

    fun startsAfter(point: TimeRangeEndpoint) = this.start > point

    fun startsAfter(point: LocalTime) = this.startsAfter(TimeRangeEndpoint.Start(point))

    fun includes(point: TimeRangeEndpoint) =
        this.start <= point && (this.end == null || point < this.end)

    fun includes(point: LocalTime) = this.includes(TimeRangeEndpoint.Start(point))

    fun asTimeRange(): TimeRange? = end?.let { TimeRange(start, it) }
}

private data class SerializableTimeInterval(val start: LocalTime, val end: LocalTime?)

class TimeIntervalJsonSerializer : ValueSerializer<TimeInterval>() {
    override fun serialize(value: TimeInterval?, gen: JsonGenerator?, ctxt: SerializationContext?) {
        if (value == null || gen == null || ctxt == null) return
        val serializer = ctxt.findValueSerializer(SerializableTimeInterval::class.java)
        serializer.serialize(
            SerializableTimeInterval(value.start.inner, value.end?.inner),
            gen,
            ctxt,
        )
    }
}

class TimeIntervalJsonDeserializer : ValueDeserializer<TimeInterval>() {
    override fun deserialize(parser: JsonParser, ctx: DeserializationContext): TimeInterval {
        val value = parser.readValueAs(SerializableTimeInterval::class.java)
        return TimeInterval(value.start, value.end)
    }
}

class TimeIntervalJsonSerializerJackson2 :
    com.fasterxml.jackson.databind.JsonSerializer<TimeInterval>() {
    override fun serialize(
        value: TimeInterval,
        gen: com.fasterxml.jackson.core.JsonGenerator,
        serializers: com.fasterxml.jackson.databind.SerializerProvider,
    ) {
        return serializers.defaultSerializeValue(
            SerializableTimeInterval(value.start.inner, value.end?.inner),
            gen,
        )
    }
}

class TimeIntervalJsonDeserializerJackson2 :
    com.fasterxml.jackson.databind.JsonDeserializer<TimeInterval>() {
    override fun deserialize(
        parser: com.fasterxml.jackson.core.JsonParser,
        ctx: com.fasterxml.jackson.databind.DeserializationContext,
    ): TimeInterval {
        val value = parser.readValueAs(SerializableTimeInterval::class.java)
        return TimeInterval(value.start, value.end)
    }
}
