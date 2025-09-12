// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.nekku.getDaycareGroupIds
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class NekkuOrderReportController(private val accessControl: AccessControl) {

    @GetMapping("/employee/reports/nekkuorders/{unitId}")
    fun getNekkuOrderReportByUnit(
        db: Database,
        clock: EvakaClock,
        user: AuthenticatedUser.Employee,
        @PathVariable unitId: DaycareId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) start: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) end: LocalDate,
        @RequestParam groupIds: List<GroupId>?,
    ): List<NekkuOrderRow> {
        if (start.isAfter(end)) throw BadRequest("Inverted time range")
        if (end.isAfter(start.plusMonths(1))) throw BadRequest("Too long time range")

        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_NEKKU_ORDER_REPORT,
                        unitId,
                    )
                    tx.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    getNekkuOrderReportRows(tx, start, end, unitId, groupIds)
                }
            }
            .also {
                Audit.NekkuOrdersReportRead.log(
                    targetId = AuditId(unitId),
                    meta =
                        mapOf(
                            "groupIds" to groupIds,
                            "startDate" to start,
                            "endDate" to end,
                            "count" to it.size,
                        ),
                )
            }
    }

    fun getNekkuOrderReportRows(
        tx: Database.Read,
        start: LocalDate,
        end: LocalDate,
        unitId: DaycareId,
        groupIds: List<GroupId>?,
    ): List<NekkuOrderRow> {

        val effectiveGroupIds = groupIds ?: tx.getDaycareGroupIds(unitId)

        val dateList = generateDateList(start, end)

        val orderRows = tx.getNekkuReportRows(unitId, effectiveGroupIds, dateList)

        return orderRows
    }
}

fun Database.Read.getNekkuReportRows(
    daycareId: DaycareId,
    groupIds: List<GroupId>,
    dates: List<LocalDate>,
): List<NekkuOrderRow> {
    return createQuery {
            sql(
                """
            SELECT 
                nor.delivery_date as date, 
                nor.meal_sku as sku,
                nor.total_quantity as quantity, 
                dg.name as groupname,
                coalesce(nor.meal_time, '{}') as mealtime,
                nor.meal_type as mealtype,
                nor.meals_by_special_diet as specialdiets,
                nor.nekku_order_info as nekkuOrderInfo
            FROM nekku_orders_report nor
                JOIN daycare_group dg
                ON nor.group_id = dg.id
                WHERE nor.daycare_id = ${bind(daycareId)}
                AND nor.group_id =ANY(${bind ( groupIds)})
                AND nor.delivery_date =ANY(${bind ( dates)})
                ORDER BY nor.delivery_date, nor.group_id, nor.meal_sku
            """
            )
        }
        .toList<NekkuOrderRow>()
}

data class NekkuOrderRow(
    val date: LocalDate,
    val sku: String,
    val quantity: Int,
    val groupName: String,
    val mealTime: List<String>,
    val mealType: String?,
    val specialDiets: String?,
    val nekkuOrderInfo: String?,
)

private fun generateDateList(start: LocalDate, end: LocalDate): List<LocalDate> =
    generateSequence(start) { it.plusDays(1) }.takeWhile { it <= end }.toList()
