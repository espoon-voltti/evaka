// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.ylojarvi.emailclient.config

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
import evaka.core.messaging.MessageType
import evaka.core.shared.ApplicationId
import evaka.core.shared.ChildId
import evaka.core.shared.HtmlSafe
import evaka.core.shared.domain.FiniteDateRange
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class EmailConfiguration {

    @Bean fun emailMessageProvider(): IEmailMessageProvider = YlojarviEmailMessageProvider()
}

class YlojarviEmailMessageProvider : IEmailMessageProvider {

    private val unsubscribeFi =
        """<p><small>Jos et halua enää saada tämänkaltaisia viestejä, voit muuttaa asetuksia eVakan Omat tiedot -sivulla</small></p>"""
    private val unsubscribeEn =
        """<p><small>If you no longer want to receive messages like this, you can change your settings on eVaka's Personal information page</small></p>"""
    private val subjectForPendingDecisionEmail: String = "Toimenpiteitäsi odotetaan"
    private val subjectForApplicationReceivedEmail: String = "Hakemus vastaanotettu"
    private val subjectForDecisionEmail: String = "Päätös eVakassa"
    private val subjectForNewDocument: String = "Uusi dokumentti eVakassa / New document in eVaka"
    private val subjectForRequestToReviewIncomeInfo: String =
        "Tulotietojen tarkastuskehotus / Request to review income information"

    override fun pendingDecisionNotification(language: Language) =
        EmailContent.fromHtml(
            subject = subjectForPendingDecisionEmail,
            html = getPendingDecisionEmailHtml(),
        )

    override fun clubApplicationReceived(language: Language) =
        EmailContent.fromHtml(
            subject = subjectForApplicationReceivedEmail,
            html = getClubApplicationReceivedEmailHtml(),
        )

    override fun daycareApplicationReceived(language: Language) =
        EmailContent.fromHtml(
            subject = subjectForApplicationReceivedEmail,
            html = getDaycareApplicationReceivedEmailHtml(),
        )

    override fun preschoolApplicationReceived(
        language: Language,
        withinApplicationPeriod: Boolean,
    ) =
        EmailContent.fromHtml(
            subject = subjectForApplicationReceivedEmail,
            html = getPreschoolApplicationReceivedEmailHtml(),
        )

    private fun getPendingDecisionEmailHtml(): String =
        """
            <p>Olet saanut päätöksen/ilmoituksen Ylöjärven varhaiskasvatukselta, joka odottaa toimenpiteitäsi. Myönnetty varhaiskasvatus-/kerhopaikka tulee hyväksyä tai hylätä kahden viikon sisällä päätöksen saapumisesta.</p>
            
            <p>Hakemuksen tekijä voi hyväksyä tai hylätä varhaiskasvatus-/kerhopaikan kirjautumalla Ylöjärven varhaiskasvatuksen verkkopalveluun eVakaan tai ottamalla yhteyttä päätöksellä mainittuun päiväkodin johtajaan.</p>
            
            <p>Tähän viestiin ei voi vastata. Tarvittaessa ole yhteydessä Varhaiskasvatuksen asiakaspalveluun.</p>
            
            $unsubscribeFi
    """
            .trimIndent()

    private fun getClubApplicationReceivedEmailHtml(): String =
        """
        <p>Hyvä huoltaja,</p>

        <p>lapsenne kerhohakemus on vastaanotettu.</p>

        <p>Hakemuksen tehnyt huoltaja voi muokata hakemusta Ylöjärven varhaiskasvatuksen verkkopalvelussa eVakassa siihen saakka, kunnes se on otettu käsittelyyn asiakaspalvelussa.</p>

        <p>Kirjallinen ilmoitus myönnetystä kerhopaikasta lähetetään huoltajalle Suomi.fi-viestit -palveluun. Mikäli huoltaja ei ole ottanut Suomi.fi-viestit -palvelua käyttöön, ilmoitus lähetetään hänelle postitse.</p>

        <p>Myönnetyn kerhopaikan voi hyväksyä / hylätä sähköisesti eVakassa. Kerhohakemus kohdistuu yhdelle kerhon toimintakaudelle. Kauden päättyessä hakemus poistetaan järjestelmästä.</p>

        <p>Lisätietoa hakemuksen käsittelystä ja kerhopaikan myöntämisestä saa varhaiskasvatuksen ja esiopetuksen asiakaspalvelusta.</p>

        <p>Tämä on automaattinen viesti, joka kertoo lomakkeen tallennuksesta. Viestiin ei voi vastata reply-/ vastaa-toiminnolla.</p>
        """
            .trimIndent()

    private fun getDaycareApplicationReceivedEmailHtml(): String =
        """
        <p>Hyvä huoltaja,</p>

        <p>lapsenne varhaiskasvatushakemus on vastaanotettu.</p>

        <p>Varhaiskasvatushakemuksella on neljän (4) kuukauden hakuaika. Hakemuksen tehnyt huoltaja voi muokata hakemusta Ylöjärven varhaiskasvatuksen verkkopalvelussa eVakassa siihen saakka, kunnes se on otettu käsittelyyn.</p>

        <p>Saatte tiedon lapsenne varhaiskasvatuspaikasta noin kuukautta ennen palvelutarpeen alkamista tai hakemuksen lakisääteisen järjestelyajan päättymistä. Hakemuksen lakisääteinen järjestelyaika on neljä (4) kuukautta hakemuksen saapumisesta.</p>

        <p>Mikäli hakemuksenne on kiireellinen, ottakaa yhteyttä viipymättä Varhaiskasvatuksen asiakaspalveluun. Hakuaika kiireellisissä hakemuksissa on minimissään kaksi (2) viikkoa ja se alkaa siitä päivästä, kun olette olleet yhteydessä asiakaspalveluun.</p>

        <p>Kirjallinen päätös varhaiskasvatuspaikasta lähetetään huoltajalle Suomi.fi-viestit -palveluun. Mikäli huoltaja ei ole ottanut Suomi.fi-viestit -palvelua käyttöön, päätös lähetetään hänelle postitse.</p>

        <p>Myönnetyn varhaiskasvatuspaikan voi hyväksyä / hylätä sähköisesti eVakassa. Mikäli haette paikkaa palvelusetelipäiväkodista, olkaa yhteydessä kyseiseen päiväkotiin viimeistään hakemuksen jättämisen jälkeen.</p>

        <p>Ilta- ja vuorohoitoa haettaessa, hakemuksen liitteeksi tulee toimittaa molempien samassa taloudessa asuvien huoltajien todistukset työnantajalta vuorotyöstä tai oppilaitoksesta iltaisin tapahtuvasta opiskelusta. Hakemusta käsitellään vuorohoidon hakemuksena vasta sen jälkeen, kun edellä mainitut todistukset on toimitettu. Tarvittavat liitteet voi lisätä suoraan sähköiselle hakemukselle tai toimittaa postitse osoitteeseen Ylöjärven kaupunki, Varhaiskasvatus ja esiopetus, Kuruntie 14, 33470 Ylöjärvi.</p>

        <p>Hakiessanne lapsellenne siirtoa toiseen varhaiskasvatusyksikköön, hakemuksella ei ole hakuaikaa. Siirrot pystytään toteuttamaan pääsääntöisesti elokuusta alkaen. Mikäli lapsen nykyinen hoitopaikka irtisanotaan, myös siirtohakemus poistuu.</p>

        <p>Lisätietoa hakemuksen käsittelystä ja varhaiskasvatuspaikan myöntämisestä saa varhaiskasvatuksen ja esiopetuksen asiakaspalvelusta.</p>

        <p>Tämä on automaattinen viesti, joka kertoo lomakkeen tallennuksesta. Viestiin ei voi vastata reply-/ vastaa-toiminnolla.</p>
        """
            .trimIndent()

    private fun getPreschoolApplicationReceivedEmailHtml(): String =
        """
        <p>Hyvä huoltaja,</p>

        <p>lapsenne esiopetukseen ilmoittautuminen on vastaanotettu.</p>

        <p>Hakemuksen tehnyt huoltaja voi muokata hakemusta Ylöjärven varhaiskasvatuksen verkkopalvelussa eVakassa siihen saakka, kunnes se on otettu käsittelyyn.</p>

        <p>Lisätietoa hakemuksen käsittelystä ja esiopetuspaikan myöntämisestä saa varhaiskasvatuksen ja esiopetuksen asiakaspalvelusta.</p>

        <p>Tämä on automaattinen viesti, joka kertoo lomakkeen tallennuksesta. Viestiin ei voi vastata reply-/ vastaa-toiminnolla.</p>
        """
            .trimIndent()

    private fun getDecisionEmailHtml(): String =
        """
            <p>Hyvä(t) huoltaja(t),</p>
    
            <p>Lapsellenne on tehty päätös liittyen varhaiskasvatukseen/esiopetukseen.</p>
    
            <p>Päätös on nähtävissä Ylöjärven varhaiskasvatuksen verkkopalvelu eVakassa Päätökset-välilehdeltä.</p>
    
            $unsubscribeFi
    """
            .trimIndent()

    override fun missingReservationsNotification(
        language: Language,
        checkedRange: FiniteDateRange,
    ): EmailContent {
        val start =
            checkedRange.start.format(
                DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
                    .withLocale(Locale.of("fi", "FI"))
            )
        return EmailContent.fromHtml(
            subject = "Läsnäolovarauksia puuttuu / There are missing attendance reservations",
            html =
                """
                <p>Läsnäolovarauksia puuttuu $start alkavalta viikolta. Käythän merkitsemässä ne mahdollisimman pian.</p>
                
                $unsubscribeFi
                
                <hr>
                
                <p>There are missing attendance reservations for the week starting $start. Please mark them as soon as possible.</p>
                
                $unsubscribeEn
            """
                    .trimIndent(),
        )
    }

    override fun missingHolidayReservationsNotification(language: Language): EmailContent =
        EmailContent.fromHtml(
            subject = "Loma-ajan ilmoitus sulkeutuu / Holiday notification period closing",
            html =
                """
<p>Loma-ajan kysely sulkeutuu kahden päivän päästä. Jos lapseltanne/lapsiltanne puuttuu loma-ajan ilmoitus yhdeltä tai useammalta lomapäivältä, teettehän ilmoituksen eVakan kalenterissa mahdollisimman pian.</p>
$unsubscribeFi
<hr>
<p>Two days left to submit a holiday notification. If you have not submitted a notification for each day, please submit them through the eVaka calendar as soon as possible.</p>
$unsubscribeEn
""",
        )

    override fun messageNotification(language: Language, thread: MessageThreadData): EmailContent =
        messageNotification(language, thread, false, null)

    override fun messageNotification(
        language: Language,
        thread: MessageThreadData,
        isSenderMunicipalAccount: Boolean,
        applicationId: ApplicationId?,
    ): EmailContent {
        val (typeFi, typeEn) =
            when (thread.type) {
                MessageType.MESSAGE ->
                    if (thread.urgent) {
                        Pair("kiireellinen viesti", "urgent message")
                    } else {
                        Pair("viesti", "message")
                    }

                MessageType.BULLETIN ->
                    if (thread.urgent) {
                        Pair("kiireellinen tiedote", "urgent bulletin")
                    } else {
                        Pair("tiedote", "bulletin")
                    }
            }
        val showSubjectInBody = isSenderMunicipalAccount && thread.type == MessageType.BULLETIN
        return EmailContent.fromHtml(
            subject = "Uusi $typeFi eVakassa / New $typeEn in eVaka",
            html =
                """
                <p>Sinulle on saapunut uusi $typeFi eVakaan lähettäjältä ${thread.senderName}${if (showSubjectInBody) " otsikolla \"" + thread.title + "\"" else ""}.</p>
                
                <p>Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.</p>
                
                $unsubscribeFi
                
                <hr>
                
                <p>You have received a new $typeEn in eVaka from ${thread.senderName}${if (showSubjectInBody) " with subject \"" + thread.title + "\"" else ""}.</p>
                
                <p>This is an automatic message from the eVaka system. Do not reply to this message.</p>
                
                $unsubscribeEn
            """
                    .trimIndent(),
        )
    }

    override fun childDocumentNotification(
        language: Language,
        childId: ChildId,
        notificationType: ChildDocumentNotificationType,
    ): EmailContent =
        EmailContent.fromHtml(
            subject = subjectForNewDocument,
            html =
                """
            <p>Sinulle on saapunut uusi dokumentti eVakaan.</p>
            
            <p>Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.</p>
            
            $unsubscribeFi
            
            <hr>
            
            <p>You have received a new eVaka document.</p>
            
            <p>This is an automatic message from the eVaka system. Do not reply to this message.</p>
            
            $unsubscribeEn
        """
                    .trimIndent(),
        )

    override fun pedagogicalDocumentNotification(
        language: Language,
        childId: ChildId,
    ): EmailContent =
        EmailContent.fromHtml(
            subject = "Uusi pedagoginen dokumentti eVakassa / New pedagogical document in eVaka",
            html =
                """
            <p>Sinulle on saapunut uusi pedagoginen dokumentti eVakaan.</p>
            
            <p>Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.</p>
            
            $unsubscribeFi
            
            <hr>
            
            <p>You have received a new eVaka pedagogical document.</p>
            
            <p>This is an automatic message from the eVaka system. Do not reply to this message.</p>
            
            $unsubscribeEn
        """
                    .trimIndent(),
        )

    override fun incomeNotification(notificationType: IncomeNotificationType, language: Language) =
        when (notificationType) {
            IncomeNotificationType.INITIAL_EMAIL -> outdatedIncomeNotificationInitial()
            IncomeNotificationType.REMINDER_EMAIL -> outdatedIncomeNotificationReminder()
            IncomeNotificationType.EXPIRED_EMAIL -> outdatedIncomeNotificationExpired()
            IncomeNotificationType.NEW_CUSTOMER -> newCustomerIncomeNotification()
        }

    private fun outdatedIncomeNotificationInitial() =
        EmailContent.fromHtml(
            subject = subjectForRequestToReviewIncomeInfo,
            html =
                """
            <p>Hyvä asiakkaamme</p>
            
            <p>Varhaiskasvatuksen asiakasmaksun tai palvelusetelin omavastuuosuuden perusteena olevat tulotiedot tarkistetaan vuosittain.</p>
            
            <p>Pyydämme toimittamaan tuloselvityksen eVakassa 28 päivän kuluessa tästä ilmoituksesta. eVakassa voitte myös antaa suostumuksen korkeimpaan maksuluokkaan tai tulorekisterin käyttöön.</p>
            
            <p>Mikäli ette toimita uusia tulotietoja, asiakasmaksu määräytyy korkeimman maksuluokan mukaan. Uusi maksupäätös astuu voimaan sen kuukauden alusta, kun tulotiedot on toimitettu asiakasmaksuihin.</p>
            
            <p>Lisätietoja saatte tarvittaessa Ylöjärven kaupungin verkkosivuilta.</p>
            
            <p>Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.</p>
            
            $unsubscribeFi
            
            <hr>
            
            <p>Dear client</p>
            
            <p>The income information used for determining the early childhood education fee or the out-of-pocket cost of a service voucher is reviewed every year.</p>
            
            <p>We ask you to submit your income statement through eVaka within 28 days of this notification. Through eVaka, you can also give your consent to the highest fee or the use of the Incomes Register.</p>
            
            <p>If you do not provide your latest income information, your client fee will be determined based on the highest fee category. The new payment decision takes effect at the beginning of the month when the income information has been submitted to customer services.</p>
            
            <p>If necessary, you can get more information from the website of the city of Ylöjärvi.</p>
            
            <p>This is an automatic message from the eVaka system. Do not reply to this message.</p>
            
            $unsubscribeEn
        """
                    .trimIndent(),
        )

    private fun outdatedIncomeNotificationReminder() =
        EmailContent.fromHtml(
            subject = subjectForRequestToReviewIncomeInfo,
            html =
                """
            <p>Hyvä asiakkaamme</p>
            
            <p>Ette ole vielä toimittaneet uusia tulotietoja. Varhaiskasvatuksen asiakasmaksun tai palvelusetelin omavastuuosuuden perusteena olevat tulotiedot tarkistetaan vuosittain.</p>
            
            <p>Pyydämme toimittamaan tuloselvityksen eVakassa 14 päivän kuluessa tästä ilmoituksesta. eVakassa voitte myös antaa suostumuksen korkeimpaan maksuluokkaan tai tulorekisterin käyttöön.</p>
            
            <p>Mikäli ette toimita uusia tulotietoja, asiakasmaksu määräytyy korkeimman maksuluokan mukaan. Uusi maksupäätös astuu voimaan sen kuukauden alusta, kun tulotiedot on toimitettu asiakasmaksuihin.</p>
            
            <p>Lisätietoja saatte tarvittaessa Ylöjärven kaupungin verkkosivuilta.</p>
            
            <p>Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.</p>
            
            $unsubscribeFi
            
            <hr>
            
            <p>Dear client</p>
            
            <p>You have not yet submitted your latest income information. The income information used for determining the early childhood education fee or the out-of-pocket cost of a service voucher is reviewed every year.</p>
            
            <p>We ask you to submit your income statement through eVaka within 14 days of this notification. Through eVaka, you can also give your consent to the highest fee or the use of the Incomes Register.</p>
            
            <p>If you do not provide your latest income information, your client fee will be determined based on the highest fee category. The new payment decision takes effect at the beginning of the month when the income information has been submitted to customer services.</p>
            
            <p>If necessary, you can get more information from the website of the city of Ylöjärvi.</p>
            
            <p>This is an automatic message from the eVaka system. Do not reply to this message.</p>
            
            $unsubscribeEn
        """
                    .trimIndent(),
        )

    private fun outdatedIncomeNotificationExpired() =
        EmailContent.fromHtml(
            subject = subjectForRequestToReviewIncomeInfo,
            html =
                """
            <p>Hyvä asiakkaamme</p>
            
            <p>Seuraava asiakasmaksunne määräytyy korkeimman maksuluokan mukaan, sillä ette ole toimittaneet uusia tulotietoja määräaikaan mennessä.</p>
            
            <p>Lisätietoja saatte tarvittaessa Ylöjärven kaupungin verkkosivuilta.</p>
            
            <p>Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.</p>
            
            $unsubscribeFi
            
            <hr>
            
            <p>Dear client</p>
            
            <p>Your next client fee will be determined based on the highest fee category as you did not provide your latest income information by the deadline.</p>
            
            <p>If necessary, you can get more information from the website of the city of Ylöjärvi.</p>
            
            <p>This is an automatic message from the eVaka system. Do not reply to this message.</p>
            
            $unsubscribeEn
        """
                    .trimIndent(),
        )

    private fun newCustomerIncomeNotification(): EmailContent =
        EmailContent.fromHtml(
            subject = "Tulotietojen tarkastuskehotus / Request to review income information",
            html =
                """
<p>Hyvä asiakkaamme</p>
<p>Lapsenne on aloittamassa varhaiskasvatuksessa tämän kuukauden aikana. Pyydämme teitä toimittamaan tulotiedot eVaka-järjestelmän kautta tämän kuukauden loppuun mennessä.</p>
<p>Lisätietoja saatte tarvittaessa Ylöjärven kaupungin verkkosivuilta.</p>
<p>Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.</p>
$unsubscribeFi
<hr>
<p>Dear client</p>
<p>Your child is starting early childhood education during this month. We ask you to submit your income information via eVaka system by the end of this month.</p>
<p>If necessary, you can get more information from the website of the city of Ylöjärvi.</p>
<p>This is an automatic message from the eVaka system. Do not reply to this message.</p>
$unsubscribeEn
            """
                    .trimIndent(),
        )

    override fun calendarEventNotification(
        language: Language,
        events: List<CalendarEventNotificationData>,
    ): EmailContent {
        val format =
            DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(Locale.of("fi", "FI"))
        val eventsHtml =
            "<ul>" +
                events.joinToString("\n") { event ->
                    var period = event.period.start.format(format)
                    if (event.period.end != event.period.start) {
                        period += "-${event.period.end.format(format)}"
                    }
                    val groupInfo =
                        if (event.groupNames.isNotEmpty()) {
                            " (${event.groupNames.joinToString(", ")})"
                        } else {
                            ""
                        }
                    "<li>$period: ${event.title}$groupInfo</li>"
                } +
                "</ul>"
        return EmailContent.fromHtml(
            subject = "Uusia kalenteritapahtumia eVakassa / New calendar events in eVaka",
            html =
                """
                <p>eVakaan on lisätty uusia kalenteritapahtumia:</p>
                
                $eventsHtml
                
                $unsubscribeFi
                
                <hr>
                
                <p>New calendar events in eVaka:</p>
                
                $eventsHtml
                
                $unsubscribeEn
            """
                    .trimIndent(),
        )
    }

    override fun decisionNotification(): EmailContent =
        EmailContent.fromHtml(
            subject = "Uusi päätös eVakassa / New decision in eVaka",
            html =
                """
<p>Sinulle on saapunut uusi päätös Ylöjärven varhaiskasvatuksen verkkopalveluun eVakaan.</p>
$unsubscribeFi
<hr>
<p>You have received a new decision in Ylöjärvi`s early childhood education system eVaka.</p>
$unsubscribeEn
            """
                    .trimIndent(),
        )

    override fun financeDecisionNotification(decisionType: FinanceDecisionType): EmailContent {
        val (decisionTypeFi, decisionTypeEn) =
            when (decisionType) {
                FinanceDecisionType.VOUCHER_VALUE_DECISION ->
                    Pair("arvopäätös", "voucher value decision")

                FinanceDecisionType.FEE_DECISION -> Pair("maksupäätös", "fee decision")
            }
        return EmailContent.fromHtml(
            subject = "Uusi $decisionTypeFi eVakassa / New $decisionTypeEn in eVaka",
            html =
                """
<p>Sinulle on saapunut uusi $decisionTypeFi Ylöjärven varhaiskasvatuksen verkkopalveluun eVakaan.</p>
$unsubscribeFi
<hr>
<p>You have received a new $decisionTypeEn in Ylöjärvi`s early childhood education system eVaka.</p>
$unsubscribeEn
            """
                    .trimIndent(),
        )
    }

    override fun discussionSurveyReservationNotification(
        language: Language,
        notificationDetails: DiscussionSurveyReservationNotificationData,
    ): EmailContent =
        EmailContent.fromHtml(
            subject =
                "Uusi keskusteluaika varattu eVakassa / New discussion time reserved in eVaka",
            html =
                """
<p>Lapsellenne on varattu keskusteluaika</p>
<p>${notificationDetails.calendarEventTime.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}</p>
<p>${notificationDetails.calendarEventTime.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} - ${notificationDetails.calendarEventTime.endTime.format(DateTimeFormatter.ofPattern("HH:mm"))}</p>
<p>Varauksen voi peruuttaa 2 arkipäivää ennen varattua aikaa suoraan eVakan kalenterinäkymästä. Myöhempää peruutusta varten ota yhteyttä henkilökuntaan.</p>
$unsubscribeFi
<hr>
<p>New discussion time reserved for your child</p>
<p>${notificationDetails.calendarEventTime.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}</p>
<p>${notificationDetails.calendarEventTime.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} - ${notificationDetails.calendarEventTime.endTime.format(DateTimeFormatter.ofPattern("HH:mm"))}</p>
<p>Reservation can be cancelled 2 business days before the reserved time using the eVaka calendar view. For later cancellations contact the daycare staff.</p>
$unsubscribeEn
<hr>
            """
                    .trimIndent(),
        )

    override fun discussionSurveyReservationCancellationNotification(
        language: Language,
        notificationDetails: DiscussionSurveyReservationNotificationData,
    ): EmailContent =
        EmailContent.fromHtml(
            subject = "Keskusteluaika peruttu eVakassa / Discussion time cancelled in eVaka",
            html =
                """
<p>Lapsellenne varattu keskusteluaika on peruttu</p>
<p>${notificationDetails.calendarEventTime.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}</p>
<p>${notificationDetails.calendarEventTime.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} - ${notificationDetails.calendarEventTime.endTime.format(DateTimeFormatter.ofPattern("HH:mm"))}</p>
$unsubscribeFi
<hr>
<p>Discussion time reserved for your child has been cancelled</p>
<p>${notificationDetails.calendarEventTime.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}</p>
<p>${notificationDetails.calendarEventTime.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} - ${notificationDetails.calendarEventTime.endTime.format(DateTimeFormatter.ofPattern("HH:mm"))}</p>
$unsubscribeEn
<hr>
            """
                    .trimIndent(),
        )

    override fun discussionSurveyCreationNotification(
        language: Language,
        notificationDetails: DiscussionSurveyCreationNotificationData,
    ): EmailContent =
        EmailContent.fromHtml(
            subject =
                "Varaa keskusteluaika varhaiskasvatukseen / Reserve a discussion time for early childhood education",
            html =
                """
<p>Ajan voi varata eVakan kalenterinäkymästä</p>
$unsubscribeFi
<hr>
<p>You can reserve a time using eVaka calendar view</p>
$unsubscribeEn
<hr>
            """
                    .trimIndent(),
        )

    override fun discussionTimeReservationReminder(
        language: Language,
        reminderData: DiscussionTimeReminderData,
    ): EmailContent =
        EmailContent.fromHtml(
            subject =
                "Muistutus tulevasta keskusteluajasta / Reminder for an upcoming discussion time",
            html =
                """
<p>Lapsellenne on varattu keskusteluaika</p>
<p>${reminderData.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}</p>
<p>${reminderData.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} - ${
            reminderData.endTime.format(
                DateTimeFormatter.ofPattern("HH:mm")
            )
        }</p>
<p>Varauksen voi peruuttaa 2 arkipäivää ennen varattua aikaa suoraan eVakan kalenterinäkymästä. Myöhempää peruutusta varten ota yhteyttä henkilökuntaan.</p>
$unsubscribeFi
<hr>
<p>New discussion time reserved for your child</p>
<p>${reminderData.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}</p>
<p>${reminderData.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} - ${
            reminderData.endTime.format(
                DateTimeFormatter.ofPattern("HH:mm")
            )
        }</p>
<p>Reservation can be cancelled 2 business days before the reserved time using the eVaka calendar view. For later cancellations contact the daycare staff.</p>
$unsubscribeEn
<hr>
            """
                    .trimIndent(),
        )

    override fun absenceApplicationDecidedNotification(
        accepted: Boolean,
        startDate: LocalDate,
        endDate: LocalDate,
    ): EmailContent =
        EmailContent.fromHtml(
            subject = "Esiopetuksen poissaolohakemus käsitelty",
            html = "<p>Lapsesi esiopetuksen poissaolohakemus käsitelty. Lue lisää eVakasta.</p>",
        )

    override fun serviceApplicationDecidedNotification(
        accepted: Boolean,
        startDate: LocalDate,
    ): EmailContent {
        val start = startDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        return EmailContent.fromHtml(
            subject = "Palveluntarpeen muutoshakemuksesi on käsitelty",
            html =
                if (accepted) {
                    "<p>Ehdottamasi palveluntarve on hyväksytty $start alkaen.</p>"
                } else {
                    "<p>Ehdottamasi palveluntarve on hylätty, lue lisätiedot hylkäyksestä eVakassa.</p>"
                },
        )
    }

    override fun confirmationCode(confirmationCode: HtmlSafe<String>): EmailContent =
        EmailContent.fromHtml(
            subject = "eVaka-vahvistuskoodi / eVaka confirmation code",
            html =
                """
<p>eVakasta on lähetetty tämä vahvistuskoodi tietojesi muokkaamista varten. Syötä oheinen vahvistuskoodi pyydettyyn kenttään eVakassa.</p>
<hr>
<p>This confirmation code has been sent from eVaka for editing your information. Enter the provided confirmation code in the requested field in eVaka.</p>
<hr>
<p>Vahvistuskoodi / confirmation code: <strong>$confirmationCode</strong></p>
""",
        )

    override fun passwordChanged(): EmailContent =
        EmailContent.fromHtml(
            subject = "eVaka-salasanasi on vaihdettu / Your eVaka password has been changed",
            html =
                """<p>eVaka-salasanasi on vaihdettu.</p>
<p>Jos vaihdoit salasanasi itse, voit jättää tämän viestin huomiotta. Muussa tapauksessa kirjaudu eVakaan vahvalla tunnistautumisella (Kirjaudu Suomi.fi:ssä) ja vaihda salasanasi.</p>
<hr>
<p>Your eVaka password has been changed.</p>
<p>If you changed your password yourself, you can ignore this message. If not, log in to eVaka with strong authentication (Sign in using Suomi.fi) and change your password.</p>
""",
        )

    override fun emailChanged(newEmail: String): EmailContent =
        EmailContent.fromHtml(
            subject =
                "eVaka-sähköpostiosoitteesi on vaihdettu / Your eVaka email address has been changed",
            html =
                """<p>eVaka-sähköpostiosoitteesi on vaihdettu. Uusi osoitteesi on $newEmail. Et saa enää eVaka-sähköposteja vanhaan osoitteeseesi.</p>
<p>Jos muutit sähköpostiosoitteesi itse, voit jättää tämän viestin huomiotta. Muussa tapauksessa kirjaudu eVakaan vahvalla tunnistautumisella (Kirjaudu Suomi.fi:ssä) ja korjaa sähköpostiosoitteesi.</p>
<hr>
<p>Your eVaka email address has been changed. Your new address is $newEmail. You will no longer receive eVaka emails to your old address.</p>
<p>If you changed your email address yourself, you can ignore this message. If not, log in to eVaka with strong authentication (Sign in using Suomi.fi) and correct your email address.</p>
""",
        )

    override fun newBrowserLoginNotification(): EmailContent =
        EmailContent.fromHtml(
            subject = "Kirjautuminen uudella laitteella eVakaan / Login on a new device to eVaka",
            html =
                """
<p><strong>Kirjautuminen uudella laitteella eVakaan</strong></p>
<p>Havaitsimme kirjautumisen eVaka-tilillesi uudelta laitteelta. Jos tämä olit sinä, voit jättää tämän viestin huomiotta.</p>
<p><strong>Jos et tunnista tätä kirjautumista:</strong></p>
<p>Kirjaudu eVakaan vahvalla tunnistautumisella (Kirjaudu Suomi.fi:ssä) ja vaihda salasanasi.</p>
<hr>
<p><strong>Login on a new device to eVaka</strong></p>
<p>We detected a login to your eVaka account from a new device. If this was you, you can ignore this message.</p>
<p><strong>If you don't recognize this login:</strong></p>
<p>Sign into eVaka using strong authentication (Sign in using Suomi.fi) and change your password.</p>
""",
        )
}
