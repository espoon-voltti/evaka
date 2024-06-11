// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.systemnotifications

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.DatabaseEnum
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class SystemNotificationsController(private val accessControl: AccessControl) {

    @PutMapping("/employee/system-notifications")
    fun putSystemNotification(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: SystemNotification
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Global.UPDATE_SYSTEM_NOTIFICATION
                )
                tx.execute {
                    sql(
                        """
                    INSERT INTO system_notification (target_group, text, valid_to) 
                    VALUES (${bind(body.targetGroup)}, ${bind(body.text)}, ${bind(body.validTo)})
                    ON CONFLICT (target_group) DO UPDATE
                        SET text = ${bind(body.text)}, valid_to = ${bind(body.validTo)}
                """
                    )
                }
            }
        }
        Audit.SystemNotificationsSet.log()
    }

    @DeleteMapping("/employee/system-notifications/{targetGroup}")
    fun deleteSystemNotification(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable targetGroup: SystemNotificationTargetGroup
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Global.UPDATE_SYSTEM_NOTIFICATION
                )
                tx.execute {
                    sql(
                        """
                    DELETE FROM system_notification
                    WHERE target_group = ${bind(targetGroup)}
                """
                    )
                }
            }
        }
        Audit.SystemNotificationsDelete.log()
    }

    @GetMapping("/employee/system-notifications")
    fun getAllSystemNotifications(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock
    ): List<SystemNotification> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.READ_SYSTEM_NOTIFICATIONS
                    )
                    tx.createQuery { sql("SELECT * FROM system_notification") }
                        .toList<SystemNotification>()
                }
            }
            .also { Audit.SystemNotificationsReadAll.log() }
    }

    data class CurrentNotificationResponse(val notification: SystemNotification?)

    @GetMapping("/citizen/public/system-notifications/current")
    fun getCurrentSystemNotificationCitizen(
        db: Database,
        clock: EvakaClock
    ): CurrentNotificationResponse {
        return db.connect { dbc ->
                dbc.read { tx ->
                    tx.createQuery {
                            sql(
                                """
                    SELECT * FROM system_notification
                    WHERE target_group = 'CITIZENS' AND valid_to > ${bind(clock.now())}
                """
                            )
                        }
                        .exactlyOneOrNull<SystemNotification>()
                        .let { CurrentNotificationResponse(it) }
                }
            }
            .also { Audit.SystemNotificationsReadCitizen.log() }
    }

    @GetMapping("/employee-mobile/system-notifications/current")
    fun getCurrentSystemNotificationEmployeeMobile(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock
    ): CurrentNotificationResponse {
        return db.connect { dbc ->
                dbc.read { tx ->
                    tx.createQuery {
                            sql(
                                """
                    SELECT * FROM system_notification
                    WHERE target_group = 'EMPLOYEES' AND valid_to > ${bind(clock.now())}
                """
                            )
                        }
                        .exactlyOneOrNull<SystemNotification>()
                        .let { CurrentNotificationResponse(it) }
                }
            }
            .also { Audit.SystemNotificationsReadEmployeeMobile.log() }
    }
}

data class SystemNotification(
    val targetGroup: SystemNotificationTargetGroup,
    val text: String,
    val validTo: HelsinkiDateTime
)

enum class SystemNotificationTargetGroup : DatabaseEnum {
    CITIZENS,
    EMPLOYEES;

    override val sqlType: String = "system_notification_target_group"
}
