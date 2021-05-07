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
        val (_, res, _) = http.post("/messages")
            .jsonBody(
                objectMapper.writeValueAsString(
                    MessageController.PostMessageBody(
                        "Juhannus",
                        "Juhannus tulee pian",
                        MessageType.MESSAGE,
                        employee1Account.id,
                        recipientAccountIds = setOf(person1Account.id, person2Account.id)
                    )
                )
            )
            .asUser(employee1)
            .response()

        assertEquals(200, res.statusCode)

        // then sender does not see it in received messages
        assertEquals(
            listOf<MessageThread>(),
            getMessages(employee1Account, employee1).third.get().data
        )

        // then recipient can see it in received messages
        val (_, _, person1Data) = getMessages(person1Account, person1)

        val person1Thread = person1Data.get().data.first()
        assertEquals("Juhannus", person1Thread.title)
        assertEquals(MessageType.MESSAGE, person1Thread.type)
        assertEquals(
            listOf(Pair(employee1Account.id, "Juhannus tulee pian")),
            person1Thread.messages.map { Pair(it.senderId, it.content) }
        )

        // when
        replyAsCitizen(
            person1,
            person1Thread.messages[0].id,
            setOf(employee1Account.id, person2Account.id),
            "No niinpä näyttää tulevan"
        )

        val (_, _, personTwoData) = getMessages(person2Account, person2)

        // then recipients see the same data
        val person2Data = personTwoData.get().data
        assertEquals(
            getMessages(person1Account, person1).third.get().data,
            person2Data
        )
        assertEquals(
            getMessages(employee1Account, employee1).third.get().data,
            person2Data
        )

        // then thread has both messages in correct order
        assertEquals(1, person2Data.size)
        val person2Thread = person2Data[0]
        assertEquals("Juhannus", person2Thread.title)
        assertEquals(
            listOf(
                Pair(employee1Account.id, "Juhannus tulee pian"),
                Pair(person1Account.id, "No niinpä näyttää tulevan")
            ),
            person2Thread.messages.map { Pair(it.senderId, it.content) }
        )
    }

    private fun replyAsCitizen(
        user: AuthenticatedUser.Citizen,
        messageId: UUID,
        recipientAccountIds: Set<UUID>,
        content: String,
    ) {
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
    }

    private fun getMessages(account: MessageAccount, user: AuthenticatedUser) = http.get(
        "${if (user.isEndUser) "/citizen" else ""}/messages/${account.id}/received",
        listOf("page" to 1, "pageSize" to 100)
    )
        .asUser(user)
        .responseObject<Paged<MessageThread>>(objectMapper)
}
