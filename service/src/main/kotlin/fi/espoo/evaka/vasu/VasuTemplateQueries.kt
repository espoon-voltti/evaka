// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vasu

import fi.espoo.evaka.shared.VasuTemplateId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange

fun Database.Transaction.insertVasuTemplate(
    name: String,
    valid: FiniteDateRange,
    type: CurriculumType,
    language: VasuLanguage,
    content: VasuContent
): VasuTemplateId {
    // language=sql
    val sql =
        """
        INSERT INTO curriculum_template (valid, type, language, name, content)
        VALUES (:valid, :type, :language, :name, :content)
        RETURNING id
    """
            .trimIndent()

    @Suppress("DEPRECATION")
    return createQuery(sql)
        .bind("name", name)
        .bind("valid", valid)
        .bind("type", type)
        .bind("language", language)
        .bind("content", content)
        .exactlyOne<VasuTemplateId>()
}

fun Database.Read.getVasuTemplate(id: VasuTemplateId): VasuTemplate? {
    // language=sql
    val sql =
        """
        SELECT ct.*, (SELECT count(*) FROM curriculum_document cd WHERE ct.id = cd.template_id) AS document_count
        FROM curriculum_template ct
        WHERE ct.id = :id
    """
            .trimIndent()

    @Suppress("DEPRECATION") return createQuery(sql).bind("id", id).exactlyOneOrNull<VasuTemplate>()
}

fun Database.Read.getVasuTemplates(
    clock: EvakaClock,
    validOnly: Boolean
): List<VasuTemplateSummary> {
    @Suppress("DEPRECATION")
    return createQuery(
            """
        SELECT 
            id,
            name,
            valid,
            type,
            language,
            (SELECT count(*) FROM curriculum_document cd WHERE cd.template_id = ct.id) AS document_count
        FROM curriculum_template ct
        ${if (validOnly) "WHERE valid @> :today" else ""}
    """
        )
        .apply { if (validOnly) bind("today", clock.today()) }
        .toList<VasuTemplateSummary>()
}

fun Database.Transaction.updateVasuTemplateContent(id: VasuTemplateId, content: VasuContent) {
    // language=sql
    val sql =
        """
        UPDATE curriculum_template
        SET content = :content
        WHERE id = :id
    """
            .trimIndent()

    @Suppress("DEPRECATION")
    createUpdate(sql).bind("id", id).bind("content", content).updateExactlyOne()
}

fun Database.Read.getVasuTemplateForUpdate(id: VasuTemplateId): VasuTemplateSummary? {
    // language=sql
    val sql =
        """
SELECT
    id,
    name,
    valid,
    type,
    language,
    (SELECT COUNT(*) FROM curriculum_document cd WHERE cd.template_id = ct.id) AS document_count
FROM curriculum_template ct
WHERE id = :id
FOR UPDATE
    """
            .trimIndent()

    @Suppress("DEPRECATION")
    return createQuery(sql).bind("id", id).exactlyOneOrNull<VasuTemplateSummary>()
}

fun Database.Transaction.updateVasuTemplate(id: VasuTemplateId, params: VasuTemplateUpdate) {
    // language=sql
    val sql =
        """
        UPDATE curriculum_template
        SET name = :name, valid = :valid
        WHERE id = :id
    """
            .trimIndent()

    @Suppress("DEPRECATION")
    createUpdate(sql)
        .bind("id", id)
        .bind("name", params.name)
        .bind("valid", params.valid)
        .updateExactlyOne()
}

fun Database.Transaction.deleteUnusedVasuTemplate(id: VasuTemplateId) {
    // language=sql
    val sql =
        """
        DELETE FROM curriculum_template ct
        WHERE ct.id = :id AND NOT EXISTS(SELECT 1 FROM curriculum_document cd WHERE cd.template_id = ct.id)
    """
            .trimIndent()

    @Suppress("DEPRECATION")
    createUpdate(sql)
        .bind("id", id)
        .updateExactlyOne(notFoundMsg = "template $id not found or is in use")
}
