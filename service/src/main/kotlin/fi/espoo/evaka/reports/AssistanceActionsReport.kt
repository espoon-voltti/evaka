// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.controllers.utils.ok
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.config.Roles.ADMIN
import fi.espoo.evaka.shared.config.Roles.DIRECTOR
import fi.espoo.evaka.shared.config.Roles.SERVICE_WORKER
import fi.espoo.evaka.shared.db.getUUID
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
class AssistanceActionsReportController(private val jdbc: NamedParameterJdbcTemplate) {
    @GetMapping("/reports/assistance-actions")
    fun getAssistanceActionReport(
        user: AuthenticatedUser,
        @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): ResponseEntity<List<AssistanceActionReportRow>> {
        Audit.AssistanceActionsReportRead.log()
        user.requireOneOfRoles(SERVICE_WORKER, ADMIN, DIRECTOR)
        return getAssistanceActionsReportRows(jdbc, date).let(::ok)
    }
}

fun getAssistanceActionsReportRows(jdbc: NamedParameterJdbcTemplate, date: LocalDate): List<AssistanceActionReportRow> {
    // language=sql
    val sql =
        """
        SELECT
            ca.name AS care_area_name,
            u.id AS unit_id,
            u.name as unit_name,
            g.id as group_id,
            initcap(g.name) as group_name,
            u.type as unit_type,
            u.provider_type as unit_provider_type,
            count(DISTINCT pl.child_id) FILTER (WHERE 'ASSISTANCE_SERVICE_CHILD'::assistance_action_type = ANY(aa.actions)) AS assistance_service_child,
            count(DISTINCT pl.child_id) FILTER (WHERE 'ASSISTANCE_SERVICE_UNIT'::assistance_action_type = ANY(aa.actions)) AS assistance_service_unit,
            count(DISTINCT pl.child_id) FILTER (WHERE 'SMALLER_GROUP'::assistance_action_type = ANY(aa.actions)) AS smaller_group,
            count(DISTINCT pl.child_id) FILTER (WHERE 'SPECIAL_GROUP'::assistance_action_type = ANY(aa.actions)) AS special_group,
            count(DISTINCT pl.child_id) FILTER (WHERE 'PERVASIVE_VEO_SUPPORT'::assistance_action_type = ANY(aa.actions)) AS pervasive_veo_support,
            count(DISTINCT pl.child_id) FILTER (WHERE 'RESOURCE_PERSON'::assistance_action_type = ANY(aa.actions)) AS resource_person,
            count(DISTINCT pl.child_id) FILTER (WHERE 'RATIO_DECREASE'::assistance_action_type = ANY(aa.actions)) AS ratio_decrease,
            count(DISTINCT pl.child_id) FILTER (WHERE 'PERIODICAL_VEO_SUPPORT'::assistance_action_type = ANY(aa.actions)) AS periodical_veo_support,
            count(DISTINCT pl.child_id) FILTER (WHERE 'OTHER'::assistance_action_type = ANY(aa.actions)) AS other,
            count(DISTINCT pl.child_id) FILTER (WHERE cardinality(aa.actions) = 0) AS none
        FROM daycare u
        JOIN care_area ca on u.care_area_id = ca.id
        JOIN daycare_group g ON g.daycare_id = u.id AND daterange(g.start_date, g.end_date, '[]') @> :target_date
        LEFT JOIN daycare_group_placement gpl ON gpl.daycare_group_id = g.id AND daterange(gpl.start_date, gpl.end_date, '[]') @> :target_date
        LEFT JOIN placement pl ON pl.id = gpl.daycare_placement_id
        LEFT JOIN assistance_action aa on aa.child_id = pl.child_id AND daterange(aa.start_date, aa.end_date, '[]') @> :target_date
        GROUP BY ca.name, u.id, u.name, g.id, g.name, u.type, u.provider_type
        ORDER BY ca.name, u.name, g.name;
        """.trimIndent()

    @Suppress("UNCHECKED_CAST")
    return jdbc.query(
        sql,
        mapOf("target_date" to date)
    ) { rs, _ ->
        AssistanceActionReportRow(
            careAreaName = rs.getString("care_area_name"),
            unitId = rs.getUUID("unit_id"),
            unitName = rs.getString("unit_name"),
            groupId = rs.getUUID("group_id"),
            groupName = rs.getString("group_name"),
            unitType = (rs.getArray("unit_type").array as? Array<String>)?.toSet()?.let(::getPrimaryUnitType),
            unitProviderType = rs.getString("unit_provider_type"),
            assistanceServiceChild = rs.getInt("assistance_service_child"),
            assistanceServiceUnit = rs.getInt("assistance_service_unit"),
            smallerGroup = rs.getInt("smaller_group"),
            specialGroup = rs.getInt("special_group"),
            pervasiveVeoSupport = rs.getInt("pervasive_veo_support"),
            resourcePerson = rs.getInt("resource_person"),
            ratioDecrease = rs.getInt("ratio_decrease"),
            periodicalVeoSupport = rs.getInt("periodical_veo_support"),
            other = rs.getInt("other"),
            none = rs.getInt("none")
        )
    }
}

data class AssistanceActionReportRow(
    val careAreaName: String,
    val unitId: UUID,
    val unitName: String,
    val groupId: UUID,
    val groupName: String,
    val unitType: UnitType?,
    val unitProviderType: String,
    val assistanceServiceChild: Int,
    val assistanceServiceUnit: Int,
    val smallerGroup: Int,
    val specialGroup: Int,
    val pervasiveVeoSupport: Int,
    val resourcePerson: Int,
    val ratioDecrease: Int,
    val periodicalVeoSupport: Int,
    val other: Int,
    val none: Int
)
