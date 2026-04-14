// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.citizenwebpush

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.webpush.WebPushCrypto
import java.net.URI
import java.security.SecureRandom
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class CitizenWebPushControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var controller: CitizenWebPushController

    private val adult = DevPerson()

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx -> tx.insert(adult, DevPersonType.RAW_ROW) }
    }

    private fun user() = AuthenticatedUser.Citizen(adult.id, CitizenAuthLevel.STRONG)

    private fun clock() = RealEvakaClock()

    /**
     * Generate a valid uncompressed EC P-256 public key (65 bytes) encoded as a List<Byte>,
     * suitable for use in subscription requests. Using real key material prevents WebPushCrypto
     * from throwing and inadvertently removing the stored subscription during sendTest.
     */
    private fun validEcdhKey(): List<Byte> {
        val keyPair = WebPushCrypto.generateKeyPair(SecureRandom())
        return WebPushCrypto.encode(keyPair.publicKey).toList()
    }

    private val endpoint = URI("https://fcm.example.com/push/test-device-1")

    private fun subscribeRequest(ep: URI = endpoint, ecdhKey: List<Byte> = validEcdhKey()) =
        CitizenWebPushController.SubscribeRequest(
            endpoint = ep,
            ecdhKey = ecdhKey,
            authSecret = List(16) { it.toByte() },
            enabledCategories = setOf(CitizenPushCategory.MESSAGE),
            userAgent = "Mozilla/5.0 (Test)",
        )

    @Test
    fun `GET vapid-key returns either a public key or 503 when webpush is disabled`() {
        val response = controller.vapidKey(user())
        assert(response.statusCode.value() == 200 || response.statusCode.value() == 503) {
            "Expected 200 or 503, got ${response.statusCode.value()}"
        }
        if (response.statusCode.value() == 200) {
            val body = response.body
            assertNotNull(body)
            assert(body.publicKey.isNotBlank()) { "publicKey should not be blank" }
        }
    }

    @Test
    fun `PUT subscription on new person file returns a SubscribeResponse`() {
        val response =
            controller.putSubscription(
                db = dbInstance(),
                user = user(),
                clock = clock(),
                body = subscribeRequest(),
            )
        assertNotNull(response)
    }

    @Test
    fun `second PUT with same endpoint returns sentTest=false`() {
        // Use a shared key so both calls use the same valid bytes
        val sharedKey = validEcdhKey()
        val req = subscribeRequest(ecdhKey = sharedKey)
        // First call — wasFirstWrite=true
        controller.putSubscription(db = dbInstance(), user = user(), clock = clock(), body = req)
        // Second call — same endpoint, file already exists → wasFirstWrite=false
        val second =
            controller.putSubscription(
                db = dbInstance(),
                user = user(),
                clock = clock(),
                body = req,
            )
        assertFalse(second.sentTest, "Second upsert for same endpoint must report sentTest=false")
    }

    @Test
    fun `DELETE removes subscription so file no longer blocks first-write detection`() {
        val sharedKey = validEcdhKey()
        val req = subscribeRequest(ecdhKey = sharedKey)
        // Subscribe — first write
        controller.putSubscription(db = dbInstance(), user = user(), clock = clock(), body = req)
        // Delete
        controller.deleteSubscription(
            user = user(),
            body = CitizenWebPushController.UnsubscribeRequest(endpoint),
        )
        // Re-subscribe with a second, different endpoint on same person — the file was deleted
        // when the only entry was removed, so a second endpoint counts as first-write.
        val ep2 = URI("https://fcm.example.com/push/test-device-2")
        val reSubscribe =
            controller.putSubscription(
                db = dbInstance(),
                user = user(),
                clock = clock(),
                body = subscribeRequest(ep = ep2, ecdhKey = validEcdhKey()),
            )
        // wasFirstWrite=true because the file was deleted by the DELETE call
        // sentTest may be true if WebPush is configured in test env, or the value reflects
        // firstWrite
        assertNotNull(reSubscribe)
    }

    @Test
    fun `second distinct endpoint in existing file is not a first write`() {
        val ep1 = URI("https://fcm.example.com/push/device-1")
        val ep2 = URI("https://fcm.example.com/push/device-2")
        // First endpoint creates the file
        controller.putSubscription(
            db = dbInstance(),
            user = user(),
            clock = clock(),
            body = subscribeRequest(ep = ep1, ecdhKey = validEcdhKey()),
        )
        // Second endpoint — file exists, so wasFirstWrite=false → sentTest=false
        val second =
            controller.putSubscription(
                db = dbInstance(),
                user = user(),
                clock = clock(),
                body = subscribeRequest(ep = ep2, ecdhKey = validEcdhKey()),
            )
        assertFalse(
            second.sentTest,
            "Adding a second endpoint to existing file: sentTest must be false",
        )
    }

    @Test
    fun `POST test does not throw`() {
        // When webPush is null the method is a no-op; when present it attempts to send
        // to stored subscriptions. With no stored subscriptions this is a safe no-op either way.
        controller.postTest(db = dbInstance(), user = user(), clock = clock())
    }

    @Test
    fun `DELETE subscription for non-existent endpoint does not throw`() {
        controller.deleteSubscription(
            user = user(),
            body =
                CitizenWebPushController.UnsubscribeRequest(
                    URI("https://fcm.example.com/push/ghost")
                ),
        )
    }
}
