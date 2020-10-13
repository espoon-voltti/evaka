// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import fi.espoo.evaka.shared.domain.Period
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class VoucherValueDecision(
    override val id: UUID,
    override val parts: List<VoucherValueDecisionPart>,
    override val validFrom: LocalDate,
    override val validTo: LocalDate?,
    val status: VoucherValueDecisionStatus,
    val decisionNumber: Long? = null,
    val headOfFamily: PersonData.JustId,
    val partner: PersonData.JustId?,
    val headOfFamilyIncome: DecisionIncome?,
    val partnerIncome: DecisionIncome?,
    val familySize: Int,
    val pricing: Pricing,
    val documentKey: String? = null,
    val createdAt: Instant = Instant.now(),
    val approvedBy: PersonData.JustId? = null,
    val approvedAt: Instant? = null,
    val sentAt: Instant? = null
) : FinanceDecision<VoucherValueDecisionPart, VoucherValueDecision>, MergeableDecision<VoucherValueDecisionPart, VoucherValueDecision> {
    override fun withParts(parts: List<VoucherValueDecisionPart>) = this.copy(parts = parts)
    override fun withRandomId() = this.copy(id = UUID.randomUUID())
    override fun withValidity(period: Period) = this.copy(validFrom = period.start, validTo = period.end)
    override fun contentEquals(decision: VoucherValueDecision): Boolean {
        return this.parts.toSet() == decision.parts.toSet() &&
            this.headOfFamily == decision.headOfFamily &&
            this.partner == decision.partner &&
            this.headOfFamilyIncome == decision.headOfFamilyIncome &&
            this.partnerIncome == decision.partnerIncome &&
            this.familySize == decision.familySize &&
            this.pricing == decision.pricing
    }

    @JsonProperty("totalCoPayment")
    fun totalCoPayment(): Int = parts.fold(0) { sum, part -> sum + part.finalCoPayment() }
}

data class VoucherValueDecisionPart(
    val child: PersonData.WithDateOfBirth,
    val placement: PermanentPlacement,
    val baseCoPayment: Int,
    val siblingDiscount: Int,
    val coPayment: Int,
    val feeAlterations: List<FeeAlterationWithEffect>
) : FinanceDecisionPart {
    @JsonProperty("finalCoPayment")
    fun finalCoPayment(): Int = coPayment + feeAlterations.sumBy { it.effect }
}

enum class VoucherValueDecisionStatus {
    DRAFT,
    WAITING_FOR_SENDING,
    WAITING_FOR_MANUAL_SENDING,
    SENT,
    ANNULLED
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class VoucherValueDecisionSummary(
    override val id: UUID,
    override val parts: List<VoucherValueDecisionPartSummary>,
    val validFrom: LocalDate,
    val validTo: LocalDate?,
    val status: VoucherValueDecisionStatus,
    val decisionNumber: Long? = null,
    val headOfFamily: PersonData.Basic,
    val totalCoPayment: Int,
    val approvedAt: Instant? = null,
    val createdAt: Instant = Instant.now(),
    val sentAt: Instant? = null
) : MergeableDecision<VoucherValueDecisionPartSummary, VoucherValueDecisionSummary> {
    override fun withParts(parts: List<VoucherValueDecisionPartSummary>) = this.copy(parts = parts)
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class VoucherValueDecisionPartSummary(
    val child: PersonData.Basic
) : FinanceDecisionPart
