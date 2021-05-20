// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.domain

import com.fasterxml.jackson.annotation.JsonProperty
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.domain.DateRange
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.json.Json
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class FeeDecision2(
    override val id: UUID,
    val children: List<FeeDecisionChild>,
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
    val pricing: Pricing,
    val documentKey: String? = null,
    @Nested("approved_by")
    val approvedBy: PersonData.JustId? = null,
    val approvedAt: Instant? = null,
    @Nested("decision_handler")
    val decisionHandler: PersonData.JustId? = null,
    val sentAt: Instant? = null,
    val created: Instant = Instant.now()
) : FinanceDecision<FeeDecision2> {
    override val validFrom: LocalDate = validDuring.start
    override val validTo: LocalDate? = validDuring.end
    override fun withRandomId() = this.copy(id = UUID.randomUUID())
    override fun withValidity(period: DateRange) = this.copy(validDuring = period)
    override fun contentEquals(decision: FeeDecision2): Boolean {
        return this.children.toSet() == decision.children.toSet() &&
            this.headOfFamily == decision.headOfFamily &&
            this.partner == decision.partner &&
            this.headOfFamilyIncome == decision.headOfFamilyIncome &&
            this.partnerIncome == decision.partnerIncome &&
            this.familySize == decision.familySize &&
            this.pricing == decision.pricing
    }

    override fun isAnnulled(): Boolean = this.status == FeeDecisionStatus.ANNULLED
    override fun isEmpty(): Boolean = this.children.isEmpty()
    override fun annul() = this.copy(status = FeeDecisionStatus.ANNULLED)

    @JsonProperty("totalFee")
    fun totalFee(): Int = children.fold(0) { sum, child -> sum + child.finalFee }
}

fun Iterable<FeeDecision2>.merge(): List<FeeDecision2> {
    val map = mutableMapOf<UUID, FeeDecision2>()
    for (decision in this) {
        val id = decision.id
        if (map.containsKey(id)) {
            val existing = map.getValue(id)
            map[id] = existing.copy(children = existing.children + decision.children)
        } else {
            map[id] = decision
        }
    }
    return map.values.toList()
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
    val descriptionSv: String
)
