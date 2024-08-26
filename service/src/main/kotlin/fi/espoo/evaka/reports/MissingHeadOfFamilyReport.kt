// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.data.DateSet
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class MissingHeadOfFamilyReportController(private val accessControl: AccessControl) {
    @GetMapping(
        "/reports/missing-head-of-family", // deprecated
        "/employee/reports/missing-head-of-family",
    )
    fun getMissingHeadOfFamilyReport(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?,
        @RequestParam showIntentionalDuplicates: Boolean = false,
    ): List<MissingHeadOfFamilyReportRow> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.READ_MISSING_HEAD_OF_FAMILY_REPORT,
                    )
                    it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    it.getMissingHeadOfFamilyRows(
                        from = from,
                        to = to,
                        includeIntentionalDuplicates = showIntentionalDuplicates,
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
    includeIntentionalDuplicates: Boolean,
): List<MissingHeadOfFamilyReportRow> =
    createQuery {
            val dateRange = DateRange(from, to)
            val duplicateFilter: Predicate =
                if (includeIntentionalDuplicates) Predicate.alwaysTrue()
                else Predicate { where("$it.duplicate_of IS NULL") }
            sql(
                """
SELECT child_id, first_name, last_name, without_head
FROM (
    SELECT
        p.id AS child_id,
        p.first_name,
        p.last_name,
        -- all placement days
        (
            SELECT range_agg(daterange(start_date, end_date, '[]') * ${bind(dateRange)})
            FROM placement
            WHERE
                child_id = p.id AND
                daterange(start_date, end_date, '[]') && ${bind(dateRange)} AND
                type <> 'CLUB'
        )
        -- remove days with head of family
        - coalesce((
            SELECT range_agg(daterange(start_date, end_date, '[]'))
            FROM fridge_child
            WHERE child_id = p.id AND conflict = false
        ), '{}')
        -- remove days with foster parent
        - coalesce((
            SELECT range_agg(valid_during)
            FROM foster_parent
            WHERE child_id = p.id
        ), '{}')
        AS without_head
    FROM person p
    JOIN child c ON c.id = p.id
    WHERE
        EXISTS (
            SELECT 1 FROM placement
            WHERE
                child_id = p.id AND
                daterange(start_date, end_date, '[]') && ${bind(dateRange)} AND
                type <> 'CLUB'
        ) AND
        ${predicate(duplicateFilter.forTable("p"))} AND
        p.date_of_death IS NULL
) s
WHERE NOT isempty(without_head)
ORDER BY last_name, first_name
        """
            )
        }
        .toList {
            MissingHeadOfFamilyReportRow(
                childId = column("child_id"),
                firstName = column("first_name"),
                lastName = column("last_name"),
                rangesWithoutHead = column<DateSet>("without_head").ranges().toList(),
            )
        }

data class MissingHeadOfFamilyReportRow(
    val childId: ChildId,
    val firstName: String,
    val lastName: String,
    val rangesWithoutHead: List<FiniteDateRange>,
)
