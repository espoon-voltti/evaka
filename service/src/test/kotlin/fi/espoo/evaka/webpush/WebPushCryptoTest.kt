// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.webpush

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class WebPushCryptoTest {
    @Test
    fun `a private key can be converted to base64 format and the entire key pair restored from it`() {
        val keyPair = WebPushCrypto.generateKeyPair()
        val base64 = keyPair.privateKeyBase64()
        assertEquals(keyPair, WebPushKeyPair.fromPrivateKey(WebPushCrypto.decodePrivateKey(base64)))
    }

    @Test
    fun `a private key can be converted to base64 format and back`() {
        val keyPair = WebPushCrypto.generateKeyPair()
        val base64 = keyPair.privateKeyBase64()
        assertEquals(keyPair.privateKey, WebPushCrypto.decodePrivateKey(base64))
    }

    @Test
    fun `a public key can be converted to base64 format and back`() {
        val keyPair = WebPushCrypto.generateKeyPair()
        val base64 = keyPair.publicKeyBase64()
        assertEquals(keyPair.publicKey, WebPushCrypto.decodePublicKey(base64))
    }
}
