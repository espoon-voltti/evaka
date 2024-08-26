// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.holidayperiod

import fi.espoo.evaka.reservations.ReservationEnabledPlacementRange
import fi.espoo.evaka.shared.HolidayPeriodId
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class HolidayPeriodTest {
    private val reservationsOpenOn = LocalDate.of(2024, 5, 1)
    private val reservationDeadline = LocalDate.of(2024, 5, 31)
    private val period = FiniteDateRange(LocalDate.of(2024, 7, 1), LocalDate.of(2024, 7, 6))
    private val holidayPeriod =
        HolidayPeriod(
            id = HolidayPeriodId(UUID.randomUUID()),
            period,
            reservationsOpenOn,
            reservationDeadline,
        )

    private val jan1 = LocalDate.of(2024, 1, 1)
    private val dec31 = LocalDate.of(2024, 12, 31)
    private val wholeYear =
        ReservationEnabledPlacementRange(created = jan1, range = FiniteDateRange(jan1, dec31))

    @Test
    fun `not yet reservable`() {
        assertEquals(
            HolidayPeriodEffect.NotYetReservable(period, reservationsOpenOn),
            holidayPeriod.effect(reservationsOpenOn.minusDays(1), listOf(wholeYear)),
        )
    }

    @Test
    fun `reservations open`() {
        assertEquals(
            HolidayPeriodEffect.ReservationsOpen,
            holidayPeriod.effect(reservationsOpenOn, listOf(wholeYear)),
        )
    }

    @Test
    fun `reservations closed`() {
        assertEquals(
            HolidayPeriodEffect.ReservationsClosed,
            holidayPeriod.effect(reservationDeadline.plusDays(1), listOf(wholeYear)),
        )
    }

    @Test
    fun `no placements - no effect`() {
        assertEquals(null, holidayPeriod.effect(reservationDeadline.plusDays(1), emptyList()))
    }

    @Test
    fun `placements outside reservation period - no effect`() {
        assertEquals(
            null,
            holidayPeriod.effect(
                reservationDeadline.plusDays(1),
                listOf(
                    ReservationEnabledPlacementRange(
                        created = jan1,
                        range = FiniteDateRange(jan1, reservationsOpenOn.minusDays(1)),
                    ),
                    ReservationEnabledPlacementRange(
                        created = reservationDeadline.plusDays(1),
                        range = FiniteDateRange(reservationDeadline.plusDays(1), dec31),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `one placement in reservation range and one created during holiday period, no gaps between placements - reservations closed`() {
        assertEquals(
            HolidayPeriodEffect.ReservationsClosed,
            holidayPeriod.effect(
                reservationDeadline.plusDays(1),
                listOf(
                    ReservationEnabledPlacementRange(
                        created = jan1,
                        range = FiniteDateRange(jan1, period.start.plusDays(2)),
                    ),
                    ReservationEnabledPlacementRange(
                        created = period.start,
                        range = FiniteDateRange(period.start.plusDays(3), period.end),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `one placement created before and one during holiday period, gap between placements - no effect`() {
        assertEquals(
            null,
            holidayPeriod.effect(
                reservationDeadline.plusDays(1),
                listOf(
                    ReservationEnabledPlacementRange(
                        created = jan1,
                        range = FiniteDateRange(jan1, period.start.plusDays(2)),
                    ),
                    // Gap of 1 day here
                    ReservationEnabledPlacementRange(
                        created = period.start,
                        range = FiniteDateRange(period.start.plusDays(4), period.end),
                    ),
                ),
            ),
        )
    }
}
