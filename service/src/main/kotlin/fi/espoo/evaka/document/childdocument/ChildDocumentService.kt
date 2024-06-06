// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document.childdocument

import fi.espoo.evaka.BucketEnv
import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.emailclient.Email
import fi.espoo.evaka.emailclient.EmailClient
import fi.espoo.evaka.emailclient.IEmailMessageProvider
import fi.espoo.evaka.pdfgen.PdfGenerator
import fi.espoo.evaka.pis.EmailMessageType
import fi.espoo.evaka.process.updateDocumentProcessHistory
import fi.espoo.evaka.s3.Document
import fi.espoo.evaka.s3.DocumentService
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import java.time.LocalDate
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

private const val childDocumentBucketPrefix = "child-documents/"

@Service
class ChildDocumentService(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    bucketEnv: BucketEnv,
    private val documentClient: DocumentService,
    private val emailClient: EmailClient,
    private val emailMessageProvider: IEmailMessageProvider,
    private val emailEnv: EmailEnv,
    private val pdfGenerator: PdfGenerator
) {
    private val bucket = bucketEnv.data

    init {
        asyncJobRunner.registerHandler<AsyncJob.CreateChildDocumentPdf> { db, _, msg ->
            createAndUploadPdf(db, msg.documentId)
        }
        asyncJobRunner.registerHandler(::sendChildDocumentNotificationEmail)
    }

    fun createAndUploadPdf(db: Database.Connection, documentId: ChildDocumentId) {
        val document =
            db.transaction { tx -> tx.getChildDocument(documentId) }
                ?: throw NotFound("document $documentId not found")
        val html = generateChildDocumentHtml(document)
        val pdfBytes = pdfGenerator.render(html)
        val key =
            documentClient.upload(
                bucketName = bucket,
                document =
                    Document(
                        name = "${childDocumentBucketPrefix}child_document_$documentId.pdf",
                        bytes = pdfBytes,
                        contentType = "application/pdf"
                    )
            )
        db.transaction { tx -> tx.updateChildDocumentKey(documentId, key.key) }
    }

    fun getPdfResponse(tx: Database.Read, documentId: ChildDocumentId): ResponseEntity<Any> {
        val documentKey =
            tx.getChildDocumentKey(documentId)
                ?: throw NotFound("Document $documentId not found or pdf not ready")
        return documentClient.responseAttachment(bucket, documentKey, null)
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
                .also {
                    schedulePdfGeneration(tx, it, now)
                    scheduleEmailNotification(tx, it, now)
                }

            tx.markCompletedAndPublish(documentIds, now)

            documentIds.forEach { documentId ->
                updateDocumentProcessHistory(
                    tx = tx,
                    documentId = documentId,
                    newStatus = DocumentStatus.COMPLETED,
                    now = now,
                    userId = AuthenticatedUser.SystemInternalUser.evakaUserId
                )
            }
        }
    }

    fun scheduleEmailNotification(
        tx: Database.Transaction,
        ids: List<ChildDocumentId>,
        now: HelsinkiDateTime
    ) {
        val payloads = ids.flatMap { getChildDocumentNotifications(tx, it, now.toLocalDate()) }
        logger.info { "Scheduling sending of ${payloads.size} child document notification emails" }
        asyncJobRunner.plan(tx, payloads = payloads, runAt = now, retryCount = 10)
    }

    fun schedulePdfGeneration(
        tx: Database.Transaction,
        ids: List<ChildDocumentId>,
        now: HelsinkiDateTime
    ) {
        logger.info { "Scheduling generation of ${ids.size} child document pdfs" }

        // set document key to null until re-generation finishes
        // the new document key will be the same, so there's no need to separately delete from s3
        tx.resetChildDocumentKey(ids)

        asyncJobRunner.plan(
            tx,
            payloads = ids.map { AsyncJob.CreateChildDocumentPdf(it) },
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
