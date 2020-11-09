package fi.espoo.evaka.reports

import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.bindNullable
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.domain.ClosedPeriod
import fi.espoo.evaka.shared.domain.Period
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
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

class ServiceVoucherValueReportController(private val jdbi: Jdbi, private val acl: AccessControlList) {
    @GetMapping("/units")
    fun getServiceVoucherValuesForAllUnits(
        user: AuthenticatedUser,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam areaId: UUID

    ): ResponseEntity<List<ServiceVoucherValueUnitAggregate>> {
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.FINANCE_ADMIN)

        return jdbi.handle { h ->
            val period = ClosedPeriod(
                LocalDate.of(year, month, 1),
                LocalDate.of(year, month, Month.of(month).length(Year.isLeap(year.toLong())))
            )
            val rows = getServiceVoucherValues(h, period, areaId = areaId)

            val aggregates = rows
                .groupBy { ServiceVoucherValueUnitAggregate.UnitData(it.unitId, it.unitName, it.areaId, it.areaName) }
                .map { (unit, rows) ->
                    ServiceVoucherValueUnitAggregate(
                        unit = unit,
                        childCount = rows.map { it.childId }.distinct().size,
                        monthlyPaymentSum = rows.sumBy { row ->
                            realizedMonthlyAmount(
                                row.serviceVoucherValue - row.serviceVoucherCoPayment,
                                row.serviceVoucherPeriod,
                                period
                            ).realizedAmount
                        }
                    )
                }

            ResponseEntity.ok(aggregates)
        }
    }

    @GetMapping("/units/{unitId}")
    fun getServiceVoucherValuesForUnit(
        user: AuthenticatedUser,
        @PathVariable("unitId") unitId: UUID,
        @RequestParam year: Int,
        @RequestParam month: Int
    ): ResponseEntity<List<ServiceVoucherValueRow>> {
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.FINANCE_ADMIN)

        return jdbi.handle { h ->
            val period = ClosedPeriod(
                LocalDate.of(year, month, 1),
                LocalDate.of(year, month, Month.of(month).length(Year.isLeap(year.toLong())))
            )
            val rows = getServiceVoucherValues(h, period, unitId = unitId)
                .map { row ->
                    row.copy(
                        derivatives = realizedMonthlyAmount(
                            row.serviceVoucherValue - row.serviceVoucherCoPayment,
                            row.serviceVoucherPeriod,
                            period
                        )
                    )
                }

            ResponseEntity.ok(rows)
        }
    }
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
    val serviceVoucherPeriod: Period,
    val serviceVoucherValue: Int,
    val serviceVoucherCoPayment: Int,
    val derivatives: ValueRowDerivatives? = null
)

data class ValueRowDerivatives(
    val realizedAmount: Int,
    val coefficient: BigDecimal,
    val realizedPeriod: ClosedPeriod,
    val numberOfDays: Long
)

private fun realizedMonthlyAmount(value: Int, decisionPeriod: Period, monthPeriod: ClosedPeriod): ValueRowDerivatives {
    val realizedPeriod = ClosedPeriod(
        maxOf(decisionPeriod.start, monthPeriod.start),
        minOf(decisionPeriod.end ?: monthPeriod.end, monthPeriod.end)
    )

    val numberOfDays = realizedPeriod.durationInDays()
    val coefficient = BigDecimal(numberOfDays).divide(BigDecimal(monthPeriod.durationInDays()), 10, RoundingMode.HALF_UP)
    val realizedAmount = (BigDecimal(value) * coefficient).setScale(0, RoundingMode.HALF_UP).toInt()

    return ValueRowDerivatives(realizedAmount, coefficient, realizedPeriod, numberOfDays)
}

private fun getServiceVoucherValues(
    h: Handle,
    period: ClosedPeriod,
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
            part.voucher_value AS service_voucher_value,
            part.co_payment AS service_voucher_co_payment,
            part.service_coefficient AS service_voucher_service_coefficient
        FROM voucher_value_decision decision
        JOIN voucher_value_decision_part part ON decision.id = part.voucher_value_decision_id
        JOIN person child ON part.child = child.id
        JOIN daycare unit ON part.placement_unit = unit.id
        JOIN care_area area ON unit.care_area_id = area.id
        LEFT JOIN (
            SELECT p.child_id, dg.name, p.start_date, p.end_date
            FROM placement p
            JOIN daycare_group_placement dgp ON p.id = dgp.daycare_placement_id
            JOIN daycare_group dg ON dgp.daycare_group_id = dg.id
        ) child_group ON child.id = child_group.child_id AND daterange(decision.valid_from, decision.valid_to, '[]') && 
                daterange(child_group.start_date, child_group.end_date, '[]')
        WHERE decision.status = :sent
        AND daterange(decision.valid_from, decision.valid_to, '[]') && :period
        AND (:areaId::uuid IS NULL OR area.id = :areaId)
        AND (:unitId::uuid IS NULL OR unit.id = :unitId)
        """.trimIndent()

    return h.createQuery(sql)
        .bind("sent", VoucherValueDecisionStatus.SENT)
        .bind("period", period)
        .bindNullable("areaId", areaId)
        .bindNullable("unitId", unitId)
        .mapTo<ServiceVoucherValueRow>()
        .toList()
}
