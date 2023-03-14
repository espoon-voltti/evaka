// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.webpush

import java.security.SecureRandom
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class WebPushCryptoTest {
    private val secureRandom = SecureRandom()
    @Test
    fun `a private key can be converted to base64 format and the entire key pair restored from it`() {
        val keyPair = WebPushCrypto.generateKeyPair(secureRandom)
        val base64 = keyPair.privateKeyBase64()
        assertEquals(keyPair, WebPushKeyPair.fromPrivateKey(WebPushCrypto.decodePrivateKey(base64)))
    }

    @Test
    fun `a private key can be converted to base64 format and back`() {
        val keyPair = WebPushCrypto.generateKeyPair(secureRandom)
        val base64 = keyPair.privateKeyBase64()
        assertEquals(keyPair.privateKey, WebPushCrypto.decodePrivateKey(base64))
    }

    @Test
    fun `a public key can be converted to base64 format and back`() {
        val keyPair = WebPushCrypto.generateKeyPair(secureRandom)
        val base64 = keyPair.publicKeyBase64()
        assertEquals(keyPair.publicKey, WebPushCrypto.decodePublicKey(base64))
    }

    @Test
    fun `the exactly same key pair can be derived from just the private key`() {
        val keyPair = WebPushCrypto.generateKeyPair(secureRandom)
        val derived = WebPushKeyPair.fromPrivateKey(keyPair.privateKey)
        assertEquals(keyPair, derived)
        assertEquals(keyPair.publicKeyBase64(), derived.publicKeyBase64())
        assertEquals(keyPair.privateKeyBase64(), derived.privateKeyBase64())
    }
}
