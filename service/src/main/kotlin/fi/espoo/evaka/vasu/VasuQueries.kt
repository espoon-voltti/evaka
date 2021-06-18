package fi.espoo.evaka.vasu

import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.updateExactlyOne
import org.jdbi.v3.core.kotlin.mapTo
import java.util.UUID

fun Database.Transaction.insertVasuDocument(childId: UUID, templateId: UUID): UUID {
    // language=sql
    val sql = """
        WITH content AS (
            INSERT INTO vasu_content (content) 
            SELECT vt.content FROM vasu_template vt WHERE vt.id = :templateId
            RETURNING id
        )
        INSERT INTO vasu_document (child_id, template_id, content_id, document_state, modified_at) 
        SELECT :childId, :templateId, ct.id, 'DRAFT', now() FROM content ct
        RETURNING id
    """.trimIndent()

    return createQuery(sql)
        .bind("childId", childId)
        .bind("templateId", templateId)
        .mapTo<UUID>()
        .one()
}

fun Database.Read.getVasuDocumentResponse(id: UUID): VasuDocumentResponse? {
    // language=sql
    val sql = """
        SELECT 
            vd.id,
            vd.child_id,
            p.first_name AS child_first_name,
            p.last_name AS child_last_name,
            vt.name AS template_name,
            vc.content
        FROM vasu_document vd
        JOIN vasu_content vc on vd.content_id = vc.id
        JOIN vasu_template vt on vd.template_id = vt.id
        JOIN person p on p.id = vd.child_id
        WHERE vd.id =:id
    """.trimIndent()

    return createQuery(sql)
        .bind("id", id)
        .mapTo<VasuDocumentResponse>()
        .firstOrNull()
}

fun Database.Transaction.updateVasuDocument(id: UUID, content: VasuContent) {
    // language=sql
    val updateContentSql = """
        UPDATE vasu_content
        SET content = :content
        WHERE id IN (SELECT vd.content_id FROM vasu_document vd WHERE vd.id = :id)
    """.trimIndent()

    createUpdate(updateContentSql)
        .bind("id", id)
        .bind("content", content)
        .updateExactlyOne()

    createUpdate("UPDATE vasu_document SET modified_at = NOW() WHERE id = :id")
        .bind("id", id)
        .updateExactlyOne()
}

fun Database.Read.getVasuDocumentSummaries(childId: UUID): List<VasuDocumentSummary> {
    // language=sql
    val sql = """
        SELECT 
            vd.id,
            vt.name,
            vd.modified_at,
            vd.published_at,
            vd.document_state
        FROM vasu_document vd
        JOIN vasu_content vc on vd.content_id = vc.id
        JOIN vasu_template vt on vd.template_id = vt.id
        JOIN child c on c.id = vd.child_id
        WHERE c.id = :childId
    """.trimIndent()

    return createQuery(sql)
        .bind("childId", childId)
        .mapTo<VasuDocumentSummary>()
        .list()
}
