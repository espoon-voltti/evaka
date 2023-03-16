// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.webpush

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Headers
import fi.espoo.evaka.WebPushEnv
import fi.espoo.evaka.shared.domain.EvakaClock
import java.lang.RuntimeException
import java.net.URI
import java.security.SecureRandom
import java.security.interfaces.ECPublicKey
import java.time.Duration
import java.time.Instant

data class WebPushNotification(val endpoint: WebPushEndpoint, val ttl: Duration)

class WebPushEndpoint(val uri: URI, val ecdhPublicKey: ECPublicKey, val authSecret: ByteArray)

class WebPushRequest(
    val uri: URI,
    val headers: WebPushRequestHeaders,
    val body: ByteArray,
) {
    fun withVapid(keyPair: WebPushKeyPair, expiresAt: Instant) =
        WebPushRequest(
            uri,
            headers.copy(authorization = vapidAuthorizationHeader(keyPair, expiresAt, uri)),
            body,
        )
    companion object {
        // Message Encryption for Web Push (MEWP)
        // Reference: https://datatracker.ietf.org/doc/html/rfc8291
        fun createEncryptedPushMessage(
            ttl: Duration,
            endpoint: WebPushEndpoint,
            messageKeyPair: WebPushKeyPair,
            salt: ByteArray,
            data: ByteArray,
        ): WebPushRequest {
            val ikm =
                WebPushCrypto.generateInputKeyingMaterial(
                    userAgentPublicKey = endpoint.ecdhPublicKey,
                    authSecret = endpoint.authSecret,
                    applicationServerKeyPair = messageKeyPair,
                )
            return WebPushRequest(
                uri = endpoint.uri,
                headers =
                    WebPushRequestHeaders(
                        ttl = ttl.toSeconds().toString(),
                        contentEncoding = "aes128gcm"
                    ),
                body =
                    httpEncryptedContentEncoding(
                        recordSize = 4096u,
                        ikm = ikm,
                        keyId = WebPushCrypto.encode(messageKeyPair.publicKey),
                        salt = salt,
                        data = data,
                    )
            )
        }
    }
}

data class WebPushRequestHeaders(
    val ttl: String,
    val contentEncoding: String,
    val authorization: String? = null,
) {
    fun toTypedArray(): Array<Pair<String, String>> =
        listOfNotNull(
                "TTL" to ttl,
                "Content-Encoding" to contentEncoding,
                authorization?.let { "Authorization" to it },
            )
            .toTypedArray()
}

data class WebPushMessage(val uri: URI)

class WebPush(env: WebPushEnv) {
    private val fuel = FuelManager()
    private val secureRandom = SecureRandom()
    private val vapidKeyPair: WebPushKeyPair =
        WebPushKeyPair.fromPrivateKey(WebPushCrypto.decodePrivateKey(env.vapidPrivateKey.value))
    val applicationServerKey: String
        get() = vapidKeyPair.publicKeyBase64()

    class SubscriptionExpired(cause: Throwable) : RuntimeException("Subscription expired", cause)

    fun send(clock: EvakaClock, notification: WebPushNotification): WebPushMessage {
        val request =
            WebPushRequest.createEncryptedPushMessage(
                    ttl = notification.ttl,
                    endpoint = notification.endpoint,
                    messageKeyPair = WebPushCrypto.generateKeyPair(secureRandom),
                    salt = secureRandom.generateSeed(16),
                    data = "[]".toByteArray()
                )
                .withVapid(keyPair = vapidKeyPair, expiresAt = clock.now().plusHours(6).toInstant())
        try {
            val (_, res, result) =
                fuel
                    .post(request.uri.toString())
                    .header(*request.headers.toTypedArray())
                    .body(request.body)
                    .validate { it.statusCode == 201 } // Push server should return 201 Created
                    .response()
            result.get()
            val messageUri =
                res.header(Headers.LOCATION).firstOrNull()?.let(::URI)
                    ?: error("No location header in response")
            return WebPushMessage(messageUri)
        } catch (e: FuelError) {
            if (e.response.statusCode == 404 || e.response.statusCode == 410) {
                throw SubscriptionExpired(e)
            }
            throw e
        }
    }
}
