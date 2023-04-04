// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.webpush

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import fi.espoo.evaka.WebPushEnv
import fi.espoo.evaka.shared.config.defaultJsonMapperBuilder
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.voltti.logging.loggers.error
import java.lang.RuntimeException
import java.net.URI
import java.security.SecureRandom
import java.security.interfaces.ECPublicKey
import java.time.Duration
import java.time.Instant
import mu.KotlinLogging

data class WebPushNotification(
    val endpoint: WebPushEndpoint,
    val ttl: Duration,
    val payloads: List<WebPushPayload>
)

enum class WebPushPayloadType {
    NotificationV1,
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
sealed class WebPushPayload(val type: WebPushPayloadType) {
    data class NotificationV1(val title: String) :
        WebPushPayload(WebPushPayloadType.NotificationV1)
}

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

class WebPush(env: WebPushEnv) {
    private val fuel = FuelManager()
    private val secureRandom = SecureRandom()
    private val jsonMapper = defaultJsonMapperBuilder().build()
    private val vapidKeyPair: WebPushKeyPair =
        WebPushKeyPair.fromPrivateKey(WebPushCrypto.decodePrivateKey(env.vapidPrivateKey.value))
    val applicationServerKey: String
        get() = vapidKeyPair.publicKeyBase64()

    private val logger = KotlinLogging.logger {}

    class SubscriptionExpired(cause: Throwable) : RuntimeException("Subscription expired", cause)

    fun send(clock: EvakaClock, notification: WebPushNotification) {
        val webPushRequest =
            WebPushRequest.createEncryptedPushMessage(
                    ttl = notification.ttl,
                    endpoint = notification.endpoint,
                    messageKeyPair = WebPushCrypto.generateKeyPair(secureRandom),
                    salt = secureRandom.generateSeed(16),
                    data = jsonMapper.writeValueAsBytes(notification.payloads)
                )
                .withVapid(keyPair = vapidKeyPair, expiresAt = clock.now().plusHours(6).toInstant())
        val (request, _, result) =
            fuel
                .post(webPushRequest.uri.toString())
                .header(*webPushRequest.headers.toTypedArray())
                .body(webPushRequest.body)
                // Push server should return 201 Created, but at least Mozilla returns 200 OK
                // instead
                .validate { it.statusCode == 200 || it.statusCode == 201 }
                .response()
        try {
            result.get()
        } catch (e: FuelError) {
            val meta =
                mapOf(
                    "method" to request.method,
                    "url" to request.url,
                    "body" to request.body.asString(contentType = null),
                )
            logger.error(e, meta) { "Web push failed, status ${e.response.statusCode}" }
            if (e.response.statusCode == 404 || e.response.statusCode == 410) {
                throw SubscriptionExpired(e)
            }
            throw e
        }
    }
}
