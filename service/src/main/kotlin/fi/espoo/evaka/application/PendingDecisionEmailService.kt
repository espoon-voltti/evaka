// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.decision.getDecision
import fi.espoo.evaka.emailclient.IEmailClient
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.SendPendingDecisionEmail
import fi.espoo.evaka.shared.db.Database
import mu.KotlinLogging
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger { }

@Service
class PendingDecisionEmailService(
    private val asyncJobRunner: AsyncJobRunner,
    private val emailClient: IEmailClient,
    private val env: Environment
) {
    init {
        asyncJobRunner.sendPendingDecisionEmail = ::doSendPendingDecisionsEmail
    }

    val fromAddress = env.getProperty("mail_reply_to_address", "no-reply.evaka@espoo.fi")

    fun doSendPendingDecisionsEmail(db: Database, msg: SendPendingDecisionEmail) {
        logger.info("Sending pending decision reminder email to decision ${msg.decisionId}")
        sendPendingDecisionEmail(db, msg.decisionId)
    }

    fun scheduleSendPendingDecisionsEmails(db: Database.Connection): Int {
        val jobCount = db.transaction { tx ->
            tx.createUpdate("DELETE FROM async_job WHERE type = 'SEND_PENDING_DECISION_EMAIL' AND (claimed_by IS NULL OR completed_at IS NOT NULL)")
                .execute()

            val pendingDecisions = tx.createQuery(
                """
SELECT DISTINCT(d.id)
FROM decision d
WHERE d.status = 'PENDING'
AND d.resolved IS NULL
AND d.sent_date + INTERVAL '1 week' < current_date
AND d.pending_decision_emails_sent_count < 2
AND d.pending_decision_email_sent IS NULL or d.pending_decision_email_sent < current_date - INTERVAL '1 week'
"""
            )
                .mapTo<UUID>()
                .list()

            logger.info("PendingDecisionEmailService: Scheduling sending ${pendingDecisions.size} pending decision emails")

            pendingDecisions.forEach { decisionId ->
                asyncJobRunner.plan(
                    tx,
                    payloads = listOf(
                        SendPendingDecisionEmail(
                            decisionId = decisionId
                        )
                    ),
                    runAt = Instant.now(),
                    retryCount = 10
                )
            }

            pendingDecisions.size
        }

        asyncJobRunner.scheduleImmediateRun()

        return jobCount
    }

    fun sendPendingDecisionEmail(db: Database, decisionId: UUID) {
        db.read { tx ->
            getDecision(tx.handle, decisionId)?.let { decision ->
                fetchApplicationDetails(tx.handle, decision.applicationId)?.let { application ->
                    tx.handle.getPersonById(application.guardianId)?.let { guardian ->
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

                            // Mark as sent
                            db.transaction { tx ->
                                tx.createUpdate(
                                    """
UPDATE decision
SET pending_decision_emails_sent_count = pending_decision_emails_sent_count + 1, pending_decision_email_sent = now()
WHERE id = :id
                                    """.trimIndent()
                                )
                                    .bind("id", decisionId)
                                    .execute()
                            }
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
