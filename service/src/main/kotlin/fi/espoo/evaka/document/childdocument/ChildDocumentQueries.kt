// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document.childdocument

import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database

fun Database.Transaction.insertChildDocument(
    document: ChildDocumentCreateRequest
): ChildDocumentId {
    return createQuery(
            """
            INSERT INTO child_document(child_id, template_id, content)
            VALUES (:childId, :templateId, :content)
            RETURNING id
        """
        )
        .bindKotlin(document)
        .bind("content", DocumentContent(answers = emptyList()))
        .mapTo<ChildDocumentId>()
        .one()
}

fun Database.Read.getChildDocuments(childId: PersonId): List<ChildDocumentSummary> {
    return createQuery(
            """
            SELECT cd.id, dt.type, cd.published
            FROM child_document cd
            JOIN document_template dt on cd.template_id = dt.id
            WHERE cd.child_id = :childId
        """
        )
        .bind("childId", childId)
        .mapTo<ChildDocumentSummary>()
        .list()
}

fun Database.Read.getChildDocument(id: ChildDocumentId): ChildDocumentDetails? {
    return createQuery(
            """
            SELECT 
                cd.id,
                cd.published,
                cd.content,
                p.id as child_id,
                p.first_name as child_first_name,
                p.last_name as child_last_name,
                p.date_of_birth as child_date_of_birth,
                dt.id as template_id,
                dt.name as template_name,
                dt.type as template_type,
                dt.language as template_language,
                dt.legal_basis as template_legal_basis,
                dt.confidential as template_confidential,
                dt.validity as template_validity,
                dt.published as template_published,
                dt.content as template_content
            FROM child_document cd
            JOIN document_template dt on cd.template_id = dt.id
            JOIN person p on cd.child_id = p.id
            WHERE cd.id = :id
        """
        )
        .bind("id", id)
        .mapTo<ChildDocumentDetails>()
        .firstOrNull()
}

fun Database.Transaction.updateDraftChildDocumentContent(
    id: ChildDocumentId,
    content: DocumentContent
) {
    createUpdate(
            """
            UPDATE child_document
            SET content = :content
            WHERE id = :id AND NOT published
        """
        )
        .bind("id", id)
        .bind("content", content)
        .updateExactlyOne()
}

fun Database.Transaction.publishChildDocument(id: ChildDocumentId) {
    createUpdate(
            """
            UPDATE child_document
            SET published = true
            WHERE id = :id
        """
        )
        .bind("id", id)
        .execute()
}
