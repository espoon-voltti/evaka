// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.messaging.message.getAuthorizedMessageAccountsForUser
import fi.espoo.evaka.messaging.message.getMessageAccountsForUser
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.auth.AuthenticatedUser
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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class MessageAccountQueriesTest : PureJdbiTest() {

    private val personId: UUID = UUID.randomUUID()
    private val employeeId: UUID = UUID.randomUUID()
    private val employee = AuthenticatedUser.Employee(id = employeeId, roles = setOf())

    @BeforeEach
    internal fun setUp() {
        db.transaction {
            it.insertTestPerson(DevPerson(id = personId, firstName = "Firstname", lastName = "Person"))
            it.execute("INSERT INTO message_account (person_id) VALUES ('$personId')")

            it.insertTestEmployee(DevEmployee(id = employeeId, firstName = "Firstname", lastName = "Employee"))
            it.execute("INSERT INTO message_account (employee_id) VALUES ('$employeeId')")
        }
    }

    @AfterEach
    internal fun tearDown() {
        db.transaction { it.resetDatabase() }
    }

    @Test
    fun `person gets access to his own account`() {
        val accounts = db.transaction { it.getMessageAccountsForUser(AuthenticatedUser.Citizen(personId)) }
        assertEquals(1, accounts.size)
        assertEquals("Person Firstname", accounts.first().name)
    }

    @Test
    fun `employee gets access to his own account`() {
        val expectedName = "Employee Firstname"

        val accounts = db.transaction { it.getMessageAccountsForUser(employee) }
        assertEquals(1, accounts.size)
        assertEquals(expectedName, accounts.first().name)

        val accounts2 = db.transaction { it.getAuthorizedMessageAccountsForUser(employee) }
        assertEquals(1, accounts2.size)
        assertEquals(expectedName, accounts2.first().name)
        assertTrue(accounts2.first().personal)
        assertNull(accounts2.first().daycareGroup)
    }

    @Test
    fun `employee gets access to group accounts`() {
        db.transaction {
            val areaId = it.insertTestCareArea(DevCareArea())
            val daycareId = it.insertTestDaycare(DevDaycare(areaId = areaId))
            val groupId = it.insertTestDaycareGroup(DevDaycareGroup(daycareId = daycareId))
            it.execute("INSERT INTO message_account (daycare_group_id) VALUES ('$groupId')")
            it.insertDaycareAclRow(daycareId, employeeId, UserRole.UNIT_SUPERVISOR)
        }

        val accounts = db.transaction { it.getMessageAccountsForUser(employee) }
        assertEquals(2, accounts.size)
        assertEquals(1, accounts.filter { it.name == "Employee Firstname" }.size)
        val groupAccountName = "Test Daycare - Testiläiset"
        assertEquals(1, accounts.filter { it.name == groupAccountName }.size)

        val accounts2 = db.transaction { it.getAuthorizedMessageAccountsForUser(employee) }
        assertEquals(2, accounts2.size)
        val groupAccount = accounts2.find { it.daycareGroup != null } ?: throw Error("Group account not found")
        assertEquals(groupAccountName, groupAccount.name)
        assertEquals("Test Daycare", groupAccount.daycareGroup?.unitName)
        assertEquals("Testiläiset", groupAccount.daycareGroup?.name)
    }
}
