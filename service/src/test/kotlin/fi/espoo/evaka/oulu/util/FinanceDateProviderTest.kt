// SPDX-FileCopyrightText: 2023-2025 City of Oulu
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.oulu.util

import fi.espoo.evaka.shared.domain.MockEvakaClock
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FinanceDateProviderTest {
    @Test
    fun `should default to today`() {
        val financeDateProvider = FinanceDateProvider()
        val invoiceIdFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        val expectedDate = LocalDate.now(ZoneId.of("Europe/Helsinki")).format(invoiceIdFormatter)

        val actualDate = financeDateProvider.currentDate()

        assertThat(actualDate).isEqualTo(expectedDate)
    }

    @Test
    fun `should obey the date in the constructor parameter if given`() {
        val financeDateProvider = FinanceDateProvider(MockEvakaClock(2025, 7, 14, 12, 34, 56))
        val expectedDate = "20250714"

        val actualDate = financeDateProvider.currentDate()

        assertThat(actualDate).isEqualTo(expectedDate)
    }

    @Test
    fun `should return correct previous month`() {
        val financeDateProvider = FinanceDateProvider(MockEvakaClock(2025, 7, 14, 12, 34, 56))
        val expectedDate = "06.2025"

        val actualDate = financeDateProvider.previousMonth()

        assertThat(actualDate).isEqualTo(expectedDate)

        val anotherFinanceDateProvider =
            FinanceDateProvider(MockEvakaClock(2025, 1, 14, 12, 34, 56))
        val anotherExpectedDate = "12.2024"

        val anotherActualDate = anotherFinanceDateProvider.previousMonth()

        assertThat(anotherActualDate).isEqualTo(anotherExpectedDate)
    }

    @Test
    fun `should return date with correct abbreviated format`() {
        val financeDateProvider = FinanceDateProvider(MockEvakaClock(2025, 7, 14, 12, 34, 56))
        val expectedDate = "250714"

        val actualDate = financeDateProvider.currentDateWithAbbreviatedYear()

        assertThat(actualDate).isEqualTo(expectedDate)
    }

    @Test
    fun `should return correct last date of previous month`() {
        val financeDateProvider = FinanceDateProvider(MockEvakaClock(2025, 7, 14, 12, 34, 56))
        val expectedDate = "250630"

        val actualDate = financeDateProvider.previousMonthLastDate()

        assertThat(actualDate).isEqualTo(expectedDate)

        val anotherFinanceDateProvider = FinanceDateProvider(MockEvakaClock(2024, 3, 7, 12, 34, 56))
        val anotherExpectedDate = "240229"

        val anotherActualDate = anotherFinanceDateProvider.previousMonthLastDate()

        assertThat(anotherActualDate).isEqualTo(anotherExpectedDate)
    }

    @Test
    fun `should return correct previous month in YYMM format`() {
        val financeDateProvider = FinanceDateProvider(MockEvakaClock(2025, 7, 14, 12, 34, 56))
        val expectedDate = "2506"

        val actualDate = financeDateProvider.previousMonthYYMM()

        assertThat(actualDate).isEqualTo(expectedDate)
    }

    @Test
    fun `should return different date tomorrow`() {
        val mockClock = MockEvakaClock(2025, 7, 14, 12, 34, 56)
        val financeDateProvider = FinanceDateProvider(mockClock)
        assertThat("20250714").isEqualTo(financeDateProvider.currentDate())
        mockClock.tick(Duration.ofHours(24))
        assertThat("20250715").isEqualTo(financeDateProvider.currentDate())
    }
}
