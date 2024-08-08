// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.application.personHasSentApplicationWithId
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.MessageContentId
import fi.espoo.evaka.shared.MessageDraftId
import fi.espoo.evaka.shared.MessageId
import fi.espoo.evaka.shared.MessageThreadId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.NotFound
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
    private val messageService: MessageService
) {

    @GetMapping("/my-accounts")
    fun getAccountsByUser(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock
    ): List<AuthorizedMessageAccount> {
        return db.connect { dbc ->
                dbc.read {
                    val filter =
                        accessControl.requireAuthorizationFilter(
                            it,
                            user,
                            clock,
                            Action.MessageAccount.ACCESS
                        )
                    it.getAuthorizedMessageAccountsForEmployee(
                        filter,
                        featureConfig.municipalMessageAccountName,
                        featureConfig.serviceWorkerMessageAccountName
                    )
                }
            }
            .also { accounts ->
                Audit.MessagingMyAccountsRead.log(
                    targetId = AuditId(accounts.map { it.account.id })
                )
            }
    }

    @GetMapping("/mobile/my-accounts/{unitId}")
    fun getAccountsByDevice(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId
    ): List<AuthorizedMessageAccount> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    if (user.employeeId != null) {
                        val filter =
                            accessControl.requireAuthorizationFilter(
                                tx,
                                user,
                                clock,
                                Action.MessageAccount.ACCESS
                            )
                        tx.getAuthorizedMessageAccountsForEmployee(
                                filter,
                                featureConfig.municipalMessageAccountName,
                                featureConfig.serviceWorkerMessageAccountName
                            )
                            // Only return group accounts of the requested unit
                            .filter {
                                it.account.type == AccountType.GROUP &&
                                    it.daycareGroup?.unitId == unitId
                            }
                    } else listOf()
                }
            }
            .also { accounts ->
                Audit.MessagingMyAccountsRead.log(
                    targetId = AuditId(accounts.map { it.account.id }),
                    objectId = AuditId(unitId)
                )
            }
    }

    @GetMapping("/{accountId}/received")
    fun getReceivedMessages(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @RequestParam pageSize: Int,
        @RequestParam page: Int
    ): PagedMessageThreads {
        return db.connect { dbc ->
                requireMessageAccountAccess(dbc, user, clock, accountId)
                dbc.read {
                    it.getReceivedThreads(
                        accountId,
                        pageSize,
                        page,
                        featureConfig.municipalMessageAccountName,
                        featureConfig.serviceWorkerMessageAccountName
                    )
                }
            }
            .also {
                Audit.MessagingReceivedMessagesRead.log(
                    targetId = AuditId(accountId),
                    meta = mapOf("total" to it.total)
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
    ): PagedMessageThreads {
        return db.connect { dbc ->
                requireMessageAccountAccess(dbc, user, clock, accountId)
                dbc.read {
                    val archiveFolderId = it.getArchiveFolderId(accountId)
                    if (archiveFolderId == null) {
                        PagedMessageThreads(emptyList(), 0, 0)
                    } else {
                        it.getReceivedThreads(
                            accountId,
                            pageSize,
                            page,
                            featureConfig.municipalMessageAccountName,
                            featureConfig.serviceWorkerMessageAccountName,
                            archiveFolderId
                        )
                    }
                }
            }
            .also {
                Audit.MessagingMessagesInFolderRead.log(
                    targetId = AuditId(accountId),
                    meta = mapOf("total" to it.total)
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
    ): PagedMessageCopies {
        return db.connect { dbc ->
                requireMessageAccountAccess(dbc, user, clock, accountId)
                dbc.read { it.getMessageCopiesByAccount(accountId, pageSize, page) }
            }
            .also {
                Audit.MessagingReceivedMessageCopiesRead.log(
                    targetId = AuditId(accountId),
                    meta = mapOf("total" to it.total)
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
    ): PagedSentMessages {
        return db.connect { dbc ->
                requireMessageAccountAccess(dbc, user, clock, accountId)
                dbc.read { it.getMessagesSentByAccount(accountId, pageSize, page) }
            }
            .also {
                Audit.MessagingSentMessagesRead.log(
                    targetId = AuditId(accountId),
                    meta = mapOf("total" to it.total)
                )
            }
    }

    @GetMapping("/{accountId}/thread/{threadId}")
    fun getThread(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @PathVariable threadId: MessageThreadId
    ): MessageThread {
        return db.connect { dbc ->
                requireMessageAccountAccess(dbc, user, clock, accountId)
                dbc.read {
                    it.getMessageThread(
                        accountId,
                        threadId,
                        featureConfig.municipalMessageAccountName,
                        featureConfig.serviceWorkerMessageAccountName
                    )
                }
            }
            .also {
                Audit.MessagingMessageThreadRead.log(
                    targetId = AuditId(listOf(accountId, threadId))
                )
            }
    }

    @GetMapping("/application/{applicationId}")
    fun getThreadByApplicationId(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable applicationId: ApplicationId
    ): MessageThread? {
        return db.connect { dbc ->
                val accountId =
                    dbc.read { it.getServiceWorkerAccountId() }
                        ?: throw NotFound("No account found")
                requireMessageAccountAccess(dbc, user, clock, accountId)
                val response =
                    dbc.read {
                        it.getMessageThreadByApplicationId(
                            accountId,
                            applicationId,
                            featureConfig.municipalMessageAccountName,
                            featureConfig.serviceWorkerMessageAccountName
                        )
                    }
                accountId to response
            }
            .let { (accountId, response) ->
                Audit.MessagingMessageThreadRead.log(
                    targetId = AuditId(listOfNotNull(accountId, response?.id)),
                    objectId = AuditId(applicationId)
                )
                response
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
                    val filter =
                        accessControl.requireAuthorizationFilter(
                            tx,
                            user,
                            clock,
                            Action.MessageAccount.ACCESS
                        )
                    tx.getUnreadMessagesCounts(filter)
                }
            }
            .also { response ->
                Audit.MessagingUnreadMessagesRead.log(
                    targetId = AuditId(response.map { it.accountId })
                )
            }
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
            .also { response ->
                Audit.MessagingUnreadMessagesRead.log(
                    targetId = AuditId(unitId),
                    objectId = AuditId(response.map { it.accountId })
                )
            }
    }

    data class PostMessageFilters(
        val yearsOfBirth: List<Int> = listOf(),
        val shiftCare: Boolean = false,
        val intermittentShiftCare: Boolean = false,
        val familyDaycare: Boolean = false
    )

    data class PostMessagePreflightBody(
        val recipients: Set<MessageRecipient>,
        val filters: PostMessageFilters? = null
    )

    data class PostMessagePreflightResponse(val numberOfRecipientAccounts: Int)

    @PostMapping("/{accountId}/preflight-check")
    fun createMessagePreflightCheck(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @RequestBody body: PostMessagePreflightBody
    ): PostMessagePreflightResponse {
        return db.connect { dbc ->
                requireMessageAccountAccess(dbc, user, clock, accountId)
                dbc.read { tx ->
                    val numberOfRecipientAccounts =
                        if (body.recipients.isEmpty()) {
                            0
                        } else {
                            tx.getMessageAccountsForRecipients(
                                    accountId = accountId,
                                    recipients = body.recipients,
                                    filters = body.filters,
                                    date = clock.today()
                                )
                                .map { it.first }
                                .toSet()
                                .size
                        }

                    PostMessagePreflightResponse(numberOfRecipientAccounts)
                }
            }
            .also { Audit.MessagingNewMessagePreflightCheck.log(targetId = AuditId(accountId)) }
    }

    data class PostMessageBody(
        val title: String,
        val content: String,
        val type: MessageType,
        val urgent: Boolean,
        val sensitive: Boolean,
        val recipients: Set<MessageRecipient>,
        val recipientNames: List<String>,
        val attachmentIds: Set<AttachmentId> = setOf(),
        val draftId: MessageDraftId? = null,
        val relatedApplicationId: ApplicationId? = null,
        val filters: PostMessageFilters? = null
    )

    @PostMapping("/{accountId}")
    fun createMessage(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @RequestBody body: PostMessageBody
    ): MessageContentId? {
        if (body.recipients.isEmpty()) {
            throw BadRequest("Message must have at least one recipient")
        }
        if (body.content.isBlank()) {
            throw BadRequest("Message content cannot be empty")
        }
        if (body.title.isBlank()) {
            throw BadRequest("Message title cannot be empty")
        }
        return db.connect { dbc ->
                requireMessageAccountAccess(dbc, user, clock, accountId)
                dbc.transaction { tx ->
                    val senderAccountType = tx.getMessageAccountType(accountId)
                    if (
                        body.sensitive &&
                            (senderAccountType != AccountType.PERSONAL ||
                                body.recipients.size != 1 ||
                                body.recipients.first().type != MessageRecipientType.CHILD)
                    ) {
                        throw BadRequest(
                            "Sensitive messages are only allowed to be sent from personal accounts to a single child recipient"
                        )
                    }
                    if (
                        senderAccountType == AccountType.MUNICIPAL &&
                            body.type != MessageType.BULLETIN
                    ) {
                        throw BadRequest(
                            "Municipal message accounts are only allowed to send bulletins"
                        )
                    }
                    if (senderAccountType == AccountType.SERVICE_WORKER) {
                        if (body.relatedApplicationId == null) {
                            throw BadRequest(
                                "Service worker message accounts must have a related application"
                            )
                        } else if (
                            body.recipients.any { it.type != MessageRecipientType.CITIZEN }
                        ) {
                            throw BadRequest(
                                "Service worker message accounts can only send messages to citizens"
                            )
                        } else if (
                            body.recipients.any {
                                !tx.personHasSentApplicationWithId(
                                    PersonId(it.id.raw),
                                    body.relatedApplicationId
                                )
                            }
                        ) {
                            throw BadRequest(
                                "Service worker message accounts can only send messages to citizens with sent applications"
                            )
                        }
                    }

                    messageService
                        .sendMessageAsEmployee(
                            tx,
                            user,
                            clock.now(),
                            accountId,
                            type = body.type,
                            msg =
                                NewMessageStub(
                                    title = body.title,
                                    content = body.content,
                                    urgent = body.urgent,
                                    sensitive = body.sensitive
                                ),
                            recipients = body.recipients,
                            recipientNames = body.recipientNames,
                            attachments = body.attachmentIds,
                            relatedApplication = body.relatedApplicationId,
                            filters = body.filters
                        )
                        .also {
                            if (body.draftId != null) {
                                tx.deleteDraft(accountId = accountId, draftId = body.draftId)
                            }
                        }
                }
            }
            .also {
                Audit.MessagingNewMessageWrite.log(
                    targetId = AuditId(accountId),
                    objectId = it?.let(AuditId::invoke)
                )
            }
    }

    @GetMapping("/{accountId}/drafts")
    fun getDraftMessages(
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
                    targetId = AuditId(accountId),
                    meta = mapOf("count" to it.size)
                )
            }
    }

    @PostMapping("/{accountId}/drafts")
    fun initDraftMessage(
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
                Audit.MessagingCreateDraft.log(
                    targetId = AuditId(accountId),
                    objectId = AuditId(messageDraftId)
                )
            }
    }

    @PutMapping("/{accountId}/drafts/{draftId}")
    fun updateDraftMessage(
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
            .also {
                Audit.MessagingUpdateDraft.log(
                    targetId = AuditId(accountId),
                    objectId = AuditId(draftId)
                )
            }
    }

    @DeleteMapping("/{accountId}/drafts/{draftId}")
    fun deleteDraftMessage(
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
            .also {
                Audit.MessagingDeleteDraft.log(
                    targetId = AuditId(accountId),
                    objectId = AuditId(draftId)
                )
            }
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
                    user = user,
                    municipalAccountName = featureConfig.municipalMessageAccountName,
                    serviceWorkerAccountName = featureConfig.serviceWorkerMessageAccountName
                )
            }
            .also {
                Audit.MessagingReplyToMessageWrite.log(
                    targetId = AuditId(listOf(accountId, messageId)),
                    objectId = AuditId(listOf(it.threadId, it.message.id))
                )
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
        Audit.MessagingMarkMessagesReadWrite.log(targetId = AuditId(listOf(accountId, threadId)))
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
        Audit.MessagingArchiveMessageWrite.log(targetId = AuditId(listOf(accountId, threadId)))
    }

    @GetMapping("/receivers")
    fun getReceiversForNewMessage(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock
    ): List<MessageReceiversResponse> {
        return db.connect { dbc ->
                dbc.read {
                    val filter =
                        accessControl.requireAuthorizationFilter(
                            it,
                            user,
                            clock,
                            Action.MessageAccount.ACCESS
                        )
                    it.getReceiversForNewMessage(filter, clock.today())
                }
            }
            .also { response ->
                Audit.MessagingMessageReceiversRead.log(
                    targetId = AuditId(response.map { it.accountId })
                )
            }
    }

    private fun requireMessageAccountAccess(
        db: Database.Connection,
        user: AuthenticatedUser,
        clock: EvakaClock,
        accountId: MessageAccountId
    ) {
        db.read {
                val filter =
                    accessControl.requireAuthorizationFilter(
                        it,
                        user,
                        clock,
                        Action.MessageAccount.ACCESS
                    )
                it.getEmployeeMessageAccountIds(filter)
            }
            .find { it == accountId } ?: throw Forbidden("Message account not found for user")
    }
}
