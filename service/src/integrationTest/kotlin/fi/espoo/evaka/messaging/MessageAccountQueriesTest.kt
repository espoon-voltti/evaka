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
import fi.espoo.evaka.shared.auth.insertDaycareGroupAcl
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MessageAccountQueriesTest : PureJdbiTest() {

    private val personId: UUID = UUID.randomUUID()
    private val supervisorId: UUID = UUID.randomUUID()
    private val employee1Id: UUID = UUID.randomUUID()
    private val employee2Id: UUID = UUID.randomUUID()

    @BeforeEach
    internal fun setUp() {
        db.transaction {
            it.insertTestPerson(DevPerson(id = personId, firstName = "Firstname", lastName = "Person"))
            it.createPersonMessageAccount(personId)

            it.insertTestEmployee(DevEmployee(id = supervisorId, firstName = "Firstname", lastName = "Supervisor"))
            it.upsertEmployeeMessageAccount(supervisorId)

            it.insertTestEmployee(DevEmployee(id = employee1Id, firstName = "Firstname", lastName = "Employee 1"))
            it.insertTestEmployee(DevEmployee(id = employee2Id, firstName = "Firstname", lastName = "Employee 2"))

            val randomUuid = it.insertTestEmployee(DevEmployee(firstName = "Random", lastName = "Employee"))
            it.upsertEmployeeMessageAccount(randomUuid)

            val areaId = it.insertTestCareArea(DevCareArea())
            val daycareId = it.insertTestDaycare(DevDaycare(areaId = areaId))

            val groupId = it.insertTestDaycareGroup(DevDaycareGroup(daycareId = daycareId, name = "Testiläiset"))
            it.createDaycareGroupMessageAccount(groupId)

            val group2Id = it.insertTestDaycareGroup(DevDaycareGroup(daycareId = daycareId, name = "Koekaniinit"))
            it.createDaycareGroupMessageAccount(group2Id)

            it.insertDaycareAclRow(daycareId, supervisorId, UserRole.UNIT_SUPERVISOR)

            it.insertDaycareAclRow(daycareId, employee1Id, UserRole.STAFF)
            it.insertDaycareGroupAcl(daycareId, employee1Id, listOf(groupId))

            // employee2 has no groups
            it.insertDaycareAclRow(daycareId, employee2Id, UserRole.STAFF)

            // There should be no permissions to anything about this daycare
            val daycare2Id = it.insertTestDaycare(DevDaycare(areaId = areaId, name = "Väärä päiväkoti"))
            val group3Id = it.insertTestDaycareGroup(DevDaycareGroup(daycareId = daycare2Id, name = "Väärät"))
            it.createDaycareGroupMessageAccount(group3Id)
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
    fun `supervisor get access to personal and all group accounts of his daycares`() {
        val personalAccountName = "Supervisor Firstname"
        val group1AccountName = "Test Daycare - Testiläiset"
        val group2AccountName = "Test Daycare - Koekaniinit"

        val accounts = db.transaction { it.getEmployeeMessageAccounts(supervisorId) }
        assertEquals(3, accounts.size)

        val accounts2 = db.read { it.getEmployeeDetailedMessageAccounts(supervisorId) }
        assertEquals(3, accounts2.size)
        val personalAccount =
            accounts2.find { it.type === AccountType.PERSONAL } ?: throw Error("Personal account not found")
        assertEquals(personalAccountName, personalAccount.name)
        assertEquals(AccountType.PERSONAL, personalAccount.type)
        assertNull(personalAccount.daycareGroup)

        val groupAccount =
            accounts2.find { it.name == group1AccountName } ?: throw Error("Group account $group1AccountName not found")
        assertEquals(AccountType.GROUP, groupAccount.type)
        assertEquals("Test Daycare", groupAccount.daycareGroup?.unitName)
        assertEquals("Testiläiset", groupAccount.daycareGroup?.name)

        val group2Account =
            accounts2.find { it.name == group2AccountName } ?: throw Error("Group account $group2AccountName not found")
        assertEquals(AccountType.GROUP, group2Account.type)
        assertEquals("Test Daycare", group2Account.daycareGroup?.unitName)
        assertEquals("Koekaniinit", group2Account.daycareGroup?.name)
    }

    @Test
    fun `employee gets access to the accounts of his groups`() {
        val groupAccountName = "Test Daycare - Testiläiset"

        val accounts = db.transaction { it.getEmployeeMessageAccounts(employee1Id) }
        assertEquals(1, accounts.size)

        val accounts2 = db.read { it.getEmployeeDetailedMessageAccounts(employee1Id) }
        assertEquals(1, accounts2.size)
        assertNull(accounts2.find { it.type === fi.espoo.evaka.messaging.message.AccountType.PERSONAL })

        val groupAccount = accounts2.find { it.daycareGroup != null } ?: throw Error("Group account not found")
        assertEquals(groupAccountName, groupAccount.name)
        assertEquals(AccountType.GROUP, groupAccount.type)
        assertEquals("Test Daycare", groupAccount.daycareGroup?.unitName)
        assertEquals("Testiläiset", groupAccount.daycareGroup?.name)
    }

    @Test
    fun `employee not in any groups sees no accounts`() {
        val accounts = db.transaction { it.getEmployeeMessageAccounts(employee2Id) }
        assertEquals(0, accounts.size)

        val accounts2 = db.read { it.getEmployeeDetailedMessageAccounts(employee2Id) }
        assertEquals(0, accounts2.size)
    }

    @Test
    fun `employee has no access to inactive accounts`() {
        assertEquals(3, db.read { it.getEmployeeMessageAccounts(supervisorId) }.size)
        db.transaction { it.deactivateEmployeeMessageAccount(supervisorId) }

        val accounts = db.transaction { it.getEmployeeMessageAccounts(supervisorId) }
        assertEquals(2, accounts.size)
    }

    @Test
    fun `unread counts`() {
        val accounts = db.read { it.getEmployeeDetailedMessageAccounts(supervisorId) }
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

        assertEquals(3, db.read { it.getUnreadMessagesCount(accounts.map { acc -> acc.id }.toSet()) })
        assertEquals(1, db.read { it.getEmployeeDetailedMessageAccounts(supervisorId) }.first().unreadCount)
    }
}
