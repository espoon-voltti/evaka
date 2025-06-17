// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.application.personHasSentApplicationWithId
import fi.espoo.evaka.invoicing.controller.SortDirection
import fi.espoo.evaka.placement.MessagingCategory
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.MessageContentId
import fi.espoo.evaka.shared.MessageDraftId
import fi.espoo.evaka.shared.MessageId
import fi.espoo.evaka.shared.MessageThreadFolderId
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

data class UnreadCountByAccount(
    val accountId: MessageAccountId,
    val unreadCopyCount: Int,
    val unreadCount: Int,
    val unreadCountByFolder: Map<MessageThreadFolderId, Int>,
) {
    val totalUnreadCount: Int
        get() = unreadCount + unreadCopyCount + unreadCountByFolder.values.sum()
}

data class UnreadCountByAccountAndGroup(
    val accountId: MessageAccountId,
    val unreadCount: Int,
    val unreadCopyCount: Int,
    val groupId: GroupId,
)

data class ReplyToMessageBody(val content: String, val recipientAccountIds: Set<MessageAccountId>)

@RestController
class MessageController(
    private val accessControl: AccessControl,
    private val featureConfig: FeatureConfig,
    private val messageService: MessageService,
) {

    @GetMapping("/employee/messages/my-accounts")
    fun getAccountsByUser(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): List<AuthorizedMessageAccount> {
        return db.connect { dbc ->
                dbc.read {
                    val filter =
                        accessControl.requireAuthorizationFilter(
                            it,
                            user,
                            clock,
                            Action.MessageAccount.ACCESS,
                        )
                    it.getAuthorizedMessageAccountsForEmployee(
                        filter,
                        featureConfig.municipalMessageAccountName,
                        featureConfig.serviceWorkerMessageAccountName,
                        featureConfig.financeMessageAccountName,
                    )
                }
            }
            .also { accounts ->
                Audit.MessagingMyAccountsRead.log(
                    targetId = AuditId(accounts.map { it.account.id })
                )
            }
    }

    @GetMapping("/employee-mobile/messages/my-accounts/{unitId}")
    fun getAccountsByDevice(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
    ): List<AuthorizedMessageAccount> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    if (user.employeeId != null) {
                        val filter =
                            accessControl.requireAuthorizationFilter(
                                tx,
                                user,
                                clock,
                                Action.MessageAccount.ACCESS,
                            )
                        tx.getAuthorizedMessageAccountsForEmployee(
                                filter,
                                featureConfig.municipalMessageAccountName,
                                featureConfig.serviceWorkerMessageAccountName,
                                featureConfig.financeMessageAccountName,
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
                    objectId = AuditId(unitId),
                )
            }
    }

    @GetMapping("/employee/messages/{accountId}/received")
    fun getReceivedMessages(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @RequestParam page: Int,
    ): PagedMessageThreads {
        return db.connect { dbc ->
            requireMessageAccountAccess(dbc, user, clock, accountId)
            getReceivedMessages(dbc, user.id, accountId, page)
        }
    }

    @GetMapping("/employee-mobile/messages/{accountId}/received")
    fun getReceivedMessages(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @RequestParam page: Int,
        @RequestParam childId: ChildId? = null,
    ): PagedMessageThreads {
        return db.connect { dbc ->
            requireMessageAccountAccess(dbc, user, clock, accountId)
            getReceivedMessages(dbc, user.employeeId!!, accountId, page, childId)
        }
    }

    private fun getReceivedMessages(
        dbc: Database.Connection,
        employeeId: EmployeeId,
        accountId: MessageAccountId,
        page: Int,
        childId: ChildId? = null,
    ): PagedMessageThreads {
        return dbc.read {
                val accountAccessLimit = it.getAccountAccessLimit(accountId, employeeId)
                it.getReceivedThreads(
                    accountId,
                    pageSize = 20,
                    page,
                    featureConfig.municipalMessageAccountName,
                    featureConfig.serviceWorkerMessageAccountName,
                    featureConfig.financeMessageAccountName,
                    accountAccessLimit = accountAccessLimit,
                    childId = childId,
                )
            }
            .also {
                Audit.MessagingReceivedMessagesRead.log(
                    targetId = AuditId(accountId),
                    meta = mapOf("total" to it.total),
                )
            }
    }

    @GetMapping("/employee/messages/{accountId}/archived")
    fun getArchivedMessages(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @RequestParam page: Int,
    ): PagedMessageThreads {
        return db.connect { dbc ->
                requireMessageAccountAccess(dbc, user, clock, accountId)
                dbc.read {
                    val archiveFolderId = it.getArchiveFolderId(accountId)
                    if (archiveFolderId == null) {
                        PagedMessageThreads(emptyList(), 0, 0)
                    } else {
                        val accountAccessLimit = it.getAccountAccessLimit(accountId, user.id)

                        it.getReceivedThreads(
                            accountId,
                            pageSize = 20,
                            page,
                            featureConfig.municipalMessageAccountName,
                            featureConfig.serviceWorkerMessageAccountName,
                            featureConfig.financeMessageAccountName,
                            archiveFolderId,
                            accountAccessLimit,
                        )
                    }
                }
            }
            .also {
                Audit.MessagingMessagesInFolderRead.log(
                    targetId = AuditId(accountId),
                    meta = mapOf("total" to it.total),
                )
            }
    }

    data class MessageThreadFolder(
        val id: MessageThreadFolderId,
        val name: String,
        val ownerId: MessageAccountId,
    )

    @GetMapping("/employee/messages/folders")
    fun getFolders(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): List<MessageThreadFolder> {
        return db.connect { dbc ->
                dbc.read {
                    val filter =
                        accessControl.requireAuthorizationFilter(
                            it,
                            user,
                            clock,
                            Action.MessageAccount.ACCESS,
                        )
                    it.getFolders(filter)
                }
            }
            .also { Audit.MessagingMessageFoldersRead.log() }
    }

    @GetMapping("/employee/messages/{accountId}/folders/{folderId}")
    fun getMessagesInFolder(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @PathVariable folderId: MessageThreadFolderId,
        @RequestParam page: Int,
    ): PagedMessageThreads {
        return db.connect { dbc ->
                requireMessageAccountAccess(dbc, user, clock, accountId)
                dbc.read {
                    it.getThreads(
                        accountId,
                        pageSize = 20,
                        page,
                        featureConfig.municipalMessageAccountName,
                        featureConfig.serviceWorkerMessageAccountName,
                        featureConfig.financeMessageAccountName,
                        folderId,
                    )
                }
            }
            .also {
                Audit.MessagingMessagesInFolderRead.log(
                    targetId = AuditId(accountId),
                    meta = mapOf("total" to it.total),
                )
            }
    }

    @GetMapping("/employee/messages/{accountId}/copies")
    fun getMessageCopies(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @RequestParam page: Int,
    ): PagedMessageCopies {
        return db.connect { dbc ->
                requireMessageAccountAccess(dbc, user, clock, accountId)
                dbc.read {
                    val accountAccessLimit = it.getAccountAccessLimit(accountId, user.id)
                    it.getMessageCopiesByAccount(accountId, pageSize = 20, page, accountAccessLimit)
                }
            }
            .also {
                Audit.MessagingReceivedMessageCopiesRead.log(
                    targetId = AuditId(accountId),
                    meta = mapOf("total" to it.total),
                )
            }
    }

    @GetMapping("/employee/messages/{accountId}/sent")
    fun getSentMessages(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @RequestParam page: Int,
    ): PagedSentMessages {
        return db.connect { dbc ->
            requireMessageAccountAccess(dbc, user, clock, accountId)
            getSentMessages(dbc, user.id, accountId, page)
        }
    }

    @GetMapping("/employee-mobile/messages/{accountId}/sent")
    fun getSentMessages(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @RequestParam page: Int,
    ): PagedSentMessages {
        return db.connect { dbc ->
            requireMessageAccountAccess(dbc, user, clock, accountId)
            getSentMessages(dbc, user.employeeId!!, accountId, page)
        }
    }

    private fun getSentMessages(
        dbc: Database.Connection,
        employeeId: EmployeeId,
        accountId: MessageAccountId,
        page: Int,
    ): PagedSentMessages {
        return dbc.read {
                val accountAccessLimit = it.getAccountAccessLimit(accountId, employeeId)
                it.getMessagesSentByAccount(accountId, pageSize = 20, page, accountAccessLimit)
            }
            .also {
                Audit.MessagingSentMessagesRead.log(
                    targetId = AuditId(accountId),
                    meta = mapOf("total" to it.total),
                )
            }
    }

    @GetMapping("/employee/messages/{accountId}/thread/{threadId}")
    fun getThread(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @PathVariable threadId: MessageThreadId,
    ): MessageThread = getThread(db, user as AuthenticatedUser, clock, accountId, threadId)

    @GetMapping("/employee-mobile/messages/{accountId}/thread/{threadId}")
    fun getThread(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @PathVariable threadId: MessageThreadId,
    ): MessageThread = getThread(db, user as AuthenticatedUser, clock, accountId, threadId)

    private fun getThread(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @PathVariable threadId: MessageThreadId,
    ): MessageThread {
        return db.connect { dbc ->
                requireMessageAccountAccess(dbc, user, clock, accountId)
                dbc.read {
                    it.getMessageThread(
                        accountId,
                        threadId,
                        featureConfig.municipalMessageAccountName,
                        featureConfig.serviceWorkerMessageAccountName,
                        featureConfig.financeMessageAccountName,
                    )
                }
            }
            .also {
                Audit.MessagingMessageThreadRead.log(
                    targetId = AuditId(listOf(accountId, threadId))
                )
            }
    }

    data class ThreadByApplicationResponse(val thread: MessageThread?)

    @GetMapping("/employee/messages/application/{applicationId}")
    fun getThreadByApplicationId(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable applicationId: ApplicationId,
    ): ThreadByApplicationResponse =
        db.connect { dbc ->
                val accountId =
                    dbc.read { it.getServiceWorkerAccountId() }
                        ?: throw NotFound("No account found")
                requireMessageAccountAccess(dbc, user, clock, accountId)
                val thread =
                    dbc.read {
                        it.getMessageThreadByApplicationId(
                            accountId,
                            applicationId,
                            featureConfig.municipalMessageAccountName,
                            featureConfig.serviceWorkerMessageAccountName,
                            featureConfig.financeMessageAccountName,
                        )
                    }
                accountId to thread
            }
            .let { (accountId, thread) ->
                Audit.MessagingMessageThreadRead.log(
                    targetId = AuditId(listOfNotNull(accountId, thread?.id)),
                    objectId = AuditId(applicationId),
                )
                ThreadByApplicationResponse(thread = thread)
            }

    @GetMapping("/employee/messages/finance/{personId}")
    fun getFinanceMessagesWithPerson(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable personId: PersonId,
    ): List<MessageThread> {
        return db.connect { dbc ->
                val accountId =
                    dbc.read { it.getFinanceAccountId() } ?: throw NotFound("No account found")
                requireMessageAccountAccess(dbc, user, clock, accountId)
                val threads =
                    dbc.read {
                        val personAccountId = it.getCitizenMessageAccount(personId)
                        val filter =
                            accessControl.requireAuthorizationFilter(
                                it,
                                user,
                                clock,
                                Action.MessageAccount.ACCESS,
                            )
                        val folderIds =
                            listOf(null) + it.getFolders(filter).map { folder -> folder.id }
                        folderIds.flatMap { folderId ->
                            it.getThreads(
                                    accountId,
                                    pageSize = 200,
                                    page = 1,
                                    featureConfig.municipalMessageAccountName,
                                    featureConfig.serviceWorkerMessageAccountName,
                                    featureConfig.financeMessageAccountName,
                                    personAccountId = personAccountId,
                                    messagesSortDirection = SortDirection.DESC,
                                    folderId = folderId,
                                )
                                .data
                        }
                    }
                accountId to threads
            }
            .let { (accountId, threads) ->
                Audit.MessagingMessagesInFolderRead.log(
                    targetId = AuditId(accountId),
                    meta = mapOf("total" to threads.size),
                )
                threads
            }
    }

    @GetMapping("/employee/messages/unread")
    fun getUnreadMessages(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): Set<UnreadCountByAccount> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    val filter =
                        accessControl.requireAuthorizationFilter(
                            tx,
                            user,
                            clock,
                            Action.MessageAccount.ACCESS,
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

    @GetMapping("/employee-mobile/messages/unread/{unitId}")
    fun getUnreadMessagesByUnit(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
    ): Set<UnreadCountByAccountAndGroup> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_UNREAD_MESSAGES,
                        unitId,
                    )
                    tx.getUnreadMessagesCountsByDaycare(unitId)
                }
            }
            .also { response ->
                Audit.MessagingUnreadMessagesRead.log(
                    targetId = AuditId(unitId),
                    objectId = AuditId(response.map { it.accountId }),
                )
            }
    }

    data class PostMessageFilters(
        val yearsOfBirth: List<Int> = listOf(),
        val shiftCare: Boolean = false,
        val intermittentShiftCare: Boolean = false,
        val familyDaycare: Boolean = false,
        val placementTypes: List<MessagingCategory> = listOf(),
    )

    data class PostMessagePreflightBody(
        val recipients: Set<MessageRecipient>,
        val filters: PostMessageFilters? = null,
    )

    data class PostMessagePreflightResponse(val numberOfRecipientAccounts: Int)

    @PostMapping("/employee/messages/{accountId}/preflight-check")
    fun createMessagePreflightCheck(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @RequestBody body: PostMessagePreflightBody,
    ): PostMessagePreflightResponse =
        createMessagePreflightCheck(db, user as AuthenticatedUser, clock, accountId, body)

    @PostMapping("/employee-mobile/messages/{accountId}/preflight-check")
    fun createMessagePreflightCheck(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @RequestBody body: PostMessagePreflightBody,
    ): PostMessagePreflightResponse =
        createMessagePreflightCheck(db, user as AuthenticatedUser, clock, accountId, body)

    private fun createMessagePreflightCheck(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @RequestBody body: PostMessagePreflightBody,
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
                                    date = clock.today(),
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
        val filters: PostMessageFilters? = null,
    )

    data class CreateMessageResponse(val createdId: MessageContentId?)

    @PostMapping("/employee/messages/{accountId}")
    fun createMessage(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @RequestParam initialFolder: MessageThreadFolderId?,
        @RequestBody body: PostMessageBody,
    ): CreateMessageResponse =
        createMessageShared(db, user as AuthenticatedUser, clock, accountId, body, initialFolder)

    @PostMapping("/employee-mobile/messages/{accountId}")
    fun createMessage(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @RequestBody body: PostMessageBody,
    ): CreateMessageResponse =
        createMessageShared(db, user as AuthenticatedUser, clock, accountId, body)

    private fun createMessageShared(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        accountId: MessageAccountId,
        body: PostMessageBody,
        initialFolder: MessageThreadFolderId? = null,
    ): CreateMessageResponse {
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
                            (body.recipients.size != 1 ||
                                !((senderAccountType == AccountType.PERSONAL &&
                                    body.recipients.first().type == MessageRecipientType.CHILD) ||
                                    (senderAccountType == AccountType.FINANCE &&
                                        body.recipients.first().type ==
                                            MessageRecipientType.CITIZEN)))
                    ) {
                        throw BadRequest(
                            "Sensitive messages are only allowed to be sent from personal accounts to a single child recipient or from financial account to a single citizen"
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
                        }
                        val citizenRecipients =
                            body.recipients.filterIsInstance<MessageRecipient.Citizen>()
                        if (body.recipients.size != citizenRecipients.size) {
                            throw BadRequest(
                                "Service worker message accounts can only send messages to citizens"
                            )
                        }

                        if (
                            citizenRecipients.any {
                                !tx.personHasSentApplicationWithId(it.id, body.relatedApplicationId)
                            }
                        ) {
                            throw BadRequest(
                                "Service worker message accounts can only send messages to citizens with sent applications"
                            )
                        }
                    }
                    if (senderAccountType == AccountType.FINANCE) {
                        if (body.recipients.size > 1) {
                            throw BadRequest(
                                "Finance message accounts can only send messages to single recipient"
                            )
                        } else if (
                            body.recipients.any { it.type != MessageRecipientType.CITIZEN }
                        ) {
                            throw BadRequest(
                                "Finance message accounts can only send messages to citizens"
                            )
                        }
                    }

                    val createdId =
                        messageService.sendMessageAsEmployee(
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
                                    sensitive = body.sensitive,
                                ),
                            recipients = body.recipients,
                            recipientNames = body.recipientNames,
                            attachments = body.attachmentIds,
                            relatedApplication = body.relatedApplicationId,
                            filters = body.filters,
                            initialFolder = initialFolder,
                        )
                    if (body.draftId != null) {
                        tx.deleteDraft(accountId = accountId, draftId = body.draftId)
                    }
                    CreateMessageResponse(createdId)
                }
            }
            .also {
                Audit.MessagingNewMessageWrite.log(
                    targetId = AuditId(accountId),
                    objectId = it.createdId?.let(AuditId::invoke),
                )
            }
    }

    @GetMapping("/employee/messages/{accountId}/drafts")
    fun getDraftMessages(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
    ): List<DraftContent> {
        return db.connect { dbc ->
            requireMessageAccountAccess(dbc, user, clock, accountId)
            getDraftMessages(dbc, accountId, user.id)
        }
    }

    @GetMapping("/employee-mobile/messages/{accountId}/drafts")
    fun getDraftMessages(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
    ): List<DraftContent> {
        return db.connect { dbc ->
            requireMessageAccountAccess(dbc, user, clock, accountId)
            getDraftMessages(dbc, accountId, user.employeeId!!)
        }
    }

    private fun getDraftMessages(
        dbc: Database.Connection,
        accountId: MessageAccountId,
        employeeId: EmployeeId,
    ): List<DraftContent> {
        return dbc.transaction {
                val accountAccessLimit = it.getAccountAccessLimit(accountId, employeeId)
                it.getDrafts(accountId, accountAccessLimit)
            }
            .also {
                Audit.MessagingDraftsRead.log(
                    targetId = AuditId(accountId),
                    meta = mapOf("count" to it.size),
                )
            }
    }

    @PostMapping("/employee/messages/{accountId}/drafts")
    fun initDraftMessage(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
    ): MessageDraftId = initDraftMessage(db, user as AuthenticatedUser, clock, accountId)

    @PostMapping("/employee-mobile/messages/{accountId}/drafts")
    fun initDraftMessage(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
    ): MessageDraftId = initDraftMessage(db, user as AuthenticatedUser, clock, accountId)

    private fun initDraftMessage(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
    ): MessageDraftId {
        return db.connect { dbc ->
                requireMessageAccountAccess(dbc, user, clock, accountId)
                dbc.transaction { it.initDraft(accountId, clock.now()) }
            }
            .also { messageDraftId ->
                Audit.MessagingCreateDraft.log(
                    targetId = AuditId(accountId),
                    objectId = AuditId(messageDraftId),
                )
            }
    }

    @PutMapping("/employee/messages/{accountId}/drafts/{draftId}")
    fun updateDraftMessage(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @PathVariable draftId: MessageDraftId,
        @RequestBody content: UpdatableDraftContent,
    ) = updateDraftMessage(db, user as AuthenticatedUser, clock, accountId, draftId, content)

    @PutMapping("/employee-mobile/messages/{accountId}/drafts/{draftId}")
    fun updateDraftMessage(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @PathVariable draftId: MessageDraftId,
        @RequestBody content: UpdatableDraftContent,
    ) = updateDraftMessage(db, user as AuthenticatedUser, clock, accountId, draftId, content)

    private fun updateDraftMessage(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @PathVariable draftId: MessageDraftId,
        @RequestBody content: UpdatableDraftContent,
    ) {
        return db.connect { dbc ->
                requireMessageAccountAccess(dbc, user, clock, accountId)
                dbc.transaction { it.updateDraft(accountId, draftId, content, clock.now()) }
            }
            .also {
                Audit.MessagingUpdateDraft.log(
                    targetId = AuditId(accountId),
                    objectId = AuditId(draftId),
                )
            }
    }

    @DeleteMapping("/employee/messages/{accountId}/drafts/{draftId}")
    fun deleteDraftMessage(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @PathVariable draftId: MessageDraftId,
    ) = deleteDraftMessage(db, user as AuthenticatedUser, clock, accountId, draftId)

    @DeleteMapping("/employee-mobile/messages/{accountId}/drafts/{draftId}")
    fun deleteDraftMessage(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @PathVariable draftId: MessageDraftId,
    ) = deleteDraftMessage(db, user as AuthenticatedUser, clock, accountId, draftId)

    private fun deleteDraftMessage(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @PathVariable draftId: MessageDraftId,
    ) {
        return db.connect { dbc ->
                requireMessageAccountAccess(dbc, user, clock, accountId)
                dbc.transaction { tx -> tx.deleteDraft(accountId, draftId) }
            }
            .also {
                Audit.MessagingDeleteDraft.log(
                    targetId = AuditId(accountId),
                    objectId = AuditId(draftId),
                )
            }
    }

    @PostMapping("/employee/messages/{accountId}/{messageId}/reply")
    fun replyToThread(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @PathVariable messageId: MessageId,
        @RequestBody body: ReplyToMessageBody,
    ): MessageService.ThreadReply =
        replyToThread(db, user as AuthenticatedUser, clock, accountId, messageId, body)

    @PostMapping("/employee-mobile/messages/{accountId}/{messageId}/reply")
    fun replyToThread(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @PathVariable messageId: MessageId,
        @RequestBody body: ReplyToMessageBody,
    ): MessageService.ThreadReply =
        replyToThread(db, user as AuthenticatedUser, clock, accountId, messageId, body)

    private fun replyToThread(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @PathVariable messageId: MessageId,
        @RequestBody body: ReplyToMessageBody,
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
                    serviceWorkerAccountName = featureConfig.serviceWorkerMessageAccountName,
                    financeAccountName = featureConfig.financeMessageAccountName,
                )
            }
            .also {
                Audit.MessagingReplyToMessageWrite.log(
                    targetId = AuditId(listOf(accountId, messageId)),
                    objectId = AuditId(listOf(it.threadId, it.message.id)),
                )
            }
    }

    @PutMapping("/employee/messages/{accountId}/threads/{threadId}/read")
    fun markThreadRead(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @PathVariable threadId: MessageThreadId,
    ) = markThreadRead(db, user as AuthenticatedUser, clock, accountId, threadId)

    @PutMapping("/employee-mobile/messages/{accountId}/threads/{threadId}/read")
    fun markThreadRead(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @PathVariable threadId: MessageThreadId,
    ) = markThreadRead(db, user as AuthenticatedUser, clock, accountId, threadId)

    private fun markThreadRead(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @PathVariable threadId: MessageThreadId,
    ) {
        db.connect { dbc ->
            requireMessageAccountAccess(dbc, user, clock, accountId)
            dbc.transaction { it.markThreadRead(clock.now(), accountId, threadId) }
        }
        Audit.MessagingMarkMessagesReadWrite.log(targetId = AuditId(listOf(accountId, threadId)))
    }

    @PutMapping("/employee/messages/{accountId}/threads/{threadId}/last-received-message/unread")
    fun markLastReceivedMessageInThreadUnread(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @PathVariable threadId: MessageThreadId,
    ) {
        db.connect { dbc ->
                requireMessageAccountAccess(dbc, user, clock, accountId)
                dbc.transaction { it.markLastReceivedMessageUnread(accountId, threadId) }
                    .also { updated ->
                        if (updated == 0) {
                            throw NotFound("No message to mark unread")
                        }
                    }
            }
            .also {
                Audit.MessagingMarkMessagesUnreadWrite.log(
                    targetId = AuditId(listOf(accountId, threadId))
                )
            }
    }

    @PutMapping("/employee/messages/{accountId}/threads/{threadId}/archive")
    fun archiveThread(
        db: Database,
        user: AuthenticatedUser.Employee,
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

    @PutMapping("/employee/messages/{accountId}/threads/{threadId}/move-to-folder/{folderId}")
    fun moveThreadToFolder(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable accountId: MessageAccountId,
        @PathVariable threadId: MessageThreadId,
        @PathVariable folderId: MessageThreadFolderId,
    ) {
        db.connect { dbc ->
            requireMessageAccountAccess(dbc, user, clock, accountId)
            dbc.transaction { it.moveThreadToFolder(accountId, threadId, folderId) }
        }
        Audit.MessagingChangeFolder.log(targetId = AuditId(listOf(accountId, threadId, folderId)))
    }

    @GetMapping("/employee/messages/selectable-recipients")
    fun getSelectableRecipients(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): List<SelectableRecipientsResponse> =
        getSelectableRecipients(db, user as AuthenticatedUser, clock)

    @GetMapping("/employee-mobile/messages/selectable-recipients")
    fun getSelectableRecipients(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
    ): List<SelectableRecipientsResponse> =
        getSelectableRecipients(db, user as AuthenticatedUser, clock)

    private fun getSelectableRecipients(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
    ): List<SelectableRecipientsResponse> {
        return db.connect { dbc ->
                dbc.read {
                    val filter =
                        accessControl.requireAuthorizationFilter(
                            it,
                            user,
                            clock,
                            Action.MessageAccount.ACCESS,
                        )
                    it.getSelectableRecipients(filter, clock.today())
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
        accountId: MessageAccountId,
    ) {
        db.read {
                val filter =
                    accessControl.requireAuthorizationFilter(
                        it,
                        user,
                        clock,
                        Action.MessageAccount.ACCESS,
                    )
                it.getEmployeeMessageAccountIds(filter)
            }
            .find { it == accountId } ?: throw Forbidden("Message account not found for user")
    }
}
