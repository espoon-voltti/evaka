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
    private val employeeId: UUID = UUID.randomUUID()

    @BeforeEach
    internal fun setUp() {
        db.transaction {
            it.insertTestPerson(DevPerson(id = person1Id, firstName = "Firstname", lastName = "Person"))
            it.execute("INSERT INTO message_account (person_id) VALUES ('$person1Id')")

            it.insertTestPerson(DevPerson(id = person2Id, firstName = "Firstname", lastName = "Person Two"))
            it.execute("INSERT INTO message_account (person_id) VALUES ('$person2Id')")

            it.insertTestEmployee(DevEmployee(id = employeeId, firstName = "Firstname", lastName = "Employee"))
            it.execute("INSERT INTO message_account (employee_id) VALUES ('$employeeId')")
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
                it.getMessageAccountsForUser(AuthenticatedUser.Employee(id = employeeId, roles = setOf())).first(),
                it.getMessageAccountsForUser(AuthenticatedUser.Citizen(id = person1Id)).first(),
                it.getMessageAccountsForUser(AuthenticatedUser.Citizen(id = person2Id)).first()
            )
        }

        db.transaction {
            it.createMessageThread("Hello", "Content", MessageType.MESSAGE, employeeAccount, listOf(person1Account.id, person2Account.id))
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
        val (employeeAccount, person1Account, person2Account) = db.read {
            listOf(
                it.getMessageAccountsForUser(AuthenticatedUser.Employee(id = employeeId, roles = setOf())).first(),
                it.getMessageAccountsForUser(AuthenticatedUser.Citizen(id = person1Id)).first(),
                it.getMessageAccountsForUser(AuthenticatedUser.Citizen(id = person2Id)).first()
            )
        }

        val thread1Id = db.transaction {
            it.createMessageThread("Hello", "Content", MessageType.MESSAGE, employeeAccount, listOf(person1Account.id, person2Account.id))
        }
        val thread2Id = db.transaction {
            it.createMessageThread("Newest thread", "Content 2", MessageType.MESSAGE, employeeAccount, listOf(person1Account.id))
        }

        // employee is not a recipient in any threads
        assertEquals(0, db.read { it.getMessagesReceivedByAccount(employeeAccount.id, 10, 1) }.data.size)

        db.transaction { it.replyToThread(thread2Id, "Just replying here", person1Account, listOf(employeeAccount.id)) }

        // employee is now recipient in a reply to thread two
        val employeeResult = db.read { it.getMessagesReceivedByAccount(employeeAccount.id, 10, 1) }
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
            listOf(Pair(employeeAccount.id, "Content 2"), Pair(person1Account.id, "Just replying here")),
            newestThread.messages.map { Pair(it.senderId, it.content) }
        )
        assertEquals(employeeResult.data[0], newestThread)

        val oldestThread = person1Result.data[1]
        assertEquals(thread1Id, oldestThread.id)

        // person 2 is recipient in the oldest thread only
        val person2Result = db.read { it.getMessagesReceivedByAccount(person2Account.id, 10, 1) }
        assertEquals(1, person2Result.data.size)
        assertEquals(oldestThread, person2Result.data[0])
    }
}
