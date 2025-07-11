// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document.childdocument

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.EvakaEnv
import fi.espoo.evaka.caseprocess.CaseProcessState
import fi.espoo.evaka.caseprocess.deleteProcessByDocumentId
import fi.espoo.evaka.caseprocess.insertCaseProcess
import fi.espoo.evaka.caseprocess.insertCaseProcessHistoryRow
import fi.espoo.evaka.caseprocess.updateDocumentCaseProcessHistory
import fi.espoo.evaka.document.ChildDocumentType
import fi.espoo.evaka.document.DocumentTemplate
import fi.espoo.evaka.document.DocumentTemplateContent
import fi.espoo.evaka.document.getTemplate
import fi.espoo.evaka.pis.Employee
import fi.espoo.evaka.pis.listPersonByDuplicateOf
import fi.espoo.evaka.placement.getPlacementsForChildDuring
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DocumentTemplateId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.DateRange
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
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/employee/child-documents")
class ChildDocumentController(
    private val accessControl: AccessControl,
    private val childDocumentService: ChildDocumentService,
    private val featureConfig: FeatureConfig,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val evakaEnv: EvakaEnv,
) {
    @PostMapping
    fun createDocument(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: ChildDocumentCreateRequest,
    ): ChildDocumentId {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Child.CREATE_CHILD_DOCUMENT,
                        body.childId,
                    )

                    val template =
                        tx.getTemplate(body.templateId)?.also {
                            if (!it.published || !it.validity.includes(clock.today())) {
                                throw BadRequest("Invalid template")
                            }
                        } ?: throw NotFound()

                    val sameTemplateAlreadyStarted =
                        tx.getNonCompletedChildDocumentChildIds(template.id, setOf(body.childId))
                            .contains(body.childId)
                    if (sameTemplateAlreadyStarted) {
                        throw Conflict("Child already has incomplete document of the same template")
                    }

                    createChildDocument(tx, user, clock, body.childId, template)
                }
            }
            .also { Audit.ChildDocumentCreate.log(targetId = AuditId(it)) }
    }

    private fun createChildDocument(
        tx: Database.Transaction,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        childId: ChildId,
        template: DocumentTemplate,
    ): ChildDocumentId {
        val now = clock.now()
        val processId =
            template.processDefinitionNumber?.let { processDefinitionNumber ->
                // guaranteed to be not null when processDefinitionNumber is not null by db
                // constraint
                val archiveDurationMonths = template.archiveDurationMonths!!
                tx.insertCaseProcess(
                        processDefinitionNumber = processDefinitionNumber,
                        year = now.year,
                        organization = featureConfig.archiveMetadataOrganization,
                        archiveDurationMonths = archiveDurationMonths,
                    )
                    .id
                    .also { processId ->
                        tx.insertCaseProcessHistoryRow(
                            processId = processId,
                            state = CaseProcessState.INITIAL,
                            now = now,
                            userId = user.evakaUserId,
                        )
                    }
            }

        return tx.insertChildDocument(
            childId = childId,
            templateId = template.id,
            now = now,
            userId = user.evakaUserId,
            processId = processId,
        )
    }

    @PutMapping
    fun createDocuments(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: ChildDocumentsCreateRequest,
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Child.CREATE_CHILD_DOCUMENT,
                        body.childIds,
                    )
                    val template =
                        tx.getTemplate(body.templateId)
                            ?: throw NotFound("Template ${body.templateId} not found")
                    if (template.type != ChildDocumentType.CITIZEN_BASIC)
                        throw BadRequest("Template ${body.templateId} not supported")
                    if (!template.validity.includes(clock.today()))
                        throw BadRequest("Template ${body.templateId} not active")
                    if (!template.published)
                        throw BadRequest("Template ${body.templateId} not published")
                    val sameTemplateAlreadyStarted =
                        tx.getNonCompletedChildDocumentChildIds(template.id, body.childIds)
                    if (sameTemplateAlreadyStarted.isNotEmpty()) {
                        throw Conflict(
                            "Children $sameTemplateAlreadyStarted already has incomplete document of the same template"
                        )
                    }
                    body.childIds.map { childId ->
                        createChildDocument(tx, user, clock, childId, template).also {
                            updateChildDocumentStatusForward(
                                tx,
                                user,
                                clock,
                                it,
                                DocumentStatus.CITIZEN_DRAFT,
                            )
                        }
                    }
                }
            }
            .also { Audit.ChildDocumentsCreate.log(targetId = AuditId(it)) }
    }

    @GetMapping
    fun getDocuments(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam childId: PersonId,
    ): List<ChildDocumentSummaryWithPermittedActions> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Child.READ_CHILD_DOCUMENT,
                        childId,
                    )
                    val documents =
                        tx.getChildDocuments(childId) +
                            tx.listPersonByDuplicateOf(childId).flatMap { duplicate ->
                                tx.getChildDocuments(duplicate.id)
                            }
                    val permittedActions =
                        accessControl.getPermittedActions<ChildDocumentId, Action.ChildDocument>(
                            tx,
                            user,
                            clock,
                            documents.map { it.id },
                        )
                    documents.mapNotNull { document ->
                        permittedActions[document.id]
                            ?.takeIf { it.contains(Action.ChildDocument.READ) }
                            ?.let { ChildDocumentSummaryWithPermittedActions(document, it) }
                    }
                }
            }
            .also { Audit.ChildDocumentRead.log(targetId = AuditId(childId)) }
    }

    data class ChildDocumentSummaryWithPermittedActions(
        val data: ChildDocumentSummary,
        val permittedActions: Set<Action.ChildDocument>,
    )

    @GetMapping("/{documentId}")
    fun getDocument(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable documentId: ChildDocumentId,
    ): ChildDocumentWithPermittedActions {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.ChildDocument.READ,
                        documentId,
                    )

                    val document =
                        tx.getChildDocument(documentId)
                            ?: throw NotFound("Document $documentId not found")

                    val permittedActions =
                        accessControl.getPermittedActions<ChildDocumentId, Action.ChildDocument>(
                            tx,
                            user,
                            clock,
                            documentId,
                        )

                    ChildDocumentWithPermittedActions(
                        data = document,
                        permittedActions = permittedActions,
                    )
                }
            }
            .also { Audit.ChildDocumentRead.log(targetId = AuditId(documentId)) }
    }

    @GetMapping("/{documentId}/decision-makers")
    fun getChildDocumentDecisionMakers(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable documentId: ChildDocumentId,
    ): List<Employee> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.ChildDocument.READ,
                        documentId,
                    )

                    tx.getChildDocumentDecisionMakers(documentId)
                }
            }
            .also { Audit.ChildDocumentReadDecisionMakers.log(targetId = AuditId(documentId)) }
    }

    @PutMapping("/{documentId}/content")
    fun updateDocumentContent(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable documentId: ChildDocumentId,
        @RequestBody body: DocumentContent,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.ChildDocument.UPDATE,
                        documentId,
                    )
                    val document =
                        tx.getChildDocument(documentId)
                            ?: throw NotFound("Document $documentId not found")

                    if (!document.status.employeeEditable)
                        throw BadRequest("Cannot update contents of document in this status")

                    validateContentAgainstTemplate(body, document.template.content)

                    tx.getCurrentWriteLock(documentId, clock.now())?.also { lock ->
                        if (lock.modifiedBy != user.id) {
                            throw Conflict(
                                message = "Did not own the lock on the document",
                                errorCode = "invalid-lock",
                            )
                        }
                    }

                    tx.updateChildDocumentContent(
                        documentId,
                        document.status,
                        body,
                        clock.now(),
                        user.evakaUserId,
                    )
                }
                .also { Audit.ChildDocumentUpdateContent.log(targetId = AuditId(documentId)) }
        }
    }

    data class DocumentLockResponse(
        val lockTakenSuccessfully: Boolean,
        val currentLock: DocumentWriteLock,
    )

    @PutMapping("/{documentId}/lock")
    fun takeDocumentWriteLock(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable documentId: ChildDocumentId,
    ): DocumentLockResponse {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.ChildDocument.UPDATE,
                        documentId,
                    )
                    val success = tx.tryTakeWriteLock(documentId, clock.now(), user.evakaUserId)
                    val currentLock =
                        tx.getCurrentWriteLock(documentId, clock.now())
                            ?: throw IllegalStateException("lock should exist now")
                    DocumentLockResponse(
                        lockTakenSuccessfully = success && currentLock.modifiedBy == user.id,
                        currentLock = currentLock,
                    )
                }
            }
            .also { Audit.ChildDocumentTryTakeLockOnContent.log(targetId = AuditId(documentId)) }
    }

    @PutMapping("/{documentId}/publish")
    fun publishDocument(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable documentId: ChildDocumentId,
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.ChildDocument.PUBLISH,
                        documentId,
                    )

                    val document =
                        tx.getChildDocument(documentId)
                            ?: throw NotFound("Document $documentId not found")
                    if (!document.template.type.manuallyPublishable)
                        throw BadRequest("Document type is not publishable")

                    val wasUpToDate = tx.isDocumentPublishedContentUpToDate(documentId)
                    tx.publishChildDocument(documentId, clock.now())
                    if (!wasUpToDate) {
                        childDocumentService.schedulePdfGeneration(
                            tx,
                            listOf(documentId),
                            clock.now(),
                        )
                        childDocumentService.scheduleEmailNotification(
                            tx,
                            listOf(documentId),
                            clock.now(),
                        )
                    }
                }
            }
            .also { Audit.ChildDocumentPublish.log(targetId = AuditId(documentId)) }
    }

    data class StatusChangeRequest(
        // needed to ensure idempotency
        val newStatus: DocumentStatus
    )

    @PutMapping("/{documentId}/next-status")
    fun nextDocumentStatus(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable documentId: ChildDocumentId,
        @RequestBody body: StatusChangeRequest,
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.ChildDocument.NEXT_STATUS,
                        documentId,
                    )

                    updateChildDocumentStatusForward(tx, user, clock, documentId, body.newStatus)
                }
            }
            .also {
                Audit.ChildDocumentNextStatus.log(
                    targetId = AuditId(documentId),
                    meta = mapOf("newStatus" to body.newStatus),
                )
                Audit.ChildDocumentPublish.log(targetId = AuditId(documentId))
            }
    }

    private fun updateChildDocumentStatusForward(
        tx: Database.Transaction,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        documentId: ChildDocumentId,
        newStatus: DocumentStatus,
    ) {
        val document = tx.getChildDocument(documentId) ?: throw NotFound()
        val statusTransition =
            validateStatusTransition(
                document = document,
                requestedStatus = newStatus,
                goingForward = true,
            )

        if (document.template.type.decision) {
            tx.changeStatus(documentId, statusTransition, clock.now())
        } else {
            val wasUpToDate = tx.isDocumentPublishedContentUpToDate(documentId)
            tx.changeStatusAndPublish(
                documentId,
                statusTransition,
                clock.now(),
                answeredBy =
                    user.evakaUserId.takeIf {
                        document.template.type == ChildDocumentType.CITIZEN_BASIC &&
                            statusTransition.newStatus == DocumentStatus.COMPLETED
                    },
            )
            if (statusTransition.newStatus == DocumentStatus.CITIZEN_DRAFT || !wasUpToDate) {
                childDocumentService.scheduleEmailNotification(tx, listOf(documentId), clock.now())
            }

            if (!wasUpToDate)
                childDocumentService.schedulePdfGeneration(tx, listOf(documentId), clock.now())
        }

        updateDocumentCaseProcessHistory(
            tx = tx,
            document = document,
            newStatus = statusTransition.newStatus,
            now = clock.now(),
            userId = user.evakaUserId,
        )
    }

    @PutMapping("/{documentId}/prev-status")
    fun prevDocumentStatus(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable documentId: ChildDocumentId,
        @RequestBody body: StatusChangeRequest,
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.ChildDocument.PREV_STATUS,
                        documentId,
                    )

                    val document =
                        tx.getChildDocument(documentId)
                            ?: throw NotFound("Document $documentId not found")

                    if (document.template.validity.end?.isBefore(clock.today()) == true) {
                        throw BadRequest(
                            "Cannot change status of document as template validity period has ended"
                        )
                    }

                    val statusTransition =
                        validateStatusTransition(
                            document = document,
                            requestedStatus = body.newStatus,
                            goingForward = false,
                        )

                    tx.changeStatus(documentId, statusTransition, clock.now())

                    updateDocumentCaseProcessHistory(
                        tx = tx,
                        document = document,
                        newStatus = statusTransition.newStatus,
                        now = clock.now(),
                        userId = user.evakaUserId,
                    )
                }
            }
            .also {
                Audit.ChildDocumentPrevStatus.log(
                    targetId = AuditId(documentId),
                    meta = mapOf("newStatus" to body.newStatus),
                )
            }
    }

    @DeleteMapping("/{documentId}")
    fun deleteDraftDocument(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable documentId: ChildDocumentId,
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.ChildDocument.DELETE,
                        documentId,
                    )
                    deleteProcessByDocumentId(tx, documentId)
                    tx.getChildDocumentKey(documentId)?.also { key ->
                        asyncJobRunner.plan(
                            tx = tx,
                            payloads = listOf(AsyncJob.DeleteChildDocumentPdf(key)),
                            runAt = clock.now(),
                        )
                    }
                    tx.deleteChildDocumentDraft(documentId)
                }
            }
            .also { Audit.ChildDocumentDelete.log(targetId = AuditId(documentId)) }
    }

    @PostMapping("/{documentId}/archive")
    fun planArchiveChildDocument(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable documentId: ChildDocumentId,
        särmäEnabled: Boolean = evakaEnv.särmäEnabled,
    ) {
        if (!särmäEnabled) {
            throw BadRequest("Document archival is not enabled")
        }

        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.ChildDocument.ARCHIVE,
                        documentId,
                    )

                    val document =
                        tx.getChildDocument(documentId)
                            ?: throw NotFound("Document $documentId not found")

                    if (!document.template.archiveExternally) {
                        throw BadRequest("Document template is not marked for external archiving")
                    }
                    if (document.status != DocumentStatus.COMPLETED) {
                        throw BadRequest("Document must be in COMPLETED status to be archived")
                    }

                    asyncJobRunner.plan(
                        tx = tx,
                        payloads = listOf(AsyncJob.ArchiveChildDocument(documentId)),
                        runAt = clock.now(),
                        retryCount = 1,
                    )
                }
            }
            .also { Audit.ChildDocumentArchive.log(targetId = AuditId(documentId)) }
    }

    @GetMapping("/non-completed")
    fun getNonCompletedChildDocumentChildIds(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam templateId: DocumentTemplateId,
        @RequestParam groupId: GroupId,
    ): Set<ChildId> {
        return db.connect { dbc ->
            dbc.read { tx ->
                tx.getNonCompletedChildDocumentChildIds(templateId, groupId, clock.today())
            }
        }
    }

    @GetMapping("/{documentId}/pdf")
    fun downloadChildDocument(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable documentId: ChildDocumentId,
    ): ResponseEntity<Any> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.ChildDocument.DOWNLOAD,
                        documentId,
                    )
                    childDocumentService.getPdfResponse(tx, documentId)
                }
            }
            .also { Audit.ChildDocumentDownload.log(targetId = AuditId(documentId)) }
    }

    data class ProposeChildDocumentDecisionRequest(val decisionMaker: EmployeeId)

    @PostMapping("/{documentId}/propose-decision")
    fun proposeChildDocumentDecision(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable documentId: ChildDocumentId,
        @RequestBody body: ProposeChildDocumentDecisionRequest,
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.ChildDocument.PROPOSE_DECISION,
                        documentId,
                    )

                    val document =
                        tx.getChildDocument(documentId)
                            ?: throw NotFound("Document $documentId not found")
                    if (!document.template.type.decision)
                        throw BadRequest("Document is not a decision")
                    if (document.status != DocumentStatus.DRAFT)
                        throw BadRequest("Document is not in correct status")

                    val validDecisionMakers = tx.getChildDocumentDecisionMakers(documentId)
                    if (validDecisionMakers.none { it.id == body.decisionMaker }) {
                        throw BadRequest("Decision maker is not valid")
                    }

                    tx.setChildDocumentDecisionMaker(documentId, body.decisionMaker)
                    tx.changeStatus(
                        documentId,
                        StatusTransition(
                            currentStatus = DocumentStatus.DRAFT,
                            newStatus = DocumentStatus.DECISION_PROPOSAL,
                        ),
                        clock.now(),
                    )

                    updateDocumentCaseProcessHistory(
                        tx = tx,
                        document = document,
                        newStatus = DocumentStatus.DECISION_PROPOSAL,
                        now = clock.now(),
                        userId = user.evakaUserId,
                    )
                }
            }
            .also { Audit.ChildDocumentProposeDecision.log(targetId = AuditId(documentId)) }
    }

    data class AcceptChildDocumentDecisionRequest(val validity: DateRange)

    @PostMapping("/{documentId}/accept")
    fun acceptChildDocumentDecision(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable documentId: ChildDocumentId,
        @RequestBody body: AcceptChildDocumentDecisionRequest,
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.ChildDocument.ACCEPT_DECISION,
                        documentId,
                    )

                    val document =
                        tx.getChildDocument(documentId)
                            ?: throw NotFound("Document $documentId not found")
                    if (!document.template.type.decision)
                        throw BadRequest("Document is not a decision")
                    if (document.status != DocumentStatus.DECISION_PROPOSAL)
                        throw BadRequest("Document is not in decision proposal status")

                    val placementDaycareId =
                        tx.getPlacementsForChildDuring(
                                childId = document.child.id,
                                start = body.validity.start,
                                end = body.validity.start,
                            )
                            .firstOrNull()
                            ?.unitId

                    tx.insertChildDocumentDecision(
                            status = ChildDocumentDecisionStatus.ACCEPTED,
                            userId = user.evakaUserId,
                            validity = body.validity,
                            daycareId = placementDaycareId,
                        )
                        .also { decisionId ->
                            tx.setChildDocumentDecisionAndPublish(
                                documentId,
                                decisionId,
                                clock.now(),
                            )
                        }

                    childDocumentService.schedulePdfGeneration(tx, listOf(documentId), clock.now())
                    childDocumentService.scheduleEmailNotification(
                        tx,
                        listOf(documentId),
                        clock.now(),
                    )

                    updateDocumentCaseProcessHistory(
                        tx = tx,
                        document = document,
                        newStatus = DocumentStatus.COMPLETED,
                        now = clock.now(),
                        userId = user.evakaUserId,
                    )
                }
            }
            .also { Audit.ChildDocumentAcceptDecision.log(targetId = AuditId(documentId)) }
    }

    @PostMapping("/{documentId}/reject")
    fun rejectChildDocumentDecision(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable documentId: ChildDocumentId,
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.ChildDocument.REJECT_DECISION,
                        documentId,
                    )

                    val document =
                        tx.getChildDocument(documentId)
                            ?: throw NotFound("Document $documentId not found")
                    if (!document.template.type.decision)
                        throw BadRequest("Document is not a decision")
                    if (document.status != DocumentStatus.DECISION_PROPOSAL)
                        throw BadRequest("Document is not in decision proposal status")

                    tx.insertChildDocumentDecision(
                            status = ChildDocumentDecisionStatus.REJECTED,
                            userId = user.evakaUserId,
                            validity = null,
                            daycareId = null,
                        )
                        .also { decisionId ->
                            tx.setChildDocumentDecisionAndPublish(
                                documentId,
                                decisionId,
                                clock.now(),
                            )
                        }

                    childDocumentService.schedulePdfGeneration(tx, listOf(documentId), clock.now())
                    childDocumentService.scheduleEmailNotification(
                        tx,
                        listOf(documentId),
                        clock.now(),
                    )

                    updateDocumentCaseProcessHistory(
                        tx = tx,
                        document = document,
                        newStatus = DocumentStatus.COMPLETED,
                        now = clock.now(),
                        userId = user.evakaUserId,
                    )
                }
            }
            .also { Audit.ChildDocumentRejectDecision.log(targetId = AuditId(documentId)) }
    }

    @PostMapping("/{documentId}/annul")
    fun annulChildDocumentDecision(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable documentId: ChildDocumentId,
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.ChildDocument.ANNUL_DECISION,
                        documentId,
                    )

                    val document =
                        tx.getChildDocument(documentId)
                            ?: throw NotFound("Document $documentId not found")
                    if (!document.template.type.decision)
                        throw BadRequest("Document is not a decision")
                    if (
                        document.status != DocumentStatus.COMPLETED ||
                            document.decision == null ||
                            document.decision.status != ChildDocumentDecisionStatus.ACCEPTED
                    )
                        throw BadRequest("Only accepted decision can be annulled")

                    tx.annulChildDocumentDecision(
                        decisionId = document.decision.id,
                        userId = user.evakaUserId,
                        now = clock.now(),
                    )
                }
            }
            .also { Audit.ChildDocumentAnnulDecision.log(targetId = AuditId(documentId)) }
    }

    data class UpdateChildDocumentDecisionValidityRequest(val newValidity: DateRange)

    @PostMapping("/{documentId}/validity")
    fun updateChildDocumentDecisionValidity(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable documentId: ChildDocumentId,
        @RequestBody body: UpdateChildDocumentDecisionValidityRequest,
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.ChildDocument.UPDATE_DECISION_VALIDITY,
                        documentId,
                    )

                    val document =
                        tx.getChildDocument(documentId)
                            ?: throw NotFound("Document $documentId not found")
                    if (!document.template.type.decision)
                        throw BadRequest("Document is not a decision")
                    if (
                        document.status != DocumentStatus.COMPLETED ||
                            document.decision?.status != ChildDocumentDecisionStatus.ACCEPTED
                    )
                        throw BadRequest("Only accepted decision can have validity updated")

                    tx.setChildDocumentDecisionValidity(
                        decisionId = document.decision.id,
                        validity = body.newValidity,
                    )
                }
            }
            .also { Audit.ChildDocumentUpdateDecisionValidity.log(targetId = AuditId(documentId)) }
    }
}

fun validateContentAgainstTemplate(
    documentContent: DocumentContent,
    templateContent: DocumentTemplateContent,
) {
    val questions = templateContent.sections.flatMap { it.questions }
    val valid =
        documentContent.answers.all { answeredQuestion ->
            questions.any { question ->
                question.id == answeredQuestion.questionId &&
                    question.type == answeredQuestion.type &&
                    answeredQuestion.isStructurallyValid(question)
            }
        }
    if (!valid) {
        throw BadRequest("Answered questions and template do not match")
    }
}

fun validateStatusTransition(
    document: ChildDocumentDetails,
    requestedStatus: DocumentStatus,
    goingForward: Boolean, // false = backwards
): StatusTransition {
    val statusList = document.template.type.statuses
    val currentIndex = statusList.indexOf(document.status)
    if (currentIndex < 0) {
        throw IllegalStateException("document ${document.id} is in invalid status")
    }
    val newStatus =
        statusList.getOrNull(if (goingForward) currentIndex + 1 else currentIndex - 1)
            ?: throw BadRequest("Already in the ${if (goingForward) "final" else "first"} status")
    if (newStatus != requestedStatus) {
        throw Conflict("Idempotency issue: statuses do not match")
    }

    if (document.template.type.decision) {
        if (goingForward && newStatus == DocumentStatus.DECISION_PROPOSAL)
            throw BadRequest(
                "Decision document cannot be moved to DECISION_PROPOSAL using normal status transitions. Please use the separate propose-decision endpoint."
            )
        if (goingForward && newStatus == DocumentStatus.COMPLETED)
            throw BadRequest(
                "Decision document cannot be marked as completed using normal status transitions. Please use the separate accept/reject endpoints."
            )

        if (!goingForward && document.status == DocumentStatus.COMPLETED)
            throw BadRequest(
                "Decision can not be reverted using normal status transitions. Please use the separate annul endpoint."
            )
    }

    return StatusTransition(currentStatus = document.status, newStatus = newStatus)
}
