// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document.childdocument

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.document.DocumentTemplateContent
import fi.espoo.evaka.document.getTemplate
import fi.espoo.evaka.pis.listPersonByDuplicateOf
import fi.espoo.evaka.process.ArchivedProcessState
import fi.espoo.evaka.process.deleteProcessByDocumentId
import fi.espoo.evaka.process.insertProcess
import fi.espoo.evaka.process.insertProcessHistoryRow
import fi.espoo.evaka.process.updateDocumentProcessHistory
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Conflict
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
@RequestMapping(
    "/child-documents", // deprecated
    "/employee/child-documents"
)
class ChildDocumentController(
    private val accessControl: AccessControl,
    private val childDocumentService: ChildDocumentService,
    private val featureConfig: FeatureConfig
) {
    @PostMapping
    fun createDocument(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: ChildDocumentCreateRequest
    ): ChildDocumentId =
        db
            .connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Child.CREATE_CHILD_DOCUMENT,
                        body.childId
                    )

                    val template =
                        tx.getTemplate(body.templateId)?.also {
                            if (!it.published || !it.validity.includes(clock.today())) {
                                throw BadRequest("Invalid template")
                            }
                        } ?: throw NotFound()

                    val sameTemplateAlreadyStarted =
                        tx.getChildDocuments(body.childId).any {
                            it.templateId == template.id && it.status != DocumentStatus.COMPLETED
                        }
                    if (sameTemplateAlreadyStarted) {
                        throw Conflict("Child already has incomplete document of the same template")
                    }

                    val now = clock.now()
                    val processId =
                        template.processDefinitionNumber?.let { processDefinitionNumber ->
                            // guaranteed to be not null when processDefinitionNumber is not null by
                            // db constraint
                            val archiveDurationMonths = template.archiveDurationMonths!!
                            tx
                                .insertProcess(
                                    processDefinitionNumber = processDefinitionNumber,
                                    year = now.year,
                                    organization = featureConfig.archiveMetadataOrganization,
                                    archiveDurationMonths = archiveDurationMonths
                                ).id
                                .also { processId ->
                                    tx.insertProcessHistoryRow(
                                        processId = processId,
                                        state = ArchivedProcessState.INITIAL,
                                        now = now,
                                        userId = user.evakaUserId
                                    )
                                }
                        }

                    tx.insertChildDocument(
                        document = body,
                        now = now,
                        userId = user.id,
                        processId = processId
                    )
                }
            }.also { Audit.ChildDocumentCreate.log(targetId = AuditId(it)) }

    @GetMapping
    fun getDocuments(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam childId: PersonId
    ): List<ChildDocumentSummaryWithPermittedActions> =
        db
            .connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Child.READ_CHILD_DOCUMENT,
                        childId
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
                            documents.map { it.id }
                        )
                    documents
                        .mapNotNull { document ->
                            permittedActions[document.id]
                                ?.takeIf { it.contains(Action.ChildDocument.READ) }
                                ?.let { ChildDocumentSummaryWithPermittedActions(document, it) }
                        }.filter { user.isAdmin || !it.data.type.isMigrated() }
                }
            }.also { Audit.ChildDocumentRead.log(targetId = AuditId(childId)) }

    data class ChildDocumentSummaryWithPermittedActions(
        val data: ChildDocumentSummary,
        val permittedActions: Set<Action.ChildDocument>
    )

    @GetMapping("/{documentId}")
    fun getDocument(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable documentId: ChildDocumentId
    ): ChildDocumentWithPermittedActions =
        db
            .connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.ChildDocument.READ,
                        documentId
                    )

                    val document =
                        tx.getChildDocument(documentId)?.takeIf {
                            user.isAdmin || !it.template.type.isMigrated()
                        } ?: throw NotFound("Document $documentId not found")

                    val permittedActions =
                        accessControl.getPermittedActions<ChildDocumentId, Action.ChildDocument>(
                            tx,
                            user,
                            clock,
                            documentId
                        )

                    ChildDocumentWithPermittedActions(
                        data = document,
                        permittedActions = permittedActions
                    )
                }
            }.also { Audit.ChildDocumentRead.log(targetId = AuditId(documentId)) }

    @PutMapping("/{documentId}/content")
    fun updateDocumentContent(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable documentId: ChildDocumentId,
        @RequestBody body: DocumentContent
    ) {
        db.connect { dbc ->
            dbc
                .transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.ChildDocument.UPDATE,
                        documentId
                    )
                    val document =
                        tx.getChildDocument(documentId)
                            ?: throw NotFound("Document $documentId not found")

                    if (!document.status.editable) {
                        throw BadRequest("Cannot update contents of document in this status")
                    }

                    validateContentAgainstTemplate(body, document.template.content)

                    tx.getCurrentWriteLock(documentId, clock.now())?.also { lock ->
                        if (lock.modifiedBy != user.id) {
                            throw Conflict(
                                message = "Did not own the lock on the document",
                                errorCode = "invalid-lock"
                            )
                        }
                    }

                    tx.updateChildDocumentContent(
                        documentId,
                        document.status,
                        body,
                        clock.now(),
                        user.id
                    )
                }.also { Audit.ChildDocumentUpdateContent.log(targetId = AuditId(documentId)) }
        }
    }

    data class DocumentLockResponse(
        val lockTakenSuccessfully: Boolean,
        val currentLock: DocumentWriteLock
    )

    @PutMapping("/{documentId}/lock")
    fun takeDocumentWriteLock(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable documentId: ChildDocumentId
    ): DocumentLockResponse =
        db
            .connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.ChildDocument.UPDATE,
                        documentId
                    )
                    val success = tx.tryTakeWriteLock(documentId, clock.now(), user.id)
                    val currentLock =
                        tx.getCurrentWriteLock(documentId, clock.now())
                            ?: throw IllegalStateException("lock should exist now")
                    DocumentLockResponse(
                        lockTakenSuccessfully = success && currentLock.modifiedBy == user.id,
                        currentLock = currentLock
                    )
                }
            }.also { Audit.ChildDocumentTryTakeLockOnContent.log(targetId = AuditId(documentId)) }

    private fun validateContentAgainstTemplate(
        documentContent: DocumentContent,
        templateContent: DocumentTemplateContent
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

    @PutMapping("/{documentId}/publish")
    fun publishDocument(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable documentId: ChildDocumentId
    ) {
        db
            .connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.ChildDocument.PUBLISH,
                        documentId
                    )
                    val wasUpToDate = tx.isDocumentPublishedContentUpToDate(documentId)
                    tx.publishChildDocument(documentId, clock.now())
                    if (!wasUpToDate) {
                        childDocumentService.schedulePdfGeneration(
                            tx,
                            listOf(documentId),
                            clock.now()
                        )
                        childDocumentService.scheduleEmailNotification(
                            tx,
                            listOf(documentId),
                            clock.now()
                        )
                    }
                }
            }.also { Audit.ChildDocumentPublish.log(targetId = AuditId(documentId)) }
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
        @RequestBody body: StatusChangeRequest
    ) {
        db
            .connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.ChildDocument.NEXT_STATUS,
                        documentId
                    )
                    val statusTransition =
                        validateStatusTransition(
                            tx = tx,
                            documentId = documentId,
                            requestedStatus = body.newStatus,
                            goingForward = true
                        )

                    val wasUpToDate = tx.isDocumentPublishedContentUpToDate(documentId)
                    tx.changeStatusAndPublish(documentId, statusTransition, clock.now())
                    if (!wasUpToDate) {
                        childDocumentService.schedulePdfGeneration(
                            tx,
                            listOf(documentId),
                            clock.now()
                        )
                        childDocumentService.scheduleEmailNotification(
                            tx,
                            listOf(documentId),
                            clock.now()
                        )
                    }
                    updateDocumentProcessHistory(
                        tx = tx,
                        documentId = documentId,
                        newStatus = statusTransition.newStatus,
                        now = clock.now(),
                        userId = user.evakaUserId
                    )
                }
            }.also {
                Audit.ChildDocumentNextStatus.log(
                    targetId = AuditId(documentId),
                    meta = mapOf("newStatus" to body.newStatus)
                )
                Audit.ChildDocumentPublish.log(targetId = AuditId(documentId))
            }
    }

    @PutMapping("/{documentId}/prev-status")
    fun prevDocumentStatus(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable documentId: ChildDocumentId,
        @RequestBody body: StatusChangeRequest
    ) {
        db
            .connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.ChildDocument.PREV_STATUS,
                        documentId
                    )
                    val statusTransition =
                        validateStatusTransition(
                            tx = tx,
                            documentId = documentId,
                            requestedStatus = body.newStatus,
                            goingForward = false
                        )
                    tx.changeStatus(documentId, statusTransition, clock.now())
                    updateDocumentProcessHistory(
                        tx = tx,
                        documentId = documentId,
                        newStatus = statusTransition.newStatus,
                        now = clock.now(),
                        userId = user.evakaUserId
                    )
                }
            }.also {
                Audit.ChildDocumentPrevStatus.log(
                    targetId = AuditId(documentId),
                    meta = mapOf("newStatus" to body.newStatus)
                )
            }
    }

    @DeleteMapping("/{documentId}")
    fun deleteDraftDocument(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable documentId: ChildDocumentId
    ) {
        db
            .connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.ChildDocument.DELETE,
                        documentId
                    )
                    deleteProcessByDocumentId(tx, documentId)
                    tx.deleteChildDocumentDraft(documentId)
                }
            }.also { Audit.ChildDocumentDelete.log(targetId = AuditId(documentId)) }
    }

    @GetMapping("/{documentId}/pdf")
    fun downloadChildDocument(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable documentId: ChildDocumentId
    ): ResponseEntity<Any> =
        db
            .connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.ChildDocument.DOWNLOAD,
                        documentId
                    )
                    childDocumentService.getPdfResponse(tx, documentId)
                }
            }.also { Audit.ChildDocumentDownload.log(targetId = AuditId(documentId)) }
}
