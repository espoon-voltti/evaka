// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service.generator

import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.invoicing.domain.ChildWithDateOfBirth
import fi.espoo.evaka.invoicing.domain.DecisionIncome
import fi.espoo.evaka.invoicing.domain.FeeDecisionThresholds
import fi.espoo.evaka.invoicing.domain.FeeThresholds
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.invoicing.domain.VoucherValueDecision
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionPlacement
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionServiceNeed
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionType
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

class VoucherValueDecisionGeneratorV2Test {
    @Test
    fun `VoucherBasis produces correct decision`() {
        val voucherBasis = createVoucherBasis()
        val decision = voucherBasis.toVoucherValueDecision()
        assertEquals(
            decision,
            VoucherValueDecision(
                id = decision.id,
                validFrom = date(10),
                validTo = date(20),
                headOfFamilyId = voucherBasis.headOfFamilyId,
                status = VoucherValueDecisionStatus.DRAFT,
                decisionNumber = null,
                decisionType = VoucherValueDecisionType.NORMAL,
                partnerId = voucherBasis.partnerId,
                headOfFamilyIncome = voucherBasis.headOfFamilyIncome,
                partnerIncome = voucherBasis.partnerIncome,
                childIncome = voucherBasis.childIncome,
                familySize = voucherBasis.familySize,
                feeThresholds =
                    FeeDecisionThresholds(
                        minIncomeThreshold = feeThresholds.minIncomeThreshold5,
                        maxIncomeThreshold = feeThresholds.maxIncomeThreshold5,
                        incomeMultiplier = feeThresholds.incomeMultiplier5,
                        maxFee = feeThresholds.maxFee,
                        minFee = feeThresholds.minFee,
                    ),
                child = voucherBasis.child,
                placement =
                    VoucherValueDecisionPlacement(
                        unitId = voucherBasis.placement!!.unitId,
                        type = voucherBasis.placement!!.placementType,
                    ),
                serviceNeed =
                    VoucherValueDecisionServiceNeed(
                        feeCoefficient = BigDecimal("1.00"),
                        voucherValueCoefficient = BigDecimal("1.00"),
                        feeDescriptionFi = "Kokopäiväinen, vähintään 35h",
                        feeDescriptionSv = "Kokopäiväinen, vähintään 35h",
                        voucherValueDescriptionFi = "Kokopäiväinen, vähintään 35h",
                        voucherValueDescriptionSv = "Kokopäiväinen, vähintään 35h",
                        missing = false,
                    ),
                baseCoPayment = 28900,
                siblingDiscount = 80,
                coPayment = 5800,
                feeAlterations = emptyList(),
                finalCoPayment = 5800,
                baseValue = 87000,
                assistanceNeedCoefficient = voucherBasis.assistanceNeedCoefficient,
                voucherValue = 174000,
                difference = emptySet(),
                created = decision.created,
            ),
        )
    }
}

private fun date(d: Int) = LocalDate.of(2000, 1, d)

private fun createVoucherBasis(): VoucherBasis {
    val childId = ChildId(UUID.randomUUID())
    return VoucherBasis(
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
        child = ChildWithDateOfBirth(id = childId, dateOfBirth = date(1).minusYears(4)),
        placement =
            PlacementDetails(
                childId = childId,
                finiteRange = FiniteDateRange(date(1), date(31)),
                placementType = PlacementType.DAYCARE,
                unitId = DaycareId(UUID.randomUUID()),
                providerType = ProviderType.PRIVATE_SERVICE_VOUCHER,
                invoicedUnit = false,
                hasServiceNeed = true,
                serviceNeedOption = snDaycareFullDay35,
                serviceNeedVoucherValues =
                    ServiceNeedOptionVoucherValueRange(
                        serviceNeedOptionId = snDaycareFullDay35.id,
                        range = DateRange(date(1), null),
                        baseValue = 87000,
                        coefficient = BigDecimal("1.00"),
                        value = 87000,
                        baseValueUnder3y = 134850,
                        coefficientUnder3y = BigDecimal("1.00"),
                        valueUnder3y = 134850,
                    ),
            ),
        assistanceNeedCoefficient = BigDecimal(2.0),
        useUnder3YoCoefficient = false,
        childIncome = null,
        feeAlterations = emptyList(),
        siblingIndex = 2,
        familySize = 5,
        feeThresholds = feeThresholds,
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
        feeDescriptionFi = "Kokopäiväinen, vähintään 35h",
        feeDescriptionSv = "Kokopäiväinen, vähintään 35h",
        voucherValueDescriptionFi = "Kokopäiväinen, vähintään 35h",
        voucherValueDescriptionSv = "Kokopäiväinen, vähintään 35h",
        validFrom = LocalDate.of(2000, 1, 1),
        validTo = null,
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
