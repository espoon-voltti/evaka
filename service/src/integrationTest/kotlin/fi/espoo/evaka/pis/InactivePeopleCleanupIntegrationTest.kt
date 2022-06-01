// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.incomestatement.IncomeStatementType
import fi.espoo.evaka.messaging.MessageType
import fi.espoo.evaka.messaging.createPersonMessageAccount
import fi.espoo.evaka.messaging.insertMessage
import fi.espoo.evaka.messaging.insertMessageContent
import fi.espoo.evaka.messaging.insertRecipients
import fi.espoo.evaka.messaging.insertThread
import fi.espoo.evaka.messaging.upsertEmployeeMessageAccount
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevChild
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevIncomeStatement
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.insertIncomeStatement
import fi.espoo.evaka.shared.dev.insertTestCareArea
import fi.espoo.evaka.shared.dev.insertTestChild
import fi.espoo.evaka.shared.dev.insertTestDaycare
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.shared.dev.insertTestParentship
import fi.espoo.evaka.shared.dev.insertTestPartnership
import fi.espoo.evaka.shared.dev.insertTestPerson
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testDaycare
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals

class InactivePeopleCleanupIntegrationTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val testDate = LocalDate.of(2020, 3, 1)
    private val testUnit = testDaycare

    @BeforeEach
    fun beforeEach() {
        db.transaction {
            it.insertTestCareArea(testArea)
            it.insertTestDaycare(DevDaycare(id = testUnit.id, name = testUnit.name, areaId = testArea.id))
        }
    }

    @Test
    fun `adult with no family is cleaned up`() {
        db.transaction { tx -> tx.insertPerson(testAdult_1) }

        assertCleanedUpPeople(testDate, setOf(testAdult_1.id))
    }

    @Test
    fun `guardian and their child are cleaned up when neither have archive data`() {
        db.transaction { tx ->
            tx.insertPerson(testAdult_1)
            tx.insertPerson(testChild_1)
            tx.insertGuardian(testAdult_1.id, testChild_1.id)
        }

        assertCleanedUpPeople(testDate, setOf(testAdult_1.id, testChild_1.id))
    }

    @Test
    fun `head of family and their child are cleaned up when neither have archive data`() {
        db.transaction { tx ->
            tx.insertPerson(testAdult_1)
            tx.insertPerson(testChild_1)
            tx.insertTestParentship(
                childId = testChild_1.id,
                headOfChild = testAdult_1.id
            )
        }

        assertCleanedUpPeople(testDate, setOf(testAdult_1.id, testChild_1.id))
    }

    @Test
    fun `head of family and their partner are cleaned up when neither have archive data`() {
        db.transaction { tx ->
            tx.insertPerson(testAdult_1)
            tx.insertPerson(testAdult_2)
            tx.insertTestPartnership(
                adult1 = testAdult_1.id,
                adult2 = testAdult_2.id
            )
        }

        assertCleanedUpPeople(testDate, setOf(testAdult_1.id, testAdult_2.id))
    }

    @Test
    fun `adult with no family that has logged in recently is not cleaned up`() {
        db.transaction { tx ->
            tx.insertPerson(testAdult_1)
            tx.execute(
                "UPDATE person SET last_login = ? WHERE id = ?",
                testDate.minusDays(14),
                testAdult_1.id
            )
        }

        assertCleanedUpPeople(testDate, setOf())
    }

    @Test
    fun `guardian and their child are not cleaned up when guardian has logged in recently`() {
        db.transaction { tx ->
            tx.insertPerson(testAdult_1)
            tx.insertPerson(testChild_1)
            tx.insertGuardian(testAdult_1.id, testChild_1.id)
            tx.execute(
                "UPDATE person SET last_login = ? WHERE id = ?",
                testDate.minusDays(14),
                testAdult_1.id
            )
        }

        assertCleanedUpPeople(testDate, setOf())
    }

    @Test
    fun `head of family and their child are not cleaned up when child has a placement`() {
        db.transaction { tx ->
            tx.insertPerson(testAdult_1)
            tx.insertChild(testChild_1)
            tx.insertTestParentship(
                headOfChild = testAdult_1.id,
                childId = testChild_1.id
            )
            tx.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testUnit.id
            )
        }

        assertCleanedUpPeople(testDate, setOf())
    }

    @Test
    fun `family with two children is not cleaned up when one of the children has a placement`() {
        db.transaction { tx ->
            tx.insertPerson(testAdult_1)
            tx.insertChild(testChild_1)
            tx.insertChild(testChild_2)
            tx.insertTestParentship(
                headOfChild = testAdult_1.id,
                childId = testChild_1.id
            )
            tx.insertTestParentship(
                headOfChild = testAdult_1.id,
                childId = testChild_2.id
            )
            tx.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testUnit.id
            )
        }

        assertCleanedUpPeople(testDate, setOf())
    }

    @Test
    fun `family with two children and two adults is not cleaned up when one of the children has a placement`() {
        db.transaction { tx ->
            tx.insertPerson(testAdult_1)
            tx.insertPerson(testAdult_2)
            tx.insertChild(testChild_1)
            tx.insertChild(testChild_2)
            tx.insertTestPartnership(
                adult1 = testAdult_1.id,
                adult2 = testAdult_2.id
            )
            tx.insertTestParentship(
                headOfChild = testAdult_1.id,
                childId = testChild_1.id
            )
            tx.insertTestParentship(
                headOfChild = testAdult_1.id,
                childId = testChild_2.id
            )
            tx.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testUnit.id
            )
        }

        assertCleanedUpPeople(testDate, setOf())
    }

    @Test
    fun `family with two children with separate heads of family is not cleaned up when one of the children has a placement`() {
        db.transaction { tx ->
            tx.insertPerson(testAdult_1)
            tx.insertPerson(testAdult_2)
            tx.insertChild(testChild_1)
            tx.insertChild(testChild_2)
            tx.insertTestPartnership(
                adult1 = testAdult_1.id,
                adult2 = testAdult_2.id
            )
            tx.insertTestParentship(
                headOfChild = testAdult_1.id,
                childId = testChild_1.id
            )
            tx.insertTestParentship(
                headOfChild = testAdult_2.id,
                childId = testChild_2.id
            )
            tx.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testUnit.id
            )
        }

        assertCleanedUpPeople(testDate, setOf())
    }

    @Test
    fun `adult with income statement is not cleaned up`() {
        db.transaction { tx ->
            tx.insertPerson(testAdult_1)
            tx.insertIncomeStatement(
                DevIncomeStatement(
                    IncomeStatementId(UUID.randomUUID()),
                    testAdult_1.id,
                    LocalDate.now(),
                    IncomeStatementType.INCOME,
                    42
                )
            )
        }

        assertCleanedUpPeople(testDate, setOf())
    }

    @Test
    fun `adult with received messages is cleaned up`() {
        db.transaction { tx ->
            val supervisorId = EmployeeId(UUID.randomUUID())
            tx.insertTestEmployee(DevEmployee(id = supervisorId, firstName = "Firstname", lastName = "Supervisor"))
            val employeeAccount = tx.upsertEmployeeMessageAccount(supervisorId)

            tx.insertPerson(testAdult_1)
            val personAccount = tx.createPersonMessageAccount(testAdult_1.id)

            val contentId = tx.insertMessageContent("content", employeeAccount)
            val threadId = tx.insertThread(MessageType.MESSAGE, "title", urgent = false)
            val messageId = tx.insertMessage(contentId, threadId, employeeAccount, listOf("recipient name"))
            tx.insertRecipients(setOf(personAccount), messageId)
        }

        assertCleanedUpPeople(testDate, setOf(testAdult_1.id))
    }

    @Test
    fun `adult with sent messages is not cleaned up`() {
        db.transaction { tx ->
            val supervisorId = EmployeeId(UUID.randomUUID())
            tx.insertTestEmployee(DevEmployee(id = supervisorId, firstName = "Firstname", lastName = "Supervisor"))
            val employeeAccount = tx.upsertEmployeeMessageAccount(supervisorId)

            tx.insertPerson(testAdult_1)
            val personAccount = tx.createPersonMessageAccount(testAdult_1.id)

            val contentId = tx.insertMessageContent("content", personAccount)
            val threadId = tx.insertThread(MessageType.MESSAGE, "title", urgent = false)
            val messageId = tx.insertMessage(contentId, threadId, personAccount, listOf("employee name"))
            tx.insertRecipients(setOf(employeeAccount), messageId)
        }

        assertCleanedUpPeople(testDate, setOf())
    }

    private fun assertCleanedUpPeople(queryDate: LocalDate, cleanedUpPeople: Set<PersonId>) {
        val result = db.transaction { cleanUpInactivePeople(it, queryDate, Duration.ofMinutes(10)) }

        assertEquals(cleanedUpPeople, result)
    }

    private fun Database.Transaction.insertPerson(person: DevPerson) = insertTestPerson(
        DevPerson(
            id = person.id,
            dateOfBirth = person.dateOfBirth,
            ssn = person.ssn,
            firstName = person.firstName,
            lastName = person.lastName,
            streetAddress = person.streetAddress,
            postalCode = person.postalCode,
            postOffice = person.postOffice,
            email = person.email,
            restrictedDetailsEnabled = person.restrictedDetailsEnabled
        )
    )

    private fun Database.Transaction.insertChild(person: DevPerson) {
        insertPerson(person)
        insertTestChild(DevChild(id = person.id))
    }
}
