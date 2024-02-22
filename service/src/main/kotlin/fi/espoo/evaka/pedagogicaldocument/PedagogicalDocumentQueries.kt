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
    @Suppress("DEPRECATION")
    return this.createQuery(
            """
            SELECT 
                a.id,
                a.name,
                a.content_type
            FROM attachment a
            JOIN pedagogical_document pd ON a.pedagogical_document_id = pd.id
            WHERE pd.id = :pedagogicalDocumentId
        """
                .trimIndent()
        )
        .bind("pedagogicalDocumentId", pedagogicalDocumentId)
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
    @Suppress("DEPRECATION")
    return this.createQuery(
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
                        coalesce(jsonb_agg(json_build_object(
                            'id', a.id,
                            'name', a.name,
                            'contentType', a.content_type
                        )), '[]'::jsonb)
                    FROM attachment a
                    WHERE a.pedagogical_document_id = pd.id
                ) AS attachments
            FROM pedagogical_document pd
            LEFT JOIN pedagogical_document_read pdr ON pd.id = pdr.pedagogical_document_id AND pdr.person_id = :userId
            WHERE pd.child_id = :childId
            ORDER BY pd.created DESC
        """
                .trimIndent()
        )
        .bind("childId", childId)
        .bind("userId", userId)
        .toList<PedagogicalDocumentCitizen>()
        .map { if (it.attachments.isEmpty()) it.copy(isRead = true) else it }
}

fun Database.Read.getPedagogicalDocumentChild(
    pedagogicalDocumentId: PedagogicalDocumentId
): ChildId {
    @Suppress("DEPRECATION")
    return createQuery(
            "SELECT child_id FROM pedagogical_document WHERE id = :pedagogicalDocumentId"
        )
        .bind("pedagogicalDocumentId", pedagogicalDocumentId)
        .exactlyOne<ChildId>()
}
