// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.domain

import fi.espoo.evaka.invoicing.testFeeAlteration
import fi.espoo.evaka.invoicing.testFeeDecisionIncome
import fi.espoo.evaka.invoicing.testPricing
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class FeeDecisionsTest {
    private fun placement(type: PlacementType, serviceNeed: ServiceNeed) =
        PermanentPlacement(UUID.randomUUID(), type, serviceNeed)

    @Test
    fun `useMaxFee behaviour`() {
        val income1 = testFeeDecisionIncome.copy(data = mapOf(IncomeType.MAIN_INCOME to 2000))
        val income2 = income1.copy(effect = IncomeEffect.MAX_FEE_ACCEPTED)
        val income3 = income1.copy(effect = IncomeEffect.INCOMPLETE)

        assertEquals(false, useMaxFee(listOf(income1, income1)))
        assertEquals(false, useMaxFee(listOf(income1)))
        assertEquals(false, useMaxFee(listOf()))

        assertEquals(true, useMaxFee(listOf(income1, income2)))
        assertEquals(true, useMaxFee(listOf(income1, income3)))
        assertEquals(true, useMaxFee(listOf(income1, null)))
        assertEquals(true, useMaxFee(listOf(income2)))
        assertEquals(true, useMaxFee(listOf(income3)))
        assertEquals(true, useMaxFee(listOf(null)))
    }

    @Test
    fun `calculateBaseFee basic case`() {
        fun twoIncomesOf(amount1: Int, amount2: Int): List<FeeDecisionIncome?> {
            val income1 = testFeeDecisionIncome.copy(data = mapOf(IncomeType.MAIN_INCOME to amount1), total = amount1)
            val income2 = testFeeDecisionIncome.copy(data = mapOf(IncomeType.MAIN_INCOME to amount2), total = amount2)
            return listOf(income1, income2)
        }

        val resultHighIncome = calculateBaseFee(testPricing, 2, twoIncomesOf(300000, 300000))
        assertEquals(28900, resultHighIncome)

        val resultMidIncome = calculateBaseFee(testPricing, 2, twoIncomesOf(150000, 150000))
        assertEquals(9600, resultMidIncome)

        val resultLowIncome = calculateBaseFee(testPricing, 2, twoIncomesOf(100000, 100000))
        assertEquals(0, resultLowIncome)
    }

    @Test
    fun `calculateFeeBeforeFeeAlterations with max service need`() {
        val result = calculateFeeBeforeFeeAlterations(28900, placement(PlacementType.DAYCARE, ServiceNeed.GTE_35), 0)

        assertEquals(28900, result)
    }

    @Test
    fun `calculateFeeBeforeFeeAlterations with max service need and discount 50 %`() {
        val result = calculateFeeBeforeFeeAlterations(28900, placement(PlacementType.DAYCARE, ServiceNeed.GTE_35), 50)

        assertEquals(14500, result)
    }

    @Test
    fun `calculateFeeBeforeFeeAlterations with max service need and discount 80 %`() {
        val result = calculateFeeBeforeFeeAlterations(28900, placement(PlacementType.DAYCARE, ServiceNeed.GTE_35), 80)

        assertEquals(5800, result)
    }

    @Test
    fun `calculateFeeBeforeFeeAlterations with over 25h under 35h service need`() {
        val result =
            calculateFeeBeforeFeeAlterations(28900, placement(PlacementType.DAYCARE, ServiceNeed.GT_25_LT_35), 0)

        assertEquals(23100, result)
    }

    @Test
    fun `calculateFeeBeforeFeeAlterations with over 25h service need`() {
        val result = calculateFeeBeforeFeeAlterations(
            28900,
            placement(PlacementType.PRESCHOOL_WITH_DAYCARE, ServiceNeed.GTE_25),
            0
        )

        assertEquals(23100, result)
    }

    @Test
    fun `calculateFeeBeforeFeeAlterations with over 15h under 25h service need`() {
        val result = calculateFeeBeforeFeeAlterations(
            28900,
            placement(PlacementType.PRESCHOOL_WITH_DAYCARE, ServiceNeed.GT_15_LT_25),
            0
        )

        assertEquals(17300, result)
    }

    @Test
    fun `calculateFeeBeforeFeeAlterations with under 25h service need`() {
        val result = calculateFeeBeforeFeeAlterations(28900, placement(PlacementType.DAYCARE, ServiceNeed.LTE_25), 0)

        assertEquals(17300, result)
    }

    @Test
    fun `calculateFeeBeforeFeeAlterations with under 15h service need`() {
        val result = calculateFeeBeforeFeeAlterations(
            28900,
            placement(PlacementType.PRESCHOOL_WITH_DAYCARE, ServiceNeed.LTE_15),
            0
        )

        assertEquals(10100, result)
    }

    @Test
    fun `calculateFeeBeforeFeeAlterations with no extra service need`() {
        val result = calculateFeeBeforeFeeAlterations(
            28900,
            placement(PlacementType.PRESCHOOL_WITH_DAYCARE, ServiceNeed.LTE_0),
            0
        )

        assertEquals(0, result)
    }

    @Test
    fun `calculateFeeBeforeFeeAlterations rounds down fees below 27`() {
        val result = calculateFeeBeforeFeeAlterations(
            2600,
            placement(PlacementType.DAYCARE, ServiceNeed.GTE_35),
            0
        )

        assertEquals(0, result)
    }

    @Test
    fun `toFeeAlterationsWithEffects with one discount fee alteration`() {
        val feeAlteration = testFeeAlteration.copy(type = FeeAlteration.Type.DISCOUNT, amount = 50, isAbsolute = false)

        val result = toFeeAlterationsWithEffects(10000, listOf(feeAlteration))

        assertEquals(
            listOf(
                FeeAlterationWithEffect(
                    type = feeAlteration.type,
                    amount = feeAlteration.amount,
                    isAbsolute = feeAlteration.isAbsolute,
                    effect = -5000
                )
            ),
            result
        )
    }

    @Test
    fun `toFeeAlterationsWithEffects with one increase fee alteration`() {
        val feeAlteration = testFeeAlteration.copy(type = FeeAlteration.Type.INCREASE, amount = 50, isAbsolute = false)

        val result = toFeeAlterationsWithEffects(10000, listOf(feeAlteration))

        assertEquals(
            listOf(
                FeeAlterationWithEffect(
                    type = feeAlteration.type,
                    amount = feeAlteration.amount,
                    isAbsolute = feeAlteration.isAbsolute,
                    effect = 5000
                )
            ),
            result
        )
    }

    @Test
    fun `toFeeAlterationsWithEffects with one absolute discount fee alteration`() {
        val feeAlteration = testFeeAlteration.copy(type = FeeAlteration.Type.DISCOUNT, amount = 30, isAbsolute = true)

        val result = toFeeAlterationsWithEffects(10000, listOf(feeAlteration))

        assertEquals(
            listOf(
                FeeAlterationWithEffect(
                    type = feeAlteration.type,
                    amount = feeAlteration.amount,
                    isAbsolute = feeAlteration.isAbsolute,
                    effect = -3000
                )
            ),
            result
        )
    }

    @Test
    fun `toFeeAlterationsWithEffects with one fee alteration with rounding`() {
        val feeAlteration = testFeeAlteration.copy(type = FeeAlteration.Type.DISCOUNT, amount = 50, isAbsolute = false)

        val result = toFeeAlterationsWithEffects(28900, listOf(feeAlteration))

        assertEquals(
            listOf(
                FeeAlterationWithEffect(
                    type = feeAlteration.type,
                    amount = feeAlteration.amount,
                    isAbsolute = feeAlteration.isAbsolute,
                    effect = -14450
                )
            ),
            result
        )
    }

    @Test
    fun `toFeeAlterationsWithEffects with multiple fee alterations`() {
        val feeAlterations = listOf(
            testFeeAlteration.copy(type = FeeAlteration.Type.DISCOUNT, amount = 50, isAbsolute = false),
            testFeeAlteration.copy(type = FeeAlteration.Type.INCREASE, amount = 20, isAbsolute = true),
            testFeeAlteration.copy(type = FeeAlteration.Type.RELIEF, amount = 50, isAbsolute = false),
            testFeeAlteration.copy(type = FeeAlteration.Type.INCREASE, amount = 25, isAbsolute = false)
        )

        val result = toFeeAlterationsWithEffects(28900, feeAlterations)

        assertEquals(
            listOf(
                FeeAlterationWithEffect(
                    type = feeAlterations[0].type,
                    amount = feeAlterations[0].amount,
                    isAbsolute = feeAlterations[0].isAbsolute,
                    effect = -14450
                ),
                FeeAlterationWithEffect(
                    type = feeAlterations[1].type,
                    amount = feeAlterations[1].amount,
                    isAbsolute = feeAlterations[1].isAbsolute,
                    effect = 2000
                ),
                FeeAlterationWithEffect(
                    type = feeAlterations[2].type,
                    amount = feeAlterations[2].amount,
                    isAbsolute = feeAlterations[2].isAbsolute,
                    effect = -8225
                ),
                FeeAlterationWithEffect(
                    type = feeAlterations[3].type,
                    amount = feeAlterations[3].amount,
                    isAbsolute = feeAlterations[3].isAbsolute,
                    effect = 2056
                )
            ),
            result
        )
    }

    @Test
    fun `toFeeAlterationsWithEffects with one absolute discount and zero base fee`() {
        val feeAlteration = testFeeAlteration.copy(type = FeeAlteration.Type.DISCOUNT, amount = 50, isAbsolute = true)

        val result = toFeeAlterationsWithEffects(0, listOf(feeAlteration))

        assertEquals(
            listOf(
                FeeAlterationWithEffect(
                    type = feeAlteration.type,
                    amount = feeAlteration.amount,
                    isAbsolute = feeAlteration.isAbsolute,
                    effect = 0
                )
            ),
            result
        )
    }
}
