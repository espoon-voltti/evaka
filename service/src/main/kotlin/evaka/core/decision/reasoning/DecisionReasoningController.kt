// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.decision.reasoning

import evaka.core.Audit
import evaka.core.AuditId
import evaka.core.EvakaEnv
import evaka.core.shared.DecisionGenericReasoningId
import evaka.core.shared.DecisionIndividualReasoningId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.Forbidden
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/employee/decision-reasonings")
class DecisionReasoningController(
    private val accessControl: AccessControl,
    private val evakaEnv: EvakaEnv,
) {

    @GetMapping("/generic")
    fun getGenericReasonings(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam collectionType: DecisionReasoningCollectionType,
    ): List<DecisionGenericReasoning> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.READ_DECISION_REASONINGS,
                    )
                    tx.getGenericReasonings(collectionType, clock.today())
                }
            }
            .also { Audit.DecisionReasoningGenericRead.log() }
    }

    @PostMapping("/generic")
    fun createGenericReasoning(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: DecisionGenericReasoningRequest,
    ): DecisionGenericReasoningId {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.WRITE_DECISION_REASONINGS,
                    )
                    tx.insertGenericReasoning(body, clock.now())
                }
            }
            .also { Audit.DecisionReasoningGenericCreate.log(targetId = AuditId(it)) }
    }

    @PutMapping("/generic/{id}")
    fun updateGenericReasoning(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: DecisionGenericReasoningId,
        @RequestBody body: DecisionGenericReasoningRequest,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Global.WRITE_DECISION_REASONINGS,
                )
                tx.updateGenericReasoning(id, body, clock.now())
            }
        }
        Audit.DecisionReasoningGenericUpdate.log(targetId = AuditId(id))
    }

    @DeleteMapping("/generic/{id}")
    fun deleteGenericReasoning(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: DecisionGenericReasoningId,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Global.WRITE_DECISION_REASONINGS,
                )
                tx.deleteGenericReasoning(id)
            }
        }
        Audit.DecisionReasoningGenericDelete.log(targetId = AuditId(id))
    }

    @PutMapping("/generic/{id}/remove")
    fun removeGenericReasoning(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: DecisionGenericReasoningId,
    ) {
        if (!evakaEnv.decisionReasoningGenericRemovalEnabled) {
            throw Forbidden("Feature not enabled")
        }
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Global.WRITE_DECISION_REASONINGS,
                )
                tx.removeGenericReasoning(id, clock.now())
            }
        }
        Audit.DecisionReasoningGenericRemove.log(targetId = AuditId(id))
    }

    @GetMapping("/individual")
    fun getIndividualReasonings(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam collectionType: DecisionReasoningCollectionType,
    ): List<DecisionIndividualReasoning> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.READ_DECISION_REASONINGS,
                    )
                    tx.getIndividualReasonings(collectionType)
                }
            }
            .also { Audit.DecisionReasoningIndividualRead.log() }
    }

    @PostMapping("/individual")
    fun createIndividualReasoning(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: DecisionIndividualReasoningRequest,
    ): DecisionIndividualReasoningId {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.WRITE_DECISION_REASONINGS,
                    )
                    tx.insertIndividualReasoning(body, clock.now())
                }
            }
            .also { Audit.DecisionReasoningIndividualCreate.log(targetId = AuditId(it)) }
    }

    @PutMapping("/individual/{id}/remove")
    fun removeIndividualReasoning(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: DecisionIndividualReasoningId,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Global.WRITE_DECISION_REASONINGS,
                )
                tx.removeIndividualReasoning(id, clock.now())
            }
        }
        Audit.DecisionReasoningIndividualRemove.log(targetId = AuditId(id))
    }
}
