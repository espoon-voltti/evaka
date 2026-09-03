// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.webpush

import evaka.core.FullApplicationTest
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.auth.CitizenAuthLevel
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.MockEvakaClock
import java.net.URI
import java.security.SecureRandom
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class CitizenWebPushControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var controller: CitizenWebPushController

    private val keyPair = WebPushCrypto.generateKeyPair(SecureRandom())
    private val clock = MockEvakaClock(2026, 1, 1, 12, 0)

    private val adult = DevPerson()
    private val otherAdult = DevPerson()

    private val endpoint = URI("https://push.example.com/subscription/1234")
    private val userAgent =
        "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1"

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(adult, DevPersonType.ADULT)
            tx.insert(otherAdult, DevPersonType.ADULT)
        }
    }

    @Test
    fun `a subscribed device is listed with the details of the browser it was created in`() {
        val device = subscribe(adult)

        val settings = controller.getPushSettings(dbInstance(), user(adult), clock)
        assertEquals(listOf(device), settings.devices)
        assertEquals(true, device.installed)
        assertEquals("iOS", device.operatingSystemName)
        assertEquals("Safari", device.agentName)
        assertNull(device.lastSentAt)
    }

    @Test
    fun `a check by the owner returns the device`() {
        val device = subscribe(adult)

        assertEquals(
            device.id,
            controller
                .checkPushSubscription(
                    dbInstance(),
                    user(adult),
                    clock,
                    CitizenWebPushController.PushSubscriptionCheckRequest(endpoint),
                )
                .deviceId,
        )
    }

    @Test
    fun `a check by another person destroys the subscription`() {
        subscribe(adult)

        assertNull(
            controller
                .checkPushSubscription(
                    dbInstance(),
                    user(otherAdult),
                    clock,
                    CitizenWebPushController.PushSubscriptionCheckRequest(endpoint),
                )
                .deviceId
        )
        assertEquals(
            emptyList(),
            controller.getPushSettings(dbInstance(), user(adult), clock).devices,
        )
    }

    @Test
    fun `subscribing in a browser somebody else used takes the endpoint over`() {
        subscribe(otherAdult)

        val device = subscribe(adult)

        assertEquals(
            listOf(device),
            controller.getPushSettings(dbInstance(), user(adult), clock).devices,
        )
        assertEquals(
            emptyList(),
            controller.getPushSettings(dbInstance(), user(otherAdult), clock).devices,
        )
    }

    @Test
    fun `a device can be revoked`() {
        val device = subscribe(adult)

        controller.deletePushDevice(dbInstance(), user(adult), clock, device.id)

        assertEquals(
            emptyList(),
            controller.getPushSettings(dbInstance(), user(adult), clock).devices,
        )
    }

    private fun user(person: DevPerson) =
        AuthenticatedUser.Citizen(person.id, CitizenAuthLevel.WEAK)

    private fun subscribe(person: DevPerson): CitizenPushDevice =
        controller.addPushSubscription(
            dbInstance(),
            user(person),
            clock,
            userAgent,
            CitizenWebPushController.NewCitizenPushSubscription(
                WebPushSubscription(
                    endpoint = endpoint,
                    expires = null,
                    authSecret = listOf(0x00, 0x11, 0x22, 0x33),
                    ecdhKey = WebPushCrypto.encode(keyPair.publicKey).toList(),
                ),
                installed = true,
            ),
        )
}
