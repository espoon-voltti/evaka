//  SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
//  SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

import fi.espoo.evaka.ConstList
import fi.espoo.evaka.shared.db.DatabaseEnum

data class PersonalDataUpdate(
    val preferredName: String,
    val phone: String,
    val backupPhone: String,
    val email: String,
)

@ConstList("emailMessageTypes")
enum class EmailMessageType : DatabaseEnum {
    /**
     * Messages sent in response to a user's action (e.g. "your application was received"). These
     * messages are always sent to the receiver.
     */
    TRANSACTIONAL,

    /** Notifications about new eVaka messages */
    MESSAGE_NOTIFICATION,

    /** Notifications about new eVaka bulletins (yleiset tiedotteet) */
    BULLETIN_NOTIFICATION,

    /** Notifications about new eVaka bulletins (tiedotteet) sent by unit supervisor */
    BULLETIN_FROM_SUPERVISOR_NOTIFICATION,

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
    DISCUSSION_TIME_RESERVATION_REMINDER;

    override val sqlType: String = "email_message_type"

    companion object {
        val alwaysEnabled: Set<EmailMessageType> = setOf(TRANSACTIONAL)
    }
}
