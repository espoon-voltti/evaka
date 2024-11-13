// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.holidays

import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.getHolidays
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class HolidaysTest {
    @Test
    fun `returns correct holidays for 2024`() {
        assertEquals(
            setOf(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 6),
                LocalDate.of(2024, 3, 29),
                LocalDate.of(2024, 3, 31),
                LocalDate.of(2024, 4, 1),
                LocalDate.of(2024, 5, 1),
                LocalDate.of(2024, 5, 9),
                LocalDate.of(2024, 5, 19),
                LocalDate.of(2024, 6, 21),
                LocalDate.of(2024, 6, 22),
                LocalDate.of(2024, 11, 2),
                LocalDate.of(2024, 12, 6),
                LocalDate.of(2024, 12, 24),
                LocalDate.of(2024, 12, 25),
                LocalDate.of(2024, 12, 26),
            ),
            getHolidays(FiniteDateRange(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31))),
        )
    }

    @Test
    fun `returns correct holidays for 2023`() {
        assertEquals(
            setOf(
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2023, 1, 6),
                LocalDate.of(2023, 4, 7),
                LocalDate.of(2023, 4, 9),
                LocalDate.of(2023, 4, 10),
                LocalDate.of(2023, 5, 1),
                LocalDate.of(2023, 5, 18),
                LocalDate.of(2023, 5, 28),
                LocalDate.of(2023, 6, 23),
                LocalDate.of(2023, 6, 24),
                LocalDate.of(2023, 11, 4),
                LocalDate.of(2023, 12, 6),
                LocalDate.of(2023, 12, 24),
                LocalDate.of(2023, 12, 25),
                LocalDate.of(2023, 12, 26),
            ),
            getHolidays(FiniteDateRange(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31))),
        )
    }
}
