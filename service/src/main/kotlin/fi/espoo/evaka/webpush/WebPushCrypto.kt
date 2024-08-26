// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.webpush

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.math.BigInteger
import java.net.URI
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECParameterSpec
import java.security.spec.ECPrivateKeySpec
import java.security.spec.ECPublicKeySpec
import java.util.Base64
import java.util.Objects
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.bouncycastle.crypto.ec.CustomNamedCurves
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util
import org.bouncycastle.math.ec.ECPoint
import org.bouncycastle.util.BigIntegers

data class WebPushKeyPair(val publicKey: ECPublicKey, val privateKey: ECPrivateKey) {
    fun privateKeyBase64(): String = WebPushCrypto.base64Encode(WebPushCrypto.encode(privateKey))

    fun publicKeyBase64(): String = WebPushCrypto.base64Encode(WebPushCrypto.encode(publicKey))

    init {
        WebPushCrypto.validate(publicKey)
        WebPushCrypto.validate(privateKey)
    }

    companion object {
        fun fromPrivateKey(privateKey: ECPrivateKey): WebPushKeyPair =
            WebPushKeyPair(WebPushCrypto.derivePublicKey(privateKey), privateKey)
    }
}

// Voluntary Application Server Identification (VAPID) for Web Push
// Reference: https://datatracker.ietf.org/doc/html/rfc8292
data class VapidJwt(
    val origin: String,
    val publicKey: ByteArray,
    val jwt: String,
    val expiresAt: HelsinkiDateTime,
) {

    // 3. VAPID Authentication Scheme
    // Reference: https://datatracker.ietf.org/doc/html/rfc8292#section-3
    fun toAuthorizationHeader(): String = "vapid t=$jwt, k=${WebPushCrypto.base64Encode(publicKey)}"

    override fun equals(other: Any?): Boolean =
        (other as? VapidJwt)?.let {
            this.origin == it.origin &&
                this.publicKey.contentEquals(it.publicKey) &&
                this.jwt == it.jwt &&
                this.expiresAt == it.expiresAt
        } ?: false

    override fun hashCode(): Int = Objects.hash(origin, publicKey.contentHashCode(), jwt, expiresAt)

    companion object {
        private fun URI.origin(): String {
            val port = ":$port".takeIf { port != -1 } ?: ""
            return "$scheme://$host$port"
        }

        fun create(keyPair: WebPushKeyPair, expiresAt: HelsinkiDateTime, uri: URI) =
            VapidJwt(
                origin = uri.origin(),
                publicKey = WebPushCrypto.encode(keyPair.publicKey),
                // 2. Application Server Self-Identification
                // Reference: https://datatracker.ietf.org/doc/html/rfc8292#section-2
                jwt =
                    JWT.create()
                        .withAudience(uri.origin())
                        .withExpiresAt(expiresAt.toInstant())
                        .withSubject("https://github.com/espoon-voltti/evaka")
                        .sign(Algorithm.ECDSA256(keyPair.privateKey)),
                expiresAt = expiresAt,
            )
    }
}

// HTTP Encrypted Content Encoding (ECE)
// Reference: https://datatracker.ietf.org/doc/html/rfc8188
fun httpEncryptedContentEncoding(
    recordSize: UInt,
    ikm: ByteArray,
    keyId: ByteArray,
    salt: ByteArray,
    data: ByteArray,
): ByteArray {
    check(recordSize >= 18u)
    check(salt.size == 16)
    check(keyId.size < 256)
    check(data.size.toUInt() < recordSize - 17u)

    fun toNetworkByteOrder(value: UInt) =
        byteArrayOf(
            (value shr 24).toByte(),
            (value shr 16).toByte(),
            (value shr 8).toByte(),
            value.toByte(),
        )

    // 2.1. Encryption Content-Encoding Header
    // Reference: https://datatracker.ietf.org/doc/html/rfc8188#section-2.1
    val header =
        byteArrayOf(*salt, *toNetworkByteOrder(recordSize), keyId.size.toUByte().toByte(), *keyId)
    // 2.2. Content-Encryption Key Derivation
    // Reference: https://datatracker.ietf.org/doc/html/rfc8188#section-2.2
    val prk = WebPushCrypto.hmacSha256(salt, ikm)
    val cek =
        WebPushCrypto.hmacSha256(
                prk,
                byteArrayOf(*"Content-Encoding: aes128gcm".toByteArray(), 0x00, 0x01),
            )
            .sliceArray(0..15)
    // 2.3. Nonce Derivation
    // Reference: https://datatracker.ietf.org/doc/html/rfc8188#section-2.3
    val nonce =
        WebPushCrypto.hmacSha256(
                prk,
                byteArrayOf(*"Content-Encoding: nonce".toByteArray(), 0x00, 0x01),
            )
            .sliceArray(0..11)

    // 2. The "aes128gcm" HTTP Content Coding
    // Reference: https://datatracker.ietf.org/doc/html/rfc8188#section-2
    val paddingDelimiter: Byte =
        0x02 // we're doing a single record, so this is the "last record delimiter"
    val cipherText =
        Cipher.getInstance("AES/GCM/NoPadding")
            .apply {
                init(Cipher.ENCRYPT_MODE, SecretKeySpec(cek, "AES"), GCMParameterSpec(128, nonce))
            }
            .doFinal(byteArrayOf(*data, paddingDelimiter))
    return byteArrayOf(*header, *cipherText)
}

object WebPushCrypto {
    // P-256 curve (a.k.a secp256r1, a.k.a prime256v1)
    // Used by both VAPID (server authentication), and MEWP (message encryption)
    // Reference: RFC8291 (MEWP): https://datatracker.ietf.org/doc/html/rfc8291#section-3.1
    //   3.1. Diffie-Hellman Key Agreement
    // Reference: RFC8292 (VAPID): https://datatracker.ietf.org/doc/html/rfc8292#section-2
    //   2. Application Server Self-Identification
    private val domainParams = ECDomainParameters(CustomNamedCurves.getByName("P-256"))
    private val parameterSpec: ECParameterSpec = EC5Util.convertToSpec(domainParams)

    private fun keyPairGenerator(secureRandom: SecureRandom): KeyPairGenerator =
        KeyPairGenerator.getInstance("EC").apply { initialize(parameterSpec, secureRandom) }

    private fun keyFactory(): KeyFactory = KeyFactory.getInstance("EC")

    private fun ecdhKeyAgreement(): KeyAgreement = KeyAgreement.getInstance("ECDH")

    fun generateKeyPair(secureRandom: SecureRandom): WebPushKeyPair {
        val keyPair = keyPairGenerator(secureRandom).generateKeyPair()
        return WebPushKeyPair(
            publicKey = keyPair.public as ECPublicKey,
            privateKey = keyPair.private as ECPrivateKey,
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

    fun encode(key: ECPrivateKey): ByteArray = domainParams.curve.fromBigInteger(key.s).encoded

    fun decodePublicKey(base64: String) = decodePublicKey(base64Decode(base64))

    fun decodePublicKey(bytes: ByteArray): ECPublicKey =
        domainParams.curve.decodePoint(bytes).toPublicKey()

    fun decodePrivateKey(base64: String) = decodePrivateKey(base64Decode(base64))

    fun decodePrivateKey(bytes: ByteArray): ECPrivateKey =
        BigIntegers.fromUnsignedByteArray(bytes).toPrivateKey()

    fun derivePublicKey(privateKey: ECPrivateKey): ECPublicKey =
        // Derive public key point from private key value "s"
        // The standard Java API doesn't seem to have a way to do this easily, so we use Bouncy
        // Castle library utils
        domainParams.g.multiply(privateKey.s).toPublicKey()

    fun validate(key: ECPublicKey) {
        domainParams.validatePublicPoint(EC5Util.convertPoint(parameterSpec, key.w))
    }

    fun validate(key: ECPrivateKey) {
        domainParams.validatePrivateScalar(key.s)
    }

    private fun generateEcdhSecret(privateKey: ECPrivateKey, publicKey: ECPublicKey): ByteArray {
        val ecdh = ecdhKeyAgreement()
        ecdh.init(privateKey)
        ecdh.doPhase(publicKey, true)
        return ecdh.generateSecret()
    }

    fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val algorithm = "HmacSHA256"
        val hmac = Mac.getInstance(algorithm)
        hmac.init(SecretKeySpec(key, algorithm))
        return hmac.doFinal(data)
    }

    // Reference: RFC8291 (MEWP): https://datatracker.ietf.org/doc/html/rfc8291#section-3.3
    //   3.3. Combining Shared and Authentication Secrets
    fun generateInputKeyingMaterial(
        userAgentPublicKey: ECPublicKey,
        authSecret: ByteArray,
        applicationServerKeyPair: WebPushKeyPair,
    ): ByteArray {
        val ecdhSecret = generateEcdhSecret(applicationServerKeyPair.privateKey, userAgentPublicKey)
        val prkKey = hmacSha256(authSecret, ecdhSecret)
        return hmacSha256(
            prkKey,
            byteArrayOf(
                *"WebPush: info".toByteArray(),
                0x00,
                *encode(userAgentPublicKey),
                *encode(applicationServerKeyPair.publicKey),
                0x01,
            ),
        )
    }

    private fun ECPoint.toPublicKey(): ECPublicKey =
        keyFactory()
            .generatePublic(
                ECPublicKeySpec(
                    EC5Util.convertPoint(domainParams.validatePublicPoint(this)),
                    parameterSpec,
                )
            ) as ECPublicKey

    private fun BigInteger.toPrivateKey(): ECPrivateKey =
        keyFactory()
            .generatePrivate(
                ECPrivateKeySpec(domainParams.validatePrivateScalar(this), parameterSpec)
            ) as ECPrivateKey
}
