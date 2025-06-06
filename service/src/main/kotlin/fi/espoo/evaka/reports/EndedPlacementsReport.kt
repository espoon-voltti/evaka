// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class EndedPlacementsReportController(private val accessControl: AccessControl) {
    @GetMapping("/employee/reports/ended-placements")
    fun getEndedPlacementsReport(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam year: Int,
        @RequestParam month: Int,
    ): List<EndedPlacementsReportRow> {
        val from = LocalDate.of(year, month, 1)
        val to = from.plusMonths(1).minusDays(1)

        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.READ_ENDED_PLACEMENTS_REPORT,
                    )
                    it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    it.getEndedPlacementsRows(from, to)
                }
            }
            .also {
                Audit.EndedPlacementsReportRead.log(
                    meta = mapOf("year" to year, "month" to month, "count" to it.size)
                )
            }
    }
}

private fun Database.Read.getEndedPlacementsRows(
    from: LocalDate,
    to: LocalDate,
): List<EndedPlacementsReportRow> {
    return createQuery {
            sql(
                """
WITH ended_placements AS (
    SELECT 
        p.id AS child_id,
        p.first_name,
        p.last_name,
        p.date_of_birth,
        max(pl.end_date) AS placement_end
    FROM placement pl
    JOIN person p ON p.id = pl.child_id
    WHERE daterange(${bind(from)}, ${bind(to)}, '[]') @> pl.end_date AND pl.type != 'CLUB'::placement_type
    GROUP BY p.id, p.first_name, p.last_name, p.date_of_birth
)
SELECT 
    ep.child_id,
    ep.first_name,
    ep.last_name,
    ep.date_of_birth,
    ep.placement_end,
    dc.name as unit_name,
    ca.name as area_name,
    min(next_pl.start_date) AS next_placement_start,
    next_dc.name AS next_placement_unit_name
FROM ended_placements ep
JOIN placement pl
    ON ep.child_id = pl.child_id AND pl.end_date = ep.placement_end
JOIN daycare dc
    ON pl.unit_id = dc.id
JOIN care_area ca
    ON dc.care_area_id = ca.id
LEFT JOIN placement next_pl
    ON next_pl.child_id = ep.child_id AND next_pl.start_date > ep.placement_end AND next_pl.type != 'CLUB'::placement_type
LEFT JOIN daycare next_dc
    ON next_dc.id = next_pl.unit_id
GROUP BY ep.child_id, ep.first_name, ep.last_name, ep.date_of_birth, ep.placement_end, dc.name, ca.name, next_dc.name
HAVING min(next_pl.start_date) IS NULL OR min(next_pl.start_date) > ${bind(to)}
ORDER BY last_name, first_name, ep.child_id
"""
            )
        }
        .toList<EndedPlacementsReportRow>()
}

data class EndedPlacementsReportRow(
    val childId: ChildId,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate?,
    val placementEnd: LocalDate,
    val unitName: String,
    val areaName: String,
    val nextPlacementStart: LocalDate?,
    val nextPlacementUnitName: String?,
)
