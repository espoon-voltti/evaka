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
import fi.espoo.evaka.user.EvakaUser
import org.jdbi.v3.core.mapper.Nested
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
@RequestMapping("/employee/pedagogical-document")
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
                    tx.createDocument(user, clock.now(), body).also {
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
                    tx.updateDocument(user, clock.now(), body, documentId).also {
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
    val createdAt: HelsinkiDateTime,
    @Nested("created_by") val createdBy: EvakaUser,
    val modifiedAt: HelsinkiDateTime,
    @Nested("modified_by") val modifiedBy: EvakaUser,
)

data class PedagogicalDocumentPostBody(val childId: ChildId, val description: String)

private fun Database.Transaction.createDocument(
    user: AuthenticatedUser,
    now: HelsinkiDateTime,
    body: PedagogicalDocumentPostBody,
): PedagogicalDocument {
    return createQuery {
            sql(
                """
WITH pd AS (
    INSERT INTO pedagogical_document(child_id, created_by, description, modified_at, modified_by)
    VALUES (${bind(body.childId)}, ${bind(user.evakaUserId)}, ${bind(body.description)}, ${bind(now)}, ${bind(user.evakaUserId)})
    RETURNING *
) SELECT
    pd.id,
    pd.child_id,
    pd.description,
    pd.created_at,
    ce.id AS created_by_id,
    ce.name AS created_by_name,
    ce.type AS created_by_type,
    pd.modified_at,
    e.id AS modified_by_id,
    e.name AS modified_by_name,
    e.type AS modified_by_type
FROM pd LEFT JOIN evaka_user ce ON pd.created_by = ce.id LEFT JOIN evaka_user e ON pd.modified_by = e.id
"""
            )
        }
        .exactlyOne<PedagogicalDocument>()
}

private fun Database.Transaction.updateDocument(
    user: AuthenticatedUser,
    now: HelsinkiDateTime,
    body: PedagogicalDocumentPostBody,
    documentId: PedagogicalDocumentId,
): PedagogicalDocument {
    return createQuery {
            sql(
                """
WITH pd AS (
    UPDATE pedagogical_document
    SET description = ${bind(body.description)}, 
        modified_at = ${bind(now)},
        modified_by = ${bind(user.evakaUserId)}
    WHERE id = ${bind(documentId)} AND child_id = ${bind(body.childId)}
    RETURNING *
) SELECT
    pd.id,
    pd.child_id,
    pd.description,
    pd.created_at,
    ce.id AS created_by_id,
    ce.name AS created_by_name,
    ce.type AS created_by_type,
    pd.modified_at,
    e.id AS modified_by_id,
    e.name AS modified_by_name,
    e.type AS modified_by_type
FROM pd LEFT JOIN evaka_user ce ON pd.created_by = ce.id LEFT JOIN evaka_user e ON pd.modified_by = e.id
"""
            )
        }
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
                    pd.created_at,
                    ce.id AS created_by_id,
                    ce.name AS created_by_name,
                    ce.type AS created_by_type,
                    pd.modified_at,
                    e.id AS modified_by_id,
                    e.name AS modified_by_name,
                    e.type AS modified_by_type
                FROM pedagogical_document pd
                LEFT JOIN evaka_user ce ON pd.created_by = ce.id
                LEFT JOIN evaka_user e ON pd.modified_by = e.id
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
