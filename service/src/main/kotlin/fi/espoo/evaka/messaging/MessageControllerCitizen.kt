// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.MessageId
import fi.espoo.evaka.shared.MessageThreadId
import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.Forbidden
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

data class CitizenMessageBody(
    val recipients: Set<MessageAccount>,
    val content: String,
    val title: String
)

@RestController
@RequestMapping("/citizen/messages")
class MessageControllerCitizen(
    messageNotificationEmailService: MessageNotificationEmailService
) {
    private val messageService = MessageService(messageNotificationEmailService)

    @GetMapping("/my-account")
    fun getMyAccount(
        db: Database,
        user: AuthenticatedUser
    ): MessageAccountId {
        Audit.MessagingMyAccountsRead.log()
        return db.connect { dbc -> requireMessageAccountAccess(dbc, user) }
    }

    @GetMapping("/unread-count")
    fun getUnreadMessages(
        db: Database,
        user: AuthenticatedUser
    ): Set<UnreadCountByAccount> {
        Audit.MessagingUnreadMessagesRead.log()
        return db.connect { dbc ->
            val accountId = requireMessageAccountAccess(dbc, user)
            dbc.read { it.getUnreadMessagesCounts(setOf(accountId)) }
        }
    }

    @PutMapping("/threads/{threadId}/read")
    fun markThreadRead(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable("threadId") threadId: MessageThreadId
    ) {
        Audit.MessagingMarkMessagesReadWrite.log(targetId = threadId)
        return db.connect { dbc ->
            val accountId = requireMessageAccountAccess(dbc, user)
            dbc.transaction { it.markThreadRead(accountId, threadId) }
        }
    }

    @GetMapping("/received")
    fun getReceivedMessages(
        db: Database,
        user: AuthenticatedUser,
        @RequestParam pageSize: Int,
        @RequestParam page: Int,
    ): Paged<MessageThread> {
        Audit.MessagingReceivedMessagesRead.log()
        return db.connect { dbc ->
            val accountId = requireMessageAccountAccess(dbc, user)
            dbc.read { it.getMessagesReceivedByAccount(accountId, true, pageSize, page) }
        }
    }

    @GetMapping("/sent")
    fun getSentMessages(
        db: Database,
        user: AuthenticatedUser,
        @RequestParam pageSize: Int,
        @RequestParam page: Int,
    ): Paged<SentMessage> {
        Audit.MessagingSentMessagesRead.log()
        return db.connect { dbc ->
            val accountId = requireMessageAccountAccess(dbc, user)
            dbc.read { it.getMessagesSentByAccount(accountId, pageSize, page) }
        }
    }

    @GetMapping("/receivers")
    fun getReceivers(
        db: Database,
        user: AuthenticatedUser,
    ): List<MessageAccount> {
        Audit.MessagingCitizenFetchReceiversForAccount.log()
        return db.connect { dbc ->
            val accountId = requireMessageAccountAccess(dbc, user)
            dbc.read { it.getCitizenReceivers(accountId) }
        }
    }

    @PostMapping("/{messageId}/reply")
    fun replyToThread(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable messageId: MessageId,
        @RequestBody body: ReplyToMessageBody,
    ): MessageService.ThreadReply {
        Audit.MessagingReplyToMessageWrite.log(targetId = messageId)
        return db.connect { dbc ->
            val accountId = requireMessageAccountAccess(dbc, user)

            messageService.replyToThread(
                db = dbc,
                replyToMessageId = messageId,
                senderAccount = accountId,
                recipientAccountIds = body.recipientAccountIds,
                content = body.content
            )
        }
    }

    @PostMapping
    fun newMessage(
        db: Database,
        user: AuthenticatedUser,
        @RequestBody body: CitizenMessageBody,
    ): List<MessageThreadId> {
        Audit.MessagingCitizenSendMessage.log()
        return db.connect { dbc ->
            val accountId = requireMessageAccountAccess(dbc, user)
            val validReceivers = dbc.read { it.getCitizenReceivers(accountId) }
            val allReceiversValid =
                body.recipients.all { validReceivers.map { receiver -> receiver.id }.contains(it.id) }
            if (allReceiversValid) {
                dbc.transaction { tx ->
                    val contentId = tx.insertMessageContent(body.content, accountId)
                    val threadId = tx.insertThread(MessageType.MESSAGE, body.title)
                    val messageId =
                        tx.insertMessage(
                            contentId = contentId,
                            threadId = threadId,
                            sender = accountId,
                            recipientNames = body.recipients.map { it.name }
                        )
                    body.recipients.map {
                        tx.insertRecipients(setOf(it.id), messageId)
                        threadId
                    }
                }
            } else {
                throw Forbidden("Permission denied.")
            }
        }
    }

    private fun requireMessageAccountAccess(
        db: Database.Connection,
        user: AuthenticatedUser
    ): MessageAccountId {
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.END_USER, UserRole.CITIZEN_WEAK)
        return db.read { it.getCitizenMessageAccount(PersonId(user.id)) }
    }
}
