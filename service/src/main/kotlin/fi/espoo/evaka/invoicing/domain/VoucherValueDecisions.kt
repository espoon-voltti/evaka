// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import fi.espoo.evaka.invoicing.domain.PlacementType.PREPARATORY
import fi.espoo.evaka.invoicing.domain.PlacementType.PREPARATORY_WITH_DAYCARE
import fi.espoo.evaka.invoicing.domain.PlacementType.PRESCHOOL
import fi.espoo.evaka.invoicing.domain.PlacementType.PRESCHOOL_WITH_DAYCARE
import fi.espoo.evaka.shared.domain.DateRange
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

data class VoucherValueDecision(
    override val id: UUID,
    override val validFrom: LocalDate,
    override val validTo: LocalDate?,
    override val headOfFamily: PersonData.JustId,
    val status: VoucherValueDecisionStatus,
    val decisionNumber: Long? = null,
    val partner: PersonData.JustId?,
    val headOfFamilyIncome: DecisionIncome?,
    val partnerIncome: DecisionIncome?,
    val familySize: Int,
    val pricing: Pricing,
    val child: PersonData.WithDateOfBirth,
    val placement: PermanentPlacementWithHours,
    val baseCoPayment: Int,
    val siblingDiscount: Int,
    val coPayment: Int,
    val feeAlterations: List<FeeAlterationWithEffect>,
    val baseValue: Int,
    val ageCoefficient: Int,
    val serviceCoefficient: Int,
    val value: Int,
    val documentKey: String? = null,
    val createdAt: Instant = Instant.now(),
    val approvedBy: PersonData.JustId? = null,
    val approvedAt: Instant? = null,
    val sentAt: Instant? = null
) : FinanceDecision<VoucherValueDecision> {
    override fun withRandomId() = this.copy(id = UUID.randomUUID())
    override fun withValidity(period: DateRange) = this.copy(validFrom = period.start, validTo = period.end)
    override fun contentEquals(decision: VoucherValueDecision): Boolean {
        return this.headOfFamily == decision.headOfFamily &&
            this.partner == decision.partner &&
            this.headOfFamilyIncome == decision.headOfFamilyIncome &&
            this.partnerIncome == decision.partnerIncome &&
            this.familySize == decision.familySize &&
            this.child == decision.child &&
            this.placement == decision.placement &&
            this.baseCoPayment == decision.baseCoPayment &&
            this.siblingDiscount == decision.siblingDiscount &&
            this.coPayment == decision.coPayment &&
            this.feeAlterations == decision.feeAlterations &&
            this.baseValue == decision.baseValue &&
            this.ageCoefficient == decision.ageCoefficient &&
            this.serviceCoefficient == decision.serviceCoefficient &&
            this.value == decision.value
    }

    override fun isAnnulled(): Boolean = this.status == VoucherValueDecisionStatus.ANNULLED
    override fun isEmpty(): Boolean = false
    override fun annul() = this.copy(status = VoucherValueDecisionStatus.ANNULLED)
}

enum class VoucherValueDecisionStatus {
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

@JsonIgnoreProperties(ignoreUnknown = true)
data class VoucherValueDecisionDetailed(
    val id: UUID,
    val validFrom: LocalDate,
    val validTo: LocalDate?,
    val status: VoucherValueDecisionStatus,
    val decisionNumber: Long? = null,
    val headOfFamily: PersonData.Detailed,
    val partner: PersonData.Detailed?,
    val headOfFamilyIncome: DecisionIncome?,
    val partnerIncome: DecisionIncome?,
    val familySize: Int,
    val pricing: Pricing,
    val child: PersonData.Detailed,
    val placement: PermanentPlacementWithHours,
    val placementUnit: UnitData.Detailed,
    val baseCoPayment: Int,
    val siblingDiscount: Int,
    val coPayment: Int,
    val feeAlterations: List<FeeAlterationWithEffect>,
    val baseValue: Int,
    val childAge: Int,
    val ageCoefficient: Int,
    val serviceCoefficient: Int,
    val value: Int,
    val documentKey: String? = null,
    val approvedBy: PersonData.WithName? = null,
    val approvedAt: Instant? = null,
    val createdAt: Instant = Instant.now(),
    val sentAt: Instant? = null,
    val financeDecisionHandlerName: String?
) {
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
        return this.headOfFamily.let {
            listOf(it.ssn, it.streetAddress, it.postalCode, it.postOffice).any { item -> item.isNullOrBlank() }
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

    @JsonProperty("finalCoPayment")
    fun finalCoPayment(): Int = coPayment + feeAlterations.sumBy { it.effect }

    @JsonProperty("serviceNeedMultiplier")
    fun serviceNeedMultiplier(): Int = getServiceNeedPercentage(placement.withoutHours())
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class VoucherValueDecisionSummary(
    val id: UUID,
    val validFrom: LocalDate,
    val validTo: LocalDate?,
    val status: VoucherValueDecisionStatus,
    val decisionNumber: Long? = null,
    val headOfFamily: PersonData.Basic,
    val child: PersonData.Basic,
    val finalCoPayment: Int,
    val value: Int,
    val approvedAt: Instant? = null,
    val createdAt: Instant = Instant.now(),
    val sentAt: Instant? = null
)

fun getAgeCoefficient(period: DateRange, dateOfBirth: LocalDate): Int {
    val thirdBirthday = dateOfBirth.plusYears(3)
    val birthdayInMiddleOfPeriod = period.includes(thirdBirthday) && thirdBirthday != period.start && thirdBirthday != period.end

    check(!birthdayInMiddleOfPeriod) {
        "Third birthday ($thirdBirthday) is in the middle of the period ($period), cannot calculate an unambiguous age coefficient"
    }

    return when {
        period.start < thirdBirthday -> 155
        else -> 100
    }
}

fun getServiceCoefficient(placement: PermanentPlacementWithHours): Int = when {
    listOf(PRESCHOOL, PRESCHOOL_WITH_DAYCARE, PREPARATORY, PREPARATORY_WITH_DAYCARE).contains(placement.type) -> 50
    placement.hours != null && placement.hours <= 25.0 -> 60
    else -> 100
}

fun calculateVoucherValue(baseValue: Int, ageCoefficient: Int, serviceCoefficient: Int): Int {
    val ageMultiplier = BigDecimal(ageCoefficient).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
    val serviceMultiplier = BigDecimal(serviceCoefficient).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
    return (BigDecimal(baseValue) * ageMultiplier * serviceMultiplier).toInt()
}
