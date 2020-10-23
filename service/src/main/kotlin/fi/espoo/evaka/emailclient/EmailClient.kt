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
    private val finnishApplicationAddress = env.getProperty("application.email.from.fi", "")
    private val swedishApplicationAddress = env.getProperty("application.email.from.sv", "")

    override fun sendApplicationEmail(personId: VolttiIdentifier, toAddress: String?, language: Language) {
        val charset = "UTF-8"
        val subject = getSubject(language)
        val htmlBody = getHtml(language)
        val textBody = getText(language)
        val fromAddress = when (language) {
            Language.fi -> finnishApplicationAddress
            Language.sv -> swedishApplicationAddress
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

    // TODO ruotsinkieliset teksit
    private fun getSubject(language: Language): String {
        return when (language) {
            Language.fi -> "Olemme vastaanottaneet hakemuksenne"
            Language.sv -> "Ruotsinkielinen otsikko"
        }
    }

    // TODO ruotsinkieliset teksit
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
Saatte tiedon lapsenne varhaiskasvatuspaikasta noin kuukautta ennen hakemuksen lakisääteisen järjestelyajan päättymistä. Hakemuksen lakisääteinen järjestelyaika on yksi (1) kuukausi hakemuksen saapumisesta. Päätös on nähtävissä ja hyväksyttävissä/hylättävissä <a href="www.espoonvarhaiskasvatus.fi">www.espoonvarhaiskasvatus.fi</a>.
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
            Language.sv -> "Ruotsinkielinen teksti"
        }
    }

    private fun getText(language: Language): String {
        return when (language) {
            Language.fi -> """
Hyvä(t) huoltaja(t),
Lapsenne varhaiskasvatushakemus on vastaanotettu.

Varhaiskasvatushakemuksella on neljän (4) kuukauden hakuaika. Hakemuksen tehnyt huoltaja voi muokata hakemusta osoitteessa www.espoonvarhaiskasvatus.fi siihen saakka, kun se on otettu käsittelyyn.

Saatte tiedon lapsenne varhaiskasvatuspaikasta noin kuukautta ennen hakemuksen lakisääteisen järjestelyajan päättymistä. Hakemuksen lakisääteinen järjestelyaika on yksi (1) kuukausi hakemuksen saapumisesta. Päätös on nähtävissä ja hyväksyttävissä/hylättävissä www.espoonvarhaiskasvatus.fi.

Mikäli valitsitte hakemuksen kiireelliseksi, teidän tulee toimittaa hakemuksen liitteeksi todistus äkillisestä työllistymisestä uuteen työpaikkaan tai todistus äkillisesti saadusta uudesta opiskelupaikasta. Hakuaika on tällöin minimissään 2 viikkoa ja alkaa todistuksen saapumispäivämäärästä.

Ympärivuorokautista- tai iltahoitoa hakiessanne, teidän tulee toimittaa molempien samassa taloudessa asuvien huoltajien todistukset työnantajalta vuorotyöstä tai oppilaitoksesta iltaisin tapahtuvasta opiskelusta. Hakemusta käsitellään vuorohoidon hakemuksena vasta kun edellä mainitut todistukset on toimitettu.

Hakiessanne lapsellenne siirtoa toiseen kunnalliseen varhaiskasvatusyksikköön, hakemuksella ei ole hakuaikaa. Hakemus on voimassa vuoden hakemuksen saapumispäivämäärästä.

Hakeminen yksityisiin varhaiskasvatusyksiköihin https://www.espoo.fi/fi-FI/Kasvatus_ja_opetus/Varhaiskasvatus/Yksityinen_varhaiskasvatus

Hakemuksen liitteet toimitetaan joko postitse osoitteeseen Espoon kaupunki, Varhaiskasvatuksen palveluohjaus, PL 3125, 02070 Espoon kaupunki tai liitetiedostona varhaiskasvatuksen.palveluohjaus@espoo.fi (huomioithan että yhteys ei ole salattu).
            """.trimIndent()
            Language.sv -> "Ruotsinkielinen teksti"
        }
    }
}
