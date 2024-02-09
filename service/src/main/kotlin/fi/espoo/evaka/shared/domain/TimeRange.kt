// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.domain

import fi.espoo.evaka.shared.data.BoundedRange
import java.time.LocalTime

private sealed interface MidnightAwareTime : Comparable<MidnightAwareTime> {
    val inner: LocalTime

    /** 00:00:00 means midnight in the same day */
    data class Start(override val inner: LocalTime) : MidnightAwareTime {
        override fun compareTo(other: MidnightAwareTime): Int =
            when (other) {
                is Start -> this.inner.compareTo(other.inner)
                is End ->
                    if (this.inner == LocalTime.MIDNIGHT || other.inner == LocalTime.MIDNIGHT) -1
                    else this.inner.compareTo(other.inner)
            }
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
    }
}

/** `end` is exclusive */
data class TimeRange(override val start: LocalTime, override val end: LocalTime) :
    BoundedRange<LocalTime, TimeRange> {
    private constructor(
        start: MidnightAwareTime,
        end: MidnightAwareTime
    ) : this(start.inner, end.inner) {
        require(start < end) {
            "Attempting to initialize invalid TimeRange with start: $start, end: $end"
        }
    }

    init {
        require(MidnightAwareTime.Start(start) < MidnightAwareTime.End(end)) {
            "Attempting to initialize invalid TimeRange with start: $start, end: $end"
        }
    }

    private fun startT() = MidnightAwareTime.Start(start)

    private fun endT() = MidnightAwareTime.End(end)

    override fun overlaps(other: TimeRange): Boolean =
        this.startT() < other.endT() && other.startT() < this.endT()

    override fun leftAdjacentTo(other: TimeRange): Boolean =
        this.endT().compareTo(other.startT()) == 0

    override fun rightAdjacentTo(other: TimeRange): Boolean =
        other.endT().compareTo(this.startT()) == 0

    override fun strictlyLeftTo(other: TimeRange): Boolean = this.endT() <= other.startT()

    override fun strictlyRightTo(other: TimeRange): Boolean = other.endT() <= this.startT()

    override fun intersection(other: TimeRange): TimeRange? =
        tryCreate(maxOf(this.startT(), other.startT()), minOf(this.endT(), other.endT()))

    override fun gap(other: TimeRange): TimeRange? =
        tryCreate(minOf(this.endT(), other.endT()), maxOf(this.startT(), other.startT()))

    override fun subtract(other: TimeRange): BoundedRange.SubtractResult<TimeRange> =
        if (this.overlaps(other)) {
            val left = tryCreate(this.startT(), other.startT())
            val right = tryCreate(other.endT(), this.endT())
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
        TimeRange(minOf(this.startT(), other.startT()), maxOf(this.endT(), other.endT()))

    override fun relationTo(other: TimeRange): BoundedRange.Relation<TimeRange> =
        when {
            this.endT() <= other.startT() ->
                BoundedRange.Relation.LeftTo(gap = tryCreate(this.endT(), other.startT()))
            other.endT() <= this.startT() ->
                BoundedRange.Relation.RightTo(gap = tryCreate(other.endT(), this.startT()))
            else ->
                BoundedRange.Relation.Overlap(
                    left =
                        when {
                            this.startT() < other.startT() ->
                                BoundedRange.Relation.Remainder(
                                    range = TimeRange(this.startT(), other.startT()),
                                    isFirst = true
                                )
                            other.startT() < this.startT() ->
                                BoundedRange.Relation.Remainder(
                                    range = TimeRange(other.startT(), this.startT()),
                                    isFirst = false
                                )
                            else -> null
                        },
                    overlap =
                        TimeRange(
                            maxOf(this.startT(), other.startT()),
                            minOf(this.endT(), other.endT())
                        ),
                    right =
                        when {
                            other.endT() < this.endT() ->
                                BoundedRange.Relation.Remainder(
                                    range = TimeRange(other.endT(), this.endT()),
                                    isFirst = true
                                )
                            this.endT() < other.endT() ->
                                BoundedRange.Relation.Remainder(
                                    range = TimeRange(this.endT(), other.endT()),
                                    isFirst = false
                                )
                            else -> null
                        }
                )
        }

    override fun includes(point: LocalTime) =
        this.startT() <= MidnightAwareTime.Start(point) &&
            MidnightAwareTime.Start(point) < this.endT()

    override fun contains(other: TimeRange): Boolean =
        this.startT() <= other.startT() && other.endT() <= this.endT()

    fun toDbString(): String {
        return "(${this.start},${this.end})"
    }

    fun durationInMinutes(): Int {
        val endHour = if (this.end == LocalTime.MIDNIGHT) 24 else this.end.hour
        return endHour * 60 + this.end.minute - this.start.hour * 60 - this.start.minute
    }

    companion object {
        private fun tryCreate(start: MidnightAwareTime, end: MidnightAwareTime): TimeRange? =
            try {
                TimeRange(start, end)
            } catch (e: IllegalArgumentException) {
                null
            }
    }
}
