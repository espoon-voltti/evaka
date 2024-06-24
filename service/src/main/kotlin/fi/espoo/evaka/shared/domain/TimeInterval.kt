// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.domain

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import java.time.LocalTime

@JsonSerialize(using = TimeIntervalJsonSerializer::class)
@JsonDeserialize(using = TimeIntervalJsonDeserializer::class)
data class TimeInterval(
    val start: TimeRangeEndpoint.Start,
    val end: TimeRangeEndpoint.End?
) {
    constructor(
        start: TimeRangeEndpoint,
        end: TimeRangeEndpoint?
    ) : this(start.asStart(), end?.asEnd())

    constructor(
        start: LocalTime,
        end: LocalTime?
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

    fun includes(point: TimeRangeEndpoint) = this.start <= point && (this.end == null || point < this.end)

    fun includes(point: LocalTime) = this.includes(TimeRangeEndpoint.Start(point))

    fun asTimeRange(): TimeRange? = end?.let { TimeRange(start, it) }
}

private data class SerializableTimeInterval(
    val start: LocalTime,
    val end: LocalTime?
)

class TimeIntervalJsonSerializer : JsonSerializer<TimeInterval>() {
    override fun serialize(
        value: TimeInterval,
        gen: JsonGenerator,
        serializers: SerializerProvider
    ) = serializers.defaultSerializeValue(
        SerializableTimeInterval(value.start.inner, value.end?.inner),
        gen
    )
}

class TimeIntervalJsonDeserializer : JsonDeserializer<TimeInterval>() {
    override fun deserialize(
        parser: JsonParser,
        ctx: DeserializationContext
    ): TimeInterval {
        val value = parser.readValueAs(SerializableTimeInterval::class.java)
        return TimeInterval(value.start, value.end)
    }
}
