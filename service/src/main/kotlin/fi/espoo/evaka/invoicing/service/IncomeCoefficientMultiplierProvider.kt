// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.invoicing.domain.IncomeCoefficient
import java.math.BigDecimal

fun interface IncomeCoefficientMultiplierProvider {
    fun multiplier(coefficient: IncomeCoefficient): BigDecimal
}
