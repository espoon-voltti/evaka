// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging.message

import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.core.mapper.PropagateNull
import org.jdbi.v3.json.Json
import java.time.LocalDate
import java.util.UUID

data class Message(
    val id: UUID,
    val senderId: UUID,
    val senderName: String,
    val sentAt: HelsinkiDateTime,
    val content: String,
    val readAt: HelsinkiDateTime? = null
)

data class MessageThread(
    val id: UUID,
    val type: MessageType,
    val title: String,
    @Json
    val messages: List<Message>,
)

data class SentMessage(
    val id: UUID,
    val content: String,
    val sentAt: HelsinkiDateTime,
    val threadId: UUID,
    val threadTitle: String,
    val type: MessageType,
    @Json
    val recipients: Set<MessageAccount>,
)

enum class MessageType {
    MESSAGE,
    BULLETIN
}

data class MessageAccount(
    val id: UUID,
    val name: String,
)

data class Group(
    @PropagateNull
    val id: UUID,
    val name: String,
    val unitId: UUID,
    val unitName: String,
)
data class AuthorizedMessageAccount(
    val id: UUID,
    val name: String,
    @Nested("group_")
    val daycareGroup: Group?,
    val personal: Boolean,
    val unreadCount: Int
)

data class MessageReceiverPerson(
    val accountId: UUID,
    val receiverFirstName: String,
    val receiverLastName: String
)
data class MessageReceiver(
    val childId: UUID,
    val childFirstName: String,
    val childLastName: String,
    val childDateOfBirth: LocalDate,
    val receiverPersons: List<MessageReceiverPerson>
)
data class MessageReceiversResponse(
    val groupId: UUID,
    val groupName: String,
    val receivers: List<MessageReceiver>
)

fun createMessageThreadsForRecipientGroups(
    tx: Database.Transaction,
    title: String,
    content: String,
    type: MessageType,
    sender: MessageAccount,
    recipientGroups: Set<Set<UUID>>,
): List<UUID> {
    // for each recipient group, create a thread, message and message_recipients while re-using content
    val contentId = tx.insertMessageContent(content, sender)
    val sentAt = HelsinkiDateTime.now()
    return recipientGroups.map {
        val threadId = tx.insertThread(type, title)
        val messageId = tx.insertMessage(contentId = contentId, threadId = threadId, sender = sender, sentAt = sentAt)
        tx.insertRecipients(it, messageId)
        threadId
    }
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

    return db.transaction { tx ->
        val contentId = tx.insertMessageContent(content, senderAccount)
        val msgId = tx.insertMessage(contentId, threadId, senderAccount, repliesToMessageId = messageId)
        tx.insertRecipients(recipientAccountIds, msgId)
        msgId
    }
}
