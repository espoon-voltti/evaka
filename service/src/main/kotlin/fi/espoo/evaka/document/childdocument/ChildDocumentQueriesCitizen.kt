// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document.childdocument

import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime

fun Database.Read.getChildDocumentCitizenSummaries(
    user: AuthenticatedUser.Citizen,
    childId: PersonId
): List<ChildDocumentCitizenSummary> =
    createQuery {
        sql(
            """
                SELECT cd.id, cd.status, dt.type, cd.published_at, dt.name as template_name,
                    (NOT EXISTS(
                        SELECT 1 FROM child_document_read cdr 
                        WHERE cdr.person_id = ${bind(user.id)} AND cdr.document_id = cd.id
                    )) as unread
                FROM child_document cd
                JOIN document_template dt on cd.template_id = dt.id
                WHERE cd.child_id = ${bind(childId)} AND published_at IS NOT NULL
                """
        )
    }.toList<ChildDocumentCitizenSummary>()
        .filter { !it.type.isMigrated() }

fun Database.Read.getCitizenChildDocument(id: ChildDocumentId): ChildDocumentCitizenDetails? =
    createQuery {
        sql(
            """
                SELECT 
                    cd.id,
                    cd.status,
                    cd.published_at,
                    cd.document_key IS NOT NULL AS downloadable,
                    cd.published_content AS content,
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
                WHERE cd.id = ${bind(id)} AND published_at IS NOT NULL
                """
        )
    }.exactlyOneOrNull<ChildDocumentCitizenDetails>()
        ?.takeIf { !it.template.type.isMigrated() }

fun Database.Transaction.markChildDocumentAsRead(
    user: AuthenticatedUser.Citizen,
    id: ChildDocumentId,
    now: HelsinkiDateTime
) {
    createUpdate {
        sql(
            """
                INSERT INTO child_document_read (document_id, person_id, read_at) 
                VALUES (${bind(id)}, ${bind(user.id)}, ${bind(now)})
                ON CONFLICT DO NOTHING;
                """
        )
    }.execute()
}
