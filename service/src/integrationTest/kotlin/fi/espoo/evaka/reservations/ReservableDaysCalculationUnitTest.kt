// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals

class ReservableDaysCalculationUnitTest {
    private val thresholdMondayAt1800 = 150L

    private val end2021 = LocalDate.of(2021, 7, 31)
    private val end2022 = LocalDate.of(2022, 7, 31)

    @Test
    fun `reservable days query date is Monday`() {
        assertEquals(
            FiniteDateRange(LocalDate.of(2021, 6, 14), end2021),
            getReservableDays(HelsinkiDateTime.of(LocalDateTime.of(2021, 6, 7, 12, 0)), thresholdMondayAt1800)
        )
    }

    @Test
    fun `reservable days query date is Tuesday`() {
        assertEquals(
            FiniteDateRange(LocalDate.of(2021, 6, 21), end2021),
            getReservableDays(HelsinkiDateTime.of(LocalDateTime.of(2021, 6, 8, 12, 0)), thresholdMondayAt1800)
        )
    }

    @Test
    fun `reservable days includes next year when start is in July`() {
        assertEquals(
            FiniteDateRange(LocalDate.of(2021, 7, 5), end2022),
            getReservableDays(HelsinkiDateTime.of(LocalDateTime.of(2021, 6, 28, 12, 0)), thresholdMondayAt1800)
        )
    }

    @Test
    fun `reservable days query date is in September`() {
        assertEquals(
            FiniteDateRange(LocalDate.of(2021, 9, 13), end2022),
            getReservableDays(HelsinkiDateTime.of(LocalDateTime.of(2021, 9, 1, 12, 0)), thresholdMondayAt1800)
        )
    }

    private val justBeforeThreshold = HelsinkiDateTime.of(LocalDateTime.of(2021, 9, 13, 17, 59))
    private val mondayNextWeekAfterThreshold = LocalDate.of(2021, 9, 20)
    @Test
    fun `reservable days query date is just before threshold`() {
        assertEquals(
            FiniteDateRange(mondayNextWeekAfterThreshold, end2022),
            getReservableDays(justBeforeThreshold, thresholdMondayAt1800)
        )
    }

    @Test
    fun `reservable days query date is just at threshold`() {
        val justAtThreshold = justBeforeThreshold.plusMinutes(1)
        assertEquals(
            FiniteDateRange(mondayNextWeekAfterThreshold.plusWeeks(1), end2022),
            getReservableDays(justAtThreshold, thresholdMondayAt1800)
        )
    }
}
