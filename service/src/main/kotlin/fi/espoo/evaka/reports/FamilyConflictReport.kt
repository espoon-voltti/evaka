// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AclAuthorization
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.bindNullable
import fi.espoo.evaka.shared.db.getUUID
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class FamilyConflictReportController(private val acl: AccessControlList) {
    @GetMapping("/reports/family-conflicts")
    fun getFamilyConflictsReport(db: Database, user: AuthenticatedUser): ResponseEntity<List<FamilyConflictReportRow>> {
        Audit.FamilyConflictReportRead.log()
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.FINANCE_ADMIN, UserRole.UNIT_SUPERVISOR)
        return db.read { it.getFamilyConflicts(acl.getAuthorizedUnits(user)) }
            .let { ResponseEntity.ok(it) }
    }
}

private fun Database.Read.getFamilyConflicts(authorizedUnits: AclAuthorization): List<FamilyConflictReportRow> {
    // language=sql
    val sql =
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
            co.partner_conflict_count,
            pu.unit_id
        FROM conflicts co
        JOIN primary_units_view pu ON co.id = pu.head_of_child
        JOIN daycare u ON u.id = pu.unit_id
        JOIN care_area ca ON ca.id = u.care_area_id
        WHERE (:units::uuid[] IS NULL OR u.id = ANY(:units))
        ORDER BY ca.name, u.name, co.last_name, co.first_name
        """.trimIndent()
    return createQuery(sql)
        .bindNullable("units", if (authorizedUnits != AclAuthorization.All) authorizedUnits.ids else null)
        .map { rs, _ ->
            FamilyConflictReportRow(
                careAreaName = rs.getString("care_area_name"),
                unitId = DaycareId(rs.getUUID("unit_id")),
                unitName = rs.getString("unit_name"),
                id = rs.getUUID("id"),
                firstName = rs.getString("first_name"),
                lastName = rs.getString("last_name"),
                socialSecurityNumber = rs.getString("social_security_number"),
                partnerConflictCount = rs.getInt("partner_conflict_count"),
                childConflictCount = rs.getInt("child_conflict_count")
            )
        }
        .toList()
}

data class FamilyConflictReportRow(
    val careAreaName: String,
    val unitId: DaycareId,
    val unitName: String,
    val id: UUID,
    val firstName: String?,
    val lastName: String?,
    val socialSecurityNumber: String?,
    val partnerConflictCount: Int,
    val childConflictCount: Int
)
