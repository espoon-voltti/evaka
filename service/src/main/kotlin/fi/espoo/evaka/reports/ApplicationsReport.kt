// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AclAuthorization
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.bindNullable
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
class ApplicationsReportController(private val accessControl: AccessControl, private val acl: AccessControlList) {
    @GetMapping("/reports/applications")
    fun getApplicationsReport(
        db: Database,
        user: AuthenticatedUser,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate
    ): List<ApplicationsReportRow> {
        Audit.ApplicationsReportRead.log()
        accessControl.requirePermissionFor(user, Action.Global.READ_APPLICATIONS_REPORT)
        if (to.isBefore(from)) throw BadRequest("Inverted time range")

        return db.connect { dbc ->
            dbc.read {
                it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                it.getApplicationsRows(from, to, acl.getAuthorizedUnits(user))
            }
        }
    }
}

private fun Database.Read.getApplicationsRows(
    from: LocalDate,
    to: LocalDate,
    aclAuth: AclAuthorization
): List<ApplicationsReportRow> {
    // language=sql
    val sql =
        """
        WITH data AS (
            SELECT
                ca.name AS care_area_name,
                u.id AS unit_id,
                u.name AS unit_name,
                u.provider_type AS unit_provider_type,
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
            WHERE (:unitIds::uuid[] IS NULL OR u.id = ANY(:unitIds))
        )
        SELECT
            care_area_name,
            unit_id,
            unit_name,
            unit_provider_type,
            count(DISTINCT child_id) FILTER ( WHERE age < 3 AND application_type = 'DAYCARE' ) AS under_3_years,
            count(DISTINCT child_id) FILTER ( WHERE age >= 3 AND application_type = 'DAYCARE' ) AS over_3_years,
            count(DISTINCT child_id) FILTER ( WHERE application_type = 'PRESCHOOL' ) AS preschool,
            count(DISTINCT child_id) FILTER ( WHERE application_type = 'CLUB' ) AS club,
            count(DISTINCT child_id) AS total
        FROM data
        GROUP BY care_area_name, unit_id, unit_name, unit_provider_type
        ORDER BY care_area_name, unit_name;
        """.trimIndent()
    return createQuery(sql)
        .bind("from", from)
        .bind("to", to)
        .bindNullable("unitIds", aclAuth.ids?.toTypedArray())
        .mapTo<ApplicationsReportRow>()
        .toList()
}

data class ApplicationsReportRow(
    val careAreaName: String,
    val unitId: DaycareId,
    val unitName: String,
    val unitProviderType: ProviderType,
    val under3Years: Int,
    val over3Years: Int,
    val preschool: Int,
    val club: Int,
    val total: Int
)
