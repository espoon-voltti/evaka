// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging.message

import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.bindNullable
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.mapToPaged
import fi.espoo.evaka.shared.withCountMapper
import org.jdbi.v3.core.kotlin.mapTo
import java.util.UUID

fun Database.Read.getUnreadMessagesCount(
    accountIds: Set<UUID>
): Int {
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

fun Database.Transaction.markThreadRead(accountId: UUID, threadId: UUID): Int {
    // language=SQL
    val sql = """
UPDATE message_recipients rec
SET read_at = :readAt
FROM message msg
WHERE rec.message_id = msg.id
  AND msg.thread_id = :threadId
  AND rec.recipient_id = :accountId
  AND read_at IS NULL;
    """.trimIndent()

    return this.createUpdate(sql)
        .bind("accountId", accountId)
        .bind("threadId", threadId)
        .bind("readAt", HelsinkiDateTime.now())
        .execute()
}

fun Database.Transaction.insertMessage(
    contentId: UUID,
    threadId: UUID,
    sender: MessageAccount,
    repliesToMessageId: UUID? = null,
    sentAt: HelsinkiDateTime? = HelsinkiDateTime.now()
): UUID {
    // language=SQL
    val insertMessageSql = """
        INSERT INTO message (content_id, thread_id, sender_id, sender_name, replies_to, sent_at)
        VALUES (:contentId, :threadId, :senderId, :senderName, :repliesToId, :sentAt)        
        RETURNING id
    """.trimIndent()
    return createQuery(insertMessageSql)
        .bind("contentId", contentId)
        .bind("threadId", threadId)
        .bindNullable("repliesToId", repliesToMessageId)
        .bind("senderId", sender.id)
        .bind("senderName", sender.name)
        .bind("sentAt", sentAt)
        .mapTo<UUID>()
        .one()
}

fun Database.Transaction.insertMessageContent(
    content: String,
    sender: MessageAccount,
): UUID {
    // language=SQL
    val messageContentSql = "INSERT INTO message_content (content, author_id) VALUES (:content, :authorId) RETURNING id"
    return createQuery(messageContentSql)
        .bind("content", content)
        .bind("authorId", sender.id)
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

fun Database.Read.getMessagesReceivedByAccount(accountId: UUID, pageSize: Int, page: Int): Paged<MessageThread> {
    val params = mapOf(
        "accountId" to accountId,
        "offset" to (page - 1) * pageSize,
        "pageSize" to pageSize
    )

    // language=SQL
    val sql = """
WITH
threads AS (
    SELECT id, message_type AS type, title, last_message, COUNT(*) OVER () AS count
    FROM message_thread t
    JOIN LATERAL (
        SELECT MAX(sent_at) last_message FROM message WHERE thread_id = t.id
    ) last_msg ON true
    WHERE EXISTS(
            SELECT 1
            FROM message_recipients rec
            JOIN message m ON rec.message_id = m.id
            WHERE rec.recipient_id = :accountId AND m.thread_id = t.id)
    GROUP BY id, message_type, title, last_message
    ORDER BY last_message DESC
    LIMIT :pageSize OFFSET :offset
),
messages AS (
    SELECT
        m.thread_id,
        m.id,
        c.content,
        rec.read_at,
        m.sent_at,
        m.sender_id,
        m.sender_name
    FROM threads t
    JOIN message m ON m.thread_id = t.id
    JOIN message_content c ON m.content_id = c.id
    LEFT JOIN message_recipients rec ON m.id = rec.message_id AND rec.recipient_id = :accountId
    WHERE
        m.sender_id = :accountId OR
        EXISTS(SELECT 1
            FROM message_recipients rec
            WHERE rec.message_id = m.id AND rec.recipient_id = :accountId)
    ORDER BY m.sent_at
)

SELECT
    t.count,
    t.id,
    t.title,
    t.type,
    (SELECT jsonb_agg(json_build_object(
        'id', msg.id,
        'sentAt', msg.sent_at,
        'content', msg.content,
        'senderName', msg.sender_name,
        'senderId', msg.sender_id,
        'readAt', msg.read_at
    ))) AS messages
    FROM threads t
          JOIN messages msg ON msg.thread_id = t.id
    GROUP BY t.count, t.id, t.type, t.title, t.last_message
    ORDER BY t.last_message DESC
    """.trimIndent()

    return this.createQuery(sql)
        .bindMap(params)
        .map(withCountMapper<MessageThread>())
        .let(mapToPaged(pageSize))
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
    SELECT id, content_id, thread_id, sent_at, COUNT(*) OVER () AS count
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
GROUP BY msg.count, msg.id, msg.sent_at, mc.content, t.id, t.title, t.message_type
ORDER BY msg.sent_at DESC
    """.trimIndent()

    return this.createQuery(sql)
        .bindMap(params)
        .map(withCountMapper<SentMessage>())
        .let(mapToPaged(pageSize))
}

data class ThreadWithParticipants(val threadId: UUID, val type: MessageType, val sender: UUID, val recipients: Set<UUID>)

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
