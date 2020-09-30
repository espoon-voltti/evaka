// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.config.Roles
import fi.espoo.evaka.shared.db.getUUID
import fi.espoo.evaka.shared.db.transaction
import fi.espoo.evaka.shared.domain.ClosedPeriod
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.statement.StatementContext
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.sql.ResultSet
import java.time.LocalDate
import java.util.UUID

@RestController
class StartingPlacementsReportController(private val jdbi: Jdbi) {
    @GetMapping("/reports/starting-placements")
    fun getStartingPlacementsReport(
        user: AuthenticatedUser,
        @RequestParam("year") year: Int,
        @RequestParam("month") month: Int
    ): ResponseEntity<List<StartingPlacementsRow>> {
        Audit.StartingPlacementsReportRead.log()
        user.requireOneOfRoles(Roles.ADMIN, Roles.SERVICE_WORKER, Roles.FINANCE_ADMIN, Roles.DIRECTOR)
        val rows = jdbi.transaction { h -> getStartingPlacementsRows(h, year, month) }
        return ResponseEntity.ok(rows)
    }
}

data class StartingPlacementsRow(
    val childId: UUID,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate,
    val ssn: String?,
    val placementStart: LocalDate
)

/*
 * The preceding placement logic has to be checked if club placements are to be added to the placements table
 */
fun getStartingPlacementsRows(h: Handle, year: Int, month: Int): List<StartingPlacementsRow> {
    val beginningOfMonth = LocalDate.of(year, month, 1)
    val period = ClosedPeriod(
        beginningOfMonth,
        beginningOfMonth.plusMonths(1).minusDays(1)
    )

    //language=SQL
    val sql =
        """
        SELECT p.child_id, p.start_date, c.first_name, c.last_name, c.date_of_birth, c.social_security_number
        FROM placement p
        JOIN person c ON p.child_id = c.id
        LEFT JOIN placement preceding ON p.child_id = preceding.child_id AND (p.start_date - interval '1 days') = preceding.end_date AND preceding.type != 'CLUB'::placement_type
        WHERE daterange(:from, :to, '[]') @> p.start_date AND preceding.id IS NULL AND p.type != 'CLUB'::placement_type
        """.trimIndent()

    return h.createQuery(sql)
        .bind("from", period.start)
        .bind("to", period.end)
        .map(toRow)
        .toList()
}

private val toRow = { rs: ResultSet, _: StatementContext ->
    StartingPlacementsRow(
        childId = rs.getUUID("child_id"),
        firstName = rs.getString("first_name"),
        lastName = rs.getString("last_name"),
        dateOfBirth = rs.getDate("date_of_birth").toLocalDate(),
        ssn = rs.getString("social_security_number"),
        placementStart = rs.getDate("start_date").toLocalDate()
    )
}
