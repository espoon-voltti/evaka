// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.webpush

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.net.URI
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
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
class WebPushController(private val accessControl: AccessControl) {
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

    data class PushSettings(
        val categories: Set<PushNotificationCategory>,
        val groups: Set<GroupId>,
    )

    @GetMapping("/mobile-devices/push-settings")
    fun getPushSettings(
        db: Database,
        clock: EvakaClock,
        user: AuthenticatedUser.MobileDevice,
    ): PushSettings =
        db.connect { dbc ->
                dbc.read { tx ->
                    val filter =
                        accessControl.requireAuthorizationFilter(
                            tx,
                            user,
                            clock,
                            Action.Group.RECEIVE_PUSH_NOTIFICATIONS
                        )
                    PushSettings(
                        categories = tx.getPushCategories(user.id),
                        groups = tx.getPushGroups(filter, user.id)
                    )
                }
            }
            .also { Audit.PushSettingsRead.log(targetId = user.id) }

    @PutMapping("/mobile-devices/push-settings")
    fun setPushSettings(
        db: Database,
        clock: EvakaClock,
        user: AuthenticatedUser.MobileDevice,
        @RequestBody settings: PushSettings
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Group.RECEIVE_PUSH_NOTIFICATIONS,
                    settings.groups
                )
                tx.setPushCategories(user.id, settings.categories)
                tx.setPushGroups(clock.now(), user.id, settings.groups)
            }
        }
        Audit.PushSettingsSet.log(
            targetId = user.id,
            meta =
                mapOf(
                    "categories" to settings.categories,
                    "groups" to settings.groups,
                )
        )
    }
}
