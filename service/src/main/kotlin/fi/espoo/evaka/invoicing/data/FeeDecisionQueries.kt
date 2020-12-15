// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.data

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fi.espoo.evaka.invoicing.controller.DistinctiveParams
import fi.espoo.evaka.invoicing.controller.FeeDecisionSortParam
import fi.espoo.evaka.invoicing.controller.SortDirection
import fi.espoo.evaka.invoicing.domain.DecisionIncome
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionDetailed
import fi.espoo.evaka.invoicing.domain.FeeDecisionPart
import fi.espoo.evaka.invoicing.domain.FeeDecisionPartDetailed
import fi.espoo.evaka.invoicing.domain.FeeDecisionPartSummary
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.FeeDecisionSummary
import fi.espoo.evaka.invoicing.domain.FeeDecisionType
import fi.espoo.evaka.invoicing.domain.PermanentPlacement
import fi.espoo.evaka.invoicing.domain.PersonData
import fi.espoo.evaka.invoicing.domain.PlacementType
import fi.espoo.evaka.invoicing.domain.ServiceNeed
import fi.espoo.evaka.invoicing.domain.UnitData
import fi.espoo.evaka.invoicing.domain.merge
import fi.espoo.evaka.shared.db.disjointNumberQuery
import fi.espoo.evaka.shared.db.freeTextSearchQuery
import fi.espoo.evaka.shared.db.getEnum
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.utils.splitSearchText
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.statement.StatementContext
import org.postgresql.util.PGobject
import java.sql.ResultSet
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

val feeDecisionQueryBase =
    """
    SELECT
        decision.*,
        part.child,
        part.date_of_birth,
        part.placement_unit,
        part.placement_type,
        part.service_need,
        part.base_fee,
        part.sibling_discount,
        part.fee,
        part.fee_alterations
    FROM fee_decision as decision
        LEFT JOIN fee_decision_part as part ON decision.id = part.fee_decision_id
    """.trimIndent()

val feeDecisionDetailedQueryBase =
    """
    SELECT
        decision.*,
        part.child,
        part.date_of_birth,
        part.placement_unit,
        part.placement_type,
        part.service_need,
        part.base_fee,
        part.sibling_discount,
        part.fee,
        part.fee_alterations,
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
        care_area.name as placement_unit_area_name
    FROM fee_decision as decision
        LEFT JOIN fee_decision_part as part ON decision.id = part.fee_decision_id
        LEFT JOIN person as head ON decision.head_of_family = head.id
        LEFT JOIN person as partner ON decision.partner = partner.id
        LEFT JOIN person as child ON part.child = child.id
        LEFT JOIN daycare ON part.placement_unit = daycare.id
        LEFT JOIN care_area ON daycare.care_area_id = care_area.id
        LEFT JOIN employee as approved_by ON decision.approved_by = approved_by.id
    """.trimIndent()

private val decisionNumberRegex = "^\\d{7,}$".toRegex()

fun upsertFeeDecisions(h: Handle, mapper: ObjectMapper, decisions: List<FeeDecision>) {
    upsertDecisions(h, mapper, decisions)
    replaceParts(h, mapper, decisions)
}

private fun upsertDecisions(h: Handle, mapper: ObjectMapper, decisions: List<FeeDecision>) {
    val sql =
        """
        INSERT INTO fee_decision (
            id,
            status,
            decision_number,
            decision_type,
            valid_from,
            valid_to,
            head_of_family,
            partner,
            head_of_family_income,
            partner_income,
            family_size,
            pricing,
            created_at
        ) VALUES (
            :id,
            :status,
            :decision_number,
            :decision_type,
            :valid_from,
            :valid_to,
            :head_of_family,
            :partner,
            :head_of_family_income,
            :partner_income,
            :family_size,
            :pricing,
            :created_at
        ) ON CONFLICT (id) DO UPDATE SET
            status = :status,
            decision_number = :decision_number,
            decision_type = :decision_type,
            valid_from = :valid_from,
            valid_to = :valid_to,
            head_of_family = :head_of_family,
            partner = :partner,
            head_of_family_income = :head_of_family_income,
            partner_income = :partner_income,
            family_size = :family_size,
            pricing = :pricing
    """

    val batch = h.prepareBatch(sql)
    decisions.forEach { decision ->
        batch
            .bindMap(
                mapOf(
                    "id" to decision.id,
                    "status" to decision.status.toString(),
                    "decision_number" to decision.decisionNumber,
                    "decision_type" to decision.decisionType.toString(),
                    "valid_from" to decision.validFrom,
                    "valid_to" to decision.validTo,
                    "head_of_family" to decision.headOfFamily.id,
                    "partner" to decision.partner?.id,
                    "family_size" to decision.familySize,
                    "pricing" to decision.pricing.let {
                        PGobject().apply {
                            type = "jsonb"
                            value = mapper.writeValueAsString(it)
                        }
                    },
                    "head_of_family_income" to decision.headOfFamilyIncome?.let {
                        PGobject().apply {
                            type = "jsonb"
                            value = mapper.writeValueAsString(it)
                        }
                    },
                    "partner_income" to decision.partnerIncome?.let {
                        PGobject().apply {
                            type = "jsonb"
                            value = mapper.writeValueAsString(it)
                        }
                    },
                    "created_at" to decision.createdAt.atOffset(ZoneOffset.UTC)
                )
            )
            .add()
    }
    batch.execute()
}

private fun replaceParts(h: Handle, mapper: ObjectMapper, decisions: List<FeeDecision>) {
    val partsWithDecisionIds = decisions.map { it.id to it.parts }
    deleteParts(h, partsWithDecisionIds.map { it.first })
    insertParts(h, mapper, partsWithDecisionIds)
}

private fun insertParts(h: Handle, mapper: ObjectMapper, decisions: List<Pair<UUID, List<FeeDecisionPart>>>) {
    val sql =
        """
        INSERT INTO fee_decision_part (
            id,
            fee_decision_id,
            child,
            date_of_birth,
            placement_unit,
            placement_type,
            service_need,
            base_fee,
            sibling_discount,
            fee,
            fee_alterations
        ) VALUES (
            :id,
            :fee_decision_id,
            :child,
            :date_of_birth,
            :placement_unit,
            :placement_type,
            :service_need,
            :base_fee,
            :sibling_discount,
            :fee,
            :fee_alterations
        )
    """

    val batch = h.prepareBatch(sql)
    decisions.forEach { (decisionId, parts) ->
        parts.forEach { part ->
            batch
                .bindMap(
                    mapOf(
                        "id" to UUID.randomUUID(),
                        "fee_decision_id" to decisionId,
                        "child" to part.child.id,
                        "date_of_birth" to part.child.dateOfBirth,
                        "placement_unit" to part.placement.unit,
                        "placement_type" to part.placement.type.toString(),
                        "service_need" to part.placement.serviceNeed.toString(),
                        "base_fee" to part.baseFee,
                        "sibling_discount" to part.siblingDiscount,
                        "fee" to part.fee,
                        "fee_alterations" to PGobject().apply {
                            type = "jsonb"
                            value = mapper.writeValueAsString(part.feeAlterations)
                        }
                    )
                )
                .add()
        }
    }
    batch.execute()
}

private fun deleteParts(h: Handle, decisionIds: List<UUID>) {
    if (decisionIds.isEmpty()) return

    h.createUpdate("DELETE FROM fee_decision_part WHERE fee_decision_id = ANY(:decisionIds)")
        .bind("decisionIds", decisionIds.toTypedArray())
        .execute()
}

fun deleteFeeDecisions(h: Handle, ids: List<UUID>) {
    if (ids.isEmpty()) return

    h.createUpdate("DELETE FROM fee_decision WHERE id = ANY(:ids)")
        .bind("ids", ids.toTypedArray())
        .execute()
}

fun searchFeeDecisions(
    h: Handle,
    page: Int,
    pageSize: Int,
    sortBy: FeeDecisionSortParam,
    sortDirection: SortDirection,
    statuses: List<FeeDecisionStatus>,
    areas: List<String>,
    unit: UUID?,
    distinctiveParams: List<DistinctiveParams>,
    searchTerms: String = "",
    startDate: LocalDate?,
    endDate: LocalDate?,
    searchByStartDate: Boolean = false,
    financeDecisionManagerId: UUID?
): Pair<Int, List<FeeDecisionSummary>> {
    val sortColumn = when (sortBy) {
        FeeDecisionSortParam.HEAD_OF_FAMILY -> "head.last_name"
        FeeDecisionSortParam.VALIDITY -> "decision.valid_from"
        FeeDecisionSortParam.NUMBER -> "decision.decision_number"
        FeeDecisionSortParam.CREATED -> "decision.created_at"
        FeeDecisionSortParam.SENT -> "decision.sent_at"
        FeeDecisionSortParam.STATUS -> "decision.status"
        FeeDecisionSortParam.FINAL_PRICE -> "sum"
    }

    val params = mapOf(
        "page" to page,
        "pageSize" to pageSize,
        "status" to statuses.map { it.toString() }.toTypedArray(),
        "area" to areas.toTypedArray(),
        "unit" to unit,
        "missingServiceNeed" to ServiceNeed.MISSING.toString(),
        "espooPostOffice" to "ESPOO",
        "start_date" to startDate,
        "end_date" to endDate,
        "finance_decision_manager" to financeDecisionManagerId
    )

    val numberParamsRaw = splitSearchText(searchTerms).filter(decisionNumberRegex::matches)
    val searchTextWithoutNumbers = searchTerms.replace(decisionNumberRegex, "")

    val (freeTextQuery, freeTextParams) = freeTextSearchQuery(listOf("head", "partner", "child"), searchTextWithoutNumbers)

    val withNullHours = distinctiveParams.contains(DistinctiveParams.UNCONFIRMED_HOURS)

    val havingExternalChildren = distinctiveParams.contains(DistinctiveParams.EXTERNAL_CHILD)

    val retroactiveOnly = distinctiveParams.contains(DistinctiveParams.RETROACTIVE)

    val (numberQuery, numberParams) = disjointNumberQuery("decision", "decision_number", numberParamsRaw)

    val conditions = listOfNotNull(
        if (statuses.isNotEmpty()) "status = ANY(:status)" else null,
        if (areas.isNotEmpty()) "youngest_child.area = ANY(:area)" else null,
        if (unit != null) "part.placement_unit = :unit" else null,
        if (withNullHours) "part.service_need = :missingServiceNeed" else null,
        if (havingExternalChildren) "child.post_office <> '' AND child.post_office NOT ILIKE :espooPostOffice" else null,
        if (retroactiveOnly) "decision.valid_from < date_trunc('month', COALESCE(decision.sent_at, now()))" else null,
        if (numberParamsRaw.isNotEmpty()) numberQuery else null,
        if (searchTextWithoutNumbers.isNotBlank()) freeTextQuery else null,
        if ((startDate != null || endDate != null) && !searchByStartDate) "daterange(:start_date, :end_date, '[]') && daterange(valid_from, valid_to, '[]')" else null,
        if ((startDate != null || endDate != null) && searchByStartDate) "daterange(:start_date, :end_date, '[]') @> valid_from" else null,
        if (financeDecisionManagerId != null) "finance_decision_manager.finance_decision_manager_id = :finance_decision_manager" else null
    )

    val youngestChildQuery =
        """
        WITH youngest_child AS (
            SELECT
                fee_decision_part.fee_decision_id AS decision_id,
                care_area.short_name AS area,
                row_number() OVER (PARTITION BY (fee_decision_id) ORDER BY date_of_birth DESC) AS rownum
            FROM fee_decision_part
            LEFT JOIN daycare ON fee_decision_part.placement_unit = daycare.id
            LEFT JOIN care_area ON daycare.care_area_id = care_area.id
        )
        """.trimIndent()
    val youngestChildJoin = "LEFT JOIN youngest_child ON decision.id = youngest_child.decision_id AND rownum = 1"

    val financeDecisionManagerQuery =
        """
        WITH finance_decision_manager AS (SELECT id as daycare_id, finance_decision_manager as finance_decision_manager_id FROM daycare)
        """.trimIndent()
    val financeDecisionManagerJoin = "LEFT JOIN finance_decision_manager ON part.placement_unit = finance_decision_manager.daycare_id"

    // language=sql
    val sql =
        """
        WITH decision_ids AS (
            ${if (areas.isNotEmpty()) youngestChildQuery else ""}
            ${if (financeDecisionManagerId != null) financeDecisionManagerQuery else ""}
            SELECT decision.id, count(*) OVER (), max(sums.sum) sum
            FROM fee_decision AS decision
            LEFT JOIN fee_decision_part AS part ON decision.id = part.fee_decision_id
            LEFT JOIN person AS head ON decision.head_of_family = head.id
            LEFT JOIN person AS partner ON decision.head_of_family = partner.id
            LEFT JOIN person AS child ON part.child = child.id
            LEFT JOIN (
                SELECT final_prices.id, sum(final_prices.final_price) sum
                FROM (
                    SELECT fd.id, coalesce(fee + coalesce(sum(effects.effect), 0), 0) AS final_price
                    FROM fee_decision fd
                    LEFT JOIN fee_decision_part fdp ON fd.id = fdp.fee_decision_id
                    LEFT JOIN (
                        SELECT id, (jsonb_array_elements(fee_alterations)->>'effect')::integer effect
                        FROM fee_decision_part
                    ) effects ON fdp.id = effects.id
                    GROUP BY fd.id, fdp.id
                ) final_prices
                GROUP BY final_prices.id
            ) sums ON decision.id = sums.id
            ${if (areas.isNotEmpty()) youngestChildJoin else ""}
            ${if (financeDecisionManagerId != null) financeDecisionManagerJoin else ""}
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
            part.child,
            part.date_of_birth,
            part.fee,
            part.fee_alterations,
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
            LEFT JOIN fee_decision_part AS part ON decision.id = part.fee_decision_id
            LEFT JOIN person AS head ON decision.head_of_family = head.id
            LEFT JOIN person AS child ON part.child = child.id
        ORDER BY $sortColumn ${sortDirection.name}, decision.id, part.date_of_birth DESC
        """.trimIndent()

    val result = h.createQuery(sql)
        .bindMap(params + freeTextParams + numberParams)
        .map { rs, ctx ->
            rs.getInt("count") to toFeeDecisionSummary(rs, ctx)
        }
        .toList()
    val count = if (result.isEmpty()) 0 else result.first().first

    return count to result.map { (_, decisions) -> decisions }
        .merge()
}

fun getFeeDecisionsByIds(h: Handle, mapper: ObjectMapper, ids: List<UUID>): List<FeeDecision> {
    if (ids.isEmpty()) return emptyList()

    val sql =
        """
        $feeDecisionQueryBase
        WHERE decision.id = ANY(:ids)
    """

    return h.createQuery(sql)
        .bind("ids", ids.toTypedArray())
        .map(toFeeDecision(mapper))
        .let { it.merge() }
}

fun getDetailedFeeDecisionsByIds(h: Handle, mapper: ObjectMapper, ids: List<UUID>): List<FeeDecisionDetailed> {
    if (ids.isEmpty()) return emptyList()

    val sql =
        """
        $feeDecisionDetailedQueryBase
        WHERE decision.id = ANY(:ids)
        ORDER BY part.date_of_birth DESC
    """

    return h.createQuery(sql)
        .bind("ids", ids.toTypedArray())
        .map(toFeeDecisionDetailed(mapper))
        .let { it.merge() }
}

fun getFeeDecision(h: Handle, mapper: ObjectMapper, uuid: UUID): FeeDecisionDetailed? {
    val sql =
        """
        $feeDecisionDetailedQueryBase
        WHERE decision.id = :id
        ORDER BY part.date_of_birth DESC
    """

    return h.createQuery(sql)
        .bind("id", uuid)
        .map(toFeeDecisionDetailed(mapper))
        .let { it.merge() }
        .firstOrNull()
}

fun findFeeDecisionsForHeadOfFamily(
    h: Handle,
    mapper: ObjectMapper,
    headOfFamilyId: UUID,
    period: DateRange?,
    status: List<FeeDecisionStatus>?
): List<FeeDecision> {
    val sql =
        """
        $feeDecisionQueryBase
        WHERE
            decision.head_of_family = :headOfFamilyId
            ${period?.let { "AND daterange(decision.valid_from, decision.valid_to, '[]') && daterange(:periodStart, :periodEnd, '[]')" } ?: ""}
            ${status?.let { "AND decision.status = ANY(:status)" } ?: ""}
    """

    return h.createQuery(sql)
        .bind("headOfFamilyId", headOfFamilyId)
        .let { query ->
            if (period != null) query.bind("periodStart", period.start).bind("periodEnd", period.end)
            else query
        }
        .let { query -> if (status != null) query.bind("status", status.map { it.name }.toTypedArray()) else query }
        .map(toFeeDecision(mapper))
        .let { it.merge() }
}

fun approveFeeDecisionDraftsForSending(h: Handle, ids: List<UUID>, approvedBy: UUID) {
    val sql =
        """
        UPDATE fee_decision
        SET
            status = :status,
            decision_number = nextval('fee_decision_number_sequence'),
            approved_by = :approvedBy,
            approved_at = NOW()
        WHERE id = :id
    """

    val batch = h.prepareBatch(sql)
    ids.map { id ->
        batch
            .bind("id", id)
            .bind("status", FeeDecisionStatus.WAITING_FOR_SENDING.toString())
            .bind("approvedBy", approvedBy)
            .add()
    }
    batch.execute()
}

fun setFeeDecisionWaitingForManualSending(h: Handle, id: UUID) {
    val sql =
        """
        UPDATE fee_decision
        SET
            status = :status
        WHERE id = :id
        AND status = :requiredStatus
    """

    h.createUpdate(sql)
        .bind("id", id)
        .bind("status", FeeDecisionStatus.WAITING_FOR_MANUAL_SENDING.toString())
        .bind("requiredStatus", FeeDecisionStatus.WAITING_FOR_SENDING.toString())
        .execute()
}

fun setFeeDecisionSent(h: Handle, ids: List<UUID>) {
    val sql =
        """
        UPDATE fee_decision
        SET
            status = :status,
            sent_at = NOW()
        WHERE id = :id
    """

    val batch = h.prepareBatch(sql)
    ids.forEach { id ->
        batch
            .bind("id", id)
            .bind("status", FeeDecisionStatus.SENT.toString())
            .add()
    }
    batch.execute()
}

fun updateFeeDecisionDocumentKey(h: Handle, id: UUID, key: String) {
    val sql =
        """
        UPDATE fee_decision
        SET document_key = :key
        WHERE id = :id
    """

    h.createUpdate(sql)
        .bind("id", id)
        .bind("key", key)
        .execute()
}

fun getFeeDecisionDocumentKey(h: Handle, decisionId: UUID): String? {
    val sql =
        """
        SELECT document_key 
        FROM fee_decision
        WHERE id = :id
    """

    return h.createQuery(sql)
        .bind("id", decisionId)
        .mapTo(String::class.java)
        .firstOrNull()
}

fun setFeeDecisionType(h: Handle, id: UUID, type: FeeDecisionType) {
    //language=SQL
    val sql =
        """
        UPDATE fee_decision
        SET decision_type = :type
        WHERE id = :id
            AND status = :requiredStatus
    """

    h.createUpdate(sql)
        .bind("id", id)
        .bind("type", type.toString())
        .bind("requiredStatus", FeeDecisionStatus.DRAFT.toString())
        .execute()
}

fun Handle.lockFeeDecisionsForHeadOfFamily(headOfFamily: UUID) {
    createUpdate("SELECT id FROM fee_decision WHERE head_of_family = :headOfFamily FOR UPDATE")
        .bind("headOfFamily", headOfFamily)
        .execute()
}

fun Handle.lockFeeDecisions(ids: List<UUID>) {
    createUpdate("SELECT id FROM fee_decision WHERE id = ANY(:ids) FOR UPDATE")
        .bind("ids", ids.toTypedArray())
        .execute()
}

fun toFeeDecision(mapper: ObjectMapper) = { rs: ResultSet, _: StatementContext ->
    FeeDecision(
        id = UUID.fromString(rs.getString("id")),
        status = FeeDecisionStatus.valueOf(rs.getString("status")),
        decisionNumber = rs.getObject("decision_number") as Long?, // getLong returns 0 for null values
        decisionType = rs.getEnum("decision_type"),
        validFrom = rs.getDate("valid_from").toLocalDate(),
        validTo = rs.getDate("valid_to")?.toLocalDate(),
        headOfFamily = PersonData.JustId(UUID.fromString(rs.getString("head_of_family"))),
        partner = rs.getString("partner")?.let { PersonData.JustId(UUID.fromString(it)) },
        headOfFamilyIncome = rs.getString("head_of_family_income")?.let { mapper.readValue<DecisionIncome>(it) },
        partnerIncome = rs.getString("partner_income")?.let { mapper.readValue<DecisionIncome>(it) },
        familySize = rs.getInt("family_size"),
        pricing = mapper.readValue(rs.getString("pricing")),
        // child is not nullable so if it's missing there was nothing to join to the decision
        parts = rs.getString("child")?.let {
            listOf(
                FeeDecisionPart(
                    child = PersonData.WithDateOfBirth(
                        id = UUID.fromString(rs.getString("child")),
                        dateOfBirth = rs.getDate("date_of_birth").toLocalDate()
                    ),
                    placement = PermanentPlacement(
                        unit = UUID.fromString(rs.getString("placement_unit")),
                        type = PlacementType.valueOf(rs.getString("placement_type")),
                        serviceNeed = ServiceNeed.valueOf(rs.getString("service_need"))
                    ),
                    baseFee = rs.getInt("base_fee"),
                    siblingDiscount = rs.getInt("sibling_discount"),
                    fee = rs.getInt("fee"),
                    feeAlterations = mapper.readValue(rs.getString("fee_alterations"))
                )
            )
        } ?: emptyList(),
        documentKey = rs.getString("document_key"),
        approvedBy = rs.getString("approved_by")?.let { PersonData.JustId(UUID.fromString(it)) },
        approvedAt = rs.getTimestamp("approved_at")?.toInstant(),
        createdAt = rs.getTimestamp("created_at").toInstant(),
        sentAt = rs.getTimestamp("sent_at")?.toInstant()
    )
}

fun toFeeDecisionDetailed(mapper: ObjectMapper) = { rs: ResultSet, _: StatementContext ->
    FeeDecisionDetailed(
        id = UUID.fromString(rs.getString("id")),
        status = FeeDecisionStatus.valueOf(rs.getString("status")),
        decisionNumber = rs.getObject("decision_number") as Long?, // getLong returns 0 for null values
        decisionType = FeeDecisionType.valueOf(rs.getString("decision_type")),
        validFrom = rs.getDate("valid_from").toLocalDate(),
        validTo = rs.getDate("valid_to")?.toLocalDate(),
        headOfFamily = PersonData.Detailed(
            id = UUID.fromString(rs.getString("head_of_family")),
            dateOfBirth = rs.getDate("head_date_of_birth").toLocalDate(),
            firstName = rs.getString("head_first_name"),
            lastName = rs.getString("head_last_name"),
            ssn = rs.getString("head_ssn"),
            streetAddress = rs.getString("head_street_address"),
            postalCode = rs.getString("head_postal_code"),
            postOffice = rs.getString("head_post_office"),
            language = rs.getString("head_language"),
            restrictedDetailsEnabled = rs.getBoolean("head_restricted_details_enabled"),
            forceManualFeeDecisions = rs.getBoolean("head_force_manual_fee_decisions")
        ),
        partner = rs.getString("partner")?.let { id ->
            PersonData.Detailed(
                id = UUID.fromString(id),
                dateOfBirth = rs.getDate("partner_date_of_birth").toLocalDate(),
                firstName = rs.getString("partner_first_name"),
                lastName = rs.getString("partner_last_name"),
                ssn = rs.getString("partner_ssn"),
                streetAddress = rs.getString("partner_street_address"),
                postalCode = rs.getString("partner_postal_code"),
                postOffice = rs.getString("partner_post_office"),
                restrictedDetailsEnabled = rs.getBoolean("partner_restricted_details_enabled")
            )
        },
        headOfFamilyIncome = rs.getString("head_of_family_income")?.let { mapper.readValue<DecisionIncome>(it) },
        partnerIncome = rs.getString("partner_income")?.let { mapper.readValue<DecisionIncome>(it) },
        familySize = rs.getInt("family_size"),
        pricing = mapper.readValue(rs.getString("pricing")),
        // child is not nullable so if it's missing there was nothing to join to the decision
        parts = rs.getString("child")?.let {
            listOf(
                FeeDecisionPartDetailed(
                    child = PersonData.Detailed(
                        id = UUID.fromString(rs.getString("child")),
                        dateOfBirth = rs.getDate("date_of_birth").toLocalDate(),
                        firstName = rs.getString("child_first_name"),
                        lastName = rs.getString("child_last_name"),
                        ssn = rs.getString("child_ssn"),
                        streetAddress = rs.getString("child_address"),
                        postalCode = rs.getString("child_postal_code"),
                        postOffice = rs.getString("child_post_office"),
                        restrictedDetailsEnabled = rs.getBoolean("child_restricted_details_enabled")
                    ),
                    placement = PermanentPlacement(
                        unit = UUID.fromString(rs.getString("placement_unit")),
                        type = PlacementType.valueOf(rs.getString("placement_type")),
                        serviceNeed = ServiceNeed.valueOf(rs.getString("service_need"))
                    ),
                    placementUnit = UnitData.Detailed(
                        id = UUID.fromString(rs.getString("placement_unit")),
                        name = rs.getString("placement_unit_name"),
                        language = rs.getString("placement_unit_lang"),
                        areaId = UUID.fromString(rs.getString("placement_unit_area_id")),
                        areaName = rs.getString("placement_unit_area_name")
                    ),
                    baseFee = rs.getInt("base_fee"),
                    siblingDiscount = rs.getInt("sibling_discount"),
                    fee = rs.getInt("fee"),
                    feeAlterations = mapper.readValue(rs.getString("fee_alterations"))
                )
            )
        } ?: emptyList(),
        documentKey = rs.getString("document_key"),
        approvedBy = rs.getString("approved_by")?.let { id ->
            PersonData.WithName(
                id = UUID.fromString(id),
                firstName = rs.getString("approved_by_first_name"),
                lastName = rs.getString("approved_by_last_name")
            )
        },
        approvedAt = rs.getTimestamp("approved_at")?.toInstant(),
        createdAt = rs.getTimestamp("created_at").toInstant(),
        sentAt = rs.getTimestamp("sent_at")?.toInstant()
    )
}

val toFeeDecisionSummary = { rs: ResultSet, _: StatementContext ->
    FeeDecisionSummary(
        id = UUID.fromString(rs.getString("id")),
        status = FeeDecisionStatus.valueOf(rs.getString("status")),
        decisionNumber = rs.getObject("decision_number") as Long?, // getLong returns 0 for null values
        validFrom = rs.getDate("valid_from").toLocalDate(),
        validTo = rs.getDate("valid_to")?.toLocalDate(),
        headOfFamily = PersonData.Basic(
            id = UUID.fromString(rs.getString("head_of_family")),
            dateOfBirth = rs.getDate("head_date_of_birth").toLocalDate(),
            firstName = rs.getString("head_first_name"),
            lastName = rs.getString("head_last_name"),
            ssn = rs.getString("head_ssn")
        ),
        // child is not nullable so if it's missing there was nothing to join to the decision
        parts = rs.getString("child")?.let {
            listOf(
                FeeDecisionPartSummary(
                    child = PersonData.Basic(
                        id = UUID.fromString(rs.getString("child")),
                        dateOfBirth = rs.getDate("date_of_birth").toLocalDate(),
                        firstName = rs.getString("child_first_name"),
                        lastName = rs.getString("child_last_name"),
                        ssn = rs.getString("child_ssn")
                    )
                )
            )
        } ?: emptyList(),
        approvedAt = rs.getTimestamp("approved_at")?.toInstant(),
        createdAt = rs.getTimestamp("created_at").toInstant(),

        sentAt = rs.getTimestamp("sent_at")?.toInstant(),
        finalPrice = rs.getInt("sum")
    )
}
