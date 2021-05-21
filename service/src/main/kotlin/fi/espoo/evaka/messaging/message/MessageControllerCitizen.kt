// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging.message

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/citizen/messages")
class MessageControllerCitizen(
    private val messageNotificationEmailService: MessageNotificationEmailService
) {
    private val messageService = MessageService(messageNotificationEmailService)

    @GetMapping("/unread-count")
    fun getUnreadMessages(
        db: Database.Connection,
        user: AuthenticatedUser
    ): Int {
        Audit.MessagingUnreadMessagesRead.log()
        user.requireOneOfRoles(UserRole.END_USER)
        return db.read {
            val account = it.getMessageAccountForEndUser(user)
            it.getUnreadMessagesCount(setOf(account.id))
        }
    }

    @PutMapping("/threads/{threadId}/read")
    fun markThreadRead(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable("threadId") threadId: UUID
    ) {
        Audit.MessagingMarkMessagesReadWrite.log(targetId = threadId)
        user.requireOneOfRoles(UserRole.END_USER)
        return db.transaction {
            val account = it.getMessageAccountForEndUser(user)
            it.markThreadRead(account.id, threadId)
        }
    }

    @GetMapping("/received")
    fun getReceivedMessages(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestParam pageSize: Int,
        @RequestParam page: Int,
    ): Paged<MessageThread> {
        Audit.MessagingReceivedMessagesRead.log()
        user.requireOneOfRoles(UserRole.END_USER)
        return db.read {
            val account = it.getMessageAccountForEndUser(user)
            it.getMessagesReceivedByAccount(account.id, pageSize, page)
        }
    }

    @GetMapping("/sent")
    fun getSentMessages(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestParam pageSize: Int,
        @RequestParam page: Int,
    ): Paged<SentMessage> {
        Audit.MessagingSentMessagesRead.log()
        user.requireOneOfRoles(UserRole.END_USER)
        return db.read {
            val account = it.getMessageAccountForEndUser(user)
            it.getMessagesSentByAccount(account.id, pageSize, page)
        }
    }

    data class ReplyToMessageBody(val content: String, val recipientAccountIds: Set<UUID>)

    @PostMapping("/{messageId}/reply")
    fun replyToThread(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable messageId: UUID,
        @RequestBody body: ReplyToMessageBody,
    ) {
        Audit.MessagingReplyToMessageWrite.log(targetId = messageId)
        user.requireOneOfRoles(UserRole.END_USER)
        val account = db.read { it.getMessageAccountForEndUser(user) }

        messageService.replyToThread(
            db = db,
            replyToMessageId = messageId,
            senderAccount = account,
            recipientAccountIds = body.recipientAccountIds,
            content = body.content
        )
    }

    @GetMapping
    fun getThreadsMock(): Paged<MessageThread> {
        return mockThreadData()
    }
}
