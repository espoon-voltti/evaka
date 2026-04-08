// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.reports

import evaka.core.shared.AreaId
import evaka.core.shared.ChildId
import evaka.core.shared.DaycareId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import evaka.core.shared.security.actionrule.AccessControlFilter
import evaka.core.shared.security.actionrule.forTable
import java.time.LocalDate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class PlacementGuaranteeReportController(private val accessControl: AccessControl) {

    @GetMapping("/employee/reports/placement-guarantee")
    fun getPlacementGuaranteeReport(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam date: LocalDate,
        @RequestParam unitId: DaycareId? = null,
    ): List<PlacementGuaranteeReportRow> {
        return db.connect { dbc ->
            dbc.read { tx ->
                val filter =
                    accessControl.requireAuthorizationFilter(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_PLACEMENT_GUARANTEE_REPORT,
                    )
                tx.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                tx.getPlacementGuaranteeRows(filter, date, unitId)
            }
        }
    }
}

private fun Database.Read.getPlacementGuaranteeRows(
    filter: AccessControlFilter<DaycareId>,
    date: LocalDate,
    unitId: DaycareId?,
): List<PlacementGuaranteeReportRow> =
    createQuery {
            sql(
                """
SELECT
  child.id AS child_id,
  child.last_name AS child_last_name,
  child.first_name AS child_first_name,
  unit.id AS unit_id,
  unit.name AS unit_name,
  area.id AS area_id,
  area.name AS area_name,
  placement.start_date AS placement_start_date,
  placement.end_date AS placement_end_date
FROM placement
JOIN daycare unit ON unit.id = placement.unit_id
JOIN care_area area ON area.id = unit.care_area_id
JOIN person child ON child.id = placement.child_id
WHERE placement.place_guarantee = TRUE
  AND ${predicate(filter.forTable("unit"))}
  AND placement.start_date > ${bind(date)}
  AND (${bind(unitId)} IS NULL OR ${bind(unitId)} = placement.unit_id)
  AND NOT EXISTS (SELECT FROM placement WHERE child_id = child.id AND ${bind(date)} BETWEEN start_date AND end_date)
    """
                    .trimIndent()
            )
        }
        .toList<PlacementGuaranteeReportRow>()

data class PlacementGuaranteeReportRow(
    val childId: ChildId,
    val childLastName: String,
    val childFirstName: String,
    val unitId: DaycareId,
    val unitName: String,
    val areaId: AreaId,
    val areaName: String,
    val placementStartDate: LocalDate,
    val placementEndDate: LocalDate,
)
