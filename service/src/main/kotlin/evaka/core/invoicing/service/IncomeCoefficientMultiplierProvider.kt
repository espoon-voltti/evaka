// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.invoicing.service

import evaka.core.invoicing.domain.IncomeCoefficient
import java.math.BigDecimal

fun interface IncomeCoefficientMultiplierProvider {
    fun multiplier(coefficient: IncomeCoefficient): BigDecimal
}
