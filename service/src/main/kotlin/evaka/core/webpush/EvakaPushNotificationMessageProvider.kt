// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.webpush

import evaka.core.decision.DecisionType
import evaka.core.document.childdocument.ChildDocumentNotificationType
import evaka.core.invoicing.service.IncomeNotificationType
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.UiLanguage
import java.time.format.DateTimeFormatter

private val dateFormat = DateTimeFormatter.ofPattern("d.M.yyyy")
private val timeFormat = DateTimeFormatter.ofPattern("HH.mm")

private val whitespace = Regex("\\s+")

class EvakaPushNotificationMessageProvider : PushNotificationMessageProvider {
    override fun messageNotification(
        language: UiLanguage,
        data: MessagePushNotificationData,
    ): PushNotificationContent =
        PushNotificationContent(
            title =
                if (data.urgent)
                    when (language) {
                        UiLanguage.FI -> "Kiireellinen: ${data.senderName}"
                        UiLanguage.SV -> "Brådskande: ${data.senderName}"
                        UiLanguage.EN -> "Urgent: ${data.senderName}"
                    }
                else data.senderName,
            body =
                if (data.sensitive)
                    when (language) {
                        UiLanguage.FI -> "Arkaluonteinen viesti"
                        UiLanguage.SV -> "Känsligt meddelande"
                        UiLanguage.EN -> "Sensitive message"
                    }
                else "${data.title}\n${data.content.replace(whitespace, " ").trim()}",
        )

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

    override fun feeDecisionNotification(
        language: UiLanguage,
        notification: CitizenPushNotification.FeeDecision,
    ): PushNotificationContent =
        PushNotificationContent(
            title =
                when (language) {
                    UiLanguage.FI -> "Uusi maksupäätös"
                    UiLanguage.SV -> "Nytt avgiftsbeslut"
                    UiLanguage.EN -> "New fee decision"
                },
            body = notification.childNames.joinToString(", "),
        )

    override fun voucherValueDecisionNotification(
        language: UiLanguage,
        notification: CitizenPushNotification.VoucherValueDecision,
    ): PushNotificationContent =
        PushNotificationContent(
            title =
                when (language) {
                    UiLanguage.FI -> "Uusi arvopäätös"
                    UiLanguage.SV -> "Nytt beslut om servicesedelns värde"
                    UiLanguage.EN -> "New voucher value decision"
                },
            body = "${notification.childName}\n${notification.unitName}",
        )

    override fun applicationDecisionsNotification(
        language: UiLanguage,
        notification: CitizenPushNotification.ApplicationDecisions,
    ): PushNotificationContent =
        PushNotificationContent(
            title =
                when (language) {
                    UiLanguage.FI -> "Uusi päätös"
                    UiLanguage.SV -> "Nytt beslut"
                    UiLanguage.EN -> "New decision"
                },
            body =
                (listOf(notification.childName) +
                        notification.decisions.map {
                            val type = decisionTypeName(language, it.type)
                            val startDate = it.startDate.format(dateFormat)
                            when (language) {
                                UiLanguage.FI -> "$type, ${it.unitName} $startDate alkaen"
                                UiLanguage.SV -> "$type, ${it.unitName} från $startDate"
                                UiLanguage.EN -> "$type, ${it.unitName} from $startDate"
                            }
                        } +
                        listOfNotNull(
                            if (notification.answerRequired)
                                when (language) {
                                    UiLanguage.FI -> "Hyväksy tai hylkää päätös eVakassa"
                                    UiLanguage.SV -> "Godkänn eller avvisa beslutet i eVaka"
                                    UiLanguage.EN -> "Accept or reject the decision in eVaka"
                                }
                            else null
                        ))
                    .joinToString("\n"),
        )

    override fun pendingDecisionsNotification(
        language: UiLanguage,
        decisions: List<PendingDecision>,
    ): PushNotificationContent =
        PushNotificationContent(
            title =
                when (language) {
                    UiLanguage.FI ->
                        if (decisions.size == 1) "Päätös odottaa vastaustasi"
                        else "${decisions.size} päätöstä odottaa vastaustasi"
                    UiLanguage.SV ->
                        if (decisions.size == 1) "Ett beslut väntar på ditt svar"
                        else "${decisions.size} beslut väntar på ditt svar"
                    UiLanguage.EN ->
                        if (decisions.size == 1) "A decision is waiting for your response"
                        else "${decisions.size} decisions are waiting for your response"
                },
            body =
                decisions.joinToString("\n") {
                    "${it.childName}: ${decisionTypeName(language, it.type)}, ${it.unitName}"
                },
        )

    private fun decisionTypeName(language: UiLanguage, type: DecisionType): String =
        when (language) {
            UiLanguage.FI ->
                when (type) {
                    DecisionType.CLUB -> "Kerho"
                    DecisionType.DAYCARE -> "Varhaiskasvatus"
                    DecisionType.DAYCARE_PART_TIME -> "Osa-aikainen varhaiskasvatus"
                    DecisionType.PRESCHOOL -> "Esiopetus"
                    DecisionType.PRESCHOOL_DAYCARE -> "Liittyvä varhaiskasvatus"
                    DecisionType.PRESCHOOL_CLUB -> "Esiopetuksen kerho"
                    DecisionType.PREPARATORY_EDUCATION -> "Valmistava opetus"
                }
            UiLanguage.SV ->
                when (type) {
                    DecisionType.CLUB -> "Klubbverksamhet"
                    DecisionType.DAYCARE -> "Småbarnspedagogik"
                    DecisionType.DAYCARE_PART_TIME -> "Deldag småbarnspedagogik"
                    DecisionType.PRESCHOOL -> "Förskola"
                    DecisionType.PRESCHOOL_DAYCARE -> "Kompletterande småbarnspedagogik"
                    DecisionType.PRESCHOOL_CLUB -> "Förskoleklubb"
                    DecisionType.PREPARATORY_EDUCATION -> "Förberedande undervisning"
                }
            UiLanguage.EN ->
                when (type) {
                    DecisionType.CLUB -> "Club"
                    DecisionType.DAYCARE -> "Early childhood education"
                    DecisionType.DAYCARE_PART_TIME -> "Part-day early childhood education"
                    DecisionType.PRESCHOOL -> "Pre-primary education"
                    DecisionType.PRESCHOOL_DAYCARE ->
                        "Early childhood education related to pre-primary education"
                    DecisionType.PRESCHOOL_CLUB -> "Pre-primary club"
                    DecisionType.PREPARATORY_EDUCATION -> "Preparatory education"
                }
        }

    override fun absenceApplicationDecisionNotification(
        language: UiLanguage,
        notification: CitizenPushNotification.AbsenceApplicationDecision,
    ): PushNotificationContent =
        PushNotificationContent(
            title =
                if (!notification.rejected)
                    when (language) {
                        UiLanguage.FI -> "Poissaolohakemus hyväksytty"
                        UiLanguage.SV -> "Frånvaroansökan godkänd"
                        UiLanguage.EN -> "Absence application accepted"
                    }
                else
                    when (language) {
                        UiLanguage.FI -> "Poissaolohakemus hylätty"
                        UiLanguage.SV -> "Frånvaroansökan avslagen"
                        UiLanguage.EN -> "Absence application rejected"
                    },
            body =
                listOfNotNull(
                        notification.childName,
                        when (language) {
                            UiLanguage.FI ->
                                "Esiopetuksen poissaolo ${formatRange(notification.range)}"
                            UiLanguage.SV ->
                                "Frånvaro från förskolan ${formatRange(notification.range)}"
                            UiLanguage.EN ->
                                "Pre-primary absence ${formatRange(notification.range)}"
                        },
                    )
                    .joinToString("\n"),
        )

    override fun serviceApplicationDecisionNotification(
        language: UiLanguage,
        notification: CitizenPushNotification.ServiceApplicationDecision,
    ): PushNotificationContent {
        val startDate = notification.startDate.format(dateFormat)
        return PushNotificationContent(
            title =
                if (!notification.rejected)
                    when (language) {
                        UiLanguage.FI -> "Palveluntarpeen muutos hyväksytty"
                        UiLanguage.SV -> "Ändring av servicebehov godkänd"
                        UiLanguage.EN -> "Service need change accepted"
                    }
                else
                    when (language) {
                        UiLanguage.FI -> "Palveluntarpeen muutos hylätty"
                        UiLanguage.SV -> "Ändring av servicebehov avslagen"
                        UiLanguage.EN -> "Service need change rejected"
                    },
            body =
                listOfNotNull(
                        notification.childName,
                        when (language) {
                            UiLanguage.FI -> "${notification.serviceNeedNameFi} $startDate alkaen"
                            UiLanguage.SV -> "${notification.serviceNeedNameSv} från $startDate"
                            UiLanguage.EN -> "${notification.serviceNeedNameEn} from $startDate"
                        },
                    )
                    .joinToString("\n"),
        )
    }

    override fun incomeNotification(
        language: UiLanguage,
        notification: CitizenPushNotification.Income,
    ): PushNotificationContent {
        val expirationDate = notification.expirationDate?.format(dateFormat)
        val deadline = notification.deadline?.format(dateFormat)
        val sentences =
            when (notification.notificationType) {
                IncomeNotificationType.INITIAL_EMAIL,
                IncomeNotificationType.REMINDER_EMAIL ->
                    when (language) {
                        UiLanguage.FI ->
                            listOf(
                                expirationDate?.let { "Tulotietosi vanhenevat $it." },
                                "Toimita uudet tulotiedot, muuten maksu määräytyy korkeimman maksuluokan mukaan.",
                            )
                        UiLanguage.SV ->
                            listOf(
                                expirationDate?.let { "Dina inkomstuppgifter föråldras $it." },
                                "Lämna nya inkomstuppgifter, annars bestäms avgiften enligt den högsta avgiftsklassen.",
                            )
                        UiLanguage.EN ->
                            listOf(
                                expirationDate?.let { "Your income information expires $it." },
                                "Submit new income information, otherwise the fee is set by the highest fee class.",
                            )
                    }
                IncomeNotificationType.EXPIRED_EMAIL ->
                    when (language) {
                        UiLanguage.FI ->
                            listOf(
                                expirationDate?.let { "Tulotietosi vanhenivat $it." },
                                "Maksu määräytyy korkeimman maksuluokan mukaan, kunnes toimitat uudet tulotiedot.",
                            )
                        UiLanguage.SV ->
                            listOf(
                                expirationDate?.let { "Dina inkomstuppgifter föråldrades $it." },
                                "Avgiften bestäms enligt den högsta avgiftsklassen tills du lämnar nya inkomstuppgifter.",
                            )
                        UiLanguage.EN ->
                            listOf(
                                expirationDate?.let { "Your income information expired $it." },
                                "The fee is set by the highest fee class until you submit new income information.",
                            )
                    }
                IncomeNotificationType.NEW_CUSTOMER ->
                    when (language) {
                        UiLanguage.FI ->
                            listOf(
                                "Lapsesi aloittaa varhaiskasvatuksessa.",
                                deadline?.let { "Toimita tulotiedot $it mennessä." }
                                    ?: "Toimita tulotiedot.",
                            )
                        UiLanguage.SV ->
                            listOf(
                                "Ditt barn börjar i småbarnspedagogik.",
                                deadline?.let { "Lämna inkomstuppgifter senast $it." }
                                    ?: "Lämna inkomstuppgifter.",
                            )
                        UiLanguage.EN ->
                            listOf(
                                "Your child is starting early childhood education.",
                                deadline?.let { "Submit your income information by $it." }
                                    ?: "Submit your income information.",
                            )
                    }
            }
        return PushNotificationContent(
            title =
                when (notification.notificationType) {
                    IncomeNotificationType.INITIAL_EMAIL ->
                        when (language) {
                            UiLanguage.FI -> "Tulotiedot on päivitettävä"
                            UiLanguage.SV -> "Inkomstuppgifterna måste uppdateras"
                            UiLanguage.EN -> "Income information must be updated"
                        }
                    IncomeNotificationType.REMINDER_EMAIL ->
                        when (language) {
                            UiLanguage.FI -> "Muistutus: tulotiedot on päivitettävä"
                            UiLanguage.SV -> "Påminnelse: inkomstuppgifterna måste uppdateras"
                            UiLanguage.EN -> "Reminder: income information must be updated"
                        }
                    IncomeNotificationType.EXPIRED_EMAIL ->
                        when (language) {
                            UiLanguage.FI -> "Tulotietosi ovat vanhentuneet"
                            UiLanguage.SV -> "Dina inkomstuppgifter har föråldrats"
                            UiLanguage.EN -> "Your income information has expired"
                        }
                    IncomeNotificationType.NEW_CUSTOMER ->
                        when (language) {
                            UiLanguage.FI -> "Toimita tulotiedot"
                            UiLanguage.SV -> "Lämna inkomstuppgifter"
                            UiLanguage.EN -> "Submit income information"
                        }
                },
            body = sentences.filterNotNull().joinToString(" "),
        )
    }

    override fun calendarEventNotification(
        language: UiLanguage,
        events: List<CalendarEventSummary>,
    ): PushNotificationContent {
        val eventLines = events.map { event ->
            val groups =
                if (event.groupNames.isEmpty()) "" else " (${event.groupNames.joinToString(", ")})"
            "${formatPeriod(event.period)}: ${event.title}$groups"
        }
        val description = events.singleOrNull()?.description?.replace(whitespace, " ")?.trim()
        return PushNotificationContent(
            title =
                when (language) {
                    UiLanguage.FI ->
                        if (events.size == 1) "Uusi kalenteritapahtuma"
                        else "${events.size} uutta kalenteritapahtumaa"
                    UiLanguage.SV ->
                        if (events.size == 1) "Ny kalenderhändelse"
                        else "${events.size} nya kalenderhändelser"
                    UiLanguage.EN ->
                        if (events.size == 1) "New calendar event"
                        else "${events.size} new calendar events"
                },
            body = (eventLines + listOfNotNull(description?.ifEmpty { null })).joinToString("\n"),
        )
    }

    override fun childDocumentNotification(
        language: UiLanguage,
        notification: CitizenPushNotification.Document,
    ): PushNotificationContent =
        PushNotificationContent(
            title =
                when (notification.notificationType) {
                    ChildDocumentNotificationType.BASIC_DOCUMENT ->
                        when (language) {
                            UiLanguage.FI -> "Uusi asiakirja"
                            UiLanguage.SV -> "Nytt dokument"
                            UiLanguage.EN -> "New document"
                        }
                    ChildDocumentNotificationType.EDITABLE_DOCUMENT ->
                        when (language) {
                            UiLanguage.FI -> "Täytettävä asiakirja"
                            UiLanguage.SV -> "Dokument att fylla i"
                            UiLanguage.EN -> "Document to fill in"
                        }
                    ChildDocumentNotificationType.DECISION_DOCUMENT ->
                        when (language) {
                            UiLanguage.FI -> "Uusi päätös"
                            UiLanguage.SV -> "Nytt beslut"
                            UiLanguage.EN -> "New decision"
                        }
                },
            body =
                listOfNotNull(
                        notification.childName,
                        if (
                            notification.notificationType ==
                                ChildDocumentNotificationType.EDITABLE_DOCUMENT
                        )
                            when (language) {
                                UiLanguage.FI -> "Täytä asiakirja eVakassa"
                                UiLanguage.SV -> "Fyll i dokumentet i eVaka"
                                UiLanguage.EN -> "Fill in the document in eVaka"
                            }
                        else null,
                    )
                    .joinToString("\n"),
        )

    override fun pedagogicalDocumentNotification(
        language: UiLanguage,
        notification: CitizenPushNotification.InformalDocument,
    ): PushNotificationContent =
        PushNotificationContent(
            title =
                when (language) {
                    UiLanguage.FI -> "Uusi pedagoginen dokumentti"
                    UiLanguage.SV -> "Nytt pedagogiskt dokument"
                    UiLanguage.EN -> "New pedagogical document"
                },
            body = notification.childName,
        )

    override fun missingReservationsNotification(
        language: UiLanguage,
        notification: CitizenPushNotification.MissingReservations,
    ): PushNotificationContent {
        val children = notification.childNames.joinToString(", ")
        val deadlineDate = notification.deadline.toLocalDate().format(dateFormat)
        val deadlineTime = notification.deadline.toLocalTime().format(timeFormat)
        return PushNotificationContent(
            title =
                when (language) {
                    UiLanguage.FI -> "Läsnäolovarauksia puuttuu"
                    UiLanguage.SV -> "Närvarobokningar saknas"
                    UiLanguage.EN -> "Attendance reservations are missing"
                },
            body =
                when (language) {
                    UiLanguage.FI ->
                        "$children\nMerkitse varaukset viimeistään $deadlineDate klo $deadlineTime"
                    UiLanguage.SV ->
                        "$children\nGör bokningarna senast $deadlineDate kl. $deadlineTime"
                    UiLanguage.EN ->
                        "$children\nMake the reservations by $deadlineDate at $deadlineTime"
                },
        )
    }

    override fun missingHolidayReservationsNotification(
        language: UiLanguage,
        notification: CitizenPushNotification.MissingHolidayReservations,
    ): PushNotificationContent {
        val period = formatRange(notification.holidayPeriod)
        val deadline = notification.deadline.format(dateFormat)
        return PushNotificationContent(
            title =
                when (language) {
                    UiLanguage.FI -> "Loma-ajan ilmoitus sulkeutuu"
                    UiLanguage.SV -> "Semesteranmälan stängs"
                    UiLanguage.EN -> "Holiday reservations are closing"
                },
            body =
                when (language) {
                    UiLanguage.FI ->
                        "Loma-aika $period\nIlmoita lasten läsnäolot ja poissaolot viimeistään $deadline"
                    UiLanguage.SV ->
                        "Semesterperioden $period\nAnmäl barnens när- och frånvaro senast $deadline"
                    UiLanguage.EN ->
                        "Holiday period $period\nReport the children's attendance and absences by $deadline"
                },
        )
    }

    override fun discussionSurveyNotification(
        language: UiLanguage,
        notification: CitizenPushNotification.DiscussionSurvey,
    ): PushNotificationContent =
        PushNotificationContent(
            title =
                when (language) {
                    UiLanguage.FI -> "Varaa keskusteluaika"
                    UiLanguage.SV -> "Boka en diskussionstid"
                    UiLanguage.EN -> "Book a discussion time"
                },
            body =
                listOfNotNull(
                        notification.title,
                        notification.description.replace(whitespace, " ").trim().ifEmpty { null },
                    )
                    .joinToString("\n"),
        )

    override fun discussionTimeNotification(
        language: UiLanguage,
        notification: CitizenPushNotification.DiscussionTime,
    ): PushNotificationContent {
        val date = notification.date.format(dateFormat)
        val time =
            "${notification.startTime.format(timeFormat)}–${notification.endTime.format(timeFormat)}"
        return PushNotificationContent(
            title =
                when (notification.event) {
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
                notification.childName +
                    "\n" +
                    notification.surveyTitle +
                    "\n" +
                    when (language) {
                        UiLanguage.FI -> "$date klo $time"
                        UiLanguage.SV -> "$date kl. $time"
                        UiLanguage.EN -> "$date at $time"
                    },
        )
    }

    private fun formatPeriod(period: FiniteDateRange): String =
        if (period.start == period.end) period.start.format(dateFormat) else formatRange(period)

    private fun formatRange(range: FiniteDateRange): String =
        "${range.start.format(dateFormat)}–${range.end.format(dateFormat)}"
}
