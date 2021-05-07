// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging.message

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.Forbidden
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/citizen/messages")
class MessageControllerCitizen {
    @GetMapping("/my-accounts")
    fun getAccountsByUser(db: Database.Connection, user: AuthenticatedUser): Set<MessageAccount> {
        Audit.MessagingMyAccountsRead.log()
        user.requireOneOfRoles(UserRole.END_USER)
        return db.read { it.getMessageAccountsForUser(user) }
    }

    @GetMapping("/unread")
    fun getUnreadMessages(
        db: Database.Connection,
        user: AuthenticatedUser
    ): UnreadMessagesResponse {
        Audit.MessagingUnreadMessagesRead.log()
        user.requireOneOfRoles(UserRole.END_USER)
        val accountIds = db.read { it.getMessageAccountsForUser(user) }.map { it.id }
        val count = if (accountIds.isEmpty()) 0 else db.read { it.getUnreadMessagesCount(accountIds.toSet()) }
        return UnreadMessagesResponse(count)
    }

    @GetMapping("/{accountId}/received")
    fun getReceivedMessages(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable accountId: UUID,
        @RequestParam pageSize: Int,
        @RequestParam page: Int,
    ): Paged<MessageThread> {
        Audit.MessagingReceivedMessagesRead.log()
        user.requireOneOfRoles(UserRole.END_USER)
        if (!db.read { it.getMessageAccountsForUser(user) }.map { it.id }.contains(accountId))
            throw Forbidden("User is not authorized to access the account")
        return db.read { it.getMessagesReceivedByAccount(accountId, pageSize, page) }
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
        val account = db.read { it.getMessageAccountsForUser(user) }.firstOrNull()
            ?: throw Forbidden("Message account not found for user")

        replyToThread(
            db = db,
            messageId = messageId,
            senderAccount = account,
            recipientAccountIds = body.recipientAccountIds,
            content = body.content
        )
    }
}
