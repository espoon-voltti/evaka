// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pedagogicaldocument

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PedagogicalDocumentId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.jdbi.v3.core.mapper.PropagateNull
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(
    "/pedagogical-document", // deprecated
    "/employee/pedagogical-document",
)
class PedagogicalDocumentController(
    private val accessControl: AccessControl,
    private val pedagogicalDocumentNotificationService: PedagogicalDocumentNotificationService,
) {
    @PostMapping
    fun createPedagogicalDocument(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: PedagogicalDocumentPostBody,
    ): PedagogicalDocument {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Child.CREATE_PEDAGOGICAL_DOCUMENT,
                        body.childId,
                    )
                    tx.createDocument(user, body).also {
                        pedagogicalDocumentNotificationService.maybeScheduleEmailNotification(
                            tx,
                            it.id,
                        )
                    }
                }
            }
            .also {
                Audit.PedagogicalDocumentCreate.log(
                    targetId = AuditId(body.childId),
                    objectId = AuditId(it.id),
                )
            }
    }

    @PutMapping("/{documentId}")
    fun updatePedagogicalDocument(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable documentId: PedagogicalDocumentId,
        @RequestBody body: PedagogicalDocumentPostBody,
    ): PedagogicalDocument {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.PedagogicalDocument.UPDATE,
                        documentId,
                    )
                    tx.updateDocument(user, body, documentId).also {
                        pedagogicalDocumentNotificationService.maybeScheduleEmailNotification(
                            tx,
                            it.id,
                        )
                    }
                }
            }
            .also { Audit.PedagogicalDocumentUpdate.log(targetId = AuditId(documentId)) }
    }

    @GetMapping("/child/{childId}")
    fun getChildPedagogicalDocuments(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
    ): List<PedagogicalDocument> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Child.READ_PEDAGOGICAL_DOCUMENTS,
                        childId,
                    )
                    it.findPedagogicalDocumentsByChild(childId)
                }
            }
            .also {
                Audit.PedagogicalDocumentRead.log(
                    targetId = AuditId(childId),
                    meta = mapOf("count" to it.size),
                )
            }
    }

    @DeleteMapping("/{documentId}")
    fun deletePedagogicalDocument(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable documentId: PedagogicalDocumentId,
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.PedagogicalDocument.DELETE,
                    documentId,
                )
                it.deleteDocument(documentId)
            }
        }
        Audit.PedagogicalDocumentUpdate.log(targetId = AuditId(documentId))
    }
}

data class Attachment(
    @PropagateNull val id: AttachmentId,
    val name: String,
    val contentType: String,
)

data class PedagogicalDocument(
    val id: PedagogicalDocumentId,
    val childId: ChildId,
    val description: String,
    val attachments: List<Attachment> = emptyList(),
    val created: HelsinkiDateTime,
    val updated: HelsinkiDateTime,
)

data class PedagogicalDocumentPostBody(val childId: ChildId, val description: String)

private fun Database.Transaction.createDocument(
    user: AuthenticatedUser,
    body: PedagogicalDocumentPostBody,
): PedagogicalDocument {
    return createUpdate {
            sql(
                """
INSERT INTO pedagogical_document(child_id, created_by, description)
VALUES (${bind(body.childId)}, ${bind(user.evakaUserId)}, ${bind(body.description)})
RETURNING *
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<PedagogicalDocument>()
}

private fun Database.Transaction.updateDocument(
    user: AuthenticatedUser,
    body: PedagogicalDocumentPostBody,
    documentId: PedagogicalDocumentId,
): PedagogicalDocument {
    return createUpdate {
            sql(
                """
UPDATE pedagogical_document
SET description = ${bind(body.description)}, 
    updated_by = ${bind(user.evakaUserId)}
WHERE id = ${bind(documentId)} AND child_id = ${bind(body.childId)}
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<PedagogicalDocument>()
}

private fun Database.Read.findPedagogicalDocumentsByChild(
    childId: ChildId
): List<PedagogicalDocument> {
    return createQuery {
            sql(
                """
                SELECT 
                    pd.id,
                    pd.child_id,
                    pd.description,
                    pd.created,
                    pd.updated
                FROM pedagogical_document pd
                WHERE child_id = ${bind(childId)}
                """
            )
        }
        .toList<PedagogicalDocument>()
        .map { pd -> pd.copy(attachments = getPedagogicalDocumentAttachments(pd.id)) }
}

private fun Database.Transaction.deleteDocument(documentId: PedagogicalDocumentId) {
    createUpdate {
            sql(
                "DELETE FROM pedagogical_document_read WHERE pedagogical_document_id = ${bind(documentId)}"
            )
        }
        .execute()
    createUpdate { sql("DELETE FROM pedagogical_document WHERE id = ${bind(documentId)}") }
        .execute()
}
