// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import com.fasterxml.jackson.annotation.JsonProperty
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.VoucherValueDecisionId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import fi.espoo.evaka.shared.security.actionrule.forTable
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/reports/service-voucher-value")
class ServiceVoucherValueReportController(private val accessControl: AccessControl) {

    @GetMapping("/units")
    fun getServiceVoucherValuesForAllUnits(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam(required = false) areaId: AreaId?
    ): ServiceVoucherReport {
        return db.connect { dbc ->
            dbc.read { tx ->
                val filter =
                    accessControl.requireAuthorizationFilter(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_SERVICE_VOUCHER_REPORT
                    )
                getServiceVoucherReport(tx, year, month, areaId, filter)
            }
        }
    }

    data class ServiceVoucherUnitReport(
        val locked: LocalDate?,
        val rows: List<ServiceVoucherValueRow>,
        val voucherTotal: Int
    )

    @GetMapping("/units/{unitId}")
    fun getServiceVoucherValuesForUnit(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @RequestParam year: Int,
        @RequestParam month: Int
    ): ServiceVoucherUnitReport {
        return db.connect { dbc ->
            dbc.read { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Unit.READ_SERVICE_VOUCHER_VALUES_REPORT,
                    unitId
                )

                val snapshotTime = tx.getSnapshotDate(year, month)
                val rows =
                    if (snapshotTime != null) {
                        tx.getSnapshotVoucherValues(year, month, unitIds = setOf(unitId))
                    } else {
                        tx.getServiceVoucherValues(year, month, unitIds = setOf(unitId))
                    }

                ServiceVoucherUnitReport(
                    locked = snapshotTime,
                    rows = rows,
                    voucherTotal = rows.sumOf { it.realizedAmount }
                )
            }
        }
    }
}

fun freezeVoucherValueReportRows(
    tx: Database.Transaction,
    year: Int,
    month: Int,
    takenAt: HelsinkiDateTime
) {
    val rows = tx.getServiceVoucherValues(year, month)

    val voucherValueReportSnapshotId =
        tx.createUpdate {
                sql(
                    "INSERT INTO voucher_value_report_snapshot (month, year, taken_at) VALUES (${bind(month)}, ${bind(year)}, ${bind(takenAt)}) RETURNING id"
                )
            }
            .executeAndReturnGeneratedKeys()
            .exactlyOne<UUID>()

    // language=sql
    val sql =
        """
        INSERT INTO voucher_value_report_decision (voucher_value_report_snapshot_id, decision_id, realized_amount, realized_period, type)
        VALUES (:voucherValueReportSnapshotId, :decisionId, :realizedAmount, :realizedPeriod, :type)
    """
            .trimIndent()
    val batch = tx.prepareBatch(sql)

    rows.forEach { row ->
        batch
            .bind("voucherValueReportSnapshotId", voucherValueReportSnapshotId)
            .bind("decisionId", row.serviceVoucherDecisionId)
            .bind("realizedAmount", row.realizedAmount)
            .bind("realizedPeriod", row.realizedPeriod)
            .bind("type", row.type)
            .add()
    }

    batch.execute()
}

data class ServiceVoucherReport(
    val locked: LocalDate?,
    val rows: List<ServiceVoucherValueUnitAggregate>
)

fun getServiceVoucherReport(
    tx: Database.Read,
    year: Int,
    month: Int,
    areaId: AreaId?,
    unitFilter: AccessControlFilter<DaycareId>
): ServiceVoucherReport {
    tx.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)

    val unitIds =
        when (unitFilter) {
            AccessControlFilter.PermitAll -> null
            is AccessControlFilter.Some ->
                tx.createQuery {
                        sql(
                            "SELECT id FROM daycare WHERE ${predicate(unitFilter.forTable("daycare"))}"
                        )
                    }
                    .toSet<DaycareId>()
        }
    val snapshotTime = tx.getSnapshotDate(year, month)
    val rows =
        if (snapshotTime != null) {
            tx.getSnapshotVoucherValues(year, month, areaId = areaId, unitIds = unitIds)
        } else {
            tx.getServiceVoucherValues(year, month, areaId = areaId, unitIds = unitIds)
        }

    val aggregates =
        rows
            .groupBy {
                ServiceVoucherValueUnitAggregate.UnitData(
                    it.unitId,
                    it.unitName,
                    it.areaId,
                    it.areaName
                )
            }
            .map { (unit, rows) ->
                ServiceVoucherValueUnitAggregate(
                    unit = unit,
                    childCount = rows.map { it.childId }.distinct().size,
                    monthlyPaymentSum = rows.sumOf { row -> row.realizedAmount }
                )
            }

    return ServiceVoucherReport(locked = snapshotTime, rows = aggregates)
}

data class ServiceVoucherValueUnitAggregate(
    val unit: UnitData,
    val childCount: Int,
    val monthlyPaymentSum: Int
) {
    data class UnitData(
        val id: DaycareId,
        val name: String,
        val areaId: AreaId,
        val areaName: String
    )
}

enum class VoucherReportRowType {
    ORIGINAL,
    REFUND,
    CORRECTION
}

data class ServiceVoucherValueRow(
    val childId: ChildId,
    val childFirstName: String,
    val childLastName: String,
    val childDateOfBirth: LocalDate,
    val childGroupName: String?,
    val unitId: DaycareId,
    val unitName: String,
    val areaId: AreaId,
    val areaName: String,
    val serviceVoucherDecisionId: VoucherValueDecisionId,
    val serviceVoucherValue: Int,
    val serviceVoucherCoPayment: Int,
    val serviceVoucherFinalCoPayment: Int,
    val serviceNeedDescription: String,
    val assistanceNeedCoefficient: BigDecimal,
    val realizedAmount: Int,
    val realizedPeriod: FiniteDateRange,
    val numberOfDays: Int,
    val type: VoucherReportRowType,
    @get:JsonProperty("isNew") val isNew: Boolean
)

private fun Database.Read.getServiceVoucherValues(
    year: Int,
    month: Int,
    areaId: AreaId? = null,
    unitIds: Set<DaycareId>? = null
): List<ServiceVoucherValueRow> {
    val reportDate = LocalDate.of(year, month, 1)
    return createQuery {
            sql(
                """
WITH min_voucher_decision_date AS (
    SELECT coalesce(min(valid_from), ${bind(reportDate)}) AS min_date FROM voucher_value_decision WHERE status != 'DRAFT'::voucher_value_decision_status
), min_change_month AS (
    SELECT make_date(extract(year from min_date)::int, extract(month from min_date)::int, 1) AS month FROM min_voucher_decision_date
), include_corrections AS (
    SELECT NOT EXISTS (SELECT 1 FROM voucher_value_report_snapshot) OR EXISTS (
        SELECT 1 FROM voucher_value_report_snapshot
        WHERE month = extract(month from (${bind(reportDate)} - interval '1 month'))
        AND year = extract(year from (${bind(reportDate)} - interval '1 month'))
    ) AS should_include
), month_periods AS (
    SELECT
        extract(year from t) AS year,
        extract(month from t) AS month,
        daterange(t::date, (t + interval '1 month')::date) AS period,
        op.operational_days,
        array_length(op.operational_days, 1) AS operational_days_count
    FROM generate_series((SELECT month FROM min_change_month), ${bind(reportDate)}, '1 month') t
    JOIN include_corrections ON should_include OR t = ${bind(reportDate)}
    JOIN LATERAL (
        SELECT array_agg(d::date) operational_days FROM generate_series(t, (t + interval '1 month' - interval '1 day'), '1 day') d
        WHERE date_part('isodow', d) = ANY('{1,2,3,4,5}') AND NOT EXISTS (SELECT 1 FROM holiday WHERE holiday.date = d)
    ) op ON true
), original AS (
    SELECT
        p.period,
        daterange(decision.valid_from, decision.valid_to, '[]') * p.period AS realized_period,
        decision.id AS decision_id,
        p.operational_days,
        p.operational_days_count
    FROM month_periods p
    JOIN voucher_value_decision decision ON daterange(decision.valid_from, decision.valid_to, '[]') && p.period
    WHERE decision.status = ANY(${bind(VoucherValueDecisionStatus.effective)}::voucher_value_decision_status[]) AND lower(p.period) = ${bind(reportDate)}
), correction_targets AS (
    -- Validity updated after last freeze to be different in this period, or to not intersect with this period at all
    SELECT
        decision.id AS decision_id,
        decision.child_id,
        p.period,
        p.operational_days,
        p.operational_days_count
    FROM month_periods p
    JOIN voucher_value_report_decision sn_decision ON sn_decision.realized_period && p.period
    JOIN voucher_value_decision decision on decision.id = sn_decision.decision_id
    WHERE lower(p.period) < ${bind(reportDate)}
        AND (decision.status = ANY(${bind(VoucherValueDecisionStatus.effective)}::voucher_value_decision_status[]) OR decision.status = 'ANNULLED')
        AND decision.validity_updated_at > (SELECT coalesce(max(taken_at), '-infinity'::timestamptz) FROM voucher_value_report_snapshot)
        AND (daterange(decision.valid_from, decision.valid_to, '[]') * p.period) <> sn_decision.realized_period

    UNION

    -- Annulled after last freeze
    SELECT
        decision.id AS decision_id,
        decision.child_id,
        p.period,
        p.operational_days,
        p.operational_days_count
    FROM month_periods p
    JOIN voucher_value_report_decision sn_decision ON sn_decision.realized_period && p.period
    JOIN voucher_value_decision decision ON decision.id = sn_decision.decision_id AND daterange(decision.valid_from,decision.valid_to,'[]') && sn_decision.realized_period
    WHERE lower(p.period) < ${bind(reportDate)}
        AND decision.status = 'ANNULLED'::voucher_value_decision_status
        AND decision.annulled_at > (SELECT coalesce(max(taken_at), '-infinity'::timestamptz) FROM voucher_value_report_snapshot)
), corrections AS (
    -- New decision created for the past
    SELECT
        decision.id AS decision_id,
        p.period,
        daterange(decision.valid_from, decision.valid_to, '[]') * p.period AS realized_period,
        p.operational_days,
        p.operational_days_count
    FROM month_periods p
    JOIN voucher_value_decision decision ON daterange(decision.valid_from, decision.valid_to, '[]') && p.period
    WHERE lower(p.period) < ${bind(reportDate)}
        AND decision.status = ANY(${bind(VoucherValueDecisionStatus.effective)}::voucher_value_decision_status[])
        AND NOT EXISTS (
            SELECT 1 FROM voucher_value_report_decision sn_decision
            WHERE sn_decision.decision_id = decision.id
                AND sn_decision.realized_period && p.period
        )

    UNION

    -- Replaced with another decision for which there is no already reported correction for the same period
    SELECT
        decision.id AS decision_id,
        p.period,
        daterange(decision.valid_from, decision.valid_to, '[]') * p.period AS realized_period,
        p.operational_days,
        p.operational_days_count
    FROM correction_targets ct
    JOIN month_periods p ON ct.period && p.period
    JOIN voucher_value_report_decision sn_decision ON sn_decision.decision_id = ct.decision_id AND sn_decision.realized_period && p.period
    JOIN voucher_value_decision decision ON daterange(decision.valid_from, decision.valid_to, '[]') && sn_decision.realized_period AND decision.child_id = ct.child_id
    LEFT JOIN voucher_value_report_decision sn_decision2 ON decision.id = sn_decision2.decision_id AND sn_decision2.realized_period = sn_decision.realized_period AND sn_decision2.type = 'CORRECTION'
    WHERE decision.status = ANY(${bind(VoucherValueDecisionStatus.effective)}::voucher_value_decision_status[]) and sn_decision2.decision_id IS NULL
), refunds AS (
    SELECT
        p.period,
        p.operational_days,
        p.operational_days_count,
        ct.decision_id,
        sn_decision.realized_amount,
        sn_decision.realized_period,
        sn_decision.type,
        rank() OVER (PARTITION BY ct.child_id, p.period, ct.decision_id ORDER BY sn.year DESC, sn.month DESC) AS rank
    FROM correction_targets ct
    JOIN month_periods p ON ct.period && p.period
    JOIN voucher_value_report_decision sn_decision ON sn_decision.decision_id = ct.decision_id AND sn_decision.realized_period && p.period
    JOIN voucher_value_report_snapshot sn ON sn.id = sn_decision.voucher_value_report_snapshot_id
), report_rows AS (
    SELECT
        decision_id,
        period,
        realized_period,
        (CASE
            WHEN period = realized_period THEN operational_days_count
            ELSE (SELECT COUNT(*) FROM unnest(operational_days) dates WHERE realized_period @> dates)
        END) AS number_of_days,
        operational_days_count,
        -realized_amount AS realized_amount,
        'REFUND' AS type,
        1 AS type_sort
    FROM refunds
    WHERE rank = 1 AND type != 'REFUND'::voucher_report_row_type

    UNION ALL

    SELECT
        decision_id,
        period,
        realized_period,
        (CASE
            WHEN period = realized_period THEN operational_days_count
            ELSE (SELECT COUNT(*) FROM unnest(operational_days) dates WHERE realized_period @> dates)
        END) AS number_of_days,
        operational_days_count,
        NULL AS realized_amount,
        'CORRECTION' AS type,
        2 AS type_sort
    FROM corrections

    UNION ALL

    SELECT
        decision_id,
        period,
        realized_period,
        (CASE
            WHEN period = realized_period THEN operational_days_count
            ELSE (SELECT COUNT(*) FROM unnest(operational_days) dates WHERE realized_period @> dates)
        END) AS number_of_days,
        operational_days_count,
        NULL AS realized_amount,
        'ORIGINAL' AS type,
        3 AS type_sort
    FROM original
)
SELECT
    child.id AS child_id,
    child.first_name AS child_first_name,
    child.last_name AS child_last_name,
    child.date_of_birth AS child_date_of_birth,
    child_group.name as child_group_name,
    unit.id AS unit_id,
    unit.name AS unit_name,
    area.id AS area_id,
    area.name AS area_name,
    decision.id AS service_voucher_decision_id,
    decision.voucher_value AS service_voucher_value,
    decision.co_payment AS service_voucher_co_payment,
    decision.final_co_payment AS service_voucher_final_co_payment,
    decision.service_need_voucher_value_description_fi AS service_need_description,
    decision.assistance_need_coefficient AS assistance_need_coefficient,
    coalesce(
        row.realized_amount,
        round((decision.voucher_value - decision.final_co_payment) * (row.number_of_days::numeric(10, 8) / row.operational_days_count::numeric(10, 8)))
    ) AS realized_amount,
    row.realized_period,
    row.number_of_days,
    row.type,
    NOT EXISTS (
        SELECT 1 FROM voucher_value_report_decision old_snapshot_decision
        WHERE old_snapshot_decision.decision_id = decision.id
    ) AS is_new
FROM report_rows row
JOIN voucher_value_decision decision ON row.decision_id = decision.id
JOIN person child ON decision.child_id = child.id
JOIN daycare unit ON decision.placement_unit_id = unit.id
JOIN care_area area ON unit.care_area_id = area.id
LEFT JOIN LATERAL (
    SELECT STRING_AGG(dg.name, ', ') AS name
    FROM placement p
    JOIN daycare_group_placement dgp ON p.id = dgp.daycare_placement_id
    JOIN daycare_group dg ON dgp.daycare_group_id = dg.id
    WHERE daterange(dgp.start_date, dgp.end_date, '[]') && row.realized_period
      AND p.unit_id = decision.placement_unit_id
      AND p.child_id = child.id
    GROUP BY p.child_id
) child_group ON true
WHERE (${bind(areaId)}::uuid IS NULL OR area.id = ${bind(areaId)}) AND (${bind(unitIds)}::uuid[] IS NULL OR unit.id = ANY(${bind(unitIds)}))
ORDER BY child_last_name, child_first_name, child_id, type_sort, realized_period
"""
            )
        }
        .toList<ServiceVoucherValueRow>()
}

private fun Database.Read.getSnapshotDate(year: Int, month: Int): LocalDate? {
    return createQuery {
            sql(
                "SELECT taken_at FROM voucher_value_report_snapshot WHERE year >= ${bind(year)} AND month >= ${bind(month)} LIMIT 1"
            )
        }
        .exactlyOneOrNull<HelsinkiDateTime>()
        ?.toLocalDate()
}

data class MonthOfYear(val year: Int, val month: Int)

fun Database.Read.getLastSnapshotMonth(): MonthOfYear? {
    return createQuery {
            sql(
                "SELECT year, month FROM voucher_value_report_snapshot ORDER BY year DESC, month DESC LIMIT 1"
            )
        }
        .exactlyOneOrNull<MonthOfYear>()
}

private fun Database.Read.getSnapshotVoucherValues(
    year: Int,
    month: Int,
    areaId: AreaId? = null,
    unitIds: Set<DaycareId>? = null
): List<ServiceVoucherValueRow> {
    return createQuery {
            sql(
                """
SELECT
    child.id AS child_id,
    child.first_name AS child_first_name,
    child.last_name AS child_last_name,
    child.date_of_birth AS child_date_of_birth,
    child_group.name as child_group_name,
    unit.id AS unit_id,
    unit.name AS unit_name,
    area.id AS area_id,
    area.name AS area_name,
    decision.id AS service_voucher_decision_id,
    decision.voucher_value AS service_voucher_value,
    decision.co_payment AS service_voucher_co_payment,
    decision.final_co_payment AS service_voucher_final_co_payment,
    decision.service_need_voucher_value_description_fi AS service_need_description,
    decision.assistance_need_coefficient AS assistance_need_coefficient,
    sn_decision.realized_amount,
    sn_decision.realized_period,
    upper(sn_decision.realized_period) - lower(sn_decision.realized_period) AS number_of_days,
    sn_decision.type,
    NOT EXISTS (
        SELECT 1 FROM voucher_value_report_decision old_snapshot_decision
        JOIN voucher_value_report_snapshot old_snapshot ON old_snapshot_decision.voucher_value_report_snapshot_id = old_snapshot.id
        WHERE old_snapshot_decision.decision_id = decision.id
        AND make_date(old_snapshot.year, old_snapshot.month, 1) < make_date(${bind(year)}, ${bind(month)}, 1)
    ) AS is_new
FROM voucher_value_report_snapshot sn
JOIN voucher_value_report_decision sn_decision ON sn.id = sn_decision.voucher_value_report_snapshot_id
JOIN voucher_value_decision decision ON decision.id = sn_decision.decision_id
JOIN person child ON decision.child_id = child.id
JOIN daycare unit ON decision.placement_unit_id = unit.id
JOIN care_area area ON unit.care_area_id = area.id
LEFT JOIN LATERAL (
    SELECT STRING_AGG(dg.name, ', ') AS name
    FROM placement p
    JOIN daycare_group_placement dgp ON p.id = dgp.daycare_placement_id
    JOIN daycare_group dg ON dgp.daycare_group_id = dg.id
    WHERE daterange(dgp.start_date, dgp.end_date, '[]') && sn_decision.realized_period
        AND p.unit_id = decision.placement_unit_id
        AND p.child_id = child.id
    GROUP BY p.child_id
) child_group ON true
WHERE sn.year = ${bind(year)} AND sn.month = ${bind(month)}
AND (${bind(areaId)}::uuid IS NULL OR area.id = ${bind(areaId)})
AND (${bind(unitIds)}::uuid[] IS NULL OR unit.id = ANY(${bind(unitIds)}))
"""
            )
        }
        .toList<ServiceVoucherValueRow>()
}
