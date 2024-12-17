// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.emailclient.Email
import fi.espoo.evaka.emailclient.EmailClient
import fi.espoo.evaka.emailclient.IEmailMessageProvider
import fi.espoo.evaka.pis.EmailMessageType
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.shared.DecisionId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.AsyncJobType
import fi.espoo.evaka.shared.async.removeUnclaimedJobs
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import java.time.Duration
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class PendingDecisionEmailService(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val emailClient: EmailClient,
    private val emailMessageProvider: IEmailMessageProvider,
    private val emailEnv: EmailEnv,
) {
    init {
        asyncJobRunner.registerHandler(::doSendPendingDecisionsEmail)
    }

    fun doSendPendingDecisionsEmail(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.SendPendingDecisionEmail,
    ) {
        logger.info("Sending pending decision reminder email to guardian ${msg.guardianId}")
        sendPendingDecisionEmail(db, clock, msg)
    }

    data class GuardianDecisions(val guardianId: PersonId, val decisionIds: List<DecisionId>)

    fun scheduleSendPendingDecisionsEmails(db: Database.Connection, clock: EvakaClock): Int {
        val jobCount =
            db.transaction { tx ->
                tx.removeUnclaimedJobs(
                    setOf(AsyncJobType(AsyncJob.SendPendingDecisionEmail::class))
                )

                val today = clock.today()
                val pendingGuardianDecisions =
                    tx.createQuery {
                            sql(
                                """
WITH pending_decisions AS (
SELECT id, application_id
FROM decision d
WHERE d.status = 'PENDING'
AND d.resolved IS NULL
AND (d.sent_date < ${bind(today)} - INTERVAL '1 week' AND d.sent_date > ${bind(today)} - INTERVAL '2 month')
AND d.pending_decision_emails_sent_count < 4
AND (d.pending_decision_email_sent IS NULL OR d.pending_decision_email_sent < ${bind(today)} - INTERVAL '1 week')),
pending_decisions_and_guardians AS (
    SELECT pending_decisions.id AS decision_id, application.guardian_id
    FROM pending_decisions
    JOIN application ON pending_decisions.application_id = application_id
    UNION
    SELECT pending_decisions.id AS decision_id, other_guardian.id AS guardian_id
    FROM pending_decisions
    JOIN application a ON pending_decisions.application_id = a.id
    JOIN application_other_guardian aog ON a.id = aog.application_id
    JOIN person other_guardian ON aog.guardian_id = other_guardian.id
    JOIN person guardian ON a.guardian_id = guardian.id
    JOIN person child ON a.child_id = child.id
    WHERE allow_other_guardian_access IS TRUE
    AND (
        EXISTS (SELECT FROM guardian g WHERE g.guardian_id = aog.guardian_id AND g.child_id = a.child_id)
        OR EXISTS (SELECT FROM foster_parent fp WHERE fp.parent_id = aog.guardian_id AND fp.child_id = a.child_id AND valid_during @> ${bind(today)})
    )
    AND NOT other_guardian.restricted_details_enabled
    AND NOT guardian.restricted_details_enabled
    AND NOT child.restricted_details_enabled
    AND other_guardian.street_address NOT ILIKE '%poste restante%'
    AND guardian.street_address NOT ILIKE '%poste restante%'
    AND child.street_address NOT ILIKE '%poste restante%'
    AND (
        (trim(other_guardian.residence_code) != '' AND
         trim(guardian.residence_code) != '' AND
         other_guardian.residence_code = guardian.residence_code) OR
        (trim(other_guardian.street_address) != '' AND
         trim(guardian.street_address) != '' AND
         trim(other_guardian.postal_code) != '' AND
         trim(guardian.postal_code) != '' AND
         lower(other_guardian.street_address) = lower(guardian.street_address) AND
         other_guardian.postal_code = guardian.postal_code)
    )
    AND (
        (trim(other_guardian.residence_code) != '' AND
         trim(child.residence_code) != '' AND
         other_guardian.residence_code = child.residence_code) OR
        (trim(other_guardian.street_address) != '' AND
         trim(child.street_address) != '' AND
         trim(other_guardian.postal_code) != '' AND
         trim(child.postal_code) != '' AND
         lower(other_guardian.street_address) = lower(child.street_address) AND
         other_guardian.postal_code = child.postal_code)
    )
)
SELECT guardian_id, array_agg(decision_id::uuid) AS decision_ids
FROM pending_decisions_and_guardians
GROUP BY guardian_id
"""
                            )
                        }
                        .toList<GuardianDecisions>()

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
                                                    language = guardian.language,
                                                    decisionIds = pendingDecision.decisionIds,
                                                )
                                            ),
                                        runAt = clock.now(),
                                        retryCount = 3,
                                        retryInterval = Duration.ofHours(1),
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
        pendingDecision: AsyncJob.SendPendingDecisionEmail,
    ) {
        logger.info("Sending pending decision email to guardian ${pendingDecision.guardianId}")
        val lang = getLanguage(pendingDecision.language)

        Email.create(
                db,
                pendingDecision.guardianId,
                EmailMessageType.DECISION_NOTIFICATION,
                emailEnv.sender(lang),
                emailMessageProvider.pendingDecisionNotification(lang),
                "${pendingDecision.guardianId} - ${pendingDecision.decisionIds.joinToString("-")}",
            )
            ?.also { emailClient.send(it) }

        val now = clock.now()
        db.transaction { tx ->
            // Mark as sent even if the recipient didn't want the email, to stop sending reminders
            // when the count reaches a threshold
            pendingDecision.decisionIds.forEach { decisionId ->
                tx.execute {
                    sql(
                        """
UPDATE decision
SET pending_decision_emails_sent_count = pending_decision_emails_sent_count + 1, pending_decision_email_sent = ${bind(now)}
WHERE id = ${bind(decisionId)}
"""
                    )
                }
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
