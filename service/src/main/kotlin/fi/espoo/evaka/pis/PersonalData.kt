//  SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
//  SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

data class PersonalDataUpdate(
    val preferredName: String,
    val phone: String,
    val backupPhone: String,
    val email: String,
)

enum class EmailMessageType {
    /**
     * Messages sent in response to a user's action (e.g. "your application was received"). These
     * messages are always sent to the receiver.
     */
    TRANSACTIONAL,

    /** Notifications about new eVaka messages */
    MESSAGE_NOTIFICATION,

    /** Notifications about new eVaka bulletins (tiedotteet) */
    BULLETIN_NOTIFICATION,

    /** Reminders about expiring or missing income info */
    OUTDATED_INCOME_NOTIFICATION,

    /** Reminders for new customers about income info */
    NEW_CUSTOMER_INCOME_NOTIFICATION,

    /** Notifications about new calendar events of daycare groups */
    CALENDAR_EVENT_NOTIFICATION,

    /**
     * Notifications about new decisions (e.g. daycare decision, assistance decision, finance
     * decisions)
     */
    DECISION_NOTIFICATION,

    /**
     * Notifications about *official documents* that are not decisions (e.g. vasu, pedagogical
     * assesments, etc.)
     */
    DOCUMENT_NOTIFICATION,

    /** Notifications about informal documents (photos of children's drawings, etc.) */
    INFORMAL_DOCUMENT_NOTIFICATION,

    /** Reminders about making attendance reservations */
    MISSING_ATTENDANCE_RESERVATION_NOTIFICATION,

    /** Reminders about missing holiday attendance reservations */
    MISSING_HOLIDAY_ATTENDANCE_RESERVATION_NOTIFICATION,

    /** Confirmation email for discussion time reservation */
    DISCUSSION_TIME_RESERVATION_CONFIRMATION,

    /** Notification on the creation of a new discussion survey * */
    DISCUSSION_SURVEY_CREATION_NOTIFICATION,

    /** Reminder of an impending discussion time reservation * */
    DISCUSSION_TIME_RESERVATION_REMINDER,
}

data class EmailNotificationSettings(
    val message: Boolean,
    val bulletin: Boolean,
    val outdatedIncome: Boolean,
    val calendarEvent: Boolean,
    val decision: Boolean,
    val document: Boolean,
    val informalDocument: Boolean,
    val missingAttendanceReservation: Boolean,
    val discussionTimeReservationConfirmation: Boolean,
    val discussionSurveyCreationNotification: Boolean,
    val discussionTimeReservationReminder: Boolean,
) {
    fun toNotificationTypes() =
        listOfNotNull(
            EmailMessageType.TRANSACTIONAL, // always enabled
            EmailMessageType.MESSAGE_NOTIFICATION.takeIf { message },
            EmailMessageType.BULLETIN_NOTIFICATION.takeIf { bulletin },
            EmailMessageType.OUTDATED_INCOME_NOTIFICATION.takeIf { outdatedIncome },
            EmailMessageType.CALENDAR_EVENT_NOTIFICATION.takeIf { calendarEvent },
            EmailMessageType.DECISION_NOTIFICATION.takeIf { decision },
            EmailMessageType.DOCUMENT_NOTIFICATION.takeIf { document },
            EmailMessageType.INFORMAL_DOCUMENT_NOTIFICATION.takeIf { informalDocument },
            EmailMessageType.MISSING_ATTENDANCE_RESERVATION_NOTIFICATION.takeIf {
                missingAttendanceReservation
            },
            EmailMessageType.DISCUSSION_TIME_RESERVATION_CONFIRMATION.takeIf {
                discussionTimeReservationConfirmation
            },
            EmailMessageType.DISCUSSION_SURVEY_CREATION_NOTIFICATION.takeIf {
                discussionSurveyCreationNotification
            },
            EmailMessageType.DISCUSSION_TIME_RESERVATION_REMINDER.takeIf {
                discussionTimeReservationReminder
            },
        )

    companion object {
        fun fromNotificationTypes(enabledNotificationTypes: List<EmailMessageType>?) =
            if (enabledNotificationTypes == null) {
                // All are enabled by default
                EmailNotificationSettings(
                    message = true,
                    bulletin = true,
                    outdatedIncome = true,
                    calendarEvent = true,
                    decision = true,
                    document = true,
                    informalDocument = true,
                    missingAttendanceReservation = true,
                    discussionTimeReservationConfirmation = true,
                    discussionSurveyCreationNotification = true,
                    discussionTimeReservationReminder = true,
                )
            } else {
                EmailNotificationSettings(
                    message = EmailMessageType.MESSAGE_NOTIFICATION in enabledNotificationTypes,
                    bulletin = EmailMessageType.BULLETIN_NOTIFICATION in enabledNotificationTypes,
                    outdatedIncome =
                        EmailMessageType.OUTDATED_INCOME_NOTIFICATION in enabledNotificationTypes,
                    calendarEvent =
                        EmailMessageType.CALENDAR_EVENT_NOTIFICATION in enabledNotificationTypes,
                    decision = EmailMessageType.DECISION_NOTIFICATION in enabledNotificationTypes,
                    document = EmailMessageType.DOCUMENT_NOTIFICATION in enabledNotificationTypes,
                    informalDocument =
                        EmailMessageType.INFORMAL_DOCUMENT_NOTIFICATION in enabledNotificationTypes,
                    missingAttendanceReservation =
                        EmailMessageType.MISSING_ATTENDANCE_RESERVATION_NOTIFICATION in
                            enabledNotificationTypes,
                    discussionTimeReservationConfirmation =
                        EmailMessageType.DISCUSSION_TIME_RESERVATION_CONFIRMATION in
                            enabledNotificationTypes,
                    discussionSurveyCreationNotification =
                        EmailMessageType.DISCUSSION_SURVEY_CREATION_NOTIFICATION in
                            enabledNotificationTypes,
                    discussionTimeReservationReminder =
                        EmailMessageType.DISCUSSION_TIME_RESERVATION_REMINDER in
                            enabledNotificationTypes,
                )
            }
    }
}
