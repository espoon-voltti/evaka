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
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class ArchivalService(
    asyncJobRunner: AsyncJobRunner<AsyncJob>?,
    private val archivalIntegrationClient: ArchivalIntegrationClient,
) {

    init {
        asyncJobRunner?.registerHandler<AsyncJob.ArchiveChildDocument> { db, _, msg ->
            uploadChildDocumentToArchive(db, msg.documentId)
        }
    }

    fun uploadChildDocumentToArchive(db: Database.Connection, documentId: ChildDocumentId) {
        logger.info { "Starting archival process for document $documentId" }

        val document =
            db.read { tx ->
                tx.getChildDocument(documentId) ?: throw NotFound("document $documentId not found")
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

        val instanceId =
            archivalIntegrationClient.uploadChildDocumentToArchive(
                caseProcess = caseProcess,
                documentId = documentId,
                childInfo = childInfo,
                childDocumentDetails = document,
                documentMetadata = documentMetadata,
                documentKey = documentKey,
            )

        db.transaction { tx -> tx.markDocumentAsArchived(documentId, HelsinkiDateTime.now()) }
        logger.info { "Successfully archived document $documentId to instance $instanceId" }
    }
}
