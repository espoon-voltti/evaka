// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document.archival

import fi.espoo.evaka.document.childdocument.*
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.process.*
import fi.espoo.evaka.s3.DocumentKey
import fi.espoo.evaka.s3.DocumentService
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.voltti.logging.loggers.info
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import org.apache.http.client.utils.URLEncodedUtils
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
            uploadToArchive(db, msg.documentId, client, documentClient)
        }
    }
}

fun uploadToArchive(
    db: Database.Connection,
    documentId: ChildDocumentId,
    uploadClient: SärmäClientInterface,
    documentClient: DocumentService,
    logger: io.github.oshai.kotlinlogging.KLogger = KotlinLogging.logger {},
) {
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
    val archivedProcess = db.read { tx -> tx.getArchiveProcessByChildDocumentId(documentId) }
    val documentMetadata = db.read { tx -> tx.getChildDocumentMetadata(documentId) }

    val documentKey =
        db.read { tx ->
            tx.getChildDocumentKey(documentId)
                ?: throw NotFound("Document key not found for document $documentId")
        }

    // Get the document from the original location
    val originalLocation = documentClient.locate(DocumentKey.ChildDocument(documentKey))
    val documentContent = documentClient.get(originalLocation)
    val masterId =
        "yleinen" // Defined as fixed value in Evaka_Särmä_metatietomääritykset.xlsx specs. TODO
    // should be mapped from metadata
    val classId =
        document.template.processDefinitionNumber
            ?: throw IllegalStateException(
                "No class ID found in document template ${document.template.id}"
            )
    val virtualArchiveId =
        "YLEINEN" // Defined as fixed value in Evaka_Särmä_metatietomääritykset.xlsx specs. TODO
    // should be mapped from metadata

    // Create metadata object and convert to XML
    val metadata =
        createDocumentMetadata(
            document,
            documentMetadata,
            archivedProcess,
            Paths.get(documentContent.name).fileName.toString(),
            childInfo.identity,
            childInfo.dateOfBirth,
        )
    val metadataXml = marshalMetadata(metadata)

    val (responseCode, responseBody) =
        uploadClient.putDocument(documentContent, metadataXml, masterId, classId, virtualArchiveId)
    logger.info { "HTTP response code: $responseCode" }
    logger.info { "Response body: $responseBody" }

    val responseData = URLEncodedUtils.parse(responseBody, StandardCharsets.UTF_8)
    val statusCode = responseData.find { it.name == "status_code" }?.value?.toIntOrNull()

    val instanceId = responseData.find { it.name == "instance_ids" }?.value

    logger.info(mapOf("documentId" to documentId)) {
        "Parsed status code from response body: $statusCode"
    }

    // Audit log the instance ID
    if (instanceId == null) {
        logger.error { "No instance ID found in response body" }
    } else {
        logger.info(mapOf("documentId" to documentId, "instanceId" to instanceId)) {
            "Parsed instance ID from response body: $instanceId"
        }
    }

    if (responseCode != 200 || statusCode != 200) {
        logger.error {
            "Failed to archive document $documentId. Response code: $responseCode, Response body: ${responseBody ?: "No response body"}, Parsed status code: $statusCode"
        }
        throw RuntimeException("Failed to archive document $documentId")
    }

    db.transaction { tx -> tx.markDocumentAsArchived(documentId, HelsinkiDateTime.now()) }
    logger.info { "Successfully archived document $documentId" }
}
