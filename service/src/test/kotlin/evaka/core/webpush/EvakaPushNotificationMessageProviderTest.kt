// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.webpush

import evaka.core.decision.DecisionType
import evaka.core.shared.ApplicationId
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.UiLanguage
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class EvakaPushNotificationMessageProviderTest {
    private val provider = EvakaPushNotificationMessageProvider()

    private val message =
        MessagePushNotificationData(
            urgent = false,
            sensitive = false,
            senderName = "Kirsikan päiväkoti",
            title = "Retki",
            content = "Lähdemme retkelle\n\nperjantaina.",
        )

    @Test
    fun `message shows the sender, the title and the content on one line`() {
        assertEquals(
            PushNotificationContent(
                "Kirsikan päiväkoti",
                "Retki\nLähdemme retkelle perjantaina.",
            ),
            provider.messageNotification(UiLanguage.FI, message),
        )
    }

    @Test
    fun `sensitive message hides the title and the content`() {
        assertEquals(
            PushNotificationContent("Kiireellinen: Kirsikan päiväkoti", "Arkaluonteinen viesti"),
            provider.messageNotification(
                UiLanguage.FI,
                message.copy(urgent = true, sensitive = true),
            ),
        )
    }

    @Test
    fun `fee decision shows only the children and nothing of the family's finances`() {
        assertEquals(
            PushNotificationContent("Uusi maksupäätös", "Matti, Maija"),
            provider.feeDecisionNotification(
                UiLanguage.FI,
                CitizenPushNotification.FeeDecision(childNames = listOf("Matti", "Maija")),
            ),
        )
    }

    @Test
    fun `application decisions list each decision and ask for an answer when one is pending`() {
        assertEquals(
            PushNotificationContent(
                "Uusi päätös",
                "Matti\n" +
                    "Esiopetus, Kirsikan koulu 11.8.2026 alkaen\n" +
                    "Liittyvä varhaiskasvatus, Kirsikan koulu 11.8.2026 alkaen\n" +
                    "Hyväksy tai hylkää päätös eVakassa",
            ),
            provider.applicationDecisionsNotification(
                UiLanguage.FI,
                CitizenPushNotification.ApplicationDecisions(
                    applicationId = ApplicationId(UUID.randomUUID()),
                    childName = "Matti",
                    decisions =
                        listOf(
                            ApplicationDecision(
                                DecisionType.PRESCHOOL,
                                "Kirsikan koulu",
                                LocalDate.of(2026, 8, 11),
                            ),
                            ApplicationDecision(
                                DecisionType.PRESCHOOL_DAYCARE,
                                "Kirsikan koulu",
                                LocalDate.of(2026, 8, 11),
                            ),
                        ),
                    answerRequired = true,
                ),
            ),
        )
    }

    @Test
    fun `calendar digest lists one line per event`() {
        assertEquals(
            PushNotificationContent(
                "2 uutta kalenteritapahtumaa",
                "1.10.2026: Retki (Ryhmä 1)\n5.10.2026–6.10.2026: Leirikoulu",
            ),
            provider.calendarEventNotification(
                UiLanguage.FI,
                listOf(
                    CalendarEventSummary(
                        "Retki",
                        "Mukaan eväät",
                        FiniteDateRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 1)),
                        listOf("Ryhmä 1"),
                    ),
                    CalendarEventSummary(
                        "Leirikoulu",
                        "",
                        FiniteDateRange(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 6)),
                        emptyList(),
                    ),
                ),
            ),
        )
    }
}
