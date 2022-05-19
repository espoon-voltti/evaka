// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vasu

import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.VasuDocumentFollowupEntryId
import fi.espoo.evaka.shared.VasuDocumentId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapJsonColumn
import fi.espoo.evaka.shared.db.updateExactlyOne
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import org.jdbi.v3.core.kotlin.mapTo
import java.util.UUID

fun Database.Transaction.insertVasuDocument(childId: ChildId, template: VasuTemplate): VasuDocumentId {
    val child = createQuery("SELECT id, first_name, last_name, date_of_birth FROM person WHERE id = :id")
        .bind("id", childId)
        .mapTo<VasuChild>()
        .one()

    val guardians = createQuery(
        """
        SELECT p.id, p.first_name, p.last_name
        FROM guardian g
        JOIN person p on p.id = g.guardian_id
        WHERE g.child_id = :id
        """.trimIndent()
    )
        .bind("id", childId)
        .mapTo<VasuGuardian>()
        .list()

    val basics = VasuBasics(
        child = child,
        guardians = guardians,
        placements = null,
        childLanguage = when (template.type) {
            CurriculumType.DAYCARE -> null
            CurriculumType.PRESCHOOL -> ChildLanguage("", "")
        }
    )

    val documentId = createQuery(
        """
        INSERT INTO curriculum_document (child_id, basics, template_id, modified_at)
        VALUES (:childId, :basics, :templateId, now())
        RETURNING id
        """.trimIndent()
    )
        .bind("childId", childId)
        .bind("basics", basics)
        .bind("templateId", template.id)
        .mapTo<VasuDocumentId>()
        .one()

    createUpdate(
        """
        INSERT INTO curriculum_content (document_id, content)
        SELECT :documentId, ct.content FROM curriculum_template ct WHERE ct.id = :templateId
        """.trimIndent()
    )
        .bind("documentId", documentId)
        .bind("templateId", template.id)
        .updateExactlyOne()

    return documentId
}

fun Database.Read.getVasuDocumentMaster(id: VasuDocumentId): VasuDocument? {
    // language=sql
    val sql = """
        SELECT
            cd.id,
            cd.child_id,
            cd.modified_at,
            cd.basics,
            ct.name AS template_name,
            ct.valid AS template_range,
            ct.type,
            ct.language,
            vc.content,
            (SELECT jsonb_agg(json_build_object(
                   'id', event.id,
                   'created', event.created,
                   'eventType', event.event_type
               ) ORDER BY event.created) 
               FROM curriculum_document_event event
               WHERE event.curriculum_document_id = :id
           ) AS events
        FROM curriculum_document cd
        JOIN curriculum_content vc ON vc.document_id = cd.id AND vc.master
        JOIN curriculum_template ct ON cd.template_id = ct.id
        WHERE cd.id =:id
    """.trimIndent()

    return createQuery(sql)
        .bind("id", id)
        .mapTo<VasuDocument>()
        .firstOrNull()
        ?.let { document ->
            if (document.basics.placements == null) {
                document.copy(
                    basics = document.basics.copy(
                        placements = getVasuPlacements(id)
                    )
                )
            } else document
        }
}

fun Database.Read.getLatestPublishedVasuDocument(id: VasuDocumentId): VasuDocument? {
    // language=sql
    val sql = """
        SELECT
            cd.id,
            cd.child_id,
            cd.basics,
            cd.modified_at,
            ct.name AS template_name,
            ct.valid AS template_range,
            ct.type,
            ct.language,
            vc.content,
            (SELECT jsonb_agg(json_build_object(
                   'id', event.id,
                   'created', event.created,
                   'eventType', event.event_type
               ) ORDER BY event.created) 
               FROM curriculum_document_event event
               WHERE event.curriculum_document_id = :id
           ) AS events
        FROM curriculum_document cd
        JOIN LATERAL (
            SELECT vc.content
            FROM curriculum_content vc
            WHERE vc.published_at IS NOT NULL AND vc.document_id = cd.id
            ORDER BY vc.published_at DESC
            LIMIT 1
        ) vc ON TRUE
        JOIN curriculum_template ct ON cd.template_id = ct.id
        WHERE cd.id =:id
    """.trimIndent()

    return createQuery(sql)
        .bind("id", id)
        .mapTo<VasuDocument>()
        .firstOrNull()
        ?.let { document ->
            if (document.basics.placements == null) {
                document.copy(
                    basics = document.basics.copy(
                        placements = getVasuPlacements(id)
                    )
                )
            } else document
        }
}

fun Database.Transaction.updateVasuDocumentMaster(
    id: VasuDocumentId,
    content: VasuContent,
    childLanguage: ChildLanguage?
) {
    // language=sql
    val updateContentSql = "UPDATE curriculum_content SET content = :content WHERE document_id = :id AND master"

    createUpdate(updateContentSql)
        .bind("id", id)
        .bind("content", content)
        .updateExactlyOne()

    createUpdate(
        """
        UPDATE curriculum_document SET
            modified_at = now(),
            basics = basics || jsonb_build_object('childLanguage', :childLanguage::jsonb)
        WHERE id = :id
        """.trimIndent()
    )
        .bind("id", id)
        .bind("childLanguage", childLanguage)
        .updateExactlyOne()
}

fun Database.Transaction.publishVasuDocument(id: VasuDocumentId) {
    // language=sql
    val insertContentSql = """
        INSERT INTO curriculum_content (document_id, published_at, content)
        SELECT vc.document_id, now(), vc.content
        FROM curriculum_content vc
        WHERE vc.document_id = :id AND master
    """.trimIndent()

    createUpdate(insertContentSql)
        .bind("id", id)
        .updateExactlyOne()

    createUpdate("UPDATE curriculum_document SET modified_at = now() WHERE id = :id")
        .bind("id", id)
        .updateExactlyOne()
}

data class SummaryResultRow(
    val id: VasuDocumentId,
    val name: String,
    val modifiedAt: HelsinkiDateTime,
    val eventId: UUID? = null,
    val eventCreated: HelsinkiDateTime? = null,
    val eventType: VasuDocumentEventType? = null
)

fun Database.Read.getVasuDocumentSummaries(childId: ChildId): List<VasuDocumentSummary> {
    // language=sql
    val sql = """
        SELECT 
            cd.id,
            ct.name,
            cd.modified_at,
            e.id AS event_id,
            e.created AS event_created,
            e.event_type
        FROM curriculum_document cd
        JOIN curriculum_template ct ON cd.template_id = ct.id
        JOIN child c ON c.id = cd.child_id
        LEFT JOIN curriculum_document_event e ON cd.id = e.curriculum_document_id
        WHERE c.id = :childId
        ORDER BY cd.modified_at DESC
    """.trimIndent()

    return createQuery(sql)
        .bind("childId", childId)
        .mapTo<SummaryResultRow>()
        .groupBy { it.id }
        .map { (documentId, documents) ->
            VasuDocumentSummary(
                id = documentId,
                name = documents[0].name,
                modifiedAt = documents[0].modifiedAt,
                events = documents.mapNotNull {
                    if (it.eventId != null && it.eventCreated != null && it.eventType != null) VasuDocumentEvent(
                        id = it.eventId,
                        created = it.eventCreated,
                        eventType = it.eventType
                    ) else null
                }.sortedBy { it.created }
            )
        }
}

fun Database.Transaction.insertVasuDocumentEvent(
    documentId: VasuDocumentId,
    eventType: VasuDocumentEventType,
    employeeId: EmployeeId
): VasuDocumentEvent {
    val sql = """
        INSERT INTO curriculum_document_event (curriculum_document_id, employee_id, event_type)
        VALUES (:documentId, :employeeId, :eventType)
        RETURNING id, created, event_type
    """.trimIndent()

    return createQuery(sql)
        .bind("documentId", documentId)
        .bind("employeeId", employeeId)
        .bind("eventType", eventType)
        .mapTo<VasuDocumentEvent>()
        .one()
}

fun Database.Transaction.freezeVasuPlacements(id: VasuDocumentId) {
    createQuery("SELECT basics FROM curriculum_document WHERE id = :id")
        .bind("id", id)
        .map { row -> row.mapJsonColumn<VasuBasics>("basics") }
        .firstOrNull()
        ?.let { basics ->
            createUpdate("UPDATE curriculum_document SET basics = :basics WHERE id = :id")
                .bind("id", id)
                .bind(
                    "basics",
                    basics.copy(
                        placements = getVasuPlacements(id)
                    )
                )
                .updateExactlyOne()
        }
}

private fun Database.Read.getVasuPlacements(id: VasuDocumentId): List<VasuPlacement> {
    return createQuery(
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
        WHERE cd.id = :id
        ORDER BY dgp.start_date
        """.trimIndent()
    )
        .bind("id", id)
        .mapTo<VasuPlacement>()
        .list()
}

fun Database.Read.getVasuFollowupEntry(id: VasuDocumentFollowupEntryId): FollowupEntry {
    val (docId, entryId) = id
    return createQuery(
        """
        WITH followup_entries AS (
            SELECT jsonb_path_query(content, '$.sections[*].questions ? (@.type=="FOLLOWUP").value[*]') AS entry 
            FROM curriculum_content
            WHERE document_id = :docId AND master = true
        )
        SELECT entry FROM followup_entries WHERE entry ->> 'id' = :entryId
        """
    )
        .bind("docId", docId)
        .bind("entryId", entryId.toString())
        .map { row -> row.mapJsonColumn<FollowupEntry>("entry") }
        .one()
}

fun Database.Transaction.setVasuGuardianHasGivenPermissionToShare(docId: VasuDocumentId, guardianId: PersonId) {
    val currentBasics = getVasuDocumentMaster(docId)?.basics
        ?: throw NotFound("Vasu document not found!")

    val (guardianFromDocument, otherGuardiansFromDocument) = currentBasics.guardians.partition {
            g ->
        g.id == guardianId
    }

    val guardian = if (guardianFromDocument.size == 1) guardianFromDocument[0] else {
        createQuery(
            """
        SELECT p.id, p.first_name, p.last_name
        FROM person p
        WHERE p.id = :id
            """.trimIndent()
        )
            .bind("id", guardianId)
            .mapTo<VasuGuardian>()
            .first()
    }

    createUpdate(
        """
UPDATE curriculum_document
SET basics = :basics
WHERE id = :id
        """.trimIndent()
    )
        .bind("id", docId)
        .bind(
            "basics",
            currentBasics.copy(
                guardians = otherGuardiansFromDocument + guardian.copy(
                    hasGivenPermissionToShare = true
                )
            )
        )
        .execute()
}
