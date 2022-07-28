// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
package fi.espoo.evaka.assistanceneed.decision

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.AssistanceNeedDecisionId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/citizen")
class AssistanceNeedDecisionCitizenController(
    private val accessControl: AccessControl
) {
    @GetMapping("/children/{childId}/assistance-need-decisions")
    fun getAssistanceNeedDecisions(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable childId: ChildId
    ): List<AssistanceNeedDecisionCitizenListItem> {
        Audit.ChildAssistanceNeedDecisionsListCitizen.log(targetId = childId)
        accessControl.requirePermissionFor(user, Action.Citizen.Child.READ_ASSISTANCE_NEED_DECISIONS, childId)

        return db.connect { dbc ->
            dbc.transaction { tx ->
                tx.getAssistanceNeedDecisionsByChildIdForCitizen(childId)
            }
        }
    }

    @GetMapping("/children/assistance-need-decision/{id}")
    fun getAssistanceNeedDecision(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable id: AssistanceNeedDecisionId
    ): AssistanceNeedDecision {
        Audit.ChildAssistanceNeedDecisionReadCitizen.log(targetId = id)
        accessControl.requirePermissionFor(user, Action.Citizen.AssistanceNeedDecision.READ, id)
        return db.connect { dbc ->
            dbc.read { tx ->
                val decision = tx.getAssistanceNeedDecisionById(id)

                if (decision.status != AssistanceNeedDecisionStatus.ACCEPTED && decision.status != AssistanceNeedDecisionStatus.REJECTED) {
                    throw NotFound("Citizen can only view accepted and rejected assistance need decisions")
                }

                decision
            }
        }
    }
}
