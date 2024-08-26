// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.webpush

import java.net.URI
import java.security.SecureRandom
import java.time.Duration
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

    @Test
    fun `RFC8188 example is encrypted correctly`() {
        // Reference: RFC8188 (ECE): https://datatracker.ietf.org/doc/html/rfc8188#section-3.1
        //   3.1. Encryption of a Response
        val data = "I am the walrus".toByteArray()
        val body =
            httpEncryptedContentEncoding(
                recordSize = 4096u,
                ikm = WebPushCrypto.base64Decode("yqdlZ-tYemfogSmv7Ws5PQ"),
                keyId = byteArrayOf(),
                salt = WebPushCrypto.base64Decode("I1BsxtFttlv3u_Oo94xnmw"),
                data = data,
            )
        assertEquals(
            """
I1BsxtFttlv3u_Oo94xnmwAAEAAA-NAVub2qFgBEuQKRapoZu-IxkIva3MEB1PD-
ly8Thjg 
"""
                .removeWhitespace(),
            WebPushCrypto.base64Encode(body),
        )
    }

    @Test
    fun `RFC8291 example is encrypted correctly`() {
        // Reference: RFC8291 (MEWP): https://datatracker.ietf.org/doc/html/rfc8291#section-5
        //   5. Push Message Encryption Example
        val data = "When I grow up, I want to be a watermelon".toByteArray()

        val asKeyPair =
            WebPushKeyPair(
                WebPushCrypto.decodePublicKey(
                    """
BP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIg
Dll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A8
"""
                        .removeWhitespace()
                ),
                WebPushCrypto.decodePrivateKey("yfWPiYE-n46HLnH0KqZOF1fJJU3MYrct3AELtAQ-oRw"),
            )
        val uaKeyPair =
            WebPushKeyPair(
                WebPushCrypto.decodePublicKey(
                    """
BCVxsr7N_eNgVRqvHtD0zTZsEc6-VV-
JvLexhqUzORcx aOzi6-AYWXvTBHm4bjyPjs7Vd8pZGH6SRpkNtoIAiw4
"""
                        .removeWhitespace()
                ),
                WebPushCrypto.decodePrivateKey("q1dXpw3UpT5VOmu_cf_v6ih07Aems3njxI-JWgLcM94"),
            )
        assertEquals(asKeyPair.publicKey, WebPushCrypto.derivePublicKey(asKeyPair.privateKey))
        assertEquals(uaKeyPair.publicKey, WebPushCrypto.derivePublicKey(uaKeyPair.privateKey))

        val endpoint =
            WebPushEndpoint(
                uri = URI("https://push.example.net/push/JzLQ3raZJfFBR0aqvOMsLrt54w4rJUsV"),
                ecdhPublicKey = uaKeyPair.publicKey,
                authSecret = WebPushCrypto.base64Decode("BTBZMqHH6r4Tts7J_aSIgg"),
            )
        val request =
            WebPushRequest.createEncryptedPushMessage(
                ttl = Duration.ofSeconds(10),
                urgency = Urgency.Normal,
                endpoint = endpoint,
                messageKeyPair = asKeyPair,
                salt = WebPushCrypto.base64Decode("DGv6ra1nlYgDCS1FRnbzlw"),
                data = data,
            )
        assertEquals(endpoint.uri, request.uri)
        assertEquals("10", request.headers.ttl)
        assertEquals("aes128gcm", request.headers.contentEncoding)
        assertEquals(
            """
DGv6ra1nlYgDCS1FRnbzlwAAEABBBP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27ml
mlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A_yl95bQpu6cVPT
pK4Mqgkf1CXztLVBSt2Ks3oZwbuwXPXLWyouBWLVWGNWQexSgSxsj_Qulcy4a-fN
"""
                .removeWhitespace(),
            WebPushCrypto.base64Encode(request.body),
        )
    }

    private fun String.removeWhitespace() = replace(Regex("""\s*"""), "")
}
