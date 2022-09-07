// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.data

import fi.espoo.evaka.invoicing.controller.DistinctiveParams
import fi.espoo.evaka.invoicing.controller.FeeDecisionSortParam
import fi.espoo.evaka.invoicing.controller.SortDirection
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionChild
import fi.espoo.evaka.invoicing.domain.FeeDecisionDetailed
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.FeeDecisionSummary
import fi.espoo.evaka.invoicing.domain.FeeDecisionType
import fi.espoo.evaka.invoicing.domain.merge
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.FeeDecisionId
import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Binding
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.disjointNumberQuery
import fi.espoo.evaka.shared.db.freeTextSearchQuery
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.mapToPaged
import fi.espoo.evaka.shared.utils.splitSearchText
import java.time.LocalDate
import java.util.UUID

// language=SQL
const val feeDecisionQueryBase =
    """
SELECT
    decision.*,
    part.child_id,
    part.child_date_of_birth,
    part.sibling_discount,
    part.placement_unit_id,
    part.placement_type,
    part.service_need_fee_coefficient,
    part.service_need_contract_days_per_month,
    part.service_need_description_fi,
    part.service_need_description_sv,
    part.service_need_missing,
    part.base_fee,
    part.fee,
    part.fee_alterations,
    part.final_fee,
    part.child_income
FROM fee_decision as decision
LEFT JOIN fee_decision_child as part ON decision.id = part.fee_decision_id
"""

// language=SQL
const val feeDecisionDetailedQueryBase =
    """
SELECT
    decision.*,
    part.child_id,
    part.child_date_of_birth,
    part.placement_unit_id,
    part.placement_type,
    part.service_need_fee_coefficient,
    part.service_need_description_fi,
    part.service_need_description_sv,
    part.service_need_missing,
    part.base_fee,
    part.sibling_discount,
    part.fee,
    part.fee_alterations,
    part.final_fee,
    part.child_income,
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
    daycare.language as placement_unit_lang,
    care_area.id as placement_unit_area_id,
    care_area.name as placement_unit_area_name,
    finance_decision_handler.first_name AS finance_decision_handler_first_name,
    finance_decision_handler.last_name AS finance_decision_handler_last_name
FROM fee_decision as decision
LEFT JOIN fee_decision_child as part ON decision.id = part.fee_decision_id
LEFT JOIN person as head ON decision.head_of_family_id = head.id
LEFT JOIN person as partner ON decision.partner_id = partner.id
LEFT JOIN person as child ON part.child_id = child.id
LEFT JOIN daycare ON part.placement_unit_id = daycare.id
LEFT JOIN care_area ON daycare.care_area_id = care_area.id
LEFT JOIN employee as approved_by ON decision.approved_by_id = approved_by.id
LEFT JOIN employee as finance_decision_handler ON finance_decision_handler.id = decision.decision_handler_id
"""

private val decisionNumberRegex = "^\\d{7,}$".toRegex()

fun Database.Transaction.upsertFeeDecisions(decisions: List<FeeDecision>) {
    upsertDecisions(decisions)
    replaceChildren(decisions)
}

private fun Database.Transaction.upsertDecisions(decisions: List<FeeDecision>) {
    val sql =
        """
        INSERT INTO fee_decision (
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
            fee_thresholds
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
            :feeThresholds
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
            fee_thresholds = :feeThresholds
    """

    val batch = prepareBatch(sql)
    decisions.forEach { decision ->
        batch
            .bindKotlin(decision)
            .add()
    }
    batch.execute()
}

private fun Database.Transaction.replaceChildren(decisions: List<FeeDecision>) {
    val partsWithDecisionIds = decisions.map { it.id to it.children }
    deleteChildren(partsWithDecisionIds.map { it.first })
    insertChildren(partsWithDecisionIds)
}

private fun Database.Transaction.insertChildren(decisions: List<Pair<FeeDecisionId, List<FeeDecisionChild>>>) {
    val sql =
        """
        INSERT INTO fee_decision_child (
            id,
            fee_decision_id,
            child_id,
            child_date_of_birth,
            placement_unit_id,
            placement_type,
            service_need_fee_coefficient,
            service_need_contract_days_per_month,
            service_need_description_fi,
            service_need_description_sv,
            service_need_missing,
            base_fee,
            sibling_discount,
            fee,
            fee_alterations,
            final_fee,
            child_income
        ) VALUES (
            :id,
            :feeDecisionId,
            :childId,
            :childDateOfBirth,
            :placementUnitId,
            :placementType,
            :serviceNeedFeeCoefficient,
            :serviceNeedContractDaysPerMonth,
            :serviceNeedDescriptionFi,
            :serviceNeedDescriptionSv,
            :serviceNeedMissing,
            :baseFee,
            :siblingDiscount,
            :fee,
            :feeAlterations,
            :finalFee,
            :childIncome
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
                .bind("placementUnitId", child.placement.unitId)
                .bind("placementType", child.placement.type)
                .bind("serviceNeedFeeCoefficient", child.serviceNeed.feeCoefficient)
                .bind("serviceNeedContractDaysPerMonth", child.serviceNeed.contractDaysPerMonth)
                .bind("serviceNeedDescriptionFi", child.serviceNeed.descriptionFi)
                .bind("serviceNeedDescriptionSv", child.serviceNeed.descriptionSv)
                .bind("serviceNeedMissing", child.serviceNeed.missing)
                .add()
        }
    }
    batch.execute()
}

private fun Database.Transaction.deleteChildren(decisionIds: List<FeeDecisionId>) {
    if (decisionIds.isEmpty()) return

    createUpdate("DELETE FROM fee_decision_child WHERE fee_decision_id = ANY(:decisionIds)")
        .bind("decisionIds", decisionIds)
        .execute()
}

fun Database.Transaction.deleteFeeDecisions(ids: List<FeeDecisionId>) {
    if (ids.isEmpty()) return

    createUpdate("DELETE FROM fee_decision WHERE id = ANY(:ids)")
        .bind("ids", ids)
        .execute()
}

fun Database.Read.searchFeeDecisions(
    clock: EvakaClock,
    page: Int,
    pageSize: Int,
    sortBy: FeeDecisionSortParam,
    sortDirection: SortDirection,
    statuses: List<FeeDecisionStatus>,
    areas: List<String>,
    unit: DaycareId?,
    distinctiveParams: List<DistinctiveParams>,
    searchTerms: String = "",
    startDate: LocalDate?,
    endDate: LocalDate?,
    searchByStartDate: Boolean = false,
    financeDecisionHandlerId: EmployeeId?
): Paged<FeeDecisionSummary> {
    val sortColumn = when (sortBy) {
        FeeDecisionSortParam.HEAD_OF_FAMILY -> "head.last_name"
        FeeDecisionSortParam.VALIDITY -> "lower(decision.valid_during)"
        FeeDecisionSortParam.NUMBER -> "decision.decision_number"
        FeeDecisionSortParam.CREATED -> "decision.created"
        FeeDecisionSortParam.SENT -> "decision.sent_at"
        FeeDecisionSortParam.STATUS -> "decision.status"
        FeeDecisionSortParam.FINAL_PRICE -> "sum"
    }

    val params = listOf(
        Binding.of("page", page),
        Binding.of("pageSize", pageSize),
        Binding.of("status", statuses),
        Binding.of("area", areas),
        Binding.of("unit", unit),
        Binding.of("espooPostOffice", "ESPOO"),
        Binding.of("start_date", startDate),
        Binding.of("end_date", endDate),
        Binding.of("finance_decision_handler", financeDecisionHandlerId),
        Binding.of("firstPlacementStartDate", clock.today().withDayOfMonth(1)),
    )

    val numberParamsRaw = splitSearchText(searchTerms).filter(decisionNumberRegex::matches)
    val searchTextWithoutNumbers = searchTerms.replace(decisionNumberRegex, "")

    val (freeTextQuery, freeTextParams) = freeTextSearchQuery(listOf("head", "partner", "child"), searchTextWithoutNumbers)

    val withNullHours = distinctiveParams.contains(DistinctiveParams.UNCONFIRMED_HOURS)

    val havingExternalChildren = distinctiveParams.contains(DistinctiveParams.EXTERNAL_CHILD)

    val retroactiveOnly = distinctiveParams.contains(DistinctiveParams.RETROACTIVE)

    val noStartingPlacements = distinctiveParams.contains(DistinctiveParams.NO_STARTING_PLACEMENTS)

    val (numberQuery, numberParams) = disjointNumberQuery("decision", "decision_number", numberParamsRaw)

    val conditions = listOfNotNull(
        if (statuses.isNotEmpty()) "status = ANY(:status::fee_decision_status[])" else null,
        if (areas.isNotEmpty()) "youngest_child.area = ANY(:area)" else null,
        if (unit != null) "part.placement_unit_id = :unit" else null,
        if (withNullHours) "part.service_need_missing" else null,
        if (havingExternalChildren) "child.post_office <> '' AND child.post_office NOT ILIKE :espooPostOffice" else null,
        if (retroactiveOnly) "lower(decision.valid_during) < date_trunc('month', COALESCE(decision.approved_at, now()))" else null,
        if (numberParamsRaw.isNotEmpty()) numberQuery else null,
        if (searchTextWithoutNumbers.isNotBlank()) freeTextQuery else null,
        if ((startDate != null || endDate != null) && !searchByStartDate) "daterange(:start_date, :end_date, '[]') && valid_during" else null,
        if ((startDate != null || endDate != null) && searchByStartDate) "daterange(:start_date, :end_date, '[]') @> lower(valid_during)" else null,
        if (financeDecisionHandlerId != null) "youngest_child.finance_decision_handler = :finance_decision_handler" else null,
        if (noStartingPlacements) "decisions_with_first_placement_starting_this_month.fee_decision_id IS NULL" else null
    )

    val youngestChildQuery =
        """
        WITH youngest_child AS (
            SELECT
                fee_decision_child.fee_decision_id AS decision_id,
                care_area.short_name AS area,
                daycare.finance_decision_handler AS finance_decision_handler,
                row_number() OVER (PARTITION BY (fee_decision_id) ORDER BY child_date_of_birth DESC) AS rownum
            FROM fee_decision_child
            LEFT JOIN daycare ON fee_decision_child.placement_unit_id = daycare.id
            LEFT JOIN care_area ON daycare.care_area_id = care_area.id
        )
        """.trimIndent()
    val youngestChildJoin = "LEFT JOIN youngest_child ON decision.id = youngest_child.decision_id AND rownum = 1"

    val firstPlacementStartingThisMonthChildQuery =
        """
        WITH decisions_with_first_placement_starting_this_month AS (    
            SELECT DISTINCT(fdc.fee_decision_id)
            FROM placement p
            JOIN person c ON p.child_id = c.id
            JOIN fee_decision_child fdc ON c.id = fdc.child_id
            LEFT JOIN placement preceding ON p.child_id = preceding.child_id AND (p.start_date - interval '1 days') = preceding.end_date AND preceding.type != 'CLUB'::placement_type
            WHERE p.start_date >= :firstPlacementStartDate AND preceding.id IS NULL AND p.type != 'CLUB'::placement_type
        )
        """.trimIndent()

    val firstPlacementStartingThisMonthChildIdsQueryJoin =
        "LEFT JOIN decisions_with_first_placement_starting_this_month ON decision.id = decisions_with_first_placement_starting_this_month.fee_decision_id"

    // language=sql
    val sql =
        """
        WITH decision_ids AS (
            ${if (areas.isNotEmpty() || financeDecisionHandlerId != null) youngestChildQuery else ""}
            ${if (noStartingPlacements) firstPlacementStartingThisMonthChildQuery else ""}
            SELECT decision.id, count(*) OVER (), max(sums.sum) sum
            FROM fee_decision AS decision
            LEFT JOIN fee_decision_child AS part ON decision.id = part.fee_decision_id
            LEFT JOIN person AS head ON decision.head_of_family_id = head.id
            LEFT JOIN person AS partner ON decision.partner_id = partner.id
            LEFT JOIN person AS child ON part.child_id = child.id
            LEFT JOIN daycare AS placement_unit ON placement_unit.id = part.placement_unit_id
            LEFT JOIN (
                SELECT fee_decision.id, coalesce(sum(fee_decision_child.final_fee), 0) sum
                FROM fee_decision
                LEFT JOIN fee_decision_child ON fee_decision.id = fee_decision_child.fee_decision_id
                GROUP BY fee_decision.id
            ) sums ON decision.id = sums.id
            ${if (areas.isNotEmpty() || financeDecisionHandlerId != null) youngestChildJoin else ""}
            ${if (noStartingPlacements) firstPlacementStartingThisMonthChildIdsQueryJoin else ""}
            ${if (conditions.isNotEmpty()) """
            WHERE ${conditions.joinToString("\nAND ")}
        """.trimIndent() else ""}
            GROUP BY decision.id
            -- we take a max here because the sort column is not in group by clause but it should be identical for all grouped rows
            ORDER BY max($sortColumn) ${sortDirection.name}, decision.id
            LIMIT :pageSize OFFSET :pageSize * :page
        )
        SELECT
            decision_ids.count,
            decision_ids.sum,
            decision.*,
            part.child_id,
            part.child_date_of_birth,
            part.fee,
            part.fee_alterations,
            part.final_fee,
            head.date_of_birth AS head_date_of_birth,
            head.first_name AS head_first_name,
            head.last_name AS head_last_name,
            head.social_security_number AS head_ssn,
            head.force_manual_fee_decisions AS head_force_manual_fee_decisions,
            child.first_name AS child_first_name,
            child.last_name AS child_last_name,
            child.social_security_number AS child_ssn
        FROM decision_ids
        LEFT JOIN fee_decision AS decision ON decision_ids.id = decision.id
        LEFT JOIN fee_decision_child AS part ON decision.id = part.fee_decision_id
        LEFT JOIN person AS head ON decision.head_of_family_id = head.id
        LEFT JOIN person AS child ON part.child_id = child.id
        ORDER BY $sortColumn ${sortDirection.name}, decision.id, part.child_date_of_birth DESC
        """.trimIndent()

    return createQuery(sql)
        .addBindings(params)
        .addBindings(freeTextParams)
        .addBindings(numberParams)
        .mapToPaged<FeeDecisionSummary>(pageSize)
        .let { it.copy(data = it.data.merge()) }
}

fun Database.Read.getFeeDecisionsByIds(ids: List<FeeDecisionId>): List<FeeDecision> {
    if (ids.isEmpty()) return emptyList()

    val sql =
        """
$feeDecisionQueryBase
WHERE decision.id = ANY(:ids)
"""

    return createQuery(sql)
        .bind("ids", ids)
        .mapTo<FeeDecision>()
        .merge()
}

fun Database.Read.getDetailedFeeDecisionsByIds(ids: List<FeeDecisionId>): List<FeeDecisionDetailed> {
    if (ids.isEmpty()) return emptyList()

    val sql =
        """
$feeDecisionDetailedQueryBase
WHERE decision.id = ANY(:ids)
ORDER BY part.child_date_of_birth DESC
"""

    return createQuery(sql)
        .bind("ids", ids)
        .mapTo<FeeDecisionDetailed>()
        .merge()
}

fun Database.Read.getFeeDecision(uuid: FeeDecisionId): FeeDecisionDetailed? {
    val sql =
        """
        $feeDecisionDetailedQueryBase
        WHERE decision.id = :id
        ORDER BY part.child_date_of_birth DESC
    """

    return createQuery(sql)
        .bind("id", uuid)
        .mapTo<FeeDecisionDetailed>()
        .merge()
        .firstOrNull()?.let { it ->
            it.copy(
                partnerIsCodebtor = partnerIsCodebtor(
                    it.headOfFamily.id,
                    it.partner?.id,
                    it.children.map { c -> c.child.id },
                    it.validDuring
                )
            )
        }
}

fun Database.Read.findFeeDecisionsForHeadOfFamily(
    headOfFamilyId: PersonId,
    period: DateRange?,
    status: List<FeeDecisionStatus>?
): List<FeeDecision> {
    val sql =
        """
        $feeDecisionQueryBase
        WHERE
            decision.head_of_family_id = :headOfFamilyId
            ${period?.let { "AND decision.valid_during && :period" } ?: ""}
            ${status?.let { "AND decision.status = ANY(:status::fee_decision_status[])" } ?: ""}
    """

    return createQuery(sql)
        .bind("headOfFamilyId", headOfFamilyId)
        .let { query ->
            if (period != null) query.bind("period", period)
            else query
        }
        .let { query -> if (status != null) query.bind("status", status.map { it.name }) else query }
        .mapTo<FeeDecision>()
        .merge()
}

fun Database.Transaction.approveFeeDecisionDraftsForSending(
    ids: List<FeeDecisionId>,
    approvedBy: EmployeeId,
    approvedAt: HelsinkiDateTime,
    isRetroactive: Boolean = false,
    alwaysUseDaycareFinanceDecisionHandler: Boolean
) {
    val sql =
        """
        WITH youngest_child AS (
            SELECT
                fee_decision_child.fee_decision_id AS decision_id,
                daycare.finance_decision_handler AS finance_decision_handler_id,
                row_number() OVER (PARTITION BY (fee_decision_id) ORDER BY child_date_of_birth DESC) AS rownum
            FROM fee_decision_child
            LEFT JOIN daycare ON fee_decision_child.placement_unit_id = daycare.id
        )
        UPDATE fee_decision
        SET
            status = :status::fee_decision_status,
            decision_number = nextval('fee_decision_number_sequence'),
            approved_by_id = :approvedBy,
            decision_handler_id = CASE
                WHEN :alwaysUseDaycareFinanceDecisionHandler = true AND youngest_child.finance_decision_handler_id IS NOT NULL 
                    THEN youngest_child.finance_decision_handler_id
                WHEN :isRetroactive = true THEN :approvedBy
                WHEN youngest_child.finance_decision_handler_id IS NOT NULL AND fd.decision_type = 'NORMAL'
                    THEN youngest_child.finance_decision_handler_id
                ELSE :approvedBy
            END,
            approved_at = :approvedAt
        FROM fee_decision AS fd
        LEFT JOIN youngest_child ON youngest_child.decision_id = :id AND rownum = 1
        WHERE fd.id = :id AND fee_decision.id = fd.id
        """.trimIndent()

    val batch = prepareBatch(sql)
    ids.map { id ->
        batch
            .bind("id", id)
            .bind("status", FeeDecisionStatus.WAITING_FOR_SENDING.toString())
            .bind("approvedBy", approvedBy)
            .bind("isRetroactive", isRetroactive)
            .bind("approvedAt", approvedAt)
            .bind("alwaysUseDaycareFinanceDecisionHandler", alwaysUseDaycareFinanceDecisionHandler)
            .add()
    }
    batch.execute()
}

fun Database.Transaction.setFeeDecisionWaitingForManualSending(id: FeeDecisionId) {
    val sql =
        """
        UPDATE fee_decision
        SET
            status = :status::fee_decision_status
        WHERE id = :id
        AND status = :requiredStatus::fee_decision_status
    """

    createUpdate(sql)
        .bind("id", id)
        .bind("status", FeeDecisionStatus.WAITING_FOR_MANUAL_SENDING.toString())
        .bind("requiredStatus", FeeDecisionStatus.WAITING_FOR_SENDING.toString())
        .execute()
}

fun Database.Transaction.setFeeDecisionSent(ids: List<FeeDecisionId>) {
    val sql =
        """
        UPDATE fee_decision
        SET
            status = :status::fee_decision_status,
            sent_at = NOW()
        WHERE id = :id
    """

    val batch = prepareBatch(sql)
    ids.forEach { id ->
        batch
            .bind("id", id)
            .bind("status", FeeDecisionStatus.SENT.toString())
            .add()
    }
    batch.execute()
}

fun Database.Transaction.updateFeeDecisionStatusAndDates(updatedDecisions: List<FeeDecision>) {
    prepareBatch("UPDATE fee_decision SET status = :status::fee_decision_status, valid_during = :validDuring WHERE id = :id")
        .also { batch ->
            updatedDecisions.forEach { decision ->
                batch
                    .bind("id", decision.id)
                    .bind("status", decision.status)
                    .bind("validDuring", decision.validDuring)
                    .add()
            }
        }
        .execute()
}

fun Database.Transaction.updateFeeDecisionDocumentKey(id: FeeDecisionId, key: String) {
    val sql =
        """
        UPDATE fee_decision
        SET document_key = :key
        WHERE id = :id
    """

    createUpdate(sql)
        .bind("id", id)
        .bind("key", key)
        .execute()
}

fun Database.Read.getFeeDecisionDocumentKey(decisionId: FeeDecisionId): String? {
    val sql =
        """
        SELECT document_key 
        FROM fee_decision
        WHERE id = :id
    """

    return createQuery(sql)
        .bind("id", decisionId)
        .mapTo<String>()
        .firstOrNull()
}

fun Database.Transaction.setFeeDecisionType(id: FeeDecisionId, type: FeeDecisionType) {
    //language=SQL
    val sql =
        """
        UPDATE fee_decision
        SET decision_type = :type::fee_decision_type
        WHERE id = :id
            AND status = :requiredStatus::fee_decision_status
    """

    createUpdate(sql)
        .bind("id", id)
        .bind("type", type.toString())
        .bind("requiredStatus", FeeDecisionStatus.DRAFT.toString())
        .execute()
}

fun Database.Transaction.lockFeeDecisionsForHeadOfFamily(headOfFamily: PersonId) {
    createUpdate("SELECT id FROM fee_decision WHERE head_of_family_id = :headOfFamily FOR UPDATE")
        .bind("headOfFamily", headOfFamily)
        .execute()
}

fun Database.Transaction.lockFeeDecisions(ids: List<FeeDecisionId>) {
    createUpdate("SELECT id FROM fee_decision WHERE id = ANY(:ids) FOR UPDATE")
        .bind("ids", ids)
        .execute()
}

fun Database.Read.partnerIsCodebtor(
    headOfFamilyId: PersonId,
    partnerId: PersonId?,
    childIds: List<ChildId>,
    dateRange: DateRange
): Boolean = partnerId != null && createQuery(
    """
WITH partner_children AS (
    SELECT COALESCE(ARRAY_AGG(child_id), '{}') AS ids
    FROM guardian WHERE guardian_id = :partnerId
), partner_fridge_children AS (
    SELECT COALESCE(ARRAY_AGG(child_id), '{}') AS ids
    FROM fridge_child WHERE NOT conflict AND head_of_child = :partnerId AND daterange(start_date, end_date, '[]') && :dateRange
)
SELECT (partner_children.ids || partner_fridge_children.ids) && :childIds
FROM partner_children, partner_fridge_children
"""
)
    .bind("headOfFamilyId", headOfFamilyId)
    .bind("partnerId", partnerId)
    .bind("childIds", childIds)
    .bind("dateRange", dateRange)
    .mapTo<Boolean>()
    .first()
