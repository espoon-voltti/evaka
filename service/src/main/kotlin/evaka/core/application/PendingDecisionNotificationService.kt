// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.application

import evaka.core.EmailEnv
import evaka.core.daycare.domain.Language
import evaka.core.emailclient.Email
import evaka.core.emailclient.EmailClient
import evaka.core.emailclient.IEmailMessageProvider
import evaka.core.pis.NotificationCategory
import evaka.core.pis.getPersonById
import evaka.core.shared.DecisionId
import evaka.core.shared.PersonId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.async.AsyncJobType
import evaka.core.shared.async.removeUnclaimedJobs
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.webpush.CitizenPushNotification
import evaka.core.webpush.CitizenPushNotifications
import evaka.core.webpush.PendingDecision
import evaka.core.webpush.hasCitizenPushSubscriptions
import evaka.core.webpush.pushChildName
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Duration
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class PendingDecisionNotificationService(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val emailClient: EmailClient,
    private val citizenPushNotifications: CitizenPushNotifications,
    private val emailMessageProvider: IEmailMessageProvider,
    private val emailEnv: EmailEnv,
) {
    init {
        asyncJobRunner.registerHandler(::doSendPendingDecisionNotification)
    }

    fun doSendPendingDecisionNotification(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.SendPendingDecisionNotification,
    ) {
        logger.info { "Sending pending decision reminder to guardian ${msg.guardianId}" }
        sendPendingDecisionNotification(db, clock, msg)
    }

    data class GuardianDecisions(val guardianId: PersonId, val decisionIds: List<DecisionId>)

    @IgnorableReturnValue
    fun schedulePendingDecisionNotifications(db: Database.Connection, clock: EvakaClock): Int {
        val jobCount = db.transaction { tx ->
            tx.removeUnclaimedJobs(
                setOf(AsyncJobType(AsyncJob.SendPendingDecisionNotification::class))
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
AND d.pending_decision_emails_sent_count < 2
AND (d.pending_decision_email_sent IS NULL OR d.pending_decision_email_sent < ${bind(today)} - INTERVAL '1 week'))
SELECT application.guardian_id as guardian_id, array_agg(pending_decisions.id::uuid) AS decision_ids
FROM pending_decisions
JOIN application ON pending_decisions.application_id = application.id
WHERE EXISTS (
    SELECT FROM guardian WHERE guardian_id = application.guardian_id AND child_id = application.child_id
) OR EXISTS (
    SELECT FROM foster_parent WHERE parent_id = application.guardian_id AND child_id = application.child_id AND valid_during @> ${bind(today)}
)
GROUP BY application.guardian_id
"""
                        )
                    }
                    .toList<GuardianDecisions>()

            val createdJobCount =
                pendingGuardianDecisions.fold(0) { count, pendingDecision ->
                    tx.getPersonById(pendingDecision.guardianId).let { guardian ->
                        when {
                            guardian == null -> {
                                logger.warn {
                                    "Could not send pending decision reminder to guardian ${pendingDecision.guardianId}: guardian not found"
                                }
                                count
                            }

                            guardian.email.isNullOrBlank() &&
                                !tx.hasCitizenPushSubscriptions(guardian.id) -> {
                                logger.warn {
                                    "Could not send pending decision reminder to guardian ${guardian.id}: no email address or push subscription"
                                }
                                count
                            }

                            else -> {
                                asyncJobRunner.plan(
                                    tx,
                                    payloads =
                                        listOf(
                                            AsyncJob.SendPendingDecisionNotification(
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

            logger.info {
                "PendingDecisionNotificationService: Scheduled sending $createdJobCount pending decision reminders"
            }
            createdJobCount
        }

        return jobCount
    }

    fun sendPendingDecisionNotification(
        db: Database.Connection,
        clock: EvakaClock,
        pendingDecision: AsyncJob.SendPendingDecisionNotification,
    ) {
        logger.info {
            "Sending pending decision reminder to guardian ${pendingDecision.guardianId}"
        }
        val lang = getLanguage(pendingDecision.language)

        Email.create(
                db,
                pendingDecision.guardianId,
                NotificationCategory.DECISION_NOTIFICATION,
                emailEnv.sender(lang),
                emailMessageProvider.pendingDecisionNotification(lang),
                "${pendingDecision.guardianId} - ${pendingDecision.decisionIds.joinToString("-")}",
            )
            ?.also { emailClient.send(it) }
        db.transaction { tx ->
            citizenPushNotifications.plan(
                tx,
                clock.now(),
                pendingDecision.guardianId,
                CitizenPushNotification.PendingDecisions(
                    tx.getPendingDecisionsForPush(pendingDecision.decisionIds)
                ),
            )
        }

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

private fun Database.Read.getPendingDecisionsForPush(
    decisionIds: List<DecisionId>
): List<PendingDecision> = createQuery {
    sql(
        """
SELECT p.first_name, p.last_name, d.type, u.name AS unit_name
FROM decision d
JOIN application a ON d.application_id = a.id
JOIN person p ON a.child_id = p.id
JOIN daycare u ON d.unit_id = u.id
WHERE d.id = ANY(${bind(decisionIds)})
ORDER BY d.sent_date, p.last_name, p.first_name, d.type
"""
    )
}
    .toList {
        PendingDecision(
            childName = pushChildName(column("first_name"), column("last_name")),
            type = column("type"),
            unitName = column("unit_name"),
        )
    }
