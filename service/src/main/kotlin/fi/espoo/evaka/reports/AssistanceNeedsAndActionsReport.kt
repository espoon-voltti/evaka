// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.controllers.utils.ok
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
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

@RestController
class AssistanceNeedsAndActionsReportController(private val acl: AccessControlList) {
    @GetMapping("/reports/assistance-needs-and-actions")
    fun getAssistanceNeedReport(
        db: Database,
        user: AuthenticatedUser,
        @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): ResponseEntity<List<AssistanceNeedsAndActionsReportRow>> {
        Audit.AssistanceNeedsReportRead.log()
        user.requireOneOfRoles(UserRole.SERVICE_WORKER, UserRole.ADMIN, UserRole.DIRECTOR, UserRole.UNIT_SUPERVISOR, UserRole.SPECIAL_EDUCATION_TEACHER)
        return db.read { it.getAssistanceNeedsAndActionsReportRows(date, acl.getAuthorizedUnits(user)).let(::ok) }
    }
}

fun Database.Read.getAssistanceNeedsAndActionsReportRows(date: LocalDate, authorizedUnits: AclAuthorization): List<AssistanceNeedsAndActionsReportRow> {
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
            
            count(DISTINCT pl.child_id) FILTER (WHERE 'AUTISM' = ANY(abo.bases)) AS autism,
            count(DISTINCT pl.child_id) FILTER (WHERE 'DEVELOPMENTAL_DISABILITY_1' = ANY(abo.bases)) AS developmental_disability_1,
            count(DISTINCT pl.child_id) FILTER (WHERE 'DEVELOPMENTAL_DISABILITY_2' = ANY(abo.bases)) AS developmental_disability_2,
            count(DISTINCT pl.child_id) FILTER (WHERE 'FOCUS_CHALLENGE' = ANY(abo.bases)) AS focus_challenge,
            count(DISTINCT pl.child_id) FILTER (WHERE 'LINGUISTIC_CHALLENGE' = ANY(abo.bases)) AS linguistic_challenge,
            count(DISTINCT pl.child_id) FILTER (WHERE 'DEVELOPMENT_MONITORING' = ANY(abo.bases)) AS development_monitoring,
            count(DISTINCT pl.child_id) FILTER (WHERE 'DEVELOPMENT_MONITORING_PENDING' = ANY(abo.bases)) AS development_monitoring_pending,
            count(DISTINCT pl.child_id) FILTER (WHERE 'MULTI_DISABILITY' = ANY(abo.bases)) AS multi_disability,
            count(DISTINCT pl.child_id) FILTER (WHERE 'LONG_TERM_CONDITION' = ANY(abo.bases)) AS long_term_condition,
            count(DISTINCT pl.child_id) FILTER (WHERE 'REGULATION_SKILL_CHALLENGE' = ANY(abo.bases)) AS regulation_skill_challenge,
            count(DISTINCT pl.child_id) FILTER (WHERE 'DISABILITY' = ANY(abo.bases)) AS disability,
            count(DISTINCT pl.child_id) FILTER (WHERE 'OTHER' = ANY(abo.bases)) AS other_assistance_need,
            count(DISTINCT pl.child_id) FILTER (WHERE cardinality(abo.bases) = 0) AS no_assistance_needs,
            
            count(DISTINCT pl.child_id) FILTER (WHERE 'ASSISTANCE_SERVICE_CHILD' = ANY(aao.actions)) AS assistance_service_child,
            count(DISTINCT pl.child_id) FILTER (WHERE 'ASSISTANCE_SERVICE_UNIT' = ANY(aao.actions)) AS assistance_service_unit,
            count(DISTINCT pl.child_id) FILTER (WHERE 'SMALLER_GROUP' = ANY(aao.actions)) AS smaller_group,
            count(DISTINCT pl.child_id) FILTER (WHERE 'SPECIAL_GROUP' = ANY(aao.actions)) AS special_group,
            count(DISTINCT pl.child_id) FILTER (WHERE 'PERVASIVE_VEO_SUPPORT' = ANY(aao.actions)) AS pervasive_veo_support,
            count(DISTINCT pl.child_id) FILTER (WHERE 'RESOURCE_PERSON' = ANY(aao.actions)) AS resource_person,
            count(DISTINCT pl.child_id) FILTER (WHERE 'RATIO_DECREASE' = ANY(aao.actions)) AS ratio_decrease,
            count(DISTINCT pl.child_id) FILTER (WHERE 'PERIODICAL_VEO_SUPPORT' = ANY(aao.actions)) AS periodical_veo_support,
            count(DISTINCT pl.child_id) FILTER (WHERE 'OTHER' = ANY(aao.actions)) AS other_assistance_action,
            count(DISTINCT pl.child_id) FILTER (WHERE cardinality(aao.actions) = 0) AS no_assistance_actions
        FROM daycare u
        JOIN care_area ca on u.care_area_id = ca.id
        JOIN daycare_group g ON g.daycare_id = u.id AND daterange(g.start_date, g.end_date, '[]') @> :target_date
        LEFT JOIN daycare_group_placement gpl ON gpl.daycare_group_id = g.id AND daterange(gpl.start_date, gpl.end_date, '[]') @> :target_date
        LEFT JOIN placement pl ON pl.id = gpl.daycare_placement_id
        LEFT JOIN assistance_need an on an.child_id = pl.child_id AND daterange(an.start_date, an.end_date, '[]') @> :target_date
        LEFT JOIN (
            SELECT r.need_id, array_remove(array_agg(o.value), null) AS bases
            FROM assistance_basis_option_ref r
            JOIN assistance_basis_option o ON o.id = r.option_id
            GROUP BY r.need_id
        ) abo ON abo.need_id = an.id
        LEFT JOIN assistance_action aa on aa.child_id = pl.child_id AND daterange(aa.start_date, aa.end_date, '[]') @> :target_date
        LEFT JOIN (
            SELECT r.action_id, array_remove(array_agg(o.value), null) AS actions
            FROM assistance_action_option_ref r
            JOIN assistance_action_option o ON o.id = r.option_id
            GROUP BY r.action_id
        ) aao ON aao.action_id = aa.id
        ${if (authorizedUnits != AclAuthorization.All) "WHERE u.id = ANY(:units)" else ""}
        GROUP BY ca.name, u.id, u.name, g.id, g.name, u.type, u.provider_type
        ORDER BY ca.name, u.name, g.name;
        """.trimIndent()

    @Suppress("UNCHECKED_CAST")
    return createQuery(sql)
        .bind("target_date", date)
        .bind("units", authorizedUnits.ids?.toTypedArray())
        .map { rs, _ ->
            AssistanceNeedsAndActionsReportRow(
                careAreaName = rs.getString("care_area_name"),
                unitId = DaycareId(rs.getUUID("unit_id")),
                unitName = rs.getString("unit_name"),
                groupId = GroupId(rs.getUUID("group_id")),
                groupName = rs.getString("group_name"),
                unitType = (rs.getArray("unit_type").array as Array<out Any>).map { it.toString() }.toSet().let(::getPrimaryUnitType),
                unitProviderType = rs.getString("unit_provider_type"),

                autism = rs.getInt("autism"),
                developmentalDisability1 = rs.getInt("developmental_disability_1"),
                developmentalDisability2 = rs.getInt("developmental_disability_2"),
                focusChallenge = rs.getInt("focus_challenge"),
                linguisticChallenge = rs.getInt("linguistic_challenge"),
                developmentMonitoring = rs.getInt("development_monitoring"),
                developmentMonitoringPending = rs.getInt("development_monitoring_pending"),
                multiDisability = rs.getInt("multi_disability"),
                longTermCondition = rs.getInt("long_term_condition"),
                regulationSkillChallenge = rs.getInt("regulation_skill_challenge"),
                disability = rs.getInt("disability"),
                otherAssistanceNeed = rs.getInt("other_assistance_need"),
                noAssistanceNeeds = rs.getInt("no_assistance_needs"),

                assistanceServiceChild = rs.getInt("assistance_service_child"),
                assistanceServiceUnit = rs.getInt("assistance_service_unit"),
                smallerGroup = rs.getInt("smaller_group"),
                specialGroup = rs.getInt("special_group"),
                pervasiveVeoSupport = rs.getInt("pervasive_veo_support"),
                resourcePerson = rs.getInt("resource_person"),
                ratioDecrease = rs.getInt("ratio_decrease"),
                periodicalVeoSupport = rs.getInt("periodical_veo_support"),
                otherAssistanceAction = rs.getInt("other_assistance_action"),
                noAssistanceActions = rs.getInt("no_assistance_actions")
            )
        }.toList()
}

data class AssistanceNeedsAndActionsReportRow(
    val careAreaName: String,
    val unitId: DaycareId,
    val unitName: String,
    val groupId: GroupId,
    val groupName: String,
    val unitType: UnitType?,
    val unitProviderType: String,

    val autism: Int,
    val developmentalDisability1: Int,
    val developmentalDisability2: Int,
    val focusChallenge: Int,
    val linguisticChallenge: Int,
    val developmentMonitoring: Int,
    val developmentMonitoringPending: Int,
    val multiDisability: Int,
    val longTermCondition: Int,
    val regulationSkillChallenge: Int,
    val disability: Int,
    val otherAssistanceNeed: Int,
    val noAssistanceNeeds: Int,

    val assistanceServiceChild: Int,
    val assistanceServiceUnit: Int,
    val smallerGroup: Int,
    val specialGroup: Int,
    val pervasiveVeoSupport: Int,
    val resourcePerson: Int,
    val ratioDecrease: Int,
    val periodicalVeoSupport: Int,
    val otherAssistanceAction: Int,
    val noAssistanceActions: Int
)
