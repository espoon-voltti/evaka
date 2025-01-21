// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.finance.notes

import fi.espoo.evaka.shared.FinanceNoteId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime

fun Database.Read.getFinanceNotes(personId: PersonId): List<FinanceNote> =
    createQuery {
            sql(
                """
SELECT
    n.id,
    n.content,
    n.created_at,
    c.id AS created_by_id,
    c.type AS created_by_type,
    c.name AS created_by_name,
    n.modified_at,
    m.id AS modified_by_id,
    m.type AS modified_by_type,
    m.name AS modified_by_name
FROM finance_note n
LEFT JOIN evaka_user c ON n.created_by = c.id
LEFT JOIN evaka_user m ON n.modified_by = m.id
WHERE n.person_id = ${bind(personId)}
ORDER BY n.created_at
"""
            )
        }
        .toList()

fun Database.Transaction.createFinanceNote(
    personId: PersonId,
    content: String,
    user: AuthenticatedUser.Employee,
    now: HelsinkiDateTime,
): FinanceNoteId =
    createQuery {
            sql(
                """
    INSERT INTO finance_note (
        person_id,
        content,
        created_at,
        created_by,
        modified_at,
        modified_by
    ) VALUES (
        ${bind(personId)},
        ${bind(content)},
        ${bind(now)},
        ${bind(user.evakaUserId)},
        ${bind(now)},
        ${bind(user.evakaUserId)},
    ) RETURNING id
"""
            )
        }
        .exactlyOne()

fun Database.Transaction.updateFinanceNote(
    id: FinanceNoteId,
    content: String,
    user: AuthenticatedUser.Employee,
    now: HelsinkiDateTime,
) =
    createUpdate {
            sql(
                """
    UPDATE finance_note SET
        content = ${bind(content)},
        modified_at = ${bind(now)},
        modified_by = ${bind(user.evakaUserId)}
    WHERE id = ${bind(id)}
"""
            )
        }
        .execute()

fun Database.Transaction.deleteFinanceNote(id: FinanceNoteId) = execute {
    sql("DELETE FROM finance_note WHERE id = ${bind(id)}")
}
