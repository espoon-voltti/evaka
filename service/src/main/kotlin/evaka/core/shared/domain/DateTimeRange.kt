// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.shared.domain

import evaka.core.shared.data.BoundedRange
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

data class FiniteDateTimeRange(
    override val start: HelsinkiDateTime,
    override val end: HelsinkiDateTime,
) : BoundedRange<HelsinkiDateTime, FiniteDateTimeRange> {
    init {
        check(start <= end) {
            "Attempting to initialize invalid time range with start: $start, end: $end"
        }
    }

    override fun toString(): String = "[$start,$end)"

    override fun overlaps(other: FiniteDateTimeRange) =
        this.start < other.end && other.start < this.end

    override fun leftAdjacentTo(other: FiniteDateTimeRange): Boolean = this.end == other.start

    override fun rightAdjacentTo(other: FiniteDateTimeRange): Boolean = other.end == this.start

    override fun strictlyLeftTo(other: FiniteDateTimeRange): Boolean = this.end <= other.start

    override fun strictlyRightTo(other: FiniteDateTimeRange): Boolean = other.end <= this.start

    override fun intersection(other: FiniteDateTimeRange): FiniteDateTimeRange? =
        tryCreate(maxOf(this.start, other.start), minOf(this.end, other.end))

    override fun gap(other: FiniteDateTimeRange): FiniteDateTimeRange? =
        tryCreate(minOf(this.end, other.end), maxOf(this.start, other.start))

    override fun subtract(
        other: FiniteDateTimeRange,
    ): BoundedRange.SubtractResult<FiniteDateTimeRange> =
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

    override fun merge(other: FiniteDateTimeRange): FiniteDateTimeRange =
        FiniteDateTimeRange(minOf(this.start, other.start), maxOf(this.end, other.end))

    override fun includes(point: HelsinkiDateTime): Boolean =
        this.start <= point && point < this.end

    override fun relationTo(
        other: FiniteDateTimeRange,
    ): BoundedRange.Relation<FiniteDateTimeRange> =
        when {
            this.end <= other.start -> {
                BoundedRange.Relation.LeftTo(gap = tryCreate(this.end, other.start))
            }

            other.end <= this.start -> {
                BoundedRange.Relation.RightTo(gap = tryCreate(other.end, this.start))
            }

            else -> {
                BoundedRange.Relation.Overlap(
                    left =
                        when {
                            this.start < other.start -> {
                                BoundedRange.Relation.Remainder(
                                    range = FiniteDateTimeRange(this.start, other.start),
                                    isFirst = true,
                                )
                            }

                            other.start < this.start -> {
                                BoundedRange.Relation.Remainder(
                                    range = FiniteDateTimeRange(other.start, this.start),
                                    isFirst = false,
                                )
                            }

                            else -> {
                                null
                            }
                        },
                    overlap =
                        FiniteDateTimeRange(
                            maxOf(this.start, other.start),
                            minOf(this.end, other.end),
                        ),
                    right =
                        when {
                            other.end < this.end -> {
                                BoundedRange.Relation.Remainder(
                                    range = FiniteDateTimeRange(other.end, this.end),
                                    isFirst = true,
                                )
                            }

                            this.end < other.end -> {
                                BoundedRange.Relation.Remainder(
                                    range = FiniteDateTimeRange(this.end, other.end),
                                    isFirst = false,
                                )
                            }

                            else -> {
                                null
                            }
                        },
                )
            }
        }

    fun getDuration(): Duration = Duration.between(start.toInstant(), end.toInstant())

    companion object {
        fun tryCreate(start: HelsinkiDateTime, end: HelsinkiDateTime): FiniteDateTimeRange? =
            if (start < end) FiniteDateTimeRange(start, end) else null

        fun of(date: LocalDate, startTime: LocalTime, endTime: LocalTime) =
            FiniteDateTimeRange(
                HelsinkiDateTime.of(date, startTime),
                HelsinkiDateTime.of(date, endTime),
            )
    }
}
