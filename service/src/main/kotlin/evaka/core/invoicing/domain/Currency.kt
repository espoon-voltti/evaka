// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.invoicing.domain

import java.math.BigDecimal
import java.math.RoundingMode

fun roundToEuros(cents: BigDecimal): BigDecimal =
    cents.divide(BigDecimal(100), 0, RoundingMode.HALF_UP).multiply(BigDecimal(100))
