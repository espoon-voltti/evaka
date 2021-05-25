// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.domain

import fi.espoo.evaka.shared.domain.DateRange
import java.math.BigDecimal

data class PricingWithValidity(
    val validDuring: DateRange,
    val multiplier: BigDecimal,
    val maxThresholdDifference: Int,
    val minThreshold2: Int,
    val minThreshold3: Int,
    val minThreshold4: Int,
    val minThreshold5: Int,
    val minThreshold6: Int,
    val thresholdIncrease6Plus: Int
) {
    fun withoutDates(): Pricing = Pricing(
        multiplier = multiplier,
        maxThresholdDifference = maxThresholdDifference,
        minThreshold2 = minThreshold2,
        minThreshold3 = minThreshold3,
        minThreshold4 = minThreshold4,
        minThreshold5 = minThreshold5,
        minThreshold6 = minThreshold6,
        thresholdIncrease6Plus = thresholdIncrease6Plus
    )
}

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
