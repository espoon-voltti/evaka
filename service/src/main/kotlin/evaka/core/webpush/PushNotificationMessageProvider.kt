// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.webpush

import evaka.core.document.childdocument.ChildDocumentNotificationType
import evaka.core.invoicing.service.IncomeNotificationType
import evaka.core.messaging.MessageType
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.UiLanguage
import java.time.LocalDate
import java.time.LocalTime

data class PushNotificationContent(val title: String, val body: String?)

data class MessagePushNotificationData(
    val type: MessageType,
    val urgent: Boolean,
    val sensitive: Boolean,
    val senderName: String?,
    val title: String,
    val isSenderMunicipalAccount: Boolean,
)

interface PushNotificationMessageProvider {
    fun messageNotification(
        language: UiLanguage,
        data: MessagePushNotificationData,
    ): PushNotificationContent

    fun testNotification(language: UiLanguage): PushNotificationContent

    fun decisionNotification(
        language: UiLanguage,
        kind: DecisionPushNotificationKind,
    ): PushNotificationContent

    fun childApplicationDecisionNotification(
        language: UiLanguage,
        kind: ChildApplicationDecisionKind,
    ): PushNotificationContent

    fun incomeNotification(
        language: UiLanguage,
        notificationType: IncomeNotificationType,
    ): PushNotificationContent

    /** [title] is set only when the digest holds exactly one event */
    fun calendarEventNotification(
        language: UiLanguage,
        count: Int,
        title: String?,
    ): PushNotificationContent

    fun childDocumentNotification(
        language: UiLanguage,
        notificationType: ChildDocumentNotificationType,
    ): PushNotificationContent

    fun pedagogicalDocumentNotification(language: UiLanguage): PushNotificationContent

    fun missingReservationsNotification(
        language: UiLanguage,
        range: FiniteDateRange,
    ): PushNotificationContent

    fun missingHolidayReservationsNotification(
        language: UiLanguage,
        deadline: LocalDate,
    ): PushNotificationContent

    fun discussionSurveyNotification(language: UiLanguage, title: String): PushNotificationContent

    fun discussionTimeNotification(
        language: UiLanguage,
        event: DiscussionTimePushNotificationEvent,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
    ): PushNotificationContent
}
