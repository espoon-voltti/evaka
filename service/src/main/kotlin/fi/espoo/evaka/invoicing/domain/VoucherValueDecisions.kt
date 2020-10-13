// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class VoucherValueDecision(
    override val id: UUID,
    override val parts: List<VoucherValueDecisionPart>,
    val status: VoucherValueDecisionStatus,
    val validFrom: LocalDate,
    val validTo: LocalDate?,
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
) : FinanceDecision<VoucherValueDecisionPart> {
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
    val status: VoucherValueDecisionStatus,
    val validFrom: LocalDate,
    val validTo: LocalDate?,
    val decisionNumber: Long? = null,
    val headOfFamily: PersonData.Basic,
    val totalCoPayment: Int,
    val approvedAt: Instant? = null,
    val createdAt: Instant = Instant.now(),
    val sentAt: Instant? = null
) : FinanceDecision<VoucherValueDecisionPartSummary>

@JsonIgnoreProperties(ignoreUnknown = true)
data class VoucherValueDecisionPartSummary(
    val child: PersonData.Basic
) : FinanceDecisionPart
