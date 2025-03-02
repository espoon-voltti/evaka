// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document.archival

import fi.espoo.evaka.document.childdocument.getChildDocumentKey
import fi.espoo.evaka.document.childdocument.markDocumentAsArchived
import fi.espoo.evaka.s3.DocumentKey
import fi.espoo.evaka.s3.DocumentService
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
class ArchiveChildDocumentService(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val documentClient: DocumentService,
    private val client: SärmäClientInterface,
) {

    init {
        asyncJobRunner.registerHandler<AsyncJob.ArchiveChildDocument> { db, _, msg ->
            uploadToArchive(db, msg.documentId)
        }
    }

    fun uploadToArchive(db: Database.Connection, documentId: ChildDocumentId) {
        logger.info { "Starting archival process for document $documentId" }

        // TODO luo document metadata ChildDocumentDetails perusteella
        //        val document =
        //            db.read { tx ->
        //                tx.getChildDocument(documentId) ?: throw NotFound("document $documentId
        // not found")
        //            }

        val documentKey =
            db.read { tx ->
                tx.getChildDocumentKey(documentId)
                    ?: throw NotFound("Document key not found for document $documentId")
            }

        // Get the document from the original location
        val originalLocation = documentClient.locate(DocumentKey.ChildDocument(documentKey))
        val documentContent = documentClient.get(originalLocation)

        val masterId =
            "yleinen" // TODO ei vielä varmuudella tiedossa. Muissa Särmä integraatioissa käytetty
        // esim. "taloushallinto" tai "paatoksenteko"
        val classId =
            "12.01.SL1.RT34" // Arvo perustuu Evaka_Särmä_metatietomääritykset.xlsx -tiedostoon
        val virtualArchiveId =
            "YLEINEN" // Arvo perustuu Evaka_Särmä_metatietomääritykset.xlsx -tiedostoon
        val (responseCode, responseBody) =
            client.putDocument(documentContent, masterId, classId, virtualArchiveId)
        logger.info { "Response code: $responseCode" }

        if (responseCode == 200) {
            db.transaction { tx -> tx.markDocumentAsArchived(documentId, HelsinkiDateTime.now()) }
            logger.info { "Successfully archived document $documentId" }
        } else {
            logger.error {
                "Failed to archive document $documentId. Response code: $responseCode, Response body: ${responseBody ?: "No response body"}"
            }
            throw RuntimeException("Failed to archive document $documentId")
        }
    }
}
