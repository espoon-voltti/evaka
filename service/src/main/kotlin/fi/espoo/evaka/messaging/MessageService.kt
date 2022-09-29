// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.MessageId
import fi.espoo.evaka.shared.MessageThreadId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound

class MessageService(
    private val notificationEmailService: MessageNotificationEmailService
) {
    fun createMessageThreadsForRecipientGroups(
        tx: Database.Transaction,
        clock: EvakaClock,
        title: String,
        content: String,
        type: MessageType,
        urgent: Boolean,
        sender: MessageAccountId,
        recipientGroups: Set<Set<MessageAccountId>>,
        recipientNames: List<String>,
        attachmentIds: Set<AttachmentId> = setOf(),
        staffCopyRecipients: Set<MessageAccountId>,
        accountIdsToChildIds: Map<MessageAccountId, ChildId>
    ) =
        // for each recipient group, create a thread, message and message_recipients while re-using content
        tx.insertMessageContent(content, sender)
            .also { contentId -> tx.reAssociateMessageAttachments(attachmentIds, contentId) }
            .let { contentId ->
                val now = clock.now()
                recipientGroups.map { recipientIds ->
                    val threadId = tx.insertThread(type, title, urgent, isCopy = false)
                    val childIds = recipientIds.mapNotNull { accId -> accountIdsToChildIds[accId] }.toSet()
                    tx.insertMessageThreadChildren(childIds, threadId)
                    tx.upsertThreadParticipants(threadId, sender, recipientIds, now)
                    val messageId =
                        tx.insertMessage(
                            now = now,
                            contentId = contentId,
                            threadId = threadId,
                            sender = sender,
                            recipientNames = recipientNames
                        )
                    tx.insertRecipients(recipientIds, messageId)
                    notificationEmailService.scheduleSendingMessageNotifications(tx, messageId)
                    threadId
                }

                if (staffCopyRecipients.isNotEmpty()) {
                    // a separate copy for staff
                    val threadId = tx.insertThread(type, title, urgent, isCopy = true)
                    tx.upsertThreadParticipants(threadId, sender, staffCopyRecipients, now)
                    val messageId =
                        tx.insertMessage(
                            now = now,
                            contentId = contentId,
                            threadId = threadId,
                            sender = sender,
                            recipientNames = recipientNames
                        )
                    tx.insertRecipients(staffCopyRecipients, messageId)
                }
            }

    data class ThreadReply(val threadId: MessageThreadId, val message: Message)

    fun replyToThread(
        db: Database.Connection,
        now: HelsinkiDateTime,
        replyToMessageId: MessageId,
        senderAccount: MessageAccountId,
        recipientAccountIds: Set<MessageAccountId>,
        content: String,
    ): ThreadReply {
        val (threadId, type, isCopy, senders, recipients) = db.read { it.getThreadByMessageId(replyToMessageId) }
            ?: throw NotFound("Message not found")

        if (isCopy) throw BadRequest("Message copies cannot be replied to")
        if (type == MessageType.BULLETIN && !senders.contains(senderAccount)) throw Forbidden("Only the author can reply to bulletin")

        val previousParticipants = recipients + senders
        if (!previousParticipants.contains(senderAccount)) throw Forbidden("Not authorized to post to message")
        if (!previousParticipants.containsAll(recipientAccountIds)) throw Forbidden("Not authorized to widen the audience")

        val message = db.transaction { tx ->
            tx.upsertThreadParticipants(threadId, senderAccount, recipientAccountIds, now)
            val recipientNames = tx.getAccountNames(recipientAccountIds)
            val contentId = tx.insertMessageContent(content, senderAccount)
            val messageId = tx.insertMessage(now, contentId, threadId, senderAccount, repliesToMessageId = replyToMessageId, recipientNames = recipientNames)
            tx.insertRecipients(recipientAccountIds, messageId)
            notificationEmailService.scheduleSendingMessageNotifications(tx, messageId)
            tx.getMessage(messageId)
        }
        return ThreadReply(threadId, message)
    }
}
