// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.messaging.message.AccountType
import fi.espoo.evaka.messaging.message.MessageAccount
import fi.espoo.evaka.messaging.message.MessageType
import fi.espoo.evaka.messaging.message.createDaycareGroupMessageAccount
import fi.espoo.evaka.messaging.message.createPersonMessageAccount
import fi.espoo.evaka.messaging.message.deactivateEmployeeMessageAccount
import fi.espoo.evaka.messaging.message.getCitizenMessageAccount
import fi.espoo.evaka.messaging.message.getEmployeeDetailedMessageAccounts
import fi.espoo.evaka.messaging.message.getEmployeeMessageAccounts
import fi.espoo.evaka.messaging.message.getUnreadMessagesCount
import fi.espoo.evaka.messaging.message.insertMessage
import fi.espoo.evaka.messaging.message.insertMessageContent
import fi.espoo.evaka.messaging.message.insertRecipients
import fi.espoo.evaka.messaging.message.insertThread
import fi.espoo.evaka.messaging.message.upsertEmployeeMessageAccount
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.insertTestCareArea
import fi.espoo.evaka.shared.dev.insertTestDaycare
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.shared.dev.insertTestPerson
import org.jdbi.v3.core.kotlin.mapTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class MessageAccountQueriesTest : PureJdbiTest() {

    private val personId: UUID = UUID.randomUUID()
    private val employeeId: UUID = UUID.randomUUID()

    @BeforeEach
    internal fun setUp() {
        db.transaction {
            it.insertTestPerson(DevPerson(id = personId, firstName = "Firstname", lastName = "Person"))
            it.createPersonMessageAccount(personId)

            it.insertTestEmployee(DevEmployee(id = employeeId, firstName = "Firstname", lastName = "Employee"))
            it.upsertEmployeeMessageAccount(employeeId)

            val randomUuid = it.insertTestEmployee(DevEmployee(firstName = "Random", lastName = "Employee"))
            it.upsertEmployeeMessageAccount(randomUuid)

            it.insertTestEmployee(DevEmployee(firstName = "Random", lastName = "Employee without account"))

            val areaId = it.insertTestCareArea(DevCareArea())
            val daycareId = it.insertTestDaycare(DevDaycare(areaId = areaId))
            val groupId = it.insertTestDaycareGroup(DevDaycareGroup(daycareId = daycareId))
            it.createDaycareGroupMessageAccount(groupId)
            it.insertDaycareAclRow(daycareId, employeeId, UserRole.UNIT_SUPERVISOR)
        }
    }

    @AfterEach
    internal fun tearDown() {
        db.transaction { it.resetDatabase() }
    }

    @Test
    fun `citizen gets access to his own account`() {
        db.read { it.getCitizenMessageAccount(personId) }
    }

    @Test
    fun `employee gets access to personal and group accounts`() {
        val personalAccountName = "Employee Firstname"
        val groupAccountName = "Test Daycare - Testiläiset"

        val accounts = db.transaction { it.getEmployeeMessageAccounts(employeeId) }
        assertEquals(2, accounts.size)

        val accounts2 = db.read { it.getEmployeeDetailedMessageAccounts(employeeId) }
        assertEquals(2, accounts2.size)
        val personalAccount =
            accounts2.find { it.type === AccountType.PERSONAL } ?: throw Error("Personal account not found")
        assertEquals(personalAccountName, personalAccount.name)
        assertEquals(AccountType.PERSONAL, personalAccount.type)
        assertNull(personalAccount.daycareGroup)
        val groupAccount = accounts2.find { it.daycareGroup != null } ?: throw Error("Group account not found")
        assertEquals(groupAccountName, groupAccount.name)
        assertEquals(AccountType.GROUP, groupAccount.type)
        assertEquals("Test Daycare", groupAccount.daycareGroup?.unitName)
        assertEquals("Testiläiset", groupAccount.daycareGroup?.name)
    }

    @Test
    fun `employee has no access to inactive accounts`() {
        assertEquals(2, db.read { it.getEmployeeMessageAccounts(employeeId) }.size)
        db.transaction { it.deactivateEmployeeMessageAccount(employeeId) }

        val accounts = db.transaction { it.getEmployeeMessageAccounts(employeeId) }
        assertEquals(1, accounts.size)
    }

    @Test
    fun `unread counts`() {
        val accounts = db.read { it.getEmployeeDetailedMessageAccounts(employeeId) }
        assertEquals(0, accounts.first().unreadCount)

        val employeeAccount = accounts.first().id
        db.transaction { tx ->
            val allAccounts =
                tx.createQuery("SELECT id, account_name as name from message_account_name_view").mapTo<MessageAccount>()
                    .list()

            val contentId = tx.insertMessageContent("content", employeeAccount)
            val threadId = tx.insertThread(MessageType.MESSAGE, "title")
            val messageId = tx.insertMessage(contentId, threadId, employeeAccount, allAccounts.map { it.name })
            tx.insertRecipients(allAccounts.map { it.id }.toSet(), messageId)
        }

        assertEquals(2, db.read { it.getUnreadMessagesCount(accounts.map { acc -> acc.id }.toSet()) })
        assertEquals(1, db.read { it.getEmployeeDetailedMessageAccounts(employeeId) }.first().unreadCount)
    }
}
