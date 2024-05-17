// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.dailyservicetimes

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.DailyServiceTimeNotificationId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/citizen")
class DailyServiceTimesCitizenController(private val accessControl: AccessControl) {
    @GetMapping("/daily-service-time-notifications")
    fun getDailyServiceTimeNotifications(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock
    ): List<DailyServiceTimeNotification> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Person.READ_DAILY_SERVICE_TIME_NOTIFICATIONS,
                        user.id
                    )
                    tx.getDailyServiceTimesNotifications(user.id)
                }
            }
            .also {
                Audit.ChildDailyServiceTimeNotificationsRead.log(
                    targetId = user.id,
                    meta = mapOf("count" to it.size)
                )
            }
    }

    @PostMapping("/daily-service-time-notifications/dismiss")
    fun dismissDailyServiceTimeNotification(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @RequestBody body: List<DailyServiceTimeNotificationId>
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl
                        .checkPermissionFor(
                            tx,
                            user,
                            clock,
                            Action.Citizen.DailyServiceTimeNotification.DISMISS,
                            body
                        )
                        .asSequence()
                        .mapNotNull { (id, permission) -> id.takeIf { permission.isPermitted() } }
                        .toList()
                        .also { tx.deleteDailyServiceTimesNotifications(it) }
                }
            }
            .also { Audit.ChildDailyServiceTimeNotificationsDismiss.log(targetId = it) }
    }
}

data class DailyServiceTimeNotification(
    val id: DailyServiceTimeNotificationId,
    val dateFrom: LocalDate,
    val hasDeletedReservations: Boolean
)

fun Database.Read.getDailyServiceTimesNotifications(
    userId: PersonId
): List<DailyServiceTimeNotification> =
    createQuery {
            sql(
                """
SELECT id, date_from, has_deleted_reservations FROM daily_service_time_notification WHERE guardian_id = ${bind(userId)}
    """
            )
        }
        .toList<DailyServiceTimeNotification>()

fun Database.Transaction.deleteDailyServiceTimesNotifications(
    notificationIds: List<DailyServiceTimeNotificationId>
) {
    execute {
        sql("DELETE FROM daily_service_time_notification WHERE id = ANY(${bind(notificationIds)})")
    }
}
