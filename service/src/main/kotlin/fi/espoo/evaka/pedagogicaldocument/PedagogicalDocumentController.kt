// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pedagogicaldocument

import fi.espoo.evaka.Audit
import fi.espoo.evaka.ForceCodeGenType
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PedagogicalDocumentId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.mapper.PropagateNull
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime

@RestController
@RequestMapping("/pedagogical-document")
class PedagogicalDocumentController(
    private val accessControl: AccessControl,
    private val pedagogicalDocumentNotificationService: PedagogicalDocumentNotificationService
) {
    @PostMapping
    fun createPedagogicalDocument(
        db: Database,
        user: AuthenticatedUser,
        @RequestBody body: PedagogicalDocumentPostBody
    ): PedagogicalDocument {
        Audit.PedagogicalDocumentUpdate.log(body.childId)
        accessControl.requirePermissionFor(user, Action.Child.CREATE_PEDAGOGICAL_DOCUMENT, body.childId)
        return db.connect {
            it.transaction { tx ->
                tx.createDocument(user, body).also {
                    pedagogicalDocumentNotificationService.maybeScheduleEmailNotification(tx, it.id)
                }
            }
        }
    }

    @PutMapping("/{documentId}")
    fun updatePedagogicalDocument(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable documentId: PedagogicalDocumentId,
        @RequestBody body: PedagogicalDocumentPostBody
    ): PedagogicalDocument {
        Audit.PedagogicalDocumentUpdate.log(documentId)
        accessControl.requirePermissionFor(user, Action.PedagogicalDocument.UPDATE, documentId)
        return db.connect {
            it.transaction { tx ->
                tx.updateDocument(user, body, documentId).also {
                    pedagogicalDocumentNotificationService.maybeScheduleEmailNotification(tx, it.id)
                }
            }
        }
    }

    @GetMapping("/child/{childId}")
    fun getChildPedagogicalDocuments(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable childId: ChildId
    ): List<PedagogicalDocument> {
        Audit.PedagogicalDocumentRead.log(childId)
        accessControl.requirePermissionFor(user, Action.Child.READ_PEDAGOGICAL_DOCUMENTS, childId)
        return db.connect { dbc ->
            dbc.read {
                it.findPedagogicalDocumentsByChild(childId)
            }
        }
    }

    @DeleteMapping("/{documentId}")
    fun deletePedagogicalDocument(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable documentId: PedagogicalDocumentId
    ) {
        Audit.PedagogicalDocumentUpdate.log(documentId)
        accessControl.requirePermissionFor(user, Action.PedagogicalDocument.DELETE, documentId)
        return db.connect { dbc ->
            dbc.transaction {
                it.deleteDocument(documentId)
            }
        }
    }
}

data class Attachment(
    @PropagateNull
    val id: AttachmentId,
    val name: String,
    val contentType: String
)

data class PedagogicalDocument(
    val id: PedagogicalDocumentId,
    val childId: ChildId,
    val description: String,
    val attachments: List<Attachment> = emptyList(),
    @ForceCodeGenType(OffsetDateTime::class)
    val created: HelsinkiDateTime,
    @ForceCodeGenType(OffsetDateTime::class)
    val updated: HelsinkiDateTime
)

data class PedagogicalDocumentPostBody(
    val childId: ChildId,
    val description: String,
)

private fun Database.Transaction.createDocument(
    user: AuthenticatedUser,
    body: PedagogicalDocumentPostBody
): PedagogicalDocument {
    return this.createUpdate(
        """
            INSERT INTO pedagogical_document(child_id, created_by, description)
            VALUES (:child_id, :created_by, :description)
            RETURNING *
        """.trimIndent()
    )
        .bind("child_id", body.childId)
        .bind("description", body.description)
        .bind("created_by", user.evakaUserId)
        .executeAndReturnGeneratedKeys()
        .mapTo<PedagogicalDocument>()
        .first()
}

private fun Database.Transaction.updateDocument(
    user: AuthenticatedUser,
    body: PedagogicalDocumentPostBody,
    documentId: PedagogicalDocumentId
): PedagogicalDocument {
    return this.createUpdate(
        """
            UPDATE pedagogical_document
            SET description = :description, 
                updated_by = :updated_by
            WHERE id = :id AND child_id = :child_id
        """.trimIndent()
    )
        .bind("id", documentId)
        .bind("child_id", body.childId)
        .bind("description", body.description)
        .bind("updated_by", user.evakaUserId)
        .executeAndReturnGeneratedKeys()
        .mapTo<PedagogicalDocument>()
        .first()
}

private fun Database.Read.findPedagogicalDocumentsByChild(
    childId: ChildId
): List<PedagogicalDocument> {
    return this.createQuery(
        """
            SELECT 
                pd.id,
                pd.child_id,
                pd.description,
                pd.created,
                pd.updated
            FROM pedagogical_document pd
            WHERE child_id = :child_id
        """.trimIndent()
    )
        .bind("child_id", childId)
        .mapTo<PedagogicalDocument>()
        .list()
        .map { pd -> pd.copy(attachments = getPedagogicalDocumentAttachments(pd.id)) }
}

private fun Database.Transaction.deleteDocument(
    documentId: PedagogicalDocumentId
) {
    this.createUpdate("DELETE FROM pedagogical_document_read WHERE pedagogical_document_id = :document_id")
        .bind("document_id", documentId)
        .execute()
    this.createUpdate("DELETE FROM attachment WHERE pedagogical_document_id = :document_id")
        .bind("document_id", documentId)
        .execute()
    this.createUpdate("DELETE FROM pedagogical_document WHERE id = :document_id")
        .bind("document_id", documentId)
        .execute()
}
