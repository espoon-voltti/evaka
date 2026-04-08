// SPDX-FileCopyrightText: 2021 City of Oulu
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.oulu

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

class OuluEmailMessageProvider : IEmailMessageProvider {
    val subjectForPendingDecisionEmail: String =
        "Toimenpiteitäsi odotetaan / Waiting for your action"
    val subjectForClubApplicationReceivedEmail: String =
        "Hakemus vastaanotettu / Application received"
    val subjectForDaycareApplicationReceivedEmail: String =
        "Hakemus vastaanotettu / Application received"
    val subjectForPreschoolApplicationReceivedEmail: String =
        "Hakemus vastaanotettu / Application received"

    private val securityFi =
        """
        <p><small>Tietoturvasyistä eVaka-viesteistä saamasi sähköpostit eivät koskaan sisällä linkkejä. Siksi emme ohjaa sinua lukemaan viestiä suoran linkin kautta.</small></p>
        """
            .trimIndent()
    private val securityEn =
        """
        <p><small>For security reasons, the emails you receive from eVaka messages never contain links. Therefore, we do not direct you to read the message via a direct link.</small></p>
        """
            .trimIndent()
    private val unsubscribeFi =
        """
        <p><small>Jos et halua enää saada tämänkaltaisia viestejä, voit muuttaa asetuksia eVakan Omat tiedot -sivulla.</small></p>
        """
            .trimIndent()
    private val unsubscribeEn =
        """
        <p><small>If you no longer want to receive messages like this, you can change your settings on eVaka's Personal information page.</small></p>
        """
            .trimIndent()

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
                <p>Katso lisää kalenterissa eVakassa.</p>
                $securityFi
                $unsubscribeFi
                <hr>
                <p>New calendar events in eVaka:</p>
                $eventsHtml
                <p>See more in the calendar in eVaka.</p>
                $securityEn
                $unsubscribeEn
                """
                    .trimIndent(),
        )
    }

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
                MessageType.MESSAGE -> {
                    if (thread.urgent) {
                        Pair("kiireellinen viesti", "urgent message")
                    } else {
                        Pair("viesti", "message")
                    }
                }

                MessageType.BULLETIN -> {
                    if (thread.urgent) {
                        Pair("kiireellinen tiedote", "urgent bulletin")
                    } else {
                        Pair("tiedote", "bulletin")
                    }
                }
            }

        val showSubjectInBody = isSenderMunicipalAccount && thread.type == MessageType.BULLETIN

        return EmailContent.fromHtml(
            subject = "Uusi $typeFi eVakassa / New $typeEn in eVaka",
            html =
                """
                <p>Sinulle on saapunut uusi ${if (applicationId != null) "hakemustasi koskeva " else ""}$typeFi eVakaan lähettäjältä ${thread.senderName}${if (showSubjectInBody) " otsikolla \"" + thread.title + "\"" else ""}. Lue viesti ${if (thread.urgent) "mahdollisimman pian " else ""}eVakassa.</p>
                <p>Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.</p>
                $securityFi
                $unsubscribeFi
                <hr>
                <p>You have received a new $typeEn ${if (applicationId != null) "regarding your application " else ""}in eVaka in eVaka from ${thread.senderName}${if (showSubjectInBody) " with title \"" + thread.title + "\"" else ""}. Read the message ${if (thread.urgent) "as soon as possible " else ""}in eVaka. </p>
                <p>This is an automatic message from the eVaka system. Do not reply to this message.</p>    
                $securityEn
                $unsubscribeEn
                """
                    .trimIndent(),
        )
    }

    override fun missingReservationsNotification(
        language: Language,
        checkedRange: FiniteDateRange,
    ): EmailContent {
        val start =
            checkedRange.start.format(
                DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
                    .withLocale(Locale.of("fi", "FI"))
            )
        return EmailContent(
            subject = "Läsnäolovarauksia puuttuu / There are missing attendance reservations",
            text =
                """
                Läsnäolovarauksia puuttuu seuraavalta viikolta: $start. Käythän merkitsemässä ne mahdollisimman pian.
                ----
                There are missing attendance reservations for the following week: $start. Please mark them as soon as possible.
                """
                    .trimIndent(),
            html =
                """
                <p>Läsnäolovarauksia puuttuu seuraavalta viikolta: $start. Käythän merkitsemässä ne mahdollisimman pian.</p>
                $securityFi
                $unsubscribeFi
                <hr>
                <p>There are missing attendance reservations for the following week: $start. Please mark them as soon as possible.</p>
                $securityEn
                $unsubscribeEn
                """
                    .trimIndent(),
        )
    }

    fun getPendingDecisionEmailHtml(): String =
        """
        <p>Hei!</p>
        <p>Sinulla on vastaamaton päätös Oulun varhaiskasvatukselta. Päätös tulee hyväksyä tai hylätä kahden viikon sisällä sen saapumisesta osoitteessa varhaiskasvatus.ouka.fi tai ottamalla yhteyttä päätöksessä mainittuun päiväkodin johtajaan.</p>
        <p>Tähän viestiin ei voi vastata.</p>
        $securityFi
        $unsubscribeFi
        <hr>
        <p>Hello!</p>
        <p>A decision has been made for you by the Oulu early childhood education and care services and remains unanswered. The decision must be accepted or rejected within two weeks of its arrival online at varhaiskasvatus.ouka.fi or by contacting the daycare centre manager listed in the decision.</p> 
        <p>You may not reply to this message.</p>
        $securityEn
        $unsubscribeEn
        """
            .trimIndent()

    fun getPendingDecisionEmailText(): String =
        """
        Hei! 

        Sinulla on vastaamaton päätös Oulun varhaiskasvatukselta. Päätös tulee hyväksyä tai hylätä kahden viikon sisällä sen saapumisesta osoitteessa varhaiskasvatus.ouka.fi tai ottamalla yhteyttä päätöksessä mainittuun päiväkodin johtajaan. 

        Tähän viestiin ei voi vastata.  

        ------------------------------------------------------------------------------

        Hello! 

        A decision has been made for you by the Oulu early childhood education and care services and remains unanswered. The decision must be accepted or rejected within two weeks of its arrival online at varhaiskasvatus.ouka.fi or by contacting the daycare centre manager listed in the decision. 

        You may not reply to this message.  

        """
            .trimIndent()

    fun getClubApplicationReceivedEmailHtml(): String =
        """
        <p>Hei!</p>

        <p>Olemme vastaanottaneet lapsenne hakemuksen avoimeen varhaiskasvatukseen. Pyydämme teitä olemaan yhteydessä suoraan toivomanne päiväkodin johtajaan ja tiedustelemaan vapaata avoimen varhaiskasvatuksen paikkaa.</p>

        <p>Hakemuksia käsitellään pääsääntöisesti vastaanottopäivämäärän mukaan. Sisarukset valitaan myös hakujärjestyksessä, ellei ole erityisperustetta.</p>

        <p>Päätös on nähtävissä ja hyväksyttävissä/hylättävissä varhaiskasvatus.ouka.fi.</p>

        <p>
        Ystävällisesti <br/>
        Varhaiskasvatuksen palveluohjaus <br/>
        </p>

        <p>Tähän viestiin ei voi vastata.</p>

        <hr>

        <p>Hello!</p>

        <p>We have received your child’s application for open early childhood education and care. We request you to directly contact the manager of the daycare centre you wish to enrol in and inquire for free places in open early childhood education and care.</p>

        <p>The applications are usually processed in the order they are received. Siblings will also be enrolled in the order of application unless special ground exist.</p>

        <p>The decision may be viewed and accepted or rejected online at varhaiskasvatus.ouka.fi.</p>

        <p>Yours, <br/>
        Early childhood education services coordination team <br/>
        </p>

        <p>You may not reply to this message.</p>

        """
            .trimIndent()

    fun getClubApplicationReceivedEmailText(): String =
        """
        Hei! 

        Olemme vastaanottaneet lapsenne hakemuksen avoimeen varhaiskasvatukseen. Pyydämme teitä olemaan yhteydessä suoraan toivomanne päiväkodin johtajaan ja tiedustelemaan vapaata avoimen varhaiskasvatuksen paikkaa. 

        Hakemuksia käsitellään pääsääntöisesti vastaanottopäivämäärän mukaan. Sisarukset valitaan myös hakujärjestyksessä, ellei ole erityisperustetta. 

        Päätös on nähtävissä ja hyväksyttävissä/hylättävissä varhaiskasvatus.ouka.fi.  

        Ystävällisesti,  

        Varhaiskasvatuksen palveluohjaus 

        Tähän viestiin ei voi vastata.

        ------------------------------------------------------------------------------

        Hello! 

        We have received your child’s application for open early childhood education and care. We request you to directly contact the manager of the daycare centre you wish to enrol in and inquire for free places in open early childhood education and care. 

        The applications are usually processed in the order they are received. Siblings will also be enrolled in the order of application unless special ground exist. 

        The decision may be viewed and accepted or rejected online at varhaiskasvatus.ouka.fi.  

        Yours, 

        Early childhood education services coordination team 

        You may not reply to this message. 

        """
            .trimIndent()

    fun getDaycareApplicationReceivedEmailHtml(): String =
        """
        <p>Hei!</p>

        <p>Lapsenne varhaiskasvatushakemus on vastaanotettu. Hakemuksen tehnyt huoltaja voi muokata hakemusta osoitteessa varhaiskasvatus.ouka.fi siihen saakka, kunnes palveluohjaus ottaa sen käsittelyyn. Varhaiskasvatuspaikan järjestelyaika on neljä kuukautta. Mikäli kyseessä on vanhemman äkillinen työllistyminen tai opintojen alkaminen, järjestelyaika on kaksi viikkoa. Toimittakaa tällöin työ- tai opiskelutodistus hakemuksen liitteeksi. Kahden viikon järjestelyaika alkaa todistuksen saapumispäivämäärästä. Jatketun aukiolon ja vuorohoidon palveluita järjestetään vanhempien vuorotyön perusteella.</p>

        <p><b>Mikäli lapsellenne järjestyy varhaiskasvatuspaikka jostakin hakemuksessa toivomastanne kunnallisesta varhaiskasvatuspaikasta</b>, ilmoitamme teille paikan viimeistään kaksi viikkoa ennen varhaiskasvatuksen toivottua aloitusajankohtaa. Muussa tapauksessa olemme teihin yhteydessä.</p>

        <p><b>Mikäli valitsitte ensimmäiseksi hakutoiveeksi yksityisen päiväkodin tai perhepäivähoitajan</b>, olkaa suoraan yhteydessä kyseiseen palveluntuottajaan varmistaaksenne varhaiskasvatuspaikan saamisen. Mikäli toivomanne palveluntuottaja ei pysty tarjoamaan varhaiskasvatuspaikkaa, pyydämme teitä olemaan yhteydessä varhaiskasvatuksen palveluohjaukseen.</p>

        <p><b>Siirtohakemukset</b> (lapsella on jo varhaiskasvatuspaikka Oulun kaupungin varhaiskasvatusyksikössä) käsitellään pääsääntöisesti hakemuksen saapumispäivämäärän mukaan. Merkittäviä syitä siirtoon ovat: aikaisemman varhaiskasvatuspaikan lakkauttaminen, sisarukset ovat eri yksiköissä, pitkä matka, huonot kulkuyhteydet, lapsen ikä, ryhmän ikärakenne, vuorohoidon tarpeen loppuminen sekä huomioon otettavat erityisperusteet.</p>

        <p><b>Mikäli ilmoititte hakemuksessa lapsenne tuen tarpeesta</b>, varhaiskasvatuksen erityisopettaja on teihin yhteydessä, jotta lapsen tuen tarpeet voidaan ottaa huomioon paikkaa osoitettaessa.</p>

        <p>Päätös on nähtävissä ja hyväksyttävissä/hylättävissä varhaiskasvatus.ouka.fi.</p>

        <p>
        Ystävällisesti <br/>
        Varhaiskasvatuksen palveluohjaus <br/>
        </p>

        <p>Tähän viestiin ei voi vastata.</p>


        <hr>

        <p>Hello!</p>

        <p>The early childhood education and care application for your child has been received. The guardian who filed the application may edit it online at varhaiskasvatus.ouka.fi until such time as the service coordination team takes it up for processing. The time necessary to organize a place in early childhood education and care is four months. If care must begin earlier due to a parent’s sudden employment or beginning of their studies, the minimum time of notice is two weeks. In such a case, a certificate of employment or student status must be presented as an appendix to the application. The two weeks’ notice begins at the date this certificate is submitted. Extended opening hours and round-the-clock care services are provided if necessitated by the parents’ working hours.</p>

        <p>If placement in early childhood care and education can be offered for your child in one of the municipal early childhood education and care locations specified in your application, we will inform you of the location two before the intended start date at the latest. If not, we will contact you by telephone.</p>

        <p>If the first care location you picked is a private daycare centre or child minder, you should directly contact the service provider in question to ensure placement can be offered to you. If the service provider your picked is unable to offer you a place in care, we request you to contact the early childhood education and care services service counselling centre.</p>

        <p>Transfer applications (for children who are already enrolled in a City of Oulu early childhood education and care unit) will usually be processed in the order such applications are received. Acceptable reasons for transfer include: shutdown of the current care location, siblings enrolled in a different unit, a long distance, poor transportation connections, the age of the child, the age structure of the group, the end of a need for round-the-clock care, and other specific grounds to be considered individually.</p>

        <p>If you have specified a need for special support for your child in the application, a special needs early childhood education teacher will contact you in order to best consider your child’s need for support in making the enrolment decision.</p>

        <p>The decision may be viewed and accepted or rejected online at varhaiskasvatus.ouka.fi</p>

        <p>Yours, <br/>
        Early childhood education services coordination team <br/>
        </p>

        <p>You may not reply to this message.</p>

        """
            .trimIndent()

    fun getDaycareApplicationReceivedEmailText(): String =
        """
        Hei! 

        Lapsenne varhaiskasvatushakemus on vastaanotettu. Hakemuksen tehnyt huoltaja voi muokata hakemusta osoitteessa varhaiskasvatus.ouka.fi siihen saakka, kunnes palveluohjaus ottaa sen käsittelyyn. Varhaiskasvatuspaikan järjestelyaika on neljä kuukautta. Mikäli kyseessä on vanhemman äkillinen työllistyminen tai opintojen alkaminen, järjestelyaika on kaksi viikkoa. Toimittakaa tällöin työ- tai opiskelutodistus hakemuksen liitteeksi. Kahden viikon järjestelyaika alkaa todistuksen saapumispäivämäärästä. Jatketun aukiolon ja vuorohoidon palveluita järjestetään vanhempien vuorotyön perusteella. 

        Mikäli lapsellenne järjestyy varhaiskasvatuspaikka jostakin hakemuksessa toivomastanne kunnallisesta varhaiskasvatuspaikasta, ilmoitamme teille paikan viimeistään kaksi viikkoa ennen varhaiskasvatuksen toivottua aloitusajankohtaa. Muussa tapauksessa olemme teihin yhteydessä.  

        Mikäli valitsitte ensimmäiseksi hakutoiveeksi yksityisen päiväkodin tai perhepäivähoitajan, olkaa suoraan yhteydessä kyseiseen palveluntuottajaan varmistaaksenne varhaiskasvatuspaikan saamisen. Mikäli toivomanne palveluntuottaja ei pysty tarjoamaan varhaiskasvatuspaikkaa, pyydämme teitä olemaan yhteydessä varhaiskasvatuksen palveluohjaukseen. 

        Siirtohakemukset (lapsella on jo varhaiskasvatuspaikka Oulun kaupungin varhaiskasvatusyksikössä) käsitellään pääsääntöisesti hakemuksen saapumispäivämäärän mukaan. Merkittäviä syitä siirtoon ovat: aikaisemman varhaiskasvatuspaikan lakkauttaminen, sisarukset ovat eri yksiköissä, pitkä matka, huonot kulkuyhteydet, lapsen ikä, ryhmän ikärakenne, vuorohoidon tarpeen loppuminen sekä huomioon otettavat erityisperusteet. 

        Mikäli ilmoititte hakemuksessa lapsenne tuen tarpeesta, varhaiskasvatuksen erityisopettaja on teihin yhteydessä, jotta lapsen tuen tarpeet voidaan ottaa huomioon paikkaa osoitettaessa.  

        Päätös on nähtävissä ja hyväksyttävissä/hylättävissä varhaiskasvatus.ouka.fi.  

        Hakemuksen liitteet voi lisätä suoraan sähköiselle hakemukselle varhaiskasvatus.ouka.fi tai postitse osoitteeseen Varhaiskasvatuksen palveluohjaus, PL 75, 90015 Oulun kaupunki. 

        Ystävällisesti, 
        Varhaiskasvatuksen palveluohjaus 

        Tähän viestiin ei voi vastata.

        ------------------------------------------------------------------------------

        Hello! 

        The early childhood education and care application for your child has been received. The guardian who filed the application may edit it online at varhaiskasvatus.ouka.fi until such time as the service coordination team takes it up for processing. The time necessary to organize a place in early childhood education and care is four months. If care must begin earlier due to a parent’s sudden employment or beginning of their studies, the minimum time of notice is two weeks. In such a case, a certificate of employment or student status must be presented as an appendix to the application. The two weeks’ notice begins at the date this certificate is submitted. Extended opening hours and round-the-clock care services are provided if necessitated by the parents’ working hours. 

        If placement in early childhood care and education can be offered for your child in one of the municipal early childhood education and care locations specified in your application, we will inform you of the location two before the intended start date at the latest. If not, we will contact you by telephone.  

        If the first care location you picked is a private daycare centre or child minder, you should directly contact the service provider in question to ensure placement can be offered to you. If the service provider your picked is unable to offer you a place in care, we request you to contact the early childhood education and care services service counselling centre. 

        Transfer applications (for children who are already enrolled in a City of Oulu early childhood education and care unit) will usually be processed in the order such applications are received. Acceptable reasons for transfer include: shutdown of the current care location, siblings enrolled in a different unit, a long distance, poor transportation connections, the age of the child, the age structure of the group, the end of a need for round-the-clock care, and other specific grounds to be considered individually. 

        If you have specified a need for special support for your child in the application, a special needs early childhood education teacher will contact you in order to best consider your child’s need for support in making the enrolment decision.  

        The decision may be viewed and accepted or rejected online at varhaiskasvatus.ouka.fi.  

        Yours, 
        Early childhood education services coordination team 

        You may not reply to this message. 

        """
            .trimIndent()

    fun getPreschoolApplicationReceivedEmailHtml(): String =
        """
        <p>Hei!</p>

        <p>Olemme vastaanottaneet lapsenne ilmoittautumisen esiopetukseen. Hakemuksen tehnyt huoltaja voi muokata hakemusta siihen saakka, kunnes palveluohjaus ottaa sen käsittelyyn. Varhaiskasvatuksen palveluohjaus sijoittaa kaikki esiopetukseen ilmoitetut lapset esiopetusyksiköihin maaliskuun aikana. Päätös on nähtävissä ja hyväksyttävissä/hylättävissä varhaiskasvatus.ouka.fi.</p>

        <p>Mikäli hakemaanne yksikköön ei perusteta esiopetusryhmää, palveluohjaus on teihin yhteydessä ja tarjoaa paikkaa sellaisesta yksiköstä, johon esiopetusryhmä on muodostunut.</p>

        <p>Mikäli ilmoititte hakemuksessa lapsenne tuen tarpeesta, varhaiskasvatuksen erityisopettaja on teihin yhteydessä, jotta lapsen tuen tarpeet voidaan ottaa huomioon paikkaa osoitettaessa.</p>

        <p><b>ESIOPETUKSEEN LIITTYVÄ VARHAISKASVATUS</b></p>

        <p>Mikäli hait esiopetukseen liittyvää varhaiskasvatusta, otathan huomioon:</p>
        <ul><li>Varhaiskasvatuspaikan järjestelyaika on neljä kuukautta. Jatketun aukiolon ja vuorohoidon palveluita järjestetään vanhempien vuorotyön tai iltaisin ja/tai viikonloppuisin tapahtuvan opiskelun perusteella.</li>
        <li><b>Mikäli lapsellenne järjestyy varhaiskasvatuspaikka jostakin hakemuksessa toivomastanne kunnallisesta varhaiskasvatuspaikasta,</b> ilmoitamme teille paikan viimeistään kaksi viikkoa ennen varhaiskasvatuksen toivottua aloitusajankohtaa. Muussa tapauksessa olemme teihin yhteydessä.</li>
        <li><b>Mikäli valitsitte ensimmäiseksi hakutoiveeksi yksityisen päiväkodin</b>, olkaa suoraan yhteydessä kyseiseen yksikköön varmistaaksenne varhaiskasvatuspaikan saamisen. Mikäli toivomanne palveluntuottaja ei pysty tarjoamaan varhaiskasvatuspaikkaa, pyydämme teitä olemaan yhteydessä varhaiskasvatuksen palveluohjaukseen.</li>
        <li><b>Siirtohakemukset</b> (lapsella on jo varhaiskasvatuspaikka Oulun kaupungin varhaiskasvatusyksikössä) käsitellään pääsääntöisesti hakemuksen saapumispäivämäärän mukaan. Merkittäviä syitä siirtoon ovat: aikaisemman varhaiskasvatuspaikan lakkauttaminen, sisarukset ovat eri yksiköissä, pitkä matka, huonot kulkuyhteydet, lapsen ikä, ryhmän ikärakenne, vuorohoidon tarpeen loppuminen sekä huomioon otettavat erityisperusteet.</li>
        </ul>
        <p>Päätös on nähtävissä ja hyväksyttävissä/hylättävissä varhaiskasvatus.ouka.fi.</p>

        <p>Hakemuksen liitteet lisätään suoraan sähköiselle hakemukselle eVakassa.</p>

        <p>
        Ystävällisesti <br/>
        Varhaiskasvatuksen palveluohjaus <br/>
        </p>

        <p>Tähän viestiin ei voi vastata.</p>

        <hr>

        <p>Hello!</p>

        <p>We have received your child’s registration for preschool education. The guardian who filed the application may edit it online until such time as the service coordination team takes it up for processing. The early childhood education services coordination team will enrol every child registered for preschool education with a preschool education unit during March. The decision may be viewed and accepted or rejected online at varhaiskasvatus.ouka.fi.</p>

        <p>If no preschool education group will be set up in the unit you have applied for, the coordination team will contact you and offer a spot in a unit where such a group will be set up.</p>

        <p>If you have specified a need for special support for your child in the application, a special needs early childhood education teacher will contact you in order to best consider your child’s need for support in making the enrolment decision.</p>

        <p>EARLY CHILDHOOD EDUCATION AND CARE IN CONJUNCTION WITH PRESCHOOL EDUCATION</p>

        <p>If you have applied for early childhood education and care services in conjunction with preschool education, please consider the following:</p>

        <ul><li>The time necessary to organize a place in early childhood education and care is four months. Extended opening hours and round-the-clock care services are provided if necessitated by the parents’ working hours or evening and/or weekend studies.</li>
        <li><b>If placement in early childhood care and education can be offered for your child in one of the municipal early childhood education and care locations specified in your application,</b> we will inform you of the location two before the intended start date at the latest. If not, we will contact you by telephone.</li>
        <li><b>If the first care location you picked is a private daycare centre,</b> you should directly contact the service provider in question to ensure placement can be offered to you. If the service provider your picked is unable to offer you a place in care, we request you to contact the early childhood education and care services service counselling centre.</li>
        <li><b>Transfer applications</b>  (for children who are already enrolled in a City of Oulu early childhood education and care unit) will usually be processed in the order such applications are received. Acceptable reasons for transfer include: shutdown of the current care location, siblings enrolled in a different unit, a long distance, poor transportation connections, the age of the child, the age structure of the group, the end of a need for round-the-clock care, and other specific grounds to be considered individually.</li>
        </ul>

        <p>The decision may be viewed and accepted or rejected online at varhaiskasvatus.ouka.fi.</p>

        <p>The appendices to the application may be directly submitted with the online application through the eVaka service.</p>

        <p>Yours, <br/>
        Early childhood education services coordination team <br/>
        </p>

        <p>You may not reply to this message.</p>
        """
            .trimIndent()

    fun getPreschoolApplicationReceivedEmailText(): String =
        """
        Hei! 

        Olemme vastaanottaneet lapsenne ilmoittautumisen esiopetukseen. Hakemuksen tehnyt huoltaja voi muokata hakemusta siihen saakka, kunnes palveluohjaus ottaa sen käsittelyyn. Varhaiskasvatuksen palveluohjaus sijoittaa kaikki esiopetukseen ilmoitetut lapset esiopetusyksiköihin maaliskuun aikana. Päätös on nähtävissä ja hyväksyttävissä/hylättävissä varhaiskasvatus.ouka.fi.  

        Mikäli hakemaanne yksikköön ei perusteta esiopetusryhmää, palveluohjaus on teihin yhteydessä ja tarjoaa paikkaa sellaisesta yksiköstä, johon esiopetusryhmä on muodostunut.          

        Mikäli ilmoititte hakemuksessa lapsenne tuen tarpeesta, varhaiskasvatuksen erityisopettaja on teihin yhteydessä, jotta lapsen tuen tarpeet voidaan ottaa huomioon paikkaa osoitettaessa.  

        ESIOPETUKSEEN LIITTYVÄ VARHAISKASVATUS 

        Mikäli hait esiopetukseen liittyvää varhaiskasvatusta, otathan huomioon: 

        - Varhaiskasvatuspaikan järjestelyaika on neljä kuukautta. Jatketun aukiolon ja vuorohoidon palveluita järjestetään vanhempien vuorotyön tai iltaisin ja/tai viikonloppuisin tapahtuvan opiskelun perusteella. 

        - Mikäli lapsellenne järjestyy varhaiskasvatuspaikka jostakin hakemuksessa toivomastanne kunnallisesta varhaiskasvatuspaikasta, ilmoitamme teille paikan viimeistään kaksi viikkoa ennen varhaiskasvatuksen toivottua aloitusajankohtaa. Muussa tapauksessa olemme teihin yhteydessä.  

        - Mikäli valitsitte ensimmäiseksi hakutoiveeksi yksityisen päiväkodin, olkaa suoraan yhteydessä kyseiseen yksikköön varmistaaksenne varhaiskasvatuspaikan saamisen. Mikäli toivomanne palveluntuottaja ei pysty tarjoamaan varhaiskasvatuspaikkaa, pyydämme teitä olemaan yhteydessä varhaiskasvatuksen palveluohjaukseen. 

        - Siirtohakemukset (lapsella on jo varhaiskasvatuspaikka Oulun kaupungin varhaiskasvatusyksikössä) käsitellään pääsääntöisesti hakemuksen saapumispäivämäärän mukaan. Merkittäviä syitä siirtoon ovat: aikaisemman varhaiskasvatuspaikan lakkauttaminen, sisarukset ovat eri yksiköissä, pitkä matka, huonot kulkuyhteydet, lapsen ikä, ryhmän ikärakenne, vuorohoidon tarpeen loppuminen sekä huomioon otettavat erityisperusteet. 

        Päätös on nähtävissä ja hyväksyttävissä/hylättävissä varhaiskasvatus.ouka.fi.  

        Hakemuksen liitteet lisätään suoraan sähköiselle hakemukselle eVakassa. 

        Ystävällisesti, 
        Varhaiskasvatuksen palveluohjaus 

        Tähän viestiin ei voi vastata.

        ------------------------------------------------------------------------------

        Hello! 

        We have received your child’s registration for preschool education. The guardian who filed the application may edit it online until such time as the service coordination team takes it up for processing. The early childhood education services coordination team will enrol every child registered for preschool education with a preschool education unit during March. The decision may be viewed and accepted or rejected online at varhaiskasvatus.ouka.fi.  

        If no preschool education group will be set up in the unit you have applied for, the coordination team will contact you and offer a spot in a unit where such a group will be set up. 

        If you have specified a need for special support for your child in the application, a special needs early childhood education teacher will contact you in order to best consider your child’s need for support in making the enrolment decision.  

        EARLY CHILDHOOD EDUCATION AND CARE IN CONJUNCTION WITH PRESCHOOL EDUCATION             

        If you have applied for early childhood education and care services in conjunction with preschool education, please consider the following: 

        - The time necessary to organize a place in early childhood education and care is four months. Extended opening hours and round-the-clock care services are provided if necessitated by the parents’ working hours or evening and/or weekend studies. 

        - If placement in early childhood care and education can be offered for your child in one of the municipal early childhood education and care locations specified in your application, we will inform you of the location two before the intended start date at the latest. If not, we will contact you by telephone.  

        - If the first care location you picked is a private daycare centre, you should directly contact the service provider in question to ensure placement can be offered to you. If the service provider your picked is unable to offer you a place in care, we request you to contact the early childhood education and care services service counselling centre. 

        - Transfer applications (for children who are already enrolled in a City of Oulu early childhood education and care unit) will usually be processed in the order such applications are received. Acceptable reasons for transfer include: shutdown of the current care location, siblings enrolled in a different unit, a long distance, poor transportation connections, the age of the child, the age structure of the group, the end of a need for round-the-clock care, and other specific grounds to be considered individually. 

        The decision may be viewed and accepted or rejected online at varhaiskasvatus.ouka.fi.         

        The appendices to the application may be directly submitted with the online application through the eVaka service.  

        Yours, 
        Early childhood education services coordination team 

        You may not reply to this message. 

        """
            .trimIndent()

    override fun pendingDecisionNotification(language: Language): EmailContent =
        EmailContent(
            subjectForPendingDecisionEmail,
            getPendingDecisionEmailText(),
            getPendingDecisionEmailHtml(),
        )

    override fun clubApplicationReceived(language: Language): EmailContent =
        EmailContent(
            subjectForClubApplicationReceivedEmail,
            getClubApplicationReceivedEmailText(),
            getClubApplicationReceivedEmailHtml(),
        )

    override fun daycareApplicationReceived(language: Language): EmailContent =
        EmailContent(
            subjectForDaycareApplicationReceivedEmail,
            getDaycareApplicationReceivedEmailText(),
            getDaycareApplicationReceivedEmailHtml(),
        )

    override fun preschoolApplicationReceived(
        language: Language,
        withinApplicationPeriod: Boolean,
    ): EmailContent =
        EmailContent(
            subjectForPreschoolApplicationReceivedEmail,
            getPreschoolApplicationReceivedEmailText(),
            getPreschoolApplicationReceivedEmailHtml(),
        )

    override fun childDocumentNotification(
        language: Language,
        childId: ChildId,
        notificationType: ChildDocumentNotificationType,
    ): EmailContent {
        if (notificationType == ChildDocumentNotificationType.EDITABLE_DOCUMENT) {
            return EmailContent.fromHtml(
                subject = "Uusi täytettävä asiakirja eVakassa / New fillable document in eVaka",
                html =
                    """
                <p>Sinua on pyydetty täyttämään asiakirja eVakassa. Lue dokumentti eVakassa.</p>
                <p>Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.</p>
                $unsubscribeFi
                <hr>
                <p>You have been requested to fill out a document in eVaka. Read the document in eVaka.</p>
                <p>This is an automatic message from the eVaka system. Do not reply to this message.</p>
                $unsubscribeEn
""",
            )
        }
        return EmailContent.fromHtml(
            subject = "Uusi dokumentti eVakassa / New document in eVaka",
            html =
                """
                <p>Sinulle on saapunut uusi dokumentti eVakaan. Lue dokumentti eVakassa.</p>
                <p>Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.</p>
                $securityFi
                $unsubscribeFi
                <hr>
                <p>You have received a new eVaka document. Read the document in eVaka.</p>
                <p>This is an automatic message from the eVaka system. Do not reply to this message.</p>
                $securityEn
                $unsubscribeEn
""",
        )
    }

    override fun pedagogicalDocumentNotification(
        language: Language,
        childId: ChildId,
    ): EmailContent =
        EmailContent(
            subject = "Uusi pedagoginen dokumentti eVakassa / New pedagogical document in eVaka",
            text =
                """
                Sinulle on saapunut uusi pedagoginen dokumentti eVakaan. Lue dokumentti eVakassa.

                Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.

                -----

                You have received a new eVaka pedagogical document. Read the document in eVaka.

                This is an automatic message from the eVaka system. Do not reply to this message.  
                """
                    .trimIndent(),
            html =
                """
                <p>Sinulle on saapunut uusi pedagoginen dokumentti eVakaan. Lue dokumentti eVakassa.</p>
                <p>Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.</p>
                $securityFi
                $unsubscribeFi
                <hr>
                
                <p>You have received a new eVaka pedagogical document. Read the document in eVaka.</p>
                <p>This is an automatic message from the eVaka system. Do not reply to this message.</p>
                $securityEn
                $unsubscribeEn
                """
                    .trimIndent(),
        )

    override fun incomeNotification(
        notificationType: IncomeNotificationType,
        language: Language,
    ): EmailContent =
        when (notificationType) {
            IncomeNotificationType.INITIAL_EMAIL -> outdatedIncomeNotificationInitial()
            IncomeNotificationType.REMINDER_EMAIL -> outdatedIncomeNotificationReminder()
            IncomeNotificationType.EXPIRED_EMAIL -> outdatedIncomeNotificationExpired()
            IncomeNotificationType.NEW_CUSTOMER -> newCustomerIncomeNotification()
        }

    fun outdatedIncomeNotificationInitial(): EmailContent =
        EmailContent(
            subject = "Tulotietojen tarkastus- kehotus / Request to review income information",
            text =
                """
                Hyvä asiakkaamme

                Varhaiskasvatuksen asiakasmaksun tai palvelusetelin omavastuuosuuden perusteena olevat tulotiedot tulotietonne ovat vanhentumassa.

                Pyydämme toimittamaan tuloselvityksen eVakassa 14 päivän kuluessa tästä ilmoituksesta.

                Mikäli ette toimita uusia tulotietoja, asiakasmaksu määräytyy korkeimman maksun mukaisesti.

                Lisätietoja saatte tarvittaessa: varhaiskasvatusmaksut@ouka.fi

                Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.

                -----

                Dear client

                The income information used for determining the early childhood education fee or the out-of-pocket cost of a service voucher is reviewed when income information is out of date.

                We ask you to submit your income statement through eVaka within 14 days of this notification.

                If you do not provide your latest income information, your client fee will be determined based on the highest fee.

                Inquiries: varhaiskasvatusmaksut@ouka.fi

                This is an automatic message from the eVaka system. Do not reply to this message.
                """
                    .trimIndent(),
            html =
                """
                <p>Hyvä asiakkaamme</p>
                <p>Varhaiskasvatuksen asiakasmaksun tai palvelusetelin omavastuuosuuden perusteena olevat tulotietonne ovat vanhentumassa.</p>
                <p>Pyydämme toimittamaan tuloselvityksen eVakassa 14 päivän kuluessa tästä ilmoituksesta.</p>
                <p>Mikäli ette toimita uusia tulotietoja, asiakasmaksu määräytyy korkeimman maksun mukaisesti.</p>
                <p>Lisätietoja saatte tarvittaessa: varhaiskasvatusmaksut@ouka.fi</p>
                <p>Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.</p>
                $securityFi
                $unsubscribeFi
                <hr>
                <p>Dear client</p>
                <p>The income information used for determining the early childhood education fee or the out-of-pocket cost of a service voucher is reviewed when income information is out of date.</p>
                <p>We ask you to submit your income statement through eVaka within 14 days of this notification.</p>
                <p>If you do not provide your latest income information, your client fee will be determined based on the highest fee.</p>
                <p>Inquiries: varhaiskasvatusmaksut@ouka.fi</p>
                <p>This is an automatic message from the eVaka system. Do not reply to this message.</p>
                $securityEn
                $unsubscribeEn
                """
                    .trimIndent(),
        )

    fun outdatedIncomeNotificationReminder(): EmailContent =
        EmailContent(
            subject = "Tulotietojen tarkastus- kehotus / Request to review income information",
            text =
                """
                Hyvä asiakkaamme

                Ette ole vielä toimittaneet uusia tulotietoja. Varhaiskasvatuksen asiakasmaksun tai palvelusetelin omavastuuosuuden perusteena olevat tulotiedot tarkistetaan vuosittain.

                Pyydämme toimittamaan tuloselvityksen eVakassa 7 päivän kuluessa tästä ilmoituksesta. eVakassa voitte myös antaa suostumuksen korkeimpaan maksuluokkaan tai tulorekisterin käyttöön.

                Mikäli ette toimita uusia tulotietoja, asiakasmaksu määräytyy korkeimman maksuluokan mukaan.

                Lisätietoja saatte tarvittaessa: varhaiskasvatusmaksut@ouka.fi

                Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.

                -----

                Dear client

                You have not yet submitted your latest income information. The income information used for determining the early childhood education fee or the out-of-pocket cost of a service voucher is reviewed every year.

                We ask you to submit your income statement through eVaka within 7 days of this notification. Through eVaka, you can also give your consent to the highest fee or the use of the Incomes Register.

                If you do not provide your latest income information, your client fee will be determined based on the highest fee category.

                Inquiries: varhaiskasvatusmaksut@ouka.fi

                This is an automatic message from the eVaka system. Do not reply to this message.
                """
                    .trimIndent(),
            html =
                """
                        <p>Hyvä asiakkaamme</p>
                        <p>Ette ole vielä toimittaneet uusia tulotietoja. Varhaiskasvatuksen asiakasmaksun tai palvelusetelin omavastuuosuuden perusteena olevat tulotiedot tarkistetaan vuosittain.</p>
                        <p>Pyydämme toimittamaan tuloselvityksen eVakassa 7 päivän kuluessa tästä ilmoituksesta. eVakassa voitte myös antaa suostumuksen korkeimpaan maksuluokkaan tai tulorekisterin käyttöön.</p>
                        <p>Mikäli ette toimita uusia tulotietoja, asiakasmaksu määräytyy korkeimman maksuluokan mukaan.</p>
                        <p>Lisätietoja saatte tarvittaessa: varhaiskasvatusmaksut@ouka.fi</p>
                        <p>Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.</p>
                        $securityFi
                        $unsubscribeFi
                        <hr>
                        <p>Dear client</p>
                        <p>You have not yet submitted your latest income information. The income information used for determining the early childhood education fee or the out-of-pocket cost of a service voucher is reviewed every year.</p>
                        <p>We ask you to submit your income statement through eVaka within 7 days of this notification. Through eVaka, you can also give your consent to the highest fee or the use of the Incomes Register.</p>
                        <p>If you do not provide your latest income information, your client fee will be determined based on the highest fee category.</p>
                        <p>Inquiries: varhaiskasvatusmaksut@ouka.fi</p>
                        <p>This is an automatic message from the eVaka system. Do not reply to this message.</p>
                        $securityEn
                $unsubscribeEn
                """
                    .trimIndent(),
        )

    fun outdatedIncomeNotificationExpired(): EmailContent =
        EmailContent(
            subject = "Tulotietojen tarkastus- kehotus / Request to review income information",
            text =
                """
                Hyvä asiakkaamme

                Seuraava asiakasmaksunne määräytyy korkeimman maksuluokan mukaan, sillä ette ole toimittaneet uusia tulotietoja määräaikaan mennessä.

                Lisätietoja saatte tarvittaessa: varhaiskasvatusmaksut@ouka.fi

                Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.

                -----

                Dear client

                Your next client fee will be determined based on the highest fee category as you did not provide your latest income information by the deadline.

                Inquiries: varhaiskasvatusmaksut@ouka.fi

                This is an automatic message from the eVaka system. Do not reply to this message.  
                """
                    .trimIndent(),
            html =
                """
                <p>Hyvä asiakkaamme</p>
                <p>Seuraava asiakasmaksunne määräytyy korkeimman maksuluokan mukaan, sillä ette ole toimittaneet uusia tulotietoja määräaikaan mennessä.</p>
                <p>Lisätietoja saatte tarvittaessa: varhaiskasvatusmaksut@ouka.fi</p>
                <p>Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.</p>
                $securityFi
                $unsubscribeFi
                <hr>
                <p>Dear client</p>
                <p>Your next client fee will be determined based on the highest fee category as you did not provide your latest income information by the deadline.</p>
                <p>Inquiries: varhaiskasvatusmaksut@ouka.fi</p>
                <p>This is an automatic message from the eVaka system. Do not reply to this message.</p>
                $securityEn
                $unsubscribeEn
                """
                    .trimIndent(),
        )

    fun newCustomerIncomeNotification(): EmailContent =
        EmailContent(
            subject = "Tulotietojen tarkastuskehotus / Request to review income information",
            text =
                """
                Hyvä asiakkaamme

                Lapsenne on aloittamassa varhaiskasvatuksessa tämän kuukauden aikana. Pyydämme teitä toimittamaan tulotiedot eVaka-järjestelmän kautta tämän kuukauden loppuun mennessä.

                Lisätietoja saatte tarvittaessa: varhaiskasvatusmaksut@ouka.fi

                Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.

                -----

                Dear client

                Your child is starting early childhood education during this month. We ask you to submit your income information via eVaka system by the end of this month.

                Inquiries: varhaiskasvatusmaksut@ouka.fi

                This is an automatic message from the eVaka system. Do not reply to this message.
                """
                    .trimIndent(),
            html =
                """
                <p>Hyvä asiakkaamme</p>
                <p>Lapsenne on aloittamassa varhaiskasvatuksessa tämän kuukauden aikana. Pyydämme teitä toimittamaan tulotiedot eVaka-järjestelmän kautta tämän kuukauden loppuun mennessä.</p>
                <p>Lisätietoja saatte tarvittaessa: varhaiskasvatusmaksut@ouka.fi</p>
                <p>Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.</p>
                $securityFi
                $unsubscribeFi
                <hr>
                <p>Dear client</p>
                <p>Your child is starting early childhood education during this month. We ask you to submit your income information via eVaka system by the end of this month.</p>
                <p>Inquiries: varhaiskasvatusmaksut@ouka.fi</p>
                <p>This is an automatic message from the eVaka system. Do not reply to this message.</p>
                $securityEn
                $unsubscribeEn
                """
                    .trimIndent(),
        )

    override fun missingHolidayReservationsNotification(language: Language): EmailContent =
        EmailContent.fromHtml(
            subject = "Loma-ajan ilmoitus sulkeutuu / Holiday notification period closing",
            html =
                """
            <p>Loma-ajan kysely sulkeutuu kahden päivän päästä. Jos lapseltanne/lapsiltanne puuttuu loma-ajan ilmoitus yhdeltä tai useammalta lomapäivältä, teettehän ilmoituksen eVakan kalenterissa mahdollisimman pian.</p>
            $securityFi
            $unsubscribeFi
            <hr>
            <p>Two days left to submit a holiday notification. If you have not submitted a notification for each day, please submit them through the eVaka calendar as soon as possible.</p>
            $securityEn
            $unsubscribeEn
            """,
        )

    override fun financeDecisionNotification(decisionType: FinanceDecisionType): EmailContent {
        val (decisionTypeFi, decisionTypeEn) =
            when (decisionType) {
                FinanceDecisionType.VOUCHER_VALUE_DECISION -> {
                    Pair("arvopäätös", "voucher value decision")
                }

                FinanceDecisionType.FEE_DECISION -> {
                    Pair("maksupäätös", "fee decision")
                }
            }
        return EmailContent.fromHtml(
            subject = "Uusi $decisionTypeFi eVakassa / New $decisionTypeEn in eVaka",
            html =
                """
                <p>Sinulle on saapunut uusi $decisionTypeFi eVakaan.</p>
                <p>Päätös on nähtävissä eVakassa.</p>
                $securityFi
                $unsubscribeFi
                <hr>
                <p>You have received a new $decisionTypeEn in eVaka.</p>
                <p>The decision can be viewed on eVaka.</p>
                $securityEn
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
                <p>Uusi keskusteluaika varattu / New discussion time reserved</p>
                <p>${notificationDetails.calendarEventTime.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}</p>
                <p>${notificationDetails.calendarEventTime.startTime.format(
                    DateTimeFormatter.ofPattern("HH:mm")
                )} - ${notificationDetails.calendarEventTime.endTime.format(DateTimeFormatter.ofPattern("HH:mm"))}</p>
                <hr>
                $securityFi
                $unsubscribeFi
                $securityEn
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
                <p>Varattu keskusteluaika peruttu / Reserved discussion time cancelled</p>
                <p>${notificationDetails.calendarEventTime.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}</p>
                <p>${notificationDetails.calendarEventTime.startTime.format(
                    DateTimeFormatter.ofPattern("HH:mm")
                )} - ${notificationDetails.calendarEventTime.endTime.format(DateTimeFormatter.ofPattern("HH:mm"))}</p>
                <hr>
                $securityFi
                $unsubscribeFi
                $securityEn
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
<p>${notificationDetails.eventTitle}</p>
<p>${notificationDetails.eventDescription}</p>
<p>Ajan voi varata eVakan kalenterinäkymästä</p>
$securityFi
$unsubscribeFi
<hr>
<p>${notificationDetails.eventTitle}</p>
<p>${notificationDetails.eventDescription}</p>
<p>You can reserve a time using eVaka calendar view</p>
$securityEn
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
$securityFi
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
$securityEn
$unsubscribeEn
<hr>
                """
                    .trimIndent(),
        )

    override fun decisionNotification(): EmailContent =
        EmailContent.fromHtml(
            subject = "Uusi päätös eVakassa / New decision in eVaka",
            html =
                """
<p>Sinulle on saapunut uusi päätös eVakaan.</p>
<p>Päätös on nähtävissä eVakassa.</p>
$securityFi
$unsubscribeFi
<hr>
<p>You have received a new decision in eVaka.</p>
<p>The decision can be viewed on eVaka.</p>
$securityEn
$unsubscribeEn
                """
                    .trimIndent(),
        )

    override fun serviceApplicationDecidedNotification(
        accepted: Boolean,
        startDate: LocalDate,
    ): EmailContent {
        val start = startDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        return EmailContent.fromHtml(
            subject =
                "Palveluntarpeen muutoshakemuksesi on käsitelty / " +
                    "Your application for a change in the service need has been processed",
            html =
                if (accepted) {
                    """
                    <p>Ehdottamasi palveluntarve on hyväksytty $start alkaen.</p>
                    $securityFi
                    $unsubscribeFi
                    <p>The service request you proposed has been approved starting from $start.</p>
                    $securityEn
                    $unsubscribeEn
                    """
                        .trimIndent()
                } else {
                    """
                    <p>Ehdottamasi palveluntarve on hylätty, lue lisätiedot hylkäyksestä eVakassa.</p>
                    $securityFi
                    $unsubscribeFi
                    <p>The service request you proposed has been rejected. Read more details about the rejection in eVaka.</p>
                    $securityEn
                    $unsubscribeEn
                    """
                        .trimIndent()
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

    override fun absenceApplicationDecidedNotification(
        accepted: Boolean,
        startDate: LocalDate,
        endDate: LocalDate,
    ): EmailContent {
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val range = "${formatter.format(startDate)} - ${formatter.format(endDate)}"
        return EmailContent.fromHtml(
            subject =
                if (accepted) {
                    "Esiopetuksen poissaolohakemus hyväksytty / The pre-primary education absence request has been approved"
                } else {
                    "Esiopetuksen poissaolohakemus hylätty / The pre-primary education absence request has been denied"
                },
            html =
                if (accepted) {
                    """
                    <p>Lapsesi esiopetuksen poissaolohakemus on hyväksytty ja poissaolot on merkitty eVakaan. Lue lisää eVakasta.</p>
                    <p>Your child's application for absence from pre-primary education has been approved, and the absence has been 
                    recorded in eVaka. Read more about eVaka.</p>
                    """
                        .trimIndent()
                } else {
                    """
                    <p>Lapsesi esiopetuksen poissaolohakemus on hylätty. Lue lisää eVakasta. </p>
                    <p>Your child's application for absence from pre-primary educationhas been declined. Read more about eVaka.</p>
                    """
                        .trimIndent()
                },
        )
    }

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
