// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import fi.espoo.evaka.shared.security.actionrule.forTable
import java.time.LocalDate
import java.time.Month
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class StartingPlacementsReportController(private val accessControl: AccessControl) {
    @GetMapping("/employee/reports/starting-placements")
    fun getStartingPlacementsReport(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam year: Int,
        @RequestParam month: Int,
    ): List<StartingPlacementsRow> {
        return db.connect { dbc ->
                dbc.read {
                    val filter =
                        accessControl.requireAuthorizationFilter(
                            it,
                            user,
                            clock,
                            Action.Unit.READ_STARTING_PLACEMENTS_REPORT,
                        )
                    it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    it.getStartingPlacementsRows(year, month, filter)
                }
            }
            .also {
                Audit.StartingPlacementsReportRead.log(
                    meta = mapOf("year" to year, "month" to month, "count" to it.size)
                )
            }
    }
}

data class StartingPlacementsRow(
    val childId: ChildId,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate,
    val placementStart: LocalDate,
    val careAreaName: String,
    val unitName: String,
    val placementId: PlacementId,
)

/*
 * The preceding placement logic has to be checked if club placements are to be added to the placements table
 */
private fun Database.Read.getStartingPlacementsRows(
    year: Int,
    month: Int,
    unitFilter: AccessControlFilter<DaycareId>,
): List<StartingPlacementsRow> {
    val range = FiniteDateRange.ofMonth(year, Month.of(month))
    return createQuery {
            sql(
                """
SELECT p.child_id, p.start_date AS placement_start, c.first_name, c.last_name, c.date_of_birth,
    (CASE
        WHEN u.provider_type = 'PRIVATE_SERVICE_VOUCHER' THEN 'palvelusetelialue'
        ELSE ca.name
    END) AS care_area_name,
    u.name as unit_name,
    p.id as placement_id
FROM placement p
JOIN person c ON p.child_id = c.id
JOIN daycare u ON p.unit_id = u.id
JOIN care_area ca ON u.care_area_id = ca.id
LEFT JOIN placement preceding ON p.child_id = preceding.child_id AND (p.start_date - interval '1 days') = preceding.end_date AND preceding.type != 'CLUB'::placement_type
WHERE between_start_and_end(${bind(range)}, p.start_date) AND preceding.id IS NULL AND p.type != 'CLUB'::placement_type
AND ${predicate(unitFilter.forTable("u"))} 
"""
            )
        }
        .toList<StartingPlacementsRow>()
}
