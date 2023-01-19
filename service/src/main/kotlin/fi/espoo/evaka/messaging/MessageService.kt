// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.MessageContentId
import fi.espoo.evaka.shared.MessageId
import fi.espoo.evaka.shared.MessageThreadId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import org.springframework.stereotype.Component

@Component
class MessageService(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val notificationEmailService: MessageNotificationEmailService
) {
    init {
        asyncJobRunner.registerHandler(::handleMarkMessageAsSent)
        asyncJobRunner.registerHandler(::handleUpdateMessageThreadRecipients)
    }

    companion object {
        val SPREAD_MESSAGE_NOTIFICATION_SECONDS: Long = 60 * 60 * 24
    }

    fun handleMarkMessageAsSent(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.MarkMessagesAsSent
    ) {
        db.transaction {
            it.upsertReceiverThreadParticipants(msg.messageContentId, msg.sentAt)
            it.markMessagesAsSent(msg.messageContentId, msg.sentAt)
        }
    }

    // TODO: Remove after the change has been deployed to all environments
    fun handleUpdateMessageThreadRecipients(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.UpdateMessageThreadRecipients
    ) {
        db.transaction { tx ->
            val contentId =
                tx.createQuery(
                        """
                    SELECT DISTINCT content_id 
                    FROM message m
                    WHERE m.thread_id = :threadId 
                    AND EXISTS(
                        SELECT 1 FROM message_recipients mr
                        WHERE mr.message_id = m.id 
                        AND mr.recipient_id = ANY(:recipientIds)
                    )
"""
                    )
                    .bind("threadId", msg.threadId)
                    .bind("recipientIds", msg.recipientIds)
                    .mapTo<MessageContentId>()
                    .one()
            tx.upsertReceiverThreadParticipants(contentId, msg.sentAt)
        }
    }

    fun createMessageThreadsForRecipientGroups(
        tx: Database.Transaction,
        clock: EvakaClock,
        title: String,
        content: String,
        type: MessageType,
        urgent: Boolean,
        sender: MessageAccountId,
        messageRecipients: List<Pair<MessageAccountId, ChildId?>>,
        recipientNames: List<String>,
        attachmentIds: Set<AttachmentId> = setOf(),
        staffCopyRecipients: Set<MessageAccountId>,
        municipalAccountName: String,
        serviceWorkerAccountName: String
    ): MessageContentId {
        val recipientGroups: List<Pair<Set<MessageAccountId>, Set<ChildId?>>> =
            if (type == MessageType.BULLETIN) {
                // bulletins cannot be replied to so there is no need to group threads for families
                messageRecipients
                    .groupBy { (accountId, _) -> accountId }
                    .map { (accountId, pairs) ->
                        setOf(accountId) to pairs.map { (_, childId) -> childId }.toSet()
                    }
            } else {
                // groupings where all the parents can read the messages of all the children
                messageRecipients
                    .groupBy { (_, childId) -> childId }
                    .mapValues { (_, accountChildPairs) ->
                        accountChildPairs.map { it.first }.toSet()
                    }
                    .toList()
                    .groupBy { (_, accounts) -> accounts }
                    .mapValues { (_, childAccountPairs) ->
                        childAccountPairs.map { it.first }.toSet()
                    }
                    .toList()
            }

        // for each recipient group, create a thread, message and message_recipients while re-using
        // content
        val contentId = tx.insertMessageContent(content, sender)
        tx.reAssociateMessageAttachments(attachmentIds, contentId)
        val now = clock.now()
        val threadAndMessageIds =
            tx.insertThreadsWithMessages(
                recipientGroups.size,
                now,
                type,
                title,
                urgent,
                false,
                contentId,
                sender,
                recipientNames,
                municipalAccountName,
                serviceWorkerAccountName
            )
        val recipientGroupsWithMessageIds = threadAndMessageIds.zip(recipientGroups)
        tx.insertMessageThreadChildren(
            recipientGroupsWithMessageIds.map { (ids, recipients) ->
                recipients.second.filterNotNull().toSet() to ids.first
            }
        )
        tx.upsertSenderThreadParticipants(
            sender,
            threadAndMessageIds.map { (threadId, _) -> threadId },
            now
        )
        tx.insertRecipients(
            recipientGroupsWithMessageIds.map { (ids, recipients) ->
                ids.second to recipients.first
            }
        )

        notificationEmailService.scheduleSendingMessageNotifications(
            tx,
            threadAndMessageIds.map { (_, messageId) -> messageId },
            now.plusSeconds(MESSAGE_UNDO_WINDOW_IN_SECONDS + 5),
            if (!urgent) SPREAD_MESSAGE_NOTIFICATION_SECONDS else 0
        )

        if (staffCopyRecipients.isNotEmpty()) {
            // a separate copy for staff
            val staffThreadAndMessageIds =
                tx.insertThreadsWithMessages(
                    staffCopyRecipients.size,
                    now,
                    type,
                    title,
                    urgent,
                    true,
                    contentId,
                    sender,
                    recipientNames,
                    municipalAccountName,
                    serviceWorkerAccountName
                )
            val staffRecipientsWithMessageIds = staffThreadAndMessageIds.zip(staffCopyRecipients)
            tx.upsertSenderThreadParticipants(
                sender,
                staffThreadAndMessageIds.map { (threadId, _) -> threadId },
                now
            )
            tx.insertRecipients(
                staffRecipientsWithMessageIds.map { (ids, recipient) ->
                    ids.second to setOf(recipient)
                }
            )
        }

        asyncJobRunner.scheduleMarkMessagesAsSent(tx, contentId, now)
        return contentId
    }

    data class ThreadReply(val threadId: MessageThreadId, val message: Message)

    fun replyToThread(
        db: Database.Connection,
        now: HelsinkiDateTime,
        replyToMessageId: MessageId,
        senderAccount: MessageAccountId,
        recipientAccountIds: Set<MessageAccountId>,
        content: String,
        municipalAccountName: String,
        serviceWorkerAccountName: String
    ): ThreadReply {
        val (threadId, type, isCopy, senders, recipients) =
            db.read { it.getThreadByMessageId(replyToMessageId) }
                ?: throw NotFound("Message not found")

        if (isCopy) throw BadRequest("Message copies cannot be replied to")
        if (type == MessageType.BULLETIN && !senders.contains(senderAccount))
            throw Forbidden("Only the author can reply to bulletin")

        val previousParticipants = recipients + senders
        if (!previousParticipants.contains(senderAccount))
            throw Forbidden("Not authorized to post to message")
        if (!previousParticipants.containsAll(recipientAccountIds))
            throw Forbidden("Not authorized to widen the audience")

        val message =
            db.transaction { tx ->
                tx.upsertSenderThreadParticipants(senderAccount, listOf(threadId), now)
                val recipientNames = tx.getAccountNames(recipientAccountIds)
                val contentId = tx.insertMessageContent(content, senderAccount)
                val messageId =
                    tx.insertMessage(
                        now = now,
                        contentId = contentId,
                        threadId = threadId,
                        sender = senderAccount,
                        repliesToMessageId = replyToMessageId,
                        recipientNames = recipientNames,
                        municipalAccountName = municipalAccountName,
                        serviceWorkerAccountName = serviceWorkerAccountName
                    )
                tx.insertRecipients(listOf(messageId to recipientAccountIds))
                asyncJobRunner.scheduleMarkMessagesAsSent(tx, contentId, now)
                notificationEmailService.scheduleSendingMessageNotifications(
                    tx,
                    listOf(messageId),
                    now,
                )
                tx.getSentMessage(senderAccount, messageId)
            }
        return ThreadReply(threadId, message)
    }
}

fun AsyncJobRunner<AsyncJob>.scheduleMarkMessagesAsSent(
    tx: Database.Transaction,
    messageContentId: MessageContentId,
    now: HelsinkiDateTime
) =
    this.plan(
        tx,
        listOf(AsyncJob.MarkMessagesAsSent(messageContentId, now)),
        runAt = now.plusSeconds(MESSAGE_UNDO_WINDOW_IN_SECONDS)
    )
