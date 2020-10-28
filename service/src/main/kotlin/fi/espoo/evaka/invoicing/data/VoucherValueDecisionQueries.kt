// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.data

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fi.espoo.evaka.invoicing.controller.SortDirection
import fi.espoo.evaka.invoicing.controller.VoucherValueDecisionSortParam
import fi.espoo.evaka.invoicing.domain.DecisionIncome
import fi.espoo.evaka.invoicing.domain.PermanentPlacement
import fi.espoo.evaka.invoicing.domain.PersonData
import fi.espoo.evaka.invoicing.domain.PlacementType
import fi.espoo.evaka.invoicing.domain.ServiceNeed
import fi.espoo.evaka.invoicing.domain.UnitData
import fi.espoo.evaka.invoicing.domain.VoucherValueDecision
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionDetailed
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionPart
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionPartDetailed
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionPartSummary
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionSummary
import fi.espoo.evaka.invoicing.domain.merge
import fi.espoo.evaka.shared.db.bindNullable
import fi.espoo.evaka.shared.db.freeTextSearchQuery
import fi.espoo.evaka.shared.db.getEnum
import fi.espoo.evaka.shared.db.getUUID
import fi.espoo.evaka.shared.domain.Period
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.statement.StatementContext
import org.postgresql.util.PGobject
import java.sql.ResultSet
import java.time.ZoneOffset
import java.util.UUID

// language=sql
val voucherValueDecisionQueryBase =
    """
    SELECT
        decision.*,
        part.child,
        part.date_of_birth,
        part.placement_unit,
        part.placement_type,
        part.service_need,
        part.base_co_payment,
        part.sibling_discount,
        part.co_payment,
        part.fee_alterations
    FROM voucher_value_decision as decision
    LEFT JOIN voucher_value_decision_part as part ON decision.id = part.voucher_value_decision_id
    """.trimIndent()

fun Handle.upsertValueDecisions(mapper: ObjectMapper, decisions: List<VoucherValueDecision>) {
    upsertDecisions(mapper, decisions)
    replaceParts(mapper, decisions)
}

private fun Handle.upsertDecisions(mapper: ObjectMapper, decisions: List<VoucherValueDecision>) {
    val sql =
        """
        INSERT INTO voucher_value_decision (
            id,
            status,
            valid_from,
            valid_to,
            decision_number,
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
            :valid_from,
            :valid_to,
            :decision_number,
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
            valid_from = :valid_from,
            valid_to = :valid_to,
            head_of_family = :head_of_family,
            partner = :partner,
            head_of_family_income = :head_of_family_income,
            partner_income = :partner_income,
            family_size = :family_size,
            pricing = :pricing
        """

    val batch = prepareBatch(sql)
    decisions.forEach { decision ->
        batch
            .bindMap(
                mapOf(
                    "id" to decision.id,
                    "status" to decision.status.toString(),
                    "valid_from" to decision.validFrom,
                    "valid_to" to decision.validTo,
                    "decision_number" to decision.decisionNumber,
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

private fun Handle.replaceParts(mapper: ObjectMapper, decisions: List<VoucherValueDecision>) {
    val partsWithDecisionIds = decisions.map { it.id to it.parts }
    deleteParts(partsWithDecisionIds.map { it.first })
    insertParts(mapper, partsWithDecisionIds)
}

private fun Handle.insertParts(mapper: ObjectMapper, decisions: List<Pair<UUID, List<VoucherValueDecisionPart>>>) {
    val sql =
        """
        INSERT INTO voucher_value_decision_part (
            id,
            voucher_value_decision_id,
            child,
            date_of_birth,
            placement_unit,
            placement_type,
            service_need,
            base_co_payment,
            sibling_discount,
            co_payment,
            fee_alterations
        ) VALUES (
            :id,
            :value_decision_id,
            :child,
            :date_of_birth,
            :placement_unit,
            :placement_type,
            :service_need,
            :base_co_payment,
            :sibling_discount,
            :co_payment,
            :fee_alterations
        )
    """

    val batch = prepareBatch(sql)
    decisions.forEach { (decisionId, parts) ->
        parts.forEach { part ->
            batch
                .bindMap(
                    mapOf(
                        "id" to UUID.randomUUID(),
                        "value_decision_id" to decisionId,
                        "child" to part.child.id,
                        "date_of_birth" to part.child.dateOfBirth,
                        "placement_unit" to part.placement.unit,
                        "placement_type" to part.placement.type.toString(),
                        "service_need" to part.placement.serviceNeed.toString(),
                        "base_co_payment" to part.baseCoPayment,
                        "sibling_discount" to part.siblingDiscount,
                        "co_payment" to part.coPayment,
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

private fun Handle.deleteParts(decisionIds: List<UUID>) {
    if (decisionIds.isEmpty()) return

    createUpdate("DELETE FROM voucher_value_decision_part WHERE voucher_value_decision_id = ANY(:decisionIds)")
        .bind("decisionIds", decisionIds.toTypedArray())
        .execute()
}

fun Handle.getValueDecisionsByIds(mapper: ObjectMapper, ids: List<UUID>): List<VoucherValueDecision> {
    // language=sql
    val sql =
        """
        $voucherValueDecisionQueryBase
        WHERE decision.id = ANY(:ids)
        """

    return createQuery(sql)
        .bind("ids", ids.toTypedArray())
        .map(toVoucherValueDecision(mapper))
        .let { it.merge() }
}

fun Handle.findValueDecisionsForHeadOfFamily(
    mapper: ObjectMapper,
    headOfFamily: UUID,
    period: Period?,
    statuses: List<VoucherValueDecisionStatus>?
): List<VoucherValueDecision> {
    // language=sql
    val sql =
        """
        $voucherValueDecisionQueryBase
        WHERE decision.head_of_family = :headOfFamily
        AND (:period::daterange IS NULL OR daterange(decision.valid_from, decision.valid_to, '[]') && :period)
        AND (:statuses::text[] IS NULL OR decision.status = ANY(:statuses))
        """

    return createQuery(sql)
        .bind("headOfFamily", headOfFamily)
        .bindNullable("period", period)
        .bindNullable("statuses", statuses)
        .map(toVoucherValueDecision(mapper))
        .let { it.merge() }
}

fun Handle.deleteValueDecisions(ids: List<UUID>) {
    if (ids.isEmpty()) return

    createUpdate("DELETE FROM voucher_value_decision WHERE id = ANY(:ids)")
        .bind("ids", ids.toTypedArray())
        .execute()
}

fun Handle.searchValueDecisions(
    page: Int,
    pageSize: Int,
    sortBy: VoucherValueDecisionSortParam,
    sortDirection: SortDirection,
    status: VoucherValueDecisionStatus,
    areas: List<String>,
    unit: UUID?,
    searchTerms: String = ""
): Pair<Int, List<VoucherValueDecisionSummary>> {
    val sortColumn = when (sortBy) {
        VoucherValueDecisionSortParam.HEAD_OF_FAMILY -> "head.last_name"
        VoucherValueDecisionSortParam.STATUS -> "decision.status"
    }

    val params = mapOf(
        "page" to page,
        "pageSize" to pageSize,
        "status" to status.name,
        "areas" to areas.toTypedArray(),
        "unit" to unit
    )

    val (freeTextQuery, freeTextParams) = freeTextSearchQuery(listOf("head", "partner", "child"), searchTerms)

    // language=sql

    val sql =
        """
        WITH decision_ids AS (
            WITH youngest_child AS (
                SELECT
                    voucher_value_decision_part.voucher_value_decision_id AS decision_id,
                    care_area.short_name AS area,
                    row_number() OVER (PARTITION BY (voucher_value_decision_id) ORDER BY date_of_birth DESC) AS rownum
                FROM voucher_value_decision_part
                LEFT JOIN daycare ON voucher_value_decision_part.placement_unit = daycare.id
                LEFT JOIN care_area ON daycare.care_area_id = care_area.id
            )
            SELECT decision.id, count(*) OVER (), max(sums.sum) total_co_payment
            FROM voucher_value_decision AS decision
            LEFT JOIN voucher_value_decision_part AS part ON decision.id = part.voucher_value_decision_id
            LEFT JOIN person AS head ON decision.head_of_family = head.id
            LEFT JOIN person AS partner ON decision.head_of_family = partner.id
            LEFT JOIN person AS child ON part.child = child.id
            LEFT JOIN youngest_child ON decision.id = youngest_child.decision_id AND rownum = 1
            LEFT JOIN (
                SELECT final_co_payments.id, sum(final_co_payments.final_co_payment) sum
                FROM (
                    SELECT vd.id, coalesce(vdp.co_payment + coalesce(sum(effects.effect), 0), 0) AS final_co_payment
                    FROM voucher_value_decision vd
                    LEFT JOIN voucher_value_decision_part vdp ON vd.id = vdp.voucher_value_decision_id
                    LEFT JOIN (
                        SELECT id, (jsonb_array_elements(fee_alterations)->>'effect')::integer effect
                        FROM voucher_value_decision_part
                    ) effects ON vdp.id = effects.id
                    GROUP BY vd.id, vdp.id, vdp.co_payment
                ) final_co_payments
                GROUP BY final_co_payments.id
            ) sums ON decision.id = sums.id
            WHERE
                status = :status
                AND youngest_child.area = ANY(:areas)
                AND (:unit::uuid IS NULL OR part.placement_unit = :unit)
                AND $freeTextQuery
            GROUP BY decision.id
            -- we take a max here because the sort column is not in group by clause but it should be identical for all grouped rows
            ORDER BY max($sortColumn) $sortDirection, decision.id
            LIMIT :pageSize OFFSET :pageSize * :page
        )
        SELECT
            decision_ids.count,
            decision_ids.total_co_payment,
            decision.*,
            part.child,
            part.date_of_birth,
            head.date_of_birth AS head_date_of_birth,
            head.first_name AS head_first_name,
            head.last_name AS head_last_name,
            head.social_security_number AS head_ssn,
            head.force_manual_fee_decisions AS head_force_manual_fee_decisions,
            child.first_name AS child_first_name,
            child.last_name AS child_last_name,
            child.social_security_number AS child_ssn
        FROM decision_ids
            LEFT JOIN voucher_value_decision AS decision ON decision_ids.id = decision.id
            LEFT JOIN voucher_value_decision_part AS part ON decision.id = part.voucher_value_decision_id
            LEFT JOIN person AS head ON decision.head_of_family = head.id
            LEFT JOIN person AS child ON part.child = child.id
        ORDER BY $sortColumn $sortDirection, decision.id, part.date_of_birth DESC
        """.trimIndent()

    val result = this.createQuery(sql)
        .bindMap(params + freeTextParams)
        .map { rs, ctx ->
            rs.getInt("count") to toVoucherValueDecisionSummary(rs, ctx)
        }
        .toList()
    val count = if (result.isEmpty()) 0 else result.first().first

    return count to result.map { (_, decision) -> decision }.merge()
}

fun Handle.getVoucherValueDecision(mapper: ObjectMapper, id: UUID): VoucherValueDecisionDetailed? {
    // language=sql
    val sql =
        """
        SELECT
            decision.*,
            part.child,
            part.date_of_birth,
            part.placement_unit,
            part.placement_type,
            part.service_need,
            part.base_co_payment,
            part.sibling_discount,
            part.co_payment,
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
        FROM voucher_value_decision as decision
            LEFT JOIN voucher_value_decision_part as part ON decision.id = part.voucher_value_decision_id
            LEFT JOIN person as head ON decision.head_of_family = head.id
            LEFT JOIN person as partner ON decision.partner = partner.id
            LEFT JOIN person as child ON part.child = child.id
            LEFT JOIN daycare ON part.placement_unit = daycare.id
            LEFT JOIN care_area ON daycare.care_area_id = care_area.id
            LEFT JOIN employee as approved_by ON decision.approved_by = approved_by.id
        WHERE decision.id = :id
        ORDER BY part.date_of_birth DESC
    """

    return createQuery(sql)
        .bind("id", id)
        .map(toVoucherValueDecisionDetailed(mapper))
        .let { it.merge() }
        .singleOrNull()
}

fun Handle.approveValueDecisionDraftsForSending(ids: List<UUID>, approvedBy: UUID) {
    // language=sql
    val sql =
        """
        UPDATE voucher_value_decision SET
            status = :status,
            decision_number = nextval('voucher_value_decision_number_sequence'),
            approved_by = :approvedBy,
            approved_at = NOW()
        WHERE id = :id
        """.trimIndent()

    val batch = prepareBatch(sql)
    ids.forEach { id ->
        batch
            .bind("status", VoucherValueDecisionStatus.WAITING_FOR_SENDING)
            .bind("approvedBy", approvedBy)
            .bind("id", id)
            .add()
    }
    batch.execute()
}

fun Handle.getVoucherValueDecisionDocumentKey(id: UUID): String? {
    // language=sql
    val sql = "SELECT document_key FROM voucher_value_decision WHERE id = :id"

    return createQuery(sql)
        .bind("id", id)
        .mapTo<String>()
        .singleOrNull()
}

fun Handle.updateVoucherValueDecisionDocumentKey(id: UUID, documentKey: String) {
    // language=sql
    val sql = "UPDATE voucher_value_decision SET document_key = :documentKey WHERE id = :id"

    createUpdate(sql)
        .bind("id", id)
        .bind("documentKey", documentKey)
        .execute()
}

fun Handle.updateVoucherValueDecisionStatus(id: UUID, status: VoucherValueDecisionStatus) {
    // language=sql
    val sql = "UPDATE voucher_value_decision SET status = :status WHERE id = :id"

    createUpdate(sql)
        .bind("id", id)
        .bind("status", status)
        .execute()
}

fun Handle.lockValueDecisionsForHeadOfFamily(headOfFamily: UUID) {
    createUpdate("SELECT id FROM voucher_value_decision WHERE head_of_family = :headOfFamily FOR UPDATE")
        .bind("headOfFamily", headOfFamily)
        .execute()
}

fun Handle.lockValueDecisions(ids: List<UUID>) {
    createUpdate("SELECT id FROM voucher_value_decision WHERE id = ANY(:ids) FOR UPDATE")
        .bind("ids", ids.toTypedArray())
        .execute()
}

fun toVoucherValueDecision(mapper: ObjectMapper) = { rs: ResultSet, _: StatementContext ->
    VoucherValueDecision(
        id = rs.getUUID("id"),
        status = rs.getEnum("status"),
        decisionNumber = rs.getObject("decision_number") as Long?, // getLong returns 0 for null values
        validFrom = rs.getDate("valid_from").toLocalDate(),
        validTo = rs.getDate("valid_to")?.toLocalDate(),
        headOfFamily = PersonData.JustId(rs.getUUID("head_of_family")),
        partner = rs.getString("partner")?.let { PersonData.JustId(UUID.fromString(it)) },
        headOfFamilyIncome = rs.getString("head_of_family_income")?.let { mapper.readValue<DecisionIncome>(it) },
        partnerIncome = rs.getString("partner_income")?.let { mapper.readValue<DecisionIncome>(it) },
        familySize = rs.getInt("family_size"),
        pricing = mapper.readValue(rs.getString("pricing")),
        // child is not nullable so if it's missing there was nothing to join to the decision
        parts = rs.getString("child")?.let {
            listOf(
                VoucherValueDecisionPart(
                    child = PersonData.WithDateOfBirth(
                        id = rs.getUUID("child"),
                        dateOfBirth = rs.getDate("date_of_birth").toLocalDate()
                    ),
                    placement = PermanentPlacement(
                        unit = rs.getUUID("placement_unit"),
                        type = PlacementType.valueOf(rs.getString("placement_type")),
                        serviceNeed = ServiceNeed.valueOf(rs.getString("service_need"))
                    ),
                    baseCoPayment = rs.getInt("base_co_payment"),
                    siblingDiscount = rs.getInt("sibling_discount"),
                    coPayment = rs.getInt("co_payment"),
                    feeAlterations = mapper.readValue(rs.getString("fee_alterations"))
                )
            )
        } ?: emptyList(),
        approvedAt = rs.getTimestamp("approved_at")?.toInstant(),
        createdAt = rs.getTimestamp("created_at").toInstant(),
        sentAt = rs.getTimestamp("sent_at")?.toInstant()
    )
}

val toVoucherValueDecisionSummary = { rs: ResultSet, _: StatementContext ->
    VoucherValueDecisionSummary(
        id = rs.getUUID("id"),
        status = rs.getEnum("status"),
        decisionNumber = rs.getObject("decision_number") as Long?, // getLong returns 0 for null values
        validFrom = rs.getDate("valid_from").toLocalDate(),
        validTo = rs.getDate("valid_to")?.toLocalDate(),
        headOfFamily = PersonData.Basic(
            id = rs.getUUID("head_of_family"),
            dateOfBirth = rs.getDate("head_date_of_birth").toLocalDate(),
            firstName = rs.getString("head_first_name"),
            lastName = rs.getString("head_last_name"),
            ssn = rs.getString("head_ssn")
        ),
        // child is not nullable so if it's missing there was nothing to join to the decision
        parts = rs.getString("child")?.let {
            listOf(
                VoucherValueDecisionPartSummary(
                    child = PersonData.Basic(
                        id = rs.getUUID("child"),
                        dateOfBirth = rs.getDate("date_of_birth").toLocalDate(),
                        firstName = rs.getString("child_first_name"),
                        lastName = rs.getString("child_last_name"),
                        ssn = rs.getString("child_ssn")
                    )
                )
            )
        } ?: emptyList(),
        totalCoPayment = rs.getInt("total_co_payment"),
        approvedAt = rs.getTimestamp("approved_at")?.toInstant(),
        createdAt = rs.getTimestamp("created_at").toInstant(),
        sentAt = rs.getTimestamp("sent_at")?.toInstant()
    )
}

fun toVoucherValueDecisionDetailed(mapper: ObjectMapper) = { rs: ResultSet, _: StatementContext ->
    VoucherValueDecisionDetailed(
        id = rs.getUUID("id"),
        status = rs.getEnum("status"),
        decisionNumber = rs.getObject("decision_number") as Long?, // getLong returns 0 for null values
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
                VoucherValueDecisionPartDetailed(
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
                    baseCoPayment = rs.getInt("base_co_payment"),
                    siblingDiscount = rs.getInt("sibling_discount"),
                    coPayment = rs.getInt("co_payment"),
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
