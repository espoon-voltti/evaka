// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.Month

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
        return db.connect { dbc ->
            dbc.read {
                it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                it.getStartingPlacementsRows(year, month)
            }
        }
    }
}

data class StartingPlacementsRow(
    val childId: ChildId,
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
        SELECT p.child_id, p.start_date AS placement_start, c.first_name, c.last_name, c.date_of_birth, c.social_security_number AS ssn, ca.name as care_area_name,
               u.provider_type = 'PRIVATE_SERVICE_VOUCHER' AS is_private_service_voucher_daycare        
        FROM placement p
        JOIN person c ON p.child_id = c.id
        JOIN daycare u ON p.unit_id = u.id
        JOIN care_area ca ON u.care_area_id = ca.id
        LEFT JOIN placement preceding ON p.child_id = preceding.child_id AND (p.start_date - interval '1 days') = preceding.end_date AND preceding.type != 'CLUB'::placement_type
        WHERE between_start_and_end(:range, p.start_date) AND preceding.id IS NULL AND p.type != 'CLUB'::placement_type
        """.trimIndent()

    return createQuery(sql)
        .bind("range", FiniteDateRange.ofMonth(year, Month.of(month)))
        .map { mapper ->
            StartingPlacementsRow(
                careAreaName = if (mapper.mapColumn("is_private_service_voucher_daycare")) "palvelusetelialue" else mapper.mapColumn("care_area_name"),
                childId = ChildId(mapper.mapColumn("child_id")),
                firstName = mapper.mapColumn("first_name"),
                lastName = mapper.mapColumn("last_name"),
                dateOfBirth = mapper.mapColumn("date_of_birth"),
                ssn = mapper.mapColumn("ssn"),
                placementStart = mapper.mapColumn("placement_start"),
            )
        }
        .toList()
}
