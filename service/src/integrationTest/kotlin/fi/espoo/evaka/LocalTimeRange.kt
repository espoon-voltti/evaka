// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka

import java.time.Duration
import java.time.LocalTime

class LocalTimeRange(
    override val start: LocalTime,
    override val endInclusive: LocalTime,
    private val step: Duration = Duration.ofMinutes(1)
) : Iterable<LocalTime>, ClosedRange<LocalTime> {
    override fun iterator(): Iterator<LocalTime> = LocalTimeIterator(start, endInclusive, step)
}

class LocalTimeIterator(
    start: LocalTime,
    private val endInclusive: LocalTime,
    private val step: Duration
) : Iterator<LocalTime> {
    private var current = start
    override fun hasNext(): Boolean = current <= endInclusive

    override fun next(): LocalTime {
        val next = current
        current += step
        return next
    }
}
