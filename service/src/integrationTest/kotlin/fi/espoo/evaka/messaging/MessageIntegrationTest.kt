// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.messaging.message.MessageAccount
import fi.espoo.evaka.messaging.message.MessageController
import fi.espoo.evaka.messaging.message.MessageControllerCitizen
import fi.espoo.evaka.messaging.message.MessageThread
import fi.espoo.evaka.messaging.message.MessageType
import fi.espoo.evaka.messaging.message.getMessageAccountsForUser
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.shared.dev.insertTestPerson
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class MessageIntegrationTest : FullApplicationTest() {

    private val person1Id: UUID = UUID.randomUUID()
    private val person2Id: UUID = UUID.randomUUID()
    private val employee1Id: UUID = UUID.randomUUID()
    private val employee2Id: UUID = UUID.randomUUID()

    @BeforeEach
    internal fun setUp() {
        db.transaction {
            it.insertTestPerson(DevPerson(id = person1Id, firstName = "Firstname", lastName = "Person"))

            it.insertTestPerson(DevPerson(id = person2Id, firstName = "Firstname", lastName = "Person Two"))
            it.execute("INSERT INTO message_account (person_id) VALUES ('$person1Id'), ('$person2Id')")

            it.insertTestEmployee(DevEmployee(id = employee1Id, firstName = "Firstname", lastName = "Employee"))
            it.insertTestEmployee(DevEmployee(id = employee2Id, firstName = "Firstname", lastName = "Employee Two"))
            it.execute("INSERT INTO message_account (employee_id) VALUES ('$employee1Id'), ('$employee2Id')")
        }
    }

    @AfterEach
    internal fun tearDown() {
        db.transaction { it.resetDatabase() }
    }

    @Test
    fun `a thread is created, accessed and replied to by several participants`() {
        // given
        val employee1 = AuthenticatedUser.Employee(id = employee1Id, roles = setOf(UserRole.UNIT_SUPERVISOR))
        val person1 = AuthenticatedUser.Citizen(id = person1Id)
        val person2 = AuthenticatedUser.Citizen(id = person2Id)
        val (employee1Account, _, person1Account, person2Account) = db.read {
            listOf(
                it.getMessageAccountsForUser(employee1).first(),
                it.getMessageAccountsForUser(AuthenticatedUser.Employee(id = employee2Id, roles = setOf())).first(),
                it.getMessageAccountsForUser(person1).first(),
                it.getMessageAccountsForUser(person2).first()
            )
        }

        // when a message thread is created
        postNewThread(
            title = "Juhannus",
            message = "Juhannus tulee pian",
            messageType = MessageType.MESSAGE,
            sender = employee1Account.id,
            recipients = setOf(person1Account.id, person2Account.id),
            user = employee1,
        )

        // then sender does not see it in received messages
        assertEquals(
            listOf<MessageThread>(),
            getMessageThreads(employee1Account, employee1)
        )

        // then recipient can see it in received messages
        val threadWithOneReply = getMessageThreads(person1Account, person1)[0]
        assertEquals("Juhannus", threadWithOneReply.title)
        assertEquals(MessageType.MESSAGE, threadWithOneReply.type)
        assertEquals(
            listOf(Pair(employee1Account.id, "Juhannus tulee pian")),
            threadWithOneReply.toSenderContentPairs()
        )

        // when
        replyAsCitizen(
            person1,
            threadWithOneReply.messages[0].id,
            setOf(employee1Account.id, person2Account.id),
            "No niinpä näyttää tulevan"
        )

        // then recipients see the same data
        val person2Threads = getMessageThreads(person2Account, person2)
        assertEquals(
            getMessageThreads(person1Account, person1),
            person2Threads
        )
        assertEquals(
            getMessageThreads(employee1Account, employee1),
            person2Threads
        )

        // then thread has both messages in correct order
        assertEquals(1, person2Threads.size)
        val person2Thread = person2Threads[0]
        assertEquals("Juhannus", person2Thread.title)
        assertEquals(
            listOf(
                Pair(employee1Account.id, "Juhannus tulee pian"),
                Pair(person1Account.id, "No niinpä näyttää tulevan")
            ),
            person2Thread.toSenderContentPairs()
        )

        // when person one replies to the employee only
        replyAsCitizen(
            person1,
            person2Thread.messages.last().id,
            setOf(employee1Account.id),
            "person 2 does not see this"
        )

        // then person one and employee see the new message
        val threadContentWithTwoReplies = listOf(
            Pair(employee1Account.id, "Juhannus tulee pian"),
            Pair(person1Account.id, "No niinpä näyttää tulevan"),
            Pair(person1Account.id, "person 2 does not see this"),
        )
        assertEquals(
            threadContentWithTwoReplies,
            getMessageThreads(person1Account, person1)[0].toSenderContentPairs()
        )
        assertEquals(
            threadContentWithTwoReplies,
            getMessageThreads(employee1Account, employee1)[0].toSenderContentPairs()
        )

        // then person two does not see the message
        assertEquals(
            listOf(
                Pair(employee1Account.id, "Juhannus tulee pian"),
                Pair(person1Account.id, "No niinpä näyttää tulevan")
            ),
            getMessageThreads(person2Account, person2)[0].toSenderContentPairs()
        )

        // when author replies to person two
        replyAsEmployee(
            user = employee1,
            account = employee1Account,
            messageId = threadWithOneReply.messages.last().id,
            recipientAccountIds = setOf(person2Account.id),
            content = "person 1 does not see this"
        )

        // then person two sees that
        assertEquals(
            listOf(
                Pair(employee1Account.id, "Juhannus tulee pian"),
                Pair(person1Account.id, "No niinpä näyttää tulevan"),
                Pair(employee1Account.id, "person 1 does not see this"),
            ),
            getMessageThreads(person2Account, person2)[0].toSenderContentPairs()
        )

        // then person one does not see that
        assertEquals(
            threadContentWithTwoReplies,
            getMessageThreads(person1Account, person1)[0].toSenderContentPairs()
        )

        // then employee sees all the messages
        assertEquals(
            listOf(
                Pair(employee1Account.id, "Juhannus tulee pian"),
                Pair(person1Account.id, "No niinpä näyttää tulevan"),
                Pair(person1Account.id, "person 2 does not see this"),
                Pair(employee1Account.id, "person 1 does not see this"),
            ),
            getMessageThreads(employee1Account, employee1)[0].toSenderContentPairs()
        )
    }

    @Test
    fun `a bulletin cannot be replied to by the recipients`() {
        // given
        val employee1 = AuthenticatedUser.Employee(id = employee1Id, roles = setOf(UserRole.UNIT_SUPERVISOR))
        val person1 = AuthenticatedUser.Citizen(id = person1Id)
        val (employee1Account, person1Account) = db.read {
            listOf(
                it.getMessageAccountsForUser(employee1).first(),
                it.getMessageAccountsForUser(person1).first(),
            )
        }

        // when a bulletin thread is created
        postNewThread(
            title = "Tiedote",
            message = "Juhannus tulee pian",
            messageType = MessageType.BULLETIN,
            sender = employee1Account.id,
            recipients = setOf(person1Account.id),
            user = employee1,
        )

        // then the recipient can see it
        val thread = getMessageThreads(person1Account, person1).first()
        assertEquals("Tiedote", thread.title)
        assertEquals(MessageType.BULLETIN, thread.type)
        assertEquals(listOf(Pair(employee1Account.id, "Juhannus tulee pian")), thread.toSenderContentPairs())

        // when the recipient tries to reply to the bulletin, it is denied
        assertEquals(
            403,
            replyAsCitizen(
                user = person1,
                messageId = thread.messages.first().id,
                recipientAccountIds = setOf(thread.messages.first().senderId),
                content = "Kiitos tiedosta"
            ).second.statusCode
        )

        // when the author himself replies to the bulletin, it succeeds
        assertEquals(
            200,
            replyAsEmployee(
                account = employee1Account,
                user = employee1,
                messageId = thread.messages.last().id,
                recipientAccountIds = setOf(person1Account.id),
                content = "Nauttikaa siitä"
            ).second.statusCode
        )

        // then the recipient can see it
        assertEquals(
            listOf(
                Pair(employee1Account.id, "Juhannus tulee pian"),
                Pair(employee1Account.id, "Nauttikaa siitä")
            ),
            getMessageThreads(person1Account, person1).first().toSenderContentPairs()
        )
    }

    private fun postNewThread(
        title: String,
        message: String,
        messageType: MessageType,
        sender: UUID,
        recipients: Set<UUID>,
        user: AuthenticatedUser.Employee,
    ) = http.post("/messages")
        .jsonBody(
            objectMapper.writeValueAsString(
                MessageController.PostMessageBody(
                    title,
                    message,
                    messageType,
                    sender,
                    recipientAccountIds = recipients
                )
            )
        )
        .asUser(user)
        .response()

    private fun replyAsCitizen(
        user: AuthenticatedUser.Citizen,
        messageId: UUID,
        recipientAccountIds: Set<UUID>,
        content: String,
    ) =
        http.post(
            "/citizen/messages/$messageId/reply",
        )
            .jsonBody(
                objectMapper.writeValueAsString(
                    MessageControllerCitizen.ReplyToMessageBody(
                        content = content,
                        recipientAccountIds = recipientAccountIds
                    )
                )
            )
            .asUser(user)
            .response()

    private fun replyAsEmployee(
        user: AuthenticatedUser.Employee,
        account: MessageAccount,
        messageId: UUID,
        recipientAccountIds: Set<UUID>,
        content: String,
    ) =
        http.post(
            "/messages/$messageId/reply",
        )
            .jsonBody(
                objectMapper.writeValueAsString(
                    MessageController.ReplyToMessageBody(
                        senderAccountId = account.id,
                        content = content,
                        recipientAccountIds = recipientAccountIds
                    )
                )
            )
            .asUser(user)
            .response()

    private fun getMessageThreads(account: MessageAccount, user: AuthenticatedUser): List<MessageThread> = http.get(
        "${if (user.isEndUser) "/citizen" else ""}/messages/${account.id}/received",
        listOf("page" to 1, "pageSize" to 100)
    )
        .asUser(user)
        .responseObject<Paged<MessageThread>>(objectMapper).third.get().data
}

fun MessageThread.toSenderContentPairs(): List<Pair<UUID, String>> = this.messages.map { Pair(it.senderId, it.content) }
