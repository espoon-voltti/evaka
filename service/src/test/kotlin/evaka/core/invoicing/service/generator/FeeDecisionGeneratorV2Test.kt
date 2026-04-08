// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.invoicing.service.generator

import evaka.core.daycare.domain.ProviderType
import evaka.core.invoicing.createFeeDecisionChildFixture
import evaka.core.invoicing.createFeeDecisionFixture
import evaka.core.invoicing.domain.ChildWithDateOfBirth
import evaka.core.invoicing.domain.DecisionIncome
import evaka.core.invoicing.domain.FeeDecision
import evaka.core.invoicing.domain.FeeDecisionChild
import evaka.core.invoicing.domain.FeeDecisionPlacement
import evaka.core.invoicing.domain.FeeDecisionServiceNeed
import evaka.core.invoicing.domain.FeeDecisionStatus
import evaka.core.invoicing.domain.FeeDecisionThresholds
import evaka.core.invoicing.domain.FeeDecisionType
import evaka.core.invoicing.domain.FeeThresholds
import evaka.core.invoicing.domain.IncomeEffect
import evaka.core.placement.PlacementType
import evaka.core.serviceneed.ServiceNeedOption
import evaka.core.shared.ChildId
import evaka.core.shared.DaycareId
import evaka.core.shared.PersonId
import evaka.core.shared.ServiceNeedOptionId
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.FiniteDateRange
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class FeeDecisionGeneratorV2Test {
    @Test
    fun `mergeAdjacentIdenticalDrafts merges if adjacent and identical`() {
        val f1 = feeDecision.copy(validDuring = FiniteDateRange(date(1), date(10)))
        val f2 = feeDecision.copy(validDuring = FiniteDateRange(date(11), date(20)))
        val f3 = feeDecision.copy(validDuring = FiniteDateRange(date(15), date(25)))
        val merged = mergeAdjacentIdenticalDrafts(listOf(f1, f2, f3))
        assertEquals(1, merged.size)
        assertEquals(date(1), merged.first().validFrom)
        assertEquals(date(25), merged.first().validTo)
    }

    @Test
    fun `mergeAdjacentIdenticalDrafts does not merge if not adjacent`() {
        val f1 = feeDecision.copy(validDuring = FiniteDateRange(date(1), date(10)))
        val f2 = feeDecision.copy(validDuring = FiniteDateRange(date(12), date(20)))
        val merged = mergeAdjacentIdenticalDrafts(listOf(f1, f2))
        assertEquals(listOf(f1, f2), merged)
    }

    @Test
    fun `mergeAdjacentIdenticalDrafts does not merge if not identical`() {
        val f1 = feeDecision.copy(validDuring = FiniteDateRange(date(1), date(10)))
        val f2 =
            feeDecision.copy(
                validDuring = FiniteDateRange(date(11), date(20)),
                partnerId = PersonId(UUID.randomUUID()),
            )
        val merged = mergeAdjacentIdenticalDrafts(listOf(f1, f2))
        assertEquals(listOf(f1, f2), merged)
    }

    @Test
    fun `FeeBasis produces correct decision`() {
        val feeBasis = createFeeBasis()
        val childBasis = feeBasis.children[2]
        val decision = feeBasis.toFeeDecision()!!
        assertEquals(
            decision,
            FeeDecision(
                id = decision.id,
                children =
                    listOf(
                        // child 0 with preschool placement and child 1 with voucher value
                        // affect sibling discount but are not invoiced
                        FeeDecisionChild(
                            child =
                                ChildWithDateOfBirth(
                                    id = childBasis.child.id,
                                    dateOfBirth = childBasis.child.dateOfBirth,
                                ),
                            placement =
                                FeeDecisionPlacement(
                                    unitId = childBasis.placement.unitId,
                                    type = childBasis.placement.placementType,
                                ),
                            serviceNeed =
                                FeeDecisionServiceNeed(
                                    optionId = childBasis.placement.serviceNeedOption.id,
                                    feeCoefficient =
                                        childBasis.placement.serviceNeedOption.feeCoefficient,
                                    contractDaysPerMonth =
                                        childBasis.placement.serviceNeedOption.contractDaysPerMonth,
                                    descriptionFi =
                                        childBasis.placement.serviceNeedOption.feeDescriptionFi,
                                    descriptionSv =
                                        childBasis.placement.serviceNeedOption.feeDescriptionSv,
                                    missing = !childBasis.placement.hasServiceNeed,
                                ),
                            baseFee = 28900,
                            siblingDiscount = 80,
                            fee = 5800,
                            feeAlterations = emptyList(),
                            finalFee = 5800,
                            childIncome = null,
                        )
                    ),
                headOfFamilyId = feeBasis.headOfFamilyId,
                validDuring = feeBasis.range.asFiniteDateRange()!!,
                status = FeeDecisionStatus.DRAFT,
                decisionNumber = null,
                decisionType = FeeDecisionType.NORMAL,
                partnerId = feeBasis.partnerId,
                headOfFamilyIncome = feeBasis.headOfFamilyIncome,
                partnerIncome = feeBasis.partnerIncome,
                familySize = 5,
                feeThresholds =
                    FeeDecisionThresholds(
                        minIncomeThreshold = feeThresholds.minIncomeThreshold5,
                        maxIncomeThreshold = feeThresholds.maxIncomeThreshold5,
                        incomeMultiplier = feeThresholds.incomeMultiplier5,
                        maxFee = feeThresholds.maxFee,
                        minFee = feeThresholds.minFee,
                    ),
                difference = emptySet(),
                created = decision.created,
            ),
        )
    }
}

private val feeDecision =
    createFeeDecisionFixture(
        status = FeeDecisionStatus.DRAFT,
        decisionType = FeeDecisionType.NORMAL,
        period = FiniteDateRange(date(1), date(31)),
        children =
            listOf(
                createFeeDecisionChildFixture(
                    childId = ChildId(UUID.randomUUID()),
                    dateOfBirth = date(1).minusYears(3),
                    placementUnitId = DaycareId(UUID.randomUUID()),
                    placementType = PlacementType.DAYCARE,
                    serviceNeed =
                        FeeDecisionServiceNeed(
                            ServiceNeedOptionId(UUID.randomUUID()),
                            BigDecimal.valueOf(0.5),
                            null,
                            "foo",
                            "foo",
                            false,
                        ),
                )
            ),
        headOfFamilyId = PersonId(UUID.randomUUID()),
    )

private fun date(d: Int) = LocalDate.of(2000, 1, d)

private fun createFeeBasis(): FeeBasis {
    return FeeBasis(
        range = DateRange(date(10), date(20)),
        headOfFamilyId = PersonId(UUID.randomUUID()),
        headOfFamilyIncome =
            DecisionIncome(
                effect = IncomeEffect.INCOME,
                data = emptyMap(),
                totalIncome = 400000,
                totalExpenses = 100000,
                total = 500000,
                worksAtECHA = false,
            ),
        partnerId = PersonId(UUID.randomUUID()),
        partnerIncome =
            DecisionIncome(
                effect = IncomeEffect.INCOME,
                data = emptyMap(),
                totalIncome = 200000,
                totalExpenses = 0,
                total = 200000,
                worksAtECHA = false,
            ),
        children =
            listOf(
                createChildFeeBasis(
                    placementType = PlacementType.DAYCARE,
                    siblingIndex = 0,
                    invoicedUnit = false,
                    providerType = ProviderType.PRIVATE_SERVICE_VOUCHER,
                ),
                createChildFeeBasis(
                    placementType = PlacementType.PRESCHOOL,
                    siblingIndex = 1,
                    invoicedUnit = false,
                    providerType = ProviderType.MUNICIPAL,
                ),
                createChildFeeBasis(
                    placementType = PlacementType.DAYCARE,
                    siblingIndex = 2,
                    invoicedUnit = true,
                    providerType = ProviderType.MUNICIPAL,
                ),
            ),
        familySize = 5,
        feeThresholds = feeThresholds,
    )
}

private fun createChildFeeBasis(
    placementType: PlacementType,
    providerType: ProviderType,
    invoicedUnit: Boolean = true,
    siblingIndex: Int = 0,
): ChildFeeBasis {
    val id = PersonId(UUID.randomUUID())
    return ChildFeeBasis(
        child = Child(id = id, dateOfBirth = date(1).minusYears(4), ssn = null),
        siblingIndex = siblingIndex,
        placement =
            PlacementDetails(
                childId = id,
                finiteRange = FiniteDateRange(date(1), date(31)),
                placementType = placementType,
                unitId = DaycareId(UUID.randomUUID()),
                providerType = providerType,
                invoicedUnit = invoicedUnit,
                hasServiceNeed = true,
                serviceNeedOption = snDaycareFullDay35,
                serviceNeedVoucherValues = null,
            ),
        serviceNeedOptionFee = null,
        feeAlterations = emptyList(),
        income = null,
    )
}

private val snDaycareFullDay35 =
    ServiceNeedOption(
        id = ServiceNeedOptionId(UUID.randomUUID()),
        nameFi = "Kokopäiväinen, vähintään 35h",
        nameSv = "Kokopäiväinen, vähintään 35h",
        nameEn = "Kokopäiväinen, vähintään 35h",
        validPlacementType = PlacementType.DAYCARE,
        defaultOption = false,
        feeCoefficient = BigDecimal("1.00"),
        occupancyCoefficient = BigDecimal("1.00"),
        occupancyCoefficientUnder3y = BigDecimal("1.75"),
        realizedOccupancyCoefficient = BigDecimal("1.00"),
        realizedOccupancyCoefficientUnder3y = BigDecimal("1.75"),
        daycareHoursPerWeek = 35,
        contractDaysPerMonth = null,
        daycareHoursPerMonth = null,
        partDay = false,
        partWeek = false,
        feeDescriptionFi = "",
        feeDescriptionSv = "",
        voucherValueDescriptionFi = "Kokopäiväinen, vähintään 35h",
        voucherValueDescriptionSv = "Kokopäiväinen, vähintään 35h",
        validFrom = LocalDate.of(2000, 1, 1),
        validTo = null,
        showForCitizen = true,
    )

private val feeThresholds =
    FeeThresholds(
        validDuring = DateRange(LocalDate.of(2000, 1, 1), null),
        minIncomeThreshold2 = 210200,
        minIncomeThreshold3 = 271300,
        minIncomeThreshold4 = 308000,
        minIncomeThreshold5 = 344700,
        minIncomeThreshold6 = 381300,
        maxIncomeThreshold2 = 479900,
        maxIncomeThreshold3 = 541000,
        maxIncomeThreshold4 = 577700,
        maxIncomeThreshold5 = 614400,
        maxIncomeThreshold6 = 651000,
        incomeMultiplier2 = BigDecimal("0.1070"),
        incomeMultiplier3 = BigDecimal("0.1070"),
        incomeMultiplier4 = BigDecimal("0.1070"),
        incomeMultiplier5 = BigDecimal("0.1070"),
        incomeMultiplier6 = BigDecimal("0.1070"),
        incomeThresholdIncrease6Plus = 14200,
        siblingDiscount2 = BigDecimal("0.5"),
        siblingDiscount2Plus = BigDecimal("0.8"),
        maxFee = 28900,
        minFee = 2700,
        temporaryFee = 2900,
        temporaryFeePartDay = 1500,
        temporaryFeeSibling = 1500,
        temporaryFeeSiblingPartDay = 800,
    )
