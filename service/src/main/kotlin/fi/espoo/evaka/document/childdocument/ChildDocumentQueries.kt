// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document.childdocument

import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound

fun Database.Transaction.insertChildDocument(
    document: ChildDocumentCreateRequest,
    now: HelsinkiDateTime
): ChildDocumentId {
    return createQuery(
            """
            INSERT INTO child_document(child_id, template_id, status, content, modified_at)
            VALUES (:childId, :templateId, 'DRAFT', :content, :now)
            RETURNING id
        """
        )
        .bindKotlin(document)
        .bind("content", DocumentContent(answers = emptyList()))
        .bind("now", now)
        .exactlyOne<ChildDocumentId>()
}

fun Database.Read.getChildDocuments(childId: PersonId): List<ChildDocumentSummary> {
    return createQuery(
            """
            SELECT cd.id, cd.status, dt.type, cd.modified_at, cd.published_at, dt.id as template_id, dt.name as template_name
            FROM child_document cd
            JOIN document_template dt on cd.template_id = dt.id
            WHERE cd.child_id = :childId
        """
        )
        .bind("childId", childId)
        .toList<ChildDocumentSummary>()
}

fun Database.Read.getChildDocument(id: ChildDocumentId): ChildDocumentDetails? {
    return createQuery(
            """
            SELECT 
                cd.id,
                cd.status,
                cd.published_at,
                cd.content,
                cd.published_content,
                p.id as child_id,
                p.first_name as child_first_name,
                p.last_name as child_last_name,
                p.date_of_birth as child_date_of_birth,
                dt.id as template_id,
                dt.name as template_name,
                dt.type as template_type,
                dt.language as template_language,
                dt.legal_basis as template_legal_basis,
                dt.confidential as template_confidential,
                dt.validity as template_validity,
                dt.published as template_published,
                dt.content as template_content
            FROM child_document cd
            JOIN document_template dt on cd.template_id = dt.id
            JOIN person p on cd.child_id = p.id
            WHERE cd.id = :id
        """
        )
        .bind("id", id)
        .exactlyOneOrNull<ChildDocumentDetails>()
}

fun Database.Transaction.updateChildDocumentContent(
    id: ChildDocumentId,
    status: DocumentStatus,
    content: DocumentContent,
    now: HelsinkiDateTime
) {
    createUpdate<Any> {
            sql(
                """
            UPDATE child_document
            SET content = ${bind(content)}, modified_at = ${bind(now)}
            WHERE id = ${bind(id)} AND status = ${bind(status)}
        """
            )
        }
        .updateExactlyOne()
}

fun Database.Read.isDocumentPublishedContentUpToDate(id: ChildDocumentId): Boolean {
    return createQuery(
            """
        SELECT (published_content IS NOT NULL AND content = published_content) AS up_to_date 
        FROM child_document 
        WHERE id = :id
    """
        )
        .bind("id", id)
        .exactlyOneOrNull<Boolean>() ?: throw NotFound("Document $id not found")
}

fun Database.Transaction.publishChildDocument(id: ChildDocumentId, now: HelsinkiDateTime) {
    createUpdate(
            """
            UPDATE child_document
            SET published_at = :now, published_content = content
            WHERE id = :id AND status <> 'COMPLETED'
        """
        )
        .bind("id", id)
        .bind("now", now)
        .updateExactlyOne()
    createUpdate(
            """
            DELETE FROM child_document_read
            WHERE document_id = :id
        """
        )
        .bind("id", id)
        .execute()
}

data class StatusTransition(val currentStatus: DocumentStatus, val newStatus: DocumentStatus)

fun validateStatusTransition(
    tx: Database.Read,
    documentId: ChildDocumentId,
    requestedStatus: DocumentStatus,
    goingForward: Boolean // false = backwards
): StatusTransition {
    val document = tx.getChildDocument(documentId) ?: throw NotFound()

    val statusList = document.template.type.statuses
    val currentIndex = statusList.indexOf(document.status)
    if (currentIndex < 0) {
        throw IllegalStateException("document $documentId is in invalid status")
    }
    val newStatus =
        statusList.getOrNull(if (goingForward) currentIndex + 1 else currentIndex - 1)
            ?: throw BadRequest("Already in the ${if (goingForward) "final" else "first"} status")
    if (newStatus != requestedStatus) {
        throw Conflict("Idempotency issue: statuses do not match")
    }
    return StatusTransition(currentStatus = document.status, newStatus = newStatus)
}

fun Database.Transaction.changeStatus(
    id: ChildDocumentId,
    statusTransition: StatusTransition,
    now: HelsinkiDateTime
) {
    createUpdate<Any> {
            sql(
                """
                UPDATE child_document
                SET status = ${bind(statusTransition.newStatus)}, modified_at = ${bind(now)}
                WHERE id = ${bind(id)} AND status = ${bind(statusTransition.currentStatus)}
            """
            )
        }
        .updateExactlyOne()
}

fun Database.Transaction.changeStatusAndPublish(
    id: ChildDocumentId,
    statusTransition: StatusTransition,
    now: HelsinkiDateTime
) {
    createUpdate<Any> {
            sql(
                """
                UPDATE child_document
                SET status = ${bind(statusTransition.newStatus)}, modified_at = ${bind(now)}, 
                    published_at = ${bind(now)}, published_content = content
                WHERE id = ${bind(id)} AND status = ${bind(statusTransition.currentStatus)}
            """
            )
        }
        .updateExactlyOne()
}

fun Database.Transaction.markCompletedAndPublish(
    ids: List<ChildDocumentId>,
    now: HelsinkiDateTime
) {
    createUpdate<Any> {
            sql(
                """
                UPDATE child_document
                SET status = 'COMPLETED', modified_at = ${bind(now)}, published_at = ${bind(now)}, published_content = content
                WHERE id = ANY(${bind(ids)})
            """
            )
        }
        .execute()
}

fun Database.Transaction.deleteChildDocumentDraft(id: ChildDocumentId) {
    createUpdate(
            """
            DELETE FROM child_document_read
            WHERE document_id = :id
        """
        )
        .bind("id", id)
        .execute()

    createUpdate(
            """
            DELETE FROM child_document
            WHERE id = :id AND status = 'DRAFT'
        """
        )
        .bind("id", id)
        .updateExactlyOne()
}
