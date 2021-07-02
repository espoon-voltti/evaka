// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.invoicing.domain.PersonData
import fi.espoo.evaka.messaging.message.MessageController
import fi.espoo.evaka.messaging.message.MessageControllerCitizen
import fi.espoo.evaka.messaging.message.MessageThread
import fi.espoo.evaka.messaging.message.MessageType
import fi.espoo.evaka.messaging.message.SentMessage
import fi.espoo.evaka.messaging.message.createPersonMessageAccount
import fi.espoo.evaka.messaging.message.getCitizenMessageAccount
import fi.espoo.evaka.messaging.message.getEmployeeMessageAccounts
import fi.espoo.evaka.messaging.message.getMessagesSentByAccount
import fi.espoo.evaka.messaging.message.upsertEmployeeMessageAccount
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevChild
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insertTestCareArea
import fi.espoo.evaka.shared.dev.insertTestChild
import fi.espoo.evaka.shared.dev.insertTestDaycare
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.shared.dev.insertTestPerson
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testAdult_3
import fi.espoo.evaka.testAdult_4
import fi.espoo.evaka.testAreaId
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testChild_4
import fi.espoo.evaka.testDaycare
import org.jdbi.v3.core.kotlin.mapTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class MessageIntegrationTest : FullApplicationTest() {

    private val person1Id: UUID = testAdult_1.id
    private val person2Id: UUID = testAdult_2.id
    private val person3Id: UUID = testAdult_3.id
    private val person4Id: UUID = testAdult_4.id
    private val employee1Id: UUID = UUID.randomUUID()
    private val employee1 = AuthenticatedUser.Employee(id = employee1Id, roles = setOf(UserRole.UNIT_SUPERVISOR))
    private val person1 = AuthenticatedUser.Citizen(id = person1Id)
    private val person2 = AuthenticatedUser.Citizen(id = person2Id)
    private val person3 = AuthenticatedUser.Citizen(id = person3Id)
    private val person4 = AuthenticatedUser.Citizen(id = person4Id)
    private val groupId = GroupId(UUID.randomUUID())
    private val placementStart = LocalDate.now().minusDays(30)
    private val placementEnd = LocalDate.now().plusDays(30)

    private fun insertChild(tx: Database.Transaction, child: PersonData.Detailed) {
        tx.insertTestPerson(DevPerson(id = child.id, firstName = child.firstName, lastName = child.lastName))
        tx.insertTestChild(DevChild(id = child.id))

        val placementId = UUID.randomUUID()
        tx.insertTestPlacement(
            DevPlacement(
                id = placementId,
                childId = child.id,
                unitId = testDaycare.id,
                startDate = placementStart,
                endDate = placementEnd
            )
        )
        tx.insertTestDaycareGroupPlacement(
            placementId,
            groupId,
            startDate = placementStart,
            endDate = placementEnd
        )
    }

    @BeforeEach
    internal fun setUp() {
        db.transaction { tx ->
            tx.resetDatabase()

            tx.insertTestCareArea(
                DevCareArea(
                    id = testAreaId,
                )
            )
            tx.insertTestDaycare(
                DevDaycare(
                    areaId = testAreaId,
                    id = testDaycare.id,
                    name = testDaycare.name,
                )
            )
            tx.insertTestDaycareGroup(
                DevDaycareGroup(
                    id = groupId,
                    daycareId = testDaycare.id,
                    startDate = placementStart
                )
            )

            listOf(testAdult_1, testAdult_2, testAdult_3, testAdult_4).map {
                tx.insertTestPerson(DevPerson(id = it.id, firstName = it.firstName, lastName = it.lastName))
                tx.createPersonMessageAccount(it.id)
            }

            // person 1 and 2 are guardians of child 1
            testChild_1.let {
                insertChild(tx, it)
                tx.insertGuardian(person1Id, it.id)
                tx.insertGuardian(person2Id, it.id)
            }

            // person 2 and 3 are guardian of child 3
            testChild_3.let {
                insertChild(tx, it)
                tx.insertGuardian(person2Id, it.id)
                tx.insertGuardian(person3Id, it.id)
            }

            testChild_4.let {
                insertChild(tx, it)
                tx.insertGuardian(person4Id, it.id)
            }

            tx.insertTestEmployee(DevEmployee(id = employee1Id, firstName = "Firstname", lastName = "Employee"))
            tx.upsertEmployeeMessageAccount(employee1Id)
            tx.insertDaycareAclRow(testDaycare.id, employee1Id, UserRole.STAFF)
        }
    }

    @Test
    fun `a thread is created, accessed and replied to by participants who are guardian of the same child`() {
        // given
        val (employee1Account, person1Account, person2Account) = db.read {
            listOf(
                it.getEmployeeMessageAccounts(employee1Id).first(),
                it.getCitizenMessageAccount(person1Id),
                it.getCitizenMessageAccount(person2Id)
            )
        }

        // when a message thread is created
        postNewThread(
            title = "Juhannus",
            message = "Juhannus tulee pian",
            messageType = MessageType.MESSAGE,
            sender = employee1Account,
            recipients = listOf(person1Account, person2Account),
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
            listOf(Pair(employee1Account, "Juhannus tulee pian")),
            threadWithOneReply.toSenderContentPairs()
        )

        // when
        replyAsCitizen(
            person1,
            threadWithOneReply.messages[0].id,
            setOf(employee1Account, person2Account),
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
                Pair(employee1Account, "Juhannus tulee pian"),
                Pair(person1Account, "No niinpä näyttää tulevan")
            ),
            person2Thread.toSenderContentPairs()
        )

        // when person one replies to the employee only
        replyAsCitizen(
            person1,
            person2Thread.messages.last().id,
            setOf(employee1Account),
            "person 2 does not see this"
        )

        // then person one and employee see the new message
        val threadContentWithTwoReplies = listOf(
            Pair(employee1Account, "Juhannus tulee pian"),
            Pair(person1Account, "No niinpä näyttää tulevan"),
            Pair(person1Account, "person 2 does not see this"),
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
                Pair(employee1Account, "Juhannus tulee pian"),
                Pair(person1Account, "No niinpä näyttää tulevan")
            ),
            getMessageThreads(person2Account, person2)[0].toSenderContentPairs()
        )

        // when author replies to person two
        replyAsEmployee(
            user = employee1,
            sender = employee1Account,
            messageId = threadWithOneReply.messages.last().id,
            recipientAccountIds = setOf(person2Account),
            content = "person 1 does not see this"
        )

        // then person two sees that
        assertEquals(
            listOf(
                Pair(employee1Account, "Juhannus tulee pian"),
                Pair(person1Account, "No niinpä näyttää tulevan"),
                Pair(employee1Account, "person 1 does not see this"),
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
                Pair(employee1Account, "Juhannus tulee pian"),
                Pair(person1Account, "No niinpä näyttää tulevan"),
                Pair(person1Account, "person 2 does not see this"),
                Pair(employee1Account, "person 1 does not see this"),
            ),
            getMessageThreads(employee1Account, employee1)[0].toSenderContentPairs()
        )

        // then employee can see all sent messages
        assertEquals(
            listOf(
                Pair("person 1 does not see this", setOf(person2Account)),
                Pair("Juhannus tulee pian", setOf(person1Account, person2Account))
            ),
            getSentMessages(employee1Account, employee1).map { it.toContentRecipientsPair() }
        )

        // then person one can see all sent messages
        assertEquals(
            listOf(
                Pair("person 2 does not see this", setOf(employee1Account)),
                Pair("No niinpä näyttää tulevan", setOf(employee1Account, person2Account))
            ),
            getSentMessages(person1Account, person1).map { it.toContentRecipientsPair() }
        )

        // then person two does not see any sent messages as she has not sent anything
        assertEquals(0, getSentMessages(person2Account, person2).size)
    }

    @Test
    fun `a message is split to several threads by guardianship`() {
        // given
        val (employee1Account, person1Account, person2Account, person3Account, person4Account) = db.read {
            listOf(
                it.getEmployeeMessageAccounts(employee1Id).first(),
                it.getCitizenMessageAccount(person1Id),
                it.getCitizenMessageAccount(person2Id),
                it.getCitizenMessageAccount(person3Id),
                it.getCitizenMessageAccount(person4Id)
            )
        }

        // when a new thread is created to several recipients who do not all have common children
        val title = "Thread splitting"
        val content = "This message is sent to several participants and split to threads"
        val recipients = listOf(person1Account, person2Account, person3Account, person4Account)
        val recipientNames = listOf("Hippiäiset", "Jani")
        postNewThread(
            title = title,
            message = content,
            messageType = MessageType.MESSAGE,
            sender = employee1Account,
            recipients = recipients,
            recipientNames = recipientNames,
            user = employee1
        )

        // then three threads should be created
        db.read {
            assertEquals(1, it.createQuery("SELECT COUNT(id) FROM message_content").mapTo<Int>().one())
            assertEquals(3, it.createQuery("SELECT COUNT(id) FROM message_thread").mapTo<Int>().one())
            assertEquals(3, it.createQuery("SELECT COUNT(id) FROM message").mapTo<Int>().one())
            assertEquals(5, it.createQuery("SELECT COUNT(id) FROM message_recipients").mapTo<Int>().one())
        }

        // then sent message is shown as one
        val sentMessages = db.read { it.getMessagesSentByAccount(employee1Account, 10, 1) }
        assertEquals(1, sentMessages.total)
        assertEquals(1, sentMessages.data.size)
        assertEquals(recipientNames, sentMessages.data.flatMap { it.recipientNames })
        assertEquals(recipients.toSet(), sentMessages.data.flatMap { msg -> msg.recipients.map { it.id } }.toSet())
        assertEquals(title, sentMessages.data[0].threadTitle)
        assertEquals(MessageType.MESSAGE, sentMessages.data[0].type)
        assertEquals(content, sentMessages.data[0].content)

        // then threads are grouped properly
        // person 1 and 2: common child
        // person 2 and 3: common child
        // person 4: no child
        val person1Threads = getMessageThreads(person1Account, person1)
        val person2Threads = getMessageThreads(person2Account, person2)
        val person3Threads = getMessageThreads(person3Account, person3)
        val person4Threads = getMessageThreads(person4Account, person4)

        assertEquals(1, person1Threads.size)
        assertEquals(2, person2Threads.size)
        assertEquals(1, person3Threads.size)
        assertEquals(1, person4Threads.size)
        assertTrue(person2Threads.any { it == person1Threads[0] })
        assertTrue(person2Threads.any { it == person3Threads[0] })
        assertNotEquals(person1Threads, person4Threads)
        assertNotEquals(person3Threads, person4Threads)

        val allThreads = listOf(person1Threads, person2Threads, person3Threads, person4Threads).flatten()
        assertEquals(5, allThreads.size)
        allThreads.forEach {
            assertEquals(title, it.title)
            assertEquals(content, it.messages[0].content)
        }

        // when person 1 replies to thread
        replyAsCitizen(
            person1,
            person1Threads.first().messages.first().id,
            setOf(employee1Account, person2Account),
            "Hello"
        )

        // then only the participants should get the message
        val employeeThreads = getMessageThreads(employee1Account, employee1)
        assertEquals(
            listOf(
                Pair(employee1Account, content),
                Pair(person1Account, "Hello")
            ),
            employeeThreads.map { it.toSenderContentPairs() }.flatten()
        )
        assertEquals(employeeThreads, getMessageThreads(person1Account, person1))
        assertEquals(
            listOf(
                listOf(
                    Pair(employee1Account, content),
                    Pair(person1Account, "Hello")
                ),
                listOf(Pair(employee1Account, content)),
            ),
            getMessageThreads(person2Account, person2).map { it.toSenderContentPairs() }
        )

        assertEquals(person3Threads, getMessageThreads(person3Account, person3))
        assertEquals(person4Threads, getMessageThreads(person4Account, person4))
    }

    @Test
    fun `a bulletin cannot be replied to by the recipients`() {
        // given
        val (employee1Account, person1Account) = db.read {
            listOf(
                it.getEmployeeMessageAccounts(employee1Id).first(),
                it.getCitizenMessageAccount(person1Id),
            )
        }

        // when a bulletin thread is created
        postNewThread(
            title = "Tiedote",
            message = "Juhannus tulee pian",
            messageType = MessageType.BULLETIN,
            sender = employee1Account,
            recipients = listOf(person1Account),
            user = employee1,
        )

        // then the recipient can see it
        val thread = getMessageThreads(person1Account, person1).first()
        assertEquals("Tiedote", thread.title)
        assertEquals(MessageType.BULLETIN, thread.type)
        assertEquals(listOf(Pair(employee1Account, "Juhannus tulee pian")), thread.toSenderContentPairs())

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
                sender = employee1Account,
                user = employee1,
                messageId = thread.messages.last().id,
                recipientAccountIds = setOf(person1Account),
                content = "Nauttikaa siitä"
            ).second.statusCode
        )

        // then the recipient can see it
        assertEquals(
            listOf(
                Pair(employee1Account, "Juhannus tulee pian"),
                Pair(employee1Account, "Nauttikaa siitä")
            ),
            getMessageThreads(person1Account, person1).first().toSenderContentPairs()
        )
    }

    @Test
    fun `messages can be marked read`() {
        // given
        val (employee1Account, person1Account, person2Account) = db.read {
            listOf(
                it.getEmployeeMessageAccounts(employee1Id).first(),
                it.getCitizenMessageAccount(person1Id),
                it.getCitizenMessageAccount(person2Id)
            )
        }

        // when a message thread is created
        postNewThread(
            title = "t1",
            message = "m1",
            messageType = MessageType.MESSAGE,
            sender = employee1Account,
            recipients = listOf(person1Account, person2Account),
            user = employee1,
        )

        // then
        val person1UnreadMessages = getUnreadMessages(person1Account, person1)
        assertEquals(1, person1UnreadMessages.size)
        assertEquals(1, getUnreadMessages(person2Account, person2).size)
        assertEquals(0, getUnreadMessages(employee1Account, employee1).size)

        // when a person replies to the thread
        replyAsCitizen(
            person1,
            person1UnreadMessages.first().id,
            setOf(employee1Account, person2Account),
            "reply"
        )

        // then
        assertEquals(1, getUnreadMessages(employee1Account, employee1).size)
        assertEquals(1, getUnreadMessages(person1Account, person1).size)
        assertEquals(2, getUnreadMessages(person2Account, person2).size)

        // when a thread is marked read
        markThreadRead(person2, person2Account, getMessageThreads(person2Account, person2).first().id)

        // then the thread is marked read
        assertEquals(1, getUnreadMessages(employee1Account, employee1).size)
        assertEquals(1, getUnreadMessages(person1Account, person1).size)
        assertEquals(0, getUnreadMessages(person2Account, person2).size)
    }

    private fun getUnreadMessages(
        accountId: UUID,
        user: AuthenticatedUser
    ) = getMessageThreads(
        accountId,
        user
    ).flatMap { it.messages.filter { m -> m.senderId != accountId && m.readAt == null } }

    private fun postNewThread(
        title: String,
        message: String,
        messageType: MessageType,
        sender: UUID,
        recipients: List<UUID>,
        recipientNames: List<String> = listOf(),
        user: AuthenticatedUser.Employee,
    ) = http.post("/messages/$sender")
        .jsonBody(
            objectMapper.writeValueAsString(
                MessageController.PostMessageBody(
                    title = title,
                    content = message,
                    type = messageType,
                    recipientNames = recipientNames,
                    recipientAccountIds = recipients.toSet()
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
        sender: UUID,
        messageId: UUID,
        recipientAccountIds: Set<UUID>,
        content: String,
    ) =
        http.post(
            "/messages/$sender/$messageId/reply",
        )
            .jsonBody(
                objectMapper.writeValueAsString(
                    MessageController.ReplyToMessageBody(
                        content = content,
                        recipientAccountIds = recipientAccountIds
                    )
                )
            )
            .asUser(user)
            .response()

    private fun markThreadRead(
        user: AuthenticatedUser,
        accountId: UUID,
        threadId: UUID
    ) =
        http.put(
            if (user.isEndUser) "/citizen/messages/threads/$threadId/read" else "/messages/$accountId/threads/$threadId/read"
        )
            .asUser(user)
            .response()

    private fun getMessageThreads(accountId: UUID, user: AuthenticatedUser): List<MessageThread> = http.get(
        if (user.isEndUser) "/citizen/messages/received" else "/messages/$accountId/received",
        listOf("page" to 1, "pageSize" to 100)
    )
        .asUser(user)
        .responseObject<Paged<MessageThread>>(objectMapper).third.get().data

    private fun getSentMessages(accountId: UUID, user: AuthenticatedUser): List<SentMessage> = http.get(
        if (user.isEndUser) "/citizen/messages/sent" else "/messages/$accountId/sent",
        listOf("page" to 1, "pageSize" to 100)
    )
        .asUser(user)
        .responseObject<Paged<SentMessage>>(objectMapper).third.get().data
}

fun MessageThread.toSenderContentPairs(): List<Pair<UUID, String>> = this.messages.map { Pair(it.senderId, it.content) }
fun SentMessage.toContentRecipientsPair(): Pair<String, Set<UUID>> =
    Pair(this.content, this.recipients.map { it.id }.toSet())
