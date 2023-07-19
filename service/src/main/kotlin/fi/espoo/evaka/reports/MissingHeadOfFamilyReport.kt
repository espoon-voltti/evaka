// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DatabaseTable
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.Timeline
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
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
        @RequestParam("showFosterChildren", required = false, defaultValue = "false")
        showFosterChildren: Boolean,
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
                    it.getMissingHeadOfFamilyRows(
                        from = from,
                        to = to,
                        includeFosterChildren = showFosterChildren,
                        includeIntentionalDuplicates = showIntentionalDuplicates,
                        idFilter = filter
                    )
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
    includeFosterChildren: Boolean,
    includeIntentionalDuplicates: Boolean,
    idFilter: AccessControlFilter<DaycareId>
): List<MissingHeadOfFamilyReportRow> =
    createQuery<DatabaseTable> {
            val dateRange = DateRange(from, to)
            val duplicateFilter: Predicate<DatabaseTable.Person> =
                if (includeIntentionalDuplicates) Predicate.alwaysTrue()
                else Predicate { where("$it.duplicate_of IS NULL") }
            val fosterPredicate: Predicate<DatabaseTable.FosterParent> =
                if (includeFosterChildren) Predicate.alwaysFalse() else Predicate.alwaysTrue()
            sql(
                """
SELECT child_id, first_name, last_name, without_head
FROM (
    SELECT
        p.id AS child_id,
        p.first_name,
        p.last_name,
        -- all placement days
        range_agg(daterange(pl.start_date, pl.end_date, '[]') * ${bind(dateRange)})
        -- remove days with head of family
        - coalesce(range_agg(fc.valid_during), '{}')
        -- remove days with foster parent
        - coalesce(range_agg(fp.valid_during), '{}')
        AS without_head
    FROM person p
    JOIN child c ON c.id = p.id
    JOIN placement pl ON pl.child_id = p.id
    JOIN daycare u ON u.id = pl.unit_id
    LEFT JOIN (
        -- convert start/end to daterange before join to avoid infinite date ranges in the outer query
        SELECT child_id, conflict, daterange(start_date, end_date, '[]') AS valid_during
        FROM fridge_child
        WHERE conflict = false
    ) fc ON fc.child_id = p.id AND fc.valid_during && ${bind(dateRange)}
    LEFT JOIN foster_parent fp ON fp.child_id = p.id AND fp.valid_during && ${bind(dateRange)} AND ${predicate(fosterPredicate.forTable("fp"))}
    WHERE
        ${predicate(idFilter.forTable("u"))} AND
        ${predicate(duplicateFilter.forTable("p"))} AND
        p.date_of_death IS NULL AND
        daterange(pl.start_date, pl.end_date, '[]') && ${bind(dateRange)} AND
        pl.type <> 'CLUB'
    GROUP BY p.id, p.first_name, p.last_name
) s
WHERE NOT isempty(without_head)
ORDER BY last_name, first_name
        """
            )
        }
        .map { row ->
            MissingHeadOfFamilyReportRow(
                childId = row.mapColumn("child_id"),
                firstName = row.mapColumn("first_name"),
                lastName = row.mapColumn("last_name"),
                rangesWithoutHead = row.mapColumn<Timeline>("without_head").ranges().toList(),
            )
        }
        .toList()

data class MissingHeadOfFamilyReportRow(
    val childId: ChildId,
    val firstName: String,
    val lastName: String,
    val rangesWithoutHead: List<FiniteDateRange>
)
