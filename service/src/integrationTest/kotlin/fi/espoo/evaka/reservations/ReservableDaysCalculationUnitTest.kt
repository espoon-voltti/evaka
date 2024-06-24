// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class ReservableDaysCalculationUnitTest {
    private val thresholdMondayAt1800 = 150L

    private val end2021 = LocalDate.of(2021, 8, 31)
    private val end2022 = LocalDate.of(2022, 8, 31)

    private fun assertReservableDays(
        expected: FiniteDateRange,
        date: LocalDateTime
    ) {
        assertEquals(expected, getReservableRange(HelsinkiDateTime.of(date), thresholdMondayAt1800))
    }

    @Test
    fun `reservable days query date is Monday`() {
        assertReservableDays(
            FiniteDateRange(LocalDate.of(2021, 6, 14), end2021),
            LocalDateTime.of(2021, 6, 7, 12, 0)
        )
    }

    @Test
    fun `reservable days query date is Tuesday`() {
        assertReservableDays(
            FiniteDateRange(LocalDate.of(2021, 6, 21), end2021),
            LocalDateTime.of(2021, 6, 8, 12, 0)
        )
    }

    @Test
    fun `reservable days includes next year when start is in July`() {
        assertReservableDays(
            FiniteDateRange(LocalDate.of(2021, 7, 5), end2022),
            LocalDateTime.of(2021, 6, 28, 12, 0)
        )
    }

    @Test
    fun `reservable days query date is in September`() {
        assertReservableDays(
            FiniteDateRange(LocalDate.of(2021, 9, 13), end2022),
            LocalDateTime.of(2021, 9, 1, 12, 0)
        )
    }

    private val justBeforeThreshold = LocalDateTime.of(2021, 9, 13, 17, 59)
    private val mondayNextWeekAfterThreshold = LocalDate.of(2021, 9, 20)

    @Test
    fun `reservable days query date is just before threshold`() {
        assertReservableDays(
            FiniteDateRange(mondayNextWeekAfterThreshold, end2022),
            justBeforeThreshold
        )
    }

    @Test
    fun `reservable days query date is just at threshold`() {
        val justAtThreshold = justBeforeThreshold.plusMinutes(1)
        assertReservableDays(
            FiniteDateRange(mondayNextWeekAfterThreshold.plusWeeks(1), end2022),
            justAtThreshold
        )
    }
}
