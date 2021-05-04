// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging.message

import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.mapToPaged
import fi.espoo.evaka.shared.withCountMapper
import org.jdbi.v3.core.kotlin.mapTo
import java.util.UUID

fun Database.Read.getMessagesByAccount(
    accountId: UUID,
    pageSize: Int,
    page: Int
): Paged<MessageThread> {
    val params = mapOf(
        "accountId" to accountId,
        "offset" to (page - 1) * pageSize,
        "pageSize" to pageSize
    )

    // language=SQL
    val sql = """
WITH thread_participation AS (
    SELECT mt.id AS thread_id,
           mp.message_id,
           mp.account_id,
           sender,
           read_at
    FROM message_participants mp
             JOIN message m ON mp.message_id = m.id
             JOIN message_thread_messages mtm ON mtm.message_id = m.id
             JOIN message_thread mt ON mtm.thread_id = mt.id
    WHERE EXISTS(SELECT 1
                 FROM message_participants participants
                 WHERE participants.message_id = m.id
                   AND participants.account_id = :accountId)
),
     threads(id, type, count) AS (
         SELECT thread.id,
                thread.message_type AS type,
                count(*) OVER ()    AS count
         FROM message_thread thread
         WHERE EXISTS(SELECT 1
                      FROM thread_participation
                      WHERE thread_participation.thread_id = thread.id)
         ORDER BY thread.created DESC
         LIMIT :pageSize OFFSET :offset
     ),
     messages AS (
         SELECT sender.thread_id,
                m.id,
                m.content,
                m.title,
                m.created,
                sender.account_id AS sender_id,
                name_view.account_name as sender_name
         FROM message m
         JOIN thread_participation sender ON sender.message_id = m.id AND sender.sender = TRUE
         JOIN message_account_name_view name_view ON sender.account_id = name_view.id
         ORDER BY m.created ASC
     ),
     message_threads AS (
         SELECT threads.count,
                threads.id,
                threads.type,
                (SELECT jsonb_agg(json_build_object('id', msg.id,
                                                    'sentAt', msg.created,
                                                    'title', msg.title,
                                                    'content', msg.content,
                                                    'senderName', msg.sender_name,
                                                    'senderId', msg.sender_id))) AS messages
         FROM threads
                  JOIN messages msg ON msg.thread_id = threads.id
         GROUP BY threads.count, threads.id, threads.type
     )

SELECT *
FROM message_threads
    """.trimIndent()

    return this.createQuery(sql)
        .bindMap(params)
        .map(withCountMapper<MessageThread>())
        .let(mapToPaged(pageSize))
}

fun Database.Read.getUnreadMessages(
    accountIds: List<UUID>
): Int {
    // language=SQL
    val sql = """
        SELECT COUNT(DISTINCT m.id) AS count
        FROM message_participants m
        WHERE m.account_id = ANY(:accountIds) AND m.read_at IS NULL
    """.trimIndent()

    return this.createQuery(sql)
        .bind("accountIds", accountIds)
        .mapTo<Int>()
        .first()
}

fun Database.Transaction.createMessageThread(
    title: String,
    content: String,
    type: MessageType,
    sender: MessageAccount,
    recipientAccountIds: List<UUID>
): UUID {
    // language=SQL
    val insertMessageSql = """
        INSERT INTO message (title, content, sender_name)
        VALUES (:title, :content, :senderName)        
        RETURNING id
    """.trimIndent()
    val messageId = this.createQuery(insertMessageSql)
        .bind("title", title)
        .bind("content", content)
        .bind("senderName", sender.name)
        .mapTo<UUID>()
        .first()

    // language=SQL
    val insertThreadSql = "INSERT INTO message_thread (message_type) VALUES (:messageType) RETURNING id"
    val threadId = this.createQuery(insertThreadSql)
        .bind("messageType", type)
        .mapTo<UUID>()
        .first()

    // language=SQL
    val insertMessageThreadMessagesSql = """
        INSERT INTO message_thread_messages (message_id, thread_id) VALUES (:messageId, :threadId)
    """.trimIndent()
    this.createUpdate(insertMessageThreadMessagesSql)
        .bind("messageId", messageId)
        .bind("threadId", threadId)
        .execute()

    // language=SQL
    val insertParticipantsSql = """
         INSERT INTO message_participants (message_id, sender, account_id)
         VALUES (:messageId, :sender, :accountId)
    """.trimIndent()

    val batch = this.prepareBatch(insertParticipantsSql)

    (listOf(Pair(sender.id, true)) + recipientAccountIds.map { Pair(it, false) }).forEach { (accountId, isSender) ->
        batch
            .bind("messageId", messageId)
            .bind("accountId", accountId)
            .bind("sender", isSender)
            .add()
    }

    batch.execute()

    return threadId
}
