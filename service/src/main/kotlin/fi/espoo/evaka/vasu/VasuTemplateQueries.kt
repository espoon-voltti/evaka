package fi.espoo.evaka.vasu

import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.updateExactlyOne
import fi.espoo.evaka.shared.domain.FiniteDateRange
import org.jdbi.v3.core.kotlin.mapTo
import java.util.UUID

fun Database.Transaction.insertVasuTemplate(
    name: String,
    valid: FiniteDateRange,
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
    return createQuery(
        """
        SELECT 
            id,
            name,
            valid,
            (SELECT count(*) FROM vasu_document vd WHERE vd.template_id = vt.id) AS document_count
        FROM vasu_template vt
    """
    )
        .mapTo<VasuTemplateSummary>()
        .list()
}

fun Database.Transaction.updateVasuTemplateContent(id: UUID, content: VasuContent) {
    // language=sql
    val sql = """
        UPDATE vasu_template
        SET content = :content
        WHERE id = :id
    """.trimIndent()

    createUpdate(sql)
        .bind("id", id)
        .bind("content", content)
        .updateExactlyOne()
}

fun Database.Transaction.deleteUnusedVasuTemplate(id: UUID) {
    // language=sql
    val sql = """
        DELETE FROM vasu_template vt
        WHERE vt.id = :id AND NOT EXISTS(SELECT 1 FROM vasu_document vd WHERE vd.template_id = vt.id)
    """.trimIndent()

    createUpdate(sql)
        .bind("id", id)
        .updateExactlyOne(notFoundMsg = "template $id not found or is in use")
}
