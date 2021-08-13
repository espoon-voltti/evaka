// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vasu

import fi.espoo.evaka.Audit
import fi.espoo.evaka.application.utils.exhaust
import fi.espoo.evaka.shared.VasuDocumentId
import fi.espoo.evaka.shared.VasuTemplateId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.vasu.VasuDocumentEventType.MOVED_TO_CLOSED
import fi.espoo.evaka.vasu.VasuDocumentEventType.MOVED_TO_READY
import fi.espoo.evaka.vasu.VasuDocumentEventType.MOVED_TO_REVIEWED
import fi.espoo.evaka.vasu.VasuDocumentEventType.PUBLISHED
import fi.espoo.evaka.vasu.VasuDocumentEventType.RETURNED_TO_READY
import fi.espoo.evaka.vasu.VasuDocumentEventType.RETURNED_TO_REVIEWED
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class VasuController(
    private val accessControl: AccessControl
) {

    data class CreateDocumentRequest(
        val templateId: VasuTemplateId
    )
    @PostMapping("/children/{childId}/vasu")
    fun createDocument(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable childId: UUID,
        @RequestBody body: CreateDocumentRequest
    ): VasuDocumentId {
        Audit.VasuDocumentCreate.log(childId)
        accessControl.requirePermissionFor(user, Action.Child.CREATE_VASU_DOCUMENT, childId)

        return db.transaction { tx ->
            if (tx.getVasuDocumentSummaries(childId).any { it.getState() != VasuDocumentState.CLOSED }) {
                throw Conflict("Cannot open a new vasu document while another is still active")
            }

            tx.getVasuTemplate(body.templateId)?.let { template ->
                if (!template.valid.includes(HelsinkiDateTime.now().toLocalDate())) {
                    throw BadRequest("Template is not currently valid")
                }
            } ?: throw NotFound("template ${body.templateId} not found")

            tx.insertVasuDocument(childId, body.templateId)
        }
    }

    @GetMapping("/children/{childId}/vasu-summaries")
    fun getVasuSummariesByChild(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable childId: UUID
    ): List<VasuDocumentSummary> {
        Audit.ChildVasuDocumentsRead.log(childId)
        accessControl.requirePermissionFor(user, Action.Child.READ_VASU_DOCUMENT, childId)

        return db.read { tx -> tx.getVasuDocumentSummaries(childId) }
    }

    data class ChangeDocumentStateRequest(
        val eventType: VasuDocumentEventType
    )

    @GetMapping("/vasu/{id}")
    fun getDocument(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable id: VasuDocumentId
    ): VasuDocument {
        Audit.VasuDocumentRead.log(id)
        accessControl.requirePermissionFor(user, Action.VasuDocument.READ, id)

        return db.read { tx ->
            tx.getVasuDocumentMaster(id) ?: throw NotFound("template $id not found")
        }
    }

    data class UpdateDocumentRequest(
        val content: VasuContent,
        val authorsContent: AuthorsContent,
        val vasuDiscussionContent: VasuDiscussionContent,
        val evaluationDiscussionContent: EvaluationDiscussionContent
    )
    @PutMapping("/vasu/{id}")
    fun putDocument(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable id: VasuDocumentId,
        @RequestBody body: UpdateDocumentRequest
    ) {
        Audit.VasuDocumentUpdate.log(id)
        accessControl.requirePermissionFor(user, Action.VasuDocument.UPDATE, id)

        db.transaction { tx ->
            val vasu = tx.getVasuDocumentMaster(id) ?: throw NotFound("vasu $id not found")
            validateVasuDocumentUpdate(vasu, body)
            tx.updateVasuDocumentMaster(
                id,
                body.content,
                body.authorsContent,
                body.vasuDiscussionContent,
                body.evaluationDiscussionContent
            )
        }
    }

    private fun validateVasuDocumentUpdate(vasu: VasuDocument, body: UpdateDocumentRequest) {
        if (vasu.getState() == VasuDocumentState.CLOSED)
            throw BadRequest("Closed vasu document cannot be edited", "CANNOT_EDIT_CLOSED_DOCUMENT")

        if (!vasu.content.matchesStructurally(body.content))
            throw BadRequest("Vasu document structure does not match template", "DOCUMENT_DOES_NOT_MATCH_TEMPLATE")
    }

    @PostMapping("/vasu/{id}/update-state")
    fun updateDocumentState(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable id: VasuDocumentId,
        @RequestBody body: ChangeDocumentStateRequest
    ) {
        Audit.VasuDocumentEventCreate.log(id)

        val events = if (body.eventType in listOf(MOVED_TO_READY, MOVED_TO_REVIEWED)) {
            listOf(PUBLISHED, body.eventType)
        } else {
            listOf(body.eventType)
        }

        events.forEach { eventType ->
            when (eventType) {
                PUBLISHED -> accessControl.requirePermissionFor(user, Action.VasuDocument.EVENT_PUBLISHED, id)
                MOVED_TO_READY -> accessControl.requirePermissionFor(user, Action.VasuDocument.EVENT_MOVED_TO_READY, id)
                RETURNED_TO_READY -> accessControl.requirePermissionFor(user, Action.VasuDocument.EVENT_RETURNED_TO_READY, id)
                MOVED_TO_REVIEWED -> accessControl.requirePermissionFor(user, Action.VasuDocument.EVENT_MOVED_TO_REVIEWED, id)
                RETURNED_TO_REVIEWED -> accessControl.requirePermissionFor(user, Action.VasuDocument.EVENT_RETURNED_TO_REVIEWED, id)
                MOVED_TO_CLOSED -> accessControl.requirePermissionFor(user, Action.VasuDocument.EVENT_MOVED_TO_CLOSED, id)
            }.exhaust()
        }

        db.transaction { tx ->
            val currentState = tx.getVasuDocumentMaster(id)?.getState() ?: throw NotFound("Vasu was not found")
            validateStateTransition(eventType = body.eventType, currentState = currentState)

            if (events.contains(PUBLISHED)) {
                tx.publishVasuDocument(id)
            }

            events.forEach { eventType ->
                tx.insertVasuDocumentEvent(
                    documentId = id,
                    eventType = eventType,
                    employeeId = user.id
                )
            }
        }
    }

    private fun validateStateTransition(eventType: VasuDocumentEventType, currentState: VasuDocumentState) {
        when (eventType) {
            PUBLISHED -> currentState !== VasuDocumentState.CLOSED
            MOVED_TO_READY -> currentState === VasuDocumentState.DRAFT
            RETURNED_TO_READY -> currentState === VasuDocumentState.REVIEWED
            MOVED_TO_REVIEWED -> currentState === VasuDocumentState.READY
            RETURNED_TO_REVIEWED -> currentState === VasuDocumentState.CLOSED
            MOVED_TO_CLOSED -> true
        }
            .exhaust()
            .let { valid -> if (!valid) throw Conflict("Invalid state transition") }
    }
}
