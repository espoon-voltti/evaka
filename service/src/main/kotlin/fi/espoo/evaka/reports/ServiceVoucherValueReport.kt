// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import com.fasterxml.jackson.annotation.JsonProperty
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.bindNullable
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.utils.europeHelsinki
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/reports/service-voucher-value")
class ServiceVoucherValueReportController(private val acl: AccessControlList) {

    data class ServiceVoucherReport(
        val locked: LocalDate?,
        val rows: List<ServiceVoucherValueUnitAggregate>
    )

    @GetMapping("/units")
    fun getServiceVoucherValuesForAllUnits(
        db: Database,
        user: AuthenticatedUser,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam(required = false) areaId: UUID?
    ): ResponseEntity<ServiceVoucherReport> {
        val authorization = acl.getAuthorizedUnits(user, setOf(UserRole.UNIT_SUPERVISOR))

        return db.read { tx ->
            val snapshotTime = tx.getSnapshotDate(year, month)
            val rows = if (snapshotTime != null) {
                tx.getSnapshotVoucherValues(year, month, areaId = areaId, unitIds = authorization.ids)
            } else {
                tx.getServiceVoucherValues(year, month, areaId = areaId, unitIds = authorization.ids)
            }

            val aggregates = rows
                .groupBy { ServiceVoucherValueUnitAggregate.UnitData(it.unitId, it.unitName, it.areaId, it.areaName) }
                .map { (unit, rows) ->
                    ServiceVoucherValueUnitAggregate(
                        unit = unit,
                        childCount = rows.map { it.childId }.distinct().size,
                        monthlyPaymentSum = rows.sumOf { row -> row.realizedAmount }
                    )
                }

            ResponseEntity.ok(
                ServiceVoucherReport(
                    locked = snapshotTime,
                    rows = aggregates
                )
            )
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
        user: AuthenticatedUser,
        @PathVariable("unitId") unitId: DaycareId,
        @RequestParam year: Int,
        @RequestParam month: Int
    ): ResponseEntity<ServiceVoucherUnitReport> {
        acl.getRolesForUnit(user, unitId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.FINANCE_ADMIN, UserRole.UNIT_SUPERVISOR)

        return db.read { tx ->
            val snapshotTime = tx.getSnapshotDate(year, month)
            val rows = if (snapshotTime != null) {
                tx.getSnapshotVoucherValues(year, month, unitIds = setOf(unitId))
            } else {
                tx.getServiceVoucherValues(year, month, unitIds = setOf(unitId))
            }

            ResponseEntity.ok(
                ServiceVoucherUnitReport(
                    locked = snapshotTime,
                    rows = rows,
                    voucherTotal = rows.sumOf { it.realizedAmount }
                )
            )
        }
    }
}

fun freezeVoucherValueReportRows(tx: Database.Transaction, year: Int, month: Int, takenAt: Instant) {
    val rows = tx.getServiceVoucherValues(year, month)

    val voucherValueReportSnapshotId =
        tx.createUpdate("INSERT INTO voucher_value_report_snapshot (month, year, taken_at) VALUES (:month, :year, :takenAt) RETURNING id")
            .bind("year", year)
            .bind("month", month)
            .bind("takenAt", takenAt)
            .executeAndReturnGeneratedKeys("id")
            .mapTo<UUID>()
            .first()

    // language=sql
    val sql = """
        INSERT INTO voucher_value_report_decision (voucher_value_report_snapshot_id, decision_id, realized_amount, realized_period, type)
        VALUES (:voucherValueReportSnapshotId, :decisionId, :realizedAmount, :realizedPeriod, :type)
    """.trimIndent()
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

data class ServiceVoucherValueUnitAggregate(
    val unit: UnitData,
    val childCount: Int,
    val monthlyPaymentSum: Int
) {
    data class UnitData(
        val id: DaycareId,
        val name: String,
        val areaId: UUID,
        val areaName: String
    )
}

enum class VoucherReportRowType {
    ORIGINAL,
    REFUND,
    CORRECTION
}

data class ServiceVoucherValueRow(
    val childId: UUID,
    val childFirstName: String,
    val childLastName: String,
    val childDateOfBirth: LocalDate,
    val childGroupName: String?,
    val unitId: DaycareId,
    val unitName: String,
    val areaId: UUID,
    val areaName: String,
    val serviceVoucherDecisionId: UUID,
    val serviceVoucherValue: Int,
    val serviceVoucherCoPayment: Int,
    val serviceNeedDescription: String,
    val realizedAmount: Int,
    val realizedPeriod: FiniteDateRange,
    val numberOfDays: Int,
    val type: VoucherReportRowType,
    @get:JsonProperty("isNew")
    val isNew: Boolean
)

private fun Database.Read.getServiceVoucherValues(
    year: Int,
    month: Int,
    areaId: UUID? = null,
    unitIds: Set<DaycareId>? = null
): List<ServiceVoucherValueRow> {
    // language=sql
    val sql = """
WITH min_voucher_decision_date AS (
    SELECT coalesce(min(valid_from), :reportDate) AS min_date FROM voucher_value_decision WHERE status = ANY(:effective::voucher_value_decision_status[])
), min_change_month AS (
    SELECT make_date(extract(year from min_date)::int, extract(month from min_date)::int, 1) AS month FROM min_voucher_decision_date
), include_corrections AS (
    SELECT NOT EXISTS (SELECT 1 FROM voucher_value_report_snapshot) OR EXISTS (
        SELECT 1 FROM voucher_value_report_snapshot
        WHERE month = extract(month from (:reportDate - interval '1 month'))
        AND year = extract(year from (:reportDate - interval '1 month'))
    ) AS should_include
), month_periods AS (
    SELECT
        extract(year from t) AS year,
        extract(month from t) AS month,
        daterange(t::date, (t + interval '1 month')::date) AS period
    FROM generate_series((SELECT month FROM min_change_month), :reportDate, '1 month') t
    JOIN include_corrections ON should_include OR t = :reportDate
), original AS (
    SELECT p.period, daterange(decision.valid_from, decision.valid_to, '[]') * p.period AS realized_period, decision.id AS decision_id
    FROM month_periods p
    JOIN voucher_value_decision decision ON daterange(decision.valid_from, decision.valid_to, '[]') && p.period
    WHERE decision.status = ANY(:effective::voucher_value_decision_status[]) AND lower(p.period) = :reportDate
), correction_targets AS (
    SELECT DISTINCT decision.child_id, p.year, p.month, p.period
    FROM month_periods p
    JOIN voucher_value_decision decision ON daterange(decision.valid_from, decision.valid_to, '[]') && p.period
    WHERE decision.status = ANY(:effective::voucher_value_decision_status[])
      AND decision.approved_at > (SELECT coalesce(max(taken_at), '-infinity'::timestamptz) FROM voucher_value_report_snapshot)
      AND lower(p.period) < :reportDate
), corrections AS (
    SELECT ct.*, decision.id AS decision_id, daterange(decision.valid_from, decision.valid_to, '[]') * ct.period AS realized_period
    FROM correction_targets ct
    JOIN voucher_value_decision decision ON daterange(decision.valid_from, decision.valid_to, '[]') && ct.period AND decision.child_id = ct.child_id
    WHERE decision.status = ANY(:effective::voucher_value_decision_status[])
), refunds AS (
    SELECT
        ct.period,
        decision.id AS decision_id,
        sn_decision.realized_amount,
        sn_decision.realized_period,
        rank() OVER (PARTITION BY decision.child_id ORDER BY sn.year desc, sn.month desc) AS rank
    FROM correction_targets ct
    JOIN voucher_value_report_decision sn_decision ON extract(year from lower(sn_decision.realized_period)) = ct.year
        AND extract(month from lower(sn_decision.realized_period)) = ct.month
    JOIN voucher_value_report_snapshot sn ON sn_decision.voucher_value_report_snapshot_id = sn.id
    JOIN voucher_value_decision decision ON sn_decision.decision_id = decision.id AND decision.child_id = ct.child_id
    WHERE sn_decision.type != 'REFUND'
), report_rows AS (
    SELECT
        decision_id,
        period,
        realized_period,
        upper(realized_period) - lower(realized_period) AS number_of_days,
        -realized_amount AS realized_amount,
        'REFUND' AS type,
        1 AS type_sort
    FROM refunds
    WHERE rank = 1

    UNION

    SELECT
        decision_id,
        period,
        realized_period,
        upper(realized_period) - lower(realized_period) AS number_of_days,
        NULL AS realized_amount,
        'CORRECTION' AS type,
        2 AS type_sort
    FROM corrections

    UNION

    SELECT
        decision_id,
        period,
        realized_period,
        upper(realized_period) - lower(realized_period) AS number_of_days,
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
    decision.service_need_voucher_value_description_fi AS service_need_description,
    coalesce(
        row.realized_amount,
        round((decision.voucher_value - decision.final_co_payment) * (row.number_of_days::numeric(10, 8) / (upper(row.period) - lower(row.period))))
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
WHERE (:areaId::uuid IS NULL OR area.id = :areaId) AND (:unitIds::uuid[] IS NULL OR unit.id = ANY(:unitIds))
ORDER BY child_last_name, child_first_name, child_id, type_sort, realized_period
"""

    return createQuery(sql)
        .bind("effective", VoucherValueDecisionStatus.effective)
        .bind("reportDate", LocalDate.of(year, month, 1))
        .bindNullable("areaId", areaId)
        .bindNullable("unitIds", unitIds?.toTypedArray())
        .mapTo<ServiceVoucherValueRow>()
        .toList()
}

private fun Database.Read.getSnapshotDate(year: Int, month: Int): LocalDate? {
    return createQuery("SELECT taken_at FROM voucher_value_report_snapshot WHERE year >= :year AND month >= :month")
        .bind("year", year)
        .bind("month", month)
        .mapTo<Instant>()
        .firstOrNull()
        ?.let { LocalDate.ofInstant(it, europeHelsinki) }
}

private fun Database.Read.getSnapshotVoucherValues(
    year: Int,
    month: Int,
    areaId: UUID? = null,
    unitIds: Set<DaycareId>? = null
): List<ServiceVoucherValueRow> {
    // language=sql
    val sql =
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
            decision.service_need_voucher_value_description_fi AS service_need_description,
            sn_decision.realized_amount,
            sn_decision.realized_period,
            upper(sn_decision.realized_period) - lower(sn_decision.realized_period) AS number_of_days,
            sn_decision.type,
            NOT EXISTS (
                SELECT 1 FROM voucher_value_report_decision old_snapshot_decision
                JOIN voucher_value_report_snapshot old_snapshot ON old_snapshot_decision.voucher_value_report_snapshot_id = old_snapshot.id
                WHERE old_snapshot_decision.decision_id = decision.id
                AND make_date(old_snapshot.year, old_snapshot.month, 1) < make_date(:year, :month, 1)
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
        WHERE sn.year = :year AND sn.month = :month
        AND (:areaId::uuid IS NULL OR area.id = :areaId)
        AND (:unitIds::uuid[] IS NULL OR unit.id = ANY(:unitIds))
        """.trimIndent()

    return createQuery(sql)
        .bind("sent", VoucherValueDecisionStatus.SENT)
        .bind("year", year)
        .bind("month", month)
        .bindNullable("areaId", areaId)
        .bindNullable("unitIds", unitIds?.toTypedArray())
        .mapTo<ServiceVoucherValueRow>()
        .toList()
}
