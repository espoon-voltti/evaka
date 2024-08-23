// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pedagogicaldocument

import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PedagogicalDocumentId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.jdbi.v3.json.Json

fun Database.Read.getPedagogicalDocumentAttachments(
    pedagogicalDocumentId: PedagogicalDocumentId
): List<Attachment> {
    return createQuery {
            sql(
                """
                SELECT 
                    a.id,
                    a.name,
                    a.content_type
                FROM attachment a
                JOIN pedagogical_document pd ON a.pedagogical_document_id = pd.id
                WHERE pd.id = ${bind(pedagogicalDocumentId)}
                """
            )
        }
        .toList<Attachment>()
}

data class PedagogicalDocumentCitizen(
    val id: PedagogicalDocumentId,
    val childId: ChildId,
    val description: String,
    @Json val attachments: List<Attachment> = emptyList(),
    val created: HelsinkiDateTime,
    val isRead: Boolean
)

fun Database.Read.getChildPedagogicalDocuments(
    childId: ChildId,
    userId: PersonId
): List<PedagogicalDocumentCitizen> {
    return createQuery {
            sql(
                """
SELECT 
    pd.id,
    pd.child_id,
    pd.description,
    pd.created,
    pd.updated,
    pdr.read_at is not null is_read,
    (
        SELECT
            coalesce(jsonb_agg(jsonb_build_object(
                'id', a.id,
                'name', a.name,
                'contentType', a.content_type
            )), '[]'::jsonb)
        FROM attachment a
        WHERE a.pedagogical_document_id = pd.id
    ) AS attachments
FROM pedagogical_document pd
LEFT JOIN pedagogical_document_read pdr ON pd.id = pdr.pedagogical_document_id AND pdr.person_id = ${bind(userId)}
WHERE pd.child_id = ${bind(childId)}
ORDER BY pd.created DESC
"""
            )
        }
        .toList<PedagogicalDocumentCitizen>()
        .map { if (it.attachments.isEmpty()) it.copy(isRead = true) else it }
}

fun Database.Read.getPedagogicalDocumentChild(
    pedagogicalDocumentId: PedagogicalDocumentId
): ChildId {
    return createQuery {
            sql(
                "SELECT child_id FROM pedagogical_document WHERE id = ${bind(pedagogicalDocumentId)}"
            )
        }
        .exactlyOne<ChildId>()
}
