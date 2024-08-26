// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pedagogicaldocument

import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.emailclient.Email
import fi.espoo.evaka.emailclient.EmailClient
import fi.espoo.evaka.emailclient.IEmailMessageProvider
import fi.espoo.evaka.pis.EmailMessageType
import fi.espoo.evaka.shared.PedagogicalDocumentId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class PedagogicalDocumentNotificationService(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val emailClient: EmailClient,
    private val emailMessageProvider: IEmailMessageProvider,
    private val emailEnv: EmailEnv,
) {
    init {
        asyncJobRunner.registerHandler(::sendNotification)
    }

    fun getPedagogicalDocumentationNotifications(
        tx: Database.Transaction,
        id: PedagogicalDocumentId,
    ): List<AsyncJob.SendPedagogicalDocumentNotificationEmail> {
        return tx.createQuery {
                sql(
                    """
SELECT DISTINCT
    doc.id as pedagogical_document_id,
    p.id as recipient_id,
    coalesce(lower(p.language), 'fi') as language
FROM pedagogical_document doc 
JOIN guardian g ON doc.child_id = g.child_id
JOIN person p on g.guardian_id = p.id
WHERE doc.id = ${bind(id)} AND p.email IS NOT NULL
"""
                )
            }
            .toList {
                AsyncJob.SendPedagogicalDocumentNotificationEmail(
                    pedagogicalDocumentId = id,
                    recipientId = column("recipient_id"),
                    language = getLanguage(column("language")),
                )
            }
    }

    private fun Database.Transaction.updateDocumentEmailJobCreatedAt(
        id: PedagogicalDocumentId,
        date: HelsinkiDateTime,
    ) {
        createUpdate {
                sql(
                    "UPDATE pedagogical_document SET email_job_created_at = ${bind(date)} WHERE id = ${bind(id)}"
                )
            }
            .updateExactlyOne()
    }

    private fun Database.Read.shouldCreateNotificationJobForDocument(
        id: PedagogicalDocumentId
    ): Boolean {
        // notification job should be created only if description is set or an attachment is
        // uploaded
        return createQuery {
                sql(
                    """
SELECT EXISTS(
    SELECT 1
    FROM pedagogical_document doc
    LEFT JOIN attachment a ON doc.id = a.pedagogical_document_id
    WHERE
        doc.id = ${bind(id)} AND
        doc.email_job_created_at IS NULL AND 
        (LENGTH(doc.description) > 0 OR a.id IS NOT NULL)
)
            """
                )
            }
            .exactlyOne<Boolean>()
    }

    fun maybeScheduleEmailNotification(tx: Database.Transaction, id: PedagogicalDocumentId) {
        if (tx.shouldCreateNotificationJobForDocument(id)) {
            tx.updateDocumentEmailJobCreatedAt(id, HelsinkiDateTime.now())

            logger.info {
                "Scheduling sending of Pedagogical Documentation notification emails (id: $id)"
            }
            asyncJobRunner.plan(
                tx,
                payloads = getPedagogicalDocumentationNotifications(tx, id),
                runAt = HelsinkiDateTime.now(),
                retryCount = 10,
            )
        }
    }

    private fun getLanguage(languageStr: String?): Language {
        return when (languageStr?.lowercase()) {
            "sv" -> Language.sv
            "en" -> Language.en
            else -> Language.fi
        }
    }

    fun sendNotification(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.SendPedagogicalDocumentNotificationEmail,
    ) {
        val childId = db.read { tx -> tx.getPedagogicalDocumentChild(msg.pedagogicalDocumentId) }
        Email.create(
                dbc = db,
                personId = msg.recipientId,
                emailType = EmailMessageType.INFORMAL_DOCUMENT_NOTIFICATION,
                fromAddress = emailEnv.sender(msg.language),
                content =
                    emailMessageProvider.pedagogicalDocumentNotification(msg.language, childId),
                traceId = msg.pedagogicalDocumentId.toString(),
            )
            ?.also {
                emailClient.send(it)
                db.transaction { tx ->
                    tx.markPedagogicalDocumentNotificationSent(msg.pedagogicalDocumentId)
                }
            }
    }
}

private fun Database.Transaction.markPedagogicalDocumentNotificationSent(
    id: PedagogicalDocumentId
) {
    createUpdate {
            sql(
                """
                UPDATE pedagogical_document
                SET email_sent = TRUE 
                WHERE id = ${bind(id)}
                """
            )
        }
        .updateExactlyOne()
}
