// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.webpush

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import fi.espoo.evaka.WebPushEnv
import fi.espoo.evaka.shared.config.SealedSubclassSimpleName
import fi.espoo.evaka.shared.config.defaultJsonMapperBuilder
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.utils.writerFor
import fi.espoo.voltti.logging.loggers.error
import java.net.URI
import java.security.SecureRandom
import java.security.interfaces.ECPublicKey
import java.time.Duration
import mu.KotlinLogging
import org.springframework.http.HttpStatus

data class WebPushNotification(
    val endpoint: WebPushEndpoint,
    val ttl: Duration,
    val payloads: List<WebPushPayload>
)

enum class Urgency {
    VeryLow,
    Low,
    Normal,
    High,
}

@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@JsonTypeIdResolver(SealedSubclassSimpleName::class)
sealed interface WebPushPayload {
    data class NotificationV1(val title: String) : WebPushPayload
}

class WebPushEndpoint(val uri: URI, val ecdhPublicKey: ECPublicKey, val authSecret: ByteArray)

class WebPushRequest(
    val uri: URI,
    val headers: WebPushRequestHeaders,
    val body: ByteArray,
) {
    fun withVapid(vapidJwt: VapidJwt) =
        WebPushRequest(
            uri,
            headers.copy(authorization = vapidJwt.toAuthorizationHeader()),
            body,
        )

    companion object {
        // Message Encryption for Web Push (MEWP)
        // Reference: https://datatracker.ietf.org/doc/html/rfc8291
        fun createEncryptedPushMessage(
            ttl: Duration,
            urgency: Urgency,
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
                        contentEncoding = "aes128gcm",
                        urgency =
                            when (urgency) {
                                Urgency.VeryLow -> "very-low"
                                Urgency.Low -> "low"
                                Urgency.Normal -> "normal"
                                Urgency.High -> "high"
                            }
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
    val urgency: String? = null,
) {
    fun toTypedArray(): Array<Pair<String, String>> =
        listOfNotNull(
                "TTL" to ttl,
                "Content-Encoding" to contentEncoding,
                authorization?.let { "Authorization" to it },
                urgency?.let { "Urgency" to it },
            )
            .toTypedArray()
}

// RFC8292 says JWT tokens must expire within 24 hours of the request we're going to make
// https://datatracker.ietf.org/doc/html/rfc8292#section-2
//
// Apple docs suggest JWT tokens should not be refreshed more than once per hour
// https://developer.apple.com/documentation/usernotifications/sending_web_push_notifications_in_web_apps_safari_and_other_browsers#3994592
//
// -> valid duration for tokens must be 1-24 hours
// We also stop using an old token slightly before its expiry time to avoid using an expired token
// if there's a difference in clocks, or we take too long to send the request
private val VAPID_JWT_NEW_VALID_DURATION = Duration.ofHours(12)
private val VAPID_JWT_MIN_VALID_DURATION = Duration.ofHours(1)

class WebPush(env: WebPushEnv) {
    private val fuel = FuelManager()
    private val secureRandom = SecureRandom()
    private val jsonWriter = defaultJsonMapperBuilder().build().writerFor<List<WebPushPayload>>()
    private val vapidKeyPair: WebPushKeyPair =
        WebPushKeyPair.fromPrivateKey(WebPushCrypto.decodePrivateKey(env.vapidPrivateKey.value))
    val applicationServerKey: String
        get() = vapidKeyPair.publicKeyBase64()

    private val logger = KotlinLogging.logger {}

    class SubscriptionExpired(val status: HttpStatus, cause: Throwable) :
        RuntimeException("Subscription expired (HTTP $status)", cause)

    fun getValidToken(tx: Database.Transaction, clock: EvakaClock, endpoint: URI): VapidJwt =
        tx.getOrRefreshToken(
            // Pessimistically create a fresh JWT token every time, but it only gets saved and used
            // if we don't find a valid existing one from the database cache
            newToken =
                VapidJwt.create(
                    vapidKeyPair,
                    expiresAt = clock.now().plus(VAPID_JWT_NEW_VALID_DURATION),
                    endpoint
                ),
            // Avoid using JWT tokens that expire very soon
            minValidThreshold = clock.now().plus(VAPID_JWT_MIN_VALID_DURATION)
        )

    fun send(vapidJwt: VapidJwt, notification: WebPushNotification) {
        val webPushRequest =
            WebPushRequest.createEncryptedPushMessage(
                    ttl = notification.ttl,
                    endpoint = notification.endpoint,
                    messageKeyPair = WebPushCrypto.generateKeyPair(secureRandom),
                    salt = secureRandom.generateSeed(16),
                    data = jsonWriter.writeValueAsBytes(notification.payloads),
                    urgency = Urgency.Normal
                )
                .withVapid(vapidJwt)
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
            if (e.response.statusCode == 404 || e.response.statusCode == 410) {
                throw SubscriptionExpired(HttpStatus.valueOf(e.response.statusCode), e)
            }
            logger.error(e, meta) { "Web push failed, status ${e.response.statusCode}" }
            throw e
        }
    }
}
