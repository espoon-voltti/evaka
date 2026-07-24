// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.decision.reasoning

import evaka.core.Audit
import evaka.core.AuditContext
import evaka.core.EvakaEnv
import evaka.core.shared.DecisionGenericReasoningId
import evaka.core.shared.DecisionIndividualReasoningId
import evaka.core.shared.FeatureConfig
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.BadRequest
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
    private val featureConfig: FeatureConfig,
) {

    @GetMapping("/generic")
    fun getGenericReasonings(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam collectionType: DecisionReasoningCollectionType,
    ): List<DecisionGenericReasoning> {
        val audit = AuditContext()
        audit.addMeta("collectionType", collectionType)
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
            .also { reasonings ->
                audit.addMeta("count", reasonings.size)
                audit.log(Audit.DecisionReasoningGenericRead, clock)
            }
    }

    @PostMapping("/generic")
    fun createGenericReasoning(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: DecisionGenericReasoningRequest,
    ): DecisionGenericReasoningId {
        val audit = AuditContext()
        audit.addMeta("collectionType", body.collectionType)
        audit.addMeta("ready", body.ready)
        audit.observeDate(body.validFrom)
        if (featureConfig.placementDecisionSwedishLanguageEnabled && body.textSv.isBlank()) {
            throw BadRequest("Swedish reasoning text is required")
        }
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
            .also { id ->
                audit.add(id)
                audit.log(Audit.DecisionReasoningGenericCreate, clock)
            }
    }

    @PutMapping("/generic/{id}")
    fun updateGenericReasoning(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: DecisionGenericReasoningId,
        @RequestBody body: DecisionGenericReasoningRequest,
    ) {
        val audit = AuditContext().add(id)
        audit.addMeta("ready", body.ready)
        audit.observeDate(body.validFrom)
        if (featureConfig.placementDecisionSwedishLanguageEnabled && body.textSv.isBlank()) {
            throw BadRequest("Swedish reasoning text is required")
        }
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
        audit.log(Audit.DecisionReasoningGenericUpdate, clock)
    }

    @DeleteMapping("/generic/{id}")
    fun deleteGenericReasoning(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: DecisionGenericReasoningId,
    ) {
        val audit = AuditContext().add(id)
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
            .also { deleted ->
                audit.addMeta("collectionType", deleted.collectionType)
                audit.observeDate(deleted.validFrom)
                audit.log(Audit.DecisionReasoningGenericDelete, clock)
            }
    }

    @PutMapping("/generic/{id}/remove")
    fun removeGenericReasoning(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: DecisionGenericReasoningId,
    ) {
        val audit = AuditContext().add(id)
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
        audit.log(Audit.DecisionReasoningGenericRemove, clock)
    }

    @GetMapping("/individual")
    fun getIndividualReasonings(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam collectionType: DecisionReasoningCollectionType,
    ): List<DecisionIndividualReasoning> {
        val audit = AuditContext()
        audit.addMeta("collectionType", collectionType)
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
            .also { reasonings ->
                audit.addMeta("count", reasonings.size)
                audit.log(Audit.DecisionReasoningIndividualRead, clock)
            }
    }

    @PostMapping("/individual")
    fun createIndividualReasoning(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: DecisionIndividualReasoningRequest,
    ): DecisionIndividualReasoningId {
        val audit = AuditContext()
        audit.addMeta("collectionType", body.collectionType)
        if (
            featureConfig.placementDecisionSwedishLanguageEnabled &&
                (body.titleSv.isBlank() || body.textSv.isBlank())
        ) {
            throw BadRequest("Swedish reasoning title and text are required")
        }
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
            .also { id ->
                audit.add(id)
                audit.log(Audit.DecisionReasoningIndividualCreate, clock)
            }
    }

    @PutMapping("/individual/{id}/remove")
    fun removeIndividualReasoning(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: DecisionIndividualReasoningId,
    ) {
        val audit = AuditContext().add(id)
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
        audit.log(Audit.DecisionReasoningIndividualRemove, clock)
    }
}
