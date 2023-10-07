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
import fi.espoo.evaka.invoicing.domain.FeeDecisionDifference
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.FeeDecisionSummary
import fi.espoo.evaka.invoicing.domain.FeeDecisionType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.FeeDecisionId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Binding
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.db.QuerySql
import fi.espoo.evaka.shared.db.disjointNumberQuery
import fi.espoo.evaka.shared.db.freeTextSearchQuery
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.mapToPaged
import fi.espoo.evaka.shared.utils.splitSearchText
import java.time.LocalDate
import java.util.UUID

fun feeDecisionQuery(
    predicate: Predicate<Any> = Predicate.alwaysTrue(),
    lockForUpdate: Boolean = false
) =
    QuerySql.of<Any> {
        sql(
            """
SELECT
    decision.id,
    decision.created,
    decision.status,
    decision.valid_during,
    decision.decision_type,
    decision.head_of_family_id,
    decision.head_of_family_income,
    decision.partner_id,
    decision.partner_income,
    decision.family_size,
    decision.fee_thresholds,
    decision.decision_number,
    decision.document_key,
    decision.approved_at,
    decision.approved_by_id,
    decision.decision_handler_id,
    decision.sent_at,
    decision.difference,
    COALESCE((
        SELECT jsonb_agg(json_build_object(
            'child', json_build_object(
                'id', part.child_id,
                'dateOfBirth', part.child_date_of_birth
            ),
            'placement', json_build_object(
                'unitId', part.placement_unit_id,
                'type', part.placement_type
            ),
            'serviceNeed', json_build_object(
                'optionId', part.service_need_option_id,
                'feeCoefficient', part.service_need_fee_coefficient,
                'contractDaysPerMonth', part.service_need_contract_days_per_month,
                'descriptionFi', part.service_need_description_fi,
                'descriptionSv', part.service_need_description_sv,
                'missing', part.service_need_missing
            ),
            'baseFee', part.base_fee,
            'siblingDiscount', part.sibling_discount,
            'fee', part.fee,
            'feeAlterations', part.fee_alterations,
            'finalFee', part.final_fee,
            'childIncome', part.child_income
        ) ORDER BY part.child_date_of_birth DESC, part.sibling_discount, part.child_id)
        FROM fee_decision_child as part
        WHERE part.fee_decision_id = decision.id
    ), '[]'::jsonb) AS children
FROM fee_decision as decision
WHERE ${predicate(predicate.forTable("decision"))}
${if (lockForUpdate) "FOR UPDATE" else ""}
"""
        )
    }

private fun feeDecisionDetailedQuery(predicate: Predicate<Any>) =
    QuerySql.of<Any> {
        sql(
            """
SELECT
    decision.id,
    decision.created,
    decision.status,
    decision.valid_during,
    decision.decision_type,
    decision.head_of_family_income,
    decision.partner_income,
    decision.family_size,
    decision.fee_thresholds,
    decision.decision_number,
    decision.document_key,
    decision.approved_at,
    decision.sent_at,
    decision.head_of_family_id AS head_id,
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
    decision.partner_id,
    partner.date_of_birth as partner_date_of_birth,
    partner.first_name as partner_first_name,
    partner.last_name as partner_last_name,
    partner.social_security_number as partner_ssn,
    partner.street_address as partner_street_address,
    partner.postal_code as partner_postal_code,
    partner.post_office as partner_post_office,
    partner.restricted_details_enabled as partner_restricted_details_enabled,
    decision.approved_by_id,
    coalesce(approved_by.preferred_first_name, approved_by.first_name) as approved_by_first_name,
    approved_by.last_name as approved_by_last_name,
    coalesce(finance_decision_handler.preferred_first_name, finance_decision_handler.first_name) AS finance_decision_handler_first_name,
    finance_decision_handler.last_name AS finance_decision_handler_last_name,
    COALESCE((
        SELECT jsonb_agg(json_build_object(
            'child', json_build_object( 
                'id', part.child_id,
                'dateOfBirth', part.child_date_of_birth,
                'firstName', child.first_name,
                'lastName', child.last_name,
                'ssn', child.social_security_number,
                'streetAddress', child.street_address,
                'postalCode', child.postal_code,
                'postOffice', child.post_office,
                'restrictedDetailsEnabled', child.restricted_details_enabled
            ),
            'placementType', part.placement_type,
            'placementUnit', json_build_object(
                'id', part.placement_unit_id,
                'name', daycare.name,
                'areaId', care_area.id,
                'areaName', care_area.name,
                'language', daycare.language
            ),
            'serviceNeedOptionId', part.service_need_option_id,
            'serviceNeedFeeCoefficient', part.service_need_fee_coefficient,
            'serviceNeedDescriptionFi', part.service_need_description_fi,
            'serviceNeedDescriptionSv', part.service_need_description_sv,
            'serviceNeedMissing', part.service_need_missing,
            'baseFee', part.base_fee,
            'siblingDiscount', part.sibling_discount,
            'fee', part.fee,
            'feeAlterations', part.fee_alterations,
            'finalFee', part.final_fee,
            'childIncome', part.child_income
        ) ORDER BY part.child_date_of_birth DESC, part.sibling_discount, part.child_id)
        FROM fee_decision_child as part 
        JOIN person as child ON part.child_id = child.id
        JOIN daycare ON part.placement_unit_id = daycare.id
        JOIN care_area ON daycare.care_area_id = care_area.id
        WHERE part.fee_decision_id = decision.id
    ), '[]'::jsonb) AS children
FROM fee_decision as decision
LEFT JOIN person as head ON decision.head_of_family_id = head.id
LEFT JOIN person as partner ON decision.partner_id = partner.id
LEFT JOIN employee as approved_by ON decision.approved_by_id = approved_by.id
LEFT JOIN employee as finance_decision_handler ON finance_decision_handler.id = decision.decision_handler_id
WHERE ${predicate(predicate.forTable("decision"))}
"""
        )
    }

private val decisionNumberRegex = "^\\d{7,}$".toRegex()

fun Database.Transaction.upsertFeeDecisions(decisions: List<FeeDecision>) {
    upsertDecisions(decisions)
    replaceChildren(decisions)
}

fun Database.Transaction.insertFeeDecisions(decisions: List<FeeDecision>) {
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
            fee_thresholds,
            difference,
            total_fee,
            created
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
            :feeThresholds,
            :difference,
            :totalFee,
            :created
        )
    """

    val batch = prepareBatch(sql)
    decisions.forEach { decision -> batch.bindKotlin(decision).add() }
    batch.execute()

    insertChildren(decisions.map { it.id to it.children })
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
            fee_thresholds,
            difference,
            total_fee,
            created
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
            :feeThresholds,
            :difference,
            :totalFee,
            :created
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
            fee_thresholds = :feeThresholds,
            difference = :difference,
            total_fee = :totalFee,
            created = :created
    """

    val batch = prepareBatch(sql)
    decisions.forEach { decision -> batch.bindKotlin(decision).add() }
    batch.execute()
}

private fun Database.Transaction.replaceChildren(decisions: List<FeeDecision>) {
    val partsWithDecisionIds = decisions.map { it.id to it.children }
    deleteFeeDecisionChildren(partsWithDecisionIds.map { it.first })
    insertChildren(partsWithDecisionIds)
}

private fun Database.Transaction.insertChildren(
    decisions: List<Pair<FeeDecisionId, List<FeeDecisionChild>>>
) {
    val sql =
        """
        INSERT INTO fee_decision_child (
            id,
            fee_decision_id,
            child_id,
            child_date_of_birth,
            placement_unit_id,
            placement_type,
            service_need_option_id,
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
            :serviceNeedOptionId,
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
                .bind("serviceNeedOptionId", child.serviceNeed.optionId)
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

fun Database.Transaction.deleteFeeDecisionChildren(decisionIds: List<FeeDecisionId>) {
    if (decisionIds.isEmpty()) return

    createUpdate("DELETE FROM fee_decision_child WHERE fee_decision_id = ANY(:decisionIds)")
        .bind("decisionIds", decisionIds)
        .execute()
}

fun Database.Transaction.deleteFeeDecisions(ids: List<FeeDecisionId>) {
    if (ids.isEmpty()) return

    createUpdate("DELETE FROM fee_decision WHERE id = ANY(:ids)").bind("ids", ids).execute()
}

data class PagedFeeDecisionSummaries(
    val data: List<FeeDecisionSummary>,
    val total: Int,
    val pages: Int
)

fun Database.Read.searchFeeDecisions(
    clock: EvakaClock,
    postOffice: String,
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
    financeDecisionHandlerId: EmployeeId?,
    difference: Set<FeeDecisionDifference>
): PagedFeeDecisionSummaries {
    val sortColumn =
        when (sortBy) {
            FeeDecisionSortParam.HEAD_OF_FAMILY -> "head.last_name"
            FeeDecisionSortParam.VALIDITY -> "lower(decision.valid_during)"
            FeeDecisionSortParam.NUMBER -> "decision.decision_number"
            FeeDecisionSortParam.CREATED -> "decision.created"
            FeeDecisionSortParam.SENT -> "decision.sent_at"
            FeeDecisionSortParam.STATUS -> "decision.status"
            FeeDecisionSortParam.FINAL_PRICE -> "total_fee"
        }

    val params =
        listOf(
            Binding.of("page", page),
            Binding.of("pageSize", pageSize),
            Binding.of("status", statuses),
            Binding.of("area", areas),
            Binding.of("unit", unit),
            Binding.of("postOffice", postOffice),
            Binding.of("start_date", startDate),
            Binding.of("end_date", endDate),
            Binding.of("finance_decision_handler", financeDecisionHandlerId),
            Binding.of("difference", difference),
            Binding.of("firstPlacementStartDate", clock.today().withDayOfMonth(1)),
            Binding.of("now", clock.now())
        )

    val numberParamsRaw = splitSearchText(searchTerms).filter(decisionNumberRegex::matches)
    val searchTextWithoutNumbers = searchTerms.replace(decisionNumberRegex, "")

    val (freeTextQuery, freeTextParams) =
        freeTextSearchQuery(listOf("head", "partner", "child"), searchTextWithoutNumbers)

    val withNullHours = distinctiveParams.contains(DistinctiveParams.UNCONFIRMED_HOURS)

    val havingExternalChildren = distinctiveParams.contains(DistinctiveParams.EXTERNAL_CHILD)

    val retroactiveOnly = distinctiveParams.contains(DistinctiveParams.RETROACTIVE)

    val noStartingPlacements = distinctiveParams.contains(DistinctiveParams.NO_STARTING_PLACEMENTS)

    val maxFeeAccepted = distinctiveParams.contains(DistinctiveParams.MAX_FEE_ACCEPTED)

    val preschoolClub = distinctiveParams.contains(DistinctiveParams.PRESCHOOL_CLUB)

    val (numberQuery, numberParams) =
        disjointNumberQuery("decision", "decision_number", numberParamsRaw)

    val conditions =
        listOfNotNull(
            if (statuses.isNotEmpty()) "status = ANY(:status::fee_decision_status[])" else null,
            if (areas.isNotEmpty()) "youngest_child.area = ANY(:area)" else null,
            if (unit != null) "part.placement_unit_id = :unit" else null,
            if (withNullHours) "part.service_need_missing" else null,
            if (havingExternalChildren)
                "child.post_office <> '' AND child.post_office NOT ILIKE :postOffice"
            else null,
            if (retroactiveOnly)
                "lower(decision.valid_during) < date_trunc('month', COALESCE(decision.approved_at, :now))"
            else null,
            if (numberParamsRaw.isNotEmpty()) numberQuery else null,
            if (searchTextWithoutNumbers.isNotBlank()) freeTextQuery else null,
            if ((startDate != null || endDate != null) && !searchByStartDate)
                "daterange(:start_date, :end_date, '[]') && valid_during"
            else null,
            if ((startDate != null || endDate != null) && searchByStartDate) {
                // valid_during overlaps but does not extend to the left of search range
                // = start date of valid_during is included in the search range
                "(valid_during && daterange(:start_date, :end_date, '[]') AND valid_during &> daterange(:start_date, :end_date, '[]'))"
            } else {
                null
            },
            if (financeDecisionHandlerId != null)
                "youngest_child.finance_decision_handler = :finance_decision_handler"
            else null,
            if (difference.isNotEmpty()) "decision.difference && :difference" else null,
            if (noStartingPlacements)
                "decisions_with_first_placement_starting_this_month.fee_decision_id IS NULL"
            else null,
            if (maxFeeAccepted)
                "(decision.head_of_family_income->>'effect' = 'MAX_FEE_ACCEPTED' OR decision.partner_income->>'effect' = 'MAX_FEE_ACCEPTED')"
            else null,
            if (preschoolClub) "decisions_with_preschool_club_placement IS NOT NULL" else null
        )

    val youngestChildQuery =
        """
        youngest_child AS (
            SELECT
                fee_decision_child.fee_decision_id AS decision_id,
                care_area.short_name AS area,
                daycare.finance_decision_handler AS finance_decision_handler,
                row_number() OVER (PARTITION BY (fee_decision_id) ORDER BY child_date_of_birth DESC, sibling_discount, child_id) AS rownum
            FROM fee_decision_child
            LEFT JOIN daycare ON fee_decision_child.placement_unit_id = daycare.id
            LEFT JOIN care_area ON daycare.care_area_id = care_area.id
        )
        """
            .trimIndent()
    val youngestChildJoin =
        "LEFT JOIN youngest_child ON decision.id = youngest_child.decision_id AND rownum = 1"

    val firstPlacementStartingThisMonthChildQuery =
        """
        decisions_with_first_placement_starting_this_month AS (    
            SELECT DISTINCT(fdc.fee_decision_id)
            FROM placement p
            JOIN person c ON p.child_id = c.id
            JOIN fee_decision_child fdc ON c.id = fdc.child_id
            LEFT JOIN placement preceding ON p.child_id = preceding.child_id AND (p.start_date - interval '1 days') = preceding.end_date AND preceding.type != 'CLUB'::placement_type
            WHERE p.start_date >= :firstPlacementStartDate AND preceding.id IS NULL AND p.type != 'CLUB'::placement_type
        )
        """
            .trimIndent()

    val firstPlacementStartingThisMonthChildIdsQueryJoin =
        "LEFT JOIN decisions_with_first_placement_starting_this_month ON decision.id = decisions_with_first_placement_starting_this_month.fee_decision_id"

    val preschoolClubPlacementQuery =
        """
        decisions_with_preschool_club_placement AS (
            SELECT DISTINCT(fdc.fee_decision_id)
            FROM fee_decision_child fdc
            WHERE fdc.placement_type = 'PRESCHOOL_CLUB'::placement_type
        )
        """
            .trimIndent()

    val preschoolClubQueryJoin =
        "LEFT JOIN decisions_with_preschool_club_placement ON decision.id = decisions_with_preschool_club_placement.fee_decision_id"

    val CTEs =
        listOf(
                if (areas.isNotEmpty() || financeDecisionHandlerId != null) youngestChildQuery
                else "",
                if (noStartingPlacements) firstPlacementStartingThisMonthChildQuery else "",
                if (preschoolClub) preschoolClubPlacementQuery else ""
            )
            .filter { it.isNotEmpty() }
            .joinToString(",")

    val sql =
        """
        WITH decision_ids AS (
            ${if (CTEs.length > 0) "WITH $CTEs" else ""}
            SELECT decision.id, count(*) OVER ()
            FROM fee_decision AS decision
            LEFT JOIN fee_decision_child AS part ON decision.id = part.fee_decision_id
            LEFT JOIN person AS head ON decision.head_of_family_id = head.id
            LEFT JOIN person AS partner ON decision.partner_id = partner.id
            LEFT JOIN person AS child ON part.child_id = child.id
            LEFT JOIN daycare AS placement_unit ON placement_unit.id = part.placement_unit_id
            ${if (areas.isNotEmpty() || financeDecisionHandlerId != null) youngestChildJoin else ""}
            ${if (noStartingPlacements) firstPlacementStartingThisMonthChildIdsQueryJoin else ""}
            ${if (preschoolClub) preschoolClubQueryJoin else ""}
            ${if (conditions.isNotEmpty()) {
            """
            WHERE ${conditions.joinToString("\nAND ")}
            """.trimIndent()
        } else {
            ""
        }}
            GROUP BY decision.id
            -- we take a max here because the sort column is not in group by clause but it should be identical for all grouped rows
            ORDER BY max($sortColumn) ${sortDirection.name}, decision.id
            LIMIT :pageSize OFFSET :pageSize * :page
        )
        SELECT
            decision_ids.count,
            decision.id,
            decision.created,
            decision.status,
            decision.valid_during,
            decision.decision_number,
            decision.approved_at,
            decision.sent_at,
            decision.total_fee AS final_price,
            decision.difference,
            decision.head_of_family_id AS head_id,
            head.date_of_birth AS head_date_of_birth,
            head.first_name AS head_first_name,
            head.last_name AS head_last_name,
            head.social_security_number AS head_ssn,
            head.force_manual_fee_decisions AS head_force_manual_fee_decisions,
            COALESCE((
                SELECT jsonb_agg(json_build_object(
                    'id', part.child_id,
                    'dateOfBirth', part.child_date_of_birth,
                    'firstName', child.first_name,
                    'lastName', child.last_name,
                    'ssn', child.social_security_number
                ) ORDER BY part.child_date_of_birth DESC, part.sibling_discount, part.child_id)
                FROM fee_decision_child AS part
                JOIN person AS child ON part.child_id = child.id
                WHERE part.fee_decision_id = decision.id 
            ), '[]'::jsonb) AS children
        FROM decision_ids
        LEFT JOIN fee_decision AS decision ON decision_ids.id = decision.id
        LEFT JOIN person AS head ON decision.head_of_family_id = head.id
        ORDER BY $sortColumn ${sortDirection.name}, decision.id
        """
            .trimIndent()

    return createQuery(sql)
        .addBindings(params)
        .addBindings(freeTextParams)
        .addBindings(numberParams)
        .mapToPaged(::PagedFeeDecisionSummaries, pageSize)
}

fun Database.Read.getFeeDecisionsByIds(ids: List<FeeDecisionId>): List<FeeDecision> {
    if (ids.isEmpty()) return emptyList()
    return createQuery(feeDecisionQuery(Predicate { where("$it.id = ANY(${bind(ids)})") }))
        .toList<FeeDecision>()
}

fun Database.Read.getDetailedFeeDecisionsByIds(
    ids: List<FeeDecisionId>
): List<FeeDecisionDetailed> {
    if (ids.isEmpty()) return emptyList()
    return createQuery(feeDecisionDetailedQuery(Predicate { where("$it.id = ANY(${bind(ids)})") }))
        .toList<FeeDecisionDetailed>()
}

fun Database.Read.getFeeDecision(uuid: FeeDecisionId): FeeDecisionDetailed? {
    return createQuery(feeDecisionDetailedQuery(Predicate { where("$it.id = ${bind(uuid)}") }))
        .exactlyOneOrNull<FeeDecisionDetailed>()
        ?.let { it ->
            it.copy(
                partnerIsCodebtor =
                    partnerIsCodebtor(
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
    period: DateRange? = null,
    status: List<FeeDecisionStatus>? = null,
    lockForUpdate: Boolean = false
): List<FeeDecision> {
    val headPredicate = Predicate<Any> { where("$it.head_of_family_id = ${bind(headOfFamilyId)}") }
    val validPredicate =
        if (period == null) Predicate.alwaysTrue()
        else Predicate<Any> { where("$it.valid_during && ${bind(period)}") }
    val statusPredicate =
        if (status == null) Predicate.alwaysTrue()
        else
            Predicate<Any> {
                where(
                    "$it.status = ANY (${bind(status.map { s -> s.name })}::fee_decision_status[])"
                )
            }
    val predicate = Predicate.all(listOf(headPredicate, validPredicate, statusPredicate))
    return createQuery(feeDecisionQuery(predicate, lockForUpdate)).toList<FeeDecision>()
}

fun Database.Transaction.approveFeeDecisionDraftsForSending(
    ids: List<FeeDecisionId>,
    approvedBy: EmployeeId,
    approvedAt: HelsinkiDateTime,
    decisionHandlerId: EmployeeId?,
    isRetroactive: Boolean = false,
    alwaysUseDaycareFinanceDecisionHandler: Boolean
) {
    val sql =
        """
        WITH youngest_child AS (
            SELECT
                fee_decision_child.fee_decision_id AS decision_id,
                daycare.finance_decision_handler AS finance_decision_handler_id,
                row_number() OVER (PARTITION BY (fee_decision_id) ORDER BY child_date_of_birth DESC, sibling_discount, child_id) AS rownum
            FROM fee_decision_child
            LEFT JOIN daycare ON fee_decision_child.placement_unit_id = daycare.id
        )
        UPDATE fee_decision
        SET
            status = :status::fee_decision_status,
            decision_number = nextval('fee_decision_number_sequence'),
            approved_by_id = :approvedBy,
            decision_handler_id = CASE
                WHEN :decisionHandlerId IS NOT NULL THEN :decisionHandlerId
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
        """
            .trimIndent()

    val batch = prepareBatch(sql)
    ids.map { id ->
        batch
            .bind("id", id)
            .bind("status", FeeDecisionStatus.WAITING_FOR_SENDING.toString())
            .bind("approvedBy", approvedBy)
            .bind("isRetroactive", isRetroactive)
            .bind("approvedAt", approvedAt)
            .bind("decisionHandlerId", decisionHandlerId)
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

fun Database.Transaction.setFeeDecisionSent(clock: EvakaClock, ids: List<FeeDecisionId>) {
    val sql =
        """
        UPDATE fee_decision
        SET
            status = :status::fee_decision_status,
            sent_at = :now
        WHERE id = :id
    """

    val batch = prepareBatch(sql)
    ids.forEach { id ->
        batch
            .bind("now", clock.now())
            .bind("id", id)
            .bind("status", FeeDecisionStatus.SENT.toString())
            .add()
    }
    batch.execute()
}

fun Database.Transaction.updateFeeDecisionStatusAndDates(updatedDecisions: List<FeeDecision>) {
    prepareBatch(
            "UPDATE fee_decision SET status = :status::fee_decision_status, valid_during = :validDuring WHERE id = :id"
        )
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

    createUpdate(sql).bind("id", id).bind("key", key).execute()
}

fun Database.Read.getFeeDecisionDocumentKey(decisionId: FeeDecisionId): String? {
    val sql =
        """
        SELECT document_key 
        FROM fee_decision
        WHERE id = :id
    """

    return createQuery(sql).bind("id", decisionId).exactlyOneOrNull<String>()
}

fun Database.Transaction.setFeeDecisionType(id: FeeDecisionId, type: FeeDecisionType) {
    // language=SQL
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

fun Database.Transaction.setFeeDecisionToIgnored(id: FeeDecisionId) {
    createUpdate("UPDATE fee_decision SET status = 'IGNORED' WHERE id = :id AND status = 'DRAFT'")
        .bind("id", id)
        .updateExactlyOne()
}

fun Database.Transaction.removeFeeDecisionIgnore(id: FeeDecisionId) {
    createUpdate("DELETE FROM fee_decision WHERE id = :id AND status = 'IGNORED'")
        .bind("id", id)
        .updateExactlyOne()
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
): Boolean =
    partnerId != null &&
        createQuery(
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
            .exactlyOne<Boolean>()
