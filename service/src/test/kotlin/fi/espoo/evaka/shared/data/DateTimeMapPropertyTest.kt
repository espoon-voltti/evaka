// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.data

import fi.espoo.evaka.shared.dateTimeMap
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.HelsinkiDateTimeRange
import fi.espoo.evaka.shared.helsinkiDateTimeRange
import io.kotest.common.runBlocking
import io.kotest.property.Arb
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.positiveInt
import io.kotest.property.checkAll
import java.time.Duration
import kotlin.sequences.map
import kotlin.test.assertNull
import org.junit.jupiter.api.Test

class DateTimeMapPropertyTest :
    RangeBasedMapPropertyTest<HelsinkiDateTime, HelsinkiDateTimeRange, DateTimeMap<Int>>() {
    override fun emptyMap(): DateTimeMap<Int> = DateTimeMap.empty()

    override fun arbitraryMap(value: Arb<Int>): Arb<DateTimeMap<Int>> = Arb.dateTimeMap(value)

    override fun arbitraryRange(duration: Arb<Int>): Arb<HelsinkiDateTimeRange> =
        Arb.helsinkiDateTimeRange(duration = duration.map { Duration.ofNanos(it.toLong() * 1000L) })

    override fun defaultDuration(): Arb<Int> = Arb.positiveInt(max = 48 * 60 * 60)

    override fun pointsOfRange(range: HelsinkiDateTimeRange): Sequence<HelsinkiDateTime> =
        if (range.start == range.end) emptySequence()
        else
            generateSequence(range.start) {
                it.plus(Duration.ofNanos(1000)).takeIf { it < range.end }
            }

    @Test
    fun `it returns null for every endpoint of every gap`() {
        runBlocking {
            checkAll(Arb.list(arbitraryRange(duration = Arb.positiveInt(10)))) { ranges ->
                val value = 1
                val map = emptyMap().setAll(ranges.asSequence().map { it to value })
                for (gap in map.gaps()) {
                    assertNull(map.getValue(gap.start))
                    if (gap.end > gap.start)
                        assertNull(map.getValue(gap.end.minus(Duration.ofNanos(1000))))
                }
            }
        }
    }
}
