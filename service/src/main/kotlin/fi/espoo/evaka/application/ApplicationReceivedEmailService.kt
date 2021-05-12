// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.emailclient.IEmailClient
import mu.KotlinLogging
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
class ApplicationReceivedEmailService(private val emailClient: IEmailClient, env: Environment) {

    private val senderAddressFi = env.getProperty("application.email.address.fi", "")
    private val senderNameFi = env.getProperty("application.email.name.fi", "")
    private val senderAddressSv = env.getProperty("application.email.address.sv", "")
    private val senderNameSv = env.getProperty("application.email.name.sv", "")

    fun sendApplicationEmail(personId: UUID, toAddress: String, language: Language, type: ApplicationType, sentWithinPreschoolApplicationPeriod: Boolean? = null) {
        val fromAddress = when (language) {
            Language.sv -> "$senderNameSv <$senderAddressSv>"
            else -> "$senderNameFi <$senderAddressFi>"
        }

        val html = when (type) {
            ApplicationType.DAYCARE -> getHtmlForDaycare()
            ApplicationType.CLUB -> getHtmlForClub()
            ApplicationType.PRESCHOOL -> getHtmlForPreschool(sentWithinPreschoolApplicationPeriod!!)
        }

        val text = when (type) {
            ApplicationType.DAYCARE -> getTextForDaycare()
            ApplicationType.CLUB -> getTextForClub()
            ApplicationType.PRESCHOOL -> getTextForPreschool(sentWithinPreschoolApplicationPeriod!!)
        }

        logger.info { "Sending application email (personId: $personId)" }
        emailClient.sendEmail(personId.toString(), toAddress, fromAddress, getSubject(language), html, text)
    }

    private fun getSubject(language: Language): String {
        val postfix = if (System.getenv("VOLTTI_ENV") == "staging") " [staging]" else ""

        return when (language) {
            Language.sv -> "Vi har tagit emot din ansökan$postfix"
            else -> "Olemme vastaanottaneet hakemuksenne$postfix"
        }
    }
    private fun getHtmlForPreschool(withinApplicationPeriod: Boolean): String {
        return if (withinApplicationPeriod) """
                <p>Hyvä(t) huoltaja(t),</p>                
                <p>Lapsenne esiopetushakemus on vastaanotettu. Hakemuksen tehnyt huoltaja voi muokata hakemusta osoitteessa <a href="https://www.espoonvarhaiskasvatus.fi">www.espoonvarhaiskasvatus.fi</a> siihen saakka, kunnes hakemus on otettu käsittelyyn.</p>
                <p>Päätökset tehdään hakuaikana (tammikuu) saapuneisiin hakemuksiin maaliskuun aikana.</p>
                <p>Päätös on nähtävissä ja hyväksyttävissä/hylättävissä <a href="https://www.espoonvarhaiskasvatus.fi">www.espoonvarhaiskasvatus.fi</a>.</p>
                <p>Hakiessanne palvelusetelipäiväkotiin, olkaa viimeistään hakemuksen jättämisen jälkeen yhteydessä suoraan kyseiseen yksikköön.</p>
                <p>Ympärivuorokautista- tai iltahoitoa hakiessanne, teidän tulee toimittaa molempien samassa taloudessa asuvien huoltajien todistukset työnantajalta vuorotyöstä tai oppilaitoksesta iltaisin tapahtuvasta opiskelusta. Hakemusta käsitellään vuorohoidon hakemuksena vasta kun edellä mainitut todistukset on toimitettu.</p>
                <p>Hakemuksen liitteet voi lisätä suoraan sähköiselle hakemukselle tai toimittaa postitse osoitteeseen Espoon kaupunki, Varhaiskasvatuksen palveluohjaus, PL 3125, 02070 Espoon kaupunki.</p>
                <p>Palvelusetelin hakeminen: <a href="https://www.espoo.fi/fi-FI/Kasvatus_ja_opetus/Varhaiskasvatus/Hakeminen_varhaiskasvatukseen/Palveluseteli/Tietoa_perheille">Palveluseteli</a></p>
                <p>Hakeminen yksityisiin varhaiskasvatusyksiköihin: <a href="https://www.espoo.fi/fi-FI/Kasvatus_ja_opetus/Varhaiskasvatus/Yksityinen_varhaiskasvatus">Yksityinen varhaiskasvatus</a></p>            
                <hr>
                <p>Bästa vårdnadshavare,</p>                
                <p>Vi har tagit emot ansökan om förskoleundervisning för ditt barn. Den vårdnadshavare som har lämnat in ansökan kan redigera ansökan på adressen <a href="https://www.esbosmabarnspedagogik.fi">www.esbosmabarnspedagogik.fi</a> tills den har tagits upp till behandling.</p>
                <p>Om de ansökningar som kommit in under ansökningstiden (januari) fattas beslut i mars.</p>
                <p>Du kan se och godkänna/förkasta beslutet på adressen <a href="https://www.esbosmabarnspedagogik.fi">www.esbosmabarnspedagogik.fi</a>.</p>
                <p>När du ansöker till ett servicesedeldaghem, kontakta daghemmet direkt senast efter att du lämnat ansökan.</p>
                <p>När du ansöker om vård dygnet runt eller kvällstid, ska du lämna in arbetsgivarens intyg över skiftarbete eller läroanstaltens intyg över kvällsstudier för båda vårdnadshavarna som bor i samma hushåll. Ansökan behandlas som en ansökan om skiftomsorg först när de ovannämnda intygen har lämnats in.</p>
                <p>Bilagor till ansökan kan bifogas direkt till ansökan på webben eller skickas per post till adressen Esbo stad, Servicehänvisningen inom småbarnspedagogiken, PB 3125, 02070 Esbo stad.</p>
                <p>Ansökan om servicesedel: <a href="https://www.esbo.fi/sv-FI/Utbildning_och_fostran/Smabarnspedagogik/Privat_smabarnspedagogik/Servicesedel">Servicesedel</a></p>                
                <p>Ansökan till privata enheter för småbarnspedagogik: <a href="https://www.esbo.fi/sv-FI/Utbildning_och_fostran/Smabarnspedagogik/Privat_smabarnspedagogik">Privat småbarnspedagogik</a></p>            
                <hr>
                <p>Dear guardian(s),</p>                
                <p>We have received your child’s application for pre-primary education. The guardian who submitted the application can make changes to it at until its processing starts.</p>
                <p>The city will make decisions on applications received during the application period (January) in March.</p>
                <p>You can see the decision and accept/reject it at <a href="https://www.espoonvarhaiskasvatus.fi">www.espoonvarhaiskasvatus.fi</a></p>
                <p>When applying to a service voucher day care centre, please contact the unit no later than after you have submitted the application.</p>
                <p>When applying for round-the-clock or evening care, both guardians living in the same household need to provide a document issued by their employer concerning shift work or a document issued by their educational institution concerning evening studies. Your application will only be processed as an application for round-the-clock care after you have provided the required documents.</p>
                <p>You can add your supporting documents to your online application or send them by post to City of Espoo, Early childhood education service guidance, P.O. Box 3125, 02070 City of Espoo.</p>
                <p>Information about applying for a service voucher: <a href="https://www.espoo.fi/en-US/Childcare_and_education/Early_childhood_education/Applying_for_early_childhood_education/Service_voucher/Information_for_families">Service voucher</a></p>
                <p>Information about applying to private early childhood education units: <a href="https://www.espoo.fi/en-US/Childcare_and_education/Early_childhood_education/Private_early_childhood_education">Private early childhood education</a></p>     
        """.trimIndent()
        else
            """
                <p>Hyvä(t) huoltaja(t),</p>
                <p>Lapsenne esiopetushakemus on vastaanotettu. Hakemuksen tehnyt huoltaja voi muokata hakemusta osoitteessa <a href="https://www.espoonvarhaiskasvatus.fi">www.espoonvarhaiskasvatus.fi</a> siihen saakka, kunnes se on otettu käsittelyyn.</p>
                <p>Saatte tiedon lapsenne esiopetuspaikasta mahdollisimman pian, huomioiden hakemuksessa oleva aloitustoive ja alueen esiopetuspaikkatilanne.</p>
                <p>Päätös on nähtävissä ja hyväksyttävissä/hylättävissä <a href="https://www.espoonvarhaiskasvatus.fi">www.espoonvarhaiskasvatus.fi</a>.</p>
                <p>Hakiessanne esiopetusta palvelusetelipäiväkotiin, olkaa viimeistään hakemuksen jättämisen jälkeen yhteydessä suoraan kyseiseen yksikköön.</p>
                <p>Mikäli valitsitte hakemuksen kiireelliseksi, teidän tulee toimittaa hakemuksen liitteeksi todistus äkillisestä työllistymisestä uuteen työpaikkaan tai todistus äkillisesti saadusta uudesta opiskelupaikasta. Hakuaika on tällöin minimissään 2 viikkoa ja alkaa todistuksen saapumispäivämäärästä.</p>
                <p>Ympärivuorokautista- tai iltahoitoa hakiessanne, teidän tulee toimittaa molempien samassa taloudessa asuvien huoltajien todistukset työnantajalta vuorotyöstä tai oppilaitoksesta iltaisin tapahtuvasta opiskelusta. Hakemusta käsitellään vuorohoidon hakemuksena vasta kun edellä mainitut todistukset on toimitettu.</p>
                <p>Hakemuksen liitteet voi lisätä suoraan sähköiselle hakemukselle tai toimittaa postitse osoitteeseen Espoon kaupunki, Varhaiskasvatuksen palveluohjaus, PL 3125, 02070 Espoon kaupunki.</p>
                <p>Hakiessanne lapsellenne siirtoa uudella hakemuksella toiseen esiopetusyksikköön: Uusi esiopetuspäätös tehdään hakija- ja paikkatilanteen sen salliessa. Mikäli lapsen nykyinen esiopetuspaikka irtisanotaan, myös siirtohakemus poistuu.</p>
                <p>Palvelusetelin hakeminen: <a href="https://www.espoo.fi/fi-FI/Kasvatus_ja_opetus/Varhaiskasvatus/Hakeminen_varhaiskasvatukseen/Palveluseteli/Tietoa_perheille">Palveluseteli</a></p>
                <p>Hakeminen yksityisiin varhaiskasvatusyksiköihin: <a href="https://www.espoo.fi/fi-FI/Kasvatus_ja_opetus/Varhaiskasvatus/Yksityinen_varhaiskasvatus">Yksityinen varhaiskasvatus</a></p>
                <hr>
                <p>Bästa vårdnadshavare,</p>
                <p>Vi har tagit emot ansökan om förskoleundervisning för ditt barn. Den vårdnadshavare som har lämnat in ansökan kan redigera ansökan på adressen <a href="https://www.esbosmabarnspedagogik.fi">www.esbosmabarnspedagogik.fi</a> tills den har tagits upp till behandling.</p>
                <p>Du får information om ditt barns förskoleplats så snart som möjligt, med beaktande av önskemålet om startdatum och läget med förskoleplatser i området.</p>
                <p>Du kan se och godkänna/förkasta beslutet på adressen <a href="https://www.espoonvarhaiskasvatus.fi">www.espoonvarhaiskasvatus.fi</a>.</p>
                <p>När du ansöker om förskoleundervisning i ett servicesedeldaghem, kontakta enheten direkt senast efter att du lämnat ansökan.</p>
                <p>Om du valde att ansökan är brådskande, ska du till ansökan bifoga ett intyg över att du plötsligt fått ett nytt jobb eller en ny studieplats. Ansökningstiden är då minst två veckor och börjar den dag då intyget inkommer.</p>
                <p>När du ansöker om vård dygnet runt eller kvällstid, ska du lämna in arbetsgivarens intyg över skiftarbete eller läroanstaltens intyg över kvällsstudier för båda vårdnadshavarna som bor i samma hushåll. Ansökan behandlas som en ansökan om skiftomsorg först när de ovannämnda intygen har lämnats in.</p>
                <p>Bilagor till ansökan kan bifogas direkt till ansökan på webben eller skickas per post till adressen Esbo stad, Servicehänvisningen inom småbarnspedagogiken, PB 3125, 02070 Esbo stad.</p>
                <p>När du ansöker om överföring till en annan enhet för förskoleundervisning med en ny ansökan, fattas ett nytt beslut om förskoleundervisning om sökande- och platsläget tillåter det. Om barnets nuvarande förskoleplats sägs upp, slopas också ansökan om överföring.</p>
                <p>Ansökan om servicesedel: <a href="https://www.esbo.fi/sv-FI/Utbildning_och_fostran/Smabarnspedagogik/Privat_smabarnspedagogik/Servicesedel">Servicesedel</a></p>
                <p>Ansökan till privata enheter för småbarnspedagogik: <a href="https://www.esbo.fi/sv-FI/Utbildning_och_fostran/Smabarnspedagogik/Privat_smabarnspedagogik">Privat småbarnspedagogik</a></p>
                <hr>
                <p>Dear guardian(s),</p>
                <p>We have received your child’s application for pre-primary education. The guardian who submitted the application can make changes to it at <a href="https://www.espoonvarhaiskasvatus.fi">www.espoonvarhaiskasvatus.fi</a> until its processing starts.</p>
                <p>You will be informed of your child’s pre-primary education unit as soon as possible, taking into account the preferred starting date indicated in your application and the availability of pre-primary education places in your area.</p>
                <p>You can see the decision and accept/reject it at <a href="https://www.espoonvarhaiskasvatus.fi">www.espoonvarhaiskasvatus.fi</a>.</p>
                <p>When applying for pre-primary education at a service voucher day care centre, please contact the unit no later than after you have submitted the application.</p>
                <p>If you chose to have your application processed urgently, you will also need to provide a document as proof of sudden employment at a new workplace or a sudden offer of a new study place. In this case, the minimum application period is two (2) weeks and begins from the date on which the required document was received.</p>
                <p>When applying for round-the-clock or evening care, both guardians living in the same household need to provide a document issued by their employer concerning shift work or a document issued by their educational institution concerning evening studies. Your application will only be processed as an application for round-the-clock care after you have provided the required documents.</p>
                <p>You can add your supporting documents to your online application or send them by post to City of Espoo, Early childhood education service guidance, P.O. Box 3125, 02070 City of Espoo.</p>
                <p>When applying for a transfer to a different pre-primary education unit by submitting a new application; the new pre-primary education decision will be made when the situation with the applicants and the available places so permit. If your child’s current pre-primary education place is terminated, your transfer application will be deleted from the system.</p>
                <p>Information about applying for a service voucher: <a href="https://www.espoo.fi/en-US/Childcare_and_education/Early_childhood_education/Applying_for_early_childhood_education/Service_voucher/Information_for_families">Service Voucher</a></p>
                <p>Information about applying to private early childhood education units: <a href="https://www.espoo.fi/en-US/Childcare_and_education/Early_childhood_education/Private_early_childhood_education">Private early childhood education</a></p>          
            """.trimIndent()
    }

    private fun getTextForPreschool(withinApplicationPeriod: Boolean): String {
        return if (withinApplicationPeriod)
            """
                Hyvä(t) huoltaja(t),
                
                Lapsenne esiopetushakemus on vastaanotettu. Hakemuksen tehnyt huoltaja voi muokata hakemusta osoitteessa www.espoonvarhaiskasvatus.fi siihen saakka, kunnes hakemus on otettu käsittelyyn. 

                Päätökset tehdään hakuaikana (tammikuu) saapuneisiin hakemuksiin maaliskuun aikana.

                Päätös on nähtävissä ja hyväksyttävissä/hylättävissä www.espoonvarhaiskasvatus.fi.

                Hakiessanne palvelusetelipäiväkotiin, olkaa viimeistään hakemuksen jättämisen jälkeen yhteydessä suoraan kyseiseen yksikköön.

                Ympärivuorokautista- tai iltahoitoa hakiessanne, teidän tulee toimittaa molempien samassa taloudessa asuvien huoltajien todistukset työnantajalta vuorotyöstä tai oppilaitoksesta iltaisin tapahtuvasta opiskelusta. Hakemusta käsitellään vuorohoidon hakemuksena vasta kun edellä mainitut todistukset on toimitettu.

                Hakemuksen liitteet voi lisätä suoraan sähköiselle hakemukselle tai toimittaa postitse osoitteeseen Espoon kaupunki, Varhaiskasvatuksen palveluohjaus, PL 3125, 02070 Espoon kaupunki. 

                Palvelusetelin hakeminen: https://www.espoo.fi/fi-FI/Kasvatus_ja_opetus/Varhaiskasvatus/Hakeminen_varhaiskasvatukseen/Palveluseteli/Tietoa_perheille

                Hakeminen yksityisiin varhaiskasvatusyksiköihin: https://www.espoo.fi/fi-FI/Kasvatus_ja_opetus/Varhaiskasvatus/Yksityinen_varhaiskasvatus
            
                -----
                
                Bästa vårdnadshavare,
                
                Vi har tagit emot ansökan om förskoleundervisning för ditt barn. Den vårdnadshavare som har lämnat in ansökan kan redigera ansökan på adressen www.esbosmabarnspedagogik.fi tills den har tagits upp till behandling. 

                Om de ansökningar som kommit in under ansökningstiden (januari) fattas beslut i mars.

                Du kan se och godkänna/förkasta beslutet på adressen www.esbosmabarnspedagogik.fi.

                När du ansöker till ett servicesedeldaghem, kontakta daghemmet direkt senast efter att du lämnat ansökan.

                När du ansöker om vård dygnet runt eller kvällstid, ska du lämna in arbetsgivarens intyg över skiftarbete eller läroanstaltens intyg över kvällsstudier för båda vårdnadshavarna som bor i samma hushåll. Ansökan behandlas som en ansökan om skiftomsorg först när de ovannämnda intygen har lämnats in.

                Bilagor till ansökan kan bifogas direkt till ansökan på webben eller skickas per post till adressen Esbo stad, Servicehänvisningen inom småbarnspedagogiken, PB 3125, 02070 Esbo stad. 
                
                Ansökan om servicesedel: https://www.esbo.fi/sv-FI/Utbildning_och_fostran/Smabarnspedagogik/Privat_smabarnspedagogik/Servicesedel
                
                Ansökan till privata enheter för småbarnspedagogik: https://www.esbo.fi/sv-FI/Utbildning_och_fostran/Smabarnspedagogik/Privat_smabarnspedagogik
            
                -----
                
                Dear guardian(s),
                
                We have received your child’s application for pre-primary education. The guardian who submitted the application can make changes to it at until its processing starts. 

                The city will make decisions on applications received during the application period (January) in March.

                You can see the decision and accept/reject it at www.espoonvarhaiskasvatus.fi

                When applying to a service voucher day care centre, please contact the unit no later than after you have submitted the application.

                When applying for round-the-clock or evening care, both guardians living in the same household need to provide a document issued by their employer concerning shift work or a document issued by their educational institution concerning evening studies. Your application will only be processed as an application for round-the-clock care after you have provided the required documents.

                You can add your supporting documents to your online application or send them by post to City of Espoo, Early childhood education service guidance, P.O. Box 3125, 02070 City of Espoo. 

                Information about applying for a service voucher: https://www.espoo.fi/en-US/Childcare_and_education/Early_childhood_education/Applying_for_early_childhood_education/Service_voucher/Information_for_families

                Information about applying to private early childhood education units: https://www.espoo.fi/en-US/Childcare_and_education/Early_childhood_education/Private_early_childhood_education
            """.trimIndent()
        else
            """
                Hyvä(t) huoltaja(t),

                Lapsenne esiopetushakemus on vastaanotettu. Hakemuksen tehnyt huoltaja voi muokata hakemusta osoitteessa www.espoonvarhaiskasvatus.fi siihen saakka, kunnes se on otettu käsittelyyn.

                Saatte tiedon lapsenne esiopetuspaikasta mahdollisimman pian, huomioiden hakemuksessa oleva aloitustoive ja alueen esiopetuspaikkatilanne.

                Päätös on nähtävissä ja hyväksyttävissä/hylättävissä www.espoonvarhaiskasvatus.fi.

                Hakiessanne esiopetusta palvelusetelipäiväkotiin, olkaa viimeistään hakemuksen jättämisen jälkeen yhteydessä suoraan kyseiseen yksikköön.

                Mikäli valitsitte hakemuksen kiireelliseksi, teidän tulee toimittaa hakemuksen liitteeksi todistus äkillisestä työllistymisestä uuteen työpaikkaan tai todistus äkillisesti saadusta uudesta opiskelupaikasta. Hakuaika on tällöin minimissään 2 viikkoa ja alkaa todistuksen saapumispäivämäärästä.

                Ympärivuorokautista- tai iltahoitoa hakiessanne, teidän tulee toimittaa molempien samassa taloudessa asuvien huoltajien todistukset työnantajalta vuorotyöstä tai oppilaitoksesta iltaisin tapahtuvasta opiskelusta. Hakemusta käsitellään vuorohoidon hakemuksena vasta kun edellä mainitut todistukset on toimitettu.

                Hakemuksen liitteet voi lisätä suoraan sähköiselle hakemukselle tai toimittaa postitse osoitteeseen Espoon kaupunki, Varhaiskasvatuksen palveluohjaus, PL 3125, 02070 Espoon kaupunki.

                Hakiessanne lapsellenne siirtoa uudella hakemuksella toiseen esiopetusyksikköön: Uusi esiopetuspäätös tehdään hakija- ja paikkatilanteen sen salliessa. Mikäli lapsen nykyinen esiopetuspaikka irtisanotaan, myös siirtohakemus poistuu.

                Palvelusetelin hakeminen: https://www.espoo.fi/fi-FI/Kasvatus_ja_opetus/Varhaiskasvatus/Hakeminen_varhaiskasvatukseen/Palveluseteli/Tietoa_perheille

                Hakeminen yksityisiin varhaiskasvatusyksiköihin: https://www.espoo.fi/fi-FI/Kasvatus_ja_opetus/Varhaiskasvatus/Yksityinen_varhaiskasvatus
                
                -----
            
                Bästa vårdnadshavare,

                Vi har tagit emot ansökan om förskoleundervisning för ditt barn. Den vårdnadshavare som har lämnat in ansökan kan redigera ansökan på adressen www.esbosmabarnspedagogik.fi tills den har tagits upp till behandling.

                Du får information om ditt barns förskoleplats så snart som möjligt, med beaktande av önskemålet om startdatum och läget med förskoleplatser i området.

                Du kan se och godkänna/förkasta beslutet på adressen www.esbosmabarnspedagogik.fi.

                När du ansöker om förskoleundervisning i ett servicesedeldaghem, kontakta enheten direkt senast efter att du lämnat ansökan.

                Om du valde att ansökan är brådskande, ska du till ansökan bifoga ett intyg över att du plötsligt fått ett nytt jobb eller en ny studieplats. Ansökningstiden är då minst två veckor och börjar den dag då intyget inkommer.

                När du ansöker om vård dygnet runt eller kvällstid, ska du lämna in arbetsgivarens intyg över skiftarbete eller läroanstaltens intyg över kvällsstudier för båda vårdnadshavarna som bor i samma hushåll. Ansökan behandlas som en ansökan om skiftomsorg först när de ovannämnda intygen har lämnats in.

                Bilagor till ansökan kan bifogas direkt till ansökan på webben eller skickas per post till adressen Esbo stad, Servicehänvisningen inom småbarnspedagogiken, PB 3125, 02070 Esbo stad.

                När du ansöker om överföring till en annan enhet för förskoleundervisning med en ny ansökan, fattas ett nytt beslut om förskoleundervisning om sökande- och platsläget tillåter det. Om barnets nuvarande förskoleplats sägs upp, slopas också ansökan om överföring.

                Ansökan om servicesedel: https://www.esbo.fi/sv-FI/Utbildning_och_fostran/Smabarnspedagogik/Privat_smabarnspedagogik/Servicesedel

                Ansökan till privata enheter för småbarnspedagogik: https://www.esbo.fi/sv-FI/Utbildning_och_fostran/Smabarnspedagogik/Privat_smabarnspedagogik 
            
                -----
                
                Dear guardian(s),

                We have received your child’s application for pre-primary education. The guardian who submitted the application can make changes to it at www.espoonvarhaiskasvatus.fi until its processing starts.

                You will be informed of your child’s pre-primary education unit as soon as possible, taking into account the preferred starting date indicated in your application and the availability of pre-primary education places in your area.

                You can see the decision and accept/reject it at www.espoonvarhaiskasvatus.fi.

                When applying for pre-primary education at a service voucher day care centre, please contact the unit no later than after you have submitted the application.

                If you chose to have your application processed urgently, you will also need to provide a document as proof of sudden employment at a new workplace or a sudden offer of a new study place. In this case, the minimum application period is two (2) weeks and begins from the date on which the required document was received.

                When applying for round-the-clock or evening care, both guardians living in the same household need to provide a document issued by their employer concerning shift work or a document issued by their educational institution concerning evening studies. Your application will only be processed as an application for round-the-clock care after you have provided the required documents.

                You can add your supporting documents to your online application or send them by post to City of Espoo, Early childhood education service guidance, P.O. Box 3125, 02070 City of Espoo.

                When applying for a transfer to a different pre-primary education unit by submitting a new application; the new pre-primary education decision will be made when the situation with the applicants and the available places so permit. If your child’s current pre-primary education place is terminated, your transfer application will be deleted from the system.

                Information about applying for a service voucher: https://www.espoo.fi/en-US/Childcare_and_education/Early_childhood_education/Applying_for_early_childhood_education/Service_voucher/Information_for_families

                Information about applying to private early childhood education units: https://www.espoo.fi/en-US/Childcare_and_education/Early_childhood_education/Private_early_childhood_education
            """.trimIndent()
    }

    private fun getHtmlForClub(): String {
        return """
            <p>Hyvä(t) huoltaja(t),</p>

            <p>Lapsenne kerhohakemus on vastaanotettu.Hakemuksen tehnyt huoltaja voi muokata hakemusta osoitteessa <a href="www.espoonvarhaiskasvatus.fi">www.espoonvarhaiskasvatus.fi</a> siihen saakka, kunnes se on otettu käsittelyyn.</p>

            <p>Syksyllä alkaviin kerhoihin tehdään päätöksiä kevään aikana hakuajan (1-31.3.) päättymisen jälkeen paikkatilanteen mukaan.</p>

            <p>Kerhoihin voi hakea myös hakuajan jälkeen koko toimintavuoden ajan mahdollisesti vapautuville paikoille.</p>

            <p>Päätös on nähtävissä ja hyväksyttävissä/hylättävissä <a href="www.espoonvarhaiskasvatus.fi">www.espoonvarhaiskasvatus.fi</a>.</p>

            <p>Hakiessanne lapsellenne siirtoa uudella hakemuksella toiseen kerhoon. Uusi kerhopäätös tehdään paikkatilanteen sen salliessa. Hakemus on voimassa kuluvan kerhokauden. </p>
        """.trimIndent()
    }

    private fun getTextForClub(): String {
        return """
            Hyvä(t) huoltaja(t),

            Lapsenne kerhohakemus on vastaanotettu.Hakemuksen tehnyt huoltaja voi muokata hakemusta osoitteessa www.espoonvarhaiskasvatus.fi siihen saakka, kunnes se on otettu käsittelyyn.

            Syksyllä alkaviin kerhoihin tehdään päätöksiä kevään aikana hakuajan (1-31.3.) päättymisen jälkeen paikkatilanteen mukaan.

            Kerhoihin voi hakea myös hakuajan jälkeen koko toimintavuoden ajan mahdollisesti vapautuville paikoille.

            Päätös on nähtävissä ja hyväksyttävissä/hylättävissä www.espoonvarhaiskasvatus.fi.

            Hakiessanne lapsellenne siirtoa uudella hakemuksella toiseen kerhoon. Uusi kerhopäätös tehdään paikkatilanteen sen salliessa. Hakemus on voimassa kuluvan kerhokauden. 
        """.trimIndent()
    }

    private fun getHtmlForDaycare(): String {
        return """
            <p>
            Hyvä(t) huoltaja(t), <br>
            Lapsenne varhaiskasvatushakemus on vastaanotettu.
            </p>
            <p>
            Varhaiskasvatushakemuksella on <strong>neljän (4) kuukauden hakuaika.</strong> Hakemuksen tehnyt huoltaja voi muokata hakemusta osoitteessa <a href="https://www.espoonvarhaiskasvatus.fi">www.espoonvarhaiskasvatus.fi</a> siihen saakka, kunnes se on otettu käsittelyyn. 
            </p>
            <p>
            Saatte tiedon lapsenne varhaiskasvatuspaikasta noin kuukautta ennen hakemuksen lakisääteisen järjestelyajan päättymistä. Hakemuksen lakisääteinen järjestelyaika on neljä (4) kuukautta hakemuksen saapumisesta.
            </p>
            <p>
            Päätös on nähtävissä ja hyväksyttävissä/hylättävissä <a href="https://www.espoonvarhaiskasvatus.fi">www.espoonvarhaiskasvatus.fi</a>.
            </p>
            <p>
            Hakiessanne palvelusetelipäiväkotiin, olkaa viimeistään hakemuksen jättämisen jälkeen yhteydessä suoraan kyseiseen yksikköön.
            </p>
            <p>
            Mikäli valitsitte hakemuksen kiireelliseksi, teidän tulee toimittaa hakemuksen liitteeksi <strong>todistus äkillisestä työllistymisestä uuteen työpaikkaan tai todistus äkillisesti saadusta uudesta opiskelupaikasta.</strong> Hakuaika on tällöin <strong>minimissään 2 viikkoa</strong> ja alkaa todistuksen saapumispäivämäärästä.
            </p>
            <p>
            <strong>Ympärivuorokautista- tai iltahoitoa</strong> hakiessanne, teidän tulee toimittaa molempien samassa taloudessa asuvien huoltajien todistukset työnantajalta vuorotyöstä tai oppilaitoksesta iltaisin tapahtuvasta opiskelusta. <strong>Hakemusta käsitellään vuorohoidon hakemuksena vasta kun edellä mainitut todistukset on toimitettu.</strong>
            </p>
            <p>
            Hakemuksen liitteet voi lisätä suoraan sähköiselle hakemukselle tai toimittaa postitse osoitteeseen Espoon kaupunki, Varhaiskasvatuksen palveluohjaus, PL 3125, 02070 Espoon kaupunki
            </p>
            <p>
            Hakiessanne lapsellenne <strong>siirtoa</strong> toiseen <strong>kunnalliseen varhaiskasvatusyksikköön</strong>, hakemuksella ei ole hakuaikaa. Hakemus on voimassa vuoden hakemuksen saapumispäivämäärästä. Mikäli lapsen nykyinen paikka irtisanotaan, myös siirtohakemus poistuu.
            </p>
            <p>
            Palvelusetelin hakeminen: <a href="https://www.espoo.fi/fi-FI/Kasvatus_ja_opetus/Varhaiskasvatus/Hakeminen_varhaiskasvatukseen/Palveluseteli/Tietoa_perheille">https://www.espoo.fi/fi-FI/Kasvatus_ja_opetus/Varhaiskasvatus/Hakeminen_varhaiskasvatukseen/Palveluseteli/Tietoa_perheille</a>
            </p>
            <p>
            Hakeminen yksityisiin varhaiskasvatusyksiköihin: <a href="https://www.espoo.fi/fi-FI/Kasvatus_ja_opetus/Varhaiskasvatus/Yksityinen_varhaiskasvatus">https://www.espoo.fi/fi-FI/Kasvatus_ja_opetus/Varhaiskasvatus/Yksityinen_varhaiskasvatus</a>
            </p>
            
            <hr>
            
            <p>
            Bästa vårdnadshavare, <br>
            Vi har tagit emot en ansökan om småbarnspedagogik för ditt barn.
            </p>
            <p>
            Ansökan om småbarnspedagogik har en <strong>ansökningstid på fyra (4) månader.</strong> Den vårdnadshavare som har lämnat in ansökan kan redigera ansökan på adressen <a href="https://www.espoonvarhaiskasvatus.fi">www.espoonvarhaiskasvatus.fi</a> tills den har tagits upp till behandling.
            </p>
            <p>
            Du får information om platsen för småbarnspedagogik för ditt barn cirka en månad (1 månad) innan ansökningstiden för ansökan går ut. Du kan se och godkänna/förkasta beslutet på <a href="https://www.espoonvarhaiskasvatus.fi">www.espoonvarhaiskasvatus.fi</a>.
            </p>
            <p>
            När du ansöker plats till ett servicesedel daghem behöver du senast  vara i kontakt med daghemmet när du lämnat in ansökan till enheten i fråga.
            </p>
            <p>
            Om du valde att ansökan är brådskande, ska du bifoga ansökan <strong>ett intyg över att du plötsligt fått ett nytt jobb eller en ny studieplats.</strong> Ansökningstiden är då <strong>minst 2 veckor</strong> och börjar den dag då intyget inkom.
            </p>
            <p>
            När du ansöker om <strong>vård dygnet runt eller kvällstid</strong>, ska du lämna in arbetsgivarens intyg över skiftarbete eller läroanstaltens intyg över kvällsstudier för båda vårdnadshavarna som bor i samma hushåll. <strong>Ansökan behandlas som ansökan om skiftvård först när de ovannämnda intygen har lämnats in.</strong>
            </p>
            <p>
            När du ansöker om <strong>byte</strong> till en annan <strong>kommunal enhet för småbarnspedagogik</strong> har ansökan ingen ansökningstid. Ansökan gäller ett år från den dag då ansökan inkom. Om du säger upp barnets nuvarande plats, faller också ansökan om byte bort.
            </p>
            <p>
            Ansökan om servicesedel: <a href="https://www.esbo.fi/sv-FI/Utbildning_och_fostran/Smabarnspedagogik/Privat_smabarnspedagogik/Servicesedel/Information_till_familjer">https://www.esbo.fi/sv-FI/Utbildning_och_fostran/Smabarnspedagogik/Privat_smabarnspedagogik/Servicesedel/Information_till_familjer</a>
            </p>
            <p>
            Ansökan till privata enheter för småbarnspedagogik: <a href="https://www.esbo.fi/sv-FI/Utbildning_och_fostran/Smabarnspedagogik/Privat_smabarnspedagogik">https://www.esbo.fi/sv-FI/Utbildning_och_fostran/Smabarnspedagogik/Privat_smabarnspedagogik</a>
            </p>
            <p>
            Bilagorna till ansökan skickas antingen per post till adressen Esbo stad, Småbarnspedagogikens servicehänvisning, PB 3125, 02070 Esbo stad eller som e-postbilaga till <a href="mailto:dagis@esbo.fi">dagis@esbo.fi</a> (observera att förbindelsen inte är krypterad).
            </p>
            <p>
            Du kan göra ändringar i ansökan så länge den inte har tagits upp till behandling. Därefter kan du göra ändringar i ansökan genom att kontakta småbarnspedagogikens servicehänvisning (tfn 09 816 31000). Du kan återta en ansökan som du redan lämnat in genom att meddela detta per e-post till småbarnspedagogikens servicehänvisning <a href="mailto:dagis@esbo.fi">dagis@esbo.fi</a>
            </p>
            
            <hr>
            
            <p>
            Dear guardian(s), <br>
            We have received your child’s application for early childhood education. The <strong>application period</strong> for early childhood education applications is <strong>four (4) months</strong>. The guardian who submitted the application can make changes to it at <a href="https://www.espoonvarhaiskasvatus.fi">www.espoonvarhaiskasvatus.fi</a> until its processing starts.
            </p>
            <p>
            You will be informed of your child’s early childhood education unit approximately one month before the end of the statutory processing period. The statutory processing period of an application is four (4) months from the receipt of the application. You can see the decision and accept/reject it at <a href="https://www.espoonvarhaiskasvatus.fi">www.espoonvarhaiskasvatus.fi</a>. If you chose to have your application processed urgently, you will also need to provide <strong>a document as proof of sudden employment at a new workplace or a sudden offer of a new study place</strong>.
            </p>
            <p>
            In this case, the <strong>minimum application period is two (2) weeks</strong> and it begins from the date on which the required document was received. When applying for <strong>round-the-clock or evening care</strong>, both guardians living in the same household need to provide a document issued by their employer concerning shift work or a document issued by their educational institution concerning evening studies. <strong>Your application will only be processed as an application for round-the-clock care after you have provided the required documents</strong>.
            </p>
            <p>
            You can add your supporting documents to your online application or send them by post to City of Espoo, Early childhood education service guidance, P.O. Box 3125, 02070 City of Espoo or as an email attachment to <a href="mailto:varhaiskasvatuksen.palveluohjaus@espoo.fi">varhaiskasvatuksen.palveluohjaus@espoo.fi</a> (please note that the connection is not encrypted).
            </p>
            <p>
            When applying for a <strong>transfer</strong> to a different <strong>municipal early childhood education unit</strong>, your application will not have a specific application period. Your application will be valid for one (1) year from the date on which it was received. If your child’s current place is terminated, your transfer application will be deleted from the system.
            </p>
            <p>
            Information about applying to private early childhood education units: <a href="https://www.espoo.fi/en-US/Childcare_and_education/Early_childhood_education/Private_early_childhood_education">www.espoo.fi/en-US/Childcare_and_education/Early_childhood_education/Private_early_childhood_education</a>. You can send your supporting documents by post to City of Espoo, Early childhood education service guidance, P.O. Box 3125, 02070 City of Espoo or as an email attachment to <a href="mailto:varhaiskasvatuksen.palveluohjaus@espoo.fi">varhaiskasvatuksen.palveluohjaus@espoo.fi</a> (please note that the connection is not encrypted).
            </p>
        """.trimIndent()
    }

    private fun getTextForDaycare(): String {
        return """
Hyvä(t) huoltaja(t),

Lapsenne varhaiskasvatushakemus on vastaanotettu.

Varhaiskasvatushakemuksella on neljän (4) kuukauden hakuaika. Hakemuksen tehnyt huoltaja voi muokata hakemusta osoitteessa www.espoonvarhaiskasvatus.fi siihen saakka, kunnes se on otettu käsittelyyn.

Saatte tiedon lapsenne varhaiskasvatuspaikasta noin kuukautta ennen hakemuksen lakisääteisen järjestelyajan päättymistä. Hakemuksen lakisääteinen järjestelyaika on neljä (4) kuukautta hakemuksen saapumisesta.

Päätös on nähtävissä ja hyväksyttävissä/hylättävissä www.espoonvarhaiskasvatus.fi.

Hakiessanne palvelusetelipäiväkotiin, olkaa viimeistään hakemuksen jättämisen jälkeen yhteydessä suoraan kyseiseen yksikköön.

Mikäli valitsitte hakemuksen kiireelliseksi, teidän tulee toimittaa hakemuksen liitteeksi todistus äkillisestä työllistymisestä uuteen työpaikkaan tai todistus äkillisesti saadusta uudesta opiskelupaikasta. Hakuaika on tällöin minimissään 2 viikkoa ja alkaa todistuksen saapumispäivämäärästä.

Ympärivuorokautista- tai iltahoitoa hakiessanne, teidän tulee toimittaa molempien samassa taloudessa asuvien huoltajien todistukset työnantajalta vuorotyöstä tai oppilaitoksesta iltaisin tapahtuvasta opiskelusta. Hakemusta käsitellään vuorohoidon hakemuksena vasta kun edellä mainitut todistukset on toimitettu.

Hakemuksen liitteet voi lisätä suoraan sähköiselle hakemukselle tai toimittaa postitse osoitteeseen Espoon kaupunki, Varhaiskasvatuksen palveluohjaus, PL 3125, 02070 Espoon kaupunki

Hakiessanne lapsellenne siirtoa toiseen kunnalliseen varhaiskasvatusyksikköön, hakemuksella ei ole hakuaikaa. Hakemus on voimassa vuoden hakemuksen saapumispäivämäärästä. Mikäli lapsen nykyinen paikka irtisanotaan, myös siirtohakemus poistuu. 

Palvelusetelin hakeminen: https://www.espoo.fi/fi-FI/Kasvatus_ja_opetus/Varhaiskasvatus/Hakeminen_varhaiskasvatukseen/Palveluseteli/Tietoa_perheille

Hakeminen yksityisiin varhaiskasvatusyksiköihin: https://www.espoo.fi/fi-FI/Kasvatus_ja_opetus/Varhaiskasvatus/Yksityinen_varhaiskasvatus

-----

Bästa vårdnadshavare,

Vi har tagit emot en ansökan om småbarnspedagogik för ditt barn.

Ansökan om småbarnspedagogik har en ansökningstid på fyra (4) månader. Den vårdnadshavare som har lämnat in ansökan kan redigera ansökan på adressen www.espoonvarhaiskasvatus.fi tills den har tagits upp till behandling.

Du får information om platsen för småbarnspedagogik för ditt barn cirka en månad (1 månad) innan ansökningstiden för ansökan går ut. Du kan se och godkänna/förkasta beslutet på www.espoonvarhaiskasvatus.fi.

När du ansöker plats till ett servicesedel daghem behöver du senast  vara i kontakt med daghemmet när du lämnat in ansökan till enheten i fråga.

Om du valde att ansökan är brådskande, ska du bifoga ansökan ett intyg över att du plötsligt fått ett nytt jobb eller en ny studieplats. Ansökningstiden är då minst 2 veckor och börjar den dag då intyget inkom.

När du ansöker om vård dygnet runt eller kvällstid, ska du lämna in arbetsgivarens intyg över skiftarbete eller läroanstaltens intyg över kvällsstudier för båda vårdnadshavarna som bor i samma hushåll. Ansökan behandlas som ansökan om skiftvård först när de ovannämnda intygen har lämnats in.

När du ansöker om byte till en annan kommunal enhet för småbarnspedagogik har ansökan ingen ansökningstid. Ansökan gäller ett år från den dag då ansökan inkom. Om du säger upp barnets nuvarande plats, faller också ansökan om byte bort.

Ansökan om servicesedel: https://www.esbo.fi/sv-FI/Utbildning_och_fostran/Smabarnspedagogik/Privat_smabarnspedagogik/Servicesedel/Information_till_familjer

Ansökan till privata enheter för småbarnspedagogik: https://www.esbo.fi/sv-FI/Utbildning_och_fostran/Smabarnspedagogik/Privat_smabarnspedagogik

Bilagorna till ansökan skickas antingen per post till adressen Esbo stad, Småbarnspedagogikens servicehänvisning, PB 3125, 02070 Esbo stad eller som e-postbilaga till dagis@esbo.fi (observera att förbindelsen inte är krypterad).

Du kan göra ändringar i ansökan så länge den inte har tagits upp till behandling. Därefter kan du göra ändringar i ansökan genom att kontakta småbarnspedagogikens servicehänvisning (tfn 09 816 31000). Du kan återta en ansökan som du redan lämnat in genom att meddela detta per e-post till småbarnspedagogikens servicehänvisning dagis@esbo.fi
            
-----

Dear guardian(s),

We have received your child’s application for early childhood education. The application period for early childhood education applications is four (4) months. The guardian who submitted the application can make changes to it at www.espoonvarhaiskasvatus.fi until its processing starts.

You will be informed of your child’s early childhood education unit approximately one month before the end of the statutory processing period. The statutory processing period of an application is four (4) months from the receipt of the application. You can see the decision and accept/reject it at www.espoonvarhaiskasvatus.fi. If you chose to have your application processed urgently, you will also need to provide a document as proof of sudden employment at a new workplace or a sudden offer of a new study place.

In this case, the minimum application period is two (2) weeks and it begins from the date on which the required document was received. When applying for round-the-clock or evening care, both guardians living in the same household need to provide a document issued by their employer concerning shift work or a document issued by their educational institution concerning evening studies. Your application will only be processed as an application for round-the-clock care after you have provided the required documents.

You can add your supporting documents to your online application or send them by post to City of Espoo, Early childhood education service guidance, P.O. Box 3125, 02070 City of Espoo or as an email attachment to varhaiskasvatuksen.palveluohjaus@espoo.fi (please note that the connection is not encrypted).

When applying for a transfer to a different municipal early childhood education unit, your application will not have a specific application period. Your application will be valid for one (1) year from the date on which it was received. If your child’s current place is terminated, your transfer application will be deleted from the system.

Information about applying to private early childhood education units: www.espoo.fi/en-US/Childcare_and_education/Early_childhood_education/Private_early_childhood_education. You can send your supporting documents by post to City of Espoo, Early childhood education service guidance, P.O. Box 3125, 02070 City of Espoo or as an email attachment to varhaiskasvatuksen.palveluohjaus@espoo.fi (please note that the connection is not encrypted).
"""
    }
}
