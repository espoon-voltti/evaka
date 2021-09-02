// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.occupancy.OccupancyType
import fi.espoo.evaka.occupancy.OccupancyValues
import fi.espoo.evaka.occupancy.calculateDailyGroupOccupancyValues
import fi.espoo.evaka.occupancy.calculateDailyUnitOccupancyValues
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
class OccupancyReportController {
    @GetMapping("/reports/occupancy-by-unit")
    fun getOccupancyUnitReport(
        db: Database,
        user: AuthenticatedUser,
        @RequestParam type: OccupancyType,
        @RequestParam careAreaId: AreaId,
        @RequestParam year: Int,
        @RequestParam month: Int
    ): ResponseEntity<List<OccupancyUnitReportResultRow>> {
        Audit.OccupancyReportRead.log(targetId = careAreaId)
        user.requireOneOfRoles(UserRole.SERVICE_WORKER, UserRole.ADMIN, UserRole.DIRECTOR)
        val from = LocalDate.of(year, month, 1)
        val to = from.plusMonths(1).minusDays(1)

        val occupancies = db.read { tx ->
            tx.calculateUnitOccupancyReport(
                LocalDate.now(),
                careAreaId,
                FiniteDateRange(from, to),
                type
            )
        }

        return ResponseEntity.ok(occupancies)
    }

    @GetMapping("/reports/occupancy-by-group")
    fun getOccupancyGroupReport(
        db: Database,
        user: AuthenticatedUser,
        @RequestParam type: OccupancyType,
        @RequestParam careAreaId: AreaId,
        @RequestParam year: Int,
        @RequestParam month: Int
    ): ResponseEntity<List<OccupancyGroupReportResultRow>> {
        Audit.OccupancyReportRead.log(targetId = careAreaId)
        user.requireOneOfRoles(UserRole.SERVICE_WORKER, UserRole.ADMIN, UserRole.DIRECTOR)
        val from = LocalDate.of(year, month, 1)
        val to = from.plusMonths(1).minusDays(1)

        val occupancies = db.read { tx ->
            tx.calculateGroupOccupancyReport(
                LocalDate.now(),
                careAreaId,
                FiniteDateRange(from, to),
                type
            )
        }

        return ResponseEntity.ok(occupancies)
    }
}

data class OccupancyUnitReportResultRow(
    val unitId: DaycareId,
    val unitName: String,
    val occupancies: Map<LocalDate, OccupancyValues>
)

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
    type: OccupancyType
): List<OccupancyUnitReportResultRow> {
    return calculateDailyUnitOccupancyValues(today, queryPeriod, type, areaId = areaId)
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
    type: OccupancyType
): List<OccupancyGroupReportResultRow> {
    if (type == OccupancyType.PLANNED) throw BadRequest("Unable to calculate planned occupancy at group level")

    return calculateDailyGroupOccupancyValues(today, queryPeriod, type, areaId = areaId)
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
