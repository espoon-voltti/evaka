// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vasu

import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.VasuDocumentId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import java.time.LocalDate
import java.util.UUID
import org.jdbi.v3.json.Json

fun Database.Transaction.insertVasuDocument(
    now: HelsinkiDateTime,
    childId: ChildId,
    template: VasuTemplate
): VasuDocumentId {
    val child =
        createQuery {
                sql(
                    "SELECT id, first_name, last_name, date_of_birth FROM person WHERE id = ${bind(childId)}"
                )
            }
            .exactlyOne<VasuChild>(qualifiers = emptyArray())

    val guardiansAndFosterParents =
        createQuery {
                sql(
                    """
                    SELECT p.id, p.first_name, p.last_name
                    FROM guardian g
                    JOIN person p on p.id = g.guardian_id
                    WHERE g.child_id = ${bind(childId)}

                    UNION

                    SELECT p.id, p.first_name, p.last_name
                    FROM foster_parent fp
                    JOIN person p on p.id = fp.parent_id
                    WHERE fp.child_id = ${bind(childId)} AND fp.valid_during @> ${bind(now.toLocalDate())}
                    """
                )
            }
            .toList<VasuGuardian>(qualifiers = emptyArray())

    val basics =
        VasuBasics(
            child = child,
            guardians = guardiansAndFosterParents,
            placements = null,
            childLanguage =
                when (template.type) {
                    CurriculumType.DAYCARE -> null
                    CurriculumType.PRESCHOOL -> ChildLanguage("", "")
                }
        )

    val documentId =
        createQuery {
                sql(
                    """
INSERT INTO curriculum_document (created, updated, child_id, basics, template_id, modified_at)
VALUES (${bind(now)}, ${bind(now)}, ${bind(childId)}, ${bind(basics)}, ${bind(template.id)}, ${bind(now)})
RETURNING id
"""
                )
            }
            .exactlyOne<VasuDocumentId>()

    createUpdate {
            sql(
                """
                INSERT INTO curriculum_content (created, updated, document_id, content)
                SELECT ${bind(now)}, ${bind(now)}, ${bind(documentId)}, ct.content
                FROM curriculum_template ct WHERE ct.id = ${bind(template.id)}
                """
            )
        }
        .updateExactlyOne()

    return documentId
}

fun Database.Read.getVasuDocumentMaster(today: LocalDate, id: VasuDocumentId): VasuDocument? {
    return createQuery {
            sql(
                """
                SELECT
                    cd.id,
                    cd.child_id,
                    cd.created,
                    cd.modified_at,
                    cd.basics,
                    ct.id AS template_id,
                    ct.name AS template_name,
                    ct.valid AS template_range,
                    ct.type,
                    ct.language,
                    vc.content,
                    vc.published_at,
                    (SELECT jsonb_agg(jsonb_build_object(
                           'id', event.id,
                           'created', event.created,
                           'eventType', event.event_type,
                           'createdBy', event.created_by
                       ) ORDER BY event.created) 
                       FROM curriculum_document_event event
                       WHERE event.curriculum_document_id = ${bind(id)}
                   ) AS events
                FROM curriculum_document cd
                JOIN curriculum_content vc ON vc.document_id = cd.id AND vc.master
                JOIN curriculum_template ct ON cd.template_id = ct.id
                WHERE cd.id = ${bind(id)}
                """
            )
        }
        .exactlyOneOrNull<VasuDocument>()
        ?.let { document ->
            if (document.basics.placements == null) {
                document.copy(
                    basics = document.basics.copy(placements = getVasuPlacements(today, id))
                )
            } else {
                document
            }
        }
}

fun Database.Read.getLatestPublishedVasuDocument(
    today: LocalDate,
    id: VasuDocumentId
): VasuDocument? {
    return createQuery {
            sql(
                """
                SELECT
                    cd.id,
                    cd.child_id,
                    cd.basics,
                    cd.created,
                    cd.modified_at,
                    ct.id AS template_id,
                    ct.name AS template_name,
                    ct.valid AS template_range,
                    ct.type,
                    ct.language,
                    vc.content,
                    vc.published_at,
                    (SELECT jsonb_agg(jsonb_build_object(
                           'id', event.id,
                           'created', event.created,
                           'eventType', event.event_type,
                           'createdBy', event.created_by
                       ) ORDER BY event.created) 
                       FROM curriculum_document_event event
                       WHERE event.curriculum_document_id = ${bind(id)}
                   ) AS events
                FROM curriculum_document cd
                JOIN LATERAL (
                    SELECT vc.content, vc.published_at
                    FROM curriculum_content vc
                    WHERE vc.published_at IS NOT NULL AND vc.document_id = cd.id
                    ORDER BY vc.published_at DESC
                    LIMIT 1
                ) vc ON TRUE
                JOIN curriculum_template ct ON cd.template_id = ct.id
                WHERE cd.id = ${bind(id)}
                """
            )
        }
        .exactlyOneOrNull<VasuDocument>()
        ?.let { document ->
            if (document.basics.placements == null) {
                document.copy(
                    basics = document.basics.copy(placements = getVasuPlacements(today, id))
                )
            } else {
                document
            }
        }
}

fun Database.Transaction.updateVasuDocumentMaster(
    now: HelsinkiDateTime,
    id: VasuDocumentId,
    content: VasuContent,
    childLanguage: ChildLanguage?
) {
    createUpdate {
            sql(
                "UPDATE curriculum_content SET content = ${bind(content)} WHERE document_id = ${bind(id)} AND master"
            )
        }
        .updateExactlyOne()

    createUpdate {
            sql(
                """
                UPDATE curriculum_document SET
                    modified_at = ${bind(now)},
                    basics = basics || jsonb_build_object('childLanguage', ${bind(childLanguage)}::jsonb)
                WHERE id = ${bind(id)}
                """
            )
        }
        .updateExactlyOne()
}

fun Database.Transaction.publishVasuDocument(now: HelsinkiDateTime, id: VasuDocumentId) {
    // language=sql

    createUpdate {
            sql(
                """
                INSERT INTO curriculum_content (document_id, published_at, content)
                SELECT vc.document_id, ${bind(now)}, vc.content
                FROM curriculum_content vc
                WHERE vc.document_id = ${bind(id)} AND master
                """
            )
        }
        .updateExactlyOne()

    createUpdate {
            sql("UPDATE curriculum_document SET modified_at = ${bind(now)} WHERE id = ${bind(id)}")
        }
        .updateExactlyOne()
}

data class SummaryResultRow(
    val id: VasuDocumentId,
    val name: String,
    val modifiedAt: HelsinkiDateTime,
    val publishedAt: HelsinkiDateTime? = null,
    @Json val basics: VasuBasics,
    val eventId: UUID? = null,
    val eventCreated: HelsinkiDateTime? = null,
    val eventType: VasuDocumentEventType? = null,
    val eventCreatedBy: EvakaUserId? = null,
    val type: CurriculumType
)

fun Database.Read.getVasuDocumentSummaries(childId: ChildId): List<VasuDocumentSummary> {
    return createQuery {
            sql(
                """
                SELECT 
                    cd.id,
                    ct.name,
                    cd.modified_at,
                    e.id AS event_id,
                    e.created AS event_created,
                    e.event_type,
                    e.created_by AS event_created_by,
                    vc.published_at,
                    cd.basics,
                    ct.type
                FROM curriculum_document cd
                JOIN curriculum_template ct ON cd.template_id = ct.id
                JOIN child c ON c.id = cd.child_id
                LEFT JOIN curriculum_document_event e ON cd.id = e.curriculum_document_id
                LEFT JOIN LATERAL (
                    SELECT vc.published_at
                    FROM curriculum_content vc
                    WHERE vc.published_at IS NOT NULL AND vc.document_id = cd.id
                    ORDER BY vc.published_at DESC
                    LIMIT 1
                ) vc ON TRUE
                WHERE c.id = ${bind(childId)}
                ORDER BY cd.modified_at DESC
                """
            )
        }
        .toList<SummaryResultRow>()
        .groupBy { it.id }
        .map { (documentId, documents) ->
            VasuDocumentSummary(
                id = documentId,
                name = documents[0].name,
                modifiedAt = documents[0].modifiedAt,
                publishedAt = documents[0].publishedAt,
                guardiansThatHaveGivenPermissionToShare =
                    documents[0]
                        .basics
                        .guardians
                        .filter { it.hasGivenPermissionToShare }
                        .map { it.id },
                events =
                    documents
                        .mapNotNull {
                            if (
                                it.eventId != null &&
                                    it.eventCreated != null &&
                                    it.eventType != null &&
                                    it.eventCreatedBy != null
                            ) {
                                VasuDocumentEvent(
                                    id = it.eventId,
                                    created = it.eventCreated,
                                    eventType = it.eventType,
                                    createdBy = it.eventCreatedBy,
                                )
                            } else {
                                null
                            }
                        }
                        .sortedBy { it.created },
                type = documents[0].type
            )
        }
}

fun Database.Transaction.insertVasuDocumentEvent(
    documentId: VasuDocumentId,
    eventType: VasuDocumentEventType,
    createdBy: EvakaUserId
): VasuDocumentEvent {
    return createQuery {
            sql(
                """
                INSERT INTO curriculum_document_event (curriculum_document_id, created_by, event_type)
                VALUES (${bind(documentId)}, ${bind(createdBy)}, ${bind(eventType)})
                RETURNING id, created, event_type, created_by
                """
            )
        }
        .exactlyOne<VasuDocumentEvent>()
}

fun Database.Transaction.freezeVasuPlacements(today: LocalDate, id: VasuDocumentId) {
    createQuery { sql("SELECT basics FROM curriculum_document WHERE id = ${bind(id)}") }
        .exactlyOneOrNull { jsonColumn<VasuBasics>("basics") }
        ?.let { basics ->
            val basicsParam = basics.copy(placements = getVasuPlacements(today, id))
            createUpdate {
                    sql(
                        "UPDATE curriculum_document SET basics = ${bind(basicsParam)} WHERE id = ${bind(id)}"
                    )
                }
                .updateExactlyOne()
        }
}

private fun Database.Read.getVasuPlacements(
    today: LocalDate,
    id: VasuDocumentId
): List<VasuPlacement> {
    return createQuery {
            sql(
                """
                SELECT 
                    d.id AS unit_id,
                    d.name AS unit_name,
                    dg.id AS group_id,
                    dg.name AS group_name,
                    daterange(dgp.start_date, dgp.end_date, '[]') AS range
                FROM curriculum_document cd
                JOIN curriculum_template ct on ct.id = cd.template_id
                JOIN placement p ON p.child_id = cd.child_id AND daterange(p.start_date, p.end_date, '[]') && ct.valid
                JOIN daycare d ON d.id = p.unit_id
                JOIN daycare_group_placement dgp ON dgp.daycare_placement_id = p.id AND daterange(dgp.start_date, dgp.end_date, '[]') && ct.valid
                JOIN daycare_group dg ON dg.id = dgp.daycare_group_id
                WHERE cd.id = ${bind(id)} AND dgp.start_date <= ${bind(today)}
                ORDER BY dgp.start_date
                """
            )
        }
        .toList<VasuPlacement>(qualifiers = emptyArray())
}

private fun Database.Read.getVasuDocumentBasics(id: VasuDocumentId): VasuBasics =
    createQuery { sql("SELECT basics FROM curriculum_document WHERE id = ${bind(id)}") }
        .exactlyOneOrNull { jsonColumn<VasuBasics>("basics") }
        ?: throw NotFound("Vasu document $id not found")

fun Database.Transaction.setVasuGuardianHasGivenPermissionToShare(
    docId: VasuDocumentId,
    guardianId: PersonId
) {
    val currentBasics = getVasuDocumentBasics(docId)

    val (guardianFromDocument, otherGuardiansFromDocument) =
        currentBasics.guardians.partition { g -> g.id == guardianId }

    val guardian =
        if (guardianFromDocument.size == 1) {
            guardianFromDocument[0]
        } else {
            createQuery {
                    sql(
                        """
                        SELECT p.id, p.first_name, p.last_name
                        FROM person p
                        WHERE p.id = ${bind(guardianId)}
                        """
                    )
                }
                .exactlyOne<VasuGuardian>(qualifiers = emptyArray())
        }

    val basics =
        currentBasics.copy(
            guardians = otherGuardiansFromDocument + guardian.copy(hasGivenPermissionToShare = true)
        )
    createUpdate {
            sql(
                """
                UPDATE curriculum_document
                SET basics = ${bind(basics)}
                WHERE id = ${bind(docId)}
                """
            )
        }
        .execute()
}

fun Database.Transaction.revokeVasuGuardianHasGivenPermissionToShare(docId: VasuDocumentId) {
    val currentBasics = getVasuDocumentBasics(docId)
    val basics =
        currentBasics.copy(
            guardians =
                currentBasics.guardians.map { guardian ->
                    guardian.copy(hasGivenPermissionToShare = false)
                }
        )
    createUpdate {
            sql(
                """
                UPDATE curriculum_document
                SET basics = ${bind(basics)}
                WHERE id = ${bind(docId)}
                """
            )
        }
        .execute()
}

fun Database.Transaction.deleteVasuDocument(id: VasuDocumentId) {
    createUpdate { sql("DELETE FROM curriculum_content WHERE document_id = ${bind(id)}") }.execute()
    createUpdate {
            sql("DELETE FROM curriculum_document_event WHERE curriculum_document_id = ${bind(id)}")
        }
        .execute()
    createUpdate { sql("DELETE FROM curriculum_document WHERE id = ${bind(id)}") }.execute()
}

fun Database.Read.getOpenVasusWithExpiredTemplate(today: LocalDate): List<VasuDocumentId> =
    createQuery {
            sql(
                """
SELECT id FROM (
    SELECT
        d.id,
        array((
            SELECT e.event_type
            FROM curriculum_document_event e
            WHERE e.curriculum_document_id = d.id ORDER BY e.created
        )) AS events
    FROM curriculum_document d
    JOIN curriculum_template t ON t.id = d.template_id
    WHERE t.valid << daterange(${bind(today)}, NULL)
) AS d
WHERE cardinality(events) = 0 OR events[cardinality(events)] <> 'MOVED_TO_CLOSED';
"""
            )
        }
        .toList<VasuDocumentId>()
