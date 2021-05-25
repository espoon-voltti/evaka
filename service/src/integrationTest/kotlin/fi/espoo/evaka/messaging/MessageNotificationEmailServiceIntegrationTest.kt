package fi.espoo.evaka.messaging

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.core.isSuccessful
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.emailclient.MockEmail
import fi.espoo.evaka.emailclient.MockEmailClient
import fi.espoo.evaka.messaging.message.MessageAccount
import fi.espoo.evaka.messaging.message.MessageController
import fi.espoo.evaka.messaging.message.MessageType
import fi.espoo.evaka.messaging.message.createMessageAccountForPerson
import fi.espoo.evaka.messaging.message.getMessageAccountForEndUser
import fi.espoo.evaka.messaging.message.getMessageAccountsForEmployee
import fi.espoo.evaka.messaging.message.upsertMessageAccountForEmployee
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.shared.dev.insertTestPerson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class MessageNotificationEmailServiceIntegrationTest : FullApplicationTest() {
    @Autowired
    lateinit var asyncJobRunner: AsyncJobRunner

    private val testPersonFi = DevPerson(email = "fi@example.com", language = "fi")
    private val testPersonSv = DevPerson(email = "sv@example.com", language = "sv")
    private val testPersonEn = DevPerson(email = "en@example.com", language = "en")
    private val testPersonNoEmail = DevPerson(email = null, language = "fi")

    private val testPersons = listOf(testPersonFi, testPersonSv, testPersonEn, testPersonNoEmail)
    private val testAddresses = testPersons.mapNotNull { it.email }

    private val employeeId: UUID = UUID.randomUUID()
    private val employee = AuthenticatedUser.Employee(id = employeeId, roles = setOf(UserRole.UNIT_SUPERVISOR))

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.resetDatabase()

            testPersons.forEach {
                tx.insertTestPerson(it)
                tx.createMessageAccountForPerson(it.id)
            }

            tx.insertTestEmployee(DevEmployee(id = employeeId))
            tx.upsertMessageAccountForEmployee(employeeId)
        }

        MockEmailClient.emails.clear()
    }

    @Test
    fun `notifications are sent to citizens`() {
        val employeeAccount = db.read { it.getMessageAccountsForEmployee(employee).first() }
        val personAccounts = db.read { tx ->
            testPersons.map {
                tx.getMessageAccountForEndUser(it.id)
            }
        }

        postNewThread(
            title = "Juhannus",
            message = "Juhannus tulee pian",
            messageType = MessageType.MESSAGE,
            sender = employeeAccount.id,
            recipients = personAccounts,
            user = employee,
        )
        asyncJobRunner.runPendingJobsSync()

        assertEquals(
            testAddresses.toSet(),
            MockEmailClient.emails.map { it.toAddress }.toSet()
        )
        assertEquals("Uusi viesti eVakassa [null]", getEmailFor(testPersonFi).subject)
        assertEquals("Ny meddelande i eVaka [null]", getEmailFor(testPersonSv).subject)
        assertEquals("New message in eVaka [null]", getEmailFor(testPersonEn).subject)
    }

    private fun postNewThread(
        title: String,
        message: String,
        messageType: MessageType,
        sender: UUID,
        recipients: List<MessageAccount>,
        user: AuthenticatedUser.Employee,
    ) {
        val (_, response) = http.post("/messages/$sender")
            .jsonBody(
                objectMapper.writeValueAsString(
                    MessageController.PostMessageBody(
                        title,
                        message,
                        messageType,
                        recipientAccountIds = recipients.map { it.id }.toSet(),
                        recipientNames = recipients.map { it.name },
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
