// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.webpush

import evaka.core.shared.domain.UiLanguage

data class PushNotificationContent(val title: String, val body: String?)

data class MessagePushNotificationData(
    val urgent: Boolean,
    val sensitive: Boolean,
    val senderName: String,
    val title: String,
    val content: String,
)

interface PushNotificationMessageProvider {
    fun messageNotification(
        language: UiLanguage,
        data: MessagePushNotificationData,
    ): PushNotificationContent

    fun testNotification(language: UiLanguage): PushNotificationContent

    fun feeDecisionNotification(
        language: UiLanguage,
        notification: CitizenPushNotification.FeeDecision,
    ): PushNotificationContent

    fun voucherValueDecisionNotification(
        language: UiLanguage,
        notification: CitizenPushNotification.VoucherValueDecision,
    ): PushNotificationContent

    fun applicationDecisionsNotification(
        language: UiLanguage,
        notification: CitizenPushNotification.ApplicationDecisions,
    ): PushNotificationContent

    fun pendingDecisionsNotification(
        language: UiLanguage,
        decisions: List<PendingDecision>,
    ): PushNotificationContent

    fun absenceApplicationDecisionNotification(
        language: UiLanguage,
        notification: CitizenPushNotification.AbsenceApplicationDecision,
    ): PushNotificationContent

    fun serviceApplicationDecisionNotification(
        language: UiLanguage,
        notification: CitizenPushNotification.ServiceApplicationDecision,
    ): PushNotificationContent

    fun incomeNotification(
        language: UiLanguage,
        notification: CitizenPushNotification.Income,
    ): PushNotificationContent

    fun calendarEventNotification(
        language: UiLanguage,
        events: List<CalendarEventSummary>,
    ): PushNotificationContent

    fun childDocumentNotification(
        language: UiLanguage,
        notification: CitizenPushNotification.Document,
    ): PushNotificationContent

    fun pedagogicalDocumentNotification(
        language: UiLanguage,
        notification: CitizenPushNotification.InformalDocument,
    ): PushNotificationContent

    fun missingReservationsNotification(
        language: UiLanguage,
        notification: CitizenPushNotification.MissingReservations,
    ): PushNotificationContent

    fun missingHolidayReservationsNotification(
        language: UiLanguage,
        notification: CitizenPushNotification.MissingHolidayReservations,
    ): PushNotificationContent

    fun discussionSurveyNotification(
        language: UiLanguage,
        notification: CitizenPushNotification.DiscussionSurvey,
    ): PushNotificationContent

    fun discussionTimeNotification(
        language: UiLanguage,
        notification: CitizenPushNotification.DiscussionTime,
    ): PushNotificationContent
}
