// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.notes.createApplicationNote
import fi.espoo.evaka.children.getChildrenByParent
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.MessageContentId
import fi.espoo.evaka.shared.MessageId
import fi.espoo.evaka.shared.MessageThreadFolderId
import fi.espoo.evaka.shared.MessageThreadId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
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
    private val notificationEmailService: MessageNotificationEmailService,
    private val messagePushNotifications: MessagePushNotifications,
    private val featureConfig: FeatureConfig,
) {
    init {
        asyncJobRunner.registerHandler(::handleMarkMessageAsSent)
    }

    fun handleMarkMessageAsSent(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.MarkMessagesAsSent,
    ) {
        db.transaction { tx ->
            tx.lockMessageContentForUpdate(msg.messageContentId)
            tx.upsertReceiverThreadParticipants(msg.messageContentId, msg.sentAt)
            val messages = tx.markMessagesAsSent(msg.messageContentId, msg.sentAt)
            notificationEmailService.scheduleSendingMessageNotifications(tx, messages, clock.now())
            asyncJobRunner.plan(
                tx,
                messagePushNotifications.getAsyncJobs(tx, messages),
                runAt = clock.now(),
            )
        }
    }

    data class CitizenMessageSent(
        val messageId: MessageId,
        val threadId: MessageThreadId,
        val contentId: MessageContentId,
    )

    fun sendMessageAsCitizen(
        tx: Database.Transaction,
        now: HelsinkiDateTime,
        sender: MessageAccountId,
        msg: NewMessageStub,
        recipients: Set<MessageAccountId>,
        children: Set<ChildId>,
    ): CitizenMessageSent {
        val contentId = tx.insertMessageContent(msg.content, sender)
        val threadId =
            tx.insertThread(
                MessageType.MESSAGE,
                msg.title,
                msg.urgent,
                sensitive = false,
                isCopy = false,
            )
        tx.upsertSenderThreadParticipants(sender, listOf(threadId), now)
        val recipientNames =
            tx.getAccountNames(
                recipients,
                featureConfig.serviceWorkerMessageAccountName,
                featureConfig.financeMessageAccountName,
            )
        val messageId =
            tx.insertMessage(
                now = now,
                contentId = contentId,
                threadId = threadId,
                sender = sender,
                recipientNames = recipientNames,
                municipalAccountName = featureConfig.municipalMessageAccountName,
                serviceWorkerAccountName = featureConfig.serviceWorkerMessageAccountName,
                financeAccountName = featureConfig.financeMessageAccountName,
            )
        tx.insertMessageThreadChildren(listOf(children to threadId))
        tx.insertRecipients(listOf(messageId to recipients))
        asyncJobRunner.scheduleMarkMessagesAsSent(tx, contentId, now)
        return CitizenMessageSent(messageId, threadId, contentId)
    }

    fun sendMessageAsEmployee(
        tx: Database.Transaction,
        user: AuthenticatedUser,
        now: HelsinkiDateTime,
        sender: MessageAccountId,
        type: MessageType,
        msg: NewMessageStub,
        recipients: Set<MessageRecipient>,
        recipientNames: List<String>,
        attachments: Set<AttachmentId>,
        relatedApplication: ApplicationId?,
        filters: MessageController.PostMessageFilters?,
        initialFolder: MessageThreadFolderId? = null,
    ): MessageContentId? {
        if (initialFolder != null) {
            tx.getFolder(initialFolder)?.takeIf { it.ownerId == sender }
                ?: throw NotFound("Folder not found")
        }

        val messageRecipients =
            tx.getMessageAccountsForRecipients(sender, recipients, filters, now.toLocalDate())
        if (messageRecipients.isEmpty()) return null

        val staffCopyRecipients =
            if (type == MessageType.BULLETIN) {
                tx.getStaffCopyRecipients(sender, recipients, now.toLocalDate())
            } else emptySet()

        val recipientGroups: List<Pair<Set<MessageAccountId>, Set<ChildId?>>> =
            if (type == MessageType.BULLETIN) {
                // bulletins cannot be replied to so there is no need to group threads
                // for families
                messageRecipients
                    .groupBy { (accountId, _) -> accountId }
                    .map { (accountId, pairs) ->
                        setOf(accountId) to pairs.map { (_, childId) -> childId }.toSet()
                    }
            } else {
                // groupings where all the parents can read the messages of all the
                // children
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
        // for each recipient group, create a thread, message and message_recipients
        // while re-using
        // content
        val contentId = tx.insertMessageContent(content = msg.content, sender = sender)
        tx.reAssociateMessageAttachments(attachmentIds = attachments, messageContentId = contentId)
        val threadAndMessageIds =
            tx.insertThreadsWithMessages(
                recipientGroups.size,
                now,
                type = type,
                title = msg.title,
                urgent = msg.urgent,
                sensitive = msg.sensitive,
                isCopy = false,
                contentId = contentId,
                senderId = sender,
                recipientNames = recipientNames,
                applicationId = relatedApplication,
                municipalAccountName = featureConfig.municipalMessageAccountName,
                serviceWorkerAccountName = featureConfig.serviceWorkerMessageAccountName,
                financeAccountName = featureConfig.financeMessageAccountName,
            )
        val recipientGroupsWithMessageIds = threadAndMessageIds.zip(recipientGroups)
        tx.insertMessageThreadChildren(
            recipientGroupsWithMessageIds.map { (ids, recipients) ->
                recipients.second.filterNotNull().toSet() to ids.first
            }
        )
        tx.upsertSenderThreadParticipants(
            senderId = sender,
            threadIds = threadAndMessageIds.map { (threadId, _) -> threadId },
            now = now,
            initialFolder = initialFolder,
        )
        tx.insertRecipients(
            recipientGroupsWithMessageIds.map { (ids, recipients) ->
                ids.second to recipients.first
            }
        )
        if (staffCopyRecipients.isNotEmpty()) {
            // a separate copy for staff
            val staffThreadAndMessageIds =
                tx.insertThreadsWithMessages(
                    staffCopyRecipients.size,
                    now,
                    type = type,
                    title = msg.title,
                    urgent = msg.urgent,
                    sensitive = false,
                    isCopy = true,
                    contentId = contentId,
                    senderId = sender,
                    recipientNames = recipientNames,
                    applicationId = relatedApplication,
                    municipalAccountName = featureConfig.municipalMessageAccountName,
                    serviceWorkerAccountName = featureConfig.serviceWorkerMessageAccountName,
                    financeAccountName = featureConfig.financeMessageAccountName,
                )
            val staffRecipientsWithMessageIds =
                staffThreadAndMessageIds.zip(other = staffCopyRecipients)
            tx.upsertSenderThreadParticipants(
                senderId = sender,
                threadIds = staffThreadAndMessageIds.map { (threadId, _) -> threadId },
                now = now,
            )
            tx.insertRecipients(
                staffRecipientsWithMessageIds.map { (ids, recipient) ->
                    ids.second to setOf(recipient)
                }
            )
        }
        asyncJobRunner.scheduleMarkMessagesAsSent(tx, contentId, now)
        if (relatedApplication != null) {
            tx.createApplicationNote(
                applicationId = relatedApplication,
                content = msg.content,
                createdBy = user.evakaUserId,
                messageContentId = contentId,
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
        municipalAccountName: String,
        serviceWorkerAccountName: String,
        financeAccountName: String,
        user: AuthenticatedUser,
    ): ThreadReply {
        val today = now.toLocalDate()
        val (
            threadId,
            type,
            isCopy,
            previousSenders,
            previousRecipients,
            applicationId,
            applicationStatus,
            children) =
            db.read { it.getThreadByMessageId(replyToMessageId) }
                ?: throw NotFound("Message not found")

        if (isCopy) throw BadRequest("Message copies cannot be replied to")
        if (type == MessageType.BULLETIN && !previousSenders.contains(senderAccount))
            throw Forbidden("Only the author can reply to bulletin")

        val previousParticipants = previousRecipients + previousSenders
        if (!previousParticipants.contains(senderAccount))
            throw Forbidden("Not authorized to post to message")
        if (!previousParticipants.containsAll(recipientAccountIds))
            throw Forbidden("Not authorized to widen the audience")
        if (user is AuthenticatedUser.Citizen) {
            val isApplication = applicationId != null
            if (
                applicationStatus in
                    setOf(
                        ApplicationStatus.REJECTED,
                        ApplicationStatus.ACTIVE,
                        ApplicationStatus.CANCELLED,
                    )
            )
                throw Forbidden("Cannot reply to application message in status $applicationStatus")
            val financeAccountId = db.read { it.getFinanceAccountId() }
            val validRecipients =
                db.read { it.getCitizenReceivers(today, senderAccount) }
                    .mapValues { entry -> entry.value.reply.map { it.account.id }.toSet() }
            val allRecipientsValid =
                recipientAccountIds.all { recipient ->
                    children.any { child ->
                        validRecipients[child]?.contains(recipient) ?: false
                    } || recipient == financeAccountId
                }
            if (!isApplication && !allRecipientsValid)
                throw Forbidden("Not authorized to send to all recipients")
            val selectedChildren =
                db.read { it.getChildrenByParent(user.id, today) }
                    .filter { children.contains(it.id) }
            val selectedChildrenInSameUnit = selectedChildren.map { it.unit?.id }.toSet().size <= 1
            if (!isApplication && !selectedChildrenInSameUnit)
                throw Forbidden("Selected children not in same unit")
        }
        val message =
            db.transaction { tx ->
                tx.upsertSenderThreadParticipants(senderAccount, listOf(threadId), now)
                val recipientNames =
                    tx.getAccountNames(
                        recipientAccountIds,
                        serviceWorkerAccountName,
                        financeAccountName,
                    )
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
                        serviceWorkerAccountName = serviceWorkerAccountName,
                        financeAccountName = financeAccountName,
                    )
                tx.insertRecipients(listOf(messageId to recipientAccountIds))
                asyncJobRunner.scheduleMarkMessagesAsSent(tx, contentId, now)
                if (applicationId != null) {
                    tx.createApplicationNote(
                        applicationId = applicationId,
                        content = content,
                        createdBy = user.evakaUserId,
                        messageContentId = contentId,
                    )
                }
                tx.getSentMessage(
                    senderAccount,
                    messageId,
                    serviceWorkerAccountName,
                    financeAccountName,
                )
            }
        return ThreadReply(threadId, message)
    }
}

fun AsyncJobRunner<AsyncJob>.scheduleMarkMessagesAsSent(
    tx: Database.Transaction,
    messageContentId: MessageContentId,
    now: HelsinkiDateTime,
) = this.plan(tx, listOf(AsyncJob.MarkMessagesAsSent(messageContentId, now)), runAt = now)
