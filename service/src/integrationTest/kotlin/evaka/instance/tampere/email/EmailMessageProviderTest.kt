// SPDX-FileCopyrightText: 2021 City of Tampere
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.tampere.email

import evaka.core.daycare.domain.Language
import evaka.core.emailclient.CalendarEventNotificationData
import evaka.core.emailclient.EmailContent
import evaka.core.emailclient.IEmailMessageProvider
import evaka.core.emailclient.MessageThreadData
import evaka.core.invoicing.domain.FinanceDecisionType
import evaka.core.invoicing.service.IncomeNotificationType
import evaka.core.messaging.AccountType
import evaka.core.messaging.MessageType
import evaka.core.shared.ChildId
import evaka.core.shared.HtmlSafe
import evaka.core.shared.MessageThreadId
import evaka.core.shared.domain.FiniteDateRange
import evaka.instance.tampere.AbstractTampereIntegrationTest
import java.time.LocalDate
import java.util.UUID
import java.util.stream.Stream
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

internal class EmailMessageProviderTest : AbstractTampereIntegrationTest() {

    @Autowired private lateinit var emailMessageProvider: IEmailMessageProvider

    @ParameterizedTest(name = "{0}")
    @MethodSource("contents")
    fun testContentDoNotContainEspooText(name: String, content: EmailContent) {
        assertNotContainEspooText(content.subject)
        assertNotContainEspooText(content.text)
        assertNotContainEspooText(content.html)
    }

    fun contents(): Stream<Arguments> =
        listOf(
                Arguments.of(
                    "daycareApplicationReceived",
                    emailMessageProvider.daycareApplicationReceived(Language.fi),
                ),
                Arguments.of(
                    "clubApplicationReceived",
                    emailMessageProvider.clubApplicationReceived(Language.fi),
                ),
                Arguments.of(
                    "pendingDecisionNotification",
                    emailMessageProvider.pendingDecisionNotification(Language.fi),
                ),
                Arguments.of(
                    "preschoolApplicationReceivedWithinApplicationPeriodTrue",
                    emailMessageProvider.preschoolApplicationReceived(Language.fi, true),
                ),
                Arguments.of(
                    "preschoolApplicationReceivedWithinApplicationPeriodFalse",
                    emailMessageProvider.preschoolApplicationReceived(Language.fi, false),
                ),
                Arguments.of(
                    "missingReservationsNotification",
                    emailMessageProvider.missingReservationsNotification(
                        Language.fi,
                        LocalDate.of(2023, 2, 13).let { FiniteDateRange(it, it.plusDays(6)) },
                    ),
                ),
                Arguments.of(
                    "messageNotification",
                    emailMessageProvider.messageNotification(
                        Language.fi,
                        MessageThreadData(
                            id = MessageThreadId(UUID.randomUUID()),
                            type = MessageType.MESSAGE,
                            title = HtmlSafe("Ensi viikolla uimaan"),
                            urgent = false,
                            sensitive = false,
                            senderName = HtmlSafe("Kaisa Kasvattaja"),
                            senderType = AccountType.PERSONAL,
                            isCopy = false,
                        ),
                    ),
                ),
                Arguments.of(
                    "messageNotification with subject",
                    emailMessageProvider.messageNotification(
                        Language.fi,
                        MessageThreadData(
                            id = MessageThreadId(UUID.randomUUID()),
                            type = MessageType.BULLETIN,
                            title = HtmlSafe("Ensi viikolla uimaan"),
                            urgent = false,
                            sensitive = false,
                            senderName = HtmlSafe("Kaisa Kasvattaja"),
                            senderType = AccountType.PERSONAL,
                            isCopy = false,
                        ),
                        isSenderMunicipalAccount = true,
                        applicationId = null,
                    ),
                ),
                Arguments.of(
                    "pedagogicalDocumentNotification",
                    emailMessageProvider.pedagogicalDocumentNotification(
                        Language.fi,
                        ChildId(UUID.randomUUID()),
                    ),
                ),
                Arguments.of(
                    "incomeNotification",
                    emailMessageProvider.incomeNotification(
                        IncomeNotificationType.INITIAL_EMAIL,
                        Language.fi,
                    ),
                ),
                Arguments.of(
                    "incomeNotification",
                    emailMessageProvider.incomeNotification(
                        IncomeNotificationType.REMINDER_EMAIL,
                        Language.fi,
                    ),
                ),
                Arguments.of(
                    "incomeNotification",
                    emailMessageProvider.incomeNotification(
                        IncomeNotificationType.EXPIRED_EMAIL,
                        Language.fi,
                    ),
                ),
                Arguments.of(
                    "incomeNotification",
                    emailMessageProvider.incomeNotification(
                        IncomeNotificationType.NEW_CUSTOMER,
                        Language.fi,
                    ),
                ),
                Arguments.of(
                    "calendarEventNotification",
                    emailMessageProvider.calendarEventNotification(
                        Language.fi,
                        listOf(
                            CalendarEventNotificationData(
                                HtmlSafe("Tapahtuma 1"),
                                FiniteDateRange(
                                    LocalDate.of(2023, 8, 21),
                                    LocalDate.of(2023, 8, 21),
                                ),
                                listOf(HtmlSafe("Ryhmä A")),
                            ),
                            CalendarEventNotificationData(
                                HtmlSafe("Tapahtuma 2"),
                                FiniteDateRange(
                                    LocalDate.of(2023, 8, 22),
                                    LocalDate.of(2023, 8, 23),
                                ),
                                listOf(HtmlSafe("Ryhmä B"), HtmlSafe("Ryhmä C")),
                            ),
                        ),
                    ),
                ),
                Arguments.of(
                    "financeDecisionNotification FEE_DECISION",
                    emailMessageProvider.financeDecisionNotification(
                        FinanceDecisionType.FEE_DECISION
                    ),
                ),
                Arguments.of(
                    "financeDecisionNotification VOUCHER_VALUE_DECISION",
                    emailMessageProvider.financeDecisionNotification(
                        FinanceDecisionType.VOUCHER_VALUE_DECISION
                    ),
                ),
            )
            .stream()

    private fun assertNotContainEspooText(message: String) {
        assertThat(message.also(::println))
            .isNotBlank
            .doesNotContainIgnoringCase("espoo")
            .doesNotContainIgnoringCase("esbo")
    }
}
