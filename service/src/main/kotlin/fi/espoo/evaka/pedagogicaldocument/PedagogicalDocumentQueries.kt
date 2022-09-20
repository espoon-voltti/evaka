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
    return this.createQuery(
            """
            SELECT 
                a.id,
                a.name,
                a.content_type
            FROM attachment a
            JOIN pedagogical_document pd ON a.pedagogical_document_id = pd.id
            WHERE pd.id = :pedagogicalDocumentId
        """.trimIndent(
            )
        )
        .bind("pedagogicalDocumentId", pedagogicalDocumentId)
        .mapTo<Attachment>()
        .list()
}

data class PedagogicalDocumentCitizen(
    val id: PedagogicalDocumentId,
    val childId: ChildId,
    val description: String,
    @Json val attachments: List<Attachment> = emptyList(),
    val created: HelsinkiDateTime,
    val isRead: Boolean
)

fun Database.Read.getPedagogicalDocumentsByChildForGuardian(
    childId: ChildId,
    guardianId: PersonId
): List<PedagogicalDocumentCitizen> {
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
            JOIN guardian g ON pd.child_id = g.child_id
            LEFT JOIN pedagogical_document_read pdr ON pd.id = pdr.pedagogical_document_id AND pdr.person_id = g.guardian_id
            WHERE pd.child_id = :childId AND g.guardian_id = :guardianId
            ORDER BY pd.created DESC
        """.trimIndent(
            )
        )
        .bind("childId", childId)
        .bind("guardianId", guardianId)
        .mapTo<PedagogicalDocumentCitizen>()
        .list()
}
