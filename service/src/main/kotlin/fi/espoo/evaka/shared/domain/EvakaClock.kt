// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.domain

import java.time.Clock
import java.time.LocalDate

interface EvakaClock {
    fun today(): LocalDate
    fun now(): HelsinkiDateTime
}

class MockEvakaClock(private val now: HelsinkiDateTime) : EvakaClock {
    override fun today(): LocalDate = now.toLocalDate()
    override fun now(): HelsinkiDateTime = now
}

class RealEvakaClock(private val clock: Clock = Clock.systemUTC()) : EvakaClock {
    override fun today(): LocalDate = LocalDate.now(europeHelsinki)
    override fun now(): HelsinkiDateTime = HelsinkiDateTime.now(clock)
}
