// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.CareType
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.occupancy.OccupancyType
import fi.espoo.evaka.occupancy.OccupancyValues
import fi.espoo.evaka.occupancy.calculateDailyGroupOccupancyValues
import fi.espoo.evaka.occupancy.calculateDailyUnitOccupancyValues
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import java.time.LocalDate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class OccupancyReportController(
    private val accessControl: AccessControl
) {
    @GetMapping(
        "/reports/occupancy-by-unit", // deprecated
        "/employee/reports/occupancy-by-unit"
    )
    fun getOccupancyUnitReport(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam type: OccupancyType,
        @RequestParam careAreaId: AreaId?,
        @RequestParam providerType: ProviderType?,
        @RequestParam unitTypes: Set<CareType>?,
        @RequestParam year: Int,
        @RequestParam month: Int
    ): List<OccupancyUnitReportResultRow> {
        val from = LocalDate.of(year, month, 1)
        val to = from.plusMonths(1).minusDays(1)

        return db
            .connect { dbc ->
                dbc.read { tx ->
                    val filter =
                        accessControl.requireAuthorizationFilter(
                            tx,
                            user,
                            clock,
                            Action.Unit.READ_OCCUPANCY_REPORT
                        )
                    tx.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    tx.calculateUnitOccupancyReport(
                        clock.today(),
                        careAreaId,
                        providerType,
                        unitTypes,
                        FiniteDateRange(from, to),
                        type,
                        filter
                    )
                }
            }.also {
                Audit.OccupancyReportRead.log(
                    meta =
                        mapOf(
                            "careAreaId" to careAreaId,
                            "providerType" to providerType,
                            "unitTypes" to unitTypes,
                            "year" to year,
                            "month" to month,
                            "count" to it.size
                        )
                )
            }
    }

    @GetMapping(
        "/reports/occupancy-by-group", // deprecated
        "/employee/reports/occupancy-by-group"
    )
    fun getOccupancyGroupReport(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam type: OccupancyType,
        @RequestParam careAreaId: AreaId?,
        @RequestParam providerType: ProviderType?,
        @RequestParam unitTypes: Set<CareType>?,
        @RequestParam year: Int,
        @RequestParam month: Int
    ): List<OccupancyGroupReportResultRow> {
        val from = LocalDate.of(year, month, 1)
        val to = from.plusMonths(1).minusDays(1)

        return db
            .connect { dbc ->
                dbc.read { tx ->
                    val filter =
                        accessControl.requireAuthorizationFilter(
                            tx,
                            user,
                            clock,
                            Action.Unit.READ_OCCUPANCY_REPORT
                        )
                    tx.calculateGroupOccupancyReport(
                        clock.today(),
                        careAreaId,
                        providerType,
                        unitTypes,
                        FiniteDateRange(from, to),
                        type,
                        filter
                    )
                }
            }.also {
                Audit.OccupancyGroupReportRead.log(
                    meta =
                        mapOf(
                            "careAreaId" to careAreaId,
                            "providerType" to providerType,
                            "unitTypes" to unitTypes,
                            "year" to year,
                            "month" to month,
                            "count" to it.size
                        )
                )
            }
    }
}

data class OccupancyUnitReportResultRow(
    val areaId: AreaId,
    val areaName: String,
    val unitId: DaycareId,
    val unitName: String,
    val occupancies: Map<LocalDate, OccupancyValues>
)

data class OccupancyGroupReportResultRow(
    val areaId: AreaId,
    val areaName: String,
    val unitId: DaycareId,
    val unitName: String,
    val groupId: GroupId,
    val groupName: String,
    val occupancies: Map<LocalDate, OccupancyValues>
)

private fun Database.Read.calculateUnitOccupancyReport(
    today: LocalDate,
    areaId: AreaId?,
    providerType: ProviderType?,
    unitTypes: Set<CareType>?,
    queryPeriod: FiniteDateRange,
    type: OccupancyType,
    unitFilter: AccessControlFilter<DaycareId>
): List<OccupancyUnitReportResultRow> =
    calculateDailyUnitOccupancyValues(
        today,
        queryPeriod,
        type,
        unitFilter,
        areaId = areaId,
        providerType = providerType,
        unitTypes = unitTypes
    ).map { (key, occupancies) ->
        OccupancyUnitReportResultRow(
            areaId = key.areaId,
            areaName = key.areaName,
            unitId = key.unitId,
            unitName = key.unitName,
            occupancies = occupancies
        )
    }.sortedWith(compareBy({ it.areaName }, { it.unitName }))

private fun Database.Read.calculateGroupOccupancyReport(
    today: LocalDate,
    areaId: AreaId?,
    providerType: ProviderType?,
    unitTypes: Set<CareType>?,
    queryPeriod: FiniteDateRange,
    type: OccupancyType,
    unitFilter: AccessControlFilter<DaycareId>
): List<OccupancyGroupReportResultRow> {
    if (type == OccupancyType.PLANNED) {
        throw BadRequest("Unable to calculate planned occupancy at group level")
    }

    return calculateDailyGroupOccupancyValues(
        today,
        queryPeriod,
        type,
        unitFilter,
        areaId = areaId,
        providerType = providerType,
        unitTypes = unitTypes
    ).map { (key, occupancies) ->
        OccupancyGroupReportResultRow(
            areaId = key.areaId,
            areaName = key.areaName,
            unitId = key.unitId,
            unitName = key.unitName,
            groupId = key.groupId,
            groupName = key.groupName,
            occupancies = occupancies
        )
    }.sortedWith(compareBy({ it.areaName }, { it.unitName }))
}
