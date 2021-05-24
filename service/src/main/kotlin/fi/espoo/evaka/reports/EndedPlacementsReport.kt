// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.controllers.utils.ok
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.getUUID
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
class EndedPlacementsReportController {
    @GetMapping("/reports/ended-placements")
    fun getEndedPlacementsReport(
        db: Database,
        user: AuthenticatedUser,
        @RequestParam year: Int,
        @RequestParam month: Int
    ): ResponseEntity<List<EndedPlacementsReportRow>> {
        Audit.EndedPlacementsReportRead.log()
        val from = LocalDate.of(year, month, 1)
        val to = from.plusMonths(1).minusDays(1)
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.FINANCE_ADMIN)

        return db.read { it.getEndedPlacementsRows(from, to) }.let(::ok)
    }
}

private fun Database.Read.getEndedPlacementsRows(from: LocalDate, to: LocalDate): List<EndedPlacementsReportRow> {
    // language=sql
    val sql =
        """
        WITH ended_placements AS (
            SELECT 
                p.id AS child_id, p.first_name, p.last_name, p.social_security_number, 
                max(pl.end_date) AS placement_end
            FROM placement pl
            JOIN person p ON p.id = pl.child_id
            WHERE daterange(:from, :to, '[]') @> pl.end_date AND pl.type != 'CLUB'::placement_type
            GROUP BY p.id, p.first_name, p.last_name, p.social_security_number
        )
        SELECT 
            ep.child_id, ep.first_name, ep.last_name, ep.social_security_number, 
            ep.placement_end, min(next.start_date) AS next_placement_start
        FROM ended_placements ep 
        LEFT JOIN placement next
            ON next.child_id = ep.child_id AND next.start_date > ep.placement_end AND next.type != 'CLUB'::placement_type
        GROUP BY ep.child_id, ep.first_name, ep.last_name, ep.social_security_number, ep.placement_end
        HAVING min(next.start_date) IS NULL OR min(next.start_date) > :to
        ORDER BY last_name, first_name, social_security_number
        """.trimIndent()
    return createQuery(sql)
        .bind("from", from)
        .bind("to", to)
        .map { rs, _ ->
            EndedPlacementsReportRow(
                childId = rs.getUUID("child_id"),
                firstName = rs.getString("first_name"),
                lastName = rs.getString("last_name"),
                ssn = rs.getString("social_security_number"),
                placementEnd = rs.getDate("placement_end").toLocalDate(),
                nextPlacementStart = rs.getDate("next_placement_start")?.toLocalDate()
            )
        }
        .toList()
}

data class EndedPlacementsReportRow(
    val childId: UUID,
    val firstName: String?,
    val lastName: String?,
    val ssn: String?,
    val placementEnd: LocalDate,
    val nextPlacementStart: LocalDate?
)
