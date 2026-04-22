// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.sficlient.rest

import evaka.core.FullApplicationTest
import evaka.core.Sensitive
import evaka.core.SfiContactOrganizationEnv
import evaka.core.SfiContactPersonEnv
import evaka.core.SfiEnv
import evaka.core.SfiPrintingEnv
import evaka.core.s3.Document
import evaka.core.sficlient.SfiMessage
import evaka.core.shared.SfiMessageId
import evaka.core.shared.config.defaultJsonMapperBuilder
import java.net.URI
import java.util.UUID
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SfiMessagesRestClientIntegrationTest : FullApplicationTest(resetDbBeforeEach = false) {
    private fun createEnv() =
        SfiEnv(
            serviceIdentifier = "espoo_ws_vaka",
            printing =
                SfiPrintingEnv(
                    billingId = "billing-username",
                    billingPassword = Sensitive("billing-password"),
                ),
            contactPerson = SfiContactPersonEnv(email = "test@example.com"),
            contactOrganization =
                SfiContactOrganizationEnv(
                    name = "eVaka Espoo",
                    streetAddress = "Karaportti 1 PL 302",
                    postalCode = "02070",
                    postOffice = "Espoo",
                ),
            restAddress = URI.create("http://localhost:$httpPort/public/mock-sfi-messages"),
            restUsername = MockSfiMessagesRestEndpoint.USERNAME,
            restPasswordSsmName = null,
        )

    private val message =
        SfiMessage(
            messageId = SfiMessageId(UUID.randomUUID()),
            documentId = "document-id",
            documentBucket = "document-bucket",
            documentKey = "document-key",
            documentDisplayName = "test.pdf",
            ssn = "010280-952L",
            firstName = "Tessa",
            lastName = "Testilä",
            streetAddress = "Henriksvägen 14 B",
            postalCode = "00370",
            postOffice = "HELSINGFORS",
            countryCode = "FI",
            messageHeader = "message-header",
            messageContent = "message-content",
            emailHeader = "message-header",
            emailContent = "message-content",
        )
    private val fileContent = byteArrayOf(1, 2, 3, 4)
    private lateinit var sfiEnv: SfiEnv
    private lateinit var client: SfiMessagesRestClient

    @BeforeEach
    fun beforeEach() {
        MockSfiMessagesRestEndpoint.reset()
        val fileContent = byteArrayOf(1, 2, 3, 4)
        sfiEnv = createEnv()
        client =
            SfiMessagesRestClient(
                sfiEnv,
                getDocument = { location ->
                    assertEquals(message.documentBucket, location.bucket)
                    assertEquals(message.documentKey, location.key)
                    Document(location.key, fileContent, contentType = "content-type")
                },
                passwordStore =
                    MockPasswordStore(
                        initialPassword = MockSfiMessagesRestEndpoint.DEFAULT_PASSWORD
                    ),
            )
    }

    @Test
    fun `it sends a sfi message successfully`() {
        client.send(message)

        val fileId =
            MockSfiMessagesRestEndpoint.getCapturedFiles().entries.let {
                assertEquals(1, it.size)
                assertEquals(message.documentDisplayName, it.first().value.name)
                assertContentEquals(fileContent, it.first().value.content)
                it.first().key
            }
        MockSfiMessagesRestEndpoint.getCapturedMessages().entries.let {
            assertEquals(1, it.size)
            assertEquals(message.messageId.toString(), it.first().key)
            val (_, captured) = it.first().value
            assertEquals(
                MultichannelMessageRequestBody(
                    externalId = message.messageId.toString(),
                    electronic =
                        ElectronicPart(
                            title = message.messageHeader,
                            body = message.messageContent,
                            attachments = listOf(AttachmentReference(fileId)),
                            bodyFormat = BodyFormat.TEXT,
                            messageServiceType = MessageServiceType.NORMAL,
                            notifications =
                                MessageNotifications(
                                    senderDetailsInNotifications =
                                        SenderDetailsInNotifications.ORGANIZATION_AND_SERVICE_NAME,
                                    unreadMessageNotification =
                                        UnreadMessageNotification(
                                            reminder = Reminder.DEFAULT_REMINDER
                                        ),
                                ),
                            replyAllowedBy = ReplyAllowedBy.NO_ONE,
                            visibility = Visibility.RECIPIENT_ONLY,
                        ),
                    paperMail =
                        PaperMailPart(
                            createAddressPage = true,
                            attachments = listOf(AttachmentReference(fileId)),
                            printingAndEnvelopingService =
                                PrintingAndEnvelopingService(
                                    PostiMessaging(
                                        contactDetails =
                                            ContactDetails(sfiEnv.contactPerson.email!!),
                                        username = sfiEnv.printing.billingId,
                                        password = sfiEnv.printing.billingPassword!!.value,
                                    )
                                ),
                            recipient =
                                NewPaperMailRecipient(
                                    Address(
                                        name = "${message.lastName} ${message.firstName}",
                                        streetAddress = message.streetAddress,
                                        zipCode = message.postalCode,
                                        city = message.postOffice,
                                    )
                                ),
                            sender =
                                NewPaperMailSender(
                                    Address(
                                        name = sfiEnv.contactOrganization.name!!,
                                        streetAddress = sfiEnv.contactOrganization.streetAddress!!,
                                        zipCode = sfiEnv.contactOrganization.postalCode!!,
                                        city = sfiEnv.contactOrganization.postOffice!!,
                                    )
                                ),
                            colorPrinting = true,
                            messageServiceType = MessageServiceType.NORMAL,
                            rotateLandscapePages = false,
                            twoSidedPrinting = true,
                        ),
                    recipient = Recipient(id = message.ssn),
                    sender = Sender(serviceId = sfiEnv.serviceIdentifier),
                ),
                captured,
            )
        }
    }

    @Test
    fun `it sends a sfi message with costPool when configured`() {
        val costPoolValue = "test-cost-pool-123"
        val envWithCostPool =
            createEnv().let {
                it.copy(printing = it.printing.copy(costPool = costPoolValue))
            }
        val costPoolClient =
            SfiMessagesRestClient(
                envWithCostPool,
                getDocument = { location ->
                    assertEquals(message.documentBucket, location.bucket)
                    assertEquals(message.documentKey, location.key)
                    Document(location.key, fileContent, contentType = "content-type")
                },
                passwordStore =
                    MockPasswordStore(
                        initialPassword = MockSfiMessagesRestEndpoint.DEFAULT_PASSWORD
                    ),
            )

        costPoolClient.send(message)

        MockSfiMessagesRestEndpoint.getCapturedMessages().entries.let {
            assertEquals(1, it.size)
            val (_, captured) = it.first().value
            assertEquals(
                costPoolValue,
                captured.paperMail.printingAndEnvelopingService.costPool,
            )
        }
    }

    @Test
    fun `it sends a sfi message without costPool when not configured`() {
        client.send(message)

        MockSfiMessagesRestEndpoint.getCapturedMessages().entries.let {
            assertEquals(1, it.size)
            val (_, captured) = it.first().value
            assertEquals(
                null,
                captured.paperMail.printingAndEnvelopingService.costPool,
            )
        }
    }

    @Test
    fun `it handles duplicate message status 409 correctly`() {
        client.send(message)
        assertEquals(1, MockSfiMessagesRestEndpoint.getCapturedMessages().size)
        client.send(message)
        assertEquals(1, MockSfiMessagesRestEndpoint.getCapturedMessages().size)
    }

    @Test
    fun `it handles access token expiry gracefully`() {
        client.send(message)
        assertEquals(1, MockSfiMessagesRestEndpoint.getCapturedMessages().size)
        MockSfiMessagesRestEndpoint.clearTokens()
        client.send(message.copy(messageId = SfiMessageId(UUID.randomUUID())))
        assertEquals(2, MockSfiMessagesRestEndpoint.getCapturedMessages().size)
    }

    @Test
    fun `changing password works`() {
        val oldPassword = MockSfiMessagesRestEndpoint.getCurrentPassword()
        client.rotatePassword()
        val newPassword = MockSfiMessagesRestEndpoint.getCurrentPassword()
        assertNotEquals(oldPassword, newPassword)

        // sending a message should still work after password change
        client.send(message)
        assertEquals(1, MockSfiMessagesRestEndpoint.getCapturedMessages().size)
    }

    @Test
    fun `password change handles access token expiry gracefully`() {
        client.send(message)
        assertEquals(1, MockSfiMessagesRestEndpoint.getCapturedMessages().size)
        MockSfiMessagesRestEndpoint.clearTokens()
        val oldPassword = MockSfiMessagesRestEndpoint.getCurrentPassword()
        client.rotatePassword()
        val newPassword = MockSfiMessagesRestEndpoint.getCurrentPassword()
        assertNotEquals(oldPassword, newPassword)

        // sending a message should still work after password change
        client.send(message)
        assertEquals(1, MockSfiMessagesRestEndpoint.getCapturedMessages().size)
    }

    @Test
    fun `costPool is omitted from JSON when null`() {
        val jsonMapper = defaultJsonMapperBuilder().build()
        val service =
            PrintingAndEnvelopingService(
                postiMessaging =
                    PostiMessaging(
                        contactDetails = ContactDetails(email = "test@example.com"),
                        username = "billing-username",
                        password = "billing-password",
                    )
            )
        val json = jsonMapper.writeValueAsString(service)

        assertFalse(json.contains("costPool"), "costPool should not appear in JSON when null")
        assertTrue(json.contains("postiMessaging"))
    }

    @Test
    fun `costPool is included in JSON when set`() {
        val jsonMapper = defaultJsonMapperBuilder().build()
        val service =
            PrintingAndEnvelopingService(
                postiMessaging =
                    PostiMessaging(
                        contactDetails = ContactDetails(email = "test@example.com"),
                        username = "billing-username",
                        password = "billing-password",
                    ),
                costPool = "my-cost-pool",
            )
        val json = jsonMapper.writeValueAsString(service)

        assertTrue(json.contains("\"costPool\""), "costPool should appear in JSON when set")
        assertTrue(json.contains("\"my-cost-pool\""))
    }

    @Test
    fun `costPool round-trips through serialization`() {
        val jsonMapper = defaultJsonMapperBuilder().build()
        val service =
            PrintingAndEnvelopingService(
                postiMessaging =
                    PostiMessaging(
                        contactDetails = ContactDetails(email = "test@example.com"),
                        username = "billing-username",
                        password = "billing-password",
                    ),
                costPool = "department-42",
            )
        val json = jsonMapper.writeValueAsString(service)
        val deserialized = jsonMapper.readValue(json, PrintingAndEnvelopingService::class.java)
        assertEquals(service, deserialized)
    }

    @Test
    fun `costPool defaults to null when absent in JSON`() {
        val jsonMapper = defaultJsonMapperBuilder().build()
        val service =
            PrintingAndEnvelopingService(
                postiMessaging =
                    PostiMessaging(
                        contactDetails = ContactDetails(email = "test@example.com"),
                        username = "billing-username",
                        password = "billing-password",
                    )
            )
        val json = jsonMapper.writeValueAsString(service)
        val deserialized = jsonMapper.readValue(json, PrintingAndEnvelopingService::class.java)
        assertEquals(null, deserialized.costPool)
    }
}
