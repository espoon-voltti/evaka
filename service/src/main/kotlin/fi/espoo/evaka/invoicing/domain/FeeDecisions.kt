// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.domain

import com.fasterxml.jackson.annotation.JsonProperty
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.domain.DateRange
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.json.Json
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.math.max

data class FeeDecision(
    override val id: UUID,
    override val children: List<FeeDecisionChild>,
    val validDuring: DateRange,
    @Nested("head_of_family")
    override val headOfFamily: PersonData.JustId,
    val status: FeeDecisionStatus,
    val decisionNumber: Long? = null,
    val decisionType: FeeDecisionType,
    @Nested("partner")
    val partner: PersonData.JustId?,
    @Json
    val headOfFamilyIncome: DecisionIncome?,
    @Json
    val partnerIncome: DecisionIncome?,
    val familySize: Int,
    @Json
    val feeThresholds: FeeDecisionThresholds,
    val documentKey: String? = null,
    @Nested("approved_by")
    val approvedBy: PersonData.JustId? = null,
    val approvedAt: Instant? = null,
    @Nested("decision_handler")
    val decisionHandler: PersonData.JustId? = null,
    val sentAt: Instant? = null,
    val created: Instant = Instant.now()
) : FinanceDecision<FeeDecision>, Mergeable<FeeDecisionChild, FeeDecision> {
    override val validFrom: LocalDate = validDuring.start
    override val validTo: LocalDate? = validDuring.end
    override fun withRandomId() = this.copy(id = UUID.randomUUID())
    override fun withValidity(period: DateRange) = this.copy(validDuring = period)
    override fun contentEquals(decision: FeeDecision): Boolean {
        return this.children.toSet() == decision.children.toSet() &&
            this.headOfFamily == decision.headOfFamily &&
            this.partner == decision.partner &&
            this.headOfFamilyIncome == decision.headOfFamilyIncome &&
            this.partnerIncome == decision.partnerIncome &&
            this.familySize == decision.familySize &&
            this.feeThresholds == decision.feeThresholds
    }

    override fun isAnnulled(): Boolean = this.status == FeeDecisionStatus.ANNULLED
    override fun isEmpty(): Boolean = this.children.isEmpty()
    override fun annul() = this.copy(status = FeeDecisionStatus.ANNULLED)
    override fun withChildren(children: List<FeeDecisionChild>) = this.copy(children = children)

    @JsonProperty("totalFee")
    fun totalFee(): Int = children.fold(0) { sum, child -> sum + child.finalFee }
}

data class FeeDecisionChild(
    @Nested("child")
    val child: PersonData.WithDateOfBirth,
    @Nested("placement")
    val placement: FeeDecisionPlacement,
    @Nested("service_need")
    val serviceNeed: FeeDecisionServiceNeed,
    val baseFee: Int,
    val siblingDiscount: Int,
    val fee: Int,
    @Json
    val feeAlterations: List<FeeAlterationWithEffect>,
    val finalFee: Int
)

data class FeeDecisionPlacement(
    @Nested("unit")
    val unit: UnitData.JustId,
    val type: PlacementType
)

data class FeeDecisionServiceNeed(
    val feeCoefficient: BigDecimal,
    val descriptionFi: String,
    val descriptionSv: String,
    val missing: Boolean
)

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

data class FeeDecisionDetailed(
    override val id: UUID,
    override val children: List<FeeDecisionChildDetailed>,
    val validDuring: DateRange,
    val status: FeeDecisionStatus,
    val decisionNumber: Long? = null,
    val decisionType: FeeDecisionType,
    val headOfFamily: PersonData.Detailed,
    val partner: PersonData.Detailed?,
    val headOfFamilyIncome: DecisionIncome?,
    val partnerIncome: DecisionIncome?,
    val familySize: Int,
    val feeThresholds: FeeDecisionThresholds,
    val documentKey: String? = null,
    val approvedBy: PersonData.WithName? = null,
    val approvedAt: Instant? = null,
    val sentAt: Instant? = null,
    val financeDecisionHandlerFirstName: String?,
    val financeDecisionHandlerLastName: String?,
    val created: Instant = Instant.now()
) : Mergeable<FeeDecisionChildDetailed, FeeDecisionDetailed> {
    override fun withChildren(children: List<FeeDecisionChildDetailed>) = this.copy(children = children)
    @JsonProperty("totalFee")
    fun totalFee(): Int = children.fold(0) { sum, part -> sum + part.finalFee }

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
        return isRetroactive(this.validDuring.start, LocalDate.from(sentAtLocalDate ?: LocalDate.now()))
    }
}

fun isRetroactive(decisionValidFrom: LocalDate, sentAt: LocalDate): Boolean {
    val retroThreshold = sentAt.withDayOfMonth(1)
    return decisionValidFrom.isBefore(retroThreshold)
}

data class FeeDecisionChildDetailed(
    val child: PersonData.Detailed,
    val placementType: PlacementType,
    val placementUnit: UnitData.Detailed,
    val serviceNeedFeeCoefficient: BigDecimal,
    val serviceNeedDescriptionFi: String,
    val serviceNeedDescriptionSv: String,
    val serviceNeedMissing: Boolean,
    val baseFee: Int,
    val siblingDiscount: Int,
    val fee: Int,
    val feeAlterations: List<FeeAlterationWithEffect>,
    val finalFee: Int
)

data class FeeDecisionSummary(
    override val id: UUID,
    override val children: List<PersonData.Basic>,
    val validDuring: DateRange,
    val status: FeeDecisionStatus,
    val decisionNumber: Long? = null,
    val headOfFamily: PersonData.Basic,
    val approvedAt: Instant? = null,
    val sentAt: Instant? = null,
    val finalPrice: Int,
    val created: Instant = Instant.now()
) : Mergeable<PersonData.Basic, FeeDecisionSummary> {
    override fun withChildren(children: List<PersonData.Basic>) = this.copy(children = children)
}

private interface Mergeable<Child, Decision : Mergeable<Child, Decision>> {
    val id: UUID
    val children: List<Child>

    fun withChildren(children: List<Child>): Decision
}

fun <Child, Decision : Mergeable<Child, Decision>, Decisions : Iterable<Decision>> Decisions.merge(): List<Decision> {
    val map = mutableMapOf<UUID, Decision>()
    for (decision in this) {
        val id = decision.id
        if (map.containsKey(id)) {
            val existing = map.getValue(id)
            map[id] = existing.withChildren(existing.children + decision.children)
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
    feeThresholds: FeeThresholds,
    familySize: Int,
    incomes: List<DecisionIncome?>
): Int {
    check(familySize > 1) { "Family size should not be less than 2" }

    val multiplier = feeThresholds.incomeMultiplier(familySize)

    val feeInCents = if (useMaxFee(incomes)) {
        multiplier * BigDecimal(
            feeThresholds.maxIncomeThreshold(familySize) - feeThresholds.minIncomeThreshold(familySize)
        )
    } else {
        val minThreshold = feeThresholds.minIncomeThreshold(familySize)
        val maxThreshold = feeThresholds.maxIncomeThreshold(familySize)
        val totalIncome = incomes.filterNotNull().sumOf { it.total }
        val totalSurplus = minOf(maxOf(totalIncome - minThreshold, 0), maxThreshold - minThreshold)
        multiplier * BigDecimal(totalSurplus)
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

fun calculateFeeBeforeFeeAlterations(baseFee: Int, serviceNeedCoefficient: BigDecimal, siblingDiscountMultiplier: BigDecimal, minFee: Int): Int {
    val feeAfterSiblingDiscount = roundToEuros(BigDecimal(baseFee) * siblingDiscountMultiplier)
    val feeBeforeRounding = roundToEuros(feeAfterSiblingDiscount * serviceNeedCoefficient).toInt()
    return feeBeforeRounding.let { fee ->
        if (fee < minFee) 0
        else fee
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
