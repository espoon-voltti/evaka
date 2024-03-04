// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionStatus
import fi.espoo.evaka.shared.AssistanceNeedDecisionId
import fi.espoo.evaka.shared.AssistanceNeedPreschoolDecisionId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import fi.espoo.evaka.shared.security.actionrule.forTable
import java.time.LocalDate
import java.util.UUID
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class AssistanceNeedDecisionsReport(private val accessControl: AccessControl) {
    @GetMapping("/reports/assistance-need-decisions")
    fun getAssistanceNeedDecisionsReport(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock
    ): List<AssistanceNeedDecisionsReportRow> {
        return db.connect { dbc ->
                dbc.read {
                    it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)

                    val filterDaycare =
                        accessControl.requireAuthorizationFilter(
                            it,
                            user,
                            clock,
                            Action.AssistanceNeedDecision.READ_IN_REPORT
                        )
                    val filterPreschool =
                        accessControl.requireAuthorizationFilter(
                            it,
                            user,
                            clock,
                            Action.AssistanceNeedPreschoolDecision.READ_IN_REPORT
                        )

                    it.getDecisionRows(user.evakaUserId, filterDaycare, filterPreschool)
                }
            }
            .also { Audit.AssistanceNeedDecisionsReportRead.log(meta = mapOf("count" to it.size)) }
    }

    @GetMapping("/reports/assistance-need-decisions/unread-count")
    fun getAssistanceNeedDecisionsReportUnreadCount(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock
    ): Int {
        return db.connect { dbc ->
                dbc.read { tx ->
                    if (
                        accessControl.isPermittedForSomeTarget(
                            tx,
                            user,
                            clock,
                            Action.AssistanceNeedDecision.READ_IN_REPORT
                        )
                    ) {
                        tx.getDecisionMakerUnreadCount(user.evakaUserId)
                    } else {
                        0
                    }
                }
            }
            .also { Audit.AssistanceNeedDecisionsReportUnreadCount.log() }
    }
}

private fun Database.Read.getDecisionRows(
    userId: EvakaUserId,
    idFilterDaycare: AccessControlFilter<AssistanceNeedDecisionId>,
    idFilterPreschool: AccessControlFilter<AssistanceNeedPreschoolDecisionId>,
): List<AssistanceNeedDecisionsReportRow> =
    createQuery {
            sql(
                """
WITH decisions AS (
    SELECT 
        id, false as preschool, status, sent_for_decision, decision_made, 
        child_id, selected_unit, decision_maker_employee_id, decision_maker_has_opened
    FROM assistance_need_decision ad
    WHERE sent_for_decision IS NOT NULL
    AND (${predicate(idFilterDaycare.forTable("ad"))})
    
    UNION ALL
    
    SELECT 
        id, true as preschool, status, sent_for_decision, decision_made, 
        child_id, selected_unit, decision_maker_employee_id, decision_maker_has_opened
    FROM assistance_need_preschool_decision apd
    WHERE sent_for_decision IS NOT NULL
    AND (${predicate(idFilterPreschool.forTable("apd"))})
)
SELECT ad.id, ad.preschool, sent_for_decision, concat(child.last_name, ' ', child.first_name) child_name,
    care_area.name care_area_name, daycare.name unit_name, decision_made, status,
    (CASE WHEN decision_maker_employee_id = ${bind(userId)} THEN decision_maker_has_opened END) is_opened
FROM decisions ad
JOIN person child ON child.id = ad.child_id
JOIN daycare ON daycare.id = ad.selected_unit
JOIN care_area ON care_area.id = daycare.care_area_id
        """
                    .trimIndent()
            )
        }
        .toList<AssistanceNeedDecisionsReportRow>()

data class AssistanceNeedDecisionsReportRow(
    val id: UUID,
    val preschool: Boolean,
    val sentForDecision: LocalDate,
    val childName: String,
    val careAreaName: String,
    val unitName: String,
    val decisionMade: LocalDate?,
    val status: AssistanceNeedDecisionStatus,
    val isOpened: Boolean?
)

private fun Database.Read.getDecisionMakerUnreadCount(userId: EvakaUserId): Int {
    return createQuery {
            sql(
                """
        SELECT COUNT(*)
        FROM (
            SELECT 1 FROM assistance_need_decision
            WHERE sent_for_decision IS NOT NULL
            AND decision_maker_employee_id = ${bind(userId)}
            AND NOT decision_maker_has_opened
            
            UNION ALL 
            
            SELECT 1 FROM assistance_need_preschool_decision
            WHERE sent_for_decision IS NOT NULL
            AND decision_maker_employee_id = ${bind(userId)}
            AND NOT decision_maker_has_opened
        ) decisions
        """
            )
        }
        .exactlyOne<Int>()
}
