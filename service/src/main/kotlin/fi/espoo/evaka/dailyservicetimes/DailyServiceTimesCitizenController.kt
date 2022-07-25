// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.dailyservicetimes

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.DailyServiceTimeNotificationId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/citizen")
class DailyServiceTimesCitizenController(private val accessControl: AccessControl) {
    @GetMapping("/daily-service-time-notifications")
    fun getDailyServiceTimeNotifications(
        db: Database,
        user: AuthenticatedUser.Citizen
    ): List<DailyServiceTimeNotification> {
        Audit.ChildDailyServiceTimeNotificationsRead.log(targetId = user.id)
        accessControl.requirePermissionFor(user, Action.Citizen.Person.READ_DAILY_SERVICE_TIME_NOTIFICATIONS, user.id)

        return db.connect { dbc -> dbc.transaction { tx -> tx.getDailyServiceTimesNotifications(user.id) } }
    }

    @PostMapping("/daily-service-time-notifications/dismiss")
    fun dismissDailyServiceTimeNotification(
        db: Database,
        user: AuthenticatedUser.Citizen,
        @RequestBody body: List<DailyServiceTimeNotificationId>
    ) {
        Audit.ChildDailyServiceTimeNotificationsDismiss.log(targetId = body)
        accessControl.requirePermissionFor(user, Action.Citizen.DailyServiceTimeNotification.DISMISS, body)

        db.connect { dbc -> dbc.transaction { tx -> tx.deleteDailyServiceTimesNotifications(body) } }
    }
}

data class DailyServiceTimeNotification(
    val id: DailyServiceTimeNotificationId,
    val dateFrom: LocalDate,
    val hasDeletedReservations: Boolean
)

fun Database.Read.getDailyServiceTimesNotifications(guardianId: PersonId): List<DailyServiceTimeNotification> = createQuery(
    """
SELECT id, date_from, has_deleted_reservations FROM daily_service_time_notification WHERE guardian_id = :guardianId
    """.trimIndent()
)
    .bind("guardianId", guardianId)
    .mapTo<DailyServiceTimeNotification>()
    .list()

fun Database.Transaction.deleteDailyServiceTimesNotifications(notificationIds: List<DailyServiceTimeNotificationId>) {
    createUpdate(
        """
DELETE FROM daily_service_time_notification WHERE id = ANY(:ids)
        """.trimIndent()
    )
        .bind("ids", notificationIds.toTypedArray())
        .execute()
}
