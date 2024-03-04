// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vasu

import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.emailclient.Email
import fi.espoo.evaka.emailclient.EmailClient
import fi.espoo.evaka.emailclient.IEmailMessageProvider
import fi.espoo.evaka.pis.EmailMessageType
import fi.espoo.evaka.shared.VasuDocumentId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class VasuNotificationService(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val emailClient: EmailClient,
    private val emailMessageProvider: IEmailMessageProvider,
    private val emailEnv: EmailEnv
) {
    init {
        asyncJobRunner.registerHandler(::sendVasuNotificationEmail)
    }

    fun scheduleEmailNotification(tx: Database.Transaction, id: VasuDocumentId) {
        logger.info { "Scheduling sending of vasu/leops notification emails (id: $id)" }
        asyncJobRunner.plan(
            tx,
            payloads = getVasuNotifications(tx, id),
            runAt = HelsinkiDateTime.now(),
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

    private fun getVasuNotifications(
        tx: Database.Read,
        vasuDocumentId: VasuDocumentId
    ): List<AsyncJob.SendVasuNotificationEmail> {
        return tx.createQuery {
                sql(
                    """
SELECT 
    doc.id AS vasu_document_id,
    child.id AS child_id,
    parent.id AS recipient_id,
    parent.language AS language
FROM curriculum_document doc
    JOIN person child ON doc.child_id = child.id
    JOIN guardian g ON doc.child_id = g.child_id
    JOIN person parent ON g.guardian_id = parent.id
WHERE
    doc.id = ${bind(vasuDocumentId)}
    AND parent.email IS NOT NULL AND parent.email != ''
"""
                )
            }
            .toList {
                AsyncJob.SendVasuNotificationEmail(
                    vasuDocumentId = vasuDocumentId,
                    childId = column("child_id"),
                    recipientId = column("recipient_id"),
                    language = getLanguage(column("language"))
                )
            }
    }

    fun sendVasuNotificationEmail(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.SendVasuNotificationEmail
    ) {
        logger.info(
            "Sending vasu/leops notification email for document ${msg.vasuDocumentId} to person ${msg.recipientId}"
        )
        Email.create(
                dbc = db,
                personId = msg.recipientId,
                emailType = EmailMessageType.DOCUMENT_NOTIFICATION,
                fromAddress = emailEnv.sender(msg.language),
                content = emailMessageProvider.vasuNotification(msg.language, msg.childId),
                traceId = msg.vasuDocumentId.toString(),
            )
            ?.also { emailClient.send(it) }
    }
}
