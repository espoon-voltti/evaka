// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.emailclient.IEmailClient
import fi.espoo.evaka.emailclient.IEmailMessageProvider
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.shared.DecisionId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import java.time.Duration
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class PendingDecisionEmailService(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val emailClient: IEmailClient,
    private val emailMessageProvider: IEmailMessageProvider,
    env: EmailEnv
) {
    init {
        asyncJobRunner.registerHandler(::doSendPendingDecisionsEmail)
    }

    val senderAddress: String = env.senderAddress
    val senderNameFi: String = env.senderNameFi
    val senderNameSv: String = env.senderNameSv

    fun getFromAddress(language: Language) =
        when (language) {
            Language.sv -> "$senderNameSv <$senderAddress>"
            else -> "$senderNameFi <$senderAddress>"
        }

    fun doSendPendingDecisionsEmail(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.SendPendingDecisionEmail
    ) {
        logger.info("Sending pending decision reminder email to guardian ${msg.guardianId}")
        sendPendingDecisionEmail(db, clock, msg)
    }

    data class GuardianDecisions(val guardianId: PersonId, val decisionIds: List<DecisionId>)

    fun scheduleSendPendingDecisionsEmails(db: Database.Connection, clock: EvakaClock): Int {
        val jobCount =
            db.transaction { tx ->
                tx.createUpdate(
                        "DELETE FROM async_job WHERE type = 'SEND_PENDING_DECISION_EMAIL' AND claimed_by IS NULL"
                    )
                    .execute()

                val pendingGuardianDecisions =
                    tx.createQuery(
                            """
WITH pending_decisions AS (
SELECT id, application_id
FROM decision d
WHERE d.status = 'PENDING'
AND d.resolved IS NULL
AND (d.sent_date < :today - INTERVAL '1 week' AND d.sent_date > :today - INTERVAL '2 month')
AND d.pending_decision_emails_sent_count < 2
AND (d.pending_decision_email_sent IS NULL OR d.pending_decision_email_sent < :today - INTERVAL '1 week'))
SELECT application.guardian_id as guardian_id, array_agg(pending_decisions.id::uuid) AS decision_ids
FROM pending_decisions JOIN application ON pending_decisions.application_id = application.id
GROUP BY application.guardian_id
"""
                        )
                        .bind("today", clock.today())
                        .mapTo<GuardianDecisions>()
                        .list()

                val createdJobCount =
                    pendingGuardianDecisions.fold(0) { count, pendingDecision ->
                        tx.getPersonById(pendingDecision.guardianId).let { guardian ->
                            when {
                                guardian == null -> {
                                    logger.warn(
                                        "Could not send pending decision email to guardian ${pendingDecision.guardianId}: guardian not found"
                                    )
                                    count
                                }
                                guardian.email.isNullOrBlank() -> {
                                    logger.warn(
                                        "Could not send pending decision email to guardian ${guardian.id}: invalid email"
                                    )
                                    count
                                }
                                else -> {
                                    asyncJobRunner.plan(
                                        tx,
                                        payloads =
                                            listOf(
                                                AsyncJob.SendPendingDecisionEmail(
                                                    guardianId = pendingDecision.guardianId,
                                                    email = guardian.email,
                                                    language = guardian.language,
                                                    decisionIds = pendingDecision.decisionIds
                                                )
                                            ),
                                        runAt = clock.now(),
                                        retryCount = 3,
                                        retryInterval = Duration.ofHours(1)
                                    )
                                    count + 1
                                }
                            }
                        }
                    }

                logger.info(
                    "PendingDecisionEmailService: Scheduled sending $createdJobCount pending decision emails"
                )
                createdJobCount
            }

        return jobCount
    }

    fun sendPendingDecisionEmail(
        db: Database.Connection,
        clock: EvakaClock,
        pendingDecision: AsyncJob.SendPendingDecisionEmail
    ) {
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
SET pending_decision_emails_sent_count = pending_decision_emails_sent_count + 1, pending_decision_email_sent = :now
WHERE id = :id
                    """.trimIndent(
                        )
                    )
                    .bind("now", clock.now())
                    .bind("id", decisionId)
                    .execute()
            }
        }
    }

    private fun getLanguage(languageStr: String?): Language {
        return when (languageStr) {
            "sv",
            "SV" -> Language.sv
            "en",
            "EN" -> Language.en
            else -> Language.fi
        }
    }
}
