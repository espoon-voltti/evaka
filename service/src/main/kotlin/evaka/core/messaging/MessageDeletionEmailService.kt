// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.messaging

import evaka.core.Audit
import evaka.core.AuditId
import evaka.core.EmailEnv
import evaka.core.daycare.domain.Language
import evaka.core.emailclient.Email
import evaka.core.emailclient.EmailClient
import evaka.core.emailclient.IEmailMessageProvider
import evaka.core.emailclient.MessageDeletionEmailData
import evaka.core.shared.FeatureConfig
import evaka.core.shared.HtmlSafe
import evaka.core.shared.MessageContentId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.HelsinkiDateTime
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Duration
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class MessageDeletionEmailService(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val emailClient: EmailClient,
    private val emailMessageProvider: IEmailMessageProvider,
    private val emailEnv: EmailEnv,
    private val featureConfig: FeatureConfig,
) {
    init {
        asyncJobRunner.registerHandler(::sendSenderEmail)
        asyncJobRunner.registerHandler(::sendNotificationEmail)
    }

    fun planDeletionEmails(
        tx: Database.Transaction,
        contentId: MessageContentId,
        runAt: HelsinkiDateTime,
    ) {
        val supervisorEmails = tx.getUnitSupervisorEmailsForContent(contentId)
        val supportEmail = featureConfig.messageSupportEmail
        val adminNotificationRecipients =
            if (supportEmail != null) setOf(supportEmail)
            else tx.getAdminEmailsForContent(contentId)
        if (adminNotificationRecipients.isEmpty()) {
            logger.warn {
                "No admin recipients for deletion notification of content $contentId (no support email configured and no active admins)"
            }
        }
        val jobs: List<AsyncJob> = buildList {
            add(AsyncJob.SendMessageDeletionSenderEmail(contentId))
            supervisorEmails.forEach {
                add(
                    AsyncJob.SendMessageDeletionNotificationEmail(contentId, it, "Yksikön johtajat")
                )
            }
            adminNotificationRecipients.forEach {
                add(
                    AsyncJob.SendMessageDeletionNotificationEmail(
                        contentId,
                        it,
                        "Kunnan eVaka-pääkäyttäjä",
                    )
                )
            }
        }
        asyncJobRunner.plan(
            tx,
            jobs,
            retryCount = 10,
            retryInterval = Duration.ofMinutes(5),
            runAt = runAt,
        )
    }

    fun sendSenderEmail(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.SendMessageDeletionSenderEmail,
    ) {
        val summary = db.read { it.deletionSummary(msg.contentId) }
        val toAddress = summary.deleterEmail
        if (toAddress == null) {
            logger.warn {
                "No email address for the deleter of content ${msg.contentId} — sender email skipped"
            }
            return
        }
        val content =
            emailMessageProvider.messageDeletionSenderEmail(
                featureConfig.messageSupportEmail,
                summary.toEmailData(),
            )
        Email.createForAddress(
                toAddress,
                emailEnv.sender(Language.fi),
                content,
                msg.contentId.toString(),
            )
            ?.let { emailClient.send(it) }
        Audit.MessagingDeletionEmailSent.log(
            targetId = AuditId(msg.contentId),
            meta = mapOf("role" to "SENDER"),
        )
    }

    fun sendNotificationEmail(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.SendMessageDeletionNotificationEmail,
    ) {
        val summary = db.read { it.deletionSummary(msg.contentId) }
        val content =
            emailMessageProvider.messageDeletionNotificationEmail(
                featureConfig.messageSupportEmail,
                summary.toEmailData(),
            )
        Email.createForAddress(
                msg.recipientEmail,
                emailEnv.sender(Language.fi),
                content,
                "${msg.contentId}:${msg.recipientEmail}",
            )
            ?.let { emailClient.send(it) }
        Audit.MessagingDeletionEmailSent.log(
            targetId = AuditId(msg.contentId),
            meta = mapOf("role" to msg.recipientLabel, "recipientEmail" to msg.recipientEmail),
        )
    }

    private fun Database.Read.deletionSummary(contentId: MessageContentId): MessageDeletionSummary =
        getMessageDeletionSummary(
            contentId,
            municipalAccountName = featureConfig.municipalMessageAccountName,
            serviceWorkerAccountName = featureConfig.serviceWorkerMessageAccountName,
            financeAccountName = featureConfig.financeMessageAccountName,
        )

    private fun MessageDeletionSummary.toEmailData() =
        MessageDeletionEmailData(
            deleterName = HtmlSafe(deleterName),
            senderAccountName = HtmlSafe(senderAccountName),
            senderAccountType = senderAccountType,
            sentAt = sentAt,
            deletedAt = deletedAt,
            recipientCount = recipientCount,
        )
}
