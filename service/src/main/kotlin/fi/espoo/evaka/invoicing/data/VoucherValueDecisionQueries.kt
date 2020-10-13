package fi.espoo.evaka.invoicing.data

import fi.espoo.evaka.invoicing.controller.SortDirection
import fi.espoo.evaka.invoicing.controller.VoucherValueDecisionSortParam
import fi.espoo.evaka.invoicing.domain.PersonData
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionPartSummary
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionSummary
import fi.espoo.evaka.invoicing.domain.mergeDecisions
import fi.espoo.evaka.shared.db.freeTextSearchQuery
import fi.espoo.evaka.shared.db.getEnum
import fi.espoo.evaka.shared.db.getUUID
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet
import java.util.UUID

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

    return count to mergeDecisions(result.map { (_, decision) -> decision }) { decision, parts -> decision.copy(parts = parts) }
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
