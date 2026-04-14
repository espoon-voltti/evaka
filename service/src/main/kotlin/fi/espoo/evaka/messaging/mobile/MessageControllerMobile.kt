// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging.mobile

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.messaging.MessageService
import fi.espoo.evaka.messaging.getCitizenMessageAccount
import fi.espoo.evaka.messaging.getMessageThread
import fi.espoo.evaka.messaging.getThreads
import fi.espoo.evaka.messaging.getUnreadMessagesCountForAccount
import fi.espoo.evaka.messaging.markThreadRead
import fi.espoo.evaka.messaging.messageAttachmentsAllowedForCitizen
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.MessageThreadId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/citizen-mobile/messages")
class MessageControllerMobile(
    private val featureConfig: FeatureConfig,
    private val messageService: MessageService,
) {

    data class ReplyBody(
        val content: String,
        val recipientAccountIds: Set<MessageAccountId>,
    )

    @GetMapping("/my-account/v1")
    fun getMyAccount(
        db: Database,
        user: AuthenticatedUser.CitizenMobile,
        clock: EvakaClock,
    ): MobileMyAccount {
        return db.connect { dbc ->
                dbc.read { tx ->
                    MobileMyAccount(
                        accountId = tx.getCitizenMessageAccount(user.id),
                        messageAttachmentsAllowed =
                            tx.messageAttachmentsAllowedForCitizen(user.id, clock.today()),
                    )
                }
            }
            .also { Audit.CitizenMobileMessagingMyAccount.log(targetId = AuditId(it.accountId)) }
    }

    @GetMapping("/unread-count/v1")
    fun getUnreadCount(
        db: Database,
        user: AuthenticatedUser.CitizenMobile,
        clock: EvakaClock,
    ): Int {
        return db.connect { dbc ->
                dbc.read { tx ->
                    val accountId = tx.getCitizenMessageAccount(user.id)
                    tx.getUnreadMessagesCountForAccount(accountId)
                }
            }
            .also { Audit.CitizenMobileMessagingUnreadCount.log(meta = mapOf("count" to it)) }
    }

    @GetMapping("/threads/v1")
    fun getThreads(
        db: Database,
        user: AuthenticatedUser.CitizenMobile,
        clock: EvakaClock,
        @RequestParam pageSize: Int,
        @RequestParam page: Int,
    ): MobileThreadListResponse {
        return db.connect { dbc ->
                val accountId = dbc.read { tx -> tx.getCitizenMessageAccount(user.id) }
                val threads =
                    dbc.read { tx ->
                        tx.getThreads(
                            accountId,
                            pageSize,
                            page,
                            featureConfig.municipalMessageAccountName,
                            featureConfig.serviceWorkerMessageAccountName,
                            featureConfig.financeMessageAccountName,
                        )
                    }
                accountId to threads
            }
            .let { (accountId, threads) ->
                val data =
                    threads.data.map { thread ->
                        val last = thread.messages.last()
                        MobileThreadListItem(
                            id = thread.id,
                            title = thread.title,
                            lastMessagePreview =
                                last.content.take(100).replace("\n", " "),
                            lastMessageAt = last.sentAt,
                            unreadCount =
                                thread.messages.count { msg ->
                                    msg.readAt == null && msg.sender.id != accountId
                                },
                            senderName = last.sender.name,
                        )
                    }
                Audit.CitizenMobileMessagingThreadsList.log(
                    targetId = AuditId(accountId),
                    meta = mapOf("total" to threads.total),
                )
                MobileThreadListResponse(data = data, hasMore = threads.pages > page)
            }
    }

    @GetMapping("/thread/{threadId}/v1")
    fun getThread(
        db: Database,
        user: AuthenticatedUser.CitizenMobile,
        clock: EvakaClock,
        @PathVariable threadId: MessageThreadId,
    ): MobileThread {
        return db.connect { dbc ->
                val accountId = dbc.read { tx -> tx.getCitizenMessageAccount(user.id) }
                val thread =
                    dbc.read { tx ->
                        tx.getMessageThread(
                            accountId,
                            threadId,
                            featureConfig.municipalMessageAccountName,
                            featureConfig.serviceWorkerMessageAccountName,
                            featureConfig.financeMessageAccountName,
                        )
                    }
                accountId to thread
            }
            .let { (accountId, thread) ->
                Audit.CitizenMobileMessagingThreadRead.log(
                    targetId = AuditId(listOf(accountId, threadId))
                )
                MobileThread(
                    id = thread.id,
                    title = thread.title,
                    messages =
                        thread.messages.map { msg ->
                            MobileMessage(
                                id = msg.id,
                                senderName = msg.sender.name,
                                senderAccountId = msg.sender.id,
                                content = msg.content,
                                sentAt = msg.sentAt,
                                readAt = msg.readAt,
                            )
                        },
                )
            }
    }

    @PostMapping("/thread/{threadId}/reply/v1")
    fun replyToThread(
        db: Database,
        user: AuthenticatedUser.CitizenMobile,
        clock: EvakaClock,
        @PathVariable threadId: MessageThreadId,
        @RequestBody body: ReplyBody,
    ): MessageService.ThreadReply {
        return db.connect { dbc ->
                val accountId = dbc.read { tx -> tx.getCitizenMessageAccount(user.id) }
                val reply =
                    messageService.replyToThread(
                        db = dbc,
                        now = clock.now(),
                        threadId = threadId,
                        senderAccount = accountId,
                        recipientAccountIds = body.recipientAccountIds,
                        content = body.content,
                        user = AuthenticatedUser.Citizen(user.id, user.authLevel),
                        municipalAccountName = featureConfig.municipalMessageAccountName,
                        serviceWorkerAccountName = featureConfig.serviceWorkerMessageAccountName,
                        financeAccountName = featureConfig.financeMessageAccountName,
                    )
                accountId to reply
            }
            .let { (accountId, reply) ->
                Audit.CitizenMobileMessagingReply.log(
                    targetId = AuditId(listOf(accountId, threadId)),
                    objectId = AuditId(reply.message.id),
                )
                reply
            }
    }

    @PostMapping("/thread/{threadId}/mark-read/v1")
    fun markThreadRead(
        db: Database,
        user: AuthenticatedUser.CitizenMobile,
        clock: EvakaClock,
        @PathVariable threadId: MessageThreadId,
    ) {
        db.connect { dbc ->
                val accountId = dbc.read { tx -> tx.getCitizenMessageAccount(user.id) }
                dbc.transaction { tx -> tx.markThreadRead(clock.now(), accountId, threadId) }
                accountId
            }
            .also { accountId ->
                Audit.CitizenMobileMessagingMarkRead.log(
                    targetId = AuditId(listOf(accountId, threadId))
                )
            }
    }
}
