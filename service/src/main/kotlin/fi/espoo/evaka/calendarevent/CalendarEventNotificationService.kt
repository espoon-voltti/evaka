// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.calendarevent

import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.emailclient.CalendarEventNotificationData
import fi.espoo.evaka.emailclient.DiscussionSurveyCreationNotificationData
import fi.espoo.evaka.emailclient.DiscussionSurveyReservationNotificationData
import fi.espoo.evaka.emailclient.Email
import fi.espoo.evaka.emailclient.EmailClient
import fi.espoo.evaka.emailclient.IEmailMessageProvider
import fi.espoo.evaka.pis.EmailMessageType
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.shared.HtmlSafe
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class CalendarEventNotificationService(
    private val emailClient: EmailClient,
    private val emailEnv: EmailEnv,
    private val emailMessageProvider: IEmailMessageProvider,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
) {

    init {
        asyncJobRunner.registerHandler(::runSendDiscussionSurveyReservationMessage)
        asyncJobRunner.registerHandler(::runSendDiscussionSurveyReservationCancellationMessage)
        asyncJobRunner.registerHandler(::runSendCalendarEventDigestEmail)
        asyncJobRunner.registerHandler(::runSendDiscussionTimeReminder)
        asyncJobRunner.registerHandler(::runSendDiscussionSurveyDigest)
    }

    fun scheduleDiscussionTimeReminders(dbc: Database.Connection, now: HelsinkiDateTime) {
        dbc.transaction { tx ->
            val recipientEventTimes =
                tx.getRecipientsForEventTimeRemindersAt(now.plusDays(2).toLocalDate())

            logger.info("Scheduling ${recipientEventTimes.size} discussion time reminder mails")
            asyncJobRunner.plan(
                tx,
                payloads =
                    recipientEventTimes.map {
                        AsyncJob.SendDiscussionReservationReminderEmail(
                            it.parentId,
                            it.language,
                            it.eventTimeId,
                        )
                    },
                runAt = now,
            )
        }
    }

    fun scheduleDiscussionSurveyDigests(dbc: Database.Connection, now: HelsinkiDateTime) {
        dbc.transaction { tx ->
            val parentsWithDiscussionSurveys =
                tx.getParentsWithNewDiscussionSurveysAfter(now.minusHours(24))

            logger.info(
                "Scheduling survey creation digests for ${parentsWithDiscussionSurveys.size} recipients"
            )
            asyncJobRunner.plan(
                tx,
                payloads =
                    parentsWithDiscussionSurveys
                        .map { recipient ->
                            recipient.surveys.map {
                                AsyncJob.SendDiscussionSurveyCreationNotificationEmail(
                                    recipientId = recipient.parentId,
                                    language = recipient.language,
                                    eventId = it,
                                )
                            }
                        }
                        .flatten(),
                runAt = now,
            )
        }
    }

    fun runSendDiscussionSurveyDigest(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.SendDiscussionSurveyCreationNotificationEmail,
    ) {

        val eventData =
            db.read {
                it.getCalendarEventById(msg.eventId) ?: throw NotFound("No discussion survey found")
            }
        val fromAddress = emailEnv.sender(Language.fi)
        val content =
            emailMessageProvider.discussionSurveyCreationNotification(
                language = msg.language,
                notificationDetails =
                    DiscussionSurveyCreationNotificationData(
                        eventId = msg.eventId,
                        eventTitle = HtmlSafe(eventData.title),
                        eventDescription = HtmlSafe(eventData.description),
                    ),
            )
        Email.create(
                db,
                msg.recipientId,
                EmailMessageType.DISCUSSION_TIME_NOTIFICATION,
                fromAddress,
                content,
                "${msg.recipientId}: ${msg.eventId}",
            )
            ?.also { emailClient.send(it) }

        logger.info {
            "Successfully processed discussion survey creation email (recipientId: ${msg.recipientId}, eventId: ${msg.eventId})."
        }
    }

    fun scheduleCalendarEventDigestEmails(dbc: Database.Connection, now: HelsinkiDateTime) {
        dbc.transaction { tx ->
            val parents = tx.getParentsWithNewEventsAfter(now.toLocalDate(), now.minusHours(24))
            logger.info { "Scheduling calendar event notifications to ${parents.size} parents" }
            asyncJobRunner.plan(
                tx,
                parents.map {
                    AsyncJob.SendCalendarEventDigestEmail(it.parentId, it.language, it.events)
                },
                runAt = now,
            )
        }
    }

    fun runSendCalendarEventDigestEmail(
        dbc: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.SendCalendarEventDigestEmail,
    ) {
        val notificationData =
            dbc.read { tx -> tx.getCalendarEventsById(msg.events.toSet()) }
                .map { CalendarEventNotificationData(HtmlSafe(it.title), it.period) }
                .sortedWith(compareBy({ it.period.start }, { it.title.toString() }))
        if (notificationData.isEmpty()) {
            logger.info { "No events to notify for parent ${msg.parentId}" }
            return
        }
        Email.create(
                dbc,
                msg.parentId,
                EmailMessageType.CALENDAR_EVENT_NOTIFICATION,
                emailEnv.sender(msg.language),
                emailMessageProvider.calendarEventNotification(msg.language, notificationData),
                "${clock.today()}:${msg.parentId}",
            )
            ?.also { emailClient.send(it) }
        logger.info {
            "Successfully sent calendar event digest email (personId: ${msg.parentId}, event count: ${notificationData.size})."
        }
    }

    fun runSendDiscussionSurveyReservationMessage(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.SendDiscussionSurveyReservationEmail,
    ) {
        val eventTime = msg.calendarEventTime
        logger.info {
            "Sending discussion time reservation email for (recipientId: ${msg.recipientId}, eventTimeId: ${eventTime.id})"
        }

        val child =
            db.read {
                it.getPersonById(msg.childId)
                    ?: throw NotFound("Child for discussion time reservation not found")
            }
        val fromAddress = emailEnv.sender(Language.fi)
        val content =
            emailMessageProvider.discussionSurveyReservationNotification(
                language = msg.language,
                notificationDetails =
                    DiscussionSurveyReservationNotificationData(calendarEventTime = eventTime),
            )
        Email.create(
                db,
                msg.recipientId,
                EmailMessageType.DISCUSSION_TIME_NOTIFICATION,
                fromAddress,
                content,
                "${eventTime.id} - ${msg.recipientId}",
            )
            ?.also { emailClient.send(it) }

        logger.info {
            "Successfully sent discussion time reservation email (recipientId: ${msg.recipientId}, eventTimeId: ${eventTime.id})."
        }
    }

    fun runSendDiscussionSurveyReservationCancellationMessage(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.SendDiscussionSurveyReservationCancellationEmail,
    ) {
        val eventTime = msg.calendarEventTime
        logger.info {
            "Sending discussion time reservation cancellation email for (recipientId: ${msg.recipientId}, eventTimeId: ${eventTime.id})"
        }
        val child =
            db.read {
                it.getPersonById(msg.childId)
                    ?: throw NotFound("Child for discussion time reservation not found")
            }
        val fromAddress = emailEnv.sender(Language.fi)
        val content =
            emailMessageProvider.discussionSurveyReservationCancellationNotification(
                language = msg.language,
                notificationDetails =
                    DiscussionSurveyReservationNotificationData(calendarEventTime = eventTime),
            )
        Email.create(
                db,
                msg.recipientId,
                EmailMessageType.DISCUSSION_TIME_NOTIFICATION,
                fromAddress,
                content,
                "${eventTime.id} - ${msg.recipientId}",
            )
            ?.also { emailClient.send(it) }

        logger.info {
            "Successfully sent discussion time reservation cancellation email (recipientId: ${msg.recipientId}, eventTimeId: ${eventTime.id})."
        }
    }

    fun runSendDiscussionTimeReminder(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.SendDiscussionReservationReminderEmail,
    ) {
        logger.info {
            "Processing discussion time reminder email for (recipientId: ${msg.recipientId}, eventTimeId: ${msg.eventTimeId})"
        }

        val messageDetails =
            db.read { it.getEventTimeReminderInfo(msg.eventTimeId) }
                ?: throw NotFound(
                    "Event time to remind of not found (eventTimeId: ${msg.eventTimeId})"
                )
        Email.create(
                db,
                msg.recipientId,
                EmailMessageType.DISCUSSION_TIME_NOTIFICATION,
                emailEnv.sender(msg.recipientLanguage),
                emailMessageProvider.discussionTimeReservationReminder(
                    msg.recipientLanguage,
                    messageDetails,
                ),
                "${msg.recipientId}: ${msg.eventTimeId}",
            )
            ?.also { content -> emailClient.send(content) }
    }
}
