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

    @PutMapping("/employee/system-notifications/citizens")
    fun putSystemNotificationCitizens(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: SystemNotificationCitizens,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Global.UPDATE_SYSTEM_NOTIFICATION,
                )
                tx.execute {
                    sql(
                        """
                    INSERT INTO system_notification (target_group, valid_to, text, text_sv, text_en) 
                    VALUES (
                        ${bind(SystemNotificationTargetGroup.CITIZENS)}, 
                        ${bind(body.validTo)}, 
                        ${bind(body.text)},
                        ${bind(body.textSv)},
                        ${bind(body.textEn)}
                    )
                    ON CONFLICT (target_group) DO UPDATE
                        SET 
                            valid_to = ${bind(body.validTo)},
                            text = ${bind(body.text)}, 
                            text_sv = ${bind(body.textSv)},
                            text_en = ${bind(body.textEn)}
                """
                    )
                }
            }
        }
        Audit.SystemNotificationsSet.log()
    }

    @PutMapping("/employee/system-notifications/employees")
    fun putSystemNotificationEmployees(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: SystemNotificationEmployees,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Global.UPDATE_SYSTEM_NOTIFICATION,
                )
                tx.execute {
                    sql(
                        """
                    INSERT INTO system_notification (target_group, valid_to, text) 
                    VALUES (
                        ${bind(SystemNotificationTargetGroup.EMPLOYEES)}, 
                        ${bind(body.validTo)}, 
                        ${bind(body.text)}
                    )
                    ON CONFLICT (target_group) DO UPDATE
                        SET 
                            valid_to = ${bind(body.validTo)},
                            text = ${bind(body.text)}
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
        @PathVariable targetGroup: SystemNotificationTargetGroup,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Global.UPDATE_SYSTEM_NOTIFICATION,
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

    data class SystemNotificationsResponse(
        val citizens: SystemNotificationCitizens?,
        val employees: SystemNotificationEmployees?,
    )

    @GetMapping("/employee/system-notifications")
    fun getAllSystemNotifications(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): SystemNotificationsResponse {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.READ_SYSTEM_NOTIFICATIONS,
                    )
                    SystemNotificationsResponse(
                        citizens =
                            tx.createQuery {
                                    sql(
                                        """
                            SELECT * FROM system_notification 
                            WHERE target_group = ${bind(SystemNotificationTargetGroup.CITIZENS)}
                        """
                                    )
                                }
                                .exactlyOneOrNull<SystemNotificationCitizens>(),
                        employees =
                            tx.createQuery {
                                    sql(
                                        """
                            SELECT * FROM system_notification 
                            WHERE target_group = ${bind(SystemNotificationTargetGroup.EMPLOYEES)}
                        """
                                    )
                                }
                                .exactlyOneOrNull<SystemNotificationEmployees>(),
                    )
                }
            }
            .also { Audit.SystemNotificationsReadAll.log() }
    }

    data class CurrentNotificationResponseCitizen(val notification: SystemNotificationCitizens?)

    @GetMapping("/citizen/public/system-notifications/current")
    fun getCurrentSystemNotificationCitizen(
        db: Database,
        clock: EvakaClock,
    ): CurrentNotificationResponseCitizen {
        return db.connect { dbc ->
                dbc.read { tx -> getCurrentSystemNotificationCitizen(tx = tx, now = clock.now()) }
            }
            .also { Audit.SystemNotificationsReadCitizen.log() }
    }

    data class CurrentNotificationResponseEmployee(val notification: SystemNotificationEmployees?)

    @GetMapping("/employee/public/system-notifications/current")
    fun getCurrentSystemNotificationEmployee(
        db: Database,
        clock: EvakaClock,
    ): CurrentNotificationResponseEmployee {
        return db.connect { dbc ->
                dbc.read { tx -> getCurrentSystemNotificationEmployee(tx = tx, now = clock.now()) }
            }
            .also { Audit.SystemNotificationsReadEmployee.log() }
    }

    @GetMapping("/employee-mobile/system-notifications/current")
    fun getCurrentSystemNotificationEmployeeMobile(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
    ): CurrentNotificationResponseEmployee {
        return db.connect { dbc ->
                dbc.read { tx -> getCurrentSystemNotificationEmployee(tx = tx, now = clock.now()) }
            }
            .also { Audit.SystemNotificationsReadEmployeeMobile.log() }
    }

    private fun getCurrentSystemNotificationCitizen(
        tx: Database.Read,
        now: HelsinkiDateTime,
    ): CurrentNotificationResponseCitizen {
        return tx.createQuery {
                sql(
                    """
            SELECT * FROM system_notification
            WHERE target_group = ${bind(SystemNotificationTargetGroup.CITIZENS)} AND valid_to > ${bind(now)}
        """
                )
            }
            .exactlyOneOrNull<SystemNotificationCitizens>()
            .let { CurrentNotificationResponseCitizen(it) }
    }

    private fun getCurrentSystemNotificationEmployee(
        tx: Database.Read,
        now: HelsinkiDateTime,
    ): CurrentNotificationResponseEmployee {
        return tx.createQuery {
                sql(
                    """
            SELECT * FROM system_notification
            WHERE target_group = ${bind(SystemNotificationTargetGroup.EMPLOYEES)} AND valid_to > ${bind(now)}
        """
                )
            }
            .exactlyOneOrNull<SystemNotificationEmployees>()
            .let { CurrentNotificationResponseEmployee(it) }
    }
}

data class SystemNotificationCitizens(
    val validTo: HelsinkiDateTime,
    val text: String,
    val textSv: String,
    val textEn: String,
)

data class SystemNotificationEmployees(val validTo: HelsinkiDateTime, val text: String)

enum class SystemNotificationTargetGroup : DatabaseEnum {
    CITIZENS,
    EMPLOYEES;

    override val sqlType: String = "system_notification_target_group"
}
