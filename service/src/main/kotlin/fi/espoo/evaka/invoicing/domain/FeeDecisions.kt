// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import fi.espoo.evaka.shared.domain.DateRange
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.math.max

@JsonIgnoreProperties(ignoreUnknown = true)
data class FeeDecision(
    override val id: UUID,
    override val parts: List<FeeDecisionPart>,
    override val validFrom: LocalDate,
    override val validTo: LocalDate?,
    override val headOfFamily: PersonData.JustId,
    val status: FeeDecisionStatus,
    val decisionNumber: Long? = null,
    val decisionType: FeeDecisionType,
    val partner: PersonData.JustId?,
    val headOfFamilyIncome: DecisionIncome?,
    val partnerIncome: DecisionIncome?,
    val familySize: Int,
    val pricing: Pricing,
    val documentKey: String? = null,
    val approvedBy: PersonData.JustId? = null,
    val approvedAt: Instant? = null,
    val decisionHandler: PersonData.JustId? = null,
    val createdAt: Instant = Instant.now(),
    val sentAt: Instant? = null
) : FinanceDecision<FeeDecision>, Mergeable<FeeDecisionPart, FeeDecision> {
    override fun withParts(parts: List<FeeDecisionPart>) = this.copy(parts = parts)
    override fun withRandomId() = this.copy(id = UUID.randomUUID())
    override fun withValidity(period: DateRange) = this.copy(validFrom = period.start, validTo = period.end)
    override fun contentEquals(decision: FeeDecision): Boolean {
        return this.parts.toSet() == decision.parts.toSet() &&
            this.headOfFamily == decision.headOfFamily &&
            this.partner == decision.partner &&
            this.headOfFamilyIncome == decision.headOfFamilyIncome &&
            this.partnerIncome == decision.partnerIncome &&
            this.familySize == decision.familySize &&
            this.pricing == decision.pricing
    }

    override fun isAnnulled(): Boolean = this.status == FeeDecisionStatus.ANNULLED
    override fun isEmpty(): Boolean = this.parts.isEmpty()
    override fun annul() = this.copy(status = FeeDecisionStatus.ANNULLED)

    @JsonProperty("totalFee")
    fun totalFee(): Int = parts.fold(0) { sum, part -> sum + part.finalFee() }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class FeeDecisionPart(
    val child: PersonData.WithDateOfBirth,
    val placement: PermanentPlacement,
    val baseFee: Int,
    val siblingDiscount: Int,
    val fee: Int,
    val feeAlterations: List<FeeAlterationWithEffect>
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
    ANNULLED;

    companion object {
        /**
         *  list of statuses that have an overlap exclusion constraint at the database level and that signal that a decision is in effect
         */
        val effective = arrayOf(SENT, WAITING_FOR_SENDING, WAITING_FOR_MANUAL_SENDING)
    }
}

enum class FeeDecisionType {
    NORMAL,
    RELIEF_REJECTED,
    RELIEF_PARTLY_ACCEPTED,
    RELIEF_ACCEPTED
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class FeeDecisionDetailed(
    override val id: UUID,
    override val parts: List<FeeDecisionPartDetailed>,
    val validFrom: LocalDate,
    val validTo: LocalDate?,
    val status: FeeDecisionStatus,
    val decisionNumber: Long? = null,
    val decisionType: FeeDecisionType,
    val headOfFamily: PersonData.Detailed,
    val partner: PersonData.Detailed?,
    val headOfFamilyIncome: DecisionIncome?,
    val partnerIncome: DecisionIncome?,
    val familySize: Int,
    val pricing: Pricing,
    val documentKey: String? = null,
    val approvedBy: PersonData.WithName? = null,
    val approvedAt: Instant? = null,
    val createdAt: Instant = Instant.now(),
    val sentAt: Instant? = null,
    val financeDecisionHandlerName: String?
) : Mergeable<FeeDecisionPartDetailed, FeeDecisionDetailed> {
    override fun withParts(parts: List<FeeDecisionPartDetailed>) = this.copy(parts = parts)

    @JsonProperty("totalFee")
    fun totalFee(): Int = parts.fold(0) { sum, part -> sum + part.finalFee() }

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
        return isRetroactive(this.validFrom, LocalDate.from(sentAtLocalDate ?: LocalDate.now()))
    }

    @JsonProperty("minThreshold")
    fun minThreshold(): Int = getMinThreshold(pricing, familySize)

    @JsonProperty("feePercent")
    fun feePercent(): BigDecimal = pricing.multiplier.multiply(BigDecimal(100)).setScale(1, RoundingMode.HALF_UP)
}

fun isRetroactive(decisionValidFrom: LocalDate, sentAt: LocalDate): Boolean {
    val retroThreshold = sentAt.withDayOfMonth(1)
    return decisionValidFrom.isBefore(retroThreshold)
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
    override val id: UUID,
    override val parts: List<FeeDecisionPartSummary>,
    val validFrom: LocalDate,
    val validTo: LocalDate?,
    val status: FeeDecisionStatus,
    val decisionNumber: Long? = null,
    val headOfFamily: PersonData.Basic,
    val approvedAt: Instant? = null,
    val createdAt: Instant = Instant.now(),
    val sentAt: Instant? = null,
    val finalPrice: Int
) : Mergeable<FeeDecisionPartSummary, FeeDecisionSummary> {
    override fun withParts(parts: List<FeeDecisionPartSummary>) = this.copy(parts = parts)
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class FeeDecisionPartSummary(
    val child: PersonData.Basic
)

private interface Mergeable<Part, Decision : Mergeable<Part, Decision>> {
    val id: UUID
    val parts: List<Part>

    fun withParts(parts: List<Part>): Decision
}

fun <Part, Decision : Mergeable<Part, Decision>, Decisions : Iterable<Decision>> Decisions.merge(): List<Decision> {
    val map = mutableMapOf<UUID, Decision>()
    for (decision in this) {
        val id = decision.id
        if (map.containsKey(id)) {
            val existing = map.getValue(id)
            map[id] = existing.withParts(existing.parts + decision.parts)
        } else {
            map[id] = decision
        }
    }
    return map.values.toList()
}

fun useMaxFee(incomes: List<DecisionIncome?>): Boolean = incomes.filterNotNull().let {
    it.size < incomes.size || it.any { income -> income.effect != IncomeEffect.INCOME }
}

fun calculateBaseFee(
    pricing: Pricing,
    familySize: Int,
    incomes: List<DecisionIncome?>
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
        PlacementType.PRESCHOOL, PlacementType.PREPARATORY -> 0
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

fun getECHAIncrease(childId: UUID, period: DateRange) = FeeAlteration(
    personId = childId,
    type = FeeAlteration.Type.INCREASE,
    amount = ECHAIncrease,
    isAbsolute = true,
    notes = "ECHA",
    validFrom = period.start,
    validTo = period.end
)
