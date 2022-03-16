// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.assistanceaction.AssistanceActionOption
import fi.espoo.evaka.assistanceaction.AssistanceMeasure
import fi.espoo.evaka.assistanceaction.getAssistanceActionOptions
import fi.espoo.evaka.assistanceneed.AssistanceBasisOption
import fi.espoo.evaka.assistanceneed.getAssistanceBasisOptions
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AclAuthorization
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.json.Json
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
class AssistanceNeedsAndActionsReportController(private val acl: AccessControlList, private val accessControl: AccessControl) {
    @GetMapping("/reports/assistance-needs-and-actions")
    fun getAssistanceNeedReport(
        db: Database,
        user: AuthenticatedUser,
        @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): AssistanceNeedsAndActionsReport {
        Audit.AssistanceNeedsReportRead.log()
        accessControl.requirePermissionFor(user, Action.Global.READ_ASSISTANCE_NEEDS_AND_ACTIONS_REPORT)
        return db.connect { dbc ->
            dbc.read {
                it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                AssistanceNeedsAndActionsReport(
                    bases = it.getAssistanceBasisOptions(),
                    actions = it.getAssistanceActionOptions(),
                    rows = it.getReportRows(date, acl.getAuthorizedUnits(user))
                )
            }
        }
    }

    data class AssistanceNeedsAndActionsReport(
        val bases: List<AssistanceBasisOption>,
        val actions: List<AssistanceActionOption>,
        val rows: List<AssistanceNeedsAndActionsReportRow>
    )

    data class AssistanceNeedsAndActionsReportRow(
        val careAreaName: String,
        val unitId: DaycareId,
        val unitName: String,
        val groupId: GroupId,
        val groupName: String,
        val unitType: UnitType,
        val unitProviderType: ProviderType,
        @Json
        val basisCounts: Map<AssistanceBasisOptionValue, Int>,
        val noBasisCount: Int,
        @Json
        val actionCounts: Map<AssistanceActionOptionValue, Int>,
        val otherActionCount: Int,
        val noActionCount: Int,
        @Json
        val measureCounts: Map<AssistanceMeasure, Int>,
    )
}

private typealias AssistanceBasisOptionValue = String
private typealias AssistanceActionOptionValue = String

private fun Database.Read.getReportRows(date: LocalDate, authorizedUnits: AclAuthorization) =
    createQuery(
        """
WITH basis_counts AS (
    SELECT
        daycare_group_id,
        jsonb_object_agg(value, count) FILTER (WHERE value IS NOT NULL) AS basis_counts,
        sum(count) FILTER (WHERE value IS NULL) AS no_basis_count
    FROM (
        SELECT
            gpl.daycare_group_id,
            o.value,
            count(an.child_id) AS count
        FROM daycare_group_placement gpl
        JOIN placement pl ON pl.id = gpl.daycare_placement_id
        JOIN assistance_need an ON an.child_id = pl.child_id
        LEFT JOIN assistance_basis_option_ref r ON r.need_id = an.id
        LEFT JOIN assistance_basis_option o on r.option_id = o.id
        WHERE daterange(gpl.start_date, gpl.end_date, '[]') @> :targetDate
        AND daterange(pl.start_date, pl.end_date, '[]') @> :targetDate
        AND daterange(an.start_date, an.end_date, '[]') @> :targetDate
        GROUP BY 1, 2
    ) basis_stats
    GROUP BY daycare_group_id
), measure_counts AS (
    SELECT
        daycare_group_id,
        jsonb_object_agg(value, count) AS measure_counts
    FROM (
        SELECT
            gpl.daycare_group_id,
            value,
            count(aa.child_id) AS count
        FROM daycare_group_placement gpl
        JOIN placement pl ON pl.id = gpl.daycare_placement_id
        JOIN assistance_action aa ON aa.child_id = pl.child_id
        JOIN LATERAL (SELECT unnest(aa.measures) AS value) measures ON true
        WHERE daterange(gpl.start_date, gpl.end_date, '[]') @> :targetDate
        AND daterange(pl.start_date, pl.end_date, '[]') @> :targetDate
        AND daterange(aa.start_date, aa.end_date, '[]') @> :targetDate
        GROUP BY 1, 2
    ) measure_stats
    GROUP BY daycare_group_id
), action_counts AS (
    SELECT
        daycare_group_id,
        jsonb_object_agg(value, count) FILTER (WHERE value IS NOT NULL) AS action_counts,
        sum(count) FILTER (WHERE has_other_action IS TRUE) AS other_action_count,
        sum(count) FILTER (WHERE has_no_action IS TRUE) AS no_action_count
    FROM (
        SELECT
            gpl.daycare_group_id,
            aa.other_action != '' AS has_other_action,
            value IS NULL AND aa.other_action = '' AS has_no_action,
            value,
            count(DISTINCT aa.child_id) AS count
        FROM daycare_group_placement gpl
        JOIN placement pl ON pl.id = gpl.daycare_placement_id
        JOIN assistance_action aa ON aa.child_id = pl.child_id
        LEFT JOIN assistance_action_option_ref r ON r.action_id = aa.id
        LEFT JOIN assistance_action_option o on r.option_id = o.id
        WHERE daterange(gpl.start_date, gpl.end_date, '[]') @> :targetDate
        AND daterange(pl.start_date, pl.end_date, '[]') @> :targetDate
        AND daterange(aa.start_date, aa.end_date, '[]') @> :targetDate
        GROUP BY GROUPING SETS ((1, 2), (1, 3), (1, 4))
    ) action_stats
    GROUP BY daycare_group_id
)
SELECT
    ca.name AS care_area_name,
    u.id AS unit_id,
    u.name AS unit_name,
    g.id AS group_id,
    initcap(g.name) AS group_name,
    u.type AS unit_type,
    u.provider_type AS unit_provider_type,
    coalesce(basis_counts, '{}') AS basis_counts,
    coalesce(no_basis_count, 0) AS no_basis_count,
    coalesce(action_counts, '{}') AS action_counts,
    coalesce(other_action_count, 0) AS other_action_count,
    coalesce(no_action_count, 0) AS no_action_count,
    coalesce(measure_counts, '{}') AS measure_counts
FROM daycare u
JOIN care_area ca ON u.care_area_id = ca.id
JOIN daycare_group g ON g.daycare_id = u.id AND daterange(g.start_date, g.end_date, '[]') @> :targetDate
LEFT JOIN basis_counts ON g.id = basis_counts.daycare_group_id
LEFT JOIN measure_counts ON g.id = measure_counts.daycare_group_id
LEFT JOIN action_counts ON g.id = action_counts.daycare_group_id
${if (authorizedUnits != AclAuthorization.All) "WHERE u.id = ANY(:units)" else ""}
ORDER BY ca.name, u.name, g.name
        """.trimIndent()
    )
        .bind("targetDate", date)
        .bind("units", authorizedUnits.ids?.toTypedArray())
        .registerColumnMapper(UnitType::class.java, UnitType.JDBI_COLUMN_MAPPER)
        .mapTo<AssistanceNeedsAndActionsReportController.AssistanceNeedsAndActionsReportRow>()
        .list()
