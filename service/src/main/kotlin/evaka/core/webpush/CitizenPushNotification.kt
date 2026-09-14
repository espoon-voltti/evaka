// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.webpush

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import evaka.core.decision.DecisionType
import evaka.core.document.childdocument.ChildDocumentNotificationType
import evaka.core.invoicing.service.IncomeNotificationType
import evaka.core.shared.ApplicationId
import evaka.core.shared.CalendarEventId
import evaka.core.shared.CalendarEventTimeId
import evaka.core.shared.ChildId
import evaka.core.shared.VoucherValueDecisionId
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import java.time.LocalTime

data class PendingDecision(val childName: String, val type: DecisionType, val unitName: String)

data class ApplicationDecision(
    val type: DecisionType,
    val unitName: String,
    val startDate: LocalDate,
)

data class CalendarEventSummary(
    val title: String,
    val description: String,
    val period: FiniteDateRange,
    val groupNames: List<String>,
)

enum class DiscussionTimePushNotificationEvent {
    CANCELLED,
    REMINDER,
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
sealed interface CitizenPushNotification {
    /* Message is not included here, because it has custom sending logic. See CitizenMessagePushNotifications.kt */

    @JsonTypeName("FEE_DECISION")
    data class FeeDecision(val childNames: List<String>) : CitizenPushNotification

    @JsonTypeName("VOUCHER_VALUE_DECISION")
    data class VoucherValueDecision(
        val decisionId: VoucherValueDecisionId,
        val childName: String,
        val unitName: String,
    ) : CitizenPushNotification

    @JsonTypeName("APPLICATION_DECISIONS")
    data class ApplicationDecisions(
        val applicationId: ApplicationId,
        val childName: String,
        val decisions: List<ApplicationDecision>,
        val answerRequired: Boolean,
    ) : CitizenPushNotification

    @JsonTypeName("PENDING_DECISIONS")
    data class PendingDecisions(val decisions: List<PendingDecision>) : CitizenPushNotification

    @JsonTypeName("ABSENCE_APPLICATION_DECISION")
    data class AbsenceApplicationDecision(
        val childName: String,
        val range: FiniteDateRange,
        val rejected: Boolean,
    ) : CitizenPushNotification

    @JsonTypeName("SERVICE_APPLICATION_DECISION")
    data class ServiceApplicationDecision(
        val childName: String,
        val serviceNeedNameFi: String,
        val serviceNeedNameSv: String,
        val serviceNeedNameEn: String,
        val startDate: LocalDate,
        val rejected: Boolean,
    ) : CitizenPushNotification

    /**
     * [expirationDate] is when the current income ends, [deadline] is when a new customer must
     * submit their income at the latest
     */
    @JsonTypeName("INCOME")
    data class Income(
        val notificationType: IncomeNotificationType,
        val expirationDate: LocalDate?,
        val deadline: LocalDate?,
    ) : CitizenPushNotification

    /** [events] are in the order they happen */
    @JsonTypeName("CALENDAR_EVENTS")
    data class CalendarEvents(val events: List<CalendarEventSummary>) : CitizenPushNotification

    @JsonTypeName("DOCUMENT")
    data class Document(
        val childId: ChildId,
        val notificationType: ChildDocumentNotificationType,
        val childName: String,
    ) : CitizenPushNotification

    @JsonTypeName("INFORMAL_DOCUMENT")
    data class InformalDocument(val childId: ChildId, val childName: String) :
        CitizenPushNotification

    /**
     * [deadline] is when reservations for [range] close, after which the notification is useless
     */
    @JsonTypeName("MISSING_RESERVATIONS")
    data class MissingReservations(
        val range: FiniteDateRange,
        val deadline: HelsinkiDateTime,
        val childNames: List<String>,
    ) : CitizenPushNotification

    @JsonTypeName("MISSING_HOLIDAY_RESERVATIONS")
    data class MissingHolidayReservations(
        val holidayPeriod: FiniteDateRange,
        val deadline: LocalDate,
    ) : CitizenPushNotification

    @JsonTypeName("DISCUSSION_SURVEY")
    data class DiscussionSurvey(
        val eventId: CalendarEventId,
        val title: String,
        val description: String,
    ) : CitizenPushNotification

    @JsonTypeName("DISCUSSION_TIME")
    data class DiscussionTime(
        val eventTimeId: CalendarEventTimeId,
        val event: DiscussionTimePushNotificationEvent,
        val date: LocalDate,
        val startTime: LocalTime,
        val endTime: LocalTime,
        val childName: String,
        val surveyTitle: String,
    ) : CitizenPushNotification
}
