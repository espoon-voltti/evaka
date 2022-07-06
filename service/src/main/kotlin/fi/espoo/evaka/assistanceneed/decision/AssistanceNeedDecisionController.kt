// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
package fi.espoo.evaka.assistanceneed.decision

import fi.espoo.evaka.Audit
import fi.espoo.evaka.pis.service.getChildGuardians
import fi.espoo.evaka.shared.AssistanceNeedDecisionId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Forbidden
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
import java.time.LocalDate

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
                var decision: AssistanceNeedDecisionForm = body.decision.copy(
                    status = AssistanceNeedDecisionStatus.DRAFT,
                    sentForDecision = null
                )
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

    @GetMapping("/assistance-need-decision/{id}")
    fun getAssistanceNeedDecision(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable id: AssistanceNeedDecisionId
    ): AssistanceNeedDecisionResponse {
        Audit.ChildAssistanceNeedDecisionRead.log(targetId = id)
        accessControl.requirePermissionFor(user, Action.AssistanceNeedDecision.READ, id)
        return db.connect { dbc ->
            dbc.read { tx ->
                val decision = tx.getAssistanceNeedDecisionById(id)

                AssistanceNeedDecisionResponse(
                    decision,
                    permittedActions = accessControl.getPermittedActions(tx, user, id),
                    hasMissingFields = hasMissingFields(decision)
                )
            }
        }
    }

    @PutMapping("/assistance-need-decision/{id}")
    fun updateAssistanceNeedDecision(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable id: AssistanceNeedDecisionId,
        @RequestBody body: AssistanceNeedDecisionRequest
    ) {
        Audit.ChildAssistanceNeedDecisionUpdate.log(targetId = id)
        accessControl.requirePermissionFor(user, Action.AssistanceNeedDecision.UPDATE, id)
        return db.connect { dbc ->
            dbc.transaction { tx ->
                val decision = tx.getAssistanceNeedDecisionById(id)

                if (decision.status != AssistanceNeedDecisionStatus.NEEDS_WORK &&
                    (decision.status != AssistanceNeedDecisionStatus.DRAFT || decision.sentForDecision != null)
                ) {
                    throw Forbidden("Only non-sent draft or workable decisions can be edited", "UNEDITABLE_DECISION")
                }

                tx.updateAssistanceNeedDecision(
                    id,
                    body.decision.copy(
                        sentForDecision = decision.sentForDecision,
                        status = decision.status
                    )
                )
            }
        }
    }

    @PostMapping("/assistance-need-decision/{id}/send")
    fun sendAssistanceNeedDecision(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable id: AssistanceNeedDecisionId
    ) {
        Audit.ChildAssistanceNeedDecisionSend.log(targetId = id)
        accessControl.requirePermissionFor(user, Action.AssistanceNeedDecision.SEND, id)
        return db.connect { dbc ->
            dbc.transaction { tx ->
                val decision = tx.getAssistanceNeedDecisionById(id)

                if (decision.status != AssistanceNeedDecisionStatus.NEEDS_WORK &&
                    (decision.status != AssistanceNeedDecisionStatus.DRAFT || decision.sentForDecision != null)
                ) {
                    throw Forbidden("Only non-sent draft or workable decisions can be sent", "UNSENDABLE_DECISION")
                }

                if (hasMissingFields(decision)) {
                    throw BadRequest("Decision has missing fields", "MISSING_FIELDS")
                }

                tx.updateAssistanceNeedDecision(
                    id,
                    decision.copy(
                        sentForDecision = LocalDate.now(),
                        status = AssistanceNeedDecisionStatus.DRAFT
                    ).toForm(),
                    false
                )
            }
        }
    }

    @GetMapping("/children/{childId}/assistance-needs/decisions")
    fun getAssistanceNeedDecisions(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable childId: ChildId
    ): List<AssistanceNeedDecisionBasicsResponse> {
        Audit.ChildAssistanceNeedDecisionsList.log(targetId = childId)
        accessControl.requirePermissionFor(user, Action.Child.READ_ASSISTANCE_NEED_DECISIONS, childId)
        return db.connect { dbc ->
            dbc.read { tx ->
                val decisions = tx.getAssistanceNeedDecisionsByChildId(childId)
                val permittedActions = accessControl.getPermittedActions<AssistanceNeedDecisionId, Action.AssistanceNeedDecision>(
                    tx,
                    user,
                    decisions.map { it.id }
                )
                decisions.map {
                    AssistanceNeedDecisionBasicsResponse(decision = it, permittedActions = permittedActions[it.id]!!)
                }
            }
        }
    }

    @DeleteMapping("/assistance-need-decision/{id}")
    fun deleteAssistanceNeedDecision(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable id: AssistanceNeedDecisionId
    ) {
        Audit.ChildAssistanceNeedDecisionDelete.log(targetId = id)
        accessControl.requirePermissionFor(user, Action.AssistanceNeedDecision.DELETE, id)
        return db.connect { dbc ->
            dbc.transaction { tx ->
                if (!tx.deleteAssistanceNeedDecision(id)) {
                    throw NotFound("Assistance need decision $id cannot found or cannot be deleted", "DECISION_NOT_FOUND")
                }
            }
        }
    }

    @PostMapping("/assistance-need-decision/{id}/decide")
    fun decideAssistanceNeedDecision(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable id: AssistanceNeedDecisionId,
        @RequestBody body: DecideAssistanceNeedDecisionRequest
    ) {
        Audit.ChildAssistanceNeedDecisionDecide.log(targetId = id)
        accessControl.requirePermissionFor(user, Action.AssistanceNeedDecision.DECIDE, id)

        if (body.status == AssistanceNeedDecisionStatus.DRAFT) {
            throw BadRequest("Assistance need decisions cannot be decided to be a draft")
        }

        return db.connect { dbc ->
            dbc.transaction { tx ->
                val decision = tx.getAssistanceNeedDecisionById(id)

                if (decision.status == AssistanceNeedDecisionStatus.ACCEPTED || decision.status == AssistanceNeedDecisionStatus.REJECTED) {
                    throw BadRequest("Already-decided decisions cannot be decided again")
                }

                tx.updateAssistanceNeedDecision(
                    id,
                    decision.copy(
                        status = body.status,
                        decisionMade = if (body.status == AssistanceNeedDecisionStatus.NEEDS_WORK) null else LocalDate.now()
                    ).toForm()
                )
            }
        }
    }

    @PostMapping("/assistance-need-decision/{id}/mark-as-opened")
    fun markAssistanceNeedDecisionAsOpened(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable id: AssistanceNeedDecisionId
    ) {
        Audit.ChildAssistanceNeedDecisionOpened.log(targetId = id)
        accessControl.requirePermissionFor(user, Action.AssistanceNeedDecision.MARK_AS_OPENED, id)

        return db.connect { dbc ->
            dbc.transaction { tx ->
                tx.markAssistanceNeedDecisionAsOpened(id)
            }
        }
    }

    @PostMapping("/assistance-need-decision/{id}/update-decision-maker")
    fun updateAssistanceNeedDecisionDecisionMaker(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable id: AssistanceNeedDecisionId,
        @RequestBody body: UpdateDecisionMakerForAssistanceNeedDecisionRequest
    ) {
        Audit.ChildAssistanceNeedDecisionUpdateDecisionMaker.log(targetId = id)
        accessControl.requirePermissionFor(user, Action.AssistanceNeedDecision.UPDATE_DECISION_MAKER, id)

        return db.connect { dbc ->
            dbc.transaction { tx ->
                val decision = tx.getAssistanceNeedDecisionById(id)

                if (decision.status == AssistanceNeedDecisionStatus.ACCEPTED ||
                    decision.status == AssistanceNeedDecisionStatus.REJECTED ||
                    decision.sentForDecision == null
                ) {
                    throw BadRequest("Decision maker cannot be changed for already-decided or unsent decisions")
                }

                tx.updateAssistanceNeedDecision(
                    id,
                    decision.copy(
                        decisionMaker = AssistanceNeedDecisionMaker(
                            employeeId = EmployeeId(user.rawId()),
                            title = body.title
                        )
                    ).toForm(),
                    true
                )
            }
        }
    }

    private fun hasMissingFields(decision: AssistanceNeedDecision): Boolean {
        return decision.selectedUnit == null
    }

    data class AssistanceNeedDecisionBasicsResponse(
        val decision: AssistanceNeedDecisionBasics,
        val permittedActions: Set<Action.AssistanceNeedDecision>
    )

    data class AssistanceNeedDecisionResponse(
        val decision: AssistanceNeedDecision,
        val permittedActions: Set<Action.AssistanceNeedDecision>,
        val hasMissingFields: Boolean
    )

    data class DecideAssistanceNeedDecisionRequest(
        val status: AssistanceNeedDecisionStatus
    )

    data class UpdateDecisionMakerForAssistanceNeedDecisionRequest(
        val title: String
    )
}
