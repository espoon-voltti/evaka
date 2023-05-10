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

fun Database.Read.getChildDocuments(childId: PersonId): List<ChildDocument> {
    return createQuery(
            """
            SELECT 
                cd.id,
                cd.child_id,
                cd.published,
                dt.id as template_id,
                dt.name as template_name,
                dt.type as template_type,
                dt.validity as template_validity,
                dt.published as template_published,
                dt.content as template_content,
                cd.content
            FROM child_document cd
            JOIN document_template dt on cd.template_id = dt.id
            WHERE cd.child_id = :childId
        """
        )
        .bind("childId", childId)
        .mapTo<ChildDocument>()
        .list()
}

fun Database.Read.getChildDocument(id: ChildDocumentId): ChildDocument? {
    return createQuery(
            """
            SELECT 
                cd.id,
                cd.child_id,
                cd.published,
                dt.id as template_id,
                dt.name as template_name,
                dt.type as template_type,
                dt.validity as template_validity,
                dt.published as template_published,
                dt.content as template_content,
                cd.content
            FROM child_document cd
            JOIN document_template dt on cd.template_id = dt.id
            WHERE cd.id = :id
        """
        )
        .bind("id", id)
        .mapTo<ChildDocument>()
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
