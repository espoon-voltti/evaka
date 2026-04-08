// SPDX-FileCopyrightText: 2021 City of Turku
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.turku

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

class TurkuEmailMessageProvider : IEmailMessageProvider {
    private val subjectForPendingDecisionEmail: String = "Toimenpiteitäsi odotetaan"
    private val subjectForClubApplicationReceivedEmail: String = "Hakemus vastaanotettu"
    private val subjectForDaycareApplicationReceivedEmail: String = "Hakemus vastaanotettu"
    private val subjectForPreschoolApplicationReceivedEmail: String = "Hakemus vastaanotettu"

    private val securityFi =
        """
        <p><small>Tietoturvasyistä eVaka-viesteistä saamasi sähköpostit eivät koskaan sisällä linkkejä. Siksi emme ohjaa sinua lukemaan viestiä suoran linkin kautta.</small></p>
        """
            .trimIndent()
    private val securitySv =
        """
        <p><small>Av säkerhetsskäl innehåller e-postmeddelanden från eVaka aldrig länkar. Därför hänvisar vi dig inte till att läsa meddelandet via en direkt länk.</small></p>
        """
            .trimIndent()
    private val securityEn =
        """
        <p><small>For security reasons, the emails you receive from eVaka never contain links. Therefore, we do not direct you to read the message via a direct link.</small></p>
        """
            .trimIndent()
    private val unsubscribeFi =
        """
        <p><small>Jos et halua enää saada tämänkaltaisia viestejä, voit muuttaa asetuksia eVakan Omat tiedot -sivulla.</small></p>
        """
            .trimIndent()
    private val unsubscribeSv =
        """
        <p><small>Om du inte längre vill ta emot meddelanden som detta, kan du ändra dina inställningar på eVakas Personuppgifter-sida.</small></p>
        """
            .trimIndent()
    private val unsubscribeEn =
        """
        <p><small>If you no longer want to receive messages like this, you can change your settings on eVaka's Personal information page.</small></p>
        """
            .trimIndent()

    override fun messageNotification(language: Language, thread: MessageThreadData): EmailContent =
        messageNotification(language, thread, false, null)

    override fun messageNotification(
        language: Language,
        thread: MessageThreadData,
        isSenderMunicipalAccount: Boolean,
        applicationId: ApplicationId?,
    ): EmailContent {
        val (typeFi, typeSv, typeEn) =
            when (thread.type) {
                MessageType.MESSAGE -> {
                    if (thread.urgent) {
                        Triple(
                            "kiireellinen viesti",
                            "brådskande personligt meddelande",
                            "urgent message",
                        )
                    } else {
                        Triple("viesti", "personligt meddelande", "message")
                    }
                }

                MessageType.BULLETIN -> {
                    if (thread.urgent) {
                        Triple(
                            "kiireellinen tiedote",
                            "brådskande allmänt meddelande",
                            "urgent bulletin",
                        )
                    } else {
                        Triple("tiedote", "allmänt meddelande", "bulletin")
                    }
                }
            }

        val showSubjectInBody = isSenderMunicipalAccount && thread.type == MessageType.BULLETIN

        return EmailContent.fromHtml(
            subject = "Uusi $typeFi eVakassa / Nytt $typeSv i eVaka / New $typeEn in eVaka",
            html =
                """
                <p>Sinulle on saapunut uusi ${if (applicationId != null) "hakemustasi koskeva " else ""}$typeFi eVakaan lähettäjältä ${thread.senderName}${if (showSubjectInBody) " otsikolla \"" + thread.title + "\"" else ""}. Lue viesti ${if (thread.urgent) "mahdollisimman pian " else ""}eVakassa.</p>
                <p>Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.</p>
                $securityFi
                $unsubscribeFi
                <hr>
                <p>Du har fått ett nytt $typeSv ${if (applicationId != null) "angående din ansökan " else ""}i eVaka från ${thread.senderName}${if (showSubjectInBody) " med titeln \"" + thread.title + "\"" else ""}. Läs meddelandet ${if (thread.urgent) "så snart som möjligt " else ""} i eVaka.</p>
                <p>Detta besked skickas automatiskt av eVaka. Svara inte på detta besked.</p>        
                $securitySv
                $unsubscribeSv
                <hr>
                <p>You have received a new $typeEn ${if (applicationId != null) "regarding your application " else ""}in eVaka from ${thread.senderName}${if (showSubjectInBody) " with title \"" + thread.title + "\"" else ""}. Read the message ${if (thread.urgent) "as soon as possible " else ""} in eVaka.</p>
                <p>This is an automatic message from the eVaka system. Do not reply to this message.</p>  
                $securityEn
                $unsubscribeEn
                """
                    .trimIndent(),
        )
    }

    override fun childDocumentNotification(
        language: Language,
        childId: ChildId,
        notificationType: ChildDocumentNotificationType,
    ): EmailContent {
        if (notificationType == ChildDocumentNotificationType.EDITABLE_DOCUMENT) {
            return EmailContent.fromHtml(
                subject =
                    "Uusi täytettävä asiakirja eVakassa / Nytt ifyllnadsdokument i eVaka / New fillable document in eVaka",
                html =
                    """
                <p>Sinua on pyydetty täyttämään asiakirja eVakassa. Lue dokumentti eVakassa.</p>
                <p>Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.</p>
                $unsubscribeFi
                <hr>
                <p>Du har blivit ombedd at fylla i ett dokument i eVaka. Läs dokumentet i eVaka.</p>
                <p>Detta besked skickas automatiskt av eVaka. Svara inte på detta besked.</p>
                $unsubscribeSv
                <hr>
                <p>You have been requested to fill out a document in eVaka. Read the document in eVaka.</p>
                <p>This is an automatic message from the eVaka system. Do not reply to this message.</p>
                $unsubscribeEn
                """,
            )
        }
        return EmailContent.fromHtml(
            subject = "Uusi dokumentti eVakassa / Nytt dokument i eVaka / New document in eVaka",
            html =
                """
                <p>Sinulle on saapunut uusi dokumentti eVakaan. Lue dokumentti eVakassa.</p>
                <p>Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.</p>
                $securityFi
                $unsubscribeFi
                <hr>
                <p>Du har fått ett nytt dokument i eVaka. Läs dokumentet i eVaka.</p>
                <p>Detta besked skickas automatiskt av eVaka. Svara inte på detta besked.</p>
                $securitySv
                $unsubscribeSv
                <hr>
                <p>You have received a new eVaka document. Read the document in eVaka.</p>
                <p>This is an automatic message from the eVaka system. Do not reply to this message.</p>
                $securityEn
                $unsubscribeEn
                
""",
        )
    }

    override fun pendingDecisionNotification(language: Language): EmailContent =
        EmailContent(
            subjectForPendingDecisionEmail,
            getPendingDecisionEmailText(),
            getPendingDecisionEmailHtml(),
        )

    fun getPendingDecisionEmailHtml(): String =
        """
        <p>Sinulla on vastaamaton päätös Turun varhaiskasvatukselta. 
        Päätös tulee hyväksyä tai hylätä kahden viikon sisällä sen saapumisesta osoitteessa evaka.turku.fi</p>
        
        <p>Tähän viestiin ei voi vastata.</p>
        
        <p>
        Ystävällisesti,<br/>
        Varhaiskasvatuksen palveluohjaus<br/>
        </p>
        $securityFi
        $unsubscribeFi
        <hr>
        
        <p>Du har ett obesvarat beslut från Åbo stads småbarnspedagogik. 
        Godkänn eller avslå beslutet inom två veckor från mottagandedatum på adressen evaka.turku.fi</p>

        <p>Svara inte på detta meddelande.</p>

        <p>
        Med vänliga hälsningar,<br/>
        småbarnspedagogikens servicehandledning<br/>
        </p>
        $securitySv
        $unsubscribeSv
        <hr>
        
        <p>You have one decision from Turku early childhood education and care that you have not replied to. 
        The decision must be accepted or rejected at evaka.turku.fi within two weeks from the date you received it.</p>

        <p>This message cannot be replied to.</p>

        <p>
        Best regards,<br/>
        Early childhood education and care service guidance<br/>
        </p>
        $securityEn
        $unsubscribeEn
        """
            .trimIndent()

    fun getPendingDecisionEmailText(): String =
        """
        Sinulla on vastaamaton päätös Turun varhaiskasvatukselta. 
        Päätös tulee hyväksyä tai hylätä kahden viikon sisällä sen saapumisesta osoitteessa evaka.turku.fi.

        Tähän viestiin ei voi vastata.

        Ystävällisesti,
        Varhaiskasvatuksen palveluohjaus

        -----

        Du har ett obesvarat beslut från Åbo stads småbarnspedagogik. 
        Godkänn eller avslå beslutet inom två veckor från mottagandedatum på adressen evaka.turku.fi.

        Svara inte på detta meddelande.

        Med vänliga hälsningar,
        småbarnspedagogikens servicehandledning

        -----

        You have one decision from Turku early childhood education and care that you have not replied to. 
        The decision must be accepted or rejected at evaka.turku.fi within two weeks from the date you received it.

        This message cannot be replied to.

        Best regards,
        Early childhood education and care service guidance

        """
            .trimIndent()

    override fun clubApplicationReceived(language: Language): EmailContent =
        EmailContent(
            subjectForClubApplicationReceivedEmail,
            getClubApplicationReceivedEmailText(),
            getClubApplicationReceivedEmailHtml(),
        )

    fun getClubApplicationReceivedEmailHtml(): String =
        """
        <p>Hei!</p>

        <p>Olemme vastaanottaneet lapsenne hakemuksen avoimeen varhaiskasvatukseen. 
        Pyydämme teitä olemaan yhteydessä suoraan avoimen yksikön lähijohtajaan ja tiedustelemaan vapaata avoimen varhaiskasvatuksen paikkaa.</p>

        <p>Hakemuksia käsitellään pääsääntöisesti vastaanottopäivämäärän mukaan.</p>

        <p>Tähän viestiin ei voi vastata.</p>

        <p>Päätös on nähtävissä ja hyväksyttävissä/hylättävissä evaka.turku.fi.</p>

        <p>
        Ystävällisesti, <br/>
        Varhaiskasvatuksen palveluohjaus<br/> 
        </p>

        <hr>

        <p>Hej!</p>

        <p>Vi har mottagit ditt barns ansökan till den öppna småbarnspedagogiken. 
        Vänligen kontakta chefen vid enheten för öppen småbarnspedagogik direkt och fråga efter en plats.</p>

        <p>Ansökningarna behandlas i ankomstordning.</p>

        <p>Svara inte på detta meddelande.</p>

        <p>Du kan läsa och godkänna/avvisa beslutet på adressen evaka.turku.fi.</p>

        <p>
        Med vänliga hälsningar,<br/> 
        småbarnspedagogikens servicehandledning<br/> 
        </p>

        <hr>

        <p>Hi!</p>

        <p>We have received your child’s application for open early childhood education and care. 
        Please contact directly the open unit’s regional manager to enquire about a place in open early childhood education and care.</p>

        <p>Applications are processed as a rule in the order they arrive.</p>

        <p>This message cannot be replied to.</p>

        <p>You can view and then either accept or reject the decision at evaka.turku.fi.</p>

        <p> 
        Best regards,<br/>
        Early childhood education and care service guidance<br/>
        </p>
        """
            .trimIndent()

    fun getClubApplicationReceivedEmailText(): String =
        """
        Hei! 

        Olemme vastaanottaneet lapsenne hakemuksen avoimeen varhaiskasvatukseen. 
        Pyydämme teitä olemaan yhteydessä suoraan avoimen yksikön lähijohtajaan ja tiedustelemaan vapaata avoimen varhaiskasvatuksen paikkaa. 

        Hakemuksia käsitellään pääsääntöisesti vastaanottopäivämäärän mukaan.

        Tähän viestiin ei voi vastata.
         
        Päätös on nähtävissä ja hyväksyttävissä/hylättävissä evaka.turku.fi.

        Ystävällisesti, 
        Varhaiskasvatuksen palveluohjaus 

        -----

        Hej!

        Vi har mottagit ditt barns ansökan till den öppna småbarnspedagogiken. 
        Vänligen kontakta chefen vid enheten för öppen småbarnspedagogik direkt och fråga efter en plats.

        Ansökningarna behandlas i ankomstordning.

        Svara inte på detta meddelande.

        Du kan läsa och godkänna/avvisa beslutet på adressen evaka.turku.fi.

        Med vänliga hälsningar,
        småbarnspedagogikens servicehandledning

        -----

        Hi!

        We have received your child’s application for open early childhood education and care. 
        Please contact directly the open unit’s regional manager to enquire about a place in open early childhood education and care.

        Applications are processed as a rule in the order they arrive.

        This message cannot be replied to.

        You can view and then either accept or reject the decision at evaka.turku.fi.

        Best regards,
        Early childhood education and care service guidance
        """
            .trimIndent()

    override fun daycareApplicationReceived(language: Language): EmailContent =
        EmailContent(
            subjectForDaycareApplicationReceivedEmail,
            getDaycareApplicationReceivedEmailText(),
            getDaycareApplicationReceivedEmailHtml(),
        )

    fun getDaycareApplicationReceivedEmailHtml(): String =
        """
        <p>Hei!</p>

        <p>Lapsenne varhaiskasvatushakemus on vastaanotettu. Hakemuksen tehnyt huoltaja voi muokata hakemusta osoitteessa evaka.turku.fi siihen saakka, kunnes varhaiskasvatuksen palveluohjaus ottaa sen käsittelyyn. 
        Varhaiskasvatuspaikan hakuaika on neljä kuukautta. Mikäli kyseessä on vanhemman äkillinen työllistyminen tai opintojen alkaminen, järjestelyaika on kaksi viikkoa. 
        Toimittakaa tällöin työ- tai opiskelutodistus hakemuksen liitteeksi. Kahden viikon järjestelyaika alkaa todistuksen saapumispäivämäärästä. Vuorohoidon palveluita järjestetään vanhempien vuorotyön perusteella, jolloin pyydämme työvuoroista todistuksen.</p>

        <p><b>Mikäli lapsellenne järjestyy varhaiskasvatuspaikka jostakin hakemuksessa toivomastanne kunnallisesta varhaiskasvatuspaikasta</b>, ilmoitamme teille paikan noin kuukautta ennen varhaiskasvatuksen toivottua aloitusajankohtaa. 
        Huomioittehan, että paikka voi järjestyä muualta kuin ensisijaisista hakutoiveista.</p>

        <p><b>Mikäli valitsitte ensimmäiseksi hakutoiveeksi yksityisen päiväkodin tai yksityisen perhepäivähoitajan</b>, olkaa suoraan yhteydessä kyseiseen palveluntuottajaan varmistaaksenne varhaiskasvatuspaikan saamisen. 
        Mikäli toivomanne palveluntuottaja ei pysty tarjoamaan hoitopaikkaa, pyydämme teitä olemaan yhteydessä varhaiskasvatuksen palveluohjaukseen.</p> 

        <p><b>Siirtohakemukset</b> (lapsella on jo varhaiskasvatuspaikka Turun kaupungin varhaiskasvatusyksikössä) käsitellään pääsääntöisesti hakemuksen saapumispäivämäärän mukaan.</p>

        <p><b>Mikäli ilmoititte hakemuksessa lapsenne tuen tarpeesta</b>, varhaiskasvatuksen erityisopettaja on teihin yhteydessä, jotta lapsen tuen tarpeet voidaan ottaa huomioon paikkaa osoitettaessa.</p>

        <p>Päätös on nähtävissä ja hyväksyttävissä/hylättävissä evaka.turku.fi</p>

        <p>Hakemuksen liitteet voi lisätä suoraan sähköiselle hakemukselle tai toimittaa postitse osoitteeseen Varhaiskasvatuksen palveluohjaus, PL 355, 20101 Turku tai toimittamalla Kauppatorin Monitoriin, Varhaiskasvatuksen palveluohjaus, Aurakatu 8.</p> 

        <p>
        Ystävällisesti, <br/>
        Varhaiskasvatuksen palveluohjaus<br/> 
        </p>

        <p>Tämä on automaattinen viesti, joka kertoo lomakkeen tallennuksesta. Viestiin ei voi vastata reply-/ vastaa-toiminnolla.</p>

        <hr>

        <p>Hej!</p>

        <p>Vi har mottagit ditt barns ansökan till småbarnspedagogik. 
        Vårdnadshavaren som skickade in ansökan kan göra ändringar i ansökan på adressen evaka.turku.fi fram till det att den behandlas av servicehandledningen. 
        Ansökningstiden för platser inom småbarnspedagogiken är fyra månader. Om ansökan har gjorts på grund av ny arbetsplats eller studieplats är handläggningstiden två veckor. 
        Då ska vårdnadshavaren lämna in ett arbets- eller studieintyg. Handläggningstiden på två veckor börjar från och med dagen då intyget tas emot. 
        Om ansökan handlar om skiftvård ber vi att vårdnadshavarna skickar in ett intyg om skiftarbete.</p>

        <p><b>Om vi kan ordna en dagvårdsplats på något av de kommunala enheterna för småbarnspedagogik som ni valde i er ansökan</b>, kommer vi att meddela er om platsen ungefär en månad före önskat startdatum. 
        Vänligen observera att platsen ni tilldelas inte nödvändigtvis är på någon av de önskade enheterna.</p>

        <p><b>Om ditt förstahandsval var ett privat daghem eller en privat familjedagvårdare</b>, vänligen kontakta dem direkt för att säkra barnets plats där. 
        Om det inte gick att erbjuda en plats på någon av era önskade platser, vänligen kontakta småbarnspedagogikens servicehandledning.</p>

        <p><b>Överföringsansökning</b> (barnet har redan en plats på en enhet för småbarnspedagogik i Åbo) handläggs i huvudsak i ankomstordning.</p>

        <p><b>Om du har angett att barnet behöver särskilt stöd</b> kommer en speciallärare inom småbarnspedagogiken att kontakta er för att säkerställa att barnets behov kan beaktas när barnet tilldelas en plats.</p>

        <p>Du kan läsa och godkänna/avvisa beslutet på adressen evaka.turku.fi</p>

        <p>Du kan bifoga bilagorna till den elektroniska ansökan, skicka dem per post till adressen Småbarnspedagogikens servicehandledning, PB 355, 20101 Åbo eller lämna in dem till Monitori vid Åbo salutorg, Småbarnspedagogikens servicehandledning, Auragatan 8.</p>

        <p>
        Med vänliga hälsningar,<br/> 
        småbarnspedagogikens servicehandledning<br/> 
        </p>

        <p>Detta är ett automatiskt meddelande som informerar dig om att formuläret har sparats. Du kan inte svara på meddelandet med svara-funktionen.</p>

        <hr>

        <p>Hello!</p>

        <p>We have received your child’s early childhood education and care application. 
        The parent or guardian who sent the application can make changes to the application at evaka.turku.fi until the early childhood education and care service guidance begins to process it. 
        The application period for early childhood education and care is four months. 
        If the parent needs to start work or studies on short notice, the minimum period of processing is two weeks. If this is the case, please attach the relevant documentation for work or study. 
        The two-week period begins from the date we have received such documentation. 
        Childcare for children with parents doing shift work are planned on the basis of work rosters, which we will need to obtain from you.</p>

        <p><b>If early childhood education and care can be provided to your child in some other municipal provider than you applied for</b>, we will inform you about this about two months before your desired starting date. 
        Please note that we may find a place for your child somewhere else than your primary choices.</p>

        <p><b>If your first choice was a private daycare provider or private family daycarer</b>, please contact them directly. 
        If they are unable to care for your child, please contact the early childhood education and care service guidance.</p>

        <p><b>As a rule, transfer applications</b> (meaning that the City of Turku’s early childhood education and care unit already provides early childhood education and care to the child) are processed in the order they arrive.</p>

        <p><b>If you said in your application that your child requires support</b>, a special needs teacher in early childhood education and care will contact you to ensure that the child’s needs are taken into account when selecting a place.</p>

        <p>You can view and either accept or reject the decision at evaka.turku.fi.</p>

        <p>Application attachments can be added directly to your online application or posted to Varhaiskasvatuksen palveluohjaus, PL 355, 20101 Turku, or taking them in person to Kauppatori Monitori, Varhaiskasvatuksen palveluohjaus, Aurakatu 8.</p>

        <p>
        Best regards,<br/>
        Early childhood education and care service guidance<br/>
        </p>
         
        <p>This is an automatic message on how the form is stored. You cannot reply to this message.</p>
        """
            .trimIndent()

    fun getDaycareApplicationReceivedEmailText(): String =
        """
        Hei! 

        Lapsenne varhaiskasvatushakemus on vastaanotettu. Hakemuksen tehnyt huoltaja voi muokata hakemusta osoitteessa evaka.turku.fi siihen saakka, kunnes varhaiskasvatuksen palveluohjaus ottaa sen käsittelyyn. Varhaiskasvatuspaikan hakuaika on neljä kuukautta. 
        Mikäli kyseessä on vanhemman äkillinen työllistyminen tai opintojen alkaminen, järjestelyaika on kaksi viikkoa. 
        Toimittakaa tällöin työ- tai opiskelutodistus hakemuksen liitteeksi. Kahden viikon järjestelyaika alkaa todistuksen saapumispäivämäärästä. Vuorohoidon palveluita järjestetään vanhempien vuorotyön perusteella, jolloin pyydämme työvuoroista todistuksen. 

        Mikäli lapsellenne järjestyy varhaiskasvatuspaikka jostakin hakemuksessa toivomastanne kunnallisesta varhaiskasvatuspaikasta, ilmoitamme teille paikan noin kuukautta ennen varhaiskasvatuksen toivottua aloitusajankohtaa. 
        Huomioittehan, että paikka voi järjestyä muualta kuin ensisijaisista hakutoiveista.  

        Mikäli valitsitte ensimmäiseksi hakutoiveeksi yksityisen päiväkodin tai yksityisen perhepäivähoitajan, olkaa suoraan yhteydessä kyseiseen palveluntuottajaan varmistaaksenne varhaiskasvatuspaikan saamisen. 
        Mikäli toivomanne palveluntuottaja ei pysty tarjoamaan hoitopaikkaa, pyydämme teitä olemaan yhteydessä varhaiskasvatuksen palveluohjaukseen. 

        Siirtohakemukset (lapsella on jo varhaiskasvatuspaikka Turun kaupungin varhaiskasvatusyksikössä) käsitellään pääsääntöisesti hakemuksen saapumispäivämäärän mukaan.

        Mikäli ilmoititte hakemuksessa lapsenne tuen tarpeesta, varhaiskasvatuksen erityisopettaja on teihin yhteydessä, jotta lapsen tuen tarpeet voidaan ottaa huomioon paikkaa osoitettaessa. 

        Päätös on nähtävissä ja hyväksyttävissä/hylättävissä evaka.turku.fi

        Hakemuksen liitteet voi lisätä suoraan sähköiselle hakemukselle tai toimittaa postitse osoitteeseen Varhaiskasvatuksen palveluohjaus, PL 355, 20101 Turku tai toimittamalla Kauppatorin Monitoriin, Varhaiskasvatuksen palveluohjaus, Aurakatu 8.

        Ystävällisesti, 
        Varhaiskasvatuksen palveluohjaus 

        Tämä on automaattinen viesti, joka kertoo lomakkeen tallennuksesta. Viestiin ei voi vastata reply-/ vastaa-toiminnolla.

        -----

        Hej!

        Vi har mottagit ditt barns ansökan till småbarnspedagogik. Vårdnadshavaren som skickade in ansökan kan göra ändringar i ansökan på adressen evaka.turku.fi fram till det att den behandlas av servicehandledningen. 
        Ansökningstiden för platser inom småbarnspedagogiken är fyra månader. Om ansökan har gjorts på grund av ny arbetsplats eller studieplats är handläggningstiden två veckor. 
        Då ska vårdnadshavaren lämna in ett arbets- eller studieintyg. Handläggningstiden på två veckor börjar från och med dagen då intyget tas emot. Om ansökan handlar om skiftvård ber vi att vårdnadshavarna skickar in ett intyg om skiftarbete.

        Om vi kan ordna en dagvårdsplats på något av de kommunala enheterna för småbarnspedagogik som ni valde i er ansökan, kommer vi att meddela er om platsen ungefär en månad före önskat startdatum. 
        Vänligen observera att platsen ni tilldelas inte nödvändigtvis är på någon av de önskade enheterna.

        Om ditt förstahandsval var ett privat daghem eller en privat familjedagvårdare, vänligen kontakta dem direkt för att säkra barnets plats där. Om det inte gick att erbjuda en plats på någon av era önskade platser, vänligen kontakta småbarnspedagogikens servicehandledning.

        Överföringsansökning (barnet har redan en plats på en enhet för småbarnspedagogik i Åbo) handläggs i huvudsak i ankomstordning.

        Om du har angett att barnet behöver särskilt stöd kommer en speciallärare inom småbarnspedagogiken att kontakta er för att säkerställa att barnets behov kan beaktas när barnet tilldelas en plats.

        Du kan läsa och godkänna/avvisa beslutet på adressen evaka.turku.fi

        Du kan bifoga bilagorna till den elektroniska ansökan, skicka dem per post till adressen Småbarnspedagogikens servicehandledning, PB 355, 20101 Åbo eller lämna in dem till Monitori vid Åbo salutorg, Småbarnspedagogikens servicehandledning, Auragatan 8.

        Med vänliga hälsningar,
        småbarnspedagogikens servicehandledning

        Detta är ett automatiskt meddelande som informerar dig om att formuläret har sparats. Du kan inte svara på meddelandet med svara-funktionen.

        -----

        Hello!

        We have received your child’s early childhood education and care application. The parent or guardian who sent the application can make changes to the application at evaka.turku.fi until the early childhood education and care service guidance begins to process it. The application period for early childhood education and care is four months. If the parent needs to start work or studies on short notice, the minimum period of processing is two weeks. If this is the case, please attach the relevant documentation for work or study. The two-week period begins from the date we have received such documentation. Childcare for children with parents doing shift work are planned on the basis of work rosters, which we will need to obtain from you.

        If early childhood education and care can be provided to your child in some other municipal provider than you applied for, we will inform you about this about two months before your desired starting date. Please note that we may find a place for your child somewhere else than your primary choices.

        If your first choice was a private daycare provider or private family daycarer, please contact them directly. If they are unable to care for your child, please contact the early childhood education and care service guidance.

        As a rule, transfer applications (meaning that the City of Turku’s early childhood education and care unit already provides early childhood education and care to the child) are processed in the order they arrive.

        If you said in your application that your child requires support, a special needs teacher in early childhood education and care will contact you to ensure that the child’s needs are taken into account when selecting a place.

        You can view and either accept or reject the decision at evaka.turku.fi.

        Application attachments can be added directly to your online application or posted to Varhaiskasvatuksen palveluohjaus, PL 355, 20101 Turku, or taking them in person to Kauppatori Monitori, Varhaiskasvatuksen palveluohjaus, Aurakatu 8.

        Best regards,
        Early childhood education and care service guidance

        This is an automatic message on how the form is stored. You cannot reply to this message.
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
        return EmailContent(
            subject =
                "Läsnäolovarauksia puuttuu / Det finns några närvarobokningar som saknas / There are missing attendance reservations",
            text =
                """
                Läsnäolovarauksia puuttuu $start alkavalta viikolta. Käythän merkitsemässä ne mahdollisimman pian.
                
                -----
                
                Det finns några närvarobokningar som saknas för veckan som börjar $start. Vänligen markera dem så snart som möjligt.
                
                ----
                
                There are missing attendance reservations for the week starting $start. Please mark them as soon as possible.
                """
                    .trimIndent(),
            html =
                """
                <p>Läsnäolovarauksia puuttuu $start alkavalta viikolta. Käythän merkitsemässä ne mahdollisimman pian.</p>
                $securityFi
                $unsubscribeFi
                <hr>
                <p>Det finns några närvarobokningar som saknas för veckan som börjar $start. Vänligen markera dem så snart som möjligt.</p>
                $securitySv
                $unsubscribeSv
                <hr>
                <p>There are missing attendance reservations for week starting $start. Please mark them as soon as possible.</p>
                $securityEn
                $unsubscribeEn
                """
                    .trimIndent(),
        )
    }

    override fun preschoolApplicationReceived(
        language: Language,
        withinApplicationPeriod: Boolean,
    ): EmailContent =
        EmailContent(
            subjectForPreschoolApplicationReceivedEmail,
            getPreschoolApplicationReceivedEmailText(),
            getPreschoolApplicationReceivedEmailHtml(),
        )

    fun getPreschoolApplicationReceivedEmailHtml(): String =
        """
        <p>Hei!</p>

        <p>Olemme vastaanottaneet lapsenne hakemuksen esiopetukseen. Hakemuksen tehnyt huoltaja voi muokata hakemusta osoitteessa evaka.turku.fi siihen saakka, kunnes varhaiskasvatuksen palveluohjaus ottaa sen käsittelyyn. Esiopetuspäätös on nähtävissä evaka.turku.fi.</p>

        <p>Mikäli ilmoititte hakemuksessa lapsenne tuen tarpeesta, varhaiskasvatuksen erityisopettaja on teihin yhteydessä, jotta lapsen tuen tarpeet voidaan ottaa huomioon paikkaa osoitettaessa.</p>

        <p>ESIOPETUKSEN TÄYDENTÄVÄ VARHAISKASVATUS</p>

        <p>Mikäli hait esiopetukseen täydentävää varhaiskasvatusta, otathan huomioon:</p>

        <p><ul>
        <li>Vuorohoidon palveluita järjestetään vanhempien vuorotyön tai iltaisin ja/tai viikonloppuisin tapahtuvan opiskelun perusteella.</li>
        <li>Mikäli lapsellenne järjestyy varhaiskasvatuspaikka jostakin hakemuksessa toivomastanne kunnallisesta varhaiskasvatuspaikasta, ilmoitamme teille paikan viimeistään kaksi viikkoa ennen varhaiskasvatuksen toivottua aloitusajankohtaa.</li>
        <li>Mikäli valitsitte ensimmäiseksi hakutoiveeksi yksityisen päiväkodin, olkaa suoraan yhteydessä kyseiseen yksikköön varmistaaksenne varhaiskasvatuspaikan saamisen. Mikäli toivomanne palveluntuottaja ei pysty tarjoamaan hoitopaikkaa, pyydämme teitä olemaan yhteydessä varhaiskasvatuksen palveluohjaukseen.</li>
        <li>Siirtohakemukset (lapsella on jo varhaiskasvatuspaikka Turun kaupungin varhaiskasvatusyksikössä) käsitellään pääsääntöisesti hakemuksen saapumispäivämäärän mukaan.</li>
        </ul></p>

        <p>Varhaiskasvatuspäätös on nähtävissä ja hyväksyttävissä/hylättävissä evaka.turku.fi</p>

        <p>Hakemuksen liitteet voi lisätä suoraan sähköiselle hakemukselle tai toimittaa postitse osoitteeseen Varhaiskasvatuksen palveluohjaus, PL 355, 20101 Turku tai toimittamalla Kauppatorin Monitoriin, Varhaiskasvatuksen palveluohjaus, Aurakatu 8.</p>

        <p>
        Ystävällisesti,<br/>
        Varhaiskasvatuksen palveluohjaus<br/>
        </p>

        <hr>

        <p>Hej!</p>

        <p>Vi har mottagit ditt barns ansökan till förskoleundervisning. Vårdnadshavaren som skickade in ansökan kan göra ändringar i ansökan på adressen evaka.turku.fi fram till det att den behandlas av servicehandledningen. Du kan läsa förskolebeslutet på adressen evaka.turku.fi.</p>

        <p>Om du har angett att barnet behöver särskilt stöd kommer en speciallärare inom småbarnspedagogiken att kontakta er för att säkerställa att barnets behov kan beaktas när barnet tilldelas en plats.</p>

        <p>KOMPLETTERANDE SMÅBARNSPEDAGOGIK FÖR BARN I FÖRSKOLEÅLDERN</p>

        <p>Om du har ansökt om kompletterande småbarnspedagogik för ett barn i förskoleåldern, vänligen ta hänsyn till följande:</p>

        <p><ul>
        <li>Skiftvården ordnas utifrån föräldrars skiftarbete eller studier på kvällar och/eller helger.</li>
        <li>Om det ordnas en förskoleplats för ditt barn från någon av de kommunala förskoleplatser som du begärt i ansökan, meddelar vi dig platsen senast två veckor innan önskad starttid för förskoleutbildningen.</li>
        <li>Om du valde ett privat daghem som din första ansökan, kontakta den aktuella enheten direkt för att säkerställa att du får en förskoleplats. Om den tjänsteleverantör du önskar inte kan erbjuda en vårdplats ber vi dig att kontakta förskoletjänstens vägledning.</li>
        <li>Ansökningar om överlåtelse (barnet har redan en förskoleplats vid Åbo stads förskoleenhet) behandlas i allmänhet enligt ansökningsdagen.</li>
        </ul></p>

        <p>Du kan läsa och godkänna/avvisa småbarnspedagogikbeslutet på adressen evaka.turku.fi</p>

        <p>Du kan bifoga bilagorna till den elektroniska ansökan, skicka dem per post till adressen Småbarnspedagogikens servicehandledning, PB 355, 20101 Åbo eller lämna in dem till Monitori vid Åbo salutorg, Småbarnspedagogikens servicehandledning, Auragatan 8.</p>

        <p>
        Med vänliga hälsningar,<br/>
        småbarnspedagogikens servicehandledning<br/>
        </p>

        <hr>

        <p>Hello!</p>

        <p>We have received your child’s application for pre-primary education. The parent or guardian who sent the application can make changes to the application at evaka.turku.fi until the early childhood education and care service guidance begins to process it. You can view the preschool decision at evaka.turku.fi.</p>

        <p>If you said in your application that your child requires support, a special needs teacher in early childhood education and care will contact you to ensure that the child’s needs are taken into account when selecting a place.</p>

        <p>EARLY CHILDHOOD EDUCATION AND CARE COMPLEMENTING PRE-PRIMARY EDUCATION</p>

        <p>If you applied for early childhood education and care to complement pre-primary education, please take note of the following:</p>

        <p><ul>
        <li>Shift care services are arranged based on parents' shift work or studying in the evenings and/or weekends.</li>
        <li>If an early childhood education place is arranged for your child from one of the municipal early childhood education places you requested in the application, we will inform you of the place no later than two weeks before the desired start time of early childhood education.</li>
        <li>If you chose a private daycare center as your first application, please contact the unit in question directly to ensure that you get an early childhood education place. If the service provider you want is not able to offer a place of care, we ask you to contact the early childhood education service guidance.</li>
        <li>Transfer applications (the child already has an early childhood education place in the early childhood education unit of the city of Turku) are generally processed according to the date of arrival of the application.</li>
        </ul></p>

        <p>You can view and either accept or reject the early childhood education decision at evaka.turku.fi.</p>

        <p>Application attachments can be added directly to your online application or posted to Varhaiskasvatuksen palveluohjaus, PL 355, 20101 Turku, or taking them in person to Kauppatori Monitori, Varhaiskasvatuksen palveluohjaus, Aurakatu 8.</p>

        <p>
        Best regards,<br/>
        Early childhood education and care service guidance<br/>
        </p>
        """
            .trimIndent()

    fun getPreschoolApplicationReceivedEmailText(): String =
        """
        Hei! 

        Olemme vastaanottaneet lapsenne hakemuksen esiopetukseen. Hakemuksen tehnyt huoltaja voi muokata hakemusta osoitteessa evaka.turku.fi siihen saakka, kunnes varhaiskasvatuksen palveluohjaus ottaa sen käsittelyyn. Päätös on nähtävissä ja hyväksyttävissä/hylättävissä evaka.turku.fi.

        Mikäli ilmoititte hakemuksessa lapsenne tuen tarpeesta, varhaiskasvatuksen erityisopettaja on teihin yhteydessä, jotta lapsen tuen tarpeet voidaan ottaa huomioon paikkaa osoitettaessa.

        ESIOPETUKSEN TÄYDENTÄVÄ VARHAISKASVATUS 

        Mikäli hait esiopetukseen täydentävää varhaiskasvatusta, otathan huomioon: 
        •	Vuorohoidon palveluita järjestetään vanhempien vuorotyön tai iltaisin ja/tai viikonloppuisin tapahtuvan opiskelun perusteella. 
        •	Mikäli lapsellenne järjestyy varhaiskasvatuspaikka jostakin hakemuksessa toivomastanne kunnallisesta varhaiskasvatuspaikasta, ilmoitamme teille paikan viimeistään kaksi viikkoa ennen varhaiskasvatuksen toivottua aloitusajankohtaa.
        •	Mikäli valitsitte ensimmäiseksi hakutoiveeksi yksityisen päiväkodin, olkaa suoraan yhteydessä kyseiseen yksikköön varmistaaksenne varhaiskasvatuspaikan saamisen. Mikäli toivomanne palveluntuottaja ei pysty tarjoamaan hoitopaikkaa, pyydämme teitä olemaan yhteydessä varhaiskasvatuksen palveluohjaukseen. 
        •	Siirtohakemukset (lapsella on jo varhaiskasvatuspaikka Turun kaupungin varhaiskasvatusyksikössä) käsitellään pääsääntöisesti hakemuksen saapumispäivämäärän mukaan.
         
        Päätös on nähtävissä ja hyväksyttävissä/hylättävissä evaka.turku.fi
         
        Hakemuksen liitteet voi lisätä suoraan sähköiselle hakemukselle tai toimittaa postitse osoitteeseen Varhaiskasvatuksen palveluohjaus, PL 355, 20101 Turku tai toimittamalla Kauppatorin Monitoriin, Varhaiskasvatuksen palveluohjaus, Aurakatu 8.

        Ystävällisesti, 
        Varhaiskasvatuksen palveluohjaus

        -----

        Hej!

        Vi har mottagit ditt barns ansökan till förskoleundervisning. Vårdnadshavaren som skickade in ansökan kan göra ändringar i ansökan på adressen evaka.turku.fi fram till det att den behandlas av servicehandledningen. Du kan läsa och godkänna/avvisa beslutet på adressen evaka.turku.fi.

        Om du har angett att barnet behöver särskilt stöd kommer en speciallärare inom småbarnspedagogiken att kontakta er för att säkerställa att barnets behov kan beaktas när barnet tilldelas en plats.

        KOMPLETTERANDE SMÅBARNSPEDAGOGIK FÖR BARN I FÖRSKOLEÅLDERN

        Om du har ansökt om kompletterande småbarnspedagogik för ett barn i förskoleåldern, vänligen ta hänsyn till följande:
        •   Skiftvården ordnas utifrån föräldrars skiftarbete eller studier på kvällar och/eller helger.
        •   Om det ordnas en förskoleplats för ditt barn från någon av de kommunala förskoleplatser som du begärt i ansökan, meddelar vi dig platsen senast två veckor innan önskad starttid för förskoleutbildningen.
        •   Om du valde ett privat daghem som din första ansökan, kontakta den aktuella enheten direkt för att säkerställa att du får en förskoleplats. Om den tjänsteleverantör du önskar inte kan erbjuda en vårdplats ber vi dig att kontakta förskoletjänstens vägledning.
        •   Ansökningar om överlåtelse (barnet har redan en förskoleplats vid Åbo stads förskoleenhet) behandlas i allmänhet enligt ansökningsdagen.

        Du kan läsa och godkänna/avvisa beslutet på adressen evaka.turku.fi

        Du kan bifoga bilagorna till den elektroniska ansökan, skicka dem per post till adressen Småbarnspedagogikens servicehandledning, PB 355, 20101 Åbo eller lämna in dem till Monitori vid Åbo salutorg, Småbarnspedagogikens servicehandledning, Auragatan 8.

        Med vänliga hälsningar,
        småbarnspedagogikens servicehandledning

        -----

        Hello!

        We have received your child’s application for pre-primary education. The parent or guardian who sent the application can make changes to the application at evaka.turku.fi until the early childhood education and care service guidance begins to process it. You can view and either accept or reject the decision at evaka.turku.fi.

        If you said in your application that your child requires support, a special needs teacher in early childhood education and care will contact you to ensure that the child’s needs are taken into account when selecting a place.

        EARLY CHILDHOOD EDUCATION AND CARE COMPLEMENTING PRE-PRIMARY EDUCATION

        If you applied for early childhood education and care to complement pre-primary education, please take note of the following:
        •   Shift care services are arranged based on parents' shift work or studying in the evenings and/or weekends.
        •   If an early childhood education place is arranged for your child from one of the municipal early childhood education places you requested in the application, we will inform you of the place no later than two weeks before the desired start time of early childhood education.
        •   If you chose a private daycare center as your first application, please contact the unit in question directly to ensure that you get an early childhood education place. If the service provider you want is not able to offer a place of care, we ask you to contact the early childhood education service guidance.
        •   Transfer applications (the child already has an early childhood education place in the early childhood education unit of the city of Turku) are generally processed according to the date of arrival of the application.

        You can view and either accept or reject the decision at evaka.turku.fi.

        Application attachments can be added directly to your online application or posted to Varhaiskasvatuksen palveluohjaus, PL 355, 20101 Turku, or taking them in person to Kauppatori Monitori, Varhaiskasvatuksen palveluohjaus, Aurakatu 8.

        Best regards,
        Early childhood education and care service guidance
        """
            .trimIndent()

    override fun pedagogicalDocumentNotification(
        language: Language,
        childId: ChildId,
    ): EmailContent =
        EmailContent(
            subject =
                "Uusi pedagoginen dokumentti eVakassa / Nytt pedagogiskt dokument i eVaka / New pedagogical document in eVaka",
            text =
                """
                Sinulle on saapunut uusi pedagoginen dokumentti eVakaan. Lue dokumentti eVakassa.

                Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.

                -----

                Du har fått ett nytt pedagogiskt dokument i eVaka. Läs dokumentet i eVaka.

                Detta besked skickas automatiskt av eVaka. Svara inte på detta besked. 

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
                <p>Du har fått ett nytt pedagogiskt dokument i eVaka. Läs dokumentet i eVaka.</p>
                <p>Detta besked skickas automatiskt av eVaka. Svara inte på detta besked.</p>        
                $securitySv
                $unsubscribeSv
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
            subject =
                "Tulotietojen tarkastus- kehotus / Uppmaning att göra en inkomstutredning / Request to review income information",
            text =
                """
                Hyvä asiakkaamme

                Varhaiskasvatuksen asiakasmaksun tai palvelusetelin omavastuuosuuden perusteena olevat tulotiedot tarkistetaan vuosittain.

                Pyydämme toimittamaan tuloselvityksen eVakassa 14 päivän kuluessa tästä ilmoituksesta. eVakassa voitte myös antaa suostumuksen korkeimpaan maksuluokkaan tai tulorekisterin käyttöön.

                Mikäli ette toimita uusia tulotietoja, asiakasmaksu määräytyy korkeimman maksuluokan mukaan. Puuttuvilla tulotiedoilla määrättyä maksua ei korjata takautuvasti.

                Voitte tarvittaessa toimittaa tulotiedot myös postitse osoitteeseen: Turun kaupunki / Kasvatuksen ja opetuksen palveluokokonaisuus, varhaiskasvatuksen asiakasmaksut/ PL 355 20101 Turun kaupunki

                Lisätietoja saatte tarvittaessa: varhaiskasvatusmaksut@turku.fi

                Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.

                -----

                Bästa klient

                Inkomstuppgifterna som ligger till grund för klientavgiften för småbarnspedagogik eller servicesedelns egenandel granskas årligen.

                Vi ber att du skickar en inkomstutredning via eVaka inom 14 dagar från den här anmälan. I eVaka kan du också ge ditt samtycke till den högsta avgiften eller till användning av inkomstregistret.

                Om du inte lämnar in en ny inkomstutredning bestäms din klientavgift enligt den högsta avgiften. En avgift som fastställts på grund av bristfälliga inkomstuppgifter korrigeras inte retroaktivt.

                Du kan vid behov också skicka inkomstutredningen per post till adressen: Åbo stad / Servicehelheten för fostran och undervisning, klientavgifter för småbarnspedagogik / PB 355 20101 Åbo stad

                Mer information: varhaiskasvatusmaksut@turku.fi

                Detta besked skickas automatiskt av eVaka. Svara inte på detta besked. 

                -----

                Dear client

                The income information used for determining the early childhood education fee or the out-of-pocket cost of a service voucher is reviewed every year.

                We ask you to submit your income statement through eVaka within 14 days of this notification. Through eVaka, you can also give your consent to the highest fee or the use of the Incomes Register.

                If you do not provide your latest income information, your client fee will be determined based on the highest fee category. We will not retroactively reimburse you for fees charged in a situation where you have not provided your income information.

                If necessary, you can also send your income information by post to the following address: City of Turku / Education Services, Early childhood education client fees / P.O. Box 355, 20101 City of Turku

                Inquiries: varhaiskasvatusmaksut@turku.fi

                This is an automatic message from the eVaka system. Do not reply to this message.
                """
                    .trimIndent(),
            html =
                """
                <p>Hyvä asiakkaamme</p>
                <p>Varhaiskasvatuksen asiakasmaksun tai palvelusetelin omavastuuosuuden perusteena olevat tulotiedot tarkistetaan vuosittain.</p>
                <p>Pyydämme toimittamaan tuloselvityksen eVakassa 14 päivän kuluessa tästä ilmoituksesta. eVakassa voitte myös antaa suostumuksen korkeimpaan maksuluokkaan tai tulorekisterin käyttöön. </p>
                <p>Mikäli ette toimita uusia tulotietoja, asiakasmaksu määräytyy korkeimman maksuluokan mukaan. Puuttuvilla tulotiedoilla määrättyä maksua ei korjata takautuvasti.</p>
                <p>Voitte tarvittaessa toimittaa tulotiedot myös postitse osoitteeseen: Turun kaupunki / Kasvatuksen ja opetuksen palveluokokonaisuus, varhaiskasvatuksen asiakasmaksut/ PL 355 20101 Turun kaupunki</p>
                <p>Lisätietoja saatte tarvittaessa: varhaiskasvatusmaksut@turku.fi</p>
                <p>Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.</p>
                $securityFi
                $unsubscribeFi
                <hr>
                <p>Bästa klient</p>
                <p>Inkomstuppgifterna som ligger till grund för klientavgiften för småbarnspedagogik eller servicesedelns egenandel granskas årligen.</p>
                <p>Vi ber att du skickar en inkomstutredning via eVaka inom 14 dagar från den här anmälan. I eVaka kan du också ge ditt samtycke till den högsta avgiften eller till användning av inkomstregistret.</p>
                <p>Om du inte lämnar in en ny inkomstutredning bestäms din klientavgift enligt den högsta avgiften. En avgift som fastställts på grund av bristfälliga inkomstuppgifter korrigeras inte retroaktivt.</p>
                <p>Du kan vid behov också skicka inkomstutredningen per post till adressen: Åbo stad / Servicehelheten för fostran och undervisning, klientavgifter för småbarnspedagogik / PB 355 20101 Åbo stad</p>
                <p>Mer information: varhaiskasvatusmaksut@turku.fi</p>
                <p>Detta besked skickas automatiskt av eVaka. Svara inte på detta besked.</p>   
                $securitySv
                $unsubscribeSv
                <hr>
                <p>Dear client</p>
                <p>The income information used for determining the early childhood education fee or the out-of-pocket cost of a service voucher is reviewed every year.</p>
                <p>We ask you to submit your income statement through eVaka within 14 days of this notification. Through eVaka, you can also give your consent to the highest fee or the use of the Incomes Register.</p>
                <p>If you do not provide your latest income information, your client fee will be determined based on the highest fee category. We will not retroactively reimburse you for fees charged in a situation where you have not provided your income information.</p>
                <p>If necessary, you can also send your income information by post to the following address: City of Turku / Education Services, Early childhood education client fees / P.O. Box 355, 20101 City of Turku</p>
                <p>Inquiries: varhaiskasvatusmaksut@turku.fi</p>
                <p>This is an automatic message from the eVaka system. Do not reply to this message.</p>
                $securityEn
                $unsubscribeEn
                """
                    .trimIndent(),
        )

    fun outdatedIncomeNotificationReminder(): EmailContent =
        EmailContent(
            subject =
                "Tulotietojen tarkastus- kehotus / Uppmaning att göra en inkomstutredning / Request to review income information",
            text =
                """
                Hyvä asiakkaamme

                Ette ole vielä toimittaneet uusia tulotietoja. Varhaiskasvatuksen asiakasmaksun tai palvelusetelin omavastuuosuuden perusteena olevat tulotiedot tarkistetaan vuosittain.

                Pyydämme toimittamaan tuloselvityksen eVakassa 7 päivän kuluessa tästä ilmoituksesta. eVakassa voitte myös antaa suostumuksen korkeimpaan maksuluokkaan tai tulorekisterin käyttöön.

                Mikäli ette toimita uusia tulotietoja, asiakasmaksu määräytyy korkeimman maksuluokan mukaan. Puuttuvilla tulotiedoilla määrättyä maksua ei korjata takautuvasti.

                Voitte tarvittaessa toimittaa tulotiedot myös postitse osoitteeseen: Turun kaupunki / Kasvatuksen ja opetuksen palveluokokonaisuus, varhaiskasvatuksen asiakasmaksut/ PL 355 20101 Turun kaupunki    

                Lisätietoja saatte tarvittaessa: varhaiskasvatusmaksut@turku.fi

                Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.

                -----

                Bästa klient

                Du har ännu inte lämnat in en ny inkomstutredning. Inkomstuppgifterna som ligger till grund för klientavgiften för småbarnspedagogik eller servicesedelns egenandel granskas årligen.

                Vi ber att du skickar en inkomstutredning via eVaka inom sju dagar från denna anmälan. I eVaka kan du också ge ditt samtycke till den högsta avgiften eller till användning av inkomstregistret.

                Om du inte lämnar in en ny inkomstutredning bestäms din klientavgift enligt den högsta avgiften. En avgift som fastställts på grund av bristfälliga inkomstuppgifter korrigeras inte retroaktivt.

                Du kan vid behov också skicka inkomstutredningen per post till adressen: Åbo stad / Servicehelheten för fostran och undervisning, klientavgifter för småbarnspedagogik / PB 355 20101 Åbo stad

                Mer information: varhaiskasvatusmaksut@turku.fi

                Detta besked skickas automatiskt av eVaka. Svara inte på detta besked. 

                -----

                Dear client

                You have not yet submitted your latest income information. The income information used for determining the early childhood education fee or the out-of-pocket cost of a service voucher is reviewed every year.

                We ask you to submit your income statement through eVaka within 7 days of this notification. Through eVaka, you can also give your consent to the highest fee or the use of the Incomes Register.

                If you do not provide your latest income information, your client fee will be determined based on the highest fee category. We will not retroactively reimburse you for fees charged in a situation where you have not provided your income information. 

                If necessary, you can also send your income information by post to the following address: City of Turku / Education Services, Early childhood education client fees / P.O. Box 355, 20101 City of Turku

                Inquiries: varhaiskasvatusmaksut@turku.fi

                This is an automatic message from the eVaka system. Do not reply to this message.  
                """
                    .trimIndent(),
            html =
                """
                <p>Hyvä asiakkaamme</p>
                <p>Ette ole vielä toimittaneet uusia tulotietoja. Varhaiskasvatuksen asiakasmaksun tai palvelusetelin omavastuuosuuden perusteena olevat tulotiedot tarkistetaan vuosittain.</p>
                <p>Pyydämme toimittamaan tuloselvityksen eVakassa 7 päivän kuluessa tästä ilmoituksesta. eVakassa voitte myös antaa suostumuksen korkeimpaan maksuluokkaan tai tulorekisterin käyttöön.</p>
                <p>Mikäli ette toimita uusia tulotietoja, asiakasmaksu määräytyy korkeimman maksuluokan mukaan. Puuttuvilla tulotiedoilla määrättyä maksua ei korjata takautuvasti.</p>
                <p>Voitte tarvittaessa toimittaa tulotiedot myös postitse osoitteeseen: Turun kaupunki / Kasvatuksen ja opetuksen palveluokokonaisuus, varhaiskasvatuksen asiakasmaksut/ PL 355 20101 Turun kaupunki</p>
                <p>Lisätietoja saatte tarvittaessa: varhaiskasvatusmaksut@turku.fi</p>
                <p>Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.</p>
                $securityFi
                $unsubscribeFi
                <hr>
                <p>Bästa klient</p>
                <p>Du har ännu inte lämnat in en ny inkomstutredning. Inkomstuppgifterna som ligger till grund för klientavgiften för småbarnspedagogik eller servicesedelns egenandel granskas årligen.</p>
                <p>Vi ber att du skickar en inkomstutredning via eVaka inom sju dagar från denna anmälan. I eVaka kan du också ge ditt samtycke till den högsta avgiften eller till användning av inkomstregistret.</p>
                <p>Om du inte lämnar in en ny inkomstutredning bestäms din klientavgift enligt den högsta avgiften. En avgift som fastställts på grund av bristfälliga inkomstuppgifter korrigeras inte retroaktivt.</p>
                <p>Du kan vid behov också skicka inkomstutredningen per post till adressen: Åbo stad / Servicehelheten för fostran och undervisning, klientavgifter för småbarnspedagogik / PB 355 20101 Åbo stad</p>
                <p>Mer information: varhaiskasvatusmaksut@turku.fi</p>
                <p>Detta besked skickas automatiskt av eVaka. Svara inte på detta besked.</p>
                $securitySv
                $unsubscribeSv
                <hr>
                <p>Dear client</p>
                <p>You have not yet submitted your latest income information. The income information used for determining the early childhood education fee or the out-of-pocket cost of a service voucher is reviewed every year.</p>
                <p>We ask you to submit your income statement through eVaka within 7 days of this notification. Through eVaka, you can also give your consent to the highest fee or the use of the Incomes Register.</p>
                <p>If you do not provide your latest income information, your client fee will be determined based on the highest fee category. We will not retroactively reimburse you for fees charged in a situation where you have not provided your income information.</p> 
                <p>If necessary, you can also send your income information by post to the following address: City of Turku / Education Services, Early childhood education client fees / P.O. Box 355, 20101 City of Turku</p>
                <p>Inquiries: varhaiskasvatusmaksut@turku.fi</p>
                <p>This is an automatic message from the eVaka system. Do not reply to this message.</p>
                $securityEn
                $unsubscribeEn
                """
                    .trimIndent(),
        )

    fun newCustomerIncomeNotification(): EmailContent =
        EmailContent(
            subject =
                "Tulotietojen tarkastuskehotus / Uppmaning att göra en inkomstutredning / Request to review income information",
            text =
                """
                Hyvä asiakkaamme

                Lapsenne on aloittamassa varhaiskasvatuksessa tämän kuukauden aikana. Pyydämme teitä toimittamaan tulotiedot eVaka-järjestelmän kautta tämän kuukauden loppuun mennessä.

                Lisätietoja saatte tarvittaessa: varhaiskasvatusmaksut@turku.fi

                Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.

                -----

                Bästä klient

                Ditt barn börjar småbarnspedagogiken under den här månaden. Vi ber dig att lämna in din inkomstinformation via eVaka-systemet senast i slutet av denna månad.

                Mer information: varhaiskasvatusmaksut@turku.fi

                Detta besked skickas automatiskt av eVaka. Svara inte på detta besked.

                -----

                Dear client

                Your child is starting early childhood education during this month. We ask you to submit your income information via eVaka system by the end of this month.

                Inquiries: varhaiskasvatusmaksut@turku.fi

                This is an automatic message from the eVaka system. Do not reply to this message.
                """
                    .trimIndent(),
            html =
                """
                <p>Hyvä asiakkaamme</p>
                <p>Lapsenne on aloittamassa varhaiskasvatuksessa tämän kuukauden aikana. Pyydämme teitä toimittamaan tulotiedot eVaka-järjestelmän kautta tämän kuukauden loppuun mennessä.</p>
                <p>Lisätietoja saatte tarvittaessa: varhaiskasvatusmaksut@turku.fi</p>
                <p>Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.</p>
                $securityFi
                $unsubscribeFi
                <hr>
                <p>Bästä klient</p>
                <p>Ditt barn börjar småbarnspedagogiken under den här månaden. Vi ber dig att lämna in din inkomstinformation via eVaka-systemet senast i slutet av denna månad.</p>
                <p>Mer information: varhaiskasvatusmaksut@turku.fi</p>
                <p>Detta besked skickas automatiskt av eVaka. Svara inte på detta besked.</p>
                $securitySv
                $unsubscribeSv
                <hr>
                <p>Dear client</p>
                <p>Your child is starting early childhood education during this month. We ask you to submit your income information via eVaka system by the end of this month.</p>
                <p>Inquiries: varhaiskasvatusmaksut@turku.fi</p>
                <p>This is an automatic message from the eVaka system. Do not reply to this message.</p>
                $securityEn
                $unsubscribeEn
                """
                    .trimIndent(),
        )

    fun outdatedIncomeNotificationExpired(): EmailContent =
        EmailContent(
            subject =
                "Tulotietojen tarkastus- kehotus / Uppmaning att göra en inkomstutredning / Request to review income information",
            text =
                """
                Hyvä asiakkaamme

                Seuraava asiakasmaksunne määräytyy korkeimman maksuluokan mukaan, sillä ette ole toimittaneet uusia tulotietoja määräaikaan mennessä.

                Lisätietoja saatte tarvittaessa: varhaiskasvatusmaksut@turku.fi

                Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.

                -----

                Bästä klient

                Din följande klientavgift bestäms enligt den högsta avgiften, eftersom du inte har lämnat in en inkomstutredning inom utsatt tid.

                Mer information: varhaiskasvatusmaksut@turku.fi

                Detta besked skickas automatiskt av eVaka. Svara inte på detta besked. 

                -----

                Dear client

                Your next client fee will be determined based on the highest fee category as you did not provide your latest income information by the deadline.

                Inquiries: varhaiskasvatusmaksut@turku.fi

                This is an automatic message from the eVaka system. Do not reply to this message.  
                """
                    .trimIndent(),
            html =
                """
                <p>Hyvä asiakkaamme</p>
                <p>Seuraava asiakasmaksunne määräytyy korkeimman maksuluokan mukaan, sillä ette ole toimittaneet uusia tulotietoja määräaikaan mennessä.</p>
                <p>Lisätietoja saatte tarvittaessa: varhaiskasvatusmaksut@turku.fi</p>
                <p>Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.</p>
                $securityFi
                $unsubscribeFi
                <hr>
                <p>Bästä klient</p>
                <p>Din följande klientavgift bestäms enligt den högsta avgiften, eftersom du inte har lämnat in en inkomstutredning inom utsatt tid.</p>
                <p>Mer information: varhaiskasvatusmaksut@turku.fi</p>
                <p>Detta besked skickas automatiskt av eVaka. Svara inte på detta besked.</p>
                $securitySv
                $unsubscribeSv
                <hr>
                <p>Dear client</p>
                <p>Your next client fee will be determined based on the highest fee category as you did not provide your latest income information by the deadline.</p>
                <p>Inquiries: varhaiskasvatusmaksut@turku.fi</p>
                <p>This is an automatic message from the eVaka system. Do not reply to this message.</p>
                $securityEn
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
            subject =
                "Uusia kalenteritapahtumia eVakassa / Nya kalenderhändelser i eVaka / New calendar events in eVaka",
            html =
                """
                <p>eVakaan on lisätty uusia kalenteritapahtumia:</p>
                $eventsHtml
                $securityFi
                $unsubscribeFi
                <p>Katso lisää kalenterissa eVakassa.</p>
                <hr>
                <p>Nya kalenderhändelser i eVaka:</p>
                $eventsHtml
                $securitySv
                $unsubscribeSv
                <p>Se mer i kalendern i eVaka.</p>
                <hr>
                <p>New calendar events in eVaka:</p>
                $eventsHtml
                $securityEn
                $unsubscribeEn
                <p>See more in the calendar in eVaka.</p>
                """
                    .trimIndent(),
        )
    }

    override fun missingHolidayReservationsNotification(language: Language): EmailContent =
        EmailContent.fromHtml(
            subject =
                "Loma-ajan ilmoitus sulkeutuu / Semesteranmälan löper ut / Holiday notification period closing",
            html =
                """
<p>Loma-ajan kysely sulkeutuu kahden päivän päästä. Jos lapseltanne/lapsiltanne puuttuu loma-ajan ilmoitus yhdeltä tai useammalta lomapäivältä, teettehän ilmoituksen eVakan kalenterissa mahdollisimman pian eVakassa.</p>
$securityFi
$unsubscribeFi
<hr>
<p>Förfrågan om barnets frånvaro i semestertider stängs om två dagar. Om ditt/dina barn saknar anmälan för en eller flera helgdagar, vänligen gör anmälan i eVaka-kalendern så snart som möjligt i eVaka.</p>
$securitySv
$unsubscribeSv
<hr>
<p>Two days left to submit a holiday notification. If you have not submitted a notification for each day, please submit them through the eVaka calendar as soon as possible in eVaka.</p>
$securityEn
$unsubscribeEn
""",
        )

    override fun financeDecisionNotification(decisionType: FinanceDecisionType): EmailContent {
        val (decisionTypeFi, decisionTypeSv, decisionTypeEn) =
            when (decisionType) {
                FinanceDecisionType.VOUCHER_VALUE_DECISION -> {
                    Triple("arvopäätös", "beslut om servicesedel", "voucher value decision")
                }

                FinanceDecisionType.FEE_DECISION -> {
                    Triple("maksupäätös", "betalningsbeslut", "fee decision")
                }
            }
        return EmailContent.fromHtml(
            subject =
                "Uusi $decisionTypeFi eVakassa / Nytt $decisionTypeSv i eVaka / New $decisionTypeEn in eVaka",
            html =
                """
                <p>Sinulle on saapunut uusi $decisionTypeFi eVakaan.</p>
                <p>Päätös on nähtävissä eVakassa.</p>
                $securityFi
                $unsubscribeFi
                <hr>
                <p>Du har fått ett nytt $decisionTypeSv i eVaka.</p>
                <p>Beslutet finns att se i eVaka.</p>
                $securitySv
                $unsubscribeSv
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
                "Uusi keskusteluaika varattu eVakassa / Ett nytt diskussionsmöte bokat i eVaka / New discussion time reserved in eVaka",
            html =
                """
                <p>Uusi keskusteluaika varattu / Ett nytt diskussionsmöte bokat / New discussion time reserved</p>
                <p>${notificationDetails.calendarEventTime.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}</p>
                <p>${notificationDetails.calendarEventTime.startTime.format(
                    DateTimeFormatter.ofPattern("HH:mm")
                )} - ${notificationDetails.calendarEventTime.endTime.format(DateTimeFormatter.ofPattern("HH:mm"))}</p>
                <hr>
                $securityFi
                $unsubscribeFi
                $securitySv
                $unsubscribeSv
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
            subject =
                "Keskusteluaika peruttu eVakassa / Diskussionsmöte avbokad i eVaka / Discussion time cancelled in eVaka",
            html =
                """
                <p>Varattu keskusteluaika peruttu / Bokad diskussionsmöte avbruten / Reserved discussion time cancelled</p>
                <p>${notificationDetails.calendarEventTime.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}</p>
                <p>${notificationDetails.calendarEventTime.startTime.format(
                    DateTimeFormatter.ofPattern("HH:mm")
                )} - ${notificationDetails.calendarEventTime.endTime.format(DateTimeFormatter.ofPattern("HH:mm"))}</p>
                <hr>
                $securityFi
                $unsubscribeFi
                $securitySv
                $unsubscribeSv
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
                "Varaa keskusteluaika varhaiskasvatukseen / Boka tid till diskussionsmöte / " +
                    "Reserve a discussion time for early childhood education",
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
                <p>Diskussionsmöte kan bokas via eVaka kalender</p>
                $securitySv
                $unsubscribeSv
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
            subject = "Uusi päätös eVakassa / Nytt beslut i eVaka / New decision in eVaka",
            html =
                """
<p>Sinulle on saapunut uusi päätös eVakaan.</p>
<p>Päätös on nähtävissä eVakassa.</p>
$securityFi
$unsubscribeFi
<hr>
<p>Du har fått ett nytt beslut i eVaka.</p>
<p>Beslutet finns att se i eVaka.</p>
$securitySv
$unsubscribeSv
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
                    "Din ansökan om ändring av servicebehovet har behandlats. / " +
                    "Your application for a change in the service need has been processed",
            html =
                if (accepted) {
                    """
                    <p>Ehdottamasi palveluntarve on hyväksytty $start alkaen.</p>
                    $securityFi
                    $unsubscribeFi
                    <p>Det tjänstebehov som ni föreslår har godkänts från och med den $start.</p>
                    $securitySv
                    $unsubscribeSv
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
                    <p>Din föreslagna serviceförfrågan har avvisats, läs mer om avvisandet i eVaka.</p>
                    $securitySv
                    $unsubscribeSv
                    <p>The service request you proposed has been rejected.</p>
                    $securityEn
                    $unsubscribeEn
                    """
                        .trimIndent()
                },
        )
    }

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
                    "Esiopetuksen poissaolohakemus hyväksytty / Ansökan om frånvaro från förskoleundervisningen har " +
                        "godkänts / The pre-primary education absence request has been approved"
                } else {
                    "Esiopetuksen poissaolohakemus hylätty / Ansökan om frånvaro från förskoleundervisningen har avslagits /" +
                        " The pre-primary education absence request has been denied"
                },
            html =
                if (accepted) {
                    """
                    <p>Lapsesi esiopetuksen poissaolohakemus on hyväksytty ja poissaolot on merkitty eVakaan. Lue lisää eVakasta.</p>
                    <p>Ditt barns ansökan om frånvaro från förskoleundervisningen har godkänts och frånvaron har markerats i eVaka. Läs mer om eVaka.</p>
                    <p>Your child's application for absence from pre-primary education has been approved, and the absence has been 
                    recorded in eVaka. Read more about eVaka.</p>
                    """
                        .trimIndent()
                } else {
                    """
                    <p>Lapsesi esiopetuksen poissaolohakemus on hylätty. Lue lisää eVakasta.</p>
                    <p>Ansökan om frånvaro från förskoleundervisningen för ditt barn  har avslagits. Läs mer om eVaka.</p>
                    <p>Your child's application for absence from pre-primary educationhas been declined. Read more about eVaka.</p>
                    """
                        .trimIndent()
                },
        )
    }

    override fun confirmationCode(confirmationCode: HtmlSafe<String>): EmailContent =
        EmailContent.fromHtml(
            subject =
                "eVaka-vahvistuskoodi / eVakas verifieringskod innehåll / eVaka confirmation code",
            html =
                """
<p>eVakasta on lähetetty tämä vahvistuskoodi tietojesi muokkaamista varten. Syötä oheinen vahvistuskoodi pyydettyyn kenttään eVakassa.</p>
<hr>
<p>Den här bekräftelsekoden har skickats från eVaka för redigering av dina uppgifter. Vänligen ange den bifogade bekräftelsekoden i det begärda fältet i eVaka.</p>
<hr>
<p>This confirmation code has been sent from eVaka for editing your information. Enter the provided confirmation code in the requested field in eVaka.</p>
<hr>
<p>Vahvistuskoodi / bekräftelsekod / confirmation code: <strong>$confirmationCode</strong></p>
""",
        )

    override fun passwordChanged(): EmailContent =
        EmailContent.fromHtml(
            subject =
                "eVaka-salasanasi on vaihdettu / Ditt eVaka lösenord har ändrats / Your eVaka password has been changed",
            html =
                """<p>eVaka-salasanasi on vaihdettu.</p>
<p>Jos vaihdoit salasanasi itse, voit jättää tämän viestin huomiotta. Muussa tapauksessa kirjaudu eVakaan vahvalla tunnistautumisella (Kirjaudu Suomi.fi:ssä) ja vaihda salasanasi.</p>
<hr>
<p>Din eVaka lösenord har ändrats.</p>
<p>Om du har ändrat ditt lösenord själv kan du ignorera det här meddelandet. Om inte, logga in i eVaka med stark identifiering (Logga in via Suomi.fi) och ändra ditt lösenord.</p>
<hr>
<p>Your eVaka password has been changed.</p>
<p>If you changed your password yourself, you can ignore this message. If not, log in to eVaka with strong authentication (Sign in using Suomi.fi) and change your password.</p>
""",
        )

    override fun emailChanged(newEmail: String): EmailContent =
        EmailContent.fromHtml(
            subject =
                "eVaka-sähköpostiosoitteesi on vaihdettu / Din eVaka e-postadress har ändrats / Your eVaka email address has been changed",
            html =
                """<p>eVaka-sähköpostiosoitteesi on vaihdettu. Uusi osoitteesi on $newEmail. Et saa enää eVaka-sähköposteja vanhaan osoitteeseesi.</p>
<p>Jos muutit sähköpostiosoitteesi itse, voit jättää tämän viestin huomiotta. Muussa tapauksessa kirjaudu eVakaan vahvalla tunnistautumisella (Kirjaudu Suomi.fi:ssä) ja korjaa sähköpostiosoitteesi.</p>
<hr>
<p>Din eVaka e-postadress har ändrats. Din nya adress är $newEmail. Du får inte längre e-post från eVaka till din gamla adress.</p>
<p>Om du har ändrat din e-postadress själv kan du ignorera det här meddelandet. Om inte, logga in i eVaka med stark identifiering (Logga in via Suomi.fi) och korrigera din e-postadress.</p>
<hr>
<p>Your eVaka email address has been changed. Your new address is $newEmail. You will no longer receive eVaka emails to your old address.</p>
<p>If you changed your email address yourself, you can ignore this message. If not, log in to eVaka with strong authentication (Sign in using Suomi.fi) and correct your email address.</p>
""",
        )

    override fun newBrowserLoginNotification(): EmailContent =
        EmailContent.fromHtml(
            subject =
                "Kirjautuminen uudella laitteella eVakaan / Inloggning med ny enhet på eVaka / Login on a new device to eVaka",
            html =
                """
<p><strong>Kirjautuminen uudella laitteella eVakaan</strong></p>
<p>Havaitsimme kirjautumisen eVaka-tilillesi uudelta laitteelta. Jos tämä olit sinä, voit jättää tämän viestin huomiotta.</p>
<p><strong>Jos et tunnista tätä kirjautumista:</strong></p>
<p>Kirjaudu eVakaan vahvalla tunnistautumisella (Kirjaudu Suomi.fi:ssä) ja vaihda salasanasi.</p>
<hr>
<p><strong>Inloggning med ny enhet på eVaka</strong></p>
<p>Vi upptäckte en inloggning på ditt eVaka-konto från en ny enhet. Om det var du kan du ignorera detta meddelande.</p>
<p><strong>Om du inte känner igen denna inloggning:</strong></p>
<p>Logga in på eVaka med stark autentisering (Logga in via Suomi.fi) och ändra ditt lösenord.</p>
<hr>
<p><strong>Login on a new device to eVaka</strong></p>
<p>We detected a login to your eVaka account from a new device. If this was you, you can ignore this message.</p>
<p><strong>If you don't recognize this login:</strong></p>
<p>Sign into eVaka using strong authentication (Sign in using Suomi.fi) and change your password.</p>
""",
        )
}
