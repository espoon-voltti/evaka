// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.emailclient

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService
import com.amazonaws.services.simpleemail.model.AccountSendingPausedException
import com.amazonaws.services.simpleemail.model.Body
import com.amazonaws.services.simpleemail.model.ConfigurationSetDoesNotExistException
import com.amazonaws.services.simpleemail.model.ConfigurationSetSendingPausedException
import com.amazonaws.services.simpleemail.model.Content
import com.amazonaws.services.simpleemail.model.Destination
import com.amazonaws.services.simpleemail.model.MailFromDomainNotVerifiedException
import com.amazonaws.services.simpleemail.model.Message
import com.amazonaws.services.simpleemail.model.SendEmailRequest
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.identity.VolttiIdentifier
import mu.KotlinLogging
import org.springframework.core.env.Environment

private val logger = KotlinLogging.logger {}

class EmailClient(private val client: AmazonSimpleEmailService, env: Environment) : IEmailClient {
    private val senderAddressFi = env.getProperty("application.email.address.fi", "")
    private val senderNameFi = env.getProperty("application.email.name.fi", "")
    private val senderAddressSv = env.getProperty("application.email.address.sv", "")
    private val senderNameSv = env.getProperty("application.email.name.sv", "")

    override fun sendApplicationEmail(personId: VolttiIdentifier, toAddress: String?, language: Language) {
        val charset = "UTF-8"
        val subject = getSubject(language)
        val htmlBody = getHtml(language)
        val textBody = getText(language)
        val fromAddress = when (language) {
            Language.fi -> "$senderNameFi <$senderAddressFi>"
            Language.sv -> "$senderNameSv <$senderAddressSv>"
        }

        logger.info { "Sending application email (personId: $personId)" }

        if (validateEmail(personId, toAddress)) {
            try {
                val request = SendEmailRequest()
                    .withDestination(Destination(listOf(toAddress)))
                    .withMessage(
                        Message()
                            .withBody(
                                Body()
                                    .withHtml(Content().withCharset(charset).withData(htmlBody))
                                    .withText(Content().withCharset(charset).withData(textBody))
                            )
                            .withSubject(Content().withCharset(charset).withData(subject))
                    )
                    .withSource(fromAddress)

                client.sendEmail(request)
                logger.info { "Application email sent (personId: $personId)" }
            } catch (e: Exception) {
                when (e) {
                    is MailFromDomainNotVerifiedException, is ConfigurationSetDoesNotExistException, is ConfigurationSetSendingPausedException, is AccountSendingPausedException ->
                        logger.error { "Couldn't send application email (personId: $personId): ${e.message}" }
                    else -> {
                        logger.error { "Couldn't send application email (personId: $personId): ${e.message}" }
                        throw e
                    }
                }
            }
        }
    }

    private fun getSubject(language: Language): String {
        val postfix = if (System.getenv("VOLTTI_ENV") == "staging") " [staging]" else ""

        return when (language) {
            Language.fi -> "Olemme vastaanottaneet hakemuksenne$postfix"
            Language.sv -> "Vi har tagit emot din ansökan$postfix"
        }
    }

    private fun getHtml(language: Language): String {
        return when (language) {
            Language.fi -> """
<p>
Hyvä(t) huoltaja(t), <br>
Lapsenne varhaiskasvatushakemus on vastaanotettu.
</p>
<p>
Varhaiskasvatushakemuksella on <strong>neljän (4) kuukauden hakuaika.</strong> Hakemuksen tehnyt huoltaja voi muokata hakemusta osoitteessa <a href="www.espoonvarhaiskasvatus.fi">www.espoonvarhaiskasvatus.fi</a> siihen saakka, kun se on otettu käsittelyyn. 
</p>
<p>
Saatte tiedon lapsenne varhaiskasvatuspaikasta noin kuukautta ennen hakemuksen lakisääteisen järjestelyajan päättymistä. Hakemuksen lakisääteinen järjestelyaika on neljä (4) kuukautta hakemuksen saapumisesta. Päätös on nähtävissä ja hyväksyttävissä/hylättävissä <a href="www.espoonvarhaiskasvatus.fi">www.espoonvarhaiskasvatus.fi</a>.
</p>
<p>
Mikäli valitsitte hakemuksen kiireelliseksi, teidän tulee toimittaa hakemuksen liitteeksi <strong>todistus äkillisestä työllistymisestä uuteen työpaikkaan tai todistus äkillisesti saadusta uudesta opiskelupaikasta.</strong> Hakuaika on tällöin <strong>minimissään 2 viikkoa</strong> ja alkaa todistuksen saapumispäivämäärästä.
</p>
<p>
<strong>Ympärivuorokautista- tai iltahoitoa</strong> hakiessanne, teidän tulee toimittaa molempien samassa taloudessa asuvien huoltajien todistukset työnantajalta vuorotyöstä tai oppilaitoksesta iltaisin tapahtuvasta opiskelusta. <strong>Hakemusta käsitellään vuorohoidon hakemuksena vasta kun edellä mainitut todistukset on toimitettu.</strong>
</p>
<p>
Hakiessanne lapsellenne <strong>siirtoa</strong> toiseen <strong>kunnalliseen varhaiskasvatusyksikköön</strong>, hakemuksella ei ole hakuaikaa. Hakemus on voimassa vuoden hakemuksen saapumispäivämäärästä.
</p>
<p>
Hakeminen yksityisiin varhaiskasvatusyksiköihin <a href="https://www.espoo.fi/fi-FI/Kasvatus_ja_opetus/Varhaiskasvatus/Yksityinen_varhaiskasvatus">https://www.espoo.fi/fi-FI/Kasvatus_ja_opetus/Varhaiskasvatus/Yksityinen_varhaiskasvatus</a>
</p>
<p>
Hakemuksen liitteet toimitetaan joko postitse osoitteeseen Espoon kaupunki, Varhaiskasvatuksen palveluohjaus, PL 3125, 02070 Espoon kaupunki tai liitetiedostona <a href="mailto:varhaiskasvatuksen.palveluohjaus@espoo.fi">varhaiskasvatuksen.palveluohjaus@espoo.fi</a> (huomioithan että yhteys ei ole salattu).
</p>
            """.trimIndent()
            Language.sv -> """
<p>
Bästa vårdnadshavare, <br>
Vi har tagit emot en ansökan om småbarnspedagogik för ditt barn.
</p>
<p>
Ansökan om småbarnspedagogik har en <strong>ansökningstid på fyra (4) månader.</strong> Den vårdnadshavare som har lämnat in ansökan kan redigera ansökan på adressen <a href="www.espoonvarhaiskasvatus.fi">www.espoonvarhaiskasvatus.fi</a> tills den har tagits upp till behandling.
</p>
<p>
Du får information om platsen för småbarnspedagogik för ditt barn cirka en månad (1 månad) innan ansökningstiden för ansökan går ut. Du kan se och godkänna/förkasta beslutet på <a href="www.espoonvarhaiskasvatus.fi">www.espoonvarhaiskasvatus.fi</a>.
</p>
<p>
Om du valde att ansökan är brådskande, ska du bifoga ansökan <strong>ett intyg över att du plötsligt fått ett nytt jobb eller en ny studieplats.</strong> Ansökningstiden är då <strong>minst 2 veckor</strong> och börjar den dag då intyget inkom.
</p>
<p>
När du ansöker om <strong>vård dygnet runt eller kvällstid</strong>, ska du lämna in arbetsgivarens intyg över skiftarbete eller läroanstaltens intyg över kvällsstudier för båda vårdnadshavarna som bor i samma hushåll. <strong>Ansökan behandlas som ansökan om skiftvård först när de ovannämnda intygen har lämnats in.</strong>
</p>
<p>
När du ansöker om <strong>flyttning</strong> till en annan <strong>kommunal enhet för småbarnspedagogik</strong> har ansökan ingen ansökningstid. Ansökan gäller ett år från den dag då ansökan inkom.
</p>
<p>
Ansökan till privata enheter för småbarnspedagogik <a href="https://www.esbo.fi/sv-FI/Utbildning_och_fostran/Smabarnspedagogik/Privat_smabarnspedagogik">https://www.esbo.fi/sv-FI/Utbildning_och_fostran/Smabarnspedagogik/Privat_smabarnspedagogik</a>
</p>
<p>
Bilagorna till ansökan skickas antingen per post till adressen Esbo stad, Småbarnspedagogikens servicehänvisning, PB 3125, 02070 Esbo stad eller som e-postbilaga till <a href="mailto:dagis@esbo.fi">dagis@esbo.fi</a> (observera att förbindelsen inte är krypterad).
</p>
<p>
Du kan göra ändringar i ansökan så länge den inte har tagits upp till behandling. Därefter kan du göra ändringar i ansökan genom att kontakta småbarnspedagogikens servicehänvisning (tfn 09 816 31000). Du kan återta en ansökan som du redan lämnat in genom att meddela detta per e-post till småbarnspedagogikens servicehänvisning <a href="mailto:dagis@esbo.fi">dagis@esbo.fi</a>
</p>
            """.trimIndent()
        }
    }

    private fun getText(language: Language): String {
        return when (language) {
            Language.fi -> """
Hyvä(t) huoltaja(t),
Lapsenne varhaiskasvatushakemus on vastaanotettu.

Varhaiskasvatushakemuksella on neljän (4) kuukauden hakuaika. Hakemuksen tehnyt huoltaja voi muokata hakemusta osoitteessa www.espoonvarhaiskasvatus.fi siihen saakka, kun se on otettu käsittelyyn.

Saatte tiedon lapsenne varhaiskasvatuspaikasta noin kuukautta ennen hakemuksen lakisääteisen järjestelyajan päättymistä. Hakemuksen lakisääteinen järjestelyaika on neljä (4) kuukautta hakemuksen saapumisesta. Päätös on nähtävissä ja hyväksyttävissä/hylättävissä www.espoonvarhaiskasvatus.fi.

Mikäli valitsitte hakemuksen kiireelliseksi, teidän tulee toimittaa hakemuksen liitteeksi todistus äkillisestä työllistymisestä uuteen työpaikkaan tai todistus äkillisesti saadusta uudesta opiskelupaikasta. Hakuaika on tällöin minimissään 2 viikkoa ja alkaa todistuksen saapumispäivämäärästä.

Ympärivuorokautista- tai iltahoitoa hakiessanne, teidän tulee toimittaa molempien samassa taloudessa asuvien huoltajien todistukset työnantajalta vuorotyöstä tai oppilaitoksesta iltaisin tapahtuvasta opiskelusta. Hakemusta käsitellään vuorohoidon hakemuksena vasta kun edellä mainitut todistukset on toimitettu.

Hakiessanne lapsellenne siirtoa toiseen kunnalliseen varhaiskasvatusyksikköön, hakemuksella ei ole hakuaikaa. Hakemus on voimassa vuoden hakemuksen saapumispäivämäärästä.

Hakeminen yksityisiin varhaiskasvatusyksiköihin https://www.espoo.fi/fi-FI/Kasvatus_ja_opetus/Varhaiskasvatus/Yksityinen_varhaiskasvatus

Hakemuksen liitteet toimitetaan joko postitse osoitteeseen Espoon kaupunki, Varhaiskasvatuksen palveluohjaus, PL 3125, 02070 Espoon kaupunki tai liitetiedostona varhaiskasvatuksen.palveluohjaus@espoo.fi (huomioithan että yhteys ei ole salattu).
            """.trimIndent()
            Language.sv -> """
Bästa vårdnadshavare,
Vi har tagit emot en ansökan om småbarnspedagogik för ditt barn.

Ansökan om småbarnspedagogik har en ansökningstid på fyra (4) månader. Den vårdnadshavare som har lämnat in ansökan kan redigera ansökan på adressen <a href="www.espoonvarhaiskasvatus.fi">www.espoonvarhaiskasvatus.fi</a> tills den har tagits upp till behandling.

Du får information om platsen för småbarnspedagogik för ditt barn cirka en månad (1 månad) innan ansökningstiden för ansökan går ut. Du kan se och godkänna/förkasta beslutet på <a href="www.espoonvarhaiskasvatus.fi">www.espoonvarhaiskasvatus.fi</a>.

Om du valde att ansökan är brådskande, ska du bifoga ansökan ett intyg över att du plötsligt fått ett nytt jobb eller en ny studieplats. Ansökningstiden är då minst 2 veckor och börjar den dag då intyget inkom.

När du ansöker om vård dygnet runt eller kvällstid, ska du lämna in arbetsgivarens intyg över skiftarbete eller läroanstaltens intyg över kvällsstudier för båda vårdnadshavarna som bor i samma hushåll. Ansökan behandlas som ansökan om skiftvård först när de ovannämnda intygen har lämnats in.

När du ansöker om flyttning till en annan kommunal enhet för småbarnspedagogik har ansökan ingen ansökningstid. Ansökan gäller ett år från den dag då ansökan inkom.

Ansökan till privata enheter för småbarnspedagogik <a href="https://www.esbo.fi/sv-FI/Utbildning_och_fostran/Smabarnspedagogik/Privat_smabarnspedagogik">https://www.esbo.fi/sv-FI/Utbildning_och_fostran/Smabarnspedagogik/Privat_smabarnspedagogik</a>

Bilagorna till ansökan skickas antingen per post till adressen Esbo stad, Småbarnspedagogikens servicehänvisning, PB 3125, 02070 Esbo stad eller som e-postbilaga till dagis@esbo.fi (observera att förbindelsen inte är krypterad).

Du kan göra ändringar i ansökan så länge den inte har tagits upp till behandling. Därefter kan du göra ändringar i ansökan genom att kontakta småbarnspedagogikens servicehänvisning (tfn 09 816 31000). Du kan återta en ansökan som du redan lämnat in genom att meddela detta per e-post till småbarnspedagogikens servicehänvisning dagis@esbo.fi
            """.trimIndent()
        }
    }
}
