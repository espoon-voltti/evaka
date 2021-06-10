package fi.espoo.evaka.vasu

import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.NotFound
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.mapper.Nested
import java.util.UUID

fun Database.Transaction.insertVasuTemplate(
    name: String,
    valid: DateRange,
    content: VasuContent
): UUID {
    // language=sql
    val sql = """
        INSERT INTO vasu_template (valid, name, content) 
        VALUES (:valid, :name, :content)
        RETURNING id
    """.trimIndent()

    return createQuery(sql)
        .bind("name", name)
        .bind("valid", valid)
        .bind("content", content)
        .mapTo<UUID>()
        .one()
}

fun Database.Read.getVasuTemplate(id: UUID): VasuTemplate {
    // language=sql
    val sql = """
        SELECT *
        FROM vasu_template
        WHERE id =:id
    """.trimIndent()

    return createQuery(sql)
        .bind("id", id)
        .mapTo<VasuTemplate>()
        .firstOrNull() ?: throw NotFound("template $id not found")
}

fun Database.Read.getVasuTemplates(): List<VasuTemplateSummary> {
    return createQuery("SELECT id, name, valid FROM vasu_template")
        .mapTo<VasuTemplateSummary>()
        .list()
}

fun Database.Transaction.insertVasuDocument(childId: UUID, templateId: UUID): UUID {
    // language=sql
    val sql = """
        WITH content AS (
            INSERT INTO vasu_content (content) 
            SELECT vt.content FROM vasu_template vt WHERE vt.id = :templateId
            RETURNING id
        )
        INSERT INTO vasu_document (child_id, template_id, content_id) 
        VALUES (:childId, :templateId, content.id)
        RETURNING id
    """.trimIndent()

    return createQuery(sql)
        .bind("childId", childId)
        .bind("templateId", templateId)
        .mapTo<UUID>()
        .one()
}

data class VasuDocumentResponse(
    val id: UUID,
    @Nested("child")
    val child: VasuDocumentResponseChild,
    val templateName: String,
    val content: VasuContent
)
data class VasuDocumentResponseChild(
    val id: UUID,
    val firstName: String,
    val lastName: String
)
fun Database.Read.getVasuDocumentResponse(id: UUID): VasuDocumentResponse {
    // language=sql
    val sql = """
        SELECT 
            vd.id,
            vd.child_id,
            p.first_name AS child_first_name,
            p.last_name AS child_last_name,
            vc.content
        FROM vasu_document vd
        JOIN vasu_content vc on vd.content_id = vc.id
        JOIN person p on p.id = vd.child_id
        WHERE vd.id =:id
    """.trimIndent()

    return createQuery(sql)
        .bind("id", id)
        .mapTo<VasuDocumentResponse>()
        .firstOrNull() ?: throw NotFound("template $id not found")
}

fun Database.Transaction.updateVasuDocument(id: UUID, content: VasuContent) {
    // language=sql
    val sql = """
        UPDATE vasu_content
        SET content = :content
        WHERE id IN (SELECT vd.content_id FROM vasu_document vd WHERE vd.id = :id)
    """.trimIndent()

    createUpdate(sql)
        .bind("id", id)
        .bind("content", content)
        .execute()
        .also { if (it < 1) throw NotFound("document content to update not found") }
}
