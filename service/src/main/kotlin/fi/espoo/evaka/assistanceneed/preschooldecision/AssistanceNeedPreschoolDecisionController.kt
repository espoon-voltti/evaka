// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
package fi.espoo.evaka.assistanceneed.preschooldecision

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionStatus
import fi.espoo.evaka.caseprocess.CaseProcessMetadataService
import fi.espoo.evaka.caseprocess.CaseProcessState
import fi.espoo.evaka.caseprocess.deleteCaseProcessByAssistanceNeedPreschoolDecisionId
import fi.espoo.evaka.caseprocess.getCaseProcessByAssistanceNeedPreschoolDecisionId
import fi.espoo.evaka.caseprocess.insertCaseProcess
import fi.espoo.evaka.caseprocess.insertCaseProcessHistoryRow
import fi.espoo.evaka.pis.Employee
import fi.espoo.evaka.pis.getEmployees
import fi.espoo.evaka.pis.getEmployeesByRoles
import fi.espoo.evaka.pis.service.getChildGuardians
import fi.espoo.evaka.shared.ArchiveProcessType
import fi.espoo.evaka.shared.AssistanceNeedPreschoolDecisionId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AssistanceNeedPreschoolDecisionController(
    private val featureConfig: FeatureConfig,
    private val accessControl: AccessControl,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val assistanceNeedPreschoolDecisionService: AssistanceNeedPreschoolDecisionService,
) {
    private val metadata = CaseProcessMetadataService(featureConfig)

    @PostMapping("/employee/children/{childId}/assistance-need-preschool-decisions")
    fun createAssistanceNeedPreschoolDecision(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
    ): AssistanceNeedPreschoolDecision {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Child.CREATE_ASSISTANCE_NEED_PRESCHOOL_DECISION,
                        childId,
                    )

                    val now = clock.now()
                    val processId =
                        metadata
                            .getProcessParams(
                                ArchiveProcessType.ASSISTANCE_NEED_DECISION_PRESCHOOL,
                                now.year,
                            )
                            ?.let { process ->
                                val processId = tx.insertCaseProcess(process).id
                                tx.insertCaseProcessHistoryRow(
                                    processId = processId,
                                    state = CaseProcessState.INITIAL,
                                    now = now,
                                    userId = user.evakaUserId,
                                )
                                processId
                            }

                    tx.insertEmptyAssistanceNeedPreschoolDecisionDraft(childId, processId, user)
                }
            }
            .also { assistanceNeedDecision ->
                Audit.ChildAssistanceNeedPreschoolDecisionCreate.log(
                    targetId = AuditId(childId),
                    objectId = AuditId(assistanceNeedDecision.id),
                )
            }
    }

    @GetMapping("/employee/assistance-need-preschool-decisions/{id}")
    fun getAssistanceNeedPreschoolDecision(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: AssistanceNeedPreschoolDecisionId,
    ): AssistanceNeedPreschoolDecisionResponse {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.AssistanceNeedPreschoolDecision.READ,
                        id,
                    )
                    val decision = tx.getAssistanceNeedPreschoolDecisionById(id)

                    AssistanceNeedPreschoolDecisionResponse(
                        decision,
                        permittedActions = accessControl.getPermittedActions(tx, user, clock, id),
                    )
                }
            }
            .also { Audit.ChildAssistanceNeedPreschoolDecisionRead.log(targetId = AuditId(id)) }
    }

    @GetMapping("/employee/assistance-need-preschool-decisions/{id}/pdf")
    fun getAssistanceNeedPreschoolDecisionPdf(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: AssistanceNeedPreschoolDecisionId,
    ): ResponseEntity<Any> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.AssistanceNeedPreschoolDecision.READ,
                        id,
                    )
                }
                assistanceNeedPreschoolDecisionService.getDecisionPdfResponse(dbc, id)
            }
            .also {
                Audit.ChildAssistanceNeedPreschoolDecisionDownloadEmployee.log(
                    targetId = AuditId(id)
                )
            }
    }

    @PutMapping("/employee/assistance-need-preschool-decisions/{id}")
    fun updateAssistanceNeedPreschoolDecision(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: AssistanceNeedPreschoolDecisionId,
        @RequestBody body: AssistanceNeedPreschoolDecisionForm,
    ) {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.AssistanceNeedPreschoolDecision.UPDATE,
                        id,
                    )

                    tx.updateAssistanceNeedPreschoolDecision(id, body)
                }
            }
            .also { Audit.ChildAssistanceNeedPreschoolDecisionUpdate.log(targetId = AuditId(id)) }
    }

    @PutMapping("/employee/assistance-need-preschool-decisions/{id}/send")
    fun sendAssistanceNeedPreschoolDecisionForDecision(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: AssistanceNeedPreschoolDecisionId,
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.AssistanceNeedPreschoolDecision.SEND,
                        id,
                    )

                    val decision = tx.getAssistanceNeedPreschoolDecisionById(id)
                    if (!decision.isValid) throw BadRequest("Decision form is not valid")

                    tx.getCaseProcessByAssistanceNeedPreschoolDecisionId(id)?.also { process ->
                        if (process.history.none { it.state == CaseProcessState.PREPARATION }) {
                            tx.insertCaseProcessHistoryRow(
                                processId = process.id,
                                state = CaseProcessState.PREPARATION,
                                now = clock.now(),
                                userId = user.evakaUserId,
                            )
                        }
                    }

                    tx.updateAssistanceNeedPreschoolDecisionToSent(id, clock.today())
                }
            }
            .also { Audit.ChildAssistanceNeedPreschoolDecisionSend.log(targetId = AuditId(id)) }
    }

    @PutMapping("/employee/assistance-need-preschool-decisions/{id}/unsend")
    fun revertAssistanceNeedPreschoolDecisionToUnsent(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: AssistanceNeedPreschoolDecisionId,
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.AssistanceNeedPreschoolDecision.REVERT_TO_UNSENT,
                        id,
                    )

                    tx.updateAssistanceNeedPreschoolDecisionToNotSent(id)
                }
            }
            .also {
                Audit.ChildAssistanceNeedPreschoolDecisionRevertToUnsent.log(targetId = AuditId(id))
            }
    }

    @PutMapping("/employee/assistance-need-preschool-decisions/{id}/mark-as-opened")
    fun markAssistanceNeedPreschoolDecisionAsOpened(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: AssistanceNeedPreschoolDecisionId,
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.AssistanceNeedPreschoolDecision.MARK_AS_OPENED,
                        id,
                    )

                    tx.markAssistanceNeedPreschoolDecisionAsOpened(id)
                }
            }
            .also { Audit.ChildAssistanceNeedPreschoolDecisionOpened.log(targetId = AuditId(id)) }
    }

    @PutMapping("/employee/assistance-need-preschool-decisions/{id}/decide")
    fun decideAssistanceNeedPreschoolDecision(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: AssistanceNeedPreschoolDecisionId,
        @RequestBody body: DecideAssistanceNeedPreschoolDecisionRequest,
    ) {
        val decided =
            when (body.status) {
                AssistanceNeedDecisionStatus.ACCEPTED,
                AssistanceNeedDecisionStatus.REJECTED -> true
                AssistanceNeedDecisionStatus.NEEDS_WORK -> false
                AssistanceNeedDecisionStatus.DRAFT ->
                    throw BadRequest("Assistance need decisions cannot be decided to be draft")
                AssistanceNeedDecisionStatus.ANNULLED ->
                    throw BadRequest("Assistance need decisions cannot be decided to be annulled")
            }

        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.AssistanceNeedPreschoolDecision.DECIDE,
                        id,
                    )

                    val decision = tx.getAssistanceNeedPreschoolDecisionById(id)

                    if (decision.status.isDecided()) {
                        throw BadRequest("Already-decided decisions cannot be decided again")
                    }
                    if (decision.form.validFrom == null) {
                        throw BadRequest("Accepted decision must have a start date")
                    }

                    val validTo =
                        if (body.status == AssistanceNeedDecisionStatus.ACCEPTED) {
                            tx.endActiveAssistanceNeedPreschoolDecisions(
                                decision.id,
                                decision.form.validFrom.minusDays(1),
                                decision.child.id,
                            )
                            tx.getNextAssistanceNeedPreschoolDecisionValidFrom(
                                    decision.child.id,
                                    decision.form.validFrom,
                                )
                                ?.minusDays(1)
                        } else null

                    tx.decideAssistanceNeedPreschoolDecision(
                        id = id,
                        status = body.status,
                        decisionMade = clock.today().takeIf { decided },
                        unreadGuardianIds =
                            if (decided) {
                                tx.getChildGuardians(decision.child.id)
                            } else {
                                null
                            },
                        validTo = validTo,
                    )

                    if (decided) {
                        tx.getCaseProcessByAssistanceNeedPreschoolDecisionId(id)?.also {
                            tx.insertCaseProcessHistoryRow(
                                processId = it.id,
                                state = CaseProcessState.DECIDING,
                                now = clock.now(),
                                userId = user.evakaUserId,
                            )
                        }

                        asyncJobRunner.plan(
                            tx,
                            listOf(AsyncJob.CreateAssistanceNeedPreschoolDecisionPdf(id)),
                            runAt = clock.now(),
                        )
                    }
                }
            }
            .also {
                Audit.ChildAssistanceNeedPreschoolDecisionDecide.log(
                    targetId = AuditId(id),
                    meta = mapOf("status" to body.status),
                )
            }
    }

    @PutMapping("/employee/assistance-need-preschool-decisions/{id}/annul")
    fun annulAssistanceNeedPreschoolDecision(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: AssistanceNeedPreschoolDecisionId,
        @RequestBody body: AnnulAssistanceNeedPreschoolDecisionRequest,
    ) {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.AssistanceNeedPreschoolDecision.ANNUL,
                        id,
                    )

                    val decision = tx.getAssistanceNeedPreschoolDecisionById(id)

                    if (!decision.status.isDecided()) {
                        throw BadRequest("Cannot annul undecided decision")
                    }

                    if (decision.status == AssistanceNeedDecisionStatus.ANNULLED) {
                        throw BadRequest("Already annulled")
                    }

                    tx.annulAssistanceNeedPreschoolDecision(id, body.reason)
                }
            }
            .also { Audit.ChildAssistanceNeedPreschoolDecisionAnnul.log(targetId = AuditId(id)) }
    }

    @GetMapping("/employee/children/{childId}/assistance-need-preschool-decisions")
    fun getAssistanceNeedPreschoolDecisions(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
    ): List<AssistanceNeedPreschoolDecisionBasicsResponse> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    val filter =
                        accessControl.requireAuthorizationFilter(
                            tx,
                            user,
                            clock,
                            Action.AssistanceNeedPreschoolDecision.READ,
                        )
                    val decisions =
                        tx.getAssistanceNeedPreschoolDecisionsByChildIdUsingFilter(childId, filter)
                    val permittedActions =
                        accessControl.getPermittedActions<
                            AssistanceNeedPreschoolDecisionId,
                            Action.AssistanceNeedPreschoolDecision,
                        >(
                            tx,
                            user,
                            clock,
                            decisions.map { it.id },
                        )
                    decisions.map {
                        AssistanceNeedPreschoolDecisionBasicsResponse(
                            decision = it,
                            permittedActions = permittedActions[it.id]!!,
                        )
                    }
                }
            }
            .also {
                Audit.ChildAssistanceNeedPreschoolDecisionsList.log(
                    targetId = AuditId(childId),
                    meta = mapOf("count" to it.size),
                )
            }
    }

    @DeleteMapping("/employee/assistance-need-preschool-decisions/{id}")
    fun deleteAssistanceNeedPreschoolDecision(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: AssistanceNeedPreschoolDecisionId,
    ) {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.AssistanceNeedPreschoolDecision.DELETE,
                        id,
                    )
                    deleteCaseProcessByAssistanceNeedPreschoolDecisionId(tx, id)
                    if (!tx.deleteAssistanceNeedPreschoolDecision(id)) {
                        throw NotFound(
                            "Assistance need preschool decision $id cannot found or cannot be deleted",
                            "DECISION_NOT_FOUND",
                        )
                    }
                }
            }
            .also { Audit.ChildAssistanceNeedPreschoolDecisionDelete.log(targetId = AuditId(id)) }
    }

    @GetMapping("/employee/assistance-need-preschool-decisions/{id}/decision-maker-options")
    fun getAssistancePreschoolDecisionMakerOptions(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: AssistanceNeedPreschoolDecisionId,
    ): List<Employee> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.AssistanceNeedPreschoolDecision.READ_DECISION_MAKER_OPTIONS,
                        id,
                    )
                    val assistanceDecision = tx.getAssistanceNeedPreschoolDecisionById(id)
                    featureConfig.preschoolAssistanceDecisionMakerRoles?.let { roles ->
                        tx.getEmployeesByRoles(roles, assistanceDecision.form.selectedUnit)
                    } ?: tx.getEmployees().sortedBy { it.email }
                }
            }
            .also {
                Audit.ChildAssistanceNeedPreschoolDecisionReadDecisionMakerOptions.log(
                    targetId = AuditId(id),
                    meta = mapOf("count" to it.size),
                )
            }
    }

    data class AssistanceNeedPreschoolDecisionBasicsResponse(
        val decision: AssistanceNeedPreschoolDecisionBasics,
        val permittedActions: Set<Action.AssistanceNeedPreschoolDecision>,
    )

    data class AssistanceNeedPreschoolDecisionResponse(
        val decision: AssistanceNeedPreschoolDecision,
        val permittedActions: Set<Action.AssistanceNeedPreschoolDecision>,
    )

    data class DecideAssistanceNeedPreschoolDecisionRequest(
        val status: AssistanceNeedDecisionStatus
    )

    data class AnnulAssistanceNeedPreschoolDecisionRequest(val reason: String)
}
