// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document

import fi.espoo.evaka.shared.DocumentTemplateId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange

fun Database.Transaction.insertTemplate(template: DocumentTemplateCreateRequest): DocumentTemplate {
    @Suppress("DEPRECATION")
    return createQuery(
            """
        INSERT INTO document_template (name, type, language, confidential, legal_basis, validity, content) 
        VALUES (:name, :type, :language, :confidential, :legalBasis, :validity, :content::jsonb)
        RETURNING *
    """
                .trimIndent()
        )
        .bindKotlin(template)
        .bind("content", DocumentTemplateContent(sections = emptyList()))
        .exactlyOne<DocumentTemplate>()
}

fun Database.Transaction.importTemplate(template: ExportedDocumentTemplate): DocumentTemplate =
    @Suppress("DEPRECATION")
    createQuery(
            """
        INSERT INTO document_template (name, type, language, confidential, legal_basis, validity, content)
        VALUES (:name, :type, :language, :confidential, :legalBasis, :validity, :content)
        RETURNING *
    """
        )
        .bindKotlin(template)
        .exactlyOne<DocumentTemplate>()

fun Database.Transaction.duplicateTemplate(
    id: DocumentTemplateId,
    template: DocumentTemplateCreateRequest
): DocumentTemplate {
    @Suppress("DEPRECATION")
    return createQuery(
            """
        INSERT INTO document_template (name, type, language, confidential, legal_basis, validity, content) 
        SELECT :name, :type, :language, :confidential, :legalBasis, :validity, content FROM document_template WHERE id = :id
        RETURNING *
    """
                .trimIndent()
        )
        .bind("id", id)
        .bindKotlin(template)
        .exactlyOne<DocumentTemplate>()
}

fun Database.Read.getTemplateSummaries(): List<DocumentTemplateSummary> {
    @Suppress("DEPRECATION")
    return createQuery(
            """
        SELECT id, name, type, language, validity, published
        FROM document_template
    """
                .trimIndent()
        )
        .toList<DocumentTemplateSummary>()
}

fun Database.Read.getTemplate(id: DocumentTemplateId): DocumentTemplate? {
    @Suppress("DEPRECATION")
    return createQuery("SELECT * FROM document_template WHERE id = :id")
        .bind("id", id)
        .exactlyOneOrNull<DocumentTemplate>()
}

fun Database.Read.exportTemplate(id: DocumentTemplateId): ExportedDocumentTemplate? {
    @Suppress("DEPRECATION")
    return createQuery("SELECT * FROM document_template WHERE id = :id")
        .bind("id", id)
        .exactlyOneOrNull<ExportedDocumentTemplate>()
}

fun Database.Transaction.updateDraftTemplateContent(
    id: DocumentTemplateId,
    content: DocumentTemplateContent
) {
    @Suppress("DEPRECATION")
    createUpdate(
            """
        UPDATE document_template
        SET content = :content
        WHERE id = :id AND published = false
    """
                .trimIndent()
        )
        .bind("id", id)
        .bind("content", content)
        .updateExactlyOne()
}

fun Database.Transaction.updateTemplateValidity(id: DocumentTemplateId, validity: DateRange) {
    @Suppress("DEPRECATION")
    createUpdate(
            """
        UPDATE document_template
        SET validity = :validity
        WHERE id = :id
    """
                .trimIndent()
        )
        .bind("id", id)
        .bind("validity", validity)
        .updateExactlyOne()
}

fun Database.Transaction.publishTemplate(id: DocumentTemplateId) {
    @Suppress("DEPRECATION")
    createUpdate(
            """
        UPDATE document_template
        SET published = true
        WHERE id = :id
    """
                .trimIndent()
        )
        .bind("id", id)
        .updateExactlyOne()
}

fun Database.Transaction.deleteDraftTemplate(id: DocumentTemplateId) {
    @Suppress("DEPRECATION")
    createUpdate(
            """
        DELETE FROM document_template 
        WHERE id = :id AND published = false
    """
                .trimIndent()
        )
        .bind("id", id)
        .updateExactlyOne()
}
