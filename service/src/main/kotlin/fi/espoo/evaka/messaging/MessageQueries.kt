package fi.espoo.evaka.messaging

import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.utils.zoneId
import org.jdbi.v3.core.kotlin.mapTo
import java.time.OffsetDateTime
import java.util.UUID

fun Database.Transaction.createMessage(
    user: AuthenticatedUser,
    title: String,
    content: String,
    recipients: Set<MessageRecipient>
) {
    // language=sql
    val insertMessage = """
        INSERT INTO message (type, title, content, created_by_employee)
        VALUES (:type, :title, :content, :createdBy)
        RETURNING id
    """.trimIndent()
    val messageId = this.createQuery(insertMessage)
        .bind("type", MessageType.RELEASE)
        .bind("title", title)
        .bind("content", content)
        .bind("createdBy", user.id)
        .mapTo<UUID>()
        .first()

    // language=sql
    val insertMessageInstance = """
        INSERT INTO message_instance (message_id, daycare_id, group_id, child_id, guardian_id)
        VALUES (:messageId, :unitId, :groupId, :childId, :guardianId)
    """.trimIndent()

    // todo use batch insert instead of for each
    recipients.forEach { recipient ->
        this.createUpdate(insertMessageInstance)
            .bind("messageId", messageId)
            .bind("unitId", recipient.unitId)
            .bind("groupId", recipient.groupId)
            .bind("childId", recipient.childId)
            .bind("guardianId", recipient.guardianId)
            .execute()
    }
}

fun Database.Read.getMessageRecipients(unitId: UUID, groupId: UUID): Set<MessageRecipient> {
    // language=sql
    val sql = """
        SELECT d.id AS unit_id, dg.id AS group_id, pl.child_id AS child_id, g.guardian_id AS guardian_id
        FROM daycare d
        JOIN daycare_group dg ON d.id = dg.daycare_id
        JOIN placement pl ON d.id = pl.unit_id AND daterange(pl.start_date, pl.end_date, '[]') @> :date
        JOIN guardian g ON pl.child_id = g.child_id
        WHERE d.id = :unitId AND dg.id = :groupId
    """.trimIndent()
    return this.createQuery(sql)
        .bind("unitId", unitId)
        .bind("groupId", groupId)
        .mapTo<MessageRecipient>()
        .toSet()
}

fun Database.Transaction.markMessageRead(
    user: AuthenticatedUser,
    messageId: UUID
) {
    // language=sql
    val sql = """
        INSERT INTO message_read (message_id, read_by, read_at)
        VALUES (:messageId, :userId, :readAt)
        ON CONFLICT DO NOTHING
    """.trimIndent()
    this.createUpdate(sql)
        .bind("messageId", messageId)
        .bind("userId", user.id)
        .bind("readAt", OffsetDateTime.now(zoneId))
        .execute()
}

fun Database.Read.getReceivedMessagesByGuardian(
    user: AuthenticatedUser
): List<ReceivedMessage> {
    data class ResultRow(
        val messageId: UUID,
        val type: MessageType,
        val title: String,
        val content: String,
        val isRead: Boolean,
        val instanceId: UUID,
        val unitId: UUID,
        val unitName: String,
        val groupId: UUID,
        val groupName: String,
        val childId: UUID,
        val childName: String
    )

    data class MessageGroup(
        val messageId: UUID,
        val type: MessageType,
        val title: String,
        val content: String,
        val isRead: Boolean
    )

    /* todo: In order to support pagination and improve performance, this may need to be changed so that we
       first fetch messages and then do a separate query for their instances (instead of fetching flat rows
       and grouping in kotlin) */

    // language=sql
    val sql = """
        SELECT 
            m.id AS message_id, m.type, m.title, m.content, (mr.read_at IS NOT NULL) AS is_read,
            mi.id AS instance_id, 
            mi.daycare_id AS unit_id, d.name as unit_name,
            mi.group_id, dg.name AS group_name,
            mi.child_id, c.first_name AS child_name
        FROM message m
        JOIN message_instance mi ON m.id = mi.message_id
        JOIN employee e ON e.id = m.created_by_employee
        JOIN daycare d ON mi.daycare_id = d.id
        JOIN daycare_group dg ON mi.group_id = dg.id
        JOIN person c ON mi.child_id = c.id
        LEFT JOIN message_read mr ON m.id = mr.message_id AND mr.read_by = :userId
        WHERE mi.guardian_id = :userId
        ORDER BY m.created DESC
    """.trimIndent()

    return this.createQuery(sql)
        .bind("userId", user.id)
        .mapTo<ResultRow>()
        .list()
        .groupBy(
            keySelector = { row ->
                MessageGroup(
                    messageId = row.messageId,
                    type = row.type,
                    title = row.title,
                    content = row.content,
                    isRead = row.isRead,
                )
            },
            valueTransform = { row ->
                ReceivedMessageInstance(
                    instanceId = row.instanceId,
                    unitId = row.unitId,
                    unitName = row.unitName,
                    groupId = row.groupId,
                    groupName = row.groupName,
                    childId = row.childId,
                    childName = row.childName
                )
            }
        )
        .entries.map { (msg, instances) ->
            ReceivedMessage(
                messageId = msg.messageId,
                type = msg.type,
                title = msg.title,
                content = msg.content,
                isRead = msg.isRead,
                instances = instances
            )
        }
}
