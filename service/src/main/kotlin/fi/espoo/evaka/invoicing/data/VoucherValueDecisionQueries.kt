// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.data

import fi.espoo.evaka.invoicing.controller.SortDirection
import fi.espoo.evaka.invoicing.controller.VoucherValueDecisionSortParam
import fi.espoo.evaka.invoicing.domain.VoucherValueDecision
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionDetailed
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionSummary
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.VoucherValueDecisionId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.bindNullable
import fi.espoo.evaka.shared.db.freeTextSearchQuery
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.mapToPaged
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo
import java.time.LocalDate

fun Database.Transaction.upsertValueDecisions(decisions: List<VoucherValueDecision>) {
    val sql =
        // language=sql
        """
INSERT INTO voucher_value_decision (
    id,
    status,
    valid_from,
    valid_to,
    decision_number,
    decision_type,
    head_of_family_id,
    partner_id,
    head_of_family_income,
    partner_income,
    child_income,
    family_size,
    fee_thresholds,
    child_id,
    child_date_of_birth,
    placement_unit_id,
    placement_type,
    service_need_fee_coefficient,
    service_need_voucher_value_coefficient,
    service_need_fee_description_fi,
    service_need_fee_description_sv,
    service_need_voucher_value_description_fi,
    service_need_voucher_value_description_sv,
    base_co_payment,
    sibling_discount,
    co_payment,
    fee_alterations,
    final_co_payment,
    base_value,
    capacity_factor,
    voucher_value
) VALUES (
    :id,
    :status::voucher_value_decision_status,
    :validFrom,
    :validTo,
    :decisionNumber,
    :decisionType::voucher_value_decision_type,
    :headOfFamilyId,
    :partnerId,
    :headOfFamilyIncome,
    :partnerIncome,
    :childIncome,
    :familySize,
    :feeThresholds,
    :childId,
    :childDateOfBirth,
    :placementUnitId,
    :placementType::placement_type,
    :serviceNeedFeeCoefficient,
    :serviceNeedVoucherValueCoefficient,
    :serviceNeedFeeDescriptionFi,
    :serviceNeedFeeDescriptionSv,
    :serviceNeedVoucherValueDescriptionFi,
    :serviceNeedVoucherValueDescriptionSv,
    :baseCoPayment,
    :siblingDiscount,
    :coPayment,
    :feeAlterations,
    :finalCoPayment,
    :baseValue,
    :capacityFactor,
    :voucherValue
) ON CONFLICT (id) DO UPDATE SET
    status = :status::voucher_value_decision_status,
    decision_number = :decisionNumber,
    valid_from = :validFrom,
    valid_to = :validTo,
    head_of_family_id = :headOfFamilyId,
    partner_id = :partnerId,
    head_of_family_income = :headOfFamilyIncome,
    partner_income = :partnerIncome,
    child_income = :childIncome,
    family_size = :familySize,
    fee_thresholds = :feeThresholds,
    child_id = :childId,
    child_date_of_birth = :childDateOfBirth,
    placement_unit_id = :placementUnitId,
    placement_type = :placementType::placement_type,
    service_need_fee_coefficient = :serviceNeedFeeCoefficient,
    service_need_voucher_value_coefficient = :serviceNeedVoucherValueCoefficient,
    service_need_fee_description_fi = :serviceNeedFeeDescriptionFi,
    service_need_fee_description_sv = :serviceNeedFeeDescriptionSv,
    service_need_voucher_value_description_fi = :serviceNeedVoucherValueDescriptionFi,
    service_need_voucher_value_description_sv = :serviceNeedVoucherValueDescriptionSv,
    base_co_payment = :baseCoPayment,
    sibling_discount = :siblingDiscount,
    co_payment = :coPayment,
    fee_alterations = :feeAlterations,
    final_co_payment = :finalCoPayment,
    base_value = :baseValue,
    capacity_factor = :capacityFactor,
    voucher_value = :voucherValue
"""

    decisions.forEach { decision ->
        // TODO: use batch once JDBI makes it less buggy
        createUpdate(sql)
            .bindKotlin(decision)
            .bind("headOfFamilyId", decision.headOfFamilyId)
            .bind("partnerId", decision.partnerId)
            .bind("childId", decision.child.id)
            .bind("childDateOfBirth", decision.child.dateOfBirth)
            .bind("placementUnitId", decision.placement.unitId)
            .bind("placementType", decision.placement.type)
            .bind("serviceNeedFeeCoefficient", decision.serviceNeed.feeCoefficient)
            .bind("serviceNeedVoucherValueCoefficient", decision.serviceNeed.voucherValueCoefficient)
            .bind("serviceNeedFeeDescriptionFi", decision.serviceNeed.feeDescriptionFi)
            .bind("serviceNeedFeeDescriptionSv", decision.serviceNeed.feeDescriptionFi)
            .bind("serviceNeedVoucherValueDescriptionFi", decision.serviceNeed.voucherValueDescriptionFi)
            .bind("serviceNeedVoucherValueDescriptionSv", decision.serviceNeed.voucherValueDescriptionFi)
            .bind("sentAt", decision.sentAt)
            .execute()
    }
}

fun Database.Read.getValueDecisionsByIds(ids: List<VoucherValueDecisionId>): List<VoucherValueDecision> {
    return createQuery(
        """
SELECT
    id,
    valid_from,
    valid_to,
    head_of_family_id,
    status,
    decision_number,
    decision_type,
    partner_id,
    head_of_family_income,
    partner_income,
    child_income,
    family_size,
    fee_thresholds,
    child_id,
    child_date_of_birth,
    placement_type,
    placement_unit_id,
    service_need_fee_coefficient,
    service_need_voucher_value_coefficient,
    service_need_fee_description_fi,
    service_need_fee_description_sv,
    service_need_voucher_value_description_fi,
    service_need_voucher_value_description_sv,
    base_co_payment,
    sibling_discount,
    co_payment,
    fee_alterations,
    final_co_payment,
    base_value,
    capacity_factor,
    voucher_value,
    document_key,
    approved_at,
    approved_by,
    sent_at,
    created
FROM voucher_value_decision
WHERE id = ANY(:ids)
        """.trimIndent()
    )
        .bind("ids", ids.toTypedArray())
        .mapTo<VoucherValueDecision>()
        .toList()
}

fun Database.Read.findValueDecisionsForChild(
    childId: ChildId,
    period: DateRange?,
    statuses: List<VoucherValueDecisionStatus>?
): List<VoucherValueDecision> {
    // language=sql
    val sql =
        """
SELECT
    id,
    valid_from,
    valid_to,
    head_of_family_id,
    status,
    decision_number,
    decision_type,
    partner_id,
    head_of_family_income,
    partner_income,
    child_income,
    family_size,
    fee_thresholds,
    child_id,
    child_date_of_birth,
    placement_type,
    placement_unit_id,
    service_need_fee_coefficient,
    service_need_voucher_value_coefficient,
    service_need_fee_description_fi,
    service_need_fee_description_sv,
    service_need_voucher_value_description_fi,
    service_need_voucher_value_description_sv,
    base_co_payment,
    sibling_discount,
    co_payment,
    fee_alterations,
    final_co_payment,
    base_value,
    capacity_factor,
    voucher_value,
    document_key,
    approved_at,
    approved_by,
    sent_at,
    created
FROM voucher_value_decision
WHERE child_id = :childId
AND (:period::daterange IS NULL OR daterange(valid_from, valid_to, '[]') && :period)
AND (:statuses::text[] IS NULL OR status = ANY(:statuses::voucher_value_decision_status[]))
"""

    return createQuery(sql)
        .bind("childId", childId)
        .bindNullable("period", period)
        .bindNullable("statuses", statuses)
        .mapTo<VoucherValueDecision>()
        .toList()
}

fun Database.Transaction.deleteValueDecisions(ids: List<VoucherValueDecisionId>) {
    if (ids.isEmpty()) return

    createUpdate("DELETE FROM voucher_value_decision WHERE id = ANY(:ids)")
        .bind("ids", ids.toTypedArray())
        .execute()
}

fun Database.Read.searchValueDecisions(
    page: Int,
    pageSize: Int,
    sortBy: VoucherValueDecisionSortParam,
    sortDirection: SortDirection,
    status: VoucherValueDecisionStatus,
    areas: List<String>,
    unit: DaycareId?,
    searchTerms: String = "",
    startDate: LocalDate?,
    endDate: LocalDate?,
    searchByStartDate: Boolean = false,
    financeDecisionHandlerId: EmployeeId?
): Paged<VoucherValueDecisionSummary> {
    val sortColumn = when (sortBy) {
        VoucherValueDecisionSortParam.HEAD_OF_FAMILY -> "head.last_name"
        VoucherValueDecisionSortParam.STATUS -> "decision.status"
    }

    val params = mapOf(
        "page" to page,
        "pageSize" to pageSize,
        "status" to status.name,
        "areas" to areas.toTypedArray(),
        "unit" to unit,
        "start_date" to startDate,
        "end_date" to endDate,
        "financeDecisionHandlerId" to financeDecisionHandlerId
    )

    val (freeTextQuery, freeTextParams) = freeTextSearchQuery(listOf("head", "partner", "child"), searchTerms)

    val conditions = listOfNotNull(
        if (areas.isNotEmpty()) "area.short_name = ANY(:areas)" else null,
        if (unit != null) "decision.placement_unit_id = :unit" else null,
        if ((startDate != null || endDate != null) && !searchByStartDate) "daterange(:start_date, :end_date, '[]') && daterange(valid_from, valid_to, '[]')" else null,
        if ((startDate != null || endDate != null) && searchByStartDate) "daterange(:start_date, :end_date, '[]') @> valid_from" else null,
        if (financeDecisionHandlerId != null) "placement_unit.finance_decision_handler = :financeDecisionHandlerId" else null
    )
    val sql =
        // language=sql
        """
SELECT
    count(*) OVER () AS count,
    decision.id,
    decision.status,
    decision.decision_number,
    decision.valid_from,
    decision.valid_to,
    decision.head_of_family_id AS head_id,
    decision.child_id,
    decision.child_date_of_birth,
    decision.final_co_payment,
    decision.voucher_value,
    decision.approved_at,
    decision.sent_at,
    decision.created,
    head.date_of_birth AS head_date_of_birth,
    head.first_name AS head_first_name,
    head.last_name AS head_last_name,
    head.social_security_number AS head_ssn,
    head.force_manual_fee_decisions AS head_force_manual_fee_decisions,
    child.first_name AS child_first_name,
    child.last_name AS child_last_name,
    child.social_security_number AS child_ssn
FROM voucher_value_decision AS decision
LEFT JOIN person AS head ON decision.head_of_family_id = head.id
LEFT JOIN person AS child ON decision.child_id = child.id
LEFT JOIN person AS partner ON decision.partner_id = partner.id
LEFT JOIN daycare AS placement_unit ON placement_unit.id = decision.placement_unit_id
LEFT JOIN care_area AS area ON placement_unit.care_area_id = area.id
WHERE
    decision.status = :status::voucher_value_decision_status
    AND $freeTextQuery
    ${if (conditions.isNotEmpty()) """AND ${conditions.joinToString("\nAND ")}
        """.trimIndent() else ""}
ORDER BY $sortColumn $sortDirection, decision.id DESC
LIMIT :pageSize OFFSET :pageSize * :page
"""
    return this.createQuery(sql)
        .bindMap(params + freeTextParams)
        .mapToPaged(pageSize)
}

fun Database.Read.getVoucherValueDecision(id: VoucherValueDecisionId): VoucherValueDecisionDetailed? {
    // language=sql
    val sql =
        """
SELECT
    decision.*,
    date_part('year', age(decision.valid_from, decision.child_date_of_birth)) child_age,
    head.id as head_id,
    head.date_of_birth as head_date_of_birth,
    head.first_name as head_first_name,
    head.last_name as head_last_name,
    head.social_security_number as head_ssn,
    head.street_address as head_street_address,
    head.postal_code as head_postal_code,
    head.post_office as head_post_office,
    head.language as head_language,
    head.restricted_details_enabled as head_restricted_details_enabled,
    head.force_manual_fee_decisions as head_force_manual_fee_decisions,
    partner.date_of_birth as partner_date_of_birth,
    partner.first_name as partner_first_name,
    partner.last_name as partner_last_name,
    partner.social_security_number as partner_ssn,
    partner.street_address as partner_street_address,
    partner.postal_code as partner_postal_code,
    partner.post_office as partner_post_office,
    partner.restricted_details_enabled as partner_restricted_details_enabled,
    approved_by.id as approved_by_id,
    approved_by.first_name as approved_by_first_name,
    approved_by.last_name as approved_by_last_name,
    child.first_name as child_first_name,
    child.last_name as child_last_name,
    child.social_security_number as child_ssn,
    child.street_address as child_address,
    child.postal_code as child_postal_code,
    child.post_office as child_post_office,
    child.restricted_details_enabled as child_restricted_details_enabled,
    daycare.name as placement_unit_name,
    daycare.language as placement_unit_language,
    care_area.id as placement_unit_area_id,
    care_area.name as placement_unit_area_name,
    finance_decision_handler.first_name as finance_decision_handler_first_name,
    finance_decision_handler.last_name AS finance_decision_handler_last_name
FROM voucher_value_decision as decision
JOIN person as head ON decision.head_of_family_id = head.id
LEFT JOIN person as partner ON decision.partner_id = partner.id
JOIN person as child ON decision.child_id = child.id
JOIN daycare ON decision.placement_unit_id = daycare.id
JOIN care_area ON daycare.care_area_id = care_area.id
LEFT JOIN employee as approved_by ON decision.approved_by = approved_by.id
LEFT JOIN employee as finance_decision_handler ON finance_decision_handler.id = decision.decision_handler
WHERE decision.id = :id
"""

    return createQuery(sql)
        .bind("id", id)
        .mapTo<VoucherValueDecisionDetailed>()
        .singleOrNull()?.let {
            it.copy(isElementaryFamily = isElementaryFamily(it.headOfFamily.id, it.partner?.id, listOf(it.child.id)))
        }
}

fun Database.Read.getHeadOfFamilyVoucherValueDecisions(headOfFamilyId: PersonId): List<VoucherValueDecisionSummary> {
    return createQuery(
        """
SELECT
    decision.id,
    decision.status,
    decision.decision_number,
    decision.valid_from,
    decision.valid_to,
    decision.head_of_family_id AS head_id,
    decision.child_id,
    decision.child_date_of_birth,
    decision.final_co_payment,
    decision.voucher_value,
    decision.approved_at,
    decision.sent_at,
    decision.created,
    head.date_of_birth AS head_date_of_birth,
    head.first_name AS head_first_name,
    head.last_name AS head_last_name,
    head.social_security_number AS head_ssn,
    head.force_manual_fee_decisions AS head_force_manual_fee_decisions,
    child.first_name AS child_first_name,
    child.last_name AS child_last_name,
    child.social_security_number AS child_ssn
FROM voucher_value_decision decision
JOIN person head ON decision.head_of_family_id = head.id
JOIN person child ON decision.child_id = child.id
WHERE decision.head_of_family_id = :headOfFamilyId
"""
    )
        .bind("headOfFamilyId", headOfFamilyId)
        .mapTo<VoucherValueDecisionSummary>()
        .toList()
}

fun Database.Transaction.approveValueDecisionDraftsForSending(
    ids: List<VoucherValueDecisionId>,
    approvedBy: EmployeeId,
    approvedAt: HelsinkiDateTime,
    alwaysUseDaycareFinanceDecisionHandler: Boolean
) {
    // language=sql
    val sql =
        """
        UPDATE voucher_value_decision SET
            status = :status::voucher_value_decision_status,
            decision_number = nextval('voucher_value_decision_number_sequence'),
            approved_by = :approvedBy,
            decision_handler = (CASE
                WHEN daycare.finance_decision_handler IS NOT NULL AND :alwaysUseDaycareFinanceDecisionHandler = true THEN daycare.finance_decision_handler
                WHEN daycare.finance_decision_handler IS NOT NULL AND vd.decision_type = 'NORMAL' THEN daycare.finance_decision_handler
                ELSE :approvedBy
            END),
            approved_at = :approvedAt
        FROM voucher_value_decision AS vd
        JOIN daycare ON vd.placement_unit_id = daycare.id
        WHERE vd.id = :id AND voucher_value_decision.id = vd.id
        """.trimIndent()

    val batch = prepareBatch(sql)
    ids.forEach { id ->
        batch
            .bind("status", VoucherValueDecisionStatus.WAITING_FOR_SENDING)
            .bind("approvedBy", approvedBy)
            .bind("approvedAt", approvedAt)
            .bind("id", id)
            .bind("alwaysUseDaycareFinanceDecisionHandler", alwaysUseDaycareFinanceDecisionHandler)
            .add()
    }
    batch.execute()
}

fun Database.Read.getVoucherValueDecisionDocumentKey(id: VoucherValueDecisionId): String? {
    // language=sql
    val sql = "SELECT document_key FROM voucher_value_decision WHERE id = :id"

    return createQuery(sql)
        .bind("id", id)
        .mapTo<String>()
        .singleOrNull()
}

fun Database.Transaction.updateVoucherValueDecisionDocumentKey(id: VoucherValueDecisionId, documentKey: String) {
    // language=sql
    val sql = "UPDATE voucher_value_decision SET document_key = :documentKey WHERE id = :id"

    createUpdate(sql)
        .bind("id", id)
        .bind("documentKey", documentKey)
        .execute()
}

fun Database.Transaction.updateVoucherValueDecisionStatus(
    ids: List<VoucherValueDecisionId>,
    status: VoucherValueDecisionStatus
) {
    // language=sql
    val sql = "UPDATE voucher_value_decision SET status = :status::voucher_value_decision_status WHERE id = ANY(:ids)"

    createUpdate(sql)
        .bind("ids", ids.toTypedArray())
        .bind("status", status)
        .execute()
}

fun Database.Transaction.setVoucherValueDecisionType(id: VoucherValueDecisionId, type: VoucherValueDecisionType) {
    //language=SQL
    val sql =
        """
        UPDATE voucher_value_decision
        SET decision_type = :type::voucher_value_decision_type
        WHERE id = :id
          AND status = :requiredStatus::voucher_value_decision_status
    """

    createUpdate(sql)
        .bind("id", id)
        .bind("type", type.toString())
        .bind("requiredStatus", VoucherValueDecisionStatus.DRAFT.toString())
        .execute()
}

fun Database.Transaction.markVoucherValueDecisionsSent(ids: List<VoucherValueDecisionId>, now: HelsinkiDateTime) {
    createUpdate("UPDATE voucher_value_decision SET status = :sent::voucher_value_decision_status, sent_at = :now WHERE id = ANY(:ids)")
        .bind("ids", ids.toTypedArray())
        .bind("sent", VoucherValueDecisionStatus.SENT)
        .bind("now", now)
        .execute()
}

fun Database.Transaction.updateVoucherValueDecisionEndDates(
    updatedDecisions: List<VoucherValueDecision>,
    now: HelsinkiDateTime
) {
    prepareBatch("UPDATE voucher_value_decision SET valid_to = :validTo, validity_updated_at = :now WHERE id = :id")
        .also { batch ->
            updatedDecisions.forEach { decision ->
                batch
                    .bind("id", decision.id)
                    .bind("validTo", decision.validTo)
                    .bind("now", now)
                    .add()
            }
        }
        .execute()
}

fun Database.Transaction.annulVoucherValueDecisions(ids: List<VoucherValueDecisionId>, now: HelsinkiDateTime) {
    if (ids.isEmpty()) return

    createUpdate("UPDATE voucher_value_decision SET status = :status, annulled_at = :now WHERE id = ANY(:ids)")
        .bind("ids", ids.toTypedArray())
        .bind("status", VoucherValueDecisionStatus.ANNULLED)
        .bind("now", now)
        .execute()
}

fun Database.Transaction.lockValueDecisionsForChild(childId: ChildId) {
    createUpdate("SELECT id FROM voucher_value_decision WHERE child_id = :childId FOR UPDATE")
        .bind("childId", childId)
        .execute()
}

fun Database.Transaction.lockValueDecisions(ids: List<VoucherValueDecisionId>) {
    createUpdate("SELECT id FROM voucher_value_decision WHERE id = ANY(:ids) FOR UPDATE")
        .bind("ids", ids.toTypedArray())
        .execute()
}
