// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.webpush

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.github.kittinunf.fuel.core.FuelManager
import fi.espoo.evaka.WebPushEnv
import java.net.URI
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

data class WebPushNotification(val uri: URI, val ttl: Duration)

class WebPush(env: WebPushEnv) {
    private val fuel = FuelManager()
    private val vapidKeyPair: WebPushKeyPair =
        WebPushKeyPair.fromPrivateKey(WebPushCrypto.decodePrivateKey(env.vapidPrivateKey.value))
    val applicationServerKey: String
        get() = vapidKeyPair.publicKeyBase64()

    fun send(notification: WebPushNotification) {
        // Reference: RFC8292 (VAPID): https://datatracker.ietf.org/doc/html/rfc8292#section-2
        // 2. Application Server Self-Identification
        val jwt =
            JWT.create()
                .withAudience(notification.uri.toString())
                .withExpiresAt(Instant.now().plus(6, ChronoUnit.HOURS))
                .sign(Algorithm.ECDSA256(vapidKeyPair.privateKey))

        fuel
            .post(notification.uri.toString())
            .header("TTL", notification.ttl.toSeconds())
            // Reference: RFC8292 (VAPID): https://datatracker.ietf.org/doc/html/rfc8292#section-3
            // 3. VAPID Authentication Scheme
            .header("Authorization", "vapid t=$jwt; k=${vapidKeyPair.publicKeyBase64()}")
            .response()
            .third
            .get()
    }
}
