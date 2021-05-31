// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging.message

import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.WithCount
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.bindNullable
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.mapToPaged
import fi.espoo.evaka.shared.withCountMapper
import org.jdbi.v3.core.kotlin.mapTo
import java.time.LocalDate
import java.util.UUID

fun Database.Read.getUnreadMessagesCount(accountIds: Set<UUID>): Int {
    // language=SQL
    val sql = """
        SELECT COUNT(DISTINCT m.id) AS count
        FROM message_recipients m
        WHERE m.recipient_id = ANY(:accountIds) AND m.read_at IS NULL
    """.trimIndent()

    return this.createQuery(sql)
        .bind("accountIds", accountIds.toTypedArray())
        .mapTo<Int>()
        .one()
}

fun Database.Read.accountHasMessages(accountId: UUID): Boolean {
    // language=SQL
    val sql = """
        SELECT (SELECT COUNT(*) FROM message WHERE sender_id = :accountId) > 0
            OR (SELECT COUNT(*) FROM message_content WHERE author_id = :accountId) > 0
            OR (SELECT COUNT(*) FROM message_recipients WHERE recipient_id = :accountId) > 0
    """.trimIndent()

    return this.createQuery(sql)
        .bind("accountId", accountId)
        .mapTo<Boolean>()
        .one()
}

fun Database.Transaction.markThreadRead(accountId: UUID, threadId: UUID): Int {
    // language=SQL
    val sql = """
UPDATE message_recipients rec
SET read_at = now()
FROM message msg
WHERE rec.message_id = msg.id
  AND msg.thread_id = :threadId
  AND rec.recipient_id = :accountId
  AND read_at IS NULL;
    """.trimIndent()

    return this.createUpdate(sql)
        .bind("accountId", accountId)
        .bind("threadId", threadId)
        .execute()
}

fun Database.Transaction.insertMessage(
    contentId: UUID,
    threadId: UUID,
    sender: UUID,
    recipientNames: List<String>,
    repliesToMessageId: UUID? = null,
    sentAt: HelsinkiDateTime? = HelsinkiDateTime.now()
): UUID {
    // language=SQL
    val insertMessageSql = """
        INSERT INTO message (content_id, thread_id, sender_id, sender_name, replies_to, sent_at, recipient_names)
        SELECT :contentId, :threadId, :senderId, name_view.account_name, :repliesToId, :sentAt, :recipientNames
        FROM message_account_name_view name_view
        WHERE name_view.id = :senderId
        RETURNING id
    """.trimIndent()
    return createQuery(insertMessageSql)
        .bind("contentId", contentId)
        .bind("threadId", threadId)
        .bindNullable("repliesToId", repliesToMessageId)
        .bind("senderId", sender)
        .bind("sentAt", sentAt)
        .bind("recipientNames", recipientNames.toTypedArray())
        .mapTo<UUID>()
        .one()
}

fun Database.Transaction.insertMessageContent(
    content: String,
    sender: UUID
): UUID {
    // language=SQL
    val messageContentSql = "INSERT INTO message_content (content, author_id) VALUES (:content, :authorId) RETURNING id"
    return createQuery(messageContentSql)
        .bind("content", content)
        .bind("authorId", sender)
        .mapTo<UUID>()
        .one()
}

fun Database.Transaction.insertRecipients(
    recipientAccountIds: Set<UUID>,
    messageId: UUID,
) {
    // language=SQL
    val insertRecipientsSql =
        "INSERT INTO message_recipients (message_id, recipient_id) VALUES (:messageId, :accountId)"

    val batch = this.prepareBatch(insertRecipientsSql)
    recipientAccountIds.forEach { batch.bind("messageId", messageId).bind("accountId", it).add() }
    batch.execute()
}

fun Database.Transaction.insertThread(
    type: MessageType,
    title: String,
): UUID {
    // language=SQL
    val insertThreadSql = "INSERT INTO message_thread (message_type, title) VALUES (:messageType, :title) RETURNING id"
    return createQuery(insertThreadSql)
        .bind("messageType", type)
        .bind("title", title)
        .mapTo<UUID>()
        .one()
}

data class ReceivedMessageResultItem(
    val count: Int,
    val id: UUID,
    val title: String,
    val type: MessageType,
    val messageId: UUID,
    val sentAt: HelsinkiDateTime,
    val content: String,
    val senderId: UUID,
    val senderName: String,
    val readAt: HelsinkiDateTime? = null,
    val recipientId: UUID,
    val recipientName: String,
)

fun Database.Read.getMessagesReceivedByAccount(accountId: UUID, pageSize: Int, page: Int): Paged<MessageThread> {
    val params = mapOf(
        "accountId" to accountId,
        "offset" to (page - 1) * pageSize,
        "pageSize" to pageSize
    )

    // language=SQL
    val sql = """
WITH
participated_messages AS (
    SELECT 
        rec.message_id,
        m.thread_id,
        m.sent_at, 
        m.sender_name,
        m.sender_id,
        c.content,
        rec.read_at,
        rec.recipient_id,
        acc.account_name recipient_name
    FROM message_recipients rec
    JOIN message m ON rec.message_id = m.id
    JOIN message_content c ON m.content_id = c.id
    JOIN message_account_name_view acc ON rec.recipient_id = acc.id
    WHERE m.sender_id = :accountId OR EXISTS (
        SELECT 1
            FROM message_recipients rec2
            WHERE rec2.message_id = m.id AND rec2.recipient_id = :accountId
    )
),
threads AS (
    SELECT id, message_type AS type, title, last_message, COUNT(*) OVER () AS count
    FROM message_thread t
    JOIN LATERAL (
        SELECT MAX(sent_at) last_message FROM message WHERE thread_id = t.id
    ) last_msg ON true
    WHERE EXISTS(
            SELECT 1
            FROM participated_messages rec
            WHERE rec.thread_id = t.id AND rec.recipient_id = :accountId)
    GROUP BY id, message_type, title, last_message
    ORDER BY last_message DESC
    LIMIT :pageSize OFFSET :offset
)

SELECT
    t.count,
    t.id,
    t.title,
    t.type,
    msg.message_id,
    msg.sent_at,
    msg.content,
    msg.sender_name,
    msg.sender_id,
    msg.read_at,
    msg.recipient_id,
    msg.recipient_name
    FROM threads t
          JOIN participated_messages msg ON msg.thread_id = t.id
    ORDER BY t.last_message DESC, msg.sent_at ASC
    """.trimIndent()

    return createQuery(sql)
        .bindMap(params)
        .mapTo<ReceivedMessageResultItem>()
        .groupBy { it.id }
        .map { (threadId, threads) ->
            WithCount(
                threads[0].count,
                MessageThread(
                    id = threadId,
                    type = threads[0].type,
                    title = threads[0].title,
                    messages = threads
                        .groupBy { it.messageId }
                        .map { (messageId, messages) ->
                            Message(
                                id = messageId,
                                content = messages[0].content,
                                senderId = messages[0].senderId,
                                senderName = messages[0].senderName,
                                sentAt = messages[0].sentAt,
                                readAt = messages.find { it.recipientId == accountId }?.readAt,
                                recipients = messages
                                    .groupBy { it.recipientId }
                                    .map { (recipientId, recipients) ->
                                        MessageAccount(recipientId, recipients[0].recipientName)
                                    }.toSet()
                            )
                        }
                )
            )
        }
        .let(mapToPaged(pageSize))
}

data class MessageResultItem(
    val id: UUID,
    val senderId: UUID,
    val senderName: String,
    val recipient_id: UUID,
    val recipient_name: String,
    val sentAt: HelsinkiDateTime,
    val content: String,
)

fun Database.Read.getMessage(id: UUID): Message {
    val sql = """
        SELECT 
            m.id,
            m.sender_id,
            m.sender_name,
            m.sent_at,
            c.content,
            rec.recipient_id,
            acc.account_name recipient_name
        FROM message m
        JOIN message_content c ON m.content_id = c.id
        JOIN message_recipients rec ON m.id = rec.message_id
        JOIN message_account_name_view acc ON rec.recipient_id = acc.id
        WHERE m.id = :id
    """.trimIndent()

    return this.createQuery(sql)
        .bind("id", id)
        .mapTo<MessageResultItem>()
        .groupBy { it.id }
        .map { (id, messages) ->
            Message(
                id = id,
                content = messages[0].content,
                sentAt = messages[0].sentAt,
                senderId = messages[0].senderId,
                senderName = messages[0].senderName,
                recipients = messages.map { MessageAccount(it.recipient_id, it.recipient_name) }.toSet()
            )
        }
        .single()
}

fun Database.Read.getMessagesSentByAccount(accountId: UUID, pageSize: Int, page: Int): Paged<SentMessage> {
    val params = mapOf(
        "accountId" to accountId,
        "offset" to (page - 1) * pageSize,
        "pageSize" to pageSize
    )

    // language=SQL
    val sql = """
WITH pageable_messages AS (
    SELECT id, content_id, thread_id, sent_at, recipient_names, COUNT(*) OVER () AS count
    FROM message m
    WHERE sender_id = :accountId
    ORDER BY sent_at DESC
    LIMIT :pageSize OFFSET :offset
),
recipients AS (
    SELECT message_id, recipient_id, name_view.account_name
    FROM message_recipients rec
    JOIN message_account_name_view name_view ON rec.recipient_id = name_view.id
)

SELECT
    msg.count,
    msg.id,
    msg.sent_at,
    msg.recipient_names,
    mc.content,
    t.id AS threadId,
    t.title AS threadTitle,
    t.message_type AS type,
    (SELECT jsonb_agg(json_build_object(
           'id', rec.recipient_id,
           'name', rec.account_name
       ))) AS recipients
FROM pageable_messages msg
JOIN recipients rec ON msg.id = rec.message_id
JOIN message_content mc ON msg.content_id = mc.id
JOIN message_thread t ON msg.thread_id = t.id
GROUP BY msg.count, msg.id, msg.sent_at, msg.recipient_names, mc.content, t.id, t.title, t.message_type
ORDER BY msg.sent_at DESC
    """.trimIndent()

    return this.createQuery(sql)
        .bindMap(params)
        .map(withCountMapper<SentMessage>())
        .let(mapToPaged(pageSize))
}

data class ThreadWithParticipants(
    val threadId: UUID,
    val type: MessageType,
    val sender: UUID,
    val recipients: Set<UUID>
)

fun Database.Read.getThreadByMessageId(messageId: UUID): ThreadWithParticipants? {
    val sql = """
        SELECT
            t.id AS threadId,
            t.message_type AS type,
            m.sender_id AS sender,
            (SELECT array_agg(recipient_id)) as recipients
            FROM message m
            JOIN message_thread t ON m.thread_id = t.id
            JOIN message_recipients rec ON rec.message_id = m.id
            WHERE m.id = :messageId
            GROUP BY t.id, t.message_type, m.sender_id
    """.trimIndent()
    return this.createQuery(sql)
        .bind("messageId", messageId)
        .mapTo<ThreadWithParticipants>()
        .firstOrNull()
}

data class MessageReceiversResult(
    val childId: UUID,
    val groupId: UUID,
    val groupName: String,
    val childFirstName: String,
    val childLastName: String,
    val childDateOfBirth: LocalDate,
    val accountId: UUID,
    val receiverFirstName: String,
    val receiverLastName: String
)

fun Database.Read.getReceiversForNewMessage(
    employeeId: UUID,
    unitId: UUID
): List<MessageReceiversResponse> {
    // language=sql
    val sql = """
        WITH children AS (
            SELECT pl.child_id, dg.id group_id, dg.name group_name
            FROM daycare_group dg
            JOIN daycare_group_placement gpl ON dg.id = gpl.daycare_group_id AND daterange(gpl.start_date, gpl.end_date, '[]') @> :date
            JOIN placement pl ON gpl.daycare_placement_id = pl.id
            WHERE pl.unit_id = :unitId AND EXISTS (
                SELECT 1 FROM child_acl_view a
                WHERE a.employee_id = :employeeId AND a.child_id = pl.child_id
            )
        ), receivers AS (
            SELECT c.child_id, c.group_id, c.group_name, g.guardian_id AS receiver_id
            FROM children c
            JOIN guardian g ON g.child_id = c.child_id
            WHERE NOT EXISTS (
                SELECT 1 FROM messaging_blocklist bl
                WHERE bl.child_id = c.child_id
                AND bl.blocked_recipient = g.guardian_id
            )

            UNION DISTINCT

            SELECT c.child_id, c.group_id, c.group_name, fc.head_of_child AS receiver_id
            FROM children c
            JOIN fridge_child fc ON fc.child_id = c.child_id AND daterange(fc.start_date, fc.end_date, '[]') @> :date
            WHERE NOT EXISTS (
                SELECT 1 FROM messaging_blocklist bl
                WHERE bl.child_id = c.child_id
                AND bl.blocked_recipient = fc.head_of_child
            )
        )
        SELECT
            acc.id account_id,
            r.group_id,
            r.group_name,
            p.first_name receiver_first_name,
            p.last_name receiver_last_name,
            r.child_id,
            c.first_name child_first_name,
            c.last_name child_last_name,
            c.date_of_birth child_date_of_birth
        FROM receivers r
        JOIN person p ON r.receiver_id = p.id
        JOIN person c ON r.child_id = c.id
        JOIN message_account acc ON p.id = acc.person_id
    """.trimIndent()

    return this.createQuery(sql)
        .bind("employeeId", employeeId)
        .bind("date", HelsinkiDateTime.now().toLocalDate())
        .bind("unitId", unitId)
        .mapTo<MessageReceiversResult>()
        .toList()
        .groupBy { it.groupId }
        .map { (groupId, receiverChildren) ->
            MessageReceiversResponse(
                groupId = groupId,
                groupName = receiverChildren.first().groupName,
                receivers = receiverChildren.groupBy { it.childId }
                    .map { (childId, receivers) ->
                        MessageReceiver(
                            childId = childId,
                            childFirstName = receivers.first().childFirstName,
                            childLastName = receivers.first().childLastName,
                            childDateOfBirth = receivers.first().childDateOfBirth,
                            receiverPersons = receivers.map { it ->
                                MessageReceiverPerson(
                                    accountId = it.accountId,
                                    receiverFirstName = it.receiverFirstName,
                                    receiverLastName = it.receiverLastName
                                )
                            }
                        )
                    }
            )
        }
}

fun Database.Read.isEmployeeAuthorizedToSendTo(employeeId: UUID, accountIds: Set<UUID>): Boolean {
    // language=SQL
    val sql = """
        WITH children AS (
            SELECT child_id
            FROM child_acl_view
            WHERE employee_id = :employeeId
        ), receivers AS (
            SELECT g.guardian_id AS receiver_id
            FROM children c
            JOIN guardian g ON g.child_id = c.child_id
            WHERE NOT EXISTS (
                SELECT 1 FROM messaging_blocklist bl
                WHERE bl.child_id = c.child_id
                AND bl.blocked_recipient = g.guardian_id
            )

            UNION DISTINCT

            SELECT fc.head_of_child AS receiver_id
            FROM children c
            JOIN fridge_child fc ON fc.child_id = c.child_id AND daterange(fc.start_date, fc.end_date, '[]') @> :date
            WHERE NOT EXISTS (
                SELECT 1 FROM messaging_blocklist bl
                WHERE bl.child_id = c.child_id
                AND bl.blocked_recipient = fc.head_of_child
            )
        )
        SELECT COUNT(*)
        FROM receivers r
        JOIN message_account acc ON acc.person_id = r.receiver_id AND acc.id = ANY(:accountIds)
    """.trimIndent()

    val numAccounts = createQuery(sql)
        .bind("employeeId", employeeId)
        .bind("date", LocalDate.now())
        .bind("accountIds", accountIds.toTypedArray())
        .mapTo<Int>()
        .one()

    return numAccounts == accountIds.size
}

fun Database.Transaction.markNotificationAsSent(messageRecipientId: UUID) {
    val sql = """
        UPDATE message_recipients
        SET notification_sent_at = now()
        WHERE id = :id
    """.trimIndent()
    this.createUpdate(sql)
        .bind("id", messageRecipientId)
        .execute()
}
