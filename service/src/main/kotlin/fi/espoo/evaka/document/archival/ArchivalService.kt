// SPDX-FileCopyrightText: 2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document.archival

import fi.espoo.evaka.caseprocess.getCaseProcessByChildDocumentId
import fi.espoo.evaka.caseprocess.getChildDocumentMetadata
import fi.espoo.evaka.document.childdocument.getChildDocument
import fi.espoo.evaka.document.childdocument.getChildDocumentKey
import fi.espoo.evaka.document.childdocument.markDocumentAsArchived
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.s3.DocumentKey
import fi.espoo.evaka.s3.DocumentService
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.user.getEvakaUser
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class ArchivalService(
    asyncJobRunner: AsyncJobRunner<AsyncJob>?,
    private val archivalIntegrationClient: ArchivalIntegrationClient,
    private val documentClient: DocumentService,
) {

    init {
        asyncJobRunner?.registerHandler<AsyncJob.ArchiveChildDocument> { db, _, msg ->
            uploadChildDocumentToArchive(db, msg)
        }
    }

    fun uploadChildDocumentToArchive(db: Database.Connection, msg: AsyncJob.ArchiveChildDocument) {
        val (documentId, user) = msg
        logger.info { "Starting archival process for document $documentId" }

        val evakaUser =
            (user ?: AuthenticatedUser.SystemInternalUser).let {
                db.read { tx ->
                    tx.getEvakaUser(it.evakaUserId)
                        ?: throw NotFound("EvakaUser not found with ${it.evakaUserId}")
                }
            }

        val document =
            db.read { tx ->
                tx.getChildDocument(msg.documentId)
                    ?: throw NotFound("document $documentId not found")
            }
        val childInfo =
            db.read { tx ->
                val childId = document.child.id
                tx.getPersonById(childId)
                    ?: throw IllegalStateException("No person found with $childId")
            }
        val caseProcess = db.read { tx -> tx.getCaseProcessByChildDocumentId(documentId) }
        val documentMetadata = db.read { tx -> tx.getChildDocumentMetadata(documentId) }

        val documentKey =
            db.read { tx ->
                tx.getChildDocumentKey(documentId)
                    ?: throw NotFound("Document key not found for document $documentId")
            }

        // Get the document from the original location
        val originalLocation = documentClient.locate(DocumentKey.ChildDocument(documentKey))
        val documentContent = documentClient.get(originalLocation)

        val instanceId =
            archivalIntegrationClient.uploadChildDocumentToArchive(
                caseProcess = caseProcess,
                documentId = documentId,
                childInfo = childInfo,
                childDocumentDetails = document,
                documentMetadata = documentMetadata,
                documentContent = documentContent,
                evakaUser = evakaUser,
            )

        db.transaction { tx -> tx.markDocumentAsArchived(documentId, HelsinkiDateTime.now()) }
        logger.info { "Successfully archived document $documentId to instance $instanceId" }
    }
}
