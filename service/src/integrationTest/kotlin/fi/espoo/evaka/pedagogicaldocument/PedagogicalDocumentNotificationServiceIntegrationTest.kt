// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pedagogicaldocument

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.core.extensions.jsonBody
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.emailclient.MockEmail
import fi.espoo.evaka.emailclient.MockEmailClient
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.messaging.upsertEmployeeMessageAccount
import fi.espoo.evaka.pis.createParentship
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.shared.dev.insertTestPerson
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.dev.resetDatabase
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PedagogicalDocumentNotificationServiceIntegrationTest : FullApplicationTest() {
    @Autowired
    lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    private val testGuardianFi = DevPerson(email = "fi@example.com", language = "fi")
    private val testGuardianSv = DevPerson(email = "sv@example.com", language = "sv")
    private val testGuardianNoEmail = DevPerson(language = "en")
    private val testHeadOfChild = DevPerson(email = "head@example.com", language = "en")

    private val testPersons = listOf(testGuardianFi, testGuardianSv, testGuardianNoEmail, testHeadOfChild)
    private val testNotificationRecipients = listOf(testGuardianFi, testGuardianSv)
    private val testAddresses = testNotificationRecipients.mapNotNull { it.email }

    private val employeeId: UUID = UUID.randomUUID()
    private val employee = AuthenticatedUser.Employee(id = employeeId, roles = setOf(UserRole.UNIT_SUPERVISOR))

    @BeforeEach
    fun beforeEach() {
        val placementStart = LocalDate.now().minusDays(30)
        val placementEnd = LocalDate.now().plusDays(30)

        db.transaction { tx ->
            tx.resetDatabase()
            tx.insertGeneralTestFixtures()

            val groupId = GroupId(UUID.randomUUID())
            tx.insertTestDaycareGroup(
                DevDaycareGroup(
                    id = groupId,
                    daycareId = testDaycare.id,
                    startDate = placementStart
                )
            )

            val placementId = tx.insertTestPlacement(
                DevPlacement(
                    childId = testChild_1.id,
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

            testPersons.forEach {
                tx.insertTestPerson(it)
            }
            tx.insertGuardian(testGuardianFi.id, testChild_1.id)
            tx.insertGuardian(testGuardianSv.id, testChild_1.id)
            tx.createParentship(
                testChild_1.id,
                testHeadOfChild.id,
                LocalDate.now().minusYears(1),
                LocalDate.now().plusYears(1)
            )

            tx.insertTestEmployee(DevEmployee(id = employeeId))
            tx.upsertEmployeeMessageAccount(employeeId)
            tx.insertDaycareAclRow(testDaycare.id, employeeId, UserRole.STAFF)
        }

        MockEmailClient.emails.clear()
    }

    @Test
    fun `notifications are sent to guardians but not heads`() {
        postNewDocument(
            user = employee,
            childId = testChild_1.id
        )
        asyncJobRunner.runPendingJobsSync()

        assertEquals(
            testAddresses.toSet(),
            MockEmailClient.emails.map { it.toAddress }.toSet()
        )
        assertEquals(
            "Uusi pedagoginen dokumentti eVakassa / Nytt pedagogiskt dokument i eVaka / New pedagogical document in eVaka [null]",
            getEmailFor(testGuardianFi).subject
        )
        assertEquals("Esbo småbarnspedagogik <no-reply.evaka@espoo.fi>", getEmailFor(testGuardianSv).fromAddress)
    }

    @Test
    fun `sending of notifications are tracked in the database`() {
        val doc = postNewDocument(
            user = employee,
            childId = testChild_1.id
        )
        asyncJobRunner.runPendingJobsSync()

        db.transaction {
            it.createQuery("SELECT email_sent FROM pedagogical_document WHERE id = :docId").bind("docId", doc.id)
                .map { row ->
                    assertTrue(row.mapColumn("email_sent"))
                }
        }
    }

    @Test
    fun `notifications are sent to the guardians with email addresses even if one guardian has no email address`() {
        db.transaction { tx ->
            tx.insertGuardian(testGuardianNoEmail.id, testChild_1.id)
        }

        postNewDocument(
            user = employee,
            childId = testChild_1.id
        )
        asyncJobRunner.runPendingJobsSync()

        assertEquals(
            testAddresses.toSet(),
            MockEmailClient.emails.map { it.toAddress }.toSet()
        )
        assertEquals(
            "Uusi pedagoginen dokumentti eVakassa / Nytt pedagogiskt dokument i eVaka / New pedagogical document in eVaka [null]",
            getEmailFor(testGuardianFi).subject
        )
        assertEquals("Esbo småbarnspedagogik <no-reply.evaka@espoo.fi>", getEmailFor(testGuardianSv).fromAddress)
    }

    private fun postNewDocument(
        user: AuthenticatedUser.Employee,
        childId: UUID
    ) = objectMapper.readValue<PedagogicalDocument>(
        http.post("/pedagogical-document")
            .jsonBody("""{"childId": "$childId", "description": "foobar"}""")
            .asUser(user)
            .responseString()
            .third.get()
    )

    private fun getEmailFor(person: DevPerson): MockEmail {
        val address = person.email ?: throw Error("$person has no email")
        return MockEmailClient.getEmail(address) ?: throw Error("No emails sent to $address")
    }
}
