// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document.childdocument

import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.decision.getSendAddress
import fi.espoo.evaka.document.DocumentType
import fi.espoo.evaka.emailclient.Email
import fi.espoo.evaka.emailclient.EmailClient
import fi.espoo.evaka.emailclient.IEmailMessageProvider
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.pdfgen.PdfGenerator
import fi.espoo.evaka.pis.EmailMessageType
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.pis.service.getChildGuardiansAndFosterParents
import fi.espoo.evaka.process.ArchivedProcessState
import fi.espoo.evaka.process.autoCompleteDocumentProcessHistory
import fi.espoo.evaka.process.getArchiveProcessByChildDocumentId
import fi.espoo.evaka.process.insertProcessHistoryRow
import fi.espoo.evaka.s3.DocumentKey
import fi.espoo.evaka.s3.DocumentService
import fi.espoo.evaka.sficlient.SentSfiMessage
import fi.espoo.evaka.sficlient.SfiMessage
import fi.espoo.evaka.sficlient.hasChildDocumentSfiMessageBeenSent
import fi.espoo.evaka.sficlient.storeSentSfiMessage
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.domain.OfficialLanguage
import fi.espoo.evaka.shared.domain.UiLanguage
import fi.espoo.evaka.shared.domain.toFiniteDateRange
import fi.espoo.evaka.shared.message.IMessageProvider
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDate
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class ChildDocumentService(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val documentClient: DocumentService,
    private val messageProvider: IMessageProvider,
    private val emailClient: EmailClient,
    private val emailMessageProvider: IEmailMessageProvider,
    private val emailEnv: EmailEnv,
    private val pdfGenerator: PdfGenerator,
) {
    init {
        asyncJobRunner.registerHandler<AsyncJob.CreateChildDocumentPdf> { db, clock, msg ->
            createAndUploadPdf(db, clock, msg.documentId)
        }
        asyncJobRunner.registerHandler(::sendSfiDecision)
        asyncJobRunner.registerHandler(::sendChildDocumentNotificationEmail)
        asyncJobRunner.registerHandler<AsyncJob.DeleteChildDocumentPdf> { _, _, msg ->
            documentClient.delete(DocumentKey.ChildDocument(msg.key))
        }
    }

    fun createAndUploadPdf(
        db: Database.Connection,
        clock: EvakaClock,
        documentId: ChildDocumentId,
    ) {
        val document =
            db.read { tx -> tx.getChildDocument(documentId) }
                ?: throw NotFound("document $documentId not found")
        val html = generateChildDocumentHtml(document)
        val pdfBytes = pdfGenerator.render(html)
        val key =
            documentClient.upload(
                DocumentKey.ChildDocument(documentId),
                pdfBytes,
                "application/pdf",
            )

        db.transaction { tx ->
            tx.updateChildDocumentKey(documentId, key.key)

            if (document.decision != null) {
                if (tx.hasChildDocumentSfiMessageBeenSent(documentId)) {
                    logger.info {
                        "Child document decision $documentId SFI message already sent to at least one guardian"
                    }
                    return@transaction
                }

                asyncJobRunner.plan(
                    tx,
                    listOf(AsyncJob.SendChildDocumentDecisionSfiMessage(documentId)),
                    runAt = clock.now(),
                )

                tx.getArchiveProcessByChildDocumentId(documentId)?.also {
                    tx.insertProcessHistoryRow(
                        processId = it.id,
                        state = ArchivedProcessState.COMPLETED,
                        now = clock.now(),
                        userId = AuthenticatedUser.SystemInternalUser.evakaUserId,
                    )
                }
            }
        }
    }

    fun sendSfiDecision(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.SendChildDocumentDecisionSfiMessage,
    ) {
        val documentId = msg.documentId

        db.transaction { tx ->
            val document =
                tx.getChildDocument(documentId)?.also {
                    if (it.decision == null)
                        throw IllegalStateException("document $documentId is not a decision")
                } ?: throw NotFound("document $documentId not found")

            val documentLocation =
                documentClient.locate(
                    DocumentKey.AssistanceNeedDecision(
                        tx.getChildDocumentKey(documentId)
                            ?: throw IllegalStateException(
                                "Decision pdf has not yet been generated"
                            )
                    )
                )

            val lang =
                if (document.template.language == UiLanguage.SV) OfficialLanguage.SV
                else OfficialLanguage.FI

            tx.getChildGuardiansAndFosterParents(document.child.id, clock.today())
                .mapNotNull { tx.getPersonById(it) }
                .forEach { guardian ->
                    if (guardian.identity !is ExternalIdentifier.SSN) {
                        logger.info {
                            "Cannot deliver child document decision $documentId to guardian via Sfi. SSN is missing."
                        }
                        return@forEach
                    }

                    val sendAddress = getSendAddress(messageProvider, guardian, lang)

                    val messageId =
                        tx.storeSentSfiMessage(
                            SentSfiMessage(guardianId = guardian.id, documentId = document.id)
                        )

                    // SFI expects unique string for each message, so document.id is not suitable as
                    // it is NOT
                    // string and NOT unique
                    val uniqueId = "${document.id}|${guardian.id}"

                    val message =
                        SfiMessage(
                            messageId = messageId,
                            documentId = uniqueId,
                            documentDisplayName = document.template.name,
                            documentBucket = documentLocation.bucket,
                            documentKey = documentLocation.key,
                            firstName = guardian.firstName,
                            lastName = guardian.lastName,
                            streetAddress = sendAddress.street,
                            postalCode = sendAddress.postalCode,
                            postOffice = sendAddress.postOffice,
                            ssn = guardian.identity.ssn,
                            messageHeader = messageProvider.getChildDocumentDecisionHeader(lang),
                            messageContent = messageProvider.getChildDocumentDecisionContent(lang),
                        )

                    asyncJobRunner.plan(
                        tx,
                        listOf(AsyncJob.SendMessage(message)),
                        runAt = clock.now(),
                    )
                }

            logger.info {
                "Successfully scheduled child document decision pdf for Suomi.fi sending (id: $documentId)."
            }
        }
    }

    fun getPdfResponse(tx: Database.Read, documentId: ChildDocumentId): ResponseEntity<Any> {
        val documentLocation =
            documentClient.locate(
                DocumentKey.ChildDocument(
                    tx.getChildDocumentKey(documentId)
                        ?: throw NotFound("Document $documentId not found or pdf not ready")
                )
            )
        return documentClient.responseAttachment(documentLocation, null)
    }

    fun completeAndPublishChildDocumentsAtEndOfTerm(
        tx: Database.Transaction,
        now: HelsinkiDateTime,
    ) {
        val documentIds =
            tx.createQuery {
                    sql(
                        """
                SELECT cd.id
                FROM child_document cd
                JOIN document_template dt on dt.id = cd.template_id
                WHERE dt.validity << ${bind(now.toLocalDate().toFiniteDateRange())} 
                    AND dt.type = ANY (${bind(
                        DocumentType.entries.filter { it.autoCompleteAtEndOfValidity }
                    )})
                    AND cd.status <> 'COMPLETED'
            """
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
                autoCompleteDocumentProcessHistory(tx = tx, documentId = documentId, now = now)
            }
        }
    }

    fun scheduleEmailNotification(
        tx: Database.Transaction,
        ids: List<ChildDocumentId>,
        now: HelsinkiDateTime,
    ) {
        val payloads = ids.flatMap { getChildDocumentNotifications(tx, it, now.toLocalDate()) }
        logger.info { "Scheduling sending of ${payloads.size} child document notification emails" }
        asyncJobRunner.plan(tx, payloads = payloads, runAt = now, retryCount = 10)
    }

    fun schedulePdfGeneration(
        tx: Database.Transaction,
        ids: List<ChildDocumentId>,
        now: HelsinkiDateTime,
    ) {
        logger.info { "Scheduling generation of ${ids.size} child document pdfs" }

        // set document key to null until re-generation finishes
        // the new document key will be the same, so there's no need to separately delete from s3
        tx.resetChildDocumentKey(ids)

        asyncJobRunner.plan(
            tx,
            payloads = ids.map { AsyncJob.CreateChildDocumentPdf(it) },
            runAt = now,
            retryCount = 10,
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
        today: LocalDate,
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
                    language = getLanguage(column("language")),
                )
            }
    }

    fun sendChildDocumentNotificationEmail(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.SendChildDocumentNotificationEmail,
    ) {
        logger.info {
            "Sending child document notification email for document ${msg.documentId} to person ${msg.recipientId}"
        }
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
