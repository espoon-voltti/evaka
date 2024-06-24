// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service.generator

import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class DateRangeUtilsTest {
    @Test
    fun `buildFiniteDateRanges returns correct ranges`() {
        val datesOfChange =
            setOf(
                LocalDate.of(2000, 5, 1),
                LocalDate.of(2000, 3, 1),
                LocalDate.of(2000, 7, 1),
                LocalDate.of(2000, 4, 1),
                LocalDate.of(2000, 4, 1),
                LocalDate.of(2000, 9, 1)
            )
        assertEquals(
            listOf(
                FiniteDateRange(LocalDate.of(2000, 3, 1), LocalDate.of(2000, 3, 31)),
                FiniteDateRange(LocalDate.of(2000, 4, 1), LocalDate.of(2000, 4, 30)),
                FiniteDateRange(LocalDate.of(2000, 5, 1), LocalDate.of(2000, 6, 30)),
                FiniteDateRange(LocalDate.of(2000, 7, 1), LocalDate.of(2000, 8, 31))
            ),
            buildFiniteDateRanges(datesOfChange)
        )
    }

    @Test
    fun `buildDateRanges returns correct ranges`() {
        val datesOfChange =
            setOf(
                LocalDate.of(2000, 5, 1),
                LocalDate.of(2000, 3, 1),
                LocalDate.of(2000, 7, 1),
                LocalDate.of(2000, 4, 1),
                LocalDate.of(2000, 4, 1),
                LocalDate.of(2000, 9, 1)
            )
        assertEquals(
            listOf(
                DateRange(LocalDate.of(2000, 3, 1), LocalDate.of(2000, 3, 31)),
                DateRange(LocalDate.of(2000, 4, 1), LocalDate.of(2000, 4, 30)),
                DateRange(LocalDate.of(2000, 5, 1), LocalDate.of(2000, 6, 30)),
                DateRange(LocalDate.of(2000, 7, 1), LocalDate.of(2000, 8, 31)),
                DateRange(LocalDate.of(2000, 9, 1), null)
            ),
            buildDateRanges(datesOfChange)
        )
    }

    @Test
    fun `getDatesOfChange returns correct dates`() {
        val entities1 =
            listOf(
                TestEntity(DateRange(LocalDate.of(2000, 5, 1), LocalDate.of(2000, 8, 1)), 0),
                TestEntity(DateRange(LocalDate.of(2000, 9, 1), null), 4)
            )
        val entities2 =
            listOf(
                TestEntityFinite(
                    FiniteDateRange(LocalDate.of(2000, 4, 1), LocalDate.of(2000, 10, 1)),
                    "b"
                )
            )
        assertEquals(
            setOf(
                LocalDate.of(2000, 5, 1),
                LocalDate.of(2000, 8, 2),
                LocalDate.of(2000, 9, 1),
                LocalDate.of(2000, 4, 1),
                LocalDate.of(2000, 10, 2)
            ),
            getDatesOfChange(*entities1.toTypedArray(), *entities2.toTypedArray())
        )
    }

    private data class TestEntity(
        override val range: DateRange,
        val value: Int
    ) : WithRange

    private data class TestEntityFinite(
        override val finiteRange: FiniteDateRange,
        val value: String
    ) : WithFiniteRange
}
