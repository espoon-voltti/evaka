// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.domain

import java.math.BigDecimal

data class Pricing(
    val multiplier: BigDecimal,
    val maxThresholdDifference: Int,
    val minThreshold2: Int,
    val minThreshold3: Int,
    val minThreshold4: Int,
    val minThreshold5: Int,
    val minThreshold6: Int,
    val thresholdIncrease6Plus: Int
)
