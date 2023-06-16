// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DatabaseTable
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
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
class MissingHeadOfFamilyReportController(private val accessControl: AccessControl) {
    @GetMapping("/reports/missing-head-of-family")
    fun getMissingHeadOfFamilyReport(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?,
        @RequestParam("showIntentionalDuplicates", required = false, defaultValue = "false")
        showIntentionalDuplicates: Boolean
    ): List<MissingHeadOfFamilyReportRow> {
        return db.connect { dbc ->
                dbc.read {
                    val filter =
                        accessControl.requireAuthorizationFilter(
                            it,
                            user,
                            clock,
                            Action.Unit.READ_MISSING_HEAD_OF_FAMILY_REPORT
                        )
                    it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    it.getMissingHeadOfFamilyRows(from, to, showIntentionalDuplicates, filter)
                }
            }
            .also {
                Audit.MissingHeadOfFamilyReportRead.log(
                    meta = mapOf("from" to from, "to" to to, "count" to it.size)
                )
            }
    }
}

private fun Database.Read.getMissingHeadOfFamilyRows(
    from: LocalDate,
    to: LocalDate?,
    showIntentionalDuplicates: Boolean,
    idFilter: AccessControlFilter<DaycareId>
): List<MissingHeadOfFamilyReportRow> =
    createQuery<DatabaseTable> {
            val dateRange = DateRange(from, to)
            sql(
                """
        SELECT 
            ca.name AS care_area_name, daycare.name AS unit_name, unit_id,
            child_id, first_name, last_name, sum(days_without_head) AS days_without_head
        FROM (
          SELECT DISTINCT
            child_id,
            unit_id,
            days - days_with_head AS days_without_head
          FROM (
            SELECT
              pl.child_id,
              pl.unit_id,
              days_in_range(pl.period) AS days,
              coalesce(sum(days_in_range(pl.period * sn.period)) OVER w, 0) AS days_with_head
            FROM (
              SELECT id, child_id, unit_id, daterange(start_date, end_date, '[]') * ${bind(dateRange)} AS period
              FROM placement
              WHERE placement.type != 'CLUB'::placement_type
            ) AS pl
            LEFT JOIN (
              SELECT child_id, daterange(start_date, end_date, '[]') * ${bind(dateRange)} AS period
              FROM fridge_child
              WHERE conflict = FALSE
            ) AS sn
            ON pl.child_id = sn.child_id
            AND pl.period && sn.period
            WINDOW w AS (PARTITION BY (pl.id))
          ) AS stats
          WHERE days - days_with_head > 0
        ) results
        JOIN person ON person.id = child_id
        JOIN daycare ON daycare.id = unit_id
        JOIN care_area ca ON ca.id = daycare.care_area_id
        WHERE person.date_of_death IS NULL
        AND ${predicate(idFilter.forTable("daycare"))}
        AND (${bind(showIntentionalDuplicates)} IS TRUE OR duplicate_of IS NULL)
        GROUP BY ca.name, daycare.name, unit_id, child_id, first_name, last_name, unit_id
        ORDER BY ca.name, daycare.name, last_name, first_name
        """
                    .trimIndent()
            )
        }
        .mapTo<MissingHeadOfFamilyReportRow>()
        .toList()

data class MissingHeadOfFamilyReportRow(
    val careAreaName: String,
    val unitId: DaycareId,
    val unitName: String,
    val childId: ChildId,
    val firstName: String?,
    val lastName: String?,
    val daysWithoutHead: Int
)
