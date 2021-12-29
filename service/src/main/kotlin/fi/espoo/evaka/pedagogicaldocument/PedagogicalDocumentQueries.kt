// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pedagogicaldocument

import fi.espoo.evaka.shared.PedagogicalDocumentId
import fi.espoo.evaka.shared.db.Database
import org.jdbi.v3.core.kotlin.mapTo

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
        """.trimIndent()
    )
        .bind("pedagogicalDocumentId", pedagogicalDocumentId)
        .mapTo<Attachment>()
        .list()
}
