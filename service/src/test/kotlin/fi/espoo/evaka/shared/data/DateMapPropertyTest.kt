// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.data

import fi.espoo.evaka.shared.dateMap
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.finiteDateRange
import io.kotest.common.runBlocking
import io.kotest.property.Arb
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.positiveInt
import io.kotest.property.checkAll
import java.time.LocalDate
import kotlin.sequences.map
import kotlin.test.assertNull
import org.junit.jupiter.api.Test

class DateMapPropertyTest : RangeBasedMapPropertyTest<LocalDate, FiniteDateRange, DateMap<Int>>() {
    override fun emptyMap(): DateMap<Int> = DateMap.empty()

    override fun arbitraryMap(value: Arb<Int>): Arb<DateMap<Int>> = Arb.dateMap(value)

    override fun arbitraryRange(duration: Arb<Int>): Arb<FiniteDateRange> =
        Arb.finiteDateRange(durationDays = duration.map { it.toLong() })

    override fun defaultDuration(): Arb<Int> = Arb.positiveInt(max = 3650)

    override fun pointsOfRange(range: FiniteDateRange): Sequence<LocalDate> = range.dates()

    @Test
    fun `it returns null for every endpoint of every gap`() {
        runBlocking {
            checkAll(Arb.list(arbitraryRange(duration = Arb.positiveInt(10)))) { ranges ->
                val value = 1
                val map = emptyMap().setAll(ranges.asSequence().map { it to value })
                for (gap in map.gaps()) {
                    assertNull(map.getValue(gap.start))
                    assertNull(map.getValue(gap.end))
                }
            }
        }
    }
}
