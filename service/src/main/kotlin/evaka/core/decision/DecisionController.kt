// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.decision

import evaka.core.Audit
import evaka.core.AuditId
import evaka.core.EvakaEnv
import evaka.core.application.fetchApplicationDetails
import evaka.core.decision.reasoning.DraftReasoningPreview
import evaka.core.decision.reasoning.applicableReasoningCollectionType
import evaka.core.decision.reasoning.deleteDecisionIndividualReasoningLink
import evaka.core.decision.reasoning.getDraftReasoningPreview
import evaka.core.decision.reasoning.getIndividualReasoning
import evaka.core.decision.reasoning.insertDecisionIndividualReasoningLink
import evaka.core.document.archival.validateArchivability
import evaka.core.pis.getPersonById
import evaka.core.shared.DecisionId
import evaka.core.shared.DecisionIndividualReasoningId
import evaka.core.shared.PersonId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.Forbidden
import evaka.core.shared.domain.NotFound
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/employee/decisions")
class DecisionController(
    private val decisionService: DecisionService,
    private val accessControl: AccessControl,
    private val evakaEnv: EvakaEnv,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
) {
    data class DecisionWithPermittedActions(
        val data: Decision,
        val permittedActions: Set<Action.Decision>,
    )

    @GetMapping("/by-guardian")
    fun getDecisionsByGuardian(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam id: PersonId,
    ): List<DecisionWithPermittedActions> {
        val decisions = db.connect { dbc ->
            dbc.read {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Person.READ_DECISIONS,
                    id,
                )
                val filter =
                    accessControl.requireAuthorizationFilter(it, user, clock, Action.Decision.READ)
                val decisions = it.getDecisionsByGuardian(id, filter)
                val permittedActions =
                    accessControl.getPermittedActions<DecisionId, Action.Decision>(
                        it,
                        user,
                        clock,
                        decisions.map(Decision::id),
                    )
                decisions.map { decision ->
                    DecisionWithPermittedActions(
                        decision,
                        permittedActions[decision.id] ?: emptySet(),
                    )
                }
            }
        }
        Audit.DecisionRead.log(targetId = AuditId(id), meta = mapOf("count" to decisions.size))
        return decisions
    }

    @GetMapping("/units")
    fun getDecisionUnits(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): List<DecisionUnit> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.READ_DECISION_UNITS,
                    )
                    getDecisionUnits(it)
                }
            }
            .also { Audit.UnitRead.log(meta = mapOf("count" to it.size)) }
    }

    @GetMapping("/{id}/draft-reasoning-preview")
    fun getDraftReasoningPreview(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: DecisionId,
    ): DraftReasoningPreview {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(tx, user, clock, Action.Decision.READ, id)
                    tx.getDraftReasoningPreview(id)
                }
            }
            .also { Audit.DecisionDraftReasoningPreviewRead.log(targetId = AuditId(id)) }
    }

    @GetMapping("/{id}/download", produces = [MediaType.APPLICATION_PDF_VALUE])
    fun downloadDecisionPdf(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: DecisionId,
    ): ResponseEntity<Any> {
        return db.connect { dbc ->
                val decision = dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Decision.DOWNLOAD_PDF,
                        id,
                    )

                    val decision =
                        tx.getDecision(id) ?: error("Cannot find decision for decision id '$id'")
                    val application =
                        tx.fetchApplicationDetails(decision.applicationId)
                            ?: error("Cannot find application for decision id '$id'")

                    val child =
                        tx.getPersonById(application.childId)
                            ?: error("Cannot find user for child id '${application.childId}'")

                    val guardian =
                        tx.getPersonById(application.guardianId)
                            ?: error("Cannot find user for guardian id '${application.guardianId}'")

                    if (
                        (child.restrictedDetailsEnabled || guardian.restrictedDetailsEnabled) &&
                            decision.documentContainsContactInfo &&
                            !user.isAdmin
                    ) {
                        throw Forbidden(
                            "Päätöksen alaisella henkilöllä on voimassa turvakielto. Osoitetietojen suojaamiseksi vain pääkäyttäjä voi ladata tämän päätöksen."
                        )
                    }

                    decision
                }
                decisionService.getDecisionPdf(dbc, decision)
            }
            .also { Audit.DecisionDownloadPdf.log(targetId = AuditId(id)) }
    }

    @PostMapping("/{decisionId}/archive")
    fun planArchiveDecision(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable decisionId: DecisionId,
        archivalEnabled: Boolean = evakaEnv.archivalEnabled,
    ) {
        if (!archivalEnabled) {
            throw BadRequest("Archival is not enabled")
        }

        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Decision.ARCHIVE,
                    decisionId,
                )

                val decision =
                    tx.getDecision(decisionId) ?: throw NotFound("Decision $decisionId not found")
                validateArchivability(decision)

                asyncJobRunner.plan(
                    tx = tx,
                    payloads = listOf(AsyncJob.ArchiveDecision(decision.id, user)),
                    runAt = clock.now(),
                    retryCount = 1,
                )
            }
        }
    }

    data class LinkIndividualReasoningBody(val reasoningId: DecisionIndividualReasoningId)

    @PostMapping("/{id}/individual-reasonings")
    fun linkIndividualReasoning(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: DecisionId,
        @RequestBody body: LinkIndividualReasoningBody,
    ) {
        val inserted = db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Decision.UPDATE_REASONING_LINKS,
                    id,
                )
                requireDecisionEditableForReasoningLinks(tx, id)
                requireReasoningEligibleForDecision(tx, id, body.reasoningId)
                tx.insertDecisionIndividualReasoningLink(
                    decisionId = id,
                    reasoningId = body.reasoningId,
                    createdAt = clock.now(),
                    createdBy = user.evakaUserId,
                )
            }
        }
        if (inserted) {
            Audit.DecisionReasoningIndividualLinkCreate.log(
                targetId = AuditId(id),
                objectId = AuditId(body.reasoningId),
            )
        }
    }

    @DeleteMapping("/{id}/individual-reasonings/{reasoningId}")
    fun unlinkIndividualReasoning(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: DecisionId,
        @PathVariable reasoningId: DecisionIndividualReasoningId,
    ) {
        val deleted = db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Decision.UPDATE_REASONING_LINKS,
                    id,
                )
                requireDecisionEditableForReasoningLinks(tx, id)
                tx.deleteDecisionIndividualReasoningLink(id, reasoningId)
            }
        }
        if (deleted) {
            Audit.DecisionReasoningIndividualLinkDelete.log(
                targetId = AuditId(id),
                objectId = AuditId(reasoningId),
            )
        }
    }

    private fun requireDecisionEditableForReasoningLinks(
        tx: Database.Read,
        decisionId: DecisionId,
    ) {
        val decision =
            tx.getDecision(decisionId) ?: throw NotFound("Decision $decisionId not found")
        if (decision.sentDate != null) {
            throw BadRequest(
                "Decision $decisionId has already been sent; " +
                    "individual reasoning links cannot be changed after send."
            )
        }
    }

    private fun requireReasoningEligibleForDecision(
        tx: Database.Read,
        decisionId: DecisionId,
        reasoningId: DecisionIndividualReasoningId,
    ) {
        val decision =
            tx.getDecision(decisionId) ?: throw NotFound("Decision $decisionId not found")
        val reasoning =
            tx.getIndividualReasoning(reasoningId)
                ?: throw NotFound("Individual reasoning $reasoningId not found")
        if (reasoning.removedAt != null) {
            throw BadRequest("Individual reasoning $reasoningId is removed")
        }
        if (reasoning.collectionType != decision.type.applicableReasoningCollectionType()) {
            throw BadRequest(
                "Individual reasoning $reasoningId has collection_type ${reasoning.collectionType}, " +
                    "which does not apply to decision $decisionId of type ${decision.type}"
            )
        }
    }
}
