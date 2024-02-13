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
import fi.espoo.evaka.shared.data.BoundedRange
import java.time.LocalDate
import java.time.LocalTime

sealed interface MidnightAwareTime : Comparable<MidnightAwareTime> {
    val inner: LocalTime

    fun asStart(): Start

    fun asEnd(): End

    fun isMidnight(): Boolean = this.inner == LocalTime.MIDNIGHT

    fun toDbString(): String

    /** 00:00:00 means midnight in the same day */
    data class Start(override val inner: LocalTime) : MidnightAwareTime {
        override fun compareTo(other: MidnightAwareTime): Int =
            when (other) {
                is Start -> this.inner.compareTo(other.inner)
                is End ->
                    if (this.inner == LocalTime.MIDNIGHT || other.inner == LocalTime.MIDNIGHT) -1
                    else this.inner.compareTo(other.inner)
            }

        override fun asStart() = this

        override fun asEnd(): End =
            if (this.inner == LocalTime.MIDNIGHT)
                throw IllegalStateException("Cannot convert midnight start to end")
            else End(inner)

        override fun toDbString(): String = inner.toString()
    }

    /** 00:00:00 means midnight in the next day */
    data class End(override val inner: LocalTime) : MidnightAwareTime {
        override fun compareTo(other: MidnightAwareTime): Int =
            when (other) {
                is Start ->
                    if (other.inner == LocalTime.MIDNIGHT || this.inner == LocalTime.MIDNIGHT) 1
                    else inner.compareTo(other.inner)
                is End ->
                    if (this.inner == LocalTime.MIDNIGHT && other.inner == LocalTime.MIDNIGHT) 0
                    else if (this.inner == LocalTime.MIDNIGHT) 1
                    else if (other.inner == LocalTime.MIDNIGHT) -1 else inner.compareTo(other.inner)
            }

        override fun asStart(): Start =
            if (this.inner == LocalTime.MIDNIGHT)
                throw IllegalStateException("Cannot convert midnight end to start")
            else Start(inner)

        override fun asEnd() = this

        override fun toDbString(): String =
            if (this.isMidnight())
                throw IllegalStateException("Cannot convert midnight end to db string")
            else inner.toString()
    }
}

/** `end` is exclusive */
@JsonSerialize(using = TimeRangeJsonSerializer::class)
@JsonDeserialize(using = TimeRangeJsonDeserializer::class)
data class TimeRange(
    override val start: MidnightAwareTime.Start,
    override val end: MidnightAwareTime.End
) : BoundedRange<MidnightAwareTime, TimeRange> {
    constructor(
        start: MidnightAwareTime,
        end: MidnightAwareTime
    ) : this(start.asStart(), end.asEnd())

    init {
        require(start < end) {
            "Attempting to initialize invalid TimeRange with start: $start, end: $end"
        }
    }

    override fun overlaps(other: TimeRange): Boolean =
        this.start < other.end && other.start < this.end

    override fun leftAdjacentTo(other: TimeRange): Boolean = this.end.compareTo(other.start) == 0

    override fun rightAdjacentTo(other: TimeRange): Boolean = other.end.compareTo(this.start) == 0

    override fun strictlyLeftTo(other: TimeRange): Boolean = this.end <= other.start

    override fun strictlyRightTo(other: TimeRange): Boolean = other.end <= this.start

    override fun intersection(other: TimeRange): TimeRange? =
        tryCreate(maxOf(this.start, other.start), minOf(this.end, other.end))

    override fun gap(other: TimeRange): TimeRange? =
        tryCreate(minOf(this.end, other.end), maxOf(this.start, other.start))

    override fun subtract(other: TimeRange): BoundedRange.SubtractResult<TimeRange> =
        if (this.overlaps(other)) {
            val left = tryCreate(this.start, other.start)
            val right = tryCreate(other.end, this.end)
            if (left != null) {
                if (right != null) {
                    BoundedRange.SubtractResult.Split(left, right)
                } else {
                    BoundedRange.SubtractResult.LeftRemainder(left)
                }
            } else {
                if (right != null) {
                    BoundedRange.SubtractResult.RightRemainder(right)
                } else {
                    BoundedRange.SubtractResult.None
                }
            }
        } else BoundedRange.SubtractResult.Original(this)

    override fun merge(other: TimeRange): TimeRange =
        TimeRange(minOf(this.start, other.start), maxOf(this.end, other.end))

    override fun relationTo(other: TimeRange): BoundedRange.Relation<TimeRange> =
        when {
            this.end <= other.start ->
                BoundedRange.Relation.LeftTo(gap = tryCreate(this.end, other.start))
            other.end <= this.start ->
                BoundedRange.Relation.RightTo(gap = tryCreate(other.end, this.start))
            else ->
                BoundedRange.Relation.Overlap(
                    left =
                        when {
                            this.start < other.start ->
                                BoundedRange.Relation.Remainder(
                                    range = TimeRange(this.start, other.start),
                                    isFirst = true
                                )
                            other.start < this.start ->
                                BoundedRange.Relation.Remainder(
                                    range = TimeRange(other.start, this.start),
                                    isFirst = false
                                )
                            else -> null
                        },
                    overlap = TimeRange(maxOf(this.start, other.start), minOf(this.end, other.end)),
                    right =
                        when {
                            other.end < this.end ->
                                BoundedRange.Relation.Remainder(
                                    range = TimeRange(other.end, this.end),
                                    isFirst = true
                                )
                            this.end < other.end ->
                                BoundedRange.Relation.Remainder(
                                    range = TimeRange(this.end, other.end),
                                    isFirst = false
                                )
                            else -> null
                        }
                )
        }

    override fun includes(point: MidnightAwareTime) = this.start <= point && point < this.end

    fun includes(point: LocalTime) = this.includes(MidnightAwareTime.Start(point))

    override fun contains(other: TimeRange): Boolean =
        this.start <= other.start && other.end <= this.end

    fun toDbString(): String {
        return "(${this.start.toDbString()},${this.end.toDbString()})"
    }

    fun durationInMinutes(): Int {
        val endHour = if (this.end.isMidnight()) 24 else this.end.inner.hour
        return endHour * 60 + this.end.inner.minute -
            this.start.inner.hour * 60 -
            this.start.inner.minute
    }

    fun asHelsinkiDateTimeRange(date: LocalDate): HelsinkiDateTimeRange {
        val endDate = if (this.end.isMidnight()) date.plusDays(1) else date
        return HelsinkiDateTimeRange(
            HelsinkiDateTime.of(date, this.start.inner),
            HelsinkiDateTime.of(endDate, this.end.inner)
        )
    }

    companion object {
        fun of(start: LocalTime, end: LocalTime): TimeRange =
            TimeRange(MidnightAwareTime.Start(start), MidnightAwareTime.End(end))

        private fun tryCreate(start: MidnightAwareTime, end: MidnightAwareTime): TimeRange? =
            try {
                TimeRange(start.asStart(), end.asEnd())
            } catch (e: IllegalArgumentException) {
                null
            }
    }
}

private data class SerializableTimeRange(val start: LocalTime, val end: LocalTime)

class TimeRangeJsonSerializer : JsonSerializer<TimeRange>() {
    override fun serialize(value: TimeRange, gen: JsonGenerator, serializers: SerializerProvider) {
        return serializers.defaultSerializeValue(
            SerializableTimeRange(value.start.inner, value.end.inner),
            gen
        )
    }
}

class TimeRangeJsonDeserializer : JsonDeserializer<TimeRange>() {
    override fun deserialize(parser: JsonParser, ctx: DeserializationContext): TimeRange {
        val value = parser.readValueAs(SerializableTimeRange::class.java)
        return TimeRange.of(value.start, value.end)
    }
}
