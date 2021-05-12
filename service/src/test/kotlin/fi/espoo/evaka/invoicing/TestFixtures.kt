// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing

import fi.espoo.evaka.invoicing.domain.DecisionIncome
import fi.espoo.evaka.invoicing.domain.FeeAlteration
import fi.espoo.evaka.invoicing.domain.FeeAlterationWithEffect
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecision2
import fi.espoo.evaka.invoicing.domain.FeeDecisionChild
import fi.espoo.evaka.invoicing.domain.FeeDecisionPart
import fi.espoo.evaka.invoicing.domain.FeeDecisionPlacement
import fi.espoo.evaka.invoicing.domain.FeeDecisionServiceNeed
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.FeeDecisionType
import fi.espoo.evaka.invoicing.domain.FridgeFamily
import fi.espoo.evaka.invoicing.domain.Income
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.invoicing.domain.IncomeType
import fi.espoo.evaka.invoicing.domain.Invoice
import fi.espoo.evaka.invoicing.domain.InvoiceRow
import fi.espoo.evaka.invoicing.domain.InvoiceStatus
import fi.espoo.evaka.invoicing.domain.PermanentPlacement
import fi.espoo.evaka.invoicing.domain.PersonData
import fi.espoo.evaka.invoicing.domain.PlacementType
import fi.espoo.evaka.invoicing.domain.Pricing
import fi.espoo.evaka.invoicing.domain.Product
import fi.espoo.evaka.invoicing.domain.ServiceNeed
import fi.espoo.evaka.invoicing.domain.UnitData
import fi.espoo.evaka.invoicing.domain.VoucherValueDecision
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionPlacement
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionServiceNeed
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.invoicing.service.DaycareCodes
import fi.espoo.evaka.pis.service.Parentship
import fi.espoo.evaka.pis.service.PersonJSON
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

val uuid1 = UUID.fromString("124c0bcf-df0d-4a89-86ff-8f971123f87d")
val uuid2 = UUID.fromString("603e5943-29b5-48de-8095-01b4ef533382")
val uuid3 = UUID.fromString("124c0bcf-df0d-4a89-86ff-8f9711622233")
val uuid4 = UUID.fromString("603e5943-29b5-48de-8095-01b4ef511122")
val uuid5 = UUID.fromString("1b533dbb-9871-4406-b48e-735153d8f36c")

val testDecisionFrom = LocalDate.of(2019, 5, 1)
val testDecisionTo = LocalDate.of(2019, 5, 31)

val testPlacement = PermanentPlacement(
    unit = UUID.randomUUID(),
    type = PlacementType.DAYCARE,
    serviceNeed = ServiceNeed.GTE_35
)

val testChild1 = PersonData.WithDateOfBirth(uuid4, LocalDate.of(2016, 1, 1))

val testChild2 = testChild1.copy(id = uuid5, dateOfBirth = testChild1.dateOfBirth.plusDays(1))

val oldTestPricing = Pricing(
    multiplier = BigDecimal("0.1"),
    maxThresholdDifference = 250000,
    minThreshold2 = 200000,
    minThreshold3 = 250000,
    minThreshold4 = 300000,
    minThreshold5 = 350000,
    minThreshold6 = 400000,
    thresholdIncrease6Plus = 50000
)

val testPricing = Pricing(
    multiplier = BigDecimal("0.1070"),
    maxThresholdDifference = 269700,
    minThreshold2 = 210200,
    minThreshold3 = 271300,
    minThreshold4 = 308000,
    minThreshold5 = 344700,
    minThreshold6 = 381300,
    thresholdIncrease6Plus = 14200
)

val testDecisionPart1 =
    FeeDecisionPart(
        child = testChild1,
        placement = testPlacement,
        baseFee = 28900,
        siblingDiscount = 0,
        fee = 28900,
        feeAlterations = listOf()
    )
val testDecisionPart2 =
    FeeDecisionPart(
        child = testChild2,
        placement = testPlacement,
        baseFee = 28900,
        siblingDiscount = 0,
        fee = 28900,
        feeAlterations = listOf()
    )

val testDecision1 = FeeDecision(
    id = uuid1,
    status = FeeDecisionStatus.DRAFT,
    decisionNumber = 1010101010L,
    decisionType = FeeDecisionType.NORMAL,
    validFrom = testDecisionFrom,
    validTo = testDecisionTo,
    headOfFamily = PersonData.JustId(uuid3),
    partner = null,
    headOfFamilyIncome = null,
    partnerIncome = null,
    familySize = 3,
    pricing = testPricing,
    parts = listOf(testDecisionPart1, testDecisionPart2.copy(siblingDiscount = 50, fee = 14500)),
    createdAt = Instant.now()
)

val testDecision2 = FeeDecision(
    id = uuid2,
    status = FeeDecisionStatus.SENT,
    decisionNumber = 11,
    decisionType = FeeDecisionType.NORMAL,
    validFrom = testDecisionFrom,
    validTo = null,
    headOfFamily = PersonData.JustId(UUID.randomUUID()),
    partner = null,
    headOfFamilyIncome = null,
    partnerIncome = null,
    familySize = 2,
    pricing = testPricing,
    parts = listOf(testDecisionPart2),
    createdAt = Instant.now()
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

val testDaycareCodes: Map<UUID, DaycareCodes> = mapOf(
    testPlacement.unit to DaycareCodes(249, "31450", "01")
)

fun testPisPerson(data: PersonData.JustId): PersonJSON {
    return PersonJSON(
        id = data.id,
        socialSecurityNumber = null,
        dateOfBirth = LocalDate.of(1970, 1, 1)
    )
}

fun testPisPerson(data: PersonData.WithDateOfBirth): PersonJSON {
    return PersonJSON(
        id = data.id,
        socialSecurityNumber = null,
        dateOfBirth = data.dateOfBirth
    )
}

val testPisFridgeParentId = UUID.randomUUID()
val testPisFridgeParent = PersonData.JustId(testPisFridgeParentId)
val testPisFridgeChildId = UUID.randomUUID()
val testPisFridgeChild = PersonData.WithDateOfBirth(testPisFridgeChildId, LocalDate.of(2017, 1, 1))

val testPeriod = LocalDate.now().let {
    DateRange(it, it.plusDays(100))
}

fun testParentship(
    child: PersonJSON = testPisPerson(testPisFridgeChild),
    parent: PersonJSON = testPisPerson(testPisFridgeParent),
    period: FiniteDateRange = testPeriod.asFiniteDateRange()!!
) = Parentship(
    id = UUID.randomUUID(),
    childId = child.id,
    child = child,
    headOfChildId = parent.id,
    headOfChild = parent,
    startDate = period.start,
    endDate = period.end
)

val testParentship = testParentship()

fun testFridgeFamily(
    parent: PersonData.JustId = testPisFridgeParent,
    children: List<PersonData.WithDateOfBirth> = listOf(testPisFridgeChild),
    period: DateRange = testPeriod
) = FridgeFamily(
    headOfFamily = parent,
    partner = null,
    children = children,
    period = period
)

val testFridgeFamily = testFridgeFamily()

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

val testFeeAlteration = FeeAlteration(
    id = UUID.randomUUID(),
    personId = UUID.randomUUID(),
    type = FeeAlteration.Type.DISCOUNT,
    amount = 100,
    isAbsolute = false,
    validFrom = LocalDate.of(2000, 1, 1),
    validTo = null,
    notes = ""
)

fun createFeeDecisionAlterationFixture(
    type: FeeAlteration.Type = FeeAlteration.Type.DISCOUNT,
    amount: Int = 100,
    isAbsolute: Boolean = false,
    effect: Int = 10000
) =
    FeeAlterationWithEffect(type, amount, isAbsolute, effect)

fun createFeeDecisionPartFixture(
    childId: UUID,
    dateOfBirth: LocalDate,
    daycareId: UUID,
    serviceNeed: ServiceNeed = ServiceNeed.MISSING,
    baseFee: Int = 28900,
    siblingDiscount: Int = 0,
    fee: Int = 28900,
    feeAlterations: List<FeeAlterationWithEffect> = listOf(),
    placementType: PlacementType = PlacementType.DAYCARE
) = FeeDecisionPart(
    child = PersonData.WithDateOfBirth(id = childId, dateOfBirth = dateOfBirth),
    placement = testPlacement.copy(unit = daycareId, serviceNeed = serviceNeed, type = placementType),
    baseFee = baseFee,
    siblingDiscount = siblingDiscount,
    fee = fee,
    feeAlterations = feeAlterations
)

fun createFeeDecisionChildFixture(
    childId: UUID,
    dateOfBirth: LocalDate,
    placementUnitId: UUID,
    placementType: fi.espoo.evaka.placement.PlacementType,
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
    finalFee = fee + feeAlterations.sumBy { it.effect }
)

fun createFeeDecisionFixture(
    status: FeeDecisionStatus,
    decisionType: FeeDecisionType,
    period: DateRange,
    headOfFamilyId: UUID,
    parts: List<FeeDecisionPart>,
    pricing: Pricing = testPricing,
    headOfFamilyIncome: DecisionIncome? = null
) = FeeDecision(
    id = UUID.randomUUID(),
    status = status,
    decisionType = decisionType,
    validFrom = period.start,
    validTo = period.end,
    headOfFamily = PersonData.JustId(headOfFamilyId),
    partner = null,
    headOfFamilyIncome = headOfFamilyIncome,
    partnerIncome = null,
    familySize = parts.size + 1,
    pricing = pricing,
    parts = parts
)

fun createFeeDecision2Fixture(
    status: FeeDecisionStatus,
    decisionType: FeeDecisionType,
    period: DateRange,
    headOfFamilyId: UUID,
    children: List<FeeDecisionChild>,
    pricing: Pricing = testPricing,
    headOfFamilyIncome: DecisionIncome? = null
) = FeeDecision2(
    id = UUID.randomUUID(),
    status = status,
    decisionType = decisionType,
    validDuring = period,
    headOfFamily = PersonData.JustId(headOfFamilyId),
    partner = null,
    headOfFamilyIncome = headOfFamilyIncome,
    partnerIncome = null,
    familySize = children.size + 1,
    pricing = pricing,
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
    placementType: fi.espoo.evaka.placement.PlacementType,
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
    pricing = testPricing,
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
    finalCoPayment = coPayment + feeAlterations.sumBy { it.effect }
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
