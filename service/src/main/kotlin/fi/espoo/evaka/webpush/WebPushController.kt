// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.webpush

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.net.URI
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

data class WebPushSubscription(
    val endpoint: URI,
    val expires: HelsinkiDateTime?,
    val authSecret: List<Byte>,
    val ecdhKey: List<Byte>
) {
    init {
        require(authSecret.size <= 1024) {
            "Expected auth secret to be at most 1024 bytes, got ${authSecret.size}"
        }
        WebPushCrypto.decodePublicKey(ecdhKey.toByteArray())
    }
}

@RestController
class WebPushController {
    @PostMapping("/mobile-devices/push-subscription")
    fun upsertPushSubscription(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        @RequestBody subscription: WebPushSubscription
    ) {
        db.connect { dbc -> dbc.transaction { it.upsertPushSubscription(user.id, subscription) } }
        Audit.PushSubscriptionUpsert.log(
            targetId = user.id,
            meta = mapOf("endpoint" to subscription.endpoint, "expires" to subscription.expires)
        )
    }
}
