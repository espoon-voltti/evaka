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

private val DEFAULT_TTL: Duration = Duration.ofDays(1)
private val MIN_TTL: Duration = Duration.ofMinutes(15)
private val MAX_TTL: Duration = Duration.ofDays(5)

// Push services refuse a payload above 4096 bytes, and names and free text such as a message have
// no length limit of their own
private const val MAX_TITLE_LENGTH = 100
private const val MAX_BODY_LENGTH = 500

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
            val delivery = delivery(clock.now(), language, job.notification)
            send(
                dbc,
                clock,
                job.subscription,
                delivery,
            )
        }

    private fun delivery(
        now: HelsinkiDateTime,
        language: UiLanguage,
        notification: CitizenPushNotification,
    ): Delivery =
        when (notification) {
            is CitizenPushNotification.FeeDecision ->
                Delivery(
                    NotificationCategory.DECISION_NOTIFICATION,
                    messageProvider.feeDecisionNotification(language, notification),
                    path = "/decisions",
                    tag = "fee-decision",
                )

            is CitizenPushNotification.VoucherValueDecision ->
                Delivery(
                    NotificationCategory.DECISION_NOTIFICATION,
                    messageProvider.voucherValueDecisionNotification(language, notification),
                    path = "/decisions",
                    tag = "voucher-value-decision-${notification.decisionId}",
                )

            is CitizenPushNotification.ApplicationDecisions ->
                Delivery(
                    NotificationCategory.DECISION_NOTIFICATION,
                    messageProvider.applicationDecisionsNotification(language, notification),
                    path = if (notification.answerRequired) "/decisions/pending" else "/decisions",
                    tag = "decision-${notification.applicationId}",
                )

            is CitizenPushNotification.PendingDecisions ->
                Delivery(
                    NotificationCategory.DECISION_NOTIFICATION,
                    messageProvider.pendingDecisionsNotification(language, notification.decisions),
                    path = "/decisions/pending",
                    tag = "decision-pending",
                )

            is CitizenPushNotification.AbsenceApplicationDecision ->
                Delivery(
                    NotificationCategory.DECISION_NOTIFICATION,
                    messageProvider.absenceApplicationDecisionNotification(language, notification),
                    path = "/children/${notification.childId}",
                    tag = "child-application-${notification.childId}",
                )

            is CitizenPushNotification.ServiceApplicationDecision ->
                Delivery(
                    NotificationCategory.DECISION_NOTIFICATION,
                    messageProvider.serviceApplicationDecisionNotification(language, notification),
                    path = "/children/${notification.childId}",
                    tag = "child-application-${notification.childId}",
                )

            is CitizenPushNotification.Income ->
                Delivery(
                    NotificationCategory.INCOME_NOTIFICATION,
                    messageProvider.incomeNotification(language, notification),
                    path = "/income",
                    tag = "income",
                )

            is CitizenPushNotification.CalendarEvents ->
                Delivery(
                    NotificationCategory.CALENDAR_EVENT_NOTIFICATION,
                    messageProvider.calendarEventNotification(language, notification.events),
                    path =
                        notification.events.singleOrNull()?.let {
                            "/calendar?day=${it.period.start}"
                        } ?: "/calendar",
                    tag = "calendar-events",
                )

            is CitizenPushNotification.Document ->
                Delivery(
                    NotificationCategory.DOCUMENT_NOTIFICATION,
                    messageProvider.childDocumentNotification(language, notification),
                    path = "/child-documents/${notification.documentId}",
                    tag = "document-${notification.documentId}",
                )

            is CitizenPushNotification.InformalDocument ->
                Delivery(
                    NotificationCategory.INFORMAL_DOCUMENT_NOTIFICATION,
                    messageProvider.pedagogicalDocumentNotification(language, notification),
                    path = "/children/${notification.childId}",
                    tag = "informal-document-${notification.childId}",
                )

            is CitizenPushNotification.MissingReservations ->
                Delivery(
                    NotificationCategory.ATTENDANCE_RESERVATION_NOTIFICATION,
                    messageProvider.missingReservationsNotification(language, notification),
                    path =
                        "/calendar?modal=reservations&startDate=${notification.range.start}&endDate=${notification.range.end}",
                    tag = "missing-reservations",
                    ttl = ttlUntil(now, notification.deadline),
                )

            is CitizenPushNotification.MissingHolidayReservations ->
                Delivery(
                    NotificationCategory.ATTENDANCE_RESERVATION_NOTIFICATION,
                    messageProvider.missingHolidayReservationsNotification(language, notification),
                    path = "/calendar?modal=holidays",
                    tag = "missing-holiday-reservations",
                    ttl = ttlUntil(now, HelsinkiDateTime.of(notification.deadline, LocalTime.MAX)),
                )

            is CitizenPushNotification.DiscussionSurvey ->
                Delivery(
                    NotificationCategory.DISCUSSION_TIME_NOTIFICATION,
                    messageProvider.discussionSurveyNotification(language, notification),
                    path = "/calendar?modal=discussions",
                    tag = "discussion-survey-${notification.eventId}",
                )

            is CitizenPushNotification.DiscussionTime ->
                Delivery(
                    NotificationCategory.DISCUSSION_TIME_NOTIFICATION,
                    messageProvider.discussionTimeNotification(language, notification),
                    path = "/calendar?day=${notification.date}",
                    tag = "discussion-time-${notification.eventTimeId}",
                    ttl =
                        when (notification.event) {
                            DiscussionTimePushNotificationEvent.REMINDER ->
                                ttlUntil(
                                    now,
                                    HelsinkiDateTime.of(notification.date, notification.startTime),
                                )
                            else -> DEFAULT_TTL
                        },
                )
        }

    private fun ttlUntil(now: HelsinkiDateTime, deadline: HelsinkiDateTime): Duration =
        Duration.between(now.toInstant(), deadline.toInstant()).coerceIn(MIN_TTL, MAX_TTL)

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
                    title = truncate(delivery.content.title, MAX_TITLE_LENGTH),
                    // URL is not visible to the citizens, so we can always use the Finnish URL
                    navigate = env.frontendBaseUrlFi + delivery.path,
                    body = delivery.content.body?.let { truncate(it, MAX_BODY_LENGTH) },
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

/** Cuts [text] to at most [maxLength] characters, the last being an ellipsis */
internal fun truncate(text: String, maxLength: Int): String =
    if (text.codePointCount(0, text.length) <= maxLength) text
    else text.substring(0, text.offsetByCodePoints(0, maxLength - 1)) + "…"

data class Delivery(
    val category: NotificationCategory?,
    val content: PushNotificationContent,
    val path: String,
    val tag: String,
    val ttl: Duration = DEFAULT_TTL,
)
