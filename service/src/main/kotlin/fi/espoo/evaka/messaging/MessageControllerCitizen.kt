// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.MessageId
import fi.espoo.evaka.shared.MessageThreadId
import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
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
    val children: Set<ChildId>,
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
        user: AuthenticatedUser.Citizen
    ): MessageAccountId {
        return db.connect { dbc ->
            dbc.read { it.getCitizenMessageAccount(user.id) }
        }.also {
            Audit.MessagingMyAccountsRead.log(targetId = user.id, objectId = it)
        }
    }

    @GetMapping("/unread-count")
    fun getUnreadMessages(
        db: Database,
        user: AuthenticatedUser.Citizen
    ): Set<UnreadCountByAccount> {
        return db.connect { dbc ->
            val accountId = dbc.read { it.getCitizenMessageAccount(user.id) }
            dbc.read { it.getUnreadMessagesCounts(setOf(accountId)) }
        }.also {
            Audit.MessagingUnreadMessagesRead.log(targetId = user.id, args = mapOf("count" to it.size))
        }
    }

    @PutMapping("/threads/{threadId}/read")
    fun markThreadRead(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable("threadId") threadId: MessageThreadId
    ) {
        db.connect { dbc ->
            val accountId = dbc.read { it.getCitizenMessageAccount(user.id) }
            dbc.transaction { it.markThreadRead(clock, accountId, threadId) }
        }
        Audit.MessagingMarkMessagesReadWrite.log(targetId = threadId)
    }

    @GetMapping("/received")
    fun getReceivedMessages(
        db: Database,
        user: AuthenticatedUser.Citizen,
        @RequestParam pageSize: Int,
        @RequestParam page: Int
    ): Paged<MessageThread> {
        return db.connect { dbc ->
            val accountId = dbc.read { it.getCitizenMessageAccount(user.id) }
            dbc.read { it.getThreads(accountId, pageSize, page) }
        }.also {
            Audit.MessagingReceivedMessagesRead.log(args = mapOf("total" to it.total))
        }
    }

    data class GetReceiversResponse(
        val messageAccounts: Set<MessageAccount>,
        val messageAccountsToChildren: Map<MessageAccountId, List<ChildId>>
    )

    @GetMapping("/receivers")
    fun getReceivers(
        db: Database,
        user: AuthenticatedUser.Citizen,
        evakaClock: EvakaClock
    ): GetReceiversResponse {
        return db.connect { dbc ->
            val accountId = dbc.read { it.getCitizenMessageAccount(user.id) }
            val accountsToChildIds = (dbc.read { it.getCitizenReceivers(evakaClock.today(), accountId) })
            GetReceiversResponse(
                messageAccounts = accountsToChildIds.keys,
                messageAccountsToChildren = accountsToChildIds.mapKeys { it.key.id }
            )
        }.also {
            Audit.MessagingCitizenFetchReceiversForAccount.log(args = mapOf("count" to it.messageAccounts))
        }
    }

    @PostMapping("/{messageId}/reply")
    fun replyToThread(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable messageId: MessageId,
        @RequestBody body: ReplyToMessageBody
    ): MessageService.ThreadReply {
        return db.connect { dbc ->
            val accountId = dbc.read { it.getCitizenMessageAccount(user.id) }

            messageService.replyToThread(
                db = dbc,
                now = clock.now(),
                replyToMessageId = messageId,
                senderAccount = accountId,
                recipientAccountIds = body.recipientAccountIds,
                content = body.content
            )
        }.also {
            Audit.MessagingReplyToMessageWrite.log(targetId = messageId)
        }
    }

    @PostMapping
    fun newMessage(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @RequestBody body: CitizenMessageBody
    ): MessageThreadId {
        val now = clock.now()
        val today = now.toLocalDate()

        return db.connect { dbc ->
            val senderId = dbc.read { it.getCitizenMessageAccount(user.id) }
            val validReceivers = dbc.read { it.getCitizenReceivers(today, senderId).keys }
            val allReceiversValid =
                body.recipients.all { validReceivers.map { receiver -> receiver.id }.contains(it.id) }
            if (allReceiversValid) {
                val recipientIds = body.recipients.map { it.id }.toSet()
                dbc.transaction { tx ->
                    val contentId = tx.insertMessageContent(body.content, senderId)
                    val threadId = tx.insertThread(MessageType.MESSAGE, body.title, urgent = false, isCopy = false)
                    tx.upsertThreadParticipants(threadId, senderId, recipientIds, now)
                    val messageId =
                        tx.insertMessage(
                            now = clock.now(),
                            contentId = contentId,
                            threadId = threadId,
                            sender = senderId,
                            recipientNames = body.recipients.map { it.name }
                        )
                    tx.insertMessageThreadChildren(body.children, threadId)
                    tx.insertRecipients(recipientIds, messageId)
                    threadId
                }
            } else {
                throw Forbidden("Permission denied.")
            }
        }.also { messageThreadId ->
            Audit.MessagingCitizenSendMessage.log(targetId = messageThreadId)
        }
    }
}
