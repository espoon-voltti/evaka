// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import org.jdbi.v3.core.kotlin.mapTo

data class Recipient(
    val personId: PersonId,
    val firstName: String,
    val lastName: String,
    val blocklisted: Boolean
)

fun Database.Transaction.addToBlocklist(childId: ChildId, recipientId: PersonId) {
    // language=sql
    val sql = """
        INSERT INTO messaging_blocklist (child_id, blocked_recipient)
        VALUES (:childId, :recipient)
        ON CONFLICT DO NOTHING
    """.trimIndent()

    this.createUpdate(sql)
        .bind("childId", childId)
        .bind("recipient", recipientId)
        .execute()
}

fun Database.Transaction.removeFromBlocklist(childId: ChildId, recipientId: PersonId) {
    // language=sql
    val sql = """
        DELETE FROM messaging_blocklist
        WHERE child_id = :childId AND blocked_recipient = :recipient
    """.trimIndent()

    this.createUpdate(sql)
        .bind("childId", childId)
        .bind("recipient", recipientId)
        .execute()
}

fun Database.Read.fetchRecipients(childId: ChildId): List<Recipient> {
    // language=sql
    val sql = """
        SELECT 
            g.guardian_id as person_id,
            p.first_name,
            p.last_name,
            EXISTS(SELECT 1 FROM messaging_blocklist bl WHERE bl.child_id = :childId AND bl.blocked_recipient = g.guardian_id) AS blocklisted
        FROM guardian g
        JOIN person p ON g.guardian_id = p.id
        WHERE g.child_id = :childId
    """.trimIndent()

    return this.createQuery(sql)
        .bind("childId", childId)
        .mapTo<Recipient>()
        .list()
}
