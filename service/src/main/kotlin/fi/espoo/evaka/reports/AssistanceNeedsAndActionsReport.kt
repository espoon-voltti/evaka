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
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.json.Json
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
class AssistanceNeedsAndActionsReportController(private val acl: AccessControlList) {
    @GetMapping("/reports/assistance-needs-and-actions")
    fun getAssistanceNeedReport(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): AssistanceNeedsAndActionsReport {
        Audit.AssistanceNeedsReportRead.log()
        user.requireOneOfRoles(UserRole.SERVICE_WORKER, UserRole.ADMIN, UserRole.DIRECTOR, UserRole.REPORT_VIEWER, UserRole.UNIT_SUPERVISOR, UserRole.SPECIAL_EDUCATION_TEACHER)
        return db.read {
            AssistanceNeedsAndActionsReport(
                bases = it.getAssistanceBasisOptions(),
                actions = it.getAssistanceActionOptions(),
                rows = it.getReportRows(date, acl.getAuthorizedUnits(user))
            )
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
SELECT
    ca.name AS care_area_name,
    u.id AS unit_id,
    u.name AS unit_name,
    g.id AS group_id,
    initcap(g.name) AS group_name,
    u.type AS unit_type,
    u.provider_type AS unit_provider_type,
    (
        SELECT jsonb_object_agg(value, count)
        FROM (
            SELECT
                o.value,
                count(an.child_id) AS count
            FROM assistance_basis_option o
            LEFT JOIN daycare_group_placement gpl ON daterange(gpl.start_date, gpl.end_date, '[]') @> :target_date
            AND gpl.daycare_group_id = g.id
            LEFT JOIN placement pl ON pl.id = gpl.daycare_placement_id
            LEFT JOIN assistance_basis_option_ref r ON r.option_id = o.id
            LEFT JOIN assistance_need an ON r.need_id = an.id
            AND an.child_id = pl.child_id
            AND daterange(an.start_date, an.end_date, '[]') @> :target_date
            GROUP BY o.value
        ) basis_counts
    ) AS basis_counts,
    coalesce(basis_stats.none_count, 0) AS no_basis_count,
    (
        SELECT jsonb_object_agg(value, count)
        FROM (
            SELECT
                o.value,
                count(aa.child_id) AS count
            FROM assistance_action_option o
            LEFT JOIN daycare_group_placement gpl ON daterange(gpl.start_date, gpl.end_date, '[]') @> :target_date
            AND gpl.daycare_group_id = g.id
            LEFT JOIN placement pl ON pl.id = gpl.daycare_placement_id
            LEFT JOIN assistance_action_option_ref r ON r.option_id = o.id
            LEFT JOIN assistance_action aa ON r.action_id = aa.id
            AND aa.child_id = pl.child_id
            AND daterange(aa.start_date, aa.end_date, '[]') @> :target_date
            GROUP BY o.value
        ) action_counts
    ) AS action_counts,
    coalesce(action_stats.other_count, 0) AS other_action_count,
    coalesce(action_stats.none_count, 0) AS no_action_count,
    (
        SELECT jsonb_object_agg(value, count)
        FROM (
            SELECT
                value,
                count(aa.child_id) AS count
            FROM unnest(enum_range(NULL::assistance_measure)) AS value
            LEFT JOIN daycare_group_placement gpl ON daterange(gpl.start_date, gpl.end_date, '[]') @> :target_date
            AND gpl.daycare_group_id = g.id
            LEFT JOIN placement pl ON pl.id = gpl.daycare_placement_id
            LEFT JOIN assistance_action aa ON value = ANY(aa.measures)
            AND aa.child_id = pl.child_id
            AND daterange(aa.start_date, aa.end_date, '[]') @> :target_date
            GROUP BY value
        ) measure_counts
    ) AS measure_counts
FROM daycare u
JOIN care_area ca ON u.care_area_id = ca.id
JOIN daycare_group g ON g.daycare_id = u.id AND daterange(g.start_date, g.end_date, '[]') @> :target_date
LEFT JOIN (
    SELECT
        gpl.daycare_group_id AS group_id,
        count(1) FILTER (WHERE NOT EXISTS (SELECT 1 FROM assistance_basis_option_ref WHERE need_id = an.id)) AS none_count
    FROM daycare_group_placement gpl
    JOIN placement pl ON pl.id = gpl.daycare_placement_id
    JOIN assistance_need an ON an.child_id = pl.child_id
    AND daterange(an.start_date, an.end_date, '[]') @> :target_date
    WHERE daterange(gpl.start_date, gpl.end_date, '[]') @> :target_date
    GROUP BY gpl.daycare_group_id
) basis_stats ON g.id = basis_stats.group_id
LEFT JOIN (
    SELECT
        gpl.daycare_group_id AS group_id,
        count(1) FILTER (WHERE aa.other_action != '') AS other_count,
        count(1) FILTER (WHERE aa.other_action = '' AND NOT EXISTS (SELECT 1 FROM assistance_action_option_ref WHERE action_id = aa.id)) AS none_count
    FROM daycare_group_placement gpl
    JOIN placement pl ON pl.id = gpl.daycare_placement_id
    JOIN assistance_action aa ON aa.child_id = pl.child_id
    AND daterange(aa.start_date, aa.end_date, '[]') @> :target_date
    WHERE daterange(gpl.start_date, gpl.end_date, '[]') @> :target_date
    GROUP BY gpl.daycare_group_id
) action_stats ON g.id = action_stats.group_id
${if (authorizedUnits != AclAuthorization.All) "WHERE u.id = ANY(:units)" else ""}
ORDER BY ca.name, u.name, g.name
        """.trimIndent()
    )
        .bind("target_date", date)
        .bind("units", authorizedUnits.ids?.toTypedArray())
        .registerColumnMapper(UnitType::class.java, UnitType.JDBI_COLUMN_MAPPER)
        .mapTo<AssistanceNeedsAndActionsReportController.AssistanceNeedsAndActionsReportRow>()
        .list()
