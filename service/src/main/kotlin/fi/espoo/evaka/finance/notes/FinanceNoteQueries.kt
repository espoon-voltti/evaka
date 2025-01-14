// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.finance.notes

import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.FinanceNoteId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime

fun Database.Read.getFinanceNotes(adultId: EvakaUserId): List<FinanceNote> =
    createQuery {
        sql(
            """
SELECT
    n.id,
    n.content,
    n.created_at,
    ceu.id AS created_by_id,
    ceu.type AS created_by_type,
    ceu.name AS created_by_name,
    n.modified_at,
    meu.id AS modified_by_id,
    neu.type AS modified_by_type,
    neu.name AS modified_by_name
FROM finance_note n
LEFT JOIN evaka_user ceu ON n.created_by = ceu.id
LEFT JOIN evaka_user meu ON n.modified_by = meu.id
WHERE n.adult_id = ${bind(adultId)}
ORDER BY n.created_at
"""
        )
    }
    .toList()

fun Database.Transaction.createFinanceNote(
    content: String,
    createdBy: EvakaUserId,
    now: HelsinkiDateTime,
): FinanceNote =
    createQuery {
        sql(
            """
WITH new_note AS (
    INSERT INTO finance_note (
        content,
        created_at,
        created_by,
        modified_at,
        modified_by
    ) VALUES (
        ${bind(content)},
        ${bind(now)},
        ${bind(createdBy)},
        ${bind(now)},
        ${bind(createdBy)}
    ) RETURNING *
)
SELECT
    n.id,
    n.content,
    n.created_at,
    eu.id AS created_by_id,
    eu.type AS created_by_type,
    eu.name AS created_by_name,
    n.modified_at,
    eu.id AS modified_by_id,
    eu.type AS modified_by_type,
    eu.name AS modified_by_name
FROM new_note n
LEFT JOIN evaka_user eu ON n.created_by = eu.id
"""
        )
    }
    .exactlyOne()

fun Database.Transaction.deleteFinanceNote(id: FinanceNoteId) = execute {
    sql("DELETE FROM finance_note WHERE id = ${bind(id)}"
}
