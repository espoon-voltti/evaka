// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service.generator

import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.invoicing.createFeeDecisionChildFixture
import fi.espoo.evaka.invoicing.createFeeDecisionFixture
import fi.espoo.evaka.invoicing.domain.ChildWithDateOfBirth
import fi.espoo.evaka.invoicing.domain.DecisionIncome
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionChild
import fi.espoo.evaka.invoicing.domain.FeeDecisionPlacement
import fi.espoo.evaka.invoicing.domain.FeeDecisionServiceNeed
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.FeeDecisionThresholds
import fi.espoo.evaka.invoicing.domain.FeeDecisionType
import fi.espoo.evaka.invoicing.domain.FeeThresholds
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.serviceneed.ServiceNeedOption
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class FeeDecisionGeneratorV2Test {
    @Test
    fun `mergeAdjacentIdenticalDrafts merges if adjacent and identical`() {
        val f1 = feeDecision.copy(validDuring = DateRange(date(1), date(10)))
        val f2 = feeDecision.copy(validDuring = DateRange(date(11), date(20)))
        val f3 = feeDecision.copy(validDuring = DateRange(date(15), date(25)))
        val merged = mergeAdjacentIdenticalDrafts(listOf(f1, f2, f3))
        assertEquals(1, merged.size)
        assertEquals(date(1), merged.first().validFrom)
        assertEquals(date(25), merged.first().validTo)
    }

    @Test
    fun `mergeAdjacentIdenticalDrafts does not merge if not adjacent`() {
        val f1 = feeDecision.copy(validDuring = DateRange(date(1), date(10)))
        val f2 = feeDecision.copy(validDuring = DateRange(date(12), date(20)))
        val merged = mergeAdjacentIdenticalDrafts(listOf(f1, f2))
        assertEquals(listOf(f1, f2), merged)
    }

    @Test
    fun `mergeAdjacentIdenticalDrafts does not merge if not identical`() {
        val f1 = feeDecision.copy(validDuring = DateRange(date(1), date(10)))
        val f2 =
            feeDecision.copy(
                validDuring = DateRange(date(11), date(20)),
                partnerId = PersonId(UUID.randomUUID())
            )
        val merged = mergeAdjacentIdenticalDrafts(listOf(f1, f2))
        assertEquals(listOf(f1, f2), merged)
    }

    @Test
    fun `FeeBasis produces correct decision`() {
        val feeBasis = createFeeBasis()
        val childBasis2 = feeBasis.children[2]
        val decision = feeBasis.toFeeDecision()
        assertEquals(
            decision,
            FeeDecision(
                id = decision.id,
                children =
                    listOf(
                        // child 0 with club and child 1 with voucher value are excluded
                        FeeDecisionChild(
                            child =
                                ChildWithDateOfBirth(
                                    id = childBasis2.child.id,
                                    dateOfBirth = childBasis2.child.dateOfBirth
                                ),
                            placement =
                                FeeDecisionPlacement(
                                    unitId = childBasis2.placement.unitId,
                                    type = childBasis2.placement.placementType
                                ),
                            serviceNeed =
                                FeeDecisionServiceNeed(
                                    optionId = childBasis2.placement.serviceNeedOption.id,
                                    feeCoefficient =
                                        childBasis2.placement.serviceNeedOption.feeCoefficient,
                                    contractDaysPerMonth =
                                        childBasis2.placement.serviceNeedOption
                                            .contractDaysPerMonth,
                                    descriptionFi =
                                        childBasis2.placement.serviceNeedOption.feeDescriptionFi,
                                    descriptionSv =
                                        childBasis2.placement.serviceNeedOption.feeDescriptionSv,
                                    missing = !childBasis2.placement.hasServiceNeed
                                ),
                            baseFee = 28900,
                            siblingDiscount = 80,
                            fee = 5800,
                            feeAlterations = emptyList(),
                            finalFee = 5800,
                            childIncome = null
                        )
                    ),
                headOfFamilyId = feeBasis.headOfFamilyId,
                validDuring = feeBasis.range,
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
                        minFee = feeThresholds.minFee
                    ),
                difference = emptySet(),
                created = decision.created
            )
        )
    }
}

private val feeDecision =
    createFeeDecisionFixture(
        status = FeeDecisionStatus.DRAFT,
        decisionType = FeeDecisionType.NORMAL,
        period = DateRange(date(1), date(31)),
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
                            false
                        )
                )
            ),
        headOfFamilyId = PersonId(UUID.randomUUID())
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
                worksAtECHA = false
            ),
        partnerId = PersonId(UUID.randomUUID()),
        partnerIncome =
            DecisionIncome(
                effect = IncomeEffect.INCOME,
                data = emptyMap(),
                totalIncome = 200000,
                totalExpenses = 0,
                total = 200000,
                worksAtECHA = false
            ),
        children =
            listOf(
                createChildFeeBasis(placementType = PlacementType.CLUB, siblingIndex = 0),
                createChildFeeBasis(
                    placementType = PlacementType.DAYCARE,
                    siblingIndex = 1,
                    invoicedUnit = false
                ),
                createChildFeeBasis(
                    placementType = PlacementType.DAYCARE,
                    siblingIndex = 2,
                    invoicedUnit = true
                )
            ),
        familySize = 5,
        feeThresholds = feeThresholds
    )
}

private fun createChildFeeBasis(
    placementType: PlacementType = PlacementType.DAYCARE,
    invoicedUnit: Boolean = true,
    siblingIndex: Int = 0
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
                providerType = ProviderType.MUNICIPAL,
                invoicedUnit = invoicedUnit,
                hasServiceNeed = true,
                serviceNeedOption = snDaycareFullDay35,
                serviceNeedVoucherValues = null
            ),
        serviceNeedOptionFee = null,
        feeAlterations = emptyList(),
        income = null
    )
}

val snDaycareFullDay35 =
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
        partDay = false,
        partWeek = false,
        feeDescriptionFi = "",
        feeDescriptionSv = "",
        voucherValueDescriptionFi = "Kokopäiväinen, vähintään 35h",
        voucherValueDescriptionSv = "Kokopäiväinen, vähintään 35h",
        active = true
    )

val feeThresholds =
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
        temporaryFeeSiblingPartDay = 800
    )
