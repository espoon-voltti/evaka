// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.data

import fi.espoo.evaka.shared.dateSet
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.finiteDateRange
import io.kotest.common.runBlocking
import io.kotest.property.Arb
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.positiveInt
import io.kotest.property.arbitrary.positiveLong
import io.kotest.property.checkAll
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DateSetPropertyTest : RangeBasedSetPropertyTest<LocalDate, FiniteDateRange, DateSet>() {
    @Test
    fun `it includes every date of every range added to it`() {
        runBlocking {
            checkAll(Arb.list(Arb.finiteDateRange(durationDays = Arb.positiveLong(max = 10)))) {
                ranges ->
                val set = emptySet().addAll(ranges)
                assertTrue(set.ranges().flatMap { it.dates() }.all { set.includes(it) })
            }
        }
    }

    @Test
    fun `when a range is removed, it no longer includes any points of the range`() {
        runBlocking {
            checkAll(
                Arb.list(arbitraryRange()),
                Arb.finiteDateRange(durationDays = Arb.positiveLong(max = 10L)),
            ) { otherRanges, range ->
                val set = DateSet.of(range).addAll(otherRanges).remove(range)
                assertFalse(range.dates().any { set.includes(it) })
            }
        }
    }

    override fun emptySet(): DateSet = DateSet.empty()

    override fun arbitrarySet(): Arb<DateSet> = Arb.dateSet()

    override fun arbitraryRange(duration: Arb<Int>): Arb<FiniteDateRange> =
        Arb.finiteDateRange(durationDays = duration.map { it.toLong() })

    override fun defaultDuration(): Arb<Int> = Arb.positiveInt(max = 3650)
}
