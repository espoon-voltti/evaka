// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.domain

import fi.espoo.evaka.espoo.invoicing.EspooIncomeCoefficientMultiplierProvider
import fi.espoo.evaka.invoicing.calculateIncomeTotal
import fi.espoo.evaka.invoicing.calculateMonthlyAmount
import fi.espoo.evaka.invoicing.domain.IncomeCoefficient.MONTHLY_NO_HOLIDAY_BONUS
import fi.espoo.evaka.invoicing.domain.IncomeCoefficient.YEARLY
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class IncomesTest {

    private val coefficientMultiplierProvider = EspooIncomeCoefficientMultiplierProvider()

    @Test
    fun `Income total with empty data`() {
        val total = calculateIncomeTotal(mapOf(), coefficientMultiplierProvider)
        assertEquals(0, total)
    }

    @Test
    fun `Income total with only MAIN_INCOME`() {
        val incomeData = mapOf("MAIN_INCOME" to IncomeValue(50000, MONTHLY_NO_HOLIDAY_BONUS, 1, 0))
        val total = calculateIncomeTotal(incomeData, coefficientMultiplierProvider)
        assertEquals(50000, total)
    }

    @Test
    fun `Income total with multiple incomes`() {
        val incomeData =
            mapOf(
                "MAIN_INCOME" to
                    IncomeValue(
                        50000,
                        MONTHLY_NO_HOLIDAY_BONUS,
                        1,
                        calculateMonthlyAmount(
                            50000,
                            coefficientMultiplierProvider.multiplier(MONTHLY_NO_HOLIDAY_BONUS)
                        )
                    ),
                "SECONDARY_INCOME" to
                    IncomeValue(
                        100000,
                        MONTHLY_NO_HOLIDAY_BONUS,
                        1,
                        calculateMonthlyAmount(
                            100000,
                            coefficientMultiplierProvider.multiplier(MONTHLY_NO_HOLIDAY_BONUS)
                        )
                    ),
                "PARENTAL_ALLOWANCE" to
                    IncomeValue(
                        20000,
                        MONTHLY_NO_HOLIDAY_BONUS,
                        1,
                        calculateMonthlyAmount(
                            20000,
                            coefficientMultiplierProvider.multiplier(MONTHLY_NO_HOLIDAY_BONUS)
                        )
                    )
            )

        val total = calculateIncomeTotal(incomeData, coefficientMultiplierProvider)
        assertEquals(170000, total)
    }

    @Test
    fun `Income total with expenses`() {
        val incomeData =
            mapOf(
                "ALL_EXPENSES" to
                    IncomeValue(
                        10000,
                        MONTHLY_NO_HOLIDAY_BONUS,
                        -1,
                        calculateMonthlyAmount(
                            10000,
                            coefficientMultiplierProvider.multiplier(MONTHLY_NO_HOLIDAY_BONUS)
                        )
                    )
            )

        val total = calculateIncomeTotal(incomeData, coefficientMultiplierProvider)
        assertEquals(-10000, total)
    }

    @Test
    fun `Income total with income and expenses`() {
        val incomeData =
            mapOf(
                "MAIN_INCOME" to
                    IncomeValue(
                        500000,
                        MONTHLY_NO_HOLIDAY_BONUS,
                        1,
                        calculateMonthlyAmount(
                            500000,
                            coefficientMultiplierProvider.multiplier(MONTHLY_NO_HOLIDAY_BONUS)
                        )
                    ),
                "ALL_EXPENSES" to
                    IncomeValue(
                        50000,
                        MONTHLY_NO_HOLIDAY_BONUS,
                        -1,
                        calculateMonthlyAmount(
                            50000,
                            coefficientMultiplierProvider.multiplier(MONTHLY_NO_HOLIDAY_BONUS)
                        )
                    )
            )

        val total = calculateIncomeTotal(incomeData, coefficientMultiplierProvider)
        assertEquals(450000, total)
    }

    @Test
    fun `Income total with YEARLY coefficient`() {
        val incomeData =
            mapOf(
                "MAIN_INCOME" to
                    IncomeValue(
                        100000,
                        YEARLY,
                        1,
                        calculateMonthlyAmount(
                            100000,
                            coefficientMultiplierProvider.multiplier(YEARLY)
                        )
                    )
            )

        val total = calculateIncomeTotal(incomeData, coefficientMultiplierProvider)
        assertEquals(8330, total)
    }
}
