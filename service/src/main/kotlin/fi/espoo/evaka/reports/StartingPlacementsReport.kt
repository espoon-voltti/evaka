// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.getUUID
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.jdbi.v3.core.statement.StatementContext
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.sql.ResultSet
import java.time.LocalDate
import java.time.Month
import java.util.UUID

@RestController
class StartingPlacementsReportController(private val accessControl: AccessControl) {
    @GetMapping("/reports/starting-placements")
    fun getStartingPlacementsReport(
        db: Database,
        user: AuthenticatedUser,
        @RequestParam("year") year: Int,
        @RequestParam("month") month: Int
    ): List<StartingPlacementsRow> {
        Audit.StartingPlacementsReportRead.log()
        accessControl.requirePermissionFor(user, Action.Global.READ_STARTING_PLACEMENTS_REPORT)
        return db.connect { dbc -> dbc.read { it.getStartingPlacementsRows(year, month) } }
    }
}

data class StartingPlacementsRow(
    val childId: UUID,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate,
    val ssn: String?,
    val placementStart: LocalDate,
    val careAreaName: String
)

/*
 * The preceding placement logic has to be checked if club placements are to be added to the placements table
 */
private fun Database.Read.getStartingPlacementsRows(year: Int, month: Int): List<StartingPlacementsRow> {
    //language=SQL
    val sql =
        """
        SELECT p.child_id, p.start_date, c.first_name, c.last_name, c.date_of_birth, c.social_security_number, ca.name as care_area_name
        FROM placement p
        JOIN person c ON p.child_id = c.id
        JOIN daycare u ON p.unit_id = u.id
        JOIN care_area ca ON u.care_area_id = ca.id
        LEFT JOIN placement preceding ON p.child_id = preceding.child_id AND (p.start_date - interval '1 days') = preceding.end_date AND preceding.type != 'CLUB'::placement_type
        WHERE between_start_and_end(:range, p.start_date) AND preceding.id IS NULL AND p.type != 'CLUB'::placement_type
        """.trimIndent()

    return createQuery(sql)
        .bind("range", FiniteDateRange.ofMonth(year, Month.of(month)))
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
        placementStart = rs.getDate("start_date").toLocalDate(),
        careAreaName = rs.getString("care_area_name")
    )
}
