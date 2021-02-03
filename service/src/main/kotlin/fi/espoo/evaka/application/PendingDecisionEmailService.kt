import fi.espoo.evaka.application.fetchApplicationDetails
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.decision.getDecision
import fi.espoo.evaka.emailclient.EmailClient
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.transaction
import mu.KotlinLogging
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.util.UUID

private val logger = KotlinLogging.logger { }

@Service
class PendingDecisionEmailService(
    private val emailClient: EmailClient,
    private val env: Environment
) {
    val fromAddress = env.getProperty("mail_reply_to_address", "no-reply.evaka@espoo.fi")

    fun sendPendingDecisionEmail(tx: Database.Read, decisionId: UUID) {
        tx.handle.transaction { h ->
            getDecision(h, decisionId)?.let { decision ->
                fetchApplicationDetails(h, decision.applicationId)?.let { application ->
                    h.getPersonById(application.guardianId)?.let { guardian ->
                        if (!guardian.email.isNullOrBlank()) {
                            logger.info("Sending pending decision email to guardian ${guardian.id}")

                            val lang = getLanguage(guardian.language)
                            emailClient.sendEmail(
                                decisionId.toString(),
                                guardian.email,
                                fromAddress,
                                getSubject(lang),
                                getHtml(lang),
                                getText(lang)
                            )
                        } else {
                            logger.warn("Could not send pending decision email to guardian ${guardian.id}: invalid email")
                        }
                    } ?: logger.warn("Could not send pending decision email for application ${application.id}: guardian cannot be found")
                } ?: logger.warn("Could not send pending decision email for decision ${decision.id}: application could not be found")
            } ?: logger.warn("Could not send pending decision email for decision $decisionId: decision missing")
        }
    }

    private fun getLanguage(languageStr: String?): Language {
        return when (languageStr) {
            "sv", "SV" -> Language.sv
            "en", "EN" -> Language.en
            else -> Language.fi
        }
    }

    private fun getSubject(language: Language): String {
        return when (language) {
            Language.en -> "Decision on early childhood education"
            Language.sv -> "Beslut om förskoleundervisning"
            else -> "Päätös varhaiskasvatuksesta"
        }
    }

    private fun getHtml(language: Language): String {
        return when (language) {
            Language.en -> """
<p>
You have an unanswered decision from Espoo’s early childhood education. The decision must be accepted or rejected within two weeks of receiving it.
</p>
<p>
The person who submitted the application can accept or reject an unanswered decision by logging in to <a href="espoonvarhaiskasvatus.fi">espoonvarhaiskasvatus.fi</a> or by sending the completed form on the last page of the decision to the address specified on the page.
</p>
<p>
You cannot reply to this message. If you have questions, please contact early childhood education service counselling, tel. 09 816 31000.                
</p>
            """.trimIndent()
            Language.sv -> """
<p>
Du har ett obesvarat beslut av småbarnspedagogiken i Esbo. Beslutet ska godkännas eller förkastas inom två veckor från att det inkommit.
</p>
<p>
Den som lämnat in ansökan kan godkänna eller förkasta obesvarade beslut genom att logga in på adressen <a href="https://espoonvarhaiskasvatus.fi">espoonvarhaiskasvatus.fi</a> eller genom att returnera den ifyllda blanketten som finns på sista sidan av beslutet till den adress som nämns på sidan.
</p>
<p>
Detta meddelande kan inte besvaras. Kontakta vid behov servicehänvisningen inom småbarnspedagogiken, tfn 09 816 7600
</p>                
            """.trimIndent()
            else -> """
<p>
Sinulla on vastaamaton päätös Espoon varhaiskasvatukselta. Päätös tulee hyväksyä tai hylätä kahden viikon sisällä sen saapumisesta.
</p>
<p>
Hakemuksen tekijä voi hyväksyä tai hylätä vastaamattomat päätökset kirjautumalla osoitteeseen <a href="https://espoonvarhaiskasvatus.fi">espoonvarhaiskasvatus.fi</a>, tai palauttamalla täytetyn lomakkeen päätöksen viimeiseltä sivulta siinä mainittuun osoitteeseen.
</p>
<p>
Tähän viestiin ei voi vastata. Tarvittaessa ole yhteydessä varhaiskasvatuksen palveluohjaukseen p. 09 816 31000
</p>                
            """.trimIndent()
        }
    }

    private fun getText(language: Language): String {
        return when (language) {
            Language.en -> """
You have an unanswered decision from Espoo’s early childhood education. The decision must be accepted or rejected within two weeks of receiving it.

The person who submitted the application can accept or reject an unanswered decision by logging in to espoonvarhaiskasvatus.fi or by sending the completed form on the last page of the decision to the address specified on the page.

You cannot reply to this message. If you have questions, please contact early childhood education service counselling, tel. 09 816 31000.                
            """.trimIndent()
            Language.sv -> """
Du har ett obesvarat beslut av småbarnspedagogiken i Esbo. Beslutet ska godkännas eller förkastas inom två veckor från att det inkommit.

Den som lämnat in ansökan kan godkänna eller förkasta obesvarade beslut genom att logga in på adressen https://espoonvarhaiskasvatus.fi eller genom att returnera den ifyllda blanketten som finns på sista sidan av beslutet till den adress som nämns på sidan.

Detta meddelande kan inte besvaras. Kontakta vid behov servicehänvisningen inom småbarnspedagogiken, tfn 09 816 7600                
            """.trimIndent()
            else -> """
Sinulla on vastaamaton päätös Espoon varhaiskasvatukselta. Päätös tulee hyväksyä tai hylätä kahden viikon sisällä sen saapumisesta.

Hakemuksen tekijä voi hyväksyä tai hylätä vastaamattomat päätökset kirjautumalla osoitteeseen https://espoonvarhaiskasvatus.fi, tai palauttamalla täytetyn lomakkeen päätöksen viimeiseltä sivulta siinä mainittuun osoitteeseen.

Tähän viestiin ei voi vastata. Tarvittaessa ole yhteydessä varhaiskasvatuksen palveluohjaukseen p. 09 816 31000                
            """.trimIndent()
        }
    }
}
