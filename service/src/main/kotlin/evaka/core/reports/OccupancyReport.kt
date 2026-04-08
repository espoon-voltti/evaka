// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.reports

import evaka.core.Audit
import evaka.core.daycare.CareType
import evaka.core.daycare.domain.ProviderType
import evaka.core.occupancy.OccupancyType
import evaka.core.occupancy.OccupancyValues
import evaka.core.occupancy.calculateDailyGroupOccupancyValues
import evaka.core.occupancy.calculateDailyUnitOccupancyValues
import evaka.core.shared.AreaId
import evaka.core.shared.DaycareId
import evaka.core.shared.GroupId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import evaka.core.shared.security.actionrule.AccessControlFilter
import java.time.LocalDate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class OccupancyReportController(private val accessControl: AccessControl) {
    @GetMapping("/employee/reports/occupancy-by-unit")
    fun getOccupancyUnitReport(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam type: OccupancyType,
        @RequestParam careAreaId: AreaId?,
        @RequestParam unitIds: Set<DaycareId>?,
        @RequestParam providerType: ProviderType?,
        @RequestParam unitTypes: Set<CareType>?,
        @RequestParam year: Int,
        @RequestParam month: Int,
    ): List<OccupancyUnitReportResultRow> {
        val from = LocalDate.of(year, month, 1)
        val to = from.plusMonths(1).minusDays(1)

        return db.connect { dbc ->
                dbc.read { tx ->
                    val filter =
                        accessControl.requireAuthorizationFilter(
                            tx,
                            user,
                            clock,
                            Action.Unit.READ_OCCUPANCY_REPORT,
                        )
                    tx.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    tx.calculateUnitOccupancyReport(
                        clock.today(),
                        careAreaId,
                        unitIds,
                        providerType,
                        unitTypes,
                        FiniteDateRange(from, to),
                        type,
                        filter,
                    )
                }
            }
            .also {
                Audit.OccupancyReportRead.log(
                    meta =
                        mapOf(
                            "careAreaId" to careAreaId,
                            "providerType" to providerType,
                            "unitTypes" to unitTypes,
                            "year" to year,
                            "month" to month,
                            "count" to it.size,
                        )
                )
            }
    }

    @GetMapping("/employee/reports/occupancy-by-group")
    fun getOccupancyGroupReport(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam type: OccupancyType,
        @RequestParam careAreaId: AreaId?,
        @RequestParam unitIds: Set<DaycareId>?,
        @RequestParam providerType: ProviderType?,
        @RequestParam unitTypes: Set<CareType>?,
        @RequestParam year: Int,
        @RequestParam month: Int,
    ): List<OccupancyGroupReportResultRow> {
        val from = LocalDate.of(year, month, 1)
        val to = from.plusMonths(1).minusDays(1)

        return db.connect { dbc ->
                dbc.read { tx ->
                    val filter =
                        accessControl.requireAuthorizationFilter(
                            tx,
                            user,
                            clock,
                            Action.Unit.READ_OCCUPANCY_REPORT,
                        )
                    tx.calculateGroupOccupancyReport(
                        clock.today(),
                        careAreaId,
                        unitIds,
                        providerType,
                        unitTypes,
                        FiniteDateRange(from, to),
                        type,
                        filter,
                    )
                }
            }
            .also {
                Audit.OccupancyGroupReportRead.log(
                    meta =
                        mapOf(
                            "careAreaId" to careAreaId,
                            "providerType" to providerType,
                            "unitTypes" to unitTypes,
                            "year" to year,
                            "month" to month,
                            "count" to it.size,
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
    val occupancies: Map<LocalDate, OccupancyValues>,
)

data class OccupancyGroupReportResultRow(
    val areaId: AreaId,
    val areaName: String,
    val unitId: DaycareId,
    val unitName: String,
    val groupId: GroupId,
    val groupName: String,
    val occupancies: Map<LocalDate, OccupancyValues>,
)

private fun Database.Read.calculateUnitOccupancyReport(
    today: LocalDate,
    areaId: AreaId?,
    unitIds: Set<DaycareId>?,
    providerType: ProviderType?,
    unitTypes: Set<CareType>?,
    queryPeriod: FiniteDateRange,
    type: OccupancyType,
    unitFilter: AccessControlFilter<DaycareId>,
): List<OccupancyUnitReportResultRow> {
    return calculateDailyUnitOccupancyValues(
            today,
            queryPeriod,
            type,
            unitFilter,
            areaId = areaId,
            unitIds = unitIds,
            providerType = providerType,
            unitTypes = unitTypes,
        )
        .map { (key, occupancies) ->
            OccupancyUnitReportResultRow(
                areaId = key.areaId,
                areaName = key.areaName,
                unitId = key.unitId,
                unitName = key.unitName,
                occupancies = occupancies,
            )
        }
        .sortedWith(compareBy({ it.areaName }, { it.unitName }))
}

private fun Database.Read.calculateGroupOccupancyReport(
    today: LocalDate,
    areaId: AreaId?,
    unitIds: Set<DaycareId>?,
    providerType: ProviderType?,
    unitTypes: Set<CareType>?,
    queryPeriod: FiniteDateRange,
    type: OccupancyType,
    unitFilter: AccessControlFilter<DaycareId>,
): List<OccupancyGroupReportResultRow> {
    if (type == OccupancyType.PLANNED)
        throw BadRequest("Unable to calculate planned occupancy at group level")

    return calculateDailyGroupOccupancyValues(
            today,
            queryPeriod,
            type,
            unitFilter,
            areaId = areaId,
            unitIds = unitIds,
            providerType = providerType,
            unitTypes = unitTypes,
        )
        .map { (key, occupancies) ->
            OccupancyGroupReportResultRow(
                areaId = key.areaId,
                areaName = key.areaName,
                unitId = key.unitId,
                unitName = key.unitName,
                groupId = key.groupId,
                groupName = key.groupName,
                occupancies = occupancies,
            )
        }
        .sortedWith(compareBy({ it.areaName }, { it.unitName }, { it.groupName }))
}
