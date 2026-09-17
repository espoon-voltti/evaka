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
import java.time.format.DateTimeFormatter

private val dateFormat = DateTimeFormatter.ofPattern("d.M.yyyy")
private val timeFormat = DateTimeFormatter.ofPattern("HH.mm")

class EvakaPushNotificationMessageProvider : PushNotificationMessageProvider {
    override fun messageNotification(
        language: UiLanguage,
        data: MessagePushNotificationData,
    ): PushNotificationContent {
        val kind =
            when (data.type) {
                MessageType.MESSAGE ->
                    when (language) {
                        UiLanguage.FI ->
                            if (data.urgent) "Uusi kiireellinen viesti" else "Uusi viesti"
                        UiLanguage.SV ->
                            if (data.urgent) "Nytt brådskande meddelande" else "Nytt meddelande"
                        UiLanguage.EN -> if (data.urgent) "New urgent message" else "New message"
                    }

                MessageType.BULLETIN ->
                    when (language) {
                        UiLanguage.FI ->
                            if (data.urgent) "Uusi kiireellinen tiedote" else "Uusi tiedote"
                        UiLanguage.SV ->
                            if (data.urgent) "Nytt brådskande meddelande" else "Nytt meddelande"
                        UiLanguage.EN -> if (data.urgent) "New urgent bulletin" else "New bulletin"
                    }
            }

        val showTitle = data.isSenderMunicipalAccount && data.type == MessageType.BULLETIN
        return PushNotificationContent(
            title = kind,
            body =
                when {
                    showTitle -> data.title
                    data.sensitive -> null
                    else -> data.senderName
                },
        )
    }

    override fun testNotification(language: UiLanguage): PushNotificationContent =
        when (language) {
            UiLanguage.FI ->
                PushNotificationContent(
                    "Testi-ilmoitus",
                    "Push-ilmoitukset toimivat tällä laitteella.",
                )

            UiLanguage.SV ->
                PushNotificationContent(
                    "Testnotis",
                    "Push-notiser fungerar på den här enheten.",
                )

            UiLanguage.EN ->
                PushNotificationContent(
                    "Test notification",
                    "Push notifications work on this device.",
                )
        }

    override fun decisionNotification(
        language: UiLanguage,
        kind: DecisionPushNotificationKind,
    ): PushNotificationContent =
        when (kind) {
            DecisionPushNotificationKind.PENDING_APPROVAL ->
                PushNotificationContent(
                    title =
                        when (language) {
                            UiLanguage.FI -> "Päätös odottaa hyväksyntääsi"
                            UiLanguage.SV -> "Ett beslut väntar på ditt godkännande"
                            UiLanguage.EN -> "A decision is waiting for your approval"
                        },
                    body = null,
                )

            else ->
                PushNotificationContent(
                    title = newDecisionTitle(language),
                    body =
                        when (kind) {
                            DecisionPushNotificationKind.APPLICATION ->
                                when (language) {
                                    UiLanguage.FI -> "Päätös hakemukseesi"
                                    UiLanguage.SV -> "Beslut om din ansökan"
                                    UiLanguage.EN -> "Decision on your application"
                                }
                            DecisionPushNotificationKind.FEE ->
                                when (language) {
                                    UiLanguage.FI -> "Maksupäätös"
                                    UiLanguage.SV -> "Avgiftsbeslut"
                                    UiLanguage.EN -> "Fee decision"
                                }
                            DecisionPushNotificationKind.VOUCHER_VALUE ->
                                when (language) {
                                    UiLanguage.FI -> "Arvopäätös"
                                    UiLanguage.SV -> "Beslut om servicesedelns värde"
                                    UiLanguage.EN -> "Voucher value decision"
                                }
                            DecisionPushNotificationKind.PENDING_APPROVAL -> null
                        },
                )
        }

    override fun childApplicationDecisionNotification(
        language: UiLanguage,
        kind: ChildApplicationDecisionKind,
    ): PushNotificationContent =
        PushNotificationContent(
            title = newDecisionTitle(language),
            body =
                when (kind) {
                    ChildApplicationDecisionKind.ABSENCE_APPLICATION ->
                        when (language) {
                            UiLanguage.FI -> "Esiopetuksen poissaolohakemus käsitelty"
                            UiLanguage.SV -> "Ansökan om frånvaro från förskolan har behandlats"
                            UiLanguage.EN -> "Preschool absence application processed"
                        }
                    ChildApplicationDecisionKind.SERVICE_APPLICATION ->
                        when (language) {
                            UiLanguage.FI -> "Palveluntarpeen muutoshakemus käsitelty"
                            UiLanguage.SV -> "Ansökan om ändring av servicebehov har behandlats"
                            UiLanguage.EN -> "Service need change application processed"
                        }
                },
        )

    private fun newDecisionTitle(language: UiLanguage): String =
        when (language) {
            UiLanguage.FI -> "Uusi päätös eVakassa"
            UiLanguage.SV -> "Nytt beslut i eVaka"
            UiLanguage.EN -> "New decision in eVaka"
        }

    override fun incomeNotification(
        language: UiLanguage,
        notificationType: IncomeNotificationType,
    ): PushNotificationContent =
        PushNotificationContent(
            title =
                when (language) {
                    UiLanguage.FI -> "Tulotiedot on tarkistettava"
                    UiLanguage.SV -> "Inkomstuppgifterna måste kontrolleras"
                    UiLanguage.EN -> "Income information needs checking"
                },
            body =
                when (notificationType) {
                    IncomeNotificationType.INITIAL_EMAIL ->
                        when (language) {
                            UiLanguage.FI -> "Tulotietosi vanhenevat pian"
                            UiLanguage.SV -> "Dina inkomstuppgifter föråldras snart"
                            UiLanguage.EN -> "Your income information expires soon"
                        }
                    IncomeNotificationType.REMINDER_EMAIL ->
                        when (language) {
                            UiLanguage.FI -> "Muistutus: tulotietosi vanhenevat pian"
                            UiLanguage.SV -> "Påminnelse: dina inkomstuppgifter föråldras snart"
                            UiLanguage.EN -> "Reminder: your income information expires soon"
                        }
                    IncomeNotificationType.EXPIRED_EMAIL ->
                        when (language) {
                            UiLanguage.FI -> "Tulotietosi ovat vanhentuneet"
                            UiLanguage.SV -> "Dina inkomstuppgifter har föråldrats"
                            UiLanguage.EN -> "Your income information has expired"
                        }
                    IncomeNotificationType.NEW_CUSTOMER ->
                        when (language) {
                            UiLanguage.FI -> "Toimita tulotiedot varhaiskasvatusmaksua varten"
                            UiLanguage.SV -> "Lämna inkomstuppgifter för avgiften"
                            UiLanguage.EN -> "Submit income information for the fee"
                        }
                },
        )

    override fun calendarEventNotification(
        language: UiLanguage,
        count: Int,
        title: String?,
    ): PushNotificationContent =
        PushNotificationContent(
            title =
                when (language) {
                    UiLanguage.FI ->
                        if (count == 1) "Uusi kalenteritapahtuma"
                        else "$count uutta kalenteritapahtumaa"
                    UiLanguage.SV ->
                        if (count == 1) "Ny kalenderhändelse" else "$count nya kalenderhändelser"
                    UiLanguage.EN ->
                        if (count == 1) "New calendar event" else "$count new calendar events"
                },
            body = title,
        )

    override fun childDocumentNotification(
        language: UiLanguage,
        notificationType: ChildDocumentNotificationType,
    ): PushNotificationContent =
        PushNotificationContent(
            title =
                when (notificationType) {
                    ChildDocumentNotificationType.BASIC_DOCUMENT ->
                        when (language) {
                            UiLanguage.FI -> "Uusi asiakirja eVakassa"
                            UiLanguage.SV -> "Nytt dokument i eVaka"
                            UiLanguage.EN -> "New document in eVaka"
                        }
                    ChildDocumentNotificationType.EDITABLE_DOCUMENT ->
                        when (language) {
                            UiLanguage.FI -> "Sinulle on uusi täytettävä asiakirja"
                            UiLanguage.SV -> "Du har ett nytt dokument att fylla i"
                            UiLanguage.EN -> "You have a new document to fill in"
                        }
                    ChildDocumentNotificationType.DECISION_DOCUMENT ->
                        when (language) {
                            UiLanguage.FI -> "Uusi päätös eVakassa"
                            UiLanguage.SV -> "Nytt beslut i eVaka"
                            UiLanguage.EN -> "New decision in eVaka"
                        }
                },
            body = null,
        )

    override fun pedagogicalDocumentNotification(language: UiLanguage): PushNotificationContent =
        PushNotificationContent(
            title =
                when (language) {
                    UiLanguage.FI -> "Uusi pedagoginen dokumentti"
                    UiLanguage.SV -> "Nytt pedagogiskt dokument"
                    UiLanguage.EN -> "New pedagogical document"
                },
            body = null,
        )

    override fun missingReservationsNotification(
        language: UiLanguage,
        range: FiniteDateRange,
    ): PushNotificationContent =
        PushNotificationContent(
            title =
                when (language) {
                    UiLanguage.FI -> "Läsnäolovarauksia puuttuu"
                    UiLanguage.SV -> "Närvarobokningar saknas"
                    UiLanguage.EN -> "Attendance reservations are missing"
                },
            body =
                "${range.start.format(dateFormat)} – ${range.end.format(dateFormat)}"
                    .let {
                        when (language) {
                            UiLanguage.FI -> "Viikolle $it"
                            UiLanguage.SV -> "För veckan $it"
                            UiLanguage.EN -> "For the week $it"
                        }
                    },
        )

    override fun missingHolidayReservationsNotification(
        language: UiLanguage,
        deadline: LocalDate,
    ): PushNotificationContent =
        PushNotificationContent(
            title =
                when (language) {
                    UiLanguage.FI -> "Loma-ajan ilmoitus sulkeutuu"
                    UiLanguage.SV -> "Semesteranmälan stängs"
                    UiLanguage.EN -> "Holiday reservations are closing"
                },
            body =
                when (language) {
                    UiLanguage.FI -> "Viimeinen ilmoituspäivä ${deadline.format(dateFormat)}"
                    UiLanguage.SV -> "Sista anmälningsdag ${deadline.format(dateFormat)}"
                    UiLanguage.EN -> "Last day to respond ${deadline.format(dateFormat)}"
                },
        )

    override fun discussionSurveyNotification(
        language: UiLanguage,
        title: String,
    ): PushNotificationContent =
        PushNotificationContent(
            title =
                when (language) {
                    UiLanguage.FI -> "Varaa keskusteluaika"
                    UiLanguage.SV -> "Boka en diskussionstid"
                    UiLanguage.EN -> "Book a discussion time"
                },
            body = title,
        )

    override fun discussionTimeNotification(
        language: UiLanguage,
        event: DiscussionTimePushNotificationEvent,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
    ): PushNotificationContent {
        val time = "${startTime.format(timeFormat)}–${endTime.format(timeFormat)}"
        return PushNotificationContent(
            title =
                when (event) {
                    DiscussionTimePushNotificationEvent.RESERVED ->
                        when (language) {
                            UiLanguage.FI -> "Keskusteluaika varattu"
                            UiLanguage.SV -> "Diskussionstid bokad"
                            UiLanguage.EN -> "Discussion time booked"
                        }
                    DiscussionTimePushNotificationEvent.CANCELLED ->
                        when (language) {
                            UiLanguage.FI -> "Keskusteluaika peruttu"
                            UiLanguage.SV -> "Diskussionstid avbokad"
                            UiLanguage.EN -> "Discussion time cancelled"
                        }
                    DiscussionTimePushNotificationEvent.REMINDER ->
                        when (language) {
                            UiLanguage.FI -> "Muistutus keskusteluajasta"
                            UiLanguage.SV -> "Påminnelse om diskussionstid"
                            UiLanguage.EN -> "Discussion time reminder"
                        }
                },
            body =
                when (language) {
                    UiLanguage.FI -> "${date.format(dateFormat)} klo $time"
                    UiLanguage.SV -> "${date.format(dateFormat)} kl. $time"
                    UiLanguage.EN -> "${date.format(dateFormat)} at $time"
                },
        )
    }
}
