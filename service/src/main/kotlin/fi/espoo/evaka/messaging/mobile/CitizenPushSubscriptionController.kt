// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging.mobile

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.shared.CitizenPushSubscriptionDeviceId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/citizen-mobile/push-subscriptions")
class CitizenPushSubscriptionController {

    data class UpsertBody(
        val deviceId: CitizenPushSubscriptionDeviceId,
        val expoPushToken: String,
    )

    @PostMapping("/v1")
    fun upsertSubscription(
        db: Database,
        user: AuthenticatedUser.CitizenMobile,
        clock: EvakaClock,
        @RequestBody body: UpsertBody,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                tx.upsertCitizenPushSubscription(user.id, body.deviceId, body.expoPushToken)
            }
        }
        Audit.CitizenMobilePushSubscriptionUpsert.log(targetId = AuditId(body.deviceId))
    }

    @DeleteMapping("/v1")
    fun deleteSubscription(
        db: Database,
        user: AuthenticatedUser.CitizenMobile,
        clock: EvakaClock,
        @RequestParam deviceId: CitizenPushSubscriptionDeviceId,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx -> tx.deleteCitizenPushSubscription(user.id, deviceId) }
        }
        Audit.CitizenMobilePushSubscriptionDelete.log(targetId = AuditId(deviceId))
    }
}
