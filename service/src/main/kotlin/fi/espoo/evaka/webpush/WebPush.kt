// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.webpush

import com.github.kittinunf.fuel.core.FuelManager
import fi.espoo.evaka.WebPushEnv
import fi.espoo.evaka.shared.domain.EvakaClock
import java.net.URI
import java.time.Duration

data class WebPushNotification(val uri: URI, val ttl: Duration)

class WebPush(env: WebPushEnv) {
    private val fuel = FuelManager()
    private val vapidKeyPair: WebPushKeyPair =
        WebPushKeyPair.fromPrivateKey(WebPushCrypto.decodePrivateKey(env.vapidPrivateKey.value))
    val applicationServerKey: String
        get() = vapidKeyPair.publicKeyBase64()

    fun send(clock: EvakaClock, notification: WebPushNotification) {
        fuel
            .post(notification.uri.toString())
            .header("TTL", notification.ttl.toSeconds())
            .header(
                "Authorization" to
                    vapidAuthorizationHeader(
                        vapidKeyPair,
                        expiresAt = clock.now().plusHours(6).toInstant(),
                        uri = notification.uri
                    )
            )
            .response()
            .third
            .get()
    }
}
