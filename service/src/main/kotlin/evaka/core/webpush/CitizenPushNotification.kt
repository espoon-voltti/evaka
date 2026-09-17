// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.webpush

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import evaka.core.document.childdocument.ChildDocumentNotificationType
import evaka.core.invoicing.service.IncomeNotificationType
import evaka.core.shared.CalendarEventTimeId
import evaka.core.shared.ChildDocumentId
import evaka.core.shared.ChildId
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import java.time.LocalTime

/** A decision that the citizen finds on the decisions page */
enum class DecisionPushNotificationKind {
    APPLICATION,
    FEE,
    VOUCHER_VALUE,
    PENDING_APPROVAL,
}

/** A decided application that the citizen finds on the child's page */
enum class ChildApplicationDecisionKind {
    ABSENCE_APPLICATION,
    SERVICE_APPLICATION,
}

data class SingleCalendarEvent(val title: String, val date: LocalDate)

enum class DiscussionTimePushNotificationEvent {
    RESERVED,
    CANCELLED,
    REMINDER,
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
sealed interface CitizenPushNotification {
    /* Message is not included here, because it has custom sending logic. See CitizenMessagePushNotifications.kt */

    @JsonTypeName("DECISION")
    data class Decision(val kind: DecisionPushNotificationKind) : CitizenPushNotification

    @JsonTypeName("CHILD_APPLICATION_DECISION")
    data class ChildApplicationDecision(
        val kind: ChildApplicationDecisionKind,
        val childId: ChildId,
    ) : CitizenPushNotification

    @JsonTypeName("INCOME")
    data class Income(val notificationType: IncomeNotificationType) : CitizenPushNotification

    /** [single] is set only when the digest holds exactly one event */
    @JsonTypeName("CALENDAR_EVENTS")
    data class CalendarEvents(val count: Int, val single: SingleCalendarEvent?) :
        CitizenPushNotification

    @JsonTypeName("DOCUMENT")
    data class Document(
        val documentId: ChildDocumentId,
        val notificationType: ChildDocumentNotificationType,
    ) : CitizenPushNotification

    @JsonTypeName("INFORMAL_DOCUMENT")
    data class InformalDocument(val childId: ChildId) : CitizenPushNotification

    /**
     * [deadline] is when reservations for [range] close, after which the notification is useless
     */
    @JsonTypeName("MISSING_RESERVATIONS")
    data class MissingReservations(val range: FiniteDateRange, val deadline: HelsinkiDateTime) :
        CitizenPushNotification

    @JsonTypeName("MISSING_HOLIDAY_RESERVATIONS")
    data class MissingHolidayReservations(val deadline: LocalDate) : CitizenPushNotification

    @JsonTypeName("DISCUSSION_SURVEY")
    data class DiscussionSurvey(val title: String) : CitizenPushNotification

    @JsonTypeName("DISCUSSION_TIME")
    data class DiscussionTime(
        val eventTimeId: CalendarEventTimeId,
        val event: DiscussionTimePushNotificationEvent,
        val date: LocalDate,
        val startTime: LocalTime,
        val endTime: LocalTime,
    ) : CitizenPushNotification
}
