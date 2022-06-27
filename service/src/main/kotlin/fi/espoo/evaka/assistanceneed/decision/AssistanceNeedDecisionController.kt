// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
package fi.espoo.evaka.assistanceneed.decision

import fi.espoo.evaka.Audit
import fi.espoo.evaka.pis.service.getChildGuardians
import fi.espoo.evaka.shared.AssistanceNeedDecisionId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

data class AssistanceNeedDecisionRequest(
    val decision: AssistanceNeedDecisionForm
)

@RestController
class AssistanceNeedDecisionController(
    private val accessControl: AccessControl
) {
    @PostMapping("/children/{childId}/assistance-needs/decision")
    fun createAssistanceNeedDecision(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable childId: ChildId,
        @RequestBody body: AssistanceNeedDecisionRequest
    ): AssistanceNeedDecision {
        Audit.ChildAssistanceNeedDecisionCreate.log(targetId = childId)
        accessControl.requirePermissionFor(user, Action.Child.CREATE_ASSISTANCE_NEED_DECISION, childId)
        return db.connect { dbc ->
            dbc.transaction { tx ->
                var decision: AssistanceNeedDecisionForm = body.decision
                if (decision.guardianInfo.isEmpty()) {
                    val guardianIds = tx.getChildGuardians(childId)
                    decision = body.decision.copy(
                        guardianInfo = guardianIds.map { gid ->
                            AssistanceNeedDecisionGuardian(
                                personId = gid,
                                name = "", // not stored
                                isHeard = false,
                                details = null
                            )
                        }.toSet()
                    )
                }

                tx.insertAssistanceNeedDecision(childId, decision)
            }
        }
    }

    @GetMapping("/children/{childId}/assistance-needs/decision/{id}")
    fun getAssistanceNeedDecision(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable childId: ChildId,
        @PathVariable id: AssistanceNeedDecisionId
    ): AssistanceNeedDecision {
        Audit.ChildAssistanceNeedDecisionRead.log(targetId = id)
        accessControl.requirePermissionFor(user, Action.Child.READ_ASSISTANCE_NEED_DECISION, childId)
        return db.connect { dbc ->
            dbc.read { tx ->
                tx.getAssistanceNeedDecisionById(id, childId)
            }
        }
    }

    @PutMapping("/children/{childId}/assistance-needs/decision/{id}")
    fun updateAssistanceNeedDecision(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable childId: ChildId,
        @PathVariable id: AssistanceNeedDecisionId,
        @RequestBody body: AssistanceNeedDecisionRequest
    ) {
        Audit.ChildAssistanceNeedDecisionUpdate.log(targetId = id)
        accessControl.requirePermissionFor(user, Action.Child.UPDATE_ASSISTANCE_NEED_DECISION, childId)
        return db.connect { dbc ->
            dbc.transaction { tx ->
                tx.updateAssistanceNeedDecision(id, childId, body.decision)
            }
        }
    }

    @GetMapping("/children/{childId}/assistance-needs/decisions")
    fun getAssistanceNeedDecisions(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable childId: ChildId
    ): List<CompactAssistanceNeedDecision> {
        Audit.ChildAssistanceNeedDecisionsList.log(targetId = childId)
        accessControl.requirePermissionFor(user, Action.Child.READ_ASSISTANCE_NEED_DECISIONS, childId)
        return db.connect { dbc ->
            dbc.read { tx ->
                tx.getAssistanceNeedDecisionsByChildId(childId)
            }
        }
    }

    @DeleteMapping("/children/{childId}/assistance-needs/decision/{id}")
    fun deleteAssistanceNeedDecision(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable childId: ChildId,
        @PathVariable id: AssistanceNeedDecisionId
    ) {
        Audit.ChildAssistanceNeedDecisionDelete.log(targetId = id, objectId = childId)
        accessControl.requirePermissionFor(user, Action.Child.DELETE_ASSISTANCE_NEED_DECISION, childId)
        return db.connect { dbc ->
            dbc.transaction { tx ->
                if (!tx.deleteAssistanceNeedDecision(id, childId)) {
                    throw NotFound("Assistance need decision $id cannot found or cannot be deleted", "DECISION_NOT_FOUND")
                }
            }
        }
    }
}
