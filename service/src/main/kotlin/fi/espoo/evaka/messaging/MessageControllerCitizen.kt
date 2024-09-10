// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.attachment.AttachmentParent
import fi.espoo.evaka.attachment.associateOrphanAttachments
import fi.espoo.evaka.children.getChildrenByParent
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.MessageId
import fi.espoo.evaka.shared.MessageThreadId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

data class CitizenMessageBody(
    val recipients: Set<MessageAccountId>,
    val children: Set<ChildId>,
    val content: String,
    val title: String,
    val attachmentIds: List<AttachmentId>,
)

@RestController
@RequestMapping("/citizen/messages")
class MessageControllerCitizen(
    private val featureConfig: FeatureConfig,
    private val accessControl: AccessControl,
    private val messageService: MessageService,
) {

    data class MyAccountResponse(
        val accountId: MessageAccountId,
        val messageAttachmentsAllowed: Boolean,
    )

    @GetMapping("/my-account")
    fun getMyAccount(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
    ): MyAccountResponse {
        return db.connect { dbc ->
                dbc.read { tx ->
                    MyAccountResponse(
                        accountId = tx.getCitizenMessageAccount(user.id),
                        messageAttachmentsAllowed =
                            tx.messageAttachmentsAllowedForCitizen(user.id, clock.today()),
                    )
                }
            }
            .also { Audit.MessagingMyAccountsRead.log(targetId = AuditId(it.accountId)) }
    }

    @GetMapping("/unread-count")
    fun getUnreadMessages(db: Database, user: AuthenticatedUser.Citizen, clock: EvakaClock): Int {
        return db.connect { dbc ->
                dbc.read { tx ->
                    tx.getUnreadMessagesCounts(
                            accessControl.requireAuthorizationFilter(
                                tx,
                                user,
                                clock,
                                Action.MessageAccount.ACCESS,
                            )
                        )
                        .firstOrNull()
                        ?.unreadCount ?: 0
                }
            }
            .also { Audit.MessagingUnreadMessagesRead.log(meta = mapOf("count" to it)) }
    }

    @PutMapping("/threads/{threadId}/read")
    fun markThreadRead(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable threadId: MessageThreadId,
    ) {
        db.connect { dbc ->
                val accountId = dbc.read { it.getCitizenMessageAccount(user.id) }
                dbc.transaction { it.markThreadRead(clock, accountId, threadId) }
                accountId
            }
            .also { accountId ->
                Audit.MessagingMarkMessagesReadWrite.log(
                    targetId = AuditId(listOf(accountId, threadId))
                )
            }
    }

    @PutMapping("/threads/{threadId}/archive")
    fun archiveThread(
        db: Database,
        user: AuthenticatedUser.Citizen,
        @PathVariable threadId: MessageThreadId,
    ) {
        db.connect { dbc ->
                val accountId = dbc.read { it.getCitizenMessageAccount(user.id) }
                dbc.transaction { it.archiveThread(accountId, threadId) }
                accountId
            }
            .also { accountId ->
                Audit.MessagingArchiveMessageWrite.log(
                    targetId = AuditId(listOf(accountId, threadId))
                )
            }
    }

    @GetMapping("/received")
    fun getReceivedMessages(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @RequestParam page: Int,
    ): PagedCitizenMessageThreads {
        return db.connect { dbc ->
                val accountId = dbc.read { it.getCitizenMessageAccount(user.id) }
                val response =
                    dbc.read {
                            it.getThreads(
                                accountId,
                                pageSize = 10,
                                page,
                                featureConfig.municipalMessageAccountName,
                                featureConfig.serviceWorkerMessageAccountName,
                            )
                        }
                        .mapTo(::PagedCitizenMessageThreads) {
                            if (user.authLevel != CitizenAuthLevel.STRONG && it.sensitive) {
                                CitizenMessageThread.Redacted.fromMessageThread(accountId, it)
                            } else {
                                CitizenMessageThread.Regular.fromMessageThread(it)
                            }
                        }
                accountId to response
            }
            .let { (accountId, response) ->
                Audit.MessagingReceivedMessagesRead.log(
                    targetId = AuditId(accountId),
                    meta = mapOf("total" to response.total),
                )
                response
            }
    }

    data class GetReceiversResponse(
        val messageAccounts: Set<MessageAccount>,
        val childrenToMessageAccounts: Map<ChildId, List<MessageAccountId>>,
    )

    @GetMapping("/receivers")
    fun getReceivers(
        db: Database,
        user: AuthenticatedUser.Citizen,
        evakaClock: EvakaClock,
    ): GetReceiversResponse {
        return db.connect { dbc ->
                val accountId = dbc.read { it.getCitizenMessageAccount(user.id) }
                val accountsPerChild =
                    (dbc.read { it.getCitizenReceivers(evakaClock.today(), accountId) })
                val response =
                    GetReceiversResponse(
                        messageAccounts = accountsPerChild.values.flatten().toSet(),
                        childrenToMessageAccounts =
                            accountsPerChild.mapValues { entry -> entry.value.map { it.id } },
                    )
                accountId to response
            }
            .let { (accountId, response) ->
                Audit.MessagingCitizenFetchReceiversForAccount.log(
                    targetId = AuditId(accountId),
                    meta = mapOf("count" to response.messageAccounts.size),
                )
                response
            }
    }

    @PostMapping("/{messageId}/reply")
    fun replyToThread(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable messageId: MessageId,
        @RequestBody body: ReplyToMessageBody,
    ): MessageService.ThreadReply {
        return db.connect { dbc ->
                val accountId = dbc.read { it.getCitizenMessageAccount(user.id) }
                val response =
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
                    )
                accountId to response
            }
            .let { (accountId, response) ->
                Audit.MessagingReplyToMessageWrite.log(
                    targetId = AuditId(listOf(accountId, messageId))
                )
                response
            }
    }

    @PostMapping
    fun newMessage(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @RequestBody body: CitizenMessageBody,
    ): MessageThreadId {
        val now = clock.now()
        val today = now.toLocalDate()

        return db.connect { dbc ->
                val senderId = dbc.read { it.getCitizenMessageAccount(user.id) }
                val validRecipients =
                    dbc.read { it.getCitizenReceivers(today, senderId) }
                        .mapValues { entry -> entry.value.map { it.id }.toSet() }
                val allRecipientsValid =
                    body.recipients.all { recipient ->
                        body.children.any { child ->
                            validRecipients[child]?.contains(recipient) ?: false
                        }
                    }
                val selectedChildren =
                    dbc.read { it.getChildrenByParent(user.id, today) }
                        .filter { body.children.contains(it.id) }
                val selectedChildrenInSameUnit =
                    selectedChildren.asSequence().map { it.unit?.id }.toSet().size == 1
                if (allRecipientsValid && selectedChildrenInSameUnit) {
                    dbc.transaction { tx ->
                        val sentMessage =
                            messageService.sendMessageAsCitizen(
                                tx,
                                now,
                                senderId,
                                NewMessageStub(
                                    title = body.title,
                                    content = body.content,
                                    urgent = false,
                                    sensitive = false,
                                ),
                                body.recipients,
                                body.children,
                            )

                        if (body.attachmentIds.isNotEmpty()) {
                            if (!tx.messageAttachmentsAllowedForCitizen(user.id, today)) {
                                throw Forbidden("Message attachments not allowed")
                            }
                            tx.associateOrphanAttachments(
                                user.evakaUserId,
                                AttachmentParent.MessageContent(sentMessage.contentId),
                                body.attachmentIds,
                            )
                        }

                        senderId to sentMessage.threadId
                    }
                } else {
                    throw BadRequest("Invalid recipients")
                }
            }
            .let { (senderId, messageThreadId) ->
                Audit.MessagingCitizenSendMessage.log(
                    targetId = AuditId(listOf(senderId, messageThreadId)),
                    meta = mapOf("attachments" to body.attachmentIds),
                )
                messageThreadId
            }
    }
}
