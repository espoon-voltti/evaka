// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging.message

import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import java.util.UUID

class MessageService(
    private val notificationEmailService: MessageNotificationEmailService
) {
    fun createMessageThreadsForRecipientGroups(
        tx: Database.Transaction,
        title: String,
        content: String,
        type: MessageType,
        sender: UUID,
        recipientGroups: Set<Set<UUID>>,
        recipientNames: List<String>,
    ): List<UUID> {
        // for each recipient group, create a thread, message and message_recipients while re-using content
        val contentId = tx.insertMessageContent(content, sender)
        val sentAt = HelsinkiDateTime.now()
        return recipientGroups.map {
            val threadId = tx.insertThread(type, title)
            val messageId =
                tx.insertMessage(
                    contentId = contentId,
                    threadId = threadId,
                    sender = sender,
                    sentAt = sentAt,
                    recipientNames = recipientNames
                )
            tx.insertRecipients(it, messageId)
            notificationEmailService.scheduleSendingMessageNotifications(tx, messageId)
            threadId
        }
    }

    data class ThreadReply(val threadId: UUID, val message: Message)

    fun replyToThread(
        db: Database.Connection,
        replyToMessageId: UUID,
        senderAccount: UUID,
        recipientAccountIds: Set<UUID>,
        content: String,
    ): ThreadReply {
        val (threadId, type, senders, recipients) = db.read { it.getThreadByMessageId(replyToMessageId) }
            ?: throw NotFound("Message not found")

        if (type == MessageType.BULLETIN && !senders.contains(senderAccount)) throw Forbidden("Only the author can reply to bulletin")

        val previousParticipants = recipients + senders
        if (!previousParticipants.contains(senderAccount)) throw Forbidden("Not authorized to post to message")
        if (!previousParticipants.containsAll(recipientAccountIds)) throw Forbidden("Not authorized to widen the audience")

        val message = db.transaction { tx ->
            val recipientNames = tx.getAccountNames(recipientAccountIds)
            val contentId = tx.insertMessageContent(content, senderAccount)
            val messageId = tx.insertMessage(contentId, threadId, senderAccount, repliesToMessageId = replyToMessageId, recipientNames = recipientNames)
            tx.insertRecipients(recipientAccountIds, messageId)
            notificationEmailService.scheduleSendingMessageNotifications(tx, messageId)
            tx.getMessage(messageId)
        }
        return ThreadReply(threadId, message)
    }
}
