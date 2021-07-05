package fi.espoo.evaka.vasu

import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.updateExactlyOne
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.jdbi.v3.core.kotlin.mapTo
import java.util.UUID

fun Database.Transaction.insertVasuDocument(childId: UUID, templateId: UUID): UUID {
    // language=sql
    val insertDocumentSql = """
        INSERT INTO vasu_document (child_id, template_id, modified_at) 
        VALUES (:childId, :templateId, now())
        RETURNING id
    """.trimIndent()

    val documentId = createQuery(insertDocumentSql)
        .bind("childId", childId)
        .bind("templateId", templateId)
        .mapTo<UUID>()
        .one()

    // language=sql
    val insertContentSql = """
        INSERT INTO vasu_content (document_id, content, authors_content, vasu_discussion_content, evaluation_discussion_content) 
        SELECT :documentId, vt.content, :authors, :discussion, :evaluation FROM vasu_template vt WHERE vt.id = :templateId
    """.trimIndent()

    createUpdate(insertContentSql)
        .bind("documentId", documentId)
        .bind("templateId", templateId)
        .bind("authors", AuthorsContent(primaryAuthor = AuthorInfo(), otherAuthors = listOf(AuthorInfo())))
        .bind("discussion", VasuDiscussionContent())
        .bind("evaluation", EvaluationDiscussionContent())
        .updateExactlyOne()

    return documentId
}

fun Database.Read.getVasuDocumentMaster(id: UUID): VasuDocument? {
    // language=sql
    val sql = """
        SELECT
            vd.id,
            vd.child_id,
            vd.modified_at,
            p.first_name AS child_first_name,
            p.last_name AS child_last_name,
            vt.name AS template_name,
            vc.content,
            vc.authors_content,
            vc.vasu_discussion_content,
            vc.evaluation_discussion_content,
            (SELECT jsonb_agg(json_build_object(
                   'id', event.id,
                   'created', event.created,
                   'eventType', event.event_type
               ) ORDER BY event.created) 
               FROM vasu_document_event event
               WHERE event.vasu_document_id = :id
           ) AS events
        FROM vasu_document vd
        JOIN vasu_content vc ON vc.document_id = vd.id AND vc.master
        JOIN vasu_template vt ON vd.template_id = vt.id
        JOIN person p ON p.id = vd.child_id
        WHERE vd.id =:id
    """.trimIndent()

    return createQuery(sql)
        .bind("id", id)
        .mapTo<VasuDocument>()
        .firstOrNull()
}

fun Database.Read.getLatestPublishedVasuDocument(id: UUID): VasuDocument? {
    // language=sql
    val sql = """
        SELECT
            vd.id,
            vd.child_id,
            vd.modified_at,
            p.first_name AS child_first_name,
            p.last_name AS child_last_name,
            vt.name AS template_name,
            vc.content,
            vc.authors_content,
            vc.vasu_discussion_content,
            vc.evaluation_discussion_content,
            (SELECT jsonb_agg(json_build_object(
                   'id', event.id,
                   'created', event.created,
                   'eventType', event.event_type
               ) ORDER BY event.created) 
               FROM vasu_document_event event
               WHERE event.vasu_document_id = :id
           ) AS events
        FROM vasu_document vd
        JOIN LATERAL (
            SELECT vc.content, vc.authors_content, vc.vasu_discussion_content, vc.evaluation_discussion_content
            FROM vasu_content vc
            WHERE vc.published_at IS NOT NULL AND vc.document_id = vd.id
            ORDER BY vc.published_at DESC
            LIMIT 1
        ) vc ON TRUE
        JOIN vasu_template vt ON vd.template_id = vt.id
        JOIN person p ON p.id = vd.child_id
        WHERE vd.id =:id
    """.trimIndent()

    return createQuery(sql)
        .bind("id", id)
        .mapTo<VasuDocument>()
        .firstOrNull()
}

fun Database.Transaction.updateVasuDocumentMaster(
    id: UUID,
    content: VasuContent,
    authorsContent: AuthorsContent,
    vasuDiscussionContent: VasuDiscussionContent,
    evaluationDiscussionContent: EvaluationDiscussionContent
) {
    // language=sql
    val updateContentSql = """
        UPDATE vasu_content
        SET 
            content = :content, 
            authors_content = :authorsContent,
            vasu_discussion_content = :vasuDiscussionContent, 
            evaluation_discussion_content = :evaluationDiscussionContent
        WHERE document_id = :id AND master
    """.trimIndent()

    createUpdate(updateContentSql)
        .bind("id", id)
        .bind("content", content)
        .bind("authorsContent", authorsContent)
        .bind("vasuDiscussionContent", vasuDiscussionContent)
        .bind("evaluationDiscussionContent", evaluationDiscussionContent)
        .updateExactlyOne()

    createUpdate("UPDATE vasu_document SET modified_at = now() WHERE id = :id")
        .bind("id", id)
        .updateExactlyOne()
}

fun Database.Transaction.publishVasuDocument(id: UUID) {
    // language=sql
    val insertContentSql = """
        INSERT INTO vasu_content (document_id, published_at, content, authors_content, vasu_discussion_content, evaluation_discussion_content)
        SELECT vc.document_id, now(), vc.content, vc.authors_content, vc.vasu_discussion_content, vc.evaluation_discussion_content
        FROM vasu_content vc
        WHERE vc.document_id = :id AND master
    """.trimIndent()

    createUpdate(insertContentSql)
        .bind("id", id)
        .updateExactlyOne()

    createUpdate("UPDATE vasu_document SET modified_at = now() WHERE id = :id")
        .bind("id", id)
        .updateExactlyOne()
}

data class SummaryResultRow(
    val id: UUID,
    val name: String,
    val modifiedAt: HelsinkiDateTime,
    val eventId: UUID? = null,
    val eventCreated: HelsinkiDateTime? = null,
    val eventType: VasuDocumentEventType? = null
)

fun Database.Read.getVasuDocumentSummaries(childId: UUID): List<VasuDocumentSummary> {
    // language=sql
    val sql = """
        SELECT 
            vd.id,
            vt.name,
            vd.modified_at,
            e.id AS event_id,
            e.created AS event_created,
            e.event_type
        FROM vasu_document vd
        JOIN vasu_template vt ON vd.template_id = vt.id
        JOIN child c ON c.id = vd.child_id
        LEFT JOIN vasu_document_event e ON vd.id = e.vasu_document_id 
        WHERE c.id = :childId
        ORDER BY vd.modified_at DESC
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

fun Database.Transaction.insertVasuDocumentEvent(documentId: UUID, eventType: VasuDocumentEventType, employeeId: UUID): VasuDocumentEvent {
    val sql = """
        INSERT INTO vasu_document_event (vasu_document_id, employee_id, event_type)
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
