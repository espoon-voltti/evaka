// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import fi.espoo.evaka.shared.security.actionrule.forTable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class FamilyConflictReportController(private val accessControl: AccessControl) {
    @GetMapping(
        "/reports/family-conflicts", // deprecated
        "/employee/reports/family-conflicts"
    )
    fun getFamilyConflictsReport(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock
    ): List<FamilyConflictReportRow> {
        return db.connect { dbc ->
                dbc.read {
                    val filter =
                        accessControl.requireAuthorizationFilter(
                            it,
                            user,
                            clock,
                            Action.Unit.READ_FAMILY_CONFLICT_REPORT
                        )
                    it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    it.getFamilyConflicts(filter)
                }
            }
            .also { Audit.FamilyConflictReportRead.log(meta = mapOf("count" to it.size)) }
    }
}

private fun Database.Read.getFamilyConflicts(
    unitFilter: AccessControlFilter<DaycareId>
): List<FamilyConflictReportRow> =
    createQuery {
            sql(
                """
        WITH child_conflicts AS (
            SELECT head_of_child as id, count(*) as child_conflict_count
            FROM fridge_child
            WHERE conflict = true
            GROUP BY head_of_child
        ), partner_conflicts AS (
            SELECT person_id as id, count(*) as partner_conflict_count
            FROM fridge_partner
            WHERE conflict = true
            GROUP BY person_id
        ), conflicts AS (
            SELECT
                p.id,
                p.first_name,
                p.last_name,
                p.social_security_number,
                coalesce(cc.child_conflict_count, 0) as child_conflict_count,
                coalesce(pc.partner_conflict_count, 0) as partner_conflict_count
            FROM child_conflicts cc
            FULL JOIN partner_conflicts pc ON cc.id = pc.id
            JOIN person p ON p.id = coalesce(cc.id, pc.id)
        )
        SELECT 
            ca.name AS care_area_name,
            u.id AS unit_id,
            u.name AS unit_name,
            co.id,
            co.first_name,
            co.last_name,
            co.social_security_number,
            co.child_conflict_count,
            co.partner_conflict_count
        FROM conflicts co
        JOIN primary_units_view pu ON co.id = pu.head_of_child
        JOIN daycare u ON u.id = pu.unit_id
        JOIN care_area ca ON ca.id = u.care_area_id
        WHERE ${predicate(unitFilter.forTable("u"))}
        ORDER BY ca.name, u.name, co.last_name, co.first_name
            """
                    .trimIndent()
            )
        }
        .toList<FamilyConflictReportRow>()

data class FamilyConflictReportRow(
    val careAreaName: String,
    val unitId: DaycareId,
    val unitName: String,
    val id: PersonId,
    val firstName: String?,
    val lastName: String?,
    val socialSecurityNumber: String?,
    val partnerConflictCount: Int,
    val childConflictCount: Int
)
