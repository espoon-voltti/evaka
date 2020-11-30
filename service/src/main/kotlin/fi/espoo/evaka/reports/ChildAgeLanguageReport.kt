// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.controllers.utils.ok
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AclAuthorization
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.getUUID
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
class ChildAgeLanguageReportController(private val acl: AccessControlList) {
    @GetMapping("/reports/child-age-language")
    fun getChildAgeLanguageReport(
        db: Database,
        user: AuthenticatedUser,
        @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): ResponseEntity<List<ChildAgeLanguageReportRow>> {
        Audit.ChildAgeLanguageReportRead.log()
        user.requireOneOfRoles(UserRole.SERVICE_WORKER, UserRole.FINANCE_ADMIN, UserRole.ADMIN, UserRole.DIRECTOR, UserRole.SPECIAL_EDUCATION_TEACHER)
        return db.read { it.getChildAgeLanguageRows(date, acl.getAuthorizedDaycares(user)) }.let(::ok)
    }
}

private fun Database.Read.getChildAgeLanguageRows(date: LocalDate, authorizedDaycares: AclAuthorization): List<ChildAgeLanguageReportRow> {
    val daycareFilter: String = if (authorizedDaycares.ids != null) "WHERE u.id = ANY(:authorizedDaycareIds)" else ""

    // language=sql
    val sql =
        """
        WITH children AS (
            SELECT id, extract(year from age(date_of_birth)) age, language
            FROM person
            WHERE date_of_birth IS NOT NULL
        )
        SELECT
            ca.name AS care_area_name,
            u.id AS unit_id,
            u.name as unit_name,
            u.type as unit_type,
            u.provider_type as unit_provider_type,
        
            count(DISTINCT ch.id) FILTER (WHERE ch.age = 0 AND ch.language IN ('fi', 'se')) as fi_0y,
            count(DISTINCT ch.id) FILTER (WHERE ch.age = 1 AND ch.language IN ('fi', 'se')) as fi_1y,
            count(DISTINCT ch.id) FILTER (WHERE ch.age = 2 AND ch.language IN ('fi', 'se')) as fi_2y,
            count(DISTINCT ch.id) FILTER (WHERE ch.age = 3 AND ch.language IN ('fi', 'se')) as fi_3y,
            count(DISTINCT ch.id) FILTER (WHERE ch.age = 4 AND ch.language IN ('fi', 'se')) as fi_4y,
            count(DISTINCT ch.id) FILTER (WHERE ch.age = 5 AND ch.language IN ('fi', 'se')) as fi_5y,
            count(DISTINCT ch.id) FILTER (WHERE ch.age = 6 AND ch.language IN ('fi', 'se')) as fi_6y,
            count(DISTINCT ch.id) FILTER (WHERE ch.age = 7 AND ch.language IN ('fi', 'se')) as fi_7y,
        
            count(DISTINCT ch.id) FILTER (WHERE ch.age = 0 AND ch.language = 'sv') as sv_0y,
            count(DISTINCT ch.id) FILTER (WHERE ch.age = 1 AND ch.language = 'sv') as sv_1y,
            count(DISTINCT ch.id) FILTER (WHERE ch.age = 2 AND ch.language = 'sv') as sv_2y,
            count(DISTINCT ch.id) FILTER (WHERE ch.age = 3 AND ch.language = 'sv') as sv_3y,
            count(DISTINCT ch.id) FILTER (WHERE ch.age = 4 AND ch.language = 'sv') as sv_4y,
            count(DISTINCT ch.id) FILTER (WHERE ch.age = 5 AND ch.language = 'sv') as sv_5y,
            count(DISTINCT ch.id) FILTER (WHERE ch.age = 6 AND ch.language = 'sv') as sv_6y,
            count(DISTINCT ch.id) FILTER (WHERE ch.age = 7 AND ch.language = 'sv') as sv_7y,
        
            count(DISTINCT ch.id) FILTER (WHERE ch.age = 0 AND ch.language NOT IN ('fi', 'se', 'sv')) as other_0y,
            count(DISTINCT ch.id) FILTER (WHERE ch.age = 1 AND ch.language NOT IN ('fi', 'se', 'sv')) as other_1y,
            count(DISTINCT ch.id) FILTER (WHERE ch.age = 2 AND ch.language NOT IN ('fi', 'se', 'sv')) as other_2y,
            count(DISTINCT ch.id) FILTER (WHERE ch.age = 3 AND ch.language NOT IN ('fi', 'se', 'sv')) as other_3y,
            count(DISTINCT ch.id) FILTER (WHERE ch.age = 4 AND ch.language NOT IN ('fi', 'se', 'sv')) as other_4y,
            count(DISTINCT ch.id) FILTER (WHERE ch.age = 5 AND ch.language NOT IN ('fi', 'se', 'sv')) as other_5y,
            count(DISTINCT ch.id) FILTER (WHERE ch.age = 6 AND ch.language NOT IN ('fi', 'se', 'sv')) as other_6y,
            count(DISTINCT ch.id) FILTER (WHERE ch.age = 7 AND ch.language NOT IN ('fi', 'se', 'sv')) as other_7y
        
        FROM daycare u
        JOIN care_area ca ON u.care_area_id = ca.id
        LEFT JOIN placement pl ON pl.unit_id = u.id AND daterange(pl.start_date, pl.end_date, '[]') @> :target_date
        LEFT JOIN children ch ON ch.id = pl.child_id
        $daycareFilter
        GROUP BY ca.name, u.id, u.name, u.type, u.provider_type
        ORDER BY ca.name, u.name;
        """.trimIndent()

    @Suppress("UNCHECKED_CAST")
    return createQuery(sql)
        .bind("target_date", date)
        .bind("authorizedDaycareIds", (authorizedDaycares.ids ?: setOf()).toTypedArray())
        .map { rs, _ ->
            ChildAgeLanguageReportRow(
                careAreaName = rs.getString("care_area_name"),
                unitId = rs.getUUID("unit_id"),
                unitName = rs.getString("unit_name"),
                unitType = (rs.getArray("unit_type").array as Array<out Any>).map { it.toString() }.toSet().let(::getPrimaryUnitType),
                unitProviderType = rs.getString("unit_provider_type"),

                fi_0y = rs.getInt("fi_0y"),
                fi_1y = rs.getInt("fi_1y"),
                fi_2y = rs.getInt("fi_2y"),
                fi_3y = rs.getInt("fi_3y"),
                fi_4y = rs.getInt("fi_4y"),
                fi_5y = rs.getInt("fi_5y"),
                fi_6y = rs.getInt("fi_6y"),
                fi_7y = rs.getInt("fi_7y"),

                sv_0y = rs.getInt("sv_0y"),
                sv_1y = rs.getInt("sv_1y"),
                sv_2y = rs.getInt("sv_2y"),
                sv_3y = rs.getInt("sv_3y"),
                sv_4y = rs.getInt("sv_4y"),
                sv_5y = rs.getInt("sv_5y"),
                sv_6y = rs.getInt("sv_6y"),
                sv_7y = rs.getInt("sv_7y"),

                other_0y = rs.getInt("other_0y"),
                other_1y = rs.getInt("other_1y"),
                other_2y = rs.getInt("other_2y"),
                other_3y = rs.getInt("other_3y"),
                other_4y = rs.getInt("other_4y"),
                other_5y = rs.getInt("other_5y"),
                other_6y = rs.getInt("other_6y"),
                other_7y = rs.getInt("other_7y")
            )
        }
        .toList()
}

data class ChildAgeLanguageReportRow(
    val careAreaName: String,
    val unitId: UUID,
    val unitName: String,
    val unitType: UnitType?,
    val unitProviderType: String,
    val fi_0y: Int,
    val fi_1y: Int,
    val fi_2y: Int,
    val fi_3y: Int,
    val fi_4y: Int,
    val fi_5y: Int,
    val fi_6y: Int,
    val fi_7y: Int,
    val sv_0y: Int,
    val sv_1y: Int,
    val sv_2y: Int,
    val sv_3y: Int,
    val sv_4y: Int,
    val sv_5y: Int,
    val sv_6y: Int,
    val sv_7y: Int,
    val other_0y: Int,
    val other_1y: Int,
    val other_2y: Int,
    val other_3y: Int,
    val other_4y: Int,
    val other_5y: Int,
    val other_6y: Int,
    val other_7y: Int
)
