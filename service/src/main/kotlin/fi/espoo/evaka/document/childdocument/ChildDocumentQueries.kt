// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document.childdocument

import fi.espoo.evaka.document.ChildDocumentType
import fi.espoo.evaka.pis.Employee
import fi.espoo.evaka.shared.CaseProcessId
import fi.espoo.evaka.shared.ChildDocumentDecisionId
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.DocumentTemplateId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.db.PredicateSql
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import java.time.LocalDate
import org.jdbi.v3.json.Json

const val lockMinutes = 5

fun Database.Transaction.insertChildDocument(
    childId: ChildId,
    templateId: DocumentTemplateId,
    now: HelsinkiDateTime,
    userId: EvakaUserId,
    processId: CaseProcessId?,
): ChildDocumentId {
    val type =
        createQuery { sql("SELECT type FROM document_template WHERE id = ${bind(templateId)}") }
            .exactlyOne<ChildDocumentType>()

    return createQuery {
            sql(
                """
INSERT INTO child_document(child_id, template_id, type, status, content, modified_at, modified_by, content_locked_at, content_locked_by, created_by, process_id, decision_maker)
VALUES (${bind(childId)}, ${bind(templateId)}, ${bind(type)}, 'DRAFT', ${bind(DocumentContent(answers = emptyList()))}, ${bind(now)}, ${bind(userId)}, ${bind(now)}, ${bind(userId)}, ${bind(userId)}, ${bind(processId)}, NULL)
RETURNING id
"""
            )
        }
        .exactlyOne<ChildDocumentId>()
}

fun Database.Read.getChildDocuments(childId: PersonId): List<ChildDocumentSummary> =
    getChildDocuments(Predicate { where("$it.child_id = ${bind(childId)}") })

fun Database.Read.getChildDocuments(
    documentPredicate: Predicate,
    documentDecisionPredicate: Predicate = Predicate.alwaysTrue(),
    statuses: Set<ChildDocumentOrDecisionStatus>? = null,
): List<ChildDocumentSummary> {
    val statusPredicate =
        if (!statuses.isNullOrEmpty()) {
            val documentStatuses = statuses.mapNotNull { it.asDocumentStatus() }
            val decisionStatuses = statuses.mapNotNull { it.asDecisionStatus() }
            PredicateSql {
                where(
                    "cd.status = ANY(${bind(documentStatuses)}) OR cdd.status = ANY(${bind(decisionStatuses)})"
                )
            }
        } else {
            PredicateSql.alwaysTrue()
        }

    val combinedPredicate = PredicateSql {
        documentPredicate
            .forTable("cd")
            .and(documentDecisionPredicate.forTable("cdd"))
            .and(statusPredicate)
    }

    return createQuery {
            sql(
                """
SELECT
    cd.id, 
    cd.status,
    dt.type,
    cd.modified_at, 
    modified_by.name AS modified_by,
    latest_version.published_at,
    latest_version.published_by,
    dt.id as template_id, 
    dt.name as template_name,
    ch.first_name AS child_first_name,
    ch.last_name AS child_last_name,
    cd.answered_at,
    answered_by.id AS answered_by_id,
    answered_by.name AS answered_by_name, 
    answered_by.type AS answered_by_type,
    decision_maker.id AS decision_maker_id,
    decision_maker.name AS decision_maker_name, 
    decision_maker.type AS decision_maker_type,
    cdd.id AS decision_id, 
    cdd.status AS decision_status,
    cdd.created_at AS decision_created_at,
    CASE WHEN cdd.valid_from IS NOT NULL THEN daterange(cdd.valid_from, cdd.valid_to, '[]') END AS decision_validity,
    cdd.decision_number AS decision_decision_number,
    d.name AS decision_daycare_name,
    cdd.annulment_reason AS decision_annulment_reason
FROM child_document cd
JOIN document_template dt on cd.template_id = dt.id
JOIN person ch ON cd.child_id = ch.id
LEFT JOIN evaka_user modified_by ON cd.modified_by = modified_by.id
LEFT JOIN LATERAL (
    SELECT v.created_at AS published_at, u.name AS published_by
    FROM child_document_published_version v
    JOIN evaka_user u ON v.created_by = u.id
    WHERE v.child_document_id = cd.id
    ORDER BY v.version_number DESC
    LIMIT 1
) latest_version ON true
LEFT JOIN evaka_user answered_by ON cd.answered_by = answered_by.id
LEFT JOIN evaka_user decision_maker ON cd.decision_maker = decision_maker.employee_id
LEFT JOIN child_document_decision cdd ON cdd.id = cd.decision_id
LEFT JOIN daycare d ON d.id = cdd.daycare_id
WHERE ${predicate(combinedPredicate)}
"""
            )
        }
        .toList<ChildDocumentSummary>()
}

private fun Database.Read.nonCompletedChildDocumentChildIdsQuery(
    templateId: DocumentTemplateId,
    predicate: Predicate,
) = createQuery {
    sql(
        """
SELECT child_id
FROM child_document
WHERE template_id = ${bind(templateId)}
  AND status != 'COMPLETED'
  AND ${predicate(predicate.forTable("child_document"))}
"""
    )
}

fun Database.Read.getNonCompletedChildDocumentChildIds(
    templateId: DocumentTemplateId,
    childIds: Set<ChildId>,
) =
    nonCompletedChildDocumentChildIdsQuery(
            templateId,
            Predicate { where("$it.child_id = ANY (${bind(childIds)})") },
        )
        .toSet<ChildId>()

fun Database.Read.getNonCompletedChildDocumentChildIds(
    templateId: DocumentTemplateId,
    groupId: GroupId,
    date: LocalDate,
) =
    nonCompletedChildDocumentChildIdsQuery(
            templateId,
            Predicate {
                where(
                    """
EXISTS (SELECT
    FROM daycare_group_placement dgp
    JOIN placement p ON dgp.daycare_placement_id = p.id
    WHERE dgp.daycare_group_id = ${bind(groupId)}
      AND ${bind(date)} BETWEEN dgp.start_date AND dgp.end_date
      AND p.child_id = $it.child_id
)
        """
                )
            },
        )
        .toSet<ChildId>()

fun Database.Read.getChildDocument(id: ChildDocumentId): ChildDocumentDetails? {
    return createQuery {
            sql(
                """
SELECT 
    cd.id,
    cd.status,
    latest_version.published_at,
    cd.archived_at,
    COALESCE(latest_version.pdf_available, false) as pdf_available,
    cd.content,
    latest_version.published_content,
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
    dt.confidentiality_basis as template_confidentiality_basis,
    dt.confidentiality_duration_years as template_confidentiality_duration_years,
    dt.validity as template_validity,
    dt.published as template_published,
    dt.content as template_content,
    dt.process_definition_number as template_process_definition_number,
    dt.archive_duration_months as template_archive_duration_months,
    dt.archive_externally as template_archive_externally,
    cd.decision_maker,
    cdd.id AS decision_id,
    cdd.status AS decision_status,
    cdd.created_at AS decision_created_at,
    CASE WHEN cdd.valid_from IS NOT NULL THEN daterange(cdd.valid_from, cdd.valid_to, '[]') END AS decision_validity,
    cdd.decision_number AS decision_decision_number,
    d.name AS decision_daycare_name,
    cdd.annulment_reason AS decision_annulment_reason
FROM child_document cd
JOIN document_template dt on cd.template_id = dt.id
JOIN person p on cd.child_id = p.id
LEFT JOIN LATERAL (
    SELECT v.created_at AS published_at, v.published_content, v.document_key IS NOT NULL as pdf_available
    FROM child_document_published_version v
    WHERE v.child_document_id = cd.id
    ORDER BY v.version_number DESC
    LIMIT 1
) latest_version ON true
LEFT JOIN child_document_decision cdd ON cdd.id = cd.decision_id
LEFT JOIN daycare d ON d.id = cdd.daycare_id
WHERE cd.id = ${bind(id)}
"""
            )
        }
        .exactlyOneOrNull<ChildDocumentDetails>()
}

fun Database.Transaction.insertChildDocumentPublishedVersion(
    documentId: ChildDocumentId,
    createdAt: HelsinkiDateTime,
    createdBy: EvakaUserId,
    publishedContent: DocumentContent,
): Int =
    createQuery {
            sql(
                """
            INSERT INTO child_document_published_version
                (child_document_id, version_number, created_at, created_by, published_content)
            VALUES (
                ${bind(documentId)},
                COALESCE(
                    (SELECT MAX(version_number) + 1 
                     FROM child_document_published_version
                     WHERE child_document_id = ${bind(documentId)}), 
                    1
                ),
                ${bind(createdAt)},
                ${bind(createdBy)},
                ${bind(publishedContent)}::jsonb
            )
            RETURNING version_number
        """
            )
        }
        .exactlyOne<Int>()

fun Database.Transaction.updateChildDocumentPublishedVersionKey(
    documentId: ChildDocumentId,
    versionNumber: Int,
    documentKey: String,
) {
    createUpdate {
            sql(
                """
            UPDATE child_document_published_version
            SET document_key = ${bind(documentKey)}
            WHERE child_document_id = ${bind(documentId)}
                AND version_number = ${bind(versionNumber)}
                AND document_key IS NULL
        """
            )
        }
        .updateExactlyOne()
}

fun Database.Read.getChildDocumentPublishedVersions(
    documentId: ChildDocumentId
): List<PublishedVersion> =
    createQuery {
            sql(
                """
            SELECT version_number, document_key, created_at
            FROM child_document_published_version
            WHERE child_document_id = ${bind(documentId)}
            ORDER BY version_number DESC
        """
            )
        }
        .toList()

fun Database.Read.getChildDocumentPublishedVersion(
    documentId: ChildDocumentId,
    versionNumber: Int? = null,
): PublishedVersion? =
    createQuery {
            sql(
                """
            SELECT version_number, document_key, created_at
            FROM child_document_published_version
            WHERE child_document_id = ${bind(documentId)}
                ${if (versionNumber != null) "AND version_number = ${bind(versionNumber)}" else ""}
            ORDER BY version_number DESC
            LIMIT 1
        """
            )
        }
        .exactlyOneOrNull<PublishedVersion>()

data class PublishedVersion(
    val versionNumber: Int,
    val documentKey: String?,
    val createdAt: HelsinkiDateTime,
)

data class DocumentWriteLock(
    val lockedBy: EvakaUserId,
    val lockedByName: String,
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
        content_locked_by AS locked_by,
        (
            SELECT e.name
            FROM evaka_user e WHERE e.id = cd.content_locked_by
        ) AS locked_by_name,
        content_locked_at + interval '$lockMinutes minutes' AS opens_at
    FROM child_document cd
    WHERE id = ${bind(id)} AND 
        content_locked_by IS NOT NULL AND 
        content_locked_at >= ${bind(now.minusMinutes(lockMinutes.toLong()))}
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
        UPDATE child_document SET content_locked_at = ${bind(now)}, content_locked_by = ${bind(userId)}
        WHERE id = ${bind(id)} AND (
            content_locked_by IS NULL OR
            content_locked_by = ${bind(userId)} OR
            content_locked_at < ${bind(now.minusMinutes(lockMinutes.toLong()))}
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
                    modified_by = ${bind(userId)},
                    content_locked_at = ${bind(now)}, 
                    content_locked_by = ${bind(userId)}
                WHERE id = ${bind(id)} AND status = ${bind(status)} AND (
                    content_locked_by IS NULL OR 
                    content_locked_by = ${bind(userId)} OR 
                    content_locked_at < ${bind(now.minusMinutes(lockMinutes.toLong()))}
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
                    modified_by = ${bind(userId)},
                    content_locked_at = ${bind(now)},
                    content_locked_by = ${bind(userId)},
                    answered_at = ${bind(now)},
                    answered_by = ${bind(userId)}
                WHERE id = ${bind(id)} AND status = ${bind(statusTransition.currentStatus)}
                """
            )
        }
        .updateExactlyOne()
}

/**
 * Creates a new published version if content has changed. Returns version number or null if
 * skipped.
 */
fun Database.Transaction.createPublishedVersionIfNeeded(
    id: ChildDocumentId,
    now: HelsinkiDateTime,
    userId: EvakaUserId,
): Int? {
    data class Result(@Json val content: DocumentContent, val upToDate: Boolean)

    val result =
        createQuery {
                sql(
                    """
            SELECT 
                cd.content,
                (lv.published_content IS NOT NULL AND cd.content = lv.published_content) AS up_to_date
            FROM child_document cd
            LEFT JOIN LATERAL (
                SELECT v.published_content
                FROM child_document_published_version v
                WHERE v.child_document_id = cd.id
                ORDER BY v.version_number DESC
                LIMIT 1
            ) lv ON true
            WHERE cd.id = ${bind(id)}
            """
                )
            }
            .exactlyOneOrNull<Result>() ?: throw NotFound("Document $id not found")

    if (result.upToDate) return null

    return insertChildDocumentPublishedVersion(id, now, userId, result.content)
}

fun Database.Transaction.deleteChildDocumentReadMarkers(documentId: ChildDocumentId) {
    createUpdate { sql("DELETE FROM child_document_read WHERE document_id = ${bind(documentId)}") }
        .execute()
}

data class StatusTransition(val currentStatus: DocumentStatus, val newStatus: DocumentStatus)

fun Database.Transaction.updateChildDocumentStatus(
    id: ChildDocumentId,
    statusTransition: StatusTransition,
    now: HelsinkiDateTime,
    answeredBy: EvakaUserId? = null,
    userId: EvakaUserId,
) {
    createUpdate {
            sql(
                """
                UPDATE child_document
                SET status = ${bind(statusTransition.newStatus)},
                    modified_at = ${bind(now)},
                    modified_by = ${bind(userId)}
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
): Map<ChildDocumentId, Int> {
    val userId = AuthenticatedUser.SystemInternalUser.evakaUserId

    // Always update status for all documents
    createUpdate {
            sql(
                """
                UPDATE child_document
                SET status = 'COMPLETED',
                    modified_at = ${bind(now)},
                    modified_by = ${bind(userId)}
                WHERE id = ANY(${bind(ids)})
                """
            )
        }
        .execute()

    // Batch insert into version table using CTE, filtering for changed content in SQL
    return createQuery {
            sql(
                """
            WITH documents_to_publish AS (
                SELECT cd.id, cd.content
                FROM child_document cd
                LEFT JOIN LATERAL (
                    SELECT v.published_content
                    FROM child_document_published_version v
                    WHERE v.child_document_id = cd.id
                    ORDER BY v.version_number DESC
                    LIMIT 1
                ) latest_version ON true
                WHERE cd.id = ANY(${bind(ids)})
                  AND (latest_version.published_content IS NULL OR cd.content != latest_version.published_content)
            ),
            next_versions AS (
                SELECT 
                    d.id,
                    d.content,
                    COALESCE(MAX(v.version_number) + 1, 1) as next_version
                FROM documents_to_publish d
                LEFT JOIN child_document_published_version v ON v.child_document_id = d.id
                GROUP BY d.id, d.content
            ),
            inserted AS (
                INSERT INTO child_document_published_version
                    (child_document_id, version_number, created_at, created_by, published_content, document_key)
                SELECT 
                    id,
                    next_version,
                    ${bind(now)},
                    ${bind(userId)},
                    content::jsonb,
                    NULL
                FROM next_versions
                RETURNING child_document_id, version_number
            )
            SELECT child_document_id, version_number FROM inserted
                """
            )
        }
        .toMap { column<ChildDocumentId>("child_document_id") to column<Int>("version_number") }
}

/** Make sure to schedule DeleteChildDocumentPdf jobs before calling this */
fun Database.Transaction.deleteChildDocumentDraft(id: ChildDocumentId) {
    deleteChildDocumentReadMarkers(id)

    createUpdate {
            sql(
                "DELETE FROM child_document_published_version WHERE child_document_id = ${bind(id)}"
            )
        }
        .execute()

    createUpdate { sql("DELETE FROM child_document WHERE id = ${bind(id)} AND status = 'DRAFT'") }
        .updateExactlyOne()
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
        JOIN daycare d ON d.id = acl.daycare_id
    WHERE acl.employee_id = e.id 
        AND acl.role = ANY(${bind(listOf(UserRole.UNIT_SUPERVISOR, UserRole.SPECIAL_EDUCATION_TEACHER))})
        AND d.provider_type <> 'PRIVATE_SERVICE_VOUCHER'
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
    daycareId: DaycareId?,
    annulmentReason: String = "",
): ChildDocumentDecisionId {
    return createUpdate {
            sql(
                """
                INSERT INTO child_document_decision (created_by, modified_by, status, valid_from, valid_to, daycare_id, annulment_reason) 
                VALUES (${bind(userId)}, ${bind(userId)}, ${bind(status)}, ${bind(validity?.start)}, ${bind(validity?.end)}, ${bind(daycareId)}, ${bind(annulmentReason)})
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
    annulmentReason: String,
    now: HelsinkiDateTime,
): ChildDocumentDecisionId {
    return createUpdate {
            sql(
                """
            UPDATE child_document_decision
            SET status = 'ANNULLED', modified_by = ${bind(userId)}, modified_at = ${bind(now)}, annulment_reason = ${bind(annulmentReason)}
            WHERE id = ${bind(decisionId)} AND status = 'ACCEPTED'
        """
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()
}

fun Database.Transaction.setChildDocumentDecisionAndComplete(
    documentId: ChildDocumentId,
    decisionId: ChildDocumentDecisionId,
    now: HelsinkiDateTime,
    userId: EvakaUserId,
) {
    createUpdate {
            sql(
                """
                UPDATE child_document
                SET decision_id = ${bind(decisionId)}, 
                    status = 'COMPLETED', 
                    modified_at = ${bind(now)},
                    modified_by = ${bind(userId)}
                WHERE id = ${bind(documentId)} AND status = 'DECISION_PROPOSAL'
                """
            )
        }
        .updateExactlyOne()
}

fun Database.Transaction.endExpiredChildDocumentDecisions(
    today: LocalDate
): List<ChildDocumentDecisionId> {
    val yesterday = today.minusDays(1)
    return createQuery {
            sql(
                """
        WITH expired AS (
            SELECT cdd.id
            FROM child_document_decision cdd
            JOIN child_document cd ON cdd.id = cd.decision_id
            JOIN document_template dt ON cd.template_id = dt.id
            LEFT JOIN placement current_placement ON cd.child_id = current_placement.child_id AND
                daterange(current_placement.start_date, current_placement.end_date, '[]') @> ${bind(today)}
            LEFT JOIN daycare current_unit ON current_unit.id = current_placement.unit_id
            LEFT JOIN placement yesterday_placement ON cd.child_id = yesterday_placement.child_id AND
                daterange(yesterday_placement.start_date, yesterday_placement.end_date, '[]') @> ${bind(yesterday)}
            LEFT JOIN daycare yesterday_unit ON yesterday_unit.id = yesterday_placement.unit_id
            WHERE cdd.status = 'ACCEPTED'
                AND (cdd.valid_from <= ${bind(yesterday)})
                AND (cdd.valid_to IS NULL OR cdd.valid_to > ${bind(yesterday)})
                AND (
                    (current_placement.id IS NULL) OR 
                    (NOT (current_placement.type = ANY(dt.placement_types))) OR 
                    (dt.end_decision_when_unit_changes = TRUE AND cdd.daycare_id IS NOT NULL AND yesterday_unit.name <> current_unit.name)
                )
        )
        UPDATE child_document_decision cdd
        SET valid_to = ${bind(yesterday)}
        FROM expired exp
        WHERE cdd.id = exp.id
        RETURNING cdd.id
    """
            )
        }
        .toList<ChildDocumentDecisionId>()
}

fun Database.Read.getAcceptedChildDocumentDecisions(
    documentId: ChildDocumentId
): List<AcceptedChildDecisions> {
    return createQuery {
            sql(
                """
SELECT
    accepted_document.decision_id AS id,
    accepted_document.template_id,
    daterange(cdd.valid_from, cdd.valid_to, '[]') AS validity,
    dt.name AS template_name
FROM child_document new_document
         JOIN child_document accepted_document ON accepted_document.child_id = new_document.child_id
         JOIN child_document_decision cdd ON accepted_document.decision_id = cdd.id
         JOIN document_template dt ON accepted_document.template_id = dt.id
WHERE new_document.id = ${bind(documentId)} AND cdd.status = 'ACCEPTED'
"""
            )
        }
        .toList<AcceptedChildDecisions>()
}

fun Database.Read.endChildDocumentDecisionsWithSubstitutiveDecision(
    childId: ChildId,
    endingDecisionIds: List<ChildDocumentDecisionId>,
    endDate: LocalDate,
): List<ChildDocumentDecisionId> {
    return createQuery {
            sql(
                """
UPDATE child_document_decision cdd
SET valid_to = ${bind(endDate)}
FROM child_document cd
WHERE cdd.id = ANY(${bind(endingDecisionIds)})
    AND cdd.status = 'ACCEPTED'
    AND (cdd.valid_to IS NULL OR cdd.valid_to > ${bind(endDate)})
    AND cd.decision_id = cdd.id
    AND cd.child_id = ${bind(childId)}
RETURNING cdd.id
"""
            )
        }
        .toList<ChildDocumentDecisionId>()
}

fun Database.Transaction.setChildDocumentDecisionValidity(
    decisionId: ChildDocumentDecisionId,
    validity: DateRange,
) {
    return createUpdate {
            sql(
                """
            UPDATE child_document_decision
            SET valid_from = ${bind(validity.start)}, valid_to = ${bind(validity.end)}
            WHERE id = ${bind(decisionId)} AND status = 'ACCEPTED'
            RETURNING id
        """
            )
        }
        .updateExactlyOne()
}

fun Database.Read.getChildDocumentsEligibleForArchival(
    eligibleDate: LocalDate
): List<ChildDocumentId> =
    createQuery {
            sql(
                """
                SELECT cd.id
                FROM child_document cd
                JOIN document_template dt ON cd.template_id = dt.id
                WHERE upper(dt.validity) <= ${bind(eligibleDate)}
                  AND dt.archive_externally = true
                  AND cd.archived_at IS NULL
                ORDER BY cd.created
                """
            )
        }
        .toList<ChildDocumentId>()
