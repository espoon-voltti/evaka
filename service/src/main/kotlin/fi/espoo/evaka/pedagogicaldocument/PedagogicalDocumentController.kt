// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pedagogicaldocument

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PedagogicalDocumentId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.result.RowView
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/pedagogical-document")
class PedagogicalDocumentController(
    private val accessControl: AccessControl,
    private val pedagogicalDocumentNotificationService: PedagogicalDocumentNotificationService
) {
    @PostMapping
    fun createPedagogicalDocument(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestBody body: PedagogicalDocumentPostBody
    ): PedagogicalDocument {
        Audit.PedagogicalDocumentUpdate.log(body.childId)
        accessControl.requirePermissionFor(user, Action.Child.CREATE_PEDAGOGICAL_DOCUMENT, body.childId.raw)
        return db.transaction { tx ->
            val document = tx.createDocument(user, body)
            pedagogicalDocumentNotificationService.scheduleSendingNotifications(tx, document.id, document.childId)
            document
        }
    }

    @PutMapping("/{documentId}")
    fun updatePedagogicalDocument(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable documentId: PedagogicalDocumentId,
        @RequestBody body: PedagogicalDocumentPostBody
    ): PedagogicalDocument {
        Audit.PedagogicalDocumentUpdate.log(documentId)
        accessControl.requirePermissionFor(user, Action.PedagogicalDocument.UPDATE, documentId)
        return db.transaction { it.updateDocument(user, body, documentId) }
    }

    @GetMapping("/child/{childId}")
    fun getChildPedagogicalDocuments(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable childId: ChildId
    ): List<PedagogicalDocument> {
        Audit.PedagogicalDocumentRead.log(childId)
        accessControl.requirePermissionFor(user, Action.Child.READ_PEDAGOGICAL_DOCUMENTS, childId.raw)
        return db.read { it.findPedagogicalDocumentsByChild(childId) }
    }

    @DeleteMapping("/{documentId}")
    fun deletePedagogicalDocument(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable documentId: PedagogicalDocumentId
    ) {
        Audit.PedagogicalDocumentUpdate.log(documentId)
        accessControl.requirePermissionFor(user, Action.PedagogicalDocument.DELETE, documentId)
        db.transaction { it.deleteDocument(documentId) }
    }
}

data class Attachment(
    val id: AttachmentId,
    val name: String,
    val contentType: String
)

data class PedagogicalDocument(
    val id: PedagogicalDocumentId,
    val childId: ChildId,
    val description: String?,
    val attachment: Attachment?,
    val created: HelsinkiDateTime,
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
            INSERT INTO pedagogical_document(child_id, created_by)
            VALUES (:child_id, :created_by)
            RETURNING *
        """.trimIndent()
    )
        .bind("child_id", body.childId)
        .bind("created_by", user.id)
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
        .bind("updated_by", user.id)
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
                pd.updated,
                a.id attachment_id,
                a.name attachment_name,
                a.content_type attachment_content_type
            FROM pedagogical_document pd
            LEFT JOIN attachment a ON a.pedagogical_document_id = pd.id
            WHERE child_id = :child_id
        """.trimIndent()
    )
        .bind("child_id", childId)
        .map { row -> mapPedagogicalDocument(row) }
        .filterNotNull()
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

fun mapPedagogicalDocument(row: RowView): PedagogicalDocument? {
    val id: PedagogicalDocumentId = row.mapColumn("id") ?: return null
    val hasAttachment: Boolean = row.mapColumn<AttachmentId?>("attachment_id") != null

    return PedagogicalDocument(
        id = id,
        childId = row.mapColumn("child_id"),
        description = row.mapColumn("description"),
        attachment = if (hasAttachment) Attachment(
            id = row.mapColumn("attachment_id"),
            name = row.mapColumn("attachment_name"),
            contentType = row.mapColumn("attachment_content_type"),
        ) else null,
        created = row.mapColumn("created"),
        updated = row.mapColumn("updated")
    )
}
