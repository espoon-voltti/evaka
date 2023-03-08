// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.webpush

import java.math.BigInteger
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPrivateKeySpec
import java.security.spec.ECPublicKeySpec
import java.util.Base64
import org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util

data class WebPushKeyPair(val publicKey: ECPublicKey, val privateKey: ECPrivateKey) {
    fun privateKeyBase64(): String = WebPushCrypto.base64Encode(WebPushCrypto.encode(privateKey))
    fun publicKeyBase64(): String = WebPushCrypto.base64Encode(WebPushCrypto.encode(publicKey))
    init {
        assert(privateKey.params == publicKey.params)
    }

    companion object {
        fun fromPrivateKey(privateKey: ECPrivateKey): WebPushKeyPair =
            WebPushKeyPair(
                WebPushCrypto.derivePublicKey(privateKey),
                privateKey,
            )
    }
}

object WebPushCrypto {
    // P-256 curve (a.k.a secp256r1, a.k.a prime256v1)
    // Used by both VAPID (server authentication), and MEWP (message encryption)
    // Reference: RFC8291 (MEWP): https://datatracker.ietf.org/doc/html/rfc8291#section-3.1
    // 3.1. Diffie-Hellman Key Agreement
    // Reference: RFC8292 (VAPID): https://datatracker.ietf.org/doc/html/rfc8292#section-2
    // 2. Application Server Self-Identification
    private val curveSpec: ECGenParameterSpec = ECGenParameterSpec("secp256r1")
    private val parameterSpec: ECParameterSpec =
        AlgorithmParameters.getInstance("EC")
            .apply { init(curveSpec) }
            .getParameterSpec(ECParameterSpec::class.java)
    private fun keyPairGenerator(): KeyPairGenerator =
        KeyPairGenerator.getInstance("EC").apply { initialize(curveSpec) }
    private fun keyFactory(): KeyFactory = KeyFactory.getInstance("EC")

    fun generateKeyPair(): WebPushKeyPair {
        val keyPair = keyPairGenerator().generateKeyPair()
        return WebPushKeyPair(
            publicKey = keyPair.public as ECPublicKey,
            privateKey = keyPair.private as ECPrivateKey
        )
    }

    // References:
    //   - RFC8292 (VAPID): https://datatracker.ietf.org/doc/html/rfc8292#section-3.2
    //     3.2. Public Key Parameter ("k")
    //   - RFC7515 (JWS): https://datatracker.ietf.org/doc/html/rfc7515#section-2
    //     2. Terminology - Base64url Encoding
    fun base64Encode(bytes: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    fun base64Decode(base64: String): ByteArray = Base64.getUrlDecoder().decode(base64)

    fun encode(key: ECPublicKey): ByteArray =
        EC5Util.convertPoint(parameterSpec, key.w).getEncoded(false)
    fun encode(key: ECPrivateKey): ByteArray = key.s.toByteArray()

    fun decodePublicKey(base64: String) = decodePublicKey(base64Decode(base64))
    fun decodePublicKey(bytes: ByteArray): ECPublicKey {
        val spec =
            ECPublicKeySpec(
                EC5Util.convertPoint(EC5Util.convertSpec(parameterSpec).curve.decodePoint(bytes)),
                parameterSpec
            )
        return keyFactory().generatePublic(spec) as ECPublicKey
    }

    fun decodePrivateKey(base64: String) = decodePrivateKey(base64Decode(base64))
    fun decodePrivateKey(bytes: ByteArray): ECPrivateKey {
        val spec = ECPrivateKeySpec(BigInteger(bytes), parameterSpec)
        return keyFactory().generatePrivate(spec) as ECPrivateKey
    }

    fun derivePublicKey(privateKey: ECPrivateKey): ECPublicKey {
        // Derive public key point "w" from private key value "s"
        // The standard Java API doesn't seem to have a way to do this easily, so we use Bouncy
        // Castle library utils
        val w = EC5Util.convertPoint(EC5Util.convertSpec(parameterSpec).g.multiply(privateKey.s))
        return keyFactory().generatePublic(ECPublicKeySpec(w, parameterSpec)) as ECPublicKey
    }
}
