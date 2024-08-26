// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.domain

import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

interface EvakaClock {
    fun today(): LocalDate

    fun now(): HelsinkiDateTime
}

class MockEvakaClock(private var now: HelsinkiDateTime) : EvakaClock {
    constructor(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        second: Int = 0,
    ) : this(
        HelsinkiDateTime.of(LocalDate.of(year, month, day), LocalTime.of(hour, minute, second))
    )

    override fun today(): LocalDate = now.toLocalDate()

    override fun now(): HelsinkiDateTime = now

    fun tick(duration: Duration = Duration.ofSeconds(1)) {
        now += duration
    }
}

class RealEvakaClock(private val clock: Clock = Clock.systemUTC()) : EvakaClock {
    override fun today(): LocalDate = LocalDate.now(europeHelsinki)

    override fun now(): HelsinkiDateTime = HelsinkiDateTime.now(clock)
}
