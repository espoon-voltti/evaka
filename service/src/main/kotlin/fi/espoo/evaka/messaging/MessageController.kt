// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.MessageDraftId
import fi.espoo.evaka.shared.MessageId
import fi.espoo.evaka.shared.MessageThreadId
import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

data class UnreadCountByAccount(val accountId: MessageAccountId, val unreadCopyCount: Int, val unreadCount: Int)
data class UnreadCountByAccountAndGroup(val accountId: MessageAccountId, val unreadCount: Int, val unreadCopyCount: Int, val groupId: GroupId)

data class ReplyToMessageBody(
    val content: String,
    val recipientAccountIds: Set<MessageAccountId>,
)

@RestController
@RequestMapping("/messages")
class MessageController(
    private val accessControl: AccessControl,
    messageNotificationEmailService: MessageNotificationEmailService
) {
    private val messageService = MessageService(messageNotificationEmailService)

    @GetMapping("/my-accounts")
    fun getAccountsByUser(db: Database, user: AuthenticatedUser.Employee, clock: EvakaClock): Set<AuthorizedMessageAccount> {
        Audit.MessagingMyAccountsRead.log()
        accessControl.requirePermissionFor(user, clock, Action.Global.READ_USER_MESSAGE_ACCOUNTS)
        return db.connect { dbc -> dbc.read { it.getAuthorizedMessageAccountsForEmployee(user.id) } }
    }

    @GetMapping("/mobile/my-accounts/{unitId}")
    fun getAccountsByDevice(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId
    ): Set<AuthorizedMessageAccount> {
        Audit.MessagingMyAccountsRead.log()
        accessControl.requirePermissionFor(user, clock, Action.Unit.READ_MESSAGING_ACCOUNTS, unitId)
        return if (user.employeeId != null) {
            db.connect { dbc -> dbc.read { it.getAuthorizedMessageAccountsForEmployee(user.employeeId) } }
        } else setOf()
    }

    @GetMapping("/{accountId}/received")
    fun getReceivedMessages(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @RequestParam pageSize: Int,
        @RequestParam page: Int,
    ): Paged<MessageThread> {
        Audit.MessagingReceivedMessagesRead.log(accountId)
        return db.connect { dbc ->
            requireMessageAccountAccess(dbc, user, clock, accountId)
            dbc.read { it.getMessagesReceivedByAccount(accountId, pageSize, page) }
        }
    }

    @GetMapping("/{accountId}/copies")
    fun getMessageCopies(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @RequestParam pageSize: Int,
        @RequestParam page: Int,
    ): Paged<MessageCopy> {
        Audit.MessagingReceivedMessagesRead.log(accountId)
        return db.connect { dbc ->
            requireMessageAccountAccess(dbc, user, clock, accountId)
            dbc.read { it.getMessageCopiesByAccount(accountId, pageSize, page) }
        }
    }

    @GetMapping("/{accountId}/sent")
    fun getSentMessages(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @RequestParam pageSize: Int,
        @RequestParam page: Int,
    ): Paged<SentMessage> {
        Audit.MessagingSentMessagesRead.log(accountId)
        return db.connect { dbc ->
            requireMessageAccountAccess(dbc, user, clock, accountId)
            dbc.read { it.getMessagesSentByAccount(accountId, pageSize, page) }
        }
    }

    @GetMapping("/unread")
    fun getUnreadMessages(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock
    ): Set<UnreadCountByAccount> {
        Audit.MessagingUnreadMessagesRead.log()
        accessControl.requirePermissionFor(user, clock, Action.Global.ACCESS_MESSAGING)
        return db.connect { dbc ->
            dbc.read { tx ->
                tx.getUnreadMessagesCounts(
                    tx.getEmployeeMessageAccountIds(
                        getEmployeeId(user)!!
                    )
                )
            }
        }
    }

    @GetMapping("/unread/{unitId}")
    fun getUnreadMessagesByUnit(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId
    ): Set<UnreadCountByAccountAndGroup> {
        Audit.MessagingUnreadMessagesRead.log()
        accessControl.requirePermissionFor(user, clock, Action.Unit.READ_UNREAD_MESSAGES, unitId)
        return db.connect { dbc -> dbc.read { tx -> tx.getUnreadMessagesCountsByDaycare(unitId) } }
    }

    data class PostMessageBody(
        val title: String,
        val content: String,
        val type: MessageType,
        val urgent: Boolean,
        val recipients: Set<MessageRecipient>,
        val recipientNames: List<String>,
        val attachmentIds: Set<AttachmentId> = setOf(),
        val draftId: MessageDraftId? = null
    )

    @PostMapping("/{accountId}")
    fun createMessage(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @RequestBody body: PostMessageBody,
    ) {
        Audit.MessagingNewMessageWrite.log(accountId)
        db.connect { dbc ->
            requireMessageAccountAccess(dbc, user, clock, accountId)
            dbc.transaction { tx ->
                val messageRecipients =
                    tx.getMessageAccountsForRecipients(accountId, body.recipients, clock.today()).toSet()

                if (messageRecipients.isEmpty()) return@transaction

                val staffCopyRecipients = if (body.recipients.none { it.type == MessageRecipientType.CHILD }) {
                    tx.getStaffCopyRecipients(
                        accountId,
                        body.recipients.mapNotNull {
                            if (it.type == MessageRecipientType.UNIT) DaycareId(it.id.raw) else null
                        },
                        body.recipients.mapNotNull {
                            if (it.type == MessageRecipientType.GROUP) GroupId(it.id.raw) else null
                        },
                        clock.today()
                    )
                } else setOf()

                val groupedRecipients = tx.groupRecipientAccountsByGuardianship(messageRecipients)
                messageService.createMessageThreadsForRecipientGroups(
                    tx,
                    clock,
                    title = body.title,
                    content = body.content,
                    sender = accountId,
                    type = body.type,
                    urgent = body.urgent,
                    recipientNames = body.recipientNames,
                    recipientGroups = groupedRecipients,
                    attachmentIds = body.attachmentIds,
                    staffCopyRecipients = staffCopyRecipients
                )
                if (body.draftId != null) tx.deleteDraft(accountId = accountId, draftId = body.draftId)
            }
        }
    }

    @GetMapping("/{accountId}/drafts")
    fun getDrafts(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
    ): List<DraftContent> {
        Audit.MessagingDraftsRead.log(accountId)
        return db.connect { dbc ->
            requireMessageAccountAccess(dbc, user, clock, accountId)
            dbc.transaction { it.getDrafts(accountId) }
        }
    }

    @PostMapping("/{accountId}/drafts")
    fun initDraft(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
    ): MessageDraftId {
        Audit.MessagingCreateDraft.log(accountId)
        return db.connect { dbc ->
            requireMessageAccountAccess(dbc, user, clock, accountId)
            dbc.transaction { it.initDraft(accountId) }
        }
    }

    @PutMapping("/{accountId}/drafts/{draftId}")
    fun upsertDraft(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @PathVariable draftId: MessageDraftId,
        @RequestBody content: UpdatableDraftContent,
    ) {
        Audit.MessagingUpdateDraft.log(accountId, draftId)
        return db.connect { dbc ->
            requireMessageAccountAccess(dbc, user, clock, accountId)
            dbc.transaction { it.updateDraft(accountId, draftId, content) }
        }
    }

    @DeleteMapping("/{accountId}/drafts/{draftId}")
    fun deleteDraft(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @PathVariable draftId: MessageDraftId,
    ) {
        Audit.MessagingDeleteDraft.log(accountId, draftId)
        return db.connect { dbc ->
            requireMessageAccountAccess(dbc, user, clock, accountId)
            dbc.transaction { tx -> tx.deleteDraft(accountId, draftId) }
        }
    }

    @PostMapping("{accountId}/{messageId}/reply")
    fun replyToThread(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @PathVariable messageId: MessageId,
        @RequestBody body: ReplyToMessageBody,
    ): MessageService.ThreadReply {
        Audit.MessagingReplyToMessageWrite.log(accountId, messageId)
        return db.connect { dbc ->
            requireMessageAccountAccess(dbc, user, clock, accountId)

            messageService.replyToThread(
                db = dbc,
                clock = clock,
                replyToMessageId = messageId,
                senderAccount = accountId,
                recipientAccountIds = body.recipientAccountIds,
                content = body.content
            )
        }
    }

    @PutMapping("/{accountId}/threads/{threadId}/read")
    fun markThreadRead(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @PathVariable threadId: MessageThreadId,
    ) {
        Audit.MessagingMarkMessagesReadWrite.log(accountId, threadId)
        return db.connect { dbc ->
            requireMessageAccountAccess(dbc, user, clock, accountId)
            dbc.transaction { it.markThreadRead(clock, accountId, threadId) }
        }
    }

    @GetMapping("/receivers")
    fun getReceiversForNewMessage(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestParam unitId: DaycareId
    ): List<MessageReceiversResponse> {
        Audit.MessagingMessageReceiversRead.log(unitId)
        accessControl.requirePermissionFor(user, clock, Action.Unit.READ_RECEIVERS_FOR_NEW_MESSAGE, unitId)
        return db.connect { dbc -> dbc.read { it.getReceiversForNewMessage(EmployeeId(user.rawId()), unitId) } }
    }

    private fun requireMessageAccountAccess(
        db: Database.Connection,
        user: AuthenticatedUser,
        clock: EvakaClock,
        accountId: MessageAccountId
    ) {
        accessControl.requirePermissionFor(user, clock, Action.Global.ACCESS_MESSAGING)
        db.read { it.getEmployeeMessageAccountIds(getEmployeeId(user)!!) }.find { it == accountId }
            ?: throw Forbidden("Message account not found for user")
    }

    fun getEmployeeId(user: AuthenticatedUser): EmployeeId? = when (user) {
        is AuthenticatedUser.MobileDevice -> user.employeeId
        is AuthenticatedUser.Employee -> user.id
        else -> null
    }
}
