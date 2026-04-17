// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.messaging

import evaka.core.FullApplicationTest
import evaka.core.pis.service.insertGuardian
import evaka.core.shared.MessageAccountId
import evaka.core.shared.MobileDeviceId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevDaycareGroup
import evaka.core.shared.dev.DevDaycareGroupPlacement
import evaka.core.shared.dev.DevMobileDevice
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.security.PilotFeature
import evaka.core.webpush.MockWebPushEndpoint
import evaka.core.webpush.PushNotificationCategory
import evaka.core.webpush.WebPushCrypto
import evaka.core.webpush.WebPushSubscription
import evaka.core.webpush.upsertPushGroup
import evaka.core.webpush.upsertPushSubscription
import java.net.URI
import java.security.SecureRandom
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class MessagePushNotificationsTest : FullApplicationTest(resetDbBeforeEach = true) {
    private lateinit var clock: MockEvakaClock
    private val keyPair = WebPushCrypto.generateKeyPair(SecureRandom())

    @Autowired private lateinit var mockEndpoint: MockWebPushEndpoint
    @Autowired private lateinit var messageService: MessageService
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    private val area = DevCareArea()
    private val daycare =
        DevDaycare(
            areaId = area.id,
            enabledPilotFeatures = setOf(PilotFeature.MESSAGING, PilotFeature.PUSH_NOTIFICATIONS),
        )
    private val group = DevDaycareGroup(daycareId = daycare.id)
    private val device =
        DevMobileDevice(
            unitId = daycare.id,
            pushNotificationCategories = setOf(PushNotificationCategory.RECEIVED_MESSAGE),
        )
    private val citizen = DevPerson()
    private val child = DevPerson()

    private lateinit var groupAccount: MessageAccountId
    private lateinit var citizenAccount: MessageAccountId
    private lateinit var municipalAccount: MessageAccountId

    private val testMessage =
        NewMessageStub(title = "Test", content = "Test", urgent = false, sensitive = false)

    @BeforeAll
    override fun beforeAll() {
        super.beforeAll()
        clock = MockEvakaClock(2023, 1, 1, 12, 0)
    }

    @BeforeEach
    fun beforeEach() {
        mockEndpoint.clearData()
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
            groupAccount = tx.createDaycareGroupMessageAccount(group.id)
            tx.insert(device)
            tx.upsertPushGroup(clock.now(), device.id, group.id)
            tx.insert(citizen, DevPersonType.ADULT)
            citizenAccount = tx.getCitizenMessageAccount(citizen.id)
            tx.insert(child, DevPersonType.CHILD)
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
            municipalAccount = tx.createMunicipalMessageAccount()
        }
    }

    @Test
    fun `a push notification is sent when a citizen sends a message to a group`() {
        val endpoint = URI("http://localhost:$httpPort/public/mock-web-push/subscription/1234")
        upsertSubscription(device.id, endpoint)

        db.transaction { tx ->
            messageService.sendMessageAsCitizen(
                tx,
                clock.now(),
                sender = citizenAccount,
                recipients = setOf(groupAccount),
                children = emptySet(),
                msg = testMessage,
            )
        }
        clock.tick(Duration.ofMinutes(30))
        asyncJobRunner.runPendingJobsSync(clock)

        val request = mockEndpoint.getCapturedRequests("1234").single()
        assertEquals("normal", request.headers["urgency"])
        assertNotNull(request.headers["ttl"]?.toIntOrNull())
        assertTrue(request.headers["authorization"]?.startsWith("vapid") ?: false)
        assertEquals("aes128gcm", request.headers["content-encoding"])
        assertTrue(request.body.isNotEmpty())
    }

    @Test
    fun `a push notification is not sent for staff copies`() {
        val endpoint = URI("http://localhost:$httpPort/public/mock-web-push/subscription/1234")
        upsertSubscription(device.id, endpoint)

        val (contentId, _) =
            db.transaction { tx ->
                messageService.sendMessageAsEmployee(
                    tx,
                    AuthenticatedUser.SystemInternalUser,
                    clock.now(),
                    sender = municipalAccount,
                    type = MessageType.BULLETIN,
                    msg = testMessage,
                    recipients = setOf(MessageRecipient.Area(area.id)),
                    recipientNames = listOf("Ryhmä"),
                    attachments = emptySet(),
                    relatedApplication = null,
                    filters = null,
                )
            }
        assertNotNull(contentId)
        clock.tick(Duration.ofMinutes(30))
        asyncJobRunner.runPendingJobsSync(clock)
        clock.tick(Duration.ofMinutes(48))
        asyncJobRunner.runPendingJobsSync(clock)

        val copy =
            db.read { tx ->
                    tx.getMessageCopiesByAccount(groupAccount, pageSize = 20, page = 1).data
                }
                .single()
        assertEquals(municipalAccount, copy.senderId)
        assertEquals(testMessage.title, copy.title)
        assertEquals(0, mockEndpoint.getCapturedRequests("1234").size)
    }

    private fun upsertSubscription(device: MobileDeviceId, endpoint: URI) =
        db.transaction { tx ->
            tx.upsertPushSubscription(
                device,
                WebPushSubscription(
                    endpoint = endpoint,
                    expires = null,
                    ecdhKey = WebPushCrypto.encode(keyPair.publicKey).toList(),
                    authSecret = listOf(0x00, 0x11, 0x22, 0x33),
                ),
            )
        }
}
