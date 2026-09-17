// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.webpush

import evaka.core.EvakaEnv
import evaka.core.pis.NotificationCategory
import evaka.core.shared.CitizenPushSubscriptionId
import evaka.core.shared.PersonId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.UiLanguage
import fi.espoo.voltti.logging.loggers.info
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Duration
import java.time.LocalTime
import org.springframework.stereotype.Service

/** A notification that is a day old has no value, so few retries are enough */
const val CITIZEN_PUSH_RETRY_COUNT = 3

private val MAX_THROTTLE_WAIT: Duration = Duration.ofHours(1)

/** Avoid hitting max payload size of 4096 bytes by limiting the body length. */
const val MAX_BODY_LENGTH = 200

private val DEFAULT_TTL: Duration = Duration.ofDays(1)
private val MIN_TTL: Duration = Duration.ofMinutes(15)
private val MAX_TTL: Duration = Duration.ofDays(5)

@Service
class CitizenPushNotifications(
    private val webPush: WebPush?,
    private val env: EvakaEnv,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val messageProvider: PushNotificationMessageProvider,
) {
    private val logger = KotlinLogging.logger {}

    init {
        asyncJobRunner.registerHandler(::runSendJob)
    }

    /** Runs [send], and plans [job] again at Retry-After if throttled */
    fun <T : AsyncJob> rescheduleIfThrottled(
        db: Database.Connection,
        clock: EvakaClock,
        job: T,
        remainingAttempts: Int,
        send: () -> Unit,
    ) {
        try {
            send()
        } catch (e: WebPush.Throttled) {
            if (e.retryAfter == null || remainingAttempts == 0) throw e
            val retryAfter = minOf(e.retryAfter, MAX_THROTTLE_WAIT)
            logger.warn(e) { "Push service asked to wait $retryAfter -> rescheduling" }
            db.transaction { tx ->
                asyncJobRunner.plan(
                    tx,
                    listOf(job),
                    retryCount = remainingAttempts,
                    runAt = clock.now().plus(retryAfter),
                )
            }
        }
    }

    /** Plans one send per subscription of the person and returns how many were planned */
    @IgnorableReturnValue
    fun plan(
        tx: Database.Transaction,
        now: HelsinkiDateTime,
        person: PersonId,
        notification: CitizenPushNotification,
    ): Int {
        val jobs =
            tx.getCitizenPushSubscriptionIds(person).map {
                AsyncJob.SendCitizenPushNotification(it, notification)
            }
        asyncJobRunner.plan(tx, jobs, retryCount = CITIZEN_PUSH_RETRY_COUNT, runAt = now)
        return jobs.size
    }

    fun runSendJob(
        dbc: Database.Connection,
        clock: EvakaClock,
        job: AsyncJob.SendCitizenPushNotification,
        remainingAttempts: Int,
    ) =
        rescheduleIfThrottled(dbc, clock, job, remainingAttempts) {
            val language =
                dbc.read { it.getCitizenPushLanguage(job.subscription) }
                    ?: return@rescheduleIfThrottled
            val delivery =
                delivery(clock.now(), language, job.notification) ?: return@rescheduleIfThrottled
            send(
                dbc,
                clock,
                job.subscription,
                delivery,
            )
        }

    /** Returns null when the deadline of [notification] has passed */
    private fun delivery(
        now: HelsinkiDateTime,
        language: UiLanguage,
        notification: CitizenPushNotification,
    ): Delivery? {
        return when (notification) {
            is CitizenPushNotification.Decision ->
                Delivery(
                    NotificationCategory.DECISION_NOTIFICATION,
                    messageProvider.decisionNotification(language, notification.kind),
                    path =
                        when (notification.kind) {
                            DecisionPushNotificationKind.PENDING_APPROVAL -> "/decisions/pending"
                            else -> "/decisions"
                        },
                    tag =
                        when (notification.kind) {
                            DecisionPushNotificationKind.PENDING_APPROVAL -> "decision-pending"
                            else -> "decision"
                        },
                )

            is CitizenPushNotification.ChildApplicationDecision ->
                Delivery(
                    NotificationCategory.DECISION_NOTIFICATION,
                    messageProvider.childApplicationDecisionNotification(
                        language,
                        notification.kind,
                    ),
                    path = "/children/${notification.childId}",
                    tag = "child-application-${notification.childId}",
                )

            is CitizenPushNotification.Income ->
                Delivery(
                    NotificationCategory.INCOME_NOTIFICATION,
                    messageProvider.incomeNotification(language, notification.notificationType),
                    path = "/income",
                    tag = "income",
                )

            is CitizenPushNotification.CalendarEvents ->
                Delivery(
                    NotificationCategory.CALENDAR_EVENT_NOTIFICATION,
                    messageProvider.calendarEventNotification(
                        language,
                        notification.count,
                        notification.single?.title,
                    ),
                    path = notification.single?.let { "/calendar?day=${it.date}" } ?: "/calendar",
                    tag = "calendar-events",
                )

            is CitizenPushNotification.Document ->
                Delivery(
                    NotificationCategory.DOCUMENT_NOTIFICATION,
                    messageProvider.childDocumentNotification(
                        language,
                        notification.notificationType,
                    ),
                    path = "/child-documents/${notification.documentId}",
                    tag = "document-${notification.documentId}",
                )

            is CitizenPushNotification.InformalDocument ->
                Delivery(
                    NotificationCategory.INFORMAL_DOCUMENT_NOTIFICATION,
                    messageProvider.pedagogicalDocumentNotification(language),
                    path = "/children/${notification.childId}",
                    tag = "informal-document-${notification.childId}",
                )

            is CitizenPushNotification.MissingReservations ->
                Delivery(
                    NotificationCategory.ATTENDANCE_RESERVATION_NOTIFICATION,
                    messageProvider.missingReservationsNotification(language, notification.range),
                    path =
                        "/calendar?modal=reservations&startDate=${notification.range.start}&endDate=${notification.range.end}",
                    tag = "missing-reservations",
                    ttl = ttlUntil(now, notification.deadline) ?: return null,
                )

            is CitizenPushNotification.MissingHolidayReservations ->
                Delivery(
                    NotificationCategory.ATTENDANCE_RESERVATION_NOTIFICATION,
                    messageProvider.missingHolidayReservationsNotification(
                        language,
                        notification.deadline,
                    ),
                    path = "/calendar?modal=holidays",
                    tag = "missing-holiday-reservations",
                    ttl =
                        ttlUntil(now, HelsinkiDateTime.of(notification.deadline, LocalTime.MAX))
                            ?: return null,
                )

            is CitizenPushNotification.DiscussionSurvey ->
                Delivery(
                    NotificationCategory.DISCUSSION_TIME_NOTIFICATION,
                    messageProvider.discussionSurveyNotification(language, notification.title),
                    path = "/calendar?modal=discussions",
                    tag = "discussion-survey",
                )

            is CitizenPushNotification.DiscussionTime ->
                Delivery(
                    NotificationCategory.DISCUSSION_TIME_NOTIFICATION,
                    messageProvider.discussionTimeNotification(
                        language,
                        notification.event,
                        notification.date,
                        notification.startTime,
                        notification.endTime,
                    ),
                    path = "/calendar?day=${notification.date}",
                    tag = "discussion-time-${notification.eventTimeId}",
                    ttl =
                        when (notification.event) {
                            DiscussionTimePushNotificationEvent.REMINDER ->
                                ttlUntil(
                                    now,
                                    HelsinkiDateTime.of(notification.date, notification.startTime),
                                ) ?: return null
                            else -> DEFAULT_TTL
                        },
                )
        }
    }

    private fun ttlUntil(now: HelsinkiDateTime, deadline: HelsinkiDateTime): Duration? =
        Duration.between(now.toInstant(), deadline.toInstant())
            .takeIf { it.isPositive }
            ?.coerceIn(MIN_TTL, MAX_TTL)

    /**
     * Sends one notification to a citizen's browser.
     *
     * A [category] the citizen has switched off stops the send. A test notification passes null,
     * because the citizen asked for it explicitly.
     */
    fun send(
        dbc: Database.Connection,
        clock: EvakaClock,
        subscription: CitizenPushSubscriptionId,
        delivery: Delivery,
    ) {
        if (webPush == null) return

        val (vapidJwt, endpoint) =
            dbc.transaction { tx ->
                tx.getCitizenPushTarget(subscription)
                    ?.takeIf {
                        delivery.category == null || delivery.category !in it.disabledCategories
                    }
                    ?.let { Pair(webPush.getValidToken(tx, clock, it.endpoint.uri), it.endpoint) }
            } ?: return
        dbc.close()

        logger.info(mapOf("endpoint" to endpoint.uri)) {
            "Sending push notification to citizen subscription $subscription"
        }
        val message =
            WebPushMessage.Declarative(
                DeclarativeNotification(
                    title = delivery.content.title,
                    // URL is not visible to the citizens, so we can always use the Finnish URL
                    navigate = env.frontendBaseUrlFi + delivery.path,
                    body = delivery.content.body?.take(MAX_BODY_LENGTH),
                    tag = delivery.tag,
                )
            )
        try {
            webPush.send(vapidJwt, WebPushNotification(endpoint, delivery.ttl, message))
            dbc.transaction { it.markCitizenPushSent(subscription, clock.now()) }
        } catch (e: WebPush.SubscriptionExpired) {
            logger.warn {
                "Subscription $subscription expired (HTTP status ${e.status}) -> deleting"
            }
            dbc.transaction { it.deleteCitizenPushSubscription(subscription) }
        } catch (e: WebPush.PermanentFailure) {
            logger.warn(e) { "Push notification to subscription $subscription refused -> dropping" }
        }
    }
}

data class Delivery(
    val category: NotificationCategory?,
    val content: PushNotificationContent,
    val path: String,
    val tag: String,
    val ttl: Duration = DEFAULT_TTL,
)
