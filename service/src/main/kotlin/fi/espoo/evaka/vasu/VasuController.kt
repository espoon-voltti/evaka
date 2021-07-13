// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vasu

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
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
    private val acl: AccessControlList
) {

    data class CreateDocumentRequest(
        val childId: UUID,
        val templateId: UUID
    )
    @PostMapping("/vasu")
    fun createDocument(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestBody body: CreateDocumentRequest
    ): UUID {
        Audit.VasuDocumentCreate.log(body.childId)
        acl.getRolesForChild(user, body.childId).requireOneOfRoles(
            UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.SPECIAL_EDUCATION_TEACHER, UserRole.GROUP_STAFF
        )

        return db.transaction { tx ->
            tx.getVasuTemplate(body.templateId)?.let { template ->
                if (!template.valid.includes(HelsinkiDateTime.now().toLocalDate())) {
                    throw BadRequest("Template is not currently valid")
                }
            } ?: throw NotFound("template ${body.templateId} not found")

            tx.insertVasuDocument(body.childId, body.templateId)
        }
    }

    @GetMapping("/vasu/{id}")
    fun getDocument(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable id: UUID
    ): VasuDocument {
        Audit.VasuDocumentRead.log(id)
        acl.getRolesForVasuDocument(user, id).requireOneOfRoles(
            UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.SPECIAL_EDUCATION_TEACHER, UserRole.GROUP_STAFF
        )

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
        @PathVariable id: UUID,
        @RequestBody body: UpdateDocumentRequest
    ) {
        Audit.VasuDocumentUpdate.log(id)
        acl.getRolesForVasuDocument(user, id).requireOneOfRoles(
            UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.SPECIAL_EDUCATION_TEACHER, UserRole.GROUP_STAFF
        )

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
        if (!vasu.content.matchesStructurally(body.content))
            throw BadRequest("Vasu document structure does not match template", "DOCUMENT_DOES_NOT_MATCH_TEMPLATE")
    }

    @GetMapping("/children/{childId}/vasu-summaries")
    fun getVasuSummariesByChild(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable childId: UUID
    ): List<VasuDocumentSummary> {
        Audit.ChildVasuDocumentsRead.log(childId)
        acl.getRolesForChild(user, childId).requireOneOfRoles(
            UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.SPECIAL_EDUCATION_TEACHER, UserRole.GROUP_STAFF
        )

        return db.read { tx -> tx.getVasuDocumentSummaries(childId) }
    }

    data class ChangeDocumentStateRequest(
        val eventType: VasuDocumentEventType
    )

    @PostMapping("/vasu/{id}/update-state")
    fun updateDocumentState(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable id: UUID,
        @RequestBody body: ChangeDocumentStateRequest
    ) {
        Audit.VasuDocumentEventCreate.log(id)
        acl.getRolesForVasuDocument(user, id).requireOneOfRoles(
            UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.SPECIAL_EDUCATION_TEACHER, UserRole.GROUP_STAFF
        )

        val events = if (body.eventType in listOf(MOVED_TO_READY, MOVED_TO_REVIEWED)) {
            listOf(PUBLISHED, body.eventType)
        } else {
            listOf(body.eventType)
        }

        db.transaction { tx ->
            val currentState = tx.getVasuDocumentMaster(id)?.getState() ?: throw NotFound("Vasu was not found")
            validateStateTransition(eventType = body.eventType, currentState = currentState, user = user)

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

    private fun validateStateTransition(eventType: VasuDocumentEventType, currentState: VasuDocumentState, user: AuthenticatedUser) {
        when (eventType) {
            PUBLISHED -> currentState !== VasuDocumentState.CLOSED
            MOVED_TO_READY -> currentState === VasuDocumentState.DRAFT
            RETURNED_TO_READY -> user.hasOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR)
            MOVED_TO_REVIEWED -> currentState === VasuDocumentState.READY
            RETURNED_TO_REVIEWED -> user.hasOneOfRoles(UserRole.ADMIN)
            MOVED_TO_CLOSED -> user.hasOneOfRoles(UserRole.ADMIN)
        }.let { valid -> if (!valid) throw Conflict("Invalid or unauthorized state transition") }
    }
}
