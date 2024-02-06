// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.domain

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.util.StdConverter
import fi.espoo.evaka.reservations.OpenTimeRange
import fi.espoo.evaka.shared.data.BoundedRange
import java.time.LocalTime

fun LocalTime.asLocalHm(): LocalHm = LocalHm(hour, minute)

@JsonSerialize(converter = LocalHm.ToJson::class)
@JsonDeserialize(converter = LocalHm.FromJson::class)
data class LocalHm(val hour: Int, val minute: Int) : Comparable<LocalHm> {
    init {
        require(hour in 0..23) { "hour must be in range 0..23" }
        require(minute in 0..59) { "minute must be in range 0..59" }
    }

    override fun compareTo(other: LocalHm): Int =
        when {
            hour != other.hour -> hour.compareTo(other.hour)
            else -> minute.compareTo(other.minute)
        }

    fun plusMinutes(minutesToAdd: Int): LocalHm? {
        var newMinute = minute + minutesToAdd
        var newHour = hour
        while (newMinute >= 60) {
            newMinute -= 60
            newHour++
        }
        return try {
            LocalHm(newHour, newMinute)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    fun minusMinutes(minutesToSubtract: Int): LocalHm? {
        var newMinute = minute - minutesToSubtract
        var newHour = hour
        while (newMinute < 0) {
            newMinute += 60
            newHour--
        }
        return try {
            LocalHm(newHour, newMinute)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    companion object {
        val MIN = LocalHm(0, 0)
        val MAX = LocalHm(23, 59)
    }

    class FromJson : StdConverter<String, LocalHm>() {
        override fun convert(value: String): LocalHm {
            val parts = value.split(":").map { it.toInt() }
            if (parts.size == 2) {
                return LocalHm(parts[0], parts[1])
            } else {
                throw IllegalArgumentException("Invalid time format: $value")
            }
        }
    }

    class ToJson : StdConverter<LocalHm, String>() {
        override fun convert(value: LocalHm): String =
            "${String.format("%02d", value.hour)}:${String.format("%02d", value.minute)}"
    }
}

fun TimeRange.asLocalHmRange(): LocalHmRange =
    LocalHmRange(this.start.asLocalHm(), this.end.asLocalHm())

fun OpenTimeRange.asLocalHmRange(): LocalHmRange? =
    endTime?.let { end -> LocalHmRange(startTime.asLocalHm(), end.asLocalHm()) }

/** `end` is inclusive */
data class LocalHmRange(override val start: LocalHm, override val end: LocalHm) :
    BoundedRange<LocalHm, LocalHmRange> {
    init {
        require(start <= end) {
            "Attempting to initialize invalid minute resolution time range with start: $start, end: $end"
        }
    }

    override fun includes(point: LocalHm) = this.start <= point && point <= this.end

    override fun overlaps(other: LocalHmRange): Boolean =
        this.start <= other.end && other.start <= this.end

    override fun leftAdjacentTo(other: LocalHmRange): Boolean =
        this.end.plusMinutes(1)?.let { it == other.start } ?: false

    override fun rightAdjacentTo(other: LocalHmRange): Boolean =
        other.end.plusMinutes(1)?.let { it == this.start } ?: false

    override fun strictlyLeftTo(other: LocalHmRange): Boolean = this.end < other.start

    override fun strictlyRightTo(other: LocalHmRange): Boolean = other.end < this.start

    override fun intersection(other: LocalHmRange): LocalHmRange? =
        tryCreate(maxOf(this.start, other.start), minOf(this.end, other.end))

    override fun gap(other: LocalHmRange): LocalHmRange? =
        tryCreate(
            minOf(this.end, other.end).plusMinutes(1),
            maxOf(this.start, other.start).minusMinutes(1)
        )

    override fun subtract(other: LocalHmRange): BoundedRange.SubtractResult<LocalHmRange> =
        if (this.overlaps(other)) {
            val left = tryCreate(this.start, other.start.minusMinutes(1))
            val right = tryCreate(other.end.plusMinutes(1), this.end)
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

    fun complement(other: LocalHmRange): List<LocalHmRange> = this.subtract(other).toList()

    fun complement(others: Collection<LocalHmRange>): List<LocalHmRange> {
        return others.fold(
            initial = listOf(this),
            operation = { acc, other -> acc.flatMap { it.complement(other) } }
        )
    }

    override fun merge(other: LocalHmRange): LocalHmRange =
        LocalHmRange(minOf(this.start, other.start), maxOf(this.end, other.end))

    override fun relationTo(other: LocalHmRange): BoundedRange.Relation<LocalHmRange> =
        when {
            this.end < other.start ->
                BoundedRange.Relation.LeftTo(
                    gap = tryCreate(this.end.plusMinutes(1), other.start.minusMinutes(1))
                )
            other.end < this.start ->
                BoundedRange.Relation.RightTo(
                    gap = tryCreate(other.end.plusMinutes(1), this.start.minusMinutes(1))
                )
            else ->
                BoundedRange.Relation.Overlap(
                    left =
                        when {
                            this.start < other.start ->
                                BoundedRange.Relation.Remainder(
                                    range = LocalHmRange(this.start, other.start.minusMinutes(1)!!),
                                    isFirst = true
                                )
                            other.start < this.start ->
                                BoundedRange.Relation.Remainder(
                                    range = LocalHmRange(other.start, this.start.minusMinutes(1)!!),
                                    isFirst = false
                                )
                            else -> null
                        },
                    overlap =
                        LocalHmRange(maxOf(this.start, other.start), minOf(this.end, other.end)),
                    right =
                        when {
                            other.end < this.end ->
                                BoundedRange.Relation.Remainder(
                                    range = LocalHmRange(other.end.plusMinutes(1)!!, this.end),
                                    isFirst = true
                                )
                            this.end < other.end ->
                                BoundedRange.Relation.Remainder(
                                    range = LocalHmRange(this.end.plusMinutes(1)!!, other.end),
                                    isFirst = false
                                )
                            else -> null
                        }
                )
        }

    fun timestamps(): Sequence<LocalHm> =
        generateSequence(start) { if (it < end) it.plusMinutes(1) else null }

    /** Works as if the end time was exclusive, i.e. 8:00-9:00 has a duration of 60 minutes */
    fun durationInMinutes(): Int = (end.hour - start.hour) * 60 + (end.minute - start.minute)

    companion object {
        private fun tryCreate(start: LocalHm?, end: LocalHm?): LocalHmRange? =
            if (start == null || end == null) null
            else {
                try {
                    LocalHmRange(start, end)
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
    }
}
