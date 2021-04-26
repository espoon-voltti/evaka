// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.messaging.message.getMessageAccountsForUser
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.shared.dev.insertTestPerson
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
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
        assertEquals("Person Firstname", accounts[0].name)
    }

    @Test
    fun `employee gets access to his own account`() {
        val accounts = db.transaction { it.getMessageAccountsForUser(AuthenticatedUser.Employee(id = employeeId, roles = setOf())) }
        assertEquals(1, accounts.size)
        assertEquals("Employee Firstname", accounts[0].name)
    }
}
