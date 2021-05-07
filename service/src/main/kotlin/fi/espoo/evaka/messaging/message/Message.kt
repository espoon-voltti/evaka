// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging.message

import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import org.jdbi.v3.json.Json
import java.util.UUID

data class Message(
    val id: UUID,
    val senderId: UUID,
    val senderName: String,
    val sentAt: HelsinkiDateTime,
    val content: String,
)

data class MessageThread(
    val id: UUID,
    val type: MessageType,
    val title: String,
    @Json
    val messages: List<Message>,
)

enum class MessageType {
    MESSAGE,
    BULLETIN
}

data class MessageAccount(
    val id: UUID,
    val name: String,
)

private fun insertMessage(
    tx: Database.Transaction,
    threadId: UUID,
    content: String,
    sender: MessageAccount,
    recipientAccountIds: Set<UUID>,
    repliesToMessageId: UUID? = null,
): UUID {
    val contentId = tx.insertMessageContent(content, sender)
    val messageId = tx.insertMessage(contentId, threadId, sender, repliesToMessageId)
    tx.insertRecipients(recipientAccountIds, messageId)
    return messageId
}

fun createMessageThread(
    tx: Database.Transaction,
    title: String,
    content: String,
    type: MessageType,
    sender: MessageAccount,
    recipientAccountIds: Set<UUID>,
): UUID {
    val threadId = tx.insertThread(type, title)
    insertMessage(tx, threadId, content, sender, recipientAccountIds)
    return threadId
}

fun replyToThread(
    db: Database.Connection,
    messageId: UUID,
    senderAccount: MessageAccount,
    recipientAccountIds: Set<UUID>,
    content: String,
): UUID {
    val (threadId, type, sender, recipients) = db.read { it.getThreadByMessageId(messageId) }
        ?: throw NotFound("Message not found")

    if (type == MessageType.BULLETIN && sender != senderAccount.id) throw Forbidden("Only the author can reply to bulletin")

    val previousParticipants = recipients + sender
    if (!previousParticipants.contains(senderAccount.id)) throw Forbidden("Not authorized to post to message")
    if (!previousParticipants.containsAll(recipientAccountIds)) throw Forbidden("Not authorized to widen the audience")

    return db.transaction {
        insertMessage(
            tx = it,
            threadId = threadId,
            repliesToMessageId = messageId,
            content = content,
            sender = senderAccount,
            recipientAccountIds = recipientAccountIds
        )
    }
}
