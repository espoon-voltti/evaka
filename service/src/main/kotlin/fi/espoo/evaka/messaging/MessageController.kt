// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.MessageDraftId
import fi.espoo.evaka.shared.MessageId
import fi.espoo.evaka.shared.MessageThreadId
import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.Forbidden
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class UnreadCountByAccount(val accountId: MessageAccountId, val unreadCount: Int)

data class ReplyToMessageBody(
    val content: String,
    val recipientAccountIds: Set<MessageAccountId>,
)

@RestController
@RequestMapping("/messages")
class MessageController(
    private val acl: AccessControlList,
    messageNotificationEmailService: MessageNotificationEmailService
) {
    private val messageService = MessageService(messageNotificationEmailService)

    fun Database.Read.getFakeUser(unitId: DaycareId) = this.createQuery(
        """
            SELECT employee_id FROM daycare_acl_view
            WHERE role = 'UNIT_SUPERVISOR'
            AND daycare_id = :unitId
        """.trimIndent()
    )
        .bind("unitId", unitId)
        .mapTo<UUID>()
        .first()

    @GetMapping("/my-accounts")
    fun getAccountsByUser(db: Database.Connection, user: AuthenticatedUser): Set<NestedMessageAccount> {
        Audit.MessagingMyAccountsRead.log()
        user.requireAnyEmployee()
        return db.read { it.getEmployeeNestedMessageAccounts(user.id) }
    }

    @GetMapping("/mobile/my-accounts/{unitId}")
    fun getAccountsByDevice(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable unitId: DaycareId
    ): Set<NestedMessageAccount> {
        val fakeUserId = db.read { it.getFakeUser(unitId) }
        return db.read { it.getEmployeeNestedMessageAccounts(fakeUserId) }
    }

    @GetMapping("/mobile/{accountId}/received")
    fun getReceivedMessagesByDevice(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable accountId: MessageAccountId,
        @RequestParam pageSize: Int,
        @RequestParam page: Int,
    ): Paged<MessageThread> {
        return db.read { it.getMessagesReceivedByAccount(accountId, pageSize, page) }
    }

    @GetMapping("/{accountId}/received")
    fun getReceivedMessages(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable accountId: MessageAccountId,
        @RequestParam pageSize: Int,
        @RequestParam page: Int,
    ): Paged<MessageThread> {
        Audit.MessagingReceivedMessagesRead.log(accountId)
        requireMessageAccountAccess(db, user, accountId)
        return db.read { it.getMessagesReceivedByAccount(accountId, pageSize, page) }
    }

    @GetMapping("/{accountId}/sent")
    fun getSentMessages(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable accountId: MessageAccountId,
        @RequestParam pageSize: Int,
        @RequestParam page: Int,
    ): Paged<SentMessage> {
        Audit.MessagingSentMessagesRead.log(accountId)
        requireMessageAccountAccess(db, user, accountId)
        return db.read { it.getMessagesSentByAccount(accountId, pageSize, page) }
    }

    @GetMapping("/mobile/unread/{unitId}")
    fun getUnreadMessagesMobile(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable unitId: DaycareId,
    ): Set<UnreadCountByAccount> {
        Audit.MessagingUnreadMessagesRead.log()
        return db.read { tx ->
            tx.getFakeUser(unitId).let { tx.getUnreadMessagesCounts(tx.getEmployeeMessageAccountIds(it)) }
        }
    }

    @GetMapping("/unread")
    fun getUnreadMessages(
        db: Database.Connection,
        user: AuthenticatedUser,
    ): Set<UnreadCountByAccount> {
        Audit.MessagingUnreadMessagesRead.log()
        requireAuthorizedMessagingRole(user)
        return db.read { tx -> tx.getUnreadMessagesCounts(tx.getEmployeeMessageAccountIds(user.id)) }
    }

    data class PostMessageBody(
        val title: String,
        val content: String,
        val type: MessageType,
        val recipientAccountIds: Set<MessageAccountId>,
        val recipientNames: List<String>,
        val attachmentIds: Set<AttachmentId> = setOf(),
        val draftId: MessageDraftId? = null
    )

    @PostMapping("/{accountId}")
    fun createMessage(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable accountId: MessageAccountId,
        @RequestBody body: PostMessageBody,
    ): List<MessageThreadId> {
        Audit.MessagingNewMessageWrite.log(accountId)
        requireMessageAccountAccess(db, user, accountId)

        checkAuthorizedRecipients(db, user, body.recipientAccountIds)
        val groupedRecipients = db.read { it.groupRecipientAccountsByGuardianship(body.recipientAccountIds) }

        return db.transaction { tx ->
            messageService.createMessageThreadsForRecipientGroups(
                tx,
                title = body.title,
                content = body.content,
                sender = accountId,
                type = body.type,
                recipientNames = body.recipientNames,
                recipientGroups = groupedRecipients,
                attachmentIds = body.attachmentIds,
            ).also {
                if (body.draftId != null) tx.deleteDraft(accountId = accountId, draftId = body.draftId)
            }
        }
    }

    @GetMapping("/{accountId}/drafts")
    fun getDrafts(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable accountId: MessageAccountId,
    ): List<DraftContent> {
        Audit.MessagingDraftsRead.log(accountId)
        requireMessageAccountAccess(db, user, accountId)
        return db.transaction { it.getDrafts(accountId) }
    }

    @PostMapping("/{accountId}/drafts")
    fun initDraft(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable accountId: MessageAccountId,
    ): MessageDraftId {
        Audit.MessagingCreateDraft.log(accountId)
        requireMessageAccountAccess(db, user, accountId)
        return db.transaction { it.initDraft(accountId) }
    }

    @PutMapping("/{accountId}/drafts/{draftId}")
    fun upsertDraft(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable accountId: MessageAccountId,
        @PathVariable draftId: MessageDraftId,
        @RequestBody content: UpsertableDraftContent,
    ) {
        Audit.MessagingUpdateDraft.log(accountId, draftId)
        requireMessageAccountAccess(db, user, accountId)
        return db.transaction { it.upsertDraft(accountId, draftId, content) }
    }

    @DeleteMapping("/{accountId}/drafts/{draftId}")
    fun deleteDraft(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable accountId: MessageAccountId,
        @PathVariable draftId: MessageDraftId,
    ) {
        Audit.MessagingDeleteDraft.log(accountId, draftId)
        requireMessageAccountAccess(db, user, accountId)
        return db.transaction { tx -> tx.deleteDraft(accountId, draftId) }
    }

    data class ReplyToMessageBody(
        val content: String,
        val recipientAccountIds: Set<MessageAccountId>,
    )

    @PostMapping("/mobile/{accountId}/{messageId}/reply")
    fun replyToThreadMobile(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable accountId: MessageAccountId,
        @PathVariable messageId: MessageId,
        @RequestBody body: ReplyToMessageBody,
    ): MessageService.ThreadReply {
        return messageService.replyToThread(
            db = db,
            replyToMessageId = messageId,
            senderAccount = accountId,
            recipientAccountIds = body.recipientAccountIds,
            content = body.content
        )
    }

    @PostMapping("{accountId}/{messageId}/reply")
    fun replyToThread(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable accountId: MessageAccountId,
        @PathVariable messageId: MessageId,
        @RequestBody body: ReplyToMessageBody,
    ): MessageService.ThreadReply {
        Audit.MessagingReplyToMessageWrite.log(accountId, messageId)
        requireMessageAccountAccess(db, user, accountId)

        return messageService.replyToThread(
            db = db,
            replyToMessageId = messageId,
            senderAccount = accountId,
            recipientAccountIds = body.recipientAccountIds,
            content = body.content
        )
    }

    @PutMapping("/mobile/{accountId}/threads/{threadId}/read")
    fun markThreadReadMobile(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable accountId: MessageAccountId,
        @PathVariable threadId: MessageThreadId,
    ) {
        return db.transaction { it.markThreadRead(accountId, threadId) }
    }

    @PutMapping("/{accountId}/threads/{threadId}/read")
    fun markThreadRead(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable accountId: MessageAccountId,
        @PathVariable threadId: MessageThreadId,
    ) {
        Audit.MessagingMarkMessagesReadWrite.log(accountId, threadId)
        requireMessageAccountAccess(db, user, accountId)
        return db.transaction { it.markThreadRead(accountId, threadId) }
    }

    @GetMapping("/receivers")
    fun getReceiversForNewMessage(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestParam unitId: DaycareId
    ): List<MessageReceiversResponse> {
        Audit.MessagingMessageReceiversRead.log(unitId)
        acl.getRolesForUnit(user, unitId).requireOneOfRoles(
            UserRole.ADMIN,
            UserRole.UNIT_SUPERVISOR,
            UserRole.STAFF,
            UserRole.SPECIAL_EDUCATION_TEACHER
        )

        return db.read { it.getReceiversForNewMessage(user.id, unitId) }
    }

    private fun requireAuthorizedMessagingRole(user: AuthenticatedUser) {
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.STAFF, UserRole.SPECIAL_EDUCATION_TEACHER)
    }

    private fun requireMessageAccountAccess(
        db: Database.Connection,
        user: AuthenticatedUser,
        accountId: MessageAccountId
    ) {
        requireAuthorizedMessagingRole(user)
        db.read { it.getEmployeeMessageAccountIds(user.id) }.find { it == accountId }
            ?: throw Forbidden("Message account not found for user")
    }

    private fun checkAuthorizedRecipients(
        db: Database.Connection,
        user: AuthenticatedUser,
        recipientAccountIds: Set<MessageAccountId>
    ) {
        db.read {
            it.isEmployeeAuthorizedToSendTo(
                user.id,
                recipientAccountIds
            )
        } || throw Forbidden("Not authorized to send to the given recipients")
    }
}
