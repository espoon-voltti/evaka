// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document.childdocument

import fi.espoo.evaka.document.DocumentType
import fi.espoo.evaka.shared.ArchivedProcessId
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound

const val lockMinutes = 5

fun Database.Transaction.insertChildDocument(
    document: ChildDocumentCreateRequest,
    now: HelsinkiDateTime,
    userId: EvakaUserId,
    processId: ArchivedProcessId?,
): ChildDocumentId {
    val type =
        createQuery {
                sql("SELECT type FROM document_template WHERE id = ${bind(document.templateId)}")
            }
            .exactlyOne<DocumentType>()

    return createQuery {
            sql(
                """
INSERT INTO child_document(child_id, template_id, type, status, content, modified_at, content_modified_at, content_modified_by, created_by, process_id)
VALUES (${bind(document.childId)}, ${bind(document.templateId)}, ${bind(type)}, 'DRAFT', ${bind(DocumentContent(answers = emptyList()))}, ${bind(now)}, ${bind(now)}, ${bind(userId)}, ${bind(userId)}, ${bind(processId)})
RETURNING id
"""
            )
        }
        .exactlyOne<ChildDocumentId>()
}

fun Database.Read.getChildDocuments(childId: PersonId): List<ChildDocumentSummary> {
    return createQuery {
            sql(
                """
SELECT cd.id, cd.status, dt.type, cd.modified_at, cd.published_at, dt.id as template_id, dt.name as template_name,
    cd.answered_at,
    answered_by.id AS answered_by_id, answered_by.name AS answered_by_name, answered_by.type AS answered_by_type
FROM child_document cd
JOIN document_template dt on cd.template_id = dt.id
LEFT JOIN evaka_user answered_by ON cd.answered_by = answered_by.id
WHERE cd.child_id = ${bind(childId)}
"""
            )
        }
        .toList<ChildDocumentSummary>()
}

fun Database.Read.getChildDocument(id: ChildDocumentId): ChildDocumentDetails? {
    return createQuery {
            sql(
                """
SELECT 
    cd.id,
    cd.status,
    cd.published_at,
    cd.archived_at,
    cd.document_key IS NOT NULL AS pdf_available,
    cd.content,
    cd.published_content,
    p.id as child_id,
    p.first_name as child_first_name,
    p.last_name as child_last_name,
    p.date_of_birth as child_date_of_birth,
    dt.id as template_id,
    dt.name as template_name,
    dt.type as template_type,
    dt.placement_types as template_placement_types,
    dt.language as template_language,
    dt.legal_basis as template_legal_basis,
    dt.confidential as template_confidential,
    dt.validity as template_validity,
    dt.published as template_published,
    dt.content as template_content
FROM child_document cd
JOIN document_template dt on cd.template_id = dt.id
JOIN person p on cd.child_id = p.id
WHERE cd.id = ${bind(id)}
"""
            )
        }
        .exactlyOneOrNull<ChildDocumentDetails>()
}

fun Database.Read.getChildDocumentKey(id: ChildDocumentId): String? {
    return createQuery {
            sql(
                """
                SELECT cd.document_key
                FROM child_document cd
                WHERE cd.id = ${bind(id)} AND published_at IS NOT NULL
                """
            )
        }
        .exactlyOneOrNull<String>()
}

data class DocumentWriteLock(
    val modifiedBy: EvakaUserId,
    val modifiedByName: String,
    val opensAt: HelsinkiDateTime,
)

fun Database.Read.getCurrentWriteLock(
    id: ChildDocumentId,
    now: HelsinkiDateTime,
): DocumentWriteLock? =
    createQuery {
            sql(
                """
    SELECT 
        content_modified_by AS modified_by,
        (
            SELECT e.name
            FROM evaka_user e WHERE e.id = cd.content_modified_by
        ) AS modified_by_name,
        content_modified_at + interval '$lockMinutes minutes' AS opens_at
    FROM child_document cd
    WHERE id = ${bind(id)} AND 
        content_modified_by IS NOT NULL AND 
        content_modified_at >= ${bind(now.minusMinutes(lockMinutes.toLong()))}
"""
            )
        }
        .exactlyOneOrNull()

fun Database.Transaction.tryTakeWriteLock(
    id: ChildDocumentId,
    now: HelsinkiDateTime,
    userId: EvakaUserId,
): Boolean =
    createUpdate {
            sql(
                """
        UPDATE child_document SET content_modified_at = ${bind(now)}, content_modified_by = ${bind(userId)}
        WHERE id = ${bind(id)} AND (
            content_modified_by IS NULL OR
            content_modified_by = ${bind(userId)} OR
            content_modified_at < ${bind(now.minusMinutes(lockMinutes.toLong()))}
        )
    """
            )
        }
        .execute()
        .let { it > 0 }

fun Database.Transaction.updateChildDocumentContent(
    id: ChildDocumentId,
    status: DocumentStatus,
    content: DocumentContent,
    now: HelsinkiDateTime,
    userId: EvakaUserId,
) {
    createUpdate {
            sql(
                """
                UPDATE child_document
                SET 
                    content = ${bind(content)}, 
                    modified_at = ${bind(now)}, 
                    content_modified_at = ${bind(now)}, 
                    content_modified_by = ${bind(userId)}
                WHERE id = ${bind(id)} AND status = ${bind(status)} AND (
                    content_modified_by IS NULL OR 
                    content_modified_by = ${bind(userId)} OR 
                    content_modified_at < ${bind(now.minusMinutes(lockMinutes.toLong()))}
                )
                """
            )
        }
        .updateExactlyOne()
}

fun Database.Transaction.updateChildDocument(
    id: ChildDocumentId,
    statusTransition: StatusTransition,
    content: DocumentContent,
    now: HelsinkiDateTime,
    userId: EvakaUserId,
) {
    createUpdate {
            sql(
                """
                UPDATE child_document
                SET
                    status = ${bind(statusTransition.newStatus)},
                    content = ${bind(content)},
                    modified_at = ${bind(now)},
                    content_modified_at = ${bind(now)},
                    content_modified_by = ${bind(userId)},
                    published_content = ${bind(content)},
                    published_at = ${bind(now)},
                    answered_at = ${bind(now)},
                    answered_by = ${bind(userId)}
                WHERE id = ${bind(id)} AND status = ${bind(statusTransition.currentStatus)}
                """
            )
        }
        .updateExactlyOne()
}

fun Database.Read.isDocumentPublishedContentUpToDate(id: ChildDocumentId): Boolean {
    return createQuery {
            sql(
                """
                SELECT (published_content IS NOT NULL AND content = published_content) AS up_to_date 
                FROM child_document 
                WHERE id = ${bind(id)}
                """
            )
        }
        .exactlyOneOrNull<Boolean>() ?: throw NotFound("Document $id not found")
}

fun Database.Transaction.publishChildDocument(id: ChildDocumentId, now: HelsinkiDateTime) {
    createUpdate {
            sql(
                """
                UPDATE child_document
                SET published_at = ${bind(now)}, published_content = content
                WHERE id = ${bind(id)} AND status <> 'COMPLETED'
                """
            )
        }
        .updateExactlyOne()

    createUpdate {
            sql(
                """
                DELETE FROM child_document_read
                WHERE document_id = ${bind(id)}
                """
            )
        }
        .execute()
}

data class StatusTransition(val currentStatus: DocumentStatus, val newStatus: DocumentStatus)

fun validateStatusTransition(
    tx: Database.Read,
    documentId: ChildDocumentId,
    requestedStatus: DocumentStatus,
    goingForward: Boolean, // false = backwards
): StatusTransition {
    val document = tx.getChildDocument(documentId) ?: throw NotFound()
    return validateStatusTransition(document, requestedStatus, goingForward)
}

fun validateStatusTransition(
    document: ChildDocumentDetails,
    requestedStatus: DocumentStatus,
    goingForward: Boolean, // false = backwards
): StatusTransition {
    val statusList = document.template.type.statuses
    val currentIndex = statusList.indexOf(document.status)
    if (currentIndex < 0) {
        throw IllegalStateException("document ${document.id} is in invalid status")
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
    now: HelsinkiDateTime,
) {
    createUpdate {
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
    now: HelsinkiDateTime,
) {
    createUpdate {
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
    now: HelsinkiDateTime,
) {
    createUpdate {
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
    createUpdate {
            sql(
                """
                DELETE FROM child_document_read
                WHERE document_id = ${bind(id)}
                """
            )
        }
        .execute()

    createUpdate {
            sql(
                """
                DELETE FROM child_document
                WHERE id = ${bind(id)} AND status = 'DRAFT'
                """
            )
        }
        .updateExactlyOne()
}

fun Database.Transaction.updateChildDocumentKey(id: ChildDocumentId, documentKey: String) {
    createUpdate {
            sql(
                """
        UPDATE child_document
        SET document_key = ${bind(documentKey)}
        WHERE id = ${bind(id)}
    """
            )
        }
        .updateExactlyOne()
}

fun Database.Transaction.resetChildDocumentKey(ids: List<ChildDocumentId>) {
    executeBatch(ids) {
        sql(
            """
UPDATE child_document
SET document_key = NULL
WHERE id = ${bind { it }}
"""
        )
    }
}

fun Database.Transaction.markDocumentAsArchived(id: ChildDocumentId, now: HelsinkiDateTime) {
    createUpdate {
            sql(
                """
            UPDATE child_document
            SET archived_at = ${bind(now)}
            WHERE id = ${bind(id)}
            """
            )
        }
        .updateExactlyOne()
}
