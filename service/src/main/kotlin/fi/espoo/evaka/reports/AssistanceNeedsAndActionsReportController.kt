// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.assistance.DaycareAssistanceLevel
import fi.espoo.evaka.assistance.OtherAssistanceMeasureType
import fi.espoo.evaka.assistance.PreschoolAssistanceLevel
import fi.espoo.evaka.assistanceaction.AssistanceActionOption
import fi.espoo.evaka.assistanceaction.getAssistanceActionOptions
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import fi.espoo.evaka.shared.security.actionrule.forTable
import java.math.BigDecimal
import java.time.LocalDate
import org.jdbi.v3.json.Json
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class AssistanceNeedsAndActionsReportController(
    private val accessControl: AccessControl,
    private val featureConfig: FeatureConfig
) {
    @GetMapping("/reports/assistance-needs-and-actions")
    fun getAssistanceNeedsAndActionsReport(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): AssistanceNeedsAndActionsReport {
        return db.connect { dbc ->
                dbc.read {
                    val filter =
                        accessControl.requireAuthorizationFilter(
                            it,
                            user,
                            clock,
                            Action.Unit.READ_ASSISTANCE_NEEDS_AND_ACTIONS_REPORT
                        )
                    it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    AssistanceNeedsAndActionsReport(
                        actions = it.getAssistanceActionOptions(),
                        rows = it.getReportRows(date, filter),
                        showAssistanceNeedVoucherCoefficient =
                            !featureConfig.valueDecisionCapacityFactorEnabled
                    )
                }
            }
            .also {
                Audit.AssistanceNeedsReportRead.log(
                    meta = mapOf("date" to date, "count" to it.rows.size)
                )
            }
    }

    data class AssistanceNeedsAndActionsReport(
        val actions: List<AssistanceActionOption>,
        val rows: List<AssistanceNeedsAndActionsReportRow>,
        val showAssistanceNeedVoucherCoefficient: Boolean
    )

    data class AssistanceNeedsAndActionsReportRow(
        val careAreaName: String,
        val unitId: DaycareId,
        val unitName: String,
        val groupId: GroupId,
        val groupName: String,
        @Json val actionCounts: Map<AssistanceActionOptionValue, Int>,
        val otherActionCount: Int,
        val noActionCount: Int,
        @Json val daycareAssistanceCounts: Map<DaycareAssistanceLevel, Int>,
        @Json val preschoolAssistanceCounts: Map<PreschoolAssistanceLevel, Int>,
        @Json val otherAssistanceMeasureCounts: Map<OtherAssistanceMeasureType, Int>,
        val assistanceNeedVoucherCoefficientCount: Int
    )

    @GetMapping("/reports/assistance-needs-and-actions/by-child")
    fun getAssistanceNeedsAndActionsReportByChild(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): AssistanceNeedsAndActionsReportByChild {
        return db.connect { dbc ->
                dbc.read {
                    val filter =
                        accessControl.requireAuthorizationFilter(
                            it,
                            user,
                            clock,
                            Action.Unit.READ_ASSISTANCE_NEEDS_AND_ACTIONS_REPORT_BY_CHILD
                        )
                    getAssistanceNeedsAndActionsReportByChild(
                        it,
                        date,
                        filter,
                        !featureConfig.valueDecisionCapacityFactorEnabled
                    )
                }
            }
            .also {
                Audit.AssistanceNeedsReportByChildRead.log(
                    meta = mapOf("date" to date, "count" to it.rows.size)
                )
            }
    }

    fun getAssistanceNeedsAndActionsReportByChild(
        tx: Database.Read,
        date: LocalDate,
        filter: AccessControlFilter<DaycareId>,
        showAssistanceNeedVoucherCoefficient: Boolean
    ): AssistanceNeedsAndActionsReportByChild {
        tx.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
        return AssistanceNeedsAndActionsReportByChild(
            actions = tx.getAssistanceActionOptions(),
            rows = tx.getReportRowsByChild(date, filter),
            showAssistanceNeedVoucherCoefficient = showAssistanceNeedVoucherCoefficient
        )
    }

    data class AssistanceNeedsAndActionsReportByChild(
        val actions: List<AssistanceActionOption>,
        val rows: List<AssistanceNeedsAndActionsReportRowByChild>,
        val showAssistanceNeedVoucherCoefficient: Boolean
    )

    data class AssistanceNeedsAndActionsReportRowByChild(
        val careAreaName: String,
        val unitId: DaycareId,
        val unitName: String,
        val groupId: GroupId,
        val groupName: String,
        val childId: PersonId,
        val childLastName: String,
        val childFirstName: String,
        val childAge: Int,
        @Json val actions: Set<AssistanceActionOptionValue>,
        val otherAction: String,
        @Json val daycareAssistanceCounts: Map<DaycareAssistanceLevel, Int>,
        @Json val preschoolAssistanceCounts: Map<PreschoolAssistanceLevel, Int>,
        @Json val otherAssistanceMeasureCounts: Map<OtherAssistanceMeasureType, Int>,
        val assistanceNeedVoucherCoefficient: BigDecimal
    )
}

private typealias AssistanceActionOptionValue = String

private fun Database.Read.getReportRows(
    date: LocalDate,
    unitFilter: AccessControlFilter<DaycareId>
) =
    createQuery {
            sql(
                """
WITH action_counts AS (
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
        WHERE daterange(gpl.start_date, gpl.end_date, '[]') @> ${bind(date)}
        AND daterange(pl.start_date, pl.end_date, '[]') @> ${bind(date)}
        AND daterange(aa.start_date, aa.end_date, '[]') @> ${bind(date)}
        GROUP BY GROUPING SETS ((1, 2), (1, 3), (1, 4))
    ) action_stats
    GROUP BY daycare_group_id
), daycare_assistance_counts AS (
    SELECT
        daycare_group_id,
        jsonb_object_agg(level, count) AS daycare_assistance_counts
    FROM (
        SELECT
            gpl.daycare_group_id,
            level,
            count(da.child_id) AS count
        FROM daycare_group_placement gpl
        JOIN placement pl ON pl.id = gpl.daycare_placement_id
        JOIN daycare_assistance da ON da.child_id = pl.child_id
        WHERE daterange(gpl.start_date, gpl.end_date, '[]') @> ${bind(date)}
        AND daterange(pl.start_date, pl.end_date, '[]') @> ${bind(date)}
        AND da.valid_during @> ${bind(date)}
        GROUP BY 1, 2
    ) daycare_assistance_stats
    GROUP BY daycare_group_id
), preschool_assistance_counts AS (
    SELECT
        daycare_group_id,
        jsonb_object_agg(level, count) AS preschool_assistance_counts
    FROM (
        SELECT
            gpl.daycare_group_id,
            level,
            count(pa.child_id) AS count
        FROM daycare_group_placement gpl
        JOIN placement pl ON pl.id = gpl.daycare_placement_id
        JOIN preschool_assistance pa ON pa.child_id = pl.child_id
        WHERE daterange(gpl.start_date, gpl.end_date, '[]') @> ${bind(date)}
        AND daterange(pl.start_date, pl.end_date, '[]') @> ${bind(date)}
        AND pa.valid_during @> ${bind(date)}
        GROUP BY 1, 2
    ) daycare_assistance_stats
    GROUP BY daycare_group_id
), other_assistance_measure_counts AS (
    SELECT
        daycare_group_id,
        jsonb_object_agg(type, count) AS other_assistance_measure_counts
    FROM (
        SELECT
            gpl.daycare_group_id,
            oam.type,
            count(oam.child_id) AS count
        FROM daycare_group_placement gpl
        JOIN placement pl ON pl.id = gpl.daycare_placement_id
        JOIN other_assistance_measure oam ON oam.child_id = pl.child_id
        WHERE daterange(gpl.start_date, gpl.end_date, '[]') @> ${bind(date)}
        AND daterange(pl.start_date, pl.end_date, '[]') @> ${bind(date)}
        AND oam.valid_during @> ${bind(date)}
        GROUP BY 1, 2
    ) daycare_assistance_stats
    GROUP BY daycare_group_id
), assistance_need_voucher_coefficient_counts AS (
    SELECT
        gpl.daycare_group_id,
        count(vc.child_id) AS count
    FROM daycare_group_placement gpl
    JOIN placement pl ON pl.id = gpl.daycare_placement_id
    JOIN assistance_need_voucher_coefficient vc ON vc.child_id = pl.child_id
    WHERE daterange(gpl.start_date, gpl.end_date, '[]') @> ${bind(date)}
    AND daterange(pl.start_date, pl.end_date, '[]') @> ${bind(date)}
    AND vc.validity_period @> ${bind(date)}
    GROUP BY daycare_group_id
) 
SELECT
    ca.name AS care_area_name,
    u.id AS unit_id,
    u.name AS unit_name,
    g.id AS group_id,
    initcap(g.name) AS group_name,
    coalesce(action_counts, '{}') AS action_counts,
    coalesce(other_action_count, 0) AS other_action_count,
    coalesce(no_action_count, 0) AS no_action_count,
    coalesce(daycare_assistance_counts, '{}') AS daycare_assistance_counts,
    coalesce(preschool_assistance_counts, '{}') AS preschool_assistance_counts,
    coalesce(other_assistance_measure_counts, '{}') AS other_assistance_measure_counts,
    coalesce(assistance_need_voucher_coefficient_counts.count, 0) AS assistance_need_voucher_coefficient_count
FROM daycare u
JOIN care_area ca ON u.care_area_id = ca.id
JOIN daycare_group g ON g.daycare_id = u.id AND daterange(g.start_date, g.end_date, '[]') @> ${bind(date)}
LEFT JOIN action_counts ON g.id = action_counts.daycare_group_id
LEFT JOIN daycare_assistance_counts ON g.id = daycare_assistance_counts.daycare_group_id
LEFT JOIN preschool_assistance_counts ON g.id = preschool_assistance_counts.daycare_group_id
LEFT JOIN other_assistance_measure_counts ON g.id = other_assistance_measure_counts.daycare_group_id
LEFT JOIN assistance_need_voucher_coefficient_counts ON g.id = assistance_need_voucher_coefficient_counts.daycare_group_id
WHERE ${predicate(unitFilter.forTable("u"))}
ORDER BY ca.name, u.name, g.name
        """
                    .trimIndent()
            )
        }
        .toList<AssistanceNeedsAndActionsReportController.AssistanceNeedsAndActionsReportRow>()

private fun Database.Read.getReportRowsByChild(
    date: LocalDate,
    unitFilter: AccessControlFilter<DaycareId>
) =
    createQuery {
            sql(
                """
WITH actions AS (
    SELECT
        gpl.daycare_group_id,
        aa.child_id,
        jsonb_agg(o.value) FILTER (WHERE value IS NOT NULL) AS actions,
        aa.other_action
    FROM daycare_group_placement gpl
    JOIN placement pl ON pl.id = gpl.daycare_placement_id
    JOIN assistance_action aa ON aa.child_id = pl.child_id
    LEFT JOIN assistance_action_option_ref r ON r.action_id = aa.id
    LEFT JOIN assistance_action_option o on r.option_id = o.id
    WHERE daterange(gpl.start_date, gpl.end_date, '[]') @> ${bind(date)}
    AND daterange(pl.start_date, pl.end_date, '[]') @> ${bind(date)}
    AND daterange(aa.start_date, aa.end_date, '[]') @> ${bind(date)}
    GROUP BY gpl.id, aa.child_id, aa.other_action
), daycare_assistance_counts AS (
    SELECT
        daycare_group_id,
        child_id,
        jsonb_object_agg(level, count) AS daycare_assistance_counts
    FROM (
        SELECT
            gpl.daycare_group_id,
            da.child_id,
            level,
            count(da.child_id) AS count
        FROM daycare_group_placement gpl
        JOIN placement pl ON pl.id = gpl.daycare_placement_id
        JOIN daycare_assistance da ON da.child_id = pl.child_id
        WHERE daterange(gpl.start_date, gpl.end_date, '[]') @> ${bind(date)}
        AND daterange(pl.start_date, pl.end_date, '[]') @> ${bind(date)}
        AND da.valid_during @> ${bind(date)}
        GROUP BY 1, 2, 3
    ) daycare_assistance_stats
    GROUP BY daycare_group_id, child_id
), preschool_assistance_counts AS (
    SELECT
        daycare_group_id,
        child_id,
        jsonb_object_agg(level, count) AS preschool_assistance_counts
    FROM (
        SELECT
            gpl.daycare_group_id,
            pa.child_id,
            level,
            count(pa.child_id) AS count
        FROM daycare_group_placement gpl
        JOIN placement pl ON pl.id = gpl.daycare_placement_id
        JOIN preschool_assistance pa ON pa.child_id = pl.child_id
        WHERE daterange(gpl.start_date, gpl.end_date, '[]') @> ${bind(date)}
        AND daterange(pl.start_date, pl.end_date, '[]') @> ${bind(date)}
        AND pa.valid_during @> ${bind(date)}
        GROUP BY 1, 2, 3
    ) daycare_assistance_stats
    GROUP BY daycare_group_id, child_id
), other_assistance_measure_counts AS (
    SELECT
        daycare_group_id,
        child_id,
        jsonb_object_agg(type, count) AS other_assistance_measure_counts
    FROM (
        SELECT
            gpl.daycare_group_id,
            oam.child_id,
            oam.type,
            count(oam.child_id) AS count
        FROM daycare_group_placement gpl
        JOIN placement pl ON pl.id = gpl.daycare_placement_id
        JOIN other_assistance_measure oam ON oam.child_id = pl.child_id
        WHERE daterange(gpl.start_date, gpl.end_date, '[]') @> ${bind(date)}
        AND daterange(pl.start_date, pl.end_date, '[]') @> ${bind(date)}
        AND oam.valid_during @> ${bind(date)}
        GROUP BY 1, 2, 3
    ) daycare_assistance_stats
    GROUP BY daycare_group_id, child_id
), assistance_need_voucher_coefficient AS (
        SELECT
            gpl.daycare_group_id,
            vc.child_id,
            vc.coefficient
        FROM daycare_group_placement gpl
        JOIN placement pl ON pl.id = gpl.daycare_placement_id
        JOIN assistance_need_voucher_coefficient vc ON vc.child_id = pl.child_id
        WHERE daterange(gpl.start_date, gpl.end_date, '[]') @> ${bind(date)}
        AND daterange(pl.start_date, pl.end_date, '[]') @> ${bind(date)}
        AND vc.validity_period @> ${bind(date)}
) 
SELECT
    ca.name AS care_area_name,
    u.id AS unit_id,
    u.name AS unit_name,
    g.id AS group_id,
    initcap(g.name) AS group_name,
    child.id AS child_id,
    child.last_name AS child_last_name,
    child.first_name AS child_first_name,
    extract(year from age(${bind(date)}, child.date_of_birth)) AS child_age,
    coalesce(actions.actions, '[]') AS actions,
    coalesce(actions.other_action, '') AS other_action,
    coalesce(daycare_assistance_counts, '{}') AS daycare_assistance_counts,
    coalesce(preschool_assistance_counts, '{}') AS preschool_assistance_counts,
    coalesce(other_assistance_measure_counts, '{}') AS other_assistance_measure_counts,
    coalesce(assistance_need_voucher_coefficient.coefficient, 1.0) AS assistance_need_voucher_coefficient
FROM daycare u
JOIN care_area ca ON u.care_area_id = ca.id
JOIN daycare_group g ON g.daycare_id = u.id AND daterange(g.start_date, g.end_date, '[]') @> ${bind(date)}
JOIN daycare_group_placement dgp ON dgp.daycare_group_id = g.id AND daterange(dgp.start_date, dgp.end_date, '[]') @> ${bind(date)}
JOIN placement p ON p.id = dgp.daycare_placement_id AND daterange(p.start_date, p.end_date, '[]') @> ${bind(date)}
JOIN person child ON child.id = p.child_id
LEFT JOIN actions ON g.id = actions.daycare_group_id
    AND child.id = actions.child_id
LEFT JOIN daycare_assistance_counts ON g.id = daycare_assistance_counts.daycare_group_id
    AND child.id = daycare_assistance_counts.child_id
LEFT JOIN preschool_assistance_counts ON g.id = preschool_assistance_counts.daycare_group_id
    AND child.id = preschool_assistance_counts.child_id
LEFT JOIN other_assistance_measure_counts ON g.id = other_assistance_measure_counts.daycare_group_id
    AND child.id = other_assistance_measure_counts.child_id
LEFT JOIN assistance_need_voucher_coefficient ON g.id = assistance_need_voucher_coefficient.daycare_group_id 
   AND child.id = assistance_need_voucher_coefficient.child_id
WHERE ${predicate(unitFilter.forTable("u"))}
ORDER BY ca.name, u.name, g.name, child.last_name, child.first_name
        """
                    .trimIndent()
            )
        }
        .toList<
            AssistanceNeedsAndActionsReportController.AssistanceNeedsAndActionsReportRowByChild
        >()
