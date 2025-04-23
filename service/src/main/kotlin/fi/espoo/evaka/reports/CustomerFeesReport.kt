// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.invoicing.domain.FinanceDecisionType
import fi.espoo.evaka.placement.PlacementType
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
        @RequestParam decisionType: FinanceDecisionType,
        @RequestParam providerType: ProviderType?,
        @RequestParam placementType: PlacementType?,
    ): List<CustomerFeesReportRow> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.READ_CUSTOMER_FEES_REPORT,
                    )
                    when (decisionType) {
                        FinanceDecisionType.FEE_DECISION ->
                            tx.getFeeDecisionRows(date, areaId, unitId, providerType, placementType)
                        FinanceDecisionType.VOUCHER_VALUE_DECISION ->
                            tx.getVoucherValueDecisionRows(
                                date,
                                areaId,
                                unitId,
                                providerType,
                                placementType,
                            )
                    }
                }
            }
            .also { Audit.CustomerFeesReportRead.log() }
    }

    private fun Database.Read.getFeeDecisionRows(
        date: LocalDate,
        areaId: AreaId?,
        unitId: DaycareId?,
        providerType: ProviderType?,
        placementType: PlacementType?,
    ): List<CustomerFeesReportRow> {
        val predicates =
            PredicateSql.allNotNull(
                areaId?.let { PredicateSql { where("d.care_area_id = ${bind(it)}") } },
                unitId?.let { PredicateSql { where("d.id = ${bind(it)}") } },
                providerType?.let { PredicateSql { where("d.provider_type = ${bind(it)}") } },
                placementType?.let { PredicateSql { where("fdc.placement_type = ${bind(it)}") } },
            )
        return createQuery {
                sql(
                    """
                    SELECT fdc.final_fee AS fee_amount, count(*) AS count
                    FROM fee_decision_child fdc
                    JOIN fee_decision fd ON fd.id = fdc.fee_decision_id
                    JOIN daycare d ON d.id = fdc.placement_unit_id
                    WHERE fd.status = 'SENT' AND ${predicate(predicates)}
                      AND fd.valid_during @> ${bind(date)}
                    GROUP BY fdc.final_fee
                """
                )
            }
            .toList()
    }

    private fun Database.Read.getVoucherValueDecisionRows(
        date: LocalDate,
        areaId: AreaId?,
        unitId: DaycareId?,
        providerType: ProviderType?,
        placementType: PlacementType?,
    ): List<CustomerFeesReportRow> {
        val predicates =
            PredicateSql.allNotNull(
                areaId?.let { PredicateSql { where("d.care_area_id = ${bind(it)}") } },
                unitId?.let { PredicateSql { where("d.id = ${bind(it)}") } },
                providerType?.let { PredicateSql { where("d.provider_type = ${bind(it)}") } },
                placementType?.let { PredicateSql { where("vvd.placement_type = ${bind(it)}") } },
            )
        return createQuery {
                sql(
                    """
                    SELECT vvd.final_co_payment AS fee_amount, count(*) AS count
                    FROM voucher_value_decision vvd
                    JOIN daycare d ON d.id = vvd.placement_unit_id
                    WHERE vvd.status = 'SENT' AND ${predicate(predicates)}
                      AND ${bind(date)} BETWEEN vvd.valid_from AND vvd.valid_to
                    GROUP BY vvd.final_co_payment
                """
                )
            }
            .toList()
    }
}
