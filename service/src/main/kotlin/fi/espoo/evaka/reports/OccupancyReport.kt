// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.ExcludeCodeGen
import fi.espoo.evaka.occupancy.OccupancyType
import fi.espoo.evaka.occupancy.OccupancyValues
import fi.espoo.evaka.occupancy.calculateDailyGroupOccupancyValues
import fi.espoo.evaka.occupancy.calculateDailyUnitOccupancyValues
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AclAuthorization
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
class OccupancyReportController(private val accessControl: AccessControl, private val acl: AccessControlList) {
    @GetMapping("/reports/occupancy-by-unit")
    fun getOccupancyUnitReport(
        db: Database,
        user: AuthenticatedUser,
        @RequestParam type: OccupancyType,
        @RequestParam careAreaId: AreaId,
        @RequestParam year: Int,
        @RequestParam month: Int
    ): List<OccupancyUnitReportResultRow> {
        Audit.OccupancyReportRead.log(targetId = careAreaId)
        accessControl.requirePermissionFor(user, Action.Global.READ_OCCUPANCY_REPORT)
        val from = LocalDate.of(year, month, 1)
        val to = from.plusMonths(1).minusDays(1)

        return db.connect { dbc ->
            dbc.read { tx ->
                tx.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                tx.calculateUnitOccupancyReport(
                    LocalDate.now(),
                    careAreaId,
                    FiniteDateRange(from, to),
                    type,
                    acl.getAuthorizedUnits(user)
                )
            }
        }
    }

    @GetMapping("/reports/occupancy-by-group")
    fun getOccupancyGroupReport(
        db: Database,
        user: AuthenticatedUser,
        @RequestParam type: OccupancyType,
        @RequestParam careAreaId: AreaId,
        @RequestParam year: Int,
        @RequestParam month: Int
    ): List<OccupancyGroupReportResultRow> {
        Audit.OccupancyReportRead.log(targetId = careAreaId)
        accessControl.requirePermissionFor(user, Action.Global.READ_OCCUPANCY_REPORT)
        val from = LocalDate.of(year, month, 1)
        val to = from.plusMonths(1).minusDays(1)

        return db.connect { dbc ->
            dbc.read { tx ->
                tx.calculateGroupOccupancyReport(
                    LocalDate.now(),
                    careAreaId,
                    FiniteDateRange(from, to),
                    type,
                    acl.getAuthorizedUnits(user)
                )
            }
        }
    }
}

@ExcludeCodeGen
data class OccupancyUnitReportResultRow(
    val unitId: DaycareId,
    val unitName: String,
    val occupancies: Map<LocalDate, OccupancyValues>
)

@ExcludeCodeGen
data class OccupancyGroupReportResultRow(
    val unitId: DaycareId,
    val unitName: String,
    val groupId: GroupId,
    val groupName: String,
    val occupancies: Map<LocalDate, OccupancyValues>
)

private fun Database.Read.calculateUnitOccupancyReport(
    today: LocalDate,
    areaId: AreaId,
    queryPeriod: FiniteDateRange,
    type: OccupancyType,
    aclAuth: AclAuthorization
): List<OccupancyUnitReportResultRow> {
    return calculateDailyUnitOccupancyValues(today, queryPeriod, type, aclAuth, areaId = areaId)
        .map { (key, occupancies) ->
            OccupancyUnitReportResultRow(
                unitId = key.unitId,
                unitName = key.unitName,
                occupancies = occupancies
            )
        }
        .sortedBy { it.unitName }
}

private fun Database.Read.calculateGroupOccupancyReport(
    today: LocalDate,
    areaId: AreaId,
    queryPeriod: FiniteDateRange,
    type: OccupancyType,
    aclAuth: AclAuthorization
): List<OccupancyGroupReportResultRow> {
    if (type == OccupancyType.PLANNED) throw BadRequest("Unable to calculate planned occupancy at group level")

    return calculateDailyGroupOccupancyValues(today, queryPeriod, type, aclAuth, areaId = areaId)
        .map { (key, occupancies) ->
            OccupancyGroupReportResultRow(
                unitId = key.unitId,
                unitName = key.unitName,
                groupId = key.groupId,
                groupName = key.groupName,
                occupancies = occupancies
            )
        }
        .sortedBy { it.unitName }
}
