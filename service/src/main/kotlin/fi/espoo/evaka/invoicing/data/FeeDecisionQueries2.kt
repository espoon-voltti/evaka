// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.data

import fi.espoo.evaka.invoicing.domain.FeeDecision2
import fi.espoo.evaka.invoicing.domain.FeeDecisionChild
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.merge
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo
import java.util.UUID

val feeDecisionQueryBase2 =
    """
    SELECT
        decision.*,
        child.child_id,
        child.child_date_of_birth,
        child.placement_unit_id,
        child.placement_type,
        child.service_need_option_id,
        child.service_need_fee_coefficient,
        child.base_fee,
        child.sibling_discount,
        child.fee,
        child.fee_alterations,
        child.final_fee
    FROM new_fee_decision as decision
    LEFT JOIN new_fee_decision_child child ON decision.id = child.fee_decision_id
    """.trimIndent()

fun Database.Transaction.upsertFeeDecisions2(decisions: List<FeeDecision2>) {
    upsertDecisions2(decisions)
    replaceChildren2(decisions)
}

private fun Database.Transaction.upsertDecisions2(decisions: List<FeeDecision2>) {
    val sql =
        """
        INSERT INTO new_fee_decision (
            id,
            status,
            decision_number,
            decision_type,
            valid_during,
            head_of_family_id,
            partner_id,
            head_of_family_income,
            partner_income,
            family_size,
            pricing
        ) VALUES (
            :id,
            :status::fee_decision_status,
            :decisionNumber,
            :decisionType::fee_decision_type,
            :validDuring,
            :headOfFamilyId,
            :partnerId,
            :headOfFamilyIncome,
            :partnerIncome,
            :familySize,
            :pricing
        ) ON CONFLICT (id) DO UPDATE SET
            status = :status::fee_decision_status,
            decision_number = :decisionNumber,
            decision_type = :decisionType::fee_decision_type,
            valid_during = :validDuring,
            head_of_family_id = :headOfFamilyId,
            partner_id = :partnerId,
            head_of_family_income = :headOfFamilyIncome,
            partner_income = :partnerIncome,
            family_size = :familySize,
            pricing = :pricing
    """

    val batch = prepareBatch(sql)
    decisions.forEach { decision ->
        batch
            .bindKotlin(decision)
            .bind("headOfFamilyId", decision.headOfFamily.id)
            .bind("partnerId", decision.partner?.id)
            .add()
    }
    batch.execute()
}

private fun Database.Transaction.replaceChildren2(decisions: List<FeeDecision2>) {
    val partsWithDecisionIds = decisions.map { it.id to it.children }
    deleteChildren2(partsWithDecisionIds.map { it.first })
    insertChildren2(partsWithDecisionIds)
}

private fun Database.Transaction.insertChildren2(decisions: List<Pair<UUID, List<FeeDecisionChild>>>) {
    val sql =
        """
        INSERT INTO new_fee_decision_child (
            id,
            fee_decision_id,
            child_id,
            child_date_of_birth,
            placement_unit_id,
            placement_type,
            service_need_option_id,
            service_need_fee_coefficient,
            base_fee,
            sibling_discount,
            fee,
            fee_alterations,
            final_fee
        ) VALUES (
            :id,
            :feeDecisionId,
            :childId,
            :childDateOfBirth,
            :placementUnitId,
            :placementType,
            :serviceNeedOptionId,
            :serviceNeedFeeCoefficient,
            :baseFee,
            :siblingDiscount,
            :fee,
            :feeAlterations,
            :finalFee
        )
    """

    val batch = prepareBatch(sql)
    decisions.forEach { (decisionId, children) ->
        children.forEach { child ->
            batch
                .bindKotlin(child)
                .bind("id", UUID.randomUUID())
                .bind("feeDecisionId", decisionId)
                .bind("childId", child.child.id)
                .bind("childDateOfBirth", child.child.dateOfBirth)
                .bind("placementUnitId", child.placement.unit.id)
                .bind("placementType", child.placement.type)
                .bind("serviceNeedOptionId", child.serviceNeed.optionId)
                .bind("serviceNeedFeeCoefficient", child.serviceNeed.feeCoefficient)
                .add()
        }
    }
    batch.execute()
}

private fun Database.Transaction.deleteChildren2(decisionIds: List<UUID>) {
    if (decisionIds.isEmpty()) return

    createUpdate("DELETE FROM new_fee_decision_child WHERE fee_decision_id = ANY(:decisionIds)")
        .bind("decisionIds", decisionIds.toTypedArray())
        .execute()
}

fun Database.Transaction.deleteFeeDecisions2(ids: List<UUID>) {
    if (ids.isEmpty()) return

    createUpdate("DELETE FROM new_fee_decision WHERE id = ANY(:ids)")
        .bind("ids", ids.toTypedArray())
        .execute()
}

fun Database.Read.findFeeDecisionsForHeadOfFamily2(
    headOfFamilyId: UUID,
    period: DateRange?,
    status: List<FeeDecisionStatus>?
): List<FeeDecision2> {
    val sql =
        """
        $feeDecisionQueryBase2
        WHERE
            decision.head_of_family_id = :headOfFamilyId
            ${period?.let { "AND decision.valid_during && daterange(:periodStart, :periodEnd, '[]')" } ?: ""}
            ${status?.let { "AND decision.status = ANY(:status::fee_decision_status[])" } ?: ""}
    """

    return createQuery(sql)
        .bind("headOfFamilyId", headOfFamilyId)
        .let { query ->
            if (period != null) query.bind("period", period)
            else query
        }
        .let { query -> if (status != null) query.bind("status", status.map { it.name }.toTypedArray()) else query }
        .mapTo<FeeDecision2>()
        .merge()
}

fun Database.Transaction.lockFeeDecisionsForHeadOfFamily2(headOfFamily: UUID) {
    createUpdate("SELECT id FROM new_fee_decision WHERE head_of_family_id = :headOfFamily FOR UPDATE")
        .bind("headOfFamily", headOfFamily)
        .execute()
}
