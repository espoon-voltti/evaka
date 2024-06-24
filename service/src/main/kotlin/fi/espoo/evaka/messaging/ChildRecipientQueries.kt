// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database

data class Recipient(
    val personId: PersonId,
    val firstName: String,
    val lastName: String,
    val blocklisted: Boolean
)

fun Database.Transaction.addToBlocklist(
    childId: ChildId,
    recipientId: PersonId
) {
    createUpdate {
        sql(
            """
INSERT INTO messaging_blocklist (child_id, blocked_recipient)
VALUES (${bind(childId)}, ${bind(recipientId)})
ON CONFLICT DO NOTHING
"""
        )
    }.execute()
}

fun Database.Transaction.removeFromBlocklist(
    childId: ChildId,
    recipientId: PersonId
) {
    createUpdate {
        sql(
            """
DELETE FROM messaging_blocklist
WHERE child_id = ${bind(childId)} AND blocked_recipient = ${bind(recipientId)}
"""
        )
    }.execute()
}

fun Database.Read.fetchRecipients(childId: ChildId): List<Recipient> =
    createQuery {
        sql(
            """
        SELECT 
            g.guardian_id as person_id,
            p.first_name,
            p.last_name,
            EXISTS(SELECT 1 FROM messaging_blocklist bl WHERE bl.child_id = ${bind(
                childId
            )} AND bl.blocked_recipient = g.guardian_id) AS blocklisted
        FROM guardian g
        JOIN person p ON g.guardian_id = p.id
        WHERE g.child_id = ${bind(childId)}
    """
        )
    }.toList<Recipient>()
