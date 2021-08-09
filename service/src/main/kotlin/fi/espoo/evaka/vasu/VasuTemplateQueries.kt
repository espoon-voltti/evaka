// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vasu

import fi.espoo.evaka.shared.VasuTemplateId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.updateExactlyOne
import fi.espoo.evaka.shared.domain.FiniteDateRange
import org.jdbi.v3.core.kotlin.mapTo

fun Database.Transaction.insertVasuTemplate(
    name: String,
    valid: FiniteDateRange,
    language: VasuLanguage,
    content: VasuContent
): VasuTemplateId {
    // language=sql
    val sql = """
        INSERT INTO vasu_template (valid, language, name, content) 
        VALUES (:valid, :language, :name, :content)
        RETURNING id
    """.trimIndent()

    return createQuery(sql)
        .bind("name", name)
        .bind("valid", valid)
        .bind("language", language)
        .bind("content", content)
        .mapTo<VasuTemplateId>()
        .one()
}

fun Database.Read.getVasuTemplate(id: VasuTemplateId): VasuTemplate? {
    // language=sql
    val sql = """
        SELECT vt.*, (SELECT count(*) FROM vasu_document vd WHERE vt.id = vd.template_id) AS document_count
        FROM vasu_template vt
        WHERE vt.id = :id
    """.trimIndent()

    return createQuery(sql)
        .bind("id", id)
        .mapTo<VasuTemplate>()
        .firstOrNull()
}

fun Database.Read.getVasuTemplates(validOnly: Boolean): List<VasuTemplateSummary> {
    return createQuery(
        """
        SELECT 
            id,
            name,
            valid,
            language,
            (SELECT count(*) FROM vasu_document vd WHERE vd.template_id = vt.id) AS document_count
        FROM vasu_template vt
        ${if (validOnly) "WHERE valid @> NOW()::date" else ""}
    """
    )
        .mapTo<VasuTemplateSummary>()
        .list()
}

fun Database.Transaction.updateVasuTemplateContent(id: VasuTemplateId, content: VasuContent) {
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

fun Database.Read.getVasuTemplateForUpdate(id: VasuTemplateId): VasuTemplateSummary? {
    // language=sql
    val sql = """
        SELECT *,
               (SELECT COUNT(*) FROM vasu_document vd WHERE vd.template_id = vt.id) AS document_count
        FROM vasu_template vt
        WHERE id = :id
        FOR UPDATE
    """.trimIndent()

    return createQuery(sql)
        .bind("id", id)
        .mapTo<VasuTemplateSummary>()
        .firstOrNull()
}

fun Database.Transaction.updateVasuTemplate(
    id: VasuTemplateId,
    params: VasuTemplateUpdate
) {
    // language=sql
    val sql = """
        UPDATE vasu_template
        SET name = :name, valid = :valid, language = :language
        WHERE id = :id
    """.trimIndent()

    createUpdate(sql)
        .bind("id", id)
        .bind("name", params.name)
        .bind("valid", params.valid)
        .bind("language", params.language)
        .updateExactlyOne()
}

fun Database.Transaction.deleteUnusedVasuTemplate(id: VasuTemplateId) {
    // language=sql
    val sql = """
        DELETE FROM vasu_template vt
        WHERE vt.id = :id AND NOT EXISTS(SELECT 1 FROM vasu_document vd WHERE vd.template_id = vt.id)
    """.trimIndent()

    createUpdate(sql)
        .bind("id", id)
        .updateExactlyOne(notFoundMsg = "template $id not found or is in use")
}
