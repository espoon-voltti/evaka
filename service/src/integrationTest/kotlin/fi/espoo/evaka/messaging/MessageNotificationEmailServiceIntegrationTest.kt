// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.core.isSuccessful
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.emailclient.MockEmail
import fi.espoo.evaka.emailclient.MockEmailClient
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.shared.dev.insertTestPerson
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class MessageNotificationEmailServiceIntegrationTest :
    FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    private val testPersonFi = DevPerson(email = "fi@example.com", language = "fi")
    private val testPersonSv = DevPerson(email = "sv@example.com", language = "sv")
    private val testPersonEn = DevPerson(email = "en@example.com", language = "en")
    private val testPersonNoEmail = DevPerson(email = null, language = "fi")

    private val testPersons = listOf(testPersonFi, testPersonSv, testPersonEn, testPersonNoEmail)
    private val testAddresses = testPersons.mapNotNull { it.email }

    private val employeeId = EmployeeId(UUID.randomUUID())
    private val employee =
        AuthenticatedUser.Employee(id = employeeId, roles = setOf(UserRole.UNIT_SUPERVISOR))

    @BeforeEach
    fun beforeEach() {
        val placementStart = LocalDate.now().minusDays(30)
        val placementEnd = LocalDate.now().plusDays(30)

        db.transaction { tx ->
            tx.insertGeneralTestFixtures()

            val groupId = GroupId(UUID.randomUUID())
            tx.insertTestDaycareGroup(
                DevDaycareGroup(
                    id = groupId,
                    daycareId = testDaycare.id,
                    startDate = placementStart
                )
            )

            val placementId =
                tx.insertTestPlacement(
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
                tx.insertGuardian(it.id, testChild_1.id)
                tx.createPersonMessageAccount(it.id)
            }

            tx.insertTestEmployee(DevEmployee(id = employeeId))
            tx.upsertEmployeeMessageAccount(employeeId)
            tx.insertDaycareAclRow(testDaycare.id, employeeId, UserRole.STAFF)
        }

        MockEmailClient.emails.clear()
    }

    @Test
    fun `notifications are sent to citizens`() {
        val employeeAccount = db.read { it.getEmployeeMessageAccountIds(employeeId).first() }

        postNewThread(
            sender = employeeAccount,
            recipients = listOf(MessageRecipient(MessageRecipientType.CHILD, testChild_1.id)),
            user = employee,
        )
        asyncJobRunner.runPendingJobsSync(RealEvakaClock())

        assertEquals(testAddresses.toSet(), MockEmailClient.emails.map { it.toAddress }.toSet())
        assertEquals(
            "Uusi viesti eVakassa / Nytt meddelande i eVaka / New message in eVaka [null]",
            getEmailFor(testPersonFi).subject
        )
        assertEquals(
            "Esbo småbarnspedagogik <no-reply.evaka@espoo.fi>",
            getEmailFor(testPersonSv).fromAddress
        )
        assertEquals(
            "Espoon Varhaiskasvatus <no-reply.evaka@espoo.fi>",
            getEmailFor(testPersonEn).fromAddress
        )
    }

    private fun postNewThread(
        sender: MessageAccountId,
        recipients: List<MessageRecipient>,
        user: AuthenticatedUser.Employee,
    ) {
        val (_, response) =
            http
                .post("/messages/$sender")
                .jsonBody(
                    jsonMapper.writeValueAsString(
                        MessageController.PostMessageBody(
                            title = "Juhannus",
                            content = "Juhannus tulee pian",
                            type = MessageType.MESSAGE,
                            recipients = recipients.toSet(),
                            recipientNames = listOf(),
                            urgent = false
                        )
                    )
                )
                .asUser(user)
                .response()
        assertTrue(response.isSuccessful)
    }

    private fun getEmailFor(person: DevPerson): MockEmail {
        val address = person.email ?: throw Error("$person has no email")
        return MockEmailClient.getEmail(address) ?: throw Error("No emails sent to $address")
    }
}
