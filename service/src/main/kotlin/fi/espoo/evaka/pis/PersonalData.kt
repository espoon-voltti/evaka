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

    /**
     * Notifications about new eVaka messages from daycare staff including bulletins sent by unit
     * supervisor
     */
    MESSAGE_NOTIFICATION,

    /** Notifications about new general bulletins from municipal accounts (tiedotteet) */
    BULLETIN_NOTIFICATION,

    /** Income related notifications such as reminders about expiring or missing income info */
    INCOME_NOTIFICATION,

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
    ATTENDANCE_RESERVATION_NOTIFICATION,

    /** Discussion time related notifications */
    DISCUSSION_TIME_NOTIFICATION;

    override val sqlType: String = "email_message_type"

    companion object {
        val alwaysEnabled: Set<EmailMessageType> = setOf(TRANSACTIONAL)
    }
}
