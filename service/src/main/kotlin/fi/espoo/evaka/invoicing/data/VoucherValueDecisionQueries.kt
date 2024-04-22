// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.data

import fi.espoo.evaka.invoicing.controller.SortDirection
import fi.espoo.evaka.invoicing.controller.VoucherValueDecisionDistinctiveParams
import fi.espoo.evaka.invoicing.controller.VoucherValueDecisionSortParam
import fi.espoo.evaka.invoicing.domain.VoucherValueDecision
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionDetailed
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionDifference
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionSummary
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.VoucherValueDecisionId
import fi.espoo.evaka.shared.db.Binding
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.freeTextSearchQuery
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.mapToPaged
import java.time.LocalDate

fun Database.Transaction.upsertValueDecisions(decisions: List<VoucherValueDecision>) {
    decisions.forEach { decision ->
        // TODO: use batch once JDBI makes it less buggy
        createUpdate {
                sql(
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
    service_need_missing,
    base_co_payment,
    sibling_discount,
    co_payment,
    fee_alterations,
    final_co_payment,
    base_value,
    assistance_need_coefficient,
    voucher_value,
    difference,
    created
) VALUES (
    ${bind(decision.id)},
    ${bind(decision.status)}::voucher_value_decision_status,
    ${bind(decision.validFrom)},
    ${bind(decision.validTo)},
    ${bind(decision.decisionNumber)},
    ${bind(decision.decisionType)}::voucher_value_decision_type,
    ${bind(decision.headOfFamilyId)},
    ${bind(decision.partnerId)},
    ${bindJson(decision.headOfFamilyIncome)},
    ${bindJson(decision.partnerIncome)},
    ${bindJson(decision.childIncome)},
    ${bind(decision.familySize)},
    ${bindJson(decision.feeThresholds)},
    ${bind(decision.child.id)},
    ${bind(decision.child.dateOfBirth)},
    ${bind(decision.placement?.unitId)},
    ${bind(decision.placement?.type)}::placement_type,
    ${bind(decision.serviceNeed?.feeCoefficient)},
    ${bind(decision.serviceNeed?.voucherValueCoefficient)},
    ${bind(decision.serviceNeed?.feeDescriptionFi)},
    ${bind(decision.serviceNeed?.feeDescriptionSv)},
    ${bind(decision.serviceNeed?.voucherValueDescriptionFi)},
    ${bind(decision.serviceNeed?.voucherValueDescriptionSv)},
    ${bind(decision.serviceNeed?.missing ?: false)},
    ${bind(decision.baseCoPayment)},
    ${bind(decision.siblingDiscount)},
    ${bind(decision.coPayment)},
    ${bindJson(decision.feeAlterations)},
    ${bind(decision.finalCoPayment)},
    ${bind(decision.baseValue)},
    ${bind(decision.assistanceNeedCoefficient)},
    ${bind(decision.voucherValue)},
    ${bind(decision.difference)},
    ${bind(decision.created)}
) ON CONFLICT (id) DO UPDATE SET
    status = ${bind(decision.status)},
    decision_number = ${bind(decision.decisionNumber)},
    valid_from = ${bind(decision.validFrom)},
    valid_to = ${bind(decision.validTo)},
    head_of_family_id = ${bind(decision.headOfFamilyId)},
    partner_id = ${bind(decision.partnerId)},
    head_of_family_income = ${bindJson(decision.headOfFamilyIncome)},
    partner_income = ${bindJson(decision.partnerIncome)},
    child_income = ${bindJson(decision.childIncome)},
    family_size = ${bind(decision.familySize)},
    fee_thresholds = ${bindJson(decision.feeThresholds)},
    child_id = ${bind(decision.child.id)},
    child_date_of_birth = ${bind(decision.child.dateOfBirth)},
    placement_unit_id = ${bind(decision.placement?.unitId)},
    placement_type = ${bind(decision.placement?.type)}::placement_type,
    service_need_fee_coefficient = ${bind(decision.serviceNeed?.feeCoefficient)},
    service_need_voucher_value_coefficient = ${bind(decision.serviceNeed?.voucherValueCoefficient)},
    service_need_fee_description_fi = ${bind(decision.serviceNeed?.feeDescriptionFi)},
    service_need_fee_description_sv = ${bind(decision.serviceNeed?.feeDescriptionSv)},
    service_need_voucher_value_description_fi = ${bind(decision.serviceNeed?.voucherValueDescriptionFi)},
    service_need_voucher_value_description_sv = ${bind(decision.serviceNeed?.voucherValueDescriptionSv)},
    service_need_missing = ${bind(decision.serviceNeed?.missing ?: false)},
    base_co_payment = ${bind(decision.baseCoPayment)},
    sibling_discount = ${bind(decision.siblingDiscount)},
    co_payment = ${bind(decision.coPayment)},
    fee_alterations = ${bindJson(decision.feeAlterations)},
    final_co_payment = ${bind(decision.finalCoPayment)},
    base_value = ${bind(decision.baseValue)},
    assistance_need_coefficient = ${bind(decision.assistanceNeedCoefficient)},
    voucher_value = ${bind(decision.voucherValue)},
    difference = ${bind(decision.difference)},
    created = ${bind(decision.created)}
"""
                )
            }
            .execute()
    }
}

fun Database.Read.getValueDecisionsByIds(
    ids: List<VoucherValueDecisionId>
): List<VoucherValueDecision> {
    return createQuery {
            sql(
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
    service_need_missing,
    base_co_payment,
    sibling_discount,
    co_payment,
    fee_alterations,
    final_co_payment,
    base_value,
    assistance_need_coefficient,
    voucher_value,
    difference,
    document_key,
    approved_at,
    approved_by,
    sent_at,
    created
FROM voucher_value_decision
WHERE id = ANY(${bind(ids)})
        """
            )
        }
        .toList<VoucherValueDecision>()
}

fun Database.Read.findValueDecisionsForChild(
    childId: ChildId,
    period: DateRange? = null,
    statuses: List<VoucherValueDecisionStatus>? = null,
    lockForUpdate: Boolean = false
): List<VoucherValueDecision> {
    return createQuery {
            sql(
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
    service_need_missing,
    base_co_payment,
    sibling_discount,
    co_payment,
    fee_alterations,
    final_co_payment,
    base_value,
    assistance_need_coefficient,
    voucher_value,
    difference,
    document_key,
    approved_at,
    approved_by,
    sent_at,
    created
FROM voucher_value_decision
WHERE child_id = ${bind(childId)}
AND (${bind(period)}::daterange IS NULL OR daterange(valid_from, valid_to, '[]') && ${bind(period)})
AND (${bind(statuses)}::text[] IS NULL OR status = ANY(${bind(statuses)}::voucher_value_decision_status[]))
${if (lockForUpdate) "FOR UPDATE" else ""}
"""
            )
        }
        .toList<VoucherValueDecision>()
}

fun Database.Transaction.deleteValueDecisions(ids: List<VoucherValueDecisionId>) {
    if (ids.isEmpty()) return

    createUpdate { sql("DELETE FROM voucher_value_decision WHERE id = ANY(${bind(ids)})") }
        .execute()
}

data class PagedVoucherValueDecisionSummaries(
    val data: List<VoucherValueDecisionSummary>,
    val total: Int,
    val pages: Int,
)

fun Database.Read.searchValueDecisions(
    evakaClock: EvakaClock,
    postOffice: String,
    page: Int,
    pageSize: Int,
    sortBy: VoucherValueDecisionSortParam,
    sortDirection: SortDirection,
    statuses: List<VoucherValueDecisionStatus>,
    areas: List<String>,
    unit: DaycareId?,
    searchTerms: String = "",
    startDate: LocalDate?,
    endDate: LocalDate?,
    searchByStartDate: Boolean = false,
    financeDecisionHandlerId: EmployeeId?,
    difference: Set<VoucherValueDecisionDifference>,
    distinctiveParams: List<VoucherValueDecisionDistinctiveParams>
): PagedVoucherValueDecisionSummaries {
    val sortColumn =
        when (sortBy) {
            VoucherValueDecisionSortParam.HEAD_OF_FAMILY -> "head.last_name"
            VoucherValueDecisionSortParam.CHILD -> "child.last_name"
            VoucherValueDecisionSortParam.VALIDITY -> "decision.valid_from"
            VoucherValueDecisionSortParam.VOUCHER_VALUE -> "decision.voucher_value"
            VoucherValueDecisionSortParam.FINAL_CO_PAYMENT -> "decision.final_co_payment"
            VoucherValueDecisionSortParam.NUMBER -> "decision.decision_number"
            VoucherValueDecisionSortParam.CREATED -> "decision.created"
            VoucherValueDecisionSortParam.SENT -> "decision.sent_at"
            VoucherValueDecisionSortParam.STATUS -> "decision.status"
        }

    val params =
        listOf(
            Binding.of("page", page),
            Binding.of("pageSize", pageSize),
            Binding.of("status", statuses),
            Binding.of("areas", areas),
            Binding.of("unit", unit),
            Binding.of("postOffice", postOffice),
            Binding.of("start_date", startDate),
            Binding.of("end_date", endDate),
            Binding.of("financeDecisionHandlerId", financeDecisionHandlerId),
            Binding.of("difference", difference),
            Binding.of("firstPlacementStartDate", evakaClock.now().toLocalDate().withDayOfMonth(1)),
            Binding.of("now", evakaClock.now())
        )

    val (freeTextQuery, freeTextParams) =
        freeTextSearchQuery(listOf("head", "partner", "child"), searchTerms)

    val withNullHours =
        distinctiveParams.contains(VoucherValueDecisionDistinctiveParams.UNCONFIRMED_HOURS)

    val havingExternalChildren =
        distinctiveParams.contains(VoucherValueDecisionDistinctiveParams.EXTERNAL_CHILD)

    val retroactiveOnly =
        distinctiveParams.contains(VoucherValueDecisionDistinctiveParams.RETROACTIVE)

    val noStartingPlacements =
        distinctiveParams.contains(VoucherValueDecisionDistinctiveParams.NO_STARTING_PLACEMENTS)

    val maxFeeAccepted =
        distinctiveParams.contains(VoucherValueDecisionDistinctiveParams.MAX_FEE_ACCEPTED)

    val noStartingPlacementsQuery =
        """
NOT EXISTS (            
    SELECT true
    FROM placement p
    JOIN person c ON p.child_id = c.id
    JOIN voucher_value_decision vvd ON c.id = vvd.child_id
    LEFT JOIN placement preceding ON p.child_id = preceding.child_id AND (p.start_date - interval '1 days') = preceding.end_date AND preceding.type != 'CLUB'::placement_type
    WHERE p.start_date >= :firstPlacementStartDate AND preceding.id IS NULL AND p.type != 'CLUB'::placement_type
        AND vvd.id = decision.id
)
        """

    val conditions =
        listOfNotNull(
            if (statuses.isNotEmpty()) "status = ANY(:status::voucher_value_decision_status[])"
            else null,
            if (areas.isNotEmpty()) "area.short_name = ANY(:areas)" else null,
            if (unit != null) "decision.placement_unit_id = :unit" else null,
            if ((startDate != null || endDate != null) && !searchByStartDate)
                "daterange(:start_date, :end_date, '[]') && daterange(valid_from, valid_to, '[]')"
            else null,
            if ((startDate != null || endDate != null) && searchByStartDate)
                "daterange(:start_date, :end_date, '[]') @> valid_from"
            else null,
            if (financeDecisionHandlerId != null)
                "placement_unit.finance_decision_handler = :financeDecisionHandlerId"
            else null,
            if (difference.isNotEmpty()) "decision.difference && :difference" else null,
            if (withNullHours) "decision.service_need_missing" else null,
            if (havingExternalChildren)
                "child.post_office <> '' AND child.post_office NOT ILIKE :postOffice"
            else null,
            if (retroactiveOnly)
                "decision.valid_from < date_trunc('month', COALESCE(decision.approved_at, :now))"
            else null,
            if (noStartingPlacements) noStartingPlacementsQuery else null,
            if (maxFeeAccepted)
                "(decision.head_of_family_income->>'effect' = 'MAX_FEE_ACCEPTED' OR decision.partner_income->>'effect' = 'MAX_FEE_ACCEPTED')"
            else null
        )
    val sql =
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
    decision.difference,
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
    $freeTextQuery
    ${if (conditions.isNotEmpty()) {
            """AND ${conditions.joinToString("\nAND ")}
            """
        } else {
            ""
        }}
ORDER BY $sortColumn $sortDirection, decision.id $sortDirection
LIMIT :pageSize OFFSET :pageSize * :page
"""
    @Suppress("DEPRECATION")
    return this.createQuery(sql)
        .addBindings(params)
        .addBindings(freeTextParams)
        .mapToPaged(::PagedVoucherValueDecisionSummaries, pageSize)
}

fun Database.Read.getVoucherValueDecision(
    id: VoucherValueDecisionId
): VoucherValueDecisionDetailed? {
    return createQuery {
            sql(
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
    coalesce(finance_decision_handler.preferred_first_name, finance_decision_handler.first_name) as finance_decision_handler_first_name,
    finance_decision_handler.last_name AS finance_decision_handler_last_name
FROM voucher_value_decision as decision
JOIN person as head ON decision.head_of_family_id = head.id
LEFT JOIN person as partner ON decision.partner_id = partner.id
JOIN person as child ON decision.child_id = child.id
JOIN daycare ON decision.placement_unit_id = daycare.id
JOIN care_area ON daycare.care_area_id = care_area.id
LEFT JOIN employee as approved_by ON decision.approved_by = approved_by.id
LEFT JOIN employee as finance_decision_handler ON finance_decision_handler.id = decision.decision_handler
WHERE decision.id = ${bind(id)}
"""
            )
        }
        .exactlyOneOrNull<VoucherValueDecisionDetailed>()
        ?.let {
            it.copy(
                partnerIsCodebtor =
                    partnerIsCodebtor(
                        it.headOfFamily.id,
                        it.partner?.id,
                        listOf(it.child.id),
                        DateRange(it.validFrom, it.validTo)
                    )
            )
        }
}

fun Database.Read.getHeadOfFamilyVoucherValueDecisions(
    headOfFamilyId: PersonId
): List<VoucherValueDecisionSummary> {
    return createQuery {
            sql(
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
    decision.difference,
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
WHERE decision.head_of_family_id = ${bind(headOfFamilyId)}
"""
            )
        }
        .toList<VoucherValueDecisionSummary>()
}

fun Database.Transaction.approveValueDecisionDraftsForSending(
    ids: List<VoucherValueDecisionId>,
    approvedBy: EmployeeId,
    approvedAt: HelsinkiDateTime,
    decisionHandlerId: EmployeeId?,
    alwaysUseDaycareFinanceDecisionHandler: Boolean
) {
    executeBatch(ids) {
        sql(
            """
UPDATE voucher_value_decision SET
    status = ${bind(VoucherValueDecisionStatus.WAITING_FOR_SENDING)},
    decision_number = nextval('voucher_value_decision_number_sequence'),
    approved_by = ${bind(approvedBy)},
    decision_handler = (CASE
        WHEN ${bind(decisionHandlerId)} IS NOT NULL THEN ${bind(decisionHandlerId)}
        WHEN daycare.finance_decision_handler IS NOT NULL AND ${bind(alwaysUseDaycareFinanceDecisionHandler)} = true THEN daycare.finance_decision_handler
        WHEN daycare.finance_decision_handler IS NOT NULL AND vd.decision_type = 'NORMAL' THEN daycare.finance_decision_handler
        ELSE ${bind(approvedBy)}
    END),
    approved_at = ${bind(approvedAt)}
FROM voucher_value_decision AS vd
JOIN daycare ON vd.placement_unit_id = daycare.id
WHERE vd.id = ${bind { id -> id }} AND voucher_value_decision.id = vd.id
"""
        )
    }
}

fun Database.Read.getVoucherValueDecisionDocumentKey(id: VoucherValueDecisionId): String? {
    return createQuery {
            sql("SELECT document_key FROM voucher_value_decision WHERE id = ${bind(id)}")
        }
        .exactlyOneOrNull<String>()
}

fun Database.Read.getVoucherValueDecisionByLiableCitizen(
    citizenId: PersonId
): List<VoucherValueDecisionCitizenInfoRow> {
    return createQuery {
            sql(
                """
SELECT vvd.id,
       vvd.valid_from,
       vvd.valid_to,
       vvd.sent_at,
       vvd.head_of_family_id,
       vvd.partner_id,
       vvd.child_id
FROM voucher_value_decision vvd
WHERE vvd.status IN ('SENT')
AND vvd.document_key IS NOT NULL
AND (vvd.head_of_family_id = ${bind(citizenId)}
    OR vvd.partner_id = ${bind(citizenId)})
"""
            )
        }
        .toList<VoucherValueDecisionCitizenInfoRow>()
}

data class VoucherValueDecisionCitizenInfoRow(
    val id: VoucherValueDecisionId,
    val validFrom: LocalDate,
    val validTo: LocalDate?,
    val sentAt: HelsinkiDateTime,
    val headOfFamilyId: PersonId,
    val partnerId: PersonId?,
    val childId: PersonId
)

fun Database.Transaction.updateVoucherValueDecisionDocumentKey(
    id: VoucherValueDecisionId,
    documentKey: String
) {
    createUpdate {
            sql(
                "UPDATE voucher_value_decision SET document_key = ${bind(documentKey)} WHERE id = ${bind(id)}"
            )
        }
        .execute()
}

fun Database.Transaction.updateVoucherValueDecisionStatus(
    ids: List<VoucherValueDecisionId>,
    status: VoucherValueDecisionStatus
) {
    createUpdate {
            sql(
                "UPDATE voucher_value_decision SET status = ${bind(status)}::voucher_value_decision_status WHERE id = ANY(${bind(ids)})"
            )
        }
        .execute()
}

fun Database.Transaction.setVoucherValueDecisionType(
    id: VoucherValueDecisionId,
    type: VoucherValueDecisionType
) {
    createUpdate {
            sql(
                """
UPDATE voucher_value_decision
SET decision_type = ${bind(type.toString())}::voucher_value_decision_type
WHERE id = ${bind(id)}
  AND status = ${bind(VoucherValueDecisionStatus.DRAFT.toString())}::voucher_value_decision_status
"""
            )
        }
        .execute()
}

fun Database.Transaction.markVoucherValueDecisionsSent(
    ids: List<VoucherValueDecisionId>,
    now: HelsinkiDateTime
) {
    createUpdate {
            sql(
                "UPDATE voucher_value_decision SET status = ${bind(VoucherValueDecisionStatus.SENT)}::voucher_value_decision_status, sent_at = ${bind(now)} WHERE id = ANY(${bind(ids)})"
            )
        }
        .execute()
}

fun Database.Transaction.updateVoucherValueDecisionEndDates(
    updatedDecisions: List<VoucherValueDecision>,
    now: HelsinkiDateTime
) {
    executeBatch(updatedDecisions) {
        sql(
            """
UPDATE voucher_value_decision
SET
    valid_to = ${bind { it.validTo }},
    validity_updated_at = ${bind(now)}
WHERE id = ${bind { it.id }}
"""
        )
    }
}

fun Database.Transaction.annulVoucherValueDecisions(
    ids: List<VoucherValueDecisionId>,
    now: HelsinkiDateTime
) {
    if (ids.isEmpty()) return

    createUpdate {
            sql(
                "UPDATE voucher_value_decision SET status = ${bind(VoucherValueDecisionStatus.ANNULLED)}, annulled_at = ${bind(now)} WHERE id = ANY(${bind(ids)})"
            )
        }
        .execute()
}

fun Database.Transaction.setVoucherValueDecisionToIgnored(id: VoucherValueDecisionId) {
    createUpdate {
            sql(
                "UPDATE voucher_value_decision SET status = 'IGNORED' WHERE id = ${bind(id)} AND status = 'DRAFT'"
            )
        }
        .updateExactlyOne()
}

fun Database.Transaction.removeVoucherValueDecisionIgnore(id: VoucherValueDecisionId) {
    createUpdate {
            sql("DELETE FROM voucher_value_decision WHERE id = ${bind(id)} AND status = 'IGNORED'")
        }
        .updateExactlyOne()
}

fun Database.Transaction.lockValueDecisionsForChild(childId: ChildId) {
    createUpdate {
            sql(
                "SELECT id FROM voucher_value_decision WHERE child_id = ${bind(childId)} FOR UPDATE"
            )
        }
        .execute()
}

fun Database.Transaction.lockValueDecisions(ids: List<VoucherValueDecisionId>) {
    createUpdate {
            sql("SELECT id FROM voucher_value_decision WHERE id = ANY(${bind(ids)}) FOR UPDATE")
        }
        .execute()
}
