// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.espoo.invoicing

import fi.espoo.evaka.invoicing.domain.IncomeCoefficient
import fi.espoo.evaka.invoicing.service.IncomeCoefficientMultiplierProvider
import java.math.BigDecimal

class EspooIncomeCoefficientMultiplierProvider : IncomeCoefficientMultiplierProvider {

    override fun multiplier(coefficient: IncomeCoefficient): BigDecimal =
        when (coefficient) {
            IncomeCoefficient.MONTHLY_WITH_HOLIDAY_BONUS -> BigDecimal("1.0417")
            IncomeCoefficient.MONTHLY_NO_HOLIDAY_BONUS -> BigDecimal("1.0000")
            IncomeCoefficient.BI_WEEKLY_WITH_HOLIDAY_BONUS -> BigDecimal("2.2323")
            IncomeCoefficient.BI_WEEKLY_NO_HOLIDAY_BONUS -> BigDecimal("2.1429")
            IncomeCoefficient.DAILY_ALLOWANCE_21_5 -> BigDecimal("21.5")
            IncomeCoefficient.DAILY_ALLOWANCE_25 -> BigDecimal("25")
            IncomeCoefficient.YEARLY -> BigDecimal("0.0833")
        }
}
