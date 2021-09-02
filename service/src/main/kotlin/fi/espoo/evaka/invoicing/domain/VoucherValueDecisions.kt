// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.domain

import com.fasterxml.jackson.annotation.JsonProperty
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.VoucherValueDecisionId
import fi.espoo.evaka.shared.domain.DateRange
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.json.Json
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

data class VoucherValueDecision(
    override val id: VoucherValueDecisionId,
    override val validFrom: LocalDate,
    override val validTo: LocalDate?,
    @Nested("head_of_family")
    override val headOfFamily: PersonData.JustId,
    val status: VoucherValueDecisionStatus,
    val decisionNumber: Long? = null,
    @Nested("partner")
    val partner: PersonData.JustId?,
    @Json
    val headOfFamilyIncome: DecisionIncome?,
    @Json
    val partnerIncome: DecisionIncome?,
    val familySize: Int,
    @Json
    val feeThresholds: FeeDecisionThresholds,
    @Nested("child")
    val child: PersonData.WithDateOfBirth,
    @Nested("placement")
    val placement: VoucherValueDecisionPlacement,
    @Nested("service_need")
    val serviceNeed: VoucherValueDecisionServiceNeed,
    val baseCoPayment: Int,
    val siblingDiscount: Int,
    val coPayment: Int,
    @Json
    val feeAlterations: List<FeeAlterationWithEffect>,
    val finalCoPayment: Int,
    val baseValue: Int,
    val ageCoefficient: BigDecimal,
    val voucherValue: Int,
    val documentKey: String? = null,
    @Nested("approved_by")
    val approvedBy: PersonData.JustId? = null,
    val approvedAt: Instant? = null,
    val sentAt: Instant? = null,
    val created: Instant = Instant.now()
) : FinanceDecision<VoucherValueDecision> {
    override fun withRandomId() = this.copy(id = VoucherValueDecisionId(UUID.randomUUID()))
    override fun withValidity(period: DateRange) = this.copy(validFrom = period.start, validTo = period.end)
    override fun contentEquals(decision: VoucherValueDecision): Boolean {
        return this.headOfFamily == decision.headOfFamily &&
            this.partner == decision.partner &&
            this.headOfFamilyIncome == decision.headOfFamilyIncome &&
            this.partnerIncome == decision.partnerIncome &&
            this.familySize == decision.familySize &&
            this.child == decision.child &&
            this.placement == decision.placement &&
            this.serviceNeed.feeCoefficient == decision.serviceNeed.feeCoefficient &&
            this.serviceNeed.voucherValueCoefficient == decision.serviceNeed.voucherValueCoefficient &&
            this.baseCoPayment == decision.baseCoPayment &&
            this.siblingDiscount == decision.siblingDiscount &&
            this.coPayment == decision.coPayment &&
            this.feeAlterations == decision.feeAlterations &&
            this.finalCoPayment == decision.finalCoPayment &&
            this.baseValue == decision.baseValue &&
            this.ageCoefficient == decision.ageCoefficient &&
            this.voucherValue == decision.voucherValue
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

data class VoucherValueDecisionDetailed(
    val id: VoucherValueDecisionId,
    val validFrom: LocalDate,
    val validTo: LocalDate?,
    val status: VoucherValueDecisionStatus,
    val decisionNumber: Long? = null,
    @Nested("head")
    val headOfFamily: PersonData.Detailed,
    @Nested("partner")
    val partner: PersonData.Detailed?,
    @Json
    val headOfFamilyIncome: DecisionIncome?,
    @Json
    val partnerIncome: DecisionIncome?,
    val familySize: Int,
    @Json
    val feeThresholds: FeeDecisionThresholds,
    @Nested("child")
    val child: PersonData.Detailed,
    @Nested("placement")
    val placement: VoucherValueDecisionPlacementDetailed,
    @Nested("service_need")
    val serviceNeed: VoucherValueDecisionServiceNeed,
    val baseCoPayment: Int,
    val siblingDiscount: Int,
    val coPayment: Int,
    @Json
    val feeAlterations: List<FeeAlterationWithEffect>,
    val finalCoPayment: Int,
    val baseValue: Int,
    val childAge: Int,
    val ageCoefficient: BigDecimal,
    val voucherValue: Int,
    val documentKey: String? = null,
    @Nested("approved_by")
    val approvedBy: PersonData.WithName? = null,
    val approvedAt: Instant? = null,
    val sentAt: Instant? = null,
    val created: Instant = Instant.now(),
    val financeDecisionHandlerFirstName: String?,
    val financeDecisionHandlerLastName: String?
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
}

data class VoucherValueDecisionSummary(
    val id: VoucherValueDecisionId,
    val validFrom: LocalDate,
    val validTo: LocalDate?,
    val status: VoucherValueDecisionStatus,
    val decisionNumber: Long? = null,
    val headOfFamily: PersonData.Basic,
    val child: PersonData.Basic,
    val finalCoPayment: Int,
    val voucherValue: Int,
    val approvedAt: Instant? = null,
    val sentAt: Instant? = null,
    val created: Instant = Instant.now(),
)

data class VoucherValueDecisionPlacement(
    @Nested("unit")
    val unit: UnitData.JustId,
    val type: PlacementType
)

data class VoucherValueDecisionPlacementDetailed(
    @Nested("unit")
    val unit: UnitData.Detailed,
    val type: PlacementType
)

data class VoucherValueDecisionServiceNeed(
    val feeCoefficient: BigDecimal,
    val voucherValueCoefficient: BigDecimal,
    val feeDescriptionFi: String,
    val feeDescriptionSv: String,
    val voucherValueDescriptionFi: String,
    val voucherValueDescriptionSv: String
)

fun firstOfMonthAfterThirdBirthday(dateOfBirth: LocalDate): LocalDate = when (dateOfBirth.dayOfMonth) {
    1 -> dateOfBirth.plusYears(3)
    else -> dateOfBirth.plusYears(3).plusMonths(1).withDayOfMonth(1)
}

fun getAgeCoefficient(period: DateRange, dateOfBirth: LocalDate, voucherValues: VoucherValue): BigDecimal {
    val thirdBirthdayPeriodStart = firstOfMonthAfterThirdBirthday(dateOfBirth)
    val periodStartInMiddleOfTargetPeriod = period.includes(thirdBirthdayPeriodStart) && thirdBirthdayPeriodStart != period.start && thirdBirthdayPeriodStart != period.end

    check(!periodStartInMiddleOfTargetPeriod) {
        "Third birthday period start ($thirdBirthdayPeriodStart) is in the middle of the period ($period), cannot calculate an unambiguous age coefficient"
    }

    return when {
        period.start < thirdBirthdayPeriodStart -> voucherValues.ageUnderThreeCoefficient
        else -> BigDecimal("1.00")
    }
}

fun calculateVoucherValue(voucherValues: VoucherValue, ageCoefficient: BigDecimal, serviceCoefficient: BigDecimal): Int {
    return (BigDecimal(voucherValues.baseValue) * ageCoefficient * serviceCoefficient).toInt()
}
