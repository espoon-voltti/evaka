// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document

import fi.espoo.evaka.shared.DocumentTemplateId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange

fun Database.Transaction.insertTemplate(template: DocumentTemplateBasicsRequest): DocumentTemplate {
    return createQuery {
            sql(
                """
INSERT INTO document_template (name, type, placement_types,  language, confidential, legal_basis, validity, process_definition_number, archive_duration_months, content) 
VALUES (${bind(template.name)}, ${bind(template.type)}, ${bind(template.placementTypes)}, ${bind(template.language)}, ${bind(template.confidential)}, ${bind(template.legalBasis)}, ${bind(template.validity)}, ${bind(template.processDefinitionNumber)}, ${bind(template.archiveDurationMonths)}, ${bind(DocumentTemplateContent(sections = emptyList()))}::jsonb)
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
INSERT INTO document_template (name, type, placement_types, language, confidential, legal_basis, validity, process_definition_number, archive_duration_months, content)
VALUES (${bind(template.name)}, ${bind(template.type)}, ${bind(template.placementTypes)}, ${bind(template.language)}, ${bind(template.confidential)}, ${bind(template.legalBasis)}, ${bind(template.validity)}, ${bind(template.processDefinitionNumber)}, ${bind(template.archiveDurationMonths)}, ${bind(template.content)})
RETURNING *
"""
            )
        }
        .exactlyOne<DocumentTemplate>()

fun Database.Transaction.duplicateTemplate(
    id: DocumentTemplateId,
    template: DocumentTemplateBasicsRequest,
): DocumentTemplate {
    return createQuery {
            sql(
                """
INSERT INTO document_template (name, type, placement_types, language, confidential, legal_basis, validity, process_definition_number, archive_duration_months, content) 
SELECT ${bind(template.name)}, ${bind(template.type)}, ${bind(template.placementTypes)}, ${bind(template.language)}, ${bind(template.confidential)}, ${bind(template.legalBasis)}, ${bind(template.validity)}, ${bind(template.processDefinitionNumber)}, ${bind(template.archiveDurationMonths)}, content FROM document_template WHERE id = ${bind(id)}
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
                SELECT
                    id,
                    name,
                    type,
                    placement_types,
                    language,
                    validity,
                    published,
                    (SELECT count(*) FROM child_document WHERE template_id = dt.id) AS document_count
                FROM document_template dt
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

fun Database.Transaction.updateDraftTemplateBasics(
    id: DocumentTemplateId,
    basics: DocumentTemplateBasicsRequest,
) {
    createUpdate {
            sql(
                """
                UPDATE document_template
                SET
                    name = ${bind(basics.name)}, 
                    type = ${bind(basics.type)},
                    placement_types = ${bind(basics.placementTypes)},
                    language = ${bind(basics.language)},
                    confidential = ${bind(basics.confidential)},
                    legal_basis = ${bind(basics.legalBasis)},
                    validity = ${bind(basics.validity)},
                    process_definition_number = ${bind(basics.processDefinitionNumber)},
                    archive_duration_months = ${bind(basics.archiveDurationMonths)}
                WHERE id = ${bind(id)} AND published = false
                """
            )
        }
        .updateExactlyOne()
}

fun Database.Transaction.updateDraftTemplateContent(
    id: DocumentTemplateId,
    content: DocumentTemplateContent,
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

// not for production use
fun Database.Transaction.forceUnpublishTemplate(id: DocumentTemplateId) {
    execute {
        sql(
            """
        WITH documents_to_delete AS (
            SELECT id, process_id FROM child_document
            WHERE template_id = ${bind(id)}
            FOR UPDATE 
        ), delete_processes AS (
            DELETE FROM archived_process ap
            WHERE ap.id IN (SELECT d2d.process_id FROM documents_to_delete d2d)
        ), delete_documents AS (
            DELETE FROM child_document cd WHERE cd.id IN (SELECT d2d.id FROM documents_to_delete d2d)
        )
        UPDATE document_template
        SET published = false
        WHERE id = ${bind(id)}
    """
        )
    }
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
