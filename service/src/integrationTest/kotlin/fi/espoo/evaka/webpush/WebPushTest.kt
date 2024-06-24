// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.webpush

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.Sensitive
import fi.espoo.evaka.WebPushEnv
import fi.espoo.evaka.shared.domain.MockEvakaClock
import java.net.URI
import java.time.Duration
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class WebPushTest : PureJdbiTest(resetDbBeforeEach = true) {
    private lateinit var clock: MockEvakaClock
    private val webPush =
        WebPush(
            WebPushEnv(vapidPrivateKey = Sensitive("eCCqlmasgp3hG9TB1W-mbDp_kEyXCxzxv6vwyRXK7y4"))
        )

    @BeforeEach
    fun beforeEach() {
        clock = MockEvakaClock(2023, 1, 1, 12, 0)
    }

    private fun getValidToken(uri: URI) = db.transaction { tx -> webPush.getValidToken(tx, clock, uri) }

    @Test
    fun `getValidToken returns the same JWT token if it's still valid`() {
        val uri = URI("http://example.com")
        val initialToken = getValidToken(uri)

        clock.tick(Duration.ofMinutes(5))
        assertEquals(initialToken, getValidToken(uri))

        clock.tick(Duration.ofHours(24))
        assertTrue(initialToken.expiresAt.isBefore(clock.now()))
        assertNotEquals(initialToken, getValidToken(uri))
    }

    @Test
    fun `getValidToken returns the same token for different URIs within the same origin`() {
        assertEquals(
            getValidToken(URI("http://example.com/first")),
            getValidToken(URI("http://example.com/second"))
        )
    }

    @Test
    fun `getValidToken returns a different JWT token for URIs with different origins`() {
        val com = getValidToken(URI("http://example.com"))
        val net = getValidToken(URI("http://example.net"))
        assertNotEquals(com.origin, net.origin)
        assertContentEquals(com.publicKey, net.publicKey) // our public key is still the same
        assertNotEquals(com.jwt, net.jwt)
    }
}
