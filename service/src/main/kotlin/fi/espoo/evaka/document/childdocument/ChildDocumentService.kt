// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document.childdocument

import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.emailclient.Email
import fi.espoo.evaka.emailclient.EmailClient
import fi.espoo.evaka.emailclient.IEmailMessageProvider
import fi.espoo.evaka.pis.EmailMessageType
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class ChildDocumentService(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val emailClient: EmailClient,
    private val emailMessageProvider: IEmailMessageProvider,
    private val emailEnv: EmailEnv
) {
    init {
        asyncJobRunner.registerHandler(::sendChildDocumentNotificationEmail)
    }

    fun completeAndPublishChildDocumentsAtEndOfTerm(
        tx: Database.Transaction,
        now: HelsinkiDateTime
    ) {
        val documentIds =
            tx.createQuery {
                    sql(
                        """
                SELECT cd.id
                FROM child_document cd 
                JOIN document_template dt on dt.id = cd.template_id
                WHERE dt.validity << ${bind(FiniteDateRange(now.toLocalDate(), now.toLocalDate()))} AND cd.status <> 'COMPLETED'
            """
                            .trimIndent()
                    )
                }
                .toList<ChildDocumentId>()

        if (documentIds.isNotEmpty()) {
            documentIds
                .filter { !tx.isDocumentPublishedContentUpToDate(it) }
                .forEach { scheduleEmailNotification(tx, it, now) }

            tx.markCompletedAndPublish(documentIds, now)
        }
    }

    fun scheduleEmailNotification(
        tx: Database.Transaction,
        id: ChildDocumentId,
        now: HelsinkiDateTime
    ) {
        logger.info { "Scheduling sending of child document notification emails (id: $id)" }
        asyncJobRunner.plan(
            tx,
            payloads = getChildDocumentNotifications(tx, id, now.toLocalDate()),
            runAt = now,
            retryCount = 10
        )
    }

    private fun getLanguage(languageStr: String?): Language {
        return when (languageStr?.lowercase()) {
            "sv" -> Language.sv
            "en" -> Language.en
            else -> Language.fi
        }
    }

    private fun getChildDocumentNotifications(
        tx: Database.Read,
        documentId: ChildDocumentId,
        today: LocalDate
    ): List<AsyncJob.SendChildDocumentNotificationEmail> {
        return tx.createQuery {
                sql(
                    """
WITH children AS (
    SELECT child_id
    FROM child_document
    WHERE id = ${bind(documentId)}
), parents AS (
    SELECT g.guardian_id AS parent_id, children.child_id
    FROM guardian g
    JOIN children ON children.child_id = g.child_id
    
    UNION DISTINCT 
    
    SELECT fp.parent_id, children.child_id
    FROM foster_parent fp
    JOIN children ON children.child_id = fp.child_id AND fp.valid_during @> ${bind(today)}
)
SELECT parents.child_id, person.id AS recipient_id, person.language
FROM parents 
JOIN person ON person.id = parents.parent_id
WHERE person.email IS NOT NULL AND person.email != ''
"""
                )
            }
            .toList {
                AsyncJob.SendChildDocumentNotificationEmail(
                    documentId = documentId,
                    childId = column("child_id"),
                    recipientId = column("recipient_id"),
                    language = getLanguage(column("language"))
                )
            }
    }

    fun sendChildDocumentNotificationEmail(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.SendChildDocumentNotificationEmail
    ) {
        logger.info(
            "Sending child document notification email for document ${msg.documentId} to person ${msg.recipientId}"
        )
        Email.create(
                dbc = db,
                personId = msg.recipientId,
                emailType = EmailMessageType.DOCUMENT_NOTIFICATION,
                fromAddress = emailEnv.sender(msg.language),
                content = emailMessageProvider.childDocumentNotification(msg.language, msg.childId),
                traceId = msg.documentId.toString(),
            )
            ?.also { emailClient.send(it) }
    }
}
