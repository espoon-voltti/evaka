//  SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
//  SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

data class PersonalDataUpdate(
    val preferredName: String,
    val phone: String,
    val backupPhone: String,
    val email: String
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

    /** Notifications about new calendar events of daycare groups */
    CALENDAR_EVENT_NOTIFICATION,

    /** Notifications about new decisions, pedagogical documents, etc. */
    DOCUMENT_NOTIFICATION,

    /** Reminders about making attendance reservations */
    MISSING_ATTENDANCE_RESERVATION_NOTIFICATION
}

data class EmailNotificationSettings(
    val message: Boolean,
    val bulletin: Boolean,
    val outdatedIncome: Boolean,
    val calendarEvent: Boolean,
    val document: Boolean,
    val missingAttendanceReservation: Boolean
) {
    fun toNotificationTypes() =
        listOfNotNull(
            EmailMessageType.TRANSACTIONAL, // always enabled
            EmailMessageType.MESSAGE_NOTIFICATION.takeIf { message },
            EmailMessageType.BULLETIN_NOTIFICATION.takeIf { bulletin },
            EmailMessageType.OUTDATED_INCOME_NOTIFICATION.takeIf { outdatedIncome },
            EmailMessageType.CALENDAR_EVENT_NOTIFICATION.takeIf { calendarEvent },
            EmailMessageType.DOCUMENT_NOTIFICATION.takeIf { document },
            EmailMessageType.MISSING_ATTENDANCE_RESERVATION_NOTIFICATION.takeIf {
                missingAttendanceReservation
            }
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
                    document = true,
                    missingAttendanceReservation = true
                )
            } else {
                EmailNotificationSettings(
                    message = EmailMessageType.MESSAGE_NOTIFICATION in enabledNotificationTypes,
                    bulletin = EmailMessageType.BULLETIN_NOTIFICATION in enabledNotificationTypes,
                    outdatedIncome =
                        EmailMessageType.OUTDATED_INCOME_NOTIFICATION in enabledNotificationTypes,
                    calendarEvent =
                        EmailMessageType.CALENDAR_EVENT_NOTIFICATION in enabledNotificationTypes,
                    document = EmailMessageType.DOCUMENT_NOTIFICATION in enabledNotificationTypes,
                    missingAttendanceReservation =
                        EmailMessageType.MISSING_ATTENDANCE_RESERVATION_NOTIFICATION in
                            enabledNotificationTypes
                )
            }
    }
}
