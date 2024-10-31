// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing

import fi.espoo.evaka.invoicing.domain.ChildWithDateOfBirth
import fi.espoo.evaka.invoicing.domain.DecisionIncome
import fi.espoo.evaka.invoicing.domain.FeeAlterationType
import fi.espoo.evaka.invoicing.domain.FeeAlterationWithEffect
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionChild
import fi.espoo.evaka.invoicing.domain.FeeDecisionPlacement
import fi.espoo.evaka.invoicing.domain.FeeDecisionServiceNeed
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.FeeDecisionThresholds
import fi.espoo.evaka.invoicing.domain.FeeDecisionType
import fi.espoo.evaka.invoicing.domain.FeeThresholds
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.invoicing.domain.VoucherValueDecision
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionPlacement
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionServiceNeed
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionType
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.FeeDecisionId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.VoucherValueDecisionId
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

val uuid1 = UUID.fromString("124c0bcf-df0d-4a89-86ff-8f971123f87d")
val uuid3 = UUID.fromString("124c0bcf-df0d-4a89-86ff-8f9711622233")
val uuid4 = UUID.fromString("603e5943-29b5-48de-8095-01b4ef511122")
val uuid5 = UUID.fromString("1b533dbb-9871-4406-b48e-735153d8f36c")

val testDecisionFrom = LocalDate.of(2019, 5, 1)
val testDecisionTo = LocalDate.of(2019, 5, 31)

val testChild1 = ChildWithDateOfBirth(PersonId(uuid4), LocalDate.of(2016, 1, 1))

val testChild2 = ChildWithDateOfBirth(PersonId(uuid5), testChild1.dateOfBirth.plusDays(1))

val oldTestFeeThresholds =
    FeeThresholds(
        validDuring = DateRange(LocalDate.of(2000, 1, 1), null),
        maxFee = 250000,
        minFee = 27000,
        minIncomeThreshold2 = 200000,
        minIncomeThreshold3 = 250000,
        minIncomeThreshold4 = 300000,
        minIncomeThreshold5 = 400000,
        minIncomeThreshold6 = 381300,
        maxIncomeThreshold2 = 50000,
        maxIncomeThreshold3 = 544300,
        maxIncomeThreshold4 = 581600,
        maxIncomeThreshold6 = 381300,
        maxIncomeThreshold5 = 344700,
        incomeMultiplier2 = BigDecimal("0.10"),
        incomeMultiplier3 = BigDecimal("0.10"),
        incomeMultiplier4 = BigDecimal("0.10"),
        incomeMultiplier5 = BigDecimal("0.10"),
        incomeMultiplier6 = BigDecimal("0.10"),
        incomeThresholdIncrease6Plus = 50000,
        siblingDiscount2 = BigDecimal("0.5"),
        siblingDiscount2Plus = BigDecimal("0.2"),
        temporaryFee = 2900,
        temporaryFeePartDay = 1500,
        temporaryFeeSibling = 1500,
        temporaryFeeSiblingPartDay = 800,
    )

val testFeeThresholds =
    FeeThresholds(
        validDuring = DateRange(LocalDate.of(2000, 1, 1), null),
        maxFee = 28900,
        minFee = 2700,
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
        siblingDiscount2 = BigDecimal("0.5000"),
        siblingDiscount2Plus = BigDecimal("0.8000"),
        temporaryFee = 2900,
        temporaryFeePartDay = 1500,
        temporaryFeeSibling = 1500,
        temporaryFeeSiblingPartDay = 800,
    )

val testDecisionChild1 =
    FeeDecisionChild(
        child = testChild1,
        placement = FeeDecisionPlacement(DaycareId(UUID.randomUUID()), PlacementType.DAYCARE),
        serviceNeed =
            FeeDecisionServiceNeed(
                ServiceNeedOptionId(UUID.randomUUID()),
                BigDecimal("1.00"),
                null,
                "palveluntarve",
                "vårdbehövet",
                false,
            ),
        baseFee = 28900,
        siblingDiscount = 0,
        fee = 28900,
        feeAlterations =
            listOf(FeeAlterationWithEffect(FeeAlterationType.RELIEF, 50, false, -10800)),
        finalFee = 28900,
        childIncome = null,
    )
val testDecisionChild2 =
    FeeDecisionChild(
        child = testChild2,
        placement = FeeDecisionPlacement(DaycareId(UUID.randomUUID()), PlacementType.DAYCARE),
        serviceNeed =
            FeeDecisionServiceNeed(
                ServiceNeedOptionId(UUID.randomUUID()),
                BigDecimal("1.00"),
                null,
                "palveluntarve",
                "vårdbehövet",
                false,
            ),
        baseFee = 28900,
        siblingDiscount = 0,
        fee = 28900,
        feeAlterations =
            listOf(FeeAlterationWithEffect(FeeAlterationType.RELIEF, 50, false, -10800)),
        finalFee = 28900,
        childIncome = null,
    )

val testDecision1 =
    FeeDecision(
        id = FeeDecisionId(uuid1),
        status = FeeDecisionStatus.DRAFT,
        decisionNumber = 1010101010L,
        decisionType = FeeDecisionType.NORMAL,
        validDuring = FiniteDateRange(testDecisionFrom, testDecisionTo),
        headOfFamilyId = PersonId(uuid3),
        partnerId = null,
        headOfFamilyIncome = null,
        partnerIncome = null,
        familySize = 3,
        feeThresholds = testFeeThresholds.getFeeDecisionThresholds(3),
        children =
            listOf(
                testDecisionChild1,
                testDecisionChild2.copy(siblingDiscount = 50, fee = 14500, finalFee = 14500),
            ),
        created = HelsinkiDateTime.now(),
        difference = emptySet(),
    )

val testDecisionIncome =
    DecisionIncome(
        effect = IncomeEffect.INCOME,
        data = mapOf("MAIN_INCOME" to 314100),
        totalIncome = 314100,
        totalExpenses = 0,
        total = 314100,
        worksAtECHA = false,
    )

fun createFeeDecisionAlterationFixture(
    type: FeeAlterationType = FeeAlterationType.DISCOUNT,
    amount: Int = 100,
    isAbsolute: Boolean = false,
    effect: Int = 10000,
) = FeeAlterationWithEffect(type, amount, isAbsolute, effect)

fun createFeeDecisionChildFixture(
    childId: ChildId,
    dateOfBirth: LocalDate,
    placementUnitId: DaycareId,
    placementType: PlacementType,
    serviceNeed: FeeDecisionServiceNeed,
    baseFee: Int = 28900,
    siblingDiscount: Int = 0,
    fee: Int = 28900,
    feeAlterations: List<FeeAlterationWithEffect> = listOf(),
) =
    FeeDecisionChild(
        child = ChildWithDateOfBirth(id = childId, dateOfBirth = dateOfBirth),
        placement = FeeDecisionPlacement(placementUnitId, placementType),
        serviceNeed = serviceNeed,
        baseFee = baseFee,
        siblingDiscount = siblingDiscount,
        fee = fee,
        feeAlterations = feeAlterations,
        finalFee = fee + feeAlterations.sumOf { it.effect },
        childIncome = null,
    )

fun createFeeDecisionFixture(
    status: FeeDecisionStatus,
    decisionType: FeeDecisionType,
    period: FiniteDateRange,
    headOfFamilyId: PersonId,
    children: List<FeeDecisionChild>,
    partnerId: PersonId? = null,
    feeThresholds: FeeDecisionThresholds =
        testFeeThresholds.getFeeDecisionThresholds(children.size + 1),
    headOfFamilyIncome: DecisionIncome? = null,
    partnerIncome: DecisionIncome? = null,
    familySize: Int = children.size + 1 + if (partnerId != null) 1 else 0,
    created: HelsinkiDateTime = HelsinkiDateTime.now(),
) =
    FeeDecision(
        id = FeeDecisionId(UUID.randomUUID()),
        status = status,
        decisionType = decisionType,
        validDuring = period,
        headOfFamilyId = headOfFamilyId,
        partnerId = partnerId,
        headOfFamilyIncome = headOfFamilyIncome,
        partnerIncome = partnerIncome,
        familySize = familySize,
        feeThresholds = feeThresholds,
        children = children,
        difference = emptySet(),
        created = created,
    )

fun createVoucherValueDecisionFixture(
    status: VoucherValueDecisionStatus,
    validFrom: LocalDate,
    validTo: LocalDate,
    headOfFamilyId: PersonId,
    childId: ChildId,
    dateOfBirth: LocalDate,
    unitId: DaycareId,
    familySize: Int = 2,
    placementType: PlacementType,
    serviceNeed: VoucherValueDecisionServiceNeed,
    baseValue: Int = 87000,
    assistanceNeedCoefficient: BigDecimal = BigDecimal("1.00"),
    value: Int = 87000,
    baseCoPayment: Int = 28900,
    siblingDiscount: Int = 0,
    coPayment: Int = 28900,
    feeAlterations: List<FeeAlterationWithEffect> = listOf(),
) =
    VoucherValueDecision(
        id = VoucherValueDecisionId(UUID.randomUUID()),
        status = status,
        decisionType = VoucherValueDecisionType.NORMAL,
        validFrom = validFrom,
        validTo = validTo,
        headOfFamilyId = headOfFamilyId,
        partnerId = null,
        headOfFamilyIncome = null,
        partnerIncome = null,
        childIncome = null,
        familySize = familySize,
        feeThresholds = testFeeThresholds.getFeeDecisionThresholds(familySize),
        child = ChildWithDateOfBirth(id = childId, dateOfBirth = dateOfBirth),
        placement = VoucherValueDecisionPlacement(unitId, placementType),
        serviceNeed = serviceNeed,
        baseValue = baseValue,
        assistanceNeedCoefficient = assistanceNeedCoefficient,
        voucherValue = value,
        baseCoPayment = baseCoPayment,
        siblingDiscount = siblingDiscount,
        coPayment = coPayment,
        feeAlterations = feeAlterations,
        finalCoPayment = coPayment + feeAlterations.sumOf { it.effect },
        difference = emptySet(),
    )
