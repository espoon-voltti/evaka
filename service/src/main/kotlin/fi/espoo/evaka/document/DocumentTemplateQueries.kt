// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document

import fi.espoo.evaka.shared.DocumentTemplateId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange

fun Database.Transaction.insertTemplate(template: DocumentTemplateCreateRequest): DocumentTemplate {
    return createQuery {
            sql(
                """
INSERT INTO document_template (name, type, language, confidential, legal_basis, validity, content) 
VALUES (${bind(template.name)}, ${bind(template.type)}, ${bind(template.language)}, ${bind(template.confidential)}, ${bind(template.legalBasis)}, ${bind(template.validity)}, ${bind(DocumentTemplateContent(sections = emptyList()))}::jsonb)
RETURNING *
"""
            )
        }
        .exactlyOne<DocumentTemplate>()
}

fun Database.Transaction.importTemplate(template: ExportedDocumentTemplate): DocumentTemplate =
    createQuery {
            sql(
                """
INSERT INTO document_template (name, type, language, confidential, legal_basis, validity, content)
VALUES (${bind(template.name)}, ${bind(template.type)}, ${bind(template.language)}, ${bind(template.confidential)}, ${bind(template.legalBasis)}, ${bind(template.validity)}, ${bind(template.content)})
RETURNING *
"""
            )
        }
        .exactlyOne<DocumentTemplate>()

fun Database.Transaction.duplicateTemplate(
    id: DocumentTemplateId,
    template: DocumentTemplateCreateRequest
): DocumentTemplate {
    return createQuery {
            sql(
                """
INSERT INTO document_template (name, type, language, confidential, legal_basis, validity, content) 
SELECT ${bind(template.name)}, ${bind(template.type)}, ${bind(template.language)}, ${bind(template.confidential)}, ${bind(template.legalBasis)}, ${bind(template.validity)}, content FROM document_template WHERE id = ${bind(id)}
RETURNING *
"""
            )
        }
        .exactlyOne<DocumentTemplate>()
}

fun Database.Read.getTemplateSummaries(): List<DocumentTemplateSummary> {
    return createQuery {
            sql(
                """
                SELECT id, name, type, language, validity, published
                FROM document_template
                """
            )
        }
        .toList<DocumentTemplateSummary>()
}

fun Database.Read.getTemplate(id: DocumentTemplateId): DocumentTemplate? {
    return createQuery { sql("SELECT * FROM document_template WHERE id = ${bind(id)}") }
        .exactlyOneOrNull<DocumentTemplate>()
}

fun Database.Read.exportTemplate(id: DocumentTemplateId): ExportedDocumentTemplate? {
    return createQuery { sql("SELECT * FROM document_template WHERE id = ${bind(id)}") }
        .exactlyOneOrNull<ExportedDocumentTemplate>()
}

fun Database.Transaction.updateDraftTemplateContent(
    id: DocumentTemplateId,
    content: DocumentTemplateContent
) {
    createUpdate {
            sql(
                """
                UPDATE document_template
                SET content = ${bind(content)}
                WHERE id = ${bind(id)} AND published = false
                """
            )
        }
        .updateExactlyOne()
}

fun Database.Transaction.updateTemplateValidity(id: DocumentTemplateId, validity: DateRange) {
    createUpdate {
            sql(
                """
                UPDATE document_template
                SET validity = ${bind(validity)}
                WHERE id = ${bind(id)}
                """
            )
        }
        .updateExactlyOne()
}

fun Database.Transaction.publishTemplate(id: DocumentTemplateId) {
    createUpdate {
            sql(
                """
                UPDATE document_template
                SET published = true
                WHERE id = ${bind(id)}
                """
            )
        }
        .updateExactlyOne()
}

fun Database.Transaction.deleteDraftTemplate(id: DocumentTemplateId) {
    createUpdate {
            sql(
                """
                DELETE FROM document_template 
                WHERE id = ${bind(id)} AND published = false
                """
            )
        }
        .updateExactlyOne()
}
