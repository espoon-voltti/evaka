// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.emailclient

import fi.espoo.evaka.calendarevent.CalendarEventTime
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.invoicing.domain.FinanceDecisionType
import fi.espoo.evaka.invoicing.service.IncomeNotificationType
import fi.espoo.evaka.messaging.MessageType
import fi.espoo.evaka.shared.CalendarEventId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.HtmlSafe
import fi.espoo.evaka.shared.MessageThreadId
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate
import java.time.LocalTime
import org.jsoup.Jsoup

data class EmailContent(
    val subject: String,
    val text: String,
    @org.intellij.lang.annotations.Language("html") val html: String,
) {
    companion object {
        private val TOO_MANY_NEWLINES = Regex("\n{3,}")

        fun fromHtml(
            subject: String,
            @org.intellij.lang.annotations.Language("html") html: String,
        ) =
            Jsoup.parseBodyFragment(html).let { doc ->
                val parsedHtml = doc.body().html()

                doc.select("hr").forEach { it.replaceWith(doc.createElement("p").text("-----")) }
                doc.select("p").forEach { it.prependText("\n").appendText("\n") }
                doc.select("a").forEach { a ->
                    val replacement = doc.createElement("a")
                    replacement.text(a.attr("href").removePrefix("mailto:"))
                    a.replaceWith(replacement)
                }

                EmailContent(
                    subject,
                    text =
                        doc.body()
                            .wholeText()
                            .lineSequence()
                            .joinToString(separator = "\n") { it.trim() }
                            .replace(TOO_MANY_NEWLINES, "\n\n")
                            .trim(),
                    html = parsedHtml,
                )
            }
    }
}

/** Use http://localhost:9099/api/internal/dev-api/email-content to preview email messages */
interface IEmailMessageProvider {
    fun pendingDecisionNotification(language: Language): EmailContent

    fun clubApplicationReceived(language: Language): EmailContent

    fun daycareApplicationReceived(language: Language): EmailContent

    fun preschoolApplicationReceived(
        language: Language,
        withinApplicationPeriod: Boolean,
    ): EmailContent

    fun assistanceNeedDecisionNotification(language: Language): EmailContent

    fun assistanceNeedPreschoolDecisionNotification(language: Language): EmailContent

    fun missingReservationsNotification(
        language: Language,
        checkedRange: FiniteDateRange,
    ): EmailContent

    fun missingHolidayReservationsNotification(language: Language): EmailContent

    fun messageNotification(language: Language, thread: MessageThreadData): EmailContent

    fun messageNotification(
        language: Language,
        thread: MessageThreadData,
        isSenderMunicipalAccount: Boolean,
    ): EmailContent = messageNotification(language, thread)

    fun childDocumentNotification(language: Language, childId: ChildId): EmailContent

    fun pedagogicalDocumentNotification(language: Language, childId: ChildId): EmailContent

    fun incomeNotification(
        notificationType: IncomeNotificationType,
        language: Language,
    ): EmailContent

    fun calendarEventNotification(
        language: Language,
        events: List<CalendarEventNotificationData>,
    ): EmailContent

    fun discussionSurveyReservationNotification(
        language: Language,
        notificationDetails: DiscussionSurveyReservationNotificationData,
    ): EmailContent

    fun discussionSurveyReservationCancellationNotification(
        language: Language,
        notificationDetails: DiscussionSurveyReservationNotificationData,
    ): EmailContent

    fun discussionSurveyCreationNotification(
        language: Language,
        notificationDetails: DiscussionSurveyCreationNotificationData,
    ): EmailContent

    fun discussionTimeReservationReminder(
        language: Language,
        reminderData: DiscussionTimeReminderData,
    ): EmailContent

    fun financeDecisionNotification(decisionType: FinanceDecisionType): EmailContent

    fun serviceApplicationDecidedNotification(accepted: Boolean, startDate: LocalDate): EmailContent
}

data class MessageThreadData(
    val id: MessageThreadId,
    val type: MessageType,
    val title: HtmlSafe<String>,
    val urgent: Boolean,
    val sensitive: Boolean,
    val isCopy: Boolean,
)

data class CalendarEventNotificationData(val title: HtmlSafe<String>, val period: FiniteDateRange)

data class DiscussionTimeReminderData(
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
)

data class DiscussionSurveyReservationNotificationData(val calendarEventTime: CalendarEventTime)

data class DiscussionSurveyCreationNotificationData(
    val eventTitle: HtmlSafe<String>,
    val eventDescription: HtmlSafe<String>,
    val eventId: CalendarEventId,
)
