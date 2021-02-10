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
import fi.espoo.evaka.shared.domain.BadRequest
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
class ApplicationsReportController {
    @GetMapping("/reports/applications")
    fun getApplicationsReport(
        db: Database,
        user: AuthenticatedUser,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate
    ): ResponseEntity<List<ApplicationsReportRow>> {
        Audit.ApplicationsReportRead.log()
        user.requireOneOfRoles(UserRole.SERVICE_WORKER, UserRole.DIRECTOR, UserRole.ADMIN)
        if (to.isBefore(from)) throw BadRequest("Inverted time range")

        return db.read { it.getApplicationsRows(from, to) }.let(::ok)
    }
}

private fun Database.Read.getApplicationsRows(from: LocalDate, to: LocalDate): List<ApplicationsReportRow> {
    // language=sql
    val sql =
        """
        WITH data AS (
            SELECT
                ca.name AS care_area_name,
                u.id AS unit_id,
                u.name AS unit_name,
                u.provider_type AS provider_type,
                a.type AS application_type,
                ch.id AS child_id,
                date_part('year', age(:to, date_of_birth)) AS age
            FROM care_area ca
            JOIN daycare u ON ca.id = u.care_area_id AND :from <= COALESCE(u.closing_date, 'infinity'::date)
            LEFT JOIN application_view a 
                ON a.preferredunit = u.id 
                AND a.status = ANY ('{SENT,WAITING_PLACEMENT,WAITING_DECISION}'::application_status_type[]) 
                AND a.startdate BETWEEN :from AND :to
                AND a.transferapplication != true
            LEFT JOIN person ch ON ch.id = a.childid
        )
        SELECT
            care_area_name,
            unit_id,
            unit_name,
            provider_type,
            count(DISTINCT child_id) FILTER ( WHERE age < 3 AND application_type = 'DAYCARE' ) AS under_3_years_count,
            count(DISTINCT child_id) FILTER ( WHERE age >= 3 AND application_type = 'DAYCARE' ) AS over_3_years_count,
            count(DISTINCT child_id) FILTER ( WHERE application_type = 'PRESCHOOL' ) AS preschool_count,
            count(DISTINCT child_id) FILTER ( WHERE application_type = 'CLUB' ) AS club_count,
            count(DISTINCT child_id) AS total_count
        FROM data
        GROUP BY care_area_name, unit_id, unit_name, provider_type
        ORDER BY care_area_name, unit_name;
        """.trimIndent()
    return createQuery(sql)
        .bind("from", from)
        .bind("to", to)
        .map { rs, _ ->
            ApplicationsReportRow(
                careAreaName = rs.getString("care_area_name"),
                unitId = rs.getUUID("unit_id"),
                unitName = rs.getString("unit_name"),
                unitProviderType = rs.getString("provider_type"),
                under3Years = rs.getInt("under_3_years_count"),
                over3Years = rs.getInt("over_3_years_count"),
                preschool = rs.getInt("preschool_count"),
                club = rs.getInt("club_count"),
                total = rs.getInt("total_count")
            )
        }
        .toList()
}

data class ApplicationsReportRow(
    val careAreaName: String,
    val unitId: UUID,
    val unitName: String,
    val unitProviderType: String,
    val under3Years: Int,
    val over3Years: Int,
    val preschool: Int,
    val club: Int,
    val total: Int
)
