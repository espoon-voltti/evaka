// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import com.fasterxml.jackson.annotation.JsonProperty
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.bindNullable
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.utils.zoneId
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
                        monthlyPaymentSum = rows.sumBy { row -> row.realizedAmount }
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
        @PathVariable("unitId") unitId: UUID,
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
                    voucherTotal = rows.sumBy { it.realizedAmount }
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
        INSERT INTO voucher_value_report_decision_part (voucher_value_report_snapshot_id, decision_part_id, realized_amount, realized_period, type)
        VALUES (:voucherValueReportSnapshotId, :decisionPartId, :realizedAmount, :realizedPeriod, :type)
    """.trimIndent()
    val batch = tx.prepareBatch(sql)

    rows.forEach { row ->
        batch
            .bind("voucherValueReportSnapshotId", voucherValueReportSnapshotId)
            .bind("decisionPartId", row.serviceVoucherPartId)
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
        val id: UUID,
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
    val unitId: UUID,
    val unitName: String,
    val areaId: UUID,
    val areaName: String,
    val serviceVoucherPartId: UUID,
    val serviceVoucherValue: Int,
    val serviceVoucherCoPayment: Int,
    val serviceVoucherServiceCoefficient: Int,
    val serviceVoucherHoursPerWeek: Int,
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
    unitIds: Set<UUID>? = null
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
    SELECT p.period, daterange(decision.valid_from, decision.valid_to, '[]') * p.period AS realized_period, part.id AS part_id
    FROM month_periods p
    JOIN voucher_value_decision decision ON daterange(decision.valid_from, decision.valid_to, '[]') && p.period
    JOIN voucher_value_decision_part part ON decision.id = part.voucher_value_decision_id
    WHERE decision.status = ANY(:effective::voucher_value_decision_status[]) AND lower(p.period) = :reportDate
), correction_targets AS (
    SELECT DISTINCT part.child, p.year, p.month, p.period
    FROM month_periods p
    JOIN voucher_value_decision decision ON daterange(decision.valid_from, decision.valid_to, '[]') && p.period
    JOIN voucher_value_decision_part part on decision.id = part.voucher_value_decision_id
    WHERE decision.status = ANY(:effective::voucher_value_decision_status[])
      AND decision.approved_at > (SELECT coalesce(max(taken_at), '-infinity'::timestamptz) FROM voucher_value_report_snapshot)
      AND lower(p.period) < :reportDate
), corrections AS (
    SELECT ct.*, part.id AS part_id, daterange(decision.valid_from, decision.valid_to, '[]') * ct.period AS realized_period
    FROM correction_targets ct
    JOIN voucher_value_decision decision ON daterange(decision.valid_from, decision.valid_to, '[]') && ct.period
    JOIN voucher_value_decision_part part ON decision.id = part.voucher_value_decision_id AND part.child = ct.child
    WHERE decision.status = ANY(:effective::voucher_value_decision_status[])
), refunds AS (
    SELECT
        ct.period,
        part.id AS part_id,
        sn_part.realized_amount,
        sn_part.realized_period,
        rank() OVER (PARTITION BY part.child ORDER BY sn.year desc, sn.month desc) AS rank
    FROM correction_targets ct
    JOIN voucher_value_report_decision_part sn_part ON extract(year from lower(sn_part.realized_period)) = ct.year AND extract(month from lower(sn_part.realized_period)) = ct.month
    JOIN voucher_value_decision_part part on part.id = sn_part.decision_part_id AND part.child = ct.child
    JOIN voucher_value_report_snapshot sn on sn_part.voucher_value_report_snapshot_id = sn.id
    JOIN voucher_value_decision vvd on part.voucher_value_decision_id = vvd.id
    WHERE sn_part.type != 'REFUND'
), report_rows AS (
    SELECT
        part_id,
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
        part_id,
        period,
        realized_period,
        upper(realized_period) - lower(realized_period) AS number_of_days,
        NULL AS realized_amount,
        'CORRECTION' AS type,
        2 AS type_sort
    FROM corrections

    UNION

    SELECT
        part_id,
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
    part.id AS service_voucher_part_id,
    part.voucher_value AS service_voucher_value,
    part.co_payment AS service_voucher_co_payment,
    part.service_coefficient AS service_voucher_service_coefficient,
    part.hours_per_week AS service_voucher_hours_per_week,
    coalesce(
        row.realized_amount,
        round((part.voucher_value - part.co_payment) * (row.number_of_days::numeric(10, 8) / (upper(row.period) - lower(row.period))))
    ) AS realized_amount,
    row.realized_period,
    row.number_of_days,
    row.type,
    NOT EXISTS (
        SELECT 1 FROM voucher_value_report_decision_part old_snapshot_part
        WHERE old_snapshot_part.decision_part_id = part.id
    ) AS is_new
FROM report_rows row
JOIN voucher_value_decision_part part ON row.part_id = part.id
JOIN person child ON part.child = child.id
JOIN daycare unit ON part.placement_unit = unit.id
JOIN care_area area ON unit.care_area_id = area.id
LEFT JOIN LATERAL (
    SELECT p.child_id, STRING_AGG(dg.name, ', ') AS name
    FROM placement p
    JOIN daycare_group_placement dgp ON p.id = dgp.daycare_placement_id
    JOIN daycare_group dg ON dgp.daycare_group_id = dg.id
    WHERE daterange(dgp.start_date, dgp.end_date, '[]') && row.realized_period
      AND p.unit_id = part.placement_unit
    GROUP BY p.child_id
) child_group ON child.id = child_group.child_id
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
        ?.let { LocalDate.ofInstant(it, zoneId) }
}

private fun Database.Read.getSnapshotVoucherValues(
    year: Int,
    month: Int,
    areaId: UUID? = null,
    unitIds: Set<UUID>? = null
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
            part.id AS service_voucher_part_id,
            part.voucher_value AS service_voucher_value,
            part.co_payment AS service_voucher_co_payment,
            part.service_coefficient AS service_voucher_service_coefficient,
            part.hours_per_week service_voucher_hours_per_week,
            sn_part.realized_amount,
            sn_part.realized_period,
            upper(sn_part.realized_period) - lower(sn_part.realized_period) AS number_of_days,
            sn_part.type,
            NOT EXISTS (
                SELECT 1 FROM voucher_value_report_decision_part old_snapshot_part
                JOIN voucher_value_report_snapshot old_snapshot ON old_snapshot_part.voucher_value_report_snapshot_id = old_snapshot.id
                WHERE old_snapshot_part.decision_part_id = part.id
                AND make_date(old_snapshot.year, old_snapshot.month, 1) < make_date(:year, :month, 1)
            ) AS is_new
        FROM voucher_value_report_snapshot sn
        JOIN voucher_value_report_decision_part sn_part ON sn.id = sn_part.voucher_value_report_snapshot_id
        JOIN voucher_value_decision_part part ON part.id = sn_part.decision_part_id
        JOIN person child ON part.child = child.id
        JOIN daycare unit ON part.placement_unit = unit.id
        JOIN care_area area ON unit.care_area_id = area.id
        LEFT JOIN LATERAL (
            SELECT p.child_id, STRING_AGG(dg.name, ', ') AS name
            FROM placement p
            JOIN daycare_group_placement dgp ON p.id = dgp.daycare_placement_id
            JOIN daycare_group dg ON dgp.daycare_group_id = dg.id
            WHERE daterange(dgp.start_date, dgp.end_date, '[]') && sn_part.realized_period
            AND p.unit_id = part.placement_unit
            GROUP BY p.child_id
        ) child_group ON child.id = child_group.child_id
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
