// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document.childdocument

import fi.espoo.evaka.document.DocumentType
import fi.espoo.evaka.pis.Employee
import fi.espoo.evaka.shared.ArchivedProcessId
import fi.espoo.evaka.shared.ChildDocumentDecisionId
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DocumentTemplateId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound

const val lockMinutes = 5

fun Database.Transaction.insertChildDocument(
    childId: ChildId,
    templateId: DocumentTemplateId,
    now: HelsinkiDateTime,
    userId: EvakaUserId,
    processId: ArchivedProcessId?,
): ChildDocumentId {
    val type =
        createQuery { sql("SELECT type FROM document_template WHERE id = ${bind(templateId)}") }
            .exactlyOne<DocumentType>()

    return createQuery {
            sql(
                """
INSERT INTO child_document(child_id, template_id, type, status, content, modified_at, content_modified_at, content_modified_by, created_by, process_id, decision_maker)
VALUES (${bind(childId)}, ${bind(templateId)}, ${bind(type)}, 'DRAFT', ${bind(DocumentContent(answers = emptyList()))}, ${bind(now)}, ${bind(now)}, ${bind(userId)}, ${bind(userId)}, ${bind(processId)}, NULL)
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
SELECT 
    cd.id, 
    cd.status,
    dt.type,
    cd.modified_at, 
    cd.published_at, 
    dt.id as template_id, 
    dt.name as template_name,
    cd.answered_at,
    answered_by.id AS answered_by_id,
    answered_by.name AS answered_by_name, 
    answered_by.type AS answered_by_type,
    cdd.id AS decision_id, 
    cdd.status AS decision_status,
    CASE WHEN cdd.valid_from IS NOT NULL THEN daterange(cdd.valid_from, cdd.valid_to, '[]') END AS decision_validity
FROM child_document cd
JOIN document_template dt on cd.template_id = dt.id
LEFT JOIN evaka_user answered_by ON cd.answered_by = answered_by.id
LEFT JOIN child_document_decision cdd ON cdd.id = cd.decision_id
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
    dt.content as template_content,
    dt.archive_externally as template_archive_externally,
    cd.decision_maker,
    cdd.id AS decision_id,
    cdd.status AS decision_status,
    CASE WHEN cdd.valid_from IS NOT NULL THEN daterange(cdd.valid_from, cdd.valid_to, '[]') END AS decision_validity
FROM child_document cd
JOIN document_template dt on cd.template_id = dt.id
JOIN person p on cd.child_id = p.id
LEFT JOIN child_document_decision cdd ON cdd.id = cd.decision_id
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
    answeredBy: EvakaUserId?,
) {
    createUpdate {
            sql(
                """
                UPDATE child_document
                SET status = ${bind(statusTransition.newStatus)}, modified_at = ${bind(now)}, 
                    published_at = ${bind(now)}, published_content = content
                    ${if (answeredBy != null) ", answered_at = ${bind(now)}, answered_by = ${bind(answeredBy)}" else ""}
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

fun Database.Read.getChildDocumentDecisionMakers(documentId: ChildDocumentId): List<Employee> =
    createQuery {
            sql(
                """
SELECT id, first_name, last_name, email, external_id, created, updated, active, (social_security_number IS NOT NULL) AS has_ssn, last_login
FROM employee e
WHERE e.roles && ${bind(listOf(UserRole.ADMIN, UserRole.DIRECTOR))} OR EXISTS(
    SELECT FROM daycare_acl acl 
    WHERE acl.employee_id = e.id AND acl.role = ANY(${bind(listOf(UserRole.UNIT_SUPERVISOR, UserRole.SPECIAL_EDUCATION_TEACHER))})
) OR EXISTS( -- always include currently selected even if roles change
    SELECT FROM child_document cd
    WHERE cd.id = ${bind(documentId)} AND cd.decision_maker = e.id
) 
"""
            )
        }
        .toList()

fun Database.Transaction.setChildDocumentDecisionMaker(
    id: ChildDocumentId,
    decisionMaker: EmployeeId?,
) =
    createUpdate {
            sql(
                """
            UPDATE child_document 
            SET decision_maker = ${bind(decisionMaker)}
            WHERE id = ${bind(id)}
        """
            )
        }
        .updateExactlyOne()

fun Database.Transaction.insertChildDocumentDecision(
    status: ChildDocumentDecisionStatus,
    userId: EvakaUserId,
    validity: DateRange?,
): ChildDocumentDecisionId {
    return createUpdate {
            sql(
                """
                INSERT INTO child_document_decision (created_by, modified_by, status, valid_from, valid_to) 
                VALUES (${bind(userId)}, ${bind(userId)}, ${bind(status)}, ${bind(validity?.start)}, ${bind(validity?.end)})
                RETURNING id
            """
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()
}

fun Database.Transaction.annulChildDocumentDecision(
    decisionId: ChildDocumentDecisionId,
    userId: EvakaUserId,
    now: HelsinkiDateTime,
): ChildDocumentDecisionId {
    return createUpdate {
            sql(
                """
            UPDATE child_document_decision
            SET status = 'ANNULLED', modified_by = ${bind(userId)}, modified_at = ${bind(now)}
            WHERE id = ${bind(decisionId)} AND status = 'ACCEPTED'
        """
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()
}

fun Database.Transaction.setChildDocumentDecisionAndPublish(
    documentId: ChildDocumentId,
    decisionId: ChildDocumentDecisionId,
    now: HelsinkiDateTime,
) {
    createUpdate {
            sql(
                """
                UPDATE child_document
                SET decision_id = ${bind(decisionId)}, 
                    status = 'COMPLETED', 
                    modified_at = ${bind(now)},
                    published_content = content,
                    published_at = ${bind(now)}
                WHERE id = ${bind(documentId)} AND status = 'DECISION_PROPOSAL'
                """
            )
        }
        .updateExactlyOne()
}
