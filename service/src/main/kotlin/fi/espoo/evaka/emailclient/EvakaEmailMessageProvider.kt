// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.emailclient

import fi.espoo.evaka.EvakaEnv
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.invoicing.service.IncomeNotificationType
import fi.espoo.evaka.messaging.MessageThreadStub
import fi.espoo.evaka.messaging.MessageType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.MessageThreadId
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import org.unbescape.html.HtmlEscape

/** Use http://localhost:9099/api/internal/dev-api/email-content to preview email messages */
class EvakaEmailMessageProvider(private val env: EvakaEnv) : IEmailMessageProvider {
    private fun link(language: Language, path: String): String {
        val baseUrl =
            when (language) {
                Language.sv -> env.frontendBaseUrlSv
                else -> env.frontendBaseUrlFi
            }
        val url = "$baseUrl$path"
        return """<a href="$url">$url</a>"""
    }

    private fun frontPageLink(language: Language) = link(language, "")

    private fun calendarLink(language: Language) = link(language, "/calendar")

    private fun messageLink(language: Language, threadId: MessageThreadId) =
        link(language, "/messages/$threadId")

    private fun childLink(language: Language, childId: ChildId) =
        link(language, "/children/$childId")

    private fun incomeLink(language: Language) = link(language, "/income")

    private fun unsubscribeLink(language: Language) =
        link(language, "/personal-details#notifications")

    private val unsubscribeFi =
        """<p><small>Jos et halua enää saada tämänkaltaisia viestejä, voit muuttaa asetuksia eVakan Omat tiedot -sivulla: ${unsubscribeLink(Language.fi)}</small></p>"""
    private val unsubscribeSv =
        """<p><small>Om du inte längre vill ta emot meddelanden som detta, kan du ändra dina inställningar på eVakas Personuppgifter-sida: ${unsubscribeLink(Language.sv)}</small></p>"""
    private val unsubscribeEn =
        """<p><small>If you no longer want to receive messages like this, you can change your settings on eVaka's Personal information page: ${unsubscribeLink(Language.en)}</small></p>"""

    override fun pendingDecisionNotification(language: Language): EmailContent {
        return EmailContent.fromHtml(
            subject =
                "Päätös varhaiskasvatuksesta / Beslut om förskoleundervisning / Decision on early childhood education",
            html =
                """
<p>Sinulla on vastaamaton päätös Espoon varhaiskasvatukselta. Päätös tulee hyväksyä tai hylätä kahden viikon sisällä sen saapumisesta.</p>
<p>Hakemuksen tekijä voi hyväksyä tai hylätä vastaamattomat päätökset kirjautumalla osoitteeseen ${frontPageLink(Language.fi)}, tai palauttamalla täytetyn lomakkeen päätöksen viimeiseltä sivulta siinä mainittuun osoitteeseen.</p>
<p>Tähän viestiin ei voi vastata. Tarvittaessa ole yhteydessä varhaiskasvatuksen palveluohjaukseen p. 09 816 31000</p>
$unsubscribeFi
<hr>
<p>Du har ett obesvarat beslut av småbarnspedagogiken i Esbo. Beslutet ska godkännas eller förkastas inom två veckor från att det inkommit.</p>
<p>Den som lämnat in ansökan kan godkänna eller förkasta obesvarade beslut genom att logga in på adressen ${frontPageLink(Language.sv)} eller genom att returnera den ifyllda blanketten som finns på sista sidan av beslutet till den adress som nämns på sidan.</p>
<p>Detta meddelande kan inte besvaras. Kontakta vid behov servicehandledningen inom småbarnspedagogiken, tfn 09 816 27600</p>
$unsubscribeSv
<hr>
<p>You have an unanswered decision from Espoo’s early childhood education. The decision must be accepted or rejected within two weeks of receiving it.</p>
<p>The person who submitted the application can accept or reject an unanswered decision by logging in to ${frontPageLink(Language.en)} or by sending the completed form on the last page of the decision to the address specified on the page.</p>
<p>You cannot reply to this message. If you have questions, please contact early childhood education service counselling, tel. 09 816 31000.</p>
$unsubscribeEn
"""
        )
    }

    private val applicationReceivedSubject =
        "Olemme vastaanottaneet hakemuksenne / Vi har tagit emot din ansökan / We have received your application"

    override fun clubApplicationReceived(language: Language): EmailContent =
        EmailContent.fromHtml(
            subject = applicationReceivedSubject,
            html =
                // This transactional message and cannot be unsubscribed from, so the unsubscribe
                // links are not included
                """
<p>Hyvä(t) huoltaja(t),</p>
<p>Lapsenne kerhohakemus on vastaanotettu. Hakemuksen tehnyt huoltaja voi muokata hakemusta osoitteessa ${frontPageLink(Language.fi)} siihen saakka, kunnes se on otettu käsittelyyn.</p>
<p>Syksyllä alkaviin kerhoihin tehdään päätöksiä kevään aikana hakuajan (1-31.3.) päättymisen jälkeen paikkatilanteen mukaan.</p>
<p>Kerhoihin voi hakea myös hakuajan jälkeen koko toimintavuoden ajan mahdollisesti vapautuville paikoille.</p>
<p>Päätös on nähtävissä ja hyväksyttävissä/hylättävissä ${frontPageLink(Language.fi)}.</p>
<p>Hakiessanne lapsellenne siirtoa uudella hakemuksella toiseen kerhoon. Uusi kerhopäätös tehdään paikkatilanteen sen salliessa. Hakemus on voimassa kuluvan kerhokauden. </p>
"""
        )

    override fun daycareApplicationReceived(language: Language): EmailContent =
        EmailContent.fromHtml(
            subject = applicationReceivedSubject,
            html =
                // This transactional message and cannot be unsubscribed from, so the unsubscribe
                // links are not included
                """
<p>Hyvä(t) huoltaja(t),<br>Lapsenne varhaiskasvatushakemus on vastaanotettu.</p>
<p>Varhaiskasvatushakemuksella on <strong>neljän (4) kuukauden hakuaika.</strong> Hakemuksen tehnyt huoltaja voi muokata hakemusta osoitteessa ${frontPageLink(Language.fi)} siihen saakka, kunnes se on otettu käsittelyyn.</p>
<p>Saatte tiedon lapsenne varhaiskasvatuspaikasta noin kuukautta ennen varhaiskasvatuksen toivottua aloittamista huomioiden varhaiskasvatuslain mukaiset neljän (4) kuukauden tai kahden viikon hakuajat.</p>
<p>Päätös on nähtävissä ja hyväksyttävissä/hylättävissä ${frontPageLink(Language.fi)}.</p>
<p>Hakiessanne palvelusetelipäiväkotiin, olkaa viimeistään hakemuksen jättämisen jälkeen yhteydessä suoraan kyseiseen yksikköön.</p>
<p>Mikäli valitsitte hakemuksen kiireelliseksi, teidän tulee toimittaa hakemuksen liitteeksi <strong>todistus äkillisestä työllistymisestä uuteen työpaikkaan tai todistus äkillisesti saadusta uudesta opiskelupaikasta.</strong> Hakuaika on tällöin <strong>minimissään 2 viikkoa</strong> ja alkaa todistuksen saapumispäivämäärästä.</p>
<p><strong>Ympärivuorokautista- tai iltahoitoa</strong> hakiessanne, teidän tulee toimittaa molempien samassa taloudessa asuvien huoltajien todistukset työnantajalta vuorotyöstä tai oppilaitoksesta iltaisin tapahtuvasta opiskelusta. <strong>Hakemusta käsitellään vuorohoidon hakemuksena vasta kun edellä mainitut todistukset on toimitettu.</strong></p>
<p>Hakemuksen liitteet voi lisätä suoraan sähköiselle hakemukselle tai toimittaa postitse osoitteeseen Espoon kaupunki, Varhaiskasvatuksen palveluohjaus, PL 3125, 02070 Espoon kaupunki</p>
<p>Hakiessanne lapsellenne <strong>siirtoa</strong> toiseen <strong>kunnalliseen varhaiskasvatusyksikköön</strong>, hakemuksella ei ole hakuaikaa. Hakemus on voimassa vuoden hakemuksen saapumispäivämäärästä. Mikäli lapsen nykyinen paikka irtisanotaan, myös siirtohakemus poistuu.</p>
<p>Palvelusetelin hakeminen: <a href="https://espoo.fi/fi/kasvatus-ja-opetus/varhaiskasvatus/varhaiskasvatuksen-palveluseteli#section-6228">espoo.fi/fi/kasvatus-ja-opetus/varhaiskasvatus/varhaiskasvatuksen-palveluseteli#section-6228</a></p>
<p>Hakeminen yksityisiin varhaiskasvatusyksiköihin: <a href="https://espoo.fi/fi/kasvatus-ja-opetus/varhaiskasvatus/yksityinen-varhaiskasvatus-ja-paivakodit">espoo.fi/fi/kasvatus-ja-opetus/varhaiskasvatus/yksityinen-varhaiskasvatus-ja-paivakodit</a></p>
<hr>
<p>Bästa vårdnadshavare,<br>Vi har tagit emot en ansökan om småbarnspedagogik för ditt barn.</p>
<p>Ansökan om småbarnspedagogik har en <strong>ansökningstid på fyra (4) månader.</strong> Den vårdnadshavare som har lämnat in ansökan kan redigera ansökan på adressen ${frontPageLink(Language.sv)} tills den har tagits upp till behandling.</p>
<p>Du får besked om ditt barns plats i småbarnspedagogiken cirka en månad före ansökt datum med beaktande av ansökningstiderna på fyra (4) månader eller två veckor enligt lagen om småbarnspedagogik.</p>
<p>Du kan se och godkänna/förkasta beslutet på ${frontPageLink(Language.sv)}.</p>
<p>När du ansöker plats till ett servicesedel daghem behöver du senast  vara i kontakt med daghemmet när du lämnat in ansökan till enheten i fråga.</p>
<p>Om du valde att ansökan är brådskande, ska du bifoga ansökan <strong>ett intyg över att du plötsligt fått ett nytt jobb eller en ny studieplats.</strong> Ansökningstiden är då <strong>minst 2 veckor</strong> och börjar den dag då intyget inkom.</p>
<p>När du ansöker om <strong>vård dygnet runt eller kvällstid</strong>, ska du lämna in arbetsgivarens intyg över skiftarbete eller läroanstaltens intyg över kvällsstudier för båda vårdnadshavarna som bor i samma hushåll. <strong>Ansökan behandlas som ansökan om skiftvård först när de ovannämnda intygen har lämnats in.</strong></p>
<p>När du ansöker om <strong>byte</strong> till en annan <strong>kommunal enhet för småbarnspedagogik</strong> har ansökan ingen ansökningstid. Ansökan gäller ett år från den dag då ansökan inkom. Om du säger upp barnets nuvarande plats, faller också ansökan om byte bort.</p>
<p>Ansökan om servicesedel: <a href="https://esbo.fi/fostran-och-utbildning/smabarnspedagogik/servicesedel-smabarnspedagogik#section-6228">esbo.fi/fostran-och-utbildning/smabarnspedagogik/servicesedel-smabarnspedagogik#section-6228</a></p>
<p>Ansökan till privata enheter för småbarnspedagogik: <a href="https://esbo.fi/tjanster/privat-smabarnspedagogik">esbo.fi/tjanster/privat-smabarnspedagogik</a></p>
<p>Bilagorna till ansökan skickas antingen per post till adressen Esbo stad, Småbarnspedagogikens servicehandledning, PB 3125, 02070 Esbo stad eller som e-postbilaga till <a href="mailto:dagis@esbo.fi">dagis@esbo.fi</a> (observera att förbindelsen inte är krypterad).</p>
<p>Du kan göra ändringar i ansökan så länge den inte har tagits upp till behandling. Därefter kan du göra ändringar i ansökan genom att kontakta småbarnspedagogikens servicehandledning (tfn 09 816 27600). Du kan återta en ansökan som du redan lämnat in genom att meddela detta per e-post till småbarnspedagogikens servicehandledning <a href="mailto:dagis@esbo.fi">dagis@esbo.fi</a></p>
<hr>
<p>Dear guardian(s),<br>We have received your child’s application for early childhood education.</p>
<p>The <strong>application period</strong> for early childhood education applications is <strong>four (4) months</strong>. The guardian who submitted the application can make changes to it at ${frontPageLink(Language.en)} until its processing starts.</p>
<p>You will be informed of your child’s early childhood education unit approximately one month before the desired start date of early childhood education, taking into account the application periods of four (4) months or two (2) weeks specified in the Act on Early Childhood Education and Care.</p>
<p>You can see the decision and accept/reject it at ${frontPageLink(Language.en)}.</p>
<p>When applying for a service voucher day care centre, please contact the unit directly at the latest after submitting your application.</p>
<p>If you chose to have your application processed urgently, you will also need to provide <strong>a document as proof of sudden employment at a new workplace or a sudden offer of a new study place.</strong> In this case, the <strong>minimum application period is two (2) weeks</strong> and it begins from the date on which the required document was received.</p>
<p>When applying for <strong>round-the-clock or evening care</strong>, both guardians living in the same household need to provide a document issued by their employer concerning shift work or a document issued by their educational institution concerning evening studies. <strong>Your application will only be processed as an application for round-the-clock care after you have provided the required documents</strong>.</p>
<p>You can add your supporting documents to your online application or send them by post to City of Espoo, Early childhood education service guidance, P.O. Box 3125, 02070 City of Espoo or as an email attachment to <a href="mailto:vaka.palveluohjaus@espoo.fi">vaka.palveluohjaus@espoo.fi</a> (please note that the connection is not encrypted).</p>
<p>When applying for a <strong>transfer</strong> to a different <strong>municipal early childhood education unit</strong>, your application will not have a specific application period. Your application will be valid for one (1) year from the date on which it was received. If your child’s current place is terminated, your transfer application will be deleted from the system.</p>
<p>How to apply for a service voucher: <a href="https://espoo.fi/en/childcare-and-education/early-childhood-education/service-voucher-early-childhood-education#section-6228">espoo.fi/en/childcare-and-education/early-childhood-education/service-voucher-early-childhood-education#section-6228</a></p>
<p>Information about applying to private early childhood education units: <a href="https://espoo.fi/en/childcare-and-education/early-childhood-education/private-early-childhood-education-and-day-care-centers">espoo.fi/en/childcare-and-education/early-childhood-education/private-early-childhood-education-and-day-care-centers</a>.</p>
"""
        )

    override fun preschoolApplicationReceived(
        language: Language,
        withinApplicationPeriod: Boolean
    ): EmailContent =
        if (withinApplicationPeriod) {
            EmailContent.fromHtml(
                subject = applicationReceivedSubject,
                html =
                    // This transactional message and cannot be unsubscribed from, so the
                    // unsubscribe links are not included
                    """
<p>Hyvä(t) huoltaja(t),</p>
<p>Lapsenne esiopetushakemus on vastaanotettu. Hakemuksen tehnyt huoltaja voi muokata hakemusta osoitteessa ${frontPageLink(Language.fi)} siihen saakka, kunnes hakemus on otettu käsittelyyn.</p>
<p>Päätökset tehdään hakuaikana (tammikuu) saapuneisiin hakemuksiin maaliskuun aikana.</p>
<p>Päätös on nähtävissä ja hyväksyttävissä/hylättävissä ${frontPageLink(Language.fi)}.</p>
<p>Hakiessanne palvelusetelipäiväkotiin, olkaa viimeistään hakemuksen jättämisen jälkeen yhteydessä suoraan kyseiseen yksikköön.</p>
<p>Ympärivuorokautista- tai iltahoitoa hakiessanne, teidän tulee toimittaa molempien samassa taloudessa asuvien huoltajien todistukset työnantajalta vuorotyöstä tai oppilaitoksesta iltaisin tapahtuvasta opiskelusta. Hakemusta käsitellään vuorohoidon hakemuksena vasta kun edellä mainitut todistukset on toimitettu.</p>
<p>Hakemuksen liitteet voi lisätä suoraan sähköiselle hakemukselle tai toimittaa postitse osoitteeseen Espoon kaupunki, Varhaiskasvatuksen palveluohjaus, PL 3125, 02070 Espoon kaupunki.</p>
<p>Palvelusetelin hakeminen: <a href="https://espoo.fi/fi/kasvatus-ja-opetus/varhaiskasvatus/varhaiskasvatuksen-palveluseteli#section-6228">Palveluseteli</a></p>
<p>Hakeminen yksityisiin varhaiskasvatusyksiköihin: <a href="https://espoo.fi/fi/kasvatus-ja-opetus/varhaiskasvatus/yksityinen-varhaiskasvatus-ja-paivakodit">Yksityinen varhaiskasvatus</a></p>
<hr>
<p>Bästa vårdnadshavare,</p>
<p>Vi har tagit emot ansökan om förskoleundervisning för ditt barn. Den vårdnadshavare som har lämnat in ansökan kan redigera ansökan på adressen ${frontPageLink(Language.sv)} tills den har tagits upp till behandling.</p>
<p>Om de ansökningar som kommit in under ansökningstiden (januari) fattas beslut i mars.</p>
<p>Du kan se och godkänna/förkasta beslutet på adressen ${frontPageLink(Language.sv)}.</p>
<p>När du ansöker till ett servicesedeldaghem, kontakta daghemmet direkt senast efter att du lämnat ansökan.</p>
<p>När du ansöker om vård dygnet runt eller kvällstid, ska du lämna in arbetsgivarens intyg över skiftarbete eller läroanstaltens intyg över kvällsstudier för båda vårdnadshavarna som bor i samma hushåll. Ansökan behandlas som en ansökan om skiftomsorg först när de ovannämnda intygen har lämnats in.</p>
<p>Bilagor till ansökan kan bifogas direkt till ansökan på webben eller skickas per post till adressen Esbo stad, Servicehandledningen inom småbarnspedagogiken, PB 32, 02070 Esbo stad.</p>
<p>Ansökan om servicesedel: <a href="https://esbo.fi/fostran-och-utbildning/smabarnspedagogik/servicesedel-smabarnspedagogik">Servicesedel</a></p>
<p>Ansökan till privata enheter för småbarnspedagogik: <a href="https://esbo.fi/tjanster/privat-smabarnspedagogik">Privat småbarnspedagogik</a></p>
<hr>
<p>Dear guardian(s),</p>
<p>We have received your child’s application for pre-primary education. The guardian who submitted the application can make changes to it at ${frontPageLink(Language.en)} until its processing starts.</p>
<p>The city will make decisions on applications received during the application period (January) in March.</p>
<p>You can see the decision and accept/reject it at ${frontPageLink(Language.en)}</p>
<p>When applying to a service voucher day care centre, please contact the unit no later than after you have submitted the application.</p>
<p>When applying for round-the-clock or evening care, both guardians living in the same household need to provide a document issued by their employer concerning shift work or a document issued by their educational institution concerning evening studies. Your application will only be processed as an application for round-the-clock care after you have provided the required documents.</p>
<p>You can add your supporting documents to your online application or send them by post to City of Espoo, Early childhood education service guidance, P.O. Box 3125, 02070 City of Espoo.</p>
<p>Information about applying for a service voucher: <a href="https://espoo.fi/en/childcare-and-education/early-childhood-education/service-voucher#section-6228">Service voucher</a></p>
<p>Information about applying to private early childhood education units: <a href="https://espoo.fi/en/childcare-and-education/early-childhood-education/private-early-childhood-education-and-day-care-centers">Private early childhood education</a></p>
"""
            )
        } else {
            EmailContent.fromHtml(
                subject = applicationReceivedSubject,
                html =
                    // This transactional message and cannot be unsubscribed from, so the
                    // unsubscribe links are not included
                    """
<p>Hyvä(t) huoltaja(t),</p>
<p>Lapsenne esiopetushakemus on vastaanotettu. Hakemuksen tehnyt huoltaja voi muokata hakemusta osoitteessa ${frontPageLink(Language.fi)} siihen saakka, kunnes se on otettu käsittelyyn.</p>
<p>Saatte tiedon lapsenne esiopetuspaikasta mahdollisimman pian, huomioiden hakemuksessa oleva aloitustoive ja alueen esiopetuspaikkatilanne.</p>
<p>Päätös on nähtävissä ja hyväksyttävissä/hylättävissä ${frontPageLink(Language.fi)}.</p>
<p>Hakiessanne esiopetusta palvelusetelipäiväkotiin, olkaa viimeistään hakemuksen jättämisen jälkeen yhteydessä suoraan kyseiseen yksikköön.</p>
<p>Mikäli valitsitte hakemuksen kiireelliseksi, teidän tulee toimittaa hakemuksen liitteeksi todistus äkillisestä työllistymisestä uuteen työpaikkaan tai todistus äkillisesti saadusta uudesta opiskelupaikasta. Hakuaika on tällöin minimissään 2 viikkoa ja alkaa todistuksen saapumispäivämäärästä.</p>
<p>Ympärivuorokautista- tai iltahoitoa hakiessanne, teidän tulee toimittaa molempien samassa taloudessa asuvien huoltajien todistukset työnantajalta vuorotyöstä tai oppilaitoksesta iltaisin tapahtuvasta opiskelusta. Hakemusta käsitellään vuorohoidon hakemuksena vasta kun edellä mainitut todistukset on toimitettu.</p>
<p>Hakemuksen liitteet voi lisätä suoraan sähköiselle hakemukselle tai toimittaa postitse osoitteeseen Espoon kaupunki, Varhaiskasvatuksen palveluohjaus, PL 3125, 02070 Espoon kaupunki.</p>
<p>Hakiessanne lapsellenne siirtoa uudella hakemuksella toiseen esiopetusyksikköön: Uusi esiopetuspäätös tehdään hakija- ja paikkatilanteen sen salliessa. Mikäli lapsen nykyinen esiopetuspaikka irtisanotaan, myös siirtohakemus poistuu.</p>
<p>Palvelusetelin hakeminen: <a href="https://espoo.fi/fi/kasvatus-ja-opetus/varhaiskasvatus/varhaiskasvatuksen-palveluseteli#section-6228">Palveluseteli</a></p>
<p>Hakeminen yksityisiin varhaiskasvatusyksiköihin: <a href="https://espoo.fi/fi/kasvatus-ja-opetus/varhaiskasvatus/yksityinen-varhaiskasvatus-ja-paivakodit">Yksityinen varhaiskasvatus</a></p>
<hr>
<p>Bästa vårdnadshavare,</p>
<p>Vi har tagit emot ansökan om förskoleundervisning för ditt barn. Den vårdnadshavare som har lämnat in ansökan kan redigera ansökan på adressen ${frontPageLink(Language.sv)} tills den har tagits upp till behandling.</p>
<p>Du får information om ditt barns förskoleplats så snart som möjligt, med beaktande av önskemålet om startdatum och läget med förskoleplatser i området.</p>
<p>Du kan se och godkänna/förkasta beslutet på adressen ${frontPageLink(Language.sv)}.</p>
<p>När du ansöker om förskoleundervisning i ett servicesedeldaghem, kontakta enheten direkt senast efter att du lämnat ansökan.</p>
<p>Om du valde att ansökan är brådskande, ska du till ansökan bifoga ett intyg över att du plötsligt fått ett nytt jobb eller en ny studieplats. Ansökningstiden är då minst två veckor och börjar den dag då intyget inkommer.</p>
<p>När du ansöker om vård dygnet runt eller kvällstid, ska du lämna in arbetsgivarens intyg över skiftarbete eller läroanstaltens intyg över kvällsstudier för båda vårdnadshavarna som bor i samma hushåll. Ansökan behandlas som en ansökan om skiftomsorg först när de ovannämnda intygen har lämnats in.</p>
<p>Bilagor till ansökan kan bifogas direkt till ansökan på webben eller skickas per post till adressen Esbo stad, Servicehandledningen inom småbarnspedagogiken, PB 32, 02070 Esbo stad.</p>
<p>När du ansöker om överföring till en annan enhet för förskoleundervisning med en ny ansökan, fattas ett nytt beslut om förskoleundervisning om sökande- och platsläget tillåter det. Om barnets nuvarande förskoleplats sägs upp, slopas också ansökan om överföring.</p>
<p>Ansökan om servicesedel: <a href="https://esbo.fi/fostran-och-utbildning/smabarnspedagogik/servicesedel-smabarnspedagogik">Servicesedel</a></p>
<p>Ansökan till privata enheter för småbarnspedagogik: <a href="https://esbo.fi/tjanster/privat-smabarnspedagogik">Privat småbarnspedagogik</a></p>
<hr>
<p>Dear guardian(s),</p>
<p>We have received your child’s application for pre-primary education. The guardian who submitted the application can make changes to it at ${frontPageLink(Language.en)} until its processing starts.</p>
<p>You will be informed of your child’s pre-primary education unit as soon as possible, taking into account the preferred starting date indicated in your application and the availability of pre-primary education places in your area.</p>
<p>You can see the decision and accept/reject it at ${frontPageLink(Language.en)}.</p>
<p>When applying for pre-primary education at a service voucher day care centre, please contact the unit no later than after you have submitted the application.</p>
<p>If you chose to have your application processed urgently, you will also need to provide a document as proof of sudden employment at a new workplace or a sudden offer of a new study place. In this case, the minimum application period is two (2) weeks and begins from the date on which the required document was received.</p>
<p>When applying for round-the-clock or evening care, both guardians living in the same household need to provide a document issued by their employer concerning shift work or a document issued by their educational institution concerning evening studies. Your application will only be processed as an application for round-the-clock care after you have provided the required documents.</p>
<p>You can add your supporting documents to your online application or send them by post to City of Espoo, Early childhood education service guidance, P.O. Box 3125, 02070 City of Espoo.</p>
<p>When applying for a transfer to a different pre-primary education unit by submitting a new application; the new pre-primary education decision will be made when the situation with the applicants and the available places so permit. If your child’s current pre-primary education place is terminated, your transfer application will be deleted from the system.</p>
<p>Information about applying for a service voucher: <a href="https://espoo.fi/en/childcare-and-education/early-childhood-education/service-voucher#section-6228">Service Voucher</a></p>
<p>Information about applying to private early childhood education units: <a href="https://espoo.fi/en/childcare-and-education/early-childhood-education/private-early-childhood-education-and-day-care-centers">Private early childhood education</a></p>
"""
            )
        }

    override fun missingReservationsNotification(
        language: Language,
        checkedRange: FiniteDateRange
    ): EmailContent {
        val start =
            checkedRange.start.format(
                DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
                    .withLocale(Locale.of("fi", "FI"))
            )
        return EmailContent.fromHtml(
            subject =
                "Läsnäolovarauksia puuttuu / Det finns några närvarobokningar som saknas / There are missing attendance reservations",
            html =
                """
<p>Läsnäolovarauksia puuttuu $start alkavalta viikolta. Käythän merkitsemässä ne mahdollisimman pian: ${calendarLink(Language.fi)}</p>
$unsubscribeFi
<hr>
<p>Det finns några närvarobokningar som saknas för veckan som börjar $start. Vänligen markera dem så snart som möjligt: ${calendarLink(Language.sv)}</p>
$unsubscribeSv
<hr>
<p>There are missing attendance reservations for week starting $start. Please mark them as soon as possible: ${calendarLink(Language.en)}</p>
$unsubscribeEn
"""
        )
    }

    override fun missingHolidayReservationsNotification(language: Language): EmailContent {
        return EmailContent.fromHtml(
            subject =
                "Loma-ajan ilmoitus sulkeutuu / Semesteranmälan löper ut / Holiday notification period closing",
            html =
                """
<p>Loma-ajan kysely sulkeutuu kahden päivän päästä. Jos lapseltanne/lapsiltanne puuttuu loma-ajan ilmoitus yhdeltä tai useammalta lomapäivältä, teettehän ilmoituksen eVakan kalenterissa mahdollisimman pian: ${calendarLink(Language.fi)}</p>
$unsubscribeFi
<hr>
<p>Förfrågan om barnets frånvaro i semestertider stängs om två dagar. Om ditt/dina barn saknar anmälan för en eller flera helgdagar, vänligen gör anmälan i eVaka-kalendern så snart som möjligt: ${calendarLink(Language.sv)}</p>
$unsubscribeSv
<hr>
<p>Two days left to submit a holiday notification. If you have not submitted a notification for each day, please submit them through the eVaka calendar as soon as possible: ${calendarLink(Language.en)}</p>
$unsubscribeEn
"""
        )
    }

    override fun assistanceNeedDecisionNotification(language: Language): EmailContent =
        EmailContent.fromHtml(
            subject = "Päätös eVakassa / Beslut i eVaka / Decision on eVaka",
            html =
                """
<p>Hyvä(t) huoltaja(t),</p>
<p>Lapsellenne on tehty päätös.</p>
<p>Päätös on nähtävissä eVakassa osoitteessa ${frontPageLink(Language.fi)}.</p>
$unsubscribeFi
<hr>
<p>Bästa vårdnadshavare,</p>
<p>Beslut har fattats angående ditt barn.</p>
<p>Beslutet finns att se i eVaka på ${frontPageLink(Language.sv)}.</p>
$unsubscribeSv
<hr>
<p>Dear guardian(s),</p>
<p>A decision has been made regarding your child.</p>
<p>The decision can be viewed on eVaka at ${frontPageLink(Language.en)}.</p>
$unsubscribeEn
"""
        )

    override fun assistanceNeedPreschoolDecisionNotification(language: Language): EmailContent =
        assistanceNeedDecisionNotification(language) // currently same content

    override fun messageNotification(language: Language, thread: MessageThreadStub): EmailContent {
        val (typeFi, typeSv, typeEn) =
            when (thread.type) {
                MessageType.MESSAGE ->
                    if (thread.urgent)
                        Triple(
                            "kiireellinen viesti",
                            "brådskande personligt meddelande",
                            "urgent message"
                        )
                    else Triple("viesti", "personligt meddelande", "message")
                MessageType.BULLETIN ->
                    if (thread.urgent)
                        Triple(
                            "kiireellinen tiedote",
                            "brådskande allmänt meddelande",
                            "urgent bulletin"
                        )
                    else Triple("tiedote", "allmänt meddelande", "bulletin")
            }
        return EmailContent.fromHtml(
            subject = "Uusi $typeFi eVakassa / Nytt $typeSv i eVaka / New $typeEn in eVaka",
            html =
                """
<p>Sinulle on saapunut uusi $typeFi eVakaan. Lue viesti ${if (thread.urgent) "mahdollisimman pian " else ""}täältä: ${messageLink(Language.fi, thread.id)}</p>
<p>Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.</p>
$unsubscribeFi
<hr>
<p>Du har fått ett nytt $typeSv i eVaka. Läs meddelandet ${if (thread.urgent) "så snart som möjligt " else ""}här: ${messageLink(Language.sv, thread.id)}</p>
<p>Detta besked skickas automatiskt av eVaka. Svara inte på detta besked.</p>
$unsubscribeSv
<hr>
<p>You have received a new $typeEn in eVaka. Read the message ${if (thread.urgent) "as soon as possible " else ""}here: ${messageLink(Language.en, thread.id)}</p>
<p>This is an automatic message from the eVaka system. Do not reply to this message.</p>
$unsubscribeEn
"""
        )
    }

    override fun childDocumentNotification(language: Language, childId: ChildId): EmailContent {
        return EmailContent.fromHtml(
            subject = "Uusi dokumentti eVakassa / Nytt dokument i eVaka / New document in eVaka",
            html =
                """
<p>Sinulle on saapunut uusi/päivitetty dokumentti eVakaan. Lue dokumentti täältä: ${childLink(Language.fi, childId)}</p>
<p>Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.</p>
$unsubscribeFi
<hr>
<p>Du har fått ett nytt/uppdaterat dokument i eVaka. Läs dokumentet här: ${childLink(Language.sv, childId)}</p>
<p>Detta besked skickas automatiskt av eVaka. Svara inte på detta besked.</p>
$unsubscribeSv
<hr>
<p>You have received a new/updated eVaka document. Read the document here: ${childLink(Language.en, childId)}</p>
<p>This is an automatic message from the eVaka system. Do not reply to this message.</p>
$unsubscribeEn
"""
        )
    }

    override fun vasuNotification(language: Language, childId: ChildId): EmailContent {
        return childDocumentNotification(language, childId)
    }

    override fun pedagogicalDocumentNotification(
        language: Language,
        childId: ChildId
    ): EmailContent {
        return EmailContent.fromHtml(
            subject =
                "Uusi pedagoginen dokumentti eVakassa / Nytt pedagogiskt dokument i eVaka / New pedagogical document in eVaka",
            html =
                """
<p>Sinulle on saapunut uusi pedagoginen dokumentti eVakaan. Lue dokumentti täältä: ${childLink(Language.fi, childId)}</p>
<p>Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.</p>
$unsubscribeFi
<hr>
<p>Du har fått ett nytt pedagogiskt dokument i eVaka. Läs dokumentet här: ${childLink(Language.sv, childId)}</p>
<p>Detta besked skickas automatiskt av eVaka. Svara inte på detta besked.</p>
$unsubscribeSv
<hr>
<p>You have received a new eVaka pedagogical document. Read the document here: ${childLink(Language.en, childId)}</p>
<p>This is an automatic message from the eVaka system. Do not reply to this message.</p>
$unsubscribeEn
"""
        )
    }

    override fun incomeNotification(
        notificationType: IncomeNotificationType,
        language: Language
    ): EmailContent {
        return when (notificationType) {
            IncomeNotificationType.INITIAL_EMAIL -> outdatedIncomeNotificationInitial()
            IncomeNotificationType.REMINDER_EMAIL -> outdatedIncomeNotificationReminder()
            IncomeNotificationType.EXPIRED_EMAIL -> outdatedIncomeNotificationExpired()
            IncomeNotificationType.NEW_CUSTOMER -> newCustomerIncomeNotification()
        }
    }

    private fun outdatedIncomeNotificationInitial(): EmailContent {
        return EmailContent.fromHtml(
            subject =
                "Tulotietojen tarkastuskehotus / Uppmaning att göra en inkomstutredning / Request to review income information",
            html =
                """
<p>Hyvä asiakkaamme</p>
<p>Varhaiskasvatuksen asiakasmaksun tai palvelusetelin omavastuuosuuden perusteena olevat tulotiedot tarkistetaan vuosittain.</p>
<p>Pyydämme toimittamaan tuloselvityksen eVakassa 28 päivän kuluessa tästä ilmoituksesta. eVakassa voitte myös antaa suostumuksen korkeimpaan maksuluokkaan tai tulorekisterin käyttöön. </p>
<p>Mikäli ette toimita uusia tulotietoja, asiakasmaksu määräytyy korkeimman maksuluokan mukaan. Puuttuvilla tulotiedoilla määrättyä maksua ei korjata takautuvasti.</p>
<p>Voitte tarvittaessa toimittaa tulotiedot myös postitse osoitteeseen Espoon kaupunki/ Kasvun ja oppimisen toimiala, talousyksikkö/ varhaiskasvatuksen asiakasmaksut PL 30 02070 Espoon kaupunki</p>
<p>Lisätietoja saatte tarvittaessa: vaka.maksut@espoo.fi</p>
<p>Tulotiedot: ${incomeLink(Language.fi)}</p>
<p>Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.</p>
$unsubscribeFi
<hr>
<p>Bästa klient</p>
<p>Inkomstuppgifterna som ligger till grund för klientavgiften för småbarnspedagogik eller servicesedelns egenandel granskas årligen.</p>
<p>Vi ber att du skickar en inkomstutredning via eVaka inom 28 dagar från den här anmälan. I eVaka kan du också ge ditt samtycke till den högsta avgiften eller till användning av inkomstregistret.</p>
<p>Om du inte lämnar in en ny inkomstutredning bestäms din klientavgift enligt den högsta avgiften. En avgift som fastställts på grund av bristfälliga inkomstuppgifter korrigeras inte retroaktivt.</p>
<p>Du kan vid behov också skicka inkomstutredningen per post till adressen Esbo stad/sektorn för fostran och lärande, ekonomienheten/klientavgifter för småbarnspedagogik PB 30 02070 Esbo stad</p>
<p>Mer information: vaka.maksut@espoo.fi</p>
<p>Inkomstuppgifterna: ${incomeLink(Language.sv)}</p>
<p>Detta besked skickas automatiskt av eVaka. Svara inte på detta besked.</p>
$unsubscribeSv
<hr>
<p>Dear client</p>
<p>The income information used for determining the early childhood education fee or the out-of-pocket cost of a service voucher is reviewed every year.</p>
<p>We ask you to submit your income statement through eVaka within 28 days of this notification. Through eVaka, you can also give your consent to the highest fee or the use of the Incomes Register.</p>
<p>If you do not provide your latest income information, your client fee will be determined based on the highest fee category. We will not retroactively reimburse you for fees charged in a situation where you have not provided your income information.</p>
<p>If necessary, you can also send your income information by post to the following address: City of Espoo / Growth and Learning Sector, Financial Management / Early childhood education client fees, P.O. Box 30, 02070 City of Espoo.</p>
<p>Inquiries: vaka.maksut@espoo.fi</p>
<p>Income information: ${incomeLink(Language.en)}</p>
<p>This is an automatic message from the eVaka system. Do not reply to this message.</p>
$unsubscribeEn
"""
        )
    }

    private fun outdatedIncomeNotificationReminder(): EmailContent {
        return EmailContent.fromHtml(
            subject =
                "Tulotietojen tarkastuskehotus / Uppmaning att göra en inkomstutredning / Request to review income information",
            html =
                """
<p>Hyvä asiakkaamme</p>
<p>Ette ole vielä toimittaneet uusia tulotietoja. Varhaiskasvatuksen asiakasmaksun tai palvelusetelin omavastuuosuuden perusteena olevat tulotiedot tarkistetaan vuosittain.</p>
<p>Pyydämme toimittamaan tuloselvityksen eVakassa 14 päivän kuluessa tästä ilmoituksesta. eVakassa voitte myös antaa suostumuksen korkeimpaan maksuluokkaan tai tulorekisterin käyttöön.</p>
<p>Mikäli ette toimita uusia tulotietoja, asiakasmaksu määräytyy korkeimman maksuluokan mukaan. Puuttuvilla tulotiedoilla määrättyä maksua ei korjata takautuvasti.</p>
<p>Voitte tarvittaessa toimittaa tulotiedot myös postitse osoitteeseen Espoon kaupunki/ Kasvun ja oppimisen toimiala, talousyksikkö/ varhaiskasvatuksen asiakasmaksut PL 30 02070 Espoon kaupunki</p>
<p>Lisätietoja saatte tarvittaessa: vaka.maksut@espoo.fi</p>
<p>Tulotiedot: ${incomeLink(Language.fi)}</p>
<p>Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.</p>
$unsubscribeFi
<hr>
<p>Bästa klient</p>
<p>Du har ännu inte lämnat in en ny inkomstutredning. Inkomstuppgifterna som ligger till grund för klientavgiften för småbarnspedagogik eller servicesedelns egenandel granskas årligen.</p>
<p>Vi ber att du skickar en inkomstutredning via eVaka inom 14 dagar från denna anmälan. I eVaka kan du också ge ditt samtycke till den högsta avgiften eller till användning av inkomstregistret.</p>
<p>Om du inte lämnar in en ny inkomstutredning bestäms din klientavgift enligt den högsta avgiften. En avgift som fastställts på grund av bristfälliga inkomstuppgifter korrigeras inte retroaktivt.</p>
<p>Du kan vid behov också skicka inkomstutredningen per post till adressen: Esbo stad/sektorn för fostran och lärande, ekonomienheten/klientavgifter för småbarnspedagogik PB 30 02070 Esbo stad</p>
<p>Mer information: vaka.maksut@espoo.fi</p>
<p>Inkomstuppgifterna: ${incomeLink(Language.sv)}</p>
<p>Detta besked skickas automatiskt av eVaka. Svara inte på detta besked.</p>
$unsubscribeSv
<hr>
<p>Dear client</p>
<p>You have not yet submitted your latest income information. The income information used for determining the early childhood education fee or the out-of-pocket cost of a service voucher is reviewed every year.</p>
<p>We ask you to submit your income statement through eVaka within 14 days of this notification. Through eVaka, you can also give your consent to the highest fee or the use of the Incomes Register.</p>
<p>If you do not provide your latest income information, your client fee will be determined based on the highest fee category. We will not retroactively reimburse you for fees charged in a situation where you have not provided your income information.</p>
<p>If necessary, you can also send your income information by post to the following address: City of Espoo / Growth and Learning Sector, Financial Management / Early childhood education client fees, P.O. Box 30, 02070 City of Espoo</p>
<p>Inquiries: vaka.maksut@espoo.fi</p>
<p>Income information: ${incomeLink(Language.en)}</p>
<p>This is an automatic message from the eVaka system. Do not reply to this message.</p>
$unsubscribeEn
"""
        )
    }

    private fun outdatedIncomeNotificationExpired(): EmailContent {
        return EmailContent.fromHtml(
            subject =
                "Tulotietojen tarkastuskehotus / Uppmaning att göra en inkomstutredning / Request to review income information",
            html =
                """
<p>Hyvä asiakkaamme</p>
<p>Seuraava asiakasmaksunne määräytyy korkeimman maksuluokan mukaan, sillä ette ole toimittaneet uusia tulotietoja määräaikaan mennessä.</p>
<p>Lisätietoja saatte tarvittaessa: vaka.maksut@espoo.fi</p>
<p>Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.</p>
$unsubscribeFi
<hr>
<p>Bästä klient</p>
<p>Din följande klientavgift bestäms enligt den högsta avgiften, eftersom du inte har lämnat in en inkomstutredning inom utsatt tid.</p>
<p>Mer information: vaka.maksut@espoo.fi</p>
<p>Detta besked skickas automatiskt av eVaka. Svara inte på detta besked.</p>
$unsubscribeSv
<hr>
<p>Dear client</p>
<p>Your next client fee will be determined based on the highest fee category as you did not provide your latest income information by the deadline.</p>
<p>Inquiries: vaka.maksut@espoo.fi</p>
<p>This is an automatic message from the eVaka system. Do not reply to this message.</p>
$unsubscribeEn
"""
        )
    }

    private fun newCustomerIncomeNotification(): EmailContent {
        return EmailContent.fromHtml(
            subject =
                "Tulotietojen tarkastuskehotus / Uppmaning att göra en inkomstutredning / Request to review income information",
            html =
                """
<p>Hyvä asiakkaamme</p>
<p>Lapsenne on aloittamassa varhaiskasvatuksessa tämän kuukauden aikana. Pyydämme teitä toimittamaan tulotiedot eVaka-järjestelmän kautta tämän kuukauden loppuun mennessä.</p>
<p>Lisätietoja saatte tarvittaessa: vaka.maksut@espoo.fi</p>
<p>Tulotiedot: ${incomeLink(Language.fi)}</p>
<p>Tämä on eVaka-järjestelmän automaattisesti lähettämä ilmoitus. Älä vastaa tähän viestiin.</p>
$unsubscribeFi
<hr>
<p>Bästä klient</p>
<p>Ditt barn börjar småbarnspedagogiken under den här månaden. Vi ber dig att lämna in din inkomstinformation via eVaka-systemet senast i slutet av denna månad.</p>
<p>Mer information: vaka.maksut@espoo.fi</p>
<p>Inkomstuppgifterna: ${incomeLink(Language.sv)}</p>
<p>Detta besked skickas automatiskt av eVaka. Svara inte på detta besked.</p>
$unsubscribeSv
<hr>
<p>Dear client</p>
<p>Your child is starting early childhood education during this month. We ask you to submit your income information via eVaka system by the end of this month.</p>
<p>Inquiries: vaka.maksut@espoo.fi</p>
<p>Income information: ${incomeLink(Language.en)}</p>
<p>This is an automatic message from the eVaka system. Do not reply to this message.</p>
$unsubscribeEn
            """
                    .trimIndent()
        )
    }

    override fun calendarEventNotification(
        language: Language,
        events: List<CalendarEventNotificationData>
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
                    "<li>$period: ${HtmlEscape.escapeHtml5(event.title)}</li>"
                } +
                "</ul>"
        return EmailContent.fromHtml(
            subject =
                "Uusia kalenteritapahtumia eVakassa / Nya kalenderhändelser i eVaka / New calendar events in eVaka",
            html =
                """
<p>eVakaan on lisätty uusia kalenteritapahtumia:</p>
$eventsHtml
<p>Katso lisää kalenterissa: ${calendarLink(Language.fi)}</p>
$unsubscribeFi
<hr>
<p>Nya kalenderhändelser i eVaka:</p>
$eventsHtml
<p>Se mer i kalendern: ${calendarLink(Language.sv)}</p>
$unsubscribeSv
<hr>
<p>New calendar events in eVaka:</p>
$eventsHtml
<p>See more in the calendar: ${calendarLink(Language.en)}</p>
$unsubscribeEn
"""
        )
    }
}
