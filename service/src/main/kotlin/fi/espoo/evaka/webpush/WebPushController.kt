// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.webpush

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.net.URI

data class WebPushSubscription(
    val endpoint: URI,
    val expires: HelsinkiDateTime?,
    val authSecret: List<Byte>,
    val ecdhKey: List<Byte>
)

@RestController
class WebPushController {
    @PostMapping("/mobile-devices/{id}/push-subscription")
    fun upsertPushSubscription(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        @PathVariable id: MobileDeviceId,
        @RequestBody subscription: WebPushSubscription
    ) {
        db.connect { dbc -> dbc.transaction { it.upsertPushSubscription(id, subscription) } }
        Audit.PushSubscriptionUpsert.log(
            targetId = id,
            meta = mapOf("endpoint" to subscription.endpoint, "expires" to subscription.expires)
        )
    }
}
