package fi.espoo.evaka.vasu

import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.updateExactlyOne
import fi.espoo.evaka.shared.domain.DateRange
import org.jdbi.v3.core.kotlin.mapTo
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

fun Database.Read.getVasuTemplate(id: UUID): VasuTemplate? {
    // language=sql
    val sql = """
        SELECT *
        FROM vasu_template
        WHERE id =:id
    """.trimIndent()

    return createQuery(sql)
        .bind("id", id)
        .mapTo<VasuTemplate>()
        .firstOrNull()
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
        SELECT :childId, :templateId, ct.id FROM content ct
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
    val sql = """
        UPDATE vasu_content
        SET content = :content
        WHERE id IN (SELECT vd.content_id FROM vasu_document vd WHERE vd.id = :id)
    """.trimIndent()

    createUpdate(sql)
        .bind("id", id)
        .bind("content", content)
        .updateExactlyOne()
}
