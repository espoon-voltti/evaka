// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document.childdocument

import fi.espoo.evaka.Audit
import fi.espoo.evaka.document.DocumentTemplateContent
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
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
@RequestMapping("/child-documents")
class ChildDocumentController(private val accessControl: AccessControl) {
    @PostMapping
    fun createDocument(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestBody body: ChildDocumentCreateRequest
    ): ChildDocumentId {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Child.CREATE_CHILD_DOCUMENT,
                        body.childId
                    )
                    tx.insertChildDocument(body)
                }
            }
            .also { Audit.ChildDocumentCreate.log(targetId = it) }
    }

    @GetMapping
    fun getDocuments(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestParam(required = true) childId: PersonId
    ): List<ChildDocumentSummary> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Child.READ_CHILD_DOCUMENT,
                        childId
                    )
                    tx.getChildDocuments(childId)
                }
            }
            .also { Audit.ChildDocumentRead.log(targetId = childId) }
    }

    @GetMapping("/{documentId}")
    fun getDocument(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable documentId: ChildDocumentId
    ): ChildDocumentDetails {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.ChildDocument.READ,
                        documentId
                    )
                    tx.getChildDocument(documentId)
                }
            }
            ?.also { Audit.ChildDocumentRead.log(targetId = documentId) }
            ?: throw NotFound("Document $documentId not found")
    }

    @PutMapping("/{documentId}/content")
    fun updateDraftDocumentContent(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable documentId: ChildDocumentId,
        @RequestBody body: DocumentContent
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
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

                    if (document.publishedAt != null)
                        throw BadRequest("Cannot update contents of published document")

                    validateContentAgainstTemplate(body, document.template.content)

                    tx.updateDraftChildDocumentContent(documentId, body)
                }
                .also { Audit.ChildDocumentUpdateContent.log(targetId = documentId) }
        }
    }

    private fun validateContentAgainstTemplate(
        documentContent: DocumentContent,
        templateContent: DocumentTemplateContent
    ) {
        val questions = templateContent.sections.flatMap { it.questions }
        val valid =
            documentContent.answers.all { answeredQuestion ->
                questions.any { question ->
                    question.id == answeredQuestion.questionId &&
                        question.type == answeredQuestion.type
                }
            }
        if (!valid) {
            throw BadRequest("Answered questions and template do not match")
        }
    }

    @PutMapping("/{documentId}/publish")
    fun publishDocument(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable documentId: ChildDocumentId
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.ChildDocument.PUBLISH,
                        documentId
                    )
                    tx.publishChildDocument(documentId, clock.now())
                }
            }
            .also { Audit.ChildDocumentPublish.log(targetId = documentId) }
    }

    @DeleteMapping("/{documentId}")
    fun deleteDraftDocument(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable documentId: ChildDocumentId
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.ChildDocument.DELETE,
                        documentId
                    )
                    tx.deleteChildDocumentDraft(documentId)
                }
            }
            .also { Audit.ChildDocumentDelete.log(targetId = documentId) }
    }
}
