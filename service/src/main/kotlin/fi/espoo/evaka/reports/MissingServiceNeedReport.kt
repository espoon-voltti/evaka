// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
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
class MissingServiceNeedReportController(private val accessControl: AccessControl) {
    @GetMapping("/reports/missing-service-need")
    fun getMissingServiceNeedReport(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?
    ): List<MissingServiceNeedReportRow> {
        if (to != null && to.isBefore(from)) throw BadRequest("Invalid time range")

        return db.connect { dbc ->
                dbc.read {
                    val filter =
                        accessControl.requireAuthorizationFilter(
                            it,
                            user,
                            clock,
                            Action.Unit.READ_MISSING_SERVICE_NEED_REPORT
                        )
                    it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    it.getMissingServiceNeedRows(from, to, filter)
                }
            }
            .also {
                Audit.MissingServiceNeedReportRead.log(
                    meta = mapOf("from" to from, "to" to to, "count" to it.size)
                )
            }
    }
}

private fun Database.Read.getMissingServiceNeedRows(
    from: LocalDate,
    to: LocalDate?,
    unitFilter: AccessControlFilter<DaycareId>
): List<MissingServiceNeedReportRow> =
    createQuery {
            val dateRange = DateRange(from, to)
            sql(
                """
        SELECT 
            (CASE
                WHEN daycare.provider_type = 'PRIVATE_SERVICE_VOUCHER' THEN 'palvelusetelialue'
                ELSE ca.name
            END) AS care_area_name,
            daycare.name AS unit_name, unit_id,
            child_id, first_name, last_name, sum(days_without_sn) AS days_without_service_need
        FROM (
          SELECT DISTINCT
            child_id,
            unit_id,
            days - days_with_sn AS days_without_sn
          FROM (
            SELECT
              pl.child_id,
              pl.unit_id,
              days_in_range(pl.period) AS days,
              coalesce(sum(days_in_range(pl.period * sn.period)) OVER w, 0) AS days_with_sn
            FROM (
              SELECT id, child_id, unit_id, daterange(start_date, end_date, '[]') * ${bind(dateRange)} AS period
              FROM placement
              WHERE placement.type IN (
                SELECT DISTINCT sno.valid_placement_type 
                FROM service_need_option sno 
                WHERE sno.default_option = FALSE
              )
            ) AS pl
            LEFT JOIN (
              SELECT placement_id, daterange(start_date, end_date, '[]') * ${bind(dateRange)} AS period
              FROM service_need
            ) AS sn
            ON pl.id = sn.placement_id AND pl.period && sn.period
            WINDOW w AS (PARTITION BY (pl.id))
          ) AS stats
          WHERE days - days_with_sn > 0
        ) results
        JOIN person ON person.id = child_id
        JOIN daycare ON daycare.id = unit_id 
            AND (daycare.invoiced_by_municipality OR daycare.provider_type = 'PRIVATE_SERVICE_VOUCHER')
        JOIN care_area ca ON ca.id = daycare.care_area_id
        WHERE ${predicate(unitFilter.forTable("daycare"))}
        GROUP BY 1, daycare.name, unit_id, child_id, first_name, last_name, unit_id
        ORDER BY 1, daycare.name, last_name, first_name
        """
                    .trimIndent()
            )
        }
        .toList<MissingServiceNeedReportRow>()

data class MissingServiceNeedReportRow(
    val careAreaName: String,
    val unitId: DaycareId,
    val unitName: String,
    val childId: ChildId,
    val firstName: String?,
    val lastName: String?,
    val daysWithoutServiceNeed: Int
)
