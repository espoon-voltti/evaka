// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.shared.dev

import evaka.core.calendarevent.CalendarEventTime
import evaka.core.daycare.domain.Language
import evaka.core.document.childdocument.ChildDocumentNotificationType
import evaka.core.emailclient.CalendarEventNotificationData
import evaka.core.emailclient.DiscussionSurveyCreationNotificationData
import evaka.core.emailclient.DiscussionSurveyReservationNotificationData
import evaka.core.emailclient.DiscussionTimeReminderData
import evaka.core.emailclient.EmailContent
import evaka.core.emailclient.IEmailMessageProvider
import evaka.core.emailclient.MessageThreadData
import evaka.core.invoicing.domain.FinanceDecisionType
import evaka.core.invoicing.service.IncomeNotificationType
import evaka.core.messaging.AccountType
import evaka.core.messaging.MessageType
import evaka.core.shared.ApplicationId
import evaka.core.shared.CalendarEventId
import evaka.core.shared.CalendarEventTimeId
import evaka.core.shared.ChildId
import evaka.core.shared.HtmlSafe
import evaka.core.shared.MessageThreadId
import evaka.core.shared.domain.FiniteDateRange
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@Suppress("EmailMessageFilter", "ktlint:standard:enum-entry-name-case")
enum class EmailMessageFilter {
    pendingDecisionNotification,
    clubApplicationReceived,
    daycareApplicationReceived,
    preschoolApplicationReceived,
    preschoolApplicationReceivedOutsideApplicationPeriod,
    missingReservationsNotification,
    missingHolidayReservationsNotification,
    messageNotification,
    urgentMessageNotification,
    applicationMessageNotification,
    bulletinNotification,
    urgentBulletinNotification,
    municipalBulletinNotification,
    childDocumentNotification,
    editableChildDocumentNotification,
    decisionChildDocumentNotification,
    pedagogicalDocumentNotification,
    outdatedIncomeNotification,
    outdatedIncomeReminderNotification,
    outdatedIncomeExpiredNotification,
    newCustomerIncomeNotification,
    calendarEventNotification,
    decisionNotification,
    feeDecisionNotification,
    voucherValueDecisionNotification,
    discussionTimeReservation,
    discussionTimeCancellation,
    discussionSurveyCreation,
    discussionTimeReservationReminder,
    absenceApplicationAcceptedNotification,
    absenceApplicationRejectedNotification,
    serviceApplicationAcceptedNotification,
    serviceApplicationRejectedNotification,
    confirmationCode,
    passwordChanged,
    emailChanged,
    newBrowserLoginNotification,
    passkeyAdded,
    passkeyRemoved,
    weakCredentialsRemoved,
}

fun emailPreviewContent(
    provider: IEmailMessageProvider,
    message: EmailMessageFilter,
    language: Language,
): EmailContent =
    when (message) {
        EmailMessageFilter.pendingDecisionNotification -> {
            provider.pendingDecisionNotification(language)
        }

        EmailMessageFilter.clubApplicationReceived -> {
            provider.clubApplicationReceived(language)
        }

        EmailMessageFilter.daycareApplicationReceived -> {
            provider.daycareApplicationReceived(language)
        }

        EmailMessageFilter.preschoolApplicationReceived -> {
            provider.preschoolApplicationReceived(language, true)
        }

        EmailMessageFilter.preschoolApplicationReceivedOutsideApplicationPeriod -> {
            provider.preschoolApplicationReceived(language, false)
        }

        EmailMessageFilter.missingReservationsNotification -> {
            provider.missingReservationsNotification(
                language,
                FiniteDateRange(LocalDate.now().minusDays(7), LocalDate.now()),
            )
        }

        EmailMessageFilter.missingHolidayReservationsNotification -> {
            provider.missingHolidayReservationsNotification(language)
        }

        EmailMessageFilter.messageNotification -> {
            exampleMessageNotification(provider, language, MessageType.MESSAGE)
        }

        EmailMessageFilter.urgentMessageNotification -> {
            exampleMessageNotification(provider, language, MessageType.MESSAGE, urgent = true)
        }

        EmailMessageFilter.applicationMessageNotification -> {
            exampleMessageNotification(
                provider,
                language,
                MessageType.MESSAGE,
                applicationId = ApplicationId(UUID.randomUUID()),
            )
        }

        EmailMessageFilter.bulletinNotification -> {
            exampleMessageNotification(provider, language, MessageType.BULLETIN)
        }

        EmailMessageFilter.urgentBulletinNotification -> {
            exampleMessageNotification(provider, language, MessageType.BULLETIN, urgent = true)
        }

        EmailMessageFilter.municipalBulletinNotification -> {
            exampleMessageNotification(
                provider,
                language,
                MessageType.BULLETIN,
                isSenderMunicipalAccount = true,
            )
        }

        EmailMessageFilter.childDocumentNotification -> {
            exampleChildDocumentNotification(
                provider,
                language,
                ChildDocumentNotificationType.BASIC_DOCUMENT,
            )
        }

        EmailMessageFilter.editableChildDocumentNotification -> {
            exampleChildDocumentNotification(
                provider,
                language,
                ChildDocumentNotificationType.EDITABLE_DOCUMENT,
            )
        }

        EmailMessageFilter.decisionChildDocumentNotification -> {
            exampleChildDocumentNotification(
                provider,
                language,
                ChildDocumentNotificationType.DECISION_DOCUMENT,
            )
        }

        EmailMessageFilter.pedagogicalDocumentNotification -> {
            provider.pedagogicalDocumentNotification(language, ChildId(UUID.randomUUID()))
        }

        EmailMessageFilter.outdatedIncomeNotification -> {
            provider.incomeNotification(IncomeNotificationType.INITIAL_EMAIL, language)
        }

        EmailMessageFilter.outdatedIncomeReminderNotification -> {
            provider.incomeNotification(IncomeNotificationType.REMINDER_EMAIL, language)
        }

        EmailMessageFilter.outdatedIncomeExpiredNotification -> {
            provider.incomeNotification(IncomeNotificationType.EXPIRED_EMAIL, language)
        }

        EmailMessageFilter.newCustomerIncomeNotification -> {
            provider.incomeNotification(IncomeNotificationType.NEW_CUSTOMER, language)
        }

        EmailMessageFilter.calendarEventNotification -> {
            provider.calendarEventNotification(
                language,
                listOf(
                    CalendarEventNotificationData(
                        HtmlSafe("Esimerkki 1"),
                        FiniteDateRange(LocalDate.now(), LocalDate.now().plusDays(1)),
                        listOf(HtmlSafe("Ryhmä A")),
                    ),
                    CalendarEventNotificationData(
                        HtmlSafe("Esimerkki 2"),
                        FiniteDateRange(LocalDate.now().plusDays(7), LocalDate.now().plusDays(7)),
                        listOf(HtmlSafe("Ryhmä B"), HtmlSafe("Ryhmä C")),
                    ),
                ),
            )
        }

        EmailMessageFilter.decisionNotification -> {
            provider.decisionNotification()
        }

        EmailMessageFilter.feeDecisionNotification -> {
            provider.financeDecisionNotification(FinanceDecisionType.FEE_DECISION)
        }

        EmailMessageFilter.voucherValueDecisionNotification -> {
            provider.financeDecisionNotification(FinanceDecisionType.VOUCHER_VALUE_DECISION)
        }

        EmailMessageFilter.discussionTimeReservation -> {
            provider.discussionSurveyReservationNotification(
                language,
                DiscussionSurveyReservationNotificationData(
                    calendarEventTime = exampleCalendarEventTime()
                ),
            )
        }

        EmailMessageFilter.discussionTimeCancellation -> {
            provider.discussionSurveyReservationCancellationNotification(
                language,
                DiscussionSurveyReservationNotificationData(
                    calendarEventTime = exampleCalendarEventTime()
                ),
            )
        }

        EmailMessageFilter.discussionTimeReservationReminder -> {
            provider.discussionTimeReservationReminder(
                language,
                DiscussionTimeReminderData(
                    date = LocalDate.now(),
                    startTime = LocalTime.of(12, 20),
                    endTime = LocalTime.of(12, 50),
                ),
            )
        }

        EmailMessageFilter.discussionSurveyCreation -> {
            provider.discussionSurveyCreationNotification(
                language,
                DiscussionSurveyCreationNotificationData(
                    eventId = CalendarEventId(UUID.randomUUID()),
                    eventTitle = HtmlSafe("VASU-keskustelut 2029 syksy"),
                    eventDescription =
                        HtmlSafe(
                            "Hei, järjestämme keskustelut lasten varhaiskasvatussuunnitelmia varten viikolla 39. Varatkaa sopiva aika ja tulkaa juttelemaan päiväkodille. Tervetuloa ja nähdään paikanpäällä! Terveisin Testiryhmä ykkösen väki."
                        ),
                ),
            )
        }

        EmailMessageFilter.absenceApplicationAcceptedNotification -> {
            provider.absenceApplicationDecidedNotification(
                accepted = true,
                startDate = LocalDate.now(),
                endDate = LocalDate.now().plusWeeks(1),
            )
        }

        EmailMessageFilter.absenceApplicationRejectedNotification -> {
            provider.absenceApplicationDecidedNotification(
                accepted = false,
                startDate = LocalDate.now(),
                endDate = LocalDate.now().plusWeeks(1),
            )
        }

        EmailMessageFilter.serviceApplicationAcceptedNotification -> {
            provider.serviceApplicationDecidedNotification(
                accepted = true,
                startDate = LocalDate.now().plusMonths(1).withDayOfMonth(1),
            )
        }

        EmailMessageFilter.serviceApplicationRejectedNotification -> {
            provider.serviceApplicationDecidedNotification(
                accepted = false,
                startDate = LocalDate.now().plusMonths(1).withDayOfMonth(1),
            )
        }

        EmailMessageFilter.confirmationCode -> {
            provider.confirmationCode(HtmlSafe("123456"))
        }

        EmailMessageFilter.passwordChanged -> {
            provider.passwordChanged()
        }

        EmailMessageFilter.emailChanged -> {
            provider.emailChanged("uusi.osoite@example.com")
        }

        EmailMessageFilter.newBrowserLoginNotification -> {
            provider.newBrowserLoginNotification()
        }

        EmailMessageFilter.passkeyAdded -> {
            provider.passkeyAdded()
        }

        EmailMessageFilter.passkeyRemoved -> {
            provider.passkeyRemoved()
        }

        EmailMessageFilter.weakCredentialsRemoved -> {
            provider.weakCredentialsRemoved()
        }
    }

private fun exampleMessageNotification(
    provider: IEmailMessageProvider,
    language: Language,
    type: MessageType,
    urgent: Boolean = false,
    isSenderMunicipalAccount: Boolean = false,
    applicationId: ApplicationId? = null,
) =
    provider.messageNotification(
        language,
        MessageThreadData(
            id = MessageThreadId(UUID.randomUUID()),
            type = type,
            title = HtmlSafe("Testiviesti"),
            urgent = urgent,
            sensitive = false,
            senderName = HtmlSafe("Testiryhmä"),
            senderType = AccountType.GROUP,
            isCopy = false,
        ),
        isSenderMunicipalAccount,
        applicationId,
    )

private fun exampleChildDocumentNotification(
    provider: IEmailMessageProvider,
    language: Language,
    type: ChildDocumentNotificationType,
) = provider.childDocumentNotification(language, ChildId(UUID.randomUUID()), type)

private fun exampleCalendarEventTime() =
    CalendarEventTime(
        id = CalendarEventTimeId(UUID.randomUUID()),
        date = LocalDate.now(),
        startTime = LocalTime.of(12, 20),
        endTime = LocalTime.of(12, 50),
        childId = null,
    )
