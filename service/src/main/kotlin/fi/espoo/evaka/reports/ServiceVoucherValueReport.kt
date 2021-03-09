// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.application.utils.currentDateInFinland
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.bindNullable
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.Month
import java.time.Year
import java.util.UUID

@RestController
@RequestMapping("/reports/service-voucher-value")
class ServiceVoucherValueReportController {
    @GetMapping("/units")
    fun getServiceVoucherValuesForAllUnits(
        db: Database,
        user: AuthenticatedUser,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam areaId: UUID
    ): ResponseEntity<List<ServiceVoucherValueUnitAggregate>> {
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.FINANCE_ADMIN)

        return db.read { tx ->
            val period = FiniteDateRange(
                LocalDate.of(year, month, 1),
                LocalDate.of(year, month, Month.of(month).length(Year.isLeap(year.toLong())))
            )
            val rows = when {
                tx.snapshotExists(year, month) -> tx.getSnapshotVoucherValues(year, month, areaId = areaId)
                else -> tx.getServiceVoucherValues(period, areaId = areaId)
                    .map { row ->
                        row.withDerivatives(
                            realizedMonthlyAmount(
                                row.serviceVoucherValue - row.serviceVoucherCoPayment,
                                row.serviceVoucherPeriod,
                                period
                            )
                        )
                    }
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

            ResponseEntity.ok(aggregates)
        }
    }

    @GetMapping("/units/{unitId}")
    fun getServiceVoucherValuesForUnit(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable("unitId") unitId: UUID,
        @RequestParam year: Int,
        @RequestParam month: Int
    ): ResponseEntity<List<ServiceVoucherValueRowWithDerivatives>> {
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.FINANCE_ADMIN)

        return db.read { tx ->
            val period = FiniteDateRange(
                LocalDate.of(year, month, 1),
                LocalDate.of(year, month, Month.of(month).length(Year.isLeap(year.toLong())))
            )
            val rows = when {
                tx.snapshotExists(year, month) -> tx.getSnapshotVoucherValues(year, month, unitId = unitId)
                else -> tx.getServiceVoucherValues(period, unitId = unitId)
                    .map { row ->
                        row.withDerivatives(
                            realizedMonthlyAmount(
                                row.serviceVoucherValue - row.serviceVoucherCoPayment,
                                row.serviceVoucherPeriod,
                                period
                            )
                        )
                    }
            }

            ResponseEntity.ok(rows)
        }
    }
}

fun freezeVoucherValueReportRows(tx: Database.Transaction) {
    val currentMonthPeriod = currentDateInFinland().withDayOfMonth(1).let { firstDayOfMonth ->
        val lastDayOfMonth = firstDayOfMonth.plusMonths(1).minusDays(1)
        FiniteDateRange(firstDayOfMonth, lastDayOfMonth)
    }

    val rows = tx.getServiceVoucherValues(currentMonthPeriod)
        .map { row ->
            row.withDerivatives(
                realizedMonthlyAmount(
                    row.serviceVoucherValue - row.serviceVoucherCoPayment,
                    row.serviceVoucherPeriod,
                    currentMonthPeriod
                )
            )
        }

    val voucherValueReportSnapshotId =
        tx.createUpdate("INSERT INTO voucher_value_report_snapshot (month, year) VALUES (:month, :year) RETURNING id")
            .bind("month", currentMonthPeriod.start.monthValue)
            .bind("year", currentMonthPeriod.start.year)
            .executeAndReturnGeneratedKeys("id")
            .mapTo<UUID>()
            .first()

    val batch = tx.prepareBatch(
        """
INSERT INTO voucher_value_report_decision_part (voucher_value_report_snapshot_id, decision_part_id, realized_amount, realized_period)
VALUES (:voucherValueReportSnapshotId, :decisionPartId, :realizedAmount, :realizedPeriod)
"""
    )

    rows.forEach { row ->
        batch
            .bind("voucherValueReportSnapshotId", voucherValueReportSnapshotId)
            .bind("decisionPartId", row.serviceVoucherPartId)
            .bind("realizedAmount", row.realizedAmount)
            .bind("realizedPeriod", row.realizedPeriod)
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
    val serviceVoucherPeriod: DateRange,
    val serviceVoucherPartId: UUID,
    val serviceVoucherValue: Int,
    val serviceVoucherCoPayment: Int,
    val serviceVoucherServiceCoefficient: Int,
    val serviceVoucherHoursPerWeek: Int
) {
    fun withDerivatives(derivatives: ValueRowDerivatives) = ServiceVoucherValueRowWithDerivatives(
        childId = this.childId,
        childFirstName = this.childFirstName,
        childLastName = this.childLastName,
        childDateOfBirth = this.childDateOfBirth,
        childGroupName = this.childGroupName,
        unitId = this.unitId,
        unitName = this.unitName,
        areaId = this.areaId,
        areaName = this.areaName,
        serviceVoucherPeriod = this.serviceVoucherPeriod,
        serviceVoucherPartId = this.serviceVoucherPartId,
        serviceVoucherValue = this.serviceVoucherValue,
        serviceVoucherCoPayment = this.serviceVoucherCoPayment,
        serviceVoucherServiceCoefficient = this.serviceVoucherServiceCoefficient,
        serviceVoucherHoursPerWeek = this.serviceVoucherHoursPerWeek,
        realizedAmount = derivatives.realizedAmount,
        realizedPeriod = derivatives.realizedPeriod,
        numberOfDays = derivatives.numberOfDays
    )
}

data class ServiceVoucherValueRowWithDerivatives(
    val childId: UUID,
    val childFirstName: String,
    val childLastName: String,
    val childDateOfBirth: LocalDate,
    val childGroupName: String?,
    val unitId: UUID,
    val unitName: String,
    val areaId: UUID,
    val areaName: String,
    val serviceVoucherPeriod: DateRange,
    val serviceVoucherPartId: UUID,
    val serviceVoucherValue: Int,
    val serviceVoucherCoPayment: Int,
    val serviceVoucherServiceCoefficient: Int,
    val serviceVoucherHoursPerWeek: Int,
    val realizedAmount: Int,
    val realizedPeriod: FiniteDateRange,
    val numberOfDays: Int
)

data class ValueRowDerivatives(
    val realizedAmount: Int,
    val realizedPeriod: FiniteDateRange,
    val numberOfDays: Int
)

private fun realizedMonthlyAmount(
    value: Int,
    decisionPeriod: DateRange,
    monthPeriod: FiniteDateRange
): ValueRowDerivatives {
    val realizedPeriod = FiniteDateRange(
        maxOf(decisionPeriod.start, monthPeriod.start),
        minOf(decisionPeriod.end ?: monthPeriod.end, monthPeriod.end)
    )

    val numberOfDays = realizedPeriod.durationInDays().toInt()
    val coefficient =
        BigDecimal(numberOfDays).divide(BigDecimal(monthPeriod.durationInDays()), 10, RoundingMode.HALF_UP)
    val realizedAmount = (BigDecimal(value) * coefficient).setScale(0, RoundingMode.HALF_UP).toInt()

    return ValueRowDerivatives(realizedAmount, realizedPeriod, numberOfDays)
}

private fun Database.Read.getServiceVoucherValues(
    period: FiniteDateRange,
    areaId: UUID? = null,
    unitId: UUID? = null
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
            daterange(decision.valid_from, decision.valid_to, '[]') AS service_voucher_period,
            part.id AS service_voucher_part_id,
            part.voucher_value AS service_voucher_value,
            part.co_payment AS service_voucher_co_payment,
            part.service_coefficient AS service_voucher_service_coefficient,
            part.hours_per_week AS service_voucher_hours_per_week
        FROM voucher_value_decision decision
        JOIN voucher_value_decision_part part ON decision.id = part.voucher_value_decision_id
        JOIN person child ON part.child = child.id
        JOIN daycare unit ON part.placement_unit = unit.id
        JOIN care_area area ON unit.care_area_id = area.id
        LEFT JOIN LATERAL (
            SELECT p.child_id, STRING_AGG(dg.name, ', ') AS name
            FROM placement p
            JOIN daycare_group_placement dgp ON p.id = dgp.daycare_placement_id
            JOIN daycare_group dg ON dgp.daycare_group_id = dg.id
            WHERE daterange(decision.valid_from, decision.valid_to, '[]') && daterange(dgp.start_date, dgp.end_date, '[]')
            AND p.unit_id = part.placement_unit
            GROUP BY p.child_id
        ) child_group ON child.id = child_group.child_id 
        WHERE decision.status = :sent
        AND daterange(decision.valid_from, decision.valid_to, '[]') && :period
        AND (:areaId::uuid IS NULL OR area.id = :areaId)
        AND (:unitId::uuid IS NULL OR unit.id = :unitId)
        """.trimIndent()

    return createQuery(sql)
        .bind("sent", VoucherValueDecisionStatus.SENT)
        .bind("period", period)
        .bindNullable("areaId", areaId)
        .bindNullable("unitId", unitId)
        .mapTo<ServiceVoucherValueRow>()
        .toList()
}

private fun Database.Read.snapshotExists(year: Int, month: Int): Boolean {
    return createQuery("SELECT 1 FROM voucher_value_report_snapshot WHERE year >= :year AND month >= :month")
        .bind("year", year)
        .bind("month", month)
        .mapTo<Int>()
        .findFirst()
        .isPresent
}

private fun Database.Read.getSnapshotVoucherValues(
    year: Int,
    month: Int,
    areaId: UUID? = null,
    unitId: UUID? = null
): List<ServiceVoucherValueRowWithDerivatives> {
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
            daterange(decision.valid_from, decision.valid_to, '[]') AS service_voucher_period,
            part.id AS service_voucher_part_id,
            part.voucher_value AS service_voucher_value,
            part.co_payment AS service_voucher_co_payment,
            part.service_coefficient AS service_voucher_service_coefficient,
            part.hours_per_week service_voucher_hours_per_week,
            snapshot_part.realized_amount,
            snapshot_part.realized_period,
            upper(snapshot_part.realized_period) - lower(snapshot_part.realized_period) AS number_of_days
        FROM voucher_value_decision decision
        JOIN voucher_value_decision_part part ON decision.id = part.voucher_value_decision_id
        JOIN voucher_value_report_snapshot snapshot ON snapshot.year = :year AND snapshot.month = :month
        JOIN voucher_value_report_decision_part snapshot_part ON snapshot.id = snapshot_part.voucher_value_report_snapshot_id AND part.id = snapshot_part.decision_part_id
        JOIN person child ON part.child = child.id
        JOIN daycare unit ON part.placement_unit = unit.id
        JOIN care_area area ON unit.care_area_id = area.id
        LEFT JOIN LATERAL (
            SELECT p.child_id, STRING_AGG(dg.name, ', ') AS name
            FROM placement p
            JOIN daycare_group_placement dgp ON p.id = dgp.daycare_placement_id
            JOIN daycare_group dg ON dgp.daycare_group_id = dg.id
            WHERE daterange(decision.valid_from, decision.valid_to, '[]') && daterange(dgp.start_date, dgp.end_date, '[]')
            AND p.unit_id = part.placement_unit
            GROUP BY p.child_id
        ) child_group ON child.id = child_group.child_id
        WHERE (:areaId::uuid IS NULL OR area.id = :areaId)
        AND (:unitId::uuid IS NULL OR unit.id = :unitId)
        """.trimIndent()

    return createQuery(sql)
        .bind("sent", VoucherValueDecisionStatus.SENT)
        .bind("year", year)
        .bind("month", month)
        .bindNullable("areaId", areaId)
        .bindNullable("unitId", unitId)
        .mapTo<ServiceVoucherValueRowWithDerivatives>()
        .toList()
}
