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
        asyncJobRunner.registerHandler {
            db: Database.Connection,
            _: EvakaClock,
            msg: AsyncJob.MarkMessageAsSent ->
            db.transaction {
                it.upsertReceiverThreadParticipants(msg.threadId, msg.recipientIds, msg.sentAt)
                it.markMessagesAsSent(msg.messageIds, msg.sentAt)
            }
        }

        // TODO: Not used anymore, remove after the above job has been deployed
        asyncJobRunner.registerHandler {
            db: Database.Connection,
            _: EvakaClock,
            msg: AsyncJob.UpdateMessageThreadRecipients ->
            db.transaction {
                it.upsertReceiverThreadParticipants(msg.threadId, msg.recipientIds, msg.sentAt)
            }
        }
    }

    companion object {
        val SPREAD_MESSAGE_NOTIFICATION_SECONDS: Long = 60 * 60 * 24
    }

    fun createMessageThreadsForRecipientGroups(
        tx: Database.Transaction,
        clock: EvakaClock,
        title: String,
        content: String,
        type: MessageType,
        urgent: Boolean,
        sender: MessageAccountId,
        messageRecipients: List<Pair<MessageAccountId, ChildId>>,
        recipientNames: List<String>,
        attachmentIds: Set<AttachmentId> = setOf(),
        staffCopyRecipients: Set<MessageAccountId>,
        municipalAccountName: String
    ): MessageContentId {
        val recipientGroups: List<Pair<Set<MessageAccountId>, Set<ChildId>>> =
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
                municipalAccountName
            )
        val recipientGroupsWithMessageIds = threadAndMessageIds.zip(recipientGroups)
        tx.insertMessageThreadChildren(
            recipientGroupsWithMessageIds.map { (ids, recipients) ->
                recipients.second to ids.first
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
        asyncJobRunner.scheduleMarkMessagesAsSent(
            tx,
            recipientGroupsWithMessageIds.map { (ids, recipients) ->
                ids.first to recipients.first
            },
            threadAndMessageIds.map { it.second }.toSet(),
            now
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
                    municipalAccountName
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
            asyncJobRunner.scheduleMarkMessagesAsSent(
                tx,
                staffRecipientsWithMessageIds.map { (ids, recipient) ->
                    ids.first to setOf(recipient)
                },
                staffThreadAndMessageIds.map { it.second }.toSet(),
                now
            )
        }

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
        municipalAccountName: String
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
                        municipalAccountName = municipalAccountName
                    )
                tx.insertRecipients(listOf(messageId to recipientAccountIds))
                asyncJobRunner.scheduleMarkMessagesAsSent(
                    tx,
                    listOf(threadId to recipientAccountIds),
                    setOf(messageId),
                    now
                )
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
    threadRecipientsPairs: List<Pair<MessageThreadId, Set<MessageAccountId>>>,
    messageIds: Set<MessageId>,
    sentAt: HelsinkiDateTime
) =
    this.plan(
        tx,
        threadRecipientsPairs.map { (threadId, recipients) ->
            AsyncJob.MarkMessageAsSent(threadId, recipients, messageIds, sentAt)
        },
        runAt = sentAt.plusSeconds(MESSAGE_UNDO_WINDOW_IN_SECONDS)
    )
