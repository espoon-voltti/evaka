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
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class UnreadMessagesResponse(val count: Int)

@RestController
@RequestMapping("/messages")
class MessageController {

    @GetMapping("/my-accounts")
    fun getAccountsByUser(db: Database.Connection, user: AuthenticatedUser): Set<MessageAccount> {
        Audit.MessagingMyAccountsRead.log()
        return db.read { it.getMessageAccountsForUser(user) }
    }

    @GetMapping("/{accountId}/received")
    fun getReceivedMessages(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable accountId: UUID,
        @RequestParam pageSize: Int,
        @RequestParam page: Int,
    ): Paged<MessageThread> {
        Audit.MessagingReceivedMessagesRead.log(targetId = accountId)
        if (!db.read { it.getMessageAccountsForUser(user) }.map { it.id }.contains(accountId))
            throw Forbidden("User is not authorized to access the account")
        return db.read { it.getMessagesReceivedByAccount(accountId, pageSize, page) }
    }

    @GetMapping("/unread")
    fun getUnreadMessages(
        db: Database.Connection,
        user: AuthenticatedUser
    ): UnreadMessagesResponse {
        Audit.MessagingUnreadMessagesRead.log()
        val accountIds = db.read { it.getMessageAccountsForUser(user) }.map { it.id }
        val count = if (accountIds.isEmpty()) 0 else db.read { it.getUnreadMessagesCount(accountIds.toSet()) }
        return UnreadMessagesResponse(count)
    }

    data class PostMessageBody(
        val title: String,
        val content: String,
        val type: MessageType,
        val senderAccountId: UUID,
        val recipientAccountIds: Set<UUID>
    )

    @PostMapping
    fun createMessage(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestBody body: PostMessageBody
    ): UUID {
        Audit.MessagingNewMessageWrite.log(targetId = body.senderAccountId)
        user.requireOneOfRoles(UserRole.UNIT_SUPERVISOR, UserRole.STAFF, UserRole.SPECIAL_EDUCATION_TEACHER)
        val sender = db.read { it.getMessageAccountsForUser(user) }.find { it.id == body.senderAccountId }
            ?: throw Forbidden("User is not authorized to access the account")

        // TODO recipient account authorization

        // TODO split messages to threads by "household"

        return db.transaction {
            it.createMessageThread(
                title = body.title,
                content = body.content,
                sender = sender,
                type = body.type,
                recipientAccountIds = body.recipientAccountIds
            )
        }
    }

    val supervisorAccount: MessageAccount = MessageAccount(id = UUID.randomUUID(), name = "Esimies Ella")
    val aatusMomAccount: MessageAccount = MessageAccount(id = UUID.randomUUID(), name = "Aatun äiti")
    val aatusDadAccount: MessageAccount = MessageAccount(id = UUID.randomUUID(), name = "Aatun isä")

    @GetMapping
    fun getThreadsMock(): Paged<MessageThread> {
        val aatuMeritahtiMessage = Message(
            id = UUID.randomUUID(),
            senderId = supervisorAccount.id,
            senderName = supervisorAccount.name,
            content = "Hei Aatun äiti ja isä! Aatu on kasvanut kovaa vauhtia ja nyt on tullut aika siirtyä seuraavaan ikäryhmään. Aatu aloittaa Meritähdissä kesäkuussa.",
            sentAt = HelsinkiDateTime.now().minusDays(3)
        )
        val aatusDadsResponse = Message(
            id = UUID.randomUUID(),
            senderId = aatusDadAccount.id,
            senderName = aatusDadAccount.name,
            content = "Kuulostaa hyvältä, kiitos tiedosta.",
            sentAt = HelsinkiDateTime.now().minusDays(2)
        )
        val aatusMomsResponse = Message(
            id = UUID.randomUUID(),
            senderId = aatusMomAccount.id,
            senderName = aatusMomAccount.name,
            content = "Joo, se käy oikein mainiosti :-) Hyvää kevään jatkoa!",
            sentAt = HelsinkiDateTime.now().minusDays(2).plusHours(2)
        )
        val bulletin = Message(
            id = UUID.randomUUID(),
            senderId = supervisorAccount.id,
            senderName = supervisorAccount.name,
            content = """Hei vanhemmat!

Kevät lähestyy jo kovaa vauhtia. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas dapibus blandit porta. Aliquam pellentesque ex sed magna efficitur convallis. Sed elementum felis quis placerat rutrum.

Suspendisse pretium pulvinar ligula, nec pellentesque tortor eleifend sed. Fusce placerat luctus erat a lobortis. Aenean et finibus purus, a rhoncus odio. Donec at velit laoreet, convallis ex vel, iaculis lorem. Vestibulum posuere dapibus est, at venenatis nunc consequat pharetra. Nullam sollicitudin orci sed accumsan consectetur. Sed urna augue, sollicitudin at ex sed, hendrerit malesuada enim.

Nyt kun viimeiset lumet sulavat ja on märät kelit, niin huolehdittehan, että lapsen kuravaatteet ovat päiväkodilla, ja että lapsella on lokerossa tarpeeksi vaihtovaatteita niin sisälle kuin ulkoleikkeihinkin.

Aurinkoisia päiviä!

T. Ella Esimies""",
            sentAt = HelsinkiDateTime.now().minusWeeks(1)
        )

        return Paged(
            total = 2, pages = 1,
            data = listOf(
                MessageThread(
                    id = UUID.randomUUID(),
                    type = MessageType.MESSAGE,
                    title = "Aatun siirtyminen Meritähtiin",
                    messages = listOf(
                        aatuMeritahtiMessage,
                        aatusDadsResponse,
                        aatusMomsResponse
                    )
                ),
                MessageThread(
                    id = UUID.randomUUID(),
                    type = MessageType.BULLETIN,
                    title = "Kevät",
                    messages = listOf(bulletin)
                )
            )
        )
    }
}
