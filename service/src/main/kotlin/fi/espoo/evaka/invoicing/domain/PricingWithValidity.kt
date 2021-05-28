// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.domain

import fi.espoo.evaka.shared.domain.DateRange
import java.math.BigDecimal
import java.util.UUID

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

data class FeeThresholdsWithValidity(
    val id: UUID,
    val validDuring: DateRange,
    val minIncomeThreshold2: Int,
    val minIncomeThreshold3: Int,
    val minIncomeThreshold4: Int,
    val minIncomeThreshold5: Int,
    val minIncomeThreshold6: Int,
    val incomeMultiplier2: BigDecimal,
    val incomeMultiplier3: BigDecimal,
    val incomeMultiplier4: BigDecimal,
    val incomeMultiplier5: BigDecimal,
    val incomeMultiplier6: BigDecimal,
    val maxIncomeThreshold2: Int,
    val maxIncomeThreshold3: Int,
    val maxIncomeThreshold4: Int,
    val maxIncomeThreshold5: Int,
    val maxIncomeThreshold6: Int,
    val thresholdIncrease6Plus: Int,
    val siblingDiscount2: BigDecimal,
    val siblingDiscount2Plus: BigDecimal,
    val maxFee: Int,
    val minFee: Int
) {
    fun withoutDates(): FeeThresholds = FeeThresholds(
        id = id,
        minIncomeThreshold2 = minIncomeThreshold2,
        minIncomeThreshold3 = minIncomeThreshold3,
        minIncomeThreshold4 = minIncomeThreshold4,
        minIncomeThreshold5 = minIncomeThreshold5,
        minIncomeThreshold6 = minIncomeThreshold6,
        incomeMultiplier2 = incomeMultiplier2,
        incomeMultiplier3 = incomeMultiplier3,
        incomeMultiplier4 = incomeMultiplier4,
        incomeMultiplier5 = incomeMultiplier5,
        incomeMultiplier6 = incomeMultiplier6,
        maxIncomeThreshold2 = maxIncomeThreshold2,
        maxIncomeThreshold3 = maxIncomeThreshold3,
        maxIncomeThreshold4 = maxIncomeThreshold4,
        maxIncomeThreshold5 = maxIncomeThreshold5,
        maxIncomeThreshold6 = maxIncomeThreshold6,
        thresholdIncrease6Plus = thresholdIncrease6Plus,
        siblingDiscount2 = siblingDiscount2,
        siblingDiscount2Plus = siblingDiscount2Plus,
        maxFee = maxFee,
        minFee = minFee
    )
}

data class FeeThresholds(
    val id: UUID,
    val minIncomeThreshold2: Int,
    val minIncomeThreshold3: Int,
    val minIncomeThreshold4: Int,
    val minIncomeThreshold5: Int,
    val minIncomeThreshold6: Int,
    val incomeMultiplier2: BigDecimal,
    val incomeMultiplier3: BigDecimal,
    val incomeMultiplier4: BigDecimal,
    val incomeMultiplier5: BigDecimal,
    val incomeMultiplier6: BigDecimal,
    val maxIncomeThreshold2: Int,
    val maxIncomeThreshold3: Int,
    val maxIncomeThreshold4: Int,
    val maxIncomeThreshold5: Int,
    val maxIncomeThreshold6: Int,
    val thresholdIncrease6Plus: Int,
    val siblingDiscount2: BigDecimal,
    val siblingDiscount2Plus: BigDecimal,
    val maxFee: Int,
    val minFee: Int
) {
    fun incomeMultiplier(familySize: Int): BigDecimal =
        if (familySize <= 1) error("Family size must be greater than 1 (was $familySize)")
        else when (familySize) {
            2 -> incomeMultiplier2
            3 -> incomeMultiplier3
            4 -> incomeMultiplier2
            5 -> incomeMultiplier5
            6 -> incomeMultiplier6
            else -> incomeMultiplier6
        }

    fun minIncomeThreshold(familySize: Int): Int =
        if (familySize <= 1) error("Family size must be greater than 1 (was $familySize)")
        else when (familySize) {
            2 -> minIncomeThreshold2
            3 -> minIncomeThreshold3
            4 -> minIncomeThreshold2
            5 -> minIncomeThreshold5
            6 -> minIncomeThreshold6
            else -> minIncomeThreshold6 + ((familySize - 6) * thresholdIncrease6Plus)
        }

    fun maxIncomeThreshold(familySize: Int): Int =
        if (familySize <= 1) error("Family size must be greater than 1 (was $familySize)")
        else when (familySize) {
            2 -> maxIncomeThreshold2
            3 -> maxIncomeThreshold3
            4 -> maxIncomeThreshold2
            5 -> maxIncomeThreshold5
            6 -> maxIncomeThreshold6
            else -> maxIncomeThreshold6 + ((familySize - 6) * thresholdIncrease6Plus)
        }
}
