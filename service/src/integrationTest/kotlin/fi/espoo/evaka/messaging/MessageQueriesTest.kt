// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.messaging.message.MessageType
import fi.espoo.evaka.messaging.message.createMessageThread
import fi.espoo.evaka.messaging.message.getMessageAccountsForUser
import fi.espoo.evaka.messaging.message.getMessagesReceivedByAccount
import fi.espoo.evaka.messaging.message.replyToThread
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.shared.dev.insertTestPerson
import org.jdbi.v3.core.kotlin.mapTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class MessageQueriesTest : PureJdbiTest() {

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
    fun `a thread can be created`() {
        val (employeeAccount, person1Account, person2Account) = db.read {
            listOf(
                it.getMessageAccountsForUser(AuthenticatedUser.Employee(id = employee1Id, roles = setOf())).first(),
                it.getMessageAccountsForUser(AuthenticatedUser.Citizen(id = person1Id)).first(),
                it.getMessageAccountsForUser(AuthenticatedUser.Citizen(id = person2Id)).first()
            )
        }

        db.transaction {
            it.createMessageThread(
                "Hello",
                "Content",
                MessageType.MESSAGE,
                employeeAccount,
                setOf(person1Account.id, person2Account.id)
            )
        }

        assertEquals(
            setOf(person1Account.id, person2Account.id),
            db.read { it.createQuery("SELECT recipient_id FROM message_recipients").mapTo<UUID>().toSet() }
        )
        assertEquals(
            "Content",
            db.read { it.createQuery("SELECT content FROM message_content").mapTo<String>().one() }
        )
        assertEquals(
            "Hello",
            db.read { it.createQuery("SELECT title FROM message_thread").mapTo<String>().one() }
        )
        assertEquals(
            "Employee Firstname",
            db.read { it.createQuery("SELECT sender_name FROM message").mapTo<String>().one() }
        )
    }

    @Test
    fun `messages received by account are grouped properly`() {
        val (employee1Account, employee2Account, person1Account, person2Account) = db.read {
            listOf(
                it.getMessageAccountsForUser(AuthenticatedUser.Employee(id = employee1Id, roles = setOf())).first(),
                it.getMessageAccountsForUser(AuthenticatedUser.Employee(id = employee2Id, roles = setOf())).first(),
                it.getMessageAccountsForUser(AuthenticatedUser.Citizen(id = person1Id)).first(),
                it.getMessageAccountsForUser(AuthenticatedUser.Citizen(id = person2Id)).first()
            )
        }

        val thread1Id = db.transaction {
            it.createMessageThread(
                "Hello",
                "Content",
                MessageType.MESSAGE,
                employee1Account,
                setOf(person1Account.id, person2Account.id)
            )
        }
        val thread2Id = db.transaction {
            it.createMessageThread(
                "Newest thread",
                "Content 2",
                MessageType.MESSAGE,
                employee1Account,
                setOf(person1Account.id)
            )
        }
        db.transaction {
            it.createMessageThread(
                "Lone thread",
                "Alone",
                MessageType.MESSAGE,
                employee2Account,
                setOf(employee2Account.id)
            )
        }

        // employee is not a recipient in any threads
        assertEquals(0, db.read { it.getMessagesReceivedByAccount(employee1Account.id, 10, 1) }.data.size)

        db.transaction { it.replyToThread(thread2Id, "Just replying here", person1Account, setOf(employee1Account.id)) }

        // employee is now recipient in a reply to thread two
        val employeeResult = db.read { it.getMessagesReceivedByAccount(employee1Account.id, 10, 1) }
        assertEquals(1, employeeResult.data.size)
        assertEquals("Newest thread", employeeResult.data[0].title)
        assertEquals(2, employeeResult.data[0].messages.size)

        // person 1 is recipient in both threads
        val person1Result = db.read { it.getMessagesReceivedByAccount(person1Account.id, 10, 1) }
        assertEquals(2, person1Result.data.size)

        val newestThread = person1Result.data[0]
        assertEquals(thread2Id, newestThread.id)
        assertEquals("Newest thread", newestThread.title)
        assertEquals(
            listOf(Pair(employee1Account.id, "Content 2"), Pair(person1Account.id, "Just replying here")),
            newestThread.messages.map { Pair(it.senderId, it.content) }
        )
        assertEquals(employeeResult.data[0], newestThread)

        val oldestThread = person1Result.data[1]
        assertEquals(thread1Id, oldestThread.id)

        // person 2 is recipient in the oldest thread only
        val person2Result = db.read { it.getMessagesReceivedByAccount(person2Account.id, 10, 1) }
        assertEquals(1, person2Result.data.size)
        assertEquals(oldestThread, person2Result.data[0])

        // employee 2 is participating with himself
        val employee2Result = db.read { it.getMessagesReceivedByAccount(employee2Account.id, 10, 1) }
        assertEquals(1, employee2Result.data.size)
        assertEquals(1, employee2Result.data[0].messages.size)
        assertEquals(employee2Account.id, employee2Result.data[0].messages[0].senderId)
        assertEquals("Alone", employee2Result.data[0].messages[0].content)
    }

    @Test
    fun `received messages can be paged`() {
        val (employee1Account, person1Account) = db.read {
            listOf(
                it.getMessageAccountsForUser(AuthenticatedUser.Employee(id = employee1Id, roles = setOf())).first(),
                it.getMessageAccountsForUser(AuthenticatedUser.Citizen(id = person1Id)).first()
            )
        }

        db.transaction {
            it.createMessageThread("t1", "c1", MessageType.MESSAGE, employee1Account, setOf(person1Account.id))
        }
        db.transaction {
            it.createMessageThread("t2", "c2", MessageType.MESSAGE, employee1Account, setOf(person1Account.id))
        }

        val messages = db.read { it.getMessagesReceivedByAccount(person1Account.id, 10, 1) }
        assertEquals(2, messages.total)
        assertEquals(2, messages.data.size)
        assertEquals("t2", messages.data[0].title)
        assertEquals("t1", messages.data[1].title)

        val (page1, page2) = db.read {
            listOf(
                it.getMessagesReceivedByAccount(person1Account.id, 1, 1),
                it.getMessagesReceivedByAccount(person1Account.id, 1, 2)
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
}
