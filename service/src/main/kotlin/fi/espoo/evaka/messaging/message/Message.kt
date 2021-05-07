// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging.message

import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
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
    tx: Database.Transaction,
    threadId: UUID,
    content: String,
    sender: MessageAccount,
    recipients: Set<UUID>,
    repliesToMessageId: UUID
): UUID {
    return insertMessage(tx, threadId, content, sender, recipients, repliesToMessageId)
}
