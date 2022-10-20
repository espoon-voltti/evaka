// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.MessageDraftId
import fi.espoo.evaka.shared.MessageId
import fi.espoo.evaka.shared.MessageThreadId
import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
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

data class UnreadCountByAccount(
    val accountId: MessageAccountId,
    val unreadCopyCount: Int,
    val unreadCount: Int
)

data class UnreadCountByAccountAndGroup(
    val accountId: MessageAccountId,
    val unreadCount: Int,
    val unreadCopyCount: Int,
    val groupId: GroupId
)

data class ReplyToMessageBody(val content: String, val recipientAccountIds: Set<MessageAccountId>)

@RestController
@RequestMapping("/messages")
class MessageController(
    private val accessControl: AccessControl,
    private val featureConfig: FeatureConfig,
    messageNotificationEmailService: MessageNotificationEmailService
) {
    private val messageService = MessageService(messageNotificationEmailService)

    @GetMapping("/my-accounts")
    fun getAccountsByUser(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock
    ): Set<AuthorizedMessageAccount> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.READ_USER_MESSAGE_ACCOUNTS
                    )
                    it.getAuthorizedMessageAccountsForEmployee(
                        user.id,
                        featureConfig.municipalMessageAccountName
                    )
                }
            }
            .also { Audit.MessagingMyAccountsRead.log(args = mapOf("count" to it.size)) }
    }

    @GetMapping("/mobile/my-accounts/{unitId}")
    fun getAccountsByDevice(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId
    ): Set<AuthorizedMessageAccount> {
        val result =
            db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Unit.READ_MESSAGING_ACCOUNTS,
                        unitId
                    )
                    if (user.employeeId != null) {
                        it.getAuthorizedMessageAccountsForEmployee(
                            user.employeeId,
                            featureConfig.municipalMessageAccountName
                        )
                    } else setOf()
                }
            }
        Audit.MessagingMyAccountsRead.log(targetId = unitId, args = mapOf("count" to result.size))
        return result
    }

    @GetMapping("/{accountId}/received")
    fun getReceivedMessages(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @RequestParam pageSize: Int,
        @RequestParam page: Int
    ): Paged<MessageThread> {
        return db.connect { dbc ->
                requireMessageAccountAccess(dbc, user, clock, accountId)
                dbc.read {
                    it.getReceivedThreads(
                        accountId,
                        pageSize,
                        page,
                        featureConfig.municipalMessageAccountName
                    )
                }
            }
            .also {
                Audit.MessagingReceivedMessagesRead.log(
                    targetId = accountId,
                    args = mapOf("total" to it.total)
                )
            }
    }

    @GetMapping("/{accountId}/archived")
    fun getArchivedMessages(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @RequestParam pageSize: Int,
        @RequestParam page: Int,
    ): Paged<MessageThread> {
        return db.connect { dbc ->
                requireMessageAccountAccess(dbc, user, clock, accountId)
                dbc.read {
                    val archiveFolderId = it.getArchiveFolderId(accountId)
                    if (archiveFolderId == null) {
                        Paged(emptyList(), 0, 0)
                    } else {
                        it.getReceivedThreads(
                            accountId,
                            pageSize,
                            page,
                            featureConfig.municipalMessageAccountName,
                            archiveFolderId
                        )
                    }
                }
            }
            .also {
                Audit.MessagingMessagesInFolderRead.log(
                    targetId = accountId,
                    args = mapOf("total" to it.total)
                )
            }
    }

    @GetMapping("/{accountId}/copies")
    fun getMessageCopies(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @RequestParam pageSize: Int,
        @RequestParam page: Int
    ): Paged<MessageCopy> {
        return db.connect { dbc ->
                requireMessageAccountAccess(dbc, user, clock, accountId)
                dbc.read { it.getMessageCopiesByAccount(accountId, pageSize, page) }
            }
            .also {
                Audit.MessagingReceivedMessagesRead.log(
                    targetId = accountId,
                    args = mapOf("total" to it.total)
                )
            }
    }

    @GetMapping("/{accountId}/sent")
    fun getSentMessages(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @RequestParam pageSize: Int,
        @RequestParam page: Int
    ): Paged<SentMessage> {
        return db.connect { dbc ->
                requireMessageAccountAccess(dbc, user, clock, accountId)
                dbc.read { it.getMessagesSentByAccount(accountId, pageSize, page) }
            }
            .also {
                Audit.MessagingSentMessagesRead.log(
                    targetId = accountId,
                    args = mapOf("total" to it.total)
                )
            }
    }

    @GetMapping("/unread")
    fun getUnreadMessages(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock
    ): Set<UnreadCountByAccount> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.ACCESS_MESSAGING
                    )
                    tx.getUnreadMessagesCounts(
                        tx.getEmployeeMessageAccountIds(getEmployeeId(user)!!)
                    )
                }
            }
            .also { Audit.MessagingUnreadMessagesRead.log(args = mapOf("count" to it.size)) }
    }

    @GetMapping("/unread/{unitId}")
    fun getUnreadMessagesByUnit(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId
    ): Set<UnreadCountByAccountAndGroup> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_UNREAD_MESSAGES,
                        unitId
                    )
                    tx.getUnreadMessagesCountsByDaycare(unitId)
                }
            }
            .also {
                Audit.MessagingUnreadMessagesRead.log(
                    targetId = unitId,
                    args = mapOf("count" to it.size)
                )
            }
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
        @RequestBody body: PostMessageBody
    ) {
        db.connect { dbc ->
            requireMessageAccountAccess(dbc, user, clock, accountId)
            dbc.transaction { tx ->
                val senderAccountType = tx.getMessageAccountType(accountId)
                if (
                    senderAccountType == AccountType.MUNICIPAL && body.type != MessageType.BULLETIN
                ) {
                    throw BadRequest(
                        "Municipal message accounts are only allowed to send bulletins"
                    )
                }

                val messageAccountIdsToChildIds =
                    tx.getMessageAccountsForRecipients(accountId, body.recipients, clock.today())
                val messageRecipients = messageAccountIdsToChildIds.keys

                if (messageRecipients.isEmpty()) return@transaction

                val staffCopyRecipients =
                    if (body.recipients.none { it.type == MessageRecipientType.CHILD }) {
                        tx.getStaffCopyRecipients(
                            accountId,
                            body.recipients.mapNotNull {
                                if (it.type == MessageRecipientType.AREA) AreaId(it.id.raw)
                                else null
                            },
                            body.recipients.mapNotNull {
                                if (it.type == MessageRecipientType.UNIT) DaycareId(it.id.raw)
                                else null
                            },
                            body.recipients.mapNotNull {
                                if (it.type == MessageRecipientType.GROUP) GroupId(it.id.raw)
                                else null
                            },
                            clock.today()
                        )
                    } else {
                        setOf()
                    }

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
                    staffCopyRecipients = staffCopyRecipients,
                    accountIdsToChildIds = messageAccountIdsToChildIds,
                    municipalAccountName = featureConfig.municipalMessageAccountName
                )
                if (body.draftId != null)
                    tx.deleteDraft(accountId = accountId, draftId = body.draftId)
            }
        }
        Audit.MessagingNewMessageWrite.log(targetId = accountId)
    }

    @GetMapping("/{accountId}/drafts")
    fun getDrafts(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId
    ): List<DraftContent> {
        return db.connect { dbc ->
                requireMessageAccountAccess(dbc, user, clock, accountId)
                dbc.transaction { it.getDrafts(accountId) }
            }
            .also {
                Audit.MessagingDraftsRead.log(
                    targetId = accountId,
                    args = mapOf("count" to it.size)
                )
            }
    }

    @PostMapping("/{accountId}/drafts")
    fun initDraft(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId
    ): MessageDraftId {
        return db.connect { dbc ->
                requireMessageAccountAccess(dbc, user, clock, accountId)
                dbc.transaction { it.initDraft(accountId) }
            }
            .also { messageDraftId ->
                Audit.MessagingCreateDraft.log(targetId = accountId, objectId = messageDraftId)
            }
    }

    @PutMapping("/{accountId}/drafts/{draftId}")
    fun upsertDraft(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @PathVariable draftId: MessageDraftId,
        @RequestBody content: UpdatableDraftContent
    ) {
        return db.connect { dbc ->
                requireMessageAccountAccess(dbc, user, clock, accountId)
                dbc.transaction { it.updateDraft(accountId, draftId, content) }
            }
            .also { Audit.MessagingUpdateDraft.log(targetId = accountId, objectId = draftId) }
    }

    @DeleteMapping("/{accountId}/drafts/{draftId}")
    fun deleteDraft(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @PathVariable draftId: MessageDraftId
    ) {
        return db.connect { dbc ->
                requireMessageAccountAccess(dbc, user, clock, accountId)
                dbc.transaction { tx -> tx.deleteDraft(accountId, draftId) }
            }
            .also { Audit.MessagingDeleteDraft.log(targetId = accountId, objectId = draftId) }
    }

    @PostMapping("{accountId}/{messageId}/reply")
    fun replyToThread(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @PathVariable messageId: MessageId,
        @RequestBody body: ReplyToMessageBody
    ): MessageService.ThreadReply {
        return db.connect { dbc ->
                requireMessageAccountAccess(dbc, user, clock, accountId)

                messageService.replyToThread(
                    db = dbc,
                    now = clock.now(),
                    replyToMessageId = messageId,
                    senderAccount = accountId,
                    recipientAccountIds = body.recipientAccountIds,
                    content = body.content,
                    municipalAccountName = featureConfig.municipalMessageAccountName
                )
            }
            .also {
                Audit.MessagingReplyToMessageWrite.log(targetId = accountId, objectId = messageId)
            }
    }

    @PutMapping("/{accountId}/threads/{threadId}/read")
    fun markThreadRead(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @PathVariable threadId: MessageThreadId
    ) {
        db.connect { dbc ->
            requireMessageAccountAccess(dbc, user, clock, accountId)
            dbc.transaction { it.markThreadRead(clock, accountId, threadId) }
        }
        Audit.MessagingMarkMessagesReadWrite.log(targetId = accountId, objectId = threadId)
    }

    @PutMapping("/{accountId}/threads/{threadId}/archive")
    fun archiveThread(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @PathVariable threadId: MessageThreadId,
    ) {
        db.connect { dbc ->
            requireMessageAccountAccess(dbc, user, clock, accountId)
            dbc.transaction { it.archiveThread(accountId, threadId) }
        }
        Audit.MessagingArchiveMessageWrite.log(targetId = accountId, objectId = threadId)
    }

    @GetMapping("/receivers")
    fun getReceiversForNewMessage(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock
    ): List<MessageReceiversResponse> {
        val employeeId = getEmployeeId(user) ?: throw Forbidden("Permission denied")
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.READ_MESSAGE_RECEIVERS
                    )
                    it.getReceiversForNewMessage(employeeId, clock.today())
                }
            }
            .also { Audit.MessagingMessageReceiversRead.log(args = mapOf("count" to it.size)) }
    }

    private fun requireMessageAccountAccess(
        db: Database.Connection,
        user: AuthenticatedUser,
        clock: EvakaClock,
        accountId: MessageAccountId
    ) {
        db.read {
                accessControl.requirePermissionFor(it, user, clock, Action.Global.ACCESS_MESSAGING)
                it.getEmployeeMessageAccountIds(getEmployeeId(user)!!)
            }
            .find { it == accountId }
            ?: throw Forbidden("Message account not found for user")
    }

    fun getEmployeeId(user: AuthenticatedUser): EmployeeId? =
        when (user) {
            is AuthenticatedUser.MobileDevice -> user.employeeId
            is AuthenticatedUser.Employee -> user.id
            else -> null
        }
}
