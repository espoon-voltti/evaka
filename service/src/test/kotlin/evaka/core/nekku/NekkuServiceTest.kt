// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.nekku

import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.isWeekendOrHoliday
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class NekkuServiceTest {
    @Test
    fun `a working day specifies the delivery dates four working days ahead`() {
        // Monday: only the Friday is four working days ahead
        assertEquals(
            FiniteDateRange(LocalDate.of(2025, 3, 28), LocalDate.of(2025, 3, 28)),
            deliveryDatesToSpecifyOn(LocalDate.of(2025, 3, 24)),
        )
        // Tuesday: the weekend and the Monday after it all have this Tuesday as their deadline
        assertEquals(
            FiniteDateRange(LocalDate.of(2025, 3, 29), LocalDate.of(2025, 3, 31)),
            deliveryDatesToSpecifyOn(LocalDate.of(2025, 3, 25)),
        )
        // the Thursday before Christmas covers the whole holiday period
        assertEquals(
            FiniteDateRange(LocalDate.of(2025, 12, 24), LocalDate.of(2025, 12, 29)),
            deliveryDatesToSpecifyOn(LocalDate.of(2025, 12, 18)),
        )
    }

    @Test
    fun `nothing is specified on a weekend or a holiday`() {
        assertNull(deliveryDatesToSpecifyOn(LocalDate.of(2025, 3, 29))) // Saturday
        assertNull(deliveryDatesToSpecifyOn(LocalDate.of(2025, 3, 30))) // Sunday
        assertNull(deliveryDatesToSpecifyOn(LocalDate.of(2025, 12, 25))) // Christmas Day
    }

    private fun deadlineFor(deliveryDate: LocalDate): LocalDate =
        generateSequence(deliveryDate.minusDays(1)) { it.minusDays(1) }
            .filter { !it.isWeekendOrHoliday() }
            .take(4)
            .last()

    @Test
    fun `every delivery date is specified exactly once, on its own deadline`() {
        val specifiedOn = mutableMapOf<LocalDate, LocalDate>()

        FiniteDateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)).dates().forEach {
            runDay ->
            deliveryDatesToSpecifyOn(runDay)?.dates()?.forEach { deliveryDate ->
                assertNull(specifiedOn[deliveryDate], "$deliveryDate specified twice")
                specifiedOn[deliveryDate] = runDay
                assertEquals(deadlineFor(deliveryDate), runDay, "wrong deadline for $deliveryDate")
            }
        }

        // deadlines of the first dates of the year fall in the previous year, outside the runs
        // above
        FiniteDateRange(LocalDate.of(2025, 1, 15), LocalDate.of(2025, 12, 31)).dates().forEach {
            deliveryDate ->
            assertNotNull(specifiedOn[deliveryDate], "$deliveryDate never specified")
        }
    }
}
