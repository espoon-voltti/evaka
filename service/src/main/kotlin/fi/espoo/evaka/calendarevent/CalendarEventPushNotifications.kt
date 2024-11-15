// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.calendarevent

import fi.espoo.evaka.shared.CalendarEventTimeId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJob.CalendarEventReservationNotificationType
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.webpush.WebPush
import fi.espoo.evaka.webpush.WebPushCrypto
import fi.espoo.evaka.webpush.WebPushEndpoint
import fi.espoo.evaka.webpush.WebPushNotification
import fi.espoo.evaka.webpush.WebPushPayload
import fi.espoo.evaka.webpush.deletePushSubscription
import fi.espoo.voltti.logging.loggers.info
import java.time.Duration
import java.time.format.DateTimeFormatter
import mu.KotlinLogging
import org.springframework.stereotype.Service

@Service
class CalendarEventPushNotifications(
    private val webPush: WebPush?,
    private val accessControl: AccessControl,
    asyncJobRunner: AsyncJobRunner<AsyncJob>,
) {
    init {
        asyncJobRunner.registerHandler(::sendCalendarEventPushNotification)
    }

    private val logger = KotlinLogging.logger {}

    data class CalendarEventReservationNotification(
        val groupId: GroupId,
        val groupName: String,
        val endpoint: WebPushEndpoint,
    )

    private fun Database.Read.getNotification(
        job: AsyncJob.SendCalendarEventReservationPushNotification
    ): CalendarEventReservationNotification? =
        createQuery {
                sql(
                    """
SELECT dg.id as group_id, dg.name as group_name, mdps.endpoint, mdps.auth_secret, mdps.ecdh_key
FROM mobile_device md
JOIN mobile_device_push_group mdpg ON mdpg.device = md.id
JOIN mobile_device_push_subscription mdps ON mdps.device = md.id
JOIN daycare_group dg ON dg.id = mdpg.daycare_group
JOIN daycare d ON d.id = dg.daycare_id
WHERE md.id = ${bind(job.device)} AND md.employee_id IS NULL AND dg.id = ${bind(job.groupId)}
AND 'PUSH_NOTIFICATIONS' = ANY(d.enabled_pilot_features)
AND 'CALENDAR_EVENT_RESERVATION' = ANY(md.push_notification_categories)
"""
                )
            }
            .exactlyOneOrNull {
                CalendarEventReservationNotification(
                    groupId = column("group_id"),
                    groupName = column("group_name"),
                    WebPushEndpoint(
                        uri = column("endpoint"),
                        ecdhPublicKey =
                            WebPushCrypto.decodePublicKey(column<ByteArray>("ecdh_key")),
                        authSecret = column("auth_secret"),
                    ),
                )
            }

    private fun sendCalendarEventPushNotification(
        dbc: Database.Connection,
        clock: EvakaClock,
        job: AsyncJob.SendCalendarEventReservationPushNotification,
    ) {
        if (webPush == null) return

        val device = job.device

        val (vapidJwt, notification) =
            dbc.transaction { tx ->
                tx.getNotification(job)
                    ?.takeIf {
                        accessControl.hasPermissionFor(
                            tx,
                            AuthenticatedUser.MobileDevice(device),
                            clock,
                            Action.Group.RECEIVE_PUSH_NOTIFICATIONS,
                            it.groupId,
                        )
                    }
                    ?.let { Pair(webPush.getValidToken(tx, clock, it.endpoint.uri), it) }
            } ?: return
        dbc.close()

        val dateFormat = DateTimeFormatter.ofPattern("d.M.")
        val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

        logger.info(mapOf("endpoint" to notification.endpoint.uri)) {
            "Sending push notification to $device"
        }
        try {
            webPush.send(
                vapidJwt,
                WebPushNotification(
                    notification.endpoint,
                    ttl = Duration.ofDays(1),
                    payloads =
                        listOf(
                            WebPushPayload.NotificationV1(
                                title =
                                    "${notification.groupName}: Huoltaja ${when (job.type) {
                                CalendarEventReservationNotificationType.RESERVED -> "varannut"
                                CalendarEventReservationNotificationType.CANCELLED -> "perunut"
                            }} keskusteluajan ${job.date.format(dateFormat)} klo ${job.startTime.format(timeFormat)} - ${job.endTime.format(timeFormat)}"
                            )
                        ),
                ),
            )
        } catch (e: WebPush.SubscriptionExpired) {
            logger.warn(
                "Subscription expired for device $device (HTTP status ${e.status}) -> deleting"
            )
            dbc.transaction { it.deletePushSubscription(device) }
        }
    }
}

data class GroupDevice(val groupId: GroupId, val device: MobileDeviceId)

fun Database.Read.getCalendarEventReservationGroupDevices(
    calendarEventTimeId: CalendarEventTimeId,
    childId: ChildId,
): Set<GroupDevice> =
    createQuery {
            sql(
                """
SELECT cea.group_id, mdpg.device
FROM calendar_event_time cet
JOIN calendar_event_attendee cea ON cea.calendar_event_id = cet.calendar_event_id
JOIN daycare_group dg ON dg.id = cea.group_id
JOIN daycare d ON d.id = dg.daycare_id AND d.id = cea.unit_id AND 'PUSH_NOTIFICATIONS' = ANY(d.enabled_pilot_features)
JOIN mobile_device_push_group mdpg ON mdpg.daycare_group = dg.id
WHERE cet.id = ${bind(calendarEventTimeId)}
AND EXISTS (
    SELECT FROM mobile_device md
    JOIN mobile_device_push_subscription mdps ON mdpg.device = md.id
    WHERE md.id = mdpg.device 
      AND md.employee_id IS NULL -- not for personal mobiles
      AND 'CALENDAR_EVENT_RESERVATION' = ANY(md.push_notification_categories)
) AND EXISTS (
    SELECT FROM daycare_group_placement dgp 
    JOIN placement pl ON dgp.daycare_placement_id = pl.id
    WHERE pl.child_id = ${bind(childId)} 
      AND dgp.daycare_group_id = cea.group_id
      AND daterange(dgp.start_date, dgp.end_date, '[]') @> cet.date
)
        """
            )
        }
        .toSet<GroupDevice>()
