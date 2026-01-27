// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document.childdocument

import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.caseprocess.CaseProcessState
import fi.espoo.evaka.caseprocess.autoCompleteDocumentCaseProcessHistory
import fi.espoo.evaka.caseprocess.getCaseProcessByChildDocumentId
import fi.espoo.evaka.caseprocess.insertCaseProcessHistoryRow
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.decision.getSendAddress
import fi.espoo.evaka.document.ChildDocumentType
import fi.espoo.evaka.emailclient.Email
import fi.espoo.evaka.emailclient.EmailClient
import fi.espoo.evaka.emailclient.IEmailMessageProvider
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.pdfgen.PdfGenerator
import fi.espoo.evaka.pis.EmailMessageType
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.pis.service.getChildGuardiansAndFosterParents
import fi.espoo.evaka.placement.getPlacementsForChild
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
import fi.espoo.evaka.shared.domain.FiniteDateRange
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

enum class EmailNotificationPolicy {
    NEVER,
    ON_NEW_VERSION,
    ALWAYS,
}

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
        asyncJobRunner.registerHandler(::createAndUploadPdf)
        asyncJobRunner.registerHandler(::sendSfiDecision)
        asyncJobRunner.registerHandler(::sendChildDocumentNotificationEmail)
        asyncJobRunner.registerHandler<AsyncJob.DeleteChildDocumentPdf> { _, _, msg ->
            documentClient.delete(DocumentKey.ChildDocument(msg.key))
        }
    }

    fun createAndUploadPdf(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.CreateChildDocumentPdf,
    ) {
        val documentId = msg.documentId
        val requestedVersion = msg.versionNumber ?: 1

        val document =
            db.read { tx -> tx.getChildDocument(documentId) }
                ?: throw NotFound("document $documentId not found")

        // Check if this version is still the latest
        val latestVersion =
            db.read { tx ->
                tx.createQuery {
                        sql(
                            """
                    SELECT MAX(version_number)
                    FROM child_document_published_version
                    WHERE child_document_id = ${bind(documentId)}
                """
                        )
                    }
                    .exactlyOneOrNull<Int>()
            }

        if (latestVersion != null && requestedVersion < latestVersion) {
            logger.warn {
                "Aborting PDF generation for document $documentId version $requestedVersion " +
                    "(superseded by version $latestVersion). Leaving document_key NULL."
            }
            return
        }

        val html = generateChildDocumentHtml(document)
        val pdfBytes = pdfGenerator.render(html)

        db.transaction { tx ->
            val versionedKey = DocumentKey.ChildDocument(documentId, requestedVersion)
            val location = documentClient.upload(versionedKey, pdfBytes, "application/pdf")

            tx.updateChildDocumentPublishedVersionKey(documentId, requestedVersion, location.key)

            // Delete old read markers now that new a PDF is ready to download
            tx.deleteChildDocumentReadMarkers(documentId)

            if (document.decision != null) {
                if (tx.hasChildDocumentSfiMessageBeenSent(documentId)) {
                    logger.info {
                        "Child document decision $documentId SFI message already sent to at least one guardian"
                    }
                    return@transaction
                }

                asyncJobRunner.plan(
                    tx,
                    listOf(
                        AsyncJob.SendChildDocumentDecisionSfiMessage(documentId, requestedVersion)
                    ),
                    runAt = clock.now(),
                )

                tx.getCaseProcessByChildDocumentId(documentId)?.also {
                    tx.insertCaseProcessHistoryRow(
                        processId = it.id,
                        state = CaseProcessState.COMPLETED,
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

            val publishedVersion =
                tx.getChildDocumentPublishedVersion(documentId, msg.versionNumber)

            if (publishedVersion?.documentKey == null) {
                throw IllegalStateException(
                    "Decision pdf version ${msg.versionNumber} does not have a document key yet for document $documentId"
                )
            }

            val documentLocation =
                documentClient.locate(DocumentKey.ChildDocument(publishedVersion.documentKey))

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

    fun getPdfResponse(
        tx: Database.Read,
        documentId: ChildDocumentId,
        versionNumber: Int? = null,
    ): ResponseEntity<Any> {
        val publishedVersion = tx.getChildDocumentPublishedVersion(documentId, versionNumber)

        if (publishedVersion?.documentKey == null) {
            throw NotFound(
                if (versionNumber != null) {
                    "Document $documentId version $versionNumber pdf not ready"
                } else {
                    "Document $documentId pdf not ready"
                }
            )
        }

        val documentLocation =
            documentClient.locate(DocumentKey.ChildDocument(publishedVersion.documentKey))

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
                        ChildDocumentType.entries.filter { it.autoCompleteAtEndOfValidity }
                    )})
                    AND cd.status <> 'COMPLETED'
                    AND EXISTS (
                        SELECT 1 FROM child_document_published_version v
                        WHERE v.child_document_id = cd.id
                    )
            """
                    )
                }
                .toList<ChildDocumentId>()

        if (documentIds.isNotEmpty()) {
            val versionMap = tx.markCompletedAndPublish(documentIds, now)

            if (versionMap.isNotEmpty()) {
                schedulePdfGeneration(tx, AuthenticatedUser.SystemInternalUser, versionMap, now)
                scheduleEmailNotification(tx, versionMap.keys.toList(), now)
            }

            documentIds.forEach { documentId ->
                autoCompleteDocumentCaseProcessHistory(tx = tx, documentId = documentId, now = now)
            }
        }
    }

    fun scheduleEmailNotification(
        tx: Database.Transaction,
        ids: List<ChildDocumentId>,
        now: HelsinkiDateTime,
    ) {
        val payloads: List<AsyncJob.SendChildDocumentNotificationEmail> =
            ids.flatMap { getChildDocumentNotifications(tx, it, now.toLocalDate()) }

        logger.info { "Scheduling sending of ${payloads.size} child document notification emails" }
        asyncJobRunner.plan(tx, payloads = payloads, runAt = now, retryCount = 10)
    }

    fun schedulePdfGeneration(
        tx: Database.Transaction,
        user: AuthenticatedUser,
        documentVersions: Map<ChildDocumentId, Int>,
        now: HelsinkiDateTime,
    ) {
        logger.info { "Scheduling generation of ${documentVersions.size} child document pdfs" }

        asyncJobRunner.plan(
            tx,
            payloads =
                documentVersions.map { (documentId, versionNumber) ->
                    AsyncJob.CreateChildDocumentPdf(documentId, versionNumber, user)
                },
            runAt = now,
            retryCount = 10,
        )
    }

    /**
     * Publishes document if content has changed, and schedules PDF generation and email
     * notification.
     *
     * @param emailPolicy Controls when to schedule email notifications
     * @return The created version number, or null if content was already up to date
     */
    fun publishAndScheduleNotifications(
        tx: Database.Transaction,
        user: AuthenticatedUser,
        documentId: ChildDocumentId,
        now: HelsinkiDateTime,
        emailPolicy: EmailNotificationPolicy,
    ): Int? {
        val versionNumber = tx.createPublishedVersionIfNeeded(documentId, now, user.evakaUserId)

        if (versionNumber != null) {
            schedulePdfGeneration(tx, user, mapOf(documentId to versionNumber), now)
        }

        val shouldScheduleEmail =
            when (emailPolicy) {
                EmailNotificationPolicy.NEVER -> false
                EmailNotificationPolicy.ON_NEW_VERSION -> versionNumber != null
                EmailNotificationPolicy.ALWAYS -> true
            }

        if (shouldScheduleEmail) {
            scheduleEmailNotification(tx, listOf(documentId), now)
        }

        return versionNumber
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
WITH child_document AS (
    SELECT child_id, type, status
    FROM child_document
    WHERE id = ${bind(documentId)}
), parents AS (
    SELECT g.guardian_id AS parent_id, child_document.child_id, child_document.type, child_document.status
    FROM guardian g
    JOIN child_document ON child_document.child_id = g.child_id
    
    UNION DISTINCT 
    
    SELECT fp.parent_id, child_document.child_id, child_document.type, child_document.status
    FROM foster_parent fp
    JOIN child_document ON child_document.child_id = fp.child_id AND fp.valid_during @> ${bind(today)}
)
SELECT parents.child_id, parents.type AS document_type, parents.status AS document_status, person.id AS recipient_id, person.language
FROM parents 
JOIN person ON person.id = parents.parent_id
WHERE person.email IS NOT NULL AND person.email != ''
"""
                )
            }
            .toList {
                val documentType = column<ChildDocumentType>("document_type")
                val documentStatus = column<DocumentStatus>("document_status")
                val notificationType =
                    when {
                        documentType.decision -> {
                            ChildDocumentNotificationType.DECISION_DOCUMENT
                        }

                        documentStatus.citizenEditable -> {
                            ChildDocumentNotificationType.EDITABLE_DOCUMENT
                        }

                        else -> {
                            ChildDocumentNotificationType.BASIC_DOCUMENT
                        }
                    }

                AsyncJob.SendChildDocumentNotificationEmail(
                    documentId = documentId,
                    childId = column("child_id"),
                    recipientId = column("recipient_id"),
                    language = getLanguage(column("language")),
                    notificationType = notificationType,
                )
            }
    }

    fun sendChildDocumentNotificationEmail(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.SendChildDocumentNotificationEmail,
    ) {
        val childHasPlacement =
            db.read { tx ->
                tx.getPlacementsForChild(msg.childId).any {
                    FiniteDateRange(it.startDate, it.endDate).includes(clock.today())
                }
            }
        if (!childHasPlacement) {
            logger.info {
                "Child ${msg.childId} has no active placement, skipping notification email for document ${msg.documentId}"
            }
            return
        }

        logger.info {
            "Sending child document notification email for document ${msg.documentId} to person ${msg.recipientId}"
        }
        Email.create(
                dbc = db,
                personId = msg.recipientId,
                emailType = EmailMessageType.DOCUMENT_NOTIFICATION,
                fromAddress = emailEnv.sender(msg.language),
                content =
                    emailMessageProvider.childDocumentNotification(
                        msg.language,
                        msg.childId,
                        msg.notificationType,
                    ),
                traceId = msg.documentId.toString(),
            )
            ?.also { emailClient.send(it) }
    }
}
