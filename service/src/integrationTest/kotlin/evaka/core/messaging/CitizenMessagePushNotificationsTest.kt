// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.messaging

import evaka.core.FullApplicationTest
import evaka.core.pis.NotificationCategory
import evaka.core.pis.service.insertGuardian
import evaka.core.pis.updateDisabledPushTypes
import evaka.core.shared.MessageAccountId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.auth.UserRole
import evaka.core.shared.auth.insertDaycareAclRow
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevDaycareGroup
import evaka.core.shared.dev.DevDaycareGroupPlacement
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.security.PilotFeature
import evaka.core.user.DeviceClass
import evaka.core.user.ParsedUserAgent
import evaka.core.webpush.MockWebPushEndpoint
import evaka.core.webpush.WebPushCrypto
import evaka.core.webpush.WebPushSubscription
import evaka.core.webpush.getCitizenPushDevices
import evaka.core.webpush.insertCitizenPushSubscription
import java.net.URI
import java.security.SecureRandom
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class CitizenMessagePushNotificationsTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>
    @Autowired private lateinit var messageController: MessageController
    @Autowired private lateinit var mockEndpoint: MockWebPushEndpoint

    private val keyPair = WebPushCrypto.generateKeyPair(SecureRandom())
    private val clock = MockEvakaClock(2026, 1, 1, 12, 0)

    private val area = DevCareArea()
    private val daycare =
        DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.MESSAGING))
    private val group = DevDaycareGroup(daycareId = daycare.id)
    private val child = DevPerson()
    private val citizen = DevPerson()
    private val employee = DevEmployee()

    private lateinit var employeeAccount: MessageAccountId

    @BeforeEach
    fun beforeEach() {
        mockEndpoint.clearData()
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(citizen, DevPersonType.ADULT)
            tx.insertGuardian(guardianId = citizen.id, childId = child.id)
            val placement =
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = clock.today(),
                    endDate = clock.today().plusYears(1),
                )
            tx.insert(placement)
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placement.id,
                    daycareGroupId = group.id,
                    startDate = placement.startDate,
                    endDate = placement.endDate,
                )
            )
            tx.insert(employee)
            employeeAccount = tx.upsertEmployeeMessageAccount(employee.id)
            tx.insertDaycareAclRow(daycare.id, employee.id, UserRole.STAFF)
        }
    }

    @Test
    fun `a push notification is sent about a new message`() {
        subscribe(URI("http://localhost:$httpPort/public/mock-web-push/subscription/1234"))

        sendMessageToChild()

        val request = mockEndpoint.getCapturedRequests("1234").single()
        assertEquals("aes128gcm", request.headers["content-encoding"])
        assertNotNull(db.read { it.getCitizenPushDevices(citizen.id) }.single().lastSentAt)
    }

    @Test
    fun `a push notification is not sent when the citizen has disabled the category`() {
        subscribe(URI("http://localhost:$httpPort/public/mock-web-push/subscription/1234"))
        db.transaction { tx ->
            tx.updateDisabledPushTypes(
                citizen.id,
                setOf(NotificationCategory.MESSAGE_NOTIFICATION),
            )
        }

        sendMessageToChild()

        assertEquals(emptyList(), mockEndpoint.getCapturedRequests("1234"))
        assertNull(db.read { it.getCitizenPushDevices(citizen.id) }.single().lastSentAt)
    }

    @Test
    fun `a subscription the push service reports gone is deleted`() {
        subscribe(URI("http://push-service.invalid/subscription/1234"))

        sendMessageToChild()

        assertEquals(emptyList(), db.read { it.getCitizenPushDevices(citizen.id) })
    }

    private fun subscribe(endpoint: URI) = db.transaction { tx ->
        tx.insertCitizenPushSubscription(
            citizen.id,
            WebPushSubscription(
                endpoint = endpoint,
                expires = null,
                authSecret = listOf(0x00, 0x11, 0x22, 0x33),
                ecdhKey = WebPushCrypto.encode(keyPair.publicKey).toList(),
            ),
            installed = true,
            client = ParsedUserAgent(DeviceClass.PHONE, "iOS", "Safari"),
        )
    }

    private fun sendMessageToChild() {
        messageController.createMessage(
            dbInstance(),
            AuthenticatedUser.Employee(employee.id, setOf(UserRole.STAFF)),
            clock,
            employeeAccount,
            null,
            MessageController.PostMessageBody(
                title = "Juhannus",
                content = "Juhannus tulee pian",
                type = MessageType.MESSAGE,
                recipients = setOf(MessageRecipient.Child(child.id)),
                recipientNames = listOf(),
                urgent = false,
                sensitive = false,
                relatedApplicationId = null,
            ),
        )
        asyncJobRunner.runPendingJobsSync(MockEvakaClock(clock.now().plusSeconds(5)))
    }
}
