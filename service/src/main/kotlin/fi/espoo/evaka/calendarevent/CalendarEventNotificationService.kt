// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.calendarevent

import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.emailclient.DiscussionSurveyReservationNotificationData
import fi.espoo.evaka.emailclient.Email
import fi.espoo.evaka.emailclient.EmailClient
import fi.espoo.evaka.emailclient.IEmailMessageProvider
import fi.espoo.evaka.pis.EmailMessageType
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.pis.service.getChildGuardiansAndFosterParents
import fi.espoo.evaka.shared.CalendarEventTimeId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import java.time.LocalDate
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class CalendarEventNotificationService(
    private val emailClient: EmailClient,
    private val emailEnv: EmailEnv,
    private val emailMessageProvider: IEmailMessageProvider,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>
) {

    init {
        asyncJobRunner.registerHandler(::runSendDiscussionSurveyReservationMessage)
        asyncJobRunner.registerHandler(::runSendDiscussionSurveyReservationCancellationMessage)
    }

    fun sendCalendarEventDigests(dbc: Database.Connection, now: HelsinkiDateTime) {
        dbc.read { tx -> tx.getParentsWithNewEventsAfter(now.minusHours(24)) }
            .also { parents ->
                logger.info { "Sending calendar event notifications to ${parents.size} parents" }
            }
            .forEach { parent ->
                Email.create(
                        dbc,
                        parent.parentId,
                        EmailMessageType.CALENDAR_EVENT_NOTIFICATION,
                        emailEnv.sender(parent.language),
                        emailMessageProvider.calendarEventNotification(
                            parent.language,
                            parent.events
                        ),
                        "${now.toLocalDate()}:${parent.parentId}",
                    )
                    ?.also { emailClient.send(it) }
            }
    }

    fun runSendDiscussionSurveyReservationMessage(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.SendDiscussionSurveyReservationEmail
    ) {

        val eventTime = msg.calendarEventTime
        logger.info {
            "Sending discussion time reservation email for (eventTimeId: ${eventTime.id})"
        }

        val recipients = getRecipientsForChild(db, msg.childId)
        recipients.forEach { recipient ->
            val fromAddress = emailEnv.sender(Language.fi)
            val content =
                emailMessageProvider.discussionSurveyReservationNotification(
                    language = msg.language,
                    notificationDetails =
                        DiscussionSurveyReservationNotificationData(
                            unitName = msg.unitName,
                            title = msg.eventTitle,
                            calendarEventTime =
                                CalendarEventTime(
                                    id = eventTime.id,
                                    date = eventTime.date,
                                    startTime = eventTime.startTime,
                                    endTime = eventTime.endTime,
                                    childId = eventTime.childId
                                )
                        )
                )
            Email.create(
                    db,
                    recipient.id,
                    EmailMessageType.DISCUSSION_TIME_RESERVATION,
                    fromAddress,
                    content,
                    "${eventTime.id} - ${recipient.id}",
                )
                ?.also { emailClient.send(it) }
        }

        logger.info {
            "Successfully sent discussion time reservation emails (${recipients.size}) (id: ${eventTime.id})."
        }
    }

    fun runSendDiscussionSurveyReservationCancellationMessage(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.SendDiscussionSurveyReservationCancellationEmail
    ) {
        val eventTime = msg.calendarEventTime

        logger.info {
            "Sending discussion time reservation cancellation email for (eventTimeId: ${eventTime.id})"
        }

        val recipients = getRecipientsForChild(db, msg.childId)
        recipients.forEach { recipient ->
            val fromAddress = emailEnv.sender(Language.fi)
            val content =
                emailMessageProvider.discussionSurveyReservationCancellationNotification(
                    language = msg.language,
                    notificationDetails =
                        DiscussionSurveyReservationNotificationData(
                            unitName = msg.unitName,
                            title = msg.eventTitle,
                            calendarEventTime =
                                CalendarEventTime(
                                    id = eventTime.id,
                                    date = eventTime.date,
                                    startTime = eventTime.startTime,
                                    endTime = eventTime.endTime,
                                    childId = eventTime.childId
                                )
                        )
                )
            Email.create(
                    db,
                    recipient.id,
                    EmailMessageType.DISCUSSION_TIME_RESERVATION_CANCELLATION,
                    fromAddress,
                    content,
                    "${eventTime.id} - ${recipient.id}",
                )
                ?.also { emailClient.send(it) }
        }

        logger.info {
            "Successfully sent discussion time reservation cancellation emails (${recipients.size}) (id: ${eventTime.id})."
        }
    }

    private fun getRecipientsForChild(db: Database.Connection, childId: ChildId): List<PersonDTO> {
        return db.read { tx ->
            tx.getChildGuardiansAndFosterParents(childId, LocalDate.now()).mapNotNull {
                tx.getPersonById(it)
            }
        }
    }

    private fun getDiscussionDetailsForEventTime(
        db: Database.Connection,
        eventTimeId: CalendarEventTimeId
    ): DiscussionTimeDetailsRow {
        return db.read { tx ->
            tx.getDiscussionTimeDetailsByEventTimeId(eventTimeId)
                ?: throw NotFound("Event not found")
        }
    }
}
