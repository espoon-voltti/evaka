// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.domain

import fi.espoo.evaka.invoicing.domain.IncomeCoefficient.MONTHLY_NO_HOLIDAY_BONUS
import fi.espoo.evaka.invoicing.domain.IncomeCoefficient.YEARLY
import fi.espoo.evaka.invoicing.testIncome
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class IncomesTest {
    @Test
    fun `Income total with empty data`() {
        val income = testIncome.copy(data = mapOf())

        Assertions.assertEquals(0, income.total())
    }

    @Test
    fun `Income total with only MAIN_INCOME`() {
        val income =
            testIncome.copy(data = mapOf(IncomeType.MAIN_INCOME to IncomeValue(50000, MONTHLY_NO_HOLIDAY_BONUS)))

        Assertions.assertEquals(50000, income.total())
    }

    @Test
    fun `Income total with multiple incomes`() {
        val income = testIncome.copy(
            data = mapOf(
                IncomeType.MAIN_INCOME to IncomeValue(50000, MONTHLY_NO_HOLIDAY_BONUS),
                IncomeType.SECONDARY_INCOME to IncomeValue(100000, MONTHLY_NO_HOLIDAY_BONUS),
                IncomeType.PARENTAL_ALLOWANCE to IncomeValue(20000, MONTHLY_NO_HOLIDAY_BONUS)
            )
        )

        Assertions.assertEquals(170000, income.total())
    }

    @Test
    fun `Income total with expenses`() {
        val income =
            testIncome.copy(data = mapOf(IncomeType.ALL_EXPENSES to IncomeValue(10000, MONTHLY_NO_HOLIDAY_BONUS)))

        Assertions.assertEquals(-10000, income.total())
    }

    @Test
    fun `Income total with income and expenses`() {
        val income = testIncome.copy(
            data = mapOf(
                IncomeType.MAIN_INCOME to IncomeValue(500000, MONTHLY_NO_HOLIDAY_BONUS),
                IncomeType.ALL_EXPENSES to IncomeValue(50000, MONTHLY_NO_HOLIDAY_BONUS)
            )
        )

        Assertions.assertEquals(450000, income.total())
    }

    @Test
    fun `Income total with YEARLY coefficient`() {
        val income =
            testIncome.copy(data = mapOf(IncomeType.MAIN_INCOME to IncomeValue(100000, YEARLY)))

        Assertions.assertEquals(8330, income.total())
    }
}
