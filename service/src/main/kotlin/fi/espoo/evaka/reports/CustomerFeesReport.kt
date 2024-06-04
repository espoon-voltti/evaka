// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.invoicing.domain.FinanceDecisionType
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.PredicateSql
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class CustomerFeesReport(private val accessControl: AccessControl) {
    data class CustomerFeesReportRow(val feeAmount: Int, val count: Int)

    @GetMapping("/employee/reports/customer-fees")
    fun getCustomerFeesReport(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
        @RequestParam areaId: AreaId?,
        @RequestParam unitId: DaycareId?,
        @RequestParam decisionType: FinanceDecisionType
    ): List<CustomerFeesReportRow> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.READ_CUSTOMER_FEES_REPORT
                    )
                    when (decisionType) {
                        FinanceDecisionType.FEE_DECISION ->
                            tx.getFeeDecisionRows(date, areaId, unitId)
                        FinanceDecisionType.VOUCHER_VALUE_DECISION ->
                            tx.getVoucherValueDecisionRows(date, areaId, unitId)
                    }
                }
            }
            .also { Audit.CustomerFeesReportRead.log() }
    }

    private fun Database.Read.getFeeDecisionRows(
        date: LocalDate,
        areaId: AreaId?,
        unitId: DaycareId?
    ): List<CustomerFeesReportRow> {
        val predicates =
            PredicateSql { where("fd.valid_during @> ${bind(date)}") }
                .and(
                    areaId?.let { PredicateSql { where("d.care_area_id = ${bind(it)}") } }
                        ?: PredicateSql.alwaysTrue()
                )
                .and(
                    unitId?.let { PredicateSql { where("d.id = ${bind(it)}") } }
                        ?: PredicateSql.alwaysTrue()
                )
        return createQuery {
                sql(
                    """
                    SELECT fdc.final_fee AS fee_amount, count(*) AS count
                    FROM fee_decision_child fdc
                    JOIN fee_decision fd ON fd.id = fdc.fee_decision_id
                    JOIN daycare d ON d.id = fdc.placement_unit_id
                    WHERE fd.status = 'SENT' AND ${predicate(predicates)}
                    GROUP BY fdc.final_fee
                    ORDER BY fdc.final_fee
                """
                )
            }
            .toList()
    }

    private fun Database.Read.getVoucherValueDecisionRows(
        date: LocalDate,
        areaId: AreaId?,
        unitId: DaycareId?
    ): List<CustomerFeesReportRow> {
        val predicates =
            PredicateSql { where("daterange(vvd.valid_from, vvd.valid_to, '[]') @> ${bind(date)}") }
                .and(
                    areaId?.let { PredicateSql { where("d.care_area_id = ${bind(it)}") } }
                        ?: PredicateSql.alwaysTrue()
                )
                .and(
                    unitId?.let { PredicateSql { where("d.id = ${bind(it)}") } }
                        ?: PredicateSql.alwaysTrue()
                )
        return createQuery {
                sql(
                    """
                    SELECT vvd.final_co_payment AS fee_amount, count(*) AS count
                    FROM voucher_value_decision vvd
                    JOIN daycare d ON d.id = vvd.placement_unit_id
                    WHERE vvd.status = 'SENT' AND ${predicate(predicates)}
                    GROUP BY vvd.final_co_payment
                """
                )
            }
            .toList()
    }
}
