package fi.espoo.evaka.messaging

import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import org.jdbi.v3.core.kotlin.mapTo
import java.util.UUID

fun Database.Transaction.createMessage(
    user: AuthenticatedUser,
    title: String,
    content: String,
    guardians: Set<UUID>
) {
    // language=sql
    val insertMessage = """
        INSERT INTO message (title, content, created_by_employee)
        VALUES (:title, :content, :createdBy)
        RETURNING id
    """.trimIndent()
    val messageId = this.createQuery(insertMessage)
        .bind("title", title)
        .bind("content", content)
        .bind("createdBy", user.id)
        .mapTo<UUID>()
        .first()

    // language=sql
    val insertMessageInstance = """
        INSERT INTO message_instance (message_id, guardian_id)
        VALUES (:messageId, :guardianId)
    """.trimIndent()

    // todo use batch insert instead of for each
    guardians.forEach { guardianId ->
        this.createUpdate(insertMessageInstance)
            .bind("messageId", messageId)
            .bind("guardianId", guardianId)
            .execute()
    }
}

fun Database.Read.getGuardiansByDaycareGroup(groupId: UUID): Set<UUID> {
    // language=sql
    val sql = """
        SELECT DISTINCT g.guardian_id AS guardian_id
        FROM daycare d
        JOIN daycare_group dg ON d.id = dg.daycare_id
        JOIN placement pl ON d.id = pl.unit_id AND daterange(pl.start_date, pl.end_date, '[]') @> :date
        JOIN guardian g ON pl.child_id = g.child_id
        WHERE d.id = :unitId AND dg.id = :groupId
    """.trimIndent()
    return this.createQuery(sql)
        .bind("groupId", groupId)
        .mapTo<UUID>()
        .toSet()
}

fun Database.Read.getReceivedMessagesByGuardian(
    user: AuthenticatedUser
): List<ReceivedMessage> {
    // language=sql
    val sql = """
        SELECT DISTINCT m.id AS message_id, m.title, m.content, m.created as sent_at
        FROM message m
        JOIN message_instance mi ON m.id = mi.message_id
        WHERE mi.guardian_id = :userId
        ORDER BY m.created DESC
    """.trimIndent()

    return this.createQuery(sql)
        .bind("userId", user.id)
        .mapTo<ReceivedMessage>()
        .list()
}
