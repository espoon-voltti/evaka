// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging.message

import fi.espoo.evaka.shared.db.Database
import org.jdbi.v3.core.kotlin.mapTo
import java.util.UUID

fun Database.Read.getUnreadMessages(
    accountIds: List<UUID>
): Int {
    // language=SQL
    val sql = """
        SELECT COUNT(DISTINCT m.id) AS count
        FROM message_recipients m
        WHERE m.recipient_id = ANY(:accountIds) AND m.read_at IS NULL
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
    val insertThreadSql = "INSERT INTO message_thread (message_type, title) VALUES (:messageType, :title) RETURNING id"
    val threadId = this.createQuery(insertThreadSql)
        .bind("messageType", type)
        .bind("title", title)
        .mapTo<UUID>()
        .first()

    // language=SQL
    val messageContentSql = "INSERT INTO message_content (content, author_id) VALUES (:content, :authorId) RETURNING id"
    val contentId = this.createQuery(messageContentSql)
        .bind("content", content)
        .bind("authorId", sender.id)
        .mapTo<UUID>()
        .first()

    // language=SQL
    val insertMessageSql = """
        INSERT INTO message (content_id, thread_id, sender_id, sender_name)
        VALUES (:contentId, :threadId, :senderId, :senderName)        
        RETURNING id
    """.trimIndent()
    val messageId = this.createQuery(insertMessageSql)
        .bind("contentId", contentId)
        .bind("threadId", threadId)
        .bind("senderId", sender.id)
        .bind("senderName", sender.name)
        .mapTo<UUID>()
        .first()

    // language=SQL
    val insertRecipientsSql = "INSERT INTO message_recipients (message_id, recipient_id) VALUES (:messageId, :accountId)"

    val batch = this.prepareBatch(insertRecipientsSql)
    recipientAccountIds.forEach { batch.bind("messageId", messageId).bind("accountId", it).add() }
    batch.execute()

    return threadId
}
