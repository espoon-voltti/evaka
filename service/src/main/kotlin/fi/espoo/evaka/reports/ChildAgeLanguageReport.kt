// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import fi.espoo.evaka.shared.security.actionrule.forTable
import java.time.LocalDate
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class ChildAgeLanguageReportController(
    private val accessControl: AccessControl
) {
    @GetMapping(
        "/reports/child-age-language", // deprecated
        "/employee/reports/child-age-language"
    )
    fun getChildAgeLanguageReport(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): List<ChildAgeLanguageReportRow> =
        db
            .connect { dbc ->
                dbc.read {
                    val filter =
                        accessControl.requireAuthorizationFilter(
                            it,
                            user,
                            clock,
                            Action.Unit.READ_CHILD_AGE_AND_LANGUAGE_REPORT
                        )
                    it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    it.getChildAgeLanguageRows(date, filter)
                }
            }.also {
                Audit.ChildAgeLanguageReportRead.log(
                    meta = mapOf("date" to date, "count" to it.size)
                )
            }
}

private fun Database.Read.getChildAgeLanguageRows(
    date: LocalDate,
    unitFilter: AccessControlFilter<DaycareId>
): List<ChildAgeLanguageReportRow> =
    createQuery {
        sql(
            """
            WITH children AS (
                SELECT id, extract(year from age(${bind(date)}, date_of_birth)) age, language
                FROM person
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
            LEFT JOIN placement pl ON pl.unit_id = u.id AND daterange(pl.start_date, pl.end_date, '[]') @> ${bind(date)}
            LEFT JOIN children ch ON ch.id = pl.child_id
            WHERE ${predicate(unitFilter.forTable("u"))}
            GROUP BY ca.name, u.id, u.name, u.type, u.provider_type
            ORDER BY ca.name, u.name;
            """.trimIndent()
        )
    }.registerColumnMapper(UnitType.JDBI_COLUMN_MAPPER)
        .toList<ChildAgeLanguageReportRow>()

data class ChildAgeLanguageReportRow(
    val careAreaName: String,
    val unitId: DaycareId,
    val unitName: String,
    val unitType: UnitType,
    val unitProviderType: ProviderType,
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
