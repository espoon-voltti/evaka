// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.webpush

import evaka.core.Audit
import evaka.core.AuditContext
import evaka.core.shared.CitizenPushSubscriptionId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import evaka.core.shared.utils.assertNotNull
import evaka.core.user.UserAgentParser
import java.net.URI
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class CitizenWebPushController(
    private val accessControl: AccessControl,
    private val userAgentParser: UserAgentParser,
    private val webPush: WebPush?,
) {
    data class CitizenPushSettings(
        /** Null when web push is not configured in this environment */
        val applicationServerKey: String?,
        val devices: List<CitizenPushDevice>,
    )

    @GetMapping("/citizen/push-settings")
    fun getPushSettings(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
    ): CitizenPushSettings {
        val audit = AuditContext()
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Person.READ_PUSH_SETTINGS,
                        user.id,
                    )
                    CitizenPushSettings(
                        applicationServerKey = webPush?.applicationServerKey,
                        devices = tx.getCitizenPushDevices(user.id),
                    )
                }
            }
            .also { audit.log(Audit.CitizenPushSettingsRead, clock) }
    }

    data class NewCitizenPushSubscription(
        val subscription: WebPushSubscription,
        /** Whether the browser is running the app installed to the home screen */
        val installed: Boolean,
    )

    @PostMapping("/citizen/push-subscription")
    fun addPushSubscription(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @RequestHeader(HttpHeaders.USER_AGENT, required = false) userAgent: String?,
        @RequestBody body: NewCitizenPushSubscription,
    ): CitizenPushDevice {
        val audit = AuditContext()
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Person.UPDATE_PUSH_SUBSCRIPTION,
                        user.id,
                    )
                    tx.insertCitizenPushSubscription(
                            user.id,
                            body.subscription,
                            body.installed,
                            userAgentParser.parse(userAgent),
                        )
                        .also { audit.add(it.id) }
                }
            }
            .also { audit.log(Audit.CitizenPushSubscriptionCreate, clock) }
    }

    data class PushSubscriptionCheckRequest(val endpoint: URI)

    data class PushSubscriptionCheckResponse(
        /** Null when the browser's subscription is not the caller's and must be unsubscribed */
        val deviceId: CitizenPushSubscriptionId?
    )

    @PostMapping("/citizen/push-subscription/check")
    fun checkPushSubscription(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @RequestBody body: PushSubscriptionCheckRequest,
    ): PushSubscriptionCheckResponse {
        val audit = AuditContext()
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Person.UPDATE_PUSH_SUBSCRIPTION,
                        user.id,
                    )
                    tx.deleteForeignCitizenPushSubscription(user.id, body.endpoint)?.also {
                        audit.add(it).addMeta("evicted", true)
                    }
                    PushSubscriptionCheckResponse(
                        tx.getCitizenPushSubscriptionId(user.id, body.endpoint)?.also {
                            audit.add(it)
                        }
                    )
                }
            }
            .also { audit.log(Audit.CitizenPushSubscriptionCheck, clock) }
    }

    @DeleteMapping("/citizen/push-devices/{id}")
    fun deletePushDevice(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable id: CitizenPushSubscriptionId,
    ) {
        val audit = AuditContext().add(id)
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Citizen.Person.DELETE_PUSH_DEVICE,
                    user.id,
                )
                tx.deleteCitizenPushDevice(user.id, id).assertNotNull(msg = "Push device not found")
            }
        }
        audit.log(Audit.CitizenPushDeviceDelete, clock)
    }
}
