// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.persistence.daycare.Adult
import fi.espoo.evaka.application.persistence.daycare.Apply
import fi.espoo.evaka.application.persistence.daycare.Child
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.emailclient.Email
import fi.espoo.evaka.emailclient.MockEmailClient
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.MessageThreadFolderId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.job.ScheduledJobs
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.toApplicationType
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class MessageNotificationEmailServiceIntegrationTest :
    FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>
    @Autowired lateinit var accessControl: AccessControl
    @Autowired lateinit var messageController: MessageController
    @Autowired lateinit var scheduledJobs: ScheduledJobs

    private val testPersonFi = DevPerson(email = "fi@example.com", language = "fi")
    private val testPersonSv = DevPerson(email = "sv@example.com", language = "sv")
    private val testPersonEn = DevPerson(email = "en@example.com", language = "en")
    private val testPersonEl = DevPerson(email = "el@example.com", language = "el")
    private val testPersonNoEmail = DevPerson(email = null, language = "fi")

    private val testPersons =
        listOf(testPersonFi, testPersonSv, testPersonEn, testPersonEl, testPersonNoEmail)
    private val testAddresses = testPersons.mapNotNull { it.email }

    private val employeeId = EmployeeId(UUID.randomUUID())
    private val employee =
        AuthenticatedUser.Employee(id = employeeId, roles = setOf(UserRole.UNIT_SUPERVISOR))

    private lateinit var clock: MockEvakaClock

    @BeforeEach
    fun beforeEach() {
        clock = MockEvakaClock(2023, 1, 1, 12, 0)
        val placementStart = clock.today().minusDays(30)
        val placementEnd = clock.today().plusDays(30)

        db.transaction { tx ->
            tx.insert(testArea)
            tx.insert(testDaycare)
            tx.insert(testChild_1, DevPersonType.CHILD)

            val groupId = GroupId(UUID.randomUUID())
            tx.insert(
                DevDaycareGroup(
                    id = groupId,
                    daycareId = testDaycare.id,
                    startDate = placementStart,
                )
            )

            val placementId =
                tx.insert(
                    DevPlacement(
                        childId = testChild_1.id,
                        unitId = testDaycare.id,
                        startDate = placementStart,
                        endDate = placementEnd,
                    )
                )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementId,
                    daycareGroupId = groupId,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )

            testPersons.forEach {
                tx.insert(it, DevPersonType.ADULT)
                tx.insertGuardian(it.id, testChild_1.id)
            }

            tx.insert(DevEmployee(id = employeeId))
            tx.upsertEmployeeMessageAccount(employeeId)
            tx.insertDaycareAclRow(testDaycare.id, employeeId, UserRole.STAFF)
        }
    }

    @Test
    fun `notifications are sent to citizens`() {
        val employeeAccount =
            db.read {
                it.getEmployeeMessageAccountIds(
                        accessControl.requireAuthorizationFilter(
                            it,
                            employee,
                            clock,
                            Action.MessageAccount.ACCESS,
                        )
                    )
                    .first()
            }

        postNewThread(
            sender = employeeAccount,
            recipients = listOf(MessageRecipient.Child(testChild_1.id)),
            user = employee,
            clock,
        )
        asyncJobRunner.runPendingJobsSync(MockEvakaClock(clock.now().plusSeconds(5)))

        assertEquals(testAddresses.toSet(), MockEmailClient.emails.map { it.toAddress }.toSet())
        assertEquals(
            "Uusi viesti eVakassa / Nytt personligt meddelande i eVaka / New message in eVaka",
            getEmailFor(testPersonFi).content.subject,
        )
        assertEquals(
            "Esbo småbarnspedagogik <no-reply.evaka@espoo.fi>",
            getEmailFor(testPersonSv).fromAddress.address,
        )
        assertEquals(
            "Espoon Varhaiskasvatus <no-reply.evaka@espoo.fi>",
            getEmailFor(testPersonEn).fromAddress.address,
        )
    }

    @Test
    fun `bulletin notifications are sent to citizens`() {
        val municipalAccountId = db.transaction { tx -> tx.createMunicipalMessageAccount() }

        val adminUser =
            db.transaction { tx ->
                AuthenticatedUser.Employee(
                    tx.insert(DevEmployee(roles = setOf(UserRole.ADMIN))),
                    roles = setOf(UserRole.ADMIN),
                )
            }

        postNewThread(
            sender = municipalAccountId,
            recipients = listOf(MessageRecipient.Child(testChild_1.id)),
            user = adminUser,
            clock,
            type = MessageType.BULLETIN,
        )
        asyncJobRunner.runPendingJobsSync(MockEvakaClock(clock.now().plusSeconds(5)))

        assertEquals(testAddresses.toSet(), MockEmailClient.emails.map { it.toAddress }.toSet())
        assertEquals(
            "Uusi tiedote eVakassa / Nytt allmänt meddelande i eVaka / New bulletin in eVaka",
            getEmailFor(testPersonFi).content.subject,
        )
        assertTrue(
            getEmailFor(testPersonFi)
                .content
                .text
                .startsWith(
                    "Sinulle on saapunut uusi tiedote eVakaan lähettäjältä Espoon kaupunki - Esbo stad - City of Espoo otsikolla \"Juhannus/Midsommar/Midsummer\"."
                )
        )

        assertEquals(
            "Esbo småbarnspedagogik <no-reply.evaka@espoo.fi>",
            getEmailFor(testPersonSv).fromAddress.address,
        )
        assertEquals(
            "Espoon Varhaiskasvatus <no-reply.evaka@espoo.fi>",
            getEmailFor(testPersonEn).fromAddress.address,
        )
    }

    @Test
    fun `a notification is not sent when the message has been already read`() {
        val employeeAccount =
            db.read {
                it.getEmployeeMessageAccountIds(
                        accessControl.requireAuthorizationFilter(
                            it,
                            employee,
                            clock,
                            Action.MessageAccount.ACCESS,
                        )
                    )
                    .first()
            }

        val contentId =
            postNewThread(
                sender = employeeAccount,
                recipients = listOf(MessageRecipient.Child(testChild_1.id)),
                user = employee,
                clock = clock,
            )
        assertNotNull(contentId)

        markAllRecipientMessagesRead(testPersonFi, clock)

        asyncJobRunner.runPendingJobsSync(MockEvakaClock(clock.now().plusSeconds(5)))

        assertEquals(3, MockEmailClient.emails.size)
        assertTrue(MockEmailClient.emails.none { email -> email.toAddress == testPersonFi.email })
    }

    @Test
    fun `notifications related to an application are sent to citizens`() {
        val guardian = testPersons.first()
        val applicationId =
            db.transaction { tx ->
                tx.insertTestApplication(
                    sentDate = clock.today(),
                    dueDate = null,
                    guardianId = guardian.id,
                    childId = testChild_1.id,
                    type = PlacementType.DAYCARE.toApplicationType(),
                    document =
                        DaycareFormV0(
                            type = ApplicationType.DAYCARE,
                            serviceStart = "08:00",
                            serviceEnd = "16:00",
                            child = Child(dateOfBirth = clock.today().minusYears(3)),
                            guardian = Adult(),
                            apply = Apply(preferredUnits = listOf(testDaycare.id)),
                            preferredStartDate = clock.today().plusMonths(5),
                        ),
                )
            }
        val employeeAccount =
            db.read {
                it.getEmployeeMessageAccountIds(
                        accessControl.requireAuthorizationFilter(
                            it,
                            employee,
                            clock,
                            Action.MessageAccount.ACCESS,
                        )
                    )
                    .first()
            }

        postNewThread(
            sender = employeeAccount,
            recipients = listOf(MessageRecipient.Child(testChild_1.id)),
            user = employee,
            clock = clock,
            relatedApplicationId = applicationId,
        )
        asyncJobRunner.runPendingJobsSync(MockEvakaClock(clock.now().plusSeconds(5)))

        assertEquals(testAddresses.toSet(), MockEmailClient.emails.map { it.toAddress }.toSet())
        assertEquals(
            "Uusi viesti eVakassa / Nytt personligt meddelande i eVaka / New message in eVaka",
            getEmailFor(testPersonFi).content.subject,
        )
        assertTrue(
            getEmailFor(testPersonFi)
                .content
                .text
                .contains(
                    "Sinulle on saapunut uusi hakemustasi koskeva viesti eVakaan lähettäjältä"
                )
        )

        assertEquals(
            "Esbo småbarnspedagogik <no-reply.evaka@espoo.fi>",
            getEmailFor(testPersonSv).fromAddress.address,
        )

        assertEquals(
            "Espoon Varhaiskasvatus <no-reply.evaka@espoo.fi>",
            getEmailFor(testPersonEn).fromAddress.address,
        )
    }

    private fun postNewThread(
        sender: MessageAccountId,
        recipients: List<MessageRecipient>,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        type: MessageType = MessageType.MESSAGE,
        initialFolder: MessageThreadFolderId? = null,
        relatedApplicationId: ApplicationId? = null,
    ) =
        messageController.createMessage(
            dbInstance(),
            user,
            clock,
            sender,
            initialFolder,
            MessageController.PostMessageBody(
                title = "Juhannus/Midsommar/Midsummer",
                content = "Juhannus tulee pian",
                type = type,
                recipients = recipients.toSet(),
                recipientNames = listOf(),
                urgent = false,
                sensitive = false,
                relatedApplicationId = relatedApplicationId,
            ),
        )

    private fun getEmailFor(person: DevPerson): Email {
        val address = person.email ?: throw Error("$person has no email")
        return MockEmailClient.getEmail(address) ?: throw Error("No emails sent to $address")
    }

    private fun markAllRecipientMessagesRead(person: DevPerson, clock: EvakaClock) {
        db.transaction { tx ->
            tx.execute {
                sql(
                    """
UPDATE message_recipients mr SET read_at = ${bind(clock.now())}
WHERE mr.id IN (
    SELECT mr.id 
    FROM message_recipients mr LEFT JOIN message_account ma ON mr.recipient_id = ma.id 
    WHERE ma.person_id = ${bind(person.id)}
)
"""
                )
            }
        }
    }
}
