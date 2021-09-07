// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.domain

import fi.espoo.evaka.invoicing.domain.IncomeCoefficient.MONTHLY_NO_HOLIDAY_BONUS
import fi.espoo.evaka.invoicing.domain.IncomeCoefficient.YEARLY
import fi.espoo.evaka.invoicing.testIncome
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class IncomesTest {
    @Test
    fun `Income total with empty data`() {
        val income = testIncome.copy(data = mapOf())

        assertEquals(0, income.total())
    }

    @Test
    fun `Income total with only MAIN_INCOME`() {
        val income =
            testIncome.copy(data = mapOf("MAIN_INCOME" to IncomeValue(50000, MONTHLY_NO_HOLIDAY_BONUS, 1)))

        assertEquals(50000, income.total())
    }

    @Test
    fun `Income total with multiple incomes`() {
        val income = testIncome.copy(
            data = mapOf(
                "MAIN_INCOME" to IncomeValue(50000, MONTHLY_NO_HOLIDAY_BONUS, 1),
                "SECONDARY_INCOME" to IncomeValue(100000, MONTHLY_NO_HOLIDAY_BONUS, 1),
                "PARENTAL_ALLOWANCE" to IncomeValue(20000, MONTHLY_NO_HOLIDAY_BONUS, 1)
            )
        )

        assertEquals(170000, income.total())
    }

    @Test
    fun `Income total with expenses`() {
        val income =
            testIncome.copy(data = mapOf("ALL_EXPENSES" to IncomeValue(10000, MONTHLY_NO_HOLIDAY_BONUS, -1)))

        assertEquals(-10000, income.total())
    }

    @Test
    fun `Income total with income and expenses`() {
        val income = testIncome.copy(
            data = mapOf(
                "MAIN_INCOME" to IncomeValue(500000, MONTHLY_NO_HOLIDAY_BONUS, 1),
                "ALL_EXPENSES" to IncomeValue(50000, MONTHLY_NO_HOLIDAY_BONUS, -1)
            )
        )

        assertEquals(450000, income.total())
    }

    @Test
    fun `Income total with YEARLY coefficient`() {
        val income =
            testIncome.copy(data = mapOf("MAIN_INCOME" to IncomeValue(100000, YEARLY, 1)))

        assertEquals(8330, income.total())
    }
}
