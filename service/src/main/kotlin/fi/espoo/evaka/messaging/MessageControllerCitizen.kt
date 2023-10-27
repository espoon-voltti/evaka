// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.MessageId
import fi.espoo.evaka.shared.MessageThreadId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
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
    val recipients: Set<MessageAccountId>,
    val children: Set<ChildId>,
    val content: String,
    val title: String
)

@RestController
@RequestMapping("/citizen/messages")
class MessageControllerCitizen(
    private val featureConfig: FeatureConfig,
    private val messageService: MessageService
) {

    @GetMapping("/my-account")
    fun getMyAccount(db: Database, user: AuthenticatedUser.Citizen): MessageAccountId {
        return db.connect { dbc -> dbc.read { it.getCitizenMessageAccount(user.id) } }
            .also { Audit.MessagingMyAccountsRead.log(targetId = user.id, objectId = it) }
    }

    @GetMapping("/unread-count")
    fun getUnreadMessages(db: Database, user: AuthenticatedUser.Citizen, clock: EvakaClock): Int {
        return db.connect { dbc ->
                dbc.read { tx ->
                    val accountId = tx.getCitizenMessageAccount(user.id)
                    val counts = tx.getUnreadMessagesCounts(clock.now(), setOf(accountId))
                    counts.firstOrNull()?.unreadCount ?: 0
                }
            }
            .also {
                Audit.MessagingUnreadMessagesRead.log(
                    targetId = user.id,
                    meta = mapOf("count" to it)
                )
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

    @PutMapping("/threads/{threadId}/archive")
    fun archiveThread(
        db: Database,
        user: AuthenticatedUser.Citizen,
        @PathVariable("threadId") threadId: MessageThreadId
    ) {
        db.connect { dbc ->
            val accountId = dbc.read { it.getCitizenMessageAccount(user.id) }
            dbc.transaction { it.archiveThread(accountId, threadId) }
        }
        Audit.MessagingArchiveMessageWrite.log(targetId = threadId)
    }

    @GetMapping("/received")
    fun getReceivedMessages(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @RequestParam pageSize: Int,
        @RequestParam page: Int
    ): PagedCitizenMessageThreads {
        return db.connect { dbc ->
                val accountId = dbc.read { it.getCitizenMessageAccount(user.id) }
                dbc.read {
                        it.getThreads(
                            accountId,
                            pageSize,
                            page,
                            featureConfig.municipalMessageAccountName,
                            featureConfig.serviceWorkerMessageAccountName
                        )
                    }
                    .mapTo(::PagedCitizenMessageThreads) {
                        if (user.authLevel != CitizenAuthLevel.STRONG && it.sensitive) {
                            CitizenMessageThread.Redacted.fromMessageThread(accountId, it)
                        } else {
                            CitizenMessageThread.Regular.fromMessageThread(it)
                        }
                    }
            }
            .also { Audit.MessagingReceivedMessagesRead.log(meta = mapOf("total" to it.total)) }
    }

    data class GetReceiversResponse(
        val messageAccounts: Set<MessageAccount>,
        val childrenToMessageAccounts: Map<ChildId, List<MessageAccountId>>
    )

    @GetMapping("/receivers")
    fun getReceivers(
        db: Database,
        user: AuthenticatedUser.Citizen,
        evakaClock: EvakaClock
    ): GetReceiversResponse {
        return db.connect { dbc ->
                val accountId = dbc.read { it.getCitizenMessageAccount(user.id) }
                val accountsPerChild =
                    (dbc.read { it.getCitizenReceivers(evakaClock.today(), accountId) })
                GetReceiversResponse(
                    messageAccounts = accountsPerChild.values.flatten().toSet(),
                    childrenToMessageAccounts =
                        accountsPerChild.mapValues { entry -> entry.value.map { it.id } }
                )
            }
            .also {
                Audit.MessagingCitizenFetchReceiversForAccount.log(
                    meta = mapOf("count" to it.messageAccounts)
                )
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
                    content = body.content,
                    user = user,
                    municipalAccountName = featureConfig.municipalMessageAccountName,
                    serviceWorkerAccountName = featureConfig.serviceWorkerMessageAccountName
                )
            }
            .also { Audit.MessagingReplyToMessageWrite.log(targetId = messageId) }
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
                val validReceivers =
                    dbc.read { it.getCitizenReceivers(today, senderId) }
                        .mapValues { entry -> entry.value.map { it.id }.toSet() }
                val allReceiversValid =
                    body.recipients.all { recipient ->
                        body.children.all { child ->
                            validReceivers[child]?.contains(recipient) ?: false
                        }
                    }
                if (allReceiversValid) {
                    dbc.transaction { tx ->
                        messageService.sendMessageAsCitizen(
                            tx,
                            now,
                            senderId,
                            NewMessageStub(
                                title = body.title,
                                content = body.content,
                                urgent = false,
                                sensitive = false
                            ),
                            body.recipients,
                            body.children
                        )
                    }
                } else {
                    throw Forbidden("Permission denied.")
                }
            }
            .also { messageThreadId ->
                Audit.MessagingCitizenSendMessage.log(targetId = messageThreadId)
            }
    }
}
