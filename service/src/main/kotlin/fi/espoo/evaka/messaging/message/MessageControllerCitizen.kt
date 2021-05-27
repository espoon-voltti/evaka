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
    messageNotificationEmailService: MessageNotificationEmailService
) {
    private val messageService = MessageService(messageNotificationEmailService)

    @GetMapping("/my-account")
    fun getMyAccount(
        db: Database.Connection,
        user: AuthenticatedUser
    ): MessageAccount {
        Audit.MessagingMyAccountsRead.log()
        return requireMessageAccountAccess(db, user)
    }
    @GetMapping("/unread-count")
    fun getUnreadMessages(
        db: Database.Connection,
        user: AuthenticatedUser
    ): Int {
        Audit.MessagingUnreadMessagesRead.log()
        val accountId = requireMessageAccountAccess(db, user)
        return db.read { it.getUnreadMessagesCount(setOf(accountId)) }
    }

    @PutMapping("/threads/{threadId}/read")
    fun markThreadRead(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable("threadId") threadId: UUID
    ) {
        Audit.MessagingMarkMessagesReadWrite.log(targetId = threadId)
        val accountId = requireMessageAccountAccess(db, user)
        return db.transaction { it.markThreadRead(accountId, threadId) }
    }

    @GetMapping("/received")
    fun getReceivedMessages(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestParam pageSize: Int,
        @RequestParam page: Int,
    ): Paged<MessageThread> {
        Audit.MessagingReceivedMessagesRead.log()
        val accountId = requireMessageAccountAccess(db, user)
        return db.read { it.getMessagesReceivedByAccount(accountId, pageSize, page) }
    }

    @GetMapping("/sent")
    fun getSentMessages(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestParam pageSize: Int,
        @RequestParam page: Int,
    ): Paged<SentMessage> {
        Audit.MessagingSentMessagesRead.log()
        val accountId = requireMessageAccountAccess(db, user)
        return db.read { it.getMessagesSentByAccount(accountId, pageSize, page) }
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
        val accountId = requireMessageAccountAccess(db, user)

        messageService.replyToThread(
            db = db,
            replyToMessageId = messageId,
            senderAccount = accountId,
            recipientAccountIds = body.recipientAccountIds,
            content = body.content
        )
    }

    private fun requireMessageAccountAccess(
        db: Database.Connection,
        user: AuthenticatedUser
    ): UUID {
        user.requireOneOfRoles(UserRole.END_USER)
        return db.read { it.getCitizenMessageAccount(user.id) }
    }
}
