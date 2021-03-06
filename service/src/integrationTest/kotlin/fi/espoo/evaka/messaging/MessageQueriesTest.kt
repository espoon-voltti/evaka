// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.messaging.message.MessageType
import fi.espoo.evaka.messaging.message.ThreadWithParticipants
import fi.espoo.evaka.messaging.message.createPersonMessageAccount
import fi.espoo.evaka.messaging.message.getAccountNames
import fi.espoo.evaka.messaging.message.getCitizenMessageAccount
import fi.espoo.evaka.messaging.message.getEmployeeMessageAccounts
import fi.espoo.evaka.messaging.message.getMessagesReceivedByAccount
import fi.espoo.evaka.messaging.message.getMessagesSentByAccount
import fi.espoo.evaka.messaging.message.getThreadByMessageId
import fi.espoo.evaka.messaging.message.getUnreadMessagesCount
import fi.espoo.evaka.messaging.message.insertMessage
import fi.espoo.evaka.messaging.message.insertMessageContent
import fi.espoo.evaka.messaging.message.insertRecipients
import fi.espoo.evaka.messaging.message.insertThread
import fi.espoo.evaka.messaging.message.markThreadRead
import fi.espoo.evaka.messaging.message.upsertEmployeeMessageAccount
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.shared.dev.insertTestPerson
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.jdbi.v3.core.kotlin.mapTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MessageQueriesTest : PureJdbiTest() {

    private val person1Id: UUID = UUID.randomUUID()
    private val person2Id: UUID = UUID.randomUUID()
    private val employee1Id: UUID = UUID.randomUUID()
    private val employee2Id: UUID = UUID.randomUUID()

    @BeforeEach
    internal fun setUp() {
        db.transaction { tx ->
            tx.insertTestPerson(DevPerson(id = person1Id, firstName = "Firstname", lastName = "Person"))
            tx.insertTestPerson(DevPerson(id = person2Id, firstName = "Firstname", lastName = "Person Two"))
            listOf(person1Id, person2Id).forEach { tx.createPersonMessageAccount(it) }

            tx.insertTestEmployee(DevEmployee(id = employee1Id, firstName = "Firstname", lastName = "Employee"))
            tx.insertTestEmployee(DevEmployee(id = employee2Id, firstName = "Firstname", lastName = "Employee Two"))
            listOf(employee1Id, employee2Id).forEach { tx.upsertEmployeeMessageAccount(it) }
        }
    }

    @AfterEach
    internal fun tearDown() {
        db.transaction { it.resetDatabase() }
    }

    @Test
    fun `a thread can be created`() {
        val (employeeAccount, person1Account, person2Account) = db.read {
            listOf(
                it.getEmployeeMessageAccounts(employee1Id).first(),
                it.getCitizenMessageAccount(person1Id),
                it.getCitizenMessageAccount(person2Id)
            )
        }

        val content = "Content"
        val title = "Hello"
        createThread(title, content, employeeAccount, listOf(person1Account, person2Account))

        assertEquals(
            setOf(person1Account, person2Account),
            db.read { it.createQuery("SELECT recipient_id FROM message_recipients").mapTo<UUID>().toSet() }
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
            db.read { it.createQuery("SELECT recipient_names FROM message").mapTo<Array<String>>().one() }.toSet()
        )
    }

    @Test
    fun `messages received by account are grouped properly`() {
        val (employee1Account, employee2Account, person1Account, person2Account) = db.read {
            listOf(
                it.getEmployeeMessageAccounts(employee1Id).first(),
                it.getEmployeeMessageAccounts(employee2Id).first(),
                it.getCitizenMessageAccount(person1Id),
                it.getCitizenMessageAccount(person2Id)
            )
        }

        val thread1Id = createThread("Hello", "Content", employee1Account, listOf(person1Account, person2Account))
        val thread2Id = createThread("Newest thread", "Content 2", employee1Account, listOf(person1Account))
        createThread("Lone Thread", "Alone", employee2Account, listOf(employee2Account))

        // employee is not a recipient in any threads
        assertEquals(0, db.read { it.getMessagesReceivedByAccount(employee1Account, 10, 1) }.data.size)
        val personResult = db.read { it.getMessagesReceivedByAccount(person1Account, 10, 1) }
        assertEquals(2, personResult.data.size)

        val thread = personResult.data.first()
        assertEquals(thread2Id, thread.id)
        assertEquals("Newest thread", thread.title)

        // when the thread is marked read for person 1
        db.transaction { it.markThreadRead(person1Account, thread1Id) }

        // then the message has correct readAt
        val person1Threads = db.read {
            it.getMessagesReceivedByAccount(person1Account, 10, 1)
        }
        assertEquals(2, person1Threads.data.size)
        val readMessages = person1Threads.data.flatMap { it.messages.mapNotNull { m -> m.readAt } }
        assertEquals(1, readMessages.size)
        assertTrue(HelsinkiDateTime.now().durationSince(readMessages[0]) < Duration.ofSeconds(5))

        // then person 2 threads are not affected
        assertEquals(
            0,
            db.read {
                it.getMessagesReceivedByAccount(
                    person2Account,
                    10,
                    1
                )
            }.data.flatMap { it.messages.mapNotNull { m -> m.readAt } }.size
        )

        // when employee gets a reply
        db.transaction {
            val recipients = listOf(employee1Account)
            val contentId = it.insertMessageContent(content = "Just replying here", sender = person1Account)
            val messageId = it.insertMessage(
                contentId = contentId,
                threadId = thread2Id,
                sender = person1Account,
                repliesToMessageId = thread.messages.last().id,
                recipientNames = listOf()
            )
            it.insertRecipients(recipientAccountIds = recipients.toSet(), messageId = messageId)
        }

        // then employee sees the thread
        val employeeResult = db.read { it.getMessagesReceivedByAccount(employee1Account, 10, 1) }
        assertEquals(1, employeeResult.data.size)
        assertEquals("Newest thread", employeeResult.data[0].title)
        assertEquals(2, employeeResult.data[0].messages.size)

        // person 1 is recipient in both threads
        val person1Result = db.read { it.getMessagesReceivedByAccount(person1Account, 10, 1) }
        assertEquals(2, person1Result.data.size)

        val newestThread = person1Result.data[0]
        assertEquals(thread2Id, newestThread.id)
        assertEquals("Newest thread", newestThread.title)
        assertEquals(
            listOf(Pair(employee1Account, "Content 2"), Pair(person1Account, "Just replying here")),
            newestThread.messages.map { Pair(it.senderId, it.content) }
        )
        assertEquals(employeeResult.data[0], newestThread)

        val oldestThread = person1Result.data[1]
        assertEquals(thread1Id, oldestThread.id)
        assertNotNull(oldestThread.messages.find { it.content == "Content" }?.readAt)
        assertNull(oldestThread.messages.find { it.content == "Just replying here" }?.readAt)

        // person 2 is recipient in the oldest thread only
        val person2Result = db.read { it.getMessagesReceivedByAccount(person2Account, 10, 1) }
        assertEquals(1, person2Result.data.size)
        assertEquals(oldestThread.id, person2Result.data[0].id)
        assertEquals(0, person2Result.data.flatMap { it.messages }.mapNotNull { it.readAt }.size)

        // employee 2 is participating with himself
        val employee2Result = db.read { it.getMessagesReceivedByAccount(employee2Account, 10, 1) }
        assertEquals(1, employee2Result.data.size)
        assertEquals(1, employee2Result.data[0].messages.size)
        assertEquals(employee2Account, employee2Result.data[0].messages[0].senderId)
        assertEquals("Alone", employee2Result.data[0].messages[0].content)
    }

    @Test
    fun `received messages can be paged`() {
        val (employee1Account, person1Account) = db.read {
            listOf(
                it.getEmployeeMessageAccounts(employee1Id).first(),
                it.getCitizenMessageAccount(person1Id)
            )
        }

        createThread("t1", "c1", employee1Account, listOf(person1Account))
        createThread("t2", "c2", employee1Account, listOf(person1Account))

        val messages = db.read { it.getMessagesReceivedByAccount(person1Account, 10, 1) }
        assertEquals(2, messages.total)
        assertEquals(2, messages.data.size)
        assertEquals("t2", messages.data[0].title)
        assertEquals("t1", messages.data[1].title)

        val (page1, page2) = db.read {
            listOf(
                it.getMessagesReceivedByAccount(person1Account, 1, 1),
                it.getMessagesReceivedByAccount(person1Account, 1, 2)
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
        val (employee1Account, person1Account, person2Account) = db.read {
            listOf(
                it.getEmployeeMessageAccounts(employee1Id).first(),
                it.getCitizenMessageAccount(person1Id),
                it.getCitizenMessageAccount(person2Id)
            )
        }

        // when two threads are created
        createThread("thread 1", "content 1", employee1Account, listOf(person1Account, person2Account))
        createThread("thread 2", "content 2", employee1Account, listOf(person1Account))

        // then sent messages are returned for sender id
        val firstPage = db.read { it.getMessagesSentByAccount(employee1Account, 1, 1) }
        assertEquals(2, firstPage.total)
        assertEquals(2, firstPage.pages)
        assertEquals(1, firstPage.data.size)

        val newestMessage = firstPage.data[0]
        assertEquals("content 2", newestMessage.content)
        assertEquals("thread 2", newestMessage.threadTitle)
        assertEquals(setOf(person1Account), newestMessage.recipients.map { it.id }.toSet())

        val secondPage = db.read { it.getMessagesSentByAccount(employee1Account, 1, 2) }
        assertEquals(2, secondPage.total)
        assertEquals(2, secondPage.pages)
        assertEquals(1, secondPage.data.size)

        val oldestMessage = secondPage.data[0]
        assertEquals("content 1", oldestMessage.content)
        assertEquals("thread 1", oldestMessage.threadTitle)
        assertEquals(setOf(person1Account, person2Account), oldestMessage.recipients.map { it.id }.toSet())

        // then fetching sent messages by recipient ids does not return the messages
        assertEquals(0, db.read { it.getMessagesSentByAccount(person1Account, 1, 1) }.total)
    }

    @Test
    fun `message participants by messageId`() {
        val (employee1Account, person1Account, person2Account) = db.read {
            listOf(
                it.getEmployeeMessageAccounts(employee1Id).first(),
                it.getCitizenMessageAccount(person1Id),
                it.getCitizenMessageAccount(person2Id)
            )
        }

        val threadId = createThread("Hello", "Content", employee1Account, listOf(person1Account, person2Account))

        val participants = db.read {
            val messageId =
                it.createQuery("SELECT id FROM message WHERE thread_id = :threadId")
                    .bind("threadId", threadId).mapTo<UUID>().one()
            it.getThreadByMessageId(messageId)
        }
        assertEquals(
            ThreadWithParticipants(
                threadId = threadId,
                type = MessageType.MESSAGE,
                senders = setOf(employee1Account),
                recipients = setOf(person1Account, person2Account)
            ),
            participants
        )

        val participants2 = db.transaction { tx ->
            val contentId = tx.insertMessageContent("foo", person2Account)
            val messageId = tx.insertMessage(
                contentId = contentId,
                threadId = threadId,
                sender = person2Account,
                recipientNames = tx.getAccountNames(setOf(employee1Account))
            )
            tx.insertRecipients(setOf(employee1Account), messageId)
            tx.getThreadByMessageId(messageId)
        }
        assertEquals(
            ThreadWithParticipants(
                threadId = threadId,
                type = MessageType.MESSAGE,
                senders = setOf(employee1Account, person2Account),
                recipients = setOf(person1Account, person2Account, employee1Account)
            ),
            participants2
        )
    }

    @Test
    fun `unread messages and marking messages read`() {
        // given
        val (employee1Account, person1Account, person2Account) = db.read {
            listOf(
                it.getEmployeeMessageAccounts(employee1Id).first(),
                it.getCitizenMessageAccount(person1Id),
                it.getCitizenMessageAccount(person2Id)
            )
        }
        val thread1 = createThread("Title", "Content", person1Account, listOf(employee1Account, person2Account))

        // then unread count is zero for sender and one for recipients
        assertEquals(0, db.read { it.getUnreadMessagesCount(setOf(person1Account)) })
        assertEquals(1, db.read { it.getUnreadMessagesCount(setOf(employee1Account)) })
        assertEquals(1, db.read { it.getUnreadMessagesCount(setOf(person2Account)) })

        // when employee reads the message
        db.transaction { it.markThreadRead(employee1Account, thread1) }

        // then the thread does not count towards unread messages
        assertEquals(0, db.read { it.getUnreadMessagesCount(setOf(person1Account)) })
        assertEquals(0, db.read { it.getUnreadMessagesCount(setOf(employee1Account)) })
        assertEquals(1, db.read { it.getUnreadMessagesCount(setOf(person2Account)) })

        // when a new thread is created
        val thread2 = createThread("Title", "Content", employee1Account, listOf(person1Account, person2Account))

        // then unread counts are bumped by one for recipients
        assertEquals(0, db.read { it.getUnreadMessagesCount(setOf(employee1Account)) })
        assertEquals(1, db.read { it.getUnreadMessagesCount(setOf(person1Account)) })
        assertEquals(2, db.read { it.getUnreadMessagesCount(setOf(person2Account)) })

        // when person two reads a thread
        db.transaction { it.markThreadRead(person2Account, thread2) }

        // then unread count goes down by one
        assertEquals(0, db.read { it.getUnreadMessagesCount(setOf(employee1Account)) })
        assertEquals(1, db.read { it.getUnreadMessagesCount(setOf(person1Account)) })
        assertEquals(1, db.read { it.getUnreadMessagesCount(setOf(person2Account)) })
    }

    private fun createThread(
        title: String,
        content: String,
        sender: UUID,
        recipientAccounts: List<UUID>
    ) =
        db.transaction { tx ->
            val contentId = tx.insertMessageContent(content, sender)
            val threadId = tx.insertThread(MessageType.MESSAGE, title)
            val messageId =
                tx.insertMessage(
                    contentId = contentId,
                    threadId = threadId,
                    sender = sender,
                    recipientNames = tx.getAccountNames(recipientAccounts.toSet())
                )
            tx.insertRecipients(recipientAccounts.toSet(), messageId)
            threadId
        }
}
