// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import fi.espoo.evaka.holidayperiod.HolidayPeriodDeadline
import fi.espoo.evaka.shared.HolidayPeriodId
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals

class ReservableDaysCalculationUnitTest {
    private val thresholdMondayAt1800 = 150L

    private val end2021 = LocalDate.of(2021, 7, 31)
    private val end2022 = LocalDate.of(2022, 7, 31)

    private fun assertSingleReservableDaysRange(expected: FiniteDateRange, date: LocalDateTime) {
        assertEquals(
            listOf(expected),
            getReservableDays(HelsinkiDateTime.of(date), thresholdMondayAt1800, listOf())
        )
    }

    @Test
    fun `reservable days query date is Monday`() {
        assertSingleReservableDaysRange(
            FiniteDateRange(LocalDate.of(2021, 6, 14), end2021),
            LocalDateTime.of(2021, 6, 7, 12, 0)
        )
    }

    @Test
    fun `reservable days query date is Tuesday`() {
        assertSingleReservableDaysRange(
            FiniteDateRange(LocalDate.of(2021, 6, 21), end2021),
            LocalDateTime.of(2021, 6, 8, 12, 0)
        )
    }

    @Test
    fun `reservable days includes next year when start is in July`() {
        assertSingleReservableDaysRange(
            FiniteDateRange(LocalDate.of(2021, 7, 5), end2022),
            LocalDateTime.of(2021, 6, 28, 12, 0)
        )
    }

    @Test
    fun `reservable days query date is in September`() {
        assertSingleReservableDaysRange(
            FiniteDateRange(LocalDate.of(2021, 9, 13), end2022),
            LocalDateTime.of(2021, 9, 1, 12, 0)
        )
    }

    private val justBeforeThreshold = LocalDateTime.of(2021, 9, 13, 17, 59)
    private val mondayNextWeekAfterThreshold = LocalDate.of(2021, 9, 20)

    @Test
    fun `reservable days query date is just before threshold`() {
        assertSingleReservableDaysRange(
            FiniteDateRange(mondayNextWeekAfterThreshold, end2022),
            justBeforeThreshold
        )
    }

    @Test
    fun `reservable days query date is just at threshold`() {
        val justAtThreshold = justBeforeThreshold.plusMinutes(1)
        assertSingleReservableDaysRange(
            FiniteDateRange(mondayNextWeekAfterThreshold.plusWeeks(1), end2022),
            justAtThreshold
        )
    }

    private val winter = HolidayPeriodDeadline(
        id = HolidayPeriodId(UUID.randomUUID()),
        period = FiniteDateRange(LocalDate.of(2021, 2, 20), LocalDate.of(2021, 2, 28)),
        reservationDeadline = LocalDate.of(2021, 1, 20)
    )

    private val summer = HolidayPeriodDeadline(
        id = HolidayPeriodId(UUID.randomUUID()),
        period = FiniteDateRange(LocalDate.of(2021, 6, 1), LocalDate.of(2021, 8, 31)),
        reservationDeadline = LocalDate.of(2021, 5, 10)
    )
    private val midnightAfterSummerDeadline =
        HelsinkiDateTime.of(summer.reservationDeadline.plusDays(1), LocalTime.of(0, 0))

    private val deadlines = listOf(winter, summer)

    private fun assertReservableDays(expected: List<FiniteDateRange>, date: HelsinkiDateTime) {
        assertEquals(
            expected,
            getReservableDays(date, thresholdMondayAt1800, deadlines)
        )
    }

    @Test
    fun `reservable days does not include holiday periods where deadline has passed`() {
        assertReservableDays(
            listOf(
                FiniteDateRange(LocalDate.of(2021, 5, 24), summer.period.start.minusDays(1))
            ),
            midnightAfterSummerDeadline,
        )
    }

    @Test
    fun `reservable days includes holiday periods where deadline is a minute away`() {
        assertReservableDays(
            listOf(
                FiniteDateRange(LocalDate.of(2021, 5, 24), end2021)
            ),
            midnightAfterSummerDeadline.minusMinutes(1),
        )
    }

    @Test
    fun `reservable days includes holiday periods where deadline has not passed`() {
        assertReservableDays(
            listOf(
                FiniteDateRange(LocalDate.of(2021, 3, 8), end2021)
            ),
            HelsinkiDateTime.of(LocalDateTime.of(2021, 3, 1, 12, 0)),
        )
    }

    @Test
    fun `reservable days is split to multiple periods when holiday period's deadline has passed`() {
        assertReservableDays(
            listOf(
                FiniteDateRange(LocalDate.of(2021, 2, 1), winter.period.start.minusDays(1)),
                FiniteDateRange(winter.period.end.plusDays(1), end2021)
            ),
            HelsinkiDateTime.of(winter.reservationDeadline.plusDays(1), LocalTime.of(0, 0)),
        )
    }
}
