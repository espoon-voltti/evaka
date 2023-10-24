// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevMobileDevice
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.security.PilotFeature
import fi.espoo.evaka.webpush.MockWebPushServer
import fi.espoo.evaka.webpush.WebPushCrypto
import fi.espoo.evaka.webpush.WebPushSubscription
import fi.espoo.evaka.webpush.upsertPushSubscription
import java.net.URI
import java.security.SecureRandom
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class MessagePushNotificationsTest : FullApplicationTest(resetDbBeforeEach = true) {
    private lateinit var mockServer: MockWebPushServer
    private lateinit var clock: MockEvakaClock
    private val keyPair = WebPushCrypto.generateKeyPair(SecureRandom())

    @Autowired private lateinit var messageService: MessageService
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    private lateinit var group: GroupId
    private lateinit var device: MobileDeviceId
    private lateinit var groupAccount: MessageAccountId
    private lateinit var citizenAccount: MessageAccountId

    @BeforeAll
    override fun beforeAll() {
        super.beforeAll()
        mockServer = MockWebPushServer.start()
        clock = MockEvakaClock(2023, 1, 1, 12, 0)
    }

    @BeforeEach
    fun beforeEach() {
        mockServer.clearData()
        db.transaction { tx ->
            val area = tx.insert(DevCareArea())
            val unit =
                tx.insert(
                    DevDaycare(
                        areaId = area,
                        enabledPilotFeatures = setOf(PilotFeature.PUSH_NOTIFICATIONS)
                    )
                )
            group = tx.insert(DevDaycareGroup(daycareId = unit))
            groupAccount = tx.createDaycareGroupMessageAccount(group)
            device = tx.insert(DevMobileDevice(unitId = unit))
            val citizen = tx.insert(DevPerson())
            citizenAccount = tx.createPersonMessageAccount(citizen)
        }
    }

    @AfterAll
    override fun afterAll() {
        super.afterAll()
        mockServer.close()
    }

    @Test
    fun `a push notification is sent when a citizen sends a message to a group`() {
        val endpoint = URI("http://localhost:${mockServer.port}/subscription/1234")
        db.transaction { tx ->
            tx.upsertPushSubscription(
                device,
                WebPushSubscription(
                    endpoint = endpoint,
                    expires = null,
                    ecdhKey = WebPushCrypto.encode(keyPair.publicKey).toList(),
                    authSecret = listOf(0x00, 0x11, 0x22, 0x33)
                )
            )
        }

        db.transaction { tx ->
            messageService.sendMessageAsCitizen(
                tx,
                clock.now(),
                sender = citizenAccount,
                recipients = setOf(groupAccount),
                children = emptySet(),
                msg =
                    NewMessageStub(
                        title = "Test",
                        content = "Test",
                        urgent = false,
                        sensitive = false
                    )
            )
        }
        clock.tick(Duration.ofMinutes(30))
        asyncJobRunner.runPendingJobsSync(clock)

        val request = mockServer.getCapturedRequests("1234").single()
        assertEquals("normal", request.headers["Urgency"])
        assertNotNull(request.headers["TTL"]?.toIntOrNull())
        assertTrue(request.headers["Authorization"]?.startsWith("vapid") ?: false)
        assertEquals("aes128gcm", request.headers["Content-Encoding"])
        assertTrue(request.body.isNotEmpty())
    }
}
