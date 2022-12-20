// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.GroupPlacementId
import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.MessageId
import fi.espoo.evaka.shared.MessageThreadId
import fi.espoo.evaka.shared.ParentshipId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.dev.DevChild
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.insertTestCareArea
import fi.espoo.evaka.shared.dev.insertTestChild
import fi.espoo.evaka.shared.dev.insertTestDaycare
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.shared.dev.insertTestParentship
import fi.espoo.evaka.shared.dev.insertTestPerson
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.shared.security.PilotFeature
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MessageQueriesTest : PureJdbiTest(resetDbBeforeEach = true) {

    private val person1Id = PersonId(UUID.randomUUID())
    private val person2Id = PersonId(UUID.randomUUID())
    private val employee1Id = EmployeeId(UUID.randomUUID())
    private val employee2Id = EmployeeId(UUID.randomUUID())
    private lateinit var clock: EvakaClock
    private val sendTime = HelsinkiDateTime.of(LocalDate.of(2022, 5, 14), LocalTime.of(12, 11))
    private val readTime = sendTime.plusSeconds(30)

    private data class TestAccounts(
        val person1: MessageAccountId,
        val person2: MessageAccountId,
        val employee1: MessageAccountId,
        val employee2: MessageAccountId
    )
    private lateinit var accounts: TestAccounts

    @BeforeEach
    fun setUp() {
        clock = MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2022, 11, 8), LocalTime.of(13, 1)))
        db.transaction { tx ->
            tx.insertTestPerson(
                DevPerson(id = person1Id, firstName = "Firstname", lastName = "Person")
            )
            tx.insertTestPerson(
                DevPerson(id = person2Id, firstName = "Firstname", lastName = "Person Two")
            )

            tx.insertTestEmployee(
                DevEmployee(id = employee1Id, firstName = "Firstname", lastName = "Employee")
            )
            tx.insertTestEmployee(
                DevEmployee(id = employee2Id, firstName = "Firstname", lastName = "Employee Two")
            )
            accounts =
                TestAccounts(
                    person1 = tx.createPersonMessageAccount(person1Id),
                    person2 = tx.createPersonMessageAccount(person2Id),
                    employee1 = tx.upsertEmployeeMessageAccount(employee1Id),
                    employee2 = tx.upsertEmployeeMessageAccount(employee2Id)
                )
        }
    }

    @Test
    fun `a thread can be created`() {
        val content = "Content"
        val title = "Hello"
        createThread(title, content, accounts.employee1, listOf(accounts.person1, accounts.person2))

        assertEquals(
            setOf(accounts.person1, accounts.person2),
            db.read {
                it.createQuery("SELECT recipient_id FROM message_recipients")
                    .mapTo<MessageAccountId>()
                    .toSet()
            }
        )
        assertEquals(
            content,
            db.read { it.createQuery("SELECT content FROM message_content").mapTo<String>().one() }
        )
        assertEquals(
            title,
            db.read { it.createQuery("SELECT title FROM message_thread").mapTo<String>().one() }
        )
        assertEquals(
            "Employee Firstname",
            db.read { it.createQuery("SELECT sender_name FROM message").mapTo<String>().one() }
        )
        assertEquals(
            setOf("Person Firstname", "Person Two Firstname"),
            db.read {
                    it.createQuery("SELECT recipient_names FROM message")
                        .mapTo<Array<String>>()
                        .one()
                }
                .toSet()
        )
    }

    @Test
    fun `messages received by account are grouped properly`() {
        val thread1Id =
            createThread(
                "Hello",
                "Content",
                accounts.employee1,
                listOf(accounts.person1, accounts.person2),
                sendTime
            )
        val thread2Id =
            createThread(
                "Newest thread",
                "Content 2",
                accounts.employee1,
                listOf(accounts.person1),
                sendTime.plusSeconds(1)
            )
        createThread(
            "Lone Thread",
            "Alone",
            accounts.employee2,
            listOf(accounts.employee2),
            sendTime.plusSeconds(2)
        )

        // employee is not a recipient in any threads
        assertEquals(
            0,
            db.read { it.getReceivedThreads(readTime, accounts.employee1, 10, 1, "Espoo") }
                .data
                .size
        )
        val personResult = db.read { it.getThreads(readTime, accounts.employee1, 10, 1, "Espoo") }
        assertEquals(2, personResult.data.size)

        val thread = personResult.data.first()
        assertEquals(thread2Id, thread.id)
        assertEquals("Newest thread", thread.title)

        // when the thread is marked read for person 1
        db.transaction { it.markThreadRead(RealEvakaClock(), accounts.person1, thread1Id) }

        // then the message has correct readAt
        val person1Threads = db.read { it.getThreads(readTime, accounts.person1, 10, 1, "Espoo") }
        assertEquals(2, person1Threads.data.size)
        val readMessages = person1Threads.data.flatMap { it.messages.mapNotNull { m -> m.readAt } }
        assertEquals(1, readMessages.size)
        assertTrue(HelsinkiDateTime.now().durationSince(readMessages[0]) < Duration.ofSeconds(5))

        // then person 2 threads are not affected
        assertEquals(
            0,
            db.read { it.getThreads(readTime, accounts.person2, 10, 1, "Espoo") }
                .data
                .flatMap { it.messages.mapNotNull { m -> m.readAt } }
                .size
        )

        // when employee gets a reply
        replyToThread(
            thread2Id,
            accounts.person1,
            setOf(accounts.employee1),
            "Just replying here",
            thread.messages.last().id,
            now = sendTime.plusSeconds(3)
        )

        // then employee sees the thread
        val employeeResult =
            db.read { it.getReceivedThreads(readTime, accounts.employee1, 10, 1, "Espoo") }
        assertEquals(1, employeeResult.data.size)
        assertEquals("Newest thread", employeeResult.data[0].title)
        assertEquals(2, employeeResult.data[0].messages.size)

        // person 1 is recipient in both threads
        val person1Result = db.read { it.getThreads(readTime, accounts.person1, 10, 1, "Espoo") }
        assertEquals(2, person1Result.data.size)

        val newestThread = person1Result.data[0]
        assertEquals(thread2Id, newestThread.id)
        assertEquals("Newest thread", newestThread.title)
        assertEquals(
            listOf(
                Pair(accounts.employee1, "Content 2"),
                Pair(accounts.person1, "Just replying here")
            ),
            newestThread.messages.map { Pair(it.sender.id, it.content) }
        )
        assertEquals(employeeResult.data[0], newestThread)

        val oldestThread = person1Result.data[1]
        assertEquals(thread1Id, oldestThread.id)
        assertNotNull(oldestThread.messages.find { it.content == "Content" }?.readAt)
        assertNull(oldestThread.messages.find { it.content == "Just replying here" }?.readAt)

        // person 2 is recipient in the oldest thread only
        val person2Result = db.read { it.getThreads(readTime, accounts.person2, 10, 1, "Espoo") }
        assertEquals(1, person2Result.data.size)
        assertEquals(oldestThread.id, person2Result.data[0].id)
        assertEquals(0, person2Result.data.flatMap { it.messages }.mapNotNull { it.readAt }.size)

        // employee 2 is participating with himself
        val employee2Result =
            db.read { it.getReceivedThreads(readTime, accounts.employee2, 10, 1, "Espoo") }
        assertEquals(1, employee2Result.data.size)
        assertEquals(1, employee2Result.data[0].messages.size)
        assertEquals(accounts.employee2, employee2Result.data[0].messages[0].sender.id)
        assertEquals("Alone", employee2Result.data[0].messages[0].content)
    }

    @Test
    fun `received messages can be paged`() {
        createThread("t1", "c1", accounts.employee1, listOf(accounts.person1))
        createThread("t2", "c2", accounts.employee1, listOf(accounts.person1))

        val messages = db.read { it.getThreads(readTime, accounts.person1, 10, 1, "Espoo") }
        assertEquals(2, messages.total)
        assertEquals(2, messages.data.size)
        assertEquals(setOf("t1", "t2"), messages.data.map { it.title }.toSet())

        val (page1, page2) =
            db.read {
                listOf(
                    it.getThreads(readTime, accounts.person1, 1, 1, "Espoo"),
                    it.getThreads(readTime, accounts.person1, 1, 2, "Espoo")
                )
            }
        assertEquals(2, page1.total)
        assertEquals(2, page1.pages)
        assertEquals(1, page1.data.size)
        assertEquals(messages.data[0], page1.data[0])

        assertEquals(2, page2.total)
        assertEquals(2, page2.pages)
        assertEquals(1, page2.data.size)
        assertEquals(messages.data[1], page2.data[0])
    }

    @Test
    fun `sent messages`() {
        // when two threads are created
        createThread(
            "thread 1",
            "content 1",
            accounts.employee1,
            listOf(accounts.person1, accounts.person2),
            sendTime
        )
        createThread(
            "thread 2",
            "content 2",
            accounts.employee1,
            listOf(accounts.person1),
            sendTime.plusSeconds(1)
        )

        // then sent messages are returned for sender id
        val firstPage = db.read { it.getMessagesSentByAccount(accounts.employee1, 1, 1) }
        assertEquals(2, firstPage.total)
        assertEquals(2, firstPage.pages)
        assertEquals(1, firstPage.data.size)

        val newestMessage = firstPage.data[0]
        assertEquals("content 2", newestMessage.content)
        assertEquals("thread 2", newestMessage.threadTitle)
        assertEquals(setOf(accounts.person1), newestMessage.recipients.map { it.id }.toSet())

        val secondPage = db.read { it.getMessagesSentByAccount(accounts.employee1, 1, 2) }
        assertEquals(2, secondPage.total)
        assertEquals(2, secondPage.pages)
        assertEquals(1, secondPage.data.size)

        val oldestMessage = secondPage.data[0]
        assertEquals("content 1", oldestMessage.content)
        assertEquals("thread 1", oldestMessage.threadTitle)
        assertEquals(
            setOf(accounts.person1, accounts.person2),
            oldestMessage.recipients.map { it.id }.toSet()
        )

        // then fetching sent messages by recipient ids does not return the messages
        assertEquals(0, db.read { it.getMessagesSentByAccount(accounts.person1, 1, 1) }.total)
    }

    @Test
    fun `message participants by messageId`() {
        val threadId =
            createThread(
                "Hello",
                "Content",
                accounts.employee1,
                listOf(accounts.person1, accounts.person2)
            )

        val participants =
            db.read {
                val messageId =
                    it.createQuery("SELECT id FROM message WHERE thread_id = :threadId")
                        .bind("threadId", threadId)
                        .mapTo<MessageId>()
                        .one()
                it.getThreadByMessageId(messageId)
            }
        assertEquals(
            ThreadWithParticipants(
                threadId = threadId,
                type = MessageType.MESSAGE,
                isCopy = false,
                senders = setOf(accounts.employee1),
                recipients = setOf(accounts.person1, accounts.person2)
            ),
            participants
        )

        val participants2 =
            db.transaction { tx ->
                val contentId = tx.insertMessageContent("foo", accounts.person2)
                val messageId =
                    tx.insertMessage(
                        RealEvakaClock().now(),
                        contentId = contentId,
                        threadId = threadId,
                        sender = accounts.person2,
                        recipientNames = tx.getAccountNames(setOf(accounts.employee1)),
                        municipalAccountName = "Espoo"
                    )
                tx.insertRecipients(listOf(messageId to setOf(accounts.employee1)))
                tx.getThreadByMessageId(messageId)
            }
        assertEquals(
            ThreadWithParticipants(
                threadId = threadId,
                type = MessageType.MESSAGE,
                isCopy = false,
                senders = setOf(accounts.employee1, accounts.person2),
                recipients = setOf(accounts.person1, accounts.person2, accounts.employee1)
            ),
            participants2
        )
    }

    @Test
    fun `query citizen receivers`() {
        lateinit var group1Id: GroupId
        lateinit var group2Id: GroupId
        lateinit var group1Account: MessageAccountId

        val today = LocalDate.now()
        val startDate = today.minusDays(30)
        val endDateGroup1 = today.plusDays(15)
        val startDateGroup2 = today.plusDays(16)
        val endDate = today.plusDays(30)

        db.transaction { tx ->
            tx.insertTestCareArea(testArea)
            tx.insertTestDaycare(
                DevDaycare(
                    areaId = testArea.id,
                    id = testDaycare.id,
                    name = testDaycare.name,
                    language = Language.fi,
                    enabledPilotFeatures = setOf(PilotFeature.MESSAGING)
                )
            )
            tx.insertDaycareAclRow(
                daycareId = testDaycare.id,
                employeeId = employee1Id,
                role = UserRole.UNIT_SUPERVISOR
            )
            group1Id =
                tx.insertTestDaycareGroup(
                    DevDaycareGroup(daycareId = testDaycare.id, name = "Testiläiset")
                )
            group1Account = tx.createDaycareGroupMessageAccount(group1Id)
            group2Id =
                tx.insertTestDaycareGroup(
                    DevDaycareGroup(daycareId = testDaycare.id, name = "Testiläiset 2")
                )
            tx.createDaycareGroupMessageAccount(group2Id)

            // and person1 has a child who is placed into a group
            tx.insertTestPerson(
                DevPerson(id = testChild_1.id, firstName = "Firstname", lastName = "Test Child")
            )
            tx.insertTestChild(DevChild(id = testChild_1.id))
            tx.insertTestParentship(
                id = ParentshipId(UUID.randomUUID()),
                childId = testChild_1.id,
                headOfChild = person1Id,
                startDate = startDate,
                endDate = endDate
            )
            tx.insertGuardian(guardianId = person1Id, childId = testChild_1.id)
            val placementId =
                tx.insertTestPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    type = PlacementType.DAYCARE,
                    startDate = startDate,
                    endDate = endDate
                )
            tx.insertTestDaycareGroupPlacement(
                id = GroupPlacementId(UUID.randomUUID()),
                daycarePlacementId = placementId,
                groupId = group1Id,
                startDate = startDate,
                endDate = endDateGroup1
            )
            tx.insertTestDaycareGroupPlacement(
                id = GroupPlacementId(UUID.randomUUID()),
                daycarePlacementId = placementId,
                groupId = group2Id,
                startDate = startDateGroup2,
                endDate = endDate
            )
        }

        val supervisorPersonalAccount = accounts.employee1

        // when we get the receivers for the citizen person1
        val receivers =
            db.read { it.getCitizenReceivers(today, accounts.person1).values.flatten().toSet() }

        assertEquals(
            setOf(
                MessageAccount(group1Account, "Testiläiset", AccountType.GROUP),
                MessageAccount(
                    supervisorPersonalAccount,
                    "Employee Firstname",
                    AccountType.PERSONAL
                )
            ),
            receivers
        )
    }

    @Test
    fun `query citizen receivers when the citizen is on a blocklist`() {
        lateinit var group1Id: GroupId
        val startDate = LocalDate.now().minusDays(30)
        val endDate = LocalDate.now().plusDays(30)
        db.transaction { tx ->
            tx.insertTestCareArea(testArea)
            tx.insertTestDaycare(
                DevDaycare(
                    areaId = testArea.id,
                    id = testDaycare.id,
                    name = testDaycare.name,
                    language = Language.fi
                )
            )
            tx.insertDaycareAclRow(
                daycareId = testDaycare.id,
                employeeId = employee1Id,
                role = UserRole.UNIT_SUPERVISOR
            )
            group1Id =
                tx.insertTestDaycareGroup(
                    DevDaycareGroup(daycareId = testDaycare.id, name = "Testiläiset")
                )
            tx.createDaycareGroupMessageAccount(group1Id)

            // and person1 has a child who is placed into the group
            tx.insertTestPerson(
                DevPerson(id = testChild_1.id, firstName = "Firstname", lastName = "Test Child")
            )
            tx.insertTestChild(DevChild(id = testChild_1.id))
            tx.insertTestParentship(
                childId = testChild_1.id,
                headOfChild = person1Id,
                startDate = startDate,
                endDate = endDate
            )
            tx.insertGuardian(guardianId = person1Id, childId = testChild_1.id)
            val placementId =
                tx.insertTestPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    type = PlacementType.DAYCARE,
                    startDate = startDate,
                    endDate = endDate
                )
            tx.insertTestDaycareGroupPlacement(
                daycarePlacementId = placementId,
                groupId = group1Id,
                startDate = startDate,
                endDate = endDate
            )

            // and person1 is a blocked receiver
            tx.addToBlocklist(testChild_1.id, person1Id)
        }

        // when we get the receivers for the citizen person1
        val receivers =
            db.read {
                it.getCitizenReceivers(LocalDate.now(), accounts.person1).values.flatten().toSet()
            }

        // the result is empty
        assertEquals(setOf(), receivers.map { it.id }.toSet())
    }

    @Test
    fun `unread messages and marking messages read`() {
        // given
        val thread1 =
            createThread(
                "Title",
                "Content",
                accounts.person1,
                listOf(accounts.employee1, accounts.person2)
            )

        // then unread count is zero for sender and one for recipients
        assertEquals(0, unreadMessagesCount(accounts.person1))
        assertEquals(1, unreadMessagesCount(accounts.employee1))
        assertEquals(1, unreadMessagesCount(accounts.person2))

        // when employee reads the message
        db.transaction { it.markThreadRead(RealEvakaClock(), accounts.employee1, thread1) }

        // then the thread does not count towards unread messages
        assertEquals(0, unreadMessagesCount(accounts.person1))
        assertEquals(0, unreadMessagesCount(accounts.employee1))
        assertEquals(1, unreadMessagesCount(accounts.person2))

        // when a new thread is created
        val thread2 =
            createThread(
                "Title",
                "Content",
                accounts.employee1,
                listOf(accounts.person1, accounts.person2)
            )

        // then unread counts are bumped by one for recipients
        assertEquals(1, unreadMessagesCount(accounts.person1))
        assertEquals(0, unreadMessagesCount(accounts.employee1))
        assertEquals(2, unreadMessagesCount(accounts.person2))

        // when person two reads a thread
        db.transaction { it.markThreadRead(RealEvakaClock(), accounts.person2, thread2) }

        // then unread count goes down by one
        assertEquals(1, unreadMessagesCount(accounts.person1))
        assertEquals(0, unreadMessagesCount(accounts.employee1))
        assertEquals(1, unreadMessagesCount(accounts.person2))
    }

    @Test
    fun `a thread can be archived`() {
        val content = "Content"
        val title = "Hello"
        val threadId = createThread(title, content, accounts.employee1, listOf(accounts.person1))

        assertEquals(1, unreadMessagesCount(accounts.person1))

        db.transaction { tx -> tx.archiveThread(accounts.person1, threadId) }

        assertEquals(0, unreadMessagesCount(accounts.person1))

        assertEquals(
            1,
            db.read {
                val archiveFolderId = it.getArchiveFolderId(accounts.person1)
                it.getReceivedThreads(readTime, accounts.person1, 50, 1, "Espoo", archiveFolderId)
                    .total
            }
        )
    }

    @Test
    fun `an archived threads returns to inbox when it receives messages`() {
        val content = "Content"
        val title = "Hello"
        val threadId = createThread(title, content, accounts.employee1, listOf(accounts.person1))
        db.transaction { tx -> tx.archiveThread(accounts.person1, threadId) }
        assertEquals(
            1,
            db.read {
                val archiveFolderId = it.getArchiveFolderId(accounts.person1)
                it.getReceivedThreads(readTime, accounts.person1, 50, 1, "Espoo", archiveFolderId)
                    .total
            }
        )

        replyToThread(threadId, accounts.employee1, setOf(accounts.person1), "Reply")

        assertEquals(
            1,
            db.read {
                it.getReceivedThreads(readTime, accounts.person1, 50, 1, "Espoo", null).total
            }
        )
        assertEquals(
            0,
            db.read {
                val archiveFolderId = it.getArchiveFolderId(accounts.person1)
                it.getReceivedThreads(readTime, accounts.person1, 50, 1, "Espoo", archiveFolderId)
                    .total
            }
        )
    }

    // TODO: Remove this function, creating threads should be MessageService's job
    private fun createThread(
        title: String,
        content: String,
        sender: MessageAccountId,
        recipientAccounts: List<MessageAccountId>,
        now: HelsinkiDateTime = sendTime
    ): MessageThreadId {
        return db.transaction { tx ->
            val contentId = tx.insertMessageContent(content, sender)
            val threadId =
                tx.insertThread(MessageType.MESSAGE, title, urgent = false, isCopy = false)
            val messageId =
                tx.insertMessage(
                    now,
                    contentId = contentId,
                    threadId = threadId,
                    sender = sender,
                    recipientNames = tx.getAccountNames(recipientAccounts.toSet()),
                    municipalAccountName = "Espoo"
                )
            tx.insertRecipients(listOf(messageId to recipientAccounts.toSet()))
            tx.upsertSenderThreadParticipants(sender, listOf(threadId), now)
            tx.upsertReceiverThreadParticipants(threadId, recipientAccounts.toSet(), now)
            threadId
        }
    }

    // TODO: Remove this function; replying to a thread should be MessageService's job
    private fun replyToThread(
        threadId: MessageThreadId,
        sender: MessageAccountId,
        recipients: Set<MessageAccountId>,
        content: String,
        repliesToMessageId: MessageId? = null,
        now: HelsinkiDateTime = sendTime
    ) {
        db.transaction {
            val contentId = it.insertMessageContent(content = content, sender = sender)
            val messageId =
                it.insertMessage(
                    now = now,
                    contentId = contentId,
                    threadId = threadId,
                    sender = sender,
                    repliesToMessageId = repliesToMessageId,
                    recipientNames = listOf(),
                    municipalAccountName = "Espoo"
                )
            it.insertRecipients(listOf(messageId to recipients))
            it.upsertSenderThreadParticipants(sender, listOf(threadId), now)
            it.upsertReceiverThreadParticipants(threadId, recipients, now)
        }
    }

    private fun unreadMessagesCount(account: MessageAccountId) =
        db.read { it.getUnreadMessagesCounts(readTime, setOf(account)).first().unreadCount }
}
