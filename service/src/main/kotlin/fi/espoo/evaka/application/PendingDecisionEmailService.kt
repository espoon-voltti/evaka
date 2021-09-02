// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.emailclient.IEmailClient
import fi.espoo.evaka.emailclient.IEmailMessageProvider
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.SendPendingDecisionEmail
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import mu.KotlinLogging
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.UUID

private val logger = KotlinLogging.logger { }

@Service
class PendingDecisionEmailService(
    private val asyncJobRunner: AsyncJobRunner,
    private val emailClient: IEmailClient,
    private val emailMessageProvider: IEmailMessageProvider,
    env: EmailEnv
) {
    init {
        asyncJobRunner.sendPendingDecisionEmail = ::doSendPendingDecisionsEmail
    }

    val senderAddress: String = env.senderAddress
    val senderNameFi: String = env.senderNameFi
    val senderNameSv: String = env.senderNameSv

    fun getFromAddress(language: Language) = when (language) {
        Language.sv -> "$senderNameSv <$senderAddress>"
        else -> "$senderNameFi <$senderAddress>"
    }

    fun doSendPendingDecisionsEmail(db: Database, msg: SendPendingDecisionEmail) {
        logger.info("Sending pending decision reminder email to guardian ${msg.guardianId}")
        sendPendingDecisionEmail(db, msg)
    }

    data class GuardianDecisions(
        val guardianId: UUID,
        val decisionIds: List<UUID>
    )

    fun scheduleSendPendingDecisionsEmails(db: Database.Connection): Int {
        val jobCount = db.transaction { tx ->
            tx.createUpdate("DELETE FROM async_job WHERE type = 'SEND_PENDING_DECISION_EMAIL' AND claimed_by IS NULL")
                .execute()

            val pendingGuardianDecisions = tx.createQuery(
                """
WITH pending_decisions AS (
SELECT id, application_id
FROM decision d
WHERE d.status = 'PENDING'
AND d.resolved IS NULL
AND (d.sent_date < current_date - INTERVAL '1 week' AND d.sent_date > current_date - INTERVAL '2 month')
AND d.pending_decision_emails_sent_count < 2
AND (d.pending_decision_email_sent IS NULL OR d.pending_decision_email_sent < current_date - INTERVAL '1 week'))
SELECT application.guardian_id as guardian_id, array_agg(pending_decisions.id::uuid) AS decision_ids
FROM pending_decisions JOIN application ON pending_decisions.application_id = application.id
GROUP BY application.guardian_id
"""
            )
                .mapTo<GuardianDecisions>()
                .list()

            var createdJobCount = 0
            pendingGuardianDecisions.forEach { pendingDecision ->
                tx.getPersonById(pendingDecision.guardianId)?.let { guardian ->
                    if (!guardian.email.isNullOrBlank()) {
                        asyncJobRunner.plan(
                            tx,
                            payloads = listOf(
                                SendPendingDecisionEmail(
                                    guardianId = pendingDecision.guardianId,
                                    email = guardian.email,
                                    language = guardian.language,
                                    decisionIds = pendingDecision.decisionIds
                                )
                            ),
                            runAt = HelsinkiDateTime.now(),
                            retryCount = 3,
                            retryInterval = Duration.ofHours(1)
                        )
                        createdJobCount++
                    } else {
                        logger.warn("Could not send pending decision email to guardian ${guardian.id}: invalid email")
                    }
                }
            }

            logger.info("PendingDecisionEmailService: Scheduled sending $createdJobCount pending decision emails")
            createdJobCount
        }

        asyncJobRunner.scheduleImmediateRun()

        return jobCount
    }

    fun sendPendingDecisionEmail(db: Database, pendingDecision: SendPendingDecisionEmail) {
        db.transaction { tx ->
            logger.info("Sending pending decision email to guardian ${pendingDecision.guardianId}")
            val lang = getLanguage(pendingDecision.language)

            emailClient.sendEmail(
                "${pendingDecision.guardianId} - ${pendingDecision.decisionIds.joinToString("-")}",
                pendingDecision.email,
                getFromAddress(lang),
                emailMessageProvider.getPendingDecisionEmailSubject(),
                emailMessageProvider.getPendingDecisionEmailHtml(),
                emailMessageProvider.getPendingDecisionEmailText()
            )

            // Mark as sent
            pendingDecision.decisionIds.forEach { decisionId ->
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
        }
    }

    private fun getLanguage(languageStr: String?): Language {
        return when (languageStr) {
            "sv", "SV" -> Language.sv
            "en", "EN" -> Language.en
            else -> Language.fi
        }
    }
}
