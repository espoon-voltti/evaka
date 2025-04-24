// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document.childdocument

import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import fi.espoo.evaka.shared.security.actionrule.toPredicate

private fun Database.Read.childDocumentCitizenSummaryQuery(
    user: AuthenticatedUser.Citizen,
    childDocumentPredicate: Predicate,
) = createQuery {
    sql(
        """
SELECT
    cd.id,
    cd.status,
    dt.type,
    dt.name AS template_name,
    cd.published_at,
    (NOT EXISTS(
        SELECT FROM child_document_read cdr
        WHERE cdr.person_id = ${bind(user.id)} AND cdr.document_id = cd.id
    )) AS unread,
    child.id AS child_id,
    child.first_name AS child_first_name,
    child.last_name AS child_last_name,
    child.date_of_birth AS child_date_of_birth,
    cd.answered_at,
    answered_by.id AS answered_by_id,
    CASE answered_by.type
        WHEN 'CITIZEN' THEN answered_by.name
        ELSE ''
    END AS answered_by_name,
    answered_by.type AS answered_by_type
FROM child_document cd
JOIN document_template dt ON cd.template_id = dt.id
JOIN person child ON cd.child_id = child.id
LEFT JOIN evaka_user answered_by ON cd.answered_by = answered_by.id
WHERE ${predicate(childDocumentPredicate.forTable("cd"))}
"""
    )
}

fun Database.Read.getChildDocumentCitizenSummaries(
    user: AuthenticatedUser.Citizen,
    childId: PersonId,
): List<ChildDocumentCitizenSummary> =
    childDocumentCitizenSummaryQuery(
            user,
            Predicate.all(
                Predicate { where("$it.child_id = ${bind(childId)}") },
                Predicate { where("$it.published_at IS NOT NULL") },
            ),
        )
        .toList<ChildDocumentCitizenSummary>()

fun Database.Read.getUnansweredChildDocuments(
    user: AuthenticatedUser.Citizen,
    filter: AccessControlFilter<ChildDocumentId>,
) =
    childDocumentCitizenSummaryQuery(
            user,
            Predicate.all(
                filter.toPredicate(),
                Predicate { where("$it.answered_at IS NULL AND $it.answered_by IS NULL") },
            ),
        )
        .toList<ChildDocumentCitizenSummary>()

fun Database.Read.getCitizenChildDocument(id: ChildDocumentId): ChildDocumentCitizenDetails? {
    return createQuery {
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
                    dt.placement_types as template_placement_types,
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
        }
        .exactlyOneOrNull<ChildDocumentCitizenDetails>()
}

fun Database.Transaction.markChildDocumentAsRead(
    user: AuthenticatedUser.Citizen,
    id: ChildDocumentId,
    now: HelsinkiDateTime,
) {
    createUpdate {
            sql(
                """
                INSERT INTO child_document_read (document_id, person_id, read_at) 
                VALUES (${bind(id)}, ${bind(user.id)}, ${bind(now)})
                ON CONFLICT DO NOTHING;
                """
            )
        }
        .execute()
}
