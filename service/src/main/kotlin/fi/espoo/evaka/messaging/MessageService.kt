// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.BulletinId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.MessageContentId
import fi.espoo.evaka.shared.MessageId
import fi.espoo.evaka.shared.MessageThreadId
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.UrgentAsyncJob
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import org.springframework.stereotype.Component

@Component
class MessageService(
    private val asyncJobRunner: AsyncJobRunner<UrgentAsyncJob>,
    private val notificationEmailService: MessageNotificationEmailService
) {
    init {
        asyncJobRunner.registerHandler {
            db: Database.Connection,
            _: EvakaClock,
            msg: UrgentAsyncJob.UpdateMessageThreadRecipients ->
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
        urgent: Boolean,
        sender: MessageAccountId,
        messageRecipients: List<Pair<MessageAccountId, ChildId>>,
        recipientNames: List<String>,
        attachmentIds: Set<AttachmentId> = setOf()
    ): MessageContentId {
        val recipientGroups: List<Pair<Set<MessageAccountId>, Set<ChildId>>> =
        // groupings where all the parents can read the messages of all the children
        messageRecipients
                .groupBy { (_, childId) -> childId }
                .mapValues { (_, accountChildPairs) -> accountChildPairs.map { it.first }.toSet() }
                .toList()
                .groupBy { (_, accounts) -> accounts }
                .mapValues { (_, childAccountPairs) -> childAccountPairs.map { it.first }.toSet() }
                .toList()

        // for each recipient group, create a thread, message and message_recipients while re-using
        // content
        val contentId = tx.insertMessageContent(content, sender)
        tx.reAssociateMessageAttachments(attachmentIds, contentId)
        val now = clock.now()
        val threadAndMessageIds =
            tx.insertThreadsWithMessages(
                recipientGroups.size,
                now,
                title,
                urgent,
                false,
                contentId,
                sender,
                recipientNames
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
        asyncJobRunner.scheduleThreadRecipientsUpdate(
            tx,
            recipientGroupsWithMessageIds.map { (ids, recipients) ->
                ids.first to recipients.first
            },
            now
        )

        notificationEmailService.scheduleSendingMessageNotifications(
            tx,
            threadAndMessageIds.map { (_, messageId) -> messageId },
            now.plusSeconds(MESSAGE_UNDO_WINDOW_IN_SECONDS + 5)
        )

        return contentId
    }

    fun createBulletinsForRecipientGroups(
        tx: Database.Transaction,
        clock: EvakaClock,
        title: String,
        content: String,
        urgent: Boolean,
        sender: MessageAccountId,
        recipientNames: List<String>,
        messageRecipients: List<Pair<MessageAccountId, ChildId>>,
        attachmentIds: Set<AttachmentId> = setOf(),
        staffCopyRecipients: Set<MessageAccountId>,
        municipalAccountName: String
    ): BulletinId {
        val recipientGroups: List<Pair<Set<MessageAccountId>, Set<ChildId>>> =
            messageRecipients
                .groupBy { (accountId, _) -> accountId }
                .map { (accountId, pairs) ->
                    setOf(accountId) to pairs.map { (_, childId) -> childId }.toSet()
                }

        val now = clock.now()
        val bulletinId =
            tx.insertBulletin(
                now,
                title,
                content,
                urgent,
                sender,
                recipientNames,
                municipalAccountName
            )
        val recipientIds =
            tx.insertBulletinRecipients(
                bulletinId,
                recipientGroups.flatMap { (recipients, _) -> recipients } + staffCopyRecipients
            )
        tx.insertBulletinChildren(
            recipientGroups.flatMap { (recipients, children) ->
                recipients.mapNotNull { recipient ->
                    recipientIds[recipient]?.let { it to children }
                }
            }
        )
        tx.reAssociateBulletinAttachments(attachmentIds, bulletinId)
        notificationEmailService.scheduleSendingBulletinNotifications(
            tx,
            bulletinId,
            recipientGroups.flatMap { (recipients, _) -> recipients },
            now.plusSeconds(MESSAGE_UNDO_WINDOW_IN_SECONDS + 5),
            if (urgent) 0 else SPREAD_MESSAGE_NOTIFICATION_SECONDS
        )

        return bulletinId
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
                asyncJobRunner.scheduleThreadRecipientsUpdate(
                    tx,
                    listOf(threadId to recipientAccountIds),
                    now
                )
                val recipientNames = tx.getAccountNames(recipientAccountIds)
                val contentId = tx.insertMessageContent(content, senderAccount)
                val messageId =
                    tx.insertMessage(
                        now,
                        contentId,
                        threadId,
                        senderAccount,
                        repliesToMessageId = replyToMessageId,
                        recipientNames = recipientNames,
                        municipalAccountName = municipalAccountName
                    )
                tx.insertRecipients(listOf(messageId to recipientAccountIds))
                notificationEmailService.scheduleSendingMessageNotifications(
                    tx,
                    listOf(messageId),
                    now.plusSeconds(MESSAGE_UNDO_WINDOW_IN_SECONDS + 5)
                )
                tx.getSentMessage(senderAccount, messageId)
            }
        return ThreadReply(threadId, message)
    }
}

fun AsyncJobRunner<UrgentAsyncJob>.scheduleThreadRecipientsUpdate(
    tx: Database.Transaction,
    threadRecipientsPairs: List<Pair<MessageThreadId, Set<MessageAccountId>>>,
    sentAt: HelsinkiDateTime
) =
    this.plan(
        tx,
        threadRecipientsPairs.map { (threadId, recipients) ->
            UrgentAsyncJob.UpdateMessageThreadRecipients(threadId, recipients, sentAt)
        },
        runAt = sentAt.plusSeconds(MESSAGE_UNDO_WINDOW_IN_SECONDS)
    )
