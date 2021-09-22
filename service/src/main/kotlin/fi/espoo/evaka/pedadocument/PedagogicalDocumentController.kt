// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pedadocument

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PedagogicalDocumentId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.bindNullable
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.http.ResponseEntity
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
) {
    @PostMapping
    fun createPedagogicalDocument(
        db: Database,
        user: AuthenticatedUser,
        @RequestBody body: PedagogicalDocumentPostBody
    ): ResponseEntity<PedagogicalDocumentId> {
        Audit.PedagogicalDocumentUpdate.log(body.id)
        accessControl.requirePermissionFor(user, Action.PedagogicalDocument.UPSERT, body.childId)
        val doc = createDocument(db, user, body)
        return ResponseEntity.ok(doc)
    }

    @PutMapping("/{documentId}")
    fun updatePedagogicalDocument(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable documentId: PedagogicalDocumentId,
        @RequestBody body: PedagogicalDocumentPostBody
    ): ResponseEntity<PedagogicalDocument> {
        Audit.PedagogicalDocumentUpdate.log(body.id)
        accessControl.requirePermissionFor(user, Action.PedagogicalDocument.UPSERT, body.childId)
        val doc = updateDocument(db, user, body)
        return ResponseEntity.ok(doc)
    }

    @GetMapping("/{childId}")
    fun getPedagogicalDocument(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable childId: ChildId
    ): ResponseEntity<List<PedagogicalDocument>> {
        Audit.PedagogicalDocumentUpdate.log(childId)
        accessControl.requirePermissionFor(user, Action.PedagogicalDocument.READ, childId)
        val docs = findPedagogicalDocumentsByChild(db, childId)
        return ResponseEntity.ok(docs)
    }
}

data class PedagogicalDocument(
    val id: PedagogicalDocumentId,
    val childId: ChildId,
    val description: String,
    val attachmentId: AttachmentId?,
    val created: HelsinkiDateTime,
    val updated: HelsinkiDateTime
)

data class PedagogicalDocumentPostBody(
    val id: PedagogicalDocumentId,
    val childId: ChildId,
    val description: String,
    val attachmentId: AttachmentId?
)

private fun createDocument(
    db: Database,
    user: AuthenticatedUser,
    body: PedagogicalDocumentPostBody
): PedagogicalDocumentId {
    return db.transaction { tx ->
        tx.createUpdate(
            """
                INSERT INTO pedagogical_document(id, child_id, created_by)
                VALUES (:id, :child_id, :created_by)
                RETURNING id
            """.trimIndent()
        )
            .bind("id", body.id)
            .bind("child_id", body.childId)
            .bind("created_by", user.id)
            .executeAndReturnGeneratedKeys()
            .mapTo<PedagogicalDocumentId>()
            .first()
    }
}

private fun updateDocument(
    db: Database,
    user: AuthenticatedUser,
    body: PedagogicalDocumentPostBody
): PedagogicalDocument {
    return db.transaction { tx ->
        tx.createUpdate(
            """
                UPDATE pedagogical_document
                SET attachment_id = :attachment_id, 
                    description = :description, 
                    updated_by = :updated_by
                WHERE id = :id AND child_id = :child_id
            """.trimIndent()
        )
            .bind("id", body.id)
            .bind("child_id", body.childId)
            .bind("description", body.description)
            .bind("updated_by", user.id)
            .bindNullable("attachment_id", body.attachmentId)
            .executeAndReturnGeneratedKeys()
            .mapTo<PedagogicalDocument>()
            .first()
    }
}

private fun findPedagogicalDocumentsByChild(
    db: Database,
    childId: ChildId
): List<PedagogicalDocument> {
    return db.read {
        it.createQuery(
            """
                SELECT id, child_id, description, attachment_id, created, updated
                FROM pedagogical_document
                WHERE child_id = :child_id
            """.trimIndent()
        )
            .bind("child_id", childId)
            .mapTo<PedagogicalDocument>()
            .toList()
    }
}
