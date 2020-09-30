// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import fi.espoo.evaka.shared.domain.Period
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.math.max

@JsonIgnoreProperties(ignoreUnknown = true)
data class FeeDecision(
    val id: UUID,
    val status: FeeDecisionStatus,
    val decisionNumber: Long? = null,
    val decisionType: FeeDecisionType,
    val validFrom: LocalDate,
    val validTo: LocalDate?,
    val headOfFamily: PersonData.JustId,
    val partner: PersonData.JustId?,
    val headOfFamilyIncome: FeeDecisionIncome?,
    val partnerIncome: FeeDecisionIncome?,
    val familySize: Int,
    val pricing: Pricing,
    val parts: List<FeeDecisionPart>,
    val documentKey: String? = null,
    val approvedBy: PersonData.JustId? = null,
    val approvedAt: Instant? = null,
    val createdAt: Instant = Instant.now(),
    val sentAt: Instant? = null
) {
    @JsonProperty("totalFee")
    fun totalFee(): Int =
        max(0, parts.fold(0) { sum, part -> sum + part.finalFee() })
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class FeeDecisionIncome(
    val effect: IncomeEffect,
    val data: Map<IncomeType, Int>,
    val total: Int,
    @get:JsonProperty("isEntrepreneur") val isEntrepreneur: Boolean = false,
    val worksAtECHA: Boolean = false,
    val validFrom: LocalDate?,
    val validTo: LocalDate?
) {
    @JsonProperty("totalIncome")
    fun totalIncome(): Int =
        data.entries
            .filter { (type, _) -> type.multiplier > 0 }
            .map { (type, value) -> type.multiplier * value }
            .sum()

    @JsonProperty("totalExpenses")
    fun totalExpenses(): Int =
        data.entries
            .filter { (type, _) -> type.multiplier < 0 }
            .map { (type, value) -> -1 * type.multiplier * value }
            .sum()
}

fun toFeeDecisionIncome(income: Income) = FeeDecisionIncome(
    effect = income.effect,
    data = income.data.mapValues { (_, value) -> value.monthlyAmount() },
    total = income.total(),
    isEntrepreneur = income.isEntrepreneur,
    worksAtECHA = income.worksAtECHA,
    validFrom = income.validFrom,
    validTo = income.validTo
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class FeeDecisionPart(
    val child: PersonData.WithDateOfBirth,
    val placement: PermanentPlacement,
    val baseFee: Int,
    val siblingDiscount: Int,
    val fee: Int,
    val feeAlterations: List<FeeAlterationWithEffect> = listOf()
) {
    @JsonProperty("finalFee")
    fun finalFee(): Int = fee + feeAlterations.sumBy { it.effect }
}

data class FeeAlterationWithEffect(
    val type: FeeAlteration.Type,
    val amount: Int,
    @get:JsonProperty("isAbsolute") val isAbsolute: Boolean,
    val effect: Int
)

enum class FeeDecisionStatus {
    DRAFT,
    WAITING_FOR_SENDING,
    WAITING_FOR_MANUAL_SENDING,
    SENT,
    ANNULLED
}

enum class FeeDecisionType {
    NORMAL,
    RELIEF_REJECTED,
    RELIEF_PARTLY_ACCEPTED,
    RELIEF_ACCEPTED
}

data class FeeDecisionInvariant(
    val headOfFamily: PersonData.JustId,
    val partner: PersonData.JustId?,
    val headOfFamilyIncome: FeeDecisionIncome?,
    val partnerIncome: FeeDecisionIncome?,
    val familySize: Int,
    val pricing: Pricing,
    val parts: Set<FeeDecisionPart>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class FeeDecisionDetailed(
    val id: UUID,
    val status: FeeDecisionStatus,
    val decisionNumber: Long? = null,
    val decisionType: FeeDecisionType,
    val validFrom: LocalDate,
    val validTo: LocalDate?,
    val headOfFamily: PersonData.Detailed,
    val partner: PersonData.Detailed?,
    val headOfFamilyIncome: FeeDecisionIncome?,
    val partnerIncome: FeeDecisionIncome?,
    val familySize: Int,
    val pricing: Pricing,
    val parts: List<FeeDecisionPartDetailed>,
    val documentKey: String? = null,
    val approvedBy: PersonData.WithName? = null,
    val approvedAt: Instant? = null,
    val createdAt: Instant = Instant.now(),
    val sentAt: Instant? = null
) {
    @JsonProperty("totalFee")
    fun totalFee(): Int =
        max(0, parts.fold(0) { sum, part -> sum + part.finalFee() })

    @JsonProperty("incomeEffect")
    fun incomeEffect(): IncomeEffect =
        getTotalIncomeEffect(partner != null, headOfFamilyIncome?.effect, partnerIncome?.effect)

    @JsonProperty("totalIncome")
    fun totalIncome(): Int? = getTotalIncome(
        partner != null,
        headOfFamilyIncome?.effect,
        headOfFamilyIncome?.total,
        partnerIncome?.effect,
        partnerIncome?.total
    )

    @JsonProperty("requiresManualSending")
    fun requiresManualSending(): Boolean {
        if (decisionType !== FeeDecisionType.NORMAL || headOfFamily.forceManualFeeDecisions) {
            return true
        }
        return headOfFamily.let {
            listOf(
                it.ssn,
                it.streetAddress,
                it.postalCode,
                it.postOffice
            ).any { item -> item.isNullOrBlank() }
        }
    }

    @JsonProperty("isRetroactive")
    fun isRetroactive(): Boolean {
        val sentAtLocalDate = sentAt?.atZone(ZoneId.of("UTC"))
        val retroThreshold = LocalDate.from(sentAtLocalDate ?: LocalDate.now()).withDayOfMonth(1)
        return this.validFrom.isBefore(retroThreshold)
    }

    @JsonProperty("minThreshold")
    fun minThreshold(): Int = getMinThreshold(pricing, familySize)

    @JsonProperty("feePercent")
    fun feePercent(): BigDecimal = pricing.multiplier.multiply(BigDecimal(100)).setScale(1, RoundingMode.HALF_UP)
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class FeeDecisionPartDetailed(
    val child: PersonData.Detailed,
    val placement: PermanentPlacement,
    val placementUnit: UnitData.Detailed,
    val baseFee: Int,
    val siblingDiscount: Int,
    val fee: Int,
    val feeAlterations: List<FeeAlterationWithEffect> = listOf()
) {
    @JsonProperty("finalFee")
    fun finalFee(): Int = fee + feeAlterations.sumBy { it.effect }

    @JsonProperty("serviceNeedMultiplier")
    fun serviceNeedMultiplier(): Int = getServiceNeedPercentage(placement)
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class FeeDecisionSummary(
    val id: UUID,
    val status: FeeDecisionStatus,
    val decisionNumber: Long? = null,
    val validFrom: LocalDate,
    val validTo: LocalDate?,
    val headOfFamily: PersonData.Basic,
    val parts: List<FeeDecisionPartSummary>,
    val approvedAt: Instant? = null,
    val createdAt: Instant = Instant.now(),
    val sentAt: Instant? = null,
    val finalPrice: Int
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class FeeDecisionPartSummary(
    val child: PersonData.Basic
)

fun useMaxFee(incomes: List<FeeDecisionIncome?>): Boolean = incomes.filterNotNull().let {
    it.size < incomes.size || it.any { income -> income.effect != IncomeEffect.INCOME }
}

fun calculateBaseFee(
    pricing: Pricing,
    familySize: Int,
    incomes: List<FeeDecisionIncome?>
): Int {
    check(familySize > 1) { "Family size should not be less than 2" }

    val feeInCents = if (useMaxFee(incomes)) {
        pricing.multiplier * BigDecimal(pricing.maxThresholdDifference)
    } else {
        val minThreshold = getMinThreshold(pricing, familySize)
        val totalIncome = incomes.filterNotNull().map { it.total }.sum()
        val totalSurplus = minOf(maxOf(totalIncome - minThreshold, 0), pricing.maxThresholdDifference)
        pricing.multiplier * BigDecimal(totalSurplus)
    }

    // round the fee to whole euros, but keep the value in cents
    return roundToEuros(feeInCents).toInt()
}

fun roundToEuros(cents: BigDecimal): BigDecimal = cents
    .divide(BigDecimal(100), 0, RoundingMode.HALF_UP)
    .multiply(BigDecimal(100))

fun getTotalIncomeEffect(
    hasPartner: Boolean,
    headIncomeEffect: IncomeEffect?,
    partnerIncomeEffect: IncomeEffect?
): IncomeEffect = when {
    headIncomeEffect == IncomeEffect.INCOME && (!hasPartner || partnerIncomeEffect == IncomeEffect.INCOME) -> IncomeEffect.INCOME
    headIncomeEffect == IncomeEffect.MAX_FEE_ACCEPTED || partnerIncomeEffect == IncomeEffect.MAX_FEE_ACCEPTED -> IncomeEffect.MAX_FEE_ACCEPTED
    headIncomeEffect == IncomeEffect.INCOMPLETE || partnerIncomeEffect == IncomeEffect.INCOMPLETE -> IncomeEffect.INCOMPLETE
    else -> IncomeEffect.NOT_AVAILABLE
}

fun getTotalIncome(
    hasPartner: Boolean,
    headIncomeEffect: IncomeEffect?,
    headIncomeTotal: Int?,
    partnerIncomeEffect: IncomeEffect?,
    partnerIncomeTotal: Int?
): Int? = when {
    headIncomeEffect == IncomeEffect.INCOME && (!hasPartner || partnerIncomeEffect == IncomeEffect.INCOME) ->
        (headIncomeTotal ?: 0) + (partnerIncomeTotal ?: 0)
    else -> null
}

fun getMinThreshold(pricing: Pricing, familySize: Int): Int = when (familySize) {
    2 -> pricing.minThreshold2
    3 -> pricing.minThreshold3
    4 -> pricing.minThreshold4
    5 -> pricing.minThreshold5
    6 -> pricing.minThreshold6
    else -> pricing.minThreshold6 + ((familySize - 6) * pricing.thresholdIncrease6Plus)
}

fun getSiblingDiscountPercent(siblingOrdinal: Int): Int {
    if (siblingOrdinal <= 0) error("Sibling ordinal must be > 0 (was $siblingOrdinal)")

    return when (siblingOrdinal) {
        1 -> 0
        2 -> 50
        else -> 80
    }
}

// Current minimum fee, if a fee drops below this threshold it goes down to zero
const val minFee = 2700

fun calculateFeeBeforeFeeAlterations(baseFee: Int, placement: PermanentPlacement, siblingDiscount: Int): Int {
    val siblingDiscountMultiplier =
        BigDecimal(1) - BigDecimal(siblingDiscount).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
    val feeAfterSiblingDiscount = roundToEuros(BigDecimal(baseFee) * siblingDiscountMultiplier)
    val serviceNeedMultiplier =
        BigDecimal(getServiceNeedPercentage(placement)).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
    val feeBeforeRounding = roundToEuros(feeAfterSiblingDiscount * serviceNeedMultiplier).toInt()
    return feeBeforeRounding.let { fee ->
        if (fee < minFee) 0
        else fee
    }
}

fun getServiceNeedPercentage(placement: PermanentPlacement): Int {
    val unexpectedCombination =
        { error("Unexpected placement and service need combination: ${placement.type} ${placement.serviceNeed}") }

    return when (placement.type) {
        PlacementType.DAYCARE -> when (placement.serviceNeed) {
            ServiceNeed.MISSING,
            ServiceNeed.GTE_35 -> 100
            ServiceNeed.GT_25_LT_35 -> 80
            ServiceNeed.LTE_25 -> 60
            else -> unexpectedCombination()
        }
        PlacementType.PRESCHOOL_WITH_DAYCARE,
        PlacementType.PREPARATORY_WITH_DAYCARE,
        PlacementType.FIVE_YEARS_OLD_DAYCARE -> when (placement.serviceNeed) {
            ServiceNeed.MISSING,
            ServiceNeed.GTE_25 -> 80
            ServiceNeed.GT_15_LT_25 -> 60
            ServiceNeed.LTE_15 -> 35
            ServiceNeed.LTE_0 -> 0
            else -> unexpectedCombination()
        }
        PlacementType.CLUB, PlacementType.PRESCHOOL, PlacementType.PREPARATORY -> 0
    }
}

fun toFeeAlterationsWithEffects(fee: Int, feeAlterations: List<FeeAlteration>): List<FeeAlterationWithEffect> {
    val (_, alterations) = feeAlterations.fold(fee to listOf<FeeAlterationWithEffect>()) { pair, feeAlteration ->
        val (currentFee, currentAlterations) = pair
        val effect = feeAlterationEffect(currentFee, feeAlteration)
        Pair(
            currentFee + effect,
            currentAlterations + FeeAlterationWithEffect(
                feeAlteration.type,
                feeAlteration.amount,
                feeAlteration.isAbsolute,
                effect
            )
        )
    }
    return alterations
}

fun feeAlterationEffect(fee: Int, feeAlteration: FeeAlteration): Int {
    val multiplier = when (feeAlteration.type) {
        FeeAlteration.Type.RELIEF, FeeAlteration.Type.DISCOUNT -> -1
        FeeAlteration.Type.INCREASE -> 1
    }

    val effect = if (feeAlteration.isAbsolute) {
        val amountInCents = feeAlteration.amount * 100
        (multiplier * amountInCents)
    } else {
        val percentageMultiplier = BigDecimal(feeAlteration.amount).divide(BigDecimal(100), 10, RoundingMode.HALF_UP)
        (BigDecimal(fee) * (BigDecimal(multiplier) * percentageMultiplier))
            .setScale(0, RoundingMode.HALF_UP)
            .toInt()
    }

    // This so that the effect of absolute discounts (eg. -10€) on 0€ fees is 0€ as well
    return max(0, fee + effect) - fee
}

// Current flat increase for children with a parent working at ECHA
const val ECHAIncrease = 93

fun getECHAIncrease(childId: UUID, period: Period) = FeeAlteration(
    personId = childId,
    type = FeeAlteration.Type.INCREASE,
    amount = ECHAIncrease,
    isAbsolute = true,
    notes = "ECHA",
    validFrom = period.start,
    validTo = period.end
)
