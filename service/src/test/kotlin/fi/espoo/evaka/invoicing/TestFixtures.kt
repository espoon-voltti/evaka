// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing

import fi.espoo.evaka.invoicing.domain.DecisionIncome
import fi.espoo.evaka.invoicing.domain.FeeAlteration
import fi.espoo.evaka.invoicing.domain.FeeAlterationWithEffect
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionChild
import fi.espoo.evaka.invoicing.domain.FeeDecisionPlacement
import fi.espoo.evaka.invoicing.domain.FeeDecisionServiceNeed
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.FeeDecisionThresholds
import fi.espoo.evaka.invoicing.domain.FeeDecisionType
import fi.espoo.evaka.invoicing.domain.FeeThresholds
import fi.espoo.evaka.invoicing.domain.Income
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.invoicing.domain.IncomeType
import fi.espoo.evaka.invoicing.domain.Invoice
import fi.espoo.evaka.invoicing.domain.InvoiceRow
import fi.espoo.evaka.invoicing.domain.InvoiceStatus
import fi.espoo.evaka.invoicing.domain.PersonData
import fi.espoo.evaka.invoicing.domain.Product
import fi.espoo.evaka.invoicing.domain.UnitData
import fi.espoo.evaka.invoicing.domain.VoucherValueDecision
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionPlacement
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionServiceNeed
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.domain.DateRange
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

val uuid1 = UUID.fromString("124c0bcf-df0d-4a89-86ff-8f971123f87d")
val uuid3 = UUID.fromString("124c0bcf-df0d-4a89-86ff-8f9711622233")
val uuid4 = UUID.fromString("603e5943-29b5-48de-8095-01b4ef511122")
val uuid5 = UUID.fromString("1b533dbb-9871-4406-b48e-735153d8f36c")

val testDecisionFrom = LocalDate.of(2019, 5, 1)
val testDecisionTo = LocalDate.of(2019, 5, 31)

val testChild1 = PersonData.WithDateOfBirth(uuid4, LocalDate.of(2016, 1, 1))

val testChild2 = testChild1.copy(id = uuid5, dateOfBirth = testChild1.dateOfBirth.plusDays(1))

val oldTestFeeThresholds = FeeThresholds(
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
    siblingDiscount2Plus = BigDecimal("0.2")
)

val testFeeThresholds = FeeThresholds(
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
    siblingDiscount2Plus = BigDecimal("0.8000")
)

val testDecisionChild1 =
    FeeDecisionChild(
        child = testChild1,
        placement = FeeDecisionPlacement(UnitData.JustId(UUID.randomUUID()), PlacementType.DAYCARE),
        serviceNeed = FeeDecisionServiceNeed(BigDecimal("1.00"), "palveluntarve", "vårdbehövet", false),
        baseFee = 28900,
        siblingDiscount = 0,
        fee = 28900,
        feeAlterations = listOf(),
        finalFee = 28900
    )
val testDecisionChild2 =
    FeeDecisionChild(
        child = testChild2,
        placement = FeeDecisionPlacement(UnitData.JustId(UUID.randomUUID()), PlacementType.DAYCARE),
        serviceNeed = FeeDecisionServiceNeed(BigDecimal("1.00"), "palveluntarve", "vårdbehövet", false),
        baseFee = 28900,
        siblingDiscount = 0,
        fee = 28900,
        feeAlterations = listOf(),
        finalFee = 28900
    )

val testDecision1 = FeeDecision(
    id = uuid1,
    status = FeeDecisionStatus.DRAFT,
    decisionNumber = 1010101010L,
    decisionType = FeeDecisionType.NORMAL,
    validDuring = DateRange(testDecisionFrom, testDecisionTo),
    headOfFamily = PersonData.JustId(uuid3),
    partner = null,
    headOfFamilyIncome = null,
    partnerIncome = null,
    familySize = 3,
    feeThresholds = testFeeThresholds.getFeeDecisionThresholds(3),
    children = listOf(testDecisionChild1, testDecisionChild2.copy(siblingDiscount = 50, fee = 14500, finalFee = 14500)),
    created = Instant.now()
)

val testInvoiceRow = InvoiceRow(
    id = UUID.randomUUID(),
    amount = 1,
    child = PersonData.WithDateOfBirth(testChild2.id, testChild2.dateOfBirth),
    periodStart = LocalDate.of(2019, 5, 1),
    periodEnd = LocalDate.of(2019, 5, 31),
    unitPrice = 28900,
    costCenter = "31450",
    subCostCenter = "01",
    product = Product.DAYCARE
)

val testInvoice = Invoice(
    id = uuid1,
    status = InvoiceStatus.DRAFT,
    periodStart = LocalDate.of(2019, 5, 1),
    periodEnd = LocalDate.of(2019, 5, 31),
    agreementType = 100,
    headOfFamily = testDecision1.headOfFamily,
    rows = listOf(testInvoiceRow)
)

val testPisFridgeParentId = UUID.randomUUID()

val testIncome = Income(
    id = UUID.randomUUID(),
    personId = UUID.randomUUID(),
    validFrom = LocalDate.of(2000, 1, 1),
    validTo = null,
    effect = IncomeEffect.INCOME,
    data = mapOf(),
    notes = ""
)

val testDecisionIncome = DecisionIncome(
    effect = IncomeEffect.INCOME,
    data = mapOf(IncomeType.MAIN_INCOME to 314100),
    total = 314100,
    validFrom = LocalDate.of(2000, 1, 1),
    validTo = null
)

fun createFeeDecisionAlterationFixture(
    type: FeeAlteration.Type = FeeAlteration.Type.DISCOUNT,
    amount: Int = 100,
    isAbsolute: Boolean = false,
    effect: Int = 10000
) =
    FeeAlterationWithEffect(type, amount, isAbsolute, effect)

fun createFeeDecisionChildFixture(
    childId: UUID,
    dateOfBirth: LocalDate,
    placementUnitId: UUID,
    placementType: PlacementType,
    serviceNeed: FeeDecisionServiceNeed,
    baseFee: Int = 28900,
    siblingDiscount: Int = 0,
    fee: Int = 28900,
    feeAlterations: List<FeeAlterationWithEffect> = listOf(),
) = FeeDecisionChild(
    child = PersonData.WithDateOfBirth(id = childId, dateOfBirth = dateOfBirth),
    placement = FeeDecisionPlacement(UnitData.JustId(placementUnitId), placementType),
    serviceNeed = serviceNeed,
    baseFee = baseFee,
    siblingDiscount = siblingDiscount,
    fee = fee,
    feeAlterations = feeAlterations,
    finalFee = fee + feeAlterations.sumOf { it.effect }
)

fun createFeeDecisionFixture(
    status: FeeDecisionStatus,
    decisionType: FeeDecisionType,
    period: DateRange,
    headOfFamilyId: UUID,
    children: List<FeeDecisionChild>,
    feeThresholds: FeeDecisionThresholds = testFeeThresholds.getFeeDecisionThresholds(children.size + 1),
    headOfFamilyIncome: DecisionIncome? = null
) = FeeDecision(
    id = UUID.randomUUID(),
    status = status,
    decisionType = decisionType,
    validDuring = period,
    headOfFamily = PersonData.JustId(headOfFamilyId),
    partner = null,
    headOfFamilyIncome = headOfFamilyIncome,
    partnerIncome = null,
    familySize = children.size + 1,
    feeThresholds = feeThresholds,
    children = children
)

fun createVoucherValueDecisionFixture(
    status: VoucherValueDecisionStatus,
    validFrom: LocalDate,
    validTo: LocalDate?,
    headOfFamilyId: UUID,
    childId: UUID,
    dateOfBirth: LocalDate,
    unitId: UUID,
    familySize: Int = 2,
    placementType: PlacementType,
    serviceNeed: VoucherValueDecisionServiceNeed,
    baseValue: Int = 87000,
    ageCoefficient: BigDecimal = BigDecimal("1.00"),
    value: Int = 87000,
    baseCoPayment: Int = 28900,
    siblingDiscount: Int = 0,
    coPayment: Int = 28900,
    feeAlterations: List<FeeAlterationWithEffect> = listOf()
) = VoucherValueDecision(
    id = UUID.randomUUID(),
    status = status,
    validFrom = validFrom,
    validTo = validTo,
    headOfFamily = PersonData.JustId(headOfFamilyId),
    partner = null,
    headOfFamilyIncome = null,
    partnerIncome = null,
    familySize = familySize,
    feeThresholds = testFeeThresholds.getFeeDecisionThresholds(familySize),
    child = PersonData.WithDateOfBirth(id = childId, dateOfBirth = dateOfBirth),
    placement = VoucherValueDecisionPlacement(UnitData.JustId(unitId), placementType),
    serviceNeed = serviceNeed,
    baseValue = baseValue,
    ageCoefficient = ageCoefficient,
    voucherValue = value,
    baseCoPayment = baseCoPayment,
    siblingDiscount = siblingDiscount,
    coPayment = coPayment,
    feeAlterations = feeAlterations,
    finalCoPayment = coPayment + feeAlterations.sumOf { it.effect }
)

fun createInvoiceRowFixture(childId: UUID) = InvoiceRow(
    id = UUID.randomUUID(),
    child = PersonData.WithDateOfBirth(childId, LocalDate.of(2017, 1, 1)),
    amount = 1,
    unitPrice = 28900,
    product = Product.DAYCARE,
    costCenter = "200",
    subCostCenter = "09",
    periodStart = LocalDate.of(2019, 1, 1),
    periodEnd = LocalDate.of(2019, 1, 31)
)

fun createInvoiceFixture(
    status: InvoiceStatus,
    headOfFamilyId: UUID,
    agreementType: Int,
    number: Long? = null,
    period: DateRange = DateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31)),
    rows: List<InvoiceRow>
) = Invoice(
    id = UUID.randomUUID(),
    status = status,
    number = number,
    agreementType = agreementType,
    headOfFamily = PersonData.JustId(headOfFamilyId),
    periodStart = period.start,
    periodEnd = period.end!!,
    rows = rows
)
